<template>
  <section class="storage-panel">
    <header class="storage-panel__header">
      <div><strong><icon-database /> 设备数据留存策略</strong><p>日志存储策略与物模型留存项相互独立，可分别修改。</p></div>
      <a-space v-hasPerm="['system:product:edit']" wrap>
        <a-button type="text" @click="openPolicyEditor"><template #icon><icon-swap /></template>修改存储策略</a-button>
        <a-button type="text" @click="openRetentionEditor"><template #icon><icon-edit /></template>编辑留存项</a-button>
      </a-space>
    </header>

    <a-descriptions :column="1" bordered>
      <a-descriptions-item label="当前日志存储策略">{{ policyLabel }}</a-descriptions-item>
    </a-descriptions>
    <a-table v-if="storageRows.length" :data="storageRows" :columns="columns" :pagination="false" :bordered="{ cell: true }" class="storage-panel__table" />
    <a-empty v-else class="storage-panel__empty" description="暂未选择需要留存的属性或事件" />

    <a-modal
      v-model:visible="policyVisible"
      title="修改日志存储策略"
      width="min(520px, calc(100vw - 32px))"
      :mask-closable="false"
      :ok-loading="policySaving"
      unmount-on-close
      @before-ok="savePolicy"
    >
      <a-alert type="warning">切换后，原存储策略中的历史数据可能无法继续查询，请确认后再修改。</a-alert>
      <a-form :model="policyFormModel" layout="vertical" class="storage-panel__modal-form">
        <a-form-item label="日志存储策略" required>
          <a-select v-model="policyForm" :options="policyOptions" placeholder="请选择日志存储策略" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="retentionVisible"
      title="编辑数据留存项"
      width="min(760px, calc(100vw - 32px))"
      :mask-closable="false"
      :ok-loading="retentionSaving"
      unmount-on-close
      @before-ok="saveRetention"
    >
      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <div class="storage-selector">
          <div class="storage-selector__header"><span>留存属性</span><a-space :size="4"><a-button type="text" size="mini" :disabled="!propertyOptions.length" @click="selectedProperties = propertyOptions.map((item) => item.value)">全选</a-button><a-button type="text" size="mini" :disabled="!selectedProperties.length" @click="selectedProperties = []">清空</a-button></a-space></div>
          <a-form-item :extra="`已选 ${selectedProperties.length} 项`">
            <a-select v-model="selectedProperties" multiple allow-search allow-clear :options="propertyOptions" placeholder="可选择多个属性" />
          </a-form-item>
        </div>
        <div class="storage-selector">
          <div class="storage-selector__header"><span>留存事件</span><a-space :size="4"><a-button type="text" size="mini" :disabled="!eventOptions.length" @click="selectedEvents = eventOptions.map((item) => item.value)">全选</a-button><a-button type="text" size="mini" :disabled="!selectedEvents.length" @click="selectedEvents = []">清空</a-button></a-space></div>
          <a-form-item :extra="`已选 ${selectedEvents.length} 项`">
            <a-select v-model="selectedEvents" multiple allow-search allow-clear :options="eventOptions" placeholder="可选择多个事件" />
          </a-form-item>
        </div>
      </a-grid>
    </a-modal>
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ProductItem, ProductMetadataItem, ProductStorePolicy } from '@/apis/iot/product'
import { Message, Tag } from '@arco-design/web-vue'
import { getEnabledStorePolicies, getProductConfigDicts, getProductEvents, getProductProperties, updateProduct, updateProductStoreConfig } from '@/apis/iot/product'

defineOptions({ name: 'ProductStoragePanel' })

const props = defineProps<{ product: ProductItem }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()

interface StorageEntry {
  id: string
  maxStorage?: number
}

const policyVisible = ref(false)
const retentionVisible = ref(false)
const policySaving = ref(false)
const retentionSaving = ref(false)
const properties = ref<ProductMetadataItem[]>([])
const events = ref<ProductMetadataItem[]>([])
const policies = ref<{ label: string, value: string }[]>([])
const policyForm = ref('')
const selectedProperties = ref<string[]>([])
const selectedEvents = ref<string[]>([])

const policyFormModel = computed(() => ({ policy: policyForm.value }))
const propertyOptions = computed(() => properties.value.map((item) => ({ label: `${item.name} (${item.id})`, value: item.id })))
const eventOptions = computed(() => events.value.map((item) => ({ label: `${item.name} (${item.id})`, value: item.id })))
const currentPolicy = computed(() => normalizePolicy(props.product.storePolicy))
const policyOptions = computed(() => {
  if (!currentPolicy.value || policies.value.some((item) => item.value === currentPolicy.value)) return policies.value
  return [{ label: `${policyName(currentPolicy.value)}（当前已禁用）`, value: currentPolicy.value, disabled: true }, ...policies.value]
})
const policyLabel = computed(() => policyName(currentPolicy.value))
const storageRows = computed(() => [
  ...selectedProperties.value.map((id) => ({ id, name: properties.value.find((item) => item.id === id)?.name || id, type: 'property' })),
  ...selectedEvents.value.map((id) => ({ id, name: events.value.find((item) => item.id === id)?.name || id, type: 'event' }))
])
const columns: TableColumnData[] = [
  { title: '标识', dataIndex: 'id', width: 220 },
  { title: '名称', dataIndex: 'name' },
  { title: '类型', width: 100, render: ({ record }) => <Tag color={record.type === 'property' ? 'arcoblue' : 'orange'}>{record.type === 'property' ? '属性' : '事件'}</Tag> }
]

function normalizePolicy(value?: string) {
  return String(value || 'none').trim().toLowerCase()
}

function policyCode(item: ProductStorePolicy | string) {
  if (typeof item === 'string') return normalizePolicy(item)
  return normalizePolicy(String(item.value || item.policyCode || item.label || item.policyName || ''))
}

function policyName(value: string) {
  return policies.value.find((item) => item.value === value)?.label || value || '不存储'
}

function parseStorage(value?: string) {
  try {
    const data = JSON.parse(value || '{}') as { properties?: StorageEntry[], event?: StorageEntry[], events?: StorageEntry[] }
    return { properties: Array.isArray(data.properties) ? data.properties : [], events: Array.isArray(data.event) ? data.event : Array.isArray(data.events) ? data.events : [] }
  } catch {
    return { properties: [], events: [] }
  }
}

function resetSelection() {
  const config = parseStorage(props.product.storePolicyConfiguration)
  selectedProperties.value = config.properties.map((item) => item.id).filter(Boolean)
  selectedEvents.value = config.events.map((item) => item.id).filter(Boolean)
}

function openPolicyEditor() {
  policyForm.value = currentPolicy.value
  policyVisible.value = true
}

function openRetentionEditor() {
  resetSelection()
  retentionVisible.value = true
}

async function savePolicy() {
  if (!policyForm.value) {
    Message.error('请选择日志存储策略')
    return false
  }
  if (policyForm.value === currentPolicy.value) return true
  policySaving.value = true
  try {
    await updateProduct({ id: props.product.id, storePolicy: policyForm.value })
    Message.success('日志存储策略修改成功')
    emit('refresh')
    return true
  } finally {
    policySaving.value = false
  }
}

async function saveRetention() {
  const original = parseStorage(props.product.storePolicyConfiguration)
  const propertyLimits = new Map(original.properties.map((item) => [item.id, item.maxStorage || 0]))
  const eventLimits = new Map(original.events.map((item) => [item.id, item.maxStorage || 0]))
  const storePolicyConfiguration = JSON.stringify({
    properties: selectedProperties.value.map((id) => ({ id, maxStorage: propertyLimits.get(id) || 0 })),
    event: selectedEvents.value.map((id) => ({ id, maxStorage: eventLimits.get(id) || 0 }))
  })
  retentionSaving.value = true
  try {
    await updateProductStoreConfig({ id: props.product.id, storePolicyConfiguration })
    Message.success('数据留存项保存成功')
    emit('refresh')
    return true
  } finally {
    retentionSaving.value = false
  }
}

async function loadOptions() {
  const [propertyRes, eventRes, policyRes, dictRes] = await Promise.all([
    getProductProperties(props.product.id),
    getProductEvents(props.product.id),
    getEnabledStorePolicies(),
    getProductConfigDicts(['log_store_policy'])
  ])
  properties.value = propertyRes.data || []
  events.value = eventRes.data || []
  const enabled = new Set((policyRes.data || []).map(policyCode))
  const labels = new Map((dictRes.data.log_store_policy || []).map((item) => [normalizePolicy(item.dictValue), item.dictLabel]))
  policies.value = [...enabled].filter(Boolean).map((value) => ({ label: labels.get(value) || value, value }))
}

watch(() => props.product, () => {
  resetSelection()
  loadOptions()
}, { immediate: true })
</script>

<style lang="scss" scoped>
.storage-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 14px;

    strong {
      display: flex;
      gap: 7px;
      align-items: center;
      font-size: 15px;
    }

    p {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__table,
  &__empty {
    margin-top: 12px;
  }

  &__modal-form {
    margin-top: 18px;
  }
}

.storage-selector__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 28px;
  margin-bottom: 4px;
  font-size: 14px;
  color: var(--color-text-2);
}

.storage-selector :deep(.arco-form-item) {
  margin-bottom: 0;
}

@media (width <= 600px) {
  .storage-panel__header {
    flex-direction: column;
  }
}
</style>
