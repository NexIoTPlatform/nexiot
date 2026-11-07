/**
 * @file 路由状态管理模块
 * @description 处理动态路由的加载、格式化和状态管理
 */

import type { RouteRecordRaw } from 'vue-router'
import type { UserRouteItem } from '@/apis/user'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { constantRoutes } from '@/router'
import { transformPathToName } from '@/utils'

/**
 * 布局组件
 * 用于页面整体布局结构
 */
const Layout = () => import('@/layout/index.vue')
const ParentView = () => import('@/layout/components/ParentView.vue')
const MigrationPending = () => import('@/views/migration/pending.vue')
const VIEW_PREFIX_RE = /^\/?views\//
const VUE_SUFFIX_RE = /\.vue$/
const IOT_DOMAIN_RE
  = /^system\/(oss|ota|network|product|instance|protocol|sort|standard)(\/.*)?$/
const EXTERNAL_URL_RE = /^(?:https?:)?\/\//
const TRAILING_SLASH_RE = /\/$/

/**
 * 视图模块映射
 * 自动导入 views 目录下的所有 .vue 文件
 * 用于动态加载路由组件
 */
const modules = import.meta.glob('@/views/**/*.vue')

/**
 * 组件路径映射缓存
 * 将视图路径映射到对应的模块加载函数，避免每次遍历查找
 * key: 视图路径（如 'system/user/index'）
 * value: 模块加载函数
 */
const viewPathMap = new Map<string, () => Promise<any>>()

/**
 * 初始化组件路径映射
 * @description 在模块加载时构建路径映射表，提升查找效率
 */
function initViewPathMap() {
  if (viewPathMap.size > 0) return // 已初始化则跳过

  for (const path in modules) {
    // 提取视图路径：从 'src/views/system/user/index.vue' 提取 'system/user/index'
    const dir = path.split('views/')[1]?.split('.vue')[0]
    if (dir) {
      viewPathMap.set(dir, () => modules[path]())
    }
  }
}

// 初始化路径映射
initViewPathMap()

/**
 * 加载视图组件
 * @description 根据路径加载对应的视图组件，使用 Map 缓存提升查找效率
 * @param {string} view - 组件路径（相对于 views 目录）
 * @returns {Function|undefined} 返回组件的加载函数或 undefined
 * @example
 * loadView('system/user/index')
 */
export const loadView = (view: string) => {
  const normalizedView = view
    .replace(VIEW_PREFIX_RE, '')
    .replace(VUE_SUFFIX_RE, '')
  if (normalizedView.toLowerCase() === 'index')
    return viewPathMap.get('home/index')
  if (normalizedView === 'monitor/logininfor/index')
    return viewPathMap.get('system/log/logininfor/index')
  if (normalizedView === 'monitor/operlog/index')
    return viewPathMap.get('system/log/operlog/index')
  if (normalizedView === 'system/standard-model/category/index')
    return viewPathMap.get('iot/standard/index')
  if (normalizedView === 'system/standard-model/field/index')
    return viewPathMap.get('iot/standard/field')
  if (normalizedView === 'MqttNetwork')
    return viewPathMap.get('iot/network/mqtt/index')
  if (normalizedView === 'HttpNetwork')
    return viewPathMap.get('iot/network/http/index')
  if (normalizedView === 'CertificateManager')
    return viewPathMap.get('iot/network/certificate/index')
  if (normalizedView === 'NoticeChannel')
    return viewPathMap.get('application/notice/channel')
  if (normalizedView === 'NoticeTemplate')
    return viewPathMap.get('application/notice/template')
  if (normalizedView === 'system/network/certificate/CertificateManager')
    return viewPathMap.get('iot/network/certificate/index')
  if (normalizedView === 'system/network/certificate/index')
    return viewPathMap.get('iot/network/certificate/index')
  if (normalizedView === 'miniapp/index')
    return viewPathMap.get('application/miniapp/index')
  if (normalizedView === 'miniapp/user/index')
    return viewPathMap.get('application/miniapp/index')
  if (normalizedView === 'system/noticeManager/NoticeChannel')
    return viewPathMap.get('application/notice/channel')
  if (normalizedView === 'system/noticeManager/NoticeTemplate')
    return viewPathMap.get('application/notice/template')
  if (normalizedView === 'system/noticeManager/index')
    return viewPathMap.get('application/notice/channel')
  if (normalizedView === 'system/databridge/connection')
    return viewPathMap.get('rule/databridge/connection/index')
  if (normalizedView === 'system/databridge/bridge')
    return viewPathMap.get('rule/databridge/bridge/index')
  if (normalizedView === 'system/taskCenter/index')
    return viewPathMap.get('task-center/index')
  if (normalizedView === 'system/taskCenter/definitions')
    return viewPathMap.get('task-center/definitions')
  if (normalizedView === 'system/taskCenter/credentials')
    return viewPathMap.get('task-center/credentials')

  const iotDomainMatch = normalizedView.match(IOT_DOMAIN_RE)
  const targetView = iotDomainMatch
    ? `iot/${iotDomainMatch[1]}${iotDomainMatch[2] || ''}`
    : normalizedView
  return viewPathMap.get(targetView)
}

/**
 * 格式化异步路由配置
 * @description 处理后端返回的菜单数据，转换为路由配置
 * @param {MenuItem[]} menus - 后端返回的菜单数据（已经过权限过滤）
 * @returns {RouteRecordRaw[]} 返回格式化后的路由配置数组
 *
 * @remarks
 * 1. 对菜单数据进行排序（根据 sort 字段）
 * 2. 将字符串类型的组件标识转换为实际组件
 * 3. 处理路由元数据（meta）
 *
 * @example
 * const routes = formatAsyncRoutes([
 *   {
 *     path: '/system',
 *     component: 'Layout',
 *     children: [...]
 *   }
 * ])
 */
const formatAsyncRoutes = (menus: UserRouteItem[]) => {
  if (!menus.length) return []

  const resolveRoutePath = (path: string, parentPath = '') => {
    if (EXTERNAL_URL_RE.test(path) || path.startsWith('/')) return path
    if (!parentPath || parentPath === '/') return `/${path}`
    return `${parentPath.replace(TRAILING_SLASH_RE, '')}/${path}`
  }

  const formatRoute = (
    item: UserRouteItem,
    parentPath = ''
  ): RouteRecordRaw => {
    const path = resolveRoutePath(item.path, parentPath)
    const children = (item.children || [])
      .map((child) => formatRoute(child, path))
      .sort((a, b) => Number(a.meta?.sort ?? 0) - Number(b.meta?.sort ?? 0))
    const isLayout = item.component === 'Layout'
    const isNestedLayout = isLayout && Boolean(parentPath)
    const isParentView
      = item.component === 'ParentView'
        || (!item.component && children.length > 0)
    const resolvedComponent
      = item.component && !isLayout && !isParentView
        ? loadView(item.component)
        : undefined
    const migrationPending = !isLayout && !isParentView && !resolvedComponent
    const isRootWorkbench
      = isLayout && item.path === '/' && children.length === 1
    const title = isRootWorkbench
      ? children[0].meta?.title || item.title
      : item.title
    const icon = isRootWorkbench
      ? children[0].meta?.icon || item.icon
      : item.icon
    const component = isLayout
      ? isNestedLayout ? ParentView : Layout
      : isParentView
        ? ParentView
        : resolvedComponent || MigrationPending

    return {
      path,
      name: transformPathToName(path),
      component: component as never,
      redirect: item.redirect,
      children,
      meta: {
        hidden: item.hidden, // 是否在菜单中隐藏
        keepAlive: item.keepAlive, // 是否缓存该路由
        title, // 路由标题，用于显示在菜单和标签页
        icon, // 图标
        affix: item.affix, // 是否固定在标签页
        breadcrumb: item.breadcrumb, // 是否显示在面包屑
        showInTabs: item.showInTabs, // 是否显示在标签页
        activeMenu: item.activeMenu, // 激活的菜单项
        alwaysShow: isRootWorkbench ? false : item.alwaysShow, // 是否总是显示
        sort: item.sort,
        migrationPending,
        sourceComponent: item.component || ''
      }
    }
  }

  const supportedMenus = (items: UserRouteItem[]): UserRouteItem[] => items.flatMap((item) => {
    const children = supportedMenus(item.children || [])
    const container = !item.component || ['Layout', 'ParentView'].includes(item.component)
    if (container) return children.length ? [{ ...item, children }] : []
    return loadView(item.component) ? [{ ...item, children }] : []
  })

  return supportedMenus(menus)
    .map((item) => formatRoute(item))
    .sort((a, b) => Number(a.meta?.sort ?? 0) - Number(b.meta?.sort ?? 0))
}

/**
 * 路由状态管理的核心逻辑
 * @description 管理路由相关的状态和操作
 */
const storeSetup = () => {
  /**
   * 所有路由配置
   * 包含常驻路由和动态路由
   */
  const routes = ref<RouteRecordRaw[]>([])

  /**
   * 动态路由配置
   * 根据用户权限动态生成的路由
   */
  const asyncRoutes = ref<RouteRecordRaw[]>([])

  /**
   * 合并路由配置
   * @description 将动态路由与常驻路由合并
   * @param {RouteRecordRaw[]} data - 动态路由配置
   */
  const setRoutes = (data: UserRouteItem[]) => {
    const formattedRoutes = formatAsyncRoutes(data)
    routes.value = constantRoutes.concat(formattedRoutes)
    asyncRoutes.value = formattedRoutes
  }

  return {
    routes,
    asyncRoutes,
    setRoutes
  }
}

/**
 * 路由状态管理 Store
 * @description 创建路由相关的状态管理 store，启用持久化存储
 */
export const useRouteStore = defineStore('route', storeSetup, {
  persist: true
})
