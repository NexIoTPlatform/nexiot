<template>
  <section class="device-compare">
    <div class="device-compare__selection">
      <div class="device-compare__intro">
        <div><h3>选择同款设备</h3><p>{{ device.productName || device.productKey }} · {{ field.name || field.id }}<span>已选 {{ selected.length }} / {{ MAX_COMPARE_DEVICES }} 台</span></p></div>
        <div class="device-compare__picker">
          <a-select :model-value="pickerValue" :options="deviceOptions" :loading="devicesLoading" :disabled="selected.length >= MAX_COMPARE_DEVICES" allow-search :filter-option="false" placeholder="搜索设备名称并添加" aria-label="添加同款设备" @search="searchDevices" @change="addDevice" @popup-visible-change="openPicker">
            <template #empty>{{ devicesError || '没有可添加的同款设备' }}</template>
            <template v-if="hasMoreDevices || devicesError" #footer>
              <a-button long type="text" :loading="devicesLoading" @click="loadDevices(!devicesError)">{{ devicesError ? '重新加载设备' : '加载更多设备' }}</a-button>
            </template>
          </a-select>
        </div>
      </div>
      <div class="device-compare__chips">
        <a-tag v-for="(item, index) in selected" :key="item.id" class="device-compare__chip" size="large" :closable="index !== 0" @close="remove(item.id)">
          <span class="device-compare__chip-name" :title="`${item.deviceName || item.deviceId} · ${item.deviceId}`">{{ item.deviceName || item.deviceId }}</span>
          <span v-if="index === 0" class="device-compare__current">当前设备</span><span v-else class="device-compare__code">{{ item.deviceId }}</span>
        </a-tag>
      </div>
    </div>

    <div class="device-compare__analysis">
      <div class="device-compare__chart-heading">
        <h3>{{ field.name || '属性' }}对比 <span v-if="unit">/ {{ unit }}</span></h3>
        <a-space wrap>
          <a-radio-group v-model="layout" type="button" size="small" aria-label="图表布局"><a-radio value="overlay">同图对比</a-radio><a-radio value="split">分别查看</a-radio></a-radio-group>
        </a-space>
      </div>
      <div class="device-compare__query-controls">
        <a-space wrap>
          <span>趋势点数</span>
          <a-radio-group v-model="maxPoints" type="button" size="small" aria-label="对比趋势点数"><a-radio v-for="points in [100, 300, 600]" :key="points" :value="points">{{ points }}</a-radio></a-radio-group>
          <span>每台采样上限</span>
          <a-radio-group v-model="maxScanRows" type="button" size="small" aria-label="对比每台采样上限"><a-radio v-for="limit in [1000, 10000, 50000, 100000]" :key="limit" :value="limit">{{ limit >= 10000 ? `${limit / 10000}万` : `${limit / 1000}千` }}</a-radio></a-radio-group>
        </a-space>
      </div>
      <a-alert v-if="sources.some(source => source.trend.sampled || source.trend.countExact === false)" type="warning" class="device-compare__sample-note">部分设备已按时段采样，统计值和记录数仅代表已读取样本，可能遗漏短时尖峰。提高采样上限或缩短时间范围可查看更多细节。</a-alert>
      <a-spin :loading="loading" class="device-compare__chart">
        <PropertyTrendChart v-if="hasData" :sources="sources" :split="layout === 'split'" :show-range="layout === 'split'" @zoom="emit('zoom', $event)" />
        <a-empty v-else :description="loading ? '正在读取设备趋势…' : results.some(item => item.status === 'error') ? '趋势加载失败，请在下方重试' : '该时段暂无数值记录，试试更长的时间范围'" />
      </a-spin>
      <p class="device-compare__hint"><icon-info-circle />{{ selected.length === 1 ? '添加同款设备后即可对比。' : '' }}各设备按{{ sources[0] ? ` ${formatInterval(sources[0].trend.intervalSeconds)} ` : '相同时间间隔' }}对齐取均值，折线连接相邻采样；长时间无上报时断线。</p>
      <a-table :data="statistics" :pagination="false" :scroll="{ x: 720 }" row-key="key" size="small" class="device-compare__table">
        <template #columns>
          <a-table-column title="设备" :width="280">
            <template #cell="{ record }"><div class="device-compare__device"><i :style="{ background: TREND_COLORS[record.colorIndex] }" /><div><strong>{{ record.device.deviceName || record.device.deviceId }}</strong><small>{{ record.device.deviceId }}{{ String(record.device.id) === String(device.id) ? ' · 当前设备' : '' }}</small></div></div></template>
          </a-table-column>
          <a-table-column :title="`平均值${unit ? ` (${unit})` : ''}`" align="right"><template #cell="{ record }">{{ formatTrendValue(record.avg) }}</template></a-table-column>
          <a-table-column title="最小值" align="right"><template #cell="{ record }">{{ formatTrendValue(record.min) }}</template></a-table-column>
          <a-table-column title="最大值" align="right"><template #cell="{ record }">{{ formatTrendValue(record.max) }}</template></a-table-column>
          <a-table-column title="已读取有效记录" align="right"><template #cell="{ record }">{{ record.source?.trend.numericCount.toLocaleString() ?? '--' }}</template></a-table-column>
          <a-table-column title="状态" :width="170"><template #cell="{ record }"><a-tooltip v-if="record.status === 'error'" :content="record.error"><a-button type="text" size="mini" status="danger" :disabled="loading" @click="query">查询失败 · 重试</a-button></a-tooltip><span v-else>{{ record.status === 'loading' ? '加载中…' : record.source?.trend.sampled || record.source?.trend.countExact === false ? '采样统计' : record.source?.trend.numericCount ? '完整统计' : '该时段无数值' }}</span></template></a-table-column>
        </template>
      </a-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { HistoryRange, TrendZoomEvent } from '@/components/PropertyTrend/types'
import { TREND_COLORS } from '@/components/PropertyTrend/chart'
import { formatInterval, formatTrendValue } from '@/components/PropertyTrend/model'
import PropertyTrendChart from '@/components/PropertyTrend/PropertyTrendChart.vue'
import { getCompareDevices } from './propertyCompare.api'
import { canAddCompareDevice, MAX_COMPARE_DEVICES } from './propertyCompareModel'
import { usePropertyCompare } from './usePropertyCompare'

defineOptions({ name: 'DevicePropertyComparePanel' })
const props = defineProps<{ device: DeviceItem, field: ProductMetadataItem, range: HistoryRange, active: boolean }>()
const emit = defineEmits<{ (e: 'zoom', value: TrendZoomEvent): void }>()
const { selected, unit, results, sources, loading, maxPoints, maxScanRows, statistics, hasData, add, remove, query } = usePropertyCompare(props)
const layout = ref('overlay')
const pickerValue = ref<string>()
const devices = ref<DeviceItem[]>([])
const devicesLoading = ref(false)
const devicesError = ref('')
const keyword = ref('')
const page = ref(0)
const total = ref(0)
let searchTimer: ReturnType<typeof setTimeout> | undefined
let deviceRequest = 0
const hasMoreDevices = computed(() => page.value * 30 < total.value)
const deviceOptions = computed(() => devices.value.filter((item) => canAddCompareDevice(props.device, selected.value, item)).map((item) => ({ value: String(item.id), label: `${item.deviceName || item.deviceId} · ${item.deviceId}` })))

async function loadDevices(more = false) {
  const request = ++deviceRequest
  const nextPage = more ? page.value + 1 : 1
  devicesLoading.value = true
  devicesError.value = ''
  try {
    const data = await getCompareDevices(props.device.productKey, { page: nextPage, size: 30, deviceName: keyword.value.trim() || undefined })
    if (request !== deviceRequest) return
    devices.value = more ? [...devices.value, ...data.records] : data.records
    total.value = data.total
    page.value = nextPage
  } catch {
    if (request !== deviceRequest) return
    devicesError.value = '设备加载失败，请重试'
  } finally {
    if (request === deviceRequest) devicesLoading.value = false
  }
}

function searchDevices(value: string) {
  keyword.value = value
  clearTimeout(searchTimer)
  ++deviceRequest
  devices.value = []
  page.value = 0
  total.value = 0
  searchTimer = setTimeout(loadDevices, 250)
}

function openPicker(visible: boolean) {
  if (visible && !page.value && !devicesLoading.value) loadDevices()
}

async function addDevice(value: unknown) {
  const item = devices.value.find((device) => String(device.id) === String(value))
  if (!item) return
  pickerValue.value = String(value)
  add(item)
  await nextTick()
  pickerValue.value = undefined
}

onScopeDispose(() => {
  ++deviceRequest
  clearTimeout(searchTimer)
})
</script>

<style lang="scss" scoped src="./DevicePropertyComparePanel.scss"></style>
