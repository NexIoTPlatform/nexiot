import type { ProductItem } from '@/apis/iot/product'
import http from '@/utils/http'

export type NetworkType
  = | 'MQTT_CLIENT'
    | 'MQTT_SERVER'
    | 'TCP_CLIENT'
    | 'TCP_SERVER'
    | 'UDP'
    | 'HTTP_SERVER'

export interface NetworkConfiguration {
  host?: string
  port?: number
  clientId?: string
  username?: string
  password?: string
  productKey?: string
  ssl?: boolean
  sslMode?: string
  sslCaCertKey?: string
  sslClientCertKey?: string
  sslHostnameVerify?: boolean
  cleanSession?: boolean
  keepAliveInterval?: number
  connectTimeout?: number
  autoReconnect?: boolean
  defaultQos?: number
  subscribeTopics?: string
  parserType?: string
  decodeType?: string
  decoderType?: string
  delimiter?: string
  delimitedMaxlength?: number
  maxFrameLength?: number
  fixedLength?: number
  lengthFieldOffset?: number
  lengthFieldLength?: number
  lengthAdjustment?: number
  initialBytesToStrip?: number
  failFast?: boolean
  byteOrderLittle?: boolean
  readerIdleTime?: number
  writerIdleTime?: number
  allIdleTime?: number
  idleInterval?: number
  readTimeout?: number
  sendTimeout?: number
  keepAlive?: boolean
  reuseAddr?: boolean
  tcpNoDelay?: boolean
  allowInsert?: boolean
  alwaysPreDecode?: boolean
  onlyCache?: boolean
  preStore?: boolean
  [key: string]: unknown
}

export interface NetworkComponentItem {
  id: string
  name: string
  unionId: string
  type: NetworkType
  typeName?: string
  productKey?: string
  description?: string
  running?: boolean
  state?: number
  stateName?: string
  configuration?: string
  configObject?: NetworkConfiguration
  createDate?: string
  createDateStr?: string
  bindTcpServerProductCount?: number
  bindProductCount?: number
  bindTcpServerProducts?: ProductItem | ProductItem[]
  bindProducts?: ProductItem[]
}

export interface NetworkComponentParams {
  page: number
  size: number
  types?: NetworkType[]
  name?: string
  state?: number
  productKey?: string
}

export function getNetworkComponentList(params: NetworkComponentParams) {
  return http.get<PageRes<NetworkComponentItem[]>>('/admin/network/list', {
    pageNum: params.page,
    pageSize: params.size,
    types: params.types,
    name: params.name,
    state: params.state,
    productKey: params.productKey
  })
}

export function getNetworkComponent(id: string) {
  return http.get<NetworkComponentItem>(`/admin/network/${id}`)
}

export function getTcpPortPolicy() {
  return http.get<{ min: number, max: number, publicHost: string }>('/admin/network/tcp/port-policy')
}

export function addNetworkComponent(data: Partial<NetworkComponentItem>) {
  return http.post<unknown>('/admin/network', data)
}

export function updateNetworkComponent(data: Partial<NetworkComponentItem>) {
  return http.put<unknown>('/admin/network', data)
}

export function deleteNetworkComponents(ids: string[]) {
  return ids.length === 1
    ? http.delete<unknown>(`/admin/network/${ids[0]}`)
    : http.delete<unknown>(`/admin/network/batch/${ids.join(',')}`)
}

export function startNetworkComponent(id: string) {
  return http.post<unknown>(`/admin/network/start/${id}`)
}

export function stopNetworkComponent(id: string) {
  return http.post<unknown>(`/admin/network/stop/${id}`)
}

export function restartNetworkComponent(id: string) {
  return http.post<unknown>(`/admin/network/restart/${id}`)
}

export function validateNetworkComponent(data: Partial<NetworkComponentItem>) {
  return http.post<unknown>('/admin/network/validate', data)
}

export function getHttpRuntimeStatus(id: string) {
  return http.get<Record<string, unknown>>(
    `/admin/http-access/networks/${id}/status`
  )
}

export function runHttpPoll(id: string) {
  return http.post<Record<string, unknown>>(
    `/admin/http-access/networks/${id}/poll`
  )
}

export function validateHttpConfiguration(configuration: string) {
  return http.post<unknown>('/admin/http-access/networks/validate', {
    configuration
  })
}

export function reloadHttpNetwork(id: string) {
  return http.post<unknown>(`/admin/http-access/networks/${id}/reload`)
}

export function updateProductNetwork(id: string, networkUnionId?: string) {
  return http.post<unknown>('/admin/v1/product/updateNetworkUnionId', {
    id,
    networkUnionId: networkUnionId || ''
  })
}

export function getProductMqttPassword(productKey: string) {
  return http.post<Record<string, unknown>>(
    '/admin/v1/product/connect/mqtt/password',
    undefined,
    { params: { productKey } }
  )
}
