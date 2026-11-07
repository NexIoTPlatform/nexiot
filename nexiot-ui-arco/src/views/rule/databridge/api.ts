import type { ApplicationItem } from '@/apis/application/application'
import type { ProductItem } from '@/apis/iot/product'
import http from '@/utils/http'

export type DataDirection = 'INPUT' | 'OUTPUT' | 'BIDIRECTIONAL'
export type ResourceDirection = 'IN' | 'OUT' | 'BOTH'

export interface ResourceConnection {
  id?: string
  name: string
  type: string
  pluginType: string
  host?: string
  port?: number
  username?: string
  password?: string
  databaseName?: string
  extraConfig?: string
  status: number
  description?: string
  direction: ResourceDirection
  dataDirection: DataDirection
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export interface DataBridgeConfig {
  id?: string
  name: string
  sourceScope: 'ALL_PRODUCTS' | 'SPECIFIC_PRODUCTS' | 'APPLICATION'
  sourceProductKeys: string | string[]
  sourceProductNames?: (string | { name?: string, productKey?: string })[]
  sourceApplicationId?: string
  sourceApplicationName?: string
  targetResourceId?: string
  targetResourceName?: string
  bridgeType: string
  template: string
  magicScript?: string
  config?: string
  status: number
  description?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface ResourceTypeGroups {
  databases: Record<string, string>
  messageQueues: Record<string, string>
  timeSeries: Record<string, string>
  searchEngines: Record<string, string>
  cloudPlatforms: Record<string, string>
  others: Record<string, string>
  inputTypes: string[]
  outputTypes: string[]
  bidirectionalTypes: string[]
}

export interface PluginOption {
  value: string
  label: string
  category?: string
  dataDirection?: DataDirection
  description?: string
  icon?: string
}

export interface DynamicFieldConfig {
  type?: 'input' | 'password' | 'number' | 'select' | 'switch' | 'textarea'
  required?: boolean
  placeholder?: string
  default?: unknown
  min?: number
  max?: number
  options?: (string | { label: string, value: unknown })[]
}

export interface DynamicField {
  field: string
  label: string
  config: DynamicFieldConfig
}

interface NexIoTDictData {
  dictLabel: string
  dictValue: string
  remark?: string
}

export function getResourceConnections() {
  return http.get<ResourceConnection[]>('/databridge/resources')
}

export function getResourceConnection(id: string) {
  return http.get<ResourceConnection>(`/databridge/resources/${id}`)
}

export function addResourceConnection(data: ResourceConnection) {
  return http.post<string>('/databridge/resources', data)
}

export function updateResourceConnection(id: string, data: ResourceConnection) {
  return http.put<unknown>(`/databridge/resources/${id}`, data)
}

export function updateResourceConnectionStatus(id: string, status: number) {
  return http.put<unknown>(`/databridge/resources/${id}/status`, undefined, { params: { status } })
}

export function deleteResourceConnection(id: string) {
  return http.delete<unknown>(`/databridge/resources/${id}`)
}

export function testSavedResourceConnection(id: string) {
  return http.post<unknown>(`/databridge/resources/${id}/test`)
}

export function testResourceConnection(data: Partial<ResourceConnection>) {
  return http.post<unknown>('/databridge/resources/test', data)
}

export function getResourceTypes(direction?: DataDirection) {
  return http.get<ResourceTypeGroups>('/databridge/resources/types', { direction })
}

export function getPluginOptions(resourceType: string) {
  return http.get<PluginOption[]>(`/databridge/resource-types/${resourceType}/plugin-types`)
}

export async function getResourceFields(resourceType: string) {
  const type = `databridge_${resourceType.toLowerCase()}_fields`
  const res = await http.get<Record<string, NexIoTDictData[]>>(`/admin/system/dict/data/types/${type}`)
  const fields = (res.data[type] || []).map<DynamicField>((item) => {
    let config: DynamicFieldConfig = {}
    try {
      config = item.remark ? JSON.parse(item.remark) : {}
    } catch {
      config = {}
    }
    return { field: item.dictValue, label: item.dictLabel, config }
  })
  return { ...res, data: fields }
}

export function getDataBridgeConfigs() {
  return http.get<DataBridgeConfig[]>('/databridge/configs')
}

export function getDataBridgeConfig(id: string) {
  return http.get<DataBridgeConfig>(`/databridge/configs/${id}`)
}

export function addDataBridgeConfig(data: DataBridgeConfig) {
  return http.post<string>('/databridge/configs', data)
}

export function updateDataBridgeConfig(id: string, data: DataBridgeConfig) {
  return http.put<unknown>(`/databridge/configs/${id}`, data)
}

export function updateDataBridgeConfigStatus(id: string, status: number) {
  return http.put<unknown>(`/databridge/configs/${id}/status`, undefined, { params: { status } })
}

export function deleteDataBridgeConfig(id: string) {
  return http.delete<unknown>(`/databridge/configs/${id}`)
}

export function validateDataBridgeConfig(id: string) {
  return http.post<boolean>(`/databridge/configs/${id}/validate`)
}

export function getDataBridgeProducts() {
  return http.get<PageRes<ProductItem[]>>('/admin/v1/product/v4/list', { pageNum: 1, pageSize: 99999, state: 0 })
}

export function getDataBridgeApplications() {
  return http.get<PageRes<ApplicationItem[]>>('/admin/v1/apply/list', { pageNum: 1, pageSize: 99999 })
}
