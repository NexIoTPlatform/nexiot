<script setup lang="ts">
import type { DashboardMapDevice } from '../types'
import Dayjs from 'dayjs'
import { isOnlineDevice } from '../types'

defineOptions({ name: 'DashboardDeviceInspector' })

const props = defineProps<{ device: DashboardMapDevice }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'open-detail', value: DashboardMapDevice): void
}>()

const UNIX_TIMESTAMP_RE = /^\d{10,13}$/

function formatTime(value?: string | number) {
  if (!value || value === '0') return '从未通信'
  const text = String(value)
  const numeric = UNIX_TIMESTAMP_RE.test(text) ? Number(text) : Number.NaN
  const date = Number.isNaN(numeric) ? Dayjs(text) : Dayjs(text.length === 10 ? numeric * 1000 : numeric)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

const coordinate = computed(() => {
  const longitude = Number(props.device.longitude)
  const latitude = Number(props.device.latitude)
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return '-'
  return `${longitude.toFixed(6)}, ${latitude.toFixed(6)}`
})
</script>

<template>
  <aside class="device-inspector">
    <header>
      <div>
        <a-tag :color="isOnlineDevice(device) ? 'green' : 'red'" size="small">
          {{ isOnlineDevice(device) ? '在线' : '离线' }}
        </a-tag>
        <span>{{ device.deviceNode === 'VIDEO_DEVICE' ? '视频设备' : '设备' }}</span>
      </div>
      <a-button type="text" size="mini" aria-label="关闭设备信息" @click="emit('close')">
        <template #icon><icon-close /></template>
      </a-button>
    </header>

    <section class="device-inspector__identity">
      <h3>{{ device.deviceName || device.deviceId || device.iotId || '未命名设备' }}</h3>
      <p>{{ device.productName || '未归类产品' }}</p>
    </section>

    <dl>
      <div><dt>DeviceId</dt><dd>{{ device.deviceId || '-' }}</dd></div>
      <div><dt>IoT ID</dt><dd>{{ device.iotId || '-' }}</dd></div>
      <div><dt>ProductKey</dt><dd>{{ device.productKey || '-' }}</dd></div>
      <div><dt>最近通信</dt><dd>{{ formatTime(device.onlineTime) }}</dd></div>
      <div><dt>设备坐标</dt><dd>{{ coordinate }}</dd></div>
    </dl>

    <footer>
      <a-button type="primary" long @click="emit('open-detail', device)">
        查看设备详情
        <template #icon><icon-launch /></template>
      </a-button>
    </footer>
  </aside>
</template>

<style lang="scss" scoped>
.device-inspector {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  display: flex;
  flex-direction: column;
  width: min(330px, calc(100% - 32px));
  max-height: calc(100% - 72px);
  overflow: hidden;
  background: color-mix(in srgb, var(--color-bg-2) 96%, transparent);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  box-shadow: 0 10px 30px rgb(0 0 0 / 14%);
  backdrop-filter: blur(10px);

  header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 48px;
    padding: 0 10px 0 14px;
    border-bottom: 1px solid var(--color-border-1);

    > div {
      display: flex;
      gap: 8px;
      align-items: center;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__identity {
    padding: 16px 16px 12px;

    h3 {
      margin: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 17px;
      font-weight: 600;
      color: var(--color-text-1);
      white-space: nowrap;
    }

    p {
      margin: 6px 0 0;
      overflow: hidden;
      text-overflow: ellipsis;
      color: var(--color-text-3);
      white-space: nowrap;
    }
  }

  dl {
    display: grid;
    gap: 0;
    padding: 0 16px 12px;
    margin: 0;
    overflow-y: auto;

    div {
      display: grid;
      grid-template-columns: 82px minmax(0, 1fr);
      gap: 10px;
      padding: 9px 0;
      border-bottom: 1px solid var(--color-border-1);
    }

    dt {
      color: var(--color-text-3);
    }

    dd {
      margin: 0;
      color: var(--color-text-2);
      text-align: right;
      overflow-wrap: anywhere;
    }
  }

  footer {
    padding: 12px 16px 16px;
  }
}

@media (width <= 560px) {
  .device-inspector {
    inset: auto 8px 8px;
    width: auto;
    max-height: min(64%, 420px);
  }
}
</style>
