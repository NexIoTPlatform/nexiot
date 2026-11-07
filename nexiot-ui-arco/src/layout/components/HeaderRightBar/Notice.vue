<template>
  <section class="notice" aria-label="消息中心">
    <!-- 头部导航与操作 -->
    <header class="notice__header">
      <div class="notice__header-left">
        <span class="notice__title">消息中心</span>
        <span v-if="store.unreadCount" class="notice__badge" aria-label="未读数量">
          {{ store.unreadCount > 99 ? '99+' : store.unreadCount }}
        </span>

        <!-- 精致实时连接微状态 -->
        <a-tooltip :content="statusTooltip">
          <div
            class="notice__status-pill"
            :class="{
              'notice__status-pill--active': receiving,
              'notice__status-pill--warning': needsRetry,
              'notice__status-pill--muted': Boolean(subscriptionHint) && !needsRetry,
            }"
            @click="handleStatusClick"
          >
            <span class="notice__status-dot" />
            <span class="notice__status-text">{{ statusShortText }}</span>
            <icon-refresh v-if="needsRetry" :class="{ notice__spin: refreshing }" />
          </div>
        </a-tooltip>
      </div>

      <div class="notice__header-actions">
        <a-tooltip content="全部标为已读">
          <a-button
            type="text"
            size="mini"
            class="notice__action-btn"
            :disabled="!store.unreadCount"
            aria-label="全部标为已读"
            @click="store.markAllRead"
          >
            <template #icon><icon-check /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip content="推送设置">
          <a-button
            type="text"
            size="mini"
            class="notice__action-btn"
            aria-label="推送设置"
            @click="openSettings"
          >
            <template #icon><icon-settings /></template>
          </a-button>
        </a-tooltip>
      </div>
    </header>

    <!-- 分类胶囊与过滤条 -->
    <div class="notice__nav">
      <div class="notice__segmented" role="tablist" aria-label="消息分类">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="notice__segmented-item"
          :class="{ 'notice__segmented-item--active': category === tab.key }"
          role="tab"
          :aria-selected="category === tab.key"
          @click="category = tab.key"
        >
          <span>{{ tab.label }}</span>
          <span v-if="tab.unread" class="notice__segmented-count">{{ tab.unread > 99 ? '99+' : tab.unread }}</span>
        </button>
      </div>

      <!-- 仅看未读过滤胶囊 -->
      <button
        type="button"
        class="notice__filter-pill"
        :class="{ 'notice__filter-pill--active': unreadOnly }"
        :aria-pressed="unreadOnly"
        @click="unreadOnly = !unreadOnly"
      >
        <span class="notice__filter-dot" />
        <span>未读</span>
      </button>
    </div>

    <!-- 消息列表 -->
    <div class="notice__list">
      <div v-if="!visibleEvents.length" class="notice__empty">
        <div class="notice__empty-icon">
          <icon-notification />
        </div>
        <div class="notice__empty-title">{{ unreadOnly ? '没有未读消息' : '暂无新消息' }}</div>
        <div class="notice__empty-desc">{{ emptyDescription }}</div>
      </div>

      <div
        v-for="event in visibleEvents"
        :key="event.id"
        class="notice__item"
        :class="{ 'notice__item--unread': !event.read }"
        role="button"
        tabindex="0"
        :aria-label="`${event.title || eventTitle(event.category)}，${getPushEventTarget(event) ? '查看详情' : '标为已读'}`"
        @click="openEvent(event)"
        @keydown.enter="openEvent(event)"
      >
        <!-- 分类图标 -->
        <div class="notice__icon" :class="`notice__icon--${eventTone(event)}`">
          <icon-plus-circle v-if="event.category === 'deviceCreate'" />
          <icon-link v-else-if="event.category === 'deviceOnline'" />
          <icon-stop v-else-if="event.category === 'deviceOffline'" />
          <icon-message v-else-if="event.category === 'commandResp'" />
          <icon-notification v-else />
        </div>

        <!-- 消息正文 -->
        <div class="notice__body">
          <div class="notice__item-top">
            <span class="notice__item-title" :title="event.title || eventTitle(event.category)">
              {{ event.title || eventTitle(event.category) }}
            </span>
            <time class="notice__item-time" :title="fullTime(event.timestamp)">
              {{ shortTime(event.timestamp) }}
            </time>
          </div>

          <div class="notice__summary" :title="formatEventContent(event)">
            {{ formatEventContent(event) }}
          </div>

          <!-- 底部详情跳转提示 -->
          <div v-if="getPushEventTarget(event)" class="notice__item-bottom">
            <span class="notice__detail-link">查看详情
              <icon-right class="notice__detail-arrow" />
            </span>
          </div>
        </div>

        <!-- 未读微标与悬停快速标为已读 -->
        <div v-if="!event.read" class="notice__item-unread-box">
          <span class="notice__unread-dot" aria-label="未读" />
          <a-tooltip content="标为已读" position="left">
            <button
              type="button"
              class="notice__item-check-btn"
              aria-label="标为已读"
              @click.stop="markEventRead(event, $event)"
            >
              <icon-check />
            </button>
          </a-tooltip>
        </div>
      </div>
    </div>

    <!-- 底部信息与操作 -->
    <footer class="notice__footer">
      <span class="notice__footer-hint">仅保留本次会话最新 50 条</span>
      <a-popconfirm content="清空本次会话消息？业务记录不受影响。" position="tl" @ok="store.clearEvents">
        <a-button type="text" size="mini" class="notice__clear-btn" :disabled="!store.events.length">
          <template #icon><icon-delete /></template>清空
        </a-button>
      </a-popconfirm>
    </footer>
  </section>
</template>

<script setup lang="ts">
import type { DevicePushEvent } from '@/stores'
import Dayjs from 'dayjs'
import { formatEventContent, getPushEventTarget, useWebSocketPushStore } from '@/stores'

defineOptions({ name: 'RealtimeNotice' })
const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'navigate'): void }>()
const router = useRouter()
const store = useWebSocketPushStore()

const categories = [
  { key: 'all', label: '全部' },
  { key: 'device', label: '设备' }
]
const category = ref('all')
const unreadOnly = ref(false)
const refreshing = ref(false)
const refreshFailed = ref(false)
const now = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | undefined

const tabs = computed(() => categories.map((tab) => ({
  ...tab,
  unread: store.events.filter((event) => !event.read && matchesCategory(event, tab.key)).length
})))

const visibleEvents = computed(() => store.events.filter((event) => matchesCategory(event, category.value) && (!unreadOnly.value || !event.read)))

const needsRetry = computed(() => refreshFailed.value || !store.profileLoaded || (store.globalEnabled && store.settings.enabled && !store.connected && !store.connecting))

const subscriptionHint = computed(() => {
  if (!store.globalEnabled) return '全局推送已关闭'
  if (!store.settings.enabled) return '个人推送未开启'
  if (category.value === 'device' && !['deviceCreate', 'deviceOnline', 'deviceOffline', 'commandResp'].some((key) => store.settings[key as keyof typeof store.settings])) return '设备订阅未开启'
  return ''
})

const receiving = computed(() => store.profileLoaded && !refreshFailed.value && !subscriptionHint.value && store.connected)

const statusShortText = computed(() => {
  if (refreshFailed.value) return '重试'
  if (!store.profileLoaded) return '同步中'
  if (subscriptionHint.value) return '设置'
  if (store.connecting) return '连接中'
  return store.connected ? '实时' : '断开'
})

const statusTooltip = computed(() => {
  if (refreshFailed.value) return '连接状态获取失败，点击重新同步'
  if (!store.profileLoaded) return '正在获取推送设置'
  if (subscriptionHint.value) return `${subscriptionHint.value}，点击前往配置`
  if (store.connecting) return '正在连接实时消息服务'
  return store.connected ? '实时消息服务已就绪' : '连接已断开，点击重新连接'
})

const emptyDescription = computed(() => {
  if (unreadOnly.value) return '所有消息均已读完'
  if (subscriptionHint.value) return '开启订阅后，新消息将实时显示在这里'
  return '当前暂无设备动态'
})

function matchesCategory(event: DevicePushEvent, key: string) {
  if (key === 'all') return true
  return key === 'device' || event.category === key
}

function eventTone(event: DevicePushEvent) {
  return event.level === 'success' ? 'success' : 'primary'
}

function eventTitle(key?: string) {
  return ({
    deviceCreate: '设备注册',
    deviceOnline: '设备上线',
    deviceOffline: '设备离线',
    commandResp: '指令回复',
  } as Record<string, string>)[key || ''] || '设备消息'
}

function eventDate(value?: string | number) {
  if (value === undefined || value === '') return undefined
  const timestamp = Number(value)
  const date = Number.isFinite(timestamp) ? Dayjs(timestamp < 10_000_000_000 ? timestamp * 1000 : timestamp) : Dayjs(value)
  return date.isValid() ? date : undefined
}

function fullTime(value?: string | number) {
  return eventDate(value)?.format('YYYY-MM-DD HH:mm:ss') || ''
}

function shortTime(value?: string | number) {
  const date = eventDate(value)
  if (!date) return '刚刚'
  const minutes = Dayjs(now.value).diff(date, 'minute')
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  return date.format(date.isSame(now.value, 'day') ? 'HH:mm' : date.isSame(now.value, 'year') ? 'MM-DD HH:mm' : 'YYYY-MM-DD')
}

async function openEvent(event: DevicePushEvent) {
  if (await store.openEvent(event)) emit('navigate')
}

function markEventRead(event: DevicePushEvent, evt: MouseEvent) {
  evt.stopPropagation()
  store.markRead(event.id)
}

function handleStatusClick() {
  if (needsRetry.value) {
    refreshConnection()
  } else if (subscriptionHint.value) {
    openSettings()
  }
}

function openSettings() {
  emit('navigate')
  router.push({ path: '/system/account', query: { tab: 'push' } })
}

async function refreshConnection() {
  if (refreshing.value) return
  refreshing.value = true
  refreshFailed.value = false
  try {
    await store.loadProfile()
  } catch {
    refreshFailed.value = true
  } finally {
    refreshing.value = false
  }
}

watch(() => props.visible, (visible) => {
  clearInterval(clockTimer)
  if (!visible) return
  now.value = Date.now()
  clockTimer = setInterval(() => {
    now.value = Date.now()
  }, 30_000)
  refreshConnection()
}, { immediate: true })

onUnmounted(() => clearInterval(clockTimer))
</script>

<style lang="scss" scoped src="./Notice.scss"></style>
