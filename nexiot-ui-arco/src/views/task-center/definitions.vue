<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool definition-list__toolbar">
      <a-space wrap><a-button v-hasPerm="['system:task:center:definition:add']" type="primary" @click="openCreate"><template #icon><icon-plus /></template>新建设备作业</a-button></a-space>
      <a-space wrap class="definition-list__filters">
        <a-input v-model="query.taskName" allow-clear placeholder="任务名称" class="definition-list__search" @press-enter="search" />
        <a-select v-model="query.productKey" :options="productOptions" allow-clear allow-search placeholder="全部产品" class="definition-list__product" />
        <a-select v-model="query.scheduleType" :options="scheduleTypeOptions" allow-clear placeholder="调度方式" class="definition-list__select" />
        <a-select v-model="query.scheduleStatus" :options="scheduleStatusOptions" allow-clear placeholder="调度状态" class="definition-list__select" />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="resetQuery" />
      </a-space>
    </a-row>
    <GiTable
      title="设备作业"
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1570 }"
      @refresh="refresh"
    />
    <DeviceJobModal ref="jobModalRef" @success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskDefinition } from './api'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Doption, Dropdown, Message, Modal, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye, IconMore, IconPauseCircle, IconPlayArrow } from '@arco-design/web-vue/es/icon'
import { getProductList } from '@/apis/iot/product'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import { deleteTaskDefinitions, getTaskDefinitions, runTaskDefinition, updateTaskDefinitionSchedule } from './api'
import DeviceJobModal from './modules/DeviceJobModal.vue'
import { definitionNextTime, scheduleStatusLabel, scheduleStatusOptions, scheduleTypeLabel, scheduleTypeOptions } from './shared'

defineOptions({ name: 'TaskCenterDefinitions' })
const router = useRouter()
const products = ref<ProductItem[]>([])
const jobModalRef = useTemplateRef<InstanceType<typeof DeviceJobModal>>('jobModalRef')
const query = reactive({ taskName: '', productKey: '', scheduleType: '', scheduleStatus: '' })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const { loading, tableData: rows, pagination, search, refresh } = useTable<TaskDefinition>({
  listAPI: (page) => getTaskDefinitions({ ...page, taskType: 'DEVICE_FUNCTION_JOB', taskName: query.taskName.trim() || undefined, productKey: query.productKey || undefined, scheduleType: query.scheduleType || undefined, scheduleStatus: query.scheduleStatus || undefined }),
  immediate: true
})

function resetQuery() {
  Object.assign(query, { taskName: '', productKey: '', scheduleType: '', scheduleStatus: '' })
  search()
}

function openCreate() {
  jobModalRef.value?.add()
}

function runDefinition(row: TaskDefinition) {
  Modal.warning({
    title: '确认立即执行该设备作业？',
    content: `${row.taskName || '设备作业'}将按当前保存的设备范围和指令创建一条执行记录。`,
    hideCancel: false,
    onBeforeOk: async () => {
      const response = await runTaskDefinition(row.id)
      Message.success(response.message || '设备作业已开始执行')
      refresh()
      return true
    }
  })
}

async function toggleSchedule(row: TaskDefinition) {
  const status = row.scheduleStatus === 'ENABLED' ? 'PAUSED' : 'ENABLED'
  await updateTaskDefinitionSchedule(row.id, status)
  Message.success(status === 'ENABLED' ? '定时调度已启用' : '定时调度已暂停')
  refresh()
}

function removeDefinition(row: TaskDefinition) {
  Modal.warning({
    title: '确认删除设备作业？',
    content: row.taskName || '设备作业',
    hideCancel: false,
    onBeforeOk: async () => {
      await deleteTaskDefinitions([row.id])
      Message.success('设备作业已删除')
      search()
      return true
    }
  })
}

function openLastTask(taskId: string) {
  const target = router.getRoutes().find((route) => route.meta.sourceComponent === 'system/taskCenter/index')
  router.push({ path: target?.path || '/taskCenter/center', query: { taskId } })
}

function handleMore(value: string | number | Record<string, any> | undefined, row: TaskDefinition) {
  if (value === 'schedule') toggleSchedule(row)
  if (value === 'last' && row.lastTaskId) openLastTask(row.lastTaskId)
  if (value === 'delete') removeDefinition(row)
}

const columns: TableColumnData[] = [
  { title: '作业名称 / 编号', dataIndex: 'taskName', width: 240, fixed: 'left', render: ({ record }) => (
    <div class="definition-list__cell">
      {hasPerm('system:task:center:definition:edit')
        ? <Button type="text" class="definition-list__name" onClick={() => jobModalRef.value?.edit(String(record.id))}><strong title={record.taskName || '-'}>{record.taskName || '-'}</strong></Button>
        : <strong title={record.taskName || '-'}>{record.taskName || '-'}</strong>}
      <code title={record.definitionNo || '-'}>{record.definitionNo || '-'}</code>
    </div>
  ) },
  { title: '所属产品 / ProductKey', dataIndex: 'productKey', width: 240, render: ({ record }) => (
    <div class="definition-list__cell">
      <strong title={record.productName || '-'}>{record.productName || '-'}</strong>
      <code title={record.productKey || '-'}>{record.productKey || '-'}</code>
    </div>
  ) },
  { title: '调度方式', dataIndex: 'scheduleType', width: 110, render: ({ record }) => <Tag size="small" color="arcoblue">{scheduleTypeLabel(record.scheduleType)}</Tag> },
  { title: '调度状态', dataIndex: 'scheduleStatus', width: 110, align: 'center', render: ({ record }) => record.scheduleType === 'CRON'
    ? (
        <Tag color={record.scheduleStatus === 'ENABLED' ? 'green' : 'orange'}>
          <span class="definition-list__status-dot" />
          {scheduleStatusLabel(record.scheduleStatus)}
        </Tag>
      )
    : '-' },
  { title: 'Cron 表达式', dataIndex: 'cronExpression', width: 180, ellipsis: true, tooltip: true, render: ({ record }) => record.scheduleType === 'CRON' ? record.cronExpression || '-' : '-' },
  { title: '下次执行', width: 180, render: ({ record }) => definitionNextTime(record as TaskDefinition) },
  { title: '最近执行', dataIndex: 'lastExecuteTime', width: 180, render: ({ record }) => record.lastExecuteTime || '未执行' },
  { title: '创建人', dataIndex: 'creatorName', width: 130, ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', width: 138, fixed: 'right', align: 'center', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('system:task:center:definition:run') && <Tooltip content="立即执行"><Button type="text" shape="circle" aria-label="立即执行" onClick={() => runDefinition(record as TaskDefinition)}><IconPlayArrow /></Button></Tooltip>}
      {hasPerm('system:task:center:definition:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑设备作业" onClick={() => jobModalRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
      <Dropdown
        trigger="click"
        onSelect={(value) => handleMore(value, record as TaskDefinition)}
        v-slots={{ content: () => (
          <>
            {record.scheduleType === 'CRON' && hasPerm('system:task:center:definition:edit') && (
              <Doption value="schedule">
                <IconPauseCircle />
                {' '}
                {record.scheduleStatus === 'ENABLED' ? '暂停调度' : '启用调度'}
              </Doption>
            )}
            {record.lastTaskId && hasPerm('system:task:center:detail') && (
              <Doption value="last">
                <IconEye />
                {' '}
                最近记录
              </Doption>
            )}
            {hasPerm('system:task:center:definition:remove') && (
              <Doption value="delete" class="definition-list__danger-option">
                <IconDelete />
                {' '}
                删除
              </Doption>
            )}
          </>
        ) }}
      >
        <Tooltip content="更多操作"><Button type="text" shape="circle" aria-label="更多操作"><IconMore /></Button></Tooltip>
      </Dropdown>
    </Space>
  ) }
]

getProductList({ page: 1, size: 1000 }).then((response) => products.value = response.data.records || [])
</script>

<style scoped lang="scss">
.definition-list__filters {
  justify-content: flex-end;
}

:deep(.definition-list__cell) {
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

:deep(.definition-list__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;

  .arco-btn-content {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

:deep(.definition-list__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

.definition-list__select {
  width: 130px;
}

.definition-list__search {
  width: 150px;
}

.definition-list__product {
  width: 220px;
}

// 下拉菜单挂载到 body，使用本页面专属类名限定样式范围。
:global(.definition-list__danger-option) {
  color: rgb(var(--danger-6));
}

@media (width <= 768px) {
  .definition-list__toolbar {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .definition-list__filters {
    width: 100%;
  }

  .definition-list__select,
  .definition-list__search,
  .definition-list__product {
    width: 100%;
  }
}
</style>
