import type { Login } from '@/apis/user'
import http from '@/utils/http'

export interface LoginTenant {
  tenantId: string
  tenantCode: string
  tenantName: string
  website?: string
  domain?: string
  siteName?: string
  loginTitle?: string
  siteLogo?: string
  siteIcon?: string
}

export function getLoginTenantList() {
  return http.get<LoginTenant[]>('/admin/system/tenant/public/list')
}

export function resolveLoginTenantByHost(host: string) {
  return http.get<LoginTenant | null>('/admin/system/tenant/public/resolve', { host })
}

export type WechatAuthStatus = 'WAITING' | 'SCANNED' | 'READY' | 'BINDING' | 'BOUND' | 'EXPIRED' | 'CANCELLED' | 'FAILED' | 'UNBOUND' | 'DENIED' | 'CONSUMED'

export interface WechatLoginOptions {
  enabled: boolean
  accountName: string
  appId: string
  loginMode: 'BOUND_USER' | 'DEMO_ACCOUNT'
  capabilityMode: 'AUTO' | 'SCENE_QR' | 'MESSAGE_CODE'
  accountQrUrl: string
  bindingEnabled: boolean
  expireSeconds: number
}

export interface WechatAuthSession {
  sessionId: string
  browserProof: string
  status: WechatAuthStatus
  qrUrl: string
  verificationCode?: string
  instructions?: string
  expiresIn: number
  transportMode?: string
}

export interface WechatSessionState {
  sessionId?: string
  status: WechatAuthStatus
  message?: string
  expiresIn?: number
}

export function getWechatLoginOptions(tenantCode?: string) {
  return http.get<WechatLoginOptions>('/auth/wechat/options', { tenantCode: tenantCode || undefined })
}

export function createWechatLoginSession(tenantCode?: string) {
  return http.post<WechatAuthSession>('/auth/wechat/sessions', { tenantCode: tenantCode || undefined })
}

const sessionPath = (sessionId: string) => `/auth/wechat/sessions/${encodeURIComponent(sessionId)}`
const sessionHeaders = (browserProof: string) => ({ 'X-Wechat-Proof': browserProof })

export function getWechatSession(session: WechatAuthSession) {
  return http.get<WechatSessionState>(sessionPath(session.sessionId), undefined, { headers: sessionHeaders(session.browserProof) })
}

export function cancelWechatSession(session: WechatAuthSession) {
  return http.delete<unknown>(sessionPath(session.sessionId), undefined, { headers: sessionHeaders(session.browserProof) })
}

export function getWechatWebSocketTicket(session: WechatAuthSession) {
  return http.post<{ ticket: string }>(`${sessionPath(session.sessionId)}/ws-ticket`, undefined, { headers: sessionHeaders(session.browserProof) })
}

export function exchangeWechatLogin(session: WechatAuthSession) {
  return http.request<Login>({
    url: '/oauth2/token',
    method: 'post',
    data: new URLSearchParams({
      grant_type: 'wechat_qr',
      session_id: session.sessionId,
      browser_proof: session.browserProof,
      client_id: 'web',
      client_secret: 'web'
    }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}
