<template>
  <GiPageLayout margin collapsed>
    <template #left>
      <a-input v-model="treeInputValue" placeholder="输入部门名称搜索" allow-clear :style="{ marginBottom: '8px' }">
        <template #prefix><icon-search /></template>
      </a-input>
      <div class="g-full-column">
        <a-scrollbar style="height: 100%; overflow: auto" outer-style="height: 100%">
          <a-tree ref="treeRef" block-node show-line default-expand-all :data="deptList" :field-names="{
            key: 'id',
            title: 'name',
            children: 'children',
          }" @select="onDeptSelect">
          </a-tree>
        </a-scrollbar>
      </div>
    </template>

    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton v-hasPerm="['system:user:add']" type="add" @click="onAdd"></GiButton>
        <GiButton v-hasPerm="['system:user:import']" type="import" @click="UserImportModalRef?.open()"></GiButton>
        <GiButton v-hasPerm="['system:user:export']" type="export" @click="onExport"></GiButton>
      </a-space>
      <a-space wrap>
        <a-input-group>
          <a-select v-model="queryParams.status" :options="dictData.STATUS" placeholder="用户状态" allow-clear
            style="width: 150px"></a-select>
          <a-input v-model="queryParams.username" placeholder="输入用户名搜索" allow-clear style="max-width: 250px">
          </a-input>
        </a-input-group>
        <GiButton type="search" @click="search"></GiButton>
        <GiButton type="reset" @click="reset"></GiButton>
      </a-space>
    </a-row>

    <a-table class="g-table" row-key="id" :loading="loading" :data="userList" :columns="tableColumns"
      :bordered="{ cell: true }" :scroll="{ x: '100%', y: '100%', minWidth: 1490 }" :pagination="pagination"
      >
    </a-table>

    <UserFormModal ref="UserFormModalRef" @save-success="search"></UserFormModal>
    <UserDetailDrawer ref="UserDetailDrawerRef"></UserDetailDrawer>
    <ResetPasswordModal ref="ResetPasswordModalRef" />
    <UserRoleModal ref="UserRoleModalRef" @save-success="search" />
    <UserImportModal ref="UserImportModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type * as T from '@/apis/system/user'
import { Link, Message, Switch, Tag } from '@arco-design/web-vue'
import { changeUserStatus, deleteUser, exportUsers, getUserList } from '@/apis/system/user'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useDict, useTable } from '@/hooks'
import { useDept } from '@/hooks/app'
import { hasPerm } from '@/utils/permission'
import ResetPasswordModal from './ResetPasswordModal.vue'
import UserDetailDrawer from './UserDetailDrawer.vue'
import UserFormModal from './UserFormModal.vue'
import UserImportModal from './UserImportModal.vue'
import UserRoleModal from './UserRoleModal.vue'

defineOptions({ name: 'SystemUser' })

const { dictData } = useDict(['STATUS'])
const treeRef = useTemplateRef('treeRef')
const UserFormModalRef = useTemplateRef('UserFormModalRef')
const UserDetailDrawerRef = useTemplateRef('UserDetailDrawerRef')
const ResetPasswordModalRef = useTemplateRef<InstanceType<typeof ResetPasswordModal>>('ResetPasswordModalRef')
const UserRoleModalRef = useTemplateRef<InstanceType<typeof UserRoleModal>>('UserRoleModalRef')
const UserImportModalRef = useTemplateRef<InstanceType<typeof UserImportModal>>('UserImportModalRef')
const treeInputValue = ref('')
const selectedDeptId = ref('')

const { deptList, getDeptList } = useDept({
  onSuccess: () => {
    nextTick(() => {
      treeRef.value?.expandAll(true)
    })
  }
})
getDeptList()

const queryParams = reactive({ status: '', username: '' })

const { loading, tableData: userList, pagination, search, fixed, onDelete } = useTable({
  listAPI: (page) => getUserList({
    ...page,
    username: queryParams.username.trim() || undefined,
    status: queryParams.status === '' ? undefined : queryParams.status as Status,
    deptId: selectedDeptId.value || undefined
  }),
  deleteAPI: deleteUser,
  immediate: true
})

const reset = () => {
  queryParams.status = ''
  queryParams.username = ''
  selectedDeptId.value = ''
  search()
}

const onDeptSelect = (keys: Array<string | number>) => {
  selectedDeptId.value = String(keys[0] || '')
  search()
}

const onStatusChange = async (item: T.ListItem, status: Status) => {
  const previous = item.status
  item.status = status
  try {
    await changeUserStatus({ id: item.id, status, username: item.username, phone: item.phone })
    Message.success('状态更新成功')
  } catch {
    item.status = previous
  }
}

const onAdd = () => {
  UserFormModalRef.value?.add()
}

const onEdit = (item: T.ListItem) => {
  UserFormModalRef.value?.edit(item.id)
}

const onDetail = (item: T.ListItem) => {
  UserDetailDrawerRef.value?.open(item.id)
}

const onExport = () => exportUsers({
  username: queryParams.username.trim() || undefined,
  status: queryParams.status === '' ? undefined : queryParams.status as Status,
  deptId: selectedDeptId.value || undefined
})

const canAssignRole = (item: T.ListItem) => item.tenantRole !== 'ADMIN' || item.platformAdmin
const canDelete = (item: T.ListItem) => !item.platformAdmin && item.tenantRole !== 'ADMIN'

const tableColumns: TableColumnData[] = [
  {
    title: '序号',
    width: 68,
    align: 'center',
    render: ({ rowIndex }) => (<span>{rowIndex + 1}</span>)
  },
  {
    title: '用户名',
    dataIndex: 'username',
    width: 120,
    render: ({ record }) => (
      <Link onClick={() => onDetail(record as T.ListItem)}>{record.username}</Link>
    )
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    width: 150,
    render: ({ record }) => (
      <GiCellAvatar avatar={record.avatar} name={record.nickname} />
    )
  },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => <Switch checkedValue="1" uncheckedValue="0" checkedText="正常" uncheckedText="禁用" modelValue={record.status} disabled={record.disabled} onUpdate:modelValue={(value) => onStatusChange(record as T.ListItem, value as Status)} />
  },
  { title: '联系方式', dataIndex: 'phone', width: 180 },
  { title: '部门', dataIndex: 'deptName', width: 180 },
  {
    title: '类型',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <>
        {record.type === 1 && <Tag>系统内置 </Tag>}
        {record.type === 2 && <Tag>自定义 </Tag>}
      </>
    )
  },
  { title: '描述', dataIndex: 'description', width: 200, ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 200 },
  {
    title: '操作',
    width: 160,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => (
      <GiTableActions actions={[
        { key: 'edit', label: '编辑', icon: 'edit', visible: hasPerm('system:user:edit'), onClick: () => onEdit(record as T.ListItem) },
        { key: 'password', label: '重置密码', icon: 'lock', visible: hasPerm('system:user:resetPwd'), onClick: () => ResetPasswordModalRef.value?.open(record as T.ListItem) },
        { key: 'role', label: '分配角色', icon: 'safe', visible: hasPerm('system:user:edit') && canAssignRole(record as T.ListItem), onClick: () => UserRoleModalRef.value?.open(record as T.ListItem) },
        { key: 'delete', label: '删除', icon: 'delete', status: 'danger', visible: hasPerm('system:user:remove') && canDelete(record as T.ListItem), confirm: '确定删除该用户吗?', onClick: () => onDelete(record as T.ListItem) }
      ]}
      />
    )
  }
]
</script>

<style lang="scss" scoped></style>
