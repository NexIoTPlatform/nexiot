<template>
  <a-modal v-model:visible="visible" title="导入产品完整包" width="620px" :mask-closable="false" @before-ok="submit" @close="file = undefined">
    <a-upload :file-list="fileList" accept="application/json,.json" :auto-upload="false" :limit="1" draggable @change="changeFile" />
    <a-alert class="product-import__tip">迁移产品、物模型、协议和网络等产品配置，设备另行导入。整批导入失败会全部回滚；已有产品会明确报错。网络导入后需在通信组件页面手动启动。</a-alert>
    <a-alert v-if="errorMessage" class="product-import__error" type="error">{{ errorMessage }}</a-alert>
  </a-modal>
  <a-modal v-model:visible="resultVisible" width="500px" hide-title :footer="false" unmount-on-close>
    <a-result class="product-import__result" status="success" title="产品导入成功" subtitle="配置已保存，可在产品详情中查看">
      <div v-if="importResult" class="product-import__stats">
        <div v-for="item in resultStats" :key="item.label" class="product-import__stat">
          <span class="product-import__count">{{ item.count }}</span>
          <span class="product-import__label">{{ item.label }}</span>
        </div>
      </div>
      <p v-else class="product-import__message">{{ successMessage }}</p>
      <a-alert v-if="importResult && importResult.networkCount > 0" class="product-import__next" title="网络配置待启用">在「通信组件」检查地址和端口后启用。</a-alert>
      <div class="product-import__actions"><a-button type="primary" @click="resultVisible = false">完成</a-button></div>
    </a-result>
  </a-modal>
</template>

<script setup lang="ts">
import type { FileItem } from '@arco-design/web-vue'
import type { ProductImportResult } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { importProducts } from '@/apis/iot/product'

defineOptions({ name: 'ProductImportModal' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const visible = ref(false)
const file = ref<File>()
const errorMessage = ref('')
const resultVisible = ref(false)
const importResult = ref<ProductImportResult>()
const successMessage = ref('')
const fileList = computed(() => file.value ? [{ uid: 'product-import', name: file.value.name, file: file.value }] : [])
const resultStats = computed(() => [
  { label: '产品', count: importResult.value?.productCount },
  { label: '协议', count: importResult.value?.protocolCount },
  { label: '网络配置', count: importResult.value?.networkCount },
  { label: '物模型版本', count: importResult.value?.metadataVersionCount }
])
function open() {
  file.value = undefined
  errorMessage.value = ''
  visible.value = true
}
function changeFile(items: FileItem[]) {
  file.value = items[0]?.file
  errorMessage.value = ''
}
async function submit() {
  if (!file.value) {
    Message.warning('请选择导入文件')
    return false
  }
  try {
    const result = await importProducts(file.value)
    importResult.value = result.data && typeof result.data.productCount === 'number' ? result.data : undefined
    successMessage.value = result.message || '产品配置已导入'
    resultVisible.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '导入失败，请检查文件后重试'
    return false
  }
  emit('save-success')
  return true
}
defineExpose({ open })
</script>

<style lang="scss" scoped>
.product-import {
  &__tip,
  &__error { margin-top: 12px; }

  &__result {
    padding: 4px 0 0;

    :deep(.arco-result-icon) { margin-bottom: 8px; }

    :deep(.arco-result-icon-tip) {
      width: 36px;
      height: 36px;
    }

    :deep(.arco-result-title) {
      font-size: 18px;
      font-weight: 600;
    }

    :deep(.arco-result-subtitle) {
      margin-top: 4px;
      font-size: 12px;
      color: var(--color-text-3);
    }

    :deep(.arco-result-content) { margin-top: 16px; }
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 8px;
    margin-bottom: 12px;
  }

  &__stat {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px 6px;
    text-align: center;
    background: var(--color-fill-2);
    border-radius: var(--border-radius-medium);
  }

  &__count {
    font-size: 24px;
    font-weight: 600;
    line-height: 1;
    color: var(--color-text-1);
  }

  &__label {
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__next {
    font-size: 12px;
    text-align: left;
  }

  &__actions {
    margin-top: 16px;
    text-align: center;
  }

  &__message {
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }
}
</style>
