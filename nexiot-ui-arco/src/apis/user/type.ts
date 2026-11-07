export interface UserInfo {
  id: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
  tenantId: string
  tenantCode: string
  tenantName: string
  tenantRole: string
  currentAdmin: boolean
}

export interface Login {
  access_token: string
  refresh_token: string
  expires_in: number
  token_type: string
}

export interface UserRouteItem {
  activeMenu: string
  alwaysShow: boolean
  breadcrumb: boolean
  children: UserRouteItem[]
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
}

export interface NexIoTMenuItem {
  id?: string | number
  parentId?: string | number
  path: string
  name?: string
  component?: string
  redirect?: string
  hidden?: boolean
  orderNum?: number
  children?: NexIoTMenuItem[]
  meta?: {
    title?: string
    icon?: string
    noCache?: boolean
    hidden?: boolean
    link?: string
    selected?: string
  }
}
