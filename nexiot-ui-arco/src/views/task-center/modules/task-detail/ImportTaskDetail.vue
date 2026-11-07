<template>
  <a-table row-key="id" :data="tableRows" :columns="columns" :loading="loading" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 850 }" />
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskDetailItem } from '../../api'
import { Tag } from '@arco-design/web-vue'
import { importItemData, itemStatusColor, itemStatusLabel } from '../../shared'

const props = defineProps<{ items: TaskDetailItem[], loading: boolean }>()
const tableRows = computed(() => props.items.map((item) => ({ ...item, ...importItemData(item) })))
const columns: TableColumnData[] = [
  { title: '行号', dataIndex: 'rowNo', width: 90 },
  { title: 'Device ID', dataIndex: 'deviceId', width: 190 },
  { title: '设备名称', dataIndex: 'deviceName', width: 190 },
  { title: '状态', width: 100, render: ({ record }) => <Tag color={itemStatusColor(record.status)}>{itemStatusLabel(record.status)}</Tag> },
  { title: '处理结果', dataIndex: 'message', minWidth: 280, ellipsis: true, tooltip: true }
]
</script>
