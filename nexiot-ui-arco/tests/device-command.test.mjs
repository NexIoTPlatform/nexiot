import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'

const source = readFileSync(new URL('../src/views/iot/instance/details/modules/deviceCommand.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
const exports = {}
vm.runInNewContext(outputText, { exports })
const { autoFillCommandFields: fill, enumOptions, coerceRuntimeValue, normalizeInputs, fieldType } = exports
const field = (id, extra = {}) => ({ id, valueType: { type: 'string' }, ...extra })
const shadow = (reported, desired) => ({ metadata: { state: { reported, desired } } })
const device = { deviceId: 'phone-1001', productKey: 'sip-product', deviceName: 'device name', configuration: '{"audioUrl":"legacy"}', devConfiguration: '{"audioUrl":"device","deviceName":"configured name"}' }

{
  const fields = [field('audioUrl'), field('deviceName'), field('productKey'), field('fallback', { defaultValue: 'default' })]
  const result = fill(fields, device, shadow({ audioUrl: 'reported' }, { audioUrl: 'desired' }))
  assert.equal(result.values.audioUrl, 'reported')
  assert.equal(result.sources.audioUrl, 'reported')
  assert.equal(result.values.deviceName, 'configured name')
  assert.equal(result.values.productKey, 'sip-product')
  assert.equal(result.values.fallback, 'default')
}
{
  const result = fill([field('audioUrl'), field('zero'), field('flag', { valueType: { type: 'boolean' } })], device, shadow({ audioUrl: ' ', zero: 0, flag: false }, { audioUrl: 'desired', zero: 10, flag: true }))
  assert.equal(result.values.audioUrl, 'desired')
  assert.equal(result.values.zero, '0')
  assert.equal(result.values.flag, false)
  assert.equal(fill([field('audioUrl')], device, shadow({}, {})).values.audioUrl, 'device')
}
{
  const result = fill([field('callId', { expands: { autoFill: false }, defaultValue: 'old-call' }), field('deviceSecret'), field('imei')], { ...device, deviceSecret: 'secret' }, shadow({ callId: 'ended-call' }, {}))
  assert.equal(result.values.callId, undefined)
  assert.equal(result.values.deviceSecret, undefined)
  assert.equal(result.values.imei, undefined)
}
{
  const result = fill([field('flag', { valueType: { type: 'boolean', default: true } }), field('audioUrl', { defaultValue: 'default' }), field('settings', { valueType: { type: 'object' } })], device, shadow({ flag: true }, {}), false)
  assert.equal(result.values.flag, undefined)
  assert.equal(result.values.audioUrl, undefined)
  assert.equal(result.values.settings, undefined)
  assert.equal(Object.keys(result.sources).length, 0)
}
{
  const url = 'https://oss.example.com/alert.wav?signature=a%2Bb'
  const input = field('audioUrl', { valueType: { type: 'enum', elements: [{ text: '报警提示音', value: url }] } })
  assert.equal(enumOptions(input)[0].label, '报警提示音')
  assert.equal(enumOptions(input)[0].value, url)
  assert.equal(fill([input], device, shadow({}, { audioUrl: url })).values.audioUrl, url)
}
{
  const numeric = field('slaveAddress', { valueType: { type: 'int' } })
  const text = field('lightMode')
  const result = fill([numeric, text], device, shadow({ slaveAddress: '9', lightMode: '06' }, {}))
  assert.equal(result.values.slaveAddress, 9)
  assert.equal(result.values.lightMode, '06')
  assert.equal(JSON.stringify(result.values), '{"slaveAddress":9,"lightMode":"06"}')
  assert.equal(coerceRuntimeValue(numeric, '1.5'), undefined)
  assert.equal(coerceRuntimeValue(numeric, 'Infinity'), undefined)
  assert.equal(coerceRuntimeValue(field('enabled', { valueType: 'boolean' }), 'false'), false)
  assert.equal(coerceRuntimeValue(field('enabled', { valueType: 'boolean' }), 'invalid'), undefined)
  const inputs = normalizeInputs([{ id: 'address', valueType: 'int' }, { id: 'level', valueType: '{"type":"long"}' }])
  assert.equal(fieldType(inputs[0]), 'int')
  assert.equal(fieldType(inputs[1]), 'long')
  assert.equal(fill(inputs, device, shadow({ address: '01', level: '2' }, {})).values.address, 1)
}
{
  const numeric = field('mode', { valueType: { type: 'enum', elements: [{ value: 6, text: '闪烁' }] }, defaultValue: '6' })
  const text = field('textMode', { valueType: { type: 'enum', elements: [{ value: '06', text: '闪烁' }] }, defaultValue: '06' })
  const explicit = field('explicit', { valueType: { type: 'enum', valueType: 'int', elements: [{ value: '06', text: '闪烁' }] }, defaultValue: '06' })
  const result = fill([numeric, text, explicit], device, shadow({}, {}))
  assert.equal(result.values.mode, 6)
  assert.equal(result.values.textMode, '06')
  assert.equal(result.values.explicit, 6)
  assert.equal(enumOptions(explicit)[0].value, 6)
}
