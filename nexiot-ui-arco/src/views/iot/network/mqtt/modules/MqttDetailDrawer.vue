<template>
  <a-modal
    v-model:visible="visible"
    :width="1100"
    :modal-style="{ maxWidth: 'calc(100vw - 32px)' }"
    :body-style="{ maxHeight: 'min(76vh, 760px)', overflowY: 'auto' }"
    align-center
    unmount-on-close
  >
    <template #title>
      <a-space>
        <span>{{ detail?.name || 'MQTT 组件详情' }}</span>
        <a-tag :color="detail?.running ? 'green' : 'gray'">
          {{ detail?.running ? '运行中' : '已停止' }}
        </a-tag>
      </a-space>
    </template>

    <a-spin :loading="loading" class="mqtt-detail__spin">
      <template v-if="detail">
        <div class="mqtt-detail__toolbar">
          <a-space wrap>
            <a-popconfirm
              v-if="detail.running"
              content="确定停止该 MQTT 组件吗？"
              @before-ok="() => changeRuntime('stop')"
            >
              <a-button
                v-hasPerm="['network:mqtt:stop']"
                status="warning"
                :loading="runtimeLoading"
              >
                <template #icon><icon-pause /></template>
                停止
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-else
              content="确定启动该 MQTT 组件吗？"
              @before-ok="() => changeRuntime('start')"
            >
              <a-button
                v-hasPerm="['network:mqtt:start']"
                type="primary"
                :loading="runtimeLoading"
                :disabled="detail.state !== 1"
              >
                <template #icon><icon-play-arrow /></template>
                启动
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              content="确定重启该 MQTT 组件吗？"
              @before-ok="() => changeRuntime('restart')"
            >
              <a-button
                v-hasPerm="['network:mqtt:restart']"
                :loading="runtimeLoading"
                :disabled="detail.state !== 1"
              >
                <template #icon><icon-refresh /></template>
                重启
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>

        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="configuration" title="配置维护">
            <section class="mqtt-detail__section">
              <h3>基础信息</h3>
              <a-descriptions :column="descriptionColumns" size="small" bordered>
                <a-descriptions-item label="组件名称">
                  {{ detail.name }}
                </a-descriptions-item>
                <a-descriptions-item label="组件类型">
                  {{ typeLabel }}
                </a-descriptions-item>
                <a-descriptions-item label="唯一标识">
                  <a-space>
                    <code>{{ detail.unionId || '-' }}</code>
                    <a-tooltip content="复制唯一标识">
                      <a-button
                        type="text"
                        shape="circle"
                        aria-label="复制唯一标识"
                        @click="copyUnionId"
                      >
                        <template #icon><icon-copy /></template>
                      </a-button>
                    </a-tooltip>
                  </a-space>
                </a-descriptions-item>
                <a-descriptions-item label="启用状态">
                  <a-tag :color="detail.state === 1 ? 'green' : 'gray'">
                    {{ detail.state === 1 ? '启用' : '停用' }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="创建时间">
                  {{ detail.createDateStr || detail.createDate || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="创建人">
                  {{ detail.createUser || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="描述" :span="2">
                  {{ detail.description || '-' }}
                </a-descriptions-item>
              </a-descriptions>
            </section>

            <section
              v-for="section in configSections"
              :key="section.key"
              class="mqtt-detail__section"
            >
              <h3>{{ section.title }}</h3>
              <a-descriptions :column="descriptionColumns" size="small" bordered>
                <a-descriptions-item
                  v-for="item in section.items"
                  :key="item.key"
                  :label="item.label"
                >
                  {{ item.value }}
                </a-descriptions-item>
              </a-descriptions>
            </section>
          </a-tab-pane>
          <a-tab-pane
            key="topics"
            :title="detail.type === 'MQTT_SERVER' ? '订阅与产品绑定' : '主题订阅'"
          >
            <MqttTopicSubscriptionPanel
              :network="detail"
              @updated="handleContentUpdated"
            />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <template #footer>
      <a-space>
        <a-button @click="visible = false">关闭</a-button>
        <a-button
          v-if="detail"
          v-hasPerm="['network:mqtt:edit']"
          type="primary"
          @click="handleEdit"
        >
          <template #icon><icon-edit /></template>
          编辑基础配置
        </a-button>
      </a-space>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import type { MqttNetworkItem } from '@/apis/iot/mqtt'
import { Message } from '@arco-design/web-vue'
import { useMediaQuery } from '@vueuse/core'
import {
  getMqttNetwork,
  parseMqttConfiguration,
  restartMqttNetwork,
  startMqttNetwork,
  stopMqttNetwork
} from '@/apis/iot/mqtt'
import { copyText } from '@/utils'
import MqttTopicSubscriptionPanel from './MqttTopicSubscriptionPanel.vue'

defineOptions({ name: 'MqttDetailDrawer' })

const emit = defineEmits<{
  (e: 'edit', id: string): void
  (e: 'updated'): void
}>()

interface ConfigDisplayItem {
  key: string
  label: string
  value: string
}

const visible = ref(false)
const loading = ref(false)
const runtimeLoading = ref(false)
const activeTab = ref('configuration')
const detail = ref<MqttNetworkItem>()
const isNarrow = useMediaQuery('(max-width: 720px)')
const descriptionColumns = computed(() => isNarrow.value ? 1 : 2)
let pollTimer: number | undefined

const typeLabel = computed(() =>
  detail.value?.type === 'MQTT_CLIENT' ? 'MQTT 客户端' : 'MQTT 服务端'
)
const configSections = computed(() => {
  const config = parseMqttConfiguration(detail.value)
  const groups: Array<{
    key: string
    title: string
    fields: Array<[string, string]>
  }> = [
    {
      key: 'connection',
      title: '连接认证',
      fields: [
        ['host', 'Broker 地址'],
        ['port', '端口'],
        ['clientId', '客户端 ID'],
        ['clusterEnabled', '集群部署'],
        ['username', '用户名'],
        ['password', '密码']
      ]
    },
    {
      key: 'tls',
      title: 'SSL / TLS',
      fields: [
        ['ssl', '启用 TLS'],
        ['sslMode', 'TLS 认证模式'],
        ['sslCaCertKey', '服务端 CA 证书'],
        ['sslClientCertKey', '客户端证书'],
        ['sslHostnameVerify', '校验服务端主机名']
      ]
    },
    {
      key: 'runtime',
      title: '运行参数',
      fields: [
        ['defaultQos', '默认 QoS'],
        ['keepAliveInterval', 'Keep Alive（秒）'],
        ['connectTimeout', '连接超时（秒）'],
        ['cleanSession', '清理会话'],
        ['autoReconnect', '自动重连']
      ]
    }
  ]
  const knownKeys = new Set([
    'subscribeTopics',
    ...groups.flatMap((group) => group.fields.map(([key]) => key))
  ])
  const sections = groups.map((group) => ({
    key: group.key,
    title: group.title,
    items: group.fields.map(([key, label]) => ({
      key,
      label,
      value: displayValue(key, config[key])
    }))
  }))
  const extraItems: ConfigDisplayItem[] = Object.entries(config)
    .filter(([key]) => !knownKeys.has(key))
    .map(([key, value]) => ({
      key,
      label: key,
      value: displayValue(key, value)
    }))
  if (extraItems.length) {
    sections.push({ key: 'extra', title: '扩展参数', items: extraItems })
  }
  return sections
})

function displayValue(key: string, value: unknown) {
  if (key.toLowerCase().includes('password')) {
    return value ? '********' : '未配置'
  }
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (value === undefined || value === null || value === '') return '未配置'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

async function loadDetail(silent = false) {
  if (!detail.value?.id) return
  if (!silent) loading.value = true
  try {
    detail.value = (await getMqttNetwork(String(detail.value.id))).data
  } finally {
    if (!silent) loading.value = false
  }
}

async function open(id: string, tab = 'configuration') {
  visible.value = true
  activeTab.value = tab
  detail.value = { id } as MqttNetworkItem
  await loadDetail()
}

async function changeRuntime(action: 'start' | 'stop' | 'restart') {
  if (!detail.value) return false
  runtimeLoading.value = true
  const expectedRunning = action !== 'stop'
  try {
    const actions = {
      start: startMqttNetwork,
      stop: stopMqttNetwork,
      restart: restartMqttNetwork
    }
    await actions[action](String(detail.value.id))
    Message.success('运行操作已提交，正在确认状态')
    await pollRuntime(expectedRunning)
    emit('updated')
    return true
  } finally {
    runtimeLoading.value = false
  }
}

function pollRuntime(expectedRunning: boolean, remaining = 8): Promise<void> {
  if (pollTimer) window.clearTimeout(pollTimer)
  if (remaining <= 0) return Promise.resolve()
  return new Promise((resolve) => {
    pollTimer = window.setTimeout(async () => {
      await loadDetail(true)
      if (Boolean(detail.value?.running) === expectedRunning) {
        Message.success(expectedRunning ? '组件已连接' : '组件已停止')
        resolve()
        return
      }
      await pollRuntime(expectedRunning, remaining - 1)
      resolve()
    }, 1000)
  })
}

async function copyUnionId() {
  if (!detail.value?.unionId) return
  try {
    await copyText(detail.value.unionId)
    Message.success('唯一标识已复制')
  } catch { Message.error('复制失败') }
}

function handleEdit() {
  if (!detail.value?.id) return
  visible.value = false
  emit('edit', String(detail.value.id))
}

async function handleContentUpdated() {
  await loadDetail(true)
  emit('updated')
}

onUnmounted(() => {
  if (pollTimer) window.clearTimeout(pollTimer)
})

defineExpose({ open })
</script>

<style lang="scss" scoped>
.mqtt-detail {
  &__spin {
    width: 100%;
  }

  &__toolbar {
    display: flex;
    gap: 16px;
    align-items: center;
    margin-bottom: 12px;
  }

  &__section {
    margin-bottom: 14px;

    h3 {
      margin: 0 0 10px;
      font-size: 15px;
      color: var(--color-text-1);
    }
  }
}

code {
  color: rgb(var(--primary-6));
}

@media (width <= 768px) {
  .mqtt-detail__toolbar {
    align-items: flex-start;
  }
}
</style>
