import http from '@/utils/http'

export interface MiniAppItem {
  id: string
  appId: string
  appSecret?: string
  name: string
  logo?: string
  qrcodeImage?: string
  accessUrl?: string
  description?: string
  dataSourceType?: number
  linkedAppId?: string
  ak?: string
  sk?: string
  primaryColor?: string
  welcomeText?: string
  contactPhone?: string
  contactEmail?: string
  userCount?: number
  deviceCount?: number
  onlineDeviceCount?: number
  status?: number
  createTime?: string
  appName?: string
}
export interface MiniUserItem {
  id: string
  appId: string
  username?: string
  openId?: string
  phone?: string
  nickname?: string
  avatar?: string
  realName?: string
  remark?: string
  status?: number
  bindTime?: string
  lastLoginTime?: string
  createTime?: string
}
export interface MiniDeviceItem {
  id?: string
  iotId: string
  deviceName?: string
  deviceId?: string
  productName?: string
  productKey?: string
  lastOnlineTime?: string
  onlineStatus?: boolean | number
}
interface PageInfo<T> {
  list?: T[]
  records?: T[]
  total?: number
}

function normalizePage<T>(page?: PageInfo<T>): PageRes<T[]> {
  const records = page?.records || page?.list || []
  return { records, total: Number(page?.total || records.length) }
}

export async function getMiniAppList(params: {
  page: number
  size: number
  name?: string
  status?: number
}) {
  const res = await http.get<PageInfo<MiniAppItem>>('/admin/mp/app/list', {
    pageNum: params.page,
    pageSize: params.size,
    name: params.name,
    status: params.status
  })
  return { ...res, data: normalizePage(res.data) }
}
export function getMiniApp(id: string) {
  return http.get<MiniAppItem>(`/admin/mp/app/${id}`)
}
export function createMiniApp(data: Partial<MiniAppItem>) {
  return http.post<MiniAppItem>('/admin/mp/app/create', data)
}
export function updateMiniApp(data: Partial<MiniAppItem>) {
  return http.put<unknown>('/admin/mp/app/update', data)
}
export function disableMiniApp(id: string) {
  return http.put<unknown>(`/admin/mp/app/${id}/disable`)
}
export function enableMiniApp(id: string) {
  return http.put<unknown>(`/admin/mp/app/${id}/enable`)
}
export function refreshMiniApp(id: string) {
  return http.post<unknown>(`/admin/mp/app/${id}/refresh`)
}
export function deleteMiniApp(id: string) {
  return http.delete<unknown>(`/admin/mp/app/${id}`)
}
export function syncMiniAppSecrets() {
  return http.post<unknown>('/admin/mp/app/sync-all-secret')
}
export async function getMiniUserList(params: {
  page: number
  size: number
  appId?: string
  phone?: string
  nickname?: string
  status?: number
}) {
  const res = await http.get<PageInfo<MiniUserItem>>('/admin/mp/user/list', {
    pageNum: params.page,
    pageSize: params.size,
    appId: params.appId,
    phone: params.phone,
    nickname: params.nickname,
    status: params.status
  })
  return { ...res, data: normalizePage(res.data) }
}
export function getMiniUser(id: string) {
  return http.get<MiniUserItem>(`/admin/mp/user/${id}`)
}
export function createMiniUser(data: Record<string, unknown>) {
  return http.post<MiniUserItem>('/admin/mp/user/create', data)
}
export function getMiniUserPermissions(userId: string, appId: string) {
  return http.get<string[]>(`/admin/mp/user/${userId}/permissions`, { appId })
}
export function updateMiniUserPermissions(
  userId: string,
  appId: string,
  permissions: string[]
) {
  return http.put<unknown>(
    `/admin/mp/user/${userId}/permissions`,
    permissions,
    { params: { appId } }
  )
}
export function getMiniUserDevices(userId: string, appId: string) {
  return http.get<string[]>(`/admin/mp/user/${userId}/devices`, { appId })
}
export function updateMiniUserDevices(
  userId: string,
  appId: string,
  iotIds: string[]
) {
  return http.put<unknown>(`/admin/mp/user/${userId}/devices`, iotIds, {
    params: { appId }
  })
}
export async function getMiniAppDevices(
  appId: string,
  params: { page: number, size: number, keyword?: string }
) {
  const res = await http.get<PageInfo<MiniDeviceItem>>('/admin/mp/user/devices', {
    appId,
    pageNum: params.page,
    pageSize: params.size,
    keyword: params.keyword
  })
  return { ...res, data: normalizePage(res.data) }
}
export function disableMiniUser(id: string) {
  return http.put<unknown>(`/admin/mp/user/${id}/disable`)
}
export function enableMiniUser(id: string) {
  return http.put<unknown>(`/admin/mp/user/${id}/enable`)
}
export function deleteMiniUser(id: string) {
  return http.delete<unknown>(`/admin/mp/user/${id}`)
}
