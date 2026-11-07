import type { DeviceHistorySlice } from './deviceHistory.api'
import type { DeviceLogItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import Dayjs from 'dayjs'

export interface TrackPoint {
  lng: number
  lat: number
  time: string
}

export const TRACK_PAGE_SIZE = 200
export const TRACK_MAX_RECORDS = 20000
const COORDINATE_PAIR = /^(-?\d+(?:\.\d+)?)\s*[,，]\s*(-?\d+(?:\.\d+)?)$/
const TIMESTAMP = /^\d{10,13}$/

export function isTrackField(field: Pick<ProductMetadataItem, 'id'>) {
  return field.id.trim().toLowerCase() === 'coordinates'
}

function coordinate(lng: unknown, lat: unknown) {
  if (![lng, lat].every((value) => (typeof value === 'number' || typeof value === 'string') && String(value).trim() !== '')) return
  const longitude = Number(lng)
  const latitude = Number(lat)
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude) || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90 || (longitude === 0 && latitude === 0)) return
  return { lng: longitude, lat: latitude }
}

export function parseCoordinates(value: unknown, depth = 0): { lng: number, lat: number } | undefined {
  if (depth > 4) return
  if (Array.isArray(value)) return value.length === 2 ? coordinate(value[0], value[1]) : undefined
  if (value && typeof value === 'object') {
    const object = value as Record<string, unknown>
    return coordinate(object.lng ?? object.lon ?? object.longitude, object.lat ?? object.latitude)
      || parseCoordinates(object.coordinates ?? object.location ?? object.gps ?? object.point ?? object.properties, depth + 1)
  }
  if (typeof value !== 'string') return
  const text = value.trim()
  const pair = text.match(COORDINATE_PAIR)
  if (pair) return coordinate(pair[1], pair[2])
  if (!['[', '{', '"'].includes(text[0])) return
  try {
    return parseCoordinates(JSON.parse(text), depth + 1)
  } catch {
    return undefined
  }
}

export function trackTimestamp(value: string) {
  return TIMESTAMP.test(value) ? Number(value) * (value.length === 10 ? 1000 : 1) : Dayjs(value).valueOf()
}

export function trackPoints(rows: DeviceLogItem[]): TrackPoint[] {
  return rows.flatMap((row) => {
    const time = String(row.createTime || row.eventTime || '')
    if (!Number.isFinite(trackTimestamp(time))) return []
    for (const value of [row.ext2, row.value, row.point, row.content]) {
      const coords = parseCoordinates(value)
      if (coords) return [{ ...coords, time }]
    }
    return []
  }).sort((first, second) => trackTimestamp(first.time) - trackTimestamp(second.time))
}

/** A map is shown only after every page in the fixed window has been read. */
export async function loadTrackRecords(
  fetchPage: (cursor?: string) => Promise<DeviceHistorySlice>,
  signal: AbortSignal,
  progress: (count: number) => void
) {
  const rows: DeviceLogItem[] = []
  const cursors = new Set<string>()
  let cursor: string | undefined
  while (!signal.aborted) {
    const page = await fetchPage(cursor)
    if (signal.aborted) break
    if (!Array.isArray(page.list) || typeof page.hasMore !== 'boolean' || page.list.length > TRACK_PAGE_SIZE) throw new Error('轨迹分页信息不完整，请刷新后重试')
    rows.push(...page.list)
    if (rows.length > TRACK_MAX_RECORDS || (rows.length === TRACK_MAX_RECORDS && page.hasMore)) throw new Error(`所选时段超过 ${TRACK_MAX_RECORDS.toLocaleString('zh-CN')} 条定位记录，请缩小时间范围后查看完整轨迹`)
    progress(rows.length)
    if (!page.hasMore) return rows
    if (!page.list.length || !page.nextCursor || cursors.has(page.nextCursor)) throw new Error('轨迹翻页位置无效，请刷新后重试')
    cursors.add(page.nextCursor)
    cursor = page.nextCursor
  }
  throw new Error('轨迹查询已取消')
}
