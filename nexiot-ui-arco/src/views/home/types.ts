export type DashboardStateFilter = 'all' | 'online' | 'offline'

export interface DashboardDeviceStats {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  onlineRate: number
}

export interface DashboardProductStats {
  totalProducts: number
  myProducts: number
  publicProducts: number
}

export interface DashboardOverview {
  deviceStats?: Partial<DashboardDeviceStats>
  productStats?: Partial<DashboardProductStats>
  applicationStats?: { totalApplications?: number }
  timestamp?: number
}

export interface DashboardMapSummary {
  totalDevices: number
  onlineDevices: number
  locatedDevices: number
  onlineLocatedDevices: number
  unlocatedDevices: number
  matchedDevices: number
}

export interface DashboardMapDevice {
  id: string | number
  iotId?: string
  deviceId?: string
  deviceName?: string
  productKey?: string
  productName?: string
  state?: number | boolean | string
  onlineTime?: number | string
  coordinate?: string
  longitude?: number | string
  latitude?: number | string
  thirdPlatform?: string
  deviceNode?: string
}

export interface DashboardMapBounds {
  westLng: number
  southLat: number
  eastLng: number
  northLat: number
  zoom: number
}

export interface DashboardMapQuery extends Partial<DashboardMapBounds> {
  preview?: boolean
  keyword?: string
  productKey?: string
  state?: number
  limit?: number
}

export interface DashboardMapResponse {
  previewMode: boolean
  hasMore: boolean
  limit: number
  list: DashboardMapDevice[]
  summary: DashboardMapSummary
  timestamp?: number
}

export interface DashboardProductOption {
  id: string
  productKey: string
  name: string
}

export interface DashboardDeviceOption extends DashboardMapDevice {
  id: string | number
  productKey: string
}

export function isOnlineDevice(device: Pick<DashboardMapDevice, 'state'>) {
  return device.state === true || device.state === 1 || device.state === '1'
}

export function hasDeviceLocation(device?: Pick<DashboardMapDevice, 'longitude' | 'latitude'>) {
  if (!device) return false
  const longitude = Number(device.longitude)
  const latitude = Number(device.latitude)
  return Number.isFinite(longitude)
    && Number.isFinite(latitude)
    && longitude >= -180
    && longitude <= 180
    && latitude >= -90
    && latitude <= 90
    && !(longitude === 0 && latitude === 0)
}

export function createEmptyMapSummary(): DashboardMapSummary {
  return {
    totalDevices: 0,
    onlineDevices: 0,
    locatedDevices: 0,
    onlineLocatedDevices: 0,
    unlocatedDevices: 0,
    matchedDevices: 0
  }
}
