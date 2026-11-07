<template>
  <section class="connection-panel">
    <div class="connection-panel__header">
      <div><h2>{{ platformTitle }} 连接信息</h2><p>{{ platform === 'mqtt' ? '选择设备接入方式，查看连接地址、认证信息和主题。' : '先准备通信组件，再将连接地址和端口配置到设备。' }}</p></div>
      <a-space><a-button :loading="loading" @click="loadInfo"><template #icon><icon-refresh /></template>刷新</a-button><a-button v-if="platform === 'mqtt'" type="primary" @click="openManager"><template #icon><icon-settings /></template>管理接入组件</a-button></a-space>
    </div>

    <a-radio-group v-if="platform === 'mqtt'" v-model="mode" type="button" class="connection-modes">
      <a-radio v-for="item in modeOptions" :key="item.value" :value="item.value" :disabled="item.disabled">{{ item.label }}</a-radio>
    </a-radio-group>
    <a-alert v-if="platform === 'mqtt' || getModeInfo('network').enabled" class="connection-panel__description">{{ modeDescription }}</a-alert>

    <a-spin :loading="loading" class="connection-panel__content">
      <a-descriptions v-if="currentDetails.length" class="connection-panel__details" :column="{ xs: 1, md: 2 }" bordered>
        <a-descriptions-item v-for="item in currentDetails" :key="item.label" :label="item.label">
          <a-space v-if="item.secret"><span>{{ revealedPassword || '••••••••' }}</span><a-button type="text" :loading="passwordLoading" @click="revealPassword"><template #icon><icon-eye /></template>查看</a-button></a-space>
          <span v-else>{{ item.value || '未配置' }}</span>
        </a-descriptions-item>
      </a-descriptions>
      <section v-if="topics.length" class="topic-section">
        <h3>主题列表</h3>
        <a-table :data="topics" :columns="topicColumns" :pagination="false" :bordered="{ cell: true }" />
        <a-alert type="warning">设备账号必须具有以下主题的发布或订阅权限；集群 Broker 需要支持 MQTT 5.0 共享订阅。</a-alert>
      </section>
    </a-spin>

    <a-modal v-model:visible="managerVisible" title="接入组件管理" width="820px" :mask-closable="false" :footer="false">
      <div class="component-status">
        <a-alert v-if="product.networkUnionId" type="success">当前已绑定接入标识：{{ product.networkUnionId }}</a-alert>
        <a-alert v-else>当前产品未绑定自建接入组件。</a-alert>
        <a-button v-if="product.networkUnionId" status="danger" :loading="binding" @click="unbind"><template #icon><icon-link-unlink /></template>解绑</a-button>
      </div>
      <div class="component-toolbar"><strong>可用组件</strong><a-button :loading="componentLoading" @click="loadComponents"><template #icon><icon-refresh /></template>刷新</a-button></div>
      <a-empty v-if="!componentLoading && !components.length" description="暂无可用接入组件" />
      <div v-else class="component-list">
        <article v-for="item in components" :key="item.id" class="component-item" :class="{ 'component-item--bound': item.unionId === product.networkUnionId }">
          <div><a-badge :status="isRunning(item) ? 'success' : 'normal'" /><strong>{{ item.name }}</strong><a-tag>{{ typeLabel(item.type) }}</a-tag></div>
          <p>{{ componentHost(item) }}</p><code>{{ item.unionId }}</code>
          <a-button type="primary" size="small" :disabled="item.unionId === product.networkUnionId || !isRunning(item)" :loading="binding" @click="bind(item)"><template #icon><icon-link /></template>绑定</a-button>
        </article>
      </div>
    </a-modal>
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { NetworkComponentItem, NetworkType } from '@/apis/iot/network'
import type { ProductConnectInfo, ProductItem } from '@/apis/iot/product'
import { Message, Tag } from '@arco-design/web-vue'
import { getNetworkComponentList, getProductMqttPassword, updateProductNetwork } from '@/apis/iot/network'
import { getProductConnectInfo } from '@/apis/iot/product'

defineOptions({ name: 'ProductConnectionPanel' })

const props = defineProps<{ product: ProductItem }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()
const loading = ref(false)
const componentLoading = ref(false)
const binding = ref(false)
const passwordLoading = ref(false)
const managerVisible = ref(false)
const mode = ref('builtin')
const info = ref<ProductConnectInfo>({})
const components = ref<NetworkComponentItem[]>([])
const revealedPassword = ref('')
const platform = computed(() => String(props.product.thirdPlatform || '').toLowerCase())
const platformTitle = computed(() => platform.value.toUpperCase() || '网络')
const modeOptions = computed(() => {
  if (platform.value === 'mqtt') return [{ label: '平台直连', value: 'builtin' }, { label: '自建接入', value: 'network', disabled: !getModeInfo('network').enabled }]
  return []
})
const modeDescription = computed(() => {
  if (mode.value === 'builtin') return '使用平台内置 MQTT Broker，设备直接连接并使用平台标准主题。'
  return `通过自建 ${platformTitle.value} 组件接入，组件需处于运行状态并允许设备网络访问。`
})
const currentDetails = computed(() => {
  const data = getModeInfo(mode.value)
  if (mode.value === 'network' && !data.enabled) return []
  const list = [
    { label: '连接方式', value: String(data.connectionType || modeOptions.value.find((item) => item.value === mode.value)?.label || '') },
    { label: platform.value === 'mqtt' ? '连接地址' : '监听地址', value: String(data.host || data.address || '') },
    { label: '端口', value: String(data.port || '') },
    { label: '用户名', value: String(data.username || '') },
    ...(platform.value === 'mqtt' ? [{ label: '密码', value: '', secret: true }] : []),
    { label: 'Client ID', value: String(data.clientIdDisplay || data.clientId || '') },
    { label: '启用状态', value: data.state === true ? '已启用' : data.state === false ? '已停用' : '' },
    { label: '配置说明', value: String(data.description || '') }
  ]
  return list.filter((item) => item.value || item.secret)
})
const topics = computed(() => flattenTopics(getModeInfo(mode.value).topics || info.value.topics))
const topicColumns: TableColumnData[] = [
  { title: '分类', dataIndex: 'category', width: 130, render: ({ record }) => <Tag>{record.category}</Tag> },
  { title: '用途', dataIndex: 'label', width: 160 },
  { title: '主题', dataIndex: 'value', ellipsis: true, tooltip: true },
  { title: '设备动作', dataIndex: 'action', width: 100 }
]

function getModeInfo(key: string): Record<string, unknown> {
  const value = info.value[key]
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}
function flattenTopics(value: unknown) {
  if (!value || typeof value !== 'object') return []
  const rows: { category: string, label: string, value: string, action: string }[] = []
  for (const [category, group] of Object.entries(value as Record<string, unknown>)) {
    if (!group || typeof group !== 'object') continue
    for (const [label, topic] of Object.entries(group as Record<string, unknown>)) {
      if (topic) rows.push({ category: category.replace('Topics', ''), label, value: String(topic), action: /down|command/i.test(label) ? '订阅' : '发布' })
    }
  }
  return rows
}
function componentConfig(item: NetworkComponentItem) {
  try {
    return JSON.parse(item.configuration || '{}') as Record<string, unknown>
  } catch {
    return {}
  }
}
function componentHost(item: NetworkComponentItem) {
  const config = componentConfig(item)
  return [config.host, config.port].filter(Boolean).join(':') || '未配置服务地址'
}
function isRunning(item: NetworkComponentItem) {
  return Boolean(item.running ?? item.state)
}
function typeLabel(type: string) {
  return ({ MQTT_SERVER: 'MQTT 服务端', MQTT_CLIENT: 'MQTT 客户端' } as Record<string, string>)[type] || type
}

async function loadInfo() {
  loading.value = true
  revealedPassword.value = ''
  try {
    info.value = (await getProductConnectInfo(props.product.productKey)).data || {}
    if (platform.value === 'mqtt') mode.value = props.product.networkUnionId ? 'network' : 'builtin'
    else mode.value = 'network'
  } finally {
    loading.value = false
  }
}
async function refreshConnection() {
  emit('refresh')
  await loadInfo()
}
async function revealPassword() {
  passwordLoading.value = true
  try {
    const data = (await getProductMqttPassword(props.product.productKey)).data
    const source = data[mode.value]
    const detail = source && typeof source === 'object' ? source as Record<string, unknown> : data
    revealedPassword.value = String(detail.password || '未返回密码')
  } finally {
    passwordLoading.value = false
  }
}
async function loadComponents() {
  componentLoading.value = true
  try {
    const types: NetworkType[] = ['MQTT_SERVER', 'MQTT_CLIENT']
    const res = await getNetworkComponentList({ page: 1, size: 100, types, productKey: props.product.productKey })
    components.value = res.data.records || []
  } finally {
    componentLoading.value = false
  }
}
async function openManager() {
  managerVisible.value = true
  await loadComponents()
}
async function bind(item: NetworkComponentItem) {
  binding.value = true
  try {
    await updateProductNetwork(props.product.id, item.unionId)
    Message.success('接入组件绑定成功')
    emit('refresh')
    managerVisible.value = false
    await loadInfo()
  } finally {
    binding.value = false
  }
}
async function unbind() {
  binding.value = true
  try {
    await updateProductNetwork(props.product.id)
    Message.success('接入组件已解绑')
    emit('refresh')
    managerVisible.value = false
    await loadInfo()
  } finally {
    binding.value = false
  }
}

watch(() => [props.product.productKey, props.product.networkUnionId], loadInfo, { immediate: true })
</script>

<style lang="scss" scoped>
.connection-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;

    h2 {
      margin: 0;
      font-size: 18px;
      letter-spacing: 0;
    }

    p {
      margin: 5px 0 0;
      color: var(--color-text-3);
    }
  }

  &__description {
    margin: 12px 0;
  }

  &__guide,
  &__advanced {
    margin-top: 16px;
  }

  &__advanced :deep(.arco-descriptions) {
    margin-top: 12px;
  }

  &__content {
    display: block;
    width: 100%;
  }

  &__content :deep(.arco-spin-child),
  &__details {
    width: 100%;
  }

  &__details :deep(.arco-descriptions-body),
  &__details :deep(.arco-descriptions-table) {
    width: 100%;
  }
}

.connection-modes {
  margin-top: 16px;
}

.topic-section {
  margin-top: 18px;

  h3 {
    margin: 0 0 10px;
    font-size: 15px;
  }

  .arco-alert {
    margin-top: 10px;
  }
}

.component-status,
.component-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.component-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.component-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 12px;
  padding: 12px;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &--bound {
    border-color: rgb(var(--primary-6));
  }

  > div {
    display: flex;
    gap: 7px;
    align-items: center;
  }

  p,
  code {
    grid-column: 1;
    margin: 0;
    color: var(--color-text-3);
  }

  .arco-btn {
    grid-row: 1 / 4;
    grid-column: 2;
    align-self: center;
  }
}

@media (width <= 720px) {
  .connection-panel__header,
  .component-status {
    flex-direction: column;
  }

  .component-list {
    grid-template-columns: 1fr;
  }

  .connection-panel__details :deep(.arco-descriptions-item-label) {
    width: 112px;
  }
}
</style>
