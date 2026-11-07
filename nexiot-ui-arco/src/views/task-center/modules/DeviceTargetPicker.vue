<template>
  <div class="target-picker">
    <a-form-item label="生效范围" class="scope-row">
      <a-radio-group v-model="scopeType" type="button">
        <a-radio value="SELECTED">手动勾选设备</a-radio>
        <a-radio value="FILTERED">当前筛选结果全部设备</a-radio>
      </a-radio-group>
    </a-form-item>

    <div class="target-filters">
      <a-select v-model="application" :options="applicationOptions" allow-clear allow-search placeholder="全部应用" />
      <a-select v-model="groupId" :options="groupOptions" allow-clear allow-search placeholder="全部分组" />
      <a-input v-model.trim="keyword" allow-clear placeholder="设备 ID / 设备名称" @press-enter="search" />
      <GiButton type="search" @click="search" />
      <GiButton type="reset" @click="resetFilters" />
    </div>

    <div class="target-summary">
      <a-tag color="arcoblue">匹配 {{ pagination.total }} 台</a-tag>
      <a-tag v-if="scopeType === 'SELECTED'" color="green">已选 {{ selectedIds.length }} 台</a-tag>
      <a-typography-text type="secondary">{{ scopeType === 'FILTERED' ? '提交时按当前筛选条件重新圈定设备' : '支持跨页保留已勾选设备' }}</a-typography-text>
    </div>

    <a-table
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', minWidth: 760 }"
      :row-selection="scopeType === 'SELECTED' ? { type: 'checkbox', showCheckedAll: true, onlyCurrent: false } : undefined"
      :selected-keys="selectedIds"
      @select="handleSelect"
      @select-all="handleSelectAll"
    />
  </div>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ApplicationItem } from '@/apis/application/application'
import type { DeviceGroupItem, DeviceItem } from '@/apis/iot/device'
import { Tag } from '@arco-design/web-vue'
import { getApplicationList } from '@/apis/application/application'
import { getDeviceGroups } from '@/apis/iot/device'
import { getDeviceJobDevices } from '../api'

const application = defineModel<string>('application', { default: '' })
const groupId = defineModel<string>('groupId', { default: '' })
const keyword = defineModel<string>('keyword', { default: '' })
const scopeType = defineModel<'SELECTED' | 'FILTERED'>('scopeType', { default: 'SELECTED' })
const selectedIds = defineModel<string[]>('selectedIds', { default: () => [] })
const matchedCount = defineModel<number>('matchedCount', { default: 0 })
const props = defineProps<{ productKey: string }>()
const rows = ref<DeviceItem[]>([])
const applications = ref<ApplicationItem[]>([])
const groups = ref<DeviceGroupItem[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: true,
  showPageSize: true,
  showJumper: true,
  onChange: (page: number) => {
    pagination.current = page
    load()
  },
  onPageSizeChange: (size: number) => {
    pagination.current = 1
    pagination.pageSize = size
    load()
  }
})
const applicationOptions = computed(() => applications.value.map((item) => ({ label: `${item.appName} (${item.appUniqueId || item.appId})`, value: item.appUniqueId || item.appId })))
const groupOptions = computed(() => flattenGroups(groups.value).map((item) => ({ label: item.groupName, value: String(item.id) })))
const columns: TableColumnData[] = [
  { title: '设备', width: 230, render: ({ record }) => (
    <div class="device-cell">
      <strong>{record.deviceName || '-'}</strong>
      <code>{record.deviceId || '-'}</code>
    </div>
  ) },
  { title: '归属应用', dataIndex: 'application', width: 190, ellipsis: true, tooltip: true },
  { title: '在线状态', width: 110, render: ({ record }) => <Tag color={record.state === true || record.state === 1 || record.state === '1' ? 'green' : 'gray'}>{record.state === true || record.state === 1 || record.state === '1' ? '在线' : '离线'}</Tag> },
  { title: 'ProductKey', dataIndex: 'productKey', width: 190 },
  { title: '所属分组', width: 180, render: ({ record }) => Array.isArray(record.devGroupName) ? record.devGroupName.join('、') : '-' }
]

function flattenGroups(items: DeviceGroupItem[]): DeviceGroupItem[] {
  return items.flatMap((item) => [item, ...flattenGroups(item.children || [])])
}

async function initializeOptions() {
  const [applicationRes, groupRes] = await Promise.all([getApplicationList({ page: 1, size: 500 }), getDeviceGroups()])
  applications.value = applicationRes.data.records || []
  groups.value = groupRes.data || []
}

async function load() {
  if (!props.productKey) {
    rows.value = []
    pagination.total = 0
    matchedCount.value = 0
    return
  }
  loading.value = true
  try {
    const response = await getDeviceJobDevices({
      page: pagination.current,
      size: pagination.pageSize,
      productKey: props.productKey,
      application: application.value || undefined,
      devGroupId: groupId.value || undefined,
      keyword: keyword.value.trim() || undefined
    })
    rows.value = (response.data.records || []) as DeviceItem[]
    pagination.total = response.data.total || 0
    matchedCount.value = pagination.total
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  if (scopeType.value === 'SELECTED') selectedIds.value = []
  load()
}

function resetFilters() {
  application.value = ''
  groupId.value = ''
  keyword.value = ''
  search()
}

function handleSelect(keys: (string | number)[]) {
  if (scopeType.value === 'FILTERED') return
  selectedIds.value = keys.map(String)
}

function handleSelectAll(checked: boolean) {
  if (scopeType.value === 'FILTERED') return
  const pageIds = rows.value.map((item) => String(item.id))
  const rest = selectedIds.value.filter((id) => !pageIds.includes(id))
  selectedIds.value = checked ? [...new Set([...rest, ...pageIds])] : rest
}

watch(() => props.productKey, () => {
  pagination.current = 1
  load()
})
watch(scopeType, (value) => {
  if (value === 'FILTERED') selectedIds.value = []
})

initializeOptions()
onMounted(load)
defineExpose({ load })
</script>

<style scoped lang="scss">
.target-filters {
  display: grid;
  grid-template-columns: minmax(170px, 1fr) minmax(170px, 1fr) minmax(190px, 1.2fr) auto auto;
  gap: 8px;
  margin-bottom: 10px;
}

.target-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.device-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.device-cell code {
  font-size: 12px;
  color: var(--color-text-3);
}

@media (width <= 800px) {
  .target-filters {
    grid-template-columns: 1fr 1fr;
  }
}

@media (width <= 520px) {
  .target-filters {
    grid-template-columns: 1fr;
  }
}
</style>
