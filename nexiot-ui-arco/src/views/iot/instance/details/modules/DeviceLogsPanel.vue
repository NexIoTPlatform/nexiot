<template>
  <section class="device-logs">
    <a-row justify="space-between" align="center" class="device-logs__filters">
      <a-space wrap>
        <a-input v-model="query.id" class="device-logs__message-id" placeholder="消息 ID" allow-clear @press-enter="handleSearch" />
        <a-select v-model="query.messageType" class="device-logs__message-type" :options="messageTypes" allow-clear placeholder="全部消息类型" />
        <a-range-picker
          v-model="dateRange"
          class="device-logs__date-range"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          :placeholder="['开始时间', '结束时间']"
        />
      </a-space>
      <a-space wrap>
        <GiButton type="search" @click="handleSearch" />
        <GiButton type="reset" @click="reset" />
        <a-button :loading="loading" aria-label="刷新日志" @click="handleSearch"><template #icon><icon-refresh /></template>刷新</a-button>
        <a-button :loading="exporting" @click="exportLogs"><template #icon><icon-export /></template>导出</a-button>
      </a-space>
    </a-row>

    <a-alert v-if="loadError" type="error" class="device-logs__error">{{ loadError }}<a-button type="text" @click="loadLogs">重试</a-button></a-alert>
    <a-table
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', minWidth: 1100 }"
    />

    <div class="device-logs__pagination">
      <span>第 {{ pagination.current }} 页<span v-if="!hasMore && !loading && !loadError && rows.length"> · 已到末页</span> · 每次可查询最多 31 天</span>
      <a-space wrap>
        <a-select :model-value="pagination.pageSize" :disabled="loading" aria-label="日志每页记录数" class="device-logs__page-size" @change="handlePageSizeChange">
          <a-option v-for="size in [10, 50, 100, 200]" :key="size" :value="size">{{ size }} 条/页</a-option>
        </a-select>
        <a-button :disabled="loading || pagination.current === 1" @click="handlePageChange(pagination.current - 1)">上一页</a-button>
        <a-button :disabled="loading || !hasMore" @click="handlePageChange(pagination.current + 1)">下一页</a-button>
      </a-space>
    </div>

    <a-drawer v-model:visible="detailVisible" :title="detailTitle" width="min(680px, 94vw)" unmount-on-close>
      <template v-if="selected">
        <div class="device-logs__detail-heading">
          <a-tag :color="messageTypeMeta(selected.messageType).color">{{ messageTypeMeta(selected.messageType).label }}</a-tag>
          <div><strong>{{ messageHeadline(selected, eventNames) }}</strong><span>{{ formatTime(selected.createTime || selected.eventTime) }}</span></div>
        </div>

        <div class="device-logs__detail-body">
          <a-descriptions :column="1" bordered size="medium" class="device-logs__base-desc">
            <a-descriptions-item v-for="item in detailItems" :key="item.key" :label="item.label">
              <code v-if="item.monospace">{{ item.value }}</code><span v-else>{{ item.value }}</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="device-logs__raw-section">
            <div class="device-logs__raw-title">
              <strong>消息数据</strong>
              <a-button type="text" size="small" @click="copyText(formatContent(selected.content))">
                <template #icon><IconCopy /></template>复制
              </a-button>
            </div>
            <pre class="device-logs__raw">{{ formatContent(selected.content) }}</pre>
          </div>
        </div>
      </template>
    </a-drawer>
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem, DeviceLogItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Popover, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconCopy, IconEye } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { copyText as copyToClipboard } from '@/utils'
import { exportDeviceRawLogs, getDeviceLogSlice } from './deviceHistory.api'
import { formatContent, messageHeadline, messageIdentifier, messagePayloadPreview, messageTypeMeta } from './deviceLogDisplay'

defineOptions({ name: 'IotDeviceLogsPanel' })

const props = defineProps<{ device: DeviceItem, product: ProductItem }>()

const router = useRouter()
const selected = ref<DeviceLogItem>()
const detailVisible = ref(false)
interface DetailItem {
  key: string
  label: string
  value: string
  monospace?: boolean
}

const loading = ref(false)
const exporting = ref(false)
const loadError = ref('')
const pageCursors = ref<(string | undefined)[]>([undefined])
const hasMore = ref(false)
const appliedParams = ref<Record<string, unknown>>()
let requestId = 0
let requestedPage = 1
const rows = ref<DeviceLogItem[]>([])
const dateRange = ref<string[]>(recentRange())
const query = reactive({ id: '', messageType: '' })
const pagination = reactive({ current: 1, pageSize: 10 })
const eventNames = computed(() => {
  const names = new Map<string, string>()
  try {
    const model = JSON.parse(props.product.metadata || '{}')
    if (Array.isArray(model.events)) {
      for (const event of model.events) {
        if (event && typeof event.id === 'string' && typeof event.name === 'string') names.set(event.id, event.name)
      }
    }
  } catch { /* The stored event name and identifier still work when metadata is unavailable. */
  }
  return names
})
const messageTypes = [
  { label: '事件上报', value: 'EVENT' },
  { label: '属性上报', value: 'PROPERTIES' },
  { label: '功能调用', value: 'FUNCTIONS' },
  { label: '功能回复', value: 'REPLY' },
  { label: '平台应答', value: 'ACK' }
]
const columns: TableColumnData[] = [
  { title: '消息 ID', width: 78, align: 'center', render: ({ record }) => (
    <div class="device-logs__id-cell">
      {record.id
        ? <Tooltip content="复制完整消息 ID"><Button type="text" size="mini" shape="circle" aria-label="复制消息 ID" onClick={() => copyText(String(record.id))}><IconCopy /></Button></Tooltip>
        : <span>-</span>}
    </div>
  ) },
  { title: '消息类型', width: 120, align: 'center', render: ({ record }) => {
    const meta = messageTypeMeta(record.messageType)
    return (
      <Tag color={meta.color}>
        <span class="device-logs__type-dot" />
        {meta.label}
      </Tag>
    )
  } },
  { title: '消息数据', minWidth: 520, align: 'center', render: ({ record }) => (
    <Popover position="top" trigger="hover">
      {{
        default: () => (
          <div class="device-logs__message-data">
            {record.messageType === 'EVENT' && (
              <div class="device-logs__event-heading">
                <strong>{messageHeadline(record, eventNames.value)}</strong>
                <span>{messageIdentifier(record)}</span>
              </div>
            )}
            <code title={messagePayloadPreview(record.content, record.messageType)}>{messagePayloadPreview(record.content, record.messageType)}</code>
          </div>
        ),
        content: () => (
          <div class="device-logs__json-popover">
            <pre>{formatContent(record.content)}</pre>
          </div>
        )
      }}
    </Popover>
  ) },
  { title: '记录时间', width: 180, render: ({ record }) => (
    <div class="device-logs__time-cell">
      <strong>{formatTime(record.createTime || record.eventTime)}</strong>
      <span>{formatTimeAgo(record.createTime || record.eventTime)}</span>
    </div>
  ) },
  { title: '详细内容', width: 110, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="查看完整日志"><Button type="text" shape="circle" aria-label="查看完整日志" onClick={() => openDetail(record as DeviceLogItem)}><IconEye /></Button></Tooltip>
      <Tooltip content="复制消息数据"><Button type="text" shape="circle" aria-label="复制消息数据" onClick={() => copyText(formatContent(record.content))}><IconCopy /></Button></Tooltip>
    </Space>
  ) }
]
const detailTitle = computed(() => selected.value ? `${messageTypeMeta(selected.value.messageType).label}详情` : '日志详情')
const detailItems = computed<DetailItem[]>(() => {
  if (!selected.value) return []
  const record = selected.value
  return [
    { key: 'id', label: '消息 ID', value: String(record.id || ''), monospace: true },
    { key: 'iotId', label: 'IoT ID', value: String(record.iotId || ''), monospace: true },
    { key: 'deviceId', label: '设备 ID', value: String(record.deviceId || ''), monospace: true },
    { key: 'productKey', label: 'ProductKey', value: String(record.productKey || ''), monospace: true },
    { key: 'deviceName', label: '设备名称', value: String(record.deviceName || props.device.deviceName || '') },
    { key: 'messageType', label: '消息类型', value: `${messageTypeMeta(record.messageType).label} (${record.messageType || '-'})` },
    { key: 'identifier', label: '属性 / 事件 / 功能', value: messageIdentifier(record), monospace: true },
    { key: 'commandId', label: '指令 ID', value: String(record.commandId || ''), monospace: true },
    { key: 'commandStatus', label: '指令状态', value: String(record.commandStatus ?? '') },
    { key: 'point', label: '经纬度', value: String(record.point || '') },
    { key: 'createTime', label: '记录时间', value: formatTime(record.createTime || record.eventTime) }
  ].filter((item) => item.value)
})

function formatTime(value?: string) {
  if (!value) return '-'
  const date = Dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

function formatTimeAgo(value?: string) {
  if (!value) return '未知时间'
  const time = Dayjs(value)
  if (!time.isValid()) return '-'
  const seconds = Math.max(0, Dayjs().diff(time, 'second'))
  if (seconds < 60) return '刚刚'
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`
  return `${Math.floor(seconds / 86400)} 天前`
}

function recentRange() {
  const end = Dayjs()
  return [end.subtract(7, 'day').format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')]
}

function buildTimeParams() {
  if (dateRange.value.length !== 2) dateRange.value = recentRange()
  const beginCreateTime = Dayjs(dateRange.value[0]).unix()
  const endCreateTime = Dayjs(dateRange.value[1]).unix()
  if (!Number.isFinite(beginCreateTime) || !Number.isFinite(endCreateTime) || endCreateTime < beginCreateTime || endCreateTime - beginCreateTime > 31 * 86400) {
    Message.warning('请选择不超过31天的有效时间范围')
    return
  }
  return { beginCreateTime, endCreateTime }
}

async function fetchLogs(page: number) {
  if (!appliedParams.value) return
  const currentRequest = ++requestId
  requestedPage = page
  loading.value = true
  loadError.value = ''
  try {
    const response = await getDeviceLogSlice({ ...appliedParams.value, cursor: pageCursors.value[page - 1], pageSize: pagination.pageSize })
    if (currentRequest !== requestId) return
    const data = response.data
    if (!Array.isArray(data.list) || (data.hasMore && !data.nextCursor)) throw new Error('日志分页信息不完整，请刷新后重试')
    rows.value = data.list
    pagination.current = page
    hasMore.value = data.hasMore
    pageCursors.value = pageCursors.value.slice(0, page)
    if (data.hasMore) pageCursors.value[page] = data.nextCursor
  } catch (error) {
    if (currentRequest === requestId) loadError.value = error instanceof Error ? error.message : '日志加载失败，请缩小时间范围或重试'
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

function loadLogs() {
  return fetchLogs(requestedPage)
}

function resetPages() {
  pagination.current = 1
  requestedPage = 1
  pageCursors.value = [undefined]
  hasMore.value = false
  rows.value = []
}

function handleSearch() {
  const timeParams = buildTimeParams()
  if (!timeParams) return
  appliedParams.value = {
    productKey: props.device.productKey,
    iotId: props.device.iotId,
    deviceId: props.device.deviceId,
    id: query.id.trim() || undefined,
    messageType: query.messageType || undefined,
    ...timeParams
  }
  resetPages()
  return loadLogs()
}

function reset() {
  Object.assign(query, { id: '', messageType: '' })
  dateRange.value = recentRange()
  return handleSearch()
}

function handlePageChange(page: number) {
  if (loading.value || page < 1 || page > pagination.current + 1 || (page > pagination.current && !hasMore.value)) return
  return fetchLogs(page)
}

function handlePageSizeChange(value: unknown) {
  const size = Number(value)
  if (![10, 50, 100, 200].includes(size)) return
  pagination.pageSize = size
  resetPages()
  return loadLogs()
}

function openDetail(row: DeviceLogItem) {
  selected.value = row
  detailVisible.value = true
}
async function copyText(value: string) {
  try {
    await copyToClipboard(value)
    Message.success('已复制到剪贴板')
  } catch {
    Message.error('复制失败')
  }
}
async function exportLogs() {
  if (exporting.value || !appliedParams.value) return
  exporting.value = true
  try {
    await exportDeviceRawLogs(appliedParams.value)
    Message.success('所选时段的原始通信日志已导出')
  } catch {
    // http 层显示后端的导出上限或失败原因。
  } finally {
    exporting.value = false
  }
}

watch(() => [props.device.iotId, props.device.productKey], () => {
  ++requestId
  reset()
}, { immediate: true })
onScopeDispose(() => {
  ++requestId
})
</script>

<style lang="scss" scoped src="./DeviceLogsPanel.scss"></style>

<style lang="scss" scoped>
:deep(.device-logs__id-cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

:deep(.device-logs__type-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

:deep(.device-logs__time-cell) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

:deep(.device-logs__message-data) {
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  min-width: 0;
  max-width: 100%;
  text-align: left;
}

:deep(.device-logs__event-heading) {
  display: flex;
  gap: 10px;
  align-items: baseline;
  min-width: 0;
  max-width: 100%;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    font-size: 13px;
    color: var(--color-text-1);
  }

  span {
    font-family: monospace;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

:deep(.device-logs__message-data code) {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-2);
  white-space: nowrap;
}

:deep(.device-logs__time-cell strong) {
  font-size: 13px;
}

.device-logs__json-popover {
  box-sizing: border-box;
  width: min(560px, calc(100vw - 48px));
  max-width: calc(100vw - 48px);

  pre {
    max-width: 100%;
    max-height: 360px;
    padding: 12px;
    margin: 0;
    overflow: auto;
    font-size: 12px;
    line-height: 1.6;
    white-space: pre-wrap;
    background: var(--color-fill-1);
    border-radius: 4px;
  }
}
</style>
