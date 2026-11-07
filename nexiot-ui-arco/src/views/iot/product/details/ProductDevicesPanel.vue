<template>
  <section class="devices-panel device-list-scope">
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['system:instance:add']" type="primary" @click="formDrawerRef?.add()"><template #icon><icon-plus /></template>新增设备</a-button>
        <a-button v-hasPerm="['system:instance:edit']" :disabled="selectedKeys.length !== 1" @click="editSelected"><template #icon><IconEdit /></template>编辑</a-button>
        <a-popconfirm content="确定删除选中的设备吗？" @before-ok="batchDelete"><a-button v-hasPerm="['system:instance:remove']" status="danger" :disabled="!selectedKeys.length"><template #icon><IconDelete /></template>删除</a-button></a-popconfirm>
        <a-button v-hasPerm="['system:instance:import']" @click="importInputRef?.click()"><template #icon><icon-import /></template>导入</a-button>
        <a-button v-hasPerm="['system:instance:export']" @click="handleExport"><template #icon><icon-export /></template>导出</a-button>
        <input ref="importInputRef" class="devices-panel__file" type="file" accept=".xlsx,.xls,.csv" @change="handleImport" />
      </a-space>
      <a-space wrap>
        <a-input v-model="query.deviceId" class="device-list__search" placeholder="DeviceId" allow-clear @press-enter="search" />
        <a-input v-model="query.deviceName" class="device-list__search" placeholder="设备名称" allow-clear @press-enter="search" />
        <a-button type="text" @click="advanced = !advanced">{{ advanced ? '收起' : '展开' }}<template #icon><icon-down v-if="!advanced" /><icon-up v-else /></template></a-button>
        <GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>
    <a-row v-if="advanced" :gutter="12" class="device-list__advanced">
      <a-col :xs="24" :sm="8"><a-input v-model="query.iotId" placeholder="IoT ID" allow-clear @press-enter="search" /></a-col>
      <a-col :xs="24" :sm="8"><a-select v-model="query.state" :options="stateOptions" placeholder="在线状态" allow-clear /></a-col>
      <a-col :xs="24" :sm="8"><a-input v-model="query.deviceTag" placeholder="设备标签值" allow-clear @press-enter="search" /></a-col>
    </a-row>
    <GiTable
      title="设备列表"
      row-key="id"
      :data="deviceList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', minWidth: 1220 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
      @refresh="refresh"
    />
    <ProductDeviceFormDrawer ref="formDrawerRef" :product="product" @save-success="search" />
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ProductDeviceItem, ProductItem } from '@/apis/iot/product'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { deleteProductDevices, exportProductDevices, getProductDeviceList, importProductDevices } from '@/apis/iot/product'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import ProductDeviceFormDrawer from './ProductDeviceFormDrawer.vue'

defineOptions({ name: 'ProductDevicesPanel' })

const props = defineProps<{ product: ProductItem }>()
const router = useRouter()
const formDrawerRef = useTemplateRef<InstanceType<typeof ProductDeviceFormDrawer>>('formDrawerRef')
const importInputRef = useTemplateRef<HTMLInputElement>('importInputRef')
const advanced = ref(false)
const query = reactive({ deviceId: '', deviceName: '', iotId: '', deviceTag: '', state: undefined as number | undefined })
const stateOptions = [{ label: '在线', value: 1 }, { label: '离线', value: 0 }]
const UNIX_TIMESTAMP_PATTERN = /^\d{10,13}$/
const { loading, tableData: deviceList, pagination, selectedKeys, search, refresh, select, selectAll } = useTable<ProductDeviceItem>({
  listAPI: (page) => getProductDeviceList({ ...page, productKey: props.product.productKey, deviceId: query.deviceId.trim() || undefined, deviceName: query.deviceName.trim() || undefined, iotId: query.iotId.trim() || undefined, deviceTag: query.deviceTag.trim() || undefined, state: query.state }),
  deleteAPI: deleteProductDevices,
  immediate: true
})

function reset() {
  Object.assign(query, { deviceId: '', deviceName: '', iotId: '', deviceTag: '', state: undefined })
  search()
}

function editSelected() {
  const id = selectedKeys.value[0]
  if (id !== undefined) formDrawerRef.value?.edit(String(id))
}

async function batchDelete() {
  await deleteProductDevices(selectedKeys.value.map(String))
  Message.success('设备删除成功')
  search()
  return true
}

async function removeDevice(record: ProductDeviceItem) {
  await deleteProductDevices([String(record.id)])
  Message.success('设备删除成功')
  search()
  return true
}

async function handleImport(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  await importProductDevices(file, props.product.productKey)
  Message.success('设备导入任务已提交')
  search()
}

async function handleExport() {
  await exportProductDevices({ productKey: props.product.productKey, deviceId: query.deviceId.trim() || undefined, deviceName: query.deviceName.trim() || undefined, iotId: query.iotId.trim() || undefined, deviceTag: query.deviceTag.trim() || undefined, state: query.state })
  Message.success('设备导出完成')
}

function openDevice(record: ProductDeviceItem) {
  router.push(`/system/instance/instance-details/${record.id}`)
}
function formatDeviceTime(value?: string | number) {
  if (!value || value === '0') return '-'
  const text = String(value).trim()
  const numeric = UNIX_TIMESTAMP_PATTERN.test(text) ? Number(text) : Number.NaN
  const date = Number.isNaN(numeric) ? Dayjs(text) : Dayjs(text.length === 10 ? numeric * 1000 : numeric)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

function fromNow(value?: string | number) {
  if (!value || value === '0') return '从未通信'
  const text = String(value)
  const timestamp = /^\d{10,13}$/.test(text) ? Number(text) * (text.length === 10 ? 1000 : 1) : Dayjs(text).valueOf()
  const diff = Date.now() - timestamp
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 2_592_000_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return `${Math.floor(diff / 2_592_000_000)} 个月前`
}

function nodeLabel(value?: string) {
  return ({ DEVICE: '直', GATEWAY: '网', GATEWAY_SUB_DEVICE: '子', VIDEO_DEVICE: '视' } as Record<string, string>)[value || ''] || value || '-'
}

function isOnline(record: ProductDeviceItem) {
  return record.state === true || record.state === 1 || record.state === '1'
}

const columns: TableColumnData[] = [
  { title: '设备名称 / DeviceId', width: 300, fixed: 'left', render: ({ record }) => (
    <div class="device-cell">
      <div class="device-cell__title">
        <Button type="text" onClick={() => openDevice(record as ProductDeviceItem)}><strong>{record.deviceName || '未命名设备'}</strong></Button>
        {(record.pendingOwner || record.creatorId === 'auto_register') && <Tag size="small" color="orange">待认领</Tag>}
      </div>
      <code>{record.deviceId}</code>
    </div>
  ) },
  { title: '所属产品 / ProductKey', width: 260, render: ({ record }) => (
    <div class="product-cell">
      <strong>{record.productName || props.product.name || '未知产品'}</strong>
      <div>
        <Tag size="small" color="arcoblue">{nodeLabel(record.deviceNode || props.product.deviceNode)}</Tag>
        <code>{record.productKey}</code>
      </div>
    </div>
  ) },
  { title: '设备状态', width: 110, align: 'center', render: ({ record }) => (
    <Tag color={isOnline(record as ProductDeviceItem) ? 'green' : 'red'}>
      <span class="device-status__dot" />
      {isOnline(record as ProductDeviceItem) ? '在线' : '离线'}
    </Tag>
  ) },
  { title: '最近通信', width: 205, fixed: 'right', render: ({ record }) => (
    <div class="communication-time">
      <strong>{formatDeviceTime(record.onlineTime)}</strong>
      <span>
        <i class={isOnline(record as ProductDeviceItem) ? 'online' : 'offline'} />
        {fromNow(record.onlineTime)}
      </span>
    </div>
  ) },
  { title: '归属账号', dataIndex: 'creatorName', width: 140, ellipsis: true, tooltip: true },
  { title: '注册时间', width: 180, render: ({ record }) => formatDeviceTime(record.registryTime) },
  { title: '操作', width: 168, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="详情"><Button type="text" shape="circle" aria-label="详情" onClick={() => openDevice(record as ProductDeviceItem)}><IconEye /></Button></Tooltip>
      {hasPerm('system:instance:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => formDrawerRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
      {hasPerm('system:instance:remove') && <Popconfirm content="确定删除该设备吗？" onBeforeOk={() => removeDevice(record as ProductDeviceItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]
</script>

<style lang="scss" scoped>
.devices-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__file {
    display: none;
  }
}
</style>

<!-- 表格 TSX 单元格共享样式，通过 device-list-scope 限定作用域。 -->
<style lang="scss" src="@/views/iot/instance/modules/deviceList.scss"></style>
