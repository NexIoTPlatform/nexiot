import type { SelectOptionData } from '@arco-design/web-vue'
import http from '@/utils/http'

interface NexIoTDictType {
  dictId: string | number
  dictName: string
  dictType: string
  status: '0' | '1'
  remark?: string
  createTime?: string
}

interface NexIoTDictData {
  dictCode: string | number
  dictLabel: string
  dictValue: string
  dictType: string
  dictSort: number
  status: '0' | '1'
  listClass?: string
  cssClass?: string
  remark?: string
  createTime?: string
}

export interface ListItem {
  id: string
  createTime: string
  name: string
  code: string
  status: Status
  description: string
}

export interface DictDataItem {
  id: string
  name: string
  value: string
  code: string
  sort: number
  status: Status
  color?: string
  remark?: string
  createTime?: string
}

function toGiStatus(status: '0' | '1'): Status {
  return status === '0' ? '1' : '0'
}

function toNexIoTStatus(status: Status): '0' | '1' {
  return status === '1' ? '0' : '1'
}

function mapType(item: NexIoTDictType): ListItem {
  return {
    id: String(item.dictId),
    name: item.dictName,
    code: item.dictType,
    status: toGiStatus(item.status),
    description: item.remark || '',
    createTime: item.createTime || ''
  }
}

function mapData(item: NexIoTDictData): DictDataItem {
  return {
    id: String(item.dictCode),
    name: item.dictLabel,
    value: item.dictValue,
    code: item.dictType,
    sort: item.dictSort,
    status: toGiStatus(item.status),
    color: item.listClass || item.cssClass || '',
    remark: item.remark,
    createTime: item.createTime
  }
}

export async function getDictTypeList(params: { page: number, size: number, name?: string, status?: Status }) {
  const res = await http.get<PageRes<NexIoTDictType[]>>('/admin/system/dict/type/list', {
    pageNum: params.page,
    pageSize: params.size,
    dictName: params.name,
    status: params.status === undefined ? undefined : toNexIoTStatus(params.status)
  })
  return { ...res, data: { records: res.data.records.map(mapType), total: res.data.total } }
}

export async function getDictType(id: string) {
  const res = await http.get<NexIoTDictType>(`/admin/system/dict/type/${id}`)
  return { ...res, data: mapType(res.data) }
}

export function addDictType(data: Omit<ListItem, 'id' | 'createTime'>) {
  return http.post<unknown>('/admin/system/dict/type', {
    dictName: data.name,
    dictType: data.code,
    status: toNexIoTStatus(data.status),
    remark: data.description
  })
}

export function updateDictType(data: Omit<ListItem, 'createTime'>) {
  return http.put<unknown>('/admin/system/dict/type', {
    dictId: data.id,
    dictName: data.name,
    dictType: data.code,
    status: toNexIoTStatus(data.status),
    remark: data.description
  })
}

export function deleteDictType(ids: string[]) {
  return http.delete<unknown>(`/admin/system/dict/type/${ids.join(',')}`)
}

export async function getDictDataList(params: { code: string, name?: string, status?: Status } & Pagination) {
  const res = await http.get<PageRes<NexIoTDictData[]>>('/admin/system/dict/data/list', {
    pageNum: params.page,
    pageSize: params.size,
    dictType: params.code,
    dictLabel: params.name,
    status: params.status === undefined ? undefined : toNexIoTStatus(params.status)
  })
  return { ...res, data: { records: res.data.records.map(mapData), total: res.data.total } }
}

export async function getDictDataDetail(params: { id: string }) {
  const res = await http.get<NexIoTDictData>(`/admin/system/dict/data/${params.id}`)
  return { ...res, data: mapData(res.data) }
}

export function addDictData(data: Omit<DictDataItem, 'id' | 'createTime'>) {
  return http.post<unknown>('/admin/system/dict/data', {
    dictLabel: data.name,
    dictValue: data.value,
    dictType: data.code,
    dictSort: data.sort,
    status: toNexIoTStatus(data.status),
    listClass: data.color,
    remark: data.remark
  })
}

export function updateDictData(data: Omit<DictDataItem, 'createTime'>) {
  return http.put<unknown>('/admin/system/dict/data', {
    dictCode: data.id,
    dictLabel: data.name,
    dictValue: data.value,
    dictType: data.code,
    dictSort: data.sort,
    status: toNexIoTStatus(data.status),
    listClass: data.color,
    remark: data.remark
  })
}

export function deleteDictData(ids: string[]) {
  return http.delete<unknown>(`/admin/system/dict/data/${ids.join(',')}`)
}

export async function getDictData() {
  const res = await http.get<Record<string, NexIoTDictData[]>>('/admin/system/dict/data/types/sys_normal_disable,sys_user_sex')
  const data = Object.fromEntries(Object.entries(res.data).map(([key, list]) => [
    key,
    list.map<SelectOptionData>((item) => ({
      label: item.dictLabel,
      value: key === 'sys_normal_disable' ? toGiStatus(item.dictValue as '0' | '1') : item.dictValue
    }))
  ]))
  data.STATUS = data.sys_normal_disable || []
  data.GENDER = data.sys_user_sex || []
  return { ...res, data }
}
