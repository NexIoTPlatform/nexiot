<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton v-hasPerm="['system:role:add']" type="add" @click="onAdd"></GiButton>
        <GiButton v-hasPerm="['system:role:remove']" type="delete" @click="onBatchDelete"></GiButton>
        <GiButton v-hasPerm="['system:role:export']" type="export" @click="onExport"></GiButton>
      </a-space>

      <a-space wrap>
        <a-input-group>
          <a-select v-model="queryParams.status" :options="dictData.STATUS" placeholder="角色状态" allow-clear
            style="width: 120px"></a-select>
          <a-input v-model="queryParams.name" placeholder="输入角色名搜索" allow-clear style="width: 250px"></a-input>
        </a-input-group>
        <GiButton type="search" @click="search"></GiButton>
        <GiButton type="reset" @click="reset"></GiButton>
      </a-space>
    </a-row>

    <a-table class="g-table" row-key="id" :data="roleList" :columns="tableColumns" :bordered="{ cell: true }"
      :loading="loading" :scroll="{ x: '100%', y: '100%', minWidth: 1200 }" :pagination="pagination"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="selectedKeys" @select="select"
      @select-all="selectAll">
    </a-table>

    <RoleFormModal ref="RoleFormModalRef" @save-success="search"></RoleFormModal>
    <RolePermModal ref="RolePermModalRef"></RolePermModal>
    <RoleDataScopeModal ref="RoleDataScopeModalRef" />
    <RoleUserModal ref="RoleUserModalRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type * as T from '@/apis/system/role'
import { Message, Switch } from '@arco-design/web-vue'
import { changeRoleStatus, deleteRole, exportRoles, getRoleList } from '@/apis/system/role'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useDict, useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import RoleDataScopeModal from './RoleDataScopeModal.vue'
import RoleFormModal from './RoleFormModal.vue'
import RolePermModal from './RolePermModal.vue'
import RoleUserModal from './RoleUserModal.vue'

defineOptions({ name: 'SystemRole' })

const { dictData } = useDict(['STATUS'])
const RoleFormModalRef = useTemplateRef('RoleFormModalRef')
const RolePermModalRef = useTemplateRef('RolePermModalRef')
const RoleDataScopeModalRef = useTemplateRef<InstanceType<typeof RoleDataScopeModal>>('RoleDataScopeModalRef')
const RoleUserModalRef = useTemplateRef<InstanceType<typeof RoleUserModal>>('RoleUserModalRef')

const queryParams = reactive({
  name: '',
  status: ''
})

const { loading, tableData: roleList, pagination, selectedKeys, search, select, selectAll, fixed, onDelete, onBatchDelete } = useTable({
  listAPI: (page) => getRoleList({
    ...page,
    name: queryParams.name.trim() || undefined,
    status: queryParams.status === '' ? undefined : queryParams.status as Status
  }),
  deleteAPI: deleteRole,
  immediate: true
})

const reset = () => {
  queryParams.name = ''
  queryParams.status = ''
  search()
}

const onAdd = () => {
  RoleFormModalRef.value?.add()
}

const onEdit = (item: T.ListItem) => {
  RoleFormModalRef.value?.edit(item.id)
}

const onPerm = (item: T.ListItem) => {
  RolePermModalRef.value?.open({ id: item.id, title: item.name })
}

const onExport = () => exportRoles({
  name: queryParams.name.trim() || undefined,
  status: queryParams.status === '' ? undefined : queryParams.status as Status
})

const onStatusChange = async (item: T.ListItem, status: Status) => {
  const previous = item.status
  item.status = status
  try {
    await changeRoleStatus(item.id, status)
    Message.success('状态更新成功')
  } catch {
    item.status = previous
  }
}

const tableColumns: TableColumnData[] = [
  {
    title: '序号',
    width: 68,
    align: 'center',
    render: ({ rowIndex }) => <span>{rowIndex + 1}</span>
  },
  { title: '角色名称', dataIndex: 'name' },
  { title: '角色编码', dataIndex: 'code', width: 150 },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => <Switch checkedValue="1" uncheckedValue="0" checkedText="正常" uncheckedText="停用" modelValue={record.status} disabled={record.disabled || !hasPerm('system:role:edit')} onUpdate:modelValue={(value) => onStatusChange(record as T.ListItem, value as Status)} />
  },
  { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  {
    title: '操作',
    width: 196,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => (
      <GiTableActions actions={[
        { key: 'edit', label: '编辑', icon: 'edit', visible: hasPerm('system:role:edit'), disabled: record.disabled, onClick: () => onEdit(record as T.ListItem) },
        { key: 'permission', label: '分配权限', icon: 'safe', visible: hasPerm('system:role:edit'), disabled: record.disabled, onClick: () => onPerm(record as T.ListItem) },
        { key: 'scope', label: '数据权限', icon: 'storage', visible: hasPerm('system:role:edit'), disabled: record.disabled, onClick: () => RoleDataScopeModalRef.value?.open(record as T.ListItem) },
        { key: 'users', label: '分配用户', icon: 'users', visible: hasPerm('system:role:edit'), disabled: record.disabled, onClick: () => RoleUserModalRef.value?.open(record as T.ListItem) },
        { key: 'delete', label: '删除', icon: 'delete', status: 'danger', visible: hasPerm('system:role:remove'), disabled: record.disabled, confirm: '确定删除该角色吗?', onClick: () => onDelete(record as T.ListItem) }
      ]}
      />
    )
  }
]
</script>

<style lang="scss" scoped></style>
