<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 32px)" :modal-style="{ maxWidth: '760px' }" :body-style="{ maxHeight: '70vh', overflowY: 'auto' }" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <a-form-item label="字段类型" field="fieldType" :rules="[{ required: true, message: '请选择字段类型' }]">
          <a-select v-model="form.fieldType" :options="fieldTypeOptions" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="状态" field="status" :rules="[{ required: true, message: '请选择状态' }]">
          <a-radio-group v-model="form.status" type="button">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="字段标识" field="fieldId" :rules="fieldIdRules">
          <a-input v-model="form.fieldId" placeholder="例如 temperature" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="字段名称" field="fieldName" :rules="[{ required: true, message: '请输入字段名称' }]">
          <a-input v-model="form.fieldName" placeholder="请输入字段名称" :max-length="64" />
        </a-form-item>
        <a-form-item v-if="form.fieldType === 'property'" label="读写模式" field="mode">
          <a-select v-model="form.mode" :options="modeOptions" />
        </a-form-item>
        <a-form-item v-if="form.fieldType === 'property'" label="属性来源" field="source">
          <a-select v-model="form.source" :options="sourceOptions" />
        </a-form-item>
        <a-form-item v-if="form.fieldType === 'event'" label="告警等级" field="level">
          <a-radio-group v-model="form.level" type="button"><a-radio value="ordinary">普通</a-radio><a-radio value="warn">提醒</a-radio><a-radio value="urgent">紧急</a-radio></a-radio-group>
        </a-form-item>
        <a-form-item v-if="form.fieldType === 'function'" label="功能类型" field="config">
          <a-checkbox v-model="form.config">配置型功能</a-checkbox>
        </a-form-item>
        <a-form-item label="推荐优先级" field="recommendPriority">
          <a-input-number v-model="form.recommendPriority" :min="0" :max="9999" />
        </a-form-item>
      </a-grid>
      <a-form-item label="搜索关键词" field="searchKeywords">
        <a-input v-model="form.searchKeywords" placeholder="多个关键词使用英文逗号分隔" :max-length="200" />
      </a-form-item>
      <a-form-item label="字段描述" field="description">
        <a-textarea v-model="form.description" :max-length="300" show-word-limit :auto-size="{ minRows: 2, maxRows: 4 }" />
      </a-form-item>

      <section v-if="form.fieldType !== 'function'" class="standard-field-schema">
        <h4>{{ form.fieldType === 'event' ? '事件输出类型' : '类型定义' }}</h4>
        <MetadataValueTypeEditor v-model="valueType" />
      </section>
      <section v-else class="standard-field-schema">
        <div class="standard-field-schema__title"><h4>输入参数</h4><a-button type="text" @click="addInput"><template #icon><icon-plus /></template>添加参数</a-button></div>
        <a-empty v-if="!form.inputs.length" description="暂无输入参数" />
        <div v-for="(input, index) in form.inputs" :key="index" class="standard-field-input">
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
import type { FieldRule, FormInstance } from '@arco-design/web-vue'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { StandardFieldItem, StandardFieldType } from '@/apis/iot/standardField'
import type { FieldType, ValueTypeForm } from '@/views/iot/product/details/metadataSchema'
import { Message } from '@arco-design/web-vue'
import { addStandardField, getStandardField, updateStandardField } from '@/apis/iot/standardField'
import { deepClone } from '@/utils'
import { buildValueType, createValueType, IDENTIFIER_RE, normalizeValueType, validateMetadataItems } from '@/views/iot/product/details/metadataSchema'
import MetadataValueTypeEditor from '@/views/iot/product/details/MetadataValueTypeEditor.vue'
import { standardFieldToMetadata } from '@/views/iot/product/details/standardFieldMetadata'

defineOptions({ name: 'StandardFieldFormModal' })

const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const original = ref<ProductMetadataItem>()
const valueType = ref<ValueTypeForm>(createValueType('string'))
const form = reactive({
  id: '',
  fieldType: 'property' as StandardFieldType,
  fieldId: '',
  fieldName: '',
  mode: 'rw',
  source: 'device',
  level: 'ordinary',
  config: false,
  inputs: [] as (ProductMetadataItem & { valueType: ValueTypeForm })[],
  description: '',
  searchKeywords: '',
  recommendPriority: 0,
  status: 1
})
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑标准字段' : '新增标准字段')
const fieldTypeOptions = [
  { label: '属性', value: 'property' },
  { label: '事件', value: 'event' },
  { label: '功能', value: 'function' }
]
const metadataTypes: Record<StandardFieldType, FieldType> = { property: 'properties', event: 'events', function: 'functions' }
const modeOptions = [{ label: '只读', value: 'r' }, { label: '只写', value: 'w' }, { label: '读写', value: 'rw' }]
const sourceOptions = [{ label: '设备', value: 'device' }, { label: '扩展', value: 'extended' }]
const fieldIdRules: FieldRule[] = [
  { required: true, message: '请输入字段标识' },
  { match: IDENTIFIER_RE, message: '须以字母开头，只能包含字母、数字和下划线' }
]

function reset() {
  Object.assign(form, {
    id: '',
    fieldType: 'property',
    fieldId: '',
    fieldName: '',
    mode: 'rw',
    source: 'device',
    level: 'ordinary',
    config: false,
    inputs: [],
    description: '',
    searchKeywords: '',
    recommendPriority: 0,
    status: 1
  })
  original.value = undefined
  valueType.value = createValueType('string')
  formRef.value?.clearValidate()
}

function add(type: StandardFieldType = 'property') {
  reset()
  form.fieldType = type
  visible.value = true
}

async function edit(id: string) {
  reset()
  const data = (await getStandardField(id)).data
  try {
    const metadata = standardFieldToMetadata(data)
    original.value = deepClone(metadata)
    Object.assign(form, {
      id: String(data.id),
      fieldType: data.fieldType,
      fieldId: metadata.id,
      fieldName: metadata.name,
      mode: metadata.mode || 'rw',
      source: metadata.source || 'device',
      level: metadata.expands?.level || 'ordinary',
      config: Boolean(metadata.config),
      inputs: (metadata.inputs || []).map((input) => ({ ...deepClone(input), description: input.description || '', valueType: normalizeValueType(input.valueType || input.type || 'string') })),
      description: metadata.description || '',
      searchKeywords: data.searchKeywords || '',
      recommendPriority: data.recommendPriority || 0,
      status: data.status
    })
    valueType.value = normalizeValueType(metadata.valueType || metadata.type || 'string')
    visible.value = true
  } catch (cause) {
    Message.error(`标准字段配置无效：${(cause as Error).message}`)
  }
}

function addInput() {
  form.inputs.push({ id: '', name: '', description: '', valueType: createValueType('string') })
}

function buildMetadata(): ProductMetadataItem {
  const base: ProductMetadataItem = { ...original.value, id: form.fieldId.trim(), name: form.fieldName.trim(), description: form.description.trim() }
  delete base.type
  if (form.fieldType === 'property') return { ...base, mode: form.mode, source: form.source, valueType: buildValueType(valueType.value) }
  if (form.fieldType === 'event') return { ...base, expands: { ...base.expands, level: form.level }, valueType: buildValueType(valueType.value) }
  return {
    ...base,
    config: form.config,
    inputs: form.inputs.map((item) => {
      const { type: _type, ...input } = item
      return { ...input, id: item.id.trim(), name: item.name.trim(), description: item.description?.trim(), valueType: buildValueType(item.valueType) }
    })
  }
}

async function save() {
  if (await formRef.value?.validate()) return false
  const metadata = buildMetadata()
  const schemaError = validateMetadataItems([metadata], metadataTypes[form.fieldType], '标准字段')
  if (schemaError) {
    Message.error(schemaError)
    return false
  }
  const type = form.fieldType === 'function' ? undefined : buildValueType(valueType.value)
  const payload: Partial<StandardFieldItem> = {
    id: form.id || undefined,
    fieldType: form.fieldType,
    fieldId: metadata.id,
    fieldName: metadata.name,
    description: metadata.description,
    searchKeywords: form.searchKeywords.trim(),
    recommendPriority: form.recommendPriority,
    status: form.status,
    mode: form.fieldType === 'property' ? form.mode : '',
    dataType: type ? String(type.type) : '',
    unit: type?.unit ? String(type.unit) : '',
    valueTypeConfig: JSON.stringify(type || {}),
    metadataConfig: JSON.stringify(metadata)
  }
  await (isEdit.value ? updateStandardField(payload) : addStandardField(payload))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.standard-field-schema {
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

.standard-field-input {
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
