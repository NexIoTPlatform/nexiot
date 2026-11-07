import http from '@/utils/http'

interface NexIoTDept {
  deptId: string | number
  parentId: string | number
  deptName: string
  orderNum: number
  leader?: string
  phone?: string
  email?: string
  status: '0' | '1'
  createTime?: string
  children?: NexIoTDept[]
}

export interface ListItem {
  id: string
  name: string
  sort: number
  status: Status
  createTime: string
  parentId: string
  leader: string
  phone: string
  email: string
  children?: ListItem[]
}

function toGiStatus(status: '0' | '1'): Status {
  return status === '0' ? '1' : '0'
}

function toNexIoTStatus(status: Status): '0' | '1' {
  return status === '1' ? '0' : '1'
}

function mapDept(item: NexIoTDept): ListItem {
  return {
    id: String(item.deptId),
    parentId: String(item.parentId ?? ''),
    name: item.deptName,
    sort: item.orderNum,
    status: toGiStatus(item.status),
    createTime: item.createTime || '',
    leader: item.leader || '',
    phone: item.phone || '',
    email: item.email || '',
    children: item.children?.map(mapDept)
  }
}

function toPayload(data: Partial<ListItem>) {
  return {
    deptId: data.id || undefined,
    parentId: data.parentId,
    deptName: data.name,
    orderNum: data.sort,
    leader: data.leader,
    phone: data.phone,
    email: data.email,
    status: data.status ? toNexIoTStatus(data.status) : '0'
  }
}

export async function getDeptList(params?: { name?: string, status?: Status }) {
  const res = await http.get<NexIoTDept[]>('/admin/system/dept/list', {
    deptName: params?.name,
    status: params?.status ? toNexIoTStatus(params.status) : undefined
  })
  return { ...res, data: res.data.map(mapDept) }
}

export async function getDeptOptions(tenantId?: string) {
  const res = await http.get<Array<NexIoTDept & { id?: string | number, label?: string }>>('/admin/system/dept/treeselect', { tenantId })
  const mapOption = (item: NexIoTDept & { id?: string | number, label?: string }): ListItem => ({
    id: String(item.id ?? item.deptId),
    parentId: String(item.parentId ?? ''),
    name: item.label || item.deptName,
    sort: item.orderNum ?? 0,
    status: '1',
    createTime: '',
    leader: '',
    phone: '',
    email: '',
    children: item.children?.map(mapOption)
  })
  return { ...res, data: res.data.map(mapOption) }
}

export async function getDept(id: string) {
  const res = await http.get<NexIoTDept>(`/admin/system/dept/${id}`)
  return { ...res, data: mapDept(res.data) }
}

export function addDept(data: Omit<ListItem, 'id' | 'createTime' | 'children'>) {
  return http.post<unknown>('/admin/system/dept', toPayload(data))
}

export function updateDept(data: Omit<ListItem, 'createTime' | 'children'>) {
  return http.put<unknown>('/admin/system/dept', toPayload(data))
}

export function deleteDept(ids: string[]) {
  return http.delete<unknown>(`/admin/system/dept/${ids.join(',')}`)
}
