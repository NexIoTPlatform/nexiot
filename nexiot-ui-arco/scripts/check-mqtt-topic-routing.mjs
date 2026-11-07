import assert from 'node:assert/strict'
import { previewTopicRoute, topicMatches, validateTopicPattern } from '../src/views/iot/network/mqtt/topicRouting.ts'

function check(name, run) {
  run()
  console.log(`PASS ${name}`)
}

const mapping = (topicPattern, productKey, topicCategory = 'PASSTHROUGH') => ({ topicPattern, productKey, topicCategory, enabled: true, qos: 1 })

check('MQTT wildcard matching preserves empty levels, literal characters and system topics', () => {
  for (const [topic, filter, expected] of [
    ['hongfa/FF8A050101005543/upload/', 'hongfa/+/upload/', true],
    ['a/', 'a/+', true],
    ['a', 'a/#', true],
    ['a/b/c', 'a/#', true],
    ['a', 'a/+', false],
    ['a/b/', 'a/b', false],
    ['a.b/x', 'a.b/+', true],
    ['axb/x', 'a.b/+', false],
    ['a/(b)/x', 'a/(b)/+', true],
    ['$SYS/a', '#', false],
    ['$SYS/a', '+/a', false],
    ['$SYS/a', '$SYS/#', true],
    ['a/x', 'a/#/x', false],
    ['a/x', 'a/x+', false],
    ['a/x', '$share/group/a/+', true],
    ['a/x', '$share//a/+', false]
  ]) assert.equal(topicMatches(topic, filter), expected, `${topic} / ${filter}`)
  assert.ok(validateTopicPattern('$share//a/+'))
})

check('automatic product recognition uses the standard Topic product position', () => {
  for (const filter of ['$thing/up/#', '$thing/up/+/+/+']) {
    const result = previewTopicRoute('$thing/up/property/prod1/dev', [mapping(filter, undefined, 'THING_MODEL')], ['prod1'])
    assert.equal(result.matched, true)
    assert.equal(result.productKey, 'prod1')
    assert.equal(result.topicCategory, 'THING_MODEL')
  }
})

check('custom Topic requires one unique bound product Key', () => {
  const rows = [mapping('vendor/#')]
  assert.equal(previewTopicRoute('vendor/device123/prod1/data', rows, ['prod1']).productKey, 'prod1')
  assert.equal(previewTopicRoute('vendor/prod1/prod1/data', rows, ['prod1']).productKey, 'prod1')
  assert.equal(previewTopicRoute('vendor/prod1/prod2/data', rows, ['prod1', 'prod2']).matched, false)
  assert.equal(previewTopicRoute('vendor/device123/upload/', rows, ['prod1']).matched, false)
})

check('Hongfa Topic succeeds with explicit binding and fails clearly without a product', () => {
  const topic = 'hongfa/FF8A050101005543/upload/'
  const product = '7Gc9jZqGthEz'
  assert.equal(previewTopicRoute(topic, [mapping(topic, product)], [product]).matched, true)
  const failed = previewTopicRoute(topic, [mapping(topic)], [product])
  assert.equal(failed.matched, false)
  assert.match(failed.message, /请选择关联产品/)
  assert.equal(previewTopicRoute(topic, [mapping(topic, product)], []).matched, false)
})

check('first enabled match supplies product and category together', () => {
  const rows = [
    { ...mapping('vendor/#', 'disabled'), enabled: false },
    mapping('vendor/#', 'prod1'),
    mapping('vendor/data', 'prod2', 'THING_MODEL')
  ]
  const result = previewTopicRoute('vendor/data', rows, ['prod1', 'prod2'])
  assert.equal(result.productKey, 'prod1')
  assert.equal(result.topicCategory, 'PASSTHROUGH')
  rows[1].productKey = undefined
  assert.equal(previewTopicRoute('vendor/data', rows, ['prod1', 'prod2']).matched, false)
})

check('unbound standard products and unknown categories do not show success', () => {
  assert.equal(previewTopicRoute('$thing/up/prod1/dev', [mapping('$thing/up/#')], []).matched, false)
  const row = mapping('vendor/#', 'prod1')
  row.topicCategory = undefined
  assert.equal(previewTopicRoute('vendor/dev', [row], ['prod1']).matched, false)
})
