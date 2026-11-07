<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 20px)" :modal-style="{ maxWidth: '520px' }"
    :mask-closable="false" @before-ok="save" @close="close">
    <GiForm ref="GiFormRef" :model-value="form" :columns="formColumns" :grid-item-props="{ span: 24 }"
      @update:model-value="Object.assign(form, $event)" />
  </a-modal>
</template>

<script setup lang="ts">
import type { FormColumnItem } from '@/components/index'
import { Message } from '@arco-design/web-vue'
import { addDept, getDept, updateDept } from '@/apis/system/dept'
import { GiForm } from '@/components/index'
import { useResetReactive } from '@/hooks'
import { useDept } from '@/hooks/app'

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const { deptList, getDeptList } = useDept()
getDeptList()

const GiFormRef = useTemplateRef<InstanceType<typeof GiForm>>('GiFormRef')
const detailId = ref('')
const visible = ref(false)
const isEdit = computed(() => !!detailId.value)
const title = computed(() => (isEdit.value ? '编辑部门' : '新增部门'))

const [form, resetForm] = useResetReactive({
  id: '',
  parentId: '',
  name: '',
  sort: 0,
  status: '1' as Status,
  leader: '',
  phone: '',
  email: ''
})

const formColumns = computed<FormColumnItem[]>(() => [
  {
    type: 'tree-select',
    label: '上级部门',
    field: 'parentId',
    props: {
      data: deptList.value,
      fieldNames: { key: 'id', title: 'name', children: 'children' },
      allowClear: true
    }
  },
  {
    type: 'input',
    label: '部门名称',
    field: 'name',
    required: true,
    rules: [
      { min: 3, max: 10, message: '长度在 3 - 10个字符' }
    ],
    props: { maxLength: 10 }
  },
  {
    type: 'input-number',
    label: '排序',
    field: 'sort',
    props: { style: { width: '120px' } }
  },
  { type: 'input', label: '负责人', field: 'leader', props: { maxLength: 30 } },
  { type: 'input', label: '联系电话', field: 'phone', rules: [{ match: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }] },
  { type: 'input', label: '邮箱', field: 'email', rules: [{ type: 'email', message: '邮箱格式不正确' }] },
  {
    type: 'switch',
    label: '状态',
    field: 'status',
    required: true,
    props: {
      type: 'round',
      checkedValue: '1',
      uncheckedValue: '0',
      checkedText: '正常',
      uncheckedText: '禁用'
    }
  }
])

const add = (parentId?: string) => {
  detailId.value = ''
  form.parentId = parentId || deptList.value[0]?.id || ''
  visible.value = true
}

const edit = async (id: string) => {
  if (!deptList.value.length) {
    await getDeptList()
  }
  detailId.value = id
  visible.value = true
  const res = await getDept(id)
  Object.assign(form, res.data)
}

const close = () => {
  GiFormRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await GiFormRef.value?.formRef?.validate()
    if (valid) return false
    const data = { ...form, id: detailId.value, createTime: '' }
    await (isEdit.value ? updateDept(data) : addDept(data))
    Message.success(isEdit.value ? '修改成功' : '新增成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

defineExpose({ add, edit })
</script>
