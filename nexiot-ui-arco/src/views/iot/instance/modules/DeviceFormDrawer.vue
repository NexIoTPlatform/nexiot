<template>
  <a-drawer v-model:visible="visible" :title="isEdit ? '编辑设备' : '新增设备'" width="min(620px, 100%)" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item field="productKey" label="所属产品" :rules="[{ required: true, message: '请选择所属产品' }]">
        <a-select v-model="form.productKey" :options="productOptions" :loading="productLoading" :disabled="isEdit" allow-search placeholder="请选择或搜索产品" @change="handleProductChange" />
      </a-form-item>
      <a-form-item v-if="selectedProduct?.deviceNode === 'GATEWAY_SUB_DEVICE'" field="extDeviceId" label="所属网关设备" :rules="[{ required: true, message: '请选择所属网关设备' }]">
        <a-select v-model="form.extDeviceId" :options="gatewayOptions" allow-search placeholder="请选择所属网关设备" />
      </a-form-item>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12">
        <a-form-item field="deviceId" label="DeviceId" :rules="deviceIdRules"><a-input v-model.trim="form.deviceId" :disabled="isEdit" placeholder="设备唯一标识" /></a-form-item>
        <a-form-item field="deviceName" label="设备名称" :rules="[{ required: true, message: '请输入设备名称' }]"><a-input v-model.trim="form.deviceName" placeholder="便于识别的设备名称" /></a-form-item>
      </a-grid>
      <a-form-item label="设备密钥"><a-input-password v-model="form.secretKey" :disabled="isEdit" placeholder="按接入方式要求填写，空值由平台生成" /></a-form-item>
      <a-form-item label="所属部门"><a-tree-select v-model="form.deptId" :data="deptOptions" :field-names="{ key: 'id', title: 'name', children: 'children' }" allow-search allow-clear placeholder="不选择时使用当前账号部门" /></a-form-item>

      <section v-if="customFields.length" class="device-form__config">
        <h4>设备接入参数</h4>
        <a-form-item v-for="field in customFields" :key="field.id" :label="field.name" :required="field.required" :extra="field.helpText || field.description">
          <a-input-number v-if="field.type === 'number'" :model-value="getNumber(field.id)" :min="field.min" :max="field.max" @update:model-value="setCustom(field.id, $event)" />
          <a-select v-else-if="field.type === 'select'" :model-value="getPrimitive(field.id)" :options="field.options" allow-clear @update:model-value="setCustom(field.id, $event)" />
          <a-switch v-else-if="field.type === 'boolean'" :model-value="Boolean(form.customValues[field.id])" @update:model-value="setCustom(field.id, $event)" />
          <a-input-password v-else-if="field.type === 'password'" :model-value="getText(field.id)" :disabled="field.disabled" :max-length="field.maxLength" :placeholder="field.placeholder" @update:model-value="setCustom(field.id, $event)" />
          <a-textarea v-else-if="field.type === 'textarea'" :model-value="getText(field.id)" :disabled="field.disabled" :max-length="field.maxLength" :auto-size="{ minRows: 3, maxRows: 6 }" :placeholder="field.placeholder" @update:model-value="setCustom(field.id, $event)" />
          <a-input v-else :model-value="getText(field.id)" :disabled="field.disabled" :max-length="field.maxLength" :placeholder="field.placeholder" @update:model-value="setCustom(field.id, $event)" />
        </a-form-item>
      </section>

      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12">
        <a-form-item label="经度"><a-input-number v-model="form.longitude" :min="-180" :max="180" placeholder="-180 ~ 180" /></a-form-item>
        <a-form-item label="纬度"><a-input-number v-model="form.latitude" :min="-90" :max="90" placeholder="-90 ~ 90" /></a-form-item>
      </a-grid>
      <a-form-item label="备注"><a-textarea v-model="form.detail" :max-length="500" show-word-limit :auto-size="{ minRows: 3, maxRows: 6 }" /></a-form-item>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { DeviceCustomField } from './deviceCustomFields'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import type { ListItem as DeptItem } from '@/apis/system/dept'
import { Message } from '@arco-design/web-vue'
import { addDevice, getDevice, updateDevice } from '@/apis/iot/device'
import { getProduct, getProductByKey, getProductGatewayDevices, getProductList } from '@/apis/iot/product'
import { getDeptOptions } from '@/apis/system/dept'
import {
  normalizeDeviceCustomValues,
  visibleDeviceCustomFields
} from './deviceCustomFields'

defineOptions({ name: 'IotDeviceFormDrawer' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const products = ref<ProductItem[]>([])
const selectedProduct = ref<ProductItem>()
const productLoading = ref(false)
let productRequestId = 0
const deptOptions = ref<DeptItem[]>([])
const gatewayOptions = ref<{ label: string, value: string }[]>([])
const form = reactive(createForm())
const isEdit = computed(() => Boolean(form.id))
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const deviceIdRules = [{ required: true, message: '请输入设备唯一标识' }, { match: /^[\w-]+$/, message: '仅支持字母、数字、下划线和短横线' }]
const allCustomFields = computed<DeviceCustomField[]>(() => {
  try {
    const config = JSON.parse(selectedProduct.value?.thirdConfiguration || '{}') as { customField?: DeviceCustomField[] }
    return config.customField || []
  } catch {
    return []
  }
})
const customFields = computed(() => visibleDeviceCustomFields(allCustomFields.value, form.customValues))

function createForm() {
  return { id: '', productKey: '', deviceId: '', deviceName: '', secretKey: '', deptId: '', extDeviceId: '', longitude: undefined as number | undefined, latitude: undefined as number | undefined, detail: '', customValues: {} as Record<string, unknown> }
}

function reset() {
  productRequestId += 1
  productLoading.value = false
  Object.assign(form, createForm())
  selectedProduct.value = undefined
  gatewayOptions.value = []
  formRef.value?.resetFields()
}

function parseObject(value?: string) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' ? parsed as Record<string, unknown> : {}
  } catch {
    return {}
  }
}

function normalizeCustomValues(source: Record<string, unknown> = {}) {
  return normalizeDeviceCustomValues(allCustomFields.value, source)
}

async function loadBaseOptions() {
  const [productRes, deptRes] = await Promise.all([
    getProductList({ page: 1, size: 500 }),
    getDeptOptions()
  ])
  products.value = productRes.data.records || []
  deptOptions.value = deptRes.data
}

async function handleProductChange(value: unknown) {
  const productKey = String(value || '')
  const requestId = ++productRequestId
  selectedProduct.value = undefined
  gatewayOptions.value = []
  form.extDeviceId = ''
  form.customValues = {}
  productLoading.value = Boolean(productKey)
  if (!productKey) return
  try {
    // 产品列表仅包含摘要，接入字段和协议配置以产品详情为准。
    const summary = products.value.find((item) => item.productKey === productKey)
    const product = (await (summary?.id ? getProduct(summary.id) : getProductByKey(productKey))).data
    if (requestId !== productRequestId) return
    if (!product?.productKey) {
      Message.error('产品信息加载失败，请重新选择产品')
      return
    }
    selectedProduct.value = product
    if (!summary) products.value.push(product)
    form.customValues = normalizeCustomValues()
    if (product.deviceNode === 'GATEWAY_SUB_DEVICE') {
      const devices = (await getProductGatewayDevices(productKey)).data || []
      if (requestId !== productRequestId) return
      gatewayOptions.value = devices.map((item) => ({ label: `${item.deviceName || item.deviceId} (${item.deviceId})`, value: item.deviceId }))
    }
  } finally {
    if (requestId === productRequestId) productLoading.value = false
  }
}

async function add(productKey?: string) {
  reset()
  await loadBaseOptions()
  if (productKey) {
    form.productKey = productKey
    await handleProductChange(productKey)
  }
  visible.value = true
}

async function edit(id: string) {
  reset()
  const [deviceRes] = await Promise.all([getDevice(id), loadBaseOptions()])
  const device = deviceRes.data
  await handleProductChange(device.productKey)
  Object.assign(form, device, {
    id: String(device.id),
    longitude: device.longitude === undefined ? undefined : Number(device.longitude),
    latitude: device.latitude === undefined ? undefined : Number(device.latitude),
    customValues: normalizeCustomValues(parseObject(device.devConfiguration ?? device.configuration))
  })
  visible.value = true
}

function getText(id: string) {
  const value = form.customValues[id]
  return value === undefined || value === null ? '' : String(value)
}

function getNumber(id: string) {
  const value = form.customValues[id]
  return value === undefined || value === '' ? undefined : Number(value)
}

function getPrimitive(id: string) {
  const value = form.customValues[id]
  return ['string', 'number', 'boolean'].includes(typeof value) ? value as string | number | boolean : undefined
}

function setCustom(id: string, value: unknown) {
  form.customValues[id] = value
  form.customValues = normalizeCustomValues(form.customValues)
}

async function save() {
  if (await formRef.value?.validate()) return false
  if (productLoading.value || !selectedProduct.value || selectedProduct.value.productKey !== form.productKey) {
    Message.warning(productLoading.value ? '正在加载产品接入参数，请稍后再保存' : '请重新选择产品并加载接入参数')
    return false
  }
  form.customValues = normalizeCustomValues(form.customValues)
  const missing = customFields.value.find((field) => field.required && (form.customValues[field.id] === undefined || form.customValues[field.id] === ''))
  if (missing) {
    Message.error(`请填写${missing.name}`)
    return false
  }
  const payload: Partial<DeviceItem> = {
    ...form,
    gwDeviceId: form.extDeviceId || undefined,
    configuration: JSON.stringify(form.customValues)
  }
  delete (payload as Partial<DeviceItem> & { customValues?: Record<string, unknown> }).customValues
  await (isEdit.value ? updateDevice(payload) : addDevice(payload))
  Message.success(isEdit.value ? '设备保存成功' : '设备添加成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.device-form {
  &__summary {
    margin-bottom: 16px;
  }

  &__config {
    padding: 14px;
    margin-bottom: 16px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-1);
    border-radius: 6px;

    h4 {
      margin: 0 0 14px;
      font-size: 14px;
    }
  }
}

:deep(.arco-input-number) {
  width: 100%;
}
</style>
