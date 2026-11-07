<template>
  <GiPageLayout margin collapsed size="300px" class="account-page" :body-style="{ padding: 0 }">
    <template #left>
      <a-skeleton v-if="loading && !profile" animation>
        <a-skeleton-shape shape="circle" size="large" />
        <a-skeleton-line :rows="8" />
      </a-skeleton>
      <AccountSummary v-else-if="profile" :profile="profile" @avatar="avatarModalRef?.open()" @profile="activeTab = 'profile'" />
    </template>

    <template #header>
      <div class="account-page__header">
        <div>
          <h1>个人中心</h1>
          <p>管理个人资料、登录安全和实时消息偏好。</p>
        </div>
        <a-space wrap>
          <a-tag v-if="profile" color="arcoblue">{{ profile.user.tenantId || '平台账户' }}</a-tag>
          <a-button :loading="loading" @click="loadProfile"><template #icon><icon-refresh /></template>刷新</a-button>
        </a-space>
      </div>
      <a-tabs v-model:active-key="activeTab" type="rounded" hide-content class="account-page__tabs">
        <a-tab-pane key="profile" title="基本资料" />
        <a-tab-pane key="security" title="安全设置" />
        <a-tab-pane key="push" title="实时推送" />
        <a-tab-pane v-if="isTenantAdmin" key="platform" title="平台设置" />
        <a-tab-pane v-if="canManageWechat" key="wechat" title="公众号登录" />
      </a-tabs>
    </template>

    <div class="account-page__body">
      <a-spin :loading="loading" class="account-page__spin">
        <a-result v-if="loadFailed" status="error" title="个人资料加载失败" subtitle="请检查网络连接后重试。">
          <template #extra><a-button type="primary" @click="loadProfile">重新加载</a-button></template>
        </a-result>
        <template v-else-if="profile">
          <ProfileSettings v-if="activeTab === 'profile'" :profile="profile" @success="handleProfileUpdated" @avatar="avatarModalRef?.open()" />
          <SecuritySettings v-else-if="activeTab === 'security'" :profile="profile" @password="passwordModalRef?.open()" />
          <WebSocketPushSettings v-else-if="activeTab === 'push'" />
          <PlatformSettings v-else-if="activeTab === 'platform' && isTenantAdmin" />
          <WechatLoginSettings v-else-if="activeTab === 'wechat' && canManageWechat" />
        </template>
      </a-spin>
    </div>

    <PasswordModal ref="passwordModalRef" />
    <AvatarCropModal ref="avatarModalRef" @success="handleAvatarUpdated" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { AccountProfile } from './api'
import { useUserStore } from '@/stores'
import { getAccountProfile } from './api'
import AccountSummary from './modules/AccountSummary.vue'
import AvatarCropModal from './modules/AvatarCropModal.vue'
import PasswordModal from './modules/PasswordModal.vue'
import PlatformSettings from './modules/PlatformSettings.vue'
import ProfileSettings from './modules/ProfileSettings.vue'
import SecuritySettings from './modules/SecuritySettings.vue'
import WebSocketPushSettings from './modules/WebSocketPushSettings.vue'
import WechatLoginSettings from './modules/WechatLoginSettings.vue'

defineOptions({ name: 'SystemAccount' })

type AccountTab = 'profile' | 'security' | 'push' | 'platform' | 'wechat'
interface OpenableModal { open: () => void }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const profile = ref<AccountProfile | null>(null)
const loading = ref(false)
const loadFailed = ref(false)
const passwordModalRef = ref<OpenableModal | null>(null)
const avatarModalRef = ref<OpenableModal | null>(null)
const activeTab = ref<AccountTab>(resolveTab(route.query.tab))
const isTenantAdmin = computed(() => {
  const tenantRole = String(profile.value?.user.tenantRole || userStore.userInfo.tenantRole || '').toUpperCase()
  return Boolean(profile.value?.user.tenantId || userStore.userInfo.tenantId)
    && !userStore.userInfo.currentAdmin
    && ['ADMIN', 'TENANT_ADMIN'].includes(tenantRole)
})
const canManageWechat = computed(() => userStore.userInfo.currentAdmin || isTenantAdmin.value)

function resolveTab(value: unknown): AccountTab {
  return ['profile', 'security', 'push', 'platform', 'wechat'].includes(String(value)) ? String(value) as AccountTab : 'profile'
}

async function loadProfile() {
  loading.value = true
  loadFailed.value = false
  try {
    profile.value = (await getAccountProfile()).data
    if (activeTab.value === 'platform' && !isTenantAdmin.value) activeTab.value = 'profile'
    if (activeTab.value === 'wechat' && !canManageWechat.value) activeTab.value = 'profile'
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function handleProfileUpdated() {
  await Promise.all([loadProfile(), userStore.fetchUserInfo()])
}

async function handleAvatarUpdated() {
  await Promise.all([loadProfile(), userStore.fetchUserInfo()])
}

watch(activeTab, (tab) => {
  if (route.query.tab === tab) return
  router.replace({ query: { ...route.query, tab } })
})

watch(() => route.query.tab, (tab) => {
  activeTab.value = resolveTab(tab)
})

watch(() => route.query.action, (action) => {
  if (action === 'password') nextTick(() => passwordModalRef.value?.open())
}, { immediate: true })

onMounted(loadProfile)
</script>

<style lang="scss" scoped>
.account-page {
  &__header {
    display: flex;
    gap: 20px;
    align-items: flex-start;
    justify-content: space-between;

    h1,
    p {
      margin: 0;
    }

    h1 {
      margin-bottom: 6px;
      font-size: 22px;
      letter-spacing: 0;
    }

    p {
      color: var(--color-text-3);
    }
  }

  &__tabs {
    margin-top: 18px;
  }

  &__body {
    box-sizing: border-box;
    height: 100%;
    padding: 24px;
    overflow-y: auto;
  }

  &__spin {
    display: block;
    min-height: 100%;
  }
}

@media (width <= 640px) {
  .account-page {
    &__header {
      flex-direction: column;
    }

    &__body {
      padding: 16px;
    }
  }
}

@media (width <= 768px) {
  .account-page:deep(>.arco-split-pane-first),
  .account-page:deep(>.arco-split-trigger) {
    display: none !important;
  }

  .account-page:deep(>.arco-split-pane-second) {
    flex: 1 1 100% !important;
    width: 100% !important;
  }
}
</style>
