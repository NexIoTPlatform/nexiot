import type { WechatAuthSession, WechatAuthStatus, WechatSessionState } from '../api'
import { cancelWechatSession, getWechatSession, getWechatWebSocketTicket } from '../api'

const TERMINAL_STATUSES = new Set<WechatAuthStatus>(['READY', 'BOUND', 'EXPIRED', 'CANCELLED', 'FAILED', 'UNBOUND', 'DENIED', 'CONSUMED'])

/** 登录与绑定共用短时会话；浏览器证明仅保存在当前页面内存。 */
export function useWechatSession() {
  const session = ref<WechatAuthSession | null>(null)
  const status = ref<WechatAuthStatus>('WAITING')
  const message = ref('')
  const error = ref('')
  const loading = ref(false)
  const remaining = ref(0)
  let generation = 0
  let deadline = 0
  let socket: WebSocket | null = null
  let timer: ReturnType<typeof setInterval> | undefined
  let pollTimer: ReturnType<typeof setTimeout> | undefined
  let pollFailures = 0

  function stopTransport() {
    if (timer) clearInterval(timer)
    if (pollTimer) clearTimeout(pollTimer)
    timer = undefined
    pollTimer = undefined
    const previous = socket
    socket = null
    previous?.close()
  }

  function applyState(next: WechatSessionState) {
    if (!session.value || (next.sessionId && next.sessionId !== session.value.sessionId)) return
    // HTTP 补查可能比 WS 返回得更晚，不能让旧结果撤回已确认的状态。
    if (TERMINAL_STATUSES.has(status.value) || (status.value === 'SCANNED' && next.status === 'WAITING')) return
    status.value = next.status
    message.value = next.message || ''
    error.value = ''
    if (typeof next.expiresIn === 'number') {
      remaining.value = Math.max(0, next.expiresIn)
      deadline = Date.now() + remaining.value * 1000
    }
    if (TERMINAL_STATUSES.has(next.status)) stopTransport()
  }

  function schedulePoll(run: number, delay: number) {
    if (pollTimer) clearTimeout(pollTimer)
    pollTimer = setTimeout(() => poll(run), delay)
  }

  async function poll(run: number) {
    if (run !== generation || !session.value || TERMINAL_STATUSES.has(status.value)) return
    try {
      const response = await getWechatSession(session.value)
      if (run !== generation) return
      pollFailures = 0
      applyState(response.data)
    } catch {
      if (run !== generation) return
      pollFailures += 1
      error.value = '暂时无法获取微信处理结果，正在重试；请保持当前页面打开。'
    }
    if (run !== generation || TERMINAL_STATUSES.has(status.value)) return
    // WS 负责即时通知；在线时也补查，防止跨节点通知漏收。
    const delay = socket?.readyState === WebSocket.OPEN ? 10000 : 2000
    schedulePoll(run, pollFailures > 2 ? Math.max(delay, 10000) : delay)
  }

  async function connect(run: number, current: WechatAuthSession) {
    try {
      const response = await getWechatWebSocketTicket(current)
      if (run !== generation || TERMINAL_STATUSES.has(status.value)) return
      const prefix = import.meta.env.VITE_API_PREFIX === '/mock' ? '' : import.meta.env.VITE_API_PREFIX || ''
      const url = new URL(`${prefix.replace(/\/$/, '')}/ws/wechat-auth`, window.location.origin)
      url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
      url.searchParams.set('ticket', response.data.ticket)
      const connection = new WebSocket(url)
      socket = connection
      connection.onmessage = (event) => {
        if (run !== generation) return
        try {
          const next = JSON.parse(String(event.data)) as WechatSessionState
          if (next.status && next.sessionId === current.sessionId) applyState(next)
        } catch {
          // 心跳或无关帧不影响 HTTP 状态补查。
        }
      }
      connection.onclose = () => {
        if (run !== generation || socket !== connection) return
        socket = null
        schedulePoll(run, 2000)
      }
      connection.onerror = () => connection.close()
    } catch {
      // WS 不可用时由轮询继续等待，扫码会话保持有效。
    }
  }

  function cancel() {
    generation += 1
    stopTransport()
    const previous = session.value
    session.value = null
    remaining.value = 0
    loading.value = false
    if (previous && !TERMINAL_STATUSES.has(status.value)) cancelWechatSession(previous).catch(() => {})
  }

  async function start(create: () => Promise<ApiRes<WechatAuthSession>>) {
    cancel()
    const run = generation
    loading.value = true
    status.value = 'WAITING'
    message.value = ''
    error.value = ''
    pollFailures = 0
    try {
      const response = await create()
      if (run !== generation) {
        cancelWechatSession(response.data).catch(() => {})
        return
      }
      session.value = response.data
      status.value = response.data.status
      remaining.value = Math.max(0, response.data.expiresIn)
      deadline = Date.now() + remaining.value * 1000
      timer = setInterval(() => {
        remaining.value = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
        if (remaining.value === 0) applyState({ status: 'EXPIRED', message: '本次二维码和一次码已过期，请刷新后重试。' })
      }, 1000)
      schedulePoll(run, 2000)
      connect(run, response.data)
    } catch (cause) {
      if (run === generation) error.value = (cause as Error).message || '微信登录会话创建失败'
    } finally {
      if (run === generation) loading.value = false
    }
  }

  function fail(reason: string) {
    generation += 1
    error.value = reason
    status.value = 'FAILED'
    stopTransport()
  }

  onBeforeUnmount(cancel)
  return { session, status, message, error, loading, remaining, start, cancel, fail }
}
