<template>
  <a-modal v-model:visible="visible" title="查看物模型" width="1000px" :footer="false" unmount-on-close>
    <a-space class="metadata-view__toolbar">
      <a-button @click="copy"><template #icon><icon-copy /></template>复制 JSON</a-button>
      <a-button @click="download"><template #icon><icon-download /></template>下载 JSON</a-button>
    </a-space>
    <a-spin :loading="loading" class="metadata-view__content">
      <GiCodeView :code-json="jsonText" />
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { MetadataDocument } from './metadataSchema'
import { Message } from '@arco-design/web-vue'
import { useClipboard } from '@vueuse/core'
import { getProduct } from '@/apis/iot/product'
import GiCodeView from '@/components/GiCodeView/GiCodeView.vue'

defineOptions({ name: 'MetadataViewModal' })
const props = defineProps<{ productId: string, productKey: string, draft?: boolean, modelValue?: MetadataDocument }>()
const visible = ref(false)
const loading = ref(false)
const jsonText = ref('')
const clipboard = useClipboard({ legacy: true })

async function open() {
  jsonText.value = ''
  visible.value = true
  loading.value = true
  try {
    const metadata = props.draft ? props.modelValue : (await getProduct(props.productId)).data.metadata
    jsonText.value = JSON.stringify(typeof metadata === 'string' ? JSON.parse(metadata || '{}') : metadata || {}, null, 2)
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '读取物模型失败')
  } finally {
    loading.value = false
  }
}
async function copy() {
  if (!jsonText.value) return
  try {
    await clipboard.copy(jsonText.value)
    Message.success('物模型 JSON 已复制')
  } catch {
    Message.error('复制失败，请选择 JSON 内容手动复制')
  }
}
function download() {
  if (!jsonText.value) return
  const url = URL.createObjectURL(new Blob([jsonText.value], { type: 'application/json;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `metadata_${props.productKey || 'draft'}.json`
  link.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
defineExpose({ open })
</script>

<style lang="scss" scoped>
.metadata-view {
  &__toolbar { margin-bottom: 12px; }

  &__content {
    display: block;
    max-height: 65vh;
    overflow: auto;
  }
}
</style>
