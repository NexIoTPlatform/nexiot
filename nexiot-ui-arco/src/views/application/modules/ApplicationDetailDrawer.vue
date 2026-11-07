<template>
  <a-modal
    v-model:visible="visible"
    :title="app?.appName || '应用详情'"
    width="min(1120px, calc(100vw - 32px))"
    :body-style="modalBodyStyle"
    :footer="false"
    align-center
    unmount-on-close
  >
    <a-spin :loading="loading" class="application-detail__spin">
      <a-tabs v-if="app" v-model:active-key="activeTab" class="application-detail__tabs">
        <a-tab-pane key="overview" title="应用信息">
          <section class="application-detail__section">
            <div class="application-detail__section-header">
              <strong>基础信息</strong>
              <a-button v-hasPerm="['application:application:edit']" type="text" @click="openApplicationEdit"><template #icon><icon-edit /></template>编辑</a-button>
            </div>
            <a-descriptions :column="descriptionColumns" bordered>
              <a-descriptions-item label="应用名称">{{ app.appName }}</a-descriptions-item>
              <a-descriptions-item label="状态"><a-tag :color="app.appStatus === 0 ? 'green' : 'red'">{{ app.appStatus === 0 ? "启用" : "停用" }}</a-tag></a-descriptions-item>
              <a-descriptions-item label="创建账号">{{ app.unionId || "-" }}</a-descriptions-item>
              <a-descriptions-item label="应用标识"><a-typography-paragraph copyable>{{ app.appUniqueId }}</a-typography-paragraph></a-descriptions-item>
              <a-descriptions-item label="设备数量">{{ app.devNum ?? 0 }}</a-descriptions-item>
              <a-descriptions-item label="创建时间">{{ app.createDate || "-" }}</a-descriptions-item>
              <a-descriptions-item label="描述" :span="3">{{ app.remark || "-" }}</a-descriptions-item>
            </a-descriptions>
          </section>

          <section class="application-detail__section">
            <div class="application-detail__section-header">
              <a-space><strong>开发信息</strong><a-button type="text" size="small" @click="openSdkConfig"><template #icon><icon-code /></template>SDK 配置</a-button></a-space>
              <a-space>
                <a-link href="https://docs.nexiot.cc/iot/http/java-sdk.html" target="_blank">SDK 文档</a-link>
                <a-button v-hasPerm="['application:application:resetSecret']" type="text" status="warning" @click="openReset"><template #icon><icon-lock /></template>重置密钥</a-button>
              </a-space>
            </div>
            <div class="application-detail__credentials">
              <div class="application-detail__credential">
                <span>Client ID</span>
                <a-typography-paragraph code copyable>{{ app.appId }}</a-typography-paragraph>
              </div>
              <div class="application-detail__credential">
                <span>Client Secret</span>
                <a-space>
                  <code>{{ showSecret ? app.appSecret || "-" : "********************************" }}</code>
                  <a-tooltip :content="showSecret ? '隐藏密钥' : '显示密钥'"><a-button type="text" shape="circle" @click="showSecret = !showSecret"><icon-eye v-if="!showSecret" /><icon-eye-invisible v-else /></a-button></a-tooltip>
                  <a-tooltip v-if="showSecret" content="复制密钥"><a-button type="text" shape="circle" @click="copy(app.appSecret)"><icon-copy /></a-button></a-tooltip>
                </a-space>
              </div>
            </div>
          </section>
        </a-tab-pane>

        <a-tab-pane key="push" title="推送配置">
          <a-row justify="end" class="g-row-tool">
            <a-button v-hasPerm="['application:application:edit']" type="primary" @click="() => openPushConfig()"><template #icon><icon-edit /></template>编辑配置</a-button>
          </a-row>
          <div class="application-detail__push-list">
            <div v-for="type in pushTypes" :key="type.key" class="application-detail__push-row">
              <div class="application-detail__push-identity">
                <component :is="type.icon" />
                <div><strong>{{ type.name }}</strong><span>{{ channelSummary(type.key) }}</span></div>
              </div>
              <a-space>
                <a-tag :color="configStatusColor(type.key)">{{ configStatusText(type.key) }}</a-tag>
                <a-tooltip content="编辑配置"><a-button v-if="hasPerm('application:application:edit')" type="text" shape="circle" @click="openPushConfig(type.key)"><icon-settings /></a-button></a-tooltip>
                <a-switch :model-value="Boolean(pushConfig[type.key]?.enable)" :disabled="!hasPerm('application:application:edit') || pushConfig[type.key]?.support === false" :loading="pushSaving === type.key" @change="(value) => changePush(type.key, Boolean(value))" />
              </a-space>
            </div>
          </div>

          <div class="application-detail__push-row application-detail__subscription-row">
            <div class="application-detail__push-identity">
              <icon-apps />
              <div><strong>产品变更订阅</strong><span>复用 HTTP 推送目标</span></div>
            </div>
            <a-space>
              <a-tag v-if="!httpReady" color="gray">{{ subscriptionEnabled ? '已订阅，HTTP 恢复后生效' : '依赖 HTTP' }}</a-tag>
              <a-switch :model-value="subscriptionEnabled" :disabled="!hasPerm('application:application:edit') || !httpReady" :loading="subscriptionSaving" @change="saveSubscription" />
            </a-space>
          </div>
        </a-tab-pane>

        <a-tab-pane key="devices" title="设备管理">
          <ApplicationDeviceBind v-if="app" ref="deviceBindRef" :app-unique-id="app.appUniqueId" @change="refreshApplication" @total-change="handleDeviceTotal" />
        </a-tab-pane>
      </a-tabs>
    </a-spin>

    <a-modal v-model:visible="resetVisible" title="重置应用密钥" width="min(560px, calc(100vw - 32px))" align-center :ok-loading="resetting" :ok-button-props="{ status: 'danger' }" @before-ok="resetSecret">
      <a-alert type="error" class="application-detail__reset-alert">
        当前密钥会立即失效，使用旧密钥的客户端将认证失败，MQTT 推送密码也会同步变化。此操作不可逆。
      </a-alert>
      <p>请输入应用 Client ID <code>{{ app?.appId }}</code> 确认：</p>
      <a-input v-model.trim="resetConfirm" placeholder="请手动输入 Client ID" @paste.prevent="Message.warning('请手动输入 Client ID')" />
    </a-modal>
  </a-modal>
  <ApplicationPushConfigDrawer ref="pushConfigDrawerRef" @save-success="handlePushConfigSaved" />
  <ApplicationFormDrawer ref="formDrawerRef" @save-success="refreshApplication" />
  <ApplicationSdkConfigModal ref="sdkModalRef" />
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import type { ApplicationItem, PushConfig } from '@/apis/application/application'
import { Message } from '@arco-design/web-vue'
import { IconCloud, IconCloudDownload, IconSend, IconSwap, IconThunderbolt, IconUpload } from '@arco-design/web-vue/es/icon'
import {
  getApplication,
  getProductSubscription,
  resetApplicationSecret,
  updateProductSubscription,
  updatePushEnabled
} from '@/apis/application/application'
import { copyText } from '@/utils'
import { hasPerm } from '@/utils/permission'
import ApplicationDeviceBind from './ApplicationDeviceBind.vue'
import ApplicationFormDrawer from './ApplicationFormDrawer.vue'
import ApplicationPushConfigDrawer from './ApplicationPushConfigDrawer.vue'
import ApplicationSdkConfigModal from './ApplicationSdkConfigModal.vue'

defineOptions({ name: 'ApplicationDetailDrawer' })
const visible = ref(false)
const loading = ref(false)
const descriptionColumns = { xs: 1, sm: 2, lg: 3 }
const app = ref<ApplicationItem>()
const activeTab = ref('overview')
const modalBodyStyle = computed(() => ({
  ...(activeTab.value === 'overview' ? { maxHeight: 'min(82vh, 820px)' } : { height: 'min(82vh, 820px)' }),
  overflow: 'hidden',
  padding: '0 20px 20px'
}))
const showSecret = ref(false)
const resetVisible = ref(false)
const resetting = ref(false)
const resetConfirm = ref('')
const subscriptionEnabled = ref(false)
const subscriptionSaving = ref(false)
const pushSaving = ref('')
const pushConfig = reactive<Record<string, PushConfig>>({})
const pushConfigDrawerRef = useTemplateRef<InstanceType<typeof ApplicationPushConfigDrawer>>('pushConfigDrawerRef')
const formDrawerRef = useTemplateRef<InstanceType<typeof ApplicationFormDrawer>>('formDrawerRef')
const sdkModalRef = useTemplateRef<InstanceType<typeof ApplicationSdkConfigModal>>('sdkModalRef')
const deviceBindRef = useTemplateRef<InstanceType<typeof ApplicationDeviceBind>>('deviceBindRef')
type PushKey = 'http' | 'externalMqtt' | 'tcp' | 'mqtt' | 'kafka' | 'rocketMQ'
const pushTypes: { key: PushKey, name: string, icon: Component }[] = [
  { key: 'http', name: 'HTTP 推送', icon: IconSend },
  { key: 'externalMqtt', name: '外部 MQTT', icon: IconUpload },
  { key: 'tcp', name: 'TCP 推送', icon: IconSwap },
  { key: 'mqtt', name: '平台 MQTT', icon: IconCloud },
  { key: 'kafka', name: 'Kafka 推送', icon: IconCloudDownload },
  { key: 'rocketMQ', name: 'RocketMQ 推送', icon: IconThunderbolt }
]
function parseCfg(cfg?: string) {
  try {
    const result = JSON.parse(cfg || '{}') as Record<string, PushConfig>
    if (!result.rocketMQ && result.rocketmq) result.rocketMQ = result.rocketmq
    return result
  } catch {
    return {}
  }
}

function isSectionConfigured(key: PushKey, config: PushConfig) {
  if (key === 'http') return Boolean(config.url && config.secret)
  if (key === 'externalMqtt') return Boolean(config.brokerUrl && config.topic)
  if (key === 'tcp') return Boolean(config.host && config.port)
  if (key === 'mqtt') return Boolean(config.url || config.wsUrl || config.topic)
  if (key === 'kafka') return Boolean(config.bootstrapServers && config.topic)
  return Boolean(config.namesrvAddr && config.topic && config.group)
}

function normalizePushConfig(application: ApplicationItem) {
  const result = parseCfg(application.cfg)
  for (const type of pushTypes) {
    const section = { ...(result[type.key] || {}) }
    section.support ??= true
    if (section.enable === undefined) {
      if (type.key === 'http' && application.httpEnable !== undefined) {
        section.enable = Number(application.httpEnable) === 1
      } else if (type.key === 'mqtt' && application.mqttEnable !== undefined) {
        section.enable = Number(application.mqttEnable) === 1
      } else {
        section.enable = isSectionConfigured(type.key, section)
      }
    }
    result[type.key] = section
  }
  return result
}

function replacePushConfig(next: Record<string, PushConfig>) {
  Object.keys(pushConfig).forEach((key) => delete pushConfig[key])
  Object.assign(pushConfig, next)
}

function isConfigured(key: PushKey) {
  const config = pushConfig[key] || {}
  return isSectionConfigured(key, config)
}

function configStatusText(key: PushKey) {
  if (key === 'mqtt') return isConfigured(key) ? '平台已生成' : '启用后生成'
  return isConfigured(key) ? '已配置' : '未配置'
}

function configStatusColor(key: PushKey) {
  if (key === 'mqtt') return 'arcoblue'
  return isConfigured(key) ? 'green' : 'orange'
}

function channelSummary(key: PushKey) {
  const config = pushConfig[key] || {}
  if (key === 'http') return String(config.url || '未设置推送地址')
  if (key === 'externalMqtt') return config.brokerUrl ? `${config.brokerUrl} · ${config.topic || '-'}` : '未设置 Broker'
  if (key === 'tcp') return config.host ? `${config.host}:${config.port}` : '未设置服务器'
  if (key === 'mqtt') return String(config.url || '平台内置 Broker')
  if (key === 'kafka') return String(config.bootstrapServers || '未设置服务器')
  return String(config.namesrvAddr || '未设置 NameServer')
}

const httpReady = computed(() => isConfigured('http') && Boolean(pushConfig.http?.enable))

async function loadApplication(id: string) {
  app.value = (await getApplication(id)).data
  replacePushConfig(normalizePushConfig(app.value))
}

async function open(id: string) {
  visible.value = true
  loading.value = true
  activeTab.value = 'overview'
  showSecret.value = false
  try {
    await loadApplication(id)
    subscriptionEnabled.value = Boolean((await getProductSubscription(id)).data?.enabled)
  } finally {
    loading.value = false
  }
}

async function refreshApplication() {
  if (!app.value) return
  await loadApplication(app.value.appUniqueId)
}

function handleDeviceTotal(total: number) {
  if (app.value) app.value.devNum = total
}

async function copy(value?: string) {
  if (!value) return
  try {
    await copyText(value)
    Message.success('已复制到剪贴板')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '复制失败')
  }
}

function openReset() {
  resetConfirm.value = ''
  resetVisible.value = true
}

async function resetSecret() {
  if (!app.value || resetConfirm.value !== app.value.appId) {
    Message.error('Client ID 输入不正确')
    return false
  }
  resetting.value = true
  try {
    await resetApplicationSecret(app.value.appUniqueId)
    await refreshApplication()
    showSecret.value = true
    Message.success('密钥已重置，请立即更新客户端配置')
    return true
  } finally {
    resetting.value = false
  }
}

function openPushConfig(type: PushKey = 'http') {
  if (!app.value) return
  const currentApp = app.value
  visible.value = false
  nextTick(() => pushConfigDrawerRef.value?.open(currentApp, pushConfig, type))
}

function openApplicationEdit() {
  if (!app.value) return
  const appUniqueId = app.value.appUniqueId
  visible.value = false
  nextTick(() => formDrawerRef.value?.edit(appUniqueId))
}

function openSdkConfig() {
  if (!app.value) return
  const currentApp = app.value
  const revealSecret = showSecret.value
  visible.value = false
  nextTick(() => sdkModalRef.value?.open(currentApp, revealSecret))
}

async function handlePushConfigSaved(next: Record<string, PushConfig>) {
  replacePushConfig(next)
  await refreshApplication()
}

async function changePush(key: PushKey, enable: boolean) {
  if (!app.value) return
  if (enable && key !== 'mqtt' && !isConfigured(key)) {
    Message.warning('请先完成该推送方式的必填配置')
    openPushConfig(key)
    return
  }
  pushSaving.value = key
  try {
    await updatePushEnabled(app.value.appUniqueId, key === 'rocketMQ' ? 'rocketmq' : key, enable)
    await refreshApplication()
    Message.success(enable ? '推送已启用' : '推送已停用')
  } finally {
    pushSaving.value = ''
  }
}

async function saveSubscription(value: boolean | string | number) {
  if (!app.value) return
  const enabled = Boolean(value)
  subscriptionSaving.value = true
  try {
    await updateProductSubscription(app.value.appUniqueId, enabled)
    subscriptionEnabled.value = enabled
    Message.success('产品订阅配置已更新')
  } finally {
    subscriptionSaving.value = false
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.application-detail {
  &__section {
    padding-top: 16px;
    margin-bottom: 8px;
  }

  &__section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  &__credentials {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  &__credential {
    min-height: 92px;
    padding: 16px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-2);
  }

  &__credential > span {
    display: block;
    margin-bottom: 10px;
    color: var(--color-text-3);
  }

  &__push-list {
    margin-bottom: 16px;
    border-top: 1px solid var(--color-border-2);
  }

  &__push-row,
  &__push-identity {
    display: flex;
    align-items: center;
  }

  &__push-row {
    gap: 16px;
    justify-content: space-between;
    min-height: 68px;
    border-bottom: 1px solid var(--color-border-2);
  }

  &__push-identity {
    gap: 12px;
    min-width: 0;
  }

  &__push-identity > svg {
    flex: 0 0 auto;
    font-size: 20px;
    color: rgb(var(--primary-6));
  }

  &__push-identity strong,
  &__push-identity span {
    display: block;
  }

  &__push-identity span {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-3);
    overflow-wrap: anywhere;
  }

  &__subscription-row {
    border-top: 1px solid var(--color-border-2);
  }

  &__reset-alert {
    margin-bottom: 16px;
  }
}

:deep(.arco-typography) {
  margin-bottom: 0;
}

.application-detail__spin,
:deep(.arco-spin-children),
.application-detail__tabs {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  height: 100%;
  min-height: 0;
}

.application-detail__spin {
  display: block;
}

.application-detail__tabs {
  display: flex;
  flex-direction: column;
}

:deep(.application-detail__tabs > .arco-tabs-nav) {
  flex: 0 0 auto;
  padding-top: 8px;
  background: var(--color-bg-2);
}

:deep(.application-detail__tabs > .arco-tabs-content) {
  flex: 1;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  min-height: 0;
  overflow: clip;
}

:deep(.application-detail__tabs > .arco-tabs-content > .arco-tabs-content-list),
:deep(.application-detail__tabs > .arco-tabs-content > .arco-tabs-content-list > .arco-tabs-content-item),
:deep(.application-detail__tabs > .arco-tabs-content > .arco-tabs-content-list > .arco-tabs-content-item > .arco-tabs-pane) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  height: 100%;
  min-height: 0;
}

:deep(.application-detail__tabs > .arco-tabs-content > .arco-tabs-content-list > .arco-tabs-content-item) {
  flex: 0 0 100%;
  overflow: clip;
}

:deep(.application-detail__tabs > .arco-tabs-content > .arco-tabs-content-list > .arco-tabs-content-item > .arco-tabs-pane) {
  padding-right: 4px;
  overflow: hidden auto;
}

:deep(.application-detail__push-card code),
.application-detail__credential code {
  overflow-wrap: anywhere;
  white-space: normal;
}

@media (width <= 900px) {
  .application-detail__credentials {
    grid-template-columns: 1fr;
  }

}
</style>
