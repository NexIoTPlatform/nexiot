<template>
  <a-modal
    :visible="visible"
    width="min(1100px, 95vw)"
    :footer="false"
    :body-style="{ maxHeight: 'calc(100vh - 84px)', overflow: 'auto', padding: '14px 20px 16px' }"
    unmount-on-close
    class="device-history-modal"
    @cancel="emit('update:visible', false)"
  >
    <template #title>
      <div class="device-history-header">
        <div class="device-history-header__main">
          <span class="device-history-header__title">{{ field?.name || field?.id || '设备数据' }}</span>
          <span v-if="field?.id" class="device-history-header__code">{{ field.id }}</span>
          <a-tag size="small" :color="props.messageType === 'EVENT' ? 'purple' : 'arcoblue'" bordered>
            {{ props.messageType === 'EVENT' ? '事件' : '属性' }}
          </a-tag>
          <span v-if="fieldType(field)" class="device-history-header__type">{{ fieldType(field) }}</span>
        </div>
        <div v-if="deviceName" class="device-history-header__device">
          <icon-desktop />
          <span>{{ deviceName }}</span>
        </div>
      </div>
    </template>

    <div class="device-history">
      <a-tabs v-if="numeric" :active-key="activeView" class="device-history__tabs" @change="handleViewChange">
        <a-tab-pane key="table" title="明细表" />
        <a-tab-pane key="chart" title="趋势图" />
        <a-tab-pane key="compare" title="属性对比" />
      </a-tabs>
      <!-- 统一全局操作控制栏 -->
      <div class="device-history__control-bar">
        <div class="control-left">
          <a-radio-group :model-value="rangeKey" type="button" size="small" @change="handleRangeChange">
            <a-radio value="1h">1h</a-radio>
            <a-radio value="24h">24h</a-radio>
            <a-radio value="7d">7d</a-radio>
            <a-radio value="15d">15d</a-radio>
          </a-radio-group>

          <div class="device-history__picker">
            <a-range-picker
              :model-value="customRange"
              show-time
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              size="small"
              @change="loadCustom"
            />
          </div>
        </div>

        <div class="control-right">
          <a-button size="small" :loading="loading || trendLoading" aria-label="刷新" @click="refresh">
            <template #icon><icon-refresh /></template>
          </a-button>
          <a-button v-if="activeView !== 'compare'" size="small" :loading="exporting" @click="exportData">
            <template #icon><icon-download /></template>导出
          </a-button>
        </div>
      </div>

      <!-- 时段与选区上下文栏 -->
      <div class="device-history__status-bar">
        <div class="status-left">
          <span class="status-label">查询范围:</span>
          <span class="status-value font-mono">{{ rangeLabel }}</span>
          <a-tag v-if="hasZoom && activeView !== 'table'" size="small" color="orange" bordered>选区缩放生效中</a-tag>
        </div>
        <div v-if="activeView !== 'table'" class="status-right">
          <a-button v-if="hasZoom" size="mini" type="text" status="warning" @click="resetZoom">
            <template #icon><icon-undo /></template>重置缩放
          </a-button>
          <a-button v-if="zoomSelection" size="mini" type="primary" status="success" @click="applyZoom">
            聚焦选中区间
          </a-button>
        </div>
      </div>

      <!-- 趋势图面板 -->
      <div v-show="activeView === 'chart' && numeric" class="device-history__chart-section">
        <div class="device-history__query-controls">
          <div class="device-history__query-control">
            <span>趋势点数</span>
            <a-radio-group :model-value="trendMaxPoints" type="button" size="small" aria-label="趋势点数" @change="handleTrendMaxPointsChange">
              <a-radio v-for="points in [100, 300, 600]" :key="points" :value="points">{{ points }}</a-radio>
            </a-radio-group>
          </div>
          <div class="device-history__query-control">
            <span>采样上限</span>
            <a-radio-group :model-value="trendMaxScanRows" type="button" size="small" aria-label="趋势采样上限" @change="handleTrendMaxScanRowsChange">
              <a-radio v-for="limit in [1000, 10000, 50000, 100000]" :key="limit" :value="limit">{{ limit >= 10000 ? `${limit / 10000}万` : `${limit / 1000}千` }}</a-radio>
            </a-radio-group>
          </div>
          <span class="device-history__query-hint">提高点数或采样上限后重新查询</span>
        </div>
        <a-spin :loading="trendLoading" class="device-history__spin">
          <a-alert v-if="trendError" type="error" class="device-history__alert">
            {{ trendError }}<a-button type="text" size="small" @click="loadTrend">重试</a-button>
          </a-alert>
          <template v-else-if="trend">
            <!-- 紧凑工业统计条 -->
            <div class="device-history__metric-strip">
              <div class="metric-item">
                <span class="metric-label">{{ sampledTrend ? '采样记录' : '记录数' }}</span>
                <span class="metric-value font-mono">{{ trend.totalCount.toLocaleString() }}</span>
                <span class="metric-sub">(有效 {{ trend.numericCount.toLocaleString() }})</span>
              </div>
              <span class="metric-sep" />
              <div class="metric-item">
                <span class="metric-dot dot-avg" />
                <span class="metric-label">平均 AVG</span>
                <span class="metric-value font-mono">{{ formatTrendValue(statistics.avg) }}</span>
                <span v-if="fieldUnit" class="metric-unit">{{ fieldUnit }}</span>
              </div>
              <span class="metric-sep" />
              <div class="metric-item">
                <span class="metric-dot dot-max" />
                <span class="metric-label">最高 MAX</span>
                <span class="metric-value font-mono">{{ formatTrendValue(statistics.max) }}</span>
                <span v-if="fieldUnit" class="metric-unit">{{ fieldUnit }}</span>
              </div>
              <span class="metric-sep" />
              <div class="metric-item">
                <span class="metric-dot dot-min" />
                <span class="metric-label">最低 MIN</span>
                <span class="metric-value font-mono">{{ formatTrendValue(statistics.min) }}</span>
                <span v-if="fieldUnit" class="metric-unit">{{ fieldUnit }}</span>
              </div>
              <span class="metric-sep" />
              <div class="metric-item">
                <span class="metric-label">步长</span>
                <span class="metric-value font-mono">{{ formatInterval(trend.intervalSeconds) }}</span>
              </div>
            </div>

            <div class="device-history__chart-controls">
              <a-checkbox v-model="showRange">最小—最大范围</a-checkbox>
              <a-tag size="small" :color="sampledTrend ? 'orange' : 'green'">{{ sampledTrend ? '采样统计' : '完整统计' }}</a-tag>
              <span>{{ sampledTrend ? '采样均值' : '时段均值' }} · 每 {{ formatInterval(trend.intervalSeconds) }} · {{ trend.buckets.length }} 个时段</span>
            </div>
            <a-alert v-if="trendNote" type="warning" class="device-history__alert">{{ trendNote }}</a-alert>
            <PropertyTrendChart v-if="trend.numericCount" :sources="chartSources" :show-range="showRange" @zoom="handleZoom" />
            <a-empty v-else class="device-history__empty" :description="trend.totalCount ? '已读取记录中没有有效数值' : sampledTrend ? '当前采样窗口暂无记录，可提高上限或缩短范围' : '所选时段暂无记录'" />
            <p class="device-history__chart-hint">折线连接{{ sampledTrend ? '采样' : '时段' }}均值，范围表示{{ sampledTrend ? '采样' : '时段' }}最小值到最大值；长时间无上报时断线。拖动底部滑块，再点击“聚焦选中区间”查看细节。</p>
          </template>
          <div v-else class="device-history__chart-wrap" />
        </a-spin>
      </div>

      <DevicePropertyComparePanel
        v-if="visible && numeric && field && compareVisited && queryRange"
        v-show="activeView === 'compare'"
        :key="`${device.id}-${field.id}`"
        :device="device"
        :field="field"
        :range="queryRange"
        :active="activeView === 'compare'"
        @zoom="handleZoom"
      />

      <!-- 数据记录表格 -->
      <div v-show="activeView === 'table' || !numeric" class="device-history__table-section">
        <div class="device-history__query-controls">
          <div class="device-history__query-control">
            <span>每页记录</span>
            <a-radio-group :model-value="pagination.pageSize" type="button" size="small" aria-label="明细每页记录数" @change="handlePageSizeChange">
              <a-radio v-for="size in [10, 50, 100, 200]" :key="size" :value="size">{{ size }} 条</a-radio>
            </a-radio-group>
          </div>
          <span class="device-history__query-hint">原始记录按时间倒序 · 逐页读取</span>
        </div>
        <a-alert v-if="tableError" type="error" class="device-history__alert">
          {{ tableError }}<a-button type="text" size="small" @click="loadTable">重试</a-button>
        </a-alert>

        <a-table
          :data="rows"
          :columns="columns"
          :loading="loading"
          :pagination="false"
          size="small"
          :bordered="{ cell: true, headerCell: true }"
          row-key="historyKey"
          :scroll="{ y: tableScrollY }"
          class="device-history__table"
        />

        <!-- 紧凑分页栏 -->
        <div class="device-history__pagination">
          <div class="pagination-info">
            <span>第 {{ pagination.current }} 页 · 本页 {{ rows.length }} 条</span>
            <span v-if="!hasMore && !loading && !tableError && rows.length" class="pagination-end">（已至末页）</span>
          </div>
          <div class="pagination-controls">
            <a-button-group size="small">
              <a-button :disabled="loading || pagination.current === 1" @click="handlePageChange(pagination.current - 1)">
                <template #icon><icon-left /></template>上一页
              </a-button>
              <a-button :disabled="loading || !hasMore" @click="handlePageChange(pagination.current + 1)">
                下一页<template #icon><icon-right /></template>
              </a-button>
            </a-button-group>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem, DeviceLogItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { useClipboard, useWindowSize } from '@vueuse/core'
import Dayjs from 'dayjs'
import { fieldType, formatInterval, formatTrendValue, historyStatistics, isNumericField, parseRecord } from '@/components/PropertyTrend/model'
import PropertyTrendChart from '@/components/PropertyTrend/PropertyTrendChart.vue'
import DevicePropertyComparePanel from './DevicePropertyComparePanel.vue'
import { useDeviceHistory } from './useDeviceHistory'

defineOptions({ name: 'DeviceHistoryModal' })

const props = defineProps<{
  visible: boolean
  device: DeviceItem
  field?: ProductMetadataItem
  messageType: 'PROPERTIES' | 'EVENT'
}>()
const emit = defineEmits<{ (e: 'update:visible', value: boolean): void }>()

const showRange = ref(true)
const numeric = computed(() => props.messageType === 'PROPERTIES' && isNumericField(props.field))
const { loading, trendLoading, exporting, rows, trend, trendNote, queryRange, tableError, trendError, rangeKey, customRange, activeView, trendMaxPoints, trendMaxScanRows, pagination, hasMore, rangeLabel, zoomSelection, hasZoom, refresh, loadTable, loadTrend, loadCustom, handleViewChange, handleRangeChange, handlePageChange, handlePageSizeChange, handleTrendMaxPointsChange, handleTrendMaxScanRowsChange, handleZoom, applyZoom, resetZoom, exportData } = useDeviceHistory(props, numeric)
const compareVisited = ref(false)
watch(activeView, (view) => {
  if (view === 'compare') compareVisited.value = true
})
watch(() => [props.visible, props.field?.id], () => {
  compareVisited.value = false
  showRange.value = true
})
const { width: viewportWidth, height: viewportHeight } = useWindowSize()
const tableScrollY = computed(() => Math.min(380, Math.max(200, viewportHeight.value - 410)))
const compactLayout = computed(() => viewportWidth.value <= 720)
const { copy } = useClipboard()

const deviceName = computed(() => props.device.name || props.device.deviceName || props.device.deviceId)
const fieldUnit = computed(() => String(parseRecord(props.field?.valueType).unit || ''))
const sampledTrend = computed(() => trend.value?.sampled || trend.value?.countExact === false)
const chartSources = computed(() => trend.value ? [{ key: props.field?.id || 'value', label: String(props.field?.name || props.field?.id || '属性'), unit: fieldUnit.value, trend: trend.value }] : [])

const statistics = computed(() => historyStatistics(trend.value))

function copyRawValue(val: string) {
  if (!val || val === '--') return
  copy(val)
  Message.success('已复制到剪贴板')
}

const columns = computed<TableColumnData[]>(() => {
  const items: TableColumnData[] = [
    {
      title: '记录时间',
      width: compactLayout.value ? 148 : 178,
      render: ({ record }) => (
        <span class="device-history__cell-time">{formatTime(record.createTime || record.eventTime)}</span>
      )
    },
    {
      title: props.messageType === 'EVENT' ? '事件内容' : '解析值',
      width: compactLayout.value ? 160 : undefined,
      render: ({ record }) => (
        <div class="device-history__cell-val-wrap">
          <span class="cell-val">{displayValue(record as DeviceLogItem)}</span>
          {displayUnit(record as DeviceLogItem) && <span class="cell-unit">{displayUnit(record as DeviceLogItem)}</span>}
        </div>
      )
    }
  ]
  if (!compactLayout.value) {
    items.push({
      title: '原始报文 / 原始值',
      width: 260,
      render: ({ record }) => {
        const val = rawValue(record as DeviceLogItem)
        return (
          <code
            class="device-history__cell-raw"
            title="点击复制原始值"
            onClick={() => copyRawValue(val)}
          >
            {val}
          </code>
        )
      }
    })
  }
  return items
})

function hasValue(value: unknown) {
  return value !== undefined && value !== null && value !== ''
}

function formatRawValue(value: unknown) {
  if (!hasValue(value)) return '--'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function rawValue(row: DeviceLogItem) {
  return formatRawValue(row.value ?? row.content)
}

function displayValue(row: DeviceLogItem) {
  if (hasValue(row.ext2)) return formatRawValue(row.ext2)
  const raw = row.value ?? row.content
  if (!hasValue(raw)) return '--'
  if (fieldType(props.field) === 'boolean') return ['true', '1'].includes(String(raw).toLowerCase()) ? '是' : '否'
  if (fieldType(props.field) === 'enum') {
    const elements = parseRecord(props.field?.valueType).elements
    if (Array.isArray(elements)) {
      const matched = elements.map(parseRecord).find((item) => String(item.value) === String(raw))
      if (matched) return String(matched.text || matched.label || raw)
    }
  }
  return formatRawValue(raw)
}

function displayUnit(row: DeviceLogItem) {
  const unit = String(row.ext3 || fieldUnit.value || '')
  return unit && hasValue(row.ext2) && String(row.ext2).trim().endsWith(unit) ? '' : unit
}

function formatTime(value?: string) {
  const time = Dayjs(value)
  return time.isValid() ? time.format('YYYY-MM-DD HH:mm:ss') : value || '-'
}
</script>

<style lang="scss" scoped src="./DeviceHistoryModal.scss"></style>
