import type { HistoryRange, PropertyTrend, PropertyTrendBucket, RawTrendPoint, TrendSource, TrendZoomEvent } from './types'
import type { DeviceLogItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import Dayjs from 'dayjs'

const NUMERIC_TYPES = new Set(['int', 'long', 'float', 'double', 'number', 'short', 'byte', 'decimal', 'numeric'])
const NUMERIC_VALUE = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i
const TIMESTAMP = /^\d{10,13}$/
const TRAILING_ZERO = /\.0$/
const HTML_CHARACTERS = /[&<>"']/g
const HTML_ENTITIES: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', '\'': '&#39;' }

export function parseRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

export function fieldType(field?: ProductMetadataItem) {
  return String(parseRecord(field?.valueType).type || field?.type || 'string').toLowerCase()
}

export function isNumericField(field?: ProductMetadataItem) {
  return NUMERIC_TYPES.has(fieldType(field))
}

export function fieldUnit(field?: ProductMetadataItem) {
  return String(parseRecord(field?.valueType).unit || '').trim()
}

export function formatTrendValue(value: number | null | undefined) {
  return value == null || !Number.isFinite(value) ? '--' : value.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

export function formatInterval(seconds: number) {
  if (seconds < 60) return `${seconds} 秒`
  if (seconds < 3600) return `${(seconds / 60).toFixed(1).replace(TRAILING_ZERO, '')} 分钟`
  return `${(seconds / 3600).toFixed(1).replace(TRAILING_ZERO, '')} 小时`
}

export function validRange(range: HistoryRange) {
  return Number.isSafeInteger(range.beginCreateTime) && Number.isSafeInteger(range.endCreateTime)
    && range.beginCreateTime >= 0 && range.endCreateTime >= range.beginCreateTime
    && range.endCreateTime - range.beginCreateTime <= 31 * 86400
}

export function zoomRange(range: HistoryRange, event: TrendZoomEvent): HistoryRange | undefined {
  const { start = 0, end = 100 } = event.batch?.[0] || event
  if (!Number.isFinite(start) || !Number.isFinite(end) || start < 0 || end > 100 || start >= end || (start === 0 && end === 100)) return
  const span = range.endCreateTime - range.beginCreateTime
  return { beginCreateTime: Math.floor(range.beginCreateTime + span * start / 100), endCreateTime: Math.ceil(range.beginCreateTime + span * end / 100) }
}

export function fillBuckets(trend: PropertyTrend): PropertyTrendBucket[] {
  const count = Math.floor((trend.endCreateTime - trend.beginCreateTime) / trend.intervalSeconds) + 1
  if (!Number.isSafeInteger(count) || count < 1 || count > 1001) return []
  const values = new Map(trend.buckets.map((bucket) => [bucket.time, bucket]))
  return Array.from({ length: count }, (_, index) => {
    const time = (trend.beginCreateTime + index * trend.intervalSeconds) * 1000
    return values.get(time) || { time, count: 0, numericCount: 0, min: null, max: null, avg: null }
  })
}

export function historyStatistics(trend?: PropertyTrend) {
  const buckets = trend?.buckets.filter((bucket) => bucket.numericCount > 0 && bucket.avg != null) || []
  let count = 0
  let avg = 0
  for (const bucket of buckets) {
    const next = count + bucket.numericCount
    avg = avg * (count / next) + Number(bucket.avg) * (bucket.numericCount / next)
    count = next
  }
  const minima = buckets.flatMap((bucket) => bucket.min == null ? [] : [bucket.min])
  const maxima = buckets.flatMap((bucket) => bucket.max == null ? [] : [bucket.max])
  return { min: minima.length ? Math.min(...minima) : null, max: maxima.length ? Math.max(...maxima) : null, avg: count ? avg : null }
}

export function rawTrendPoints(rows: DeviceLogItem[], range: HistoryRange): RawTrendPoint[] {
  return rows.flatMap((row) => {
    const stamp = String(row.createTime || row.eventTime || '')
    const date = TIMESTAMP.test(stamp) ? Dayjs(Number(stamp) * (stamp.length === 10 ? 1000 : 1)) : Dayjs(stamp)
    const time = date.valueOf()
    if (!date.isValid() || time < range.beginCreateTime * 1000 || time > range.endCreateTime * 1000) return []
    const text = String(row.value ?? row.content ?? '').trim()
    const value = NUMERIC_VALUE.test(text) && Number.isFinite(Number(text)) ? Number(text) : null
    return [{ time, value }]
  }).sort((a, b) => a.time - b.time)
}

// 原始点之间只有出现明显长时间断报时才插入断点。
// 设备上报周期可能比聚合步长更长，按一个桶断线会让正常采样看起来像散点。
export function trendLineData(source: TrendSource): [number, number | null][] {
  const points = [...(source.rawPoints ?? source.trend.buckets.filter((bucket) => bucket.count > 0).map((bucket) => ({ time: bucket.time, value: bucket.avg })))].sort((a, b) => a.time - b.time)
  const step = source.trend.intervalSeconds * 1000
  const gaps = points.slice(1).map((point, index) => point.time - points[index].time).filter((gap) => gap > 0)
  const medianGap = gaps.length ? [...gaps].sort((a, b) => a - b)[Math.floor((gaps.length - 1) / 2)] : 0
  const gapLimit = Math.max(step * 4, medianGap * 3)
  return points.flatMap((point, index) => {
    const previous = points[index - 1]
    const value: [number, number | null] = [point.time, point.value]
    if (previous && point.time - previous.time > gapLimit) {
      return [[previous.time + step, null] as [number, null], value]
    }
    return [value]
  })
}

export function escapeHtml(value: string) {
  return value.replace(HTML_CHARACTERS, (character) => HTML_ENTITIES[character])
}
