<template>
  <a-drawer v-model:visible="visible" :title="`${scene?.sceneName || ''} - 执行日志`" width="min(820px, calc(100vw - 24px))" :footer="false" unmount-on-close>
    <a-table row-key="id" :data="rows" :columns="columns" :loading="loading" :pagination="pagination" :scroll="{ x: '100%', minWidth: 720 }" @page-change="handlePageChange" @page-size-change="handlePageSizeChange">
      <template #cstatus="{ record }"><a-tag :color="getStatusColor(record.cstatus)">{{ getStatusLabel(record.cstatus) }}</a-tag></template>
      <template #triggerType="{ record }"><a-tag :color="getTriggerColor(record.triggerType)">{{ getTriggerLabel(record.triggerType) }}</a-tag></template>
      <template #createTime="{ record }">{{ formatDateTime(record.createTime) }}</template>
      <template #operation="{ record }"><a-button type="text" shape="circle" @click="detailRef?.open(record)"><template #icon><icon-eye /></template></a-button></template>
    </a-table>
    <LogDetailDrawer ref="detailRef" />
  </a-drawer>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { SceneLinkage, SceneLog } from '../api'
import { getSceneLogs } from '../api'
import { formatDateTime, getStatusColor, getStatusLabel, getTriggerColor, getTriggerLabel } from '../shared'
import LogDetailDrawer from './LogDetailDrawer.vue'

const visible = ref(false)
const loading = ref(false)
const scene = ref<SceneLinkage>()
const rows = ref<SceneLog[]>([])
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const detailRef = useTemplateRef<InstanceType<typeof LogDetailDrawer>>('detailRef')
const pagination = computed(() => ({ current: page.current, pageSize: page.pageSize, total: page.total, showTotal: true, showPageSize: true }))
const columns: TableColumnData[] = [
  { title: '执行时间', slotName: 'createTime', width: 190 },
  { title: '触发方式', slotName: 'triggerType', width: 120 },
  { title: '状态', slotName: 'cstatus', width: 100 },
  { title: '执行人', dataIndex: 'createBy', width: 130, ellipsis: true, tooltip: true },
  { title: '操作', slotName: 'operation', width: 80, align: 'center' }
]
async function load() { if (!scene.value?.id) return; loading.value = true; try { const res = await getSceneLogs(scene.value.id, { page: page.current, size: page.pageSize }); rows.value = res.data.records || []; page.total = res.data.total } finally { loading.value = false } }
function handlePageChange(value: number) { page.current = value; load() }
function handlePageSizeChange(value: number) { page.pageSize = value; page.current = 1; load() }
function open(value: SceneLinkage) { scene.value = value; page.current = 1; visible.value = true; load() }
defineExpose({ open })
</script>
