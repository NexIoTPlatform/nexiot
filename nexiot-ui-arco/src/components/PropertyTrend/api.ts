import type { HistoryRange, PropertyTrend } from './types'
import http from '@/utils/http'

type TrendNumber = number | string

interface PropertyTrendResponse {
  beginCreateTime: TrendNumber
  endCreateTime: TrendNumber
  intervalSeconds: TrendNumber
  totalCount: TrendNumber
  numericCount: TrendNumber
  sampled?: boolean
  countExact?: boolean
  scannedCount?: TrendNumber
  scanLimit?: TrendNumber
  samplingMode?: 'time-window-sample' | 'exact'
  buckets: {
    time: TrendNumber
    count: TrendNumber
    numericCount: TrendNumber
    min: TrendNumber | null
    max: TrendNumber | null
    avg: TrendNumber | null
  }[]
}

function trendNumber(value: TrendNumber, integer = false): number {
  const number = Number(value)
  if ((typeof value !== 'number' && typeof value !== 'string') || String(value).trim() === '' || !Number.isFinite(number) || (integer && (!Number.isSafeInteger(number) || number < 0))) {
    throw new Error('趋势数据格式异常，请刷新后重试')
  }
  return number
}

export function normalizePropertyTrend(data: PropertyTrendResponse): PropertyTrend {
  if (!data || !Array.isArray(data.buckets)) throw new Error('趋势数据格式异常，请刷新后重试')
  if ((data.sampled !== undefined && typeof data.sampled !== 'boolean') || (data.countExact !== undefined && typeof data.countExact !== 'boolean') || (data.samplingMode !== undefined && !['time-window-sample', 'exact'].includes(data.samplingMode))) throw new Error('趋势采样信息异常，请刷新后重试')
  // 正式 Spring MVC 将 Long 序列化为字符串；图表时间索引和加权统计必须先转成数字。
  const normalized = {
    beginCreateTime: trendNumber(data.beginCreateTime, true),
    endCreateTime: trendNumber(data.endCreateTime, true),
    intervalSeconds: trendNumber(data.intervalSeconds, true),
    totalCount: trendNumber(data.totalCount, true),
    numericCount: trendNumber(data.numericCount, true),
    sampled: data.sampled ?? false,
    countExact: data.countExact ?? !data.sampled,
    scannedCount: trendNumber(data.scannedCount ?? data.totalCount, true),
    scanLimit: trendNumber(data.scanLimit ?? data.totalCount, true),
    samplingMode: data.samplingMode ?? 'exact',
    buckets: data.buckets.map((bucket) => ({
      time: trendNumber(bucket.time, true),
      count: trendNumber(bucket.count, true),
      numericCount: trendNumber(bucket.numericCount, true),
      min: bucket.min === null ? null : trendNumber(bucket.min),
      max: bucket.max === null ? null : trendNumber(bucket.max),
      avg: bucket.avg === null ? null : trendNumber(bucket.avg)
    }))
  }
  if (normalized.intervalSeconds === 0 || normalized.endCreateTime < normalized.beginCreateTime || (normalized.endCreateTime - normalized.beginCreateTime) / normalized.intervalSeconds > 1000) {
    throw new Error('趋势时间范围异常，请刷新后重试')
  }
  return normalized
}

export async function getDevicePropertyTrend(params: Record<string, unknown>, signal?: AbortSignal) {
  const response = await http.get<PropertyTrendResponse>('/admin/v2/device/log/meta/trend', params, { signal })
  return { ...response, data: normalizePropertyTrend(response.data) }
}

export interface TrendQuery extends HistoryRange {
  productKey: string
  iotId: string
  deviceId?: string
  property: string
  maxPoints?: number
  maxScanRows?: number
}

export interface TrendData {
  trend: PropertyTrend
  note?: string
}

export async function loadPropertyTrend(query: TrendQuery, signal: AbortSignal): Promise<TrendData> {
  const { data: trend } = await getDevicePropertyTrend({ ...query, samplingMode: 'sample' }, signal)
  const note = trend.sampled || trend.countExact === false
    ? `已按时间窗口在查询时段内分布采样，采样上限 ${(trend.scanLimit ?? query.maxScanRows ?? 10000).toLocaleString()} 条。记录数与均值、最小值、最大值仅代表采样结果，可能遗漏短时尖峰；可提高采样上限或缩短时间范围查看细节。`
    : undefined
  return { trend, note }
}
