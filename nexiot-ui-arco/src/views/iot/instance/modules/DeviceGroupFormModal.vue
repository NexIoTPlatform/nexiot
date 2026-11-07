<template>
  <a-modal v-model:visible="visible" :title="title" width="min(520px, calc(100% - 20px))" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item v-if="mode !== 'root'" field="parentId" label="父级分组" :rules="[{ required: true, message: '请选择父级分组' }]">
        <a-tree-select v-model="form.parentId" :data="treeData" :field-names="{ key: 'id', title: 'groupName', children: 'children' }" :disabled="mode === 'edit'" allow-search placeholder="请选择父级分组" />
      </a-form-item>
      <a-form-item field="groupName" label="分组名称" :rules="[{ required: true, message: '请输入分组名称' }, { maxLength: 50, message: '不能超过 50 个字符' }]">
        <a-input v-model.trim="form.groupName" :max-length="50" show-word-limit placeholder="请输入分组名称" />
      </a-form-item>
      <a-form-item label="分组描述"><a-textarea v-model="form.groupDescribe" :max-length="200" show-word-limit :auto-size="{ minRows: 3, maxRows: 6 }" placeholder="描述该分组的业务用途" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { DeviceGroupItem } from '@/apis/iot/device'
import { Message } from '@arco-design/web-vue'
import { addDeviceGroup, getDeviceGroup, updateDeviceGroup } from '@/apis/iot/device'

defineOptions({ name: 'IotDeviceGroupFormModal' })

defineProps<{ treeData: DeviceGroupItem[] }>()
const emit = defineEmits<{
  (e: 'save-success', id?: string): void
}>()

type Mode = 'root' | 'child' | 'edit'
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const mode = ref<Mode>('root')
const form = reactive({ id: '', parentId: '0', groupName: '', groupDescribe: '' })
const title = computed(() => mode.value === 'root' ? '新增根分组' : mode.value === 'child' ? '新增子分组' : '编辑分组')

function reset() {
  Object.assign(form, { id: '', parentId: '0', groupName: '', groupDescribe: '' })
  formRef.value?.resetFields()
}

function addRoot() {
  reset()
  mode.value = 'root'
  visible.value = true
}

function addChild(parentId: string) {
  reset()
  mode.value = 'child'
  form.parentId = parentId
  visible.value = true
}

async function edit(id: string) {
  reset()
  mode.value = 'edit'
  Object.assign(form, (await getDeviceGroup(id)).data, { id: String(id) })
  visible.value = true
}

async function save() {
  if (await formRef.value?.validate()) return false
  await (mode.value === 'edit' ? updateDeviceGroup(form) : addDeviceGroup({ ...form, parentId: mode.value === 'root' ? '0' : form.parentId }))
  Message.success(mode.value === 'edit' ? '分组保存成功' : '分组新增成功')
  emit('save-success', form.id || undefined)
  return true
}

defineExpose({ addRoot, addChild, edit })
</script>

<style lang="scss" scoped></style>
