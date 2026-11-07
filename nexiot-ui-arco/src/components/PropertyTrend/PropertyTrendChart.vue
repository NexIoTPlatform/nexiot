<template>
  <div class="property-trend" :style="{ height: `${height}px` }" role="img" :aria-label="description">
    <VCharts :key="chartKey" :option="option" :update-options="{ notMerge: true }" autoresize @datazoom="emit('zoom', $event)" />
  </div>
</template>

<script setup lang="ts">
import type { TrendSource, TrendZoomEvent } from './types'
import { useWindowSize } from '@vueuse/core'
import { LineChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import VCharts from 'vue-echarts'
import { useTheme } from '@/hooks'
import { buildTrendOption } from './chart'

defineOptions({ name: 'PropertyTrendChart' })
const props = withDefaults(defineProps<{ sources: TrendSource[], split?: boolean, showRange?: boolean }>(), { split: false, showRange: true })
const emit = defineEmits<{ (e: 'zoom', value: TrendZoomEvent): void }>()

// 所有使用方共用按需注册；渲染器只能从 echarts/renderers 导入。
use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, TitleComponent])
const { isDark } = useTheme()
const { width } = useWindowSize()
const compact = computed(() => width.value < 720)
const height = computed(() => props.split ? Math.max(1, props.sources.length) * 230 + 60 : (props.sources.length > 1 ? (compact.value ? 390 : 360) : 340))
const chartKey = computed(() => `${props.split}-${props.sources.map((source) => `${source.key}:${source.trend.beginCreateTime}:${source.trend.endCreateTime}`).join('|')}`)
const description = computed(() => `${props.sources.map((source) => source.label).join('、')}历史折线图，相邻采样点连线，长时间无上报时断线，可拖动底部滑块选择时间范围`)
const option = computed(() => buildTrendOption(props.sources, { dark: isDark.value, compact: compact.value, split: props.split, showRange: props.showRange }))
</script>

<style lang="scss" scoped>
.property-trend {
  width: 100%;
  min-width: 0;
}
</style>
