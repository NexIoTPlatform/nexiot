<template>
  <div class="device-bind">
    <a-form :model="query" layout="inline" class="device-bind__filters">
      <a-form-item label="设备编号">
        <a-input v-model.trim="query.deviceId" allow-clear placeholder="DeviceId" @press-enter="search" />
      </a-form-item>
      <a-form-item label="设备名称">
        <a-input v-model.trim="query.deviceName" allow-clear placeholder="设备名称" @press-enter="search" />
      </a-form-item>
      <a-form-item label="产品">
        <a-select
          v-model="query.productKey"
          :options="productOptions"
          allow-clear
          allow-search
          placeholder="产品名称 / ProductKey"
          style="width: 220px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model="query.state" :options="stateOptions" allow-clear placeholder="在线状态" />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="search"><template #icon><icon-search /></template>查询</a-button>
          <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['application:application:bind']" type="primary" @click="openBind"><template #icon><icon-link /></template>绑定设备</a-button>
        <a-popconfirm v-hasPerm="['application:application:unbind']" content="确定解绑选中的设备吗？" @before-ok="unbindSelected">
          <a-button status="danger" :disabled="!selectedKeys.length"><template #icon><IconClose /></template>批量解绑</a-button>
        </a-popconfirm>
        <a-button v-hasPerm="['application:application:bind']" @click="openGroup"><template #icon><icon-folder-add /></template>按组绑定</a-button>
        <a-popconfirm v-hasPerm="['system:instance:export']" content="将导出当前筛选条件下的全部已绑定设备，确定继续吗？" @before-ok="exportDevices">
          <a-button><template #icon><icon-download /></template>导出</a-button>
        </a-popconfirm>
      </a-space>
      <a-button shape="circle" :loading="loading" aria-label="刷新" @click="load"><icon-refresh /></a-button>
    </a-row>

    <a-table
      row-key="id"
      :data="rows"
      :loading="loading"
      :columns="columns"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', minWidth: 920 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="(keys) => selectedKeys = keys.map(String)"
      @select-all="(checked) => selectAll(checked)"
      @page-change="changePage"
      @page-size-change="changePageSize"
    />

    <a-modal
      v-model:visible="bindVisible"
      title="选择未绑定设备"
      width="min(1040px, calc(100vw - 32px))"
      :mask-closable="false"
      :body-style="modalBodyStyle"
      align-center
      @before-ok="bindSelected"
    >
      <a-form :model="availableQuery" layout="inline" class="device-bind__filters">
        <a-form-item label="设备编号">
          <a-input v-model.trim="availableQuery.deviceId" allow-clear @press-enter="searchAvailable" />
        </a-form-item>
        <a-form-item label="设备名称">
          <a-input v-model.trim="availableQuery.deviceName" allow-clear @press-enter="searchAvailable" />
        </a-form-item>
        <a-form-item label="产品">
          <a-select v-model="availableQuery.productKey" :options="productOptions" allow-clear allow-search style="width: 210px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model="availableQuery.state" :options="stateOptions" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="searchAvailable"><template #icon><icon-search /></template>查询</a-button>
            <a-button @click="resetAvailable"><template #icon><icon-refresh /></template>重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
      <a-table
        row-key="id"
        :data="availableRows"
        :loading="availableLoading"
        :columns="availableColumns"
        :pagination="availablePagination"
        :bordered="{ cell: true }"
        :scroll="{ x: '100%', minWidth: 760 }"
        :row-selection="{ type: 'checkbox', showCheckedAll: true }"
        :selected-keys="availableSelectedKeys"
        @select="(keys) => availableSelectedKeys = keys.map(String)"
        @select-all="(checked) => selectAllAvailable(checked)"
        @page-change="changeAvailablePage"
        @page-size-change="changeAvailablePageSize"
      />
    </a-modal>

    <a-modal v-model:visible="groupVisible" title="按设备组绑定" width="min(640px, calc(100vw - 32px))" :body-style="groupModalBodyStyle" align-center @before-ok="bindGroup">
      <a-alert type="info" class="device-bind__group-tip">
        将绑定该分组下的全部设备；已经绑定到其他应用的设备由服务端按权限和业务规则处理。
      </a-alert>
      <a-tree
        block-node
        default-expand-all
        :data="groups"
        :field-names="{ key: 'id', title: 'groupName', children: 'children' }"
        :selected-keys="selectedGroupId ? [selectedGroupId] : []"
        @select="selectGroup"
      />
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceGroupItem, DeviceItem } from '@/apis/iot/device'
import { Button, Message, Popconfirm, Tag, Tooltip } from '@arco-design/web-vue'
import { IconClose } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import {
  bindDevicesToApplication,
  exportApplicationDevices,
  getApplicationDeviceGroups,
  getApplicationDevices,
  getApplicationGroupDevices,
  getUnboundApplicationDevices
} from '@/apis/application/application'
import { getProductList } from '@/apis/iot/product'
import { hasPerm } from '@/utils/permission'

defineOptions({ name: 'ApplicationDeviceBind' })
const props = defineProps<{ appUniqueId: string }>()
const emit = defineEmits<{
  (e: 'change'): void
  (e: 'total-change', total: number): void
}>()
const UNIX_TIMESTAMP_RE = /^\d{10,13}$/
const loading = ref(false)
const rows = ref<DeviceItem[]>([])
const selectedKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true })
const query = reactive({ deviceId: '', deviceName: '', productKey: '', state: undefined as number | undefined })
const bindVisible = ref(false)
const availableLoading = ref(false)
const availableRows = ref<DeviceItem[]>([])
const availableSelectedKeys = ref<string[]>([])
const availablePagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true })
const availableQuery = reactive({ deviceId: '', deviceName: '', productKey: '', state: undefined as number | undefined })
const productOptions = ref<{ label: string, value: string }[]>([])
const groupVisible = ref(false)
const groups = ref<DeviceGroupItem[]>([])
const selectedGroupId = ref('')
const modalBodyStyle = { maxHeight: 'min(72vh, 720px)', overflowY: 'auto' as const }
const groupModalBodyStyle = { maxHeight: 'min(64vh, 560px)', overflowY: 'auto' as const }
const stateOptions = [{ label: '在线', value: 1 }, { label: '离线', value: 0 }]

function params(source: typeof query, page = pagination) {
  return {
    page: page.current,
    size: page.pageSize,
    deviceId: source.deviceId || undefined,
    deviceName: source.deviceName || undefined,
    productKey: source.productKey || undefined,
    state: source.state
  }
}

async function load() {
  if (!props.appUniqueId) return
  loading.value = true
  try {
    const res = await getApplicationDevices(props.appUniqueId, params(query))
    rows.value = res.data.records || []
    pagination.total = res.data.total || 0
    emit('total-change', pagination.total)
    selectedKeys.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  load()
}

function reset() {
  Object.assign(query, { deviceId: '', deviceName: '', productKey: '', state: undefined })
  search()
}

function changePage(page: number) {
  pagination.current = page
  load()
}

function changePageSize(size: number) {
  pagination.current = 1
  pagination.pageSize = size
  load()
}

function selectAll(checked: boolean) {
  selectedKeys.value = checked ? rows.value.map((item) => item.id) : []
}

async function loadAvailable() {
  availableLoading.value = true
  try {
    const res = await getUnboundApplicationDevices(params(availableQuery, availablePagination))
    availableRows.value = res.data.records || []
    availablePagination.total = res.data.total || 0
  } finally {
    availableLoading.value = false
  }
}

async function openBind() {
  availableSelectedKeys.value = []
  availablePagination.current = 1
  bindVisible.value = true
  await loadAvailable()
}

function searchAvailable() {
  availablePagination.current = 1
  loadAvailable()
}

function resetAvailable() {
  Object.assign(availableQuery, { deviceId: '', deviceName: '', productKey: '', state: undefined })
  searchAvailable()
}

function changeAvailablePage(page: number) {
  availablePagination.current = page
  loadAvailable()
}

function changeAvailablePageSize(size: number) {
  availablePagination.current = 1
  availablePagination.pageSize = size
  loadAvailable()
}

function selectAllAvailable(checked: boolean) {
  availableSelectedKeys.value = checked ? availableRows.value.map((item) => item.id) : []
}

async function bindSelected() {
  if (!availableSelectedKeys.value.length) {
    Message.warning('请选择设备')
    return false
  }
  await bindDevicesToApplication(availableSelectedKeys.value, props.appUniqueId)
  Message.success(`已绑定 ${availableSelectedKeys.value.length} 台设备`)
  bindVisible.value = false
  await load()
  emit('change')
  return true
}

async function unbind(ids: string[]) {
  if (!ids.length) return false
  await bindDevicesToApplication(ids)
  Message.success(`已解绑 ${ids.length} 台设备`)
  await load()
  emit('change')
  return true
}

function unbindSelected() {
  return unbind(selectedKeys.value)
}

async function openGroup() {
  groups.value = (await getApplicationDeviceGroups()).data || []
  selectedGroupId.value = ''
  groupVisible.value = true
}

function selectGroup(keys: (string | number)[]) {
  selectedGroupId.value = String(keys[0] || '')
}

async function bindGroup() {
  if (!selectedGroupId.value) {
    Message.warning('请选择设备组')
    return false
  }
  const res = await getApplicationGroupDevices(selectedGroupId.value)
  const ids = (res.data.records || []).map((item) => item.id)
  if (!ids.length) {
    Message.warning('该分组下没有设备')
    return false
  }
  await bindDevicesToApplication(ids, props.appUniqueId)
  Message.success(`已按组绑定 ${ids.length} 台设备`)
  groupVisible.value = false
  await load()
  emit('change')
  return true
}

async function exportDevices() {
  await exportApplicationDevices(props.appUniqueId, {
    deviceId: query.deviceId || undefined,
    deviceName: query.deviceName || undefined,
    productKey: query.productKey || undefined,
    state: query.state
  })
  Message.success('导出任务已完成')
  return true
}

const baseColumns: TableColumnData[] = [
  { title: '设备名称', dataIndex: 'deviceName', width: 170, ellipsis: true, tooltip: true },
  { title: 'DeviceId', dataIndex: 'deviceId', width: 170, ellipsis: true, tooltip: true },
  { title: '产品', dataIndex: 'productName', width: 160, ellipsis: true, tooltip: true },
  { title: 'ProductKey', dataIndex: 'productKey', width: 170, ellipsis: true, tooltip: true },
  {
    title: '在线状态',
    width: 100,
    align: 'center',
    render: ({ record }) => <Tag color={isOnline(record.state) ? 'green' : 'gray'}>{isOnline(record.state) ? '在线' : '离线'}</Tag>
  },
  { title: '最近上线', width: 180, render: ({ record }) => formatDeviceTime(record.onlineTime) }
]
const availableColumns = baseColumns
const columns: TableColumnData[] = [
  ...baseColumns,
  {
    title: '操作',
    width: 76,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => hasPerm('application:application:unbind')
      ? (
          <Popconfirm content="确定解绑该设备吗？" onBeforeOk={() => unbind([String(record.id)])}>
            <Tooltip content="解绑">
              <Button type="text" status="danger" shape="circle"><IconClose /></Button>
            </Tooltip>
          </Popconfirm>
        )
      : null
  }
]

async function loadProducts() {
  const res = await getProductList({ page: 1, size: 100, state: 0 })
  productOptions.value = (res.data.records || []).map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey }))
}

function isOnline(state: DeviceItem['state']) {
  return state === true || Number(state) === 1
}

function formatDeviceTime(value?: string | number) {
  if (!value) return '-'
  const text = String(value).trim()
  const numeric = UNIX_TIMESTAMP_RE.test(text) ? Number(text) : Number.NaN
  const date = Number.isNaN(numeric)
    ? Dayjs(text)
    : Dayjs(text.length === 10 ? numeric * 1000 : numeric)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

watch(() => props.appUniqueId, (value) => value && load(), { immediate: true })
onMounted(loadProducts)
defineExpose({ load })
</script>

<style lang="scss" scoped>
.device-bind {
  &__filters {
    padding: 14px 14px 0;
    margin-bottom: 16px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-2);
  }

  &__group-tip {
    margin-bottom: 16px;
  }
}
</style>
