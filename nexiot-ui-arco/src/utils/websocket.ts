type MessageHandler = (message: Record<string, any>) => void
type OpenHandler = () => void
type CloseHandler = (event: CloseEvent) => void
type ErrorHandler = (event: Event) => void

interface ConnectionConfig {
  url: string
  token: string
}

class WebSocketService {
  private socket: WebSocket | null = null
  private reconnectTimer?: ReturnType<typeof setTimeout>
  private heartbeatTimer?: ReturnType<typeof setInterval>
  private reconnectAttempts = 0
  private readonly maxReconnectAttempts = 10
  private readonly reconnectInterval = 3000
  private readonly heartbeatInterval = 30000
  private manualClose = false
  private currentConfig: ConnectionConfig | null = null
  private readonly messageHandlers = new Set<MessageHandler>()
  private readonly openHandlers = new Set<OpenHandler>()
  private readonly closeHandlers = new Set<CloseHandler>()
  private readonly errorHandlers = new Set<ErrorHandler>()

  connect(config: ConnectionConfig) {
    this.currentConfig = config
    const currentState = this.socket?.readyState
    if (currentState === WebSocket.OPEN || currentState === WebSocket.CONNECTING) return

    this.manualClose = false
    this.socket = new WebSocket(this.buildUrl(config.url, config.token))
    this.socket.onopen = () => {
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.openHandlers.forEach((handler) => handler())
    }
    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(String(event.data)) as Record<string, any>
        this.messageHandlers.forEach((handler) => handler(message))
      } catch (error) {
        console.error('WebSocket 消息解析失败:', error)
      }
    }
    this.socket.onclose = (event) => {
      this.stopHeartbeat()
      this.closeHandlers.forEach((handler) => handler(event))
      if (!this.manualClose && event.code !== 1000) this.scheduleReconnect()
    }
    this.socket.onerror = (event) => {
      this.errorHandlers.forEach((handler) => handler(event))
    }
  }

  disconnect() {
    this.manualClose = true
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.stopHeartbeat()
    this.socket?.close(1000, 'manual close')
    this.socket = null
    this.reconnectAttempts = 0
  }

  send(message: Record<string, unknown>) {
    if (this.socket?.readyState !== WebSocket.OPEN) return false
    this.socket.send(JSON.stringify(message))
    return true
  }

  updateSubscriptions(settings: Record<string, unknown>) {
    return this.send({ type: 'subscribe', settings })
  }

  getConnectionState() {
    if (!this.socket) return 'CLOSED' as const
    return ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED'][this.socket.readyState] as 'CONNECTING' | 'OPEN' | 'CLOSING' | 'CLOSED'
  }

  addMessageHandler(handler: MessageHandler) { this.messageHandlers.add(handler) }
  addOpenHandler(handler: OpenHandler) { this.openHandlers.add(handler) }
  addCloseHandler(handler: CloseHandler) { this.closeHandlers.add(handler) }
  addErrorHandler(handler: ErrorHandler) { this.errorHandlers.add(handler) }

  private buildUrl(url: string, token: string) {
    if (!token) return url
    return `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'heartbeat', timestamp: Date.now() })
    }, this.heartbeatInterval)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer)
    this.heartbeatTimer = undefined
  }

  private scheduleReconnect() {
    if (!this.currentConfig || this.reconnectAttempts >= this.maxReconnectAttempts) return
    this.reconnectAttempts += 1
    this.reconnectTimer = setTimeout(() => {
      if (this.currentConfig) this.connect(this.currentConfig)
    }, this.reconnectInterval)
  }
}

export default new WebSocketService()
