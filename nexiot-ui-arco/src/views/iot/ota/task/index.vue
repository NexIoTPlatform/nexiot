<template>
  <GiPageLayout margin>
    <div class="ota-task__filters">
      <div class="ota-task__field ota-task__field--product">
        <a-select v-model="query.productKey" :options="productOptions" :loading="productSearchLoading" allow-search allow-clear placeholder="全部产品" @search="searchProducts" @dropdown-visible-change="refreshProducts" />
      </div>
      <div class="ota-task__field ota-task__field--search">
        <a-input v-model="query.taskName" placeholder="搜索计划名称" allow-clear @press-enter="search" />
      </div>
      <div class="ota-task__field">
        <a-select v-model="query.status" :options="statusOptions" allow-clear placeholder="全部状态" />
      </div>
      <a-space>
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </div>
    <GiTable
      title="升级计划列表"
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :table-layout-fixed="true"
      :scroll="{ x: '100%', y: '100%', minWidth: 1100 }"
      @refresh="refresh"
    >
      <template #custom-title>
        <a-button type="primary" @click="formDrawerRef?.open(query.productKey || undefined)"><template #icon><icon-plus /></template>创建升级计划</a-button>
      </template>
    </GiTable>
    <OtaTaskFormDrawer ref="formDrawerRef" :products="availableProducts" @save-success="search" />
    <a-drawer v-model:visible="detailVisible" title="计划设备明细" width="min(1100px, 100%)">
      <a-row justify="space-between" align="center" class="g-row-tool"><div class="ota-task__detail-context"><strong>{{ detailTask?.taskName || '升级计划' }}</strong><a-typography-text type="secondary">目标版本 {{ detailTask?.targetVersion || '-' }} · 每 5 秒刷新</a-typography-text></div><a-button :loading="deviceLoading" @click="loadDevices"><template #icon><icon-refresh /></template>刷新</a-button></a-row>
      <a-typography-paragraph type="secondary">文件传完后，需设备安装并上报结果。自动补发保留已确认分片；人工重试会重新传输。</a-typography-paragraph>
      <a-alert v-if="deviceError" type="error">设备明细加载失败，请点击刷新重试。</a-alert>
      <a-table row-key="id" :data="taskDevices" :columns="deviceColumns" :loading="deviceLoading" :pagination="devicePagination" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 960 }" @page-change="changeDevicePage" @page-size-change="changeDevicePageSize">
        <template #empty><a-empty :description="deviceError ? '暂时无法加载设备明细' : '此计划暂无设备记录'" /></template>
      </a-table>
    </a-drawer>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
/* eslint-disable style/max-statements-per-line */
import type { OtaTaskDeviceItem, OtaTaskItem } from '@/apis/iot/ota'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Tag, Tooltip } from '@arco-design/web-vue'
import { cancelOtaTask, dispatchOtaTask, getOtaTaskDevices, getOtaTaskList, retryOtaTaskDevice } from '@/apis/iot/ota'
import { getProductList } from '@/apis/iot/product'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import OtaTaskFormDrawer from '../modules/OtaTaskFormDrawer.vue'
import { useOtaProducts } from '../useOtaProducts'
import { describeOtaTransfer, otaDeviceStatusLabels } from '../utils'

defineOptions({ name: 'IotOtaTask' })
const route = useRoute()
const router = useRouter()
const products = ref<ProductItem[]>([])
const formDrawerRef = useTemplateRef<InstanceType<typeof OtaTaskFormDrawer>>('formDrawerRef')
const query = reactive({ productKey: typeof route.query.productKey === 'string' ? route.query.productKey : '', taskName: '', status: '' })
const { availableProducts, productOptions, productSearchLoading, searchProducts, refreshProducts } = useOtaProducts(() => products.value)
const statusMap: Record<string, { label: string, color: string }> = {
  DRAFT: { label: '草稿', color: 'gray' },
  PENDING: { label: '待执行', color: 'orange' },
  RUNNING: { label: '执行中', color: 'arcoblue' },
  COMPLETED: { label: '已完成', color: 'green' },
  SUCCESS: { label: '已完成', color: 'green' },
  PARTIAL_SUCCESS: { label: '部分成功', color: 'orange' },
  FAILED: { label: '失败', color: 'red' },
  CANCELED: { label: '已取消', color: 'gray' }
}
const statusOptions = Object.entries(statusMap).filter(([value]) => value !== 'SUCCESS').map(([value, item]) => ({ label: item.label, value }))
const rolloutLabels: Record<string, string> = { ALL: '全部设备', SPECIFIED: '指定设备', GRAY: '灰度发布' }
const detailVisible = ref(false)
const detailTask = ref<OtaTaskItem>()
const taskDevices = ref<OtaTaskDeviceItem[]>([])
const deviceLoading = ref(false)
const deviceError = ref(false)
const detailTaskId = ref('')
const devicePagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: true, showPageSize: true })
let deviceRequestId = 0
let refreshTimer: ReturnType<typeof setInterval> | undefined
const { loading, tableData: rows, pagination, search, refresh } = useTable<OtaTaskItem>({ listAPI: (page) => getOtaTaskList({ ...page, productKey: query.productKey || undefined, taskName: query.taskName.trim() || undefined, status: query.status || undefined }), immediate: true })
function reset() {
  Object.assign(query, { productKey: '', taskName: '', status: '' })
  search()
};
async function dispatch(row: OtaTaskItem) {
  await dispatchOtaTask(row.id)
  Message.success('升级任务已开始执行')
  refresh()
};
async function cancel(row: OtaTaskItem) {
  await cancelOtaTask(row.id)
  Message.success('升级任务已取消')
  refresh()
};
async function showDevices(row: OtaTaskItem) {
  detailTask.value = row
  detailTaskId.value = row.id
  taskDevices.value = []
  deviceError.value = false
  Object.assign(devicePagination, { current: 1, total: 0 })
  detailVisible.value = true
  await loadDevices()
};
async function loadDevices() {
  if (!detailTaskId.value) return
  const requestId = ++deviceRequestId
  const id = detailTaskId.value
  deviceLoading.value = true
  try {
    const page = devicePagination.current
    const size = devicePagination.pageSize
    const { data } = await getOtaTaskDevices(id, { page, size })
    if (requestId === deviceRequestId && id === detailTaskId.value && page === devicePagination.current && size === devicePagination.pageSize) {
      taskDevices.value = Array.isArray(data) ? data : data?.records || []
      devicePagination.total = Array.isArray(data) ? data.length : data?.total || 0
      deviceError.value = false
    }
  } catch {
    if (requestId === deviceRequestId && id === detailTaskId.value) deviceError.value = true
  } finally { if (requestId === deviceRequestId) deviceLoading.value = false }
};
async function changeDevicePage(page: number) {
  devicePagination.current = page
  await loadDevices()
}
async function changeDevicePageSize(size: number) {
  devicePagination.pageSize = size
  devicePagination.current = 1
  await loadDevices()
}
async function retry(row: OtaTaskDeviceItem) {
  await retryOtaTaskDevice(row.id)
  Message.success('设备升级已重新入队')
  await loadDevices()
  refresh()
}
const columns: TableColumnData[] = [
  { title: '升级计划', dataIndex: 'taskName', width: 220, fixed: 'left', render: ({ record }) => (
    <div class="ota-task__cell">
      <Tooltip content={`${record.taskName || '未命名计划'} · 编号: ${record.taskNo || record.id}`}><Button type="text" class="ota-task__name" onClick={() => showDevices(record as OtaTaskItem)}><span class="ota-task__ellipsis">{record.taskName || '未命名计划'}</span></Button></Tooltip>
      <span class="ota-task__secondary">{`${rolloutLabels[record.rolloutMode] || record.rolloutMode} · ${record.deviceCount ?? record.totalCount ?? 0} 台设备`}</span>
    </div>
  ) },
  { title: '所属产品', dataIndex: 'productKey', width: 160, render: ({ record }) => (
    <Tooltip content={`${record.productName || '-'} · ProductKey: ${record.productKey}`}><span class="ota-task__ellipsis">{record.productName || record.productKey || '-'}</span></Tooltip>
  ) },
  { title: '升级包 / 目标版本', dataIndex: 'packageName', width: 180, render: ({ record }) => (
    <div class="ota-task__cell">
      <Tooltip content={record.packageName || '-'}><span class="ota-task__ellipsis">{record.packageName || '-'}</span></Tooltip>
      <Tooltip content={`目标版本：${record.targetVersion || '-'}`}><code class="ota-task__version">{record.targetVersion || '-'}</code></Tooltip>
    </div>
  ) },
  { title: '执行结果', dataIndex: 'status', width: 180, render: ({ record }) => (
    <div class="ota-task__progress">
      <Tag color={statusMap[record.status]?.color || 'arcoblue'}>{statusMap[record.status]?.label || record.status || '-'}</Tag>
      <div class="ota-task__counts">
        <span>
          成功
          <b class="ota-task__success">{record.successCount || 0}</b>
        </span>
        <span>
          失败
          <b class={(record.failedCount ?? record.failureCount ?? 0) > 0 ? 'ota-task__failure' : ''}>{record.failedCount ?? record.failureCount ?? 0}</b>
        </span>
      </div>
    </div>
  ) },
  { title: '创建时间', dataIndex: 'createTime', width: 156, ellipsis: true, tooltip: true },
  { title: '操作', width: 138, fixed: 'right', align: 'center', render: ({ record }) => (
    <GiTableActions actions={[
      { key: 'devices', label: '设备明细', icon: 'view', onClick: () => showDevices(record as OtaTaskItem) },
      { key: 'dispatch', label: '执行', icon: 'play', status: 'success', visible: ['DRAFT', 'PENDING'].includes(String(record.status)), onClick: () => dispatch(record as OtaTaskItem) },
      { key: 'cancel', label: '取消', icon: 'stop', status: 'danger', visible: ['PENDING', 'RUNNING'].includes(String(record.status)), onClick: () => cancel(record as OtaTaskItem) }
    ]}
    />
  ) }
]
const deviceColumns: TableColumnData[] = [
  { title: '设备', width: 180, render: ({ record }) => (
    <div class="ota-task__cell">
      <Tooltip content={record.deviceName || record.deviceId}><strong class="ota-task__ellipsis">{record.deviceName || record.deviceId}</strong></Tooltip>
      <Tooltip content={record.deviceId}><code class="ota-task__secondary ota-task__ellipsis">{record.deviceId}</code></Tooltip>
    </div>
  ) },
  { title: '当前 / 目标版本', width: 150, render: ({ record }) => `${record.currentVersion || '-'} / ${record.targetVersion || '-'}` },
  { title: '设备进度', width: 90, render: ({ record }) => `${record.progress || 0}%` },
  { title: '分包传输', width: 220, render: ({ record }) => {
    const transfer = describeOtaTransfer(record.transferState, record.status)
    return (
      <div class="ota-task__cell">
        <strong>{transfer.chunks}</strong>
        <span>{transfer.phase}</span>
        {transfer.retries > 0 && <span>{`当前分片已补发 ${transfer.retries} 次`}</span>}
      </div>
    )
  } },
  { title: '状态', width: 120, render: ({ record }) => <Tag color={record.status === 'FAILED' ? 'red' : record.status === 'TIMEOUT' ? 'orange' : record.status === 'SUCCESS' ? 'green' : ['SKIPPED', 'CANCELED'].includes(String(record.status)) ? 'gray' : 'arcoblue'}>{otaDeviceStatusLabels[record.status] || record.status || '-'}</Tag> },
  { title: '失败原因', dataIndex: 'errorMessage', width: 180, ellipsis: true, tooltip: true, render: ({ record }) => record.errorMessage ? ['FAILED', 'TIMEOUT'].includes(String(record.status)) ? record.errorMessage : `上次失败：${record.errorMessage}` : '-' },
  { title: '操作', width: 76, fixed: 'right', render: ({ record }) => ['FAILED', 'TIMEOUT'].includes(String(record.status)) ? <GiTableActions actions={[{ key: 'retry', label: '重试', icon: 'retry', onClick: () => retry(record as OtaTaskDeviceItem) }]} /> : '-' }
]
watch(detailVisible, (value) => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (value) refreshTimer = setInterval(() => { if (!deviceError.value && !deviceLoading.value) loadDevices() }, 5000)
  else deviceRequestId++
})
let routeRequestId = 0
watch(() => route.fullPath, async () => {
  const currentRequest = ++routeRequestId
  if (route.name !== 'IotOtaTask') return
  const productKey = typeof route.query.productKey === 'string' ? route.query.productKey : ''
  if (productKey && query.productKey !== productKey) {
    query.productKey = productKey
    search()
  }
  if (route.query.create !== '1') return
  const originalPath = route.fullPath
  const deviceId = typeof route.query.deviceId === 'string' ? route.query.deviceId : ''
  if (productKey && !products.value.some((item) => item.productKey === productKey)) {
    const { data } = await getProductList({ page: 1, size: 50, productKey })
    if (currentRequest !== routeRequestId) return
    products.value = [...new Map([...products.value, ...(data.records || [])].map((item) => [item.productKey, item])).values()]
  }
  await nextTick()
  if (currentRequest !== routeRequestId || route.fullPath !== originalPath) return
  await formDrawerRef.value?.open(productKey || undefined, deviceId ? [deviceId] : [])
  if (currentRequest === routeRequestId && route.fullPath === originalPath) {
    const { create: _create, deviceId: _deviceId, ...remainingQuery } = route.query
    await router.replace({ query: remainingQuery })
  }
}, { immediate: true })
onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
onDeactivated(() => {
  detailVisible.value = false
})
getProductList({ page: 1, size: 500 }).then(({ data }) => {
  products.value = [...new Map([...products.value, ...(data.records || [])].map((item) => [item.productKey, item])).values()]
})
</script>

<style lang="scss" scoped>
.ota-task {
  &__filters {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    padding-bottom: 14px;
    margin-bottom: 14px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__field {
    flex: 0 0 160px;
    width: 160px;
    min-width: 0;
    max-width: 100%;

    &--search {
      flex-basis: 220px;
      width: 220px;
    }

    &--product {
      flex-basis: 280px;
      width: 280px;
    }
  }

  &__detail-context {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;

    strong {
      overflow-wrap: anywhere;
    }
  }
}

:deep(.ota-task__cell),
:deep(.ota-task__progress) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

:deep(.ota-task__ellipsis),
:deep(.ota-task__version),
:deep(.ota-task__secondary) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ota-task__secondary),
:deep(.ota-task__counts) {
  font-size: 12px;
  color: var(--color-text-3);
}

:deep(.ota-task__version) {
  align-self: flex-start;
  max-width: 100%;
  padding: 1px 6px;
  font-size: 12px;
  color: var(--color-text-2);
  background: var(--color-fill-2);
  border-radius: 3px;
}

:deep(.ota-task__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;
  overflow: hidden;
  font-weight: 500;
  text-align: left;

  .ota-task__ellipsis {
    display: block;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

:deep(.ota-task__progress .arco-tag) {
  align-self: flex-start;
}

:deep(.ota-task__counts) {
  display: flex;
  gap: 14px;

  > span {
    display: inline-flex;
    gap: 4px;
  }

  b {
    font-weight: 500;
  }
}

:deep(.ota-task__success) {
  color: rgb(var(--success-6));
}

:deep(.ota-task__failure) {
  color: rgb(var(--danger-6));
}
</style>
