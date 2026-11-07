<template>
  <a-drawer v-model:visible="visible" title="创建升级计划" width="min(720px, 100%)" :mask-closable="false" :ok-text="form.autoDispatch ? '开始升级' : '保存计划'" :ok-button-props="{ disabled: productLoading || !profileLoaded }" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12">
        <a-form-item field="productKey" label="所属产品" :rules="required('请选择产品')"><a-select v-model="form.productKey" :options="productOptions" :loading="productSearchLoading" allow-search placeholder="请选择产品" @search="searchProducts" @dropdown-visible-change="refreshProducts" @change="handleProduct" /></a-form-item>
        <a-form-item field="packageId" label="已发布升级包" :rules="required('请选择已发布升级包')"><a-select v-model="form.packageId" :options="packageOptions" :loading="productLoading" :disabled="!form.productKey || productLoading" allow-search placeholder="选择要升级的版本" @change="handlePackage" /></a-form-item>
      </a-grid>
      <a-alert v-if="form.productKey && !productLoading && !profileLoaded" type="warning" class="ota-task-form__notice">当前厂家适配未能加载，请重试；如后端尚未升级，需要先更新后端才能使用此功能。<a-link @click="handleProduct(form.productKey)">重新加载</a-link></a-alert>
      <a-alert v-if="profileLoaded" class="ota-task-form__notice">
        <template #title>本次传输：{{ transferLabel }}<template v-if="profile.mode !== 'URL'"> · 每包 {{ profile.chunkSize }} 字节<template v-if="chunkCount"> · 共 {{ chunkCount }} 包</template></template></template>
        {{ profile.mode === 'URL' ? '设备获取完整文件的下载地址。' : '平台自动切包，保留已确认的分片进度；收齐文件后仍需设备安装并上报结果。' }}
        <a-link @click="profileModalRef?.open(form.productKey)">厂家适配</a-link>
      </a-alert>
      <a-form-item field="taskName" label="计划名称" :rules="required('请输入计划名称')"><a-input v-model.trim="form.taskName" placeholder="选择升级包后自动填写" /></a-form-item>
      <a-form-item label="升级范围"><a-radio-group v-model="form.rolloutMode" type="button" :options="modeOptions" /></a-form-item>
      <a-form-item v-if="form.rolloutMode === 'SPECIFIED'" field="deviceIds" label="目标设备" :rules="required('请选择至少一台设备')">
        <a-select v-model="form.deviceIds" :options="deviceOptions" :loading="productLoading || deviceSearchLoading" :disabled="!form.productKey || productLoading" :filter-option="false" multiple allow-search placeholder="输入设备编号搜索，建议先选 1 台验证" @search="handleDeviceSearch" />
      </a-form-item>
      <a-alert v-if="form.rolloutMode === 'ALL'" type="warning" class="ota-task-form__notice">此计划将面向该产品的全部符合条件的设备。请先用指定设备验证固件。</a-alert>
      <a-grid v-if="form.rolloutMode === 'GRAY'" :cols="{ xs: 1, sm: 2 }" :col-gap="12"><a-form-item label="灰度百分比"><a-input-number v-model="form.grayPercent" :min="1" :max="100" :precision="0" /></a-form-item><a-form-item label="灰度设备上限"><a-input-number v-model="form.grayDeviceLimit" :min="1" :precision="0" /></a-form-item></a-grid>
      <a-collapse>
        <a-collapse-item key="advanced" header="高级设置（分批、定时与失败处理）">
          <a-grid :cols="{ xs: 1, sm: 3 }" :col-gap="12">
            <a-form-item label="每批设备数"><a-input-number v-model="form.batchSize" :min="1" :precision="0" /></a-form-item>
            <a-form-item label="批次间隔（秒）"><a-input-number v-model="form.batchIntervalSeconds" :min="0" :precision="0" /></a-form-item>
            <a-form-item label="单设备超时（秒）"><a-input-number v-model="form.timeoutSeconds" :min="30" :precision="0" /></a-form-item>
          </a-grid>
          <div class="ota-task-form__hint">每批设备数决定同时安排多少台设备，与固件的分包大小无关。</div>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12"><a-form-item label="整次升级重试次数"><a-input-number v-model="form.maxRetryCount" :min="0" :max="10" :precision="0" /></a-form-item><a-form-item label="失败阈值（%）"><a-input-number v-model="form.failureThresholdPercent" :min="0" :max="100" /></a-form-item></a-grid>
          <a-form-item label="计划开始时间"><a-date-picker v-model="form.plannedStartTime" class="ota-task-form__date" show-time value-format="YYYY-MM-DD HH:mm:ss" /></a-form-item>
          <a-space wrap><a-checkbox v-model="form.requireVersionCheck">检查当前版本</a-checkbox><a-checkbox v-model="form.rollbackOnFailure">失败自动回滚</a-checkbox><a-checkbox v-model="form.autoDispatch">创建后自动执行</a-checkbox></a-space>
          <a-form-item v-if="form.rollbackOnFailure" label="回滚升级包" class="ota-task-form__rollback"><a-select v-model="form.rollbackPackageId" :options="rollbackOptions" /></a-form-item>
          <a-form-item label="备注" class="ota-task-form__rollback"><a-textarea v-model="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" /></a-form-item>
        </a-collapse-item>
      </a-collapse>
    </a-form>
    <OtaTransferProfileModal ref="profileModalRef" :products="availableProducts" @save-success="reloadProfile" />
  </a-drawer>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { OtaPackageItem } from '@/apis/iot/ota'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { useDebounceFn } from '@vueuse/core'
import { createOtaTask, getOtaPackageList } from '@/apis/iot/ota'
import { getProductDeviceList } from '@/apis/iot/product'
import { createTransferProfile, getOtaTransferProfile, transferModeOptions } from '../api'
import { useOtaProducts } from '../useOtaProducts'

import OtaTransferProfileModal from './OtaTransferProfileModal.vue'

defineOptions({ name: 'IotOtaTaskFormDrawer' })
const props = defineProps<{ products: ProductItem[] }>()
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const profileModalRef = useTemplateRef<InstanceType<typeof OtaTransferProfileModal>>('profileModalRef')
const visible = ref(false)
const productLoading = ref(false)
const profileLoaded = ref(false)
const deviceSearchLoading = ref(false)
const packages = ref<OtaPackageItem[]>([])
const deviceOptions = ref<{ label: string, value: string }[]>([])
const profile = reactive(createTransferProfile())
const form = reactive(createForm())
const { availableProducts, productOptions, productSearchLoading, searchProducts, refreshProducts } = useOtaProducts(() => props.products)
const packageOptions = computed(() => packages.value.filter((item) => item.status === 'PUBLISHED').map((item) => ({ label: `${item.packageName} (${item.version})`, value: String(item.id) })))
const rollbackOptions = computed(() => packageOptions.value.filter((item) => item.value !== form.packageId))
const transferLabel = computed(() => transferModeOptions.find((item) => item.value === profile.mode)?.label || profile.mode)
const chunkCount = computed(() => {
  const fileSize = packages.value.find((item) => String(item.id) === form.packageId)?.fileSize
  return fileSize ? Math.ceil(fileSize / profile.chunkSize) : 0
})
const modeOptions = [{ label: '指定设备', value: 'SPECIFIED' }, { label: '灰度发布', value: 'GRAY' }, { label: '全部设备', value: 'ALL' }]
const required = (message: string) => [{ required: true, message }]
let requestId = 0
let deviceRequestId = 0
const searchDevices = useDebounceFn(async (key: string, value: string, currentRequest: number) => {
  if (!key || currentRequest !== deviceRequestId) return
  deviceSearchLoading.value = true
  try {
    const { data } = await getProductDeviceList({ page: 1, size: 500, productKey: key, deviceId: value.trim() || undefined })
    if (currentRequest !== deviceRequestId) return
    const selected = deviceOptions.value.filter((item) => form.deviceIds.includes(item.value))
    const options = (data.records || []).map((item) => ({ label: `${item.deviceName || item.deviceId} (${item.deviceId})`, value: item.deviceId }))
    deviceOptions.value = [...new Map([...selected, ...options].map((item) => [item.value, item])).values()]
  } finally {
    if (currentRequest === deviceRequestId) deviceSearchLoading.value = false
  }
}, 250)

function handleDeviceSearch(value: string) {
  searchDevices(form.productKey, value, ++deviceRequestId).catch(() => {})
}

function createForm() {
  return { productKey: '', productId: '', packageId: '', taskName: '', rolloutMode: 'SPECIFIED', deviceIds: [] as string[], maxRetryCount: 3, timeoutSeconds: 300, plannedStartTime: '', requireVersionCheck: true, grayPercent: 10, grayDeviceLimit: 100, batchSize: 50, batchIntervalSeconds: 0, failureThresholdPercent: 20, rollbackOnFailure: false, rollbackPackageId: '', description: '', autoDispatch: true }
}
function reset() {
  requestId++
  deviceRequestId++
  Object.assign(form, createForm())
  Object.assign(profile, createTransferProfile())
  packages.value = []
  deviceOptions.value = []
  productLoading.value = false
  profileLoaded.value = false
  deviceSearchLoading.value = false
  formRef.value?.clearValidate()
}
async function handleProduct(value: unknown) {
  const key = String(value || '')
  const currentRequest = ++requestId
  deviceRequestId++
  deviceSearchLoading.value = false
  const product = availableProducts.value.find((item) => item.productKey === key)
  form.productId = product?.id || ''
  form.packageId = ''
  form.deviceIds = []
  form.rollbackPackageId = ''
  packages.value = []
  deviceOptions.value = []
  profileLoaded.value = false
  if (!key) return
  productLoading.value = true
  try {
    const [packageRes, deviceRes, profileRes] = await Promise.all([getOtaPackageList({ page: 1, size: 500, productKey: key, status: 'PUBLISHED' }), getProductDeviceList({ page: 1, size: 500, productKey: key }), getOtaTransferProfile(key)])
    if (currentRequest !== requestId) return
    packages.value = packageRes.data.records || []
    deviceOptions.value = (deviceRes.data.records || []).map((item) => ({ label: `${item.deviceName || item.deviceId} (${item.deviceId})`, value: item.deviceId }))
    Object.assign(profile, createTransferProfile(), profileRes.data)
    profileLoaded.value = true
  } finally {
    if (currentRequest === requestId) productLoading.value = false
  }
}
async function reloadProfile() {
  const key = form.productKey
  const currentRequest = requestId
  profileLoaded.value = false
  const { data } = await getOtaTransferProfile(key)
  if (currentRequest !== requestId) return
  Object.assign(profile, createTransferProfile(), data)
  profileLoaded.value = true
}
function handlePackage(value: unknown) {
  const item = packages.value.find((record) => String(record.id) === String(value || ''))
  if (item && !form.taskName) form.taskName = `${item.packageName} ${item.version}`
}
async function findTargetDevice(productKey: string, deviceId: string, currentRequest: number) {
  const size = 50
  let loadedCount = 0
  for (let page = 1; ; page++) {
    if (currentRequest !== requestId || !visible.value || form.productKey !== productKey) return
    const { data } = await getProductDeviceList({ page, size, productKey, deviceId })
    if (currentRequest !== requestId || !visible.value || form.productKey !== productKey) return
    const records = data.records || []
    const device = records.find((item) => item.deviceId === deviceId && item.productKey === productKey)
    if (device) return device
    loadedCount += records.length
    if (!records.length || loadedCount >= data.total) return
  }
}
async function open(productKey?: string, deviceIds: string[] = []) {
  reset()
  visible.value = true
  if (productKey) {
    form.productKey = productKey
    const currentRequest = requestId + 1
    await handleProduct(productKey)
    if (currentRequest !== requestId || !visible.value || form.productKey !== productKey) return
    const selectedIds = [...new Set(deviceIds.filter(Boolean))]
    const missingIds = selectedIds.filter((id) => !deviceOptions.value.some((item) => item.value === id))
    productLoading.value = true
    try {
      const devices = await Promise.all(missingIds.map((deviceId) => findTargetDevice(productKey, deviceId, currentRequest)))
      if (currentRequest !== requestId || !visible.value || form.productKey !== productKey) return
      devices.forEach((item) => {
        if (item && !deviceOptions.value.some((option) => option.value === item.deviceId)) deviceOptions.value.push({ label: `${item.deviceName || item.deviceId} (${item.deviceId})`, value: item.deviceId })
      })
      form.deviceIds = selectedIds.filter((id) => deviceOptions.value.some((item) => item.value === id))
      if (form.deviceIds.length !== selectedIds.length) Message.warning('部分目标设备已不可用，请重新选择')
    } finally {
      if (currentRequest === requestId) productLoading.value = false
    }
  }
}
async function save() {
  if (productLoading.value || !profileLoaded.value || await formRef.value?.validate()) return false
  if (form.rolloutMode === 'SPECIFIED' && !form.deviceIds.length) {
    Message.error('请选择目标设备')
    return false
  }
  if (form.rollbackOnFailure && !form.rollbackPackageId) {
    Message.error('请选择回滚升级包')
    return false
  }
  // 非灰度范围不发送隐藏参数，后端默认使用全部候选设备且不限制数量。
  const { grayPercent, grayDeviceLimit, ...task } = form
  await createOtaTask(form.rolloutMode === 'GRAY' ? { ...task, grayPercent, grayDeviceLimit } : task)
  Message.success(form.autoDispatch ? '升级计划已创建并开始执行' : '升级计划已保存')
  emit('save-success')
  return true
}
defineExpose({ open })
</script>

<style lang="scss" scoped>
.ota-task-form {
  &__notice {
    margin-bottom: 16px;
  }

  &__hint {
    margin-top: -8px;
    margin-bottom: 16px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__date {
    width: 100%;
  }

  &__rollback {
    margin-top: 16px;
  }
}
</style>
