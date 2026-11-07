<template>
  <a-modal v-model:visible="visible" title="重置密码" width="500px" :mask-closable="false" @before-ok="save" @close="close">
    <a-form ref="formRef" :model="form" auto-label-width>
      <a-form-item label="用户名">
        <a-input :model-value="form.username" disabled />
      </a-form-item>
      <a-form-item field="password" label="新密码" required :rules="passwordRules">
        <a-input-password v-model="form.password" :max-length="30" allow-clear />
      </a-form-item>
      <a-form-item field="confirmPassword" label="确认密码" required :rules="confirmRules">
        <a-input-password v-model="form.confirmPassword" :max-length="30" allow-clear />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FieldRule, FormInstance } from '@arco-design/web-vue'
import type { ListItem } from '@/apis/system/user'
import { Message } from '@arco-design/web-vue'
import { resetUserPassword } from '@/apis/system/user'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'ResetPasswordModal' })

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const [form, resetForm] = useResetReactive({ id: '', username: '', password: '', confirmPassword: '' })
const passwordPattern = /^(?=.*[a-z])(?=.*\d)(?=.*[^a-z\d]).{8,30}$/i
const passwordRules: FieldRule[] = [{ match: passwordPattern, message: '密码必须包含字母、数字和特殊字符，长度 8-30 位' }]
const confirmRules: FieldRule[] = [{
  validator: (value, callback) => {
    if (value !== form.password) callback('两次输入的密码不一致')
    else callback()
  }
}]

function open(user: ListItem) {
  resetForm()
  Object.assign(form, { id: user.id, username: user.username })
  visible.value = true
}

function close() {
  formRef.value?.resetFields()
  resetForm()
}

async function save() {
  if (await formRef.value?.validate()) return false
  await resetUserPassword(form.id, form.password)
  Message.success('密码重置成功')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped></style>
