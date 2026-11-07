<script setup lang="ts">
import type {
  DashboardDeviceOption,
  DashboardMapBounds,
  DashboardMapDevice,
  DashboardMapSummary,
  DashboardOverview,
  DashboardProductOption,
  DashboardStateFilter
} from '../types'
import { Message } from '@arco-design/web-vue'
import { useDebounceFn } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useWebSocketPushStore } from '@/stores'
import {
  getDashboardDeviceOptions,
  getDashboardMapDevices,
  getDashboardOverview,
  getDashboardProducts
} from '../api'
import { hasDeviceLocation } from '../types'
import DashboardToolbar from './DashboardToolbar.vue'
import DeviceInspector from './DeviceInspector.vue'
import DeviceMap from './DeviceMap.vue'
import OverviewPanel from './OverviewPanel.vue'
import RealtimeEventsDrawer from './RealtimeEventsDrawer.vue'

defineOptions({ name: 'HomeMapDashboard' })

const router = useRouter()
const pushStore = useWebSocketPushStore()
const { connected, events, unreadCount } = storeToRefs(pushStore)
const mapRef = useTemplateRef<InstanceType<typeof DeviceMap>>('mapRef')

const overview = ref<DashboardOverview>({})
const summary = ref<DashboardMapSummary | null>(null)
const summaryTimestamp = ref<number | string | null>(null)
const mapDevices = ref<DashboardMapDevice[]>([])
const products = ref<DashboardProductOption[]>([])
const deviceOptions = ref<DashboardDeviceOption[]>([])
const selectedDevice = ref<DashboardMapDevice>()
const productKey = ref<string>()
const deviceKey = ref<string>()
const state = ref<DashboardStateFilter>('all')
const currentBounds = ref<DashboardMapBounds>()
const overviewLoading = ref(false)
const mapLoading = ref(false)
const productLoading = ref(false)
const deviceLoading = ref(false)
const refreshing = ref(false)
const hasMore = ref(false)
const eventDrawerVisible = ref(false)
const overviewVisible = ref(true)
let mapRequestSequence = 0
let productRequestSequence = 0
let deviceRequestSequence = 0

function resolveStateValue() {
  if (state.value === 'online') return 1
  if (state.value === 'offline') return 0
  return undefined
}

function resolveDeviceKey(device: DashboardMapDevice) {
  return String(device.iotId || device.id || device.deviceId)
}

async function loadOverview() {
  overviewLoading.value = true
  try {
    const response = await getDashboardOverview()
    overview.value = response.data || {}
  } finally {
    overviewLoading.value = false
  }
}

async function loadProducts(keyword = '') {
  const sequence = ++productRequestSequence
  productLoading.value = true
  try {
    const response = await getDashboardProducts({ name: keyword.trim() || undefined })
    if (sequence === productRequestSequence) products.value = response.data.records || []
  } finally {
    if (sequence === productRequestSequence) productLoading.value = false
  }
}

async function loadDeviceOptions(keyword = '') {
  const sequence = ++deviceRequestSequence
  deviceLoading.value = true
  try {
    const response = await getDashboardDeviceOptions({
      productKey: productKey.value,
      deviceName: keyword.trim() || undefined,
      state: resolveStateValue()
    })
    if (sequence === deviceRequestSequence) deviceOptions.value = response.data.records || []
  } finally {
    if (sequence === deviceRequestSequence) deviceLoading.value = false
  }
}

async function loadMapDevices() {
  if (!currentBounds.value) return
  const sequence = ++mapRequestSequence
  mapLoading.value = true
  try {
    const response = await getDashboardMapDevices({
      ...currentBounds.value,
      preview: false,
      limit: 180,
      productKey: productKey.value,
      state: resolveStateValue()
    })
    if (sequence !== mapRequestSequence) return
    mapDevices.value = (response.data.list || []).filter(hasDeviceLocation)
    summary.value = response.data.summary || null
    summaryTimestamp.value = response.data.timestamp ?? null
    hasMore.value = Boolean(response.data.hasMore)
    if (selectedDevice.value) {
      const refreshed = mapDevices.value.find((item) => String(item.id) === String(selectedDevice.value?.id))
      if (refreshed) selectedDevice.value = refreshed
    }
  } finally {
    if (sequence === mapRequestSequence) mapLoading.value = false
  }
}

const searchProducts = useDebounceFn((keyword: string) => loadProducts(keyword), 300)
const searchDevices = useDebounceFn((keyword: string) => loadDeviceOptions(keyword), 300)

async function updateProduct(value?: string) {
  productKey.value = value
  deviceKey.value = undefined
  selectedDevice.value = undefined
  deviceOptions.value = []
  await Promise.all([loadDeviceOptions(), loadMapDevices()])
}

async function updateState(value: DashboardStateFilter) {
  state.value = value
  deviceKey.value = undefined
  selectedDevice.value = undefined
  deviceOptions.value = []
  await Promise.all([loadDeviceOptions(), loadMapDevices()])
}

async function updateDevice(value?: string) {
  deviceKey.value = value
  selectedDevice.value = undefined
  if (!value) {
    await loadMapDevices()
    return
  }

  const option = deviceOptions.value.find((item) => resolveDeviceKey(item) === value)
  if (option && hasDeviceLocation(option)) {
    selectAndFocusDevice(option)
    return
  }

  const response = await getDashboardMapDevices({
    preview: true,
    limit: 12,
    keyword: option?.deviceId || option?.iotId || value,
    productKey: productKey.value,
    state: resolveStateValue()
  })
  const matched = (response.data.list || []).find(hasDeviceLocation)
  if (!matched) {
    Message.warning('该设备暂无有效位置信息')
    return
  }
  selectAndFocusDevice(matched)
}

function selectAndFocusDevice(device: DashboardMapDevice) {
  selectedDevice.value = device
  const exists = mapDevices.value.some((item) => String(item.id) === String(device.id))
  if (!exists) mapDevices.value = [device, ...mapDevices.value]
  nextTick(() => mapRef.value?.focusDevice(device))
}

function handleBoundsChange(bounds: DashboardMapBounds) {
  currentBounds.value = bounds
  loadMapDevices()
}

function resetFilters() {
  productKey.value = undefined
  deviceKey.value = undefined
  state.value = 'all'
  selectedDevice.value = undefined
  Promise.all([loadProducts(), loadDeviceOptions(), loadMapDevices()])
}

async function refreshDashboard() {
  refreshing.value = true
  try {
    await Promise.all([loadOverview(), loadProducts(), loadDeviceOptions(), loadMapDevices()])
  } finally {
    refreshing.value = false
  }
}

function openDeviceDetail(device: DashboardMapDevice) {
  router.push(`/system/instance/instance-details/${device.id}`)
}

onMounted(() => {
  Promise.all([loadOverview(), loadProducts(), loadDeviceOptions()])
  pushStore.initialize()
})
</script>

<template>
  <main class="dashboard-workbench">
    <DashboardToolbar
      :product-key="productKey"
      :device-key="deviceKey"
      :state="state"
      :products="products"
      :devices="deviceOptions"
      :product-loading="productLoading"
      :device-loading="deviceLoading"
      :refreshing="refreshing"
      :event-count="unreadCount"
      @update:product-key="updateProduct"
      @update:device-key="updateDevice"
      @update:state="updateState"
      @search-product="searchProducts"
      @search-device="searchDevices"
      @refresh="refreshDashboard"
      @reset="resetFilters"
      @reset-map="mapRef?.resetCenter()"
      @open-events="eventDrawerVisible = true"
    >
      <template #view-switch><slot name="view-switch" /></template>
    </DashboardToolbar>

    <section class="dashboard-workbench__map-shell">
      <DeviceMap
        ref="mapRef"
        :devices="mapDevices"
        :selected-id="selectedDevice?.id"
        :loading="mapLoading"
        :has-more="hasMore"
        :matched-devices="summary?.matchedDevices ?? 0"
        @bounds-change="handleBoundsChange"
        @select-device="selectedDevice = $event"
      />
      <OverviewPanel
        v-if="overviewVisible"
        :overview="overview"
        :summary="summary"
        :timestamp="summary ? summaryTimestamp : overview.timestamp"
        :loading="overviewLoading || mapLoading"
        @filter-state="updateState"
        @collapse="overviewVisible = false"
      />
      <a-tooltip v-else content="显示设备概览">
        <a-button class="overview-restore" aria-label="显示设备概览" @click="overviewVisible = true">
          <template #icon><icon-bar-chart /></template>
        </a-button>
      </a-tooltip>

      <transition name="inspector">
        <DeviceInspector
          v-if="selectedDevice"
          :device="selectedDevice"
          @close="selectedDevice = undefined"
          @open-detail="openDeviceDetail"
        />
      </transition>
    </section>

    <RealtimeEventsDrawer
      v-model:visible="eventDrawerVisible"
      :events="events"
      :connected="connected"
      @mark-all-read="pushStore.markAllRead()"
      @clear="pushStore.clearEvents()"
    />
  </main>
</template>

<style lang="scss" scoped>
.dashboard-workbench {
  box-sizing: border-box;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  padding: 10px;
  overflow: hidden;
  background: var(--color-fill-2);

  &__map-shell {
    position: relative;
    flex: 1;
    min-width: 0;
    min-height: 0;
    overflow: hidden;
    border: 1px solid var(--color-border-2);
    border-radius: 6px;
  }
}

.overview-restore {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 6;
  box-shadow: 0 4px 12px rgb(0 0 0 / 12%);
}

.inspector-enter-active,
.inspector-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.inspector-enter-from,
.inspector-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

@media (width <= 560px) {
  .inspector-enter-from,
  .inspector-leave-to {
    transform: translateY(12px);
  }
}
</style>
