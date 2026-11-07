<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space><GiButton v-hasPerm="['system:post:add']" type="add" @click="formModalRef?.add()" /><GiButton v-hasPerm="['system:post:remove']" type="delete" @click="onBatchDelete" /></a-space>
      <a-space><a-input v-model="query.name" placeholder="岗位名称" allow-clear /><a-input v-model="query.code" placeholder="岗位编码" allow-clear /><a-select v-model="query.status" :options="dictData.STATUS" placeholder="状态" allow-clear /><GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" /></a-space>
    </a-row>
    <a-table class="g-table" row-key="id" :data="postList" :columns="columns" :loading="loading" :pagination="pagination" :bordered="{ cell: true }" :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="selectedKeys" @select="select" @select-all="selectAll" />
    <PostFormModal ref="formModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { PostItem } from '@/apis/system/post'
import { deletePost, getPostList } from '@/apis/system/post'

import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useDict, useTable } from '@/hooks'
import PostFormModal from './PostFormModal.vue'

defineOptions({ name: 'SystemPost' })
const formModalRef = useTemplateRef<InstanceType<typeof PostFormModal>>('formModalRef')
const { dictData } = useDict(['STATUS'])
const query = reactive({ name: '', code: '', status: '' })
const { loading, tableData: postList, pagination, selectedKeys, search, select, selectAll, fixed, onDelete, onBatchDelete } = useTable<PostItem>({
  listAPI: (page) => getPostList({ ...page, name: query.name.trim() || undefined, code: query.code.trim() || undefined, status: query.status === '' ? undefined : query.status as Status }),
  deleteAPI: deletePost,
  immediate: true
})
function reset() {
  Object.assign(query, { name: '', code: '', status: '' })
  search()
}
const columns: TableColumnData[] = [
  { title: '岗位名称', dataIndex: 'name' },
  { title: '岗位编码', dataIndex: 'code' },
  { title: '排序', dataIndex: 'sort', width: 90, align: 'center' },
  { title: '状态', width: 90, align: 'center', render: ({ record }) => <GiCellStatus status={record.status} /> },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', width: 92, align: 'center', fixed: fixed.value, render: ({ record }) => (
    <GiTableActions actions={[
      { key: 'edit', label: '编辑', icon: 'edit', onClick: () => formModalRef.value?.edit(String(record.id)) },
      { key: 'delete', label: '删除', icon: 'delete', status: 'danger', confirm: '确定删除该岗位吗?', onClick: () => onDelete(record as PostItem) }
    ]}
    />
  ) }
]
</script>

<style lang="scss" scoped></style>
