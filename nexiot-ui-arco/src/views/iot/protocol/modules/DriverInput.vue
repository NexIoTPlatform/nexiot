<template>
  <div class="driver-input">
    <a-form layout="vertical" :model="{ method, payload, context }" :disabled="disabled" size="small">
      <a-form-item label="执行方法"><a-select v-model="method" :options="methods" /></a-form-item>
      <a-form-item label="报文 payload"><a-textarea v-model="payload" placeholder="粘贴原始 HEX、JSON 或文本报文" :auto-size="{ minRows: 7, maxRows: 18 }" /></a-form-item>
      <a-collapse :default-active-key="context ? ['context'] : []" :bordered="false">
        <a-collapse-item key="context" header="上下文 context（可选）"><a-textarea v-model="context" :disabled="disabled" placeholder="按字符串传入；留空为 null" :auto-size="{ minRows: 3, maxRows: 10 }" /></a-collapse-item>
      </a-collapse>
    </a-form>
    <p class="driver-input__hint">运行当前草稿。保存并应用后更新驱动。</p>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'DriverInput' })
const method = defineModel<string>('method', { required: true })
const payload = defineModel<string>('payload', { required: true })
const context = defineModel<string>('context', { required: true })
defineProps<{ disabled: boolean }>()
const methods = [
  { label: 'decode · 上行解码', value: 'decode' },
  { label: 'encode · 下行编码', value: 'encode' },
  { label: 'preDecode · 前置解析', value: 'preDecode' },
  { label: 'iotToYour · 平台转第三方', value: 'iotToYour' },
  { label: 'yourToIot · 第三方转平台', value: 'yourToIot' }
]
</script>

<style lang="scss" scoped>
.driver-input {
  flex: 1;
  min-height: 0;
  padding: 16px 14px;
  overflow: auto;

  &__hint {
    margin: 16px 0 0;
    font-size: 12px;
    line-height: 1.8;
    color: var(--color-text-3);
  }

  :deep(.arco-textarea) {
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
  }

  :deep(.arco-collapse-item-content) {
    padding: 0;
    background: transparent;
  }

  :deep(.arco-collapse-item-header) {
    padding-left: 36px;
    font-size: 12px;
  }
}
</style>
