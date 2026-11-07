<template>
  <a-modal v-model:visible="visible" :title="title" width="900px" :mask-closable="false" @before-ok="save" @close="reset">
    <a-alert class="metadata-import__tip">{{ tip }}</a-alert>
    <a-radio-group v-if="workflow !== 'json'" v-model="mode" type="button"><a-radio value="merge">合并到现有物模型</a-radio><a-radio value="replace">替换现有物模型</a-radio></a-radio-group>

    <template v-if="workflow === 'standard'">
      <a-space class="metadata-import__search" wrap>
        <a-input v-model="keyword" placeholder="模型名称" allow-clear @press-enter="search" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-table row-key="id" :data="products" :columns="productColumns" :loading="loading" :pagination="pagination" :row-selection="{ type: 'radio' }" :selected-keys="selectedProductId ? [selectedProductId] : []" @select="handleProductSelect" />
    </template>
    <template v-else-if="workflow === 'fields'">
      <a-space class="metadata-import__search" wrap>
        <a-input v-model="keyword" placeholder="字段名称" allow-clear @press-enter="search" />
        <a-input v-model="fieldId" placeholder="字段标识" allow-clear @press-enter="search" />
        <a-select v-model="fieldType" :options="fieldTypeOptions" allow-clear placeholder="字段类型" @change="search" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-space v-if="selectedFieldIds.length" class="metadata-import__selection">
        <span>已选择 {{ selectedFieldIds.length }} 个字段（保留跨页选择）</span>
        <a-button type="text" size="small" @click="selectedFields.clear()">清空选择</a-button>
      </a-space>
      <a-table row-key="id" :data="fields" :columns="fieldColumns" :loading="loading" :pagination="pagination" :row-selection="{ type: 'checkbox', showCheckedAll: true, onlyCurrent: true }" :selected-keys="selectedFieldIds" @select="handleFieldSelect" @select-all="handleFieldSelectAll" />
    </template>
    <template v-else>
      <a-upload :auto-upload="false" :show-file-list="false" accept=".json" @change="handleFile"><template #upload-button><a-button><template #icon><icon-upload /></template>选择 JSON 文件</a-button></template></a-upload>
      <a-textarea v-model="jsonText" class="metadata-import__json" :auto-size="{ minRows: 18, maxRows: 26 }" placeholder="粘贴物模型 JSON，必须包含 properties、events、functions 数组" />
    </template>
    <a-alert v-if="errorMessage" class="metadata-import__error" type="error">{{ errorMessage }}</a-alert>
  </a-modal>
</template>

<script setup lang="ts">
import type { FileItem, TableColumnData } from '@arco-design/web-vue'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { StandardFieldItem, StandardFieldType } from '@/apis/iot/standardField'
import type { StandardProductItem } from '@/apis/iot/standardModel'
import { Message } from '@arco-design/web-vue'
import { updateProductMetadata } from '@/apis/iot/product'
import { getStandardFieldList } from '@/apis/iot/standardField'
import { getStandardProduct, getStandardProductList } from '@/apis/iot/standardModel'
import { usePagination } from '@/hooks'
import { standardFieldToMetadata } from './standardFieldMetadata'

defineOptions({ name: 'MetadataImportModal' })

const props = defineProps<{ productKey: string }>()
const emit = defineEmits<{ (e: 'save-success'): void }>()
type Workflow = 'standard' | 'fields' | 'json'
const visible = ref(false)
const workflow = ref<Workflow>('standard')
const mode = ref<'merge' | 'replace'>('merge')
const loading = ref(false)
const keyword = ref('')
const fieldId = ref('')
const fieldType = ref<StandardFieldType>()
const products = ref<StandardProductItem[]>([])
const fields = ref<StandardFieldItem[]>([])
const selectedProductId = ref('')
const selectedFields = reactive(new Map<string, StandardFieldItem>())
const selectedFieldIds = computed(() => [...selectedFields.keys()])
const jsonText = ref('')
const errorMessage = ref('')
const query = reactive({ name: '', id: '', type: undefined as StandardFieldType | undefined })
const { pagination } = usePagination(loadRows)
let listRequestId = 0
const title = computed(() => ({ standard: '从公共物模型初始化', fields: '从标准字段批量添加', json: '导入物模型' })[workflow.value])
const tip = computed(() => ({ standard: '选择一个公共物模型，将其属性、事件和功能导入当前产品。', fields: '可跨类型选择标准字段并一次性合并到当前产品。', json: '导入会完整替换现有物模型，请先通过“查看物模型”下载 JSON 备份。支持上传或粘贴 JSON，校验或保存失败时保留原物模型。' })[workflow.value])
const fieldTypeOptions = [{ label: '属性', value: 'property' }, { label: '事件', value: 'event' }, { label: '功能', value: 'function' }]
const productColumns: TableColumnData[] = [{ title: '模型名称', dataIndex: 'productName' }, { title: '模型 Key', dataIndex: 'productKey', width: 200 }, { title: '分类', dataIndex: 'categoryName', width: 160 }, { title: '使用次数', dataIndex: 'useCount', width: 100 }]
const fieldColumns: TableColumnData[] = [{ title: '字段名称', dataIndex: 'fieldName' }, { title: '字段标识', dataIndex: 'fieldId', width: 180 }, { title: '类型', dataIndex: 'fieldType', width: 100 }, { title: '数据类型', dataIndex: 'dataType', width: 110 }, { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true }]

function reset() {
  mode.value = 'merge'
  keyword.value = ''
  fieldId.value = ''
  fieldType.value = undefined
  Object.assign(query, { name: '', id: '', type: undefined })
  Object.assign(pagination, { current: 1, pageSize: 10, total: 0 })
  products.value = []
  fields.value = []
  selectedProductId.value = ''
  selectedFields.clear()
  jsonText.value = ''
  errorMessage.value = ''
  listRequestId += 1
  loading.value = false
}
async function open(type: Workflow) {
  reset()
  workflow.value = type
  if (type === 'json') mode.value = 'replace'
  visible.value = true
  if (type !== 'json') await loadRows()
}
async function loadRows() {
  const requestId = ++listRequestId
  loading.value = true
  errorMessage.value = ''
  try {
    const page = { page: pagination.current, size: pagination.pageSize }
    if (workflow.value === 'standard') {
      const res = await getStandardProductList({ ...page, productName: query.name || undefined })
      if (requestId !== listRequestId) return
      products.value = (res.data.records || []).map((item) => ({ ...item, id: String(item.id) }))
      pagination.total = res.data.total
    } else if (workflow.value === 'fields') {
      const res = await getStandardFieldList({ ...page, fieldName: query.name || undefined, fieldId: query.id || undefined, fieldType: query.type })
      if (requestId !== listRequestId) return
      fields.value = (res.data.records || []).map((item) => ({ ...item, id: String(item.id) }))
      pagination.total = res.data.total
    }
  } catch (error) {
    if (requestId === listRequestId) {
      products.value = []
      fields.value = []
      pagination.total = 0
      errorMessage.value = error instanceof Error ? error.message : '列表加载失败，请重试'
    }
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}
function search() {
  Object.assign(query, { name: keyword.value.trim(), id: fieldId.value.trim(), type: fieldType.value })
  pagination.current = 1
  return loadRows()
}
function resetSearch() {
  keyword.value = ''
  fieldId.value = ''
  fieldType.value = undefined
  return search()
}
function handleProductSelect(keys: (string | number)[]) {
  selectedProductId.value = String(keys[0] || '')
}
function handleFieldSelect(keys: (string | number)[]) {
  const selected = new Set(keys.map(String))
  for (const item of fields.value) {
    if (selected.has(item.id)) selectedFields.set(item.id, item)
    else selectedFields.delete(item.id)
  }
}
function handleFieldSelectAll(checked: boolean) {
  for (const item of fields.value) {
    if (checked) selectedFields.set(item.id, item)
    else selectedFields.delete(item.id)
  }
}
async function handleFile(fileList: FileItem[]) {
  const file = fileList[0]?.file
  if (file) jsonText.value = await file.text()
}
function parseMetadata(value?: string) {
  if (!value?.trim()) throw new Error('请输入物模型 JSON 或选择文件')
  let data: Record<string, unknown>
  try {
    data = JSON.parse(value) as Record<string, unknown>
  } catch {
    throw new Error('物模型 JSON 语法错误，请检查引号、逗号和括号')
  }
  if (!data || typeof data !== 'object' || Array.isArray(data)) throw new Error('物模型必须是 JSON 对象')
  const result = { properties: [] as ProductMetadataItem[], events: [] as ProductMetadataItem[], functions: [] as ProductMetadataItem[] }
  for (const type of ['properties', 'events', 'functions'] as const) {
    if (!Array.isArray(data[type])) throw new Error(`${type} 必须是数组，空类型请填写 []`)
    const ids = new Set<string>()
    for (const item of data[type]) {
      if (!item || typeof item !== 'object' || Array.isArray(item) || typeof item.id !== 'string' || !item.id.trim() || typeof item.name !== 'string' || !item.name.trim()) throw new Error(`${type} 中每个字段必须包含非空 id 和 name`)
      if (ids.has(item.id)) throw new Error(`${type} 中标识 ${item.id} 重复`)
      ids.add(item.id)
    }
    result[type] = data[type] as ProductMetadataItem[]
  }
  return result
}
async function save() {
  errorMessage.value = ''
  let metadata = { properties: [] as ProductMetadataItem[], events: [] as ProductMetadataItem[], functions: [] as ProductMetadataItem[] }
  let standardProductId: string | undefined
  try {
    if (workflow.value === 'standard') {
      if (!selectedProductId.value) {
        Message.error('请选择公共物模型')
        return false
      }
      const product = (await getStandardProduct(selectedProductId.value)).data
      metadata = parseMetadata(product.metadata)
      standardProductId = product.id
    } else if (workflow.value === 'fields') {
      const selected = [...selectedFields.values()]
      if (!selected.length) {
        Message.error('请选择标准字段')
        return false
      }
      for (const item of selected) {
        const target = item.fieldType === 'property' ? metadata.properties : item.fieldType === 'event' ? metadata.events : metadata.functions
        target.push(standardFieldToMetadata(item))
      }
    } else {
      metadata = parseMetadata(jsonText.value)
    }
    await updateProductMetadata({ productKey: props.productKey, mode: workflow.value === 'json' ? 'replace' : mode.value, ...metadata, standardProductId, metadata: workflow.value === 'json' ? jsonText.value : undefined })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '物模型导入失败，请检查后重试'
    return false
  }
  Message.success('物模型导入成功')
  emit('save-success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.metadata-import {
  &__tip,
  &__search,
  &__selection {
    margin-bottom: 12px;
  }

  &__search {
    margin-top: 12px;
  }

  &__error,
  &__json {
    margin-top: 12px;
  }

  &__json {
    :deep(textarea) {
      font-family: Consolas, Monaco, monospace;
    }
  }
}
</style>
