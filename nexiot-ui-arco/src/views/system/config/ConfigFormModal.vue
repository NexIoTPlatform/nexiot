<template>
  <a-modal v-model:visible="visible" :title="title" width="560px" :mask-closable="false" @before-ok="save" @close="close">
    <GiForm
      ref="giFormRef"
      :model-value="form"
      :columns="columns"
      :grid-item-props="{ span: 24 }"
      @update:model-value="Object.assign(form, $event)"
    />
  </a-modal>
</template>

<script setup lang="ts">
import type { FormColumnItem } from '@/components/index'
import { Message } from '@arco-design/web-vue'
import { addConfig, getConfig, updateConfig } from '@/apis/system/config'
import { GiForm } from '@/components/index'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'ConfigFormModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const giFormRef = useTemplateRef<InstanceType<typeof GiForm>>('giFormRef')
const visible = ref(false)
const [form, resetForm] = useResetReactive({ id: '', name: '', key: '', value: '', system: false, remark: '', createTime: '' })
const isEdit = computed(() => Boolean(form.id))
const title = computed(() => isEdit.value ? '编辑参数' : '新增参数')
const columns: FormColumnItem[] = [
  { type: 'input', label: '参数名称', field: 'name', required: true, props: { maxLength: 100 } },
  { type: 'input', label: '参数键名', field: 'key', required: true, props: { maxLength: 100 } },
  { type: 'textarea', label: '参数键值', field: 'value', required: true, props: { maxLength: 500, showWordLimit: true, autoSize: { minRows: 3 } } },
  { type: 'radio-group', label: '系统内置', field: 'system', required: true, props: { type: 'button', options: [{ label: '是', value: true }, { label: '否', value: false }] } },
  { type: 'textarea', label: '备注', field: 'remark', props: { maxLength: 200, showWordLimit: true } }
]

function add() {
  resetForm()
  visible.value = true
}

async function edit(id: string) {
  resetForm()
  visible.value = true
  Object.assign(form, (await getConfig(id)).data)
}

function close() {
  giFormRef.value?.formRef?.resetFields()
  resetForm()
}

async function save() {
  if (await giFormRef.value?.formRef?.validate()) return false
  await (isEdit.value ? updateConfig(form) : addConfig(form))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped></style>
