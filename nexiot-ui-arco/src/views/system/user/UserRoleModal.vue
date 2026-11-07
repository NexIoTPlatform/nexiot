<template>
  <a-modal v-model:visible="visible" :title="title" width="700px" :mask-closable="false" @before-ok="save" @close="close">
    <a-table
      row-key="id"
      :loading="loading"
      :data="roleList"
      :columns="columns"
      :pagination="false"
      :bordered="{ cell: true }"
      :scroll="{ y: 360 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedRoleIds"
      @selection-change="selectedRoleIds = $event.map(String)"
    />
  </a-modal>
</template>

<script setup lang="ts">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ListItem, UserRoleOption } from '@/apis/system/user'
import { Message } from '@arco-design/web-vue'
import { getUserAuthRoles, updateUserAuthRoles } from '@/apis/system/user'

defineOptions({ name: 'UserRoleModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const user = ref<ListItem>()
const roleList = ref<UserRoleOption[]>([])
const selectedRoleIds = ref<string[]>([])
const title = computed(() => user.value ? `分配角色 - ${user.value.nickname}` : '分配角色')
const columns: TableColumnData[] = [
  { title: '角色名称', dataIndex: 'name' },
  { title: '权限字符', dataIndex: 'code' },
  { title: '创建时间', dataIndex: 'createTime', width: 180 }
]

async function open(item: ListItem) {
  visible.value = true
  loading.value = true
  selectedRoleIds.value = []
  try {
    const res = await getUserAuthRoles(item.id)
    user.value = res.data.user
    roleList.value = res.data.roles
    selectedRoleIds.value = res.data.roles.filter((role) => role.assigned).map((role) => role.id)
  } finally {
    loading.value = false
  }
}

function close() {
  user.value = undefined
  roleList.value = []
  selectedRoleIds.value = []
}

async function save() {
  if (!user.value) return false
  await updateUserAuthRoles(user.value.unionId, selectedRoleIds.value)
  Message.success('角色分配成功')
  emit('save-success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped></style>
