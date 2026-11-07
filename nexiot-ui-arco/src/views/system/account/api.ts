import dayjs from 'dayjs'
import http from '@/utils/http'

const ROLE_SEPARATOR_RE = /[,、]/

export interface AccountUser {
  id: string | number
  unionId: string
  username: string
  alias?: string
  mobile?: string
  email?: string
  avatar?: string
  tenantId?: string
  tenantRole?: string
  identity?: number
  status?: number
  deptId?: string | number
  deptName?: string
  loginIp?: string
  loginDate?: string
  createDate?: string
  cfg?: string
  remark?: string
}

export interface AccountProfile {
  user: AccountUser
  roleGroup: Array<string | { roleName?: string, name?: string, roleKey?: string }> | string
}

export interface ProfilePayload {
  alias: string
  mobile: string
  email: string
  cfg?: string
}

export interface WebSocketPushSettings {
  enabled: boolean
  deviceCreate: boolean
  deviceOnline: boolean
  deviceOffline: boolean
  commandResp: boolean
  alarm: boolean
  workOrder: boolean
}

export interface WebSocketPushProfile {
  globalEnabled: boolean
  adminEditable: boolean
  websocketPath: string
  settings: WebSocketPushSettings
}

export interface TenantBranding {
  siteName: string
  siteLogo?: string
  siteIcon?: string
  watermarkEnabled: boolean
  watermarkText?: string
}

export const DEFAULT_PUSH_SETTINGS: WebSocketPushSettings = {
  enabled: false,
  deviceCreate: true,
  deviceOnline: true,
  deviceOffline: true,
  commandResp: false,
  alarm: false,
  workOrder: false
}

export function getAccountProfile() {
  return http.get<AccountProfile>('/admin/system/user/profile')
}

export function updateAccountProfile(data: ProfilePayload) {
  return http.put<unknown>('/admin/system/user/profile', data)
}

export function updateAccountPassword(oldPassword: string, newPassword: string) {
  return http.put<unknown>('/admin/system/user/profile/updatePwd', { oldPassword, newPassword })
}

export function uploadAccountAvatar(file: Blob, fileName = 'avatar.jpg') {
  const data = new FormData()
  data.append('avatarfile', file, fileName)
  return http.post<{ imgUrl: string }>('/admin/system/user/profile/avatar', data, { timeout: 120000 })
}

export function getWebSocketPushProfile() {
  return http.get<WebSocketPushProfile>('/admin/system/user/profile/websocketPush')
}

export function updateWebSocketPushProfile(data: Partial<WebSocketPushProfile>) {
  return http.put<WebSocketPushProfile>('/admin/system/user/profile/websocketPush', data)
}

export function getTenantBranding() {
  return http.get<TenantBranding>('/admin/system/tenant/current/branding')
}

export function updateTenantBranding(data: TenantBranding) {
  return http.put<TenantBranding>('/admin/system/tenant/current/branding', data)
}

export function normalizeRoleGroup(roleGroup: AccountProfile['roleGroup']) {
  if (Array.isArray(roleGroup)) {
    return roleGroup.map((item) => typeof item === 'string' ? item : item.roleName || item.name || item.roleKey || '').filter(Boolean)
  }
  return String(roleGroup || '').split(ROLE_SEPARATOR_RE).map((item) => item.trim()).filter(Boolean)
}

export function formatAccountDate(value?: string) {
  if (!value) return '暂无记录'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}
