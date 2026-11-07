import type { WebSocketPushProfile, WebSocketPushSettings } from '@/views/system/account/api'
import { Button, Notification } from '@arco-design/web-vue'
import { defineStore } from 'pinia'
import { computed, h, reactive, ref } from 'vue'
import router from '@/router'
import { getToken } from '@/utils/auth'
import wsService from '@/utils/websocket'
import { DEFAULT_PUSH_SETTINGS, getWebSocketPushProfile, updateWebSocketPushProfile } from '@/views/system/account/api'

export interface DevicePushEvent {
  id: string
  title?: string
  summary?: string
  content?: string
  category?: string
  referenceId?: string
  routePath?: string
  level?: 'info' | 'success' | 'warning'
  deviceId?: string
  deviceName?: string
  timestamp?: number | string
  read?: boolean
}

function resolveWsUrl(path: string) {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL
  const prefix = import.meta.env.VITE_API_PREFIX === '/mock' ? '' : import.meta.env.VITE_API_PREFIX
  const url = new URL(`${prefix || ''}${path.startsWith('/') ? path : `/${path}`}`, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  return url.toString()
}

export function formatEventContent(event: DevicePushEvent) {
  if (event.summary || event.content) return event.summary || event.content || ''
  const device = event.deviceName && event.deviceId && event.deviceName !== event.deviceId
    ? `${event.deviceName}（${event.deviceId}）`
    : event.deviceName || event.deviceId
  return device ? `${device} 有一条新动态` : '收到一条设备消息'
}

export function getPushEventTarget(event: DevicePushEvent) {
  const path = event.routePath
  return path && router.getRoutes().some((route) => route.path === path && !route.meta.migrationPending)
    ? { path }
    : undefined
}

export const useWebSocketPushStore = defineStore('websocket-push', () => {
  const initialized = ref(false)
  const profileLoaded = ref(false)
  const connected = ref(false)
  const connecting = ref(false)
  const globalEnabled = ref(false)
  const adminEditable = ref(false)
  const websocketPath = ref('/ws/device-events')
  const settings = reactive<WebSocketPushSettings>({ ...DEFAULT_PUSH_SETTINGS })
  const events = ref<DevicePushEvent[]>([])
  const unreadCount = computed(() => events.value.filter((event) => !event.read).length)
  let handlersBound = false

  function applyProfile(profile?: Partial<WebSocketPushProfile>) {
    if (!profile) return
    if (profile.globalEnabled !== undefined) globalEnabled.value = Boolean(profile.globalEnabled)
    if (profile.adminEditable !== undefined) adminEditable.value = Boolean(profile.adminEditable)
    websocketPath.value = profile.websocketPath || websocketPath.value
    Object.assign(settings, DEFAULT_PUSH_SETTINGS, profile.settings || {}, { alarm: false, workOrder: false })
    profileLoaded.value = true
  }

  function disconnect() {
    wsService.disconnect()
    connected.value = false
    connecting.value = false
  }

  function connect() {
    const token = getToken()
    if (!token || !globalEnabled.value || !settings.enabled) {
      disconnect()
      return
    }
    const state = wsService.getConnectionState()
    if (state === 'OPEN') {
      connected.value = true
      connecting.value = false
      wsService.updateSubscriptions(settings)
      return
    }
    if (state === 'CONNECTING') {
      connecting.value = true
      return
    }
    connecting.value = true
    wsService.connect({ url: resolveWsUrl(websocketPath.value), token })
  }

  function applyConnection() {
    if (globalEnabled.value && settings.enabled) connect()
    else disconnect()
  }

  function notifyEvent(event: DevicePushEvent) {
    const method = event.level === 'warning'
      ? Notification.warning
      : event.level === 'success'
        ? Notification.success
        : Notification.info
    const target = getPushEventTarget(event)
    const notification = method({
      id: event.id,
      title: event.title || '实时消息',
      content: formatEventContent(event),
      duration: target ? 8000 : 4000,
      footer: target
        ? () => h(Button, {
            type: 'text',
            size: 'small',
            onClick: async () => {
              if (await openEvent(event)) notification.close()
            }
          }, { default: () => '查看详情' })
        : undefined
    })
  }

  function handleMessage(message: Record<string, any>) {
    if (!message?.type || message.type === 'heartbeat_response') return
    if (message.type === 'connected' || message.type === 'subscription_ack') {
      applyProfile(message.data)
      connected.value = true
      connecting.value = false
      return
    }
    if (message.type !== 'device_push' || !message.data) return
    const event: DevicePushEvent = {
      ...message.data,
      id: message.data.id || `${message.data.category || 'event'}_${message.data.deviceId || 'device'}_${message.data.timestamp || Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      read: false
    }
    if (['alarm', 'workOrder'].includes(event.category || '')) return
    if (events.value.some((item) => item.id === event.id)) return
    events.value = [event, ...events.value].slice(0, 50)
    notifyEvent(event)
  }

  function bindHandlers() {
    if (handlersBound) return
    handlersBound = true
    wsService.addMessageHandler(handleMessage)
    wsService.addOpenHandler(() => {
      connected.value = true
      connecting.value = false
      wsService.updateSubscriptions(settings)
    })
    wsService.addCloseHandler(() => {
      connected.value = false
      connecting.value = false
    })
    wsService.addErrorHandler(() => {
      connected.value = false
      connecting.value = false
    })
  }

  async function loadProfile() {
    const res = await getWebSocketPushProfile()
    applyProfile(res.data)
    applyConnection()
    return res.data
  }

  async function saveProfile(profile: Partial<WebSocketPushProfile>) {
    const res = await updateWebSocketPushProfile(profile)
    applyProfile(res.data)
    applyConnection()
    return res.data
  }

  async function initialize() {
    if (initialized.value) return
    initialized.value = true
    bindHandlers()
    try {
      await loadProfile()
    } catch {
      initialized.value = false
    }
  }

  function markAllRead() {
    events.value = events.value.map((event) => ({ ...event, read: true }))
  }

  function markRead(id: string) {
    const event = events.value.find((item) => item.id === id)
    if (event) event.read = true
  }

  async function openEvent(event: DevicePushEvent) {
    const target = getPushEventTarget(event)
    if (!target) {
      markRead(event.id)
      return false
    }
    const failure = await router.push(target)
    if (failure) return false
    markRead(event.id)
    return true
  }

  function clearEvents() {
    events.value = []
  }

  function reset() {
    disconnect()
    initialized.value = false
    profileLoaded.value = false
    globalEnabled.value = false
    adminEditable.value = false
    websocketPath.value = '/ws/device-events'
    Object.assign(settings, DEFAULT_PUSH_SETTINGS)
    events.value = []
  }

  return {
    profileLoaded,
    connected,
    connecting,
    globalEnabled,
    adminEditable,
    websocketPath,
    settings,
    events,
    unreadCount,
    initialize,
    loadProfile,
    saveProfile,
    applyConnection,
    markAllRead,
    markRead,
    openEvent,
    clearEvents,
    reset
  }
})
