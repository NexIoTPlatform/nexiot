import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import ts from 'typescript'
import { effectScope, nextTick, reactive, ref, watch } from 'vue'

const printer = ts.createPrinter()
async function readScript(relative) {
  const text = await readFile(new URL(relative, import.meta.url), 'utf8')
  const script = text.match(/<script setup[^>]*>([\s\S]*?)<\/script>/)?.[1]
  assert.ok(script, `Missing production script: ${relative}`)
  return ts.createSourceFile(relative, script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX)
}
function printStatements(source, statements) {
  return statements.map((item) => printer.printNode(ts.EmitHint.Unspecified, item, source)).join('\n')
}
function extractFunctions(source, names) {
  return printStatements(source, names.map((name) => {
    const statement = source.statements.find((item) => ts.isFunctionDeclaration(item) && item.name?.text === name)
    assert.ok(statement, `Missing production function: ${name}`)
    return statement
  }))
}
function execute(code, bindings) {
  const { outputText } = ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2020 } })
  return compileFunction(outputText, Object.keys(bindings))(...Object.values(bindings))
}
function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { promise, resolve }
}
async function settle() {
  for (let i = 0; i < 5; i++) {
    await Promise.resolve()
    await nextTick()
  }
}

const taskSource = await readScript('../src/views/iot/ota/task/index.vue')
const routeStatements = taskSource.statements.filter((item) => (
  ts.isVariableStatement(item) && item.declarationList.declarations.some((declaration) => declaration.name.getText(taskSource) === 'routeRequestId')
) || (ts.isExpressionStatement(item) && item.getText(taskSource).startsWith('watch(() => route.fullPath,')))
assert.equal(routeStatements.length, 2, '必须执行真实路由 watcher 及请求序号声明')
const routeCode = printStatements(taskSource, routeStatements)
const initialProductStatement = taskSource.statements.find((item) => ts.isExpressionStatement(item) && item.getText(taskSource).startsWith('getProductList({ page: 1, size: 500 }).then('))
assert.ok(initialProductStatement, '必须执行真实初始产品列表加载逻辑')
const initialProductCode = printStatements(taskSource, [initialProductStatement])

function routeHarness(getProductList, open = async () => {}) {
  const route = reactive({ name: 'IotOtaTask', fullPath: '/iot/ota/task', query: {} })
  const products = ref([])
  const replaced = []
  const opened = []
  const scope = effectScope()
  scope.run(() => execute(routeCode, {
    watch,
    route,
    query: reactive({ productKey: '' }),
    products,
    getProductList,
    nextTick,
    formDrawerRef: ref({ open: async (...args) => {
      opened.push(args)
      await open(...args)
    } }),
    router: { replace: async (target) => {
      replaced.push(target)
      route.query = target.query
      route.fullPath = `/iot/ota/task?productKey=${target.query.productKey}`
    } },
    search: () => {}
  }))
  function navigate(productKey, deviceId) {
    route.query = { create: '1', productKey, deviceId }
    route.fullPath = `/iot/ota/task?create=1&productKey=${productKey}&deviceId=${deviceId}`
  }
  return { route, products, opened, replaced, navigate, stop: () => scope.stop() }
}

const formSource = await readScript('../src/views/iot/ota/modules/OtaTaskFormDrawer.vue')
const formCode = extractFunctions(formSource, ['createForm', 'reset', 'handleProduct', 'findTargetDevice', 'open'])
function formHarness(getProductDeviceList) {
  const visible = ref(false)
  const productLoading = ref(false)
  const deviceOptions = ref([])
  const warnings = []
  const form = execute(`let requestId = 0; let deviceRequestId = 0; ${formCode}\nconst form = reactive(createForm()); return { form, open, reset };`, {
    reactive,
    visible,
    productLoading,
    deviceOptions,
    deviceSearchLoading: ref(false),
    profileLoaded: ref(false),
    profile: reactive({}),
    packages: ref([]),
    availableProducts: ref([{ id: '1', productKey: 'product-a' }, { id: '2', productKey: 'product-b' }]),
    formRef: ref(),
    createTransferProfile: () => ({ mode: 'URL' }),
    getOtaPackageList: async () => ({ data: { records: [] } }),
    getOtaTransferProfile: async () => ({ data: { mode: 'URL' } }),
    getProductDeviceList,
    Message: { warning: (value) => warnings.push(value) }
  })
  return { ...form, visible, productLoading, deviceOptions, warnings }
}

let checks = 0
async function check(name, run) {
  await run()
  checks++
  process.stdout.write(`PASS ${name}\n`)
}

await check('旧产品请求未完成时，新设备创建入口仍立即执行且只消费最新 query', async () => {
  const pendingA = deferred()
  const requests = []
  const harness = routeHarness(async ({ productKey }) => {
    requests.push(productKey)
    return productKey === 'product-a' ? pendingA.promise : { data: { records: [{ productKey }] } }
  })
  try {
    harness.navigate('product-a', 'device-a')
    await settle()
    harness.navigate('product-b', 'device-b')
    await settle()
    assert.deepEqual(requests, ['product-a', 'product-b'])
    assert.deepEqual(harness.opened, [['product-b', ['device-b']]])
    assert.deepEqual(harness.replaced, [{ query: { productKey: 'product-b' } }])
    pendingA.resolve({ data: { records: [{ productKey: 'product-a' }] } })
    await settle()
    assert.deepEqual(harness.products.value.map((item) => item.productKey), ['product-b'])
    assert.equal(harness.opened.length, 1)
    assert.equal(harness.replaced.length, 1)
  } finally { harness.stop() }
})

await check('旧弹窗仍在加载时可打开新设备，旧完成事件不移除新入口参数', async () => {
  const pendingA = deferred()
  const pendingB = deferred()
  const harness = routeHarness(async ({ productKey }) => ({ data: { records: [{ productKey }] } }), (key) => key === 'product-a' ? pendingA.promise : pendingB.promise)
  try {
    harness.navigate('product-a', 'device-a')
    await settle()
    harness.navigate('product-b', 'device-b')
    await settle()
    assert.deepEqual(harness.opened, [['product-a', ['device-a']], ['product-b', ['device-b']]])
    pendingA.resolve()
    await settle()
    assert.equal(harness.replaced.length, 0)
    assert.equal(harness.route.query.deviceId, 'device-b')
    pendingB.resolve()
    await settle()
    assert.deepEqual(harness.replaced, [{ query: { productKey: 'product-b' } }])
  } finally { harness.stop() }
})

await check('离开缓存任务页后，旧查询不会弹窗或改写其他页面参数', async () => {
  const pending = deferred()
  const harness = routeHarness(() => pending.promise)
  try {
    harness.navigate('product-a', 'device-a')
    await settle()
    harness.route.name = 'IotInstance'
    harness.route.query = { tab: 'ota' }
    harness.route.fullPath = '/system/instance/1206?tab=ota'
    await settle()
    pending.resolve({ data: { records: [{ productKey: 'product-a' }] } })
    await settle()
    assert.equal(harness.opened.length, 0)
    assert.equal(harness.replaced.length, 0)
    assert.deepEqual(harness.route.query, { tab: 'ota' })
  } finally { harness.stop() }
})

await check('首次 500 条产品列表迟到返回时，保留按入口补入的旧产品并合并去重', async () => {
  const initialRequest = deferred()
  const harness = routeHarness(async ({ productKey }) => ({ data: { records: [{ productKey }] } }))
  try {
    execute(initialProductCode, { getProductList: () => initialRequest.promise, products: harness.products })
    harness.navigate('old-product', 'device-a')
    await settle()
    assert.equal(harness.products.value[0].productKey, 'old-product')
    harness.products.value.push({ productKey: 'fresh-0' })
    initialRequest.resolve({ data: { records: Array.from({ length: 500 }, (_, index) => ({ productKey: `fresh-${index}` })) } })
    await settle()
    assert.equal(harness.products.value.length, 501)
    assert.equal(harness.products.value.some((item) => item.productKey === 'old-product'), true)
    assert.equal(harness.products.value.filter((item) => item.productKey === 'fresh-0').length, 1)
  } finally { harness.stop() }
})

await check('超过 500 台且编号模糊匹配超过 50 条时，逐页找到准确目标并去重预选', async () => {
  const devices = Array.from({ length: 700 }, (_, index) => ({ productKey: 'product-a', deviceId: `device-${699 - index}` }))
  const pages = []
  const harness = formHarness(async ({ page, size, productKey, deviceId }) => {
    const matches = devices.filter((item) => item.productKey === productKey && (!deviceId || item.deviceId.includes(deviceId)))
    if (deviceId) pages.push(page)
    return { data: { records: matches.slice((page - 1) * size, page * size), total: matches.length } }
  })
  await harness.open('product-a', ['device-1', 'device-1'])
  assert.deepEqual(pages, [1, 2, 3])
  assert.deepEqual(harness.form.deviceIds, ['device-1'])
  assert.equal(harness.deviceOptions.value.filter((item) => item.value === 'device-1').length, 1)
  assert.equal(harness.warnings.length, 0)
  assert.equal(harness.productLoading.value, false)
})

await check('模糊匹配包含另一产品同编号时，只选当前产品且搜索耗尽后才提示缺失', async () => {
  const pages = []
  const harness = formHarness(async ({ page, deviceId }) => {
    if (!deviceId) return { data: { records: [], total: 0 } }
    pages.push(page)
    const records = page === 1 ? [{ productKey: 'product-ab', deviceId }] : []
    return { data: { records, total: 2 } }
  })
  await harness.open('product-a', ['device-1'])
  assert.deepEqual(pages, [1, 2])
  assert.deepEqual(harness.form.deviceIds, [])
  assert.equal(harness.warnings.length, 1)
})

await check('关闭弹窗后忽略正在返回的分页，停止后续查询且不覆盖再次打开的产品', async () => {
  const secondPage = deferred()
  const pages = []
  const harness = formHarness(async ({ page, productKey, deviceId }) => {
    if (!deviceId) return { data: { records: [{ productKey, deviceId: 'other' }], total: 1 } }
    pages.push(page)
    if (page === 2) return secondPage.promise
    return { data: { records: Array.from({ length: 50 }, (_, index) => ({ productKey, deviceId: `device-1-${index}` })), total: 120 } }
  })
  const oldOpen = harness.open('product-a', ['device-1'])
  await settle()
  assert.deepEqual(pages, [1, 2])
  assert.equal(harness.productLoading.value, true)
  harness.visible.value = false
  harness.reset()
  await harness.open('product-b')
  secondPage.resolve({ data: { records: [{ productKey: 'product-a', deviceId: 'device-1' }], total: 120 } })
  await oldOpen
  assert.deepEqual(pages, [1, 2])
  assert.equal(harness.form.productKey, 'product-b')
  assert.deepEqual(harness.form.deviceIds, [])
  assert.equal(harness.deviceOptions.value.some((item) => item.value === 'device-1'), false)
  assert.equal(harness.warnings.length, 0)
  assert.equal(harness.productLoading.value, false)
})

process.stdout.write(`OTA 创建入口回归 ${checks}/${checks} 通过\n`)
