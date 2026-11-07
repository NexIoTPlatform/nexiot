<template>
  <a-modal v-model:visible="visible" title="新建三元组生成任务" :width="modalWidth" :modal-style="{ maxWidth: 'calc(100vw - 24px)' }" :body-style="{ maxHeight: '76vh', overflowY: 'auto' }" :mask-closable="false" unmount-on-close @close="reset">
    <a-spin :loading="loading || submitting" class="credential-modal-loading">
      <section class="credential-section">
        <header><span>1</span><div><strong>选择 MQTT 产品</strong><small>仅平台直连 MQTT 产品支持批量生成设备凭证</small></div></header>
        <a-row :gutter="12">
          <a-col :xs="24" :md="12"><a-form-item label="产品" required><a-select v-model="form.productKey" :options="productOptions" allow-search placeholder="请选择 MQTT 产品" @change="selectProduct" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="任务名称"><a-input v-model.trim="form.taskName" allow-clear :max-length="128" placeholder="不填则按产品和时间自动命名" /></a-form-item></a-col>
        </a-row>
        <div v-if="product" class="product-context">
          <div><span>产品名称</span><strong>{{ product.name }}</strong></div>
          <div><span>ProductKey</span><code>{{ product.productKey }}</code></div>
          <div><span>接入方式</span><a-tag color="green">MQTT</a-tag></div>
          <div><span>设备类型</span><strong>{{ deviceNodeLabel(product.deviceNode) }}</strong></div>
        </div>
        <a-alert v-else-if="!loading && !products.length" type="warning">暂无可用 MQTT 产品，请先创建或切换产品接入方式。</a-alert>
      </section>

      <section class="credential-section">
        <header><span>2</span><div><strong>设置生成规则</strong><small>单次最多生成 5000 台设备，DeviceId 必须唯一</small></div></header>
        <a-form-item label="生成方式">
          <a-radio-group v-model="form.generateMode" type="button">
            <a-radio value="AUTO">自动编号</a-radio>
            <a-radio value="LIST">指定 DeviceId</a-radio>
          </a-radio-group>
        </a-form-item>
        <div v-if="form.generateMode === 'AUTO'" class="auto-grid">
          <a-form-item label="DeviceId 前缀"><a-input v-model.trim="form.deviceIdPrefix" allow-clear placeholder="DEV" /></a-form-item>
          <a-form-item label="起始序号"><a-input-number v-model="form.deviceIdStartNo" :min="0" :precision="0" /></a-form-item>
          <a-form-item label="序号长度"><a-input-number v-model="form.deviceIdLength" :min="1" :max="18" :precision="0" /></a-form-item>
          <a-form-item label="生成数量"><a-input-number v-model="form.count" :min="1" :max="5000" :precision="0" /></a-form-item>
        </div>
        <a-form-item v-else label="DeviceId 列表">
          <a-textarea v-model.trim="form.deviceIdsText" :auto-size="{ minRows: 7, maxRows: 12 }" placeholder="每行一个 DeviceId，也支持逗号、分号或空格分隔" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :xs="24" :md="10"><a-form-item label="设备名称前缀"><a-input v-model.trim="form.deviceNamePrefix" allow-clear placeholder="为空时使用 DeviceId" /></a-form-item></a-col>
          <a-col :xs="24" :md="14"><a-form-item label="设备备注"><a-input v-model.trim="form.detail" allow-clear placeholder="写入每台设备的备注" /></a-form-item></a-col>
        </a-row>
      </section>

      <section class="credential-section">
        <header><span>3</span><div><strong>确认连接凭证</strong><small>展示前 10 台设备的连接标识，密码将在任务完成后导出</small></div><a-tag color="arcoblue">{{ summaryCount }} 台</a-tag></header>
        <div class="credential-rules">
          <div><span>Client ID</span><code>ProductKey.DeviceId</code></div>
          <div><span>Username</span><code>DeviceId&amp;ProductKey</code></div>
          <div><span>Password</span><code>DeviceSecret</code></div>
        </div>
        <div class="credential-preview">
          <a-table row-key="rowKey" :data="previewRows" :columns="previewColumns" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', y: 220, minWidth: 920 }" />
        </div>
      </section>
    </a-spin>
    <template #footer>
      <a-button :disabled="submitting" @click="visible = false">取消</a-button>
      <a-button type="primary" :loading="submitting" :disabled="!products.length" @click="submit"><template #icon><icon-play-arrow /></template>创建任务</a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import type { TableColumnData } from '@arco-design/web-vue'
import type { CredentialTaskPayload } from '../api'
import type { ProductItem } from '@/apis/iot/product'
import { Message, Modal } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { getProductByKey, getProductList } from '@/apis/iot/product'
import { createCredentialTask } from '../api'

defineOptions({ name: 'TaskCenterCredentialModal' })

const emit = defineEmits<{ success: [taskId?: string] }>()
const DEVICE_ID_SEPARATOR_RE = /[,，;；\s]+/
const DEVICE_ID_PATTERN = /^[\w.:-]{1,64}$/
const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const products = ref<ProductItem[]>([])
const product = ref<ProductItem>()
const { width } = useWindowSize()
const modalWidth = computed(() => Math.min(940, Math.max(300, width.value - 24)))
const form = reactive({ taskName: '', productKey: '', generateMode: 'AUTO' as 'AUTO' | 'LIST', count: 10, deviceIdPrefix: 'DEV', deviceIdStartNo: 1, deviceIdLength: 6, deviceNamePrefix: '', deviceIdsText: '', detail: '' })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const textDeviceIds = computed(() => form.deviceIdsText.split(DEVICE_ID_SEPARATOR_RE).map((item) => item.trim()).filter(Boolean))
const previewDeviceIds = computed(() => {
  if (form.generateMode === 'LIST') return textDeviceIds.value.slice(0, 10)
  return Array.from({ length: Math.min(Number(form.count || 0), 10) }, (_, index) => `${form.deviceIdPrefix || 'DEV'}${String(Number(form.deviceIdStartNo || 0) + index).padStart(Number(form.deviceIdLength || 1), '0')}`)
})
const summaryCount = computed(() => form.generateMode === 'LIST' ? textDeviceIds.value.length : Number(form.count || 0))
const previewRows = computed(() => previewDeviceIds.value.map((deviceId, index) => ({ rowKey: `${deviceId}-${index}`, deviceId, deviceName: form.deviceNamePrefix ? `${form.deviceNamePrefix}${index + 1}` : deviceId, iotId: `${form.productKey || 'ProductKey'}${deviceId}`, clientId: `${form.productKey || 'ProductKey'}.${deviceId}`, username: `${deviceId}&${form.productKey || 'ProductKey'}` })))
const previewColumns: TableColumnData[] = [
  { title: 'DeviceId', dataIndex: 'deviceId', width: 180 },
  { title: '设备名称', dataIndex: 'deviceName', width: 180 },
  { title: 'IoTId', dataIndex: 'iotId', width: 220 },
  { title: 'Client ID', dataIndex: 'clientId', width: 220 },
  { title: 'Username', dataIndex: 'username', width: 220 }
]

async function open(productKey = '') {
  reset()
  loading.value = true
  try {
    const response = await getProductList({ page: 1, size: 1000 })
    products.value = (response.data.records || []).filter((item) => String(item.thirdPlatform || '').toLowerCase() === 'mqtt')
    if (productKey && products.value.some((item) => item.productKey === productKey)) {
      form.productKey = productKey
      await selectProduct(productKey)
    }
    visible.value = true
  } finally {
    loading.value = false
  }
}

async function selectProduct(value: string | number | boolean | Record<string, any>) {
  const productKey = String(value || '')
  form.productKey = productKey
  product.value = products.value.find((item) => item.productKey === productKey)
  if (!productKey) return
  try {
    const response = await getProductByKey(productKey)
    if (String(response.data?.thirdPlatform || '').toLowerCase() !== 'mqtt') {
      Message.warning('三元组生成仅支持 MQTT 接入产品')
      form.productKey = ''
      product.value = undefined
      return
    }
    product.value = response.data
  } catch {
    product.value = products.value.find((item) => item.productKey === productKey)
  }
}

function validate() {
  if (!form.productKey || !products.value.some((item) => item.productKey === form.productKey)) return '请选择 MQTT 产品'
  if (!summaryCount.value) return '请至少生成 1 台设备'
  if (summaryCount.value > 5000) return '一次最多生成 5000 台设备'
  const ids = allCheckIds()
  const invalid = ids.find((item) => !DEVICE_ID_PATTERN.test(item))
  if (invalid) return `DeviceId 格式不合法：${invalid}`
  if (form.generateMode === 'LIST') {
    const duplicated = ids.filter((item, index) => ids.indexOf(item) !== index)
    if (duplicated.length) return `DeviceId 重复：${[...new Set(duplicated)].slice(0, 3).join('、')}`
  }
  return ''
}

function allCheckIds() {
  if (form.generateMode === 'LIST') return textDeviceIds.value
  const first = previewDeviceIds.value[0]
  const last = `${form.deviceIdPrefix || 'DEV'}${String(Number(form.deviceIdStartNo || 0) + Number(form.count || 0) - 1).padStart(Number(form.deviceIdLength || 1), '0')}`
  return [first, last].filter(Boolean)
}

function submit() {
  const error = validate()
  if (error) {
    Message.warning(error)
    return
  }
  if (summaryCount.value > 1000) {
    Modal.warning({ title: `确认创建 ${summaryCount.value} 台设备三元组？`, content: '任务会异步执行，完成后可在任务详情导出。', hideCancel: false, onBeforeOk: doSubmit })
    return
  }
  doSubmit()
}

async function doSubmit() {
  submitting.value = true
  try {
    const payload: CredentialTaskPayload = {
      ...form,
      taskName: form.taskName.trim() || undefined,
      deviceIds: form.generateMode === 'LIST' ? [] : undefined,
      deviceIdsText: form.generateMode === 'LIST' ? textDeviceIds.value.join('\n') : undefined
    }
    const response = await createCredentialTask(payload)
    Message.success(response.message || '三元组生成任务已创建')
    visible.value = false
    emit('success', response.data?.taskId)
    return true
  } finally {
    submitting.value = false
  }
}

function deviceNodeLabel(value?: string) {
  return ({ DEVICE: '直连设备', GATEWAY: '网关设备', GATEWAY_SUB_DEVICE: '网关子设备', VIDEO_DEVICE: '视频设备' } as Record<string, string>)[value || ''] || value || '-'
}

function reset() {
  product.value = undefined
  Object.assign(form, { taskName: '', productKey: '', generateMode: 'AUTO', count: 10, deviceIdPrefix: 'DEV', deviceIdStartNo: 1, deviceIdLength: 6, deviceNamePrefix: '', deviceIdsText: '', detail: '' })
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.credential-modal-loading {
  display: block;
  width: 100%;
  min-width: 0;
}

.credential-section {
  min-width: 0;
  padding: 18px 0;
  border-top: 1px solid var(--color-border-2);
}

.credential-section:first-child {
  padding-top: 0;
  border-top: 0;
}

.credential-section > header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.credential-section > header > span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: rgb(var(--primary-6));
  background: var(--color-primary-light-1);
  border-radius: 50%;
}

.credential-section > header > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.credential-section header small,
.product-context span,
.credential-rules span {
  font-size: 12px;
  color: var(--color-text-3);
}

.product-context,
.credential-rules,
.auto-grid {
  display: grid;
  gap: 10px;
}

.product-context {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 12px;
  background: var(--color-fill-1);
  border-radius: 6px;
}

.product-context > div,
.credential-rules > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.auto-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.auto-grid :deep(.arco-input-number) {
  width: 100%;
}

.credential-rules {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 12px;
}

.credential-rules > div {
  padding: 10px 12px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
}

.credential-preview {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
}

@media (width <= 720px) {
  .product-context,
  .auto-grid {
    grid-template-columns: 1fr 1fr;
  }

  .credential-rules {
    grid-template-columns: 1fr;
  }
}

@media (width <= 420px) {
  .product-context,
  .auto-grid {
    grid-template-columns: 1fr;
  }
}
</style>
