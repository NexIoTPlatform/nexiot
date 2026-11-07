import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import Dayjs from 'dayjs'
import ts from 'typescript'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'

const component = await readFile(new URL('../src/views/iot/instance/details/modules/DeviceRealtimePanel.vue', import.meta.url), 'utf8')
const script = component.match(/<script setup lang="tsx">([\s\S]*?)<\/script>/)[1]
const sourceFile = ts.createSourceFile('DeviceRealtimePanel.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX)
const printer = ts.createPrinter()
const body = sourceFile.statements.filter((statement) => !ts.isImportDeclaration(statement)).map((statement) => printer.printNode(ts.EmitHint.Unspecified, statement, sourceFile)).join('\n')
const { outputText } = ts.transpileModule(`${body}\nreturn { loadData, loadEventTotals, refreshData, activeType, loading, reported, eventTotals, eventsLoading, eventsError };`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } })
const requests = []
let shadows = 0
let now = 1_000_000
const scope = effectScope()
const bindings = {
  computed,
  reactive,
  ref,
  watch,
  Dayjs,
  Date: { now: () => now },
  defineOptions: () => {},
  defineProps: () => ({ device: { productKey: 'product', iotId: 'device' }, product: { id: 'product' } }),
  useUserStore: () => ({}),
  onMounted: () => {},
  onBeforeUnmount: () => {},
  getProductProperties: async () => ({ data: [{ id: 'temperature' }] }),
  getProductEvents: async () => ({ data: [{ id: 'alarm' }] }),
  batchGetDeviceShadows: async () => ({ data: [{ metadata: { reported: { temperature: ++shadows } } }] }),
  getDeviceEventTotals: () => new Promise((resolve, reject) => requests.push({ resolve, reject }))
}
const panel = scope.run(() => compileFunction(outputText, Object.keys(bindings))(...Object.values(bindings)))
async function settle() {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

try {
  await panel.loadData()
  await panel.loadEventTotals()
  assert.equal(requests.length, 0, '属性页不请求事件统计')
  assert.equal(panel.reported.temperature, 1)

  panel.activeType.value = 'events'
  await settle()
  assert.equal(requests.length, 1, '切入事件页签开始加载')
  assert.equal(panel.eventsLoading.value, true)
  await panel.loadData()
  assert.equal(panel.reported.temperature, 2, '事件请求未完成时影子仍独立更新')
  assert.equal(panel.loading.value, false)
  await panel.loadEventTotals(true)
  assert.equal(requests.length, 1, '请求未完成时不重复发起')
  requests[0].resolve({ data: { records: [{ id: 'alarm', qty: '99+' }] } })
  await settle()
  assert.equal(panel.eventTotals.value[0].qty, '99+')

  now += 29_000
  await panel.loadEventTotals()
  assert.equal(requests.length, 1, '30秒内复用统计')
  now += 1_000
  const failed = panel.loadEventTotals()
  assert.equal(requests.length, 2)
  requests[1].reject(new Error('timeout'))
  await failed
  assert.match(panel.eventsError.value, /暂不可用/)
  assert.equal(panel.eventTotals.value[0].qty, '99+', '超时保留旧结果，不伪装成零')
  assert.equal(panel.eventsLoading.value, false)

  const retry = panel.loadEventTotals(true)
  assert.equal(requests.length, 3, '手动刷新可立即重试')
  requests[2].resolve({ data: { records: [{ id: 'alarm', qty: '99+' }] } })
  await retry
  assert.equal(panel.eventsError.value, '')
  panel.activeType.value = 'properties'
  now += 30_000
  await settle()
  await panel.loadEventTotals()
  assert.equal(requests.length, 3, '隐藏事件页签不轮询')
  process.stdout.write('PASS 事件按需加载、影子独立刷新、请求去重、30秒节流、超时降级及手动重试\n')
} finally {
  scope.stop()
}
