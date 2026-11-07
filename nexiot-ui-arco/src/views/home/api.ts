import type {
  DashboardDeviceOption,
  DashboardMapQuery,
  DashboardMapResponse,
  DashboardOverview,
  DashboardProductOption
} from './types'
import http from '@/utils/http'

export function getDashboardOverview() {
  return http.get<DashboardOverview>('/dashboard/overview')
}

export function getDashboardMapDevices(params: DashboardMapQuery) {
  return http.get<DashboardMapResponse>('/dashboard/map/devices', params)
}

export function getDashboardProducts(params: { page?: number, size?: number, name?: string } = {}) {
  return http.get<PageRes<DashboardProductOption[]>>('/admin/v1/product/v4/list', {
    pageNum: params.page || 1,
    pageSize: params.size || 30,
    name: params.name
  })
}

export function getDashboardDeviceOptions(params: {
  page?: number
  size?: number
  productKey?: string
  deviceName?: string
  state?: number
} = {}) {
  return http.get<PageRes<DashboardDeviceOption[]>>('/admin/v1/device/list', {
    pageNum: params.page || 1,
    pageSize: params.size || 30,
    productKey: params.productKey,
    deviceName: params.deviceName,
    state: params.state
  })
}
