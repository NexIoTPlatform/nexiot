<template>
  <GiPageLayout margin>
    <RulePageToolbar>
      <template #actions>
        <a-button v-if="hasPerm('databridge:resource:add')" type="primary" @click="formRef?.open()">
          <template #icon><icon-plus /></template>新增连接
        </a-button>
      </template>
      <a-input v-model.trim="query.name" placeholder="连接名称" allow-clear @press-enter="search" />
      <a-select v-model="query.type" :options="typeOptions" placeholder="资源类型" allow-search allow-clear />
      <a-select v-model="query.direction" :options="directionOptions" placeholder="数据方向" allow-clear />
      <a-select v-model="query.status" :options="statusOptions" placeholder="状态" allow-clear />
      <GiButton type="search" @click="search" />
      <GiButton type="reset" @click="reset" />
    </RulePageToolbar>

    <GiTable
      title="连接管理"
      row-key="id"
      :data="pageRows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1780 }"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
      @refresh="load"
    />

    <ResourceFormDrawer ref="formRef" @save-success="load" />
    <ResourceDetailDrawer ref="detailRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { ResourceConnection } from '../api'
import { Button, Message, Popconfirm, Space, Switch, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye, IconThunderbolt } from '@arco-design/web-vue/es/icon'
import { hasPerm } from '@/utils/permission'
import RuleNameCell from '../../components/RuleNameCell.vue'
import RulePageToolbar from '../../components/RulePageToolbar.vue'
import { deleteResourceConnection, getResourceConnections, testSavedResourceConnection, updateResourceConnectionStatus } from '../api'
import { formatDateTime, getDirectionColor, getDirectionLabel, getResourceAddress, getResourceTypeColor } from '../shared'
import ResourceDetailDrawer from './modules/ResourceDetailDrawer.vue'
import ResourceFormDrawer from './modules/ResourceFormDrawer.vue'

defineOptions({ name: 'RuleDataBridgeConnection' })
const loading = ref(false)
const rows = ref<ResourceConnection[]>([])
const query = reactive<{ name: string, type?: string, direction?: string, status?: number }>({ name: '' })
const page = reactive({ current: 1, pageSize: 10 })
const formRef = useTemplateRef<InstanceType<typeof ResourceFormDrawer>>('formRef')
const detailRef = useTemplateRef<InstanceType<typeof ResourceDetailDrawer>>('detailRef')
const directionOptions = [{ label: '输入', value: 'INPUT' }, { label: '输出', value: 'OUTPUT' }, { label: '双向', value: 'BIDIRECTIONAL' }]
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const typeOptions = computed(() => Array.from(new Set(rows.value.map((item) => item.type).filter(Boolean)), (value) => ({ label: value, value })))
const filteredRows = computed(() => rows.value.filter((item) => {
  const keyword = query.name.toLowerCase()
  return (!keyword || item.name.toLowerCase().includes(keyword))
    && (!query.type || item.type === query.type)
    && (!query.direction || item.dataDirection === query.direction)
    && (query.status === undefined || item.status === query.status)
}))
const pageRows = computed(() => filteredRows.value.slice((page.current - 1) * page.pageSize, page.current * page.pageSize))
const pagination = computed(() => ({ current: page.current, pageSize: page.pageSize, total: filteredRows.value.length, showTotal: true, showPageSize: true }))

function search() { page.current = 1 }
function reset() { Object.assign(query, { name: '', type: undefined, direction: undefined, status: undefined }); search() }
function handlePageChange(value: number) { page.current = value }
function handlePageSizeChange(value: number) { page.pageSize = value; page.current = 1 }
async function load() {
  loading.value = true
  try { rows.value = (await getResourceConnections()).data || [] } finally { loading.value = false }
}
async function test(row: ResourceConnection) {
  if (!row.id) return
  const res = await testSavedResourceConnection(row.id)
  Message.success(res.message || '连接测试成功')
}
async function changeStatus(row: ResourceConnection, enabled: boolean) {
  if (!row.id) return
  await updateResourceConnectionStatus(row.id, enabled ? 1 : 0)
  row.status = enabled ? 1 : 0
  Message.success(enabled ? '连接已启用' : '连接已停用')
}
async function remove(row: ResourceConnection) {
  if (!row.id) return false
  await deleteResourceConnection(row.id)
  Message.success('连接删除成功')
  await load()
  return true
}

const columns: TableColumnData[] = [
  { title: '连接名称', width: 220, fixed: 'left', render: ({ record }) => (
    <RuleNameCell
      title={record.name}
      fallback="未命名连接"
      ariaLabel={`查看连接 ${record.name || ''}`}
      onClick={() => detailRef.value?.open(record as ResourceConnection)}
    />
  ) },
  { title: '资源类型', width: 130, render: ({ record }) => <Tag color={getResourceTypeColor(record.type)}>{record.type || '-'}</Tag> },
  { title: '插件类型', dataIndex: 'pluginType', width: 150, ellipsis: true, tooltip: true, render: ({ record }) => record.pluginType || '-' },
  { title: '连接地址', width: 220, ellipsis: true, tooltip: true, render: ({ record }) => getResourceAddress(record as ResourceConnection) },
  { title: '数据库', dataIndex: 'databaseName', width: 140, ellipsis: true, tooltip: true, render: ({ record }) => record.databaseName || '-' },
  { title: '账号', dataIndex: 'username', width: 140, ellipsis: true, tooltip: true, render: ({ record }) => record.username || '-' },
  { title: '数据方向', width: 110, align: 'center', render: ({ record }) => <Tag color={getDirectionColor(record.dataDirection)}>{getDirectionLabel(record.dataDirection)}</Tag> },
  { title: '状态', width: 110, align: 'center', render: ({ record }) => hasPerm('databridge:resource:edit')
    ? <Switch modelValue={record.status === 1} onChange={(value) => changeStatus(record as ResourceConnection, Boolean(value))} />
    : <Tag color={record.status === 1 ? 'green' : 'gray'}>{record.status === 1 ? '启用' : '停用'}</Tag> },
  { title: '描述', dataIndex: 'description', width: 240, ellipsis: true, tooltip: true, render: ({ record }) => record.description || '-' },
  { title: '更新时间', dataIndex: 'updateTime', width: 180, render: ({ record }) => formatDateTime(record.updateTime || record.createTime) },
  { title: '操作', width: 170, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="详情"><Button type="text" shape="circle" aria-label="查看连接详情" onClick={() => detailRef.value?.open(record as ResourceConnection)}><IconEye /></Button></Tooltip>
      {hasPerm('databridge:resource:test') && <Tooltip content="测试连接"><Button type="text" shape="circle" aria-label="测试连接" onClick={() => test(record as ResourceConnection)}><IconThunderbolt /></Button></Tooltip>}
      {hasPerm('databridge:resource:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑连接" onClick={() => formRef.value?.open(record as ResourceConnection)}><IconEdit /></Button></Tooltip>}
      {hasPerm('databridge:resource:remove') && <Popconfirm content={`确定删除“${record.name}”吗？`} onBeforeOk={() => remove(record as ResourceConnection)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除连接"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]
load()
</script>
