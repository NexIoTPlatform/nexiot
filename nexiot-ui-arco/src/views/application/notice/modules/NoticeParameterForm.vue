<template>
  <div>
    <a-form-item v-for="variable in variables" :key="variable" :label="variable" required>
      <a-input :model-value="String(modelValue[variable] ?? '')" :placeholder="`请输入 ${variable}`" @update:model-value="change(variable, $event)" />
    </a-form-item>
    <a-alert v-if="template.channelType === 'wechat' && !template.wechat" type="warning">此微信模板尚未配置字段映射，请先编辑模板。</a-alert>
    <a-card v-if="template.wechat" title="微信卡片预览" size="small">
      <div v-for="field in preview" :key="field.key" class="notice-field"><b>{{ field.label }}：</b>{{ field.value }}</div>
      <p>以微信实际模板排版为准。</p>
    </a-card>
    <a-button :loading="loading" style="margin-top: 12px" @click="check">服务端校验与预览</a-button>
    <a-alert v-if="error" type="error" style="margin-top: 8px">{{ error }}</a-alert>
    <a-card v-if="checked" title="服务端解析结果" size="small" style="margin-top: 8px">
      <template v-if="checked.payload">
        <div>微信模板 ID：{{ checked.payload.template_id }}</div>
        <div v-for="(field, key) in checked.payload.data" :key="key" class="notice-field"><b>{{ key }}：</b>{{ field.value }}</div>
        <div v-if="checked.payload.url">外链：{{ checked.payload.url }}</div>
        <div v-if="checked.payload.miniprogram">小程序：{{ checked.payload.miniprogram.appid }} / {{ checked.payload.miniprogram.pagepath }}</div>
      </template>
      <div v-else>{{ checked.content }}</div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import type { NoticePreview, NoticeTemplate } from '@/apis/application/notice'
import { previewNoticeTemplate } from '@/apis/application/notice'
import { noticeVariables, previewWechat } from './wechat'

const props = defineProps<{ template: NoticeTemplate, modelValue: Record<string, unknown> }>()
const emit = defineEmits<{ (e: 'update:model-value', value: Record<string, unknown>): void }>()
const variables = computed(() => noticeVariables(props.template))
const preview = computed(() => previewWechat(props.template, props.modelValue))
const checked = ref<NoticePreview>()
const error = ref('')
const loading = ref(false)
function change(key: string, value: string) {
  checked.value = undefined
  error.value = ''
  emit('update:model-value', { ...props.modelValue, [key]: value })
}
async function check() {
  loading.value = true
  error.value = ''
  checked.value = undefined
  try {
    checked.value = (await previewNoticeTemplate({ templateId: String(props.template.id), params: props.modelValue })).data
  } catch (e) {
    error.value = e instanceof Error ? e.message : '参数校验失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.notice-field { margin: 8px 0; white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
