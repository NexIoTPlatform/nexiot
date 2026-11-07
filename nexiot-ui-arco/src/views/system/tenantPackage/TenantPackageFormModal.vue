<template>
  <a-modal v-model:visible="visible" :title="title" width="680px" :mask-closable="false" @before-ok="save" @close="close">
    <a-spin :loading="loading" class="tenant-package-form__spin">
      <a-form ref="formRef" :model="form" auto-label-width>
        <a-form-item field="name" label="套餐名称" required>
          <a-input v-model="form.name" :max-length="50" />
        </a-form-item>
        <a-form-item field="status" label="状态" required>
          <a-radio-group v-model="form.status" type="button">
            <a-radio :value="0">正常</a-radio>
            <a-radio :value="1">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="menuIds" label="菜单权限" required>
          <div class="tenant-package-form__tree-panel">
            <a-space class="tenant-package-form__tools">
              <a-button size="mini" @click="expandAll">展开</a-button>
              <a-button size="mini" @click="expandedKeys = []">折叠</a-button>
              <a-button size="mini" @click="checkAll">全选</a-button>
              <a-button size="mini" @click="clearAll">清空</a-button>
            </a-space>
            <a-tree
              v-model:checked-keys="checkedKeys"
              v-model:expanded-keys="expandedKeys"
              :data="menuTree"
              :field-names="{ key: 'id', title: 'title', children: 'children' }"
              checkable
              @check="handleCheck"
            />
          </div>
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="form.remark" :max-length="200" show-word-limit />
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { MenuOptionsItem } from '@/apis/system/menu'
import { Message } from '@arco-design/web-vue'
import {
  addTenantPackage,
  getTenantPackage,
  getTenantPackageMenuTree,
  updateTenantPackage
} from '@/apis/system/tenantPackage'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'TenantPackageFormModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const loading = ref(false)
const checkedKeys = ref<string[]>([])
const halfCheckedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const menuTree = ref<MenuOptionsItem[]>([])

const [form, resetForm] = useResetReactive({ id: '', name: '', status: 0 as 0 | 1, remark: '', menuIds: [] as string[] })
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑租户套餐' : '新增租户套餐')

function collectIds(nodes: MenuOptionsItem[], bucket: string[] = []) {
  for (const node of nodes) {
    bucket.push(node.id)
    collectIds(node.children || [], bucket)
  }
  return bucket
}

function handleCheck(keys: Array<string | number>, info: { halfCheckedKeys?: Array<string | number> }) {
  checkedKeys.value = keys.map(String)
  halfCheckedKeys.value = (info.halfCheckedKeys || []).map(String)
}

function expandAll() {
  expandedKeys.value = collectIds(menuTree.value)
}

function checkAll() {
  checkedKeys.value = collectIds(menuTree.value)
  halfCheckedKeys.value = []
}

function clearAll() {
  checkedKeys.value = []
  halfCheckedKeys.value = []
}

async function loadMenuTree() {
  const res = await getTenantPackageMenuTree()
  menuTree.value = res.data
}

async function add() {
  resetForm()
  clearAll()
  visible.value = true
  loading.value = true
  try {
    await loadMenuTree()
  } finally {
    loading.value = false
  }
}

async function edit(id: string) {
  resetForm()
  clearAll()
  visible.value = true
  loading.value = true
  try {
    const [, detail] = await Promise.all([loadMenuTree(), getTenantPackage(id)])
    Object.assign(form, detail.data)
    checkedKeys.value = detail.data.menuIds
  } finally {
    loading.value = false
  }
}

function close() {
  formRef.value?.resetFields()
  resetForm()
  clearAll()
}

async function save() {
  form.menuIds = [...new Set([...checkedKeys.value, ...halfCheckedKeys.value])]
  if (!form.menuIds.length) {
    Message.warning('请至少选择一个菜单')
    return false
  }
  const errors = await formRef.value?.validate()
  if (errors) return false
  await (isEdit.value ? updateTenantPackage(form) : addTenantPackage(form))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.tenant-package-form {
  &__spin {
    width: 100%;
  }

  &__tree-panel {
    width: 100%;
    max-height: 380px;
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
