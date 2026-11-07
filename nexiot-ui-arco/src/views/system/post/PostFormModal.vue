<template>
  <a-modal v-model:visible="visible" :title="title" width="520px" :mask-closable="false" @before-ok="save" @close="close">
    <GiForm ref="giFormRef" :model-value="form" :columns="columns" :grid-item-props="{ span: 24 }"
      @update:model-value="Object.assign(form, $event)" />
  </a-modal>
</template>

<script setup lang="ts">
import type { FormColumnItem } from '@/components/index'
import { Message } from '@arco-design/web-vue'
import { addPost, getPost, updatePost } from '@/apis/system/post'
import { GiForm } from '@/components/index'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'PostFormModal' })

const emit = defineEmits<{ (e: 'save-success'): void }>()
const giFormRef = useTemplateRef<InstanceType<typeof GiForm>>('giFormRef')
const visible = ref(false)
const [form, resetForm] = useResetReactive({ id: '', code: '', name: '', sort: 0, status: '1' as Status, remark: '', createTime: '' })
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑岗位' : '新增岗位')
const columns: FormColumnItem[] = [
  { type: 'input', label: '岗位名称', field: 'name', required: true, props: { maxLength: 50 } },
  { type: 'input', label: '岗位编码', field: 'code', required: true, props: { maxLength: 50 } },
  { type: 'input-number', label: '显示顺序', field: 'sort', required: true, props: { min: 0 } },
  { type: 'switch', label: '状态', field: 'status', props: { checkedValue: '1', uncheckedValue: '0', checkedText: '正常', uncheckedText: '停用' } },
  { type: 'textarea', label: '备注', field: 'remark', props: { maxLength: 200, showWordLimit: true } }
]

function add() {
  resetForm()
  visible.value = true
}

async function edit(id: string) {
  resetForm()
  visible.value = true
  Object.assign(form, (await getPost(id)).data)
}

function close() {
  giFormRef.value?.formRef?.resetFields()
  resetForm()
}

async function save() {
  if (await giFormRef.value?.formRef?.validate()) return false
  await (isEdit.value ? updatePost(form) : addPost(form))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped></style>
