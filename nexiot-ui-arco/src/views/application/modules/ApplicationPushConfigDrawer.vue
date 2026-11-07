<template>
  <a-modal
    v-model:visible="visible"
    title="配置消息推送"
    width="min(760px, calc(100vw - 24px))"
    :mask-closable="false"
    :body-style="modalBodyStyle"
    align-center
    unmount-on-close
  >
    <a-steps :current="step" small class="push-wizard__steps">
      <a-step title="选择方式" />
      <a-step title="连接目标" />
      <a-step title="消息格式" />
    </a-steps>

    <div v-show="step === 1" class="push-wizard__body">
      <a-radio-group v-model="selectedType" type="button" class="push-wizard__types">
        <a-radio v-for="channel in channels" :key="channel.key" :value="channel.key">
          <component :is="channel.icon" /> {{ channel.name }}
        </a-radio>
      </a-radio-group>
    </div>

    <div v-show="step === 2" class="push-wizard__body">
      <a-form :model="draft" layout="vertical">
        <template v-if="selectedType === 'http'">
          <a-form-item label="推送地址" required><a-input v-model.trim="draft.url" placeholder="https://example.com/callback" /></a-form-item>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
            <a-form-item label="密钥请求头" required><a-input v-model.trim="draft.header" /></a-form-item>
            <a-form-item label="推送密钥" required><a-input-password v-model="draft.secret" :placeholder="secretPlaceholder('secret')" /></a-form-item>
          </a-grid>
        </template>

        <template v-if="selectedType === 'externalMqtt'">
          <a-form-item label="Broker 地址" required><a-input v-model.trim="draft.brokerUrl" placeholder="tcp://broker.example.com:1883" /></a-form-item>
          <a-form-item label="Topic" required><a-input v-model.trim="draft.topic" placeholder="devices/${productKey}/${deviceId}" /></a-form-item>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
            <a-form-item label="Client ID"><a-input v-model.trim="draft.clientId" /></a-form-item>
            <a-form-item label="用户名"><a-input v-model.trim="draft.username" /></a-form-item>
            <a-form-item label="密码"><a-input-password v-model="draft.password" :placeholder="secretPlaceholder('password')" /></a-form-item>
          </a-grid>
          <a-collapse :bordered="false" class="push-wizard__advanced">
            <a-collapse-item key="mqtt" header="高级设置">
              <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
                <a-form-item label="QoS"><a-select v-model="draft.qos" :options="qosOptions" /></a-form-item>
                <a-form-item label="Keep Alive（秒）"><a-input-number v-model="draft.keepAliveSeconds" :min="5" :max="3600" /></a-form-item>
                <a-form-item label="集群 Client ID"><a-switch v-model="draft.clusterClientId" /></a-form-item>
                <a-form-item label="Retained"><a-switch v-model="draft.retained" /></a-form-item>
              </a-grid>
            </a-collapse-item>
          </a-collapse>
          <a-button :loading="testing" @click="testConnection"><template #icon><icon-link /></template>测试连接</a-button>
        </template>

        <template v-if="selectedType === 'tcp'">
          <a-grid :cols="{ xs: 1, sm: 3 }" :col-gap="16">
            <a-form-item label="服务器地址" required :span="2"><a-input v-model.trim="draft.host" placeholder="192.168.1.20" /></a-form-item>
            <a-form-item label="端口" required><a-input-number v-model="draft.port" :min="1" :max="65535" /></a-form-item>
          </a-grid>
          <a-collapse :bordered="false" class="push-wizard__advanced">
            <a-collapse-item key="tcp" header="高级设置">
              <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
                <a-form-item label="连接方式"><a-select v-model="draft.connectionMode" :options="connectionModeOptions" /></a-form-item>
                <a-form-item label="报文边界"><a-select v-model="draft.frameMode" :options="frameModeOptions" /></a-form-item>
                <a-form-item v-if="draft.frameMode !== 'RAW'" label="分隔符" required>
                  <a-select
                    v-model="draft.delimiter"
                    :options="delimiterOptions"
                    allow-create
                    allow-search
                    placeholder="选择或输入分隔符"
                  />
                </a-form-item>
                <a-form-item label="TLS"><a-switch v-model="draft.tls" /></a-form-item>
              </a-grid>
            </a-collapse-item>
          </a-collapse>
          <a-button :loading="testing" @click="testConnection"><template #icon><icon-link /></template>测试连接</a-button>
        </template>

        <template v-if="selectedType === 'mqtt'">
          <a-descriptions :column="1" bordered>
            <a-descriptions-item label="连接方式">平台 MQTT</a-descriptions-item>
            <a-descriptions-item label="Client ID">{{ application?.appId || '-' }}</a-descriptions-item>
          </a-descriptions>
        </template>

        <template v-if="selectedType === 'kafka'">
          <a-form-item label="Bootstrap Servers" required><a-input v-model.trim="draft.bootstrapServers" placeholder="kafka-1:9092,kafka-2:9092" /></a-form-item>
          <a-form-item label="Topic" required><a-input v-model.trim="draft.topic" /></a-form-item>
          <a-collapse :bordered="false" class="push-wizard__advanced">
            <a-collapse-item key="kafkaSecurity" header="安全鉴权（高级）">
              <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
                <a-form-item label="安全协议"><a-select v-model="draft.securityProtocol" :options="kafkaSecurityProtocolOptions" /></a-form-item>
                <a-form-item v-if="kafkaUsesSasl" label="SASL 机制"><a-select v-model="draft.saslMechanism" :options="kafkaSaslMechanismOptions" /></a-form-item>
                <a-form-item v-if="kafkaUsesSasl" label="用户名" required><a-input v-model.trim="draft.username" /></a-form-item>
                <a-form-item v-if="kafkaUsesSasl" label="密码" required><a-input-password v-model="draft.password" :placeholder="secretPlaceholder('password')" /></a-form-item>
              </a-grid>
            </a-collapse-item>
          </a-collapse>
          <a-button :loading="testing" @click="testConnection"><template #icon><icon-link /></template>测试连接</a-button>
        </template>

        <template v-if="selectedType === 'rocketMQ'">
          <a-form-item label="NameServer" required><a-input v-model.trim="draft.namesrvAddr" placeholder="rocketmq.example.com:9876" /></a-form-item>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
            <a-form-item label="Topic" required><a-input v-model.trim="draft.topic" /></a-form-item>
            <a-form-item label="Producer Group" required><a-input v-model.trim="draft.group" /></a-form-item>
          </a-grid>
          <a-collapse :bordered="false" class="push-wizard__advanced">
            <a-collapse-item key="rocketSecurity" header="安全鉴权（高级）">
              <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
                <a-form-item label="AccessKey"><a-input v-model.trim="draft.accessKey" /></a-form-item>
                <a-form-item label="SecretKey"><a-input-password v-model="draft.secretKey" :placeholder="secretPlaceholder('secretKey')" /></a-form-item>
                <a-form-item label="SecurityToken"><a-input-password v-model="draft.securityToken" :placeholder="secretPlaceholder('securityToken')" /></a-form-item>
                <a-form-item label="Namespace"><a-input v-model.trim="draft.namespace" /></a-form-item>
                <a-form-item label="TLS"><a-switch v-model="draft.useTLS" /></a-form-item>
              </a-grid>
            </a-collapse-item>
          </a-collapse>
          <a-button :loading="testing" @click="testConnection"><template #icon><icon-link /></template>测试连接</a-button>
        </template>
      </a-form>
    </div>

    <div v-show="step === 3" class="push-wizard__body">
      <a-form :model="transform" layout="vertical">
        <a-form-item label="消息格式">
          <a-radio-group v-model="formatMode" type="button" class="push-wizard__formats">
            <a-radio value="ORIGINAL">平台原始格式</a-radio>
            <a-radio value="TEMPLATE">JSON 模板</a-radio>
            <a-radio value="SCRIPT">脚本转换</a-radio>
            <a-radio v-if="formatMode === 'PRESET'" value="PRESET">内置格式（历史）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="formatMode === 'PRESET'" label="内置格式" required>
          <a-select v-model="transform.presetCode" :options="presetOptions" />
        </a-form-item>
        <a-form-item v-if="formatMode === 'TEMPLATE' || formatMode === 'SCRIPT'" :label="formatMode === 'SCRIPT' ? '脚本内容' : 'JSON 模板'" required>
          <a-textarea v-model="transform.content" class="push-wizard__editor" :auto-size="{ minRows: 9, maxRows: 14 }" />
        </a-form-item>
        <template v-if="formatMode !== 'ORIGINAL'">
          <a-form-item label="输入样例"><a-textarea v-model="transform.sampleInput" class="push-wizard__editor" :auto-size="{ minRows: 6, maxRows: 10 }" /></a-form-item>
          <a-button :loading="previewing" @click="previewTransform"><template #icon><icon-play-arrow /></template>预览转换</a-button>
          <a-form-item v-if="previewOutput" label="转换结果" class="push-wizard__preview"><a-textarea :model-value="previewOutput" readonly class="push-wizard__editor" :auto-size="{ minRows: 6, maxRows: 10 }" /></a-form-item>
        </template>
        <a-divider />
        <a-form-item label="保存后启用"><a-switch v-model="enableAfterSave" /></a-form-item>
      </a-form>
    </div>

    <template #footer>
      <div class="push-wizard__footer">
        <a-button v-if="step > 1" @click="step--">上一步</a-button><span />
        <a-button v-if="step < 3" type="primary" @click="nextStep">下一步</a-button>
        <a-button v-else type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import type { ApplicationItem, PushConfig, TransformRule } from '@/apis/application/application'
import { Message } from '@arco-design/web-vue'
import { IconCloud, IconCloudDownload, IconSend, IconSwap, IconThunderbolt, IconUpload } from '@arco-design/web-vue/es/icon'
import { deepClone } from '@/utils'
import {
  getApplicationTransform,
  previewApplicationTransform,
  saveApplicationTransform,
  testPushConnection,
  updateApplicationPush,
  updateApplicationTransform,
  updatePushEnabled
} from '../api'

defineOptions({ name: 'ApplicationPushConfigDrawer' })
const emit = defineEmits<{ (e: 'save-success', config: Record<string, PushConfig>): void }>()
type PushKey = 'http' | 'externalMqtt' | 'tcp' | 'mqtt' | 'kafka' | 'rocketMQ'
const visible = ref(false)
const step = ref(1)
const selectedType = ref<PushKey>('http')
const application = ref<ApplicationItem>()
const original = ref<Record<string, PushConfig>>({})
const draft = reactive<Record<string, any>>({})
const saving = ref(false)
const testing = ref(false)
const previewing = ref(false)
const enableAfterSave = ref(true)
const formatMode = ref('ORIGINAL')
const loadingTransform = ref(false)
const presets = ref<{ code: string, name: string, implemented?: boolean }[]>([])
const previewOutput = ref('')
const transform = reactive<TransformRule>({ ruleName: '应用消息格式', scopeType: 'GLOBAL', ruleType: 'TEMPLATE', content: '', presetCode: '', options: '{}', sampleInput: '' })
const modalBodyStyle = { maxHeight: 'min(76vh, 760px)', overflowY: 'auto' as const }
const DEFAULT_SAMPLE = JSON.stringify({ messageType: 'PROPERTIES', productKey: 'demoProduct', deviceId: 'device01', properties: { temperature: 23.5, humidity: 56 }, time: 1710000000000 }, null, 2)
const DEFAULT_TEMPLATE = `{
  "deviceId": "\${deviceId}",
  "productKey": "\${productKey}",
  "messageType": "\${messageType}",
  "timestamp": "\${time}",
  "payload": \${properties}
}`
const DEFAULT_SCRIPT = `var transform = (ctx, source, request, requestMap) => {
  return {
    deviceId: ctx.deviceId,
    productKey: ctx.productKey,
    messageType: ctx.messageType,
    timestamp: ctx.time,
    body: ctx.properties || ctx.data || {}
  };
};
return transform(ctx, source, request, requestMap);`
const formatContents = reactive<Record<string, string>>({ TEMPLATE: DEFAULT_TEMPLATE, SCRIPT: DEFAULT_SCRIPT })
const channels: { key: PushKey, name: string, icon: Component }[] = [
  { key: 'http', name: 'HTTP', icon: IconSend },
  { key: 'externalMqtt', name: '外部 MQTT', icon: IconUpload },
  { key: 'tcp', name: 'TCP', icon: IconSwap },
  { key: 'mqtt', name: '平台 MQTT', icon: IconCloud },
  { key: 'kafka', name: 'Kafka', icon: IconCloudDownload },
  { key: 'rocketMQ', name: 'RocketMQ', icon: IconThunderbolt }
]
const qosOptions = [0, 1, 2].map((value) => ({ label: `QoS ${value}`, value }))
const connectionModeOptions = [{ label: '长连接', value: 'PERSISTENT' }, { label: '短连接', value: 'SHORT' }]
const frameModeOptions = [{ label: '追加分隔符', value: 'LINE' }, { label: '原始字节', value: 'RAW' }]
const delimiterOptions = [
  { label: 'LF (\\n)', value: '\\n' },
  { label: 'CRLF (\\r\\n)', value: '\\r\\n' },
  { label: 'CR (\\r)', value: '\\r' },
  { label: 'NUL (\\0)', value: '\\0' },
  { label: 'TAB (\\t)', value: '\\t' }
]
const kafkaSecurityProtocolOptions = ['PLAINTEXT', 'SASL_PLAINTEXT', 'SASL_SSL', 'SSL'].map((value) => ({ label: value, value }))
const kafkaSaslMechanismOptions = ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'].map((value) => ({ label: value, value }))
const presetOptions = computed(() => presets.value.filter((item) => item.implemented !== false).map((item) => ({ label: item.name, value: item.code })))
const kafkaUsesSasl = computed(() => String(draft.securityProtocol || '').startsWith('SASL_'))

watch(selectedType, (value) => {
  resetDraft()
  enableAfterSave.value = Boolean(original.value[value]?.enable) || !isConfigured(value)
})
watch(formatMode, (value, previous) => {
  if (loadingTransform.value) return
  if (previous === 'TEMPLATE' || previous === 'SCRIPT') formatContents[previous] = transform.content || ''
  if (value === 'TEMPLATE' || value === 'SCRIPT') transform.content = formatContents[value]
  if (value === 'PRESET' && !transform.presetCode) transform.presetCode = presetOptions.value[0]?.value || ''
}, { flush: 'sync' })

function defaults(type: PushKey): Record<string, unknown> {
  const values: Record<PushKey, Record<string, unknown>> = {
    http: { url: '', header: 'X-Nexiot-Secret', secret: '' },
    externalMqtt: { brokerUrl: '', clientId: '', username: '', password: '', topic: `devices/\${productKey}/\${deviceId}`, qos: 1, retained: false, keepAliveSeconds: 60, connectTimeoutSeconds: 10, publishTimeoutSeconds: 10, clusterClientId: true },
    tcp: { host: '', port: 9000, tls: false, connectionMode: 'PERSISTENT', frameMode: 'LINE', delimiter: '\\n', charset: 'UTF-8', connectTimeoutMillis: 5000, writeTimeoutMillis: 3000 },
    mqtt: {},
    kafka: { bootstrapServers: '', topic: '', username: '', password: '', securityProtocol: 'PLAINTEXT', saslMechanism: 'PLAIN' },
    rocketMQ: { namesrvAddr: '', topic: '', group: '', accessKey: '', secretKey: '', securityToken: '', namespace: '', useTLS: false }
  }
  return values[type]
}

function resetDraft() {
  Object.keys(draft).forEach((key) => delete draft[key])
  const source = original.value[selectedType.value] || {}
  Object.assign(draft, defaults(selectedType.value), source)
  if (selectedType.value === 'tcp') {
    draft.delimiter = normalizeTcpDelimiter(draft.delimiter)
  }
  if (selectedType.value === 'kafka' && !source.securityProtocol && (source.username || source.password)) {
    draft.securityProtocol = 'SASL_PLAINTEXT'
  }
  for (const field of ['secret', 'password', 'secretKey', 'securityToken']) {
    if (field in draft) draft[field] = ''
  }
}

async function open(app: ApplicationItem, current: Record<string, PushConfig>, initialType: PushKey = 'http') {
  application.value = app
  original.value = deepClone(current)
  selectedType.value = initialType
  step.value = 1
  previewOutput.value = ''
  enableAfterSave.value = Boolean(current[initialType]?.enable) || !isConfigured(initialType)
  resetDraft()
  visible.value = true
  await loadTransform()
}

function isConfigured(type: PushKey) {
  const config = original.value[type] || {}
  if (type === 'http') return Boolean(config.url && config.secret)
  if (type === 'externalMqtt') return Boolean(config.brokerUrl && config.topic)
  if (type === 'tcp') return Boolean(config.host && config.port)
  if (type === 'mqtt') return Boolean(config.url || application.value?.mqttEnable !== undefined)
  if (type === 'kafka') return Boolean(config.bootstrapServers && config.topic)
  return Boolean(config.namesrvAddr && config.topic && config.group)
}

async function loadTransform() {
  if (!application.value) return
  loadingTransform.value = true
  formatContents.TEMPLATE = DEFAULT_TEMPLATE
  formatContents.SCRIPT = DEFAULT_SCRIPT
  try {
    const overview = (await getApplicationTransform(application.value.appUniqueId)).data || {}
    const rule = overview.rule || overview.rules?.[0]
    presets.value = overview.presets || []
    Object.assign(transform, { id: undefined, ruleName: '应用消息格式', scopeType: 'GLOBAL', ruleType: 'TEMPLATE', content: '', presetCode: '', options: '{}', sampleInput: DEFAULT_SAMPLE })
    if (rule) {
      Object.assign(transform, rule, { scopeType: 'GLOBAL', sampleInput: rule.sampleInput || DEFAULT_SAMPLE })
      if (rule.ruleType === 'TEMPLATE' || rule.ruleType === 'SCRIPT') {
        formatContents[rule.ruleType] = rule.content || formatContents[rule.ruleType]
      }
    }
    formatMode.value = overview.config?.enable && rule ? rule.ruleType || 'ORIGINAL' : 'ORIGINAL'
  } finally {
    loadingTransform.value = false
  }
}

function secretPlaceholder(field: string) {
  return original.value[selectedType.value]?.[field] ? '留空保留原值' : '请输入'
}

function mergedDraft(): Record<string, any> {
  const source = original.value[selectedType.value] || {}
  const merged: Record<string, any> = { ...source, ...deepClone(draft), support: true, enable: false }
  for (const field of ['secret', 'password', 'secretKey', 'securityToken']) {
    if (!draft[field] && source[field]) merged[field] = source[field]
    else if (!draft[field]) delete merged[field]
  }
  if (selectedType.value === 'kafka' && !String(merged.securityProtocol || '').startsWith('SASL_')) {
    delete merged.username
    delete merged.password
    delete merged.saslMechanism
  }
  return merged
}

function normalizeTcpDelimiter(delimiter: unknown) {
  if (typeof delimiter !== 'string') return '\\n'
  return delimiter
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/\0/g, '\\0')
    .replace(/\t/g, '\\t')
}

function validateTcpDelimiter(delimiter: unknown) {
  if (typeof delimiter !== 'string' || delimiter.length === 0) return '请选择或输入 TCP 分隔符'
  let characterCount = 0
  for (let index = 0; index < delimiter.length;) {
    if (delimiter[index] === '\\') {
      if (index + 1 >= delimiter.length) return 'TCP 分隔符不能以反斜杠结尾'
      if (!['r', 'n', '0', 't'].includes(delimiter[index + 1])) return `TCP 分隔符不支持转义 \\${delimiter[index + 1]}`
      index += 2
    } else {
      const codePoint = delimiter.codePointAt(index)!
      const character = String.fromCodePoint(codePoint)
      if (codePoint <= 31 || codePoint === 127) return 'TCP 分隔符包含不支持的控制字符'
      index += character.length
    }
    if (++characterCount > 16) return 'TCP 分隔符解析后不能超过 16 个字符'
  }
  return ''
}

function validateConnection() {
  const c = mergedDraft()
  if (selectedType.value === 'http' && (!c.url || !/^https?:\/\//i.test(c.url) || !c.header || !c.secret)) return '请完整填写 HTTP 地址、请求头和密钥'
  if (selectedType.value === 'externalMqtt' && (!/^(?:tcp|ssl|ws|wss):\/\//i.test(c.brokerUrl || '') || !c.topic)) return '请填写正确的 Broker 地址和 Topic'
  if (selectedType.value === 'tcp') {
    if (!c.host || !c.port) return '请填写 TCP 服务器地址和端口'
    if (c.frameMode !== 'RAW') {
      const delimiterError = validateTcpDelimiter(c.delimiter)
      if (delimiterError) return delimiterError
    }
  }
  if (selectedType.value === 'kafka') {
    if (!c.bootstrapServers || !c.topic) return '请填写 Kafka 服务器和 Topic'
    if (String(c.securityProtocol || '').startsWith('SASL_') && (!c.username || !c.password)) return '请完整填写 Kafka SASL 用户名和密码'
  }
  if (selectedType.value === 'rocketMQ') {
    if (!c.namesrvAddr || !c.topic || !c.group) return '请填写 NameServer、Topic 和 Producer Group'
    if (Boolean(c.accessKey) !== Boolean(c.secretKey)) return 'RocketMQ AccessKey 和 SecretKey 必须同时填写'
    if (c.securityToken && (!c.accessKey || !c.secretKey)) return 'SecurityToken 必须配合 AccessKey 和 SecretKey 使用'
  }
  return ''
}

function nextStep() {
  if (step.value === 2) {
    const message = validateConnection()
    if (message) return Message.warning(message)
  }
  step.value++
}

async function testConnection() {
  const message = validateConnection()
  if (message || !application.value) return Message.warning(message)
  testing.value = true
  try {
    await testPushConnection(application.value.appUniqueId, selectedType.value, mergedDraft())
    Message.success('连接成功')
  } finally {
    testing.value = false
  }
}

async function previewTransform() {
  if (!application.value) return
  if (formatMode.value === 'PRESET' && !transform.presetCode) return Message.warning('请选择内置格式')
  if (['TEMPLATE', 'SCRIPT'].includes(formatMode.value) && !transform.content) return Message.warning('请输入转换内容')
  previewing.value = true
  try {
    const result = await previewApplicationTransform({ appUniqueId: application.value.appUniqueId, ruleType: formatMode.value, presetCode: formatMode.value === 'PRESET' ? transform.presetCode : '', content: formatMode.value === 'PRESET' ? '' : transform.content, options: transform.options || '{}', sampleInput: transform.sampleInput || DEFAULT_SAMPLE })
    previewOutput.value = result.data.outputJson || ''
    Message.success('预览完成')
  } finally {
    previewing.value = false
  }
}

async function saveTransform() {
  if (!application.value) return
  if (formatMode.value === 'ORIGINAL') {
    await updateApplicationTransform(application.value.appUniqueId, { enable: false, support: true, fallbackMode: 'ORIGINAL' })
    return
  }
  await saveApplicationTransform(application.value.appUniqueId, { ...transform, appUniqueId: application.value.appUniqueId, ruleName: '应用消息格式', ruleType: formatMode.value, presetCode: formatMode.value === 'PRESET' ? transform.presetCode : '', content: formatMode.value === 'PRESET' ? '' : transform.content, sampleOutput: previewOutput.value })
}

async function save() {
  const message = validateConnection()
  if (message || !application.value) return Message.warning(message)
  saving.value = true
  try {
    let nextConfig = deepClone(original.value)
    if (selectedType.value !== 'mqtt') {
      nextConfig = { ...nextConfig, [selectedType.value]: mergedDraft() }
      delete nextConfig.rocketmq
      await updateApplicationPush({ appUniqueId: application.value.appUniqueId, cfg: JSON.stringify(nextConfig), notifyUrl: selectedType.value === 'http' ? nextConfig.http?.url : application.value.notifyUrl })
    }
    await saveTransform()
    await updatePushEnabled(application.value.appUniqueId, selectedType.value === 'rocketMQ' ? 'rocketmq' : selectedType.value, enableAfterSave.value)
    visible.value = false
    emit('save-success', nextConfig)
    Message.success('推送配置已保存')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.push-wizard__steps {
  margin-bottom: 24px;
}

.push-wizard__body {
  min-height: 390px;
}

.push-wizard__types {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.push-wizard__types :deep(.arco-radio-button) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
}

.push-wizard__advanced {
  margin-bottom: 16px;
}

.push-wizard__formats {
  display: flex;
  flex-wrap: wrap;
}

.push-wizard__editor {
  font-family: SFMono-Regular, Consolas, monospace;
}

.push-wizard__preview {
  margin-top: 16px;
}

.push-wizard__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

:deep(.arco-input-number) {
  width: 100%;
}

@media (width <= 640px) {
  .push-wizard__types {
    grid-template-columns: 1fr;
  }
}
</style>
