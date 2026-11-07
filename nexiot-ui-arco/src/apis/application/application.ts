import type { DeviceGroupItem, DeviceItem } from '@/apis/iot/device'
import http from '@/utils/http'

export interface ApplicationItem {
  appUniqueId: string
  appName: string
  appId: string
  appSecret?: string
  appStatus: number
  remark?: string
  notifyUrl?: string
  upTopic?: string
  downTopic?: string
  devNum?: number
  httpEnable?: number
  mqttEnable?: number
  cfg?: string
  unionId?: string
  tenantId?: string
  createDate?: string
}

export interface PushConfig {
  enable?: boolean
  support?: boolean
  [key: string]: unknown
}
export interface TransformRule {
  id?: string | number
  appUniqueId?: string
  ruleName: string
  scopeType: string
  productKey?: string
  ruleType: string
  presetCode?: string
  content?: string
  options?: string
  sampleInput?: string
  sampleOutput?: string
  status?: number
  version?: number
  createTime?: string
  updateTime?: string
  publishTime?: string
}
export interface TransformOverview {
  config?: {
    enable?: boolean
    support?: boolean
    defaultRuleId?: string | number
    fallbackMode?: string
  }
  rule?: TransformRule
  rules?: TransformRule[]
  presets?: {
    code: string
    name: string
    description?: string
    implemented?: boolean
  }[]
}
export interface TransformPreview {
  success?: boolean
  inputJson?: string
  outputJson?: string
  message?: string
}
export interface ApplicationDeviceQuery {
  page: number
  size: number
  deviceId?: string
  deviceName?: string
  productKey?: string
  state?: number | string
}
export interface ProductSubscription {
  enabled?: boolean
}

export function getApplicationList(params: {
  page: number
  size: number
  appName?: string
  appStatus?: number
}) {
  return http.get<PageRes<ApplicationItem[]>>('/admin/v1/apply/list', {
    pageNum: params.page,
    pageSize: params.size,
    appName: params.appName,
    appStatus: params.appStatus
  })
}
export function getApplication(id: string) {
  return http.get<ApplicationItem>(`/admin/v1/apply/${id}`)
}
export function addApplication(data: Partial<ApplicationItem>) {
  return http.post<unknown>('/admin/v1/apply', data)
}
export function updateApplication(data: Partial<ApplicationItem>) {
  return http.put<unknown>('/admin/v1/apply', data)
}
export function updateApplicationStatus(
  appUniqueId: string,
  appStatus: number
) {
  return http.put<unknown>('/admin/v1/apply', { appUniqueId, appStatus })
}
export function deleteApplication(id: string) {
  return http.delete<unknown>(`/admin/v1/apply/${id}`)
}
export function resetApplicationSecret(appUniqueId: string) {
  return http.put<unknown>('/admin/v1/apply/reset', { appUniqueId })
}
export function checkApplicationUrl(notifyUrl: string) {
  return http.get<string>('/admin/v1/apply/check', { notifyUrl })
}
export function updatePushEnabled(
  appUniqueId: string,
  pushType: string,
  enable: boolean
) {
  return http.post<unknown>(
    `/admin/v1/apply/${appUniqueId}/${pushType}/${enable}`
  )
}
export function getProductSubscription(appUniqueId: string) {
  return http.get<ProductSubscription>(
    `/admin/v1/apply/subscription/${appUniqueId}`
  )
}
export function updateProductSubscription(
  appUniqueId: string,
  enabled: boolean
) {
  return http.post<unknown>(`/admin/v1/apply/subscription/${appUniqueId}`, {
    enabled
  })
}
export function getTransformOverview(appUniqueId: string) {
  return http.get<TransformOverview>(
    `/admin/v1/apply/transform/${appUniqueId}`
  )
}
export function updateTransformConfig(
  appUniqueId: string,
  data: TransformOverview['config']
) {
  return http.put<unknown>(
    `/admin/v1/apply/transform/${appUniqueId}/config`,
    data
  )
}
export function addTransformRule(data: TransformRule) {
  return http.post<TransformRule>('/admin/v1/apply/transform/rule', data)
}
export function getTransformRule(id: string | number) {
  return http.get<TransformRule>(`/admin/v1/apply/transform/rule/${id}`)
}
export function updateTransformRule(data: TransformRule) {
  return http.put<TransformRule>('/admin/v1/apply/transform/rule', data)
}
export function deleteTransformRule(id: string | number) {
  return http.delete<unknown>(`/admin/v1/apply/transform/rule/${id}`)
}
export function publishTransformRule(id: string | number) {
  return http.post<unknown>(`/admin/v1/apply/transform/rule/${id}/publish`)
}
export function previewTransform(data: {
  appUniqueId: string
  ruleType: string
  presetCode?: string
  content?: string
  options?: string
  sampleInput: string
}) {
  return http.post<TransformPreview>('/admin/v1/apply/transform/preview', data)
}
export function bindDevicesToApplication(ids: string[], appUniqueId?: string) {
  return http.put<unknown>('/admin/v1/device/app', {
    ids: ids.join(','),
    appUniqueId
  })
}

function toDeviceParams(params: ApplicationDeviceQuery) {
  return {
    pageNum: params.page,
    pageSize: params.size,
    deviceId: params.deviceId,
    deviceName: params.deviceName,
    productKey: params.productKey,
    state: params.state
  }
}

export function getApplicationDevices(
  appUniqueId: string,
  params: ApplicationDeviceQuery
) {
  return http.get<PageRes<DeviceItem[]>>('/admin/v1/device/list', {
    ...toDeviceParams(params),
    application: appUniqueId
  })
}

export function getUnboundApplicationDevices(params: ApplicationDeviceQuery) {
  return http.get<PageRes<DeviceItem[]>>(
    '/admin/v1/device/unlist',
    toDeviceParams(params)
  )
}

export function getApplicationDeviceGroups() {
  return http.get<DeviceGroupItem[]>('/admin/v1/instance/group/list')
}

export function getApplicationGroupDevices(groupId: string) {
  return http.get<PageRes<DeviceItem[]>>(
    '/admin/v1/instance/group/device/list',
    { groupId, pageNum: 1, pageSize: 10000 }
  )
}

export function exportApplicationDevices(
  appUniqueId: string,
  params: Omit<ApplicationDeviceQuery, 'page' | 'size'>
) {
  return http.download('/admin/v1/device/export', {
    method: 'post',
    params: { ...params, application: appUniqueId },
    fileName: `application_devices_${Date.now()}.xlsx`
  })
}
