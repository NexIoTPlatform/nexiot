import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import process from 'node:process'
import vm from 'node:vm'
import ts from 'typescript'

function loadModule(path, dependencies = {}) {
  const source = readFileSync(new URL(path, import.meta.url), 'utf8')
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } })
  const exports = {}
  vm.runInNewContext(outputText, { exports, require: (name) => {
    if (!(name in dependencies)) throw new Error(`Unexpected dependency: ${name}`)
    return dependencies[name]
  } })
  return exports
}

const commands = loadModule('../src/views/iot/instance/details/modules/deviceCommand.ts')
const { buildDeviceAccessParameters: build, getDeviceAccessValuePresentation: present } = loadModule('../src/views/iot/instance/details/modules/deviceAccessParameters.ts', {
  './deviceCommand': commands
})

const cases = []
function test(name, run) {
  cases.push({ name, run })
}

test('objects and arrays have compact summaries and complete formatted details', () => {
  const configuration = {
    hosting: { platform: 'sample', enabled: true },
    channelList: [{ channelId: '01' }, { channelId: '02' }],
    emptyObject: {},
    emptyArray: []
  }
  const items = build(configuration)
  for (const item of items) {
    const value = configuration[item.key]
    const result = present(item)
    assert.equal(result.kind, Array.isArray(value) ? 'array' : 'object')
    assert.equal(result.expandable, true)
    assert.equal(result.detailValue, JSON.stringify(value, null, 2))
    assert.equal(item.rawValue, JSON.stringify(value))
    assert.equal(item.displayValue, JSON.stringify(value))
  }
  assert.equal(present(items[0]).previewValue, '对象 · 2 个字段')
  assert.equal(present(items[1]).previewValue, '数组 · 2 项')
  assert.equal(present(items[2]).previewValue, '对象 · 0 个字段')
  assert.equal(present(items[3]).previewValue, '数组 · 0 项')
})

test('serialized JSON is summarized while copied source retains whitespace and numeric lexemes', () => {
  const json = '  { "serial": "000123", "timestamp": 1791678901234567890, "channels": [1, 2] }  '
  const arrays = '\n [ "01", "02" ] \n'
  const items = build({ snapshot: json, channels: arrays })
  assert.equal(present(items[0]).kind, 'object')
  assert.equal(present(items[0]).previewValue, '对象 · 3 个字段')
  assert.equal(present(items[0]).detailValue.includes('1791678901234567890'), true)
  assert.equal(present(items[1]).kind, 'array')
  assert.equal(present(items[1]).previewValue, '数组 · 2 项')
  assert.equal(present(items[1]).detailValue, '[\n  "01",\n  "02"\n]')
  assert.equal(items[0].rawValue, json)
  assert.equal(items[0].displayValue, json)
  assert.equal(items[1].rawValue, arrays)
  assert.equal(items[1].displayValue, arrays)
})

test('formatted JSON preserves nested large integers and original decimal and exponent notation', () => {
  const source = '{ "nested": { "timestamp": 1791678901234567890, "negative": -9007199254740993 }, "numbers": [1.2300, -0, 6.022E+23, 1e-07, 1e999] }'
  const item = build({ snapshot: source })[0]
  const expected = [
    '{',
    '  "nested": {',
    '    "timestamp": 1791678901234567890,',
    '    "negative": -9007199254740993',
    '  },',
    '  "numbers": [',
    '    1.2300,',
    '    -0,',
    '    6.022E+23,',
    '    1e-07,',
    '    1e999',
    '  ]',
    '}'
  ].join('\n')
  assert.equal(present(item).detailValue, expected)
  assert.equal(item.rawValue, source)
  assert.equal(item.displayValue, source)
})

test('JSON punctuation inside strings and escaped quotes never become layout tokens', () => {
  const source = String.raw`{"punctuation":"a, b: {c} [d]", "escaped":"say \"hello\"; path C:\\camera", "spacing":"  first\nsecond\tline  ", "unicode":"\u4e2d\u6587"}`
  const item = build({ snapshot: source })[0]
  const expected = [
    '{',
    '  "punctuation": "a, b: {c} [d]",',
    String.raw`  "escaped": "say \"hello\"; path C:\\camera",`,
    String.raw`  "spacing": "  first\nsecond\tline  ",`,
    String.raw`  "unicode": "\u4e2d\u6587"`,
    '}'
  ].join('\n')
  assert.equal(present(item).detailValue, expected)
  assert.equal(JSON.stringify(JSON.parse(present(item).detailValue)), JSON.stringify(JSON.parse(source)))
  assert.equal(item.rawValue, source)
})

test('nested empty arrays and objects stay compact without creating blank lines', () => {
  const item = build({ snapshot: '{"items": [ {}, [] ], "emptyObject": { }, "emptyArray": [ ]}' })[0]
  assert.equal(present(item).detailValue, [
    '{',
    '  "items": [',
    '    {},',
    '    []',
    '  ],',
    '  "emptyObject": {},',
    '  "emptyArray": []',
    '}'
  ].join('\n'))
  assert.equal(present(build({ object: ' { } ' })[0]).detailValue, '{}')
  assert.equal(present(build({ array: ' [ ] ' })[0]).detailValue, '[]')
})

test('invalid JSON falls back to the original text without throwing or losing its contents', () => {
  const short = '{not-json}'
  const long = `{"unfinished":"${'x'.repeat(100)}`
  const items = build({ short, long })
  assert.equal(present(items[0]).kind, 'scalar')
  assert.equal(present(items[0]).detailValue, short)
  assert.equal(present(items[1]).kind, 'long-text')
  assert.equal(present(items[1]).detailValue, long)
  assert.equal(items[1].rawValue, long)
})

test('long or multiline text has a bounded single-line preview and full detail and copy values', () => {
  const long = '长'.repeat(120)
  const multiline = 'first line\r\nsecond\tline'
  const items = build({ long, multiline, boundary: 'x'.repeat(80) })
  for (const item of items.slice(0, 2)) {
    const result = present(item)
    assert.equal(result.kind, 'long-text')
    assert.equal(result.expandable, true)
    assert.equal(/[\r\n]/.test(result.previewValue), false)
    assert.ok(result.previewValue.length <= 80)
    assert.equal(result.detailValue, item.displayValue)
    assert.equal(item.rawValue, item.key === 'long' ? long : multiline)
  }
  assert.equal(present(items[0]).previewValue, `${'长'.repeat(79)}…`)
  assert.equal(present(items[1]).previewValue, 'first line second line')
  assert.equal(present(items[2]).kind, 'scalar')
})

test('empty values, zero, false and mapped enums preserve their existing readable display', () => {
  const items = build({ missing: undefined, empty: '', nothing: null, zero: 0, flag: false, parity: 'none' })
  const expected = ['-', '-', '-', '0', '否 (false)', '无校验 (None)']
  for (const [index, item] of items.entries()) {
    const result = present(item)
    assert.equal(result.kind, 'scalar')
    assert.equal(result.expandable, false)
    assert.equal(result.previewValue, expected[index])
    assert.equal(result.detailValue, expected[index])
  }
  assert.equal(items[3].rawValue, '0')
  assert.equal(items[4].rawValue, 'false')
  assert.equal(items[5].rawValue, 'none')
})

test('numeric identifiers and timestamps never pass through JSON parsing or lose leading zeros', () => {
  const values = ['1791678901234', '1791678901234567890', '0001791678901234567890', '3.14159265358979323846', '1e100', 'null', 'true']
  for (const value of values) {
    const item = build({ timestamp: value })[0]
    const result = present(item)
    assert.equal(result.kind, 'scalar', value)
    assert.equal(result.expandable, false, value)
    assert.equal(result.previewValue, value)
    assert.equal(result.detailValue, value)
    assert.equal(item.rawValue, value)
  }
  const numericTimestamp = build({ timestamp: 1791678901234 })[0]
  assert.equal(present(numericTimestamp).previewValue, '1791678901234')
})

test('secret values remain masked and cannot open structured or long-text details', () => {
  const values = { apiToken: { privateValue: 'private-object' }, password: 'private-text'.repeat(20), emptySecret: '' }
  const items = build(values)
  for (const item of items) {
    const result = present(item)
    assert.equal(item.secret, true)
    assert.equal(item.rawValue, '')
    assert.equal(result.kind, 'scalar')
    assert.equal(result.expandable, false)
    assert.equal(result.previewValue, item.displayValue)
    assert.equal(result.detailValue, item.displayValue)
    assert.equal(JSON.stringify({ item, result }).includes('private-'), false)
  }
  assert.equal(items[0].displayValue, '已设置（不回显）')
  assert.equal(items[2].displayValue, '未配置')
  const legacy = { ...items[0], rawValue: '{"privateValue":"private-object"}' }
  assert.equal(present(legacy).expandable, false)
  assert.equal(JSON.stringify(present(legacy)).includes('private-'), false)
})

for (const { name, run } of cases) {
  run()
  process.stdout.write(`PASS ${name}\n`)
}
