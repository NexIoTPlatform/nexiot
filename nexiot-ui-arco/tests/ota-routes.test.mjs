import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import { camelCase, upperFirst } from 'lodash-es'
import ts from 'typescript'
import { createMemoryHistory, createRouter } from 'vue-router'

const printer = ts.createPrinter()
async function extractDeclarations(relative, names) {
  const text = await readFile(new URL(relative, import.meta.url), 'utf8')
  const source = ts.createSourceFile(relative, text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const statements = names.map((name) => {
    const statement = source.statements.find((item) => (
      ts.isVariableStatement(item) && item.declarationList.declarations.some((declaration) => declaration.name.getText(source) === name)
    ) || (ts.isFunctionDeclaration(item) && item.name?.text === name))
    assert.ok(statement, `Missing production declaration: ${name}`)
    return printer.printNode(ts.EmitHint.Unspecified, statement, source).replace(/^export /gm, '')
  })
  return ts.transpileModule(`${statements.join('\n')}\nreturn { ${names.join(', ')} };`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
}

const routeCode = await extractDeclarations('../src/router/index.ts', ['otaRoutes', 'legacyOtaRoutes', 'errorRoutes'])
const routeDeclarations = compileFunction(routeCode, ['Layout'])({})
// 页面组件不挂载；其余 path/name/redirect/meta 与生产声明保持一致。
function stubComponents(route) {
  return {
    ...route,
    ...(route.component ? { component: {} } : {}),
    ...(route.children ? { children: route.children.map(stubComponents) } : {})
  }
}
function createOtaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [routeDeclarations.otaRoutes, ...routeDeclarations.legacyOtaRoutes, ...routeDeclarations.errorRoutes].map(stubComponents)
  })
}

const nameCode = await extractDeclarations('../src/utils/index.ts', ['transformPathToName'])
const { transformPathToName } = compileFunction(nameCode, ['isExternal', 'camelCase', 'upperFirst'])(() => false, camelCase, upperFirst)
const menuCode = await extractDeclarations('../src/stores/useRouteStore.ts', ['EXTERNAL_URL_RE', 'TRAILING_SLASH_RE', 'formatAsyncRoutes'])
const { formatAsyncRoutes } = compileFunction(menuCode, ['Layout', 'ParentView', 'MigrationPending', 'loadView', 'transformPathToName'])({}, {}, {}, () => ({}), transformPathToName)
function addDynamicMenus(router) {
  const menus = formatAsyncRoutes([{
    path: '/iot',
    component: 'Layout',
    children: [{
      path: 'ota',
      component: 'ParentView',
      children: [
        { path: 'package', component: 'system/ota/package/index', title: '升级包' },
        { path: 'task', component: 'system/ota/task/index', title: '升级计划' }
      ]
    }]
  }])
  menus.forEach((route) => router.addRoute(route))
}

const entries = [
  { page: 'task', name: 'IotOtaTask' },
  { page: 'package', name: 'IotOtaPackage' }
]
let checks = 0
async function check(name, run) {
  await run()
  checks++
  process.stdout.write(`PASS ${name}\n`)
}

await check('OTA 静态深链在菜单加载前即可直接导航', async () => {
  const router = createOtaRouter()
  for (const { page, name } of entries) {
    await router.push(`/iot/ota/${page}`)
    assert.equal(router.currentRoute.value.name, name)
    assert.equal(router.currentRoute.value.meta.activeMenu, `/iot/ota/${page}`)
  }
})

await check('旧 OTA 深链重定向并保留 query/hash', async () => {
  const router = createOtaRouter()
  for (const { page, name } of entries) {
    await router.push({ path: `/ota/${page}`, query: { productKey: 'product-a', deviceId: 'device-a', create: '1' }, hash: '#progress' })
    assert.equal(router.currentRoute.value.path, `/iot/ota/${page}`)
    assert.equal(router.currentRoute.value.name, name)
    assert.deepEqual(router.currentRoute.value.query, { productKey: 'product-a', deviceId: 'device-a', create: '1' })
    assert.equal(router.currentRoute.value.hash, '#progress')
  }
})

await check('真实动态菜单格式化产生同名路由，覆盖后新深链仍可用', async () => {
  const router = createOtaRouter()
  addDynamicMenus(router)
  for (const { page, name } of entries) {
    await router.push(`/iot/ota/${page}`)
    assert.equal(router.currentRoute.value.name, name)
    assert.equal(router.currentRoute.value.meta.sourceComponent, `system/ota/${page}/index`)
  }
})

await check('动态同名覆盖后旧深链继续保留入口及全部参数', async () => {
  const router = createOtaRouter()
  addDynamicMenus(router)
  for (const { page, name } of entries) {
    const query = { productKey: 'product-b', deviceId: 'device-b', create: '1', tag: ['one', 'two'] }
    await router.push({ path: `/ota/${page}`, query, hash: '#device' })
    assert.equal(router.currentRoute.value.path, `/iot/ota/${page}`)
    assert.equal(router.currentRoute.value.name, name)
    assert.deepEqual(router.currentRoute.value.query, query)
    assert.equal(router.currentRoute.value.hash, '#device')
  }
})

await check('设备及产品入口的 named 导航始终到达标准路径', async () => {
  for (const dynamic of [false, true]) {
    const router = createOtaRouter()
    if (dynamic) addDynamicMenus(router)
    const query = { productKey: 'product-c', deviceId: 'device-c', create: '1' }
    await router.push({ name: 'IotOtaTask', query })
    assert.equal(router.currentRoute.value.path, '/iot/ota/task')
    assert.deepEqual(router.currentRoute.value.query, query)
    await router.push({ name: 'IotOtaPackage', query: { productKey: 'product-c' } })
    assert.equal(router.currentRoute.value.path, '/iot/ota/package')
  }
})

process.stdout.write(`OTA 路由回归 ${checks}/${checks} 通过\n`)
