<template>
  <a-modal
    v-model:visible="visible"
    :title="isEdit ? '编辑 MQTT 组件' : '新增 MQTT 组件'"
    :width="820"
    :modal-style="{ maxWidth: 'calc(100vw - 32px)' }"
    :body-style="{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto' }"
    :mask-closable="false"
    align-center
    unmount-on-close
  >
    <a-form ref="formRef" :model="form" layout="vertical" class="mqtt-form">
      <a-divider orientation="left">基础信息</a-divider>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item
          label="组件类型"
          field="type"
          :rules="[{ required: true, message: '请选择组件类型' }]"
        >
          <a-select
            v-model="form.type"
            :options="typeOptions"
            :disabled="isEdit"
            @change="handleTypeChange"
          />
        </a-form-item>
        <a-form-item
          label="组件名称"
          field="name"
          :rules="[{ required: true, message: '请输入组件名称' }]"
        >
          <a-input v-model.trim="form.name" :max-length="64" />
        </a-form-item>
        <a-form-item label="启用状态">
          <a-switch
            v-model="enabled"
            checked-text="启用"
            unchecked-text="停用"
          />
        </a-form-item>
        <a-form-item v-if="isEdit" label="唯一标识">
          <a-input :model-value="form.unionId" disabled />
        </a-form-item>
      </a-grid>
      <a-divider orientation="left">连接认证</a-divider>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item
          label="Broker 地址"
          field="configuration.host"
          :rules="[{ required: true, message: '请输入 Broker 地址' }]"
        >
          <a-input
            v-model.trim="form.configuration.host"
            placeholder="tcp://127.0.0.1:1883"
          />
        </a-form-item>
        <a-form-item label="端口">
          <a-input-number
            v-model="form.configuration.port"
            :min="1"
            :max="65535"
            placeholder="地址中已包含端口时可留空"
          />
        </a-form-item>
        <a-form-item label="客户端 ID">
          <a-input
            v-model.trim="form.configuration.clientId"
            placeholder="留空时由系统自动生成"
          />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model.trim="form.configuration.username" />
        </a-form-item>
        <a-form-item label="密码">
          <a-input-password
            v-model="form.configuration.password"
            placeholder="编辑时留空表示保留原密码"
          />
        </a-form-item>
        <a-form-item label="集群部署">
          <a-switch v-model="form.configuration.clusterEnabled" />
          <template #extra>
            开启后客户端 ID 会追加节点标识，固定客户端 ID 场景请关闭。
          </template>
        </a-form-item>
      </a-grid>

      <a-divider orientation="left">运行参数</a-divider>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item label="默认 QoS">
          <a-select
            v-model="form.configuration.defaultQos"
            :options="qosOptions"
          />
        </a-form-item>
        <a-form-item label="Keep Alive（秒）">
          <a-input-number
            v-model="form.configuration.keepAliveInterval"
            :min="0"
            :max="3600"
          />
        </a-form-item>
        <a-form-item label="连接超时（秒）">
          <a-input-number
            v-model="form.configuration.connectTimeout"
            :min="1"
            :max="600"
          />
        </a-form-item>
        <a-form-item label="清理会话">
          <a-switch v-model="form.configuration.cleanSession" />
        </a-form-item>
        <a-form-item label="自动重连">
          <a-switch v-model="form.configuration.autoReconnect" />
        </a-form-item>
      </a-grid>

      <template v-if="dynamicFields.length">
        <a-divider orientation="left">扩展参数</a-divider>
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <a-form-item
            v-for="field in dynamicFields"
            :key="field.key"
            :label="field.label"
            :required="field.required"
            :extra="field.help"
          >
            <a-input-number
              v-if="field.type === 'int' || field.type === 'number'"
              :model-value="getDynamicNumber(field.key)"
              :min="field.min"
              :max="field.max"
              :precision="field.type === 'int' ? 0 : undefined"
              @update:model-value="setDynamicValue(field.key, $event)"
            />
            <a-switch
              v-else-if="field.type === 'boolean'"
              :model-value="getDynamicBoolean(field.key)"
              @update:model-value="setDynamicValue(field.key, $event)"
            />
            <a-select
              v-else-if="field.type === 'select' || field.type === 'certificate'"
              :model-value="getDynamicSelect(field.key)"
              :options="field.type === 'certificate' ? certificateOptions : field.options"
              allow-clear
              allow-search
              @update:model-value="setDynamicValue(field.key, $event)"
            />
            <a-textarea
              v-else-if="field.type === 'json' || field.type === 'textarea'"
              :model-value="getDynamicText(field.key)"
              :auto-size="{ minRows: 3, maxRows: 7 }"
              @update:model-value="setDynamicValue(field.key, $event)"
            />
            <a-input-password
              v-else-if="field.type === 'password'"
              :model-value="getDynamicText(field.key)"
              @update:model-value="setDynamicValue(field.key, $event)"
            />
            <a-input
              v-else
              :model-value="getDynamicText(field.key)"
              :max-length="field.maxLength"
              allow-clear
              @update:model-value="setDynamicValue(field.key, $event)"
            />
          </a-form-item>
        </a-grid>
      </template>

      <a-divider orientation="left">SSL / TLS</a-divider>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item label="启用 TLS">
          <a-switch
            v-model="form.configuration.ssl"
            @change="handleSslChange"
          />
        </a-form-item>
        <a-form-item v-if="form.configuration.ssl" label="TLS 认证模式">
          <a-select
            v-model="form.configuration.sslMode"
            :options="sslModeOptions"
            @change="handleSslModeChange"
          />
        </a-form-item>
        <a-form-item v-if="form.configuration.ssl" label="服务端 CA 证书">
          <a-select
            v-model="form.configuration.sslCaCertKey"
            :options="certificateOptions"
            :loading="certificateLoading"
            allow-clear
            allow-search
          />
        </a-form-item>
        <a-form-item
          v-if="form.configuration.sslMode === 'MUTUAL_AUTH'"
          label="客户端证书"
        >
          <a-select
            v-model="form.configuration.sslClientCertKey"
            :options="certificateOptions"
            :loading="certificateLoading"
            allow-clear
            allow-search
          />
        </a-form-item>
        <a-form-item v-if="form.configuration.ssl" label="校验服务端主机名">
          <a-switch v-model="form.configuration.sslHostnameVerify" />
        </a-form-item>
      </a-grid>

      <a-form-item label="详细描述">
        <a-textarea
          v-model="form.description"
          :max-length="300"
          show-word-limit
          :auto-size="{ minRows: 3, maxRows: 5 }"
        />
      </a-form-item>
    </a-form>

    <template #footer>
      <a-space>
        <a-button @click="visible = false">取消</a-button>
        <a-button :loading="validating" @click="handleValidate">
          验证配置
        </a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><icon-save /></template>
          保存
        </a-button>
      </a-space>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance, SelectOptionData } from '@arco-design/web-vue'
import type {
  MqttConfiguration,
  MqttNetworkItem,
  MqttNetworkType
} from '@/apis/iot/mqtt'
import type { ProductConfigDictItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { getCertificateList } from '@/apis/iot/certificate'
import {
  addMqttNetwork,
  getMqttNetwork,
  parseMqttConfiguration,
  updateMqttNetwork,
  validateMqttNetwork
} from '@/apis/iot/mqtt'
import { getProductConfigDicts } from '@/apis/iot/product'

defineOptions({ name: 'MqttFormDrawer' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

interface MqttFormModel {
  id?: string
  type: MqttNetworkType
  name: string
  unionId?: string
  state: number
  description: string
  configuration: MqttConfiguration
}

interface DynamicMqttField {
  key: string
  label: string
  type: string
  help: string
  required: boolean
  min?: number
  max?: number
  maxLength?: number
  defaultValue?: string | number | boolean
  options: { label: string, value: string | number | boolean }[]
}

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const validating = ref(false)
const certificateLoading = ref(false)
const certificateOptions = ref<SelectOptionData[]>([])
const dynamicFields = ref<DynamicMqttField[]>([])
const dynamicOptionDicts = reactive<Record<string, ProductConfigDictItem[]>>({})
const form = reactive<MqttFormModel>(createDefaultForm())
const KNOWN_CONFIGURATION_KEYS = new Set([
  'host',
  'port',
  'clientId',
  'username',
  'password',
  'clusterEnabled',
  'defaultQos',
  'keepAliveInterval',
  'connectTimeout',
  'cleanSession',
  'autoReconnect',
  'ssl',
  'sslMode',
  'sslCaCertKey',
  'sslClientCertKey',
  'sslHostnameVerify',
  'subscribeTopics'
])

const typeOptions = [
  { label: 'MQTT 服务端', value: 'MQTT_SERVER' },
  { label: 'MQTT 客户端', value: 'MQTT_CLIENT' }
]
const qosOptions = [
  { label: 'QoS 0 - 最多一次', value: 0 },
  { label: 'QoS 1 - 至少一次', value: 1 },
  { label: 'QoS 2 - 仅一次', value: 2 }
]
const sslModeOptions = [
  { label: '单向认证', value: 'SERVER_AUTH' },
  { label: '双向认证', value: 'MUTUAL_AUTH' }
]
const enabled = computed({
  get: () => form.state === 1,
  set: (value: boolean) => {
    form.state = value ? 1 : 0
  }
})
function createDefaultConfiguration(): MqttConfiguration {
  return {
    host: 'tcp://127.0.0.1:1883',
    clientId: '',
    username: '',
    password: '',
    clusterEnabled: false,
    defaultQos: 1,
    keepAliveInterval: 60,
    connectTimeout: 30,
    cleanSession: true,
    autoReconnect: true,
    ssl: false,
    sslMode: 'DISABLED',
    sslCaCertKey: '',
    sslClientCertKey: '',
    sslHostnameVerify: true,
    subscribeTopics: []
  }
}

function createDefaultForm(): MqttFormModel {
  return {
    type: 'MQTT_SERVER',
    name: '',
    state: 0,
    description: '',
    configuration: createDefaultConfiguration()
  }
}

function resetForm() {
  Object.assign(form, createDefaultForm())
  form.configuration = createDefaultConfiguration()
  isEdit.value = false
  formRef.value?.clearValidate()
}

function handleTypeChange() {
  form.configuration = createDefaultConfiguration()
}

function handleSslChange(value: boolean | string | number) {
  form.configuration.sslMode = value ? 'SERVER_AUTH' : 'DISABLED'
  if (!value) form.configuration.sslClientCertKey = ''
}

function handleSslModeChange() {
  if (form.configuration.sslMode !== 'MUTUAL_AUTH') {
    form.configuration.sslClientCertKey = ''
  }
}

function normalizeConfiguration() {
  const config = { ...form.configuration }
  if (config.ssl) {
    config.sslMode
      = config.sslMode && config.sslMode !== 'DISABLED'
        ? config.sslMode
        : 'SERVER_AUTH'
  } else {
    config.sslMode = 'DISABLED'
    config.sslClientCertKey = ''
  }
  config.sslCaCertKey ||= ''
  config.sslClientCertKey ||= ''
  config.sslHostnameVerify ??= true
  return config
}

function parseDynamicMeta(item: ProductConfigDictItem) {
  try {
    return JSON.parse(item.remark || '{}') as {
      type?: string
      remark?: string
      help?: string
      url?: string
      hide?: string | boolean
      required?: string | boolean
      min?: string | number
      max?: string | number
      default?: string | number | boolean
      options?: Array<{ label?: string, dictLabel?: string, value?: string | number | boolean, dictValue?: string }>
    }
  } catch {
    return { remark: item.remark || '' }
  }
}

function toOptionalNumber(value?: string | number) {
  if (value === undefined || value === '') return undefined
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

function buildDynamicField(item: ProductConfigDictItem): DynamicMqttField {
  const meta = parseDynamicMeta(item)
  const remoteOptions = meta.url ? dynamicOptionDicts[meta.url] || [] : []
  const inlineOptions = Array.isArray(meta.options) ? meta.options : []
  const type = meta.type || 'string'
  const max = toOptionalNumber(meta.max)
  return {
    key: item.dictValue,
    label: item.dictLabel,
    type,
    help: meta.help || meta.remark || '',
    required: meta.required === true || meta.required === 'true',
    min: toOptionalNumber(meta.min),
    max: type === 'int' || type === 'number' ? max : undefined,
    maxLength: type === 'string' ? max : undefined,
    defaultValue: meta.default,
    options: remoteOptions.length
      ? remoteOptions.map((option) => ({ label: option.dictLabel, value: option.dictValue }))
      : inlineOptions.map((option) => ({
          label: option.label || option.dictLabel || String(option.value ?? option.dictValue ?? ''),
          value: option.value ?? option.dictValue ?? ''
        }))
  }
}

async function loadDynamicFields() {
  const { data } = await getProductConfigDicts(['mqtt_parser_type'])
  const dictItems = data.mqtt_parser_type || []
  const optionTypes = [...new Set(dictItems.map((item) => parseDynamicMeta(item).url).filter(Boolean))] as string[]
  if (optionTypes.length) Object.assign(dynamicOptionDicts, (await getProductConfigDicts(optionTypes)).data)
  dynamicFields.value = dictItems
    .filter((item) => {
      const meta = parseDynamicMeta(item)
      return !KNOWN_CONFIGURATION_KEYS.has(item.dictValue)
        && meta.hide !== true
        && meta.hide !== 'true'
    })
    .map(buildDynamicField)
}

function applyDynamicDefaults() {
  for (const field of dynamicFields.value) {
    if (form.configuration[field.key] === undefined && field.defaultValue !== undefined) {
      form.configuration[field.key] = field.defaultValue
    }
  }
}

function getDynamicText(key: string) {
  const value = form.configuration[key]
  return value === undefined || value === null ? '' : String(value)
}

function getDynamicNumber(key: string) {
  const value = form.configuration[key]
  if (value === undefined || value === null || value === '') return undefined
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

function getDynamicBoolean(key: string) {
  const value = form.configuration[key]
  return value === true || value === 'true'
}

function getDynamicSelect(key: string) {
  const value = form.configuration[key]
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? value : undefined
}

function setDynamicValue(key: string, value: unknown) {
  form.configuration[key] = value
}

function buildPayload(): Partial<MqttNetworkItem> {
  return {
    id: form.id,
    type: form.type,
    name: form.name,
    unionId: form.unionId,
    state: form.state,
    description: form.description,
    configuration: JSON.stringify(normalizeConfiguration())
  }
}

async function loadCertificates() {
  certificateLoading.value = true
  try {
    const { data } = await getCertificateList({ page: 1, size: 1000 })
    certificateOptions.value = data.records.map((item) => ({
      label: `${item.name || item.sslKey} (${item.sslKey})`,
      value: item.sslKey
    }))
  } finally {
    certificateLoading.value = false
  }
}

async function add() {
  resetForm()
  visible.value = true
  await Promise.all([loadCertificates(), loadDynamicFields()])
  applyDynamicDefaults()
}

async function edit(id: string) {
  resetForm()
  visible.value = true
  isEdit.value = true
  const [{ data }] = await Promise.all([getMqttNetwork(id), loadCertificates(), loadDynamicFields()])
  Object.assign(form, {
    id: String(data.id),
    type: data.type,
    name: data.name,
    unionId: data.unionId,
    state: Number(data.state) === 1 ? 1 : 0,
    description: data.description || '',
    configuration: parseMqttConfiguration(data)
  })
  applyDynamicDefaults()
}

async function validateForm() {
  if (await formRef.value?.validate()) return false
  if (!form.configuration.host?.trim()) {
    Message.warning('请输入 Broker 地址')
    return false
  }
  const missingField = dynamicFields.value.find((field) => {
    if (!field.required) return false
    const value = form.configuration[field.key]
    return value === undefined || value === null || value === ''
  })
  if (missingField) {
    Message.warning(`请配置${missingField.label}`)
    return false
  }
  return true
}

async function handleValidate() {
  if (!(await validateForm())) return
  validating.value = true
  try {
    await validateMqttNetwork(buildPayload())
    Message.success('配置验证通过')
  } finally {
    validating.value = false
  }
}

async function handleSave() {
  if (!(await validateForm())) return
  saving.value = true
  try {
    await (isEdit.value
      ? updateMqttNetwork(buildPayload())
      : addMqttNetwork(buildPayload()))
    Message.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('save-success')
  } finally {
    saving.value = false
  }
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.mqtt-form {
  :deep(.arco-divider-horizontal) {
    margin: 8px 0 14px;
  }

  :deep(.arco-form-item) {
    margin-bottom: 14px;
  }

  :deep(.arco-alert) {
    margin-bottom: 12px;
  }
}

:deep(.arco-input-number) {
  width: 100%;
}
</style>
