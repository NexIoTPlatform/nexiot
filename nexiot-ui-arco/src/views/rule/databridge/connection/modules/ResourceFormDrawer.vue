<template>
  <a-drawer
    v-model:visible="visible"
    :title="form.id ? '编辑连接' : '新增连接'"
    width="min(780px, calc(100vw - 24px))"
    :mask-closable="false"
    :footer="false"
    unmount-on-close
  >
    <a-steps :current="step" small class="resource-form__steps">
      <a-step title="基本信息" />
      <a-step title="连接配置" />
      <a-step title="高级设置" />
    </a-steps>

    <a-form ref="formRef" :model="form" layout="vertical">
      <section v-show="step === 1" class="resource-form__section">
        <a-alert>先选择数据方向，系统会只显示当前方向可用的资源与插件。</a-alert>
        <a-form-item field="name" label="连接名称" :rules="[{ required: true, message: '请输入连接名称' }]">
          <a-input v-model.trim="form.name" :max-length="100" placeholder="例如：生产 PostgreSQL" show-word-limit />
        </a-form-item>
        <a-form-item field="dataDirection" label="数据方向" :rules="[{ required: true, message: '请选择数据方向' }]">
          <a-radio-group v-model="form.dataDirection" type="button" @change="handleDirectionChange">
            <a-radio v-for="item in dataDirectionOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-radio>
          </a-radio-group>
          <template #extra>{{ currentDirectionDescription }}</template>
        </a-form-item>
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <a-form-item field="type" label="资源类型" :rules="[{ required: true, message: '请选择资源类型' }]">
            <a-select v-model="form.type" allow-search placeholder="请选择" @change="handleTypeChange">
              <a-optgroup v-for="group in groupedTypeOptions" :key="group.label" :label="group.label">
                <a-option v-for="item in group.options" :key="item.value" :value="item.value">
                  <a-tag :color="getResourceTypeColor(item.value)" size="small">{{ item.label }}</a-tag>
                </a-option>
              </a-optgroup>
            </a-select>
          </a-form-item>
          <a-form-item field="pluginType" label="插件类型" :rules="[{ required: true, message: '请选择插件类型' }]">
            <a-select v-model="form.pluginType" :loading="pluginLoading" placeholder="请选择" @change="loadFields">
              <a-option v-for="item in pluginOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-switch v-model="enabled" checked-text="启用" unchecked-text="停用" />
          </a-form-item>
        </a-grid>
        <a-form-item label="描述">
          <a-textarea v-model="form.description" :max-length="255" :auto-size="{ minRows: 3, maxRows: 5 }" show-word-limit />
        </a-form-item>
      </section>

      <section v-show="step === 2" class="resource-form__section">
        <a-skeleton v-if="fieldLoading" animation :loading="true" />
        <a-empty v-else-if="!coreFields.length" description="当前类型未配置核心连接字段">
          <template #extra><a-button @click="step = 3">前往高级设置</a-button></template>
        </a-empty>
        <a-grid v-else :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <DynamicFieldInput
            v-for="field in coreFields"
            :key="field.field"
            v-model="dynamicConfig[field.field]"
            :field="field"
          />
        </a-grid>
        <a-alert v-if="testMessage" :type="testPassed ? 'success' : 'error'" closable @close="testMessage = ''">
          {{ testMessage }}
        </a-alert>
        <div class="resource-form__test">
          <a-button type="primary" :loading="testing" @click="testConnection">
            <template #icon><icon-thunderbolt /></template>测试连接
          </a-button>
          <span>连接成功后才能进入高级设置。</span>
        </div>
      </section>

      <section v-show="step === 3" class="resource-form__section">
        <a-grid v-if="advancedFields.length" :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <DynamicFieldInput
            v-for="field in advancedFields"
            :key="field.field"
            v-model="dynamicConfig[field.field]"
            :field="field"
          />
        </a-grid>
        <a-empty v-else description="当前连接没有额外配置项" />
        <a-form-item label="补充 JSON 配置" extra="字段会与上方结构化配置合并；重名字段以上方配置为准。">
          <a-textarea v-model="extraJson" class="resource-form__code" :auto-size="{ minRows: 7, maxRows: 12 }" placeholder="{}" />
        </a-form-item>
      </section>
    </a-form>

    <div class="resource-form__footer">
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
import type { DataDirection, DynamicField, PluginOption, ResourceConnection, ResourceTypeGroups } from '../../api'
import { Message } from '@arco-design/web-vue'
import { addResourceConnection, getPluginOptions, getResourceConnection, getResourceFields, getResourceTypes, testResourceConnection, updateResourceConnection } from '../../api'
import { dataDirectionOptions, directionToLegacy, getResourceTypeColor, parseJsonObject } from '../../shared'
import DynamicFieldInput from './DynamicFieldInput.vue'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const visible = ref(false)
const step = ref(1)
const saving = ref(false)
const testing = ref(false)
const fieldLoading = ref(false)
const pluginLoading = ref(false)
const testPassed = ref(false)
const testMessage = ref('')
const formRef = useTemplateRef<FormInstance>('formRef')
const typeGroups = ref<ResourceTypeGroups>()
const pluginOptions = ref<PluginOption[]>([])
const fields = ref<DynamicField[]>([])
const dynamicConfig = reactive<Record<string, any>>({})
const extraJson = ref('')
const form = reactive<ResourceConnection>(createDefault())
const CORE_FIELDS = new Set(['host', 'port', 'username', 'password', 'database', 'databaseName', 'region', 'regionId', 'accessKeyId', 'accessKeySecret', 'secretId', 'secretKey', 'instanceId', 'projectId', 'appId', 'appSecret', 'baseUrl'])

function createDefault(): ResourceConnection {
  return { name: '', type: '', pluginType: '', status: 1, direction: 'OUT', dataDirection: 'OUTPUT', description: '' }
}

const enabled = computed({ get: () => form.status === 1, set: (value) => { form.status = value ? 1 : 0 } })
const coreFields = computed(() => fields.value.filter((item) => item.config.required && CORE_FIELDS.has(item.field)))
const advancedFields = computed(() => fields.value.filter((item) => !coreFields.value.includes(item)))
const currentDirectionDescription = computed(() => dataDirectionOptions.find((item) => item.value === form.dataDirection)?.description || '')
const groupedTypeOptions = computed(() => {
  const groups = typeGroups.value
  if (!groups) return []
  return [
    ['数据库', groups.databases],
    ['消息与协议', groups.messageQueues],
    ['时序数据库', groups.timeSeries],
    ['搜索引擎', groups.searchEngines],
    ['云平台', groups.cloudPlatforms],
    ['其他', groups.others]
  ].map(([label, items]) => ({ label: label as string, options: Object.entries(items as Record<string, string>).map(([value, text]) => ({ value, label: text })) })).filter((item) => item.options.length)
})

async function handleDirectionChange(value: string | number | boolean) {
  form.dataDirection = value as DataDirection
  form.direction = directionToLegacy(form.dataDirection)
  form.type = ''
  form.pluginType = ''
  pluginOptions.value = []
  fields.value = []
  testPassed.value = false
  typeGroups.value = (await getResourceTypes(form.dataDirection)).data
}

async function handleTypeChange() {
  pluginLoading.value = true
  testPassed.value = false
  form.pluginType = ''
  try {
    pluginOptions.value = (await getPluginOptions(form.type)).data
    if (pluginOptions.value.length === 1) form.pluginType = pluginOptions.value[0].value
    await loadFields()
  } finally {
    pluginLoading.value = false
  }
}

async function loadFields() {
  if (!form.type) return
  fieldLoading.value = true
  try {
    fields.value = (await getResourceFields(form.type)).data
    for (const field of fields.value) {
      if (dynamicConfig[field.field] === undefined && field.config.default !== undefined) dynamicConfig[field.field] = field.config.default
    }
  } finally {
    fieldLoading.value = false
  }
}

function validateCoreFields() {
  const missing = coreFields.value.find((field) => field.config.required && (dynamicConfig[field.field] === undefined || dynamicConfig[field.field] === ''))
  if (missing) Message.warning(`请填写${missing.label}`)
  return !missing
}

function parseExtraJson() {
  if (!extraJson.value.trim()) return {}
  try {
    const value = JSON.parse(extraJson.value)
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('Expected a JSON object')
    return value as Record<string, unknown>
  } catch {
    Message.error('补充 JSON 配置格式错误')
    return null
  }
}

function buildPayload() {
  const extra = parseExtraJson()
  if (extra === null) return null
  const merged = { ...extra, ...dynamicConfig }
  return {
    ...form,
    direction: directionToLegacy(form.dataDirection),
    host: String(dynamicConfig.host || form.host || ''),
    port: dynamicConfig.port === undefined || dynamicConfig.port === '' ? form.port : Number(dynamicConfig.port),
    username: String(dynamicConfig.username || form.username || ''),
    password: String(dynamicConfig.password || form.password || ''),
    databaseName: String(dynamicConfig.databaseName || dynamicConfig.database || form.databaseName || ''),
    extraConfig: Object.keys(merged).length ? JSON.stringify(merged) : ''
  } as ResourceConnection
}

async function testConnection() {
  if (!validateCoreFields()) return
  const payload = buildPayload()
  if (!payload) return
  testing.value = true
  testMessage.value = ''
  try {
    const res = await testResourceConnection(payload)
    testPassed.value = true
    testMessage.value = res.message || '连接测试成功'
  } catch (error) {
    testPassed.value = false
    testMessage.value = error instanceof Error ? error.message : '连接测试失败'
  } finally {
    testing.value = false
  }
}

async function next() {
  if (step.value === 1) {
    if (!form.name.trim() || !form.dataDirection || !form.type || !form.pluginType) { Message.warning('请完整填写连接名称、数据方向、资源类型和插件类型'); return }
    await loadFields()
    step.value = 2
    return
  }
  if (!validateCoreFields()) return
  if (!testPassed.value) {
    Message.warning('请先完成连接测试')
    return
  }
  step.value = 3
}

async function save() {
  const payload = buildPayload()
  if (!payload) return
  saving.value = true
  try {
    if (payload.id) await updateResourceConnection(payload.id, payload)
    else await addResourceConnection(payload)
    Message.success('连接保存成功')
    visible.value = false
    emit('save-success')
  } finally {
    saving.value = false
  }
}

async function open(record?: ResourceConnection) {
  Object.keys(dynamicConfig).forEach((key) => delete dynamicConfig[key])
  Object.assign(form, createDefault())
  step.value = 1
  testPassed.value = false
  testMessage.value = ''
  extraJson.value = ''
  visible.value = true
  if (record?.id) {
    const detail = (await getResourceConnection(record.id)).data
    Object.assign(form, detail)
    const parsed = parseJsonObject(detail.extraConfig)
    Object.assign(dynamicConfig, parsed)
    for (const key of ['host', 'port', 'username', 'password', 'databaseName'] as const) {
      if (detail[key] !== undefined && dynamicConfig[key] === undefined) dynamicConfig[key] = detail[key]
    }
    typeGroups.value = (await getResourceTypes(detail.dataDirection)).data
    pluginOptions.value = (await getPluginOptions(detail.type)).data
    await loadFields()
  } else {
    typeGroups.value = (await getResourceTypes(form.dataDirection)).data
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.resource-form__steps { margin-bottom: 20px; }
.resource-form__section { display: grid; gap: 14px; }
.resource-form__test { display: flex; gap: 12px; align-items: center; color: var(--color-text-3); }
.resource-form__footer { position: sticky; bottom: 0; display: flex; justify-content: space-between; padding: 14px 20px; margin: 24px -20px -20px; background: var(--color-bg-2); border-top: 1px solid var(--color-border-2); }
.resource-form__code { font-family: Monaco, Menlo, Consolas, monospace; }

@media (width <= 576px) { .resource-form__test { flex-direction: column; align-items: flex-start; } }
</style>
