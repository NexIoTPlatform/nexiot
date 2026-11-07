<template>
  <GiPageLayout margin>
    <RulePageToolbar>
      <template #actions>
        <a-button v-if="hasPerm('databridge:config:add')" type="primary" @click="formRef?.open()"><template #icon><icon-plus /></template>新增规则</a-button>
      </template>
      <a-input v-model.trim="query.name" placeholder="规则名称" allow-clear @press-enter="search" />
      <a-select v-model="query.bridgeType" :options="typeOptions" placeholder="桥接类型" allow-search allow-clear />
      <a-select v-model="query.sourceScope" :options="scopeOptions" placeholder="数据范围" allow-clear />
      <a-select v-model="query.status" :options="statusOptions" placeholder="状态" allow-clear />
      <GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" />
    </RulePageToolbar>
    <GiTable title="桥接规则" row-key="id" :data="pageRows" :columns="columns" :loading="loading" :pagination="pagination" :scroll="{ x: '100%', y: '100%', minWidth: 1450 }" @page-change="handlePageChange" @page-size-change="handlePageSizeChange" @refresh="load" />
    <BridgeFormDrawer ref="formRef" @save-success="load" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { DataBridgeConfig } from '../api'
import { Button, Message, Popconfirm, Space, Switch, Tag, Tooltip } from '@arco-design/web-vue'
import { IconCheckCircle, IconDelete, IconEdit } from '@arco-design/web-vue/es/icon'
import { hasPerm } from '@/utils/permission'
import RuleNameCell from '../../components/RuleNameCell.vue'
import RulePageToolbar from '../../components/RulePageToolbar.vue'
import { deleteDataBridgeConfig, getDataBridgeConfigs, updateDataBridgeConfigStatus, validateDataBridgeConfig } from '../api'
import { formatDateTime, parseStringArray } from '../shared'
import BridgeFormDrawer from './modules/BridgeFormDrawer.vue'

defineOptions({ name: 'RuleDataBridgeConfig' })
const loading = ref(false)
const rows = ref<DataBridgeConfig[]>([])
const query = reactive<{ name: string, bridgeType?: string, sourceScope?: string, status?: number }>({ name: '' })
const page = reactive({ current: 1, pageSize: 10 })
const formRef = useTemplateRef<InstanceType<typeof BridgeFormDrawer>>('formRef')
const scopeOptions = [{ label: '全部产品', value: 'ALL_PRODUCTS' }, { label: '指定产品', value: 'SPECIFIC_PRODUCTS' }, { label: '指定应用', value: 'APPLICATION' }]
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const typeOptions = computed(() => Array.from(new Set(rows.value.map((item) => item.bridgeType).filter(Boolean)), (value) => ({ label: value, value })))
const filteredRows = computed(() => rows.value.filter((item) => (!query.name || item.name.toLowerCase().includes(query.name.toLowerCase())) && (!query.bridgeType || item.bridgeType === query.bridgeType) && (!query.sourceScope || item.sourceScope === query.sourceScope) && (query.status === undefined || item.status === query.status)))
const pageRows = computed(() => filteredRows.value.slice((page.current - 1) * page.pageSize, page.current * page.pageSize))
const pagination = computed(() => ({ current: page.current, pageSize: page.pageSize, total: filteredRows.value.length, showTotal: true, showPageSize: true }))
function search() { page.current = 1 }
function reset() { Object.assign(query, { name: '', bridgeType: undefined, sourceScope: undefined, status: undefined }); search() }
function handlePageChange(value: number) { page.current = value }
function handlePageSizeChange(value: number) { page.pageSize = value; page.current = 1 }
async function load() { loading.value = true; try { rows.value = (await getDataBridgeConfigs()).data || [] } finally { loading.value = false } }
function sourceLabel(row: DataBridgeConfig) {
  if (row.sourceScope === 'ALL_PRODUCTS') return '全部产品'
  if (row.sourceScope === 'APPLICATION') return row.sourceApplicationName || '指定应用'
  const keys = parseStringArray(row.sourceProductKeys)
  return row.sourceProductNames?.length
    ? row.sourceProductNames.map((item) => typeof item === 'string' ? item : item.name || item.productKey || '-').join('、')
    : `${keys.length} 个产品`
}
async function validate(row: DataBridgeConfig) { if (!row.id) return; const res = await validateDataBridgeConfig(row.id); Message.success(res.message || '配置验证成功') }
async function changeStatus(row: DataBridgeConfig, enabled: boolean) { if (!row.id) return; await updateDataBridgeConfigStatus(row.id, enabled ? 1 : 0); row.status = enabled ? 1 : 0; Message.success(enabled ? '规则已启用' : '规则已停用') }
async function remove(row: DataBridgeConfig) { if (!row.id) return false; await deleteDataBridgeConfig(row.id); Message.success('桥接规则删除成功'); await load(); return true }
const columns: TableColumnData[] = [
  { title: '规则名称', width: 220, fixed: 'left', render: ({ record }) => (
    <RuleNameCell
      title={record.name}
      fallback="未命名规则"
      ariaLabel={`编辑桥接规则 ${record.name || ''}`}
      onClick={() => formRef.value?.open(record as DataBridgeConfig)}
    />
  ) },
  { title: '桥接类型', width: 130, render: ({ record }) => <Tag color="cyan">{record.bridgeType || '-'}</Tag> },
  { title: '目标连接', dataIndex: 'targetResourceName', width: 210, render: ({ record }) => record.targetResourceName || '未配置' },
  { title: '数据范围', width: 260, ellipsis: true, tooltip: true, render: ({ record }) => sourceLabel(record as DataBridgeConfig) },
  { title: '状态', width: 100, align: 'center', render: ({ record }) => hasPerm('databridge:config:edit') ? <Switch modelValue={record.status === 1} onChange={(value) => changeStatus(record as DataBridgeConfig, Boolean(value))} /> : <Tag color={record.status === 1 ? 'green' : 'gray'}>{record.status === 1 ? '启用' : '停用'}</Tag> },
  { title: '描述', dataIndex: 'description', width: 260, ellipsis: true, tooltip: true, render: ({ record }) => record.description || '-' },
  { title: '更新时间', dataIndex: 'updateTime', width: 180, render: ({ record }) => formatDateTime(record.updateTime || record.createTime) },
  { title: '操作', width: 140, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('databridge:config:validate') && <Tooltip content="验证配置"><Button type="text" shape="circle" aria-label="验证配置" onClick={() => validate(record as DataBridgeConfig)}><IconCheckCircle /></Button></Tooltip>}
      {hasPerm('databridge:config:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑桥接规则" onClick={() => formRef.value?.open(record as DataBridgeConfig)}><IconEdit /></Button></Tooltip>}
      {hasPerm('databridge:config:remove') && <Popconfirm content={`确定删除“${record.name}”吗？`} onBeforeOk={() => remove(record as DataBridgeConfig)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除桥接规则"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]
load()
</script>
