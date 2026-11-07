<template>
  <a-modal v-model:visible="visible" title="选择设备" width="min(880px, calc(100vw - 32px))" align-center :footer="false" unmount-on-close>
    <a-space wrap class="device-picker__filters">
      <a-select v-model="query.productKey" :options="productOptions" allow-search allow-clear placeholder="全部产品" @change="search" />
      <a-input v-model.trim="query.deviceName" allow-clear placeholder="设备名称" @press-enter="search" />
      <a-input v-model.trim="query.deviceId" allow-clear placeholder="设备 ID" @press-enter="search" />
      <GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" />
    </a-space>
    <a-table
      v-model:selected-keys="selectedKeys"
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :row-selection="{ type: 'radio', showCheckedAll: false }"
      :scroll="{ x: '100%', minWidth: 760 }"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />
    <div class="device-picker__footer"><a-button @click="visible = false">取消</a-button><a-button type="primary" :disabled="!selectedKeys.length" @click="confirm">选择设备</a-button></div>
  </a-modal>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { getSceneDevices, getSceneProducts } from '../api'

const emit = defineEmits<{ (e: 'select', value: DeviceItem): void }>()
const visible = ref(false)
const loading = ref(false)
const rows = ref<DeviceItem[]>([])
const products = ref<ProductItem[]>([])
const selectedKeys = ref<string[]>([])
const query = reactive({ productKey: '', deviceName: '', deviceId: '' })
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const pagination = computed(() => ({ current: page.current, pageSize: page.pageSize, total: page.total, showTotal: true, showPageSize: true }))
const columns: TableColumnData[] = [
  { title: '设备名称', dataIndex: 'deviceName', width: 190, ellipsis: true, tooltip: true },
  { title: '设备 ID', dataIndex: 'deviceId', width: 180, ellipsis: true, tooltip: true },
  { title: '产品', dataIndex: 'productName', width: 180, ellipsis: true, tooltip: true },
  { title: 'ProductKey', dataIndex: 'productKey', width: 170 },
  { title: '状态', width: 90, render: ({ record }) => Number(record.state) === 1 ? '在线' : '离线' }
]
async function load() {
  loading.value = true
  try {
    const res = await getSceneDevices({ page: page.current, size: page.pageSize, productKey: query.productKey || undefined, deviceName: query.deviceName || undefined, deviceId: query.deviceId || undefined })
    rows.value = res.data.records || []
    page.total = res.data.total
  } finally { loading.value = false }
}
function search() { page.current = 1; selectedKeys.value = []; load() }
function reset() { Object.assign(query, { productKey: '', deviceName: '', deviceId: '' }); search() }
function handlePageChange(value: number) { page.current = value; load() }
function handlePageSizeChange(value: number) { page.pageSize = value; page.current = 1; load() }
function confirm() { const row = rows.value.find((item) => item.id === selectedKeys.value[0]); if (!row) return; emit('select', row); visible.value = false }
async function open(productKey?: string) {
  visible.value = true
  selectedKeys.value = []
  query.productKey = productKey || ''
  if (!products.value.length) products.value = (await getSceneProducts()).data.records || []
  search()
}
defineExpose({ open })
</script>

<style scoped>
/* stylelint-disable declaration-block-single-line-max-declarations */
.device-picker__filters { width: 100%; margin-bottom: 16px; }
.device-picker__filters :deep(.arco-select), .device-picker__filters :deep(.arco-input-wrapper) { width: 190px; }
.device-picker__footer { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }

@media (width <= 576px) { .device-picker__filters :deep(.arco-select), .device-picker__filters :deep(.arco-input-wrapper) { width: 100%; } }
</style>
