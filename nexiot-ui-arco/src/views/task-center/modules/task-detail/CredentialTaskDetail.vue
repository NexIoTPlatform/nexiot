<template>
  <div class="credential-detail">
    <div class="credential-rules">
      <div><span>Client ID</span><code>ProductKey.DeviceId</code></div>
      <div><span>Username</span><code>DeviceId&amp;ProductKey</code></div>
      <div><span>Password</span><code>DeviceSecret</code></div>
    </div>
    <a-table row-key="id" :data="tableRows" :columns="columns" :loading="loading" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 1400 }" />
  </div>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskCenterItem, TaskDetailItem } from '../../api'
import { Button, Message, Tag, Tooltip } from '@arco-design/web-vue'
import { IconCopy } from '@arco-design/web-vue/es/icon'
import { copyText as copyToClipboard } from '@/utils'
import { credentialItemData, itemStatusColor, itemStatusLabel } from '../../shared'

const props = defineProps<{ task: TaskCenterItem, items: TaskDetailItem[], loading: boolean }>()
const tableRows = computed(() => props.items.map((item) => ({ ...item, ...credentialItemData(item, props.task.productKey) })))

function credentialCell(value: unknown, secret = false) {
  const text = String(value || '')
  return (
    <div class={['credential-cell', secret && 'credential-cell--secret']}>
      <span>{text || '-'}</span>
      {text && <Tooltip content="复制"><Button type="text" size="mini" shape="circle" aria-label="复制凭证" onClick={() => copyText(text)}><IconCopy /></Button></Tooltip>}
    </div>
  )
}

async function copyText(text: string) {
  try {
    await copyToClipboard(text)
    Message.success('已复制')
  } catch {
    Message.error('复制失败')
  }
}

const columns: TableColumnData[] = [
  { title: '设备', width: 210, render: ({ record }) => (
    <div class="info-cell">
      <strong>{record.deviceName}</strong>
      <code>{record.deviceId}</code>
    </div>
  ) },
  { title: 'IoT ID', dataIndex: 'iotId', width: 220, ellipsis: true, tooltip: true },
  { title: 'Client ID', width: 230, render: ({ record }) => credentialCell(record.clientId) },
  { title: 'Username', width: 220, render: ({ record }) => credentialCell(record.username) },
  { title: 'Password', width: 260, render: ({ record }) => credentialCell(record.password, true) },
  { title: '状态', width: 100, render: ({ record }) => <Tag color={itemStatusColor(record.status)}>{itemStatusLabel(record.status)}</Tag> },
  { title: '说明', dataIndex: 'message', minWidth: 220, ellipsis: true, tooltip: true }
]
</script>

<style scoped lang="scss">
.credential-rules {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.credential-rules > div {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
}

.credential-rules span,
.info-cell code {
  font-size: 12px;
  color: var(--color-text-3);
}

.credential-rules code {
  color: var(--color-text-1);
  word-break: break-all;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.credential-cell {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
  min-width: 0;
}

.credential-cell span {
  overflow-wrap: anywhere;
}

.credential-cell--secret {
  font-family: monospace;
  font-size: 12px;
}

@media (width <= 640px) {
  .credential-rules {
    grid-template-columns: 1fr;
  }
}
</style>
