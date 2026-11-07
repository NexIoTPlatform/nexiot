import type { Ref } from 'vue'
import type { HistoryRange, PropertyTrend } from './deviceHistory.api'
import type { DeviceItem, DeviceLogItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import Dayjs from 'dayjs'
import { exportDeviceMetadataLogs } from '@/apis/iot/device'
import { loadPropertyTrend } from '@/components/PropertyTrend/api'
import { zoomRange } from '@/components/PropertyTrend/model'
import { getDeviceMetadataSlice } from './deviceHistory.api'

interface HistoryProps {
  visible: boolean
  device: DeviceItem
  field?: ProductMetadataItem
  messageType: 'PROPERTIES' | 'EVENT'
}

export function useDeviceHistory(props: HistoryProps, numeric: Ref<boolean>) {
  const loading = ref(false)
  const trendLoading = ref(false)
  const exporting = ref(false)
  const rows = ref<DeviceLogItem[]>([])
  const trend = ref<PropertyTrend>()
  const trendNote = ref('')
  let tableController: AbortController | undefined
  let trendController: AbortController | undefined
  const tableError = ref('')
  const trendError = ref('')
  const rangeKey = ref('24h')
  const customRange = ref<string[]>([])
  const activeView = ref('table')
  const trendMaxPoints = ref(100)
  const trendMaxScanRows = ref(10000)
  const pagination = reactive({ current: 1, pageSize: 50 })
  const pageCursors = ref<(string | undefined)[]>([undefined])
  const hasMore = ref(false)
  const fullRange = ref<HistoryRange>()
  const queryRange = ref<HistoryRange>()
  const zoomSelection = ref<HistoryRange>()
  let requestedPage = 1
  let tableRequest = 0
  let trendRequest = 0
  let contextRequest = 0
  let tableLoaded = false
  let initializingView = false

  const hasZoom = computed(() => queryRange.value?.beginCreateTime !== fullRange.value?.beginCreateTime || queryRange.value?.endCreateTime !== fullRange.value?.endCreateTime)
  const rangeLabel = computed(() => queryRange.value ? `${Dayjs.unix(queryRange.value.beginCreateTime).format('YYYY-MM-DD HH:mm:ss')} 至 ${Dayjs.unix(queryRange.value.endCreateTime).format('YYYY-MM-DD HH:mm:ss')}` : '')

  function queryParams() {
    return {
      productKey: props.device.productKey,
      iotId: props.device.iotId,
      deviceId: props.device.deviceId,
      messageType: props.messageType,
      ...(props.messageType === 'EVENT' ? { event: props.field?.id } : { property: props.field?.id }),
      ...queryRange.value
    }
  }

  async function fetchTable(page: number) {
    if (!props.visible || !props.field || !queryRange.value) return
    const request = ++tableRequest
    tableController?.abort()
    const controller = new AbortController()
    tableController = controller
    requestedPage = page
    loading.value = true
    tableError.value = ''
    try {
      const response = await getDeviceMetadataSlice({ ...queryParams(), cursor: pageCursors.value[page - 1], pageSize: pagination.pageSize }, controller.signal)
      if (request !== tableRequest || controller.signal.aborted) return
      const data = response.data
      if (!Array.isArray(data.list) || typeof data.hasMore !== 'boolean' || (data.hasMore && !data.nextCursor)) throw new Error('数据记录分页信息不完整，请刷新后重试')
      rows.value = data.list.map((record, index) => ({ ...record, historyKey: `${record.id ?? record.createTime ?? record.eventTime}-${index}` }))
      tableLoaded = true
      pagination.current = page
      hasMore.value = data.hasMore
      pageCursors.value = pageCursors.value.slice(0, page)
      if (data.hasMore) pageCursors.value[page] = data.nextCursor
    } catch (error) {
      if (request === tableRequest && !controller.signal.aborted) tableError.value = error instanceof Error ? error.message : '数据记录加载失败，请重试'
    } finally {
      if (request === tableRequest) loading.value = false
    }
  }

  function loadTable() {
    return fetchTable(requestedPage)
  }

  function resetPages() {
    ++tableRequest
    tableController?.abort()
    loading.value = false
    tableLoaded = false
    tableError.value = ''
    pagination.current = 1
    requestedPage = 1
    pageCursors.value = [undefined]
    hasMore.value = false
    rows.value = []
  }

  async function loadTrend() {
    const request = ++trendRequest
    trendController?.abort()
    zoomSelection.value = undefined
    trend.value = undefined
    trendNote.value = ''
    trendError.value = ''
    if (!numeric.value || !props.visible || !props.field || !queryRange.value) {
      trendLoading.value = false
      return
    }
    trendLoading.value = true
    const controller = new AbortController()
    trendController = controller
    try {
      const data = await loadPropertyTrend({ ...queryRange.value, productKey: props.device.productKey, iotId: props.device.iotId || '', deviceId: props.device.deviceId, property: props.field.id, maxPoints: trendMaxPoints.value, maxScanRows: trendMaxScanRows.value }, controller.signal)
      if (request === trendRequest && !controller.signal.aborted) {
        trend.value = data.trend
        trendNote.value = data.note || ''
      }
    } catch (error) {
      if (request === trendRequest && !controller.signal.aborted) trendError.value = error instanceof Error ? error.message : '趋势加载失败，请重试或查看数据记录'
    } finally {
      if (request === trendRequest) trendLoading.value = false
    }
  }

  function loadWindow(range: HistoryRange) {
    ++trendRequest
    trendLoading.value = false
    trendController?.abort()
    trend.value = undefined
    trendNote.value = ''
    trendError.value = ''
    queryRange.value = { ...range }
    zoomSelection.value = undefined
    resetPages()
    return loadActiveView()
  }

  async function loadActiveView() {
    if (activeView.value === 'chart' && numeric.value) {
      if (!trend.value && !trendLoading.value) await loadTrend()
    } else if (activeView.value === 'table' && !tableLoaded && !loading.value) {
      await loadTable()
    }
  }

  function handleViewChange(view: string | number | boolean) {
    if (initializingView || (view !== 'table' && (!['chart', 'compare'].includes(String(view)) || !numeric.value))) return
    if (view !== 'table' && loading.value) {
      ++tableRequest
      tableController?.abort()
      loading.value = false
      tableLoaded = false
    }
    if (view !== 'chart' && trendLoading.value) {
      ++trendRequest
      trendController?.abort()
      trendLoading.value = false
    }
    activeView.value = String(view)
    return loadActiveView()
  }

  function presetRange(): HistoryRange {
    const end = Math.floor(Dayjs().unix() / 60) * 60
    const seconds = { '1h': 3600, '24h': 86400, '7d': 7 * 86400, '15d': 15 * 86400 }[rangeKey.value] || 86400
    return { beginCreateTime: end - seconds, endCreateTime: end }
  }

  function refresh() {
    const range = customRange.value.length === 2
      ? { beginCreateTime: Dayjs(customRange.value[0]).unix(), endCreateTime: Dayjs(customRange.value[1]).unix() }
      : presetRange()
    if (!Number.isFinite(range.beginCreateTime) || !Number.isFinite(range.endCreateTime) || range.endCreateTime < range.beginCreateTime || range.endCreateTime - range.beginCreateTime > 31 * 86400) {
      Message.warning('请选择不超过31天的有效时间范围')
      return
    }
    fullRange.value = { ...range }
    return loadWindow(range)
  }

  function handleRangeChange(value: string | number | boolean) {
    rangeKey.value = String(value)
    customRange.value = []
    return refresh()
  }

  function loadCustom(value: (string | number | Date | undefined)[] | undefined) {
    const nextRange = (value || []).filter((item) => item !== undefined).map(String)
    if (nextRange.length !== 0 && nextRange.length !== 2) return
    const previousRange = customRange.value
    const previousKey = rangeKey.value
    customRange.value = nextRange
    if (customRange.value.length === 2) rangeKey.value = ''
    if (!customRange.value.length && !rangeKey.value) rangeKey.value = '24h'
    const request = refresh()
    if (!request) {
      customRange.value = previousRange
      rangeKey.value = previousKey
    }
    return request
  }

  function handlePageChange(page: number) {
    if (loading.value || page < 1 || page > pagination.current + 1 || (page > pagination.current && !hasMore.value)) return
    return fetchTable(page)
  }

  function handlePageSizeChange(value: unknown) {
    const size = Number(value)
    if (![10, 50, 100, 200].includes(size)) return
    pagination.pageSize = size
    resetPages()
    return loadTable()
  }

  function handleTrendMaxPointsChange(value: unknown) {
    const maxPoints = Number(value)
    if (![100, 300, 600].includes(maxPoints) || maxPoints === trendMaxPoints.value) return
    trendMaxPoints.value = maxPoints
    return loadTrend()
  }

  function handleTrendMaxScanRowsChange(value: unknown) {
    const maxScanRows = Number(value)
    if (![1000, 10000, 50000, 100000].includes(maxScanRows) || maxScanRows === trendMaxScanRows.value) return
    trendMaxScanRows.value = maxScanRows
    return loadTrend()
  }

  function handleZoom(event: { start?: number, end?: number, batch?: { start?: number, end?: number }[] }) {
    if (!queryRange.value) return
    zoomSelection.value = zoomRange(queryRange.value, event)
  }

  function applyZoom() {
    if (zoomSelection.value) return loadWindow(zoomSelection.value)
  }

  function resetZoom() {
    if (fullRange.value) return loadWindow(fullRange.value)
  }

  async function exportData() {
    if (exporting.value || !queryRange.value) return
    exporting.value = true
    try {
      await exportDeviceMetadataLogs(queryParams())
      Message.success('所选时段的原始记录已导出')
    } catch {
      // http 层显示后端的导出上限或失败原因。
    } finally {
      exporting.value = false
    }
  }

  watch(() => [props.visible, props.field?.id, props.device.iotId, props.device.productKey, props.messageType, numeric.value] as const, async ([visible]) => {
    const context = ++contextRequest
    ++tableRequest
    ++trendRequest
    tableController?.abort()
    trendController?.abort()
    loading.value = false
    trendLoading.value = false
    initializingView = visible
    if (!visible) {
      trendController?.abort()
      return
    }
    rangeKey.value = '24h'
    customRange.value = []
    activeView.value = 'table'
    // Conditional panes and late metadata can settle after the modal starts mounting.
    await nextTick()
    if (context !== contextRequest || !props.visible) return
    activeView.value = 'table'
    initializingView = false
    await refresh()
  }, { immediate: true })

  onScopeDispose(() => {
    tableController?.abort()
    trendController?.abort()
    ++tableRequest
    ++trendRequest
    ++contextRequest
  })

  return { loading, trendLoading, exporting, rows, trend, trendNote, tableError, trendError, rangeKey, customRange, activeView, trendMaxPoints, trendMaxScanRows, pagination, hasMore, rangeLabel, queryRange, zoomSelection, hasZoom, refresh, loadTable, loadTrend, loadCustom, handleViewChange, handleRangeChange, handlePageChange, handlePageSizeChange, handleTrendMaxPointsChange, handleTrendMaxScanRowsChange, handleZoom, applyZoom, resetZoom, exportData }
}
