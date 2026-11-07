<template>
  <a-modal v-model:visible="visible" title="升级标签管理" width="min(760px, calc(100% - 20px))" :footer="false" :mask-closable="false" @close="resetForm">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12"><a-form-item field="tagName" label="标签名称" :rules="required('请输入标签名称')"><a-input v-model.trim="form.tagName" /></a-form-item><a-form-item field="tagCode" label="标签编码" :rules="required('请输入标签编码')"><a-input v-model.trim="form.tagCode" /></a-form-item><a-form-item label="颜色"><a-select v-model="form.color" :options="colors" /></a-form-item><a-form-item label="描述"><a-input v-model.trim="form.description" /></a-form-item></a-grid>
      <a-row justify="end"><a-space><a-button v-if="form.id" @click="resetForm">取消编辑</a-button><a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>{{ form.id ? '保存标签' : '新增标签' }}</a-button></a-space></a-row>
    </a-form>
    <a-divider />
    <a-table row-key="id" :data="tags" :columns="columns" :loading="loading" :pagination="false" :bordered="{ cell: true }" />
  </a-modal>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { FormInstance, TableColumnData } from '@arco-design/web-vue'
import type { OtaTagItem } from '@/apis/iot/ota'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit } from '@arco-design/web-vue/es/icon'
import { addOtaTag, deleteOtaTag, getOtaTagList, updateOtaTag } from '@/apis/iot/ota'

defineOptions({ name: 'IotOtaTagManagerModal' })
const emit = defineEmits<{ (e: 'change', tags: OtaTagItem[]): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const tags = ref<OtaTagItem[]>([])
const form = reactive(createForm())
const colors = ['blue', 'green', 'orange', 'red', 'purple', 'cyan', 'magenta'].map((value) => ({ label: value, value }))
const required = (message: string) => [{ required: true, message }]
function createForm() { return { id: '', tagName: '', tagCode: '', color: 'blue', description: '' } }
function resetForm() {
  Object.assign(form, createForm())
  formRef.value?.resetFields()
}
async function load() {
  loading.value = true
  try {
    tags.value = (await getOtaTagList({ page: 1, size: 500 })).data.records || []
    emit('change', tags.value)
  } finally { loading.value = false }
}
async function open() {
  resetForm()
  visible.value = true
  await load()
}
function edit(row: OtaTagItem) { Object.assign(form, row, { id: String(row.id) }) }
async function save() {
  if (await formRef.value?.validate()) return
  if (tags.value.some((item) => item.tagCode === form.tagCode && item.id !== form.id)) {
    Message.error('标签编码已存在')
    return
  } saving.value = true
  try {
    await (form.id ? updateOtaTag(form) : addOtaTag(form))
    Message.success(form.id ? '标签保存成功' : '标签新增成功')
    resetForm()
    await load()
  } finally { saving.value = false }
}
async function remove(row: OtaTagItem) {
  await deleteOtaTag(row.id)
  Message.success('标签删除成功')
  await load()
  return true
}
const columns: TableColumnData[] = [{ title: '标签名称', width: 150, render: ({ record }) => <Tag color={record.color || 'arcoblue'}>{record.tagName}</Tag> }, { title: '标签编码', dataIndex: 'tagCode', width: 160 }, { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true }, { title: '操作', width: 100, align: 'center', render: ({ record }) => (
  <Space size={2}>
    <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑标签" onClick={() => edit(record as OtaTagItem)}><IconEdit /></Button></Tooltip>
    <Popconfirm content="确定删除该标签吗？" onBeforeOk={() => remove(record as OtaTagItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除标签"><IconDelete /></Button></Tooltip></Popconfirm>
  </Space>
) }]
defineExpose({ open, load, tags })
</script>

<style lang="scss" scoped></style>
