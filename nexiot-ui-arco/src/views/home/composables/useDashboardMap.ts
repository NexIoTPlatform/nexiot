import type { DashboardMapBounds, DashboardMapDevice } from '../types'
import AMapLoader from '@amap/amap-jsapi-loader'
import { amapConfig, applyAMapSecurityConfig } from '@/config/amap'
import { useTheme } from '@/hooks'
import { isOnlineDevice } from '../types'

const CHINA_CENTER: [number, number] = [105.602725, 37.076636]
const CLUSTER_THRESHOLD = 30

interface DashboardMapOptions {
  onBoundsChange: (bounds: DashboardMapBounds) => void
  onDeviceSelect: (device: DashboardMapDevice) => void
}

export function useDashboardMap(options: DashboardMapOptions) {
  const { isDark } = useTheme()
  const mapReady = ref(false)
  const mapError = ref('')
  let AMap: any
  let map: any
  let markers: any[] = []
  let cluster: any
  let boundsTimer: ReturnType<typeof setTimeout> | undefined
  let resizeObserver: ResizeObserver | undefined
  let initializationVersion = 0

  function resolveMapStyle() {
    return isDark.value ? 'amap://styles/dark' : 'amap://styles/whitesmoke'
  }

  function readBounds(): DashboardMapBounds | undefined {
    if (!map) return
    const bounds = map.getBounds()
    if (!bounds) return
    const southWest = bounds.getSouthWest()
    const northEast = bounds.getNorthEast()
    return {
      westLng: Number(southWest.lng),
      southLat: Number(southWest.lat),
      eastLng: Number(northEast.lng),
      northLat: Number(northEast.lat),
      zoom: Math.round(map.getZoom())
    }
  }

  function emitBoundsChange() {
    clearTimeout(boundsTimer)
    boundsTimer = setTimeout(() => {
      const bounds = readBounds()
      if (bounds) options.onBoundsChange(bounds)
    }, 400)
  }

  async function initialize(container: HTMLElement) {
    if (map || mapReady.value) return
    const version = ++initializationVersion
    if (!amapConfig.key) {
      mapError.value = '未配置高德地图 Key，请检查 VITE_AMAP_KEY。'
      return
    }
    mapError.value = ''
    try {
      applyAMapSecurityConfig()
      const mapApi = await AMapLoader.load({
        key: amapConfig.key,
        version: '2.0',
        plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.MarkerCluster']
      })
      // 视图切换可能先销毁容器，忽略此时才完成的地图脚本加载。
      if (version !== initializationVersion) return
      AMap = mapApi
      map = new AMap.Map(container, {
        zoom: 5,
        center: CHINA_CENTER,
        viewMode: '2D',
        resizeEnable: true,
        rotateEnable: false,
        mapStyle: resolveMapStyle(),
        zooms: [3, 18]
      })
      map.addControl(new AMap.Scale())
      map.addControl(new AMap.ToolBar({ position: 'RB' }))
      map.on('zoomend', emitBoundsChange)
      map.on('moveend', emitBoundsChange)
      if (typeof ResizeObserver === 'function') {
        resizeObserver = new ResizeObserver(() => map?.resize())
        resizeObserver.observe(container)
      }
      mapReady.value = true
      emitBoundsChange()
    } catch (error) {
      if (version !== initializationVersion) return
      mapError.value = error instanceof Error ? error.message : '地图初始化失败，请检查地图配置和网络连接。'
    }
  }

  function clearMarkers() {
    if (cluster) {
      cluster.setMap(null)
      cluster = undefined
    }
    if (map && markers.length) map.remove(markers)
    markers = []
  }

  function createMarkerContent(device: DashboardMapDevice, active: boolean) {
    const button = document.createElement('button')
    button.type = 'button'
    button.className = [
      'dashboard-device-marker',
      isOnlineDevice(device) ? 'is-online' : 'is-offline',
      active ? 'is-active' : ''
    ].filter(Boolean).join(' ')
    button.setAttribute('aria-label', device.deviceName || device.deviceId || device.iotId || '设备位置')
    const core = document.createElement('span')
    core.className = 'dashboard-device-marker__core'
    button.appendChild(core)
    return button
  }

  function renderClusterMarker(context: any) {
    const count = Number(context.count || 0)
    const size = Math.min(54, Math.max(34, 30 + Math.sqrt(count) * 2))
    const content = document.createElement('div')
    content.className = 'dashboard-device-cluster'
    content.style.width = `${size}px`
    content.style.height = `${size}px`
    content.textContent = String(count)
    context.marker.setContent(content)
    context.marker.setOffset(new AMap.Pixel(-size / 2, -size / 2))
  }

  function renderDevices(devices: DashboardMapDevice[], selectedId?: string | number) {
    if (!map || !AMap) return
    clearMarkers()
    markers = devices.map((device) => {
      const marker = new AMap.Marker({
        position: [Number(device.longitude), Number(device.latitude)],
        anchor: 'bottom-center',
        content: createMarkerContent(device, String(selectedId ?? '') === String(device.id)),
        extData: device,
        zIndex: String(selectedId ?? '') === String(device.id) ? 140 : isOnlineDevice(device) ? 120 : 100
      })
      marker.on('click', () => options.onDeviceSelect(device))
      return marker
    })
    if (!markers.length) return
    if (markers.length >= CLUSTER_THRESHOLD) {
      cluster = new AMap.MarkerCluster(map, markers, {
        gridSize: 54,
        renderClusterMarker
      })
    } else {
      map.add(markers)
    }
  }

  function focusDevice(device: DashboardMapDevice) {
    if (!map) return
    map.setZoomAndCenter(Math.max(12, Number(map.getZoom()) || 5), [Number(device.longitude), Number(device.latitude)])
  }

  function resetCenter() {
    map?.setZoomAndCenter(5, CHINA_CENTER)
  }

  function destroy() {
    initializationVersion++
    clearTimeout(boundsTimer)
    resizeObserver?.disconnect()
    resizeObserver = undefined
    clearMarkers()
    if (map) {
      map.off('zoomend', emitBoundsChange)
      map.off('moveend', emitBoundsChange)
      map.destroy()
    }
    map = undefined
    AMap = undefined
    mapReady.value = false
  }

  watch(isDark, () => map?.setMapStyle(resolveMapStyle()))
  onBeforeUnmount(destroy)

  return {
    mapReady,
    mapError,
    initialize,
    renderDevices,
    focusDevice,
    resetCenter,
    destroy
  }
}
