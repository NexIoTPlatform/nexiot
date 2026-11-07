import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import dayjs from 'dayjs'
import ts from 'typescript'

const source = await readFile(new URL('../src/views/iot/instance/details/modules/deviceTrack.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true } })
const track = {}
const cases = []
function test(name, run) {
  cases.push({ name, run })
}
compileFunction(outputText, ['require', 'exports'])((name) => {
  if (name === 'dayjs') return dayjs
  throw new Error(`Unexpected import: ${name}`)
}, track)

test('轨迹入口只匹配 coordinates，不把位置组合或其他名称当作坐标字段', () => {
  assert.equal(track.isTrackField({ id: 'coordinates' }), true)
  assert.equal(track.isTrackField({ id: 'Coordinates' }), true)
  for (const id of ['locationInfo', 'location', 'gps', 'position', 'track', 'coordinatesText']) {
    assert.equal(track.isTrackField({ id, valueType: { type: 'geopoint' } }), false)
  }
})

test('接受合法的坐标格式，不从组合文本、JSON标量或非法经纬度猜测坐标', () => {
  for (const value of ['118.155533,28.390863', '[118.155533,28.390863]', { longitude: 118.155533, latitude: 28.390863 }, { coordinates: [118.155533, 28.390863] }]) {
    assert.deepEqual(track.parseCoordinates(value), { lng: 118.155533, lat: 28.390863 })
  }
  for (const value of ['118.155533,28.390863#东北#0.0#161', '123', '"123"', 'true', 123, null, '200,30', '118,91', '0,0', { lng: '', lat: 30 }]) {
    assert.equal(track.parseCoordinates(value), undefined)
  }
})

test('解析并按时间升序保留全部有效记录，支持秒与毫秒时间戳', () => {
  const points = track.trackPoints([
    { value: '118.2,28.2', createTime: '1790322000000' },
    { value: '118.1,28.1', createTime: '1790320000' },
    { value: '118.1,28.1', createTime: '1790321000' },
    { value: '118.1,28.1', createTime: '' },
    { value: 'not a coordinate', createTime: '1790320000' }
  ])
  assert.equal(points.length, 3)
  assert.deepEqual(points.map((point) => point.time), ['1790320000', '1790321000', '1790322000000'])
})

test('按返回游标遍历所有页面，完整保留跨页记录和进度', async () => {
  const cursors = []
  const progress = []
  const rows = await track.loadTrackRecords(async (cursor) => {
    cursors.push(cursor)
    if (!cursor) return { list: Array.from({ length: 200 }, (_, index) => ({ id: index + 1 })), hasMore: true, nextCursor: 'page2' }
    return { list: [{ id: 201 }, { id: 202 }], hasMore: false }
  }, new AbortController().signal, (count) => progress.push(count))
  assert.equal(rows.length, 202)
  assert.equal(new Set(rows.map((row) => row.id)).size, 202)
  assert.deepEqual(cursors, [undefined, 'page2'])
  assert.deepEqual(progress, [200, 202])
})

test('不接受缺失或重复游标，禁止展示被截断的轨迹', async () => {
  for (const page of [{ list: [{}], hasMore: true }, { list: [], hasMore: true, nextCursor: 'same' }, { list: [{}], hasMore: true, nextCursor: 'same' }]) {
    await assert.rejects(track.loadTrackRecords(async () => page, new AbortController().signal, () => {}), /翻页位置无效/)
  }
  await assert.rejects(track.loadTrackRecords(async () => ({ list: [{}], hasMore: 'false' }), new AbortController().signal, () => {}), /分页信息不完整/)
})

test('超出地图记录上限明确失败，正好到上限且末页允许完成', async () => {
  const pageSize = track.TRACK_PAGE_SIZE
  let page = 0
  const fetchPage = async () => {
    page += 1
    return { list: Array.from({ length: pageSize }, () => ({})), hasMore: true, nextCursor: String(page) }
  }
  await assert.rejects(track.loadTrackRecords(fetchPage, new AbortController().signal, () => {}), /超过 20,000 条定位记录/)
  assert.equal(page * pageSize, track.TRACK_MAX_RECORDS)
  page = 0
  const rows = await track.loadTrackRecords(async () => {
    const result = await fetchPage()
    return { ...result, hasMore: page * pageSize < track.TRACK_MAX_RECORDS }
  }, new AbortController().signal, () => {})
  assert.equal(rows.length, track.TRACK_MAX_RECORDS)
})

test('切换时段或关窗取消后不继续翻页或回写进度', async () => {
  const controller = new AbortController()
  let calls = 0
  let updates = 0
  await assert.rejects(track.loadTrackRecords(async () => {
    calls += 1
    controller.abort()
    return { list: [{}], hasMore: true, nextCursor: 'next' }
  }, controller.signal, () => { updates += 1 }), /已取消/)
  assert.equal(calls, 1)
  assert.equal(updates, 0)
})

for (const { name, run } of cases) {
  await run()
  process.stdout.write(`PASS ${name}\n`)
}
