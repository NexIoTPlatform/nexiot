<template>
  <a-modal
    v-model:visible="visible"
    :title="isEdit ? '编辑应用' : '新增应用'"
    width="min(640px, calc(100vw - 32px))"
    :mask-closable="false"
    :body-style="modalBodyStyle"
    align-center
    unmount-on-close
  >
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item
        label="应用名称"
        field="appName"
        :rules="[{ required: true, message: '请输入应用名称' }]"
        >
<a-input v-model.trim="form.appName" :max-length="64"
      />
</a-form-item>
      <a-form-item label="应用状态"
        >
<a-switch v-model="enabled" checked-text="启用" unchecked-text="停用"
      />
</a-form-item>
      <a-form-item label="应用描述"
        >
<a-textarea
          v-model="form.remark"
          :max-length="300"
          show-word-limit
          :auto-size="{ minRows: 4, maxRows: 6 }"
      />
</a-form-item>
    </a-form>
    <template #footer
      >
<a-space
        >
<a-button @click="visible = false">取消</a-button
        ><a-button type="primary" :loading="saving" @click="save"
          >
<template #icon><icon-save /></template>保存
</a-button
        >
</a-space
      >
</template
    >
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { ApplicationItem } from '@/apis/application/application'
import { Message } from '@arco-design/web-vue'
import {
  addApplication,
  getApplication,
  updateApplication
} from '@/apis/application/application'

defineOptions({ name: 'ApplicationFormDrawer' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const saving = ref(false)
const modalBodyStyle = { maxHeight: 'min(70vh, 620px)', overflowY: 'auto' as const }
const isEdit = ref(false)
const form = reactive<Partial<ApplicationItem>>({
  appUniqueId: '',
  appName: '',
  appStatus: 0,
  remark: ''
})
const enabled = computed({
  get: () => form.appStatus === 0,
  set: (value) => {
    form.appStatus = value ? 0 : 1
  }
})
function reset() {
  Object.assign(form, {
    appUniqueId: '',
    appName: '',
    appStatus: 0,
    remark: ''
  })
  isEdit.value = false
  formRef.value?.clearValidate()
}
function add() {
  reset()
  visible.value = true
}
async function edit(id: string) {
  reset()
  Object.assign(form, (await getApplication(id)).data)
  isEdit.value = true
  visible.value = true
}
async function save() {
  if (await formRef.value?.validate()) return
  saving.value = true
  try {
    await (isEdit.value ? updateApplication(form) : addApplication(form))
    Message.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('save-success')
  } finally {
    saving.value = false
  }
}
defineExpose({ add, edit })
</script>
