import http from '@/utils/http'

interface NexIoTMenu {
  menuId: string | number
  parentId: string | number
  menuName: string
  orderNum: number
  path?: string
  component?: string
  isFrame?: number
  isCache?: number
  menuType: 'M' | 'C' | 'F'
  visible?: '0' | '1'
  status?: '0' | '1'
  perms?: string
  icon?: string
  children?: NexIoTMenu[]
}

export interface ListItem {
  activeMenu: string
  alwaysShow: boolean
  breadcrumb: boolean
  children: ListItem[]
  component: string
  hidden: boolean
  icon: string
  id: string
  keepAlive: boolean
  parentId: string
  path: string
  permission: string
  redirect: string
  roles: string[]
  showInTabs: boolean
  sort: number
  status: Status
  title: string
  type: 1 | 2 | 3
  affix: boolean
  isExternalUrl: boolean
}

export interface MenuOptionsItem {
  id: string
  title: string
  children: MenuOptionsItem[]
}

interface NexIoTTreeItem {
  id: string | number
  label: string
  children?: NexIoTTreeItem[]
}

interface RoleMenuTreeResult {
  menus: NexIoTTreeItem[]
  checkedKeys: Array<string | number>
}

function toGiType(type: NexIoTMenu['menuType']): ListItem['type'] {
  return type === 'M' ? 1 : type === 'C' ? 2 : 3
}

function toNexIoTType(type: ListItem['type']): NexIoTMenu['menuType'] {
  return type === 1 ? 'M' : type === 2 ? 'C' : 'F'
}

function mapMenu(item: NexIoTMenu): ListItem {
  return {
    id: String(item.menuId),
    parentId: String(item.parentId ?? '0'),
    title: item.menuName,
    sort: item.orderNum ?? 0,
    path: item.path || '',
    component: item.component || '',
    type: toGiType(item.menuType),
    status: item.status === '1' ? '0' : '1',
    hidden: item.visible === '1',
    keepAlive: item.isCache === 0,
    permission: item.perms || '',
    icon: item.icon || '',
    children: item.children?.map(mapMenu) || [],
    isExternalUrl: item.isFrame === 0,
    activeMenu: '',
    alwaysShow: false,
    breadcrumb: true,
    redirect: '',
    roles: [],
    showInTabs: true,
    affix: false
  }
}

function toPayload(data: Partial<ListItem>) {
  return {
    menuId: data.id || undefined,
    parentId: data.parentId || '0',
    menuName: data.title,
    orderNum: data.sort,
    path: data.path,
    component: data.component,
    isFrame: data.isExternalUrl ? 0 : 1,
    isCache: data.keepAlive ? 0 : 1,
    menuType: data.type ? toNexIoTType(data.type) : 'M',
    visible: data.hidden ? '1' : '0',
    status: data.status === '0' ? '1' : '0',
    perms: data.permission,
    icon: data.icon
  }
}

function buildTree(items: ListItem[], parentId = '0'): ListItem[] {
  return items
    .filter((item) => item.parentId === parentId)
    .sort((a, b) => a.sort - b.sort)
    .map((item) => ({ ...item, children: buildTree(items, item.id) }))
}

function mapTreeOptions(items: NexIoTTreeItem[]): MenuOptionsItem[] {
  return items.map((item) => ({
    id: String(item.id),
    title: item.label,
    children: mapTreeOptions(item.children || [])
  }))
}

export async function getMenuList(params?: { name?: string, status?: Status }) {
  const res = await http.get<NexIoTMenu[]>('/admin/system/menu/list', {
    menuName: params?.name,
    status: params?.status === undefined ? undefined : params.status === '1' ? '0' : '1'
  })
  const menus = res.data.map(mapMenu)
  return { ...res, data: buildTree(menus) }
}

export async function getMenu(id: string) {
  const res = await http.get<NexIoTMenu>(`/admin/system/menu/${id}`)
  return { ...res, data: mapMenu(res.data) }
}

export function addMenu(data: Partial<ListItem>) {
  return http.post<unknown>('/admin/system/menu', toPayload(data))
}

export function updateMenu(data: Partial<ListItem>) {
  return http.put<unknown>('/admin/system/menu', toPayload(data))
}

export function deleteMenu(ids: string[]) {
  return http.delete<unknown>(`/admin/system/menu/${ids.join(',')}`)
}

export async function getMenuOptions() {
  const res = await http.get<NexIoTTreeItem[]>('/admin/system/menu/treeselect')
  return { ...res, data: mapTreeOptions(res.data) }
}

export async function getRoleMenuTree(roleId: string) {
  const res = await http.get<RoleMenuTreeResult>(`/admin/system/menu/roleMenuTreeselect/${roleId}`)
  return {
    ...res,
    data: {
      menus: mapTreeOptions(res.data.menus || []),
      checkedKeys: (res.data.checkedKeys || []).map(String)
    }
  }
}
