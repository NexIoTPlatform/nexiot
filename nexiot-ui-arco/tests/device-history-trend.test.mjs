import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import dayjs from 'dayjs'
import ts from 'typescript'

async function loadTs(relative, modules) {
  const source = await readFile(new URL(relative, import.meta.url), 'utf8')
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true } })
  const exported = {}
  compileFunction(outputText, ['require', 'exports'])((name) => {
    assert.ok(name in modules, `Unexpected import ${name}`)
    return modules[name]
  }, exported)
  return exported
}

let response
const requests = []
const { getDevicePropertyTrend, loadPropertyTrend, normalizePropertyTrend } = await loadTs('../src/components/PropertyTrend/api.ts', {
  '@/utils/http': { get: async (url, params, config) => {
    requests.push({ url, params, config })
    return { data: response, code: 0, success: true }
  } }
})
const { formatTrendValue, historyChartOption, historyStatistics } = await loadTs('../src/views/iot/instance/details/modules/deviceHistoryChart.ts', { dayjs })
const trendModel = await loadTs('../src/components/PropertyTrend/model.ts', { dayjs })
const { buildTrendOption } = await loadTs('../src/components/PropertyTrend/chart.ts', { dayjs, './model': trendModel })
const begin = 1790216160

function fixture(count = 25) {
  // JacksonConfig serializes long fields as strings in the real Spring MVC response.
  return {
    beginCreateTime: String(begin),
    endCreateTime: String(begin + 86400),
    intervalSeconds: '180',
    totalCount: String(count),
    numericCount: String(count),
    buckets: Array.from({ length: count }, (_, index) => ({
      time: String((begin + index * 3600) * 1000),
      count: '1',
      numericCount: '1',
      min: 1014,
      max: 1014,
      avg: 1014
    }))
  }
}

const checks = []
function check(name, run) {
  checks.push({ name, run })
}

check('正式接口 Long 字符串响应能生成真实趋势点，25 个相同值的平均值仍是 1014', async () => {
  response = fixture()
  const { data } = await getDevicePropertyTrend({})
  assert.equal(data.totalCount, 25)
  assert.equal(data.numericCount, 25)
  const statistics = historyStatistics(data)
  assert.equal(statistics.min, 1014)
  assert.equal(statistics.max, 1014)
  assert.ok(Math.abs(statistics.avg - 1014) < 1e-9)
  assert.equal(formatTrendValue(statistics.avg), '1,014')
  const option = historyChartOption(data, 'kW·h')
  for (const series of option.series) {
    const points = series.data.filter(([, value]) => value !== null)
    assert.equal(points.length, 25)
    assert.deepEqual(points[0], [begin * 1000, 1014])
    assert.deepEqual(points.at(-1), [(begin + 86400) * 1000, 1014])
  }
  assert.match(option.tooltip.formatter({ dataIndex: 0 }), /有效 1/)
  assert.match(option.tooltip.formatter({ dataIndex: 1 }), /有效 0/)
})

check('各桶记录数先转为数字，加权平均值按原始样本数计算', () => {
  const source = fixture(2)
  source.totalCount = source.numericCount = '3'
  source.buckets[0] = { ...source.buckets[0], count: '2', numericCount: '2', min: 5, max: 15, avg: 10 }
  source.buckets[1] = { ...source.buckets[1], count: '1', numericCount: '1', min: 40, max: 40, avg: 40 }
  assert.deepEqual(historyStatistics(normalizePropertyTrend(source)), { min: 5, max: 40, avg: 20 })
})

check('数字响应和字符串响应使用相同的图表与统计契约', () => {
  const normalized = normalizePropertyTrend(fixture())
  assert.deepEqual(normalizePropertyTrend(normalized), normalized)
  const source = fixture(1)
  source.totalCount = source.numericCount = '3000000000'
  source.buckets[0].count = source.buckets[0].numericCount = '3000000000'
  assert.equal(normalizePropertyTrend(source).totalCount, 3000000000)
})

check('空结果中的字符串零转换为数字零，不再误入空白图表', () => {
  const data = normalizePropertyTrend(fixture(0))
  assert.equal(data.numericCount, 0)
  assert.deepEqual(historyStatistics(data), { min: null, max: null, avg: null })
  assert.ok(historyChartOption(data, '').series.every((series) => series.data.every(([, value]) => value === null)))
})

check('缺测桶保留空值，真实数值零不会被转换成缺测', () => {
  const source = fixture(2)
  source.buckets[0] = { ...source.buckets[0], min: '0', max: '0', avg: '0' }
  source.buckets[1] = { ...source.buckets[1], numericCount: '0', min: null, max: null, avg: null }
  const data = normalizePropertyTrend(source)
  assert.deepEqual(historyStatistics(data), { min: 0, max: 0, avg: 0 })
  assert.deepEqual(historyChartOption(data, '').series[0].data[0], [begin * 1000, 0])
})

check('异常时间间隔、无效数字和不安全整数明确失败，避免无限循环或伪统计', () => {
  for (const value of ['0', '-1', 'not-a-number']) assert.throws(() => normalizePropertyTrend({ ...fixture(), intervalSeconds: value }))
  assert.throws(() => normalizePropertyTrend({ ...fixture(), intervalSeconds: '1' }))
  assert.throws(() => normalizePropertyTrend({ ...fixture(), totalCount: '9007199254740993' }))
  const source = fixture(1)
  source.buckets[0].avg = 'Infinity'
  assert.throws(() => normalizePropertyTrend(source))
})

check('大数据趋势保留采样口径和扫描元数据，提示样本统计可能遗漏尖峰', async () => {
  response = { ...fixture(2), sampled: true, countExact: false, scannedCount: '10100', scanLimit: '10000', samplingMode: 'time-window-sample' }
  const data = normalizePropertyTrend(response)
  assert.equal(data.sampled, true)
  assert.equal(data.countExact, false)
  assert.equal(data.scannedCount, 10100)
  assert.equal(data.scanLimit, 10000)
  const chart = buildTrendOption([{ key: 'M200', label: '测试属性', unit: '', trend: data }])
  assert.match(chart.tooltip.formatter({ seriesId: 'M200', value: [begin * 1000, 1014] }), /采样记录：1 条/)
  const controller = new AbortController()
  requests.length = 0
  const result = await loadPropertyTrend({ productKey: 'test', iotId: 'device', property: 'M200', beginCreateTime: begin, endCreateTime: begin + 86400, maxPoints: 100, maxScanRows: 10000 }, controller.signal)
  assert.match(result.note, /仅代表采样结果/)
  assert.match(result.note, /尖峰/)
  assert.equal(requests.length, 1, '采样趋势不能再偷偷查询原始记录')
  assert.equal(requests[0].params.maxPoints, 100)
  assert.equal(requests[0].params.maxScanRows, 10000)
  assert.equal(requests[0].params.samplingMode, 'sample')
  assert.equal(requests[0].config.signal, controller.signal)
})

check('旧全量响应保持兼容，错误采样标识被拒绝以免把样本显示为全量', () => {
  const data = normalizePropertyTrend(fixture())
  assert.equal(data.sampled, false)
  assert.equal(data.countExact, true)
  for (const change of [{ sampled: 'false' }, { countExact: 'true' }, { scannedCount: 'Infinity' }, { scanLimit: '-1' }, { samplingMode: 'unknown' }]) {
    assert.throws(() => normalizePropertyTrend({ ...fixture(), ...change }))
  }
})

for (const { name, run } of checks) {
  await run()
  process.stdout.write(`PASS ${name}\n`)
}
