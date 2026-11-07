import type { MenuOptionsItem } from '@/apis/system/menu'
import http from '@/utils/http'

interface NexIoTTenantPackage {
  packageId: string | number
  packageName: string
  menuCount?: number
  tenantCount?: number
  status: 0 | 1
  createTime?: string
  remark?: string
  menuIds?: Array<string | number>
}

interface NexIoTTreeItem {
  id: string | number
  label: string
  children?: NexIoTTreeItem[]
}

export interface TenantPackageItem {
  id: string
  name: string
  menuCount: number
  tenantCount: number
  status: 0 | 1
  createTime: string
  remark: string
  menuIds: string[]
}

function mapPackage(item: NexIoTTenantPackage): TenantPackageItem {
  return {
    id: String(item.packageId),
    name: item.packageName,
    menuCount: item.menuCount || 0,
    tenantCount: item.tenantCount || 0,
    status: item.status,
    createTime: item.createTime || '',
    remark: item.remark || '',
    menuIds: (item.menuIds || []).map(String)
  }
}

function mapTree(items: NexIoTTreeItem[]): MenuOptionsItem[] {
  return items.map((item) => ({ id: String(item.id), title: item.label, children: mapTree(item.children || []) }))
}

function toPayload(data: Partial<TenantPackageItem>) {
  return {
    packageId: data.id || undefined,
    packageName: data.name,
    status: data.status ?? 0,
    remark: data.remark,
    menuIds: data.menuIds || []
  }
}

export async function getTenantPackageList(params: { page: number, size: number, name?: string, status?: number }) {
  const res = await http.get<PageRes<NexIoTTenantPackage[]>>('/admin/system/tenant-package/list', {
    pageNum: params.page,
    pageSize: params.size,
    packageName: params.name,
    status: params.status
  })
  return { ...res, data: { records: res.data.records.map(mapPackage), total: res.data.total } }
}

export async function getTenantPackage(id: string) {
  const res = await http.get<NexIoTTenantPackage>(`/admin/system/tenant-package/${id}`)
  return { ...res, data: mapPackage(res.data) }
}

export function addTenantPackage(data: Partial<TenantPackageItem>) {
  return http.post<unknown>('/admin/system/tenant-package', toPayload(data))
}

export function updateTenantPackage(data: Partial<TenantPackageItem>) {
  return http.put<unknown>('/admin/system/tenant-package', toPayload(data))
}

export function deleteTenantPackage(ids: string[]) {
  return http.delete<unknown>(`/admin/system/tenant-package/${ids.join(',')}`)
}

export function changeTenantPackageStatus(data: Pick<TenantPackageItem, 'id' | 'status'>) {
  return http.put<unknown>('/admin/system/tenant-package/changeStatus', { packageId: data.id, status: data.status })
}

export async function getTenantPackageMenuTree() {
  const res = await http.get<NexIoTTreeItem[]>('/admin/system/tenant-package/menu-treeselect')
  return { ...res, data: mapTree(res.data) }
}
