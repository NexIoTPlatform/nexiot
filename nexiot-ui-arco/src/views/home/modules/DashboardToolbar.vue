<script setup lang="ts">
import type { DashboardDeviceOption, DashboardProductOption, DashboardStateFilter } from '../types'

defineOptions({ name: 'DashboardToolbar' })

const props = defineProps<{
  productKey?: string
  deviceKey?: string
  state: DashboardStateFilter
  products: DashboardProductOption[]
  devices: DashboardDeviceOption[]
  productLoading: boolean
  deviceLoading: boolean
  refreshing: boolean
  eventCount: number
}>()

const emit = defineEmits<{
  (e: 'update:product-key', value: string | undefined): void
  (e: 'update:device-key', value: string | undefined): void
  (e: 'update:state', value: DashboardStateFilter): void
  (e: 'search-product', value: string): void
  (e: 'search-device', value: string): void
  (e: 'refresh'): void
  (e: 'reset'): void
  (e: 'reset-map'): void
  (e: 'open-events'): void
}>()

const productOptions = computed(() => props.products.map((item) => ({
  label: `${item.name || item.productKey} (${item.productKey})`,
  value: item.productKey
})))

const deviceOptions = computed(() => props.devices.map((item) => ({
  label: item.deviceName || item.deviceId || item.iotId || String(item.id),
  value: String(item.iotId || item.id || item.deviceId),
  extra: item.deviceId || item.iotId || String(item.id)
})))
</script>

<template>
  <header class="dashboard-toolbar">
    <div class="dashboard-toolbar__selectors">
      <a-select
        :model-value="productKey"
        :options="productOptions"
        :loading="productLoading"
        allow-clear
        allow-search
        :filter-option="false"
        placeholder="全部产品"
        class="dashboard-toolbar__product"
        @update:model-value="emit('update:product-key', $event as string | undefined)"
        @search="emit('search-product', $event)"
      />
      <a-select
        :model-value="deviceKey"
        :loading="deviceLoading"
        allow-clear
        allow-search
        :filter-option="false"
        placeholder="搜索设备名称或编号"
        class="dashboard-toolbar__device"
        @update:model-value="emit('update:device-key', $event as string | undefined)"
        @search="emit('search-device', $event)"
      >
        <a-option v-for="item in deviceOptions" :key="item.value" :value="item.value">
          <span class="dashboard-toolbar__option">
            <strong>{{ item.label }}</strong>
            <small>{{ item.extra }}</small>
          </span>
        </a-option>
      </a-select>
      <a-radio-group
        :model-value="state"
        type="button"
        class="dashboard-toolbar__state"
        @change="emit('update:state', $event as DashboardStateFilter)"
      >
        <a-radio value="all">全部</a-radio>
        <a-radio value="online">在线</a-radio>
        <a-radio value="offline">离线</a-radio>
      </a-radio-group>
    </div>

    <div class="dashboard-toolbar__actions">
      <a-space :size="6">
      <a-tooltip content="回到全国视野">
        <a-button aria-label="回到全国视野" @click="emit('reset-map')">
          <template #icon><icon-location /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip content="重置筛选">
        <a-button aria-label="重置筛选" @click="emit('reset')">
          <template #icon><icon-undo /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip content="刷新工作台">
        <a-button aria-label="刷新工作台" :loading="refreshing" @click="emit('refresh')">
          <template #icon><icon-refresh /></template>
        </a-button>
      </a-tooltip>
      </a-space>
      <slot name="view-switch" />
    </div>
  </header>
</template>

<style lang="scss" scoped>
.dashboard-toolbar {
  box-sizing: border-box;
  display: flex;
  flex: none;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 5px 10px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__selectors {
    display: flex;
    flex: 1 1 550px;
    gap: 8px;
    align-items: center;
    min-width: 0;
  }

  &__product {
    flex: 0 1 210px;
    min-width: 150px;
  }

  &__device {
    flex: 1 1 230px;
    min-width: 160px;
    max-width: 300px;
  }

  &__state {
    flex: none;
  }

  &__option {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    strong,
    small {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      font-weight: 500;
    }

    small {
      color: var(--color-text-3);
    }
  }

  &__actions {
    display: flex;
    flex: none;
    gap: 12px;
    align-items: center;
    margin-left: auto;
  }
}

@media (width <= 900px) {
  .dashboard-toolbar {
    &__selectors {
      flex-basis: 100%;
    }
  }
}

@media (width <= 560px) {
  .dashboard-toolbar {
    &__selectors {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    &__product,
    &__device {
      width: 100%;
      min-width: 0;
      max-width: none;
    }

    &__state {
      grid-column: 1 / -1;
      width: max-content;
      max-width: 100%;
    }

    &__actions {
      justify-content: space-between;
      width: 100%;
    }
  }
}
</style>
