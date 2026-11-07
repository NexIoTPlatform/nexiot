<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 32px)" :modal-style="{ maxWidth: '760px' }" :body-style="{ maxHeight: '70vh', overflowY: 'auto' }" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <a-form-item field="name" :label="`${typeLabel}名称`" :rules="[{ required: true, message: `请输入${typeLabel}名称` }]">
          <a-auto-complete v-if="!oldId" v-model="form.name" :data="standardFieldOptions" :filter-option="false" :placeholder="`输入${typeLabel}名称搜索标准字段，也可自定义`" allow-clear @search="searchStandardField" @select="applyStandardField" />
          <a-input v-else v-model="form.name" :placeholder="`请输入${typeLabel}名称`" />
        </a-form-item>
        <a-form-item field="id" :label="`${typeLabel}标识`" :rules="identifierRules">
          <a-input v-model="form.id" placeholder="字母开头，仅支持字母、数字和下划线" />
        </a-form-item>
      </a-grid>

      <a-grid v-if="fieldType === 'properties'" :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <a-form-item label="读写权限"><a-radio-group v-model="form.mode" type="button"><a-radio value="r">只读</a-radio><a-radio value="rw">读写</a-radio></a-radio-group></a-form-item>
        <a-form-item label="属性来源"><a-select v-model="form.source" :options="sourceOptions" /></a-form-item>
      </a-grid>
      <a-form-item v-if="fieldType === 'events'" label="告警等级"><a-radio-group v-model="form.level" type="button"><a-radio value="ordinary">普通</a-radio><a-radio value="warn">提醒</a-radio><a-radio value="urgent">紧急</a-radio></a-radio-group></a-form-item>
      <a-form-item v-if="fieldType === 'functions'" label="功能类型"><a-checkbox v-model="form.config">配置型功能</a-checkbox></a-form-item>
      <a-form-item label="描述"><a-textarea v-model="form.description" :auto-size="{ minRows: 2, maxRows: 5 }" :placeholder="`请输入${typeLabel}描述`" /></a-form-item>

      <section v-if="fieldType !== 'functions'" class="schema-section">
        <h4>类型定义</h4>
        <MetadataValueTypeEditor :model-value="valueType" @update:model-value="replaceValueType" />
      </section>

      <section v-else class="schema-section">
        <div class="schema-section__title"><h4>输入参数</h4><a-button type="text" @click="addInput"><template #icon><icon-plus /></template>添加参数</a-button></div>
        <a-empty v-if="!form.inputs.length" description="暂无输入参数" />
        <div v-for="(input, index) in form.inputs" :key="index" class="input-editor">
          <header><strong>参数 {{ index + 1 }}</strong><a-button type="text" status="danger" aria-label="删除参数" @click="form.inputs.splice(index, 1)"><icon-delete /></a-button></header>
          <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="12">
            <a-form-item label="参数名称"><a-input v-model="input.name" placeholder="参数名称" /></a-form-item>
            <a-form-item label="参数标识"><a-input v-model="input.id" placeholder="参数标识" /></a-form-item>
          </a-grid>
          <a-form-item label="参数描述"><a-input v-model="input.description" placeholder="可选" /></a-form-item>
          <MetadataValueTypeEditor v-model="input.valueType" />
        </div>
      </section>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { FieldType, ValueTypeForm } from './metadataSchema'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { StandardFieldItem, StandardFieldType } from '@/apis/iot/standardField'
import { Message } from '@arco-design/web-vue'
import { debounce } from 'lodash-es'
import { addProductMetadata, editProductMetadata } from '@/apis/iot/product'
import { searchStandardFields } from '@/apis/iot/standardField'
import { deepClone } from '@/utils'
import { buildValueType, createValueType, IDENTIFIER_RE, normalizeValueType, validateMetadataItems } from './metadataSchema'
import MetadataValueTypeEditor from './MetadataValueTypeEditor.vue'
import { standardFieldToMetadata } from './standardFieldMetadata'

defineOptions({ name: 'MetadataFieldModal' })

const props = defineProps<{ productKey?: string, saveField?: (type: FieldType, item: ProductMetadataItem, oldId: string) => boolean | void | Promise<boolean | void> }>()
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const fieldType = ref<FieldType>('properties')
const oldId = ref('')
const original = ref<ProductMetadataItem>()
const standardFields = ref<StandardFieldItem[]>([])
const standardFieldOptions = computed(() => standardFields.value.map((item) => ({ value: String(item.id), label: `${item.fieldName}（${item.fieldId}）${item.dataType ? ` · ${item.dataType}` : ''}` })))
let standardSearchId = 0
const form = reactive({ id: '', name: '', description: '', mode: 'rw', source: 'device', level: 'ordinary', config: false, inputs: [] as (ProductMetadataItem & { valueType: ValueTypeForm })[] })
const valueType = reactive<ValueTypeForm>(createValueType('string'))
const typeLabels: Record<FieldType, string> = { properties: '属性', events: '事件', functions: '功能' }
const typeLabel = computed(() => typeLabels[fieldType.value])
const title = computed(() => `${oldId.value ? '编辑' : '添加'}${typeLabel.value}`)
const identifierRules = [{ required: true, message: '请输入标识' }, { match: IDENTIFIER_RE, message: '标识必须以字母开头，仅支持字母、数字和下划线' }]
const sourceOptions = [{ label: '设备', value: 'device' }, { label: '扩展', value: 'extended' }]
const standardFieldTypes: Record<FieldType, StandardFieldType> = { properties: 'property', events: 'event', functions: 'function' }
const loadStandardFields = debounce(async (keyword: string, requestId: number, type: StandardFieldType) => {
  if (requestId !== standardSearchId) return
  try {
    const res = await searchStandardFields(keyword, type)
    if (requestId === standardSearchId) standardFields.value = res.data || []
  } catch {
    if (requestId === standardSearchId) standardFields.value = []
  }
}, 250)

function searchStandardField(value: string) {
  const requestId = ++standardSearchId
  standardFields.value = []
  loadStandardFields.cancel()
  if (value.trim() && !oldId.value) loadStandardFields(value.trim(), requestId, standardFieldTypes[fieldType.value])
}

async function applyStandardField(id: string) {
  const field = standardFields.value.find((item) => String(item.id) === id)
  if (!field) return
  loadStandardFields.cancel()
  const requestId = ++standardSearchId
  await nextTick()
  if (!visible.value || requestId !== standardSearchId) return
  try {
    const record = standardFieldToMetadata(field)
    fillRecord(record)
    standardFields.value = []
    formRef.value?.clearValidate()
  } catch (error) {
    form.name = field.fieldName
    Message.error(error instanceof Error ? error.message : '标准字段配置无效，请检查')
  }
}

function replaceValueType(next: ValueTypeForm) {
  Object.keys(valueType).forEach((key) => delete (valueType as unknown as Record<string, unknown>)[key])
  Object.assign(valueType, next)
}

function parseValueType(record?: ProductMetadataItem) {
  replaceValueType(normalizeValueType(record?.valueType || record?.type || 'string'))
}

function reset() {
  Object.assign(form, { id: '', name: '', description: '', mode: 'rw', source: 'device', level: 'ordinary', config: false, inputs: [] })
  replaceValueType(createValueType('string'))
  oldId.value = ''
  original.value = undefined
  standardFields.value = []
  standardSearchId += 1
  loadStandardFields.cancel()
  formRef.value?.clearValidate()
}

function fillRecord(record: ProductMetadataItem) {
  original.value = deepClone(record)
  Object.assign(form, {
    id: record.id,
    name: record.name,
    description: record.description || '',
    mode: record.mode || 'rw',
    source: record.source || 'device',
    level: record.expands?.level || 'ordinary',
    config: Boolean(record.config),
    inputs: (Array.isArray(record.inputs) ? record.inputs : []).map((item) => ({ ...deepClone(item), description: item.description || '', valueType: normalizeValueType(item.valueType || item.type || 'string') }))
  })
  parseValueType(record)
}

function open(type: FieldType, record?: ProductMetadataItem) {
  reset()
  fieldType.value = type
  if (record) {
    oldId.value = record.id
    fillRecord(record)
  }
  visible.value = true
}

function addInput() {
  form.inputs.push({ id: '', name: '', description: '', valueType: createValueType('string') })
}

function buildPayload(): ProductMetadataItem {
  const base: ProductMetadataItem = { ...original.value, id: form.id.trim(), name: form.name.trim(), description: form.description.trim() }
  delete base.type
  if (fieldType.value === 'properties') return { ...base, mode: form.mode, source: form.source, valueType: buildValueType(valueType) }
  if (fieldType.value === 'events') return { ...base, expands: { ...original.value?.expands, level: form.level }, valueType: buildValueType(valueType) }
  return { ...base, config: form.config, inputs: form.inputs.map((item) => {
    const { type: _type, ...input } = item
    return { ...input, id: item.id.trim(), name: item.name.trim(), description: item.description?.trim(), valueType: buildValueType(item.valueType) }
  }) }
}

async function save() {
  const errors = await formRef.value?.validate()
  if (errors) return false
  const payload = buildPayload()
  const schemaError = validateMetadataItems([payload], fieldType.value, typeLabel.value)
  if (schemaError) {
    Message.error(schemaError)
    return false
  }
  if (props.saveField) {
    if (await props.saveField(fieldType.value, payload, oldId.value) === false) return false
  } else {
    if (!props.productKey) return false
    const data = { productKey: props.productKey, type: fieldType.value, ioTProductMetadataBO: payload }
    if (oldId.value) await editProductMetadata({ ...data, oldId: oldId.value })
    else await addProductMetadata(data)
  }
  Message.success(`${typeLabel.value}${oldId.value ? '保存' : '添加'}成功`)
  emit('save-success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.schema-section {
  padding: 14px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-1);
  border-radius: 6px;

  h4 {
    margin: 0 0 14px;
    font-size: 14px;
  }

  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.input-editor {
  padding: 12px;
  margin-top: 12px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  > header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
}
</style>
