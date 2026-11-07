import type { EChartsOption, LineSeriesOption, XAXisComponentOption, YAXisComponentOption } from 'echarts'
import type { TrendSource } from './types'
import Dayjs from 'dayjs'
import { escapeHtml, fillBuckets, formatTrendValue, trendLineData } from './model'

export const TREND_COLORS = ['#165dff', '#e87808', '#00a58b', '#8b5cf6', '#d94f87', '#198bb8']
const DARK_COLORS = ['#74a3ff', '#ffb363', '#51d7be', '#b69aff', '#ff8eba', '#67c9ef']

interface ChartOptions {
  dark?: boolean
  split?: boolean
  showRange?: boolean
  compact?: boolean
}

export function buildTrendOption(sources: TrendSource[], options: ChartOptions = {}): EChartsOption {
  const { dark = false, split = false, showRange = true, compact = false } = options
  const text = dark ? '#c9cdd4' : '#4e5969'
  const muted = dark ? '#a0a7b1' : '#6b7785'
  const gridColor = dark ? '#343a46' : '#edf0f5'
  const palette = dark ? DARK_COLORS : TREND_COLORS
  const groups = split ? sources.map((source) => [source]) : [sources]
  const series: LineSeriesOption[] = []
  const grids: NonNullable<EChartsOption['grid']>[] = []
  const xAxes: XAXisComponentOption[] = []
  const yAxes: YAXisComponentOption[] = []
  const titles: { text: string, top: number, left: number, textStyle: { color: string, fontSize: number, fontWeight: 'normal' } }[] = []
  const reference = sources[0]?.trend
  if (!reference) return {}
  const bucketsByKey = new Map(sources.map((source) => [source.key, fillBuckets(source.trend)]))
  const top = sources.length > 1 && !split ? (compact ? 92 : 62) : 44
  const panelHeight = split ? 230 : 290
  groups.forEach((group, groupIndex) => {
    const panelTop = top + groupIndex * panelHeight
    const values = group.flatMap((source) => source.trend.buckets.flatMap((bucket) => [bucket.min, bucket.max])).filter((value): value is number => value !== null)
    const min = values.length ? Math.min(...values) : 0
    const max = values.length ? Math.max(...values) : 1
    const padding = Math.max((max - min) * 0.12, Math.abs(max) * 0.005, 0.01)
    const roughStep = (max - min + padding * 2) / 4
    const magnitude = 10 ** Math.floor(Math.log10(roughStep))
    const interval = ([1, 2, 2.5, 5, 10].find((step) => step * magnitude >= roughStep) || 10) * magnitude
    const unit = group[0]?.unit || '未配置单位'
    grids.push({ top: panelTop, left: compact ? 54 : 70, right: 24, height: panelHeight - 65 })
    if (split) titles.push({ text: group[0]?.label || '', top: panelTop - 35, left: compact ? 16 : 24, textStyle: { color: text, fontSize: 12, fontWeight: 'normal' } })
    xAxes.push({ gridIndex: groupIndex, type: 'time', min: reference.beginCreateTime * 1000, max: reference.endCreateTime * 1000, boundaryGap: [0, 0], splitNumber: compact ? 3 : 6, axisTick: { show: false }, axisLine: { lineStyle: { color: gridColor } }, axisLabel: { color: muted, hideOverlap: true, fontSize: 11 }, axisPointer: { show: true, label: { formatter: (params) => Dayjs(Number(params.value)).format('MM-DD HH:mm:ss') } } })
    yAxes.push({ gridIndex: groupIndex, type: 'value', name: unit, nameGap: 12, nameTextStyle: { color: muted, fontSize: 11 }, min: Math.floor((min - padding) / interval) * interval, max: Math.ceil((max + padding) / interval) * interval, interval, axisLabel: { color: muted, fontSize: 11, formatter: (value: number) => formatTrendValue(value) }, splitLine: { lineStyle: { color: gridColor } } })
    group.forEach((source) => {
      const index = sources.indexOf(source)
      const color = palette[(source.colorIndex ?? index) % palette.length]
      const buckets = bucketsByKey.get(source.key) || []
      const hasRaw = source.rawPoints !== undefined
      const validCount = hasRaw ? source.rawPoints!.filter((point) => point.value !== null).length : source.trend.buckets.filter((bucket) => bucket.avg !== null).length
      const lineData = trendLineData(source)
      if (showRange && !hasRaw && (split || sources.length === 1)) {
        const stack = `range-${source.key}`
        series.push({ id: `${stack}-base`, name: `${source.label}下界`, type: 'line', xAxisIndex: groupIndex, yAxisIndex: groupIndex, stack, stackStrategy: 'all', data: buckets.map((bucket) => [bucket.time, bucket.min]), symbol: 'none', lineStyle: { opacity: 0 }, itemStyle: { opacity: 0 }, silent: true, tooltip: { show: false } })
        series.push({ id: `${stack}-width`, name: `${source.label}范围`, type: 'line', xAxisIndex: groupIndex, yAxisIndex: groupIndex, stack, stackStrategy: 'all', data: buckets.map((bucket) => [bucket.time, bucket.min === null || bucket.max === null ? null : bucket.max - bucket.min]), symbol: 'none', lineStyle: { opacity: 0 }, areaStyle: { color, opacity: dark ? 0.22 : 0.12 }, silent: true, tooltip: { show: false } })
        // 独立时间桶也保留最小/最大端点，不依赖相邻桶形成面积。
        for (const key of ['min', 'max'] as const) series.push({ id: `${source.key}-${key}`, name: source.label, type: 'line', xAxisIndex: groupIndex, yAxisIndex: groupIndex, data: buckets.map((bucket) => [bucket.time, bucket[key]]), showSymbol: true, symbolSize: validCount <= 80 ? 3 : 0, symbol: 'circle', lineStyle: { opacity: 0.25, width: 1 }, itemStyle: { color, opacity: 0.5 }, tooltip: { show: false }, silent: true, connectNulls: false })
      }
      series.push({ id: source.key, name: source.label, type: 'line', xAxisIndex: groupIndex, yAxisIndex: groupIndex, data: lineData, smooth: false, connectNulls: false, showSymbol: true, symbol: ['circle', 'rect', 'triangle', 'diamond'][index % 4], symbolSize: (_value, params) => {
        if (validCount <= 80) return 6
        if (validCount <= 300) return 4
        const i = params.dataIndex
        return lineData[i - 1]?.[1] == null || lineData[i + 1]?.[1] == null ? 5 : 0
      }, lineStyle: { width: 2, color, type: index % 2 ? 'dashed' : 'solid' }, itemStyle: { color }, emphasis: { focus: 'series' }, clip: true })
    })
  })
  return {
    animation: false,
    backgroundColor: 'transparent',
    color: palette,
    title: titles,
    grid: grids as EChartsOption['grid'],
    xAxis: xAxes,
    yAxis: yAxes,
    legend: { type: 'scroll', show: sources.length > 1 && !split, top: 6, left: 16, right: 16, textStyle: { color: text, fontSize: 12, width: compact ? 150 : 280, overflow: 'truncate' }, data: sources.map((source) => source.label), tooltip: { show: true } },
    axisPointer: { link: [{ xAxisIndex: 'all' }] },
    tooltip: {
      trigger: 'axis',
      confine: true,
      backgroundColor: dark ? '#232832' : '#fff',
      borderColor: gridColor,
      textStyle: { color: text, fontSize: 12 },
      formatter(params) {
        const list = Array.isArray(params) ? params : [params]
        const active = list.filter((item) => sources.some((source) => source.key === item.seriesId))
        const first = active[0]
        if (!first || !Array.isArray(first.value)) return ''
        const time = Number(first.value[0])
        const content = active.map((item) => {
          const source = sources.find((entry) => entry.key === item.seriesId)!
          const suffix = escapeHtml(source.unit)
          if (source.rawPoints) return `${escapeHtml(source.label)}：${formatTrendValue(Array.isArray(item.value) && item.value[1] != null ? Number(item.value[1]) : null)} ${suffix}`
          const bucket = bucketsByKey.get(source.key)?.find((entry) => entry.time === Number(Array.isArray(item.value) ? item.value[0] : Number.NaN))
          if (!bucket || bucket.avg === null) return `${escapeHtml(source.label)}：无有效数值`
          const countLabel = source.trend.sampled || source.trend.countExact === false ? '采样记录' : '记录'
          return `${escapeHtml(source.label)}：${formatTrendValue(bucket.avg)} ${suffix}<br>最小—最大：${formatTrendValue(bucket.min)}–${formatTrendValue(bucket.max)} ${suffix}<br>有效数值：${bucket.numericCount} 条 / ${countLabel}：${bucket.count} 条`
        })
        const prefix = sources[0]?.rawPoints ? '原始记录' : `聚合时段 ${Dayjs(time).format('HH:mm:ss')}–${Dayjs(Math.min(time + reference.intervalSeconds * 1000 - 1, reference.endCreateTime * 1000)).format('HH:mm:ss')}`
        return `${Dayjs(time).format('YYYY-MM-DD HH:mm:ss')}<br>${prefix}<br>${content.join('<br>')}`
      }
    },
    dataZoom: [
      { type: 'inside', xAxisIndex: xAxes.map((_, index) => index), filterMode: 'none', zoomOnMouseWheel: 'ctrl', moveOnMouseWheel: false },
      { type: 'slider', xAxisIndex: xAxes.map((_, index) => index), filterMode: 'none', bottom: 8, height: 20, showDataShadow: false, borderColor: 'transparent', backgroundColor: dark ? '#2b3340' : '#f2f4f8', fillerColor: dark ? 'rgba(116,163,255,0.18)' : 'rgba(22,93,255,0.10)', textStyle: { color: muted } }
    ],
    series
  }
}
