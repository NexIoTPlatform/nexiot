<script setup lang="ts">
import type { DashboardMapSummary, DashboardOverview, DashboardStateFilter } from '../types'
import Dayjs from 'dayjs'

defineOptions({ name: 'DashboardOverviewPanel' })

const props = defineProps<{
  overview: DashboardOverview
  summary: DashboardMapSummary | null
  timestamp?: number | string | null
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'filter-state', value: DashboardStateFilter): void
  (e: 'collapse'): void
}>()

function count(value: unknown) {
  if (value == null || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : null
}

function formatCount(value: number | null) {
  return value == null ? '—' : value.toLocaleString('zh-CN')
}

// 地图摘要返回后统一使用同一份统计，真实的 0 不回退到较旧的概览值。
const totalDevices = computed(() => count(props.summary ? props.summary.totalDevices : props.overview.deviceStats?.totalDevices))
const onlineDevices = computed(() => count(props.summary ? props.summary.onlineDevices : props.overview.deviceStats?.onlineDevices))
const offlineDevices = computed(() => {
  if (!props.summary) return count(props.overview.deviceStats?.offlineDevices)
  return totalDevices.value == null || onlineDevices.value == null ? null : Math.max(0, totalDevices.value - onlineDevices.value)
})
const locatedDevices = computed(() => count(props.summary?.locatedDevices))
const updatedAt = computed(() => {
  const value = props.timestamp === undefined ? props.overview.timestamp : props.timestamp
  if (value == null || String(value).trim() === '') return '—'
  const numeric = Number(value)
  const date = Number.isFinite(numeric) ? Dayjs(numeric < 10_000_000_000 ? numeric * 1000 : numeric) : Dayjs(value)
  return date.isValid() && date.valueOf() > 0 ? date.format('HH:mm') : '—'
})
</script>

<template>
  <aside class="overview-panel">
    <header class="overview-panel__header">
      <div>
        <span>设备运营概览</span>
        <small>更新于 {{ updatedAt }}</small>
      </div>
      <div class="overview-panel__header-actions">
        <a-spin v-if="loading" :size="16" />
        <a-tooltip content="隐藏概览">
          <a-button aria-label="隐藏概览" size="mini" type="text" @click="emit('collapse')">
            <template #icon><icon-up /></template>
          </a-button>
        </a-tooltip>
      </div>
    </header>

    <div class="overview-panel__metrics">
      <button type="button" class="overview-metric" @click="emit('filter-state', 'all')">
        <span class="overview-metric__icon is-total"><icon-computer /></span>
        <span><small>设备总数</small><strong>{{ formatCount(totalDevices) }}</strong></span>
      </button>
      <button type="button" class="overview-metric" @click="emit('filter-state', 'online')">
        <span class="overview-metric__icon is-online"><icon-wifi /></span>
        <span><small>在线设备</small><strong>{{ formatCount(onlineDevices) }}</strong></span>
      </button>
      <button type="button" class="overview-metric" @click="emit('filter-state', 'offline')">
        <span class="overview-metric__icon is-offline"><icon-close-circle /></span>
        <span><small>离线设备</small><strong>{{ formatCount(offlineDevices) }}</strong></span>
      </button>
      <div class="overview-metric">
        <span class="overview-metric__icon is-located"><icon-location /></span>
        <span><small>已定位</small><strong>{{ formatCount(locatedDevices) }}</strong></span>
      </div>
    </div>
  </aside>
</template>

<style lang="scss" scoped>
.overview-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 4;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 286px;
  max-width: calc(100% - 24px);
  min-height: 0;
  max-height: calc(100% - 64px);
  padding: 14px;
  overflow-y: auto;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  box-shadow: 0 3px 14px rgb(0 0 0 / 8%);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--color-border-1);

    > div:first-child {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    span {
      font-size: 13px;
      font-weight: 600;
      color: var(--color-text-1);
    }

    small {
      font-size: 11px;
      color: var(--color-text-3);
    }
  }

  &__header-actions {
    display: flex;
    gap: 4px;
    align-items: center;
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
    padding-top: 10px;
  }
}

.overview-metric {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 58px;
  padding: 8px;
  text-align: left;
  background: var(--color-fill-1);
  border: 1px solid transparent;
  border-radius: 4px;

  &[type='button'] {
    cursor: pointer;

    &:hover {
      border-color: var(--color-border-3);
    }
  }

  > span:last-child {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  small {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 11px;
    color: var(--color-text-3);
    white-space: nowrap;
  }

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 19px;
    font-weight: 600;
    line-height: 1;
    color: var(--color-text-1);
  }

  &__icon {
    display: grid;
    flex: none;
    place-items: center;
    width: 26px;
    height: 26px;
    color: rgb(var(--primary-6));
    background: rgb(var(--primary-1));
    border-radius: 4px;

    &.is-online {
      color: rgb(var(--success-6));
      background: rgb(var(--success-1));
    }

    &.is-offline {
      color: rgb(var(--danger-6));
      background: rgb(var(--danger-1));
    }

    &.is-located {
      color: rgb(var(--warning-6));
      background: rgb(var(--warning-1));
    }
  }
}

@media (width <= 560px), (height <= 680px) {
  .overview-panel {
    width: 264px;
    max-height: min(202px, calc(100% - 64px));
    padding: 10px;

    &__metrics {
      gap: 5px;
      padding-top: 8px;
    }
  }

  .overview-metric {
    min-height: 48px;
    padding: 6px;

    strong {
      font-size: 17px;
    }

    &__icon {
      width: 23px;
      height: 23px;
    }
  }
}
</style>
