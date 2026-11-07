import http from '@/utils/http'

interface NexIoTRole {
  roleId: string | number
  roleName: string
  roleKey: string
  roleSort: number
  status: '0' | '1'
  remark?: string
  createBy?: string
  createTime?: string
  dataScope?: string
  roleType?: string
  menuCheckStrictly?: boolean
  deptCheckStrictly?: boolean
  menuIds?: Array<string | number>
  deptIds?: Array<string | number>
}

interface NexIoTRoleUser {
  id?: string | number
  unionId: string
  username: string
  alias?: string
  email?: string
  mobile?: string
  status: number | string
  createDate?: string
}

interface NexIoTDeptTreeItem {
  id: string | number
  label: string
  children?: NexIoTDeptTreeItem[]
}

export interface RoleUserItem {
  id: string
  unionId: string
  username: string
  nickname: string
  email: string
  phone: string
  status: Status
  createTime: string
}

export interface DeptTreeItem {
  id: string
  title: string
  children: DeptTreeItem[]
}

export interface ListItem {
  id: string
  createUserString: string
  createTime: string
  disabled: boolean
  name: string
  code: string
  sort: number
  status: Status
  type: number
  description: string
  dataScope: string
  roleType: string
  menuCheckStrictly: boolean
  deptCheckStrictly: boolean
  menuIds: string[]
}

function mapRole(item: NexIoTRole): ListItem {
  return {
    id: String(item.roleId),
    createUserString: item.createBy || '',
    createTime: item.createTime || '',
    disabled: String(item.roleId) === '1',
    name: item.roleName,
    code: item.roleKey,
    sort: item.roleSort ?? 0,
    status: item.status === '0' ? '1' : '0',
    type: 2,
    description: item.remark || '',
    dataScope: item.dataScope || '1',
    roleType: item.roleType || '',
    menuCheckStrictly: Boolean(item.menuCheckStrictly),
    deptCheckStrictly: Boolean(item.deptCheckStrictly),
    menuIds: (item.menuIds || []).map(String)
  }
}

function toPayload(data: Partial<ListItem>) {
  return {
    roleId: data.id || undefined,
    roleName: data.name,
    roleKey: data.code,
    roleSort: data.sort ?? 0,
    status: data.status === '0' ? '1' : '0',
    remark: data.description,
    dataScope: data.dataScope || '1',
    menuCheckStrictly: data.menuCheckStrictly ?? false,
    deptCheckStrictly: data.deptCheckStrictly ?? false,
    menuIds: data.menuIds || []
  }
}

export async function getRoleList(params: { page: number, size: number, name?: string, status?: Status }) {
  const res = await http.get<PageRes<NexIoTRole[]>>('/admin/system/role/list', {
    pageNum: params.page,
    pageSize: params.size,
    roleName: params.name,
    status: params.status === undefined ? undefined : params.status === '1' ? '0' : '1'
  })
  return { ...res, data: { records: res.data.records.map(mapRole), total: res.data.total } }
}

export async function getRole(id: string) {
  const res = await http.get<NexIoTRole>(`/admin/system/role/${id}`)
  return { ...res, data: mapRole(res.data) }
}

export function addRole(data: Partial<ListItem>) {
  return http.post<unknown>('/admin/system/role', toPayload(data))
}

export function updateRole(data: Partial<ListItem>) {
  return http.put<unknown>('/admin/system/role', toPayload(data))
}

export function deleteRole(ids: string[]) {
  return http.delete<unknown>(`/admin/system/role/${ids.join(',')}`)
}

export function changeRoleStatus(id: string, status: Status) {
  return http.put<unknown>('/admin/system/role/changeStatus', {
    roleId: id,
    status: status === '1' ? '0' : '1'
  })
}

export function updateRoleDataScope(data: Pick<ListItem, 'id' | 'dataScope' | 'deptCheckStrictly'> & { deptIds: string[] }) {
  return http.put<unknown>('/admin/system/role/dataScope', {
    roleId: data.id,
    dataScope: data.dataScope,
    deptCheckStrictly: data.deptCheckStrictly,
    deptIds: data.deptIds
  })
}

export async function getRoleDeptTree(roleId: string) {
  const res = await http.get<{ checkedKeys: Array<string | number>, depts: NexIoTDeptTreeItem[] }>(`/admin/system/dept/roleDeptTreeselect/${roleId}`)
  const mapTree = (items: NexIoTDeptTreeItem[]): DeptTreeItem[] => items.map((item) => ({
    id: String(item.id),
    title: item.label,
    children: mapTree(item.children || [])
  }))
  return { ...res, data: { checkedKeys: res.data.checkedKeys.map(String), depts: mapTree(res.data.depts || []) } }
}

function mapRoleUser(item: NexIoTRoleUser): RoleUserItem {
  return {
    id: String(item.id || item.unionId),
    unionId: item.unionId,
    username: item.username,
    nickname: item.alias || item.username,
    email: item.email || '',
    phone: item.mobile || '',
    status: String(item.status) === '0' ? '1' : '0',
    createTime: item.createDate || ''
  }
}

async function getRoleUserList(type: 'allocatedList' | 'unallocatedList', params: { page: number, size: number, roleId: string, username?: string, phone?: string }) {
  const res = await http.get<PageRes<NexIoTRoleUser[]>>(`/admin/system/role/authUser/${type}`, {
    pageNum: params.page,
    pageSize: params.size,
    roleId: params.roleId,
    username: params.username,
    mobile: params.phone
  })
  return { ...res, data: { records: res.data.records.map(mapRoleUser), total: res.data.total } }
}

export function getAllocatedRoleUsers(params: Parameters<typeof getRoleUserList>[1]) {
  return getRoleUserList('allocatedList', params)
}

export function getUnallocatedRoleUsers(params: Parameters<typeof getRoleUserList>[1]) {
  return getRoleUserList('unallocatedList', params)
}

export function cancelRoleUsers(roleId: string, unionIds: string[]) {
  return http.put<unknown>('/admin/system/role/authUser/cancelAll', undefined, { params: { roleId, unionIds } })
}

export function selectRoleUsers(roleId: string, unionIds: string[]) {
  return http.put<unknown>('/admin/system/role/authUser/selectAll', undefined, { params: { roleId, unionIds } })
}

export function exportRoles(params?: { name?: string, status?: Status }) {
  return http.download('/admin/system/role/export', {
    params: {
      roleName: params?.name,
      status: params?.status === undefined ? undefined : params.status === '1' ? '0' : '1'
    },
    fileName: `角色数据_${Date.now()}.xlsx`
  })
}

export async function updateRoleMenus(roleId: string, menuIds: string[]) {
  const detail = await getRole(roleId)
  return updateRole({ ...detail.data, menuIds })
}

export async function getRoleOptions() {
  const res = await getRoleList({ page: 1, size: 999 })
  return { ...res, data: res.data.records }
}
