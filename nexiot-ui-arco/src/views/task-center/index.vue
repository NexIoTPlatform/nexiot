<template>
  <GiPageLayout margin>
    <div class="task-records">
      <!-- <TaskOverviewCharts :rows="rows" /> -->
      <a-row justify="end" class="g-row-tool task-records__toolbar">
        <a-space wrap class="task-records__filters">
          <a-select v-model="query.taskType" :options="taskTypeOptions" allow-clear placeholder="全部类型" class="task-records__select" />
          <a-input v-model="query.taskName" allow-clear placeholder="任务名称" class="task-records__search" @press-enter="search" />
          <a-select v-model="query.productKey" :options="productOptions" allow-clear allow-search placeholder="全部产品" class="task-records__product" />
          <a-select v-model="query.status" :options="taskStatusOptions" allow-clear placeholder="全部状态" class="task-records__select" />
          <GiButton type="search" @click="search" />
          <GiButton type="reset" @click="resetQuery" />
        </a-space>
      </a-row>
      <GiTable
        title="任务记录"
        row-key="id"
        :data="rows"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: '100%', y: '100%', minWidth: 1670 }"
        @refresh="refresh"
      />
    </div>
    <TaskDetailDrawer ref="detailRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskCenterItem } from './api'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Progress, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconEye } from '@arco-design/web-vue/es/icon'
import { getProductList } from '@/apis/iot/product'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import { getTaskCenterList } from './api'
import TaskDetailDrawer from './modules/TaskDetailDrawer.vue'
// import TaskOverviewCharts from './modules/TaskOverviewCharts.vue'
import { progressPercent, taskStatusColor, taskStatusLabel, taskStatusOptions, taskTypeLabel, taskTypeOptions, triggerTypeLabel } from './shared'

defineOptions({ name: 'TaskCenterRecords' })
const route = useRoute()
const products = ref<ProductItem[]>([])
const detailRef = useTemplateRef<InstanceType<typeof TaskDetailDrawer>>('detailRef')
const query = reactive({ taskType: '', taskName: '', productKey: '', status: '' })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const { loading, tableData: rows, pagination, search, refresh } = useTable<TaskCenterItem>({
  listAPI: (page) => getTaskCenterList({ ...page, taskType: query.taskType || undefined, taskName: query.taskName.trim() || undefined, productKey: query.productKey || undefined, status: query.status || undefined }),
  immediate: true
})

function resetQuery() {
  Object.assign(query, { taskType: '', taskName: '', productKey: '', status: '' })
  search()
}

function openDetail(id: string) {
  detailRef.value?.open(id)
}

const columns: TableColumnData[] = [
  { title: '任务名称 / 编号', dataIndex: 'taskName', width: 240, fixed: 'left', render: ({ record }) => (
    <div class="task-records__cell">
      {hasPerm('system:task:center:detail')
        ? <Button type="text" class="task-records__name" onClick={() => openDetail(String(record.id))}><strong title={record.taskName || '-'}>{record.taskName || '-'}</strong></Button>
        : <strong title={record.taskName || '-'}>{record.taskName || '-'}</strong>}
      <code title={record.taskNo || '-'}>{record.taskNo || '-'}</code>
    </div>
  ) },
  { title: '任务类型', dataIndex: 'taskType', width: 130, render: ({ record }) => <Tag size="small" color="arcoblue">{taskTypeLabel(record.taskType)}</Tag> },
  { title: '所属产品 / ProductKey', dataIndex: 'productKey', width: 240, render: ({ record }) => (
    <div class="task-records__cell">
      <strong title={record.productName || '-'}>{record.productName || '-'}</strong>
      <code title={record.productKey || '-'}>{record.productKey || '-'}</code>
    </div>
  ) },
  { title: '触发方式', dataIndex: 'triggerType', width: 110, render: ({ record }) => triggerTypeLabel(record.triggerType) },
  { title: '任务状态', dataIndex: 'status', width: 110, align: 'center', render: ({ record }) => (
    <Tag color={taskStatusColor(record.status)}>
      <span class="task-records__status-dot" />
      {taskStatusLabel(record.status)}
    </Tag>
  ) },
  { title: '执行进度', width: 170, render: ({ record }) => <Progress percent={progressPercent(record as TaskCenterItem)} size="small" status={record.status === 'FAILED' ? 'danger' : record.status === 'COMPLETED' ? 'success' : 'normal'} /> },
  { title: '成功', width: 80, align: 'center', render: ({ record }) => Number(record.successCount || 0) },
  { title: '失败', width: 80, align: 'center', render: ({ record }) => Number(record.failCount || 0) },
  { title: '总数', width: 80, align: 'center', render: ({ record }) => Number(record.totalCount || 0) },
  { title: '创建人', dataIndex: 'creatorName', width: 130, ellipsis: true, tooltip: true },
  { title: '开始时间', dataIndex: 'startTime', width: 180, render: ({ record }) => record.startTime || '未开始' },
  { title: '结束时间', dataIndex: 'endTime', width: 180, render: ({ record }) => record.endTime || '-' },
  { title: '操作', width: 80, fixed: 'right', align: 'center', render: ({ record }) => hasPerm('system:task:center:detail') ? <Space size={2}><Tooltip content="查看详情"><Button type="text" shape="circle" aria-label="查看任务详情" onClick={() => openDetail(String(record.id))}><IconEye /></Button></Tooltip></Space> : null }
]

watch(() => route.query.taskId, (taskId) => {
  if (taskId) nextTick(() => openDetail(String(taskId)))
}, { immediate: true })

getProductList({ page: 1, size: 1000 }).then((response) => products.value = response.data.records || [])
</script>

<style scoped lang="scss">
.task-records {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.task-records__toolbar {
  flex: 0 0 auto;
}

:deep(.task-records__cell) {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong,
  code {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  code {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

:deep(.task-records__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;

  .arco-btn-content {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

:deep(.task-records__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

.task-records__select {
  width: 130px;
}

.task-records__search {
  width: 150px;
}

.task-records__product {
  width: 220px;
}

@media (width <= 768px) {
  .task-records {
    overflow: auto;
  }

  .task-records__toolbar {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .task-records__filters {
    width: 100%;
  }

  .task-records__select,
  .task-records__search,
  .task-records__product {
    width: 100%;
  }
}
</style>
