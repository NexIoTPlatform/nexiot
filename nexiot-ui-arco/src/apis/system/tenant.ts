import http from '@/utils/http'

export interface TenantListItem {
  tenantId: string
  tenantName: string
  tenantCode: string
  domain: string
  packageId: string
  packageName: string
  accountCount: number
  userCount: number
  adminAlias: string
  adminUsername: string
  adminMobile: string
  expireTime?: string
  status: 0 | 1
  loginTemplate?: string
  cfg?: string
  remark?: string
}

export interface TenantForm extends Omit<TenantListItem, 'domain' | 'packageName' | 'userCount'> {
  domain: string[]
  adminPassword?: string
}

export interface TenantPackageOption {
  packageId: string
  packageName: string
}

export interface TenantQuery {
  page: number
  size: number
  tenantName?: string
  tenantCode?: string
  domain?: string
  status?: number
}

export function getTenantList(params: TenantQuery) {
  const { page, size, ...filters } = params
  return http.get<PageRes<TenantListItem[]>>('/admin/system/tenant/list', {
    ...filters,
    pageNum: page,
    pageSize: size
  })
}

export function getTenant(tenantId: string) {
  return http.get<TenantListItem>(`/admin/system/tenant/${tenantId}`)
}

export function addTenant(data: Record<string, unknown>) {
  return http.post<unknown>('/admin/system/tenant', data)
}

export function updateTenant(data: Record<string, unknown>) {
  return http.put<unknown>('/admin/system/tenant', data)
}

export function changeTenantStatus(data: Pick<TenantListItem, 'tenantId' | 'status'>) {
  return http.put<unknown>('/admin/system/tenant/changeStatus', data)
}

export function deleteTenant(tenantIds: string[]) {
  return http.delete<unknown>(`/admin/system/tenant/${tenantIds.join(',')}`)
}

export function getTenantPackageOptions() {
  return http.get<TenantPackageOption[]>('/admin/system/tenant-package/simple-list')
}
