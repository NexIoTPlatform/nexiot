import http from '@/utils/http'

export interface OssFileItem {
  ossId: string
  fileName: string
  originalName: string
  fileSuffix: string
  url: string
  service: string
  createBy: string
  createTime: string
}

export interface OssProviderOption {
  label: string
  value: string
}

export interface OssStorageConfig {
  configId?: string
  provider: string
  endpoint: string
  publicEndpoint?: string
  domain?: string
  prefix: string
  accessKey: string
  secretKey: string
  bucketName: string
  region?: string
  isHttps: boolean
  active?: boolean
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface OssConfigSummary {
  current: string
  providers: OssProviderOption[]
  configs: OssStorageConfig[]
}

export async function getOssFileList(params: { page: number, size: number, originalName?: string, service?: string }) {
  const res = await http.get<PageRes<OssFileItem[]>>('/admin/system/oss/list', {
    pageNum: params.page,
    pageSize: params.size,
    originalName: params.originalName,
    service: params.service
  })
  return { ...res, data: { records: res.data.records.map((item) => ({ ...item, ossId: String(item.ossId) })), total: res.data.total } }
}

export function uploadOssFile(file: File) {
  const data = new FormData()
  data.append('file', file)
  return http.post<{ url: string, fileName: string }>('/admin/system/oss/upload', data, { timeout: 120000 })
}

export function downloadOssFile(item: OssFileItem) {
  return http.download(`/admin/system/oss/download/${item.ossId}`, {
    method: 'get',
    fileName: item.originalName || item.fileName
  })
}

export function getOssConfig() {
  return http.get<OssConfigSummary>('/admin/system/oss/config')
}

export function getOssStorageConfig(provider: string) {
  return http.get<OssStorageConfig>(`/admin/system/oss/config/${provider}`)
}

export function saveOssStorageConfig(data: OssStorageConfig) {
  return http.put<OssStorageConfig>('/admin/system/oss/config', data)
}

export function switchOssStorage(type: string) {
  return http.put<unknown>('/admin/system/oss/config/switch', { type })
}

export function testOssStorage(type: string) {
  return http.post<{ type: string, endpoint: string }>('/admin/system/oss/config/test', { type })
}
