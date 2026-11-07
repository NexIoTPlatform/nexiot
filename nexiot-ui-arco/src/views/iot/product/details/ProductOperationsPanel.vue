<template>
  <section class="operations-panel">
    <header class="operations-panel__header">
      <div><h3>产品级操作</h3><p>同步、调试和运维能力会随当前产品协议自动加载。</p></div>
      <a-space><a-tag color="arcoblue">总计 {{ operations.length }}</a-tag><a-tag color="green">可执行 {{ readyCount }}</a-tag><a-button :loading="loading" @click="loadOperations"><template #icon><icon-refresh /></template>刷新</a-button></a-space>
    </header>
    <a-empty v-if="!loading && !operations.length" description="当前产品暂无产品级操作" />
    <a-spin :loading="loading">
      <div class="operation-grid">
        <article v-for="operation in operations" :key="operation.code" class="operation-card" :class="{ 'operation-card--disabled': !operation.enabled }">
          <header><div><h4>{{ operation.name }}</h4><code>{{ operation.code }}</code></div><a-tag :color="operation.enabled ? 'green' : 'gray'">{{ operation.enabled ? '可执行' : '未就绪' }}</a-tag></header>
          <p>{{ operation.description || operation.disabledReason || '暂无操作说明' }}</p>
          <a-form v-if="operation.fields?.length" :model="formState[operation.code]" layout="vertical">
            <a-form-item v-for="field in operation.fields" :key="field.key" :label="field.label" :required="field.required">
              <a-select v-if="field.type === 'select'" :model-value="formState[operation.code][field.key]" :options="field.options" :placeholder="field.placeholder" @change="value => setValue(operation.code, field.key, value)" />
              <a-switch v-else-if="field.type === 'boolean'" :model-value="Boolean(formState[operation.code][field.key])" @change="value => setValue(operation.code, field.key, value)" />
              <a-input-number v-else-if="field.type === 'int'" :model-value="numberValue(operation.code, field.key)" :placeholder="field.placeholder" @change="value => setValue(operation.code, field.key, value)" />
              <a-textarea v-else-if="field.type === 'textarea'" :model-value="stringValue(operation.code, field.key)" :placeholder="field.placeholder" :auto-size="{ minRows: 2, maxRows: 5 }" @input="value => setValue(operation.code, field.key, value)" />
              <a-input v-else :model-value="stringValue(operation.code, field.key)" :placeholder="field.placeholder" @input="value => setValue(operation.code, field.key, value)" />
              <div v-if="field.description" class="field-description">{{ field.description }}</div>
            </a-form-item>
          </a-form>
          <footer>
            <a-popconfirm v-if="operation.confirmText" :content="operation.confirmText" @before-ok="() => execute(operation)"><a-button type="primary" :disabled="!operation.enabled" :loading="executingCode === operation.code"><template #icon><icon-play-arrow /></template>执行</a-button></a-popconfirm>
            <a-button v-else type="primary" :disabled="!operation.enabled" :loading="executingCode === operation.code" @click="execute(operation)"><template #icon><icon-play-arrow /></template>执行</a-button>
          </footer>
        </article>
      </div>
    </a-spin>
  </section>
</template>

<script setup lang="ts">
import type { ProductOperation } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { executeProductOperation, getProductOperations } from '@/apis/iot/product'

defineOptions({ name: 'ProductOperationsPanel' })
const props = defineProps<{ productId: string }>()
const loading = ref(false)
const executingCode = ref('')
const operations = ref<ProductOperation[]>([])
const formState = reactive<Record<string, Record<string, string | number | boolean | undefined>>>({})
const readyCount = computed(() => operations.value.filter((item) => item.enabled).length)

function setValue(operationCode: string, fieldKey: string, value: unknown) {
  formState[operationCode][fieldKey] = ['string', 'number', 'boolean'].includes(typeof value)
    ? value as string | number | boolean
    : undefined
}

function numberValue(operationCode: string, fieldKey: string) {
  const value = formState[operationCode][fieldKey]
  return typeof value === 'number' ? value : undefined
}

function stringValue(operationCode: string, fieldKey: string) {
  const value = formState[operationCode][fieldKey]
  return value === undefined ? '' : String(value)
}

async function loadOperations() {
  loading.value = true
  try {
    operations.value = (await getProductOperations(props.productId)).data || []
    operations.value.forEach((operation) => {
      formState[operation.code] = Object.fromEntries((operation.fields || []).map((field) => [field.key, field.defaultValue]))
    })
  } finally { loading.value = false }
}
async function execute(operation: ProductOperation) {
  executingCode.value = operation.code
  try {
    await executeProductOperation(props.productId, operation.code, formState[operation.code] || {})
    Message.success(`${operation.name}执行成功`)
    await loadOperations()
    return true
  } finally {
    executingCode.value = ''
  }
}
watch(() => props.productId, loadOperations, { immediate: true })
</script>

<style lang="scss" scoped>
.operations-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.operations-panel__header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
}

.operations-panel__header h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.operations-panel__header p {
  margin: 0;
  color: var(--color-text-3);
}

.operation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 12px;
}

.operation-card {
  display: flex;
  flex-direction: column;
  min-height: 190px;
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
}

.operation-card--disabled {
  opacity: 0.68;
}

.operation-card > header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.operation-card h4 {
  margin: 0 0 5px;
  font-size: 15px;
}

.operation-card code {
  font-size: 12px;
  color: var(--color-text-3);
}

.operation-card > p {
  line-height: 1.6;
  color: var(--color-text-2);
}

.operation-card > footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  margin-top: auto;
}

.field-description {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
}

@media (width <= 720px) {
  .operations-panel__header {
    flex-direction: column;
  }
}
</style>
