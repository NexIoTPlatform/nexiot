<template>
  <a-input-number v-if="numeric" v-model="numberValue" :min="numberOption('min')" :max="numberOption('max')" :step="numberOption('step')" :placeholder="placeholder" style="width: 100%">
    <template v-if="schema.unit" #suffix>{{ schema.unit }}</template>
  </a-input-number>
  <a-select v-else-if="fieldType === 'boolean'" v-model="optionValue" :options="booleanOptions" allow-clear :placeholder="placeholder" />
  <a-select v-else-if="fieldType === 'enum'" v-model="optionValue" :options="enumOptions" allow-clear allow-search :placeholder="placeholder" />
  <a-textarea v-else-if="complex" v-model="textValue" :auto-size="{ minRows: 3, maxRows: 7 }" :placeholder="complexPlaceholder" />
  <a-date-picker v-else-if="fieldType === 'date' || fieldType === 'datetime'" v-model="dateValue" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
  <a-input v-else v-model="textValue" :placeholder="placeholder">
    <template v-if="schema.unit" #suffix>{{ schema.unit }}</template>
  </a-input>
</template>

<script setup lang="ts">
import type { ProductMetadataItem } from '@/apis/iot/product'
import { parseRecord } from '../shared'

const model = defineModel<unknown>()
const props = defineProps<{ field: ProductMetadataItem }>()
const schema = computed(() => parseRecord(props.field.valueType))
const fieldType = computed(() => String(schema.value.type || props.field.type || 'string').toLowerCase())
const numeric = computed(() => ['int', 'integer', 'long', 'float', 'double', 'number'].includes(fieldType.value))
const complex = computed(() => ['object', 'array', 'geopoint', 'geoshape'].includes(fieldType.value))
const placeholder = computed(() => props.field.description || `请输入${props.field.name || props.field.id}`)
const complexPlaceholder = computed(() => fieldType.value === 'array' ? '请输入 JSON 数组' : '请输入 JSON 对象')
const booleanOptions = [{ label: '是', value: true }, { label: '否', value: false }]
const enumOptions = computed(() => {
  const elements = Array.isArray(schema.value.elements) ? schema.value.elements : []
  return elements.map((entry: unknown) => {
    const item = parseRecord(entry)
    return { label: String(item.text ?? item.label ?? item.value), value: item.value as string | number }
  })
})
const numberValue = computed<number | undefined>({
  get: () => model.value === undefined || model.value === '' ? undefined : Number(model.value),
  set: (value) => model.value = value
})
const textValue = computed<string>({
  get: () => model.value === undefined || model.value === null ? '' : typeof model.value === 'string' ? model.value : JSON.stringify(model.value),
  set: (value) => model.value = value
})
const dateValue = computed<string | undefined>({
  get: () => model.value ? String(model.value) : undefined,
  set: (value) => model.value = value
})
const optionValue = computed<any>({
  get: () => model.value,
  set: (value) => model.value = value
})

function numberOption(key: 'min' | 'max' | 'step') {
  const value = schema.value[key]
  return value === undefined || value === null || value === '' ? undefined : Number(value)
}
</script>
