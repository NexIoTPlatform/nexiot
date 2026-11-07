import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import Dayjs from 'dayjs'
import ts from 'typescript'
import { computed, effectScope, nextTick, onScopeDispose, reactive, ref, watch } from 'vue'

const pending = { table: [], trend: [] }
function defer(kind, params, signal) {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  pending[kind].push({ params, signal, resolve })
  return promise
}
const source = await readFile(new URL('../src/views/iot/instance/details/modules/useDeviceHistory.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true } })
const modules = {
  '@arco-design/web-vue': { Message: { warning: () => {}, success: () => {} } },
  'dayjs': Dayjs,
  '@/apis/iot/device': { exportDeviceMetadataLogs: async () => {} },
  '@/components/PropertyTrend/api': { loadPropertyTrend: (params, signal) => defer('trend', params, signal) },
  '@/components/PropertyTrend/model': { zoomRange: (range, event) => {
    const span = range.endCreateTime - range.beginCreateTime
    return { beginCreateTime: Math.floor(range.beginCreateTime + span * event.start / 100), endCreateTime: Math.ceil(range.beginCreateTime + span * event.end / 100) }
  } },
  './deviceHistory.api': { getDeviceMetadataSlice: (params, signal) => defer('table', params, signal) }
}
const exported = {}
compileFunction(outputText, ['require', 'exports', 'computed', 'nextTick', 'onScopeDispose', 'reactive', 'ref', 'watch'])(
  (name) => {
    assert.ok(name in modules, `Unexpected import ${name}`)
    return modules[name]
  },
  exported,
  computed,
  nextTick,
  onScopeDispose,
  reactive,
  ref,
  watch
)
async function settle() {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}
const props = reactive({ visible: true, device: { productKey: 'product', iotId: 'iot', deviceId: 'device' }, field: { id: 'M200' }, messageType: 'PROPERTIES' })
const scope = effectScope()
const history = scope.run(() => exported.useDeviceHistory(props, ref(true)))
await settle()
assert.equal(history.activeView.value, 'table')
assert.equal(pending.table.length, 1)
assert.equal(pending.trend.length, 0, '打开属性历史不能触发趋势查询')
assert.equal(pending.table[0].params.pageSize, 50)
pending.table[0].resolve({ data: { list: [{ id: 'first', value: '1' }], hasMore: true, nextCursor: 'next' } })
await settle()
assert.equal(history.rows.value[0].id, 'first')
process.stdout.write('PASS 默认先读取明细，趋势按需加载\n')

history.handleViewChange('chart')
assert.equal(pending.trend.length, 1)
assert.equal(pending.trend[0].params.maxPoints, 100)
assert.equal(pending.trend[0].params.maxScanRows, 10000)
await history.handleViewChange('table')
assert.equal(pending.trend[0].signal.aborted, true)
assert.equal(pending.table.length, 1, '已经加载的明细切回时复用结果')
pending.trend[0].resolve({ trend: { totalCount: 999, numericCount: 999 } })
await settle()
assert.equal(history.trend.value, undefined, '取消后的旧趋势不能覆盖当前结果')
assert.equal(history.trendError.value, '')
process.stdout.write('PASS 切换标签取消隐藏查询，旧结果不回写\n')

history.handleViewChange('chart')
history.handleTrendMaxScanRowsChange(50000)
assert.equal(pending.trend[1].signal.aborted, true)
assert.equal(pending.trend[2].params.maxScanRows, 50000)
pending.trend[2].resolve({ trend: { totalCount: 2, numericCount: 2 }, note: '采样' })
pending.trend[1].resolve({ trend: { totalCount: 100, numericCount: 100 } })
await settle()
assert.equal(history.trend.value.totalCount, 2)
assert.equal(history.trendNote.value, '采样')
process.stdout.write('PASS 当前标签的采样上限生效，过期趋势结果被忽略\n')

await history.handleViewChange('table')
history.handlePageChange(2)
assert.equal(pending.table[1].params.cursor, 'next')
history.handleRangeChange('1h')
assert.equal(pending.table[1].signal.aborted, true)
assert.equal(pending.table[2].params.cursor, undefined)
pending.table[1].resolve({ data: { list: [{ id: 'stale', value: '999' }], hasMore: false } })
pending.table[2].resolve({ data: { list: [{ id: 'latest', value: '2' }], hasMore: false } })
await settle()
assert.equal(history.rows.value[0].id, 'latest')
assert.equal(history.pagination.current, 1)
assert.equal(history.tableError.value, '')
process.stdout.write('PASS 修改范围重置游标，取消旧明细查询并阻止过期分页回写\n')

history.handlePageSizeChange(100)
assert.equal(pending.table[3].params.pageSize, 100)
props.visible = false
await settle()
assert.equal(pending.table[3].signal.aborted, true)
pending.table[3].resolve({ data: { list: [{ id: 'closed' }], hasMore: false } })
await settle()
assert.equal(history.rows.value.length, 0)
props.visible = true
await settle()
pending.table[4].resolve({ data: { list: [], hasMore: false } })
await settle()
history.handleViewChange('chart')
pending.trend[3].resolve({ trend: { totalCount: 2, numericCount: 2 } })
await settle()
history.handleZoom({ start: 20, end: 40 })
assert.ok(history.zoomSelection.value)
history.handleTrendMaxPointsChange(300)
assert.equal(history.zoomSelection.value, undefined, '改趋势点数重建图表时清空旧滑块选区')
pending.trend[4].resolve({ trend: { totalCount: 2, numericCount: 2 } })
await settle()
history.handleZoom({ start: 30, end: 60 })
assert.ok(history.zoomSelection.value)
history.handleTrendMaxScanRowsChange(100000)
assert.equal(history.zoomSelection.value, undefined, '改采样上限重建图表时清空旧滑块选区')
pending.trend[5].resolve({ trend: { totalCount: 2, numericCount: 2 } })
await settle()
history.handleZoom({ start: 10, end: 70 })
assert.ok(history.zoomSelection.value)
history.loadTrend()
assert.equal(history.zoomSelection.value, undefined, '重试趋势时清空旧滑块选区')
pending.trend[6].resolve({ trend: { totalCount: 2, numericCount: 2 } })
await settle()
process.stdout.write('PASS 改点数、采样上限与重试重建图表时清空旧选区\n')
scope.stop()
process.stdout.write('PASS 每页数量在明细独立设置，关闭弹窗取消请求\n')
