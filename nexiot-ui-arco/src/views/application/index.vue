<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['application:application:add']" type="primary" @click="formDrawerRef?.add()">
          <template #icon><icon-plus /></template>新增应用
        </a-button>
      </a-space>
      <a-space wrap>
        <a-input
          v-model.trim="query.appName"
          class="application-index__search"
          placeholder="应用名称"
          allow-clear
          @press-enter="search"
        />
        <a-select
          v-model="query.appStatus"
          class="application-index__status"
          :options="statusOptions"
          placeholder="状态"
          allow-clear
        />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>
    <GiTable
      title="应用列表"
      row-key="appUniqueId"
      :loading="loading"
      :data="tableData"
      :columns="columns"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1100 }"
      @refresh="refresh"
    />
    <ApplicationFormDrawer ref="formDrawerRef" @save-success="search" />
    <ApplicationDetailDrawer ref="detailDrawerRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ApplicationItem } from '@/apis/application/application'
import {
  Button,
  Message,
  Popconfirm,
  Space,
  Tag,
  Tooltip
} from '@arco-design/web-vue'
import {
  IconDelete,
  IconEdit,
  IconEye,
  IconPause,
  IconPlayArrow
} from '@arco-design/web-vue/es/icon'
import {
  deleteApplication,
  getApplicationList,
  updateApplicationStatus
} from '@/apis/application/application'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import ApplicationDetailDrawer from './modules/ApplicationDetailDrawer.vue'
import ApplicationFormDrawer from './modules/ApplicationFormDrawer.vue'

defineOptions({ name: 'ApplicationIndex' })
const formDrawerRef
  = useTemplateRef<InstanceType<typeof ApplicationFormDrawer>>('formDrawerRef')
const detailDrawerRef
  = useTemplateRef<InstanceType<typeof ApplicationDetailDrawer>>(
    'detailDrawerRef'
  )
const query = reactive({
  appName: '',
  appStatus: undefined as number | undefined
})
const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]
const { loading, tableData, pagination, search, refresh } = useTable<ApplicationItem>({
  listAPI: (page) =>
    getApplicationList({
      ...page,
      appName: query.appName || undefined,
      appStatus: query.appStatus
    }),
  immediate: true,
  rowKey: 'appUniqueId'
})
function reset() {
  Object.assign(query, { appName: '', appStatus: undefined })
  search()
}
async function remove(record: ApplicationItem) {
  await deleteApplication(record.appUniqueId)
  Message.success('删除成功')
  search()
  return true
}
async function changeStatus(record: ApplicationItem) {
  const nextStatus = record.appStatus === 0 ? 1 : 0
  await updateApplicationStatus(record.appUniqueId, nextStatus)
  record.appStatus = nextStatus
  Message.success(nextStatus === 0 ? '应用已启用' : '应用已停用')
  return true
}
const columns: TableColumnData[] = [
  {
    title: '应用名称 / 标识',
    dataIndex: 'appName',
    width: 260,
    fixed: 'left',
    render: ({ record }) => (
      <div class="application-index__cell">
        <Button type="text" class="application-index__name" onClick={() => detailDrawerRef.value?.open(record.appUniqueId)}>
          <strong title={record.appName}>{record.appName || '未命名应用'}</strong>
        </Button>
        <code title={record.appUniqueId}>{record.appUniqueId}</code>
      </div>
    )
  },
  {
    title: 'Client ID',
    dataIndex: 'appId',
    width: 180,
    ellipsis: true,
    tooltip: true
  },
  { title: '描述', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '设备数', dataIndex: 'devNum', width: 90, align: 'center' },
  {
    title: '状态',
    dataIndex: 'appStatus',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag color={record.appStatus === 0 ? 'green' : 'red'}>
        <span class="application-index__status-dot" />
        {record.appStatus === 0 ? '启用' : '停用'}
      </Tag>
    )
  },
  { title: '创建时间', dataIndex: 'createDate', width: 180 },
  {
    title: '操作',
    width: 164,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space size={2}>
        <Tooltip content="详情">
          <Button
            type="text"
            shape="circle"
            aria-label="详情"
            onClick={() => detailDrawerRef.value?.open(record.appUniqueId)}
          >
            <IconEye />
          </Button>
        </Tooltip>
        {hasPerm('application:application:edit') && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              aria-label="编辑"
              onClick={() => formDrawerRef.value?.edit(record.appUniqueId)}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {hasPerm('application:application:edit') && (
          <Popconfirm
            content={`确定${record.appStatus === 0 ? '停用' : '启用'}该应用吗？`}
            onBeforeOk={() => changeStatus(record as ApplicationItem)}
          >
            <Tooltip content={record.appStatus === 0 ? '停用' : '启用'}>
              <Button
                type="text"
                status={record.appStatus === 0 ? 'warning' : 'success'}
                shape="circle"
                aria-label={record.appStatus === 0 ? '停用' : '启用'}
              >
                {record.appStatus === 0 ? <IconPause /> : <IconPlayArrow />}
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
        {hasPerm('application:application:remove') && (
          <Popconfirm
            content="确定删除该应用吗？已绑定设备时服务端将拒绝删除。"
            onBeforeOk={() => remove(record as ApplicationItem)}
          >
            <Tooltip content="删除">
              <Button
                type="text"
                status="danger"
                shape="circle"
                aria-label="删除"
              >
                <IconDelete />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]
</script>

<style lang="scss" scoped>
.application-index {
  &__search {
    width: 150px;
  }

  &__status {
    width: 130px;
  }
}

:deep(.application-index__cell) {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  code {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--color-text-3);
    white-space: nowrap;
  }
}

:deep(.application-index__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;

  .arco-btn-content {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

:deep(.application-index__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

@media (width <= 768px) {
  .application-index {
    &__search,
    &__status {
      width: 100%;
    }
  }
}
</style>
