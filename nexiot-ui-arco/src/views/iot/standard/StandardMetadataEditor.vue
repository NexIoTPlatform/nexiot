<template>
  <div class="standard-metadata-editor">
    <a-radio-group :model-value="mode" type="button" @change="changeMode">
      <a-radio value="visual">可视化编辑</a-radio>
      <a-radio value="json">JSON 编辑</a-radio>
    </a-radio-group>
    <a-alert v-if="error" type="error">{{ error }}</a-alert>
    <ProductMetadataPanel v-if="mode === 'visual'" v-model="metadata" draft />
    <a-textarea v-else v-model="json" class="standard-metadata-editor__json" :auto-size="{ minRows: 12, maxRows: 22 }" aria-label="物模型 JSON" />
  </div>
</template>

<script setup lang="ts">
import type { MetadataDocument } from '@/views/iot/product/details/metadataSchema'
import { parseMetadata, validateMetadata } from '@/views/iot/product/details/metadataSchema'

defineOptions({ name: 'StandardMetadataEditor' })
const props = defineProps<{ modelValue: string }>()
const ProductMetadataPanel = defineAsyncComponent(() => import('@/views/iot/product/details/ProductMetadataPanel.vue'))
const metadata = ref<MetadataDocument>({ properties: [], events: [], functions: [] })
const mode = ref('visual')
const json = ref('')
const error = ref('')

function parseJson() {
  try {
    const data = parseMetadata(json.value)
    const schemaError = validateMetadata(data)
    if (schemaError) throw new Error(schemaError)
    error.value = ''
    return data
  } catch (cause) {
    error.value = cause instanceof SyntaxError ? '请输入有效的 JSON' : (cause as Error).message
    return undefined
  }
}

function changeMode(value: unknown) {
  if (value === 'visual') {
    const data = parseJson()
    if (!data) return
    metadata.value = data
  } else {
    json.value = JSON.stringify(metadata.value, null, 2)
  }
  error.value = ''
  mode.value = String(value)
}

function getMetadata() {
  const data = mode.value === 'json' ? parseJson() : metadata.value
  if (!data) return undefined
  error.value = validateMetadata(data) || ''
  return error.value ? undefined : JSON.stringify(data)
}

watch(() => props.modelValue, (value) => {
  json.value = value
  error.value = ''
  try {
    metadata.value = parseMetadata(value)
    mode.value = 'visual'
  } catch {
    error.value = '物模型 JSON 无效，请修正后保存'
    mode.value = 'json'
  }
}, { immediate: true })

defineExpose({ getMetadata })
</script>

<style lang="scss" scoped>
.standard-metadata-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__json :deep(textarea) {
    font-family: Consolas, Monaco, monospace;
  }
}
</style>
