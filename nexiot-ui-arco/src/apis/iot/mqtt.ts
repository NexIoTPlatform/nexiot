import type { ProductItem } from '@/apis/iot/product'
import http from '@/utils/http'

const TOPIC_SEPARATOR_PATTERN = /[,;]/

export type MqttNetworkType = 'MQTT_CLIENT' | 'MQTT_SERVER'
export type MqttTopicCategory = 'THING_MODEL' | 'PASSTHROUGH'

export interface MqttTopicMapping {
  topicPattern: string
  topic?: string
  topicCategory?: MqttTopicCategory
  qos: 0 | 1 | 2
  productKey?: string
  enabled: boolean
}

export interface MqttConfiguration {
  host?: string
  port?: number
  clientId?: string
  username?: string
  password?: string
  clusterEnabled?: boolean
  ssl?: boolean
  sslMode?: 'DISABLED' | 'SERVER_AUTH' | 'MUTUAL_AUTH'
  sslCaCertKey?: string
  sslClientCertKey?: string
  sslHostnameVerify?: boolean
  cleanSession?: boolean
  keepAliveInterval?: number
  connectTimeout?: number
  autoReconnect?: boolean
  defaultQos?: 0 | 1 | 2
  subscribeTopics?:
    | string
    | Array<string | MqttTopicMapping>
    | {
      thingTopics?: Record<string, string>
      passthroughTopics?: Record<string, string>
    }
  [key: string]: unknown
}

export interface MqttBoundProduct extends Partial<ProductItem> {
  id?: string
  productKey: string
  name: string
}

export interface MqttNetworkItem {
  id: string
  name: string
  unionId: string
  type: MqttNetworkType
  typeName?: string
  productKey?: string
  description?: string
  running?: boolean
  state?: number
  stateName?: string
  enableName?: string
  configuration?: string
  configObject?: MqttConfiguration
  createDate?: string
  createDateStr?: string
  createUser?: string
  bindProductCount?: number
  bindProducts?: MqttBoundProduct[]
}

export interface MqttListParams {
  page: number
  size: number
  type?: MqttNetworkType
  name?: string
  state?: number
  productKey?: string
}

export function getMqttNetworkList(params: MqttListParams) {
  return http.get<PageRes<MqttNetworkItem[]>>('/admin/network/list', {
    pageNum: params.page,
    pageSize: params.size,
    types: params.type ? undefined : ['MQTT_CLIENT', 'MQTT_SERVER'],
    type: params.type,
    name: params.name,
    state: params.state,
    productKey: params.productKey
  })
}

export function getMqttNetwork(id: string) {
  return http.get<MqttNetworkItem>(`/admin/network/${id}`)
}

export function addMqttNetwork(data: Partial<MqttNetworkItem>) {
  return http.post<unknown>('/admin/network', data)
}

export function updateMqttNetwork(data: Partial<MqttNetworkItem>) {
  return http.put<unknown>('/admin/network', data)
}

export function deleteMqttNetworks(ids: string[]) {
  return ids.length === 1
    ? http.delete<unknown>(`/admin/network/${ids[0]}`)
    : http.delete<unknown>(`/admin/network/batch/${ids.join(',')}`)
}

export function startMqttNetwork(id: string) {
  return http.post<unknown>(`/admin/network/start/${id}`)
}

export function stopMqttNetwork(id: string) {
  return http.post<unknown>(`/admin/network/stop/${id}`)
}

export function restartMqttNetwork(id: string) {
  return http.post<unknown>(`/admin/network/restart/${id}`)
}

export function validateMqttNetwork(data: Partial<MqttNetworkItem>) {
  return http.post<unknown>('/admin/network/validate', data)
}

export function updateMqttProductBinding(id: string, networkUnionId?: string) {
  return http.post<unknown>('/admin/v1/product/updateNetworkUnionId', {
    id,
    networkUnionId: networkUnionId || ''
  })
}

export function parseMqttConfiguration(item?: Partial<MqttNetworkItem>) {
  if (item?.configObject) return { ...item.configObject }
  try {
    return JSON.parse(item?.configuration || '{}') as MqttConfiguration
  } catch {
    return {} as MqttConfiguration
  }
}

export function parseMqttTopicMappings(config: MqttConfiguration) {
  const topics = config.subscribeTopics
  if (!topics) return []
  if (typeof topics === 'string') {
    return topics
      .split(TOPIC_SEPARATOR_PATTERN)
      .map((topic) => topic.trim())
      .filter(Boolean)
      .map<MqttTopicMapping>((topicPattern) => ({
        topicPattern,
        qos: config.defaultQos ?? 0,
        enabled: true
      }))
  }
  if (!Array.isArray(topics)) {
    const legacyGroups: Array<{
      values?: Record<string, string>
      topicCategory: MqttTopicCategory
    }> = [
      { values: topics.thingTopics, topicCategory: 'THING_MODEL' },
      { values: topics.passthroughTopics, topicCategory: 'PASSTHROUGH' }
    ]
    return legacyGroups.flatMap(({ values, topicCategory }) =>
      Object.values(values || {})
        .filter((topicPattern) => Boolean(topicPattern?.trim()))
        .map<MqttTopicMapping>((topicPattern) => ({
          topicPattern,
          topicCategory,
          qos: config.defaultQos ?? 0,
          enabled: true
        })))
  }
  return topics
    .map<MqttTopicMapping>((topic) =>
      typeof topic === 'string'
        ? {
            topicPattern: topic,
            qos: config.defaultQos ?? 0,
            enabled: true
          }
        : {
            topicPattern: topic.topicPattern || topic.topic || '',
            topicCategory: topic.topicCategory,
            qos: topic.qos ?? config.defaultQos ?? 0,
            productKey: topic.productKey || undefined,
            enabled: topic.enabled ?? true
          })
    .filter((topic) => Boolean(topic.topicPattern))
}
