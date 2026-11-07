<template>
  <div class="wechat-editor">
    <a-alert>一个公众号可配置多个通知模板。微信字段名保持原样，识别字段后可直接选择取值变量，也可配置固定文本。</a-alert>
    <a-form-item v-for="field in metadata" :key="field.key" :label="field.label" :required="field.meta.includes('required')">
      <a-input :model-value="fieldValue(field.key)" :placeholder="field.meta.split('|')[2]" @update:model-value="set(field.key, $event)" />
    </a-form-item>
    <a-form-item label="微信模板原文" required>
      <a-space direction="vertical" class="wechat-editor__source">
        <a-textarea :model-value="content" :auto-size="{ minRows: 4, maxRows: 10 }" placeholder="粘贴公众号模板原文，如 姓名: {{deviceName.DATA}}" @update:model-value="emit('update:content', $event)" />
        <a-button @click="identify">识别微信字段</a-button>
      </a-space>
    </a-form-item>
    <div v-if="modelValue.fields.length" class="wechat-editor__mapping">
      <div class="wechat-editor__summary">已配置 {{ configuredCount }} / {{ modelValue.fields.length }} 个字段；可按中文名称或变量名搜索。</div>
      <div class="wechat-editor__row wechat-editor__row--header"><b>微信字段</b><b>显示名称</b><b>取值来源</b></div>
      <div v-for="(field, index) in modelValue.fields" :key="field.key" class="wechat-editor__row">
        <code>{{ field.key }}</code>
        <a-input :model-value="field.label" :aria-label="`${field.key} 显示名称`" @update:model-value="setField(index, 'label', $event)" />
        <div class="wechat-editor__value">
          <a-select
            :model-value="selectedSource(field)"
            allow-search
            :aria-label="`${field.key} 取值来源`"
            placeholder="请选择取值变量或固定文本"
            @change="selectValue(index, $event)"
          >
            <a-optgroup v-for="group in wechatVariableGroups" :key="group.label" :label="group.label">
              <a-option v-for="option in group.options" :key="option.key" :value="`#{${option.key}}`" :label="`${option.label} · ${option.key}`" />
            </a-optgroup>
            <a-option :value="CUSTOM_VALUE" label="固定文本 / 自定义表达式" />
          </a-select>
          <div v-if="!isCustom(field)" class="wechat-editor__description">
            {{ findWechatVariable(field.value)?.description }}
          </div>
          <a-input v-else :model-value="field.value" :aria-label="`${field.key} 固定文本或自定义表达式`" placeholder="固定文本，或自定义取值如 #{properties.amount}" @update:model-value="setCustomValue(index, $event)" />
          <div v-if="!field.value.trim()" class="wechat-editor__pending">待配置：请选择变量或填写固定文本</div>
        </div>
      </div>
    </div>
    <p class="wechat-editor__hint">场景变量适用于手动、定时和设备触发；设备与消息变量需实际报文提供。自定义属性可使用 #{properties.amount}、#{data.value}，请替换为真实字段；不能直接选择 properties、trigger 等整个对象。外链和小程序可同时配置，微信优先打开小程序。</p>
  </div>
</template>

<script setup lang="ts">
import type { WechatConfig, WechatField } from './wechat'
import { Message } from '@arco-design/web-vue'
import { listNoticeChannelFields } from '@/apis/application/notice'
import { findWechatVariable, identifyFields, wechatVariableGroups } from './wechat'

const props = defineProps<{ modelValue: WechatConfig, content: string }>()
const emit = defineEmits<{ (e: 'update:model-value', value: WechatConfig): void, (e: 'update:content', value: string): void }>()
type ConfigKey = keyof Omit<WechatConfig, 'fields'>
const CUSTOM_VALUE = '__custom__'
const customFields = ref<Record<string, boolean>>({})
watch(() => props.modelValue.fields.length, (length) => {
  if (!length) customFields.value = {}
})
const configuredCount = computed(() => props.modelValue.fields.filter((field) => field.value.trim()).length)
const metadata = ref<Array<{ key: string, label: string, meta: string }>>([])
onMounted(async () => {
  try {
    metadata.value = (await listNoticeChannelFields('notice_wechat_template_field')).data || []
  } catch {
    Message.error('微信模板字典加载失败')
  }
})
function fieldValue(key: string) {
  return String(props.modelValue[key as ConfigKey] || '')
}
function set(key: string, value: string) {
  emit('update:model-value', { ...props.modelValue, [key]: value })
}
function setField(index: number, key: keyof WechatField, value: string) {
  emit('update:model-value', { ...props.modelValue, fields: props.modelValue.fields.map((field, i) => i === index ? { ...field, [key]: value } : field) })
}
function isCustom(field: WechatField) {
  return customFields.value[field.key] || !findWechatVariable(field.value)
}
function selectedSource(field: WechatField) {
  return !isCustom(field) ? field.value : (customFields.value[field.key] || field.value ? CUSTOM_VALUE : undefined)
}
function setCustomValue(index: number, value: string) {
  customFields.value[props.modelValue.fields[index].key] = true
  setField(index, 'value', value)
}
function selectValue(index: number, value: unknown) {
  if (typeof value !== 'string') return
  customFields.value[props.modelValue.fields[index].key] = value === CUSTOM_VALUE
  if (value !== CUSTOM_VALUE) setField(index, 'value', value)
}
function identify() {
  const fields = identifyFields(props.content, props.modelValue.fields)
  if (!fields.length) {
    Message.warning('未识别到 {{字段.DATA}}，请检查微信模板原文')
    return
  }
  emit('update:model-value', { ...props.modelValue, fields })
  const pending = fields.filter((field) => !field.value.trim()).length
  Message.success(`已识别 ${fields.length} 个微信字段，${pending ? `还有 ${pending} 个待选择取值` : '取值已配置，可继续调整'}`)
}
</script>

<style scoped lang="scss">
.wechat-editor {
  width: 100%;
}

.wechat-editor__source,
.wechat-editor__value > * {
  width: 100%;
}

.wechat-editor__mapping {
  margin: 16px 0;
  overflow-x: auto;
}

.wechat-editor__summary {
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--color-text-3);
}

.wechat-editor__row {
  display: grid;
  grid-template-columns: 90px 110px minmax(240px, 1fr);
  gap: 8px;
  align-items: start;
  min-width: 480px;
  margin-bottom: 16px;
}

.wechat-editor__row > * {
  min-width: 0;
}

.wechat-editor__row code {
  padding-top: 6px;
  overflow-wrap: anywhere;
}

.wechat-editor__row--header {
  margin-bottom: 8px;
}

.wechat-editor__value {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wechat-editor__description,
.wechat-editor__hint {
  line-height: 1.8;
  color: var(--color-text-3);
}

.wechat-editor__description,
.wechat-editor__pending {
  font-size: 12px;
}

.wechat-editor__pending {
  color: rgb(var(--orange-6));
}
</style>
