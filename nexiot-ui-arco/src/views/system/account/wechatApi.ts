import type { WechatAuthSession, WechatLoginOptions } from '@/views/login/api'
import http from '@/utils/http'

export interface WechatBinding {
  id?: string | number
  appId: string
  accountName?: string
  openIdMasked?: string
  nickname?: string
  boundAt?: string
  createTime?: string
}

export interface WechatLoginConfig extends WechatLoginOptions {
  tenantId?: string
  configVersion?: number
  appSecret?: string
  callbackToken?: string
  encodingAESKey?: string
  demoUserUnionId: string
  callbackUrl?: string
  appSecretConfigured?: boolean
  callbackTokenConfigured?: boolean
  encodingAESKeyConfigured?: boolean
  clearEncodingAESKey?: boolean
}

export interface WechatDemoUser {
  unionId: string
  username: string
  nickname?: string
}

export function getWechatBindings() {
  return http.get<WechatBinding[]>('/admin/system/profile/wechat')
}

export function createWechatBindingSession() {
  return http.post<WechatAuthSession>('/admin/system/profile/wechat/sessions')
}

export function confirmWechatBinding(session: WechatAuthSession) {
  return http.post<unknown>(`/admin/system/profile/wechat/sessions/${encodeURIComponent(session.sessionId)}/confirm`, undefined, {
    headers: { 'X-Wechat-Proof': session.browserProof }
  })
}

export function removeWechatBinding() {
  return http.delete<unknown>('/admin/system/profile/wechat')
}

export function getWechatLoginConfig(tenantId?: string) {
  return http.get<WechatLoginConfig>('/admin/system/wechat/config', { tenantId })
}

export function saveWechatLoginConfig(data: WechatLoginConfig, tenantId?: string) {
  return http.put<WechatLoginConfig>('/admin/system/wechat/config', data, { params: { tenantId } })
}

export function getWechatDemoUsers(keyword?: string, tenantId?: string) {
  return http.get<WechatDemoUser[]>('/admin/system/wechat/users', { keyword: keyword || undefined, tenantId })
}
