<template>
  <main class="login">
    <aside class="login__visual" aria-label="物联网">
      <LoginScene :icon-url="activeIconUrl" :platform-name="platformName" :private-mode="passwordFocused" :is-typing="usernameFocused" />
    </aside>

    <div class="login__workspace">
      <header class="login__header">
        <div class="login__brand">
          <img :src="activeIconUrl" alt="" class="login__logo" />
          <span v-if="platformName">{{ platformName }}</span>
        </div>
        <GiThemeBtn aria-label="切换主题" />
      </header>

      <div class="login__body">
        <section class="login__panel" aria-label="登录">
          <div class="login__heading">
            <button type="button" class="login__title" aria-label="欢迎登录，连续点击三次切换租户" @click="enableTenantSelector">欢迎登录</button>
            <div v-if="tenantSelectorEnabled && !domainTenant" class="login__tenant-select">
              <a-select
                id="login-tenant"
                v-model="form.tenantCode"
                aria-label="租户"
                :options="tenantSelectOptions"
                :loading="tenantLoading"
                :disabled="loading"
                :bordered="false"
                :trigger-props="{ contentClass: 'login-tenant-popup', autoFitPopupWidth: false, autoFitPopupMinWidth: false, position: 'bottom', popupStyle: { width: '220px', maxWidth: 'calc(100vw - 48px)' } }"
                placeholder="选择租户"
                size="small"
                allow-search
                allow-clear
              />
            </div>
            <p v-else-if="activeTenant" class="login__tenant">{{ activeTenant.tenantName }}</p>
          </div>

          <a-alert v-if="tenantLoadError" type="warning" class="login__tenant-alert">
            {{ tenantLoadError }}
            <template #action>
              <a-button size="mini" @click="initializeTenantContext">重试</a-button>
            </template>
          </a-alert>

          <div class="login__entry">
            <div v-if="wechatOptions?.enabled" class="login__methods" role="group" aria-label="登录方式">
              <button type="button" class="login__method" :aria-pressed="wechatVisible" :disabled="loading" @click="changeLoginMethod('wechat')"><icon-qrcode /><span>微信扫码</span></button>
              <button type="button" class="login__method" :aria-pressed="!wechatVisible" :disabled="loading" @click="changeLoginMethod('password')"><icon-user /><span>密码登录</span></button>
            </div>

            <div class="login__auth">
              <section v-if="wechatOptions?.enabled" v-show="wechatVisible" class="login__scan" aria-label="微信扫码">
                <WechatSessionPanel
                  :session="wechatSession" :status="wechatStatus" :remaining="wechatRemaining"
                  :account-name="wechatOptions.accountName" :loading="wechatLoading" :busy="loading"
                  :message="wechatMessage" :error="wechatError" appearance="login" @refresh="startWechatLogin"
                />
              </section>

              <section v-show="!wechatVisible" class="login__password" aria-label="密码登录">
                <a-form
                  ref="formRef"
                  class="login__form"
                  size="large"
                  :model="form"
                  :rules="rules"
                  layout="vertical"
                  :disabled="loading"
                  @submit="onSubmit"
                >
                  <a-form-item field="username" label="账号">
                    <a-input v-model="form.username" placeholder="请输入账号" allow-clear autocomplete="username" @focus="usernameFocused = true" @blur="usernameFocused = false">
                      <template #prefix><icon-user class="login__input-icon" /></template>
                    </a-input>
                  </a-form-item>

                  <a-form-item field="password" label="密码">
                    <a-input-password
                      v-model="form.password"
                      placeholder="请输入密码"
                      allow-clear
                      autocomplete="current-password"
                      @focus="passwordFocused = true"
                      @blur="passwordFocused = false"
                      @press-enter="login"
                    >
                      <template #prefix><icon-lock class="login__input-icon" /></template>
                    </a-input-password>
                  </a-form-item>

                  <a-form-item v-if="captchaEnabled" field="code" label="验证码">
                    <a-input v-model="form.code" placeholder="请输入验证码" allow-clear @press-enter="login">
                      <template #append>
                        <button type="button" class="login__captcha" title="刷新验证码" aria-label="刷新验证码" :disabled="loading" @click="loadCaptcha">
                          <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                          <icon-refresh v-else />
                        </button>
                      </template>
                    </a-input>
                  </a-form-item>

                  <a-form-item hide-label class="login__remember">
                    <a-checkbox v-model="rememberAccount">记住账号</a-checkbox>
                  </a-form-item>

                  <a-form-item hide-label class="login__submit-item">
                    <a-button
                      class="login__btn"
                      type="primary"
                      size="large"
                      long
                      html-type="submit"
                      :loading="loading"
                      :disabled="loading"
                    >
                      <span>登录</span><icon-arrow-right class="login__btn-arrow" />
                    </a-button>
                  </a-form-item>
                </a-form>
              </section>
            </div>
          </div>

        </section>
      </div>
    </div>

  </main>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { LoginTenant, WechatLoginOptions } from './api'
import { Message } from '@arco-design/web-vue'
import { getCaptcha } from '@/apis/user'
import { useLoading } from '@/hooks'
import { useTabsStore, useUserStore } from '@/stores'
import { resolveApiResourceUrl } from '@/utils'
import { createWechatLoginSession, exchangeWechatLogin, getLoginTenantList, getWechatLoginOptions, resolveLoginTenantByHost } from './api'
import LoginScene from './modules/LoginScene.vue'
import { useWechatSession } from './modules/useWechatSession'
import WechatSessionPanel from './modules/WechatSessionPanel.vue'

defineOptions({ name: 'Login' })

const LOGIN_ACCOUNT_STORAGE_KEY = 'nexiot-login-account'
const DEFAULT_SITE_NAME = 'NexIoT'
const DEFAULT_LOGO_URL = '/iot.svg'
const platformName = import.meta.env.VITE_LOGIN_PLATFORM_NAME?.trim() || ''
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const tenantOptions = ref<LoginTenant[]>([])
const domainTenant = ref<LoginTenant | null>(null)
const tenantLoading = ref(false)
const tenantLoadError = ref('')
const loginMethod = ref<'password' | 'wechat'>('password')
const wechatOptions = ref<WechatLoginOptions | null>(null)
const usernameFocused = ref(false)
const passwordFocused = ref(false)
const wechatVisible = computed(() => !!wechatOptions.value?.enabled && loginMethod.value === 'wechat')
let wechatOptionsGeneration = 0
let preferredLoginMethod: 'password' | 'wechat' | undefined
let tenantContextReady = false
let disposed = false
const {
  session: wechatSession,
  status: wechatStatus,
  message: wechatMessage,
  error: wechatError,
  loading: wechatLoading,
  remaining: wechatRemaining,
  start: startWechatSession,
  cancel: cancelWechatSession,
  fail: failWechatSession
} = useWechatSession()

const form = reactive({
  tenantCode: '',
  username: '',
  password: '',
  code: '',
  uuid: ''
})

const captchaEnabled = import.meta.env.VITE_LOGIN_GRANT === 'captcha'
const captchaImage = ref('')
const rsaKey = ref('')
const formRef = useTemplateRef<FormInstance>('formRef')
const { loading, setLoading } = useLoading()
const rememberAccount = ref(false)
const manualTenantSelectorEnabled = ref(false)
let tenantSwitchClickCount = 0
let tenantSwitchClickStartedAt = 0
const TENANT_SWITCH_CLICK_WINDOW = 2000

function queryText(value: unknown) {
  if (Array.isArray(value)) return String(value[0] || '')
  return String(value || '')
}

function pageQuery(name: string) {
  return typeof window === 'undefined' ? '' : new URLSearchParams(window.location.search).get(name) || ''
}

const tenantQuery = computed(() => queryText(route.query.tenant) || pageQuery('tenant'))
const queryTenantSelectorEnabled = computed(() => tenantQuery.value.toLowerCase() === 'sys')
const tenantSelectorEnabled = computed(() => queryTenantSelectorEnabled.value || manualTenantSelectorEnabled.value)
const queryTenantCode = computed(() => queryTenantSelectorEnabled.value ? '' : tenantQuery.value.trim())
const activeTenant = computed(() => domainTenant.value || tenantOptions.value.find((item) => item.tenantCode === form.tenantCode))
const activeSiteName = computed(() => activeTenant.value?.siteName || activeTenant.value?.tenantName || DEFAULT_SITE_NAME)
const activeIconUrl = computed(() => resolveBrandResource(activeTenant.value?.siteIcon || activeTenant.value?.siteLogo))
const tenantSelectOptions = computed(() => tenantOptions.value.map((tenant) => ({
  label: tenant.tenantName,
  value: tenant.tenantCode
})))

const rules: FormInstance['rules'] = {
  username: [
    { required: true, message: '请输入账号' },
    { minLength: 2, message: '账号至少 2 个字符' }
  ],
  password: [{ required: true, message: '请输入密码' }],
  code: captchaEnabled ? [{ required: true, message: '请输入验证码' }] : []
}

function resolveBrandResource(path?: string) {
  if (!path || path === DEFAULT_LOGO_URL) return DEFAULT_LOGO_URL
  return resolveApiResourceUrl(path)
}

async function enableTenantSelector() {
  if (domainTenant.value || tenantQuery.value.trim() || manualTenantSelectorEnabled.value) {
    tenantSwitchClickCount = 0
    return
  }
  const now = Date.now()
  if (!tenantSwitchClickCount || now - tenantSwitchClickStartedAt > TENANT_SWITCH_CLICK_WINDOW) {
    tenantSwitchClickCount = 0
    tenantSwitchClickStartedAt = now
  }
  tenantSwitchClickCount += 1
  if (tenantSwitchClickCount < 3) return
  tenantSwitchClickCount = 0
  manualTenantSelectorEnabled.value = true
  await initializeTenantContext()
  if (!domainTenant.value) Message.success('已切换到多租户登录')
}

function addTenantOption(tenant: LoginTenant) {
  if (tenantOptions.value.some((item) => item.tenantCode === tenant.tenantCode)) return
  tenantOptions.value = [...tenantOptions.value, tenant]
}

function restoreLoginAccount() {
  try {
    const saved = JSON.parse(localStorage.getItem(LOGIN_ACCOUNT_STORAGE_KEY) || '{}') as {
      username?: string
      tenantCode?: string
    }
    if (saved.username) {
      form.username = saved.username
      rememberAccount.value = true
    }
    if (saved.tenantCode) form.tenantCode = saved.tenantCode
  } catch {
    localStorage.removeItem(LOGIN_ACCOUNT_STORAGE_KEY)
  }
}

function persistLoginAccount() {
  if (!rememberAccount.value) {
    localStorage.removeItem(LOGIN_ACCOUNT_STORAGE_KEY)
    return
  }
  localStorage.setItem(LOGIN_ACCOUNT_STORAGE_KEY, JSON.stringify({
    username: form.username,
    tenantCode: form.tenantCode
  }))
}

async function initializeTenantContext() {
  tenantLoading.value = true
  tenantLoadError.value = ''
  const host = typeof window === 'undefined' ? '' : window.location.host
  try {
    const [listResult, domainResult] = await Promise.allSettled([
      getLoginTenantList(),
      host ? resolveLoginTenantByHost(host) : Promise.resolve({ data: null })
    ])
    tenantOptions.value = listResult.status === 'fulfilled' ? listResult.value.data || [] : []
    const resolved = domainResult.status === 'fulfilled' ? domainResult.value.data : null
    if (resolved?.tenantCode) {
      domainTenant.value = resolved
      form.tenantCode = resolved.tenantCode
      addTenantOption(resolved)
      return
    }
    domainTenant.value = null
    if (queryTenantCode.value) {
      form.tenantCode = queryTenantCode.value
      const matched = tenantOptions.value.find((item) => item.tenantCode === queryTenantCode.value)
      if (!matched) tenantLoadError.value = '租户不存在'
    } else if (!tenantSelectorEnabled.value) {
      form.tenantCode = ''
    }
    if (listResult.status === 'rejected' && tenantSelectorEnabled.value) {
      tenantLoadError.value = '租户列表加载失败'
    }
  } finally {
    tenantLoading.value = false
  }
}

async function loadCaptcha() {
  if (!captchaEnabled) return
  rsaKey.value = ''
  form.uuid = ''
  form.code = ''
  const res = await getCaptcha()
  form.uuid = res.data.uuid
  rsaKey.value = res.data.key || ''
  captchaImage.value = res.data.img.startsWith('data:') ? res.data.img : `data:image/png;base64,${res.data.img}`
}

async function login() {
  if (loading.value || loginMethod.value !== 'password') return
  let submitted = false
  let authenticated = false
  try {
    const valid = await formRef.value?.validate()
    if (valid || loading.value || loginMethod.value !== 'password') return
    submitted = true
    setLoading(true)
    // 密码提交接管本次登录，避免扫码结果同时兑换。
    cancelWechatSession()
    await userStore.login({ ...form, rsaKey: rsaKey.value, tenantCode: form.tenantCode || undefined })
    authenticated = true
    persistLoginAccount()
    await finishLogin()
  } catch (error) {
    Message.error((error as Error).message || '登录失败，请重试')
    await loadCaptcha()
  } finally {
    if (submitted) {
      setLoading(false)
      if (!authenticated && wechatVisible.value && !wechatSession.value) startWechatLogin()
    }
  }
}

async function finishLogin() {
  tabsStore.reset()
  const { redirect, tenant: _tenant, template: _legacyTemplate, ...otherQuery } = router.currentRoute.value.query
  await router.push({ path: (redirect as string) || '/', query: otherQuery })
  Message.success('登录成功')
}

async function loadWechatOptions() {
  const run = ++wechatOptionsGeneration
  cancelWechatSession()
  wechatOptions.value = null
  try {
    const response = await getWechatLoginOptions(form.tenantCode)
    if (run !== wechatOptionsGeneration) return
    wechatOptions.value = response.data
    const desiredMethod = response.data.enabled ? preferredLoginMethod || 'wechat' : 'password'
    if (loginMethod.value !== desiredMethod) loginMethod.value = desiredMethod
  } catch {
    if (run === wechatOptionsGeneration) loginMethod.value = 'password'
  }
}

function changeLoginMethod(method: string | number) {
  if (method !== 'password' && method !== 'wechat') return
  preferredLoginMethod = method
  usernameFocused.value = false
  passwordFocused.value = false
  loginMethod.value = method
}

function startWechatLogin() {
  if (disposed || !wechatVisible.value || loading.value) return
  startWechatSession(() => createWechatLoginSession(form.tenantCode))
}

watch(wechatVisible, (visible) => {
  if (visible) startWechatLogin()
  else cancelWechatSession()
})

watch([wechatStatus, loading], async ([status, busy]) => {
  const current = wechatSession.value
  if (status !== 'READY' || !current || busy || !wechatVisible.value) return
  let authenticated = false
  setLoading(true)
  try {
    const response = await exchangeWechatLogin(current)
    if (wechatSession.value?.sessionId !== current.sessionId || !wechatVisible.value) return
    userStore.acceptLogin(response.data)
    authenticated = true
    await finishLogin()
  } catch (cause) {
    failWechatSession((cause as Error).message || '微信登录失败，请重新获取二维码。')
  } finally {
    setLoading(false)
    if (!authenticated && wechatVisible.value && !wechatSession.value) startWechatLogin()
  }
})

watch(() => form.tenantCode, () => {
  if (tenantContextReady) loadWechatOptions()
})

function onSubmit(data: { values: Record<string, unknown>, errors: Record<string, unknown> | undefined }) {
  if (data.errors) return
  login()
}

watch([activeSiteName, activeIconUrl], ([siteName, iconUrl]) => {
  document.title = siteName
  let favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (!favicon) {
    favicon = document.createElement('link')
    favicon.rel = 'icon'
    document.head.append(favicon)
  }
  favicon.href = iconUrl
}, { immediate: true })

watch(() => route.query.tenant, () => {
  tenantSwitchClickCount = 0
  manualTenantSelectorEnabled.value = false
  initializeTenantContext()
})

onMounted(async () => {
  restoreLoginAccount()
  await Promise.all([initializeTenantContext(), loadCaptcha()])
  tenantContextReady = true
  await loadWechatOptions()
})

onBeforeUnmount(() => {
  disposed = true
  wechatOptionsGeneration += 1
})
</script>

<style lang="scss" scoped>
.login {
  box-sizing: border-box;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 1fr);
  width: 100%;
  height: 100dvh;
  min-height: 100dvh;
  overflow-y: auto;
  color: var(--color-text-1);
  background: var(--color-bg-1);

  &__visual {
    height: 100dvh;
    min-height: 600px;
  }

  &__workspace {
    position: relative;
    min-width: 0;
    min-height: 100dvh;
  }

  &__header {
    position: absolute;
    inset: 0 0 auto;
    z-index: 1;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    height: 112px;
    padding: 40px 48px;
  }

  &__brand {
    display: none;
    gap: 10px;
    align-items: center;
    font-size: 17px;
    font-weight: 600;
  }

  &__logo {
    width: 32px;
    height: 32px;
    object-fit: contain;
  }

  &__body {
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100dvh;
    padding: 88px 48px;
  }

  &__panel {
    width: 360px;
    max-width: 100%;
  }

  &__heading {
    margin-bottom: 24px;
    text-align: center;
  }

  &__title {
    padding: 0;
    margin: 0;
    font-family: inherit;
    font-size: 28px;
    font-weight: 600;
    line-height: 1.4;
    color: var(--color-text-1);
    letter-spacing: -0.5px;
    cursor: pointer;
    background: none;
    border: 0;

    &:focus-visible {
      outline: 2px solid rgb(var(--primary-6));
      outline-offset: 5px;
      border-radius: 2px;
    }
  }

  &__tenant {
    margin: 6px 0 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--color-text-3);
    white-space: nowrap;
  }

  &__tenant-alert {
    margin-bottom: 24px;
  }

  &__tenant-select {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-3);

    :deep(.arco-select-view-single) {
      justify-content: center;
      width: auto;
      min-width: 112px;
      max-width: 240px;
      height: 28px;
      padding: 0 6px;
      font-size: 12px;
      line-height: 28px;
      color: var(--color-text-2);
      background: transparent;
      border: 0;
      border-radius: 0;
      box-shadow: none;

      &:hover,
      &:focus-within {
        color: var(--color-text-1);
        background: transparent;
      }
    }

    :deep(.arco-select-view-value) {
      width: auto;
      padding: 0;
      text-align: center;
    }

    :deep(.arco-select-view-input) {
      text-align: center;
    }

    :deep(.arco-select-view-suffix) {
      padding-left: 6px;
      font-size: 10px;
    }
  }

  &__entry {
    min-width: 0;
  }

  &__methods {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 20px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__method {
    position: relative;
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: center;
    height: 40px;
    padding: 0 8px 12px;
    font-family: inherit;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-3);
    cursor: pointer;
    background: transparent;
    border: 0;
    transition: color 0.2s ease;

    .arco-icon {
      font-size: 14px;
    }

    &::after {
      position: absolute;
      bottom: -1px;
      left: 50%;
      width: 24px;
      height: 2px;
      content: '';
      background: transparent;
      border-radius: 2px;
      transform: translateX(-50%);
      transition: background-color 0.2s ease;
    }

    &[aria-pressed='true'] {
      color: var(--color-text-1);

      &::after {
        background: #7364db;
      }
    }

    &:hover {
      color: var(--color-text-1);
    }

    &:focus-visible {
      outline: 2px solid rgb(var(--primary-6));
      outline-offset: 2px;
    }

    &:disabled {
      cursor: default;
      opacity: 0.6;
    }
  }

  &__auth {
    min-width: 0;
  }

  &__scan {
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  &__password {
    min-width: 0;
  }

  &__form {
    :deep(.arco-form-item) {
      margin-bottom: 14px;
    }

    :deep(.arco-form-item-label-col) {
      margin-bottom: 6px;
      line-height: 20px;
    }

    :deep(.arco-form-item-label-col > label) {
      font-size: 12px;
      font-weight: 500;
      line-height: 20px;
      color: var(--color-text-2);
    }

    :deep(.arco-form-item-label-required-symbol) {
      display: none;
    }

    :deep(.arco-input-prefix) {
      margin-right: 10px;
    }

    :deep(.arco-input-append) {
      padding: 0 4px;
      background: var(--color-bg-1);
      border: 0;
      border-bottom: 1px solid var(--color-border-2);
      border-radius: 0;
    }
  }

  &__input-icon {
    font-size: 17px;
    color: var(--color-text-3);
  }

  :deep(.arco-input-wrapper) {
    box-sizing: border-box;
    height: 44px;
    padding: 0;
    background: transparent;
    border: 0;
    border-bottom: 1px solid var(--color-border-2);
    border-radius: 0;
    transition: border-color 0.2s ease;

    &:hover {
      border-bottom-color: var(--color-text-4);
    }

    &:focus-within {
      border-bottom-color: #7364db;
      box-shadow: none;
    }
  }

  &__captcha {
    display: grid;
    place-items: center;
    width: 100px;
    height: 44px;
    padding: 0;
    overflow: hidden;
    color: var(--color-text-3);
    cursor: pointer;
    background: none;
    border: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__form &__remember {
    margin: 2px 0 20px;

    :deep(.arco-checkbox-label) {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__form &__submit-item {
    margin-bottom: 0;
  }

  &__btn {
    height: 44px;
    font-size: 14px;
    font-weight: 600;
    color: var(--color-bg-1);
    background: var(--color-text-1);
    border: 1px solid transparent;
    border-radius: 4px;
    transition: background-color 0.2s ease, color 0.2s ease;

    &:not(.arco-btn-disabled):hover {
      color: var(--color-bg-1);
      background: #6655d2;
      border-color: transparent;

      .login__btn-arrow {
        width: 16px;
        margin-left: 8px;
        opacity: 1;
        transform: translateX(0);
      }
    }
  }

  &__btn-arrow {
    width: 0;
    height: 16px;
    margin-left: 0;
    opacity: 0;
    transform: translateX(-4px);
    transition: width 0.2s ease, margin 0.2s ease, opacity 0.2s ease, transform 0.2s ease;
  }

  &__downloads {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: center;
    margin-top: 24px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__downloads-trigger {
    display: inline-flex;
    gap: 4px;
    align-items: center;
    padding: 0;
    font: inherit;
    color: var(--color-text-2);
    cursor: pointer;
    background: transparent;
    border: 0;
    transition: color 0.2s ease;

    &:hover {
      color: #6655d2;
    }

    &:focus-visible {
      outline: 2px solid rgb(var(--primary-6));
      outline-offset: 4px;
      border-radius: 2px;
    }
  }

  &__download-modal {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__download-option {
    display: flex;
    gap: 12px;
    align-items: center;
    width: 100%;
    padding: 14px 16px;
    color: var(--color-text-1);
    text-align: left;
    cursor: pointer;
    background: var(--color-bg-1);
    border: 1px solid var(--color-border-2);
    border-radius: 6px;
    transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;

    &:hover:not(:disabled) {
      background: var(--color-fill-1);
      border-color: #7364db;
      box-shadow: 0 4px 14px rgb(30 32 55 / 8%);
    }

    &:focus-visible {
      outline: 2px solid rgb(var(--primary-6));
      outline-offset: 2px;
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.5;
    }
  }

  &__download-option-icon {
    display: grid;
    flex: 0 0 40px;
    place-items: center;
    width: 40px;
    height: 40px;
    color: var(--color-text-1);
  }

  &__download-option-logo {
    width: 32px;
    height: 32px;
    fill: currentcolor;

    &--windows {
      color: #0078d4;
    }
  }

  &__download-option-content {
    display: flex;
    flex: 1;
    align-items: center;
    min-width: 0;

    strong {
      font-size: 14px;
      font-weight: 600;
    }
  }

  &__download-option-action {
    flex: 0 0 auto;
    color: var(--color-text-3);
  }
}

/* The tenant menu is teleported to body; this class belongs only to the login select. */
:global(.login-tenant-popup .arco-select-dropdown) {
  padding: 4px;
  background: var(--color-bg-1);
  border: 0;
  border-radius: 2px;
  box-shadow: 0 8px 28px rgb(30 32 55 / 10%);
}

:global(.login-tenant-popup .arco-select-option) {
  padding: 6px 10px;
  font-size: 12px;
  line-height: 20px;
  background: transparent;
  border-radius: 2px;
}

:global(.login-tenant-popup .arco-select-option-active) {
  background: var(--color-fill-1);
}

:global(.login-tenant-popup .arco-select-option-selected) {
  font-weight: 500;
  color: var(--color-text-1);
  background: var(--color-fill-1);
}

:global(.login-tenant-popup .arco-select-option-selected .arco-icon) {
  color: #7364db;
}

@media (width <= 900px) {
  .login {
    grid-template-columns: minmax(0, 1fr);

    &__visual {
      display: none;
    }

    &__header {
      justify-content: space-between;
      height: 88px;
      padding: 28px;
    }

    &__brand {
      display: flex;
    }

    &__body {
      align-items: flex-start;
      padding: 128px 28px 48px;
    }

    &__title {
      font-size: 28px;
    }
  }
}

@media (height <= 700px) and (width > 900px) {
  .login {
    &__header {
      height: 80px;
      padding: 24px 32px;
    }

    &__body {
      align-items: flex-start;
      padding: 96px 40px 40px;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .login {
    &__method,
    &__btn,
    &__btn-arrow,
    :deep(.arco-input-wrapper),
    :deep(.arco-select-view-single) {
      transition: none;
    }
  }
}
</style>
