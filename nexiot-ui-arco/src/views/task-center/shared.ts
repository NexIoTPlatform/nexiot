import type { TaskCenterItem, TaskDefinition, TaskDetailItem } from './api'
import type { ProductMetadataItem } from '@/apis/iot/product'

export const taskTypeOptions = [
  { label: '三元组生成', value: 'DEVICE_CREDENTIAL_GENERATE' },
  { label: '设备导入', value: 'DEVICE_IMPORT' },
  { label: '设备作业', value: 'DEVICE_FUNCTION_JOB' }
]

export const taskStatusOptions = [
  { label: '待执行', value: 'PENDING' },
  { label: '进行中', value: 'RUNNING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '部分成功', value: 'PARTIAL_SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' }
]

export const itemStatusOptions = [
  { label: '待执行', value: 'PENDING' },
  { label: '进行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' }
]

export const scheduleTypeOptions = [
  { label: '手动触发', value: 'MANUAL' },
  { label: '定时调度', value: 'CRON' }
]

export const scheduleStatusOptions = [
  { label: '已启用', value: 'ENABLED' },
  { label: '已暂停', value: 'PAUSED' }
]

export function optionLabel(options: { label: string, value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function taskTypeLabel(value?: string) {
  return optionLabel(taskTypeOptions, value)
}

export function taskStatusLabel(value?: string) {
  return optionLabel(taskStatusOptions, value)
}

export function itemStatusLabel(value?: string) {
  return optionLabel(itemStatusOptions, value)
}

export function taskStatusColor(value?: string) {
  return ({ PENDING: 'gray', RUNNING: 'arcoblue', COMPLETED: 'green', PARTIAL_SUCCESS: 'orange', FAILED: 'red', CANCELED: 'gray' } as Record<string, string>)[value || ''] || 'gray'
}

export function itemStatusColor(value?: string) {
  return ({ PENDING: 'gray', RUNNING: 'arcoblue', SUCCESS: 'green', FAILED: 'red' } as Record<string, string>)[value || ''] || 'gray'
}

export function triggerTypeLabel(value?: string) {
  return ({ DIRECT: '立即执行', MANUAL: '手动触发', SCHEDULE: '定时触发' } as Record<string, string>)[value || ''] || value || '-'
}

export function scheduleTypeLabel(value?: string) {
  return optionLabel(scheduleTypeOptions, value)
}

export function scheduleStatusLabel(value?: string) {
  return optionLabel(scheduleStatusOptions, value)
}

export function progressPercent(task: Pick<TaskCenterItem, 'totalCount' | 'successCount' | 'failCount'>) {
  const total = Number(task.totalCount || 0)
  if (!total) return 0
  return Math.min(1, Number(((Number(task.successCount || 0) + Number(task.failCount || 0)) / total).toFixed(4)))
}

export function parseRecord(value: unknown): Record<string, any> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, any>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

export function parseArray(value: unknown): unknown[] {
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(String(value || '[]'))
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function formatJson(value: unknown, compact = false) {
  const parsed = typeof value === 'string' ? parseJsonValue(value) : value
  try {
    return JSON.stringify(parsed ?? {}, null, compact ? 0 : 2)
  } catch {
    return String(value ?? '')
  }
}

function parseJsonValue(value: string) {
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

export function normalizeFunctions(productMetadata: unknown) {
  const metadata = parseRecord(productMetadata)
  return (Array.isArray(metadata.functions) ? metadata.functions : []) as ProductMetadataItem[]
}

export function normalizeFunctionInputs(value: unknown, defaultRequired = true) {
  return parseArray(value).map((item) => {
    const field = parseRecord(item) as ProductMetadataItem & { required?: boolean, expands?: Record<string, unknown> | string }
    const expands = parseRecord(field.expands)
    return { ...field, valueType: parseRecord(field.valueType), required: Boolean(field.required ?? expands.required ?? defaultRequired) }
  })
}

export function taskTimeText(task: TaskCenterItem) {
  if (!task.startTime) return '未开始'
  return task.endTime ? `${task.startTime} 至 ${task.endTime}` : `${task.startTime} 至今`
}

export function definitionNextTime(definition: TaskDefinition) {
  if (definition.scheduleType !== 'CRON') return '手动触发'
  return definition.nextExecuteTime || (definition.scheduleStatus === 'PAUSED' ? '已暂停' : '待计算')
}

export function credentialItemData(item: TaskDetailItem, fallbackProductKey = '') {
  const raw = parseRecord(item.rawData)
  const result = parseRecord(item.resultData)
  const productKey = String(result.productKey || raw.productKey || fallbackProductKey)
  const deviceId = String(result.deviceId || raw.deviceId || item.itemKey || '')
  return {
    deviceId: deviceId || '-',
    deviceName: result.deviceName || raw.deviceName || item.itemName || '-',
    iotId: result.iotId || raw.iotId || (productKey && deviceId ? `${productKey}${deviceId}` : '-'),
    clientId: result.clientId || raw.clientId || (productKey && deviceId ? `${productKey}.${deviceId}` : '-'),
    username: result.username || raw.username || (productKey && deviceId ? `${deviceId}&${productKey}` : '-'),
    password: result.password || result.deviceSecret || result.secretKey || raw.password || raw.deviceSecret || raw.secretKey || ''
  }
}

export function importItemData(item: TaskDetailItem) {
  const raw = parseRecord(item.rawData)
  return {
    rowNo: item.rowNo || raw.rowNo || '-',
    deviceId: raw.deviceId || item.itemKey || '-',
    deviceName: raw.deviceName || item.itemName || '-',
    message: item.message || '-'
  }
}
