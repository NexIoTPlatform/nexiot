import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import ts from 'typescript'
import { computed, effectScope, nextTick, onScopeDispose, reactive, ref, watch } from 'vue'

const component = await readFile(new URL('../src/views/login/index.vue', import.meta.url), 'utf8')
const script = component.split('<script setup lang="ts">')[1].split('</script>')[0]
const { outputText } = ts.transpileModule(`${script
  .replaceAll('import.meta.env.VITE_LOGIN_GRANT', '\'password\'')
  .replaceAll('import.meta.env.VITE_LOGIN_PLATFORM_NAME', '\'\'')}
return { form, login, loadWechatOptions, loginMethod, wechatVisible, changeLoginMethod, loading }
`, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } })

function deferred() {
  let resolve
  let reject
  const promise = new Promise((done, fail) => {
    resolve = done
    reject = fail
  })
  return { promise, resolve, reject }
}

async function settle() {
  for (let turn = 0; turn < 8; turn += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

function createLogin() {
  const scope = effectScope()
  const beforeUnmount = []
  const session = ref(null)
  const status = ref('WAITING')
  const creating = ref(false)
  const calls = { created: [], passwords: 0, exchanged: [], accepted: [], redirects: [] }
  const handlers = {
    validate: async () => undefined,
    password: async () => {},
    options: async () => ({ data: { enabled: true } }),
    exchange: async () => ({ data: { access_token: 'test-token' } })
  }
  let generation = 0
  let unmounted = false

  function cancel() {
    generation += 1
    session.value = null
    creating.value = false
  }

  function unmount() {
    if (unmounted) return
    unmounted = true
    beforeUnmount.forEach((callback) => callback())
    scope.stop()
  }

  const router = {
    currentRoute: ref({ query: {} }),
    push: async (destination) => {
      calls.redirects.push(destination)
      unmount()
    }
  }
  const modules = {
    '@arco-design/web-vue': { Message: { success: () => {}, error: () => {} } },
    './modules/LoginScene.vue': { default: {} },
    '@/apis/user': { getCaptcha: async () => ({ data: { uuid: 'test', key: 'test', img: 'test' } }) },
    '@/hooks': { useLoading: () => {
      const loading = ref(false)
      return { loading, setLoading: (value) => {
        loading.value = value
      } }
    } },
    '@/stores': {
      useTabsStore: () => ({ reset: () => {} }),
      useUserStore: () => ({
        login: () => {
          calls.passwords += 1
          return handlers.password()
        },
        acceptLogin: (value) => calls.accepted.push(value)
      })
    },
    '@/utils': { resolveApiResourceUrl: (path) => path },
    './api': {
      getLoginTenantList: async () => ({ data: [] }),
      resolveLoginTenantByHost: async () => ({ data: null }),
      getWechatLoginOptions: () => handlers.options(),
      createWechatLoginSession: async (tenant) => {
        calls.created.push(tenant)
        return { data: { sessionId: `session-${calls.created.length}`, status: 'WAITING', expiresIn: 180 } }
      },
      exchangeWechatLogin: (current) => {
        calls.exchanged.push(current.sessionId)
        return handlers.exchange()
      }
    },
    // 这里只隔离会话传输；真实 Vue 调度负责验证页面的可见性和登录竞争。
    // 会话传输自身的迟到响应、WS/轮询和计时器清理由 wechat-session.test.mjs 覆盖。
    './modules/useWechatSession': { useWechatSession: () => {
      onScopeDispose(cancel)
      return {
        session,
        status,
        message: ref(''),
        error: ref(''),
        loading: creating,
        remaining: ref(180),
        cancel,
        fail: () => { status.value = 'FAILED' },
        start: async (create) => {
          cancel()
          const run = generation
          creating.value = true
          status.value = 'WAITING'
          const response = await create()
          if (run !== generation) return
          session.value = response.data
          creating.value = false
        }
      }
    } }
  }
  const globals = {
    require: (name) => {
      assert.ok(name in modules, `Unexpected import ${name}`)
      return modules[name]
    },
    exports: {},
    ref,
    reactive,
    computed,
    watch,
    defineOptions: () => {},
    useRouter: () => router,
    useRoute: () => reactive({ query: {} }),
    useTemplateRef: () => ref({ validate: () => handlers.validate() }),
    onMounted: () => {},
    onBeforeUnmount: (callback) => beforeUnmount.push(callback),
    document: { querySelector: () => ({}) },
    window: { location: { search: '', host: 'localhost' } },
    localStorage: { getItem: () => null, removeItem: () => {}, setItem: () => {} }
  }
  const model = scope.run(() => compileFunction(outputText, Object.keys(globals))(...Object.values(globals)))
  return { model, session, status, calls, handlers, unmount }
}

async function scenario(name, run) {
  const login = createLogin()
  try {
    await run(login)
    process.stdout.write(`PASS ${name}\n`)
  } finally {
    login.unmount()
  }
}

await scenario('默认扫码，切换账号密码取消二维码，切回扫码仅创建一次新会话', async ({ model, calls, session }) => {
  await model.loadWechatOptions()
  await settle()
  assert.equal(model.loginMethod.value, 'wechat')
  assert.equal(model.wechatVisible.value, true)
  assert.equal(calls.created.length, 1)
  model.changeLoginMethod('password')
  await settle()
  assert.equal(model.wechatVisible.value, false)
  assert.equal(session.value, null)
  assert.equal(calls.created.length, 1)
  model.changeLoginMethod('wechat')
  await settle()
  assert.equal(model.wechatVisible.value, true)
  assert.equal(calls.created.length, 2)
  assert.ok(session.value)
  model.changeLoginMethod('wechat')
  await settle()
  assert.equal(calls.created.length, 2)
})

await scenario('微信关闭或配置失败回退密码，重新启用后默认扫码', async ({ model, handlers, session, calls }) => {
  await model.loadWechatOptions()
  await settle()
  handlers.options = async () => ({ data: { enabled: false } })
  await model.loadWechatOptions()
  await settle()
  assert.equal(model.loginMethod.value, 'password')
  assert.equal(model.wechatVisible.value, false)
  assert.equal(session.value, null)
  handlers.options = async () => ({ data: { enabled: true } })
  await model.loadWechatOptions()
  await settle()
  assert.equal(model.loginMethod.value, 'wechat')
  assert.equal(calls.created.length, 2)
  handlers.options = async () => {
    throw new Error('unavailable')
  }
  await model.loadWechatOptions()
  await settle()
  assert.equal(model.loginMethod.value, 'password')
  assert.equal(model.wechatVisible.value, false)
  assert.equal(session.value, null)
})

await scenario('配置异步刷新保留用户手动选择的账号密码方式', async ({ model, session, calls }) => {
  await model.loadWechatOptions()
  await settle()
  model.changeLoginMethod('password')
  await settle()
  await model.loadWechatOptions()
  await settle()
  assert.equal(model.loginMethod.value, 'password')
  assert.equal(model.wechatVisible.value, false)
  assert.equal(session.value, null)
  assert.equal(calls.created.length, 1)
})

await scenario('旧租户配置响应不能重新开启二维码', async ({ model, handlers, calls }) => {
  const previous = deferred()
  handlers.options = () => previous.promise
  const pending = model.loadWechatOptions()
  handlers.options = async () => ({ data: { enabled: false } })
  await model.loadWechatOptions()
  previous.resolve({ data: { enabled: true } })
  await pending
  await settle()
  assert.equal(model.wechatVisible.value, false)
  assert.equal(calls.created.length, 0)
})

await scenario('并发密码校验只提交一次，迟到校验不释放正在提交的锁', async ({ model, handlers, calls, session }) => {
  await model.loadWechatOptions()
  await settle()
  model.changeLoginMethod('password')
  await settle()
  const validations = [deferred(), deferred()]
  const password = deferred()
  let index = 0
  handlers.validate = () => validations[index++].promise
  handlers.password = () => password.promise
  const first = model.login()
  const second = model.login()
  validations[0].resolve(undefined)
  await settle()
  assert.equal(model.loading.value, true)
  assert.equal(session.value, null)
  validations[1].resolve(undefined)
  await settle()
  assert.equal(model.loading.value, true)
  assert.equal(calls.passwords, 1)
  password.resolve()
  await Promise.all([first, second])
  await settle()
  assert.equal(calls.redirects.length, 1)
  assert.equal(calls.created.length, 1)
})

await scenario('密码表单无效或提交失败时保留密码方式，不创建隐藏二维码', async ({ model, handlers, calls, session }) => {
  await model.loadWechatOptions()
  await settle()
  model.changeLoginMethod('password')
  await settle()
  handlers.validate = async () => ({ password: 'required' })
  await model.login()
  await settle()
  assert.equal(calls.passwords, 0)
  assert.equal(session.value, null)
  handlers.validate = async () => undefined
  handlers.password = async () => {
    throw new Error('rejected')
  }
  await model.login()
  await settle()
  assert.equal(calls.created.length, 1)
  assert.equal(model.loginMethod.value, 'password')
  assert.equal(session.value, null)
})

await scenario('切回扫码后迟到的有效密码校验不提交，也不取消新二维码', async ({ model, handlers, calls, session }) => {
  await model.loadWechatOptions()
  await settle()
  model.changeLoginMethod('password')
  await settle()
  const validation = deferred()
  handlers.validate = () => validation.promise
  const password = model.login()
  model.changeLoginMethod('wechat')
  await settle()
  const current = session.value.sessionId
  validation.resolve(undefined)
  await password
  await settle()
  assert.equal(calls.passwords, 0)
  assert.equal(model.loading.value, false)
  assert.equal(session.value.sessionId, current)
  assert.equal(calls.created.length, 2)
})

await scenario('隐藏的密码登录入口不校验或提交，也不取消当前扫码会话', async ({ model, handlers, calls, session }) => {
  await model.loadWechatOptions()
  await settle()
  const current = session.value.sessionId
  handlers.validate = () => assert.fail('扫码方式不应触发表单校验')
  await model.login()
  await settle()
  assert.equal(calls.passwords, 0)
  assert.equal(session.value.sessionId, current)
})

await scenario('二维码兑换优先于尚未完成的密码校验，成功只接收一次', async ({ model, status, handlers, calls }) => {
  await model.loadWechatOptions()
  await settle()
  model.changeLoginMethod('password')
  await settle()
  const validation = deferred()
  const exchange = deferred()
  handlers.validate = () => validation.promise
  handlers.exchange = () => exchange.promise
  const password = model.login()
  model.changeLoginMethod('wechat')
  await settle()
  status.value = 'READY'
  await settle()
  validation.resolve({ password: 'required' })
  await password
  await settle()
  assert.equal(model.loading.value, true)
  assert.equal(calls.passwords, 0)
  assert.equal(calls.exchanged.length, 1)
  exchange.resolve({ data: { access_token: 'test-token' } })
  await settle()
  assert.equal(calls.accepted.length, 1)
  assert.equal(calls.redirects.length, 1)
})

for (const restore of ['hidden', 'switch', 'options']) {
  await scenario(`兑换期间 ${restore} 变更隔离旧结果，并恢复仍可见的二维码`, async ({ model, handlers, calls, status, session }) => {
    await model.loadWechatOptions()
    await settle()
    const exchange = deferred()
    handlers.exchange = () => exchange.promise
    status.value = 'READY'
    await settle()
    if (restore === 'options') {
      await model.loadWechatOptions()
    } else {
      model.changeLoginMethod('password')
      await settle()
      assert.equal(session.value, null)
      if (restore === 'switch') model.changeLoginMethod('wechat')
    }
    await settle()
    exchange.resolve({ data: { access_token: 'stale-token' } })
    await settle()
    assert.equal(calls.accepted.length, 0)
    assert.equal(calls.exchanged.length, 1)
    assert.equal(!!session.value, restore !== 'hidden')
    assert.equal(calls.created.length, restore === 'hidden' ? 1 : 2)
  })
}

for (const terminal of ['FAILED', 'EXPIRED']) {
  await scenario(`${terminal} 保留原会话和手动刷新入口，不自动重建`, async ({ model, status, handlers, calls, session }) => {
    await model.loadWechatOptions()
    await settle()
    if (terminal === 'FAILED') {
      handlers.exchange = async () => {
        throw new Error('denied')
      }
      status.value = 'READY'
    } else {
      status.value = 'EXPIRED'
    }
    await settle()
    assert.equal(status.value, terminal)
    assert.ok(session.value)
    assert.equal(calls.created.length, 1)
  })
}

for (const pending of ['exchange', 'password']) {
  await scenario(`卸载后 ${pending} 请求完成不会接收旧身份或补建二维码`, async ({ model, handlers, status, calls, session, unmount }) => {
    await model.loadWechatOptions()
    await settle()
    const response = deferred()
    let submitting
    if (pending === 'exchange') {
      handlers.exchange = () => response.promise
      status.value = 'READY'
    } else {
      model.changeLoginMethod('password')
      await settle()
      handlers.password = () => response.promise
      submitting = model.login()
    }
    await settle()
    unmount()
    if (pending === 'exchange') response.resolve({ data: { access_token: 'stale-token' } })
    else response.reject(new Error('late failure'))
    await submitting
    await settle()
    assert.equal(calls.accepted.length, 0)
    assert.equal(calls.redirects.length, 0)
    assert.equal(calls.created.length, 1)
    assert.equal(session.value, null)
  })
}
