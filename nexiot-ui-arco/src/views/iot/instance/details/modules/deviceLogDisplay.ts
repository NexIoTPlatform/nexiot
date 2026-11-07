import type { DeviceLogItem } from '@/apis/iot/device'

const technicalFields = new Set(['sourcePayload', 'imei', 'csq', 'timestamp', 'gps', 'iccid', 'imsi', 'heartbeatType'])
const WHITESPACE_RE = /\s+/g
const fieldAliases: Record<string, string> = {
  openState: '开关状态',
  openStateDesc: '设备状态',
  operationResult: '执行结果',
  meterData: '电表读数',
  voltage: '电压',
  current: '电流',
  power: '功率',
  remainEnergy: '剩余电量',
  totalEnergy: '总电量',
  meterNo: '表号',
  valveOn: '阀门状态'
}

export function messageTypeMeta(type?: string) {
  return ({
    EVENT: { label: '事件上报', color: 'orange' },
    PROPERTIES: { label: '属性上报', color: 'arcoblue' },
    FUNCTIONS: { label: '功能调用', color: 'purple' },
    REPLY: { label: '功能回复', color: 'green' },
    ACK: { label: '平台应答', color: 'gray' }
  } as Record<string, { label: string, color: string }>)[type || ''] || { label: type || '未知消息', color: 'gray' }
}

export function formatContent(content?: string) {
  if (!content) return '-'
  try {
    return JSON.stringify(JSON.parse(content), null, 2)
  } catch {
    return content
  }
}

export function messagePayloadPreview(content?: string, messageType?: string) {
  if (!content) return '-'
  try {
    const payload = JSON.parse(content)
    return JSON.stringify(payload)
  } catch {
    return content.replace(WHITESPACE_RE, ' ').trim()
  }
}

export function messageHeadline(record: DeviceLogItem, eventNames?: ReadonlyMap<string, string>) {
  const content = parseContent(record.content)
  if (record.messageType === 'EVENT') {
    const identifier = messageIdentifier(record)
    return String(eventNames?.get(identifier) || content.eventName || eventLabel(identifier))
  }
  if (record.messageType === 'PROPERTIES') {
    const count = businessEntries(content).length
    return count ? `设备上报 ${count} 项数据` : '设备通信数据已更新'
  }
  if (record.messageType === 'FUNCTIONS') return '功能指令下发'
  if (record.messageType === 'REPLY') return '设备执行回复'
  if (record.messageType === 'ACK') return '平台处理应答'
  return record.event || '设备消息'
}

export function messageIdentifier(record: DeviceLogItem) {
  const content = parseContent(record.content)
  if (record.messageType === 'EVENT') return String(record.event || content.event || content.eventName || '-')
  if (['FUNCTIONS', 'REPLY', 'ACK'].includes(record.messageType || '')) return String(record.event || content.function || '-')
  if (record.messageType === 'PROPERTIES') return record.property || '属性上报'
  return String(record.event || content.event || content.function || '-')
}

export function messageSummary(record: DeviceLogItem, propertyNames: ReadonlyMap<string, string>) {
  const content = parseContent(record.content)
  if (record.messageType === 'EVENT') {
    const data = eventData(content)
    return Object.keys(data).length ? summarizeEntries(data, propertyNames) : '设备事件已上报'
  }
  if (record.messageType === 'PROPERTIES') {
    const entries = businessEntries(content)
    return entries.length ? summarizeEntries(Object.fromEntries(entries), propertyNames) : '基础通信数据已接收'
  }
  const data = parseRecord(content.data || content.value || content)
  const entries = Object.entries(data).filter(([key]) => !['function', 'commandId'].includes(key))
  return entries.length ? summarizeEntries(Object.fromEntries(entries), propertyNames) : '无业务参数'
}

function parseContent(content?: string): Record<string, unknown> {
  if (!content) return {}
  try {
    const value = JSON.parse(content)
    return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : { value }
  } catch {
    return { value: content }
  }
}

function parseRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function businessEntries(content: Record<string, unknown>) {
  const properties = parseRecord(content.properties)
  const data = content.messageType === 'PROPERTIES' || Object.keys(properties).length
    ? properties
    : Object.keys(parseRecord(content.data)).length ? parseRecord(content.data) : content
  return Object.entries(data).filter(([key]) => !technicalFields.has(key) && !['function', 'commandId'].includes(key))
}

function eventData(content: Record<string, unknown>) {
  if (content.messageType === 'EVENT' && 'event' in content) return parseRecord(content.data)
  if (['cameraEventType', 'evidence', 'instanceKey'].some((key) => key in content)) return content
  return Object.keys(parseRecord(content.data)).length ? parseRecord(content.data) : content
}

function summarizeEntries(data: Record<string, unknown>, propertyNames: ReadonlyMap<string, string>) {
  const entries = Object.entries(data).filter(([key]) => !technicalFields.has(key))
  return entries.length
    ? entries.slice(0, 3).map(([key, value]) => `${propertyNames.get(key) || fieldAliases[key] || key} ${compactValue(value)}`).join(' · ')
    : '事件数据已接收'
}

function compactValue(value: unknown) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function eventLabel(event: string) {
  return ({ online: '设备上线', offline: '设备离线' } as Record<string, string>)[event.toLowerCase()] || event || '设备事件'
}
