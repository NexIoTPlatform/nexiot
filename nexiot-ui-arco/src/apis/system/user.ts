import http from '@/utils/http'

interface NexIoTUser {
  id: string | number
  unionId?: string
  username: string
  alias?: string
  mobile?: string
  email?: string
  avatar?: string
  deptId?: string | number
  deptName?: string
  tenantId?: string
  tenantRole?: string
  identity?: number
  status: number | string
  remark?: string
  createBy?: string
  createDate?: string
  roleIds?: Array<string | number>
  roleNames?: string[]
}

interface NexIoTUserDetail {
  data?: NexIoTUser
  roles?: NexIoTRole[]
  roleIds?: Array<string | number>
  createMode?: 'TENANT_ADMIN' | 'TENANT_MEMBER'
  canManageUsers?: boolean
  currentTenantId?: string
}

interface NexIoTRole {
  roleId: string | number
  roleName: string
  roleKey?: string
  createTime?: string
  flag?: boolean
}

export interface UserRoleOption {
  id: string
  name: string
  code: string
  createTime: string
  assigned: boolean
}

export interface UserFormContext {
  user?: ListItem
  roles: UserRoleOption[]
  createMode: 'TENANT_ADMIN' | 'TENANT_MEMBER'
  canManageUsers: boolean
  currentTenantId: string
}

export interface ListItem {
  id: string
  unionId: string
  createUserString: string
  createTime: string
  disabled: boolean
  deptId: string
  deptName: string
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  gender: Gender
  status: Status
  type: 1 | 2
  description: string
  tenantRole: string
  tenantId: string
  platformAdmin: boolean
  roleIds: string[]
  roleNames: string[]
  password?: string
}

function toGiStatus(status: number | string): Status {
  return String(status) === '0' ? '1' : '0'
}

function toNexIoTStatus(status: Status) {
  return status === '1' ? '0' : '1'
}

function mapUser(item: NexIoTUser, detail?: NexIoTUserDetail): ListItem {
  return {
    id: String(item.id),
    unionId: item.unionId || item.username,
    username: item.username,
    nickname: item.alias || item.username,
    phone: item.mobile || '',
    gender: '3',
    email: item.email || '',
    avatar: item.avatar || '',
    deptId: String(item.deptId || ''),
    deptName: item.deptName || '',
    tenantRole: item.tenantRole || '',
    tenantId: item.tenantId || '',
    status: toGiStatus(item.status),
    type: item.identity === 0 ? 1 : 2,
    description: item.remark || '',
    createUserString: item.createBy || '',
    createTime: item.createDate || '',
    disabled: item.identity === 0,
    platformAdmin: item.identity === 0,
    roleIds: (detail?.roleIds || item.roleIds || []).map(String),
    roleNames: item.roleNames || detail?.roles?.map((role) => role.roleName) || []
  }
}

function toPayload(data: Partial<ListItem>) {
  return {
    id: data.id || undefined,
    unionId: data.unionId || undefined,
    username: data.username,
    alias: data.nickname,
    mobile: data.phone,
    email: data.email,
    deptId: data.deptId || undefined,
    status: data.status ? toNexIoTStatus(data.status) : '0',
    remark: data.description,
    roleIds: data.roleIds,
    password: data.password || undefined
  }
}

export async function getUserList(params: { page: number, size: number, username?: string, status?: Status, deptId?: string }) {
  const res = await http.get<PageRes<NexIoTUser[]>>('/admin/system/user/list', {
    pageNum: params.page,
    pageSize: params.size,
    username: params.username,
    status: params.status ? toNexIoTStatus(params.status) : undefined,
    deptId: params.deptId
  })
  return { ...res, data: { records: res.data.records.map((item) => mapUser(item)), total: res.data.total } }
}

export async function getUserContext(id?: string) {
  const res = await http.get<NexIoTUserDetail | NexIoTUser>(`/admin/system/user/${id || ''}`)
  const detail = res.data as NexIoTUserDetail
  const user = detail.data || (id ? res.data as NexIoTUser : undefined)
  const data: UserFormContext = {
    user: user ? mapUser(user, detail) : undefined,
    roles: (detail.roles || []).map((role) => ({
      id: String(role.roleId),
      name: role.roleName,
      code: role.roleKey || '',
      createTime: role.createTime || '',
      assigned: Boolean(role.flag)
    })),
    createMode: detail.createMode || 'TENANT_MEMBER',
    canManageUsers: Boolean(detail.canManageUsers),
    currentTenantId: detail.currentTenantId || ''
  }
  return { ...res, data }
}

export async function getUser(id: string) {
  const res = await getUserContext(id)
  if (!res.data.user) throw new Error('用户不存在')
  return { ...res, data: res.data.user }
}

export function addUser(data: Partial<ListItem>) {
  return http.post<unknown>('/admin/system/user', toPayload(data))
}

export function updateUser(data: Partial<ListItem>) {
  return http.put<unknown>('/admin/system/user', toPayload(data))
}

export function deleteUser(ids: string[]) {
  return http.delete<unknown>(`/admin/system/user/${ids.join(',')}`)
}

export function changeUserStatus(data: Pick<ListItem, 'id' | 'status' | 'username' | 'phone'>) {
  return http.put<unknown>('/admin/system/user', toPayload(data))
}

export function resetUserPassword(id: string, password: string) {
  return http.put<unknown>('/admin/system/user/resetPwd', { id, password })
}

export async function getUserAuthRoles(id: string) {
  const res = await http.get<{ user: NexIoTUser, roles: NexIoTRole[] }>(`/admin/system/user/authRole/${id}`)
  return {
    ...res,
    data: {
      user: mapUser(res.data.user),
      roles: res.data.roles.map((role) => ({
        id: String(role.roleId),
        name: role.roleName,
        code: role.roleKey || '',
        createTime: role.createTime || '',
        assigned: Boolean(role.flag)
      }))
    }
  }
}

export function updateUserAuthRoles(unionId: string, roleIds: string[]) {
  return http.put<unknown>('/admin/system/user/authRole', undefined, { params: { unionId, roleIds } })
}

export function exportUsers(params?: { username?: string, status?: Status, deptId?: string }) {
  return http.download('/admin/system/user/export', {
    params: {
      username: params?.username,
      status: params?.status ? toNexIoTStatus(params.status) : undefined,
      deptId: params?.deptId
    },
    fileName: `用户数据_${Date.now()}.xlsx`
  })
}

export function downloadUserImportTemplate() {
  return http.download('/admin/system/user/importTemplate', {
    fileName: `用户导入模板_${Date.now()}.xlsx`
  })
}

export function importUsers(data: FormData) {
  return http.post<unknown>('/admin/system/user/importData', data, { timeout: 120000 })
}
