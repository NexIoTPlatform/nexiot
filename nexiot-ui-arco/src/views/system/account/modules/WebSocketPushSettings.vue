<template>
  <section class="push-settings">
    <div class="push-settings__heading">
      <div>
        <h2>实时推送</h2>
        <p>控制当前账户接收的设备消息。</p>
      </div>
      <a-space wrap>
        <a-tag :color="connectionColor">{{ connectionText }}</a-tag>
        <a-tag :color="form.globalEnabled ? 'green' : 'red'">全局{{ form.globalEnabled ? '已开启' : '已关闭' }}</a-tag>
      </a-space>
    </div>

    <a-alert :type="effectiveEnabled ? 'success' : 'warning'" show-icon>{{ statusMessage }}</a-alert>

    <a-form :model="form" layout="vertical" class="push-settings__form">
      <div class="push-settings__master">
        <div><strong>个人订阅总开关</strong><span>关闭后当前账户不会接收实时推送。</span></div>
        <a-switch v-model="form.enabled" checked-text="开" unchecked-text="关" />
      </div>

      <div class="push-settings__subscriptions">
        <article v-for="item in subscriptionItems" :key="item.key">
          <div><GiIconBox :color="item.color"><component :is="item.icon" /></GiIconBox></div>
          <div><strong>{{ item.title }}</strong><span>{{ item.description }}</span></div>
          <a-switch v-model="form[item.key]" :disabled="!form.enabled" />
        </article>
      </div>

      <div v-if="store.adminEditable" class="push-settings__master push-settings__global">
        <div><strong>平台全局开关</strong><span>关闭后所有账户和节点都不会继续发送实时消息。</span></div>
        <a-switch v-model="form.globalEnabled" checked-text="开" unchecked-text="关" />
      </div>

      <a-space>
        <a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存设置</a-button>
        <a-button :disabled="saving" @click="reset"><template #icon><icon-refresh /></template>恢复当前配置</a-button>
      </a-space>
    </a-form>
  </section>
</template>

<script setup lang="ts">
import type { WebSocketPushSettings } from '../api'
import { Message } from '@arco-design/web-vue'
import { useWebSocketPushStore } from '@/stores'
import { DEFAULT_PUSH_SETTINGS } from '../api'

defineOptions({ name: 'AccountWebSocketPushSettings' })

type SettingKey = keyof Omit<WebSocketPushSettings, 'enabled'>
interface PushForm extends WebSocketPushSettings { globalEnabled: boolean }

const store = useWebSocketPushStore()
const saving = ref(false)
const form = reactive<PushForm>({ ...DEFAULT_PUSH_SETTINGS, globalEnabled: false })
const subscriptionItems: Array<{ key: SettingKey, title: string, description: string, icon: string, color: 'primary' | 'success' | 'warning' | 'danger' }> = [
  { key: 'deviceCreate', title: '设备注册', description: '新设备接入、批量导入或自动建档时通知。', icon: 'icon-plus-circle', color: 'primary' },
  { key: 'deviceOnline', title: '设备上线', description: '设备恢复在线后立即通知值守人员。', icon: 'icon-link', color: 'success' },
  { key: 'deviceOffline', title: '设备离线', description: '设备离线时快速发现异常并安排巡检。', icon: 'icon-disconnect', color: 'danger' },
  { key: 'commandResp', title: '指令回复', description: '接收设备对下发指令返回的执行结果。', icon: 'icon-message', color: 'warning' },
]
const effectiveEnabled = computed(() => form.globalEnabled && form.enabled)
const connectionText = computed(() => store.connecting ? '连接中' : store.connected ? '已连接' : '未连接')
const connectionColor = computed(() => store.connecting ? 'arcoblue' : store.connected ? 'green' : 'gray')
const statusMessage = computed(() => {
  if (!form.globalEnabled) return '管理员已关闭全局 WebSocket 推送，当前不会接收到任何实时消息。'
  if (!form.enabled) return '当前账户尚未开启个人订阅，请按需选择需要接收的消息类型。'
  return '当前账户已开启实时订阅，切换页面后连接仍会保持。'
})

function reset() {
  Object.assign(form, DEFAULT_PUSH_SETTINGS, store.settings, { globalEnabled: store.globalEnabled })
}

async function save() {
  saving.value = true
  try {
    await store.saveProfile({
      settings: {
        enabled: form.enabled,
        deviceCreate: form.deviceCreate,
        deviceOnline: form.deviceOnline,
        deviceOffline: form.deviceOffline,
        commandResp: form.commandResp,
        alarm: false,
        workOrder: false
      },
      ...(store.adminEditable ? { globalEnabled: form.globalEnabled } : {})
    })
    reset()
    Message.success('实时推送设置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!store.profileLoaded) await store.loadProfile()
  reset()
})
</script>

<style lang="scss" scoped>
.push-settings {
  max-width: 920px;

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

  &__form {
    margin-top: 20px;
  }

  &__master {
    display: flex;
    gap: 20px;
    align-items: center;
    justify-content: space-between;
    padding: 18px;
    margin-bottom: 16px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-2);

    >div {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    span {
      line-height: 20px;
      color: var(--color-text-3);
    }
  }

  &__subscriptions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;

    article {
      display: grid;
      grid-template-columns: 32px minmax(0, 1fr) auto;
      gap: 12px;
      align-items: center;
      padding: 18px;
      border: 1px solid var(--color-border-2);
    }

    article>div:nth-child(2) {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    span {
      font-size: 12px;
      line-height: 20px;
      color: var(--color-text-3);
    }
  }

  &__global {
    border-color: rgb(var(--warning-3));
  }
}

@media (width <= 720px) {
  .push-settings {
    &__heading {
      flex-direction: column;
    }

    &__subscriptions {
      grid-template-columns: minmax(0, 1fr);
    }
  }
}
</style>
