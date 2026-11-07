<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <GiButton v-hasPerm="['monitor:operlog:export']" type="export" @click="handleExport" />
      <a-space wrap>
        <a-input v-model="query.title" placeholder="系统模块" allow-clear />
        <a-input v-model="query.operName" placeholder="操作人员" allow-clear />
        <a-select v-model="query.businessType" :options="businessTypeOptions" placeholder="操作类型" allow-clear />
        <a-select v-model="query.status" :options="statusOptions" placeholder="操作状态" allow-clear />
        <a-range-picker v-model="dateRange" value-format="YYYY-MM-DD" style="width: 240px" />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <a-table
      class="g-table"
      row-key="operId"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1280 }"
    />

    <OperLogDetailDrawer ref="detailDrawerRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { OperLogItem } from '@/apis/system/log'
import { Tag } from '@arco-design/web-vue'
import { exportOperLogs, getOperLogList } from '@/apis/system/log'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import OperLogDetailDrawer from './OperLogDetailDrawer.vue'

defineOptions({ name: 'SystemOperlog' })

const businessTypeOptions = [
  { label: '其他', value: 0 },
  { label: '新增', value: 1 },
  { label: '修改', value: 2 },
  { label: '删除', value: 3 },
  { label: '授权', value: 4 },
  { label: '导出', value: 5 },
  { label: '导入', value: 6 },
  { label: '强退', value: 7 },
  { label: '生成代码', value: 8 },
  { label: '清空数据', value: 9 }
]
const statusOptions = [{ label: '正常', value: 0 }, { label: '异常', value: 1 }]
const query = reactive({ title: '', operName: '', businessType: undefined as number | undefined, status: undefined as number | undefined })
const dateRange = ref<string[]>([])
const detailDrawerRef = useTemplateRef<InstanceType<typeof OperLogDetailDrawer>>('detailDrawerRef')

const { loading, tableData, pagination, search } = useTable<OperLogItem>({
  listAPI: (page) => getOperLogList({
    ...page,
    title: query.title.trim() || undefined,
    operName: query.operName.trim() || undefined,
    businessType: query.businessType,
    status: query.status,
    beginTime: dateRange.value[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
    endTime: dateRange.value[1] ? `${dateRange.value[1]} 23:59:59` : undefined
  })
})

function reset() {
  Object.assign(query, { title: '', operName: '', businessType: undefined, status: undefined })
  dateRange.value = []
  search()
}

async function handleExport() {
  await exportOperLogs({
    title: query.title.trim() || undefined,
    operName: query.operName.trim() || undefined,
    businessType: query.businessType,
    status: query.status,
    beginTime: dateRange.value[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
    endTime: dateRange.value[1] ? `${dateRange.value[1]} 23:59:59` : undefined
  })
}

function getBusinessTypeLabel(value: number) {
  return businessTypeOptions.find((item) => item.value === value)?.label || '其他'
}

const columns: TableColumnData[] = [
  { title: '系统模块', dataIndex: 'title', width: 160, ellipsis: true, tooltip: true },
  { title: '操作类型', width: 100, align: 'center', render: ({ record }) => <Tag color="blue">{getBusinessTypeLabel(record.businessType)}</Tag> },
  { title: '请求方式', dataIndex: 'requestMethod', width: 100, align: 'center' },
  { title: '操作人员', dataIndex: 'operName', width: 140, ellipsis: true, tooltip: true },
  { title: '操作 IP', dataIndex: 'operIp', width: 150 },
  { title: '操作地点', dataIndex: 'operLocation', minWidth: 160, ellipsis: true, tooltip: true },
  { title: '状态', width: 90, align: 'center', render: ({ record }) => record.status === 0 ? <Tag color="green">正常</Tag> : <Tag color="red">异常</Tag> },
  { title: '操作时间', dataIndex: 'operTime', width: 180 },
  {
    title: '操作',
    width: 90,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => <GiTableActions actions={[{ key: 'detail', label: '详情', icon: 'view', visible: hasPerm('monitor:operlog:query'), onClick: () => detailDrawerRef.value?.open(record as OperLogItem) }]} />
  }
]
</script>

<style lang="scss" scoped></style>
