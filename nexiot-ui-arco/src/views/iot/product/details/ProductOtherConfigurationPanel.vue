<template>
  <section class="other-config">
    <header class="other-config__header">
      <div><strong><icon-safe /> 第三方平台参数</strong><p>定义设备创建或认证时需要填写的平台参数。</p></div>
      <a-button v-hasPerm="['system:product:edit']" type="primary" @click="openField()"><template #icon><icon-plus /></template>添加参数</a-button>
    </header>
    <a-empty v-if="!fields.length" description="暂无第三方平台参数" />
    <a-table v-else row-key="id" :data="fields" :columns="columns" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 850 }" />

    <a-drawer v-model:visible="visible" :title="editIndex < 0 ? '添加平台参数' : '编辑平台参数'" width="520px" :mask-closable="false" @before-ok="saveField" @close="reset">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-alert v-if="form.system" type="info" class="system-field-alert">系统字段由接入协议维护，仅允许查看。</a-alert>
        <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="12">
          <a-form-item field="id" label="字段 ID" :rules="identifierRules"><a-input v-model="form.id" :disabled="form.system" placeholder="如 securityCode" /></a-form-item>
          <a-form-item field="name" label="显示名称" :rules="[{ required: true, message: '请输入显示名称' }]"><a-input v-model="form.name" :disabled="form.system" placeholder="如 安全码" /></a-form-item>
          <a-form-item label="字段类型"><a-select v-model="form.type" :options="typeOptions" /></a-form-item>
          <a-form-item label="必填"><a-switch v-model="form.required" /></a-form-item>
        </a-grid>
        <a-form-item label="字段描述"><a-textarea v-model="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" /></a-form-item>
        <a-form-item label="占位提示"><a-input v-model="form.placeholder" /></a-form-item>
        <a-grid v-if="form.type === 'number'" :cols="2" :col-gap="12"><a-form-item label="最小值"><a-input-number v-model="form.min" /></a-form-item><a-form-item label="最大值"><a-input-number v-model="form.max" /></a-form-item></a-grid>
        <a-grid v-if="['string', 'textarea', 'password'].includes(form.type)" :cols="{ xs: 1, md: 2 }" :col-gap="12"><a-form-item label="最小长度"><a-input-number v-model="form.minLength" :min="0" /></a-form-item><a-form-item label="最大长度"><a-input-number v-model="form.maxLength" :min="1" /></a-form-item></a-grid>
        <a-form-item v-if="['string', 'textarea', 'password'].includes(form.type)" label="正则校验"><a-input v-model="form.pattern" placeholder="可选，输入正则表达式" /></a-form-item>
        <a-form-item v-if="form.type === 'number'" label="自动生成设备 ID"><a-switch v-model="form.autoGenerate" /><template #extra>用于网关子设备时，可根据字段值生成设备 ID。</template></a-form-item>
        <a-form-item v-if="form.type === 'select'" label="选项">
          <div class="option-editor">
            <a-space v-for="(option, index) in form.options" :key="index" fill><a-input v-model="option.value" placeholder="选项值" /><a-input v-model="option.label" placeholder="显示文本" /><a-button type="text" status="danger" shape="circle" @click="form.options.splice(index, 1)"><IconDelete /></a-button></a-space>
            <a-button type="text" @click="form.options.push({ value: '', label: '' })"><template #icon><icon-plus /></template>添加选项</a-button>
          </div>
        </a-form-item>
        <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="12"><a-form-item label="默认禁用"><a-switch v-model="form.disabled" /></a-form-item><a-form-item label="默认隐藏"><a-switch v-model="form.hidden" /></a-form-item></a-grid>
        <a-form-item label="帮助文本"><a-input v-model="form.helpText" /></a-form-item>
      </a-form>
    </a-drawer>
  </section>
</template>

<script setup lang="tsx">
import type { FormInstance, TableColumnData } from '@arco-design/web-vue'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit } from '@arco-design/web-vue/es/icon'
import { updateProductOtherConfig } from '@/apis/iot/product'

defineOptions({ name: 'ProductOtherConfigurationPanel' })

const props = defineProps<{ product: ProductItem }>()

const emit = defineEmits<{ (e: 'refresh'): void }>()

interface OtherField {
  id: string
  name: string
  type: string
  required: boolean
  description: string
  placeholder: string
  min?: number
  max?: number
  minLength?: number
  maxLength?: number
  pattern: string
  options: { value: string, label: string }[]
  disabled: boolean
  hidden: boolean
  helpText: string
  system: boolean
  autoGenerate: boolean
}

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const editIndex = ref(-1)
const fields = ref<OtherField[]>([])
const form = reactive<OtherField>(createField())
const identifierRules = [{ required: true, message: '请输入字段 ID' }, { match: /^[A-Z]\w*$/i, message: '字段 ID 必须以字母开头，仅支持字母、数字和下划线' }]
const typeOptions = [{ label: '文本', value: 'string' }, { label: '多行文本', value: 'textarea' }, { label: '密码', value: 'password' }, { label: '数字', value: 'number' }, { label: '下拉选择', value: 'select' }, { label: '开关', value: 'boolean' }]
const columns: TableColumnData[] = [
  { title: '字段 ID', dataIndex: 'id', width: 180 },
  { title: '显示名称', dataIndex: 'name', width: 160 },
  { title: '类型', width: 110, render: ({ record }) => <Tag>{typeOptions.find((item) => item.value === record.type)?.label || record.type}</Tag> },
  { title: '来源', width: 90, align: 'center', render: ({ record }) => record.system ? <Tag color="arcoblue">系统</Tag> : '自定义' },
  { title: '必填', width: 80, align: 'center', render: ({ record }) => record.required ? '是' : '否' },
  { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true },
  { title: '操作', width: 100, align: 'center', fixed: 'right', render: ({ record, rowIndex }) => (
    <Space size={2}>
      <Tooltip content={record.system ? '系统字段不可编辑' : '编辑'}><Button type="text" shape="circle" disabled={record.system} aria-label="编辑" onClick={() => openField(record as OtherField, rowIndex)}><IconEdit /></Button></Tooltip>
      {record.system
        ? <Tooltip content="系统字段不可删除"><Button type="text" status="danger" shape="circle" disabled aria-label="删除"><IconDelete /></Button></Tooltip>
        : <Popconfirm content="确定删除该参数吗？" onBeforeOk={() => removeField(rowIndex)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]

function createField(): OtherField {
  return { id: '', name: '', type: 'string', required: false, description: '', placeholder: '', pattern: '', options: [], disabled: false, hidden: false, helpText: '', system: false, autoGenerate: false }
}

function cloneField(field: OtherField): OtherField {
  return {
    ...field,
    options: (field.options || []).map((option) => ({ ...option }))
  }
}

function parseFields(value?: string) {
  try {
    const result = JSON.parse(value || '{}') as { customField?: OtherField[] }
    return Array.isArray(result.customField) ? result.customField.map((field) => cloneField({ ...createField(), ...field })) : []
  } catch {
    return []
  }
}

function reset() {
  Object.assign(form, createField())
  editIndex.value = -1
  formRef.value?.resetFields()
}

function openField(field?: OtherField, index = -1) {
  reset()
  if (field) Object.assign(form, cloneField(field))
  editIndex.value = index
  visible.value = true
}

async function persist(next: OtherField[]) {
  await updateProductOtherConfig({ productId: props.product.id, customField: next.map((field) => cloneField(field)) })
  fields.value = next
  emit('refresh')
}

async function saveField() {
  if (await formRef.value?.validate()) return false
  if (form.type === 'number' && form.min !== undefined && form.max !== undefined && form.min > form.max) {
    Message.error('最小值不能大于最大值')
    return false
  }
  const duplicate = fields.value.some((item, index) => item.id === form.id && index !== editIndex.value)
  if (duplicate) {
    Message.error('字段 ID 已存在')
    return false
  }
  const next = [...fields.value]
  const value = cloneField(form)
  if (editIndex.value < 0) next.push(value)
  else next[editIndex.value] = value
  await persist(next)
  Message.success(editIndex.value < 0 ? '平台参数添加成功' : '平台参数保存成功')
  return true
}

async function removeField(index: number) {
  const next = fields.value.filter((_, rowIndex) => rowIndex !== index)
  await persist(next)
  Message.success('平台参数删除成功')
  return true
}

watch(() => props.product.thirdConfiguration, (value) => {
  fields.value = parseFields(value)
}, { immediate: true })
</script>

<style lang="scss" scoped>
.other-config {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 14px;

    strong {
      display: flex;
      gap: 7px;
      align-items: center;
      font-size: 15px;
    }

    p {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}

.option-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
