<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool credential-list__toolbar">
      <a-space wrap><a-button v-hasPerm="['system:task:center:credential:add']" type="primary" @click="openCreate"><template #icon><icon-plus /></template>新建生成任务</a-button></a-space>
      <a-space wrap class="credential-list__filters">
        <a-input v-model="query.taskName" allow-clear placeholder="任务名称" class="credential-list__search" @press-enter="search" />
        <a-select v-model="query.productKey" :options="productOptions" allow-clear allow-search placeholder="全部 MQTT 产品" class="credential-list__product" />
        <a-select v-model="query.status" :options="taskStatusOptions" allow-clear placeholder="全部状态" class="credential-list__select" />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="resetQuery" />
      </a-space>
    </a-row>
    <GiTable
      title="设备凭证任务"
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1370 }"
      @refresh="refresh"
    />
    <CredentialTaskModal ref="formRef" @success="handleCreated" />
    <TaskDetailDrawer ref="detailRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskCenterItem } from './api'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Progress, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDownload, IconEye } from '@arco-design/web-vue/es/icon'
import { getProductList } from '@/apis/iot/product'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import { exportCredentials, getCredentialTasks } from './api'
import CredentialTaskModal from './modules/CredentialTaskModal.vue'
import TaskDetailDrawer from './modules/TaskDetailDrawer.vue'
import { progressPercent, taskStatusColor, taskStatusLabel, taskStatusOptions } from './shared'

defineOptions({ name: 'TaskCenterCredentials' })
const products = ref<ProductItem[]>([])
const formRef = useTemplateRef<InstanceType<typeof CredentialTaskModal>>('formRef')
const detailRef = useTemplateRef<InstanceType<typeof TaskDetailDrawer>>('detailRef')
const query = reactive({ taskName: '', productKey: '', status: '' })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const { loading, tableData: rows, pagination, search, refresh } = useTable<TaskCenterItem>({
  listAPI: (page) => getCredentialTasks({ ...page, taskName: query.taskName.trim() || undefined, productKey: query.productKey || undefined, status: query.status || undefined }),
  immediate: true
})

function resetQuery() {
  Object.assign(query, { taskName: '', productKey: '', status: '' })
  search()
}

function openCreate() {
  formRef.value?.open(query.productKey)
}

function openDetail(id: string) {
  detailRef.value?.open(id, { credentialOnly: true })
}

function handleCreated(taskId?: string) {
  search()
  if (taskId) nextTick(() => openDetail(taskId))
}

const columns: TableColumnData[] = [
  { title: '任务名称 / 编号', dataIndex: 'taskName', width: 240, fixed: 'left', render: ({ record }) => (
    <div class="credential-list__cell">
      {hasPerm('system:task:center:credential:detail')
        ? <Button type="text" class="credential-list__name" onClick={() => openDetail(String(record.id))}><strong title={record.taskName || '-'}>{record.taskName || '-'}</strong></Button>
        : <strong title={record.taskName || '-'}>{record.taskName || '-'}</strong>}
      <code title={record.taskNo || '-'}>{record.taskNo || '-'}</code>
    </div>
  ) },
  { title: '所属产品 / ProductKey', dataIndex: 'productKey', width: 240, render: ({ record }) => (
    <div class="credential-list__cell">
      <strong title={record.productName || '-'}>{record.productName || '-'}</strong>
      <code title={record.productKey || '-'}>{record.productKey || '-'}</code>
    </div>
  ) },
  { title: '任务状态', dataIndex: 'status', width: 110, align: 'center', render: ({ record }) => (
    <Tag color={taskStatusColor(record.status)}>
      <span class="credential-list__status-dot" />
      {taskStatusLabel(record.status)}
    </Tag>
  ) },
  { title: '执行进度', width: 170, render: ({ record }) => <Progress percent={progressPercent(record as TaskCenterItem)} size="small" status={record.status === 'FAILED' ? 'danger' : record.status === 'COMPLETED' ? 'success' : 'normal'} /> },
  { title: '成功', width: 80, align: 'center', render: ({ record }) => Number(record.successCount || 0) },
  { title: '失败', width: 80, align: 'center', render: ({ record }) => Number(record.failCount || 0) },
  { title: '总数', width: 80, align: 'center', render: ({ record }) => Number(record.totalCount || 0) },
  { title: '创建人', dataIndex: 'creatorName', width: 130, ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', width: 110, fixed: 'right', align: 'center', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('system:task:center:credential:detail') && <Tooltip content="查看详情"><Button type="text" shape="circle" aria-label="查看任务详情" onClick={() => openDetail(String(record.id))}><IconEye /></Button></Tooltip>}
      {hasPerm('system:task:center:credential:export') && <Tooltip content={Number(record.successCount || 0) > 0 ? '导出三元组' : '暂无成功凭证'}><Button type="text" shape="circle" aria-label="导出三元组" disabled={Number(record.successCount || 0) <= 0} onClick={() => exportCredentials(String(record.id), record.taskNo)}><IconDownload /></Button></Tooltip>}
    </Space>
  ) }
]

getProductList({ page: 1, size: 1000 }).then((response) => products.value = (response.data.records || []).filter((item) => String(item.thirdPlatform || '').toLowerCase() === 'mqtt'))
</script>

<style scoped lang="scss">
.credential-list__filters {
  justify-content: flex-end;
}

:deep(.credential-list__cell) {
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

:deep(.credential-list__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;

  .arco-btn-content {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

:deep(.credential-list__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

.credential-list__select {
  width: 130px;
}

.credential-list__search {
  width: 150px;
}

.credential-list__product {
  width: 220px;
}

@media (width <= 768px) {
  .credential-list__toolbar {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .credential-list__filters {
    width: 100%;
  }

  .credential-list__select,
  .credential-list__search,
  .credential-list__product {
    width: 100%;
  }
}
</style>
