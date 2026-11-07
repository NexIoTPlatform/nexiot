import http from '@/utils/http'

export interface DeviceItem {
  id: string
  deviceId: string
  deviceName?: string
  iotId?: string
  extDeviceId?: string
  productKey: string
  productName?: string
  productId?: string
  state?: boolean | number | string
  deviceNode?: string
  thirdPlatform?: string
  pendingOwner?: boolean
  creatorId?: string
  creatorName?: string
  deptId?: string
  application?: string
  configuration?: string
  /** Device-scoped configuration. `configuration` is retained for legacy responses. */
  devConfiguration?: string
  detail?: string
  longitude?: number | string
  latitude?: number | string
  registryTime?: number | string
  onlineTime?: number | string
  createTime?: string
  updateTime?: string
  secretKey?: string
  gwProductKey?: string
  gwDeviceId?: string
  gwDeviceInfo?: string
  devGroupName?: string[]
  deriveMetadata?: string
  features?: string
  hasProtocol?: boolean
  [key: string]: unknown
}

export interface DeviceQuery {
  page: number
  size: number
  deviceId?: string
  deviceName?: string
  iotId?: string
  productKey?: string
  gwProductKey?: string
  extDeviceId?: string
  state?: number | string
  deviceTag?: string
  slaveAddress?: number
}

export interface GatewayDeviceCandidate {
  productKey: string
  deviceId: string
  deviceName?: string
  productName?: string
  state?: boolean | number | string
}

export interface GatewaySubDeviceItem {
  iotId?: string
  productKey: string
  deviceId: string
  deviceName?: string
  productName?: string
  state?: boolean | number | string
  onlineTime?: string | number
  configuration?: string
  ext1?: string
  photoUrl?: string
}

export interface DeviceDeleteImpact {
  source: string
  endpointCount: number
  pointCount: number
  collectGroupCount: number
  assetCount: number
  runtimeStatCount: number
}

export interface DeviceDeletePreview {
  id: string
  iotId: string
  productKey: string
  deviceId: string
  deviceName?: string
  childDeviceCount: number
  blocked: boolean
  blockers: string[]
  moduleImpacts: DeviceDeleteImpact[]
}

export interface DeviceShadow {
  id?: string
  iotId?: string
  metadata?: string | Record<string, unknown>
  version?: number | string
  lastTime?: number | string
  [key: string]: unknown
}

export interface DeviceGroupItem {
  id: string
  parentId?: string
  groupName: string
  groupCode?: string
  groupDescribe?: string
  groupLevel?: number
  hasChild?: boolean
  remark?: string
  deviceCount?: number
  children?: DeviceGroupItem[]
}

export interface DeviceLogItem {
  id?: string | number
  productKey?: string
  deviceId?: string
  iotId?: string
  deviceName?: string
  messageType?: string
  commandId?: string
  commandStatus?: number | string
  event?: string
  property?: string
  point?: string
  content?: string
  value?: unknown
  ext1?: string
  ext2?: string
  ext3?: string
  createTime?: string
  eventTime?: string
  [key: string]: unknown
}

export interface DeviceEventTotalItem {
  id: string
  name: string
  level?: string
  valueType?: unknown
  expands?: Record<string, unknown>
  description?: string
  qty?: string | number
  time?: string
  storagePolicy?: boolean
}

export interface DeviceSubscribeItem {
  id: string
  productKey?: string
  iotId?: string
  subType?: string | number
  msgType?: string | number | string[]
  url?: string
  status?: string | number
  createTime?: string
  [key: string]: unknown
}

export interface GatewayPollingConfig {
  id?: string
  productKey: string
  deviceId: string
  taskType: 'CLOUD_POLLING'
  enabled: boolean
  intervalSeconds: number
  timeoutSeconds: number
  retryTimes: number
  commandIntervalMs: number
  commands?: Record<string, unknown>[]
  [key: string]: unknown
}

export interface DeviceGroupPayload extends Partial<DeviceGroupItem> {
  devIds?: string[]
  deviceIds?: string[]
}

export function getDeviceList(params: DeviceQuery) {
  return http.get<PageRes<DeviceItem[]>>('/admin/v1/device/list', {
    pageNum: params.page,
    pageSize: params.size,
    deviceId: params.deviceId,
    deviceName: params.deviceName,
    iotId: params.iotId,
    productKey: params.productKey,
    gwProductKey: params.gwProductKey,
    extDeviceId: params.extDeviceId,
    state: params.state,
    deviceTag: params.deviceTag,
    slaveAddress: params.slaveAddress
  })
}

export function getDevice(id: string) {
  return http.get<DeviceItem>(`/admin/v1/device/${id}`)
}

export function addDevice(data: Partial<DeviceItem>) {
  return http.post<unknown>('/admin/v1/device', data)
}

export function updateDevice(data: Partial<DeviceItem>) {
  return http.put<unknown>('/admin/v1/device', data)
}

export function updateDeviceOwner(id: string, data: { unionId: string, appUniqueId?: string }) {
  return http.put<unknown>(`/admin/v1/device/${id}/owner`, data)
}

export function updateSubDeviceGateway(id: string, data: { gwProductKey: string, gwDeviceId: string }) {
  return http.put<unknown>(`/admin/v1/device/${id}/gateway`, data)
}

export function getSubDeviceGatewayCandidates(id: string) {
  return http.get<GatewayDeviceCandidate[]>(`/admin/v1/device/${id}/gateway/candidates`)
}

export function deleteDevices(ids: string[]) {
  return http.delete<unknown>(`/admin/v1/device/${ids.join(',')}`)
}

export function getDeviceDeletePreview(ids: string[]) {
  return http.get<DeviceDeletePreview[]>(`/admin/v1/device/delete-preview/${ids.join(',')}`)
}

export interface DeviceImportParams {
  productKey: string
  gwDeviceId?: string
  gwProductKey?: string
}

export function importDevices(file: File, params: DeviceImportParams) {
  const data = new FormData()
  data.append('file', file)
  return http.request<unknown>({ method: 'post', url: '/admin/v1/device/import', params, data })
}

export function downloadDeviceTemplate(productKey: string) {
  return http.download('/admin/v1/device/import/template', {
    method: 'post',
    params: { productKey },
    fileName: `device_template_${productKey}.xlsx`
  })
}

export function exportDevices(params: Omit<DeviceQuery, 'page' | 'size'>) {
  return http.download('/admin/v1/device/export', {
    method: 'post',
    params,
    fileName: `devices_${Date.now()}.xlsx`
  })
}

export function getDeviceShadow(id: string) {
  return http.get<DeviceShadow>(`/admin/v1/device/shadow/${id}`)
}

export function batchGetDeviceShadows(iotIds: string[]) {
  return http.post<DeviceShadow[]>('/admin/v1/device/shadow/batch', iotIds)
}

export function sendDeviceCommand(productKey: string, data: Record<string, unknown>) {
  return http.post<unknown>(`/admin/v1/device/down/${productKey}`, data)
}

export function simulateDeviceUp(productKey: string, data: Record<string, unknown>) {
  return http.post<unknown>(`/admin/v1/device/up/${productKey}`, data)
}


export function getDeviceGroups() {
  return http.get<DeviceGroupItem[]>('/admin/v1/instance/group/list')
}

export function getDeviceGroup(id: string) {
  return http.get<DeviceGroupItem>(`/admin/v1/instance/group/${id}`)
}

export function addDeviceGroup(data: DeviceGroupPayload) {
  return http.post<unknown>('/admin/v1/instance/group', data)
}

export function updateDeviceGroup(data: DeviceGroupPayload) {
  return http.put<unknown>('/admin/v1/instance/group', data)
}

export function deleteDeviceGroup(id: string) {
  return http.delete<unknown>(`/admin/v1/instance/group/${id}`)
}

export function bindDevicesToGroup(data: DeviceGroupPayload) {
  return http.post<unknown>('/admin/v1/instance/group/binds', data)
}

export function getGroupDeviceList(params: { page: number, size: number, groupId: string, deviceId?: string, deviceName?: string }) {
  return http.get<PageRes<DeviceItem[]>>('/admin/v1/instance/group/device/list', {
    pageNum: params.page,
    pageSize: params.size,
    groupId: params.groupId,
    deviceId: params.deviceId,
    deviceName: params.deviceName
  })
}

export function getUnboundDeviceList(params: { page: number, size: number, groupId: string, deviceId?: string, deviceName?: string, iotId?: string, productKey?: string, state?: number }) {
  return http.get<PageRes<DeviceItem[]>>(`/admin/v1/device/list/${params.groupId}`, {
    pageNum: params.page,
    pageSize: params.size,
    deviceId: params.deviceId,
    deviceName: params.deviceName,
    iotId: params.iotId,
    productKey: params.productKey,
    state: params.state
  })
}

export function unbindDevicesFromGroup(groupId: string, deviceIds: string[]) {
  return http.delete<unknown>(`/admin/v1/instance/group/${groupId}/${deviceIds.join(',')}`)
}

export function getDeviceLogList(params: Record<string, unknown>) {
  return http.get<PageRes<DeviceLogItem[]>>('/admin/v2/device/log/list', params)
}

export function getDeviceMetadataLogList(params: Record<string, unknown>) {
  return http.get<PageRes<DeviceLogItem[]>>('/admin/v2/device/log/meta/list', params)
}

export function getDeviceEventTotals(productKey: string, iotId: string) {
  return http.get<PageRes<DeviceEventTotalItem[]>>(`/admin/v2/device/log/event/total/${productKey}/${iotId}`)
}

export function exportDeviceMetadataLogs(params: Record<string, unknown>) {
  return http.download('/admin/v2/device/log/meta/export', {
    method: 'post',
    params,
    fileName: `device_logs_${Date.now()}.xlsx`
  })
}

export function getDeviceSubscriptions(params: { page: number, size: number, productKey: string, iotId: string }) {
  return http.get<PageRes<DeviceSubscribeItem[]>>('/admin/v1/subscribe/list', {
    pageNum: params.page,
    pageSize: params.size,
    productKey: params.productKey,
    iotId: params.iotId
  })
}

export function addDeviceSubscription(data: Partial<DeviceSubscribeItem>) {
  return http.post<unknown>('/admin/v1/subscribe/add', data)
}

export function updateDeviceSubscription(data: Partial<DeviceSubscribeItem>) {
  return http.post<unknown>('/admin/v1/subscribe/update', data)
}

export function deleteDeviceSubscriptions(productKey: string, iotId: string, ids: string[]) {
  return http.post<unknown>(`/admin/v1/subscribe/delete/${productKey}/${iotId}`, ids)
}

export function getGatewaySubDeviceRelation(productKey: string, deviceId: string) {
  return http.get<GatewaySubDeviceItem[]>(`/admin/v1/device/gateway/sub/relation/${productKey}/${deviceId}`)
}

export function getGatewayPollingConfig(productKey: string, deviceId: string, taskType: GatewayPollingConfig['taskType']) {
  return http.get<GatewayPollingConfig>(`/admin/v1/gateway/polling/${taskType}/get/${productKey}/${deviceId}`)
}

export function saveGatewayPollingConfig(data: GatewayPollingConfig) {
  return http.post<unknown>(`/admin/v1/gateway/polling/${data.taskType}/save`, data)
}

export function deleteGatewayPollingConfig(productKey: string, deviceId: string, taskType: GatewayPollingConfig['taskType']) {
  return http.delete<unknown>(`/admin/v1/gateway/polling/${taskType}/delete/${productKey}/${deviceId}`)
}

export function testGatewayPolling(productKey: string, deviceId: string, taskType: GatewayPollingConfig['taskType']) {
  return http.post<Record<string, unknown>>(`/admin/v1/gateway/polling/${taskType}/test/${productKey}/${deviceId}`)
}

export function getDeviceAlgorithmResults(params: { page: number, size: number, deviceId: string, algorithmType?: string, startTime?: string, endTime?: string }) {
  return http.get<PageRes<Record<string, unknown>[]>>(`/admin/algorithm/result/${params.deviceId}`, {
    pageNum: params.page,
    pageSize: params.size,
    algorithmType: params.algorithmType,
    startTime: params.startTime,
    endTime: params.endTime
  })
}
