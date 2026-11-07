import type { DeviceQuery } from '@/apis/iot/device'
import { getDeviceList } from '@/apis/iot/device'

export { loadPropertyTrend } from '@/components/PropertyTrend/api'

// 所有搜索、翻页都携带当前产品；响应再次过滤，避免错误数据进入选择器。
export async function getCompareDevices(productKey: string, params: Omit<DeviceQuery, 'productKey'>) {
  const response = await getDeviceList({ ...params, productKey })
  return { records: response.data.records.filter((device) => device.productKey === productKey), total: Number(response.data.total) }
}
