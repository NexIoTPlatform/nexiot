<template>
  <GiPageLayout margin>
    <RulePageToolbar>
      <template #actions>
        <a-button v-if="hasPerm('rule:flow:add')" type="primary" @click="formRef?.open()"><template #icon><icon-plus /></template>新增转发规则</a-button>
      </template>
      <a-input v-model.trim="query.ruleName" placeholder="规则名称" allow-clear @press-enter="search" />
      <a-select v-model="query.status" :options="statusOptions" placeholder="运行状态" allow-clear />
      <GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" />
    </RulePageToolbar>
    <GiTable title="数据转发" row-key="id" :data="tableData" :columns="columns" :loading="loading" :pagination="pagination" :scroll="{ x: '100%', y: '100%', minWidth: 1160 }" @refresh="refresh" />
    <RuleFormDrawer ref="formRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { ForwardRule } from './api'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconPause, IconPlayArrow } from '@arco-design/web-vue/es/icon'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import RuleNameCell from '../components/RuleNameCell.vue'
import RulePageToolbar from '../components/RulePageToolbar.vue'
import { deleteForwardRule, getForwardRules, updateForwardRuleStatus } from './api'
import RuleFormDrawer from './modules/RuleFormDrawer.vue'

defineOptions({ name: 'RuleFlow' })
const formRef = useTemplateRef<InstanceType<typeof RuleFormDrawer>>('formRef')
const query = reactive<{ ruleName: string, status?: 'run' | 'stop' }>({ ruleName: '' })
const statusOptions = [{ label: '运行中', value: 'run' }, { label: '已停止', value: 'stop' }]
const dataLevelLabels = { product: '产品', group: '设备群组', device: '指定设备' }
const { loading, tableData, pagination, search, refresh } = useTable<ForwardRule>({ listAPI: (page) => getForwardRules({ ...page, ruleName: query.ruleName || undefined, status: query.status }), immediate: true })
function reset() { Object.assign(query, { ruleName: '', status: undefined }); search() }
async function changeStatus(row: ForwardRule) {
  const next = row.status === 'run' ? 'stop' : 'run'
  await updateForwardRuleStatus({ ...row, status: next })
  Message.success(next === 'run' ? '规则已启动' : '规则已停止')
  refresh()
}
async function remove(row: ForwardRule) { if (!row.id) return false; await deleteForwardRule(row.id); Message.success('规则删除成功'); search(); return true }
const columns: TableColumnData[] = [
  { title: '规则名称', width: 220, fixed: 'left', render: ({ record }) => (
    <RuleNameCell
      title={record.ruleName}
      fallback="未命名转发规则"
      ariaLabel={`编辑转发规则 ${record.ruleName || ''}`}
      onClick={() => formRef.value?.open(record as ForwardRule)}
    />
  ) },
  { title: '数据范围', width: 120, render: ({ record }) => <Tag>{dataLevelLabels[record.dataLevel as keyof typeof dataLevelLabels] || record.dataLevel}</Tag> },
  { title: '转发目标', width: 100, align: 'center', render: ({ record }) => record.config?.targets?.length || 0 },
  { title: '状态', width: 110, align: 'center', render: ({ record }) => <Tag color={record.status === 'run' ? 'green' : 'gray'}>{record.status === 'run' ? '运行中' : '已停止'}</Tag> },
  { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true, width: 280, render: ({ record }) => record.description || '-' },
  { title: '创建时间', dataIndex: 'createTime', width: 190 },
  { title: '操作', width: 145, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('rule:flow:status') && <Popconfirm content={`确定${record.status === 'run' ? '停止' : '启动'}该规则吗？`} onBeforeOk={() => changeStatus(record as ForwardRule)}><Tooltip content={record.status === 'run' ? '停止' : '启动'}><Button type="text" shape="circle" aria-label="切换规则状态">{record.status === 'run' ? <IconPause /> : <IconPlayArrow />}</Button></Tooltip></Popconfirm>}
      {hasPerm('rule:flow:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑转发规则" onClick={() => formRef.value?.open(record as ForwardRule)}><IconEdit /></Button></Tooltip>}
      {hasPerm('rule:flow:del') && <Popconfirm content={`确定删除“${record.ruleName}”吗？`} onBeforeOk={() => remove(record as ForwardRule)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除转发规则"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]
</script>
