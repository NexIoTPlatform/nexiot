import assert from 'node:assert/strict'
import { Buffer } from 'node:buffer'
import { constants, generateKeyPairSync, privateDecrypt } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import process from 'node:process'
import { compileFunction } from 'node:vm'
import ts from 'typescript'

const require = createRequire(import.meta.url)
const JSEncrypt = require('jsencrypt')

const source = await readFile(new URL('../src/apis/user/index.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source.replaceAll('import.meta.env.VITE_LOGIN_GRANT', 'loginGrant'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true }
})

function loadLogin(loginGrant) {
  const exported = {}
  const requests = []
  compileFunction(outputText, ['require', 'exports', 'loginGrant'])((name) => {
    if (name === 'jsencrypt') return JSEncrypt
    if (name === '@/utils') return { resolveApiResourceUrl: (url) => url }
    if (name === '@/utils/http') return { request: (config) => requests.push(config) }
    throw new Error(`Unexpected import: ${name}`)
  }, exported, loginGrant)
  return { login: exported.login, requests }
}

function createKeys() {
  const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 1024 })
  return { rsaKey: publicKey.export({ type: 'spki', format: 'der' }).toString('base64'), privateKey }
}

function decryptPassword(ciphertext, privateKey) {
  // Node 22 禁用 PKCS1 私钥解密：用独立的 OpenSSL RSA 运算验证 Java RSA 所需的 PKCS#1 v1.5 编码。
  const block = privateDecrypt({ key: privateKey, padding: constants.RSA_NO_PADDING }, Buffer.from(ciphertext, 'base64'))
  assert.equal(block[0], 0)
  assert.equal(block[1], 2)
  const separator = block.indexOf(0, 2)
  assert.ok(separator >= 10, 'PKCS#1 v1.5 至少包含 8 字节随机填充')
  return block.subarray(separator + 1).toString('utf8')
}

const firstKeys = createKeys()
const input = Object.freeze({ username: 'example', password: 'Login@123+&=', code: 'abcd', uuid: 'captcha-id', tenantCode: 'example', rsaKey: firstKeys.rsaKey })
const captcha = loadLogin('captcha')
captcha.login(input)
const request = captcha.requests[0]
assert.equal(request.url, '/oauth/token')
assert.equal(request.headers['Content-Type'], 'application/x-www-form-urlencoded')
const form = new URLSearchParams(request.data.toString())
assert.notEqual(form.get('password'), input.password)
assert.equal(decryptPassword(form.get('password'), firstKeys.privateKey), input.password)
assert.deepEqual(Object.fromEntries([...form].filter(([key]) => key !== 'password')), {
  username: input.username,
  grant_type: 'captcha',
  client_id: 'web',
  client_secret: 'web',
  code: input.code,
  uuid: input.uuid,
  tenantCode: input.tenantCode
})
process.stdout.write('PASS 验证码登录提交可被 RSA 私钥解密的密码，并保留 OAuth、验证码和租户参数\n')

captcha.login(input)
assert.equal(decryptPassword(captcha.requests[1].data.get('password'), firstKeys.privateKey), input.password)
assert.notEqual(captcha.requests[1].data.get('password'), form.get('password'))
process.stdout.write('PASS 重试不修改原始表单、不重复加密上一次密文\n')

const refreshedKeys = createKeys()
captcha.login({ ...input, rsaKey: refreshedKeys.rsaKey, uuid: 'refreshed-captcha' })
assert.equal(decryptPassword(captcha.requests[2].data.get('password'), refreshedKeys.privateKey), input.password)
assert.equal(captcha.requests[2].data.get('uuid'), 'refreshed-captcha')
process.stdout.write('PASS 刷新后使用新的公钥和验证码标识\n')

assert.throws(() => captcha.login({ ...input, rsaKey: undefined }), /登录密钥未加载/)
assert.throws(() => captcha.login({ ...input, rsaKey: 'invalid-key' }), /密码加密失败/)
assert.equal(captcha.requests.length, 3)
process.stdout.write('PASS 公钥缺失或无效时阻止提交明文密码\n')

for (const grant of ['password', undefined]) {
  const plain = loadLogin(grant)
  plain.login({ username: input.username, password: input.password })
  assert.equal(plain.requests[0].data.get('grant_type'), 'password')
  assert.equal(plain.requests[0].data.get('password'), input.password)
}
process.stdout.write('PASS password 模式与默认模式保留原有登录协议\n')
