<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 32px)" :modal-style="{ maxWidth: '1080px' }" :body-style="{ maxHeight: '72vh', overflowY: 'auto' }" unmount-on-close :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item label="所属分类" field="categoryId" :rules="[{ required: true, message: '请选择所属分类' }]">
        <a-tree-select v-model="form.categoryId" :data="categoryTree" :field-names="{ key: 'id', title: 'categoryName', children: 'children' }" allow-search placeholder="可选择任意一级分类" />
      </a-form-item>
      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <a-form-item label="产品名称" field="productName" :rules="[{ required: true, message: '请输入产品名称' }]"><a-input v-model="form.productName" /></a-form-item>
        <a-form-item label="产品标识" field="productKey" :rules="productKeyRules"><a-input v-model="form.productKey" :disabled="isEdit" placeholder="例如 smart_meter" /></a-form-item>
        <a-form-item label="状态" field="status"><a-radio-group v-model="form.status" type="button"><a-radio :value="1">启用</a-radio><a-radio :value="0">禁用</a-radio></a-radio-group></a-form-item>
        <a-form-item label="模板"><a-switch v-model="template" /></a-form-item>
      </a-grid>
      <a-form-item label="图片地址"><a-input v-model="form.photoUrl" placeholder="可选，填写可访问的图片 URL" /></a-form-item>
      <a-form-item label="描述"><a-textarea v-model="form.description" :max-length="300" show-word-limit :auto-size="{ minRows: 3, maxRows: 5 }" /></a-form-item>
    </a-form>
    <a-divider orientation="left">物模型</a-divider>
    <StandardMetadataEditor v-if="visible" ref="metadataEditorRef" :model-value="form.metadata" />
  </a-modal>
</template>

<script setup lang="ts">
import type { FieldRule, FormInstance } from '@arco-design/web-vue'
import type { StandardCategoryItem, StandardCategoryTreeNode } from '@/apis/iot/standardModel'
import { Message } from '@arco-design/web-vue'
import { addStandardProduct, getStandardCategoryTree, getStandardProduct, toStandardCategoryTree, updateStandardProduct } from '@/apis/iot/standardModel'

import StandardMetadataEditor from './StandardMetadataEditor.vue'

defineOptions({ name: 'StandardProductFormModal' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const metadataEditorRef = useTemplateRef<InstanceType<typeof StandardMetadataEditor>>('metadataEditorRef')
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const categoryTree = ref<StandardCategoryTreeNode[]>([])
const form = reactive({ id: '', categoryId: '', productName: '', productKey: '', photoUrl: '', status: 1, isTemplate: 0, description: '', metadata: '{\n  "properties": [],\n  "events": [],\n  "functions": []\n}' })
const template = computed({
  get: () => form.isTemplate === 1,
  set: (value) => {
    form.isTemplate = value ? 1 : 0
  }
})
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑标准产品' : '新增标准产品')
const productKeyRules: FieldRule[] = [{ required: true, message: '请输入产品标识' }, { match: /^[A-Z]\w*$/i, message: '须以字母开头，只能包含字母、数字和下划线' }]

function reset() {
  Object.assign(form, { id: '', categoryId: '', productName: '', productKey: '', photoUrl: '', status: 1, isTemplate: 0, description: '', metadata: '{\n  "properties": [],\n  "events": [],\n  "functions": []\n}' })
  formRef.value?.clearValidate()
}

async function loadTree() {
  categoryTree.value = toStandardCategoryTree((await getStandardCategoryTree()).data)
}
async function add(category?: StandardCategoryItem) {
  reset()
  await loadTree()
  form.categoryId = category?.id || ''
  visible.value = true
}
async function edit(id: string) {
  reset()
  await loadTree()
  const data = (await getStandardProduct(id)).data
  Object.assign(form, data, { id: String(data.id), categoryId: String(data.categoryId), metadata: data.metadata || form.metadata })
  visible.value = true
}
async function save() {
  if (await formRef.value?.validate()) return false
  const metadata = metadataEditorRef.value?.getMetadata()
  if (metadata === undefined) return false
  form.metadata = metadata
  await (isEdit.value ? updateStandardProduct(form) : addStandardProduct(form))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}
defineExpose({ add, edit })
</script>
