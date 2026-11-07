<template>
  <a-drawer v-model:visible="visible" title="任务详情" :width="drawerWidth" :footer="false" unmount-on-close @close="reset">
    <a-spin :loading="loading" class="detail-loading">
      <template v-if="task">
        <section class="stat-strip">
          <div><GiIconBox color="primary" :size="36"><icon-clock-circle /></GiIconBox><span>状态<strong><a-tag :color="taskStatusColor(task.status)">{{ taskStatusLabel(task.status) }}</a-tag></strong></span></div>
          <div><GiIconBox color="#4e5969" :size="36"><icon-list /></GiIconBox><span>总数<strong>{{ task.totalCount || 0 }}</strong></span></div>
          <div><GiIconBox color="success" :size="36"><icon-check-circle /></GiIconBox><span>成功<strong>{{ task.successCount || 0 }}</strong></span></div>
          <div><GiIconBox color="danger" :size="36"><icon-close-circle /></GiIconBox><span>失败<strong>{{ task.failCount || 0 }}</strong></span></div>
        </section>

        <a-descriptions :column="descriptionColumns" bordered class="overview">
          <a-descriptions-item label="任务编号">{{ task.taskNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="任务名称">{{ task.taskName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="任务类型">{{ taskTypeLabel(task.taskType) }}</a-descriptions-item>
          <a-descriptions-item label="触发方式">{{ triggerTypeLabel(task.triggerType) }}</a-descriptions-item>
          <a-descriptions-item label="所属产品">{{ task.productName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="ProductKey">{{ task.productKey || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ task.creatorName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="执行时间">{{ taskTimeText(task) }}</a-descriptions-item>
          <a-descriptions-item v-if="task.errorMessage" label="错误信息" :span="descriptionColumns">
            <a-typography-text type="danger">{{ task.errorMessage }}</a-typography-text>
          </a-descriptions-item>
        </a-descriptions>

        <section class="detail-section">
          <header>
            <strong>执行明细</strong>
            <a-button v-if="canExportFailed" type="text" @click="downloadFailed"><template #icon><icon-download /></template>下载失败项</a-button>
            <a-button v-if="canExportCredential" v-hasPerm="['system:task:center:credential:export']" type="text" @click="downloadCredentials"><template #icon><icon-download /></template>导出三元组</a-button>
          </header>
          <div class="item-filter">
            <a-input v-model.trim="itemQuery.keyword" allow-clear placeholder="设备 ID / 名称 / 原因" @press-enter="searchItems" />
            <a-select v-model="itemQuery.status" allow-clear placeholder="全部状态" :options="itemStatusOptions" />
            <GiButton type="search" @click="searchItems" />
            <GiButton type="reset" @click="resetItems" />
          </div>

          <ImportTaskDetail v-if="task.taskType === 'DEVICE_IMPORT'" :items="items" :loading="itemLoading" />
          <DeviceJobTaskDetail v-else-if="task.taskType === 'DEVICE_FUNCTION_JOB'" :task="task" :items="items" :loading="itemLoading" />
          <CredentialTaskDetail v-else-if="task.taskType === 'DEVICE_CREDENTIAL_GENERATE'" :task="task" :items="items" :loading="itemLoading" />
          <a-table v-else row-key="id" :data="items" :columns="genericColumns" :loading="itemLoading" :pagination="false" :bordered="{ cell: true }" />

          <a-pagination v-if="itemTotal" v-model:current="itemQuery.page" v-model:page-size="itemQuery.size" :total="itemTotal" show-total show-page-size show-jumper @change="loadItems" @page-size-change="handlePageSizeChange" />
        </section>

        <section class="detail-section log-section">
          <header><strong>执行日志</strong><a-tag>{{ logs.length }} 条</a-tag></header>
          <a-timeline v-if="logs.length">
            <a-timeline-item v-for="log in logs" :key="log.id" :dot-color="logColor(log.logLevel)">
              <div class="log-head"><time>{{ log.createTime || '-' }}</time><a-tag size="small" :color="logColor(log.logLevel)">{{ log.logType || log.logLevel || 'INFO' }}</a-tag></div>
              <p>{{ log.content || '-' }}</p>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无执行日志" />
        </section>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskCenterItem, TaskDetailItem, TaskLog } from '../api'
import { useWindowSize } from '@vueuse/core'
import {
  exportCredentials,
  exportFailedItems,
  getCredentialTask,
  getCredentialTaskItems,
  getCredentialTaskLogs,
  getTaskCenter,
  getTaskItems,
  getTaskLogs
} from '../api'
import { itemStatusOptions, taskStatusColor, taskStatusLabel, taskTimeText, taskTypeLabel, triggerTypeLabel } from '../shared'
import CredentialTaskDetail from './task-detail/CredentialTaskDetail.vue'
import DeviceJobTaskDetail from './task-detail/DeviceJobTaskDetail.vue'
import ImportTaskDetail from './task-detail/ImportTaskDetail.vue'

defineOptions({ name: 'TaskCenterDetailDrawer' })

const emit = defineEmits<{ opened: [task: TaskCenterItem] }>()
const visible = ref(false)
const loading = ref(false)
const itemLoading = ref(false)
const task = ref<TaskCenterItem>()
const items = ref<TaskDetailItem[]>([])
const logs = ref<TaskLog[]>([])
const itemTotal = ref(0)
const itemQuery = reactive({ page: 1, size: 10, keyword: '', status: '' })
const credentialOnly = ref(false)
const { width } = useWindowSize()
const drawerWidth = computed(() => Math.min(1080, Math.max(320, width.value - 24)))
const descriptionColumns = computed(() => width.value <= 640 ? 1 : 2)
const canExportFailed = computed(() => task.value?.taskType === 'DEVICE_IMPORT' && Number(task.value.failCount || 0) > 0)
const canExportCredential = computed(() => task.value?.taskType === 'DEVICE_CREDENTIAL_GENERATE' && Number(task.value.successCount || 0) > 0)
const genericColumns: TableColumnData[] = [
  { title: '编号', dataIndex: 'itemNo', width: 120 },
  { title: '标识', dataIndex: 'itemKey', width: 180 },
  { title: '名称', dataIndex: 'itemName', width: 180 },
  { title: '状态', dataIndex: 'status', width: 110 },
  { title: '说明', dataIndex: 'message' }
]

async function open(taskId: string, options: { credentialOnly?: boolean } = {}) {
  credentialOnly.value = Boolean(options.credentialOnly)
  visible.value = true
  loading.value = true
  Object.assign(itemQuery, { page: 1, size: 10, keyword: '', status: '' })
  try {
    const [taskRes, logRes] = await Promise.all([
      credentialOnly.value ? getCredentialTask(taskId) : getTaskCenter(taskId),
      credentialOnly.value ? getCredentialTaskLogs(taskId) : getTaskLogs(taskId)
    ])
    task.value = taskRes.data
    logs.value = [...(logRes.data || [])].reverse()
    await loadItems()
    if (task.value) emit('opened', task.value)
  } finally {
    loading.value = false
  }
}

async function loadItems() {
  if (!task.value?.id) return
  itemLoading.value = true
  try {
    const params = { ...itemQuery, keyword: itemQuery.keyword.trim() || undefined, status: itemQuery.status || undefined }
    const response = credentialOnly.value ? await getCredentialTaskItems(task.value.id, params) : await getTaskItems(task.value.id, params)
    items.value = response.data.records || []
    itemTotal.value = response.data.total || 0
  } finally {
    itemLoading.value = false
  }
}

function searchItems() {
  itemQuery.page = 1
  loadItems()
}

function resetItems() {
  Object.assign(itemQuery, { page: 1, keyword: '', status: '' })
  loadItems()
}

function handlePageSizeChange(size: number) {
  itemQuery.page = 1
  itemQuery.size = size
  loadItems()
}

function downloadFailed() {
  if (task.value) exportFailedItems(task.value.id, task.value.taskNo)
}

function downloadCredentials() {
  if (task.value) exportCredentials(task.value.id, task.value.taskNo)
}

function logColor(level?: string) {
  return ({ INFO: 'arcoblue', WARN: 'orange', ERROR: 'red' } as Record<string, string>)[level || ''] || 'arcoblue'
}

function reset() {
  task.value = undefined
  items.value = []
  logs.value = []
  itemTotal.value = 0
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.detail-loading {
  display: block;
  min-height: 260px;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.stat-strip > div {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 12px;
  background: var(--color-fill-1);
  border-radius: 6px;
}

.stat-strip span {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  font-size: 12px;
  color: var(--color-text-3);
}

.stat-strip strong {
  font-size: 18px;
  color: var(--color-text-1);
}

.overview {
  margin-bottom: 16px;
}

.detail-section {
  padding: 16px 0;
  border-top: 1px solid var(--color-border-2);
}

.detail-section > header,
.log-head {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.detail-section > header {
  min-height: 32px;
  margin-bottom: 12px;
}

.item-filter {
  display: grid;
  grid-template-columns: minmax(180px, 260px) 140px auto auto;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-section :deep(.arco-pagination) {
  justify-content: flex-end;
  margin-top: 14px;
}

.log-section p {
  margin: 6px 0 0;
  color: var(--color-text-2);
  white-space: pre-wrap;
}

.log-head {
  justify-content: flex-start;
}

.log-head time {
  font-size: 12px;
  color: var(--color-text-3);
}

@media (width <= 720px) {
  .stat-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .item-filter {
    grid-template-columns: 1fr 1fr;
  }
}

@media (width <= 430px) {
  .item-filter {
    grid-template-columns: 1fr;
  }
}
</style>
