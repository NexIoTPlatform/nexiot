<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['standard:field:add']" type="primary" @click="formModalRef?.add(activeType || 'property')">
          <template #icon><icon-plus /></template>
          新增字段
        </a-button>
        <a-popconfirm content="确定删除选中的标准字段吗？" @before-ok="batchDelete">
          <a-button v-hasPerm="['standard:field:remove']" status="danger" :disabled="!selectedKeys.length">
            <template #icon><IconDelete /></template>
            批量删除
          </a-button>
        </a-popconfirm>
      </a-space>
      <a-space wrap>
        <a-input v-model="query.fieldId" placeholder="字段标识" allow-clear @press-enter="search" />
        <a-input v-model="query.fieldName" placeholder="字段名称" allow-clear @press-enter="search" />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <a-radio-group v-model="activeType" type="button" class="type-filter" @change="search">
      <a-radio value="">全部</a-radio>
      <a-radio value="property">属性</a-radio>
      <a-radio value="event">事件</a-radio>
      <a-radio value="function">功能</a-radio>
    </a-radio-group>

    <a-table
      class="g-table"
      row-key="id"
      :data="fieldList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :expandable="{ title: '结构', width: 64 }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1200 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
    >
      <template #expand-icon="{ expanded, record }">
        <span role="img" :aria-label="`${expanded ? '收起' : '展开'}${record.fieldName}类型结构`">{{ expanded ? '−' : '+' }}</span>
      </template>
      <template #expand-row="{ record }">
        <a-table row-key="key" :data="schemaRows(record)" :columns="schemaColumns" :pagination="false" :bordered="{ cell: true }" default-expand-all-rows>
          <template #empty><a-empty :description="record.fieldType === 'function' ? '暂无输入参数' : '暂无类型定义'" /></template>
        </a-table>
      </template>
    </a-table>
    <StandardFieldFormModal ref="formModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { StandardFieldItem, StandardFieldType } from '@/apis/iot/standardField'
import type { ValueTypeForm } from '@/views/iot/product/details/metadataSchema'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit } from '@arco-design/web-vue/es/icon'
import { deleteStandardFields, getStandardFieldList } from '@/apis/iot/standardField'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import { normalizeValueType } from '@/views/iot/product/details/metadataSchema'
import { standardFieldToMetadata } from '@/views/iot/product/details/standardFieldMetadata'
import StandardFieldFormModal from './StandardFieldFormModal.vue'

defineOptions({ name: 'IotStandardField' })

const formModalRef = useTemplateRef<InstanceType<typeof StandardFieldFormModal>>('formModalRef')
const activeType = ref<StandardFieldType | ''>('')
const query = reactive({ fieldId: '', fieldName: '' })
const typeMap: Record<StandardFieldType, { label: string, color: string }> = {
  property: { label: '属性', color: 'blue' },
  event: { label: '事件', color: 'orange' },
  function: { label: '功能', color: 'purple' }
}
const modeLabels: Record<string, string> = { r: '只读', w: '只写', rw: '读写' }
const levelLabels: Record<string, string> = { ordinary: '普通', warn: '提醒', urgent: '紧急' }
interface SchemaRow {
  key: string
  id: string
  name: string
  type: string
  definition: string
  children?: SchemaRow[]
}
const schemaColumns: TableColumnData[] = [
  { title: '标识', dataIndex: 'id', width: 200 },
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: '数据类型', dataIndex: 'type', width: 120 },
  { title: '约束 / 枚举定义', dataIndex: 'definition' }
]

const { loading, tableData: fieldList, pagination, selectedKeys, search, select, selectAll, onDelete } = useTable<StandardFieldItem>({
  listAPI: (page) => getStandardFieldList({
    ...page,
    fieldId: query.fieldId.trim() || undefined,
    fieldName: query.fieldName.trim() || undefined,
    fieldType: activeType.value || undefined
  }),
  deleteAPI: deleteStandardFields,
  immediate: true
})

function reset() {
  Object.assign(query, { fieldId: '', fieldName: '' })
  activeType.value = ''
  search()
}

async function batchDelete() {
  await deleteStandardFields(selectedKeys.value.map(String))
  Message.success('删除成功')
  search()
  return true
}

function structureSummary(record: StandardFieldItem) {
  try {
    const metadata = standardFieldToMetadata(record)
    if (record.fieldType === 'function') return `输入 ${metadata.inputs?.length || 0} 项 / ${metadata.config ? '配置型' : '普通功能'}`
    const type = normalizeValueType(metadata.valueType)
    const summary = [type.type, type.type === 'enum' ? `${type.elements.length} 个枚举项` : '', type.type === 'object' ? `${type.properties.length} 个字段` : '', type.unit]
    if (record.fieldType === 'property') summary.push(modeLabels[metadata.mode || 'rw'])
    else summary.push(levelLabels[metadata.expands?.level || 'ordinary'])
    return summary.filter(Boolean).join(' / ')
  } catch {
    return '配置格式无效'
  }
}

function typeDefinition(type: ValueTypeForm) {
  const details = [type.unit ? `单位 ${type.unit}` : '']
  if (['int', 'long', 'float', 'double'].includes(type.type)) {
    if (type.min !== undefined || type.max !== undefined) details.push(`范围 ${type.min ?? '-'} ~ ${type.max ?? '-'}`)
    if (type.step !== undefined) details.push(`步长 ${type.step}`)
    if (type.precision !== undefined) details.push(`精度 ${type.precision}`)
  }
  if (type.type === 'enum') details.push(...type.elements.map((item) => `${item.value} = ${item.text}${item.description ? `（${item.description}）` : ''}`))
  if (type.type === 'boolean') details.push(`${type.trueValue ?? 'true'} = ${type.trueText || '是'}`, `${type.falseValue ?? 'false'} = ${type.falseText || '否'}`)
  if (type.expands.maxLength !== undefined) details.push(`最大长度 ${type.expands.maxLength}`)
  if (type.pattern) details.push(`正则 ${type.pattern}`)
  if (type.type === 'array' && (type.expands.minItems !== undefined || type.expands.maxItems !== undefined)) details.push(`项数 ${type.expands.minItems ?? 0} ~ ${type.expands.maxItems ?? '不限'}`)
  if (type.type === 'file') details.push(type.bodyType || 'url', type.expands.accept || '不限格式', type.expands.maxSize ? `最大 ${type.expands.maxSize} MB` : '')
  if (type.type === 'date') details.push(type.format || 'timestamp')
  if (type.type === 'geoPoint') details.push(`经度 ${type.lonProperty || 'lon'}`, `纬度 ${type.latProperty || 'lat'}`)
  if (type.type === 'geoShape') details.push(type.expands.defaultShapeType || 'Point')
  return details.filter(Boolean).join('；') || '-'
}

function schemaRow(item: Pick<ProductMetadataItem, 'id' | 'name' | 'valueType' | 'type'>, key: string): SchemaRow {
  const type = normalizeValueType(item.valueType || item.type || 'string')
  const row: SchemaRow = { key, id: item.id, name: item.name, type: type.type, definition: typeDefinition(type) }
  if (type.type === 'object') row.children = type.properties.map((property, index) => schemaRow(property, `${key}.${index}`))
  if (type.type === 'array') row.children = [schemaRow({ id: '[]', name: '数组元素', valueType: type.elementType }, `${key}.element`)]
  return row
}

function schemaRows(record: StandardFieldItem): SchemaRow[] {
  try {
    const metadata = standardFieldToMetadata(record)
    return (record.fieldType === 'function' ? metadata.inputs || [] : [metadata]).map((item, index) => schemaRow(item, String(index)))
  } catch {
    return [{ key: 'error', id: record.fieldId, name: record.fieldName, type: record.dataType || '-', definition: '配置格式无效，请检查原字段配置' }]
  }
}

const columns: TableColumnData[] = [
  { title: '字段标识', dataIndex: 'fieldId', width: 180, ellipsis: true, tooltip: true },
  { title: '字段名称', dataIndex: 'fieldName', width: 180, ellipsis: true, tooltip: true },
  { title: '字段类型', width: 100, align: 'center', render: ({ record }) => <Tag color={typeMap[record.fieldType as StandardFieldType]?.color}>{typeMap[record.fieldType as StandardFieldType]?.label || record.fieldType}</Tag> },
  { title: '结构摘要', width: 200, render: ({ record }) => structureSummary(record as StandardFieldItem) },
  { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true },
  { title: '状态', width: 90, align: 'center', render: ({ record }) => <Tag color={record.status === 1 ? 'green' : 'gray'}>{record.status === 1 ? '启用' : '禁用'}</Tag> },
  { title: '使用次数', dataIndex: 'useCount', width: 100, align: 'center' },
  { title: '更新时间', dataIndex: 'updateTime', width: 180 },
  {
    title: '操作',
    width: 110,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        {hasPerm('standard:field:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => formModalRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
        {hasPerm('standard:field:remove') && <Popconfirm content="确定删除该字段吗？" onBeforeOk={() => onDelete(record as StandardFieldItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
      </Space>
    )
  }
]
</script>

<style lang="scss" scoped>
.type-filter {
  margin-bottom: 12px;
}
</style>
