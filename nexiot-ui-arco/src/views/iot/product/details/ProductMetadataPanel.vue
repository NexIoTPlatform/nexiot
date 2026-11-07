<template>
  <section class="metadata-panel">
    <div class="metadata-panel__toolbar">
      <a-tabs v-model:active-key="activeSection" type="rounded" class="metadata-panel__sections">
        <a-tab-pane key="properties" :title="`属性 (${metadata.properties.length})`" />
        <a-tab-pane key="events" :title="`事件 (${metadata.events.length})`" />
        <a-tab-pane key="functions" :title="`功能 (${metadata.functions.length})`" />
      </a-tabs>
      <a-space wrap>
        <a-button @click="viewModalRef?.open()"><template #icon><IconEye /></template>查看物模型</a-button>
        <a-button v-if="canImport" @click="importModalRef?.open('json')"><template #icon><icon-import /></template>导入物模型</a-button>
        <a-button v-if="!draft" @click="loadData"><template #icon><icon-refresh /></template>刷新</a-button>
        <a-dropdown v-if="canAdd">
          <a-button type="primary"><template #icon><icon-plus /></template>快速创建<icon-down /></a-button>
          <template #content>
            <a-doption @click="fieldModalRef?.open('properties')"><icon-settings /> 添加属性</a-doption>
            <a-doption @click="fieldModalRef?.open('events')"><icon-notification /> 添加事件</a-doption>
            <a-doption @click="fieldModalRef?.open('functions')"><icon-code-block /> 添加功能</a-doption>
            <a-divider v-if="!draft" margin="4px" />
            <a-doption v-if="!draft" @click="importModalRef?.open('standard')"><icon-cloud-download /> 从公共物模型初始化</a-doption>
            <a-doption v-if="!draft" @click="importModalRef?.open('fields')"><icon-list /> 从标准字段批量添加</a-doption>
          </template>
        </a-dropdown>
      </a-space>
    </div>

    <a-table v-if="activeSection === 'properties'" row-key="id" :data="metadata.properties" :columns="propertyColumns" :loading="loading" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 980 }" />
    <a-table v-else-if="activeSection === 'events'" row-key="id" :data="metadata.events" :columns="eventColumns" :loading="loading" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 850 }" />
    <a-table v-else row-key="id" :data="metadata.functions" :columns="functionColumns" :loading="loading" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 850 }" />

    <a-drawer v-model:visible="detailVisible" :title="`${typeLabel}详情`" width="520px" unmount-on-close>
      <a-descriptions v-if="selectedField" :column="1" bordered>
        <a-descriptions-item label="名称">{{ selectedField.name }}</a-descriptions-item>
        <a-descriptions-item label="标识">{{ selectedField.id }}</a-descriptions-item>
        <a-descriptions-item v-if="activeSection === 'properties'" label="读写权限">{{ selectedField.mode === 'rw' ? '读写' : '只读' }}</a-descriptions-item>
        <a-descriptions-item v-if="activeSection === 'properties'" label="来源">{{ selectedField.source === 'extended' ? '扩展' : '设备' }}</a-descriptions-item>
        <a-descriptions-item v-if="activeSection === 'events'" label="告警等级">{{ levelLabel(selectedField) }}</a-descriptions-item>
        <a-descriptions-item v-if="activeSection !== 'functions'" label="数据类型">{{ getValueType(selectedField) }}</a-descriptions-item>
        <a-descriptions-item label="描述">{{ selectedField.description || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div v-if="selectedField && activeSection === 'functions'" class="parameter-list">
        <h4>输入参数</h4>
        <a-table :data="selectedInputs" :columns="parameterColumns" :pagination="false" :bordered="{ cell: true }" />
      </div>
    </a-drawer>

    <MetadataFieldModal ref="fieldModalRef" :product-key="productKey" :save-field="draft ? saveDraftField : undefined" @save-success="loadData" />
    <MetadataImportModal v-if="!draft" ref="importModalRef" :product-key="productKey" @save-success="loadData" />
    <MetadataViewModal ref="viewModalRef" :product-id="productId" :product-key="productKey" :draft="draft" :model-value="modelValue" />
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { FieldType, MetadataDocument } from './metadataSchema'
import type { ProductMetadataItem } from '@/apis/iot/product'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye } from '@arco-design/web-vue/es/icon'
import { deleteProductMetadata, getProductEvents, getProductFunctions, getProductProperties } from '@/apis/iot/product'
import { hasPerm } from '@/utils/permission'
import MetadataFieldModal from './MetadataFieldModal.vue'
import MetadataImportModal from './MetadataImportModal.vue'
import MetadataViewModal from './MetadataViewModal.vue'

defineOptions({ name: 'ProductMetadataPanel' })

const props = withDefaults(defineProps<{ productId?: string, productKey?: string, readonly?: boolean, draft?: boolean, modelValue?: MetadataDocument }>(), { productId: '', productKey: '', readonly: false, draft: false })
const emit = defineEmits<{ (e: 'update:modelValue', value: MetadataDocument): void }>()
const canAdd = computed(() => !props.readonly && (props.draft || hasPerm('system:metadata:add')))
const canImport = computed(() => !props.readonly && !props.draft && hasPerm('system:metadata:edit'))
const viewModalRef = useTemplateRef<InstanceType<typeof MetadataViewModal>>('viewModalRef')
const fieldModalRef = useTemplateRef<InstanceType<typeof MetadataFieldModal>>('fieldModalRef')
const importModalRef = useTemplateRef<InstanceType<typeof MetadataImportModal>>('importModalRef')
const loading = ref(false)
const detailVisible = ref(false)
const activeSection = ref<FieldType>('properties')
const selectedField = ref<ProductMetadataItem>()
const metadata = reactive<Record<FieldType, ProductMetadataItem[]>>({ properties: [], events: [], functions: [] })
const typeLabel = computed(() => ({ properties: '属性', events: '事件', functions: '功能' })[activeSection.value])
const selectedInputs = computed(() => Array.isArray(selectedField.value?.inputs) ? selectedField.value.inputs : [])

const baseColumns: TableColumnData[] = [
  { title: '标识', dataIndex: 'id', width: 190, ellipsis: true, tooltip: true },
  { title: '名称', dataIndex: 'name', width: 180, ellipsis: true, tooltip: true }
]
const descriptionColumn: TableColumnData = { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true }
const propertyColumns: TableColumnData[] = [
  ...baseColumns,
  { title: '读写权限', width: 100, render: ({ record }) => <Tag color={record.mode === 'rw' ? 'green' : 'arcoblue'}>{record.mode === 'rw' ? '读写' : '只读'}</Tag> },
  { title: '来源', width: 100, render: ({ record }) => record.source === 'extended' ? '扩展' : '设备' },
  { title: '数据类型', width: 180, render: ({ record }) => getValueType(record as ProductMetadataItem) },
  descriptionColumn,
  operationColumn('properties')
]
const eventColumns: TableColumnData[] = [
  ...baseColumns,
  { title: '告警等级', width: 100, render: ({ record }) => <Tag color={levelColor(record as ProductMetadataItem)}>{levelLabel(record as ProductMetadataItem)}</Tag> },
  { title: '数据类型', width: 180, render: ({ record }) => getValueType(record as ProductMetadataItem) },
  descriptionColumn,
  operationColumn('events')
]
const functionColumns: TableColumnData[] = [
  ...baseColumns,
  { title: '输入参数', width: 100, align: 'center', render: ({ record }) => Array.isArray(record.inputs) ? record.inputs.length : 0 },
  { title: '功能类型', width: 110, render: ({ record }) => record.config ? <Tag color="purple">配置型</Tag> : '普通' },
  descriptionColumn,
  operationColumn('functions')
]
const parameterColumns: TableColumnData[] = [
  { title: '标识', dataIndex: 'id', width: 160 },
  { title: '名称', dataIndex: 'name' },
  { title: '类型', width: 100, render: ({ record }) => getValueType(record as ProductMetadataItem) }
]

function operationColumn(type: FieldType): TableColumnData {
  return {
    title: '操作',
    width: 132,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space size={2}>
        <Tooltip content="查看"><Button type="text" shape="circle" aria-label="查看" onClick={() => openDetail(type, record as ProductMetadataItem)}><IconEye /></Button></Tooltip>
        {!props.readonly && (props.draft || hasPerm('system:metadata:edit')) && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => fieldModalRef.value?.open(type, record as ProductMetadataItem)}><IconEdit /></Button></Tooltip>}
        {!props.readonly && (props.draft || hasPerm('system:metadata:delete')) && <Popconfirm content={`确定删除${typeLabelFor(type)}“${record.name}”吗？`} onBeforeOk={() => removeField(type, record as ProductMetadataItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
      </Space>
    )
  }
}

function typeLabelFor(type: FieldType) {
  return ({ properties: '属性', events: '事件', functions: '功能' })[type]
}

function getValueType(record: ProductMetadataItem) {
  const valueType = record.valueType && typeof record.valueType === 'object' ? record.valueType as Record<string, unknown> : {}
  const type = String(record.type || valueType.type || '-')
  const highlights = [valueType.unit, valueType.min !== undefined || valueType.max !== undefined ? `${valueType.min ?? '-'} ~ ${valueType.max ?? '-'}` : ''].filter(Boolean)
  return highlights.length ? `${type} (${highlights.join(' / ')})` : type
}

function levelLabel(record: ProductMetadataItem) {
  return ({ ordinary: '普通', warn: '提醒', urgent: '紧急' } as Record<string, string>)[record.expands?.level || 'ordinary'] || '普通'
}

function levelColor(record: ProductMetadataItem) {
  return ({ ordinary: 'arcoblue', warn: 'orange', urgent: 'red' } as Record<string, string>)[record.expands?.level || 'ordinary'] || 'arcoblue'
}

function openDetail(type: FieldType, record: ProductMetadataItem) {
  activeSection.value = type
  selectedField.value = record
  detailVisible.value = true
}

async function removeField(type: FieldType, record: ProductMetadataItem) {
  if (props.draft) {
    updateDraft(type, metadata[type].filter((item) => item.id !== record.id))
    return true
  }
  await deleteProductMetadata({ productKey: props.productKey, type, oldId: record.id })
  Message.success(`${typeLabelFor(type)}删除成功`)
  await loadData()
  return true
}

async function loadData() {
  if (props.draft) return
  loading.value = true
  try {
    const [properties, events, functions] = await Promise.all([getProductProperties(props.productId), getProductEvents(props.productId), getProductFunctions(props.productId)])
    metadata.properties = properties.data || []
    metadata.events = events.data || []
    metadata.functions = functions.data || []
  } finally {
    loading.value = false
  }
}

function updateDraft(type: FieldType, items: ProductMetadataItem[]) {
  emit('update:modelValue', { ...props.modelValue, ...metadata, [type]: items })
}

function saveDraftField(type: FieldType, item: ProductMetadataItem, oldId: string) {
  if (metadata[type].some((field) => field.id === item.id && field.id !== oldId)) {
    Message.error(`已存在标识 ${item.id}`)
    return false
  }
  const items = [...metadata[type]]
  const index = oldId ? items.findIndex((field) => field.id === oldId) : -1
  if (index >= 0) items.splice(index, 1, item)
  else items.push(item)
  updateDraft(type, items)
}

watch(() => props.modelValue, (value) => {
  if (props.draft) Object.assign(metadata, { properties: value?.properties || [], events: value?.events || [], functions: value?.functions || [] })
}, { immediate: true })
watch(() => props.productId, loadData, { immediate: true })
</script>

<style lang="scss" scoped>
.metadata-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__toolbar {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__sections {
    flex: 1;
    min-width: 0;
  }

  &__sections :deep(.arco-tabs-content) {
    display: none;
  }

  &__sections :deep(.arco-tabs-nav::before) {
    display: none;
  }
}

.parameter-list {
  margin-top: 16px;

  h4 {
    margin: 0 0 10px;
  }
}

@media (width <= 720px) {
  .metadata-panel__toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
