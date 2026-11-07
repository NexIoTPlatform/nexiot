import http from '@/utils/http'

export interface OtaPackageItem {
  id: string
  productId?: string
  productKey: string
  productName?: string
  packageName: string
  version: string
  packageType: string
  customType?: string
  module?: string
  description?: string
  fileUrl: string
  fileName?: string
  fileSize?: number
  checksumAlgorithm?: string
  checksum?: string
  signatureUrl?: string
  minSoftwareVersion?: string
  minHardwareVersion?: string
  maxHardwareVersion?: string
  forceUpgrade?: boolean | number
  tagIds?: string[]
  tagNames?: string[]
  tagIdsText?: string
  tagNamesText?: string
  releaseNotes?: string
  status?: string
  createTime?: string
}

export interface OtaTagItem {
  id: string
  tagCode: string
  tagName: string
  color?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface OtaTaskItem {
  id: string
  taskNo?: string
  taskName: string
  productKey: string
  productName?: string
  packageId: string
  packageName?: string
  targetVersion?: string
  rolloutMode: string
  deviceIds?: string[]
  maxRetryCount?: number
  timeoutSeconds?: number
  plannedStartTime?: string
  requireVersionCheck?: boolean | number
  grayPercent?: number
  grayDeviceLimit?: number
  batchSize?: number
  batchIntervalSeconds?: number
  failureThresholdPercent?: number
  rollbackOnFailure?: boolean | number
  rollbackPackageId?: string
  autoDispatch?: boolean | number
  description?: string
  status?: string
  deviceCount?: number
  failedCount?: number
  totalCount?: number
  successCount?: number
  failureCount?: number
  createTime?: string
}

export interface OtaTaskDeviceItem {
  id: string
  taskId?: string
  deviceId: string
  deviceName?: string
  currentVersion?: string
  targetVersion?: string
  status?: string
  retryCount?: number
  progress?: number
  transferState?: string
  errorMessage?: string
  createTime?: string
  updateTime?: string
  pushedTime?: string
  lastReportTime?: string
  successTime?: string
}

export interface OtaDeviceSummary {
  currentVersion?: string | { currentVersion?: string }
  targetVersion?: string
  latestTask?: OtaTaskItem
  totalTasks?: number
  successTasks?: number
  failedTasks?: number
  [key: string]: unknown
}

export function getOtaPackageList(params: { page: number, size: number, productKey?: string, version?: string, packageType?: string, status?: string }) {
  return http.get<PageRes<OtaPackageItem[]>>('/admin/v1/ota/package/list', { pageNum: params.page, pageSize: params.size, ...params })
}

export function getOtaPackage(id: string) {
  return http.get<OtaPackageItem>(`/admin/v1/ota/package/${id}`)
}

export function addOtaPackage(data: Partial<OtaPackageItem>) {
  return http.post<unknown>('/admin/v1/ota/package', data)
}

export function updateOtaPackage(data: Partial<OtaPackageItem>) {
  return http.put<unknown>('/admin/v1/ota/package', data)
}

export function deleteOtaPackage(id: string) {
  return http.delete<unknown>(`/admin/v1/ota/package/${id}`)
}

export function updateOtaPackageStatus(id: string, status: string) {
  return http.put<unknown>(`/admin/v1/ota/package/status/${id}`, undefined, { params: { status } })
}

export function uploadOtaPackage(file: File) {
  const data = new FormData()
  data.append('file', file)
  return http.request<{ url?: string, fileName?: string, fileSize?: number, sha256?: string, sha512?: string, md5?: string, hashSkipped?: boolean }>({ method: 'post', url: '/admin/v1/ota/package/upload', data })
}

export function getOtaTagList(params: { page: number, size: number }) {
  return http.get<PageRes<OtaTagItem[]>>('/admin/v1/ota/tag/list', { pageNum: params.page, pageSize: params.size })
}

export function addOtaTag(data: Partial<OtaTagItem>) {
  return http.post<unknown>('/admin/v1/ota/tag', data)
}

export function updateOtaTag(data: Partial<OtaTagItem>) {
  return http.put<unknown>('/admin/v1/ota/tag', data)
}

export function deleteOtaTag(id: string) {
  return http.delete<unknown>(`/admin/v1/ota/tag/${id}`)
}

export function getOtaTaskList(params: { page: number, size: number, productKey?: string, taskName?: string, status?: string }) {
  return http.get<PageRes<OtaTaskItem[]>>('/admin/v1/ota/task/list', { pageNum: params.page, pageSize: params.size, ...params })
}

export function getOtaTask(id: string) {
  return http.get<OtaTaskItem>(`/admin/v1/ota/task/${id}`)
}

export function createOtaTask(data: Partial<OtaTaskItem>) {
  return http.post<unknown>('/admin/v1/ota/task', data)
}

export function dispatchOtaTask(id: string) {
  return http.post<unknown>(`/admin/v1/ota/task/dispatch/${id}`)
}

export function cancelOtaTask(id: string) {
  return http.post<unknown>(`/admin/v1/ota/task/cancel/${id}`)
}

export function getOtaTaskDevices(taskId: string, params = { page: 1, size: 20 }) {
  return http.get<PageRes<OtaTaskDeviceItem[]> | OtaTaskDeviceItem[]>(`/admin/v1/ota/task/devices/${taskId}`, { pageNum: params.page, pageSize: params.size })
}

export function retryOtaTaskDevice(taskDeviceId: string) {
  return http.post<unknown>(`/admin/v1/ota/task/retry/device/${taskDeviceId}`)
}

export function getOtaDeviceSummary(productKey: string, deviceId: string) {
  return http.get<OtaDeviceSummary>(`/admin/v1/ota/task/summary/device/${productKey}/${deviceId}`)
}

export function getOtaDeviceVersion(productKey: string, deviceId: string) {
  return http.get<Record<string, unknown>>(`/admin/v1/ota/task/version/device/${productKey}/${deviceId}`)
}
