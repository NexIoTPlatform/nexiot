<template>
  <GiPageLayout margin>
    <RulePageToolbar>
      <a-input v-model.trim="query.cName" placeholder="场景名称" allow-clear @press-enter="searchAll" />
      <a-select v-model="query.cStatus" :options="statusOptions" placeholder="执行状态" allow-clear />
      <a-select v-model="query.triggerType" :options="triggerOptions" placeholder="触发方式" allow-clear />
      <a-input v-model.trim="query.cId" placeholder="业务 ID" allow-clear @press-enter="searchAll" />
      <a-select v-model="query.cType" :options="businessTypeOptions" placeholder="业务类型" allow-clear />
      <a-range-picker v-model="dateRange" class="log-page__date" show-time value-format="YYYY-MM-DD HH:mm:ss" />
      <GiButton type="search" @click="searchAll" /><GiButton type="reset" @click="reset" />
    </RulePageToolbar>
    <GiTable title="联动日志" row-key="id" :data="tableData" :columns="columns" :loading="loading" :pagination="pagination" :scroll="{ x: '100%', y: '100%', minWidth: 1260 }" @refresh="refresh" />
    <LogDetailDrawer ref="detailRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { SceneLog } from './api'
import { Button, Tag, Tooltip } from '@arco-design/web-vue'
import { IconEye } from '@arco-design/web-vue/es/icon'
import { useTable } from '@/hooks'
import RuleNameCell from '../components/RuleNameCell.vue'
import RulePageToolbar from '../components/RulePageToolbar.vue'
import { getSceneLogPage } from './api'
import LogDetailDrawer from './modules/LogDetailDrawer.vue'
import { formatDateTime, getStatusColor, getStatusLabel, getTriggerColor, getTriggerLabel } from './shared'

defineOptions({ name: 'RuleLinkageLogs' })
const detailRef = useTemplateRef<InstanceType<typeof LogDetailDrawer>>('detailRef')
const query = reactive<{ cName: string, cStatus?: number, cId: string, cType?: number, triggerType?: string }>({ cName: '', cId: '' })
const dateRange = ref<string[]>([])
const statusOptions = [{ label: '成功', value: 1 }, { label: '失败', value: 2 }, { label: '部分成功', value: 0 }]
const triggerOptions = [{ label: '设备触发', value: 'device' }, { label: '定时触发', value: 'time' }, { label: '手动触发', value: 'manual' }]
const businessTypeOptions = [{ label: '场景联动', value: 1 }, { label: '数据流转', value: 2 }]
function params(page: { page: number, size: number }) { return { ...page, cName: query.cName || undefined, cStatus: query.cStatus, cId: query.cId || undefined, cType: query.cType, triggerType: query.triggerType, createTimeStart: dateRange.value?.[0], createTimeEnd: dateRange.value?.[1] } }
const { loading, tableData, pagination, search, refresh } = useTable<SceneLog>({ listAPI: (page) => getSceneLogPage(params(page)), immediate: true })
function searchAll() { search() }
function reset() { Object.assign(query, { cName: '', cStatus: undefined, cId: '', cType: undefined, triggerType: undefined }); dateRange.value = []; search() }
const columns: TableColumnData[] = [
  { title: '场景 / 业务', width: 200, fixed: 'left', render: ({ record }) => (
    <RuleNameCell
      title={record.cname || record.sceneName}
      fallback="未命名执行记录"
      ariaLabel={`查看日志 ${record.cname || record.sceneName || ''}`}
      onClick={() => detailRef.value?.open(record as SceneLog)}
    />
  ) },
  { title: '业务类型', width: 100, render: ({ record }) => <Tag color={record.ctype === 1 ? 'arcoblue' : 'green'}>{record.ctype === 1 ? '场景联动' : '数据流转'}</Tag> },
  { title: '业务 ID', dataIndex: 'cid', width: 100 },
  { title: '触发方式', width: 120, render: ({ record }) => <Tag color={getTriggerColor(record.triggerType)}>{getTriggerLabel(record.triggerType)}</Tag> },
  { title: '执行状态', width: 110, render: ({ record }) => <Tag color={getStatusColor(record.cstatus)}>{getStatusLabel(record.cstatus)}</Tag> },
  { title: '执行人', dataIndex: 'createBy', width: 130, ellipsis: true, tooltip: true },
  { title: '执行时间', width: 190, render: ({ record }) => formatDateTime(record.createTime) },
  { title: '操作', width: 80, align: 'center', fixed: 'right', render: ({ record }) => <Tooltip content="详情"><Button type="text" shape="circle" aria-label="查看日志详情" onClick={() => detailRef.value?.open(record as SceneLog)}><IconEye /></Button></Tooltip> }
]
</script>

<style scoped>
.log-page__date {
  width: 290px;
}
</style>
