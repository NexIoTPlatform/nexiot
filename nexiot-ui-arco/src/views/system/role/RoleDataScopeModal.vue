<template>
  <a-modal v-model:visible="visible" title="分配数据权限" width="620px" :mask-closable="false" @before-ok="save" @close="close">
    <a-spin :loading="loading" class="role-data-scope__spin">
      <a-form :model="form" auto-label-width>
        <a-form-item label="角色名称"><a-input :model-value="roleName" disabled /></a-form-item>
        <a-form-item label="权限范围">
          <a-select v-model="form.dataScope" :options="scopeOptions" />
        </a-form-item>
        <a-form-item v-if="form.dataScope === '2'" label="数据权限">
          <div class="role-data-scope__tree-panel">
            <a-space class="role-data-scope__tools">
              <a-checkbox v-model="form.deptCheckStrictly">父子联动</a-checkbox>
              <a-button size="mini" @click="expandedKeys = collectIds(deptTree)">展开</a-button>
              <a-button size="mini" @click="expandedKeys = []">折叠</a-button>
              <a-button size="mini" @click="checkedKeys = collectIds(deptTree)">全选</a-button>
              <a-button size="mini" @click="checkedKeys = []">清空</a-button>
            </a-space>
            <a-tree
              v-model:checked-keys="checkedKeys"
              v-model:expanded-keys="expandedKeys"
              :data="deptTree"
              :field-names="{ key: 'id', title: 'title', children: 'children' }"
              :check-strictly="!form.deptCheckStrictly"
              checkable
              @check="handleCheck"
            />
          </div>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { DeptTreeItem, ListItem } from '@/apis/system/role'
import { Message } from '@arco-design/web-vue'
import { getRole, getRoleDeptTree, updateRoleDataScope } from '@/apis/system/role'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'RoleDataScopeModal' })

const visible = ref(false)
const loading = ref(false)
const roleName = ref('')
const deptTree = ref<DeptTreeItem[]>([])
const checkedKeys = ref<string[]>([])
const halfCheckedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const [form, resetForm] = useResetReactive({ id: '', dataScope: '1', deptCheckStrictly: true })
const scopeOptions = [
  { value: '1', label: '全部数据权限' },
  { value: '2', label: '自定义数据权限' },
  { value: '3', label: '本部门数据权限' },
  { value: '4', label: '本部门及以下数据权限' },
  { value: '5', label: '仅本人数据权限' }
]

function collectIds(nodes: DeptTreeItem[], result: string[] = []) {
  nodes.forEach((node) => {
    result.push(node.id)
    collectIds(node.children, result)
  })
  return result
}

function handleCheck(keys: Array<string | number> | { checked: Array<string | number>, halfChecked: Array<string | number> }, info: { halfCheckedKeys?: Array<string | number> }) {
  if (Array.isArray(keys)) {
    checkedKeys.value = keys.map(String)
    halfCheckedKeys.value = (info.halfCheckedKeys || []).map(String)
  } else {
    checkedKeys.value = keys.checked.map(String)
    halfCheckedKeys.value = keys.halfChecked.map(String)
  }
}

async function open(item: ListItem) {
  resetForm()
  checkedKeys.value = []
  halfCheckedKeys.value = []
  expandedKeys.value = []
  visible.value = true
  loading.value = true
  try {
    const [roleRes, treeRes] = await Promise.all([getRole(item.id), getRoleDeptTree(item.id)])
    Object.assign(form, {
      id: roleRes.data.id,
      dataScope: roleRes.data.dataScope,
      deptCheckStrictly: roleRes.data.deptCheckStrictly
    })
    roleName.value = roleRes.data.name
    deptTree.value = treeRes.data.depts
    checkedKeys.value = treeRes.data.checkedKeys
  } finally {
    loading.value = false
  }
}

function close() {
  resetForm()
  deptTree.value = []
  checkedKeys.value = []
  halfCheckedKeys.value = []
}

async function save() {
  const deptIds = form.dataScope === '2' ? [...new Set([...checkedKeys.value, ...halfCheckedKeys.value])] : []
  await updateRoleDataScope({ ...form, deptIds })
  Message.success('数据权限更新成功')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.role-data-scope {
  &__spin {
    width: 100%;
  }

  &__tree-panel {
    width: 100%;
    max-height: 360px;
    overflow: auto;
    border: 1px solid var(--color-border-2);
  }

  &__tools {
    position: sticky;
    top: 0;
    z-index: 1;
    box-sizing: border-box;
    width: 100%;
    padding: 8px;
    background: var(--color-bg-2);
  }
}
</style>
