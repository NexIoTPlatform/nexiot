import type * as T from './type'
import JSEncrypt from 'jsencrypt'
import { resolveApiResourceUrl } from '@/utils'
import http from '@/utils/http'

export type * from './type'

interface NexIoTUserInfo {
  user: {
    id?: string
    unionId?: string
    username: string
    alias?: string
    avatar?: string
    admin?: boolean
    tenantId?: string
    tenantCode?: string
    tenantName?: string
    tenantRole?: string
  }
  roles?: string[]
  permissions?: string[]
  tenantId?: string
  tenantCode?: string
  tenantName?: string
  tenantRole?: string
}

export interface LoginParams {
  username: string
  password: string
  code?: string
  uuid?: string
  rsaKey?: string
  tenantCode?: string
}

export function login(data: LoginParams) {
  const grantType = import.meta.env.VITE_LOGIN_GRANT || 'password'
  let password = data.password
  if (grantType === 'captcha') {
    if (!data.rsaKey) throw new Error('登录密钥未加载，请刷新验证码后重试')
    const encryptor = new JSEncrypt()
    encryptor.setPublicKey(data.rsaKey)
    const encryptedPassword = encryptor.encrypt(password)
    if (!encryptedPassword) throw new Error('密码加密失败，请刷新验证码后重试')
    password = encryptedPassword
  }
  const form = new URLSearchParams({
    username: data.username,
    password,
    grant_type: grantType,
    client_id: 'web',
    client_secret: 'web'
  })
  if (data.code) form.set('code', data.code)
  if (data.uuid) form.set('uuid', data.uuid)
  if (data.tenantCode) form.set('tenantCode', data.tenantCode)
  return http.request<T.Login>({
    url: '/oauth/token',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

export function getCaptcha() {
  return http.get<{ uuid: string, img: string, key?: string }>('/getCaptchaCode')
}

export function logout() {
  return http.get<unknown>('/admin/logout')
}

export async function getUserInfo() {
  const res = await http.get<NexIoTUserInfo>('/admin/getInfo')
  const { user } = res.data
  const data: T.UserInfo = {
    id: String(user.id || user.unionId || ''),
    nickname: user.alias || user.username,
    avatar: resolveApiResourceUrl(user.avatar),
    roles: res.data.roles?.length ? res.data.roles : ['ROLE_DEFAULT'],
    permissions: res.data.permissions || [],
    tenantId: res.data.tenantId || user.tenantId || '',
    tenantCode: res.data.tenantCode || user.tenantCode || '',
    tenantName: res.data.tenantName || user.tenantName || '',
    tenantRole: res.data.tenantRole || user.tenantRole || '',
    currentAdmin: Boolean(user.admin)
  }
  return { ...res, data }
}

function mapRoute(item: T.NexIoTMenuItem, index: number): T.UserRouteItem {
  const meta = item.meta || {}
  return {
    id: String(item.id || `${item.path}-${index}`),
    parentId: String(item.parentId || ''),
    path: item.path,
    component: item.component || '',
    redirect: item.redirect || '',
    title: meta.title || item.name || item.path,
    icon: meta.icon || '',
    hidden: Boolean(item.hidden || meta.hidden),
    keepAlive: !meta.noCache,
    activeMenu: meta.selected || '',
    alwaysShow: Boolean(item.children?.length),
    breadcrumb: true,
    showInTabs: true,
    sort: item.orderNum ?? index,
    status: '1',
    permission: item.name || '',
    roles: [],
    type: item.children?.length ? 1 : 2,
    affix: false,
    children: (item.children || []).map(mapRoute)
  }
}

export async function getUserRoutes() {
  const res = await http.get<T.NexIoTMenuItem[]>('/admin/getRouters')
  return { ...res, data: res.data.map(mapRoute) }
}
