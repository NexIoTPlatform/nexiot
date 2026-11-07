<template>
  <a-modal v-model:visible="visible" title="编辑物模型" width="calc(100% - 32px)" :modal-style="{ maxWidth: '1080px' }" :body-style="{ maxHeight: '72vh', overflowY: 'auto' }" :mask-closable="false" unmount-on-close @before-ok="save">
    <StandardMetadataEditor v-if="visible" ref="editorRef" :model-value="metadata" />
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { getStandardProduct, updateStandardProductMetadata } from '@/apis/iot/standardModel'
import StandardMetadataEditor from './StandardMetadataEditor.vue'

defineOptions({ name: 'MetadataEditorModal' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const editorRef = useTemplateRef<InstanceType<typeof StandardMetadataEditor>>('editorRef')
const visible = ref(false)
const id = ref('')
const metadata = ref('')
async function open(productId: string) {
  const data = (await getStandardProduct(productId)).data
  id.value = productId
  metadata.value = data.metadata || ''
  visible.value = true
}
async function save() {
  const data = editorRef.value?.getMetadata()
  if (data === undefined) return false
  await updateStandardProductMetadata(id.value, data)
  Message.success('物模型保存成功')
  emit('save-success')
  return true
}
defineExpose({ open })
</script>
