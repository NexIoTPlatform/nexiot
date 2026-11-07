import { getNoticeTemplates } from '@/apis/application/notice'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem, ProductMetadataItem } from '@/apis/iot/product'
import http from '@/utils/http'

export interface SceneFilter {
  key?: string
  operator?: string
  value?: unknown
}

export interface SceneTrigger {
  trigger?: 'device' | 'timer' | 'manual'
  cron?: string
  deviceId?: string
  deviceName?: string
  productKey?: string
  type?: 'properties' | 'event'
  modelId?: string
  filters?: SceneFilter[]
}

export interface SceneActionParam extends ProductMetadataItem {
  params?: unknown
}

export interface SceneAction {
  trigger?: 'device' | 'notice'
  type?: 'functions'
  deviceId?: string
  deviceName?: string
  productKey?: string
  modelId?: string
  execData?: SceneActionParam[]
  noticeTemplateId?: string
  noticeTemplateName?: string
  receivers?: string
  filters?: SceneFilter[]
}

export interface SceneLinkage {
  id?: string
  sceneName: string
  touch: 'one' | 'all'
  triggerCondition: string | SceneTrigger[]
  execAction: string | SceneAction[]
  sleepCycle: number
  status: number
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface SceneLogAction {
  trigger?: string
  target?: string
  targetName?: string
  param?: unknown
  result?: unknown
  success?: boolean
}

export interface SceneLog {
  id: string
  createTime?: string
  updateTime?: string
  createBy?: string
  content?: string
  sceneName?: string
  touch?: string
  triggerCondition?: string
  execAction?: string
  sceneStatus?: number
  triggerType?: string
  cname?: string
  cstatus?: number
  cid?: string
  ctype?: number
  cdeviceMeta?: string
}

export interface ProductMetadata {
  properties: ProductMetadataItem[]
  events: ProductMetadataItem[]
  functions: ProductMetadataItem[]
}

export function getSceneLinkages(params: { page: number, size: number, sceneName?: string, triggerCondition?: string, status?: number }) {
  return http.get<PageRes<SceneLinkage[]>>('/admin/v1/scene/linkage/list', {
    pageNum: params.page,
    pageSize: params.size,
    sceneName: params.sceneName,
    triggerCondition: params.triggerCondition,
    status: params.status
  })
}

export function getSceneLinkage(id: string) {
  return http.get<SceneLinkage>(`/admin/v1/scene/linkage/${id}`)
}

export function addSceneLinkage(data: SceneLinkage) {
  return http.post<unknown>('/admin/v1/scene/linkage', data)
}

export function updateSceneLinkage(data: Partial<SceneLinkage>) {
  return http.put<unknown>('/admin/v1/scene/linkage', data)
}

export function deleteSceneLinkage(id: string) {
  return http.delete<unknown>(`/admin/v1/scene/linkage/${id}`)
}

export function executeSceneLinkage(id: string) {
  return http.get<unknown>(`/admin/v1/scene/linkage/exec/${id}`)
}

export function getSceneLogs(id: string, params: { page: number, size: number }) {
  return http.get<PageRes<SceneLog[]>>(`/admin/v1/scene/linkage/log/${id}`, { pageNum: params.page, pageSize: params.size })
}

export function getSceneLogPage(params: {
  page: number
  size: number
  cName?: string
  cStatus?: number
  cId?: string
  cType?: number
  triggerType?: string
  createTimeStart?: string
  createTimeEnd?: string
}) {
  return http.get<PageRes<SceneLog[]>>('/admin/v1/scene/linkage/log/page', {
    pageNum: params.page,
    pageSize: params.size,
    cName: params.cName,
    cStatus: params.cStatus,
    cId: params.cId,
    cType: params.cType,
    triggerType: params.triggerType,
    createTimeStart: params.createTimeStart,
    createTimeEnd: params.createTimeEnd
  })
}

export function getSceneProducts(name?: string) {
  return http.get<PageRes<ProductItem[]>>('/admin/v1/product/v4/list', { pageNum: 1, pageSize: 100, name })
}

export function getSceneDevices(params: { page: number, size: number, productKey?: string, deviceName?: string, deviceId?: string }) {
  return http.get<PageRes<DeviceItem[]>>('/admin/v1/device/list', {
    pageNum: params.page,
    pageSize: params.size,
    productKey: params.productKey,
    deviceName: params.deviceName,
    deviceId: params.deviceId
  })
}

export async function getSceneProductMetadata(productKey: string) {
  const res = await http.get<unknown>(`/admin/v1/product/metadata/${productKey}`)
  const raw = (res.data ?? res.message) as { msg?: string } | string | ProductMetadata
  let data: ProductMetadata = { properties: [], events: [], functions: [] }
  try {
    if (typeof raw === 'string') data = JSON.parse(raw)
    else if (raw && typeof raw === 'object' && 'msg' in raw && raw.msg) data = JSON.parse(raw.msg)
    else data = raw as ProductMetadata
  } catch {
    data = { properties: [], events: [], functions: [] }
  }
  return { ...res, data }
}

export function getSceneNoticeTemplates(params: { page: number, size: number, name?: string }) {
  return getNoticeTemplates(params)
}
