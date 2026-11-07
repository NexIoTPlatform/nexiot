<template>
  <div class="metadata-input">
    <div><strong>{{ model.name || model.id }}</strong><code>{{ model.id }}</code></div>
    <a-input-number v-if="numeric" v-model="numericParam" :precision="integer ? 0 : undefined" :min="valueType.min" :max="valueType.max" :placeholder="model.description || '请输入参数'" style="width: 100%">
      <template v-if="valueType.unit" #suffix>{{ valueType.unit }}</template>
    </a-input-number>
    <a-select v-else-if="type === 'boolean' || type === 'bool'" v-model="optionParam" :options="booleanOptions" placeholder="请选择" />
    <a-select v-else-if="type === 'enum'" v-model="optionParam" :options="enumOptions" placeholder="请选择" />
    <a-input v-else v-model="textParam" :disabled="fixed" :placeholder="model.description || '请输入参数'">
      <template v-if="valueType.unit" #suffix>{{ valueType.unit }}</template>
    </a-input>
  </div>
</template>

<script setup lang="ts">
import type { SceneActionParam } from '../api'
import type { RuntimeField } from '@/views/iot/instance/details/modules/deviceCommand'
import { coerceRuntimeValue, enumOptions as commandEnumOptions, fieldSchema, fieldType, isNumberField } from '@/views/iot/instance/details/modules/deviceCommand'

const model = defineModel<SceneActionParam>({ required: true })
const field = computed(() => model.value as RuntimeField)
const valueType = computed<Record<string, any>>(() => fieldSchema(field.value))
const type = computed(() => fieldType(field.value))
const numeric = computed(() => isNumberField(field.value))
const integer = computed(() => ['int', 'integer', 'short', 'byte', 'long'].includes(type.value))
const fixed = computed(() => ['childDeviceId', 'deviceId', 'meterNo', 'imei'].includes(model.value.id) && model.value.params !== undefined)
const numericParam = computed<number | undefined>({
  get: () => model.value.params === undefined || model.value.params === '' ? undefined : Number(model.value.params),
  set: (value) => { model.value.params = value }
})
const optionParam = computed<string | number | boolean | Record<string, any> | (string | number | boolean | Record<string, any>)[] | undefined>({
  get: () => coerceRuntimeValue(field.value, model.value.params),
  set: (value) => { model.value.params = coerceRuntimeValue(field.value, value) }
})
const textParam = computed<string>({
  get: () => model.value.params === undefined || model.value.params === null ? '' : String(model.value.params),
  set: (value) => { model.value.params = value }
})
const booleanOptions = [{ label: '是', value: true }, { label: '否', value: false }]
const enumOptions = computed(() => commandEnumOptions(field.value))
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.metadata-input { display: grid; grid-template-columns: minmax(160px, .8fr) minmax(220px, 1.2fr); gap: 12px; align-items: center; min-height: 56px; padding: 8px 0; border-bottom: 1px solid var(--color-border-2); }
.metadata-input > div { display: flex; flex-direction: column; gap: 3px; }
.metadata-input code { font-size: 12px; color: var(--color-text-3); }

@media (width <= 576px) { .metadata-input { grid-template-columns: 1fr; } }
</style>
