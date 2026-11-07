<template>
  <GiPageLayout margin :body-style="{ overflowY: 'auto' }">
    <a-row justify="space-between" class="g-row-tool">
      <a-button v-hasPerm="['network:http:add']" type="primary" @click="openForm()"
        >
<template #icon><icon-plus /></template>新增 HTTP 服务
</a-button
      >
      <a-space
        wrap
        >
<a-input
          v-model.trim="query.name"
          placeholder="服务名称"
          allow-clear
          @press-enter="search" /><a-select
          v-model="query.state"
          :options="stateOptions"
          placeholder="启用状态"
          allow-clear /><GiButton type="search" @click="search" /><GiButton
          type="reset"
          @click="reset"
      /><a-radio-group v-model="viewMode" type="button">
          <a-radio value="card"><icon-apps /> 卡片</a-radio>
          <a-radio value="table"><icon-list /> 列表</a-radio>
        </a-radio-group>
</a-space>
    </a-row>
    <a-spin v-if="viewMode === 'card'" :loading="loading" class="http-network__view">
      <a-empty v-if="!loading && !tableData.length" description="暂无 HTTP 服务" />
      <div v-else class="http-network__grid">
        <article v-for="record in tableData" :key="record.id" class="http-network__card">
          <header><div class="http-network__icon"><icon-code /></div><div class="http-network__identity"><a-link v-if="canQuery" @click="openDetail(record)"><strong>{{ record.name || '未命名服务' }}</strong></a-link><strong v-else>{{ record.name || '未命名服务' }}</strong><span>HTTP 接入服务</span></div><a-tag :color="record.running ? 'green' : 'gray'">{{ record.running ? '运行中' : '已停止' }}</a-tag></header>
          <div class="http-network__callback"><span>设备回调地址</span><a-typography-text copyable :ellipsis="{ showTooltip: true }" :title="callbackUrl(record)">{{ callbackUrl(record) }}</a-typography-text></div>
          <div class="http-network__facts"><div><span>鉴权方式</span><strong>{{ authTypeText(parseConfig(record.configuration).inbound.auth.type) }}</strong></div><div><span>请求限流</span><strong>{{ parseConfig(record.configuration).inbound.rateLimitPerMinute }}/分钟</strong></div><div><span>最大请求体</span><strong>{{ formatBytes(parseConfig(record.configuration).inbound.maxBodyBytes) }}</strong></div><div><span>出站范围</span><strong>{{ parseConfig(record.configuration).outbound.allowPrivateNetwork ? '允许内网' : '仅公网' }}</strong></div></div>
          <NetworkProductPreview :products="httpProducts(record)" :total="httpProductCount(record)" :disabled="!httpProductCount(record) && !canEdit" @click="handleHttpProductPreview(record)" />
          <footer><a-tag size="small" :color="record.state === 1 ? 'blue' : 'gray'">{{ record.state === 1 ? '已启用' : '已停用' }}</a-tag><a-space :size="2"><a-tooltip v-if="canQuery" content="运行详情"><a-button type="text" shape="circle" @click="openDetail(record)"><IconEye /></a-button></a-tooltip><a-tooltip v-if="canEdit" content="编辑"><a-button type="text" shape="circle" @click="openForm(record)"><IconEdit /></a-button></a-tooltip><a-popconfirm v-if="record.running ? canStop : canStart" :content="record.running ? '确定停止该服务吗？' : '确定启动该服务吗？'" @before-ok="() => changeRuntime(record, !record.running)"><a-tooltip :content="record.running ? '停止' : record.state === 1 ? '启动' : '组件已停用'"><a-button type="text" shape="circle" :disabled="!record.running && record.state !== 1"><IconPause v-if="record.running" /><IconPlayArrow v-else /></a-button></a-tooltip></a-popconfirm><a-popconfirm v-if="canRestart" content="确定重启该服务吗？" @before-ok="() => restart(record)"><a-tooltip content="重启"><a-button type="text" shape="circle" :disabled="record.state !== 1"><IconRefresh /></a-button></a-tooltip></a-popconfirm><a-popconfirm v-if="canDelete" content="确定删除该 HTTP 服务吗？" @before-ok="() => remove(record)"><a-tooltip content="删除"><a-button type="text" status="danger" shape="circle"><IconDelete /></a-button></a-tooltip></a-popconfirm></a-space></footer>
        </article>
      </div>
      <div v-if="tableData.length" class="http-network__pagination"><a-pagination v-bind="pagination" /></div>
    </a-spin>
    <a-table
      v-else
      class="g-table"
      row-key="id"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1200 }"
    />
    <a-modal
      v-model:visible="formVisible"
      :title="form.id ? '编辑 HTTP 服务' : '新增 HTTP 服务'"
      width="min(900px, calc(100vw - 32px))"
      align-center
      :mask-closable="false"
      :body-style="modalBodyStyle"
      unmount-on-close
    >
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
          >
<a-form-item
            label="服务名称"
            field="name"
            :rules="[{ required: true, message: '请输入服务名称' }]"
            >
<a-input v-model.trim="form.name" />
</a-form-item
          ><a-form-item label="绑定 HTTP 产品" field="productKey" :rules="[{ required: true, message: '请选择绑定产品' }]"
            >
<a-select v-model="form.productKey" :options="productOptions" :loading="productLoading" :disabled="!!form.id" allow-search placeholder="请选择 HTTP 产品" @change="selectProduct" />
</a-form-item
          ><a-form-item label="访问码"
            >
<a-input v-model.trim="config.accessCode" :disabled="!!form.id" />
</a-form-item
          ><a-form-item label="启用状态"
            >
<a-switch
              v-model="enabled"
              checked-text="启用"
              unchecked-text="停用" />
</a-form-item
        >
</a-grid>
        <a-divider orientation="left">入站请求</a-divider>
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
          >
<a-form-item label="鉴权方式"
            >
<a-select
              v-model="config.inbound.auth.type"
              :options="authOptions" />
</a-form-item
          ><a-form-item
            v-if="config.inbound.auth.type === 'API_KEY'"
            label="请求头名称"
            >
<a-input
              v-model.trim="config.inbound.auth.headerName" />
</a-form-item
          ><a-form-item
            v-if="config.inbound.auth.type === 'API_KEY'"
            label="API Key"
            >
<a-input-password v-model="config.inbound.auth.value"><template #suffix><a-tooltip content="生成新密钥"><IconRefresh class="http-network__generate" @click="regenerateSecret('value')" /></a-tooltip></template></a-input-password>
</a-form-item
          ><a-form-item
            v-if="config.inbound.auth.type === 'HMAC_SHA256'"
            label="HMAC 密钥"
            >
<a-input-password v-model="config.inbound.auth.secret"><template #suffix><a-tooltip content="生成新密钥"><IconRefresh class="http-network__generate" @click="regenerateSecret('secret')" /></a-tooltip></template></a-input-password>
</a-form-item
          ><a-form-item label="每分钟限流"
            >
<a-input-number
              v-model="config.inbound.rateLimitPerMinute"
              :min="1" />
</a-form-item
          ><a-form-item label="最大请求体（字节）"
            >
<a-input-number
              v-model="config.inbound.maxBodyBytes"
              :min="1024" />
</a-form-item
        >
</a-grid>
        <a-divider orientation="left">出站安全</a-divider>
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
          >
<a-form-item label="连接超时（毫秒）"
            >
<a-input-number
              v-model="config.outbound.connectTimeoutMs"
              :min="100" />
</a-form-item
          ><a-form-item label="请求超时（毫秒）"
            >
<a-input-number
              v-model="config.outbound.requestTimeoutMs"
              :min="100" />
</a-form-item
          ><a-form-item label="最大响应（字节）"
            >
<a-input-number
              v-model="config.outbound.maxResponseBytes"
              :min="1024" />
</a-form-item
          ><a-form-item label="最大出站请求（字节）"><a-input-number v-model="config.outbound.maxRequestBytes" :min="1" /></a-form-item
          ><a-form-item label="允许访问内网"
            >
<a-switch
              v-model="config.outbound.allowPrivateNetwork" />
</a-form-item
        >
</a-grid>
        <template v-if="config.inbound.auth.type === 'HMAC_SHA256'">
          <a-divider orientation="left">HMAC 签名</a-divider>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
            <a-form-item label="签名请求头"><a-input v-model.trim="config.inbound.auth.signatureHeader" /></a-form-item>
            <a-form-item label="时间戳请求头"><a-input v-model.trim="config.inbound.auth.timestampHeader" /></a-form-item>
            <a-form-item label="随机数请求头"><a-input v-model.trim="config.inbound.auth.nonceHeader" /></a-form-item>
            <a-form-item label="时间偏差（秒）"><a-input-number v-model="config.inbound.auth.clockSkewSeconds" :min="1" :max="3600" /></a-form-item>
          </a-grid>
        </template>
        <a-form-item label="描述"
          >
<a-textarea
            v-model="form.description"
            :auto-size="{ minRows: 3, maxRows: 5 }"
        />
</a-form-item>
      </a-form>
      <template #footer
        >
<a-space
          >
<a-button @click="formVisible = false">取消</a-button
          ><a-button :loading="validating" @click="validateConfig"
            >
验证配置
</a-button
          ><a-button type="primary" :loading="saving" @click="save"
            >
<template #icon><icon-save /></template>保存
</a-button
          >
</a-space
        >
</template
      >
    </a-modal>
    <a-modal
      v-model:visible="detailVisible"
      title="HTTP 服务运行详情"
      width="min(820px, calc(100vw - 32px))"
      align-center
      :body-style="modalBodyStyle"
      unmount-on-close
      >
<a-descriptions v-if="current" :column="descriptionColumns" bordered
        >
<a-descriptions-item label="服务名称">
{{
          current.name
        }}
</a-descriptions-item
        ><a-descriptions-item label="运行状态"
          >
<a-tag :color="current.running ? 'green' : 'gray'">
{{
            current.running ? "运行中" : "已停止"
          }}
</a-tag>
</a-descriptions-item
        ><a-descriptions-item label="回调地址" :span="2"
          >
<a-typography-paragraph copyable>
{{
            callbackUrl(current)
          }}
</a-typography-paragraph>
</a-descriptions-item
        ><a-descriptions-item label="ProductKey">{{ current.productKey || '-' }}</a-descriptions-item
        ><a-descriptions-item label="鉴权方式">{{ authTypeText(parseConfig(current.configuration).inbound.auth.type) }}</a-descriptions-item
        ><a-descriptions-item label="访问标识">{{ parseConfig(current.configuration).accessCode }}</a-descriptions-item
        ><a-descriptions-item label="配置版本">{{ parseConfig(current.configuration).revision }}</a-descriptions-item
        ><a-descriptions-item label="运行指标" :span="2">
          <pre class="http-network__runtime">{{
            JSON.stringify(runtimeStatus, null, 2)
          }}</pre>
        </a-descriptions-item>
</a-descriptions
      ><template #footer
        >
<a-space wrap
          >
<a-button :loading="runtimeLoading" @click="loadRuntime"
            >
<template #icon><IconRefresh /></template>刷新状态
</a-button
          ><a-button :loading="polling" @click="poll"
            >
<template #icon><icon-thunderbolt /></template>立即轮询
</a-button
          ><a-button :loading="reloading" @click="reload"
            >
<template #icon><icon-sync /></template>重载配置
</a-button
          >
</a-space
        >
</template
      >
</a-modal
    >
    <NetworkBoundProductsModal ref="boundProductsRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { FormInstance, TableColumnData } from '@arco-design/web-vue'
import type { NetworkComponentItem } from '@/apis/iot/network'
import {
  Button,
  Message,
  Popconfirm,
  Space,
  Tag,
  Tooltip
} from '@arco-design/web-vue'
import {
  IconDelete,
  IconEdit,
  IconEye,
  IconPause,
  IconPlayArrow,
  IconRefresh
} from '@arco-design/web-vue/es/icon'
import { useMediaQuery, useStorage } from '@vueuse/core'
import {
  addNetworkComponent,
  deleteNetworkComponents,
  getHttpRuntimeStatus,
  getNetworkComponent,
  getNetworkComponentList,
  reloadHttpNetwork,
  restartNetworkComponent,
  runHttpPoll,
  startNetworkComponent,
  stopNetworkComponent,
  updateNetworkComponent,
  validateHttpConfiguration
} from '@/apis/iot/network'
import { getProductList } from '@/apis/iot/product'
import { createUuid } from '@/utils'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import NetworkBoundProductsModal from '../modules/NetworkBoundProductsModal.vue'
import NetworkProductPreview from '../modules/NetworkProductPreview.vue'

defineOptions({ name: 'IotNetworkHttp' })
const UUID_HYPHEN_RE = /-/g
const PRODUCT_SUFFIX_RE = /\s*\([^)]*\)$/

interface HttpConfig {
  schemaVersion: number
  revision: number
  accessCode: string
  inbound: {
    auth: {
      type: string
      headerName: string
      value: string
      secret: string
      signatureHeader: string
      timestampHeader: string
      nonceHeader: string
      clockSkewSeconds: number
    }
    maxBodyBytes: number
    rateLimitPerMinute: number
  }
  outbound: {
    allowPrivateNetwork: boolean
    connectTimeoutMs: number
    requestTimeoutMs: number
    maxRequestBytes: number
    maxResponseBytes: number
  }
}

function createConfig(): HttpConfig {
  return {
    schemaVersion: 2,
    revision: 1,
    accessCode: `http-${Date.now().toString(36)}`,
    inbound: {
      auth: {
        type: 'API_KEY',
        headerName: 'X-API-Key',
        value: createUuid().replace(UUID_HYPHEN_RE, ''),
        secret: '',
        signatureHeader: 'X-Signature',
        timestampHeader: 'X-Timestamp',
        nonceHeader: 'X-Nonce',
        clockSkewSeconds: 300
      },
      maxBodyBytes: 262144,
      rateLimitPerMinute: 600
    },
    outbound: {
      allowPrivateNetwork: false,
      connectTimeoutMs: 5000,
      requestTimeoutMs: 10000,
      maxRequestBytes: 1048576,
      maxResponseBytes: 4194304
    }
  }
}

const query = reactive({ name: '', state: undefined as number | undefined })
const viewMode = useStorage<'card' | 'table'>('nexiot-network-http-view', 'card')
const canQuery = computed(() => hasPerm('network:http:query') || hasPerm('tool:http:list'))
const canEdit = computed(() => hasPerm('network:http:edit'))
const canDelete = computed(() => hasPerm('network:http:del'))
const canStart = computed(() => hasPerm('network:http:start'))
const canStop = computed(() => hasPerm('network:http:stop'))
const canRestart = computed(() => hasPerm('network:http:restart'))
const stateOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]
const authOptions = [
  { label: '无鉴权', value: 'NONE' },
  { label: 'API Key', value: 'API_KEY' },
  { label: 'HMAC-SHA256', value: 'HMAC_SHA256' }
]
const { loading, tableData, pagination, search }
  = useTable<NetworkComponentItem>({
    listAPI: (page) =>
      getNetworkComponentList({
        ...page,
        types: ['HTTP_SERVER'],
        name: query.name || undefined,
        state: query.state
      }),
    immediate: true
  })
const formRef = useTemplateRef<FormInstance>('formRef')
const boundProductsRef = useTemplateRef<InstanceType<typeof NetworkBoundProductsModal>>('boundProductsRef')
const productLoading = ref(false)
const productOptions = ref<{ label: string, value: string }[]>([])
const formVisible = ref(false)
const saving = ref(false)
const validating = ref(false)
const form = reactive<Partial<NetworkComponentItem>>({
  id: '',
  name: '',
  type: 'HTTP_SERVER',
  productKey: '',
  state: 1,
  description: ''
})
const config = reactive<HttpConfig>(createConfig())
const enabled = computed({
  get: () => form.state === 1,
  set: (value) => {
    form.state = value ? 1 : 0
  }
})
const detailVisible = ref(false)
const current = ref<NetworkComponentItem>()
const runtimeStatus = ref<Record<string, unknown>>({})
const runtimeLoading = ref(false)
const polling = ref(false)
const reloading = ref(false)
let runtimeTimer: ReturnType<typeof setInterval> | undefined
const modalBodyStyle = {
  maxHeight: 'min(72vh, 720px)',
  overflowY: 'auto' as const
}
const isNarrow = useMediaQuery('(max-width: 720px)')
const descriptionColumns = computed(() => isNarrow.value ? 1 : 2)

function reset() {
  Object.assign(query, { name: '', state: undefined })
  search()
}
function resetForm() {
  Object.assign(form, {
    id: '',
    name: '',
    type: 'HTTP_SERVER',
    productKey: '',
    state: 1,
    description: ''
  })
  Object.assign(config, createConfig())
}
function parseConfig(value?: string) {
  try {
    const defaults = createConfig()
    const parsed = JSON.parse(value || '{}')
    return {
      ...defaults,
      ...parsed,
      inbound: {
        ...defaults.inbound,
        ...(parsed.inbound || {}),
        auth: {
          ...defaults.inbound.auth,
          ...(parsed.inbound?.auth || {})
        }
      },
      outbound: { ...defaults.outbound, ...(parsed.outbound || {}) }
    } as HttpConfig
  } catch {
    return createConfig()
  }
}
async function openForm(record?: NetworkComponentItem) {
  resetForm()
  await loadProducts()
  if (record) {
    const detail = (await getNetworkComponent(String(record.id))).data
    Object.assign(form, detail)
    Object.assign(config, parseConfig(detail.configuration))
  }
  formVisible.value = true
}
async function loadProducts() {
  productLoading.value = true
  try {
    const res = await getProductList({ page: 1, size: 1000 })
    productOptions.value = (res.data.records || [])
      .filter((item) => String(item.thirdPlatform || '').toLowerCase() === 'http')
      .map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey }))
  } finally { productLoading.value = false }
}
function selectProduct(productKey: unknown) {
  if (form.name) return
  const selected = productOptions.value.find((item) => item.value === String(productKey))
  if (selected) form.name = `${selected.label.replace(PRODUCT_SUFFIX_RE, '')} HTTP 服务`
}
function authTypeText(type: string) {
  return ({ NONE: '无鉴权', API_KEY: 'API Key', HMAC_SHA256: 'HMAC-SHA256' } as Record<string, string>)[type] || type
}
function regenerateSecret(field: 'value' | 'secret') {
  config.inbound.auth[field] = createUuid().replace(UUID_HYPHEN_RE, '') + createUuid().replace(UUID_HYPHEN_RE, '').slice(0, 16)
}
function buildPayload() {
  return {
    ...form,
    type: 'HTTP_SERVER' as const,
    configuration: JSON.stringify(config)
  }
}
async function validateConfig() {
  validating.value = true
  try {
    await validateHttpConfiguration(JSON.stringify(config))
    Message.success('配置验证通过')
  } finally {
    validating.value = false
  }
}
async function save() {
  if (await formRef.value?.validate()) return
  saving.value = true
  try {
    if (form.id) config.revision = Number(config.revision || 0) + 1
    await validateHttpConfiguration(JSON.stringify(config))
    await (form.id
      ? updateNetworkComponent(buildPayload())
      : addNetworkComponent(buildPayload()))
    Message.success('保存成功')
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}
function callbackUrl(record: NetworkComponentItem) {
  return `${window.location.origin}/iot/http/v2/${parseConfig(record.configuration).accessCode}`
}
function formatBytes(value: number) {
  if (value >= 1024 * 1024) return `${value / 1024 / 1024} MB`
  if (value >= 1024) return `${value / 1024} KB`
  return `${value} B`
}
function httpProducts(record: NetworkComponentItem) {
  if (record.bindProducts?.length) return record.bindProducts
  return record.productKey ? [{ productKey: record.productKey }] : []
}
function httpProductCount(record: NetworkComponentItem) {
  return Math.max(record.bindProductCount || 0, httpProducts(record).length)
}
function openBoundProducts(record: NetworkComponentItem) {
  boundProductsRef.value?.open(record.name, httpProducts(record), httpProductCount(record))
}
function handleHttpProductPreview(record: NetworkComponentItem) {
  if (httpProductCount(record)) openBoundProducts(record)
  else if (canEdit.value) openForm(record)
}
async function changeRuntime(record: NetworkComponentItem, running: boolean) {
  await (running
    ? startNetworkComponent(String(record.id))
    : stopNetworkComponent(String(record.id)))
  search()
  return true
}
async function restart(record: NetworkComponentItem) {
  await restartNetworkComponent(String(record.id))
  Message.success('重启成功')
  search()
  return true
}
async function remove(record: NetworkComponentItem) {
  await deleteNetworkComponents([String(record.id)])
  search()
  return true
}
async function openDetail(record: NetworkComponentItem) {
  current.value = (await getNetworkComponent(String(record.id))).data
  detailVisible.value = true
  await loadRuntime()
  clearInterval(runtimeTimer)
  runtimeTimer = setInterval(() => {
    if (detailVisible.value) loadRuntime()
  }, 10000)
}

watch(detailVisible, (value) => {
  if (!value) {
    clearInterval(runtimeTimer)
    runtimeTimer = undefined
  }
})
onBeforeUnmount(() => clearInterval(runtimeTimer))
async function loadRuntime() {
  if (!current.value) return
  runtimeLoading.value = true
  try {
    runtimeStatus.value
      = (await getHttpRuntimeStatus(String(current.value.id))).data || {}
  } finally {
    runtimeLoading.value = false
  }
}
async function poll() {
  if (!current.value) return
  polling.value = true
  try {
    runtimeStatus.value
      = (await runHttpPoll(String(current.value.id))).data || {}
    Message.success('轮询完成')
  } finally {
    polling.value = false
  }
}
async function reload() {
  if (!current.value) return
  reloading.value = true
  try {
    await reloadHttpNetwork(String(current.value.id))
    Message.success('配置已重载')
    await loadRuntime()
  } finally {
    reloading.value = false
  }
}

const columns: TableColumnData[] = [
  {
    title: '服务名称',
    width: 220,
    render: ({ record }) => (
      <div>
        <strong>{record.name}</strong>
        <div class="http-network__subtitle">HTTP 接入服务</div>
      </div>
    )
  },
  {
    title: '回调地址',
    width: 320,
    render: ({ record }) => callbackUrl(record as NetworkComponentItem)
  },
  {
    title: '鉴权',
    width: 120,
    render: ({ record }) => (
      <Tag>{parseConfig(record.configuration).inbound.auth.type}</Tag>
    )
  },
  {
    title: '绑定产品',
    width: 130,
    render: ({ record }) => httpProductCount(record as NetworkComponentItem)
      ? (
          <Button type="text" onClick={() => openBoundProducts(record as NetworkComponentItem)}>
            {httpProductCount(record as NetworkComponentItem)}
            个产品
          </Button>
        )
      : '未绑定'
  },
  {
    title: '启用状态',
    width: 100,
    render: ({ record }) => (
      <Tag color={record.state === 1 ? 'green' : 'gray'}>
        {record.state === 1 ? '启用' : '停用'}
      </Tag>
    )
  },
  {
    title: '运行状态',
    width: 100,
    render: ({ record }) => (
      <Tag color={record.running ? 'green' : 'gray'}>
        {record.running ? '运行中' : '已停止'}
      </Tag>
    )
  },
  { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true },
  {
    title: '操作',
    width: 190,
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        {canQuery.value && (
          <Tooltip content="详情">
            <Button
              type="text"
              shape="circle"
              onClick={() => openDetail(record as NetworkComponentItem)}
            >
              <IconEye />
            </Button>
          </Tooltip>
        )}
        {canEdit.value && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              onClick={() => openForm(record as NetworkComponentItem)}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {(record.running ? canStop.value : canStart.value) && (
          <Popconfirm
            content={record.running ? '确定停止该服务吗？' : '确定启动该服务吗？'}
            onBeforeOk={() =>
              changeRuntime(record as NetworkComponentItem, !record.running)}
          >
            <Tooltip content={record.running ? '停止' : record.state === 1 ? '启动' : '组件已停用'}>
              <Button type="text" shape="circle" disabled={!record.running && record.state !== 1}>
                {record.running ? <IconPause /> : <IconPlayArrow />}
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
        {canRestart.value && <Popconfirm content="确定重启该服务吗？" onBeforeOk={() => restart(record as NetworkComponentItem)}><Tooltip content={record.state === 1 ? '重启' : '组件已停用'}><Button type="text" shape="circle" disabled={record.state !== 1}><IconRefresh /></Button></Tooltip></Popconfirm>}
        {canDelete.value && (
          <Popconfirm
            content="确定删除该 HTTP 服务吗？"
            onBeforeOk={() => remove(record as NetworkComponentItem)}
          >
            <Tooltip content="删除">
              <Button type="text" status="danger" shape="circle">
                <IconDelete />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]
</script>

<style lang="scss" scoped>
.http-network {
  &__subtitle {
    margin-top: 3px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__runtime {
    max-height: 360px;
    margin: 0;
    overflow: auto;
    word-break: break-all;
    white-space: pre-wrap;
  }

  &__view {
    display: block;
    padding: 2px 4px 4px 0;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(min(100%, 360px), 1fr));
    gap: 14px;
  }

  &__card {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
    background: var(--color-bg-2);
    border: 1px solid var(--color-border-2);
    border-radius: 6px;
    transition: border-color .2s, box-shadow .2s;
  }

  &__card:hover {
    border-color: var(--color-border-3);
    box-shadow: 0 4px 12px rgb(0 0 0 / 5%);
  }

  &__card :deep(.arco-tag) {
    background: transparent;
    border: 1px solid var(--color-border-2);
  }

  &__card header {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    padding: 14px 14px 0;
  }

  &__identity {
    flex: 1;
    min-width: 0;
  }

  &__icon {
    display: flex;
    flex: 0 0 auto;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    font-size: 18px;
    color: rgb(var(--primary-6));
    background: var(--color-primary-light-1);
    border-radius: 6px;
  }

  &__card header :deep(.arco-link) {
    display: block;
    max-width: 100%;
  }

  &__card header strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-1);
    white-space: nowrap;
  }

  &__card header span {
    display: block;
    margin-top: 3px;
    font-size: 11px;
    color: var(--color-text-3);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__callback {
    padding: 8px 10px;
    margin: 10px 14px 0;
    overflow: hidden;
    background: var(--color-fill-1);
    border-radius: 6px;
  }

  &__callback :deep(.arco-typography) {
    display: block;
    max-width: 100%;
  }

  &__callback > span,
  &__facts span {
    display: block;
    margin-bottom: 2px;
    font-size: 11px;
    color: var(--color-text-3);
  }

  &__facts {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    padding: 10px 14px 0;
    margin-top: 0;
  }

  &__facts > div {
    min-width: 0;
    padding: 8px 10px;
    background: var(--color-fill-1);
    border-radius: 6px;
  }

  &__facts strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    font-weight: 500;
    color: var(--color-text-1);
    white-space: nowrap;
  }

  &__metric-action {
    display: inline-flex;
    max-width: 100%;
    height: auto;
    padding: 0;
    font-weight: 500;
  }

  &__metric-action span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__metric-action svg {
    flex: 0 0 auto;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  &__generate {
    color: rgb(var(--primary-6));
    cursor: pointer;
  }

  &__card footer {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
    padding: 8px 14px;
    margin-top: auto;
    background: var(--color-fill-1);
    border-top: 1px solid var(--color-border-2);
  }
}

.http-network__card :deep(.network-product-preview) {
  margin: 10px 14px 14px;
  width: calc(100% - 28px);
}

:deep(.arco-input-number) {
  width: 100%;
}

@media (width <= 640px) {
  .http-network__grid {
    grid-template-columns: 1fr;
  }

  .http-network__card footer {
    align-items: flex-start;
  }
}
</style>
