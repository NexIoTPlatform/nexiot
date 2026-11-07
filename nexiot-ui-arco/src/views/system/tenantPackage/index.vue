<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton v-hasPerm="['system:tenant-package:add']" type="add" @click="handleAdd" />
        <GiButton v-hasPerm="['system:tenant-package:remove']" type="delete" @click="onBatchDelete" />
      </a-space>
      <a-space wrap>
        <a-input v-model="query.name" placeholder="套餐名称" allow-clear />
        <a-select v-model="query.status" :options="statusOptions" placeholder="状态" allow-clear />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>
    <a-table class="g-table" row-key="id" :data="packageList" :columns="columns" :loading="loading"
      :bordered="{ cell: true }" :pagination="pagination" :scroll="{ x: '100%', y: '100%', minWidth: 1100 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="selectedKeys"
      @select="select" @select-all="selectAll" />
    <TenantPackageFormModal ref="formModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TenantPackageItem } from '@/apis/system/tenantPackage'
import { Message, Switch } from '@arco-design/web-vue'
import {
  changeTenantPackageStatus,
  deleteTenantPackage,
  getTenantPackageList
} from '@/apis/system/tenantPackage'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import TenantPackageFormModal from './TenantPackageFormModal.vue'

defineOptions({ name: 'SystemTenantPackage' })

const formModalRef = useTemplateRef<InstanceType<typeof TenantPackageFormModal>>('formModalRef')
const statusOptions = [{ label: '正常', value: 0 }, { label: '停用', value: 1 }]
const query = reactive({ name: '', status: undefined as number | undefined })

const { loading, tableData: packageList, pagination, selectedKeys, search, select, selectAll, fixed, onDelete, onBatchDelete } = useTable<TenantPackageItem>({
  listAPI: (page) => getTenantPackageList({ ...page, name: query.name.trim() || undefined, status: query.status }),
  deleteAPI: deleteTenantPackage,
  immediate: true
})

function reset() {
  Object.assign(query, { name: '', status: undefined })
  search()
}

function handleAdd() {
  formModalRef.value?.add()
}

function handleEdit(item: TenantPackageItem) {
  formModalRef.value?.edit(item.id)
}

async function handleStatus(item: TenantPackageItem, status: 0 | 1) {
  const previous = item.status
  item.status = status
  try {
    await changeTenantPackageStatus({ id: item.id, status })
    Message.success(status === 0 ? '启用成功' : '停用成功')
  } catch {
    item.status = previous
  }
}

const columns: TableColumnData[] = [
  { title: '套餐编号', dataIndex: 'id', width: 160 },
  { title: '套餐名称', dataIndex: 'name', width: 180 },
  { title: '菜单数', dataIndex: 'menuCount', width: 100, align: 'center' },
  { title: '关联租户数', dataIndex: 'tenantCount', width: 120, align: 'center' },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => <Switch modelValue={record.status} checkedValue={0} uncheckedValue={1} checkedText="正常" uncheckedText="停用" disabled={!hasPerm('system:tenant-package:changeStatus')} onUpdate:modelValue={(value) => handleStatus(record as TenantPackageItem, value as 0 | 1)} />
  },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  {
    title: '操作',
    width: 92,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => (
      <GiTableActions actions={[
        { key: 'edit', label: '编辑', icon: 'edit', visible: hasPerm('system:tenant-package:edit'), onClick: () => handleEdit(record as TenantPackageItem) },
        { key: 'delete', label: '删除', icon: 'delete', status: 'danger', visible: hasPerm('system:tenant-package:remove'), confirm: '确定删除该套餐吗?', onClick: () => onDelete(record as TenantPackageItem) }
      ]}
      />
    )
  }
]
</script>

<style lang="scss" scoped></style>
