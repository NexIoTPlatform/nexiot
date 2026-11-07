<template>
  <a-modal v-model:visible="visible" :title="title" width="960px" :footer="false" :mask-closable="false" @close="close">
    <a-tabs v-model:active-key="activeTab" @change="handleTabChange">
      <a-tab-pane key="allocated" title="已授权用户">
        <a-row justify="space-between" class="role-user__tools">
          <a-space>
            <a-popconfirm content="确定取消选中用户的授权吗?" @before-ok="cancelSelected">
              <a-button status="danger" :disabled="!allocatedSelectedKeys.length">取消授权</a-button>
            </a-popconfirm>
          </a-space>
          <a-space>
            <a-input v-model="allocatedQuery.username" placeholder="用户名" allow-clear />
            <a-input v-model="allocatedQuery.phone" placeholder="手机号" allow-clear />
            <GiButton type="search" @click="allocatedSearch" />
            <GiButton type="reset" @click="resetAllocated" />
          </a-space>
        </a-row>
        <a-table class="role-user__table" row-key="unionId" :loading="allocatedLoading" :data="allocatedUsers" :columns="allocatedColumns" :pagination="allocatedPagination" :bordered="{ cell: true }" :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="allocatedSelectedKeys" @select="allocatedSelect" @select-all="allocatedSelectAll" />
      </a-tab-pane>
      <a-tab-pane key="unallocated" title="待授权用户">
        <a-row justify="space-between" class="role-user__tools">
          <a-space>
            <a-button type="primary" :disabled="!unallocatedSelectedKeys.length" @click="selectUsers">确认授权</a-button>
          </a-space>
          <a-space>
            <a-input v-model="unallocatedQuery.username" placeholder="用户名" allow-clear />
            <a-input v-model="unallocatedQuery.phone" placeholder="手机号" allow-clear />
            <GiButton type="search" @click="unallocatedSearch" />
            <GiButton type="reset" @click="resetUnallocated" />
          </a-space>
        </a-row>
        <a-table class="role-user__table" row-key="unionId" :loading="unallocatedLoading" :data="unallocatedUsers" :columns="userColumns" :pagination="unallocatedPagination" :bordered="{ cell: true }" :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="unallocatedSelectedKeys" @select="unallocatedSelect" @select-all="unallocatedSelectAll" />
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ListItem, RoleUserItem } from '@/apis/system/role'
import { Message } from '@arco-design/web-vue'
import { cancelRoleUsers, getAllocatedRoleUsers, getUnallocatedRoleUsers, selectRoleUsers } from '@/apis/system/role'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'

defineOptions({ name: 'RoleUserModal' })

const visible = ref(false)
const role = ref<ListItem>()
const activeTab = ref('allocated')
const title = computed(() => role.value ? `分配用户 - ${role.value.name}` : '分配用户')
const allocatedQuery = reactive({ username: '', phone: '' })
const unallocatedQuery = reactive({ username: '', phone: '' })

const allocatedTable = useTable<RoleUserItem>({
  listAPI: (page) => getAllocatedRoleUsers({ ...page, roleId: role.value?.id || '', username: allocatedQuery.username.trim() || undefined, phone: allocatedQuery.phone.trim() || undefined }),
  immediate: false,
  rowKey: 'unionId'
})
const unallocatedTable = useTable<RoleUserItem>({
  listAPI: (page) => getUnallocatedRoleUsers({ ...page, roleId: role.value?.id || '', username: unallocatedQuery.username.trim() || undefined, phone: unallocatedQuery.phone.trim() || undefined }),
  immediate: false,
  rowKey: 'unionId'
})

const { loading: allocatedLoading, tableData: allocatedUsers, pagination: allocatedPagination, selectedKeys: allocatedSelectedKeys, search: allocatedSearch, select: allocatedSelect, selectAll: allocatedSelectAll } = allocatedTable
const { loading: unallocatedLoading, tableData: unallocatedUsers, pagination: unallocatedPagination, selectedKeys: unallocatedSelectedKeys, search: unallocatedSearch, select: unallocatedSelect, selectAll: unallocatedSelectAll } = unallocatedTable

const userColumns: TableColumnData[] = [
  { title: '用户名', dataIndex: 'username' },
  { title: '昵称', dataIndex: 'nickname' },
  { title: '邮箱', dataIndex: 'email' },
  { title: '手机号', dataIndex: 'phone', width: 140 },
  { title: '状态', width: 90, align: 'center', render: ({ record }) => <GiCellStatus status={record.status} /> },
  { title: '创建时间', dataIndex: 'createTime', width: 180 }
]
const allocatedColumns: TableColumnData[] = [
  ...userColumns,
  {
    title: '操作',
    width: 110,
    align: 'center',
    render: ({ record }) => <GiTableActions actions={[{ key: 'cancel', label: '取消授权', icon: 'unlink', status: 'danger', confirm: '确定取消该用户的授权吗?', onClick: () => cancelUsers([String(record.unionId)]) }]} />
  }
]

async function open(item: ListItem) {
  role.value = item
  activeTab.value = 'allocated'
  visible.value = true
  await allocatedSearch()
}

function close() {
  role.value = undefined
  Object.assign(allocatedQuery, { username: '', phone: '' })
  Object.assign(unallocatedQuery, { username: '', phone: '' })
}

function handleTabChange(key: string | number) {
  if (key === 'unallocated') unallocatedSearch()
  else allocatedSearch()
}

function resetAllocated() {
  Object.assign(allocatedQuery, { username: '', phone: '' })
  allocatedSearch()
}

function resetUnallocated() {
  Object.assign(unallocatedQuery, { username: '', phone: '' })
  unallocatedSearch()
}

async function cancelUsers(unionIds: string[]) {
  if (!role.value) return false
  await cancelRoleUsers(role.value.id, unionIds)
  Message.success('取消授权成功')
  await allocatedSearch()
  return true
}

function cancelSelected() {
  return cancelUsers(allocatedSelectedKeys.value.map(String))
}

async function selectUsers() {
  if (!role.value) return
  await selectRoleUsers(role.value.id, unallocatedSelectedKeys.value.map(String))
  Message.success('用户授权成功')
  await Promise.all([unallocatedSearch(), allocatedSearch()])
  activeTab.value = 'allocated'
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.role-user {
  &__tools {
    margin-bottom: 12px;
  }

  &__table {
    min-height: 420px;
  }
}
</style>
