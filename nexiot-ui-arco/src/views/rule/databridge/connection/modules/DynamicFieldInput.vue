<template>
  <a-form-item :label="field.label" :required="field.config.required">
    <a-input-password v-if="field.config.type === 'password'" v-model="model" :placeholder="placeholder" allow-clear />
    <a-input-number v-else-if="field.config.type === 'number'" v-model="model" :min="field.config.min" :max="field.config.max" :placeholder="placeholder" style="width: 100%" />
    <a-select v-else-if="field.config.type === 'select'" v-model="model" :options="options" :placeholder="placeholder" allow-clear />
    <a-switch v-else-if="field.config.type === 'switch'" v-model="model" />
    <a-textarea v-else-if="field.config.type === 'textarea'" v-model="model" :placeholder="placeholder" :auto-size="{ minRows: 3, maxRows: 6 }" />
    <a-input v-else v-model="model" :placeholder="placeholder" allow-clear />
  </a-form-item>
</template>

<script setup lang="ts">
import type { DynamicField } from '../../api'

const model = defineModel<any>()
const props = defineProps<{ field: DynamicField }>()
const placeholder = computed(() => props.field.config.placeholder || `${props.field.config.required ? '请输入' : '可选：'}${props.field.label}`)
const options = computed(() => (props.field.config.options || []).map((item) => {
  if (typeof item === 'object') {
    const value = item.value
    return { label: item.label, value: typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? value : String(value ?? '') }
  }
  return { label: item, value: item }
}))
</script>
