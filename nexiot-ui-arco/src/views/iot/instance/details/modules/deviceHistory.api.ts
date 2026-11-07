import type { DeviceLogItem } from '@/apis/iot/device'
import http from '@/utils/http'

export { getDevicePropertyTrend, normalizePropertyTrend } from '@/components/PropertyTrend/api'
export type { HistoryRange, PropertyTrend, PropertyTrendBucket } from '@/components/PropertyTrend/types'

export interface DeviceHistorySlice {
  list: DeviceLogItem[]
  hasMore: boolean
  nextCursor?: string
}

export function getDeviceMetadataSlice(params: Record<string, unknown>, signal?: AbortSignal) {
  return http.get<DeviceHistorySlice>('/admin/v2/device/log/meta/slice', params, { signal })
}

export function getDeviceLogSlice(params: Record<string, unknown>) {
  return http.get<DeviceHistorySlice>('/admin/v2/device/log/slice', params)
}

export function exportDeviceRawLogs(params: Record<string, unknown>) {
  return http.download('/admin/v2/device/log/export', {
    method: 'post',
    params,
    fileName: `device_logs_${Date.now()}.xlsx`
  })
}
