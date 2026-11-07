import type { MockMethod } from 'vite-plugin-mock'
import { defineMock } from './_base'
import { getDelayTime, resultSuccess } from './_utils'

const products = [
  { id: 'p-1', productKey: 'weather-station', name: '园区气象站' },
  { id: 'p-2', productKey: 'water-meter', name: '智能水表' },
  { id: 'p-3', productKey: 'camera', name: '园区摄像机' },
  { id: 'p-4', productKey: 'jt808', name: '4G-JT808定位器' }
]

const devices = [
  { id: 'd-1', iotId: 'weather-station.001', deviceId: 'WS-001', deviceName: '东门气象站', productKey: 'weather-station', productName: '园区气象站', state: 1, onlineTime: Date.now() - 90_000, longitude: 116.397, latitude: 39.909, deviceNode: 'DEVICE' },
  { id: 'd-2', iotId: 'weather-station.002', deviceId: 'WS-002', deviceName: '北区气象站', productKey: 'weather-station', productName: '园区气象站', state: 1, onlineTime: Date.now() - 220_000, longitude: 121.474, latitude: 31.230, deviceNode: 'DEVICE' },
  { id: 'd-3', iotId: 'water-meter.001', deviceId: 'WM-001', deviceName: '一号楼水表', productKey: 'water-meter', productName: '智能水表', state: 0, onlineTime: Date.now() - 7_200_000, longitude: 113.264, latitude: 23.129, deviceNode: 'DEVICE' },
  { id: 'd-4', iotId: 'camera.001', deviceId: 'CAM-001', deviceName: '西门摄像机', productKey: 'camera', productName: '园区摄像机', state: 1, onlineTime: Date.now() - 35_000, longitude: 114.057, latitude: 22.543, deviceNode: 'VIDEO_DEVICE' },
  { id: 'd-5', iotId: 'camera.002', deviceId: 'CAM-002', deviceName: '仓库摄像机', productKey: 'camera', productName: '园区摄像机', state: 0, onlineTime: Date.now() - 3_600_000, longitude: 104.066, latitude: 30.572, deviceNode: 'VIDEO_DEVICE' },
  { id: 'd-6', iotId: 'jt808018220444386', deviceId: '018220444386', deviceName: '沪A88688-JT808', productKey: 'jt808', productName: '4G-JT808定位器', state: 1, onlineTime: Date.now() - 60_000, longitude: 120.172232, latitude: 30.216384, deviceNode: 'DEVICE' }
]

const routes = [
  {
    id: 'dashboard-root',
    path: '/',
    component: 'Layout',
    title: '工作台',
    name: '工作台',
    icon: 'icon-dashboard',
    children: [{ id: 'dashboard-index', path: 'index', component: 'index', title: '工作台', name: '工作台', icon: 'icon-dashboard', meta: { title: '工作台' } }]
  }
]

function matchDevices(query: Record<string, any>) {
  const keyword = String(query.deviceName || '').trim().toLowerCase()
  return devices.filter((device) => {
    const matchesProduct = !query.productKey || device.productKey === query.productKey
    const matchesState = query.state === undefined || query.state === '' || Number(query.state) === device.state
    const matchesKeyword = !keyword || [device.deviceName, device.deviceId, device.iotId].some((value) => String(value).toLowerCase().includes(keyword))
    return matchesProduct && matchesState && matchesKeyword
  })
}

export default defineMock([
  {
    url: '/oauth/token',
    method: 'post',
    timeout: getDelayTime(),
    response: () => ({ access_token: 'preview-token', refresh_token: 'preview-refresh', expires_in: 3600, token_type: 'bearer' })
  },
  {
    url: '/admin/getInfo',
    method: 'get',
    response: () => resultSuccess({ user: { id: 'preview-user', unionId: 'preview-user', username: 'preview', alias: '工作台预览', admin: true }, roles: ['ROLE_ADMIN'], permissions: ['*'] })
  },
  {
    url: '/admin/getRouters',
    method: 'get',
    response: () => resultSuccess(routes)
  },
  {
    url: '/admin/system/user/profile/websocketPush',
    method: 'get',
    response: () => resultSuccess({ globalEnabled: false, adminEditable: false, websocketPath: '/ws/device-events', settings: { enabled: false } })
  },
  {
    url: '/admin/system/dict/data/types/sys_normal_disable,sys_user_sex',
    method: 'get',
    response: () => resultSuccess({
      sys_normal_disable: [
        { dictCode: 'preview-enabled', dictLabel: '正常', dictValue: '1', dictType: 'sys_normal_disable', dictSort: 1 },
        { dictCode: 'preview-disabled', dictLabel: '停用', dictValue: '0', dictType: 'sys_normal_disable', dictSort: 2 }
      ],
      sys_user_sex: [
        { dictCode: 'preview-male', dictLabel: '男', dictValue: '1', dictType: 'sys_user_sex', dictSort: 1 },
        { dictCode: 'preview-female', dictLabel: '女', dictValue: '2', dictType: 'sys_user_sex', dictSort: 2 }
      ]
    })
  },
  {
    url: '/admin/v1/product/v4/list',
    method: 'get',
    response: ({ query }) => {
      const name = String(query.name || '').trim().toLowerCase()
      const rows = products.filter((item) => !name || item.name.toLowerCase().includes(name) || item.productKey.includes(name))
      return { code: 0, rows, total: rows.length, msg: '' }
    }
  },
  {
    url: '/admin/v1/device/list',
    method: 'get',
    response: ({ query }) => {
      const rows = matchDevices(query)
      return { code: 0, rows, total: rows.length, msg: '' }
    }
  },
  {
    url: '/dashboard/overview',
    method: 'get',
    response: () => ({ deviceStats: { totalDevices: devices.length, onlineDevices: 3, offlineDevices: 2, onlineRate: 60 }, productStats: { totalProducts: products.length, myProducts: 2, publicProducts: 1 }, applicationStats: { totalApplications: 4 }, timestamp: Date.now() })
  },
  {
    url: '/dashboard/map/devices',
    method: 'get',
    response: ({ query }) => {
      const rows = matchDevices(query).filter((device) => query.preview || (!query.westLng || (device.longitude >= Number(query.westLng) && device.longitude <= Number(query.eastLng) && device.latitude >= Number(query.southLat) && device.latitude <= Number(query.northLat))))
      return { previewMode: Boolean(query.preview), hasMore: false, limit: Number(query.limit || 180), list: rows, summary: { totalDevices: devices.length, onlineDevices: 3, locatedDevices: devices.length, onlineLocatedDevices: 3, unlocatedDevices: 0, matchedDevices: rows.length }, timestamp: Date.now() }
    }
  }
] as MockMethod[])
