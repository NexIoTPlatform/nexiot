<script setup lang="ts">
import type { DashboardMapBounds, DashboardMapDevice } from '../types'
import { useDashboardMap } from '../composables/useDashboardMap'

defineOptions({ name: 'DashboardDeviceMap' })

const props = defineProps<{
  devices: DashboardMapDevice[]
  selectedId?: string | number
  loading: boolean
  hasMore: boolean
  matchedDevices: number
}>()

const emit = defineEmits<{
  (e: 'bounds-change', value: DashboardMapBounds): void
  (e: 'select-device', value: DashboardMapDevice): void
}>()

const mapContainer = useTemplateRef<HTMLElement>('mapContainer')
const {
  mapReady,
  mapError,
  initialize,
  renderDevices,
  focusDevice: focusMapDevice,
  resetCenter
} = useDashboardMap({
  onBoundsChange: (bounds) => emit('bounds-change', bounds),
  onDeviceSelect: (device) => emit('select-device', device)
})

async function retryMap() {
  if (mapContainer.value) await initialize(mapContainer.value)
}

function focusDevice(device: DashboardMapDevice) {
  focusMapDevice(device)
}

watch(
  () => [props.devices, props.selectedId] as const,
  ([devices, selectedId]) => renderDevices(devices, selectedId),
  { deep: true }
)

onMounted(async () => {
  if (!mapContainer.value) return
  await initialize(mapContainer.value)
  renderDevices(props.devices, props.selectedId)
})

defineExpose({ focusDevice, resetCenter })
</script>

<template>
  <section class="device-map">
    <div ref="mapContainer" class="device-map__canvas" />

    <div v-if="!mapReady && !mapError" class="device-map__state">
      <a-spin :size="32" />
      <span>正在加载地图</span>
    </div>
    <div v-else-if="mapError" class="device-map__state">
      <a-result status="warning" title="地图暂时无法加载" :subtitle="mapError">
        <template #extra>
          <a-button type="primary" size="small" @click="retryMap">重新加载</a-button>
        </template>
      </a-result>
    </div>

    <div class="device-map__legend" aria-label="地图状态图例">
      <span><i class="is-online" />在线</span>
      <span><i class="is-offline" />离线</span>
    </div>

    <transition name="map-notice">
      <div v-if="hasMore" class="device-map__notice">
        <icon-info-circle />
        当前视野共 {{ matchedDevices.toLocaleString() }} 台设备，请放大地图查看
      </div>
    </transition>

    <div v-if="loading && mapReady" class="device-map__updating">
      <a-spin :size="14" />
      <span>更新设备</span>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.device-map {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--color-fill-2);

  &__canvas {
    width: 100%;
    height: 100%;
  }

  &__state {
    position: absolute;
    inset: 0;
    z-index: 5;
    display: grid;
    gap: 12px;
    align-content: center;
    justify-items: center;
    color: var(--color-text-3);
    background: var(--color-bg-2);
  }

  &__legend,
  &__updating,
  &__notice {
    position: absolute;
    z-index: 4;
    display: flex;
    align-items: center;
    font-size: 12px;
    color: var(--color-text-2);
    background: color-mix(in srgb, var(--color-bg-2) 92%, transparent);
    border: 1px solid var(--color-border-2);
    box-shadow: 0 4px 14px rgb(0 0 0 / 8%);
    backdrop-filter: blur(8px);
  }

  &__legend {
    bottom: 16px;
    left: 16px;
    gap: 12px;
    height: 32px;
    padding: 0 10px;
    border-radius: 4px;

    span {
      display: flex;
      gap: 5px;
      align-items: center;
    }

    i {
      width: 8px;
      height: 8px;
      background: rgb(var(--success-6));
      border-radius: 50%;

      &.is-offline {
        background: rgb(var(--danger-5));
      }
    }
  }

  &__notice {
    bottom: 16px;
    left: 50%;
    gap: 6px;
    min-height: 32px;
    padding: 6px 10px;
    color: rgb(var(--warning-7));
    border-color: rgb(var(--warning-3));
    border-radius: 4px;
    transform: translateX(-50%);
  }

  &__updating {
    right: 16px;
    bottom: 16px;
    gap: 6px;
    height: 32px;
    padding: 0 10px;
    border-radius: 4px;
  }
}

:global(.dashboard-device-marker) {
  position: relative;
  display: grid;
  place-items: start center;
  width: 26px;
  height: 34px;
  padding: 0;
  color: rgb(var(--success-6));
  cursor: pointer;
  background: transparent;
  border: 0;
  filter: drop-shadow(0 3px 5px rgb(0 0 0 / 24%));
}

:global(.dashboard-device-marker::after) {
  position: absolute;
  top: 14px;
  left: 9px;
  width: 8px;
  height: 16px;
  content: '';
  background: currentcolor;
  clip-path: polygon(0 0, 100% 0, 50% 100%);
}

:global(.dashboard-device-marker__core) {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  width: 20px;
  height: 20px;
  background: currentcolor;
  border: 3px solid white;
  border-radius: 50%;
}

:global(.dashboard-device-marker.is-offline) {
  color: rgb(var(--danger-5));
}

:global(.dashboard-device-marker.is-active) {
  color: rgb(var(--primary-6));
  transform: scale(1.25);
  transform-origin: bottom center;
}

:global(.dashboard-device-cluster) {
  box-sizing: border-box;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  background: rgb(var(--primary-6));
  border: 3px solid rgb(255 255 255 / 90%);
  border-radius: 50%;
  box-shadow: 0 5px 16px rgb(22 93 255 / 28%);
}

.map-notice-enter-active,
.map-notice-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.map-notice-enter-from,
.map-notice-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}

@media (width <= 560px) {
  .device-map {
    &__legend {
      bottom: 10px;
      left: 10px;
    }

    &__notice {
      right: 10px;
      bottom: 50px;
      left: 10px;
      justify-content: center;
      width: auto;
      text-align: center;
      transform: none;
    }

    &__updating {
      right: 10px;
      bottom: 10px;
    }
  }

  .map-notice-enter-from,
  .map-notice-leave-to {
    transform: translateY(8px);
  }
}
</style>
