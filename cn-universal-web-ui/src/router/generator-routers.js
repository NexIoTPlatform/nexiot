// eslint-disable-next-line
import { getRouters } from '@/api/menu'
import { otherRouterMap } from '@/config/router.config'
import allIcon from '@/core/icons'
import { BlankLayout, PageView, UserLayout } from '@/layouts'
import { validURL } from '@/utils/validate'

// 菜单标题翻译映射
const menuTitleMap = {
  '工作台': 'menu.dashboard',
  '系统管理': 'menu.system',
  '用户管理': 'menu.system.user',
  '角色管理': 'menu.system.role',
  '代码生成': 'menu.system.gen',
  '菜单管理': 'menu.system.menu',
  '字典管理': 'menu.system.dict',
  '参数设置': 'menu.system.config',
  '日志管理': 'menu.system.monitor',
  '登录日志': 'menu.system.monitor.login',
  '操作日志': 'menu.system.monitor.oper',
  '产品开发': 'menu.product',
  '产品管理': 'menu.product.management',
  '产品分类': 'menu.product.category',
  '协议管理': 'menu.product.protocol',
  '设备管理': 'menu.device',
  '设备列表': 'menu.device.list',
  '设备群组': 'menu.device.group',
  '网络组件': 'menu.network',
  'MQTT服务': 'menu.network.mqtt',
  'TCP服务': 'menu.network.tcp',
  'WebSocket服务': 'menu.network.websocket',
  '证书管理': 'menu.network.cert',
  '视频监控': 'menu.video',
  '乐橙云': 'menu.video.imoulife',
  '萤石云(不可用)': 'menu.video.ezviz',
  '数据流转': 'menu.rule',
  '数据转发': 'menu.rule.flow',
  '规则编排': 'menu.rule.chain',
  '数据桥接': 'menu.rule.bridge',
  '连接管理': 'menu.rule.bridge.connection',
  '桥接规则': 'menu.rule.bridge.rule',
  '场景联动': 'menu.linkage',
  '联动管理': 'menu.linkage.management',
  '联动日志': 'menu.linkage.logs',
  '北向应用': 'menu.app',
  '应用开发': 'menu.app.development',
  '通知管理': 'menu.notice',
  '通知渠道': 'menu.notice.channel',
  '通知模版': 'menu.notice.template'
}

// 将菜单标题转换为翻译键
const translateMenuTitle = (title) => {
  return menuTitleMap[title] || title
}
// 前端路由表
const constantRouterComponents = {
  // 基础页面 layout 必须引入
  BasicLayout: () => import('@/layouts/BasicLayout'),
  BlankLayout: BlankLayout,
  RouteView: () => import('@/layouts/RouteView'),
  PageView: PageView,
  UserLayout: UserLayout, // 登陆注册页面的通用布局

  // 你需要动态引入的页面组件
  'Index': () => import('@/views/dashboard/index'),
  // account
  'AccountCenter': () => import('@/views/account/center'),
  'AccountSettings': () => import('@/views/account/settings/index'),
  'BaseSettings': () => import('@/views/account/settings/BaseSetting'),
  'SecuritySettings': () => import('@/views/account/settings/Security'),
  // job log
  'JobLog': () => import('@/views/monitor/job/log'),
  // 授权用户
  'AuthUser': () => import('@/views/system/role/authUser'),
  // 公告新增修改
  'NoticeForm': () => import('@/views/system/notice/CreateForm'),
  // 修改生成配置
  'GenEdit': () => import('@/views/tool/gen/modules/GenEdit'),
  // 设备绑定页面
  'DeviceBind': () => import('@/views/application/modules/DeviceBind'),
  'applicationDetails': () => import('@/views/application/modules/ApplicationDetail'),
  // 产品详情
  'productDetails': () => import('@/views/system/product/modules/productDetails'),
  // 产品详情
  'instanceDetails': () => import('@/views/system/instance/modules/InstanceDetails'),
  // 日志管理
  'LogManage': () => import('@/views/system/instance/modules/logManage'),
  // 产品中设备管理
  'productInstance': () => import('@/views/system/product/modules/productInstance'),
  // 批量配置历史
  'batchHistory': () => import('@/views/system/instance/config/history.vue'),
  // TCP网络组件
  'TcpNetwork': () => import('@/views/system/network/tcp/index'),
  'TcpNetworkDetail': () => import('@/views/system/network/tcp/detail'),
  // MQTT网络组件
  'MqttNetwork': () => import('@/views/system/network/mqtt/index'),
  'MqttNetworkDetail': () => import('@/views/system/network/mqtt/detail'),
  // WebSocket网络组件
  'WebSocketNetwork': () => import('@/views/system/network/websocket/index'),
  'WebSocketNetworkDetail': () => import('@/views/system/network/websocket/detail'),
  // 证书管理组件
  'CertificateManager': () => import('@/views/system/network/certificate/CertificateManager.vue'),
  // 通知管理组件
  'NoticeChannel': () => import('@/views/system/noticeManager/NoticeChannel'),
  'NoticeTemplate': () => import('@/views/system/noticeManager/NoticeTemplate'),
  // Node-RED组件已移除（相关功能未实现）
  // 规则流转详情
  'ruleFlowDetail': () => import('@/views/rule/flow/detail'),
  // 视频监控
  'VideoEzviz': () => import('@/views/system/video/VideoEzviz'),
  'Imoulife': () => import('@/views/system/video/Imoulife'),
  'EzvizPlayer': () => import('@/views/system/video/EzvizPlayer'),
  'ImouPlayer': () => import('@/views/system/video/ImouPlayer'),
  // 视频中心（新版）
  'VideoCenter': () => import('@/views/video/VideoCenter'),
  'VideoPlatformDetail': () => import('@/views/video/VideoPlatformDetail'),
  'WvpPlatformDetail': () => import('@/views/video/WvpPlatformDetail'),
  'HikIcsPlatformDetail': () => import('@/views/video/HikIcsPlatformDetail'),
  'DahuaIccPlatformDetail': () => import('@/views/video/DahuaIccPlatformDetail'),
  'VideoDevices': () => import('@/views/video/VideoDevices'),
  'WvpDeviceDetailsPro': () => import('@/views/video/WvpDeviceDetailsPro'),
  // 数据桥接
  'DataBridge': () => import('@/views/system/databridge/index'),
  'DataBridgeConnection': () => import('@/views/system/databridge/connection'),
  'DataBridgeBridge': () => import('@/views/system/databridge/bridge')
}

// 前端未找到页面路由（固定不用改）
const notFoundRouter = {
  path: '*', redirect: '/404', hidden: true
}

// 根级菜单
const rootRouter = {
  key: '',
  name: 'index',
  path: '',
  component: 'BasicLayout',
  redirect: '/index',
  meta: {
    title: '首页'
  },
  children: []
}

/**
 * 为解决缓存问题，自定义页面添加一层父级
 */
const bashRouter = {
  path: '/',
  name: '',
  component: 'Layout',
  hidden: true
}

/**
 * 动态生成菜单
 * @param token
 * @returns {Promise<Router>}
 */
export const generatorDynamicRouter = (token) => {
  return new Promise((resolve, reject) => {
    // 向后端请求路由数据
    getRouters().then(res => {
      const menuNav = []
      const routerData = res.data
      bashRouter.children = otherRouterMap
      routerData.unshift(bashRouter)
      rootRouter.children = routerData
      menuNav.push(rootRouter)
      const routers = generator(menuNav)
      routers.push(notFoundRouter)
      resolve(routers)
    }).catch(err => {
      reject(err)
    })
  })
}

/**
 * 格式化树形结构数据 生成 vue-router 层级路由表
 *
 * @param routerMap
 * @param parent
 * @returns {*}
 */
export const generator = (routerMap, parent) => {
  return routerMap.map(item => {
    const {
      title,
      show,
      hideChildren,
      hiddenHeaderContent,
      hidden,
      icon,
      noCache,
      selected,
      open
    } = item.meta || {}
    if (item.component) {
      // Layout ParentView 组件特殊处理
      if (item.component === 'Layout') {
        item.component = 'RouteView'
      } else if (item.component === 'ParentView') {
        // 三级菜单处理
        item.component = 'RouteView'
        item.path = '/' + item.path
      }
    }
    if (item.path) {
      // item.path = '/' + item.path
    }
    if (item.isFrame === 0) {
      item.target = '_blank'
    }
    const currentRouter = {
      // 如果路由设置了 path，则作为默认 path，否则 路由地址 动态拼接生成如 /dashboard/workplace
      path: item.path || `${parent && parent.path || ''}/${item.path}`,
      // 路由名称，建议唯一
      name: item.name || item.key || '',
      // 该路由对应页面的 组件(动态加载)
      component: (constantRouterComponents[item.component || item.key])
        || (() => import(`@/views/${item.component}`)),
      hidden: item.hidden,
      // meta: 页面标题, 菜单图标, 页面权限(供指令权限用，可去掉)
      meta: {
        title: translateMenuTitle(title),
        icon: allIcon[icon] || allIcon[icon + 'Icon'] || icon,
        hiddenHeaderContent: hiddenHeaderContent,
        // 目前只能通过判断path的http链接来判断是否外链，适配若依
        target: validURL(item.path) ? '_blank' : '',
        permission: item.name,
        keepAlive: noCache === undefined ? false : !noCache,
        hidden: hidden,
        selected,
        open
      },
      redirect: item.redirect
    }
    // 是否设置了隐藏菜单
    if (show === false) {
      currentRouter.hidden = true
    }
    // 适配若依，若依为缩写路径，而antdv-pro的pro-layout要求每个路径需为全路径
    if (!constantRouterComponents[item.component || item.key]) {
      currentRouter.path = `${parent && parent.path || ''}/${item.path}`
    }
    // 是否设置了隐藏子菜单
    if (hideChildren) {
      currentRouter.hideChildrenInMenu = true
    }
    // 是否有子菜单，并递归处理，并将父path传入
    if (item.children && item.children.length > 0) {
      // Recursion
      currentRouter.children = generator(item.children, currentRouter)
    }
    return currentRouter
  })
}
