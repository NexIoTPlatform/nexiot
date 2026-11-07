import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import { compileScript, parse } from '@vue/compiler-sfc'
import Dayjs from 'dayjs'
import ts from 'typescript'
import * as Vue from 'vue'

function evaluate(source, dependencies = {}) {
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
  const exports = {}
  vm.runInNewContext(outputText, {
    ...Vue,
    exports,
    structuredClone,
    require: (name) => {
      if (name === 'vue') return Vue
      if (name.endsWith('.vue')) return { default: {} }
      assert.ok(name in dependencies, `Unexpected dependency: ${name}`)
      return dependencies[name]
    }
  })
  return exports
}

function loadUtility(path, dependencies) {
  return evaluate(readFileSync(new URL(path, import.meta.url), 'utf8'), dependencies)
}

const renderer = Vue.createRenderer({
  createComment: () => ({}),
  createText: () => ({}),
  createElement: () => ({}),
  insert() {},
  remove() {},
  setText() {},
  setElementText() {},
  patchProp() {},
  parentNode: () => null,
  nextSibling: () => null
})

function mountEditor(name, model, api) {
  const filename = new URL(`../src/views/rule/linkage/modules/${name}.vue`, import.meta.url)
  const { descriptor } = parse(readFileSync(filename, 'utf8'))
  const script = compileScript(descriptor, { id: name })
  const component = evaluate(script.content, {
    '../api': api,
    '../metadataFields': loadUtility('../src/views/rule/linkage/metadataFields.ts'),
    '@/apis/iot/device': { getDeviceShadow: async () => ({ data: {} }) },
    '@/views/application/notice/modules/wechat': { noticeVariables: () => [] },
    '@/views/iot/instance/details/modules/deviceCommand': loadUtility('../src/views/iot/instance/details/modules/deviceCommand.ts')
  }).default
  component.render = () => null
  const app = renderer.createApp({
    setup: () => () => Vue.h(component, { 'modelValue': model.value, 'onUpdate:modelValue': (value) => { model.value = value } })
  })
  app.mount({})
  return { state: app._instance.subTree.component.setupState, close: () => app.unmount() }
}

async function flush() {
  await Vue.nextTick()
  await new Promise((resolve) => setImmediate(resolve))
  await Vue.nextTick()
}

const metadata = {
  properties: [{ id: 'temperature', name: '温度' }],
  events: [{ id: 'motionDetection', name: '移动侦测', valueType: { type: 'object', properties: [
    { id: 'channelId', name: '通道', valueType: { type: 'string' } },
    { id: 'alarmState', name: '告警状态', valueType: { type: 'enum' } },
    { id: 'evidence', name: '取证文件', valueType: { type: 'array', elementType: { type: 'object', properties: [{ id: 'url', name: '地址', valueType: { type: 'string' } }] } } },
    { id: 'rawData', name: '原始数据', valueType: { type: 'object', properties: [{ id: 'vendor.score', name: '厂家分数', valueType: { type: 'double' } }] } },
    { id: 'nativeAdditionalParameters', name: '原生参数', valueType: { type: 'array', elementType: { type: 'int' } } },
    { id: 'regions', name: '区域', valueType: { type: 'array', elementType: { type: 'array', elementType: { type: 'int' } } } },
    { id: 'outputData', name: '厂商事件数据', valueType: { type: 'object', properties: [{ id: '349401', name: '区域', valueType: { type: 'string' } }] } }
  ] } }],
  functions: [{ id: 'setVolume', name: '设置音量', inputs: [{ id: 'volume', name: '音量', valueType: { type: 'int' } }] }]
}

// The drawer mounts empty editors before its asynchronous detail request completes.
{
  const calls = []
  const model = Vue.ref({ filters: [] })
  const editor = mountEditor('SceneTriggerEditor', model, {
    getSceneProductMetadata: async (key) => {
      calls.push(key)
      return { data: metadata }
    }
  })
  await flush()
  assert.equal(calls.length, 0)
  model.value = { trigger: 'device', productKey: 'camera', deviceId: 'camera-1', type: 'event', modelId: 'motionDetection', filters: [{ key: 'data.channelId', operator: 'eq', value: '1' }] }
  await flush()
  assert.deepEqual(calls, ['camera'])
  assert.equal(editor.state.eventOptions[0].label, '移动侦测 (motionDetection)')
  assert.equal(editor.state.fieldOptions[0].value, 'data.channelId')
  const fields = editor.state.fieldOptions.map((item) => item.value)
  for (const path of ['data.alarmState', 'data.evidence.0.url', 'data.rawData.vendor.score', 'data.nativeAdditionalParameters.0', 'data.regions.0.0', 'data.outputData.349401']) {
    assert.ok(fields.includes(path), `事件数据字段可选择: ${path}`)
  }
  assert.equal(model.value.modelId, 'motionDetection')
  assert.equal(model.value.filters[0].value, '1')

  // Reusing a row with the same product still exposes property fields.
  model.value = { trigger: 'device', productKey: 'camera', deviceId: 'camera-1', type: 'properties', filters: [{ key: 'temperature', operator: 'gt', value: '30' }] }
  await flush()
  assert.equal(editor.state.fieldOptions[0].value, 'temperature')
  assert.equal(model.value.filters[0].value, '30')
  assert.equal(calls.length, 1)

  // New-device selection uses the same loading path; clearing it drops old choices.
  editor.state.selectDevice({ deviceId: 'meter-1', productKey: 'meter' })
  await flush()
  assert.deepEqual(calls, ['camera', 'meter'])
  editor.state.changeTrigger()
  await flush()
  assert.equal(editor.state.eventOptions.length, 0)
  assert.equal(editor.state.fieldOptions.length, 0)
  editor.close()
}

// An old product response must not replace choices for a newly selected product.
{
  const pending = new Map()
  const model = Vue.ref({ productKey: 'old' })
  const editor = mountEditor('SceneTriggerEditor', model, {
    getSceneProductMetadata: (key) => new Promise((resolve) => pending.set(key, resolve))
  })
  model.value.productKey = 'new'
  await flush()
  pending.get('new')({ data: metadata })
  await flush()
  pending.get('old')({ data: { properties: [{ id: 'obsolete', name: '旧属性' }], events: [] } })
  await flush()
  assert.equal(editor.state.fieldOptions[0].value, 'temperature')
  assert.equal(editor.state.eventOptions[0].value, 'motionDetection')
  editor.close()
}

// Notice templates and function schemas must also load after detail hydration.
{
  const model = Vue.ref({ filters: [] })
  const editor = mountEditor('SceneActionEditor', model, {
    getSceneProductMetadata: async () => ({ data: metadata }),
    getSceneDevices: async () => ({ data: { records: [] } }),
    getSceneNoticeTemplates: async () => ({ data: { records: [{ id: '14', name: '报警通知', channelType: 'wechat' }] } })
  })
  model.value = { trigger: 'notice', noticeTemplateId: '14', receivers: 'user:binding' }
  await flush()
  assert.equal(editor.state.templateOptions[0].label, '报警通知 / wechat')
  assert.equal(model.value.noticeTemplateId, '14')
  assert.equal(model.value.receivers, 'user:binding')

  model.value = { trigger: 'device', productKey: 'speaker', deviceId: 'speaker-1', type: 'functions', modelId: 'setVolume', execData: [{ id: 'volume', params: '7' }] }
  await flush()
  assert.equal(editor.state.functionOptions[0].label, '设置音量 (setVolume)')
  assert.equal(model.value.execData[0].valueType.type, 'int')
  assert.equal(model.value.execData[0].params, 7)
  assert.equal(model.value.modelId, 'setVolume')
  editor.close()
}

// Log condition values must follow the backend's exact-key priority and array paths.
{
  const { triggerFieldValue, isFilterMatched } = loadUtility('../src/views/rule/linkage/shared.ts', { dayjs: { default: Dayjs } })
  const trigger = { type: 'event' }
  const actual = { data: {
    channelId: '1',
    alarmState: 'start',
    evidence: [{ url: 'https://example.test/alarm.jpg' }],
    rawData: { 'vendor.score': 0, 'vendor': { score: 5 }, 'enabled': false },
    nativeAdditionalParameters: [0],
    regions: [[2]]
  } }
  const expected = {
    'data.channelId': '1',
    'data.alarmState': 'start',
    'data.evidence.0.url': 'https://example.test/alarm.jpg',
    'data.rawData.vendor.score': 0,
    'data.rawData.enabled': false,
    'data.nativeAdditionalParameters.0': 0,
    'data.regions.0.0': 2
  }
  for (const [path, value] of Object.entries(expected)) assert.equal(triggerFieldValue(actual, trigger, path), value, path)
  assert.equal(triggerFieldValue(actual, trigger, 'channelId'), '1', '保留不带 data 前缀的旧条件')
  assert.equal(triggerFieldValue({ data: { 'data.channelId': 'literal', 'channelId': 'nested' } }, trigger, 'data.channelId'), 'literal')
  assert.equal(triggerFieldValue(actual, trigger, 'data.evidence.9.url'), undefined)
  assert.equal(triggerFieldValue({ properties: { temperature: 30 } }, { type: 'properties' }, 'properties.temperature'), 30)
  for (const operator of ['eq', 'ne', 'not', 'gt', 'nin', 'like']) {
    for (const missing of [null, undefined]) assert.equal(isFilterMatched({ operator, value: '1' }, missing), false, `缺失值不满足 ${operator}`)
  }
  assert.equal(isFilterMatched({ operator: 'eq', value: '0' }, triggerFieldValue(actual, trigger, 'data.rawData.vendor.score')), true)
  assert.equal(isFilterMatched({ operator: 'eq', value: 'false' }, triggerFieldValue(actual, trigger, 'data.rawData.enabled')), true)

  // 精简告警只保存 cameraEventType，旧规则的 alarmType 路径仍可读取。
  for (const [field, alias] of [['alarmType', 'cameraEventType'], ['cameraEventType', 'alarmType']]) {
    for (const path of [field, `data.${field}`]) {
      assert.equal(triggerFieldValue({ data: { [alias]: 'motion' } }, trigger, path), 'motion')
      assert.equal(triggerFieldValue({ data: { [field]: 'original', [alias]: 'fallback' } }, trigger, path), 'original')
      for (const explicit of [null, '']) assert.equal(triggerFieldValue({ data: { [field]: explicit, [alias]: 'fallback' } }, trigger, path), explicit)
    }
  }
  assert.equal(triggerFieldValue({ data: { rawData: { cameraEventType: 'motion' } } }, trigger, 'data.rawData.alarmType'), undefined)
  assert.equal(triggerFieldValue({ properties: { cameraEventType: 'motion' } }, { type: 'properties' }, 'alarmType'), undefined)

  // 乐橙非空 outputData 只保存一份，历史平铺规则仍显示后端实际匹配的值。
  const outputData = { '349401': 'A', 'vendor.score': 0, 'vendor': { score: 5 }, 'detections': [{ enabled: false }] }
  const cloud = { data: { platformType: 'IMOULIFE', outputData } }
  for (const [path, expectedValue] of Object.entries({ '349401': 'A', 'vendor.score': 0, 'detections.0.enabled': false })) {
    for (const prefix of ['', 'data.', 'data.outputData.']) {
      assert.equal(triggerFieldValue(cloud, trigger, `${prefix}${path}`), expectedValue)
    }
  }
  assert.equal(triggerFieldValue({ data: { ...cloud.data, platformType: 'imoulife' } }, trigger, '349401'), 'A')
  for (const value of [null, '', false, 0]) {
    assert.equal(triggerFieldValue({ data: { ...cloud.data, 349401: value } }, trigger, 'data.349401'), value)
  }
  assert.equal(triggerFieldValue({ data: { ...cloud.data, vendor: null } }, trigger, 'data.vendor.score'), undefined)
  assert.equal(triggerFieldValue({ data: { ...cloud.data, platformType: 'EZVIZ' } }, trigger, 'data.349401'), undefined)
  assert.equal(triggerFieldValue({ properties: cloud.data }, { type: 'properties' }, '349401'), undefined)
  assert.equal(isFilterMatched({ operator: 'eq', value: 'A' }, triggerFieldValue(cloud, trigger, 'data.349401')), true)
}

process.stdout.write('Scene editor regression checks passed\n')
