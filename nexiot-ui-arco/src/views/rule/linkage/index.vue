<template>
  <GiPageLayout margin>
    <RulePageToolbar>
      <template #actions>
        <a-button v-if="hasPerm('rule:linkage:add')" type="primary" @click="formRef?.open()"><template #icon><icon-plus /></template>新增场景</a-button>
      </template>
      <a-input v-model.trim="query.sceneName" placeholder="场景名称" allow-clear @press-enter="search" />
      <a-select v-model="query.triggerCondition" :options="triggerOptions" placeholder="触发方式" allow-clear />
      <a-select v-model="query.status" :options="statusOptions" placeholder="启用状态" allow-clear />
      <GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" />
    </RulePageToolbar>
    <GiTable title="联动管理" row-key="id" :data="tableData" :columns="columns" :loading="loading" :pagination="pagination" :scroll="{ x: '100%', y: '100%', minWidth: 1400 }" @refresh="refresh" />
    <SceneFormDrawer ref="formRef" @save-success="search" />
    <SceneLogDrawer ref="logRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { SceneLinkage } from './api'
import { Button, Message, Popconfirm, Space, Switch, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconHistory, IconPlayArrow } from '@arco-design/web-vue/es/icon'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import RuleNameCell from '../components/RuleNameCell.vue'
import RulePageToolbar from '../components/RulePageToolbar.vue'
import { deleteSceneLinkage, executeSceneLinkage, getSceneLinkages, updateSceneLinkage } from './api'
import SceneFormDrawer from './modules/SceneFormDrawer.vue'
import SceneLogDrawer from './modules/SceneLogDrawer.vue'
import { formatDateTime, getSceneTriggerTypes, getTriggerColor, getTriggerLabel, parseList } from './shared'

defineOptions({ name: 'RuleLinkageManagement' })
const formRef = useTemplateRef<InstanceType<typeof SceneFormDrawer>>('formRef')
const logRef = useTemplateRef<InstanceType<typeof SceneLogDrawer>>('logRef')
const query = reactive<{ sceneName: string, triggerCondition?: string, status?: number }>({ sceneName: '' })
const triggerOptions = [{ label: '设备触发', value: 'device' }, { label: '定时触发', value: 'timer' }, { label: '手动触发', value: 'manual' }]
const statusOptions = [{ label: '启用', value: 0 }, { label: '停用', value: 1 }]
const { loading, tableData, pagination, search, refresh } = useTable<SceneLinkage>({ listAPI: (page) => getSceneLinkages({ ...page, sceneName: query.sceneName || undefined, triggerCondition: query.triggerCondition, status: query.status }), immediate: true })
function reset() { Object.assign(query, { sceneName: '', triggerCondition: undefined, status: undefined }); search() }
async function changeStatus(row: SceneLinkage, enabled: boolean) {
  await updateSceneLinkage({ id: row.id, status: enabled ? 0 : 1 })
  row.status = enabled ? 0 : 1
  Message.success(enabled ? '场景已启用' : '场景已停用')
}
async function execute(row: SceneLinkage) { if (!row.id) return; const res = await executeSceneLinkage(row.id); Message.success(res.message || '场景触发成功') }
async function remove(row: SceneLinkage) { if (!row.id) return false; await deleteSceneLinkage(row.id); Message.success('场景删除成功'); search(); return true }
function triggerCount(row: SceneLinkage) { return parseList(row.triggerCondition).length }
function actionCount(row: SceneLinkage) { return parseList(row.execAction).length }
const columns: TableColumnData[] = [
  { title: '场景名称', width: 300, fixed: 'left', render: ({ record }) => (
    <RuleNameCell
      title={record.sceneName}
      fallback="未命名场景"
      ariaLabel={`编辑联动场景 ${record.sceneName || ''}`}
      onClick={() => formRef.value?.open(record as SceneLinkage)}
    />
  ) },
  { title: '触发方式', width: 100, render: ({ record }) => <Space wrap size={4}>{getSceneTriggerTypes(record.triggerCondition).map((item) => <Tag key={item} color={getTriggerColor(item)}>{getTriggerLabel(item)}</Tag>)}</Space> },
  { title: '触发条件', width: 90, align: 'center', render: ({ record }) => triggerCount(record as SceneLinkage) },
  { title: '执行动作', width: 90, align: 'center', render: ({ record }) => actionCount(record as SceneLinkage) },
  { title: '条件关系', width: 120, align: 'center', render: ({ record }) => record.touch === 'all' ? '满足全部' : '满足任意一个' },
  { title: '静默周期', width: 110, align: 'center', render: ({ record }) => `${record.sleepCycle || 0} 秒` },
  { title: '状态', width: 100, align: 'center', render: ({ record }) => hasPerm('rule:linkage:edit') ? <Switch modelValue={record.status === 0} onChange={(value) => changeStatus(record as SceneLinkage, Boolean(value))} /> : <Tag color={record.status === 0 ? 'green' : 'gray'}>{record.status === 0 ? '启用' : '停用'}</Tag> },
  { title: '创建人', dataIndex: 'createBy', width: 130, ellipsis: true, tooltip: true },
  { title: '创建时间', width: 190, render: ({ record }) => formatDateTime(record.createTime) },
  { title: '操作', width: 185, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('rule:linkage:exec') && <Popconfirm content={`确定手动触发“${record.sceneName}”吗？`} onBeforeOk={() => execute(record as SceneLinkage)}><Tooltip content="手动触发"><Button type="text" shape="circle" aria-label="手动触发场景"><IconPlayArrow /></Button></Tooltip></Popconfirm>}
      <Tooltip content="执行日志"><Button type="text" shape="circle" aria-label="查看场景日志" onClick={() => logRef.value?.open(record as SceneLinkage)}><IconHistory /></Button></Tooltip>
      {hasPerm('rule:linkage:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑场景" onClick={() => formRef.value?.open(record as SceneLinkage)}><IconEdit /></Button></Tooltip>}
      {hasPerm('rule:linkage:remove') && <Popconfirm content={`确定删除“${record.sceneName}”吗？`} onBeforeOk={() => remove(record as SceneLinkage)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除场景"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]
</script>
