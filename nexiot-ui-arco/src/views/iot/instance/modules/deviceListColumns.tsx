import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem } from '@/apis/iot/device'
import { Button, Tag } from '@arco-design/web-vue'
import Dayjs from 'dayjs'

function formatTime(value?: string | number) {
  if (!value || value === '0') return '-'
  const text = String(value)
  const numeric = /^\d{10,13}$/.test(text) ? Number(text) : Number.NaN
  const date = Number.isNaN(numeric) ? Dayjs(text) : Dayjs(text.length === 10 ? numeric * 1000 : numeric)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

function fromNow(value?: string | number) {
  if (!value || value === '0') return '从未通信'
  const text = String(value)
  const timestamp = /^\d{10,13}$/.test(text) ? Number(text) * (text.length === 10 ? 1000 : 1) : Dayjs(text).valueOf()
  const diff = Date.now() - timestamp
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 2_592_000_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return `${Math.floor(diff / 2_592_000_000)} 个月前`
}

function nodeLabel(value?: string) {
  return ({ DEVICE: '直', GATEWAY: '网', GATEWAY_SUB_DEVICE: '子', VIDEO_DEVICE: '视' } as Record<string, string>)[value || ''] || value || '-'
}

function isOnline(record: DeviceItem) {
  return record.state === true || record.state === 1 || record.state === '1'
}

export function createDeviceListColumns(onDeviceClick: (record: DeviceItem) => void): TableColumnData[] {
  return [
    { title: '设备名称 / DeviceId', width: 300, fixed: 'left', render: ({ record }) => (
      <div class="device-cell">
        <div class="device-cell__title">
          <Button type="text" onClick={() => onDeviceClick(record as DeviceItem)}><strong>{record.deviceName || '未命名设备'}</strong></Button>
          {(record.pendingOwner || record.creatorId === 'auto_register') && <Tag size="small" color="orange">待认领</Tag>}
        </div>
        <code>{record.deviceId}</code>
      </div>
    ) },
    { title: '所属产品 / ProductKey', width: 260, render: ({ record }) => (
      <div class="product-cell">
        <strong>{record.productName || '未知产品'}</strong>
        <div>
          <Tag size="small" color="arcoblue">{nodeLabel(record.deviceNode)}</Tag>
          <code>{record.productKey}</code>
        </div>
      </div>
    ) },
    { title: '设备状态', width: 110, align: 'center', render: ({ record }) => (
      <Tag color={isOnline(record as DeviceItem) ? 'green' : 'red'}>
        <span class="device-status__dot" />
        {isOnline(record as DeviceItem) ? '在线' : '离线'}
      </Tag>
    ) },
    { title: '最近通信', width: 205, fixed: 'right', render: ({ record }) => (
      <div class="communication-time">
        <strong>{formatTime(record.onlineTime)}</strong>
        <span>
          <i class={isOnline(record as DeviceItem) ? 'online' : 'offline'} />
          {fromNow(record.onlineTime)}
        </span>
      </div>
    ) },
    { title: '归属账号', dataIndex: 'creatorName', width: 140, ellipsis: true, tooltip: true },
    { title: '注册时间', width: 180, render: ({ record }) => formatTime(record.registryTime) }
  ]
}
