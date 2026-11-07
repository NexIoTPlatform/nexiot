<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton type="add" @click="onAdd"></GiButton>
        <GiButton type="delete" @click="onBatchDelete"></GiButton>
      </a-space>

      <a-space wrap>
        <a-input-group>
          <a-select v-model="queryParams.status" :options="dictData.STATUS" placeholder="部门状态"
            style="width: 120px"></a-select>
          <a-input v-model="queryParams.name" placeholder="输入部门名称搜索" allow-clear style="width: 250px">
            <template #prefix><icon-search /></template>
          </a-input>
        </a-input-group>
        <GiButton type="search" @click="search"></GiButton>
        <GiButton type="reset" @click="reset"></GiButton>
      </a-space>
    </a-row>

    <a-table ref="tableRef" class="g-table" row-key="id" :bordered="{ cell: true }" :data="deptList" :loading="loading"
      :columns="tableColumns" :scroll="{ x: '100%', y: '100%', minWidth: 1000 }" :pagination="false"
      :row-selection="{ type: 'checkbox', showCheckedAll: false }" :selected-keys="selectedKeys" @select="select"
      @select-all="selectAll">
      <template #expand-icon="{ expanded }">
        <IconDown v-if="expanded" />
        <IconRight v-else />
      </template>
    </a-table>

    <DeptFormModal ref="DeptFormModalRef" @save-success="search"></DeptFormModal>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type * as T from '@/apis/system/dept'
import { deleteDept, getDeptList } from '@/apis/system/dept'

import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useDict, useTable } from '@/hooks'

import DeptFormModal from './DeptFormModal.vue'

defineOptions({ name: 'SystemDept' })

const { dictData } = useDict(['STATUS'])
const DeptFormModalRef = useTemplateRef('DeptFormModalRef')
const tableRef = useTemplateRef('tableRef')

const queryParams = reactive({
  name: '',
  status: ''
})

const { loading, tableData: deptList, selectedKeys, search, select, selectAll, fixed, onDelete, onBatchDelete } = useTable({
  listAPI: () => getDeptList({
    name: queryParams.name.trim() || undefined,
    status: queryParams.status === '' ? undefined : queryParams.status as Status
  }),
  deleteAPI: deleteDept,
  immediate: true,
  onSuccess: () => {
    nextTick(() => {
      tableRef.value?.expandAll(true)
    })
  }
})

const reset = () => {
  queryParams.name = ''
  queryParams.status = ''
  search()
}

const onAdd = (parentId?: string) => {
  DeptFormModalRef.value?.add(parentId)
}

const onEdit = (item: T.ListItem) => {
  DeptFormModalRef.value?.edit(item.id)
}

const tableColumns: TableColumnData[] = [
  { title: '部门名称', dataIndex: 'name', width: 160 },
  { title: '排序', dataIndex: 'sort', width: 100, align: 'center' },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => <GiCellStatus status={record.status} />
  },
  { title: '负责人', dataIndex: 'leader', width: 120 },
  { title: '联系电话', dataIndex: 'phone', width: 150 },
  { title: '邮箱', dataIndex: 'email', width: 200, ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 200 },
  {
    title: '操作',
    width: 126,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => (
      <GiTableActions actions={[
        { key: 'edit', label: '编辑', icon: 'edit', disabled: record.disabled, onClick: () => onEdit(record as T.ListItem) },
        { key: 'add', label: '新增子部门', icon: 'add', visible: Boolean(record.parentId), disabled: record.disabled, onClick: () => onAdd(String(record.id)) },
        { key: 'delete', label: '删除', icon: 'delete', status: 'danger', disabled: record.disabled, confirm: '您确定要删除该项吗?', onClick: () => onDelete(record as T.ListItem) }
      ]}
      />
    )
  }
]
</script>

<style lang="scss" scoped></style>
