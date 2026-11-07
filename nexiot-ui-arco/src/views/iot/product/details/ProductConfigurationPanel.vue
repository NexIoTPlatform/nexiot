<template>
  <div class="configuration-panel">
    <a-alert v-if="product.thirdPlatform === 'ezviz'" type="info" show-icon>
      萤石托管需配置应用 AppKey、AppSecret 和控制台提供的托管授权地址。开发者回调填写公网地址
      https://你的域名/api/iot/ezviz/events（路径以网关实际配置为准）。选择“需要授权码”可扫码自动入库；选择“不需要授权码”后需在视频中心的托管设备中刷新并导入。
    </a-alert>
    <section v-for="section in sections" :key="section.key" class="configuration-section">
      <header class="configuration-section__header">
        <div>
          <strong>
            <component :is="section.icon"/>
            {{ section.title }}</strong>
          <p>{{ section.description }}</p>
        </div>
        <a-button v-hasPerm="['system:product:edit']" type="text"
                  :loading="loadingSection === section.key" @click="startEdit(section)">
          <template #icon>
            <icon-edit/>
          </template>
          编辑
        </a-button>
      </header>

      <a-empty v-if="!section.fields.length" description="当前接入方式没有可配置项"/>
      <a-descriptions v-else :column="{ xs: 1, md: 2 }" bordered size="large"
                      class="configuration-section__values">
        <a-descriptions-item v-for="field in section.fields" :key="field.key" :label="field.label">
          <span class="configuration-value">{{ displayValue(field, config[field.key]) }}</span>
        </a-descriptions-item>
      </a-descriptions>
    </section>

    <a-modal
        v-model:visible="editVisible"
        :title="editingSection ? `编辑${editingSection.title}` : '编辑配置'"
        width="min(760px, calc(100vw - 32px))"
        :mask-closable="false"
        :ok-loading="saving"
        unmount-on-close
        @before-ok="saveSection"
        @close="resetEditor"
    >
      <a-form v-if="editingSection" :model="form" layout="vertical"
              class="configuration-section__form">
        <component
            :is="protocolPlugin.component"
            v-if="editingSection.key === 'private' && protocolPlugin"
            v-model="protocolForm"
            :editing="Boolean(props.product.id)"
        />
        <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
          <a-form-item
              v-for="field in editingSection.fields.filter((item) => !isProtocolField(item.key))"
              :key="field.key" :label="field.label" :extra="field.help">
            <a-input-number v-if="field.type === 'int'" :model-value="getNumberValue(field.key)"
                            :placeholder="field.placeholder" :precision="0"
                            @update:model-value="setValue(field.key, $event)"/>
            <a-select v-else-if="field.type === 'boolean'" :model-value="getSelectValue(field.key)"
                      :placeholder="field.placeholder" allow-clear :options="booleanOptions"
                      @update:model-value="setValue(field.key, $event)"/>
            <a-select v-else-if="field.type === 'select'" :model-value="getSelectValue(field.key)"
                      :placeholder="field.placeholder" allow-clear allow-search
                      :options="field.options" @update:model-value="setValue(field.key, $event)"/>
            <a-textarea v-else-if="field.type === 'json'" :model-value="getTextValue(field.key)"
                        :placeholder="field.placeholder" :auto-size="{ minRows: 3, maxRows: 8 }"
                        @update:model-value="setValue(field.key, $event)"/>
            <a-input-password v-else-if="field.type === 'password'"
                              :model-value="getTextValue(field.key)"
                              :placeholder="field.placeholder" autocomplete="off"
                              @update:model-value="setValue(field.key, $event)"/>
            <a-input v-else :model-value="getTextValue(field.key)" :placeholder="field.placeholder"
                     allow-clear @update:model-value="setValue(field.key, $event)"/>
          </a-form-item>
        </a-grid>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type {Component} from 'vue'
import type {ProductConfigDictItem, ProductItem} from '@/apis/iot/product'
import {getProduct, getProductConfigDicts, updateProductConfig} from '@/apis/iot/product'
import {Message} from '@arco-design/web-vue'
import {IconCloud, IconSettings} from '@arco-design/web-vue/es/icon'
import {getProtocolConfigurationPlugin} from './protocolConfigurationRegistry'

defineOptions({name: 'ProductConfigurationPanel'})

const props = defineProps<{ product: ProductItem }>()

const emit = defineEmits<{ (e: 'refresh'): void }>()

interface ConfigField {
  key: string
  label: string
  type: string
  placeholder: string
  help: string
  options: { label: string, value: string | number | boolean }[]
}

interface ConfigSection {
  key: 'public' | 'private'
  title: string
  description: string
  icon: Component
  fields: ConfigField[]
}

const saving = ref(false)
const editing = ref<ConfigSection['key']>()
const loadingSection = ref<ConfigSection['key']>()
const editVisible = ref(false)
const config = reactive<Record<string, unknown>>({})
const form = reactive<Record<string, unknown>>({})
const protocolForm = ref<Record<string, unknown>>({})
const privateDicts = ref<ProductConfigDictItem[]>([])
const publicDicts = ref<ProductConfigDictItem[]>([])
const optionDicts = reactive<Record<string, ProductConfigDictItem[]>>({})
const booleanOptions = [{label: '是', value: 'true'}, {label: '否', value: 'false'}]
const protocolPlugin = computed(() => getProtocolConfigurationPlugin(props.product.thirdPlatform))
const protocolFieldKeys = computed(() => new Set(protocolPlugin.value?.fields.map((field) => field.key) || []))

const sections = computed<ConfigSection[]>(() => [
  {
    key: 'private',
    title: '端云配置',
    description: `${props.product.thirdPlatform || '当前接入方式'} 的设备侧与云端连接参数。`,
    icon: IconCloud,
    fields: [
      ...privateDicts.value.map(buildField).filter((field) => !protocolFieldKeys.value.has(field.key)),
      ...(protocolPlugin.value?.fields || []).map(buildProtocolField)
    ]
  }, {
    key: 'public',
    title: '公共配置',
    description: '设备通信、离线判定、数据解析等产品通用行为。',
    icon: IconSettings,
    fields: publicDicts.value.map(buildField)
  }
])
const editingSection = computed(() => sections.value.find((section) => section.key === editing.value))

function parseObject(value?: string) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function readConfiguration(product: ProductItem) {
  const current = parseObject(product.configuration)
  if (product.thirdPlatform !== 'ezviz') return current
  const legacy = parseObject(product.thirdConfiguration)
  const effective = {...legacy, ...legacy.ezviz, ...current, ...current.ezviz}
  for (const key of ['appKey', 'appSecret', 'baseUrl', 'hostingUrl', 'hostingCallbackUrl']) {
    if (effective[key] !== undefined) current[key] = effective[key]
  }
  return current
}

function parseRemark(item: ProductConfigDictItem) {
  try {
    return JSON.parse(item.remark || '{}') as { type?: string, remark?: string, url?: string }
  } catch {
    return {remark: item.remark || ''}
  }
}

function buildField(item: ProductConfigDictItem): ConfigField {
  const meta = parseRemark(item)
  return {
    key: item.dictValue,
    label: item.dictLabel,
    type: item.dictValue === 'appSecret' ? 'password' : meta.type || 'string',
    placeholder: meta.remark || `请输入${item.dictLabel}`,
    help: meta.remark || '',
    options: ((meta.url ? optionDicts[meta.url] : []) || []).map((option) => ({
      label: option.dictLabel,
      value: option.dictValue
    }))
  }
}

function buildProtocolField(field: NonNullable<typeof protocolPlugin.value>['fields'][number]): ConfigField {
  return {
    key: field.key,
    label: field.label,
    type: field.type,
    placeholder: field.placeholder || `请输入${field.label}`,
    help: field.help || '',
    options: field.options || []
  }
}

function isProtocolField(key: string) {
  return protocolFieldKeys.value.has(key)
}

function displayValue(field: ConfigField, value: unknown) {
  if (value === undefined || value === null || value === '') return '未配置'
  if (field.type === 'password') return '••••••••'
  if (field.type === 'boolean') return String(value) === 'true' ? '是' : '否'
  if (field.type === 'select') return field.options.find((item) => String(item.value) === String(value))?.label || String(value)
  if (typeof value === 'object') return '[已配置]'
  return String(value)
}

function getNumberValue(key: string) {
  const value = form[key]
  return typeof value === 'number' ? value : value === '' || value === undefined ? undefined : Number(value)
}

function getTextValue(key: string) {
  const value = form[key]
  return value === undefined || value === null ? '' : String(value)
}

function getSelectValue(key: string) {
  const value = form[key]
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? value : undefined
}

function setValue(key: string, value: unknown) {
  form[key] = value
}

async function loadDicts() {
  const privateType = `product_configuration_${props.product.thirdPlatform || ''}`
  const res = await getProductConfigDicts(['product_configuration_public', privateType])
  publicDicts.value = res.data.product_configuration_public || []
  privateDicts.value = (res.data[privateType] || []).filter((item) => props.product.thirdPlatform !== 'ezviz' || !['hostingAutoAccept', 'hostingPermissions'].includes(item.dictValue))
  const optionTypes = [...new Set([...publicDicts.value, ...privateDicts.value].map((item) => parseRemark(item).url).filter(Boolean))] as string[]
  if (optionTypes.length) Object.assign(optionDicts, (await getProductConfigDicts(optionTypes)).data)
}

async function startEdit(section: ConfigSection) {
  loadingSection.value = section.key
  try {
    const raw = (await getProduct(props.product.id)).data
    Object.keys(config).forEach((key) => delete config[key])
    Object.assign(config, readConfiguration(raw))
    Object.keys(form).forEach((key) => delete form[key])
    for (const field of section.fields) {
      if (!isProtocolField(field.key)) form[field.key] = config[field.key]
    }
    protocolForm.value = protocolPlugin.value?.createModel(config) || {}
    editing.value = section.key
    editVisible.value = true
  } finally {
    loadingSection.value = undefined
  }
}

function resetEditor() {
  editing.value = undefined
  Object.keys(form).forEach((key) => delete form[key])
  protocolForm.value = {}
}

async function saveSection() {
  const section = editingSection.value
  if (!section) return false
  const next = {...config}
  for (const field of section.fields) {
    if (isProtocolField(field.key)) continue
    const value = form[field.key]
    if (value === undefined || value === null || value === '') delete next[field.key]
    else next[field.key] = value
    if (props.product.thirdPlatform === 'ezviz' && next.ezviz && typeof next.ezviz === 'object' && field.key in next.ezviz) {
      next.ezviz = {...next.ezviz, [field.key]: value}
    }
  }
  if (section.key === 'private' && protocolPlugin.value) {
    const error = protocolPlugin.value.validate?.(protocolForm.value, true)
    if (error) {
      Message.warning(error)
      return false
    }
    Object.assign(next, protocolPlugin.value.merge(next, protocolForm.value))
  }
  saving.value = true
  try {
    await updateProductConfig({id: props.product.id, configuration: JSON.stringify(next)})
    Object.keys(config).forEach((key) => delete config[key])
    Object.assign(config, next)
    Message.success(`${section.title}保存成功`)
    emit('refresh')
    return true
  } finally {
    saving.value = false
  }
}

watch(() => props.product, async (product) => {
  Object.keys(config).forEach((key) => delete config[key])
  Object.assign(config, readConfiguration(product))
  await loadDicts()
}, {immediate: true})
</script>

<style lang="scss" scoped>
.configuration-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.configuration-section {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 14px;

    strong {
      display: flex;
      gap: 7px;
      align-items: center;
      font-size: 15px;
    }

    p {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__form :deep(.arco-input-number) {
    width: 100%;
  }
}

.configuration-value {
  overflow-wrap: anywhere;
}

.configuration-section__values :deep(.arco-descriptions-item-label) {
  width: 180px;
}

@media (width <= 600px) {
  .configuration-section__header {
    flex-direction: column;
  }

  .configuration-section__values :deep(.arco-descriptions-item-label) {
    width: 150px;
    min-width: 150px;
    max-width: 150px;
  }
}
</style>
