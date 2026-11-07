<template>
  <a-modal v-model:visible="visible" :title="title" width="560px" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item label="上级分类">
        <a-tree-select
          v-model="form.parentId"
          :data="treeData"
          :field-names="{ key: 'id', title: 'categoryName', children: 'children' }"
          placeholder="留空表示一级分类"
          allow-clear
          allow-search
          :disabled="isEdit"
        />
      </a-form-item>
      <a-grid :cols="2" :col-gap="16">
        <a-form-item label="分类名称" field="categoryName" :rules="[{ required: true, message: '请输入分类名称' }]">
          <a-input v-model="form.categoryName" :max-length="50" />
        </a-form-item>
        <a-form-item label="分类标识" field="categoryKey" :rules="categoryKeyRules">
          <a-input v-model="form.categoryKey" placeholder="例如 smart_city" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="排序" field="listOrder">
          <a-input-number v-model="form.listOrder" :min="0" :max="9999" />
        </a-form-item>
        <a-form-item label="状态" field="status">
          <a-radio-group v-model="form.status" type="button"><a-radio :value="1">启用</a-radio><a-radio :value="0">禁用</a-radio></a-radio-group>
        </a-form-item>
      </a-grid>
      <a-form-item label="描述"><a-textarea v-model="form.description" :max-length="200" show-word-limit :auto-size="{ minRows: 3, maxRows: 5 }" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FieldRule, FormInstance } from '@arco-design/web-vue'
import type { StandardCategoryTreeNode } from '@/apis/iot/standardModel'
import { Message } from '@arco-design/web-vue'
import { addStandardCategory, getStandardCategory, getStandardCategoryTree, toStandardCategoryTree, updateStandardCategory } from '@/apis/iot/standardModel'

defineOptions({ name: 'StandardCategoryFormModal' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const treeData = ref<StandardCategoryTreeNode[]>([])
const form = reactive({ id: '', parentId: undefined as string | undefined, categoryName: '', categoryKey: '', listOrder: 0, status: 1, description: '' })
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑模型分类' : '新增模型分类')
const categoryKeyRules: FieldRule[] = [{ required: true, message: '请输入分类标识' }, { match: /^[A-Z]\w*$/i, message: '须以字母开头，只能包含字母、数字和下划线' }]

function reset() {
  Object.assign(form, { id: '', parentId: undefined, categoryName: '', categoryKey: '', listOrder: 0, status: 1, description: '' })
  formRef.value?.clearValidate()
}

async function loadTree() {
  treeData.value = toStandardCategoryTree((await getStandardCategoryTree()).data)
}

async function add(parent?: StandardCategoryTreeNode) {
  reset()
  if (parent?.level === 3) {
    Message.warning('标准模型分类最多为三级')
    return
  }
  await loadTree()
  form.parentId = parent?.id
  visible.value = true
}

async function edit(id: string) {
  reset()
  await loadTree()
  const data = (await getStandardCategory(id)).data
  Object.assign(form, data, { id: String(data.id), parentId: data.parentId && String(data.parentId) !== '0' ? String(data.parentId) : undefined })
  visible.value = true
}

async function save() {
  if (await formRef.value?.validate()) return false
  const payload = { ...form, parentId: form.parentId || '0' }
  await (isEdit.value ? updateStandardCategory(payload) : addStandardCategory(payload))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>
