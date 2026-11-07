<template>
  <a-modal v-model:visible="visible" :title="title" width="560px" :mask-closable="false" @before-ok="save" @close="close">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item label="上级分类" field="parentId" :rules="[{ required: true, message: '请选择上级分类' }]">
        <a-tree-select
          v-model="form.parentId"
          :data="treeOptions"
          :field-names="{ key: 'id', title: 'classifiedName', children: 'children' }"
          placeholder="请选择上级分类"
          allow-clear
          allow-search
          :disabled="isEdit"
        />
      </a-form-item>
      <a-form-item label="分类名称" field="classifiedName" :rules="[{ required: true, message: '请输入分类名称' }]">
        <a-input v-model="form.classifiedName" placeholder="请输入分类名称" :max-length="50" show-word-limit />
      </a-form-item>
      <a-form-item
        label="英文标识"
        field="identification"
        :rules="[
          { required: true, message: '请输入英文标识' },
          { match: /^[A-Za-z][A-Za-z0-9_]*$/, message: '须以字母开头，只能包含字母、数字和下划线' },
        ]"
      >
        <a-input v-model="form.identification" placeholder="例如 meter_gateway" :max-length="64" />
      </a-form-item>
      <a-form-item label="说明" field="description">
        <a-textarea v-model="form.description" placeholder="请输入分类说明" :max-length="200" show-word-limit :auto-size="{ minRows: 3, maxRows: 5 }" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { ProductCategoryItem } from '@/apis/iot/productCategory'
import { Message } from '@arco-design/web-vue'
import {
  addProductCategory,
  getProductCategory,
  getProductCategoryList,
  updateProductCategory
} from '@/apis/iot/productCategory'

defineOptions({ name: 'ProductCategoryFormModal' })

const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const treeOptions = ref<ProductCategoryItem[]>([])
const form = reactive({ id: '', parentId: '0', classifiedName: '', identification: '', description: '' })
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑产品分类' : '新增产品分类')

function buildTree(list: ProductCategoryItem[], parentId = '0'): ProductCategoryItem[] {
  return list
    .filter((item) => String(item.parentId || '0') === parentId)
    .map((item) => ({ ...item, children: buildTree(list, String(item.id)) }))
}

async function loadTree() {
  const res = await getProductCategoryList()
  const children = buildTree(res.data)
  treeOptions.value = [{ id: '0', parentId: '', identification: 'root', classifiedName: '主目录', children }]
}

function reset() {
  Object.assign(form, { id: '', parentId: '0', classifiedName: '', identification: '', description: '' })
  formRef.value?.clearValidate()
}

async function add(parent?: ProductCategoryItem) {
  reset()
  await loadTree()
  if (parent) {
    if (String(parent.id).length >= 9) {
      Message.warning('产品分类最多为三级')
      return
    }
    form.parentId = String(parent.id)
  }
  visible.value = true
}

async function edit(id: string) {
  reset()
  await loadTree()
  Object.assign(form, (await getProductCategory(id)).data)
  form.id = String(form.id)
  form.parentId = String(form.parentId || '0')
  visible.value = true
}

async function save() {
  if (await formRef.value?.validate()) return false
  await (isEdit.value ? updateProductCategory(form) : addProductCategory(form))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

function close() {
  reset()
}

defineExpose({ add, edit })
</script>
