<template>
  <section class="subscriptions-panel">
    <a-row justify="space-between" class="g-row-tool"><a-space><a-button type="primary" @click="openForm()"><template #icon><icon-plus /></template>新增订阅</a-button><a-popconfirm content="确定删除选中的订阅吗？" @before-ok="batchDelete"><a-button status="danger" :disabled="!selectedKeys.length"><template #icon><IconDelete /></template>删除</a-button></a-popconfirm></a-space><a-button @click="refresh"><template #icon><icon-refresh /></template>刷新</a-button></a-row>
    <a-table row-key="id" :data="rows" :columns="columns" :loading="loading" :pagination="pagination" :bordered="{ cell: true }" :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="selectedKeys" @select="select" @select-all="selectAll" />
    <a-modal v-model:visible="formVisible" :title="form.id ? '编辑订阅' : '新增订阅'" :mask-closable="false" @before-ok="saveForm" @close="resetForm"><a-form ref="formRef" :model="form" layout="vertical"><a-form-item field="subType" label="订阅级别" :rules="[{ required: true, message: '请选择订阅级别' }] "><a-select v-model="form.subType" :options="levelOptions" /></a-form-item><a-form-item field="msgType" label="消息类型" :rules="[{ required: true, message: '请选择消息类型' }] "><a-select v-model="form.msgType" :options="messageOptions" multiple /></a-form-item><a-form-item field="url" label="推送地址" :rules="[{ required: true, message: '请输入推送地址' }] "><a-input v-model.trim="form.url" placeholder="https://example.com/webhook" /></a-form-item></a-form></a-modal>
  </section>
</template>

<script setup lang="tsx">
/* eslint-disable style/max-statements-per-line */
import type { FormInstance, TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem, DeviceSubscribeItem } from '@/apis/iot/device'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit } from '@arco-design/web-vue/es/icon'
import { addDeviceSubscription, deleteDeviceSubscriptions, getDeviceSubscriptions, updateDeviceSubscription } from '@/apis/iot/device'

defineOptions({ name: 'IotDeviceSubscriptionsPanel' })
const props = defineProps<{ device: DeviceItem }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const loading = ref(false)
const rows = ref<DeviceSubscribeItem[]>([])
const selectedKeys = ref<(string | number)[]>([])
const formVisible = ref(false)
const form = reactive({ id: '', subType: 'DEVICE', msgType: [] as string[], url: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true })
const levelOptions = [{ label: '设备级别', value: 'DEVICE' }, { label: '产品级别', value: 'PRODUCT' }]
const messageOptions = [{ label: '属性', value: 'PROPERTY' }, { label: '事件', value: 'EVENT' }, { label: '状态', value: 'STATE' }]
const columns: TableColumnData[] = [
  { title: '订阅级别', width: 140, render: ({ record }) => <Tag color="arcoblue">{record.subType || '-'}</Tag> },
  { title: '消息类型', width: 240, render: ({ record }) => Array.isArray(record.msgType) ? record.msgType.join('、') : String(record.msgType || '-') },
  { title: '推送地址', dataIndex: 'url', ellipsis: true, tooltip: true },
  { title: '状态', width: 100, render: ({ record }) => <Tag color={String(record.status) === '1' || record.status === 'ENABLED' ? 'green' : 'gray'}>{String(record.status) === '1' || record.status === 'ENABLED' ? '启用' : '停用'}</Tag> },
  { title: '操作', width: 100, align: 'center', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑订阅" onClick={() => openForm(record as DeviceSubscribeItem)}><IconEdit /></Button></Tooltip>
      <Popconfirm content="确定删除该订阅吗？" onBeforeOk={() => remove(record as DeviceSubscribeItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除订阅"><IconDelete /></Button></Tooltip></Popconfirm>
    </Space>
  ) }
]

function normalizeMessageTypes(value: unknown) { return Array.isArray(value) ? value.map(String) : String(value || '').split(',').map((item) => item.trim()).filter(Boolean) }
async function refresh() {
  loading.value = true
  try {
    const res = await getDeviceSubscriptions({ page: pagination.current, size: pagination.pageSize, productKey: props.device.productKey, iotId: props.device.iotId || '' })
    const data = res.data as unknown as { records?: DeviceSubscribeItem[], total?: number } | DeviceSubscribeItem[]
    rows.value = Array.isArray(data) ? data : data.records || []
    pagination.total = Array.isArray(data) ? rows.value.length : Number(data.total || rows.value.length)
  } finally { loading.value = false }
}
function resetForm() {
  Object.assign(form, { id: '', subType: 'DEVICE', msgType: [], url: '' })
  formRef.value?.resetFields()
}
function openForm(row?: DeviceSubscribeItem) {
  resetForm()
  if (row) Object.assign(form, row, { msgType: normalizeMessageTypes(row.msgType) })
  formVisible.value = true
}
async function saveForm() {
  if (await formRef.value?.validate()) return false
  const payload = { ...form, productKey: props.device.productKey, iotId: props.device.iotId }
  await (form.id ? updateDeviceSubscription(payload) : addDeviceSubscription(payload))
  Message.success(form.id ? '订阅保存成功' : '订阅创建成功')
  formVisible.value = false
  refresh()
  return true
}
async function remove(row: DeviceSubscribeItem) {
  await deleteDeviceSubscriptions(props.device.productKey, props.device.iotId || '', [row.id])
  Message.success('订阅删除成功')
  refresh()
  return true
}
async function batchDelete() {
  await deleteDeviceSubscriptions(props.device.productKey, props.device.iotId || '', selectedKeys.value.map(String))
  Message.success('订阅删除成功')
  selectedKeys.value = []
  refresh()
  return true
}
function select(keys: (string | number)[]) { selectedKeys.value = keys }
function selectAll(checked: boolean) { selectedKeys.value = checked ? rows.value.map((item) => item.id) : [] }

refresh()
</script>

<style lang="scss" scoped></style>
