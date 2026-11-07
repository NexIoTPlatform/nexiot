import type { DeviceItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { HistoryRange, TrendSource } from '@/components/PropertyTrend/types'
import { fieldUnit, historyStatistics, isNumericField, validRange } from '@/components/PropertyTrend/model'
import { loadPropertyTrend } from './propertyCompare.api'
import { canAddCompareDevice, runCompareQueries } from './propertyCompareModel'

interface CompareResult {
  key: string
  device: DeviceItem
  colorIndex: number
  status: 'loading' | 'success' | 'error'
  source?: TrendSource
  error?: string
}

interface CompareProps {
  device: DeviceItem
  field: ProductMetadataItem
  range: HistoryRange
  active: boolean
}

export function usePropertyCompare(props: CompareProps) {
  const selected = ref<DeviceItem[]>([props.device])
  const results = ref<CompareResult[]>([])
  const loading = ref(false)
  const maxPoints = ref(100)
  const maxScanRows = ref(10000)
  const unit = computed(() => fieldUnit(props.field))
  const sources = computed(() => results.value.flatMap((result) => result.source ? [result.source] : []))
  const hasData = computed(() => sources.value.some((source) => source.trend.numericCount > 0))
  const statistics = computed(() => results.value.map((result) => ({ ...result, ...historyStatistics(result.source?.trend) })))
  let controller: AbortController | undefined
  let revision = 0
  const colorById = new Map([[String(props.device.id), 0]])

  function stop() {
    ++revision
    controller?.abort()
    loading.value = false
  }

  async function query() {
    stop()
    if (!props.active || !isNumericField(props.field) || !validRange(props.range)) return
    const request = revision
    const abort = new AbortController()
    controller = abort
    const property = props.field.id
    const range = { ...props.range }
    const sourceUnit = unit.value
    results.value = selected.value.map((device) => ({ key: String(device.id), device, colorIndex: colorById.get(String(device.id)) || 0, status: 'loading' }))
    loading.value = true
    await runCompareQueries([...results.value], async (result) => {
      try {
        if (result.device.productKey !== props.device.productKey || !result.device.iotId) throw new Error('设备不可用于当前产品对比')
        const { trend } = await loadPropertyTrend({ ...range, productKey: props.device.productKey, iotId: result.device.iotId, deviceId: result.device.deviceId, property, maxPoints: maxPoints.value, maxScanRows: maxScanRows.value }, abort.signal)
        if (request !== revision || abort.signal.aborted) return
        result.source = { key: result.key, label: `${result.device.deviceName || result.device.deviceId} · ${result.device.deviceId}`, unit: sourceUnit, trend, colorIndex: result.colorIndex }
        result.status = 'success'
      } catch (cause) {
        if (request !== revision || abort.signal.aborted) return
        result.status = 'error'
        result.error = cause instanceof Error ? cause.message : '查询失败，请重试'
      }
    }, abort.signal)
    if (request === revision) loading.value = false
  }

  function add(device: DeviceItem) {
    if (!canAddCompareDevice(props.device, selected.value, device)) return
    const colors = selected.value.map((item) => colorById.get(String(item.id)))
    colorById.set(String(device.id), [0, 1, 2, 3, 4, 5].find((color) => !colors.includes(color)) || 0)
    selected.value.push(device)
    return query()
  }

  function remove(id: string) {
    if (String(id) === String(props.device.id)) return
    selected.value = selected.value.filter((device) => String(device.id) !== String(id))
    return query()
  }

  watch(() => [props.active, props.range, props.field.id, maxPoints.value, maxScanRows.value], query, { immediate: true })
  onScopeDispose(stop)
  return { selected, unit, results, sources, loading, maxPoints, maxScanRows, statistics, hasData, add, remove, query }
}
