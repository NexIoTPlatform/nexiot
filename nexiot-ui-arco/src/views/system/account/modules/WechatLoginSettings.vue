<template>
  <section class="wechat-settings">
    <div class="wechat-settings__heading">
      <div><h2>公众号登录</h2><p>按租户配置公众号、微信验证方式和演示登录账号。</p></div>
      <a-button :loading="loading" :disabled="saving || (isPlatformAdmin && !selectedTenantId)" @click="loadConfig"><template #icon><icon-refresh /></template>刷新配置</a-button>
    </div>
    <a-form-item v-if="isPlatformAdmin" label="管理租户"><a-select v-model="selectedTenantId" :options="tenantOptions" :loading="tenantsLoading" :disabled="saving" placeholder="请选择需要配置公众号的租户" allow-search /></a-form-item>
    <a-alert type="info" class="wechat-settings__notice">个人主体公众号可使用“发送一次码”：用户扫码关注公众号后，发送页面显示的 8 位一次码完成验证，无需 AppSecret。带参数二维码需要公众号实际具备相应接口权限。</a-alert>
    <a-spin :loading="loading" class="wechat-settings__spin">
      <a-empty v-if="isPlatformAdmin && !selectedTenantId" description="请先选择需要管理的租户" />
      <a-result v-else-if="loadFailed" status="error" title="公众号配置加载失败"><template #extra><a-button @click="loadConfig">重新加载</a-button></template></a-result>
      <a-form v-else ref="formRef" :model="form" layout="vertical" class="wechat-settings__form">
        <a-form-item label="微信登录"><a-switch v-model="form.enabled" /><span class="wechat-settings__switch-label">在当前租户登录页显示微信登录入口</span></a-form-item>
        <a-row :gutter="20">
          <a-col :xs="24" :md="12"><a-form-item field="accountName" label="公众号名称" :rules="[{ required: true, message: '请输入公众号名称' }]"><a-input v-model.trim="form.accountName" :max-length="80" placeholder="显示在登录二维码下方" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item field="appId" label="AppID" :rules="[{ required: true, message: '请输入公众号 AppID' }]"><a-input v-model.trim="form.appId" placeholder="wx 开头的公众号 AppID" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="微信验证方式">
          <a-select v-model="form.capabilityMode" :options="capabilityOptions" />
          <template #extra>个人主体公众号建议保留“发送一次码”；填写 AppSecret 不会自动切换验证方式。自动模式会尝试带参数二维码，接口权限不足时使用发送一次码。</template>
        </a-form-item>
        <a-form-item field="accountQrUrl" label="公众号静态二维码" :rules="staticQrRules">
          <a-input v-model.trim="form.accountQrUrl" placeholder="填写可公开访问的二维码图片地址" allow-clear />
          <div class="wechat-settings__qr-field">
            <img v-if="qrPreview" :src="qrPreview" alt="公众号二维码预览" />
            <div>
              <input ref="qrInputRef" class="wechat-settings__file" type="file" accept="image/png,image/jpeg,image/webp" @change="uploadQr" />
              <a-button :loading="uploading" @click="qrInputRef?.click()"><template #icon><icon-upload /></template>上传二维码</a-button>
              <p>用于发送一次码模式；请上传公众号后台下载的二维码图片，最大 5 MB。</p>
            </div>
          </div>
        </a-form-item>
        <a-divider>微信服务器配置</a-divider>
        <a-form-item v-if="form.callbackUrl" label="回调 URL"><a-input :model-value="form.callbackUrl" readonly /><template #extra>将此地址填写到公众号后台的服务器配置中。</template></a-form-item>
        <a-form-item label="Token">
          <a-input-password v-model.trim="form.callbackToken" autocomplete="new-password" :placeholder="form.callbackTokenConfigured ? '已配置；留空保留原值' : '与公众号服务器配置中的 Token 一致'" />
        </a-form-item>
        <a-form-item label="公众号消息加密方式">
          <a-alert>自动按收到的消息识别明文、兼容或安全模式，并使用对应方式回复，无需手动切换。</a-alert>
        </a-form-item>
        <a-form-item label="EncodingAESKey">
          <a-input-password v-model.trim="form.encodingAESKey" autocomplete="new-password" :max-length="43" :placeholder="form.encodingAESKeyConfigured ? '已配置；留空保留原值' : '43 位消息加解密密钥'" />
          <template #extra>兼容或安全模式须填写与公众号后台一致的密钥；明文消息无需密钥。已保存的密钥可保留，无需清除也能接收明文消息。留空保留原值，加载配置时不会回显。</template>
        </a-form-item>
        <a-form-item label="AppSecret">
          <a-input-password v-model.trim="form.appSecret" autocomplete="new-password" :placeholder="form.appSecretConfigured ? '已配置；留空保留原值' : '发送一次码模式无需填写'" />
          <template #extra>带参数二维码需要 AppSecret 和对应接口权限；仅有 AppID 不代表具备这些权限。</template>
        </a-form-item>
        <a-divider>登录与绑定策略</a-divider>
        <a-form-item label="登录模式"><a-radio-group v-model="form.loginMode" type="button"><a-radio value="BOUND_USER">登录已绑定账号</a-radio><a-radio value="DEMO_ACCOUNT">演示账号登录</a-radio></a-radio-group></a-form-item>
        <a-alert v-if="form.loginMode === 'DEMO_ACCOUNT'" type="warning" class="wechat-settings__notice">开启后，任何完成微信验证的用户都会进入下方指定账号。请使用权限受限的演示账号。</a-alert>
        <a-form-item v-if="form.loginMode === 'DEMO_ACCOUNT'" field="demoUserUnionId" label="演示账号" :rules="[{ required: true, message: '请选择当前租户的演示账号' }]">
          <a-select v-model="form.demoUserUnionId" :options="userOptions" :loading="usersLoading" allow-search :filter-option="false" placeholder="搜索并选择当前租户账号" @search="searchUsers" />
          <template #extra>扫码用户无需先绑定；账号启用状态、所属租户和权限仍由后端校验。</template>
        </a-form-item>
        <a-form-item label="个人中心绑定"><a-switch v-model="form.bindingEnabled" /><span class="wechat-settings__switch-label">允许当前租户用户绑定自己的微信</span></a-form-item>
        <a-form-item field="expireSeconds" label="会话有效期（秒）"><a-input-number v-model="form.expireSeconds" :min="60" :max="600" :step="30" /></a-form-item>
        <a-space><a-button type="primary" :loading="saving" :disabled="loading || uploading" @click="save"><template #icon><icon-save /></template>保存公众号配置</a-button><a-button :disabled="saving" @click="loadConfig">恢复</a-button></a-space>
      </a-form>
    </a-spin>
  </section>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { WechatDemoUser, WechatLoginConfig } from '../wechatApi'
import { Message } from '@arco-design/web-vue'
import { uploadOssFile } from '@/apis/iot/oss'
import { useUserStore } from '@/stores'
import { resolveApiResourceUrl } from '@/utils'
import { getLoginTenantList } from '@/views/login/api'
import { getWechatDemoUsers, getWechatLoginConfig, saveWechatLoginConfig } from '../wechatApi'

defineOptions({ name: 'WechatLoginSettings' })

const capabilityOptions = [
  { label: '发送一次码（个人主体公众号可用）', value: 'MESSAGE_CODE' },
  { label: '带参数二维码（需要接口权限）', value: 'SCENE_QR' },
  { label: '自动选择可用方式', value: 'AUTO' }
]
const formRef = useTemplateRef<FormInstance>('formRef')
const qrInputRef = useTemplateRef<HTMLInputElement>('qrInputRef')
const loading = ref(false)
const userStore = useUserStore()
const isPlatformAdmin = computed(() => userStore.userInfo.currentAdmin)
const selectedTenantId = ref('')
const tenantOptions = ref<Array<{ label: string, value: string }>>([])
const tenantsLoading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const loadFailed = ref(false)
const usersLoading = ref(false)
const users = ref<WechatDemoUser[]>([])
const form = reactive<WechatLoginConfig>({
  enabled: false,
  accountName: '',
  appId: '',
  appSecret: '',
  callbackToken: '',
  encodingAESKey: '',
  accountQrUrl: '',
  capabilityMode: 'MESSAGE_CODE',
  loginMode: 'BOUND_USER',
  bindingEnabled: true,
  demoUserUnionId: '',
  expireSeconds: 180
})
let searchTimer: ReturnType<typeof setTimeout> | undefined
let userSearchGeneration = 0
let configGeneration = 0
const managedTenantId = computed(() => isPlatformAdmin.value ? selectedTenantId.value : undefined)
const qrPreview = computed(() => resolveApiResourceUrl(form.accountQrUrl))
const staticQrRules = computed(() => form.capabilityMode !== 'SCENE_QR' ? [{ required: true, message: '请填写或上传公众号静态二维码' }] : [])
const userOptions = computed(() => {
  const options = users.value.map((user) => ({ label: user.nickname ? `${user.nickname}（${user.username}）` : user.username, value: user.unionId }))
  if (form.demoUserUnionId && !options.some((option) => option.value === form.demoUserUnionId)) options.push({ label: form.demoUserUnionId, value: form.demoUserUnionId })
  return options
})

async function loadUsers(keyword?: string) {
  const run = ++userSearchGeneration
  usersLoading.value = true
  try {
    const response = await getWechatDemoUsers(keyword, managedTenantId.value)
    if (run === userSearchGeneration) users.value = response.data || []
  } finally {
    if (run === userSearchGeneration) usersLoading.value = false
  }
}

function searchUsers(keyword: string) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadUsers(keyword).catch(() => {}), 300)
}

async function loadConfig() {
  if (isPlatformAdmin.value && !selectedTenantId.value) return
  const run = ++configGeneration
  loading.value = true
  loadFailed.value = false
  try {
    const response = await getWechatLoginConfig(managedTenantId.value)
    if (run !== configGeneration) return
    Object.assign(form, response.data, { appSecret: '', callbackToken: '', encodingAESKey: '' })
    formRef.value?.clearValidate()
    await loadUsers()
  } catch {
    if (run === configGeneration) loadFailed.value = true
  } finally {
    if (run === configGeneration) loading.value = false
  }
}

async function uploadQr(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    Message.warning('请选择不超过 5 MB 的 PNG、JPG 或 WebP 图片')
    return
  }
  uploading.value = true
  try {
    form.accountQrUrl = (await uploadOssFile(file)).data.url
    Message.success('公众号二维码已上传，请保存配置')
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (await formRef.value?.validate()) return
  if (form.encodingAESKey && !/^[A-Za-z0-9+/]{43}$/.test(form.encodingAESKey)) {
    Message.warning('EncodingAESKey 须为公众号后台生成的 43 位密钥')
    return
  }
  saving.value = true
  try {
    await saveWechatLoginConfig({ ...form }, managedTenantId.value)
    Message.success('公众号登录配置已保存')
    await loadConfig()
  } finally {
    saving.value = false
  }
}

watch(selectedTenantId, () => {
  userSearchGeneration += 1
  loadConfig()
})

onMounted(async () => {
  if (!isPlatformAdmin.value) return loadConfig()
  tenantsLoading.value = true
  try {
    tenantOptions.value = ((await getLoginTenantList()).data || []).map((tenant) => ({
      label: `${tenant.tenantName}（${tenant.tenantCode}）`,
      value: tenant.tenantId
    }))
  } finally {
    tenantsLoading.value = false
  }
})
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  userSearchGeneration += 1
  configGeneration += 1
})
</script>

<style lang="scss" scoped>
.wechat-settings {
  max-width: 880px;

  &__heading {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 20px;

    h2,
    p {
      margin: 0;
    }

    h2 {
      margin-bottom: 6px;
      font-size: 18px;
    }

    p {
      color: var(--color-text-3);
    }
  }

  &__notice {
    margin-bottom: 20px;
  }

  &__spin {
    display: block;
  }

  &__form {
    max-width: 760px;
  }

  &__switch-label {
    margin-left: 12px;
    color: var(--color-text-2);
  }

  &__qr-field {
    display: flex;
    gap: 16px;
    align-items: center;
    width: 100%;
    margin-top: 12px;

    img {
      width: 92px;
      height: 92px;
      object-fit: contain;
      background: #fff;
      border: 1px solid var(--color-border-2);
      border-radius: 6px;
    }

    p {
      margin-bottom: 0;
      line-height: 1.6;
      color: var(--color-text-3);
    }
  }

  &__file {
    display: none;
  }
}
</style>
