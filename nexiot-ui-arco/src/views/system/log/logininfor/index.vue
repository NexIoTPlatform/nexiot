<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton v-hasPerm="['monitor:logininfor:remove']" type="delete" @click="onBatchDelete" />
        <a-popconfirm content="清空后无法恢复，确定清空全部登录日志吗？" @before-ok="handleClean">
          <a-button v-hasPerm="['monitor:logininfor:remove']" status="danger">
            <template #icon><icon-delete /></template>
            清空
          </a-button>
        </a-popconfirm>
        <GiButton v-hasPerm="['monitor:logininfor:export']" type="export" @click="handleExport" />
      </a-space>
      <a-space wrap>
        <a-input v-model="query.userName" placeholder="用户名称" allow-clear />
        <a-input v-model="query.ipaddr" placeholder="登录 IP" allow-clear />
        <a-select v-model="query.status" :options="statusOptions" placeholder="登录状态" allow-clear />
        <a-range-picker v-model="dateRange" value-format="YYYY-MM-DD" style="width: 240px" />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <a-table
      class="g-table"
      row-key="infoId"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1250 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
    />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { LoginLogItem } from '@/apis/system/log'
import { Message, Tag } from '@arco-design/web-vue'
import { cleanLoginLogs, deleteLoginLogs, exportLoginLogs, getLoginLogList } from '@/apis/system/log'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'

defineOptions({ name: 'SystemLogininfor' })

const statusOptions = [
  { label: '成功', value: '0' },
  { label: '失败', value: '1' }
]
const query = reactive({ userName: '', ipaddr: '', status: undefined as string | undefined })
const dateRange = ref<string[]>([])

const { loading, tableData, pagination, selectedKeys, search, select, selectAll, fixed, onDelete, onBatchDelete, refresh } = useTable<LoginLogItem>({
  listAPI: (page) => getLoginLogList({
    ...page,
    userName: query.userName.trim() || undefined,
    ipaddr: query.ipaddr.trim() || undefined,
    status: query.status,
    beginTime: dateRange.value[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
    endTime: dateRange.value[1] ? `${dateRange.value[1]} 23:59:59` : undefined
  }),
  deleteAPI: deleteLoginLogs,
  rowKey: 'infoId'
})

function reset() {
  Object.assign(query, { userName: '', ipaddr: '', status: undefined })
  dateRange.value = []
  search()
}

async function handleClean() {
  await cleanLoginLogs()
  selectedKeys.value = []
  refresh()
  Message.success('登录日志已清空')
  return true
}

async function handleExport() {
  await exportLoginLogs({
    userName: query.userName.trim() || undefined,
    ipaddr: query.ipaddr.trim() || undefined,
    status: query.status,
    beginTime: dateRange.value[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
    endTime: dateRange.value[1] ? `${dateRange.value[1]} 23:59:59` : undefined
  })
}

const columns: TableColumnData[] = [
  { title: '用户名称', dataIndex: 'userName', width: 150, ellipsis: true, tooltip: true },
  {
    title: '登录状态',
    width: 100,
    align: 'center',
    render: ({ record }) => String(record.status) === '0' ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag>
  },
  { title: '登录 IP', dataIndex: 'ipaddr', width: 150 },
  { title: '登录地点', dataIndex: 'loginLocation', width: 160, ellipsis: true, tooltip: true },
  { title: '浏览器', dataIndex: 'browser', width: 140, ellipsis: true, tooltip: true },
  { title: '操作系统', dataIndex: 'os', width: 170, ellipsis: true, tooltip: true },
  { title: '提示信息', dataIndex: 'msg', minWidth: 220, ellipsis: true, tooltip: true },
  { title: '登录时间', dataIndex: 'loginTime', width: 180 },
  {
    title: '操作',
    width: 90,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => <GiTableActions actions={[{ key: 'delete', label: '删除', icon: 'delete', status: 'danger', visible: hasPerm('monitor:logininfor:remove'), confirm: '确定删除该登录日志吗？', onClick: () => onDelete(record as LoginLogItem) }]} />
  }
]
</script>

<style lang="scss" scoped></style>
