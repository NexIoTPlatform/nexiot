import http from '@/utils/http'

interface NexIoTConfig {
  configId: string | number
  configName: string
  configKey: string
  configValue: string
  configType: 'Y' | 'N'
  remark?: string
  createTime?: string
}

export interface ConfigItem {
  id: string
  name: string
  key: string
  value: string
  system: boolean
  remark: string
  createTime: string
}

function mapConfig(item: NexIoTConfig): ConfigItem {
  return {
    id: String(item.configId),
    name: item.configName,
    key: item.configKey,
    value: item.configValue,
    system: item.configType === 'Y',
    remark: item.remark || '',
    createTime: item.createTime || ''
  }
}

function toPayload(data: Partial<ConfigItem>) {
  return {
    configId: data.id || undefined,
    configName: data.name,
    configKey: data.key,
    configValue: data.value,
    configType: data.system ? 'Y' : 'N',
    remark: data.remark
  }
}

export async function getConfigList(params: { page: number, size: number, name?: string, key?: string, system?: boolean }) {
  const res = await http.get<PageRes<NexIoTConfig[]>>('/admin/system/config/list', {
    pageNum: params.page,
    pageSize: params.size,
    configName: params.name,
    configKey: params.key,
    configType: params.system === undefined ? undefined : params.system ? 'Y' : 'N'
  })
  return { ...res, data: { records: res.data.records.map(mapConfig), total: res.data.total } }
}

export async function getConfig(id: string) {
  const res = await http.get<NexIoTConfig>(`/admin/system/config/${id}`)
  return { ...res, data: mapConfig(res.data) }
}

export function addConfig(data: Partial<ConfigItem>) {
  return http.post<unknown>('/admin/system/config', toPayload(data))
}

export function updateConfig(data: Partial<ConfigItem>) {
  return http.put<unknown>('/admin/system/config', toPayload(data))
}

export function deleteConfig(ids: string[]) {
  return http.delete<unknown>(`/admin/system/config/${ids.join(',')}`)
}

export function refreshConfigCache() {
  return http.delete<unknown>('/admin/system/config/refreshCache')
}
