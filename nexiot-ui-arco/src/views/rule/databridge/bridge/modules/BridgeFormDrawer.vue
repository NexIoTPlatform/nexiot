<template>
  <a-drawer v-model:visible="visible" :title="form.id ? '编辑桥接规则' : '新增桥接规则'" width="min(820px, calc(100vw - 24px))" :mask-closable="false" :footer="false" unmount-on-close>
    <a-steps :current="step" small class="bridge-form__steps">
      <a-step title="数据来源" />
      <a-step title="转发目标" />
      <a-step title="处理配置" />
    </a-steps>
    <a-form ref="formRef" :model="form" layout="vertical">
      <section v-show="step === 1" class="bridge-form__section">
        <a-form-item field="name" label="规则名称" :rules="[{ required: true, message: '请输入规则名称' }]">
          <a-input v-model.trim="form.name" :max-length="100" show-word-limit />
        </a-form-item>
        <a-form-item field="sourceScope" label="数据范围" :rules="[{ required: true, message: '请选择数据范围' }]">
          <a-radio-group v-model="form.sourceScope" type="button">
            <a-radio value="ALL_PRODUCTS">全部产品</a-radio>
            <a-radio value="SPECIFIC_PRODUCTS">指定产品</a-radio>
            <a-radio value="APPLICATION">指定应用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="form.sourceScope === 'SPECIFIC_PRODUCTS'" label="产品" required>
          <a-select v-model="productKeys" :options="productOptions" multiple allow-search allow-clear :max-tag-count="3" placeholder="请选择一个或多个产品" />
        </a-form-item>
        <a-form-item v-if="form.sourceScope === 'APPLICATION'" label="应用" required>
          <a-select v-model="form.sourceApplicationId" :options="applicationOptions" allow-search allow-clear placeholder="请选择应用" />
        </a-form-item>
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <a-form-item label="状态"><a-switch v-model="enabled" checked-text="启用" unchecked-text="停用" /></a-form-item>
        </a-grid>
        <a-form-item label="描述"><a-textarea v-model="form.description" :auto-size="{ minRows: 3, maxRows: 5 }" :max-length="255" show-word-limit /></a-form-item>
      </section>
      <section v-show="step === 2" class="bridge-form__section">
        <a-alert>目标连接决定实际使用的桥接插件和可用能力。</a-alert>
        <a-form-item field="targetResourceId" label="目标连接" :rules="[{ required: true, message: '请选择目标连接' }]">
          <a-select v-model="form.targetResourceId" allow-search placeholder="请选择已启用的输出或双向连接" @change="handleResourceChange">
            <a-option v-for="item in resourceOptions" :key="item.id" :value="item.id!">
              <a-space><a-tag :color="getResourceTypeColor(item.type)">{{ item.type }}</a-tag><span>{{ item.name }}</span><small>{{ getResourceAddress(item) }}</small></a-space>
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="bridgeType" label="桥接类型" :rules="[{ required: true, message: '请选择桥接类型' }]">
          <a-select v-model="form.bridgeType" :options="bridgeTypeOptions" allow-search @change="applyTemplate" />
        </a-form-item>
        <a-form-item field="template" label="输出模板" :rules="[{ required: true, message: '请输入输出模板' }]">
          <a-textarea v-model="form.template" class="bridge-form__code" :auto-size="{ minRows: 10, maxRows: 18 }" />
          <template #extra>支持 `deviceKey`、`productKey`、`messageType`、`timestamp`、`properties` 等运行时变量；保留后端现有 `#{...}` 与 `{...}` 模板语法。</template>
        </a-form-item>
      </section>
      <section v-show="step === 3" class="bridge-form__section">
        <a-form-item label="MagicScript">
          <a-textarea v-model="form.magicScript" class="bridge-form__code" :auto-size="{ minRows: 10, maxRows: 18 }" placeholder="function iotToYour(data, config) { return data; }" />
          <template #extra>可实现 `iotToYour(data, config)` 与 `yourToIot(data, config)`。</template>
        </a-form-item>
        <a-form-item label="统一 JSON 配置">
          <a-textarea v-model="form.config" class="bridge-form__code" :auto-size="{ minRows: 8, maxRows: 16 }" placeholder="{}" />
          <template #extra>用于字段映射、过滤、批处理、重试和请求头等插件配置。</template>
        </a-form-item>
      </section>
    </a-form>
    <div class="bridge-form__footer">
      <a-button @click="visible = false">取消</a-button>
      <a-space>
        <a-button v-if="step > 1" @click="step--"><template #icon><icon-left /></template>上一步</a-button>
        <a-button v-if="step < 3" type="primary" @click="next"><template #icon><icon-right /></template>下一步</a-button>
        <a-button v-else type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存</a-button>
      </a-space>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { FormInstance } from '@arco-design/web-vue'
import type { DataBridgeConfig, ResourceConnection } from '../../api'
import type { ApplicationItem } from '@/apis/application/application'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { addDataBridgeConfig, getDataBridgeApplications, getDataBridgeConfig, getDataBridgeProducts, getResourceConnections, updateDataBridgeConfig } from '../../api'
import { getResourceAddress, getResourceTypeColor, parseStringArray } from '../../shared'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const visible = ref(false)
const step = ref(1)
const saving = ref(false)
const formRef = useTemplateRef<FormInstance>('formRef')
const resources = ref<ResourceConnection[]>([])
const products = ref<ProductItem[]>([])
const applications = ref<ApplicationItem[]>([])
const productKeys = ref<string[]>([])
const form = reactive<DataBridgeConfig>(createDefault())
const templates: Record<string, string> = {
  JDBC: 'INSERT INTO device_data (device_key, message_type, timestamp, properties) VALUES (\'{deviceKey}\', \'{messageType}\', \'{timestamp}\', \'{properties}\')',
  KAFKA: '{\n  "deviceKey": "{deviceKey}",\n  "messageType": "{messageType}",\n  "timestamp": "{timestamp}",\n  "properties": {properties}\n}',
  MQTT: '{properties}',
  HTTP: '{\n  "deviceKey": "{deviceKey}",\n  "data": {properties}\n}',
  IOTDB: 'INSERT INTO root.device_data (device_key, message_type, timestamp, properties) VALUES (\'{deviceKey}\', \'{messageType}\', {timestamp}, \'{properties}\')',
  INFLUXDB: 'device_data,device_key={deviceKey},message_type={messageType} properties="{properties}" {timestamp}'
}
function createDefault(): DataBridgeConfig { return { name: '', sourceScope: 'ALL_PRODUCTS', sourceProductKeys: [], bridgeType: '', template: '', magicScript: '', config: '', status: 1, description: '' } }
const enabled = computed({ get: () => form.status === 1, set: (value) => { form.status = value ? 1 : 0 } })
const resourceOptions = computed(() => resources.value.filter((item) => item.status === 1 && item.dataDirection !== 'INPUT'))
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const applicationOptions = computed(() => applications.value.map((item) => ({ label: `${item.appName} (${item.appUniqueId})`, value: item.appUniqueId })))
const bridgeTypeOptions = computed(() => Array.from(new Set(resources.value.map((item) => item.pluginType || item.type).filter(Boolean)), (value) => ({ label: value, value })))
function handleResourceChange(value: unknown) {
  const resource = resources.value.find((item) => item.id === String(value))
  if (!resource) return
  form.bridgeType = resource.pluginType || resource.type
  applyTemplate()
}
function applyTemplate() { if (!form.template) form.template = templates[form.bridgeType] || '' }
function validateStep() {
  if (step.value === 1 && form.sourceScope === 'SPECIFIC_PRODUCTS' && !productKeys.value.length) { Message.warning('请选择至少一个产品'); return false }
  if (step.value === 1 && form.sourceScope === 'APPLICATION' && !form.sourceApplicationId) { Message.warning('请选择应用'); return false }
  return true
}
async function next() {
  if (!validateStep()) return
  if (step.value === 1 && !form.name.trim()) { Message.warning('请输入规则名称'); return }
  if (step.value === 2 && (!form.targetResourceId || !form.bridgeType || !form.template.trim())) { Message.warning('请完整填写目标连接、桥接类型和输出模板'); return }
  step.value++
}
function normalizeJson(value?: string) {
  if (!value?.trim()) return ''
  try { return JSON.stringify(JSON.parse(value)) } catch { Message.error('统一 JSON 配置格式错误'); return null }
}
async function save() {
  const config = normalizeJson(form.config)
  if (config === null) return
  const payload: DataBridgeConfig = { ...form, config, sourceProductKeys: JSON.stringify(form.sourceScope === 'SPECIFIC_PRODUCTS' ? productKeys.value : []) }
  if (payload.sourceScope !== 'APPLICATION') payload.sourceApplicationId = undefined
  saving.value = true
  try {
    if (payload.id) await updateDataBridgeConfig(payload.id, payload)
    else await addDataBridgeConfig(payload)
    Message.success('桥接规则保存成功')
    visible.value = false
    emit('save-success')
  } finally { saving.value = false }
}
async function loadDependencies() {
  const [resourceRes, productRes, applicationRes] = await Promise.all([getResourceConnections(), getDataBridgeProducts(), getDataBridgeApplications()])
  resources.value = resourceRes.data || []
  products.value = productRes.data.records || []
  applications.value = applicationRes.data.records || []
}
async function open(record?: DataBridgeConfig) {
  Object.assign(form, createDefault())
  productKeys.value = []
  step.value = 1
  visible.value = true
  await loadDependencies()
  if (record?.id) {
    const detail = (await getDataBridgeConfig(record.id)).data
    Object.assign(form, detail)
    productKeys.value = parseStringArray(detail.sourceProductKeys)
  }
}
defineExpose({ open })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.bridge-form__steps { margin-bottom: 20px; }
.bridge-form__section { display: grid; gap: 14px; }
.bridge-form__code { font-family: Monaco, Menlo, Consolas, monospace; }
.bridge-form__footer { position: sticky; bottom: 0; display: flex; justify-content: space-between; padding: 14px 20px; margin: 24px -20px -20px; background: var(--color-bg-2); border-top: 1px solid var(--color-border-2); }
small { color: var(--color-text-3); }
</style>
