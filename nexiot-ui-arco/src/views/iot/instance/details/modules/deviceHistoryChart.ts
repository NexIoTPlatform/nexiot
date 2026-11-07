import type { EChartsOption } from 'echarts'
import type { PropertyTrend, PropertyTrendBucket } from './deviceHistory.api'
import Dayjs from 'dayjs'

const TRAILING_ZERO_REGEX = /\.0$/

export function historyChartOption(trend: PropertyTrend, unit: string, isDark = false): EChartsOption {
  const values = new Map(trend.buckets.map((bucket) => [bucket.time, bucket]))
  const buckets: PropertyTrendBucket[] = []
  for (let time = trend.beginCreateTime * 1000; time <= trend.endCreateTime * 1000; time += trend.intervalSeconds * 1000) {
    buckets.push(values.get(time) || { time, count: 0, numericCount: 0, min: null, max: null, avg: null })
  }

  const textColor = isDark ? '#a9aeb8' : '#4e5969'
  const subTextColor = isDark ? '#636b77' : '#86909c'
  const splitLineColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.04)'
  const axisLineColor = isDark ? '#2e3238' : '#e5e6eb'
  const popupBg = isDark ? 'rgba(29, 33, 41, 0.95)' : 'rgba(255, 255, 255, 0.96)'
  const popupBorder = isDark ? '#373c44' : '#e5e6eb'
  const dividerColor = isDark ? '#2a2f37' : '#f2f3f5'

  const stats = historyStatistics(trend)
  const markLine = stats.avg != null
    ? {
        silent: true,
        symbol: 'none',
        lineStyle: {
          color: isDark ? 'rgba(22, 93, 255, 0.45)' : 'rgba(22, 93, 255, 0.35)',
          type: 'dotted' as const,
          width: 1
        },
        label: {
          show: true,
          position: 'insideEndTop' as const,
          formatter: `均值: ${formatTrendValue(stats.avg)}${unit ? ` ${unit}` : ''}`,
          fontSize: 10,
          color: subTextColor,
          fontFamily: 'monospace'
        },
        data: [{ yAxis: stats.avg }]
      }
    : undefined

  return {
    backgroundColor: 'transparent',
    color: ['#165dff', '#ff7d00', '#00b42a'],
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 2,
      itemGap: 14,
      textStyle: { color: textColor, fontSize: 11 },
      data: ['平均值', '最高值', '最低值']
    },
    grid: {
      top: 28,
      left: 6,
      right: 14,
      bottom: 36,
      containLabel: true
    },
    axisPointer: {
      link: [{ xAxisIndex: 'all' }],
      label: {
        backgroundColor: isDark ? '#373c44' : '#1d2129',
        fontSize: 11,
        fontFamily: 'monospace'
      }
    },
    tooltip: {
      trigger: 'axis',
      confine: true,
      backgroundColor: popupBg,
      borderColor: popupBorder,
      borderWidth: 1,
      padding: [8, 12],
      extraCssText: 'box-shadow: 0 4px 16px rgba(0,0,0,0.12); backdrop-filter: blur(8px); border-radius: 6px;',
      axisPointer: {
        type: 'cross',
        lineStyle: {
          color: isDark ? 'rgba(60, 126, 255, 0.5)' : '#165dff',
          width: 1,
          type: 'dashed'
        },
        crossStyle: {
          color: isDark ? '#636b77' : '#c9cdd4'
        }
      },
      formatter(params) {
        const item = Array.isArray(params) ? params[0] : params
        const bucket = buckets[item.dataIndex]
        if (!bucket) return ''
        const start = Dayjs(bucket.time).format('YYYY-MM-DD HH:mm:ss')
        const end = Dayjs(Math.min(bucket.time + trend.intervalSeconds * 1000 - 1000, trend.endCreateTime * 1000)).format('HH:mm:ss')
        const unitSuffix = unit ? ` ${unit}` : ''

        return `
          <div style="font-size: 12px; line-height: 1.5; min-width: 175px;">
            <div style="color: ${subTextColor}; font-family: monospace; border-bottom: 1px solid ${dividerColor}; padding-bottom: 4px; margin-bottom: 6px;">
              ${start} ~ ${end}
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 3px;">
              <span style="color: ${textColor}; display: flex; align-items: center; gap: 6px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #165dff;"></span>平均值
              </span>
              <span style="font-weight: 600; font-family: monospace; color: ${isDark ? '#f2f3f5' : '#1d2129'};">${formatTrendValue(bucket.avg)}${unitSuffix}</span>
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 3px;">
              <span style="color: ${textColor}; display: flex; align-items: center; gap: 6px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #ff7d00;"></span>最高值
              </span>
              <span style="font-weight: 600; font-family: monospace; color: ${isDark ? '#f2f3f5' : '#1d2129'};">${formatTrendValue(bucket.max)}${unitSuffix}</span>
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px;">
              <span style="color: ${textColor}; display: flex; align-items: center; gap: 6px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #00b42a;"></span>最低值
              </span>
              <span style="font-weight: 600; font-family: monospace; color: ${isDark ? '#f2f3f5' : '#1d2129'};">${formatTrendValue(bucket.min)}${unitSuffix}</span>
            </div>
            <div style="font-size: 11px; color: ${subTextColor}; display: flex; justify-content: space-between; border-top: 1px dashed ${dividerColor}; padding-top: 4px; margin-top: 4px;">
              <span>采样 ${bucket.count}</span>
              <span>有效 ${bucket.numericCount}</span>
            </div>
          </div>
        `
      }
    },
    xAxis: {
      type: 'time',
      min: trend.beginCreateTime * 1000,
      max: trend.endCreateTime * 1000,
      boundaryGap: [0, 0],
      axisLabel: {
        hideOverlap: true,
        color: subTextColor,
        fontSize: 10,
        fontFamily: 'monospace',
        margin: 8
      },
      axisLine: { lineStyle: { color: axisLineColor } },
      axisTick: { show: false },
      splitLine: {
        show: true,
        lineStyle: { type: 'dashed', color: splitLineColor }
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      name: unit ? `${unit}` : undefined,
      nameTextStyle: {
        color: subTextColor,
        fontSize: 10,
        align: 'left',
        padding: [0, 0, 4, -6]
      },
      axisLabel: {
        color: subTextColor,
        fontSize: 10,
        fontFamily: 'monospace'
      },
      splitLine: {
        lineStyle: { type: 'dashed', color: splitLineColor }
      }
    },
    dataZoom: [
      { type: 'inside', filterMode: 'none' },
      {
        type: 'slider',
        filterMode: 'none',
        height: 16,
        bottom: 2,
        throttle: 80,
        borderColor: 'transparent',
        backgroundColor: isDark ? 'rgba(255, 255, 255, 0.02)' : 'rgba(0, 0, 0, 0.02)',
        fillerColor: isDark ? 'rgba(60, 126, 255, 0.22)' : 'rgba(22, 93, 255, 0.12)',
        dataBackground: {
          lineStyle: { opacity: 0 },
          areaStyle: { opacity: 0 }
        },
        selectedDataBackground: {
          lineStyle: { opacity: 0 },
          areaStyle: { opacity: 0 }
        },
        handleSize: '100%',
        handleStyle: {
          color: isDark ? '#4e5969' : '#c9cdd4',
          borderColor: 'transparent'
        },
        textStyle: {
          color: subTextColor,
          fontSize: 9,
          fontFamily: 'monospace'
        }
      }
    ],
    series: [
      {
        name: '平均值',
        type: 'line',
        smooth: 0.1,
        connectNulls: false,
        showSymbol: buckets.length < 30,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { width: 1.8, color: '#165dff' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: isDark ? 'rgba(22, 93, 255, 0.28)' : 'rgba(22, 93, 255, 0.14)' },
              { offset: 1, color: isDark ? 'rgba(22, 93, 255, 0.00)' : 'rgba(22, 93, 255, 0.00)' }
            ]
          }
        },
        markLine,
        data: buckets.map((bucket) => [bucket.time, bucket.avg])
      },
      {
        name: '最高值',
        type: 'line',
        smooth: 0.1,
        connectNulls: false,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 3,
        lineStyle: { width: 1, type: [3, 3], color: '#ff7d00' },
        data: buckets.map((bucket) => [bucket.time, bucket.max])
      },
      {
        name: '最低值',
        type: 'line',
        smooth: 0.1,
        connectNulls: false,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 3,
        lineStyle: { width: 1, type: [3, 3], color: '#00b42a' },
        data: buckets.map((bucket) => [bucket.time, bucket.min])
      }
    ]
  }
}

export function formatInterval(seconds?: number) {
  if (!seconds || seconds <= 0) return '--'
  if (seconds < 60) return `${seconds}s`
  if (seconds < 3600) return `${Math.round(seconds / 60)}m`
  if (seconds < 86400) return `${(seconds / 3600).toFixed(1).replace(TRAILING_ZERO_REGEX, '')}h`
  return `${(seconds / 86400).toFixed(1).replace(TRAILING_ZERO_REGEX, '')}d`
}

export function formatTrendValue(value: number | null | undefined) {
  return value == null || !Number.isFinite(Number(value)) ? '--' : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

export function historyStatistics(trend?: PropertyTrend) {
  const buckets = trend?.buckets.filter((bucket) => bucket.numericCount > 0) || []
  let count = 0
  let avg = 0
  for (const bucket of buckets) {
    const nextCount = count + bucket.numericCount
    avg = avg * (count / nextCount) + Number(bucket.avg) * (bucket.numericCount / nextCount)
    count = nextCount
  }
  return {
    min: buckets.length ? Math.min(...buckets.map((bucket) => Number(bucket.min))) : null,
    max: buckets.length ? Math.max(...buckets.map((bucket) => Number(bucket.max))) : null,
    avg: count ? avg : null
  }
}
