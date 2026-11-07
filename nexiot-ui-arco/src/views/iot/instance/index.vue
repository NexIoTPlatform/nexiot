<template>
  <GiPageLayout margin class="device-list-scope">
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['system:instance:add']" type="primary" @click="formDrawerRef?.add(query.productKey || undefined)"><template #icon><icon-plus /></template>新增设备</a-button>
        <a-button v-hasPerm="['system:instance:edit']" :disabled="selectedKeys.length !== 1" @click="editSelected"><template #icon><IconEdit /></template>编辑</a-button>
        <a-button v-hasPerm="['system:instance:remove']" status="danger" :disabled="!selectedKeys.length" @click="requestDeviceDelete(selectedKeys.map(String))"><template #icon><IconDelete /></template>删除</a-button>
        <a-button v-hasPerm="['system:instance:import']" @click="importDrawerRef?.open(query.productKey || undefined)"><template #icon><icon-import /></template>导入</a-button>
        <a-button v-hasPerm="['system:instance:export']" @click="handleExport"><template #icon><icon-export /></template>导出</a-button>
      </a-space>
      <a-space wrap>
        <a-input v-model="query.deviceId" class="device-list__search" placeholder="DeviceId" allow-clear @press-enter="search" />
        <a-select v-model="query.productKey" class="device-list__product" :options="productOptions" allow-search allow-clear placeholder="所属产品" />
        <a-input v-model="query.deviceName" class="device-list__search" placeholder="设备名称" allow-clear @press-enter="search" />
        <a-button type="text" @click="advanced = !advanced">{{ advanced ? '收起' : '展开' }}<template #icon><icon-down v-if="!advanced" /><icon-up v-else /></template></a-button>
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
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
      :scroll="{ x: '100%', y: '100%', minWidth: 1220 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
      @refresh="refresh"
    >
    </GiTable>

    <DeviceFormDrawer ref="formDrawerRef" @save-success="search" />
    <DeviceImportDrawer ref="importDrawerRef" @import-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Modal, Space, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye } from '@arco-design/web-vue/es/icon'
import { useRoute, useRouter } from 'vue-router'
import { deleteDevices, exportDevices, getDeviceList } from '@/apis/iot/device'
import { getProductList } from '@/apis/iot/product'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import DeviceFormDrawer from './modules/DeviceFormDrawer.vue'
import DeviceImportDrawer from './modules/DeviceImportDrawer.vue'
import { createDeviceListColumns } from './modules/deviceListColumns'

defineOptions({ name: 'IotDevice' })

const route = useRoute()
const router = useRouter()
const formDrawerRef = useTemplateRef<InstanceType<typeof DeviceFormDrawer>>('formDrawerRef')
const importDrawerRef = useTemplateRef<InstanceType<typeof DeviceImportDrawer>>('importDrawerRef')
const products = ref<ProductItem[]>([])
const advanced = ref(false)
const query = reactive({
  deviceId: '',
  productKey: '',
  deviceName: '',
  iotId: '',
  state: ['0', '1'].includes(String(route.query.state)) ? Number(route.query.state) : undefined,
  deviceTag: ''
})
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const stateOptions = [{ label: '在线', value: 1 }, { label: '离线', value: 0 }]
const { loading, tableData: deviceList, pagination, selectedKeys, search, refresh, select, selectAll } = useTable<DeviceItem>({
  listAPI: (page) => getDeviceList({
    ...page,
    deviceId: query.deviceId.trim() || undefined,
    productKey: query.productKey || undefined,
    deviceName: query.deviceName.trim() || undefined,
    iotId: query.iotId.trim() || undefined,
    state: query.state,
    deviceTag: query.deviceTag.trim() || undefined
  }),
  deleteAPI: deleteDevices,
  immediate: true
})

const deviceListPath = route.path
watch(() => route.fullPath, () => {
  if (route.path !== deviceListPath || route.query.from !== 'overview') return
  Object.assign(query, {
    deviceId: '',
    productKey: '',
    deviceName: '',
    iotId: '',
    deviceTag: '',
    state: ['0', '1'].includes(String(route.query.state)) ? Number(route.query.state) : undefined
  })
  search()
})

function reset() {
  Object.assign(query, { deviceId: '', productKey: '', deviceName: '', iotId: '', state: undefined, deviceTag: '' })
  search()
}

function editSelected() {
  const id = selectedKeys.value[0]
  if (id !== undefined) formDrawerRef.value?.edit(String(id))
}

function confirmStandardDeviceDelete(ids: string[]) {
  Modal.warning({
    title: '确认删除所选设备？',
    content: `即将删除 ${ids.length} 台设备，删除后无法恢复。`,
    hideCancel: false,
    onBeforeOk: async () => {
      await deleteDevices(ids)
      Message.success('删除成功')
      selectedKeys.value = []
      search()
      return true
    }
  })
}

async function handleExport() {
  await exportDevices({
    deviceId: query.deviceId.trim() || undefined,
    productKey: query.productKey || undefined,
    deviceName: query.deviceName.trim() || undefined,
    iotId: query.iotId.trim() || undefined,
    state: query.state,
    deviceTag: query.deviceTag.trim() || undefined
  })
  Message.success('设备导出完成')
}

function requestDeviceDelete(ids: string[]) {
  if (ids.length) confirmStandardDeviceDelete(ids)
}
function openDeviceRecordDetail(record: DeviceItem) {
  router.push(`/system/instance/instance-details/${record.id}`)
}
const columns: TableColumnData[] = [
  ...createDeviceListColumns(openDeviceRecordDetail),
  { title: '操作', width: 138, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="设备详情"><Button type="text" shape="circle" aria-label="设备详情" onClick={() => openDeviceRecordDetail(record as DeviceItem)}><IconEye /></Button></Tooltip>
      {hasPerm('system:instance:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑设备" onClick={() => formDrawerRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
      {hasPerm('system:instance:remove') && <Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除设备" onClick={() => requestDeviceDelete([String(record.id)])}><IconDelete /></Button></Tooltip>}
    </Space>
  ) }
]

getProductList({ page: 1, size: 500 }).then((res) => {
  products.value = res.data.records || []
})
</script>

<style lang="scss" scoped>
.delete-preview__spin {
  width: 100%;
  min-height: 120px;
}
.delete-preview__item { padding-top: 16px; }

.delete-preview__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: baseline;
  margin-bottom: 10px;
}

.delete-preview__title code {
  font-size: 12px;
  color: var(--color-text-3);
}
.delete-preview__blocker { margin-bottom: 10px; }
</style>

<!-- 表格 TSX 单元格共享样式，通过 device-list-scope 限定作用域。 -->
<style lang="scss" src="@/views/iot/instance/modules/deviceList.scss"></style>
