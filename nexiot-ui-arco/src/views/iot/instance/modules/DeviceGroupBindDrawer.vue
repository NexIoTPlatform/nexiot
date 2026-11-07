<template>
  <a-drawer v-model:visible="visible" title="绑定设备" width="min(860px, 100%)" :mask-closable="false" :footer="false" @close="reset">
<!-- <a-alert type="info" class="group-bind__notice">每台设备最多可加入 10 个设备群组。当前列表仅展示尚未绑定到本群组的设备。</a-alert> -->
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-select v-model="query.productKey" class="group-bind__product" :options="productOptions" allow-search allow-clear placeholder="所有产品" />
        <a-select v-model="query.field" :options="fieldOptions" class="group-bind__field" />
        <a-input v-model="query.keyword" placeholder="请输入搜索内容" allow-clear @press-enter="search" />
        <a-select v-model="query.state" class="group-bind__state" :options="stateOptions" allow-clear placeholder="在线状态" />
        <GiButton type="search" @click="search" /><GiButton type="reset" @click="resetQuery" />
      </a-space>
      <a-button type="primary" :disabled="!selectedKeys.length" @click="bindSelected"><template #icon><IconLink /></template>绑定选中设备</a-button>
    </a-row>
    <a-table class="g-table" row-key="id" :data="deviceList" :columns="columns" :loading="loading" :pagination="pagination" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 780 }" :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="selectedKeys" @select="select" @select-all="selectAll" />
  </a-drawer>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Tag, Tooltip } from '@arco-design/web-vue'
import { IconLink } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { bindDevicesToGroup, getUnboundDeviceList } from '@/apis/iot/device'
import { getProductList } from '@/apis/iot/product'
import { useTable } from '@/hooks'

defineOptions({ name: 'IotDeviceGroupBindDrawer' })

const emit = defineEmits<{
  (e: 'bind-success'): void
}>()

const visible = ref(false)
const groupId = ref('')
const products = ref<ProductItem[]>([])
const query = reactive({ productKey: '', field: 'deviceName', keyword: '', state: undefined as number | undefined })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const fieldOptions = [{ label: '设备名称', value: 'deviceName' }, { label: 'IoT ID', value: 'iotId' }, { label: 'DeviceId', value: 'deviceId' }]
const stateOptions = [{ label: '在线', value: 1 }, { label: '离线', value: 0 }]
const { loading, tableData: deviceList, pagination, selectedKeys, search, select, selectAll } = useTable<DeviceItem>({
  listAPI: (page) => getUnboundDeviceList({
    ...page,
    groupId: groupId.value,
    productKey: query.productKey || undefined,
    state: query.state,
    [query.field]: query.keyword.trim() || undefined
  } as { page: number, size: number, groupId: string, deviceId?: string, deviceName?: string, iotId?: string, productKey?: string, state?: number }),
  immediate: false
})

function reset() {
  Object.assign(query, { productKey: '', field: 'deviceName', keyword: '', state: undefined })
  selectedKeys.value = []
}

function resetQuery() {
  reset()
  search()
}

async function open(id: string) {
  reset()
  groupId.value = id
  products.value = (await getProductList({ page: 1, size: 500 })).data.records || []
  visible.value = true
  search()
}

async function bindRows(rows: DeviceItem[]) {
  const iotIds = rows.map((item) => item.iotId).filter(Boolean) as string[]
  await bindDevicesToGroup({ id: groupId.value, devIds: iotIds })
  Message.success(`已绑定 ${iotIds.length} 台设备`)
  emit('bind-success')
  search()
}

async function bindSelected() {
  await bindRows(deviceList.value.filter((item) => selectedKeys.value.includes(item.id)))
}

function formatTime(value?: string | number) {
  if (!value) return '-'
  const text = String(value)
  const date = /^\d{10,13}$/.test(text) ? Dayjs(Number(text) * (text.length === 10 ? 1000 : 1)) : Dayjs(text)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

function isOnline(state: DeviceItem['state']) {
  return state === true || Number(state) === 1
}

const columns: TableColumnData[] = [
  { title: 'DeviceId', dataIndex: 'deviceId', width: 180, ellipsis: true, tooltip: true },
  { title: '设备名称', dataIndex: 'deviceName', width: 180, ellipsis: true, tooltip: true },
  { title: '所属产品', dataIndex: 'productName', width: 180, ellipsis: true, tooltip: true },
  { title: '在线状态', width: 100, align: 'center', render: ({ record }) => <Tag color={isOnline(record.state) ? 'green' : 'gray'}>{isOnline(record.state) ? '在线' : '离线'}</Tag> },
  { title: '最近通信', width: 180, render: ({ record }) => formatTime(record.onlineTime) },
  { title: '操作', width: 80, align: 'center', render: ({ record }) => <Tooltip content="绑定"><Button type="text" shape="circle" aria-label="绑定设备" onClick={() => bindRows([record as DeviceItem])}><IconLink /></Button></Tooltip> }
]

defineExpose({ open })
</script>

<style lang="scss" scoped>
.group-bind {
  &__notice {
    margin-bottom: 16px;
  }

  &__product {
    width: 220px;
  }

  &__field {
    width: 110px;
  }

  &__state {
    width: 120px;
  }
}
</style>
