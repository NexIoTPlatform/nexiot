<template>
  <a-modal
    :visible="visible"
    :title="`${field?.name || field?.id || '设备位置'}轨迹`"
    width="min(1180px, 96vw)"
    :footer="false"
    unmount-on-close
    @cancel="emit('update:visible', false)"
  >
    <div class="device-track">
      <aside class="device-track__side">
        <a-radio-group :model-value="rangeKey" type="button" @change="handleRangeChange">
          <a-radio value="6h">6 小时</a-radio>
          <a-radio value="24h">24 小时</a-radio>
          <a-radio value="7d">7 天</a-radio>
          <a-radio value="15d">15 天</a-radio>
        </a-radio-group>
        <a-range-picker :model-value="customRange" show-time value-format="YYYY-MM-DD HH:mm:ss" @change="loadCustom" />

        <div class="device-track__stats">
          <article><span>轨迹点</span><strong>{{ points.length }}</strong></article>
          <article><span>总里程</span><strong>{{ distanceText }}</strong></article>
        </div>

        <a-alert v-if="queryError" type="error">{{ queryError }}</a-alert>
        <a-alert v-else-if="loading">已读取 {{ loadedCount.toLocaleString() }} 条定位记录，正在加载完整轨迹…</a-alert>
        <a-alert v-else-if="invalidCount" type="warning">{{ invalidCount.toLocaleString() }} 条记录没有有效坐标或时间，未绘入地图。</a-alert>
        <div class="device-track__timeline-head"><strong>轨迹详情</strong><a-button type="text" size="mini" :loading="loading" @click="loadTrack"><template #icon><icon-refresh /></template>刷新</a-button></div>
        <div class="device-track__timeline">
          <button v-for="{ point, index } in timelinePoints" :key="index" type="button" :class="{ active: activePoint === index }" @click="focusPoint(index)">
            <i :class="{ start: index === 0, end: index === points.length - 1 }" />
            <span><strong>{{ index === 0 ? '起点' : index === points.length - 1 ? '终点' : `轨迹点 ${index + 1}` }}</strong><small>{{ formatTime(point.time) }}</small><code>{{ point.lng.toFixed(6) }}, {{ point.lat.toFixed(6) }}</code></span>
          </button>
          <a-empty v-if="!loading && !queryError && !points.length" description="当前时间范围暂无有效轨迹数据" />
        </div>
        <a-pagination v-if="points.length > TIMELINE_PAGE_SIZE" v-model:current="timelinePage" :page-size="TIMELINE_PAGE_SIZE" :total="points.length" simple size="small" />
      </aside>

      <main class="device-track__map-wrap">
        <a-spin :loading="loading" class="device-track__map-spin">
          <div ref="mapContainer" class="device-track__map" />
          <a-result v-if="mapError" status="warning" title="地图暂时无法加载" :subtitle="mapError" />
        </a-spin>
        <div v-if="points.length" class="device-track__playback">
          <a-button type="primary" shape="circle" :aria-label="playing ? '暂停轨迹' : '播放轨迹'" @click="togglePlayback">
            <template #icon><icon-pause v-if="playing" /><icon-play-arrow v-else /></template>
          </a-button>
          <a-button shape="circle" aria-label="重播轨迹" @click="resetPlayback"><template #icon><icon-refresh /></template></a-button>
          <a-slider :model-value="activePoint" :min="0" :max="Math.max(0, points.length - 1)" :show-tooltip="false" @change="seekPlayback" />
          <span>{{ activePoint + 1 }} / {{ points.length }}</span>
        </div>
      </main>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import type { TrackPoint } from './deviceTrack'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import AMapLoader from '@amap/amap-jsapi-loader'
import Dayjs from 'dayjs'
import { amapConfig, applyAMapSecurityConfig } from '@/config/amap'
import { getDeviceMetadataSlice } from './deviceHistory.api'
import { loadTrackRecords, TRACK_PAGE_SIZE, trackPoints, trackTimestamp } from './deviceTrack'

defineOptions({ name: 'DeviceTrackModal' })

const props = defineProps<{ visible: boolean, device: DeviceItem, field?: ProductMetadataItem }>()
const emit = defineEmits<{ (e: 'update:visible', value: boolean): void }>()

const TIMELINE_PAGE_SIZE = 100
const loading = ref(false)
const loadedCount = ref(0)
const invalidCount = ref(0)
const queryError = ref('')
const rangeKey = ref('24h')
const customRange = ref<string[]>([])
const points = ref<TrackPoint[]>([])
const activePoint = ref(0)
const timelinePage = ref(1)
const mapContainer = ref<HTMLElement>()
const mapError = ref('')
const playing = ref(false)
let AMap: any
let map: any
let overlays: any[] = []
let movingMarker: any
let playbackTimer: ReturnType<typeof setInterval> | undefined
let requestController: AbortController | undefined

const timelinePoints = computed(() => {
  const start = (timelinePage.value - 1) * TIMELINE_PAGE_SIZE
  return points.value.slice(start, start + TIMELINE_PAGE_SIZE).map((point, offset) => ({ point, index: start + offset }))
})

const distanceText = computed(() => {
  let distance = 0
  for (let index = 1; index < points.value.length; index += 1) distance += haversine(points.value[index - 1], points.value[index])
  return distance < 1 ? `${Math.round(distance * 1000)} m` : `${distance.toFixed(2)} km`
})

function timeParams() {
  if (customRange.value.length === 2) return { beginCreateTime: Dayjs(customRange.value[0]).unix(), endCreateTime: Dayjs(customRange.value[1]).unix() }
  const end = Dayjs()
  const start = rangeKey.value === '6h' ? end.subtract(6, 'hour') : rangeKey.value === '24h' ? end.subtract(1, 'day') : rangeKey.value === '7d' ? end.subtract(7, 'day') : end.subtract(15, 'day')
  return { beginCreateTime: start.unix(), endCreateTime: end.unix() }
}

async function ensureMap(controller: AbortController) {
  if (map || !mapContainer.value) return
  try {
    mapError.value = ''
    if (!amapConfig.key) throw new Error('未配置高德地图 Key')
    applyAMapSecurityConfig()
    AMap = await AMapLoader.load({ key: amapConfig.key, version: '2.0', plugins: ['AMap.Scale'] })
    if (controller.signal.aborted || !props.visible || !mapContainer.value) return
    map = new AMap.Map(mapContainer.value, { zoom: 5, center: [105.602725, 37.076636], resizeEnable: true })
    map.addControl(new AMap.Scale())
  } catch (error) {
    if (!controller.signal.aborted) mapError.value = error instanceof Error ? error.message : '请检查地图密钥或网络连接'
  }
}

async function loadTrack() {
  if (!props.visible || !props.field) return
  requestController?.abort()
  const controller = new AbortController()
  requestController = controller
  loading.value = true
  loadedCount.value = 0
  invalidCount.value = 0
  queryError.value = ''
  mapError.value = ''
  points.value = []
  activePoint.value = 0
  timelinePage.value = 1
  stopPlayback()
  clearTrack()
  const time = timeParams()
  const params = {
    productKey: props.device.productKey,
    iotId: props.device.iotId,
    deviceId: props.device.deviceId,
    messageType: 'PROPERTIES',
    property: props.field.id,
    ...time,
    pageSize: TRACK_PAGE_SIZE
  }
  const timeout = setTimeout(() => {
    if (requestController !== controller || controller.signal.aborted) return
    queryError.value = '轨迹加载超时，请缩小时间范围后重试'
    controller.abort()
    loading.value = false
    points.value = []
    clearTrack()
  }, 60000)
  try {
    if (!Number.isFinite(time.beginCreateTime) || !Number.isFinite(time.endCreateTime) || time.beginCreateTime > time.endCreateTime || time.endCreateTime - time.beginCreateTime > 31 * 86400) throw new Error('请选择不超过31天的有效时间范围')
    const rows = await loadTrackRecords(
      (cursor) => getDeviceMetadataSlice({ ...params, cursor }, controller.signal).then((response) => response.data),
      controller.signal,
      (count) => { loadedCount.value = count }
    )
    if (controller.signal.aborted) return
    points.value = trackPoints(rows)
    invalidCount.value = rows.length - points.value.length
    await nextTick()
    await ensureMap(controller)
    if (controller.signal.aborted) return
    drawTrack()
  } catch (error) {
    if (!controller.signal.aborted) queryError.value = error instanceof Error ? error.message : '轨迹数据加载失败，请刷新后重试'
  } finally {
    clearTimeout(timeout)
    if (requestController === controller) {
      requestController = undefined
      loading.value = false
    }
  }
}

function drawTrack() {
  if (!map || !AMap) return
  clearTrack()
  if (!points.value.length) return
  const path = points.value.map((point) => [point.lng, point.lat])
  const polyline = new AMap.Polyline({ path, strokeColor: '#165dff', strokeWeight: 5, strokeOpacity: 0.82, showDir: true, lineJoin: 'round' })
  const start = new AMap.Marker({ position: path[0], title: '起点', label: { content: '起点', direction: 'top' } })
  const end = new AMap.Marker({ position: path.slice(-1)[0], title: '终点', label: { content: '终点', direction: 'top' } })
  overlays = [polyline, start, end]
  map.add(overlays)
  map.setFitView(overlays, false, [50, 50, 50, 50])
  updateMovingMarker(0)
}

function clearTrack() {
  if (map) map.remove([...overlays, ...(movingMarker ? [movingMarker] : [])])
  overlays = []
  movingMarker = undefined
}

function destroyMap() {
  clearTrack()
  map?.destroy()
  map = undefined
}

function focusPoint(index: number) {
  activePoint.value = index
  const point = points.value[index]
  if (map && point) {
    map.setZoomAndCenter(16, [point.lng, point.lat])
    updateMovingMarker(index)
  }
}

function updateMovingMarker(index: number) {
  const point = points.value[index]
  if (!map || !AMap || !point) return
  if (!movingMarker) {
    movingMarker = new AMap.Marker({ position: [point.lng, point.lat], title: '轨迹回放', label: { content: '回放', direction: 'top' }, zIndex: 220 })
    map.add(movingMarker)
  } else {
    movingMarker.setPosition([point.lng, point.lat])
  }
}

function togglePlayback() {
  if (playing.value) {
    stopPlayback()
    return
  }
  if (activePoint.value >= points.value.length - 1) activePoint.value = 0
  playing.value = true
  playbackTimer = setInterval(() => {
    const next = activePoint.value + 1
    if (next >= points.value.length) {
      stopPlayback()
      return
    }
    activePoint.value = next
    updateMovingMarker(next)
  }, 140)
}

function stopPlayback() {
  playing.value = false
  if (playbackTimer) clearInterval(playbackTimer)
  playbackTimer = undefined
}

function resetPlayback() {
  stopPlayback()
  activePoint.value = 0
  updateMovingMarker(0)
  if (map && points.value[0]) map.setZoomAndCenter(15, [points.value[0].lng, points.value[0].lat])
}

function seekPlayback(value: number | [number, number]) {
  stopPlayback()
  activePoint.value = Array.isArray(value) ? Number(value[0]) : Number(value)
  updateMovingMarker(activePoint.value)
}

function handleRangeChange(value: string | number | boolean) {
  rangeKey.value = String(value)
  customRange.value = []
  loadTrack()
}

function loadCustom(value: (string | number | Date | undefined)[] | undefined) {
  const range = (value || []).filter((item) => item !== undefined).map(String)
  if (range.length !== 0 && range.length !== 2) return
  customRange.value = range
  if (range.length === 2) rangeKey.value = ''
  else if (!rangeKey.value) rangeKey.value = '24h'
  loadTrack()
}

function haversine(start: TrackPoint, end: TrackPoint) {
  const rad = Math.PI / 180
  const lat1 = start.lat * rad
  const lat2 = end.lat * rad
  const deltaLat = (end.lat - start.lat) * rad
  const deltaLng = (end.lng - start.lng) * rad
  const value = Math.sin(deltaLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) ** 2
  return 6371 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value))
}

function formatTime(value: string) {
  const time = Dayjs(trackTimestamp(value))
  return time.isValid() ? time.format('MM-DD HH:mm:ss') : value || '-'
}

watch(() => [props.visible, props.field?.id, props.device.iotId, props.device.productKey] as const, async ([visible]) => {
  requestController?.abort()
  stopPlayback()
  if (!visible) {
    destroyMap()
    return
  }
  customRange.value = []
  rangeKey.value = '24h'
  await nextTick()
  if (props.visible) loadTrack()
}, { immediate: true })
watch(activePoint, (index) => {
  timelinePage.value = Math.floor(index / TIMELINE_PAGE_SIZE) + 1
})
onBeforeUnmount(() => {
  requestController?.abort()
  stopPlayback()
  destroyMap()
})
</script>

<style lang="scss" scoped src="./DeviceTrackModal.scss"></style>

<style lang="scss" scoped>
.device-track__map-spin {
  :deep(.arco-spin-child) {
    width: 100%;
    height: 100%;
    min-height: 0;
  }

  :deep(.arco-result) {
    position: absolute;
    inset: 0;
    background: var(--color-bg-2);
  }
}
</style>
