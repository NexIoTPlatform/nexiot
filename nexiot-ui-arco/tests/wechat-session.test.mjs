import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { compileFunction } from 'node:vm'
import ts from 'typescript'

const source = await readFile(new URL('../src/views/login/modules/useWechatSession.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source.replaceAll('import.meta.env.VITE_API_PREFIX', '\'/api\''), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
})
const flush = async () => {
  await Promise.resolve()
  await Promise.resolve()
  await Promise.resolve()
}
const deferred = () => {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { promise, resolve }
}
const makeSession = (id) => ({ sessionId: id, browserProof: `proof-${id}`, status: 'WAITING', qrUrl: '/qr.png', verificationCode: '12345678', expiresIn: 180 })

function loadSession() {
  const exported = {}
  const timers = new Map()
  const intervals = new Map()
  const sockets = []
  const cancelled = []
  let nextTimer = 0
  let readState = async () => ({ data: { status: 'WAITING' } })
  const api = {
    cancelWechatSession: async (session) => { cancelled.push(session.sessionId) },
    getWechatSession: (session) => readState(session),
    getWechatWebSocketTicket: async () => ({ data: { ticket: 'short-lived-ticket' } })
  }
  class FakeSocket {
    static OPEN = 1
    readyState = 1
    constructor(url) {
      this.url = url
      sockets.push(this)
    }

    close() {
      this.readyState = 3
      this.onclose?.()
    }

    receive(data) {
      this.onmessage?.({ data: JSON.stringify(data) })
    }
  }
  compileFunction(outputText, ['require', 'exports', 'ref', 'onBeforeUnmount', 'setInterval', 'clearInterval', 'setTimeout', 'clearTimeout', 'window', 'WebSocket'])(
    () => api,
    exported,
    (value) => ({ value }),
    () => {},
    (fn) => {
      const id = ++nextTimer
      intervals.set(id, fn)
      return id
    },
    (id) => intervals.delete(id),
    (fn) => {
      const id = ++nextTimer
      timers.set(id, fn)
      return id
    },
    (id) => timers.delete(id),
    { location: { origin: 'https://tenant.example.com' } },
    FakeSocket
  )
  return {
    model: exported.useWechatSession(),
    sockets,
    cancelled,
    intervals,
    setRead: (fn) => { readState = fn },
    poll: async () => {
      const entry = timers.entries().next().value
      assert.ok(entry, '应存在状态补查任务')
      timers.delete(entry[0])
      await entry[1]()
      await flush()
    }
  }
}

{
  const test = loadSession()
  const pending = deferred()
  const starting = test.model.start(() => pending.promise)
  test.model.cancel()
  pending.resolve({ data: makeSession('cancelled-before-create') })
  await starting
  assert.equal(test.model.session.value, null)
  assert.deepEqual(test.cancelled, ['cancelled-before-create'])
  assert.equal(test.sockets.length, 0)
}

{
  const test = loadSession()
  await test.model.start(async () => ({ data: makeSession('first') }))
  await flush()
  assert.equal(test.sockets[0].url.origin, 'wss://tenant.example.com')
  assert.equal(test.sockets[0].url.pathname, '/api/ws/wechat-auth')
  const stale = deferred()
  test.setRead(() => stale.promise)
  const polling = test.poll()
  test.sockets[0].receive({ sessionId: 'first', status: 'READY' })
  stale.resolve({ data: { status: 'WAITING' } })
  await polling
  assert.equal(test.model.status.value, 'READY', '迟到的 HTTP 结果不得撤回 WS 已确认状态')
  assert.equal(test.intervals.size, 0)
}

{
  const test = loadSession()
  await test.model.start(async () => ({ data: makeSession('old') }))
  await flush()
  const oldSocket = test.sockets[0]
  await test.model.start(async () => ({ data: makeSession('new') }))
  await flush()
  oldSocket.receive({ sessionId: 'old', status: 'READY' })
  assert.equal(test.model.status.value, 'WAITING', '旧会话的 WS 结果不得登录新会话')
  assert.equal(test.model.session.value.sessionId, 'new')
  assert.deepEqual(test.cancelled, ['old'])
  test.sockets[1].close()
  test.setRead(async () => ({ data: { status: 'READY' } }))
  await test.poll()
  assert.equal(test.model.status.value, 'READY', 'WS 断开后 HTTP 仍能完成验证')
}

{
  const test = loadSession()
  await test.model.start(async () => ({ data: makeSession('binding') }))
  await flush()
  const stale = deferred()
  test.setRead(() => stale.promise)
  const polling = test.poll()
  test.sockets[0].receive({ sessionId: 'binding', status: 'SCANNED' })
  stale.resolve({ data: { status: 'WAITING' } })
  await polling
  assert.equal(test.model.status.value, 'SCANNED', '绑定应等待页面本人确认，不自动完成')
  assert.equal(test.intervals.size, 1, '待确认绑定仍应受会话有效期限制')
  test.model.cancel()
  assert.equal(test.intervals.size, 0)
  assert.deepEqual(test.cancelled, ['binding'])
}

{
  const test = loadSession()
  await test.model.start(async () => ({ data: makeSession('failed') }))
  await flush()
  const stale = deferred()
  test.setRead(() => stale.promise)
  const polling = test.poll()
  test.model.fail('兑换已被拒绝')
  stale.resolve({ data: { status: 'READY' } })
  await polling
  assert.equal(test.model.status.value, 'FAILED', '失败后的迟到请求不能恢复登录授权')
  assert.equal(test.model.error.value, '兑换已被拒绝')
}

process.stdout.write('PASS 微信会话创建取消、迟到响应隔离、终态保持、WS 断线轮询、绑定确认与定时器清理\n')
