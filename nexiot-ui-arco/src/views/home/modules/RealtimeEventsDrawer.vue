<script setup lang="ts">
import type { DevicePushEvent } from '@/stores/useWebSocketPushStore'
import Dayjs from 'dayjs'

defineOptions({ name: 'DashboardRealtimeEventsDrawer' })

defineProps<{
  visible: boolean
  events: DevicePushEvent[]
  connected: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'mark-all-read'): void
  (e: 'clear'): void
}>()

function formatTime(value?: number | string) {
  if (!value) return '-'
  const time = Dayjs(value)
  return time.isValid() ? time.format('MM-DD HH:mm:ss') : String(value)
}

function resolveColor(level?: DevicePushEvent['level']) {
  if (level === 'warning') return 'orange'
  if (level === 'success') return 'green'
  return 'blue'
}
</script>

<template>
  <a-drawer
    :visible="visible"
    title="实时动态"
    width="min(420px, 100%)"
    :footer="false"
    unmount-on-close
    @cancel="emit('update:visible', false)"
  >
    <template #title>
      <div class="event-drawer__title">
        <span>实时动态</span>
        <a-badge :status="connected ? 'success' : 'normal'" :text="connected ? '已连接' : '未连接'" />
      </div>
    </template>
    <div class="event-drawer__toolbar">
      <span>仅展示本次会话最近 50 条设备事件</span>
      <a-space :size="4">
        <a-button type="text" size="mini" :disabled="!events.length" @click="emit('mark-all-read')">全部已读</a-button>
        <a-popconfirm content="确定清空当前实时动态吗？" @ok="emit('clear')">
          <a-button type="text" size="mini" status="danger" :disabled="!events.length">清空</a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <div v-if="events.length" class="event-drawer__list">
      <article v-for="event in events" :key="event.id" :class="{ unread: !event.read }">
        <i :class="`is-${event.level || 'info'}`" />
        <div>
          <header>
            <strong>{{ event.title || '设备消息' }}</strong>
            <a-tag :color="resolveColor(event.level)" size="small">{{ event.category || '设备动态' }}</a-tag>
          </header>
          <p>{{ event.summary || event.content || `${event.deviceName || event.deviceId || '设备'} 有一条新动态` }}</p>
          <footer>
            <span>{{ event.deviceName || event.deviceId || '-' }}</span>
            <time>{{ formatTime(event.timestamp) }}</time>
          </footer>
        </div>
      </article>
    </div>
    <a-empty v-else description="暂无实时设备动态" />
  </a-drawer>
</template>

<style lang="scss" scoped>
.event-drawer {
  &__title {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  &__toolbar {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
    min-height: 42px;
    margin-bottom: 10px;
    font-size: 12px;
    color: var(--color-text-3);
    border-bottom: 1px solid var(--color-border-1);
  }

  &__list {
    display: grid;

    article {
      position: relative;
      display: grid;
      grid-template-columns: 10px minmax(0, 1fr);
      gap: 10px;
      padding: 14px 0;
      border-bottom: 1px solid var(--color-border-1);

      > i {
        width: 8px;
        height: 8px;
        margin-top: 6px;
        background: rgb(var(--primary-5));
        border-radius: 50%;

        &.is-warning {
          background: rgb(var(--warning-6));
        }

        &.is-success {
          background: rgb(var(--success-6));
        }
      }

      &.unread::after {
        position: absolute;
        top: 15px;
        right: 0;
        width: 6px;
        height: 6px;
        content: '';
        background: rgb(var(--danger-6));
        border-radius: 50%;
      }

      header,
      footer {
        display: flex;
        gap: 8px;
        align-items: center;
        justify-content: space-between;
      }

      strong {
        overflow: hidden;
        text-overflow: ellipsis;
        color: var(--color-text-1);
        white-space: nowrap;
      }

      p {
        margin: 7px 0;
        line-height: 1.55;
        color: var(--color-text-2);
      }

      footer {
        font-size: 12px;
        color: var(--color-text-3);
      }
    }
  }
}

@media (width <= 480px) {
  .event-drawer__toolbar {
    flex-direction: column;
    align-items: flex-start;
    padding-bottom: 8px;
  }
}
</style>
