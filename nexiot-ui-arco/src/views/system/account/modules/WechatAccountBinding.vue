<template>
  <section v-if="!userStore.userInfo.currentAdmin" class="wechat-binding">
    <div class="wechat-binding__heading">
      <div><h3>微信登录绑定</h3><p>绑定当前租户的公众号后，可使用微信验证登录此账号。</p></div>
      <a-button :loading="loading" @click="loadBindings"><template #icon><icon-refresh /></template>刷新</a-button>
    </div>
    <a-spin :loading="loading">
      <a-alert v-if="loadFailed" type="warning">微信绑定状态加载失败，请刷新重试。</a-alert>
      <a-list v-else-if="bindings.length" :bordered="false">
        <a-list-item v-for="(binding, index) in bindings" :key="binding.id || `${binding.appId}-${index}`">
          <a-list-item-meta :title="binding.accountName || '微信公众号'" :description="bindingDescription(binding)">
            <template #avatar><GiIconBox color="success"><icon-link /></GiIconBox></template>
          </a-list-item-meta>
          <template #actions>
            <a-tag color="green">已绑定</a-tag>
            <a-popconfirm content="解除绑定后，此微信将不能用于登录当前账号，是否继续？" @ok="unbind">
              <a-button type="text" status="danger" :loading="unbinding">解除绑定</a-button>
            </a-popconfirm>
          </template>
        </a-list-item>
      </a-list>
      <a-empty v-else description="当前账号尚未绑定微信" />
    </a-spin>
    <a-alert v-if="!loading && !loadFailed && !bindingEnabled" type="info">当前租户尚未开启个人微信绑定，请联系管理员配置公众号登录。</a-alert>
    <a-button v-if="!loadFailed && !bindings.length && bindingEnabled" type="primary" :disabled="loading" @click="openBinding"><template #icon><icon-scan /></template>扫码绑定微信</a-button>
    <a-modal v-model:visible="visible" title="绑定微信到当前账号" :footer="false" :width="440" unmount-on-close @close="cancel">
      <a-alert type="info" class="wechat-binding__notice">请使用本人的微信验证。收到身份后，仍需在当前页面点击“确认绑定”。</a-alert>
      <WechatSessionPanel
        :session="session" :status="status" :remaining="remaining" :loading="creating" :busy="confirming"
        :message="message" :error="error" :account-name="accountName" purpose="bind" @refresh="startBinding" @confirm="confirm"
      />
    </a-modal>
  </section>
</template>

<script setup lang="ts">
import type { WechatBinding } from '../wechatApi'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores'
import { getWechatLoginOptions } from '@/views/login/api'
import { useWechatSession } from '@/views/login/modules/useWechatSession'
import WechatSessionPanel from '@/views/login/modules/WechatSessionPanel.vue'
import { formatAccountDate } from '../api'
import { confirmWechatBinding, createWechatBindingSession, getWechatBindings, removeWechatBinding } from '../wechatApi'

defineOptions({ name: 'WechatAccountBinding' })

const bindings = ref<WechatBinding[]>([])
const userStore = useUserStore()
const bindingEnabled = ref(false)
const accountName = ref('')
const loading = ref(false)
const loadFailed = ref(false)
const unbinding = ref(false)
const visible = ref(false)
const confirming = ref(false)
const { session, status, remaining, loading: creating, message, error, start, cancel, fail } = useWechatSession()

function bindingDescription(binding: WechatBinding) {
  return [binding.nickname || binding.openIdMasked, `绑定时间：${formatAccountDate(binding.boundAt || binding.createTime)}`].filter(Boolean).join(' · ')
}

async function loadBindings() {
  if (userStore.userInfo.currentAdmin) return
  loading.value = true
  loadFailed.value = false
  try {
    const [bindingResponse, optionsResponse] = await Promise.all([getWechatBindings(), getWechatLoginOptions(userStore.userInfo.tenantCode)])
    bindings.value = bindingResponse.data || []
    bindingEnabled.value = optionsResponse.data.bindingEnabled
    accountName.value = optionsResponse.data.accountName
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function startBinding() {
  start(createWechatBindingSession)
}

function openBinding() {
  visible.value = true
  startBinding()
}

async function confirm() {
  if (!session.value || status.value !== 'SCANNED' || confirming.value) return
  confirming.value = true
  try {
    await confirmWechatBinding(session.value)
    status.value = 'BOUND'
    visible.value = false
    cancel()
    Message.success('微信已绑定到当前账号')
    await loadBindings()
  } catch (cause) {
    fail((cause as Error).message || '绑定失败，请重新获取二维码。')
  } finally {
    confirming.value = false
  }
}

async function unbind() {
  unbinding.value = true
  try {
    await removeWechatBinding()
    Message.success('微信绑定已解除')
    await loadBindings()
  } finally {
    unbinding.value = false
  }
}

onMounted(loadBindings)
</script>

<style lang="scss" scoped>
.wechat-binding {
  margin-top: 28px;

  &__heading {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;

    h3,
    p {
      margin: 0;
    }

    h3 {
      margin-bottom: 6px;
      font-size: 16px;
    }

    p {
      line-height: 1.6;
      color: var(--color-text-3);
    }
  }

  &__notice {
    margin-bottom: 20px;
  }
}
</style>
