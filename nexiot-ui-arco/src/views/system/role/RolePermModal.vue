<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 20px)" :modal-style="{ maxWidth: '600px' }"
    :mask-closable="true" :body-style="{ maxHeight: '500px' }" @ok="save">
    <a-tree ref="treeRef" v-model:checked-keys="menuIds" size="mini" checkable :check-strictly="true" :data="treeData"
      :field-names="{ key: 'id' }"></a-tree>
  </a-modal>
</template>

<script lang="ts" setup>
import type { MenuOptionsItem } from '@/apis/system/menu'
import { Message } from '@arco-design/web-vue'
import { getRoleMenuTree } from '@/apis/system/menu'
import { updateRoleMenus } from '@/apis/system/role'

defineOptions({ name: 'RolePermModal' })

const treeRef = useTemplateRef('treeRef')
const treeData = ref<MenuOptionsItem[]>([])
const visible = ref(false)
const roleId = ref('')
const roleTitle = ref('')
const menuIds = ref<string[]>([])
const title = computed(() => roleTitle.value ? `分配权限 - ${roleTitle.value}` : '分配权限')
const getTreeData = async (roleId: string) => {
  const res = await getRoleMenuTree(roleId)
  treeData.value = res.data.menus
  menuIds.value = res.data.checkedKeys
  nextTick(() => {
    treeRef.value?.expandAll()
  })
}
const open = async (data: { id: string, title: string }) => {
  menuIds.value = []
  roleId.value = data.id
  roleTitle.value = data.title
  visible.value = true
  await getTreeData(data.id)
}

const save = async () => {
  await updateRoleMenus(roleId.value, menuIds.value)
  Message.success('权限分配成功')
  visible.value = false
}

defineExpose({ open })
</script>

<style lang="scss" scoped></style>
