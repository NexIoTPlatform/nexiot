/* eslint-disable style/max-statements-per-line */
import type { SceneAction, SceneFilter, SceneLog, SceneLogAction, SceneTrigger } from './api'
import Dayjs from 'dayjs'

export function parseList<T>(value?: string | T[]) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed as T[] : []
  } catch {
    return []
  }
}

export function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = Dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

export function getTriggerLabel(type?: string) {
  return { device: '设备触发', timer: '定时触发', time: '定时触发', manual: '手动触发', unknown: '未知触发' }[type || ''] || type || '未知'
}

export function getTriggerColor(type?: string) {
  return { device: 'arcoblue', timer: 'orange', time: 'orange', manual: 'purple' }[type || ''] || 'gray'
}

export function getActionLabel(type?: string) {
  return type === 'notice' ? '通知发送' : type === 'device' ? '设备联动' : type || '未知动作'
}

export function getStatusLabel(status?: number) {
  return status === 1 ? '成功' : status === 2 ? '失败' : '部分成功'
}

export function getStatusColor(status?: number) {
  return status === 1 ? 'green' : status === 2 ? 'red' : 'orange'
}

export function getSceneTriggerTypes(value?: string | SceneTrigger[]) {
  return [...new Set(parseList<SceneTrigger>(value).map((item) => item.trigger).filter(Boolean))] as string[]
}

export function parseLogActions(log?: SceneLog) {
  return parseList<SceneLogAction>(log?.content)
}

export function parseDeviceMeta(value?: string) {
  if (!value) return undefined
  try { return JSON.parse(value) as Record<string, any> } catch { return undefined }
}

export function formatResult(value: unknown) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'string') {
    try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return value }
  }
  try { return JSON.stringify(value, null, 2) } catch { return String(value) }
}

export function getOperatorLabel(value?: string) {
  return { eq: '=', ne: '!=', not: '!=', gt: '>', lt: '<', gte: '>=', lte: '<=', in: '包含', nin: '不包含', like: '模糊匹配', regex: '正则匹配' }[value || ''] || value || '-'
}

export function isFilterMatched(filter: SceneFilter, actual: unknown) {
  const expected = filter.value
  if (actual === undefined || actual === null || expected === undefined || expected === null || !filter.operator) return false
  if (['gt', 'lt', 'gte', 'lte'].includes(filter.operator || '')) {
    const left = Number(actual)
    const right = Number(expected)
    if (!Number.isFinite(left) || !Number.isFinite(right)) return false
    return filter.operator === 'gt' ? left > right : filter.operator === 'lt' ? left < right : filter.operator === 'gte' ? left >= right : left <= right
  }
  if (filter.operator === 'ne' || filter.operator === 'not') return String(actual) !== String(expected)
  if (filter.operator === 'in') return String(expected).split(',').map((item) => item.trim()).includes(String(actual))
  if (filter.operator === 'nin') return !String(expected).split(',').map((item) => item.trim()).includes(String(actual))
  if (filter.operator === 'like') return String(actual).includes(String(expected))
  return String(actual) === String(expected)
}

export function isValidTrigger(trigger: SceneTrigger) {
  if (trigger.trigger === 'manual') return true
  if (trigger.trigger === 'timer') return Boolean(trigger.cron?.trim())
  if (trigger.trigger !== 'device' || !trigger.deviceId || !trigger.productKey || !trigger.type) return false
  if (trigger.type === 'event' && !trigger.modelId) return false
  if (trigger.type === 'event' && !trigger.filters?.length) return true
  return Boolean(trigger.filters?.length && trigger.filters.every((item) => item.key && item.operator && item.value !== undefined && item.value !== ''))
}

export function triggerFieldValue(actual: Record<string, unknown>, trigger: SceneTrigger, key?: string): unknown {
  if (!key?.trim()) return undefined
  let value = trigger.type === 'event' ? actual.data : actual.properties
  if (!value || typeof value !== 'object') return undefined
  if (Object.prototype.hasOwnProperty.call(value, key)) return (value as Record<string, unknown>)[key]
  const path = String(key || '').replace(trigger.type === 'event' ? /^data\./ : /^properties\./, '')
  if (Object.prototype.hasOwnProperty.call(value, path)) return (value as Record<string, unknown>)[path]
  if (trigger.type === 'event' && ['alarmType', 'cameraEventType'].includes(path)) {
    const alias = path === 'alarmType' ? 'cameraEventType' : 'alarmType'
    if (Object.prototype.hasOwnProperty.call(value, alias)) return (value as Record<string, unknown>)[alias]
  }
  const data = value as Record<string, unknown>
  if (trigger.type === 'event' && !Object.prototype.hasOwnProperty.call(data, path.split('.')[0])
    && String(data.platformType).toUpperCase() === 'IMOULIFE'
    && data.outputData && typeof data.outputData === 'object' && !Array.isArray(data.outputData)) {
    // 旧规则的平铺字段从唯一保存的 outputData 读取，与后端执行路径一致。
    value = data.outputData
  }
  const segments = path.split('.')
  for (let index = 0; index < segments.length; index++) {
    if (!value || typeof value !== 'object') return undefined
    const part = segments[index]
    if (Array.isArray(value)) {
      if (!/^\d{1,6}$/.test(part)) return undefined
      value = value[Number(part)]
    } else {
      // 与规则执行保持一致：厂家字面字段中的点号优先于对象路径。
      const remaining = segments.slice(index).join('.')
      if (Object.prototype.hasOwnProperty.call(value, remaining)) return (value as Record<string, unknown>)[remaining]
      if (!Object.prototype.hasOwnProperty.call(value, part)) return undefined
      value = (value as Record<string, unknown>)[part]
    }
  }
  return value
}

export function isValidAction(action: SceneAction) {
  if (action.trigger === 'notice') return Boolean(action.noticeTemplateId)
  if (action.trigger !== 'device' || !action.deviceId || !action.productKey || !action.modelId) return false
  return (action.execData || []).every((item) => item.params !== undefined && item.params !== '')
}
