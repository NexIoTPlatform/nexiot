export interface HistoryRange {
  beginCreateTime: number
  endCreateTime: number
}

export interface PropertyTrendBucket {
  time: number
  count: number
  numericCount: number
  min: number | null
  max: number | null
  avg: number | null
}

export interface PropertyTrend extends HistoryRange {
  intervalSeconds: number
  totalCount: number
  numericCount: number
  sampled?: boolean
  countExact?: boolean
  scannedCount?: number
  scanLimit?: number
  samplingMode?: 'time-window-sample' | 'exact'
  buckets: PropertyTrendBucket[]
}

export interface RawTrendPoint {
  time: number
  value: number | null
}

export interface TrendSource {
  key: string
  label: string
  unit: string
  trend: PropertyTrend
  rawPoints?: RawTrendPoint[]
  colorIndex?: number
}

export interface TrendZoomEvent {
  start?: number
  end?: number
  batch?: { start?: number, end?: number }[]
}
