/** @file 路由配置模块 - 处理路由的创建、守卫和重置 */

import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHashHistory } from 'vue-router'
import createRouteGuard from '@/router/guard'
import { useRouteStore } from '@/stores'

/** 默认布局组件 */
const Layout = () => import('@/layout/index.vue')

/** 基础路由配置 */
const baseRoutes: RouteRecordRaw[] = [
  {
    path: '/redirect',
    component: Layout,
    meta: { hidden: true },
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { hidden: true }
  }
]

/** 错误页面路由配置 */
const errorRoutes: RouteRecordRaw[] = [
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  },
  {
    path: '/403',
    component: () => import('@/views/error/403.vue'),
    meta: { hidden: true }
  }
]

/** 主页路由配置 */
const homeRoute: RouteRecordRaw = {
  path: '/',
  name: 'Home',
  component: Layout,
  redirect: '/index',
  meta: { hidden: true },
  children: [
    {
      path: '/home',
      component: () => import('@/views/home/index.vue'),
      name: 'HomeIndex',
      meta: {
        title: '首页',
        icon: 'icon-dashboard',
        affix: true,
        hidden: false,
        breadcrumb: false
      }
    }
  ]
}

/** 产品详情保留为独立页面，菜单继续高亮“所有产品”。 */
const productDetailRoute: RouteRecordRaw = {
  path: '/product/details/:id',
  component: Layout,
  meta: { hidden: true },
  children: [
    {
      path: '',
      name: 'IotProductDetail',
      component: () => import('@/views/iot/product/details/index.vue'),
      meta: {
        title: '产品详情',
        hidden: true,
        activeMenu: '/product/list',
        keepAlive: false,
        showInTabs: true
      }
    }
  ]
}

/** 设备详情保留旧系统深链，菜单继续高亮“所有设备”。 */
const deviceDetailRoute: RouteRecordRaw = {
  path: '/system/instance/instance-details/:id',
  component: Layout,
  meta: { hidden: true },
  children: [
    {
      path: '',
      name: 'IotDeviceDetail',
      component: () => import('@/views/iot/instance/details/index.vue'),
      meta: {
        title: '设备详情',
        hidden: true,
        activeMenu: '/iot/device',
        keepAlive: false,
        showInTabs: true
      }
    }
  ]
}

/** 个人中心为右上角账户入口，不依赖后端菜单下发。 */
const accountRoute: RouteRecordRaw = {
  path: '/system/account',
  component: Layout,
  meta: { hidden: true },
  children: [
    {
      path: '',
      name: 'SystemAccount',
      component: () => import('@/views/system/account/index.vue'),
      meta: {
        title: '个人中心',
        hidden: true,
        keepAlive: false,
        showInTabs: true
      }
    }
  ]
}

/** OTA 菜单保留稳定深链，避免三级动态路由刷新时先命中 404。 */
const otaRoutes: RouteRecordRaw = {
  path: '/iot/ota',
  component: Layout,
  meta: { hidden: true },
  children: [
    {
      path: 'package',
      name: 'IotOtaPackage',
      component: () => import('@/views/iot/ota/package/index.vue'),
      meta: { title: '升级包', activeMenu: '/iot/ota/package', keepAlive: true }
    },
    {
      path: 'task',
      name: 'IotOtaTask',
      component: () => import('@/views/iot/ota/task/index.vue'),
      meta: { title: '升级计划', activeMenu: '/iot/ota/task', keepAlive: true }
    }
  ]
}

// 旧入口独立重定向，避免动态菜单替换同名页面路由时连同别名一起移除。
const legacyOtaRoutes: RouteRecordRaw[] = [
  { path: '/ota/package', redirect: '/iot/ota/package', meta: { hidden: true } },
  { path: '/ota/task', redirect: '/iot/ota/task', meta: { hidden: true } }
]

/** 静态路由配置 */
export const constantRoutes: RouteRecordRaw[] = [...baseRoutes, productDetailRoute, deviceDetailRoute, accountRoute, otaRoutes, ...legacyOtaRoutes, ...errorRoutes, homeRoute]

/** 创建路由实例 */
const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ left: 0, top: 0 })
})

// 创建路由守卫
createRouteGuard(router)

/** 重置路由配置 - 清除所有动态添加的路由 */
export function resetRouter(): void {
  try {
    const routeStore = useRouteStore()
    // 移除所有动态路由
    routeStore.asyncRoutes.forEach((route) => {
      const { name } = route
      if (name && router.hasRoute(name)) {
        router.removeRoute(name)
      }
    })
  } catch (error) {
    console.error('重置路由失败:', error)
    // 降级处理：刷新页面以重置路由状态
    window.location.reload()
  }
}

export default router
