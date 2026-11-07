import http from '@/utils/http'

export interface ProtocolItem {
  id: string
  name: string
  description?: string
  state: number
  type: string
  configuration?: string
  version?: string
  fileName?: string
  url?: string
  jscript?: string
  expectedSourceHash?: string
  beanName?: string
  connectionProtocol?: string
  protocolMark?: string
  createTime?: string
  productName?: string
}

export interface ProtocolProductOption {
  productKey?: string
  id?: string
  name: string
}

export type ProtocolProductPage = PageRes<ProtocolProductOption[]> | ProtocolProductOption[]

export interface ProtocolCodecCandidate {
  className: string
  simpleName?: string
  productKey?: string
  desc?: string
}

export interface ProtocolJarUploadResult {
  url: string
  fileName?: string
  packageName?: string
  codecCandidates?: ProtocolCodecCandidate[]
}

export function getProtocolList(params: { page: number, size: number, id?: string, name?: string, state?: number }) {
  return http.get<PageRes<ProtocolItem[]>>('/admin/v1/protocol/list', { pageNum: params.page, pageSize: params.size, id: params.id, name: params.name, state: params.state })
}
export function getProtocol(id: string) {
  return http.get<ProtocolItem>(`/admin/v1/protocol/${id}`)
}

export function addProtocol(data: Partial<ProtocolItem>) {
  return http.post<unknown>('/admin/v1/protocol', data)
}

export function updateProtocol(data: Partial<ProtocolItem>) {
  return http.put<unknown>('/admin/v1/protocol', data)
}

export function deleteProtocols(ids: string[]) {
  return http.delete<unknown>(`/admin/v1/protocol/${ids.join(',')}`)
}

export function getProductsWithoutProtocol(params: { searchKey?: string, pageNum: number, pageSize: number }) {
  return http.get<ProtocolProductPage>('/admin/v1/protocol/products/without-protocol', params)
}

export function getAvailableCodecBeans() {
  return http.get<{ beanName: string, className: string, isRegistered?: boolean }[]>('/admin/v1/protocol/beans/available')
}

export function uploadProtocolJar(file: File) {
  const data = new FormData()
  data.append('file', file)
  return http.post<ProtocolJarUploadResult>('/admin/system/oss/upload', data, { timeout: 120000 })
}

export function previewProtocolCodec(data: object) {
  return http.post<unknown>('/admin/v1/protocol/codec/preview', data)
}
