import type { DataDirection, ResourceConnection, ResourceDirection } from './api'
import Dayjs from 'dayjs'

export const resourceTypeColors: Record<string, string> = {
  MYSQL: 'blue',
  POSTGRESQL: 'cyan',
  H2: 'green',
  ORACLE: 'red',
  SQLSERVER: 'arcoblue',
  KAFKA: 'orange',
  MQTT: 'green',
  HTTP: 'magenta',
  IOTDB: 'purple',
  INFLUXDB: 'cyan',
  REDIS: 'red',
  ELASTICSEARCH: 'orangered',
  ALIYUN_IOT: 'gold',
  TENCENT_IOT: 'lime',
  HUAWEI_IOT: 'purple'
}

export const dataDirectionOptions = [
  { label: '数据输出', value: 'OUTPUT', description: 'IoT 平台数据推送到外部系统' },
  { label: '数据输入', value: 'INPUT', description: '外部系统数据导入 IoT 平台' },
  { label: '双向流转', value: 'BIDIRECTIONAL', description: '同时支持数据输入与输出' }
] as const

export function directionToLegacy(direction: DataDirection): ResourceDirection {
  return direction === 'INPUT' ? 'IN' : direction === 'BIDIRECTIONAL' ? 'BOTH' : 'OUT'
}

export function getDirectionLabel(direction?: DataDirection | ResourceDirection) {
  return {
    INPUT: '输入',
    OUTPUT: '输出',
    BIDIRECTIONAL: '双向',
    IN: '输入',
    OUT: '输出',
    BOTH: '双向'
  }[direction || ''] || '未知'
}

export function getDirectionColor(direction?: DataDirection | ResourceDirection) {
  return direction === 'INPUT' || direction === 'IN'
    ? 'arcoblue'
    : direction === 'OUTPUT' || direction === 'OUT'
      ? 'green'
      : 'orange'
}

export function parseJsonObject(value?: string) {
  if (!value) return {} as Record<string, unknown>
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown>
      : {}
  } catch {
    return {}
  }
}

export function getResourceAddress(resource: ResourceConnection) {
  const config = parseJsonObject(resource.extraConfig)
  const host = resource.host || String(config.host || config.baseUrl || '')
  const port = resource.port || config.port
  if (!host) return '未配置地址'
  return port ? `${host}:${port}` : host
}

export function getResourceTypeColor(type?: string) {
  return resourceTypeColors[type || ''] || 'arcoblue'
}

export function parseStringArray(value?: string | string[]) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return value.split(',').map((item) => item.trim()).filter(Boolean)
  }
}

export function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = Dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}
