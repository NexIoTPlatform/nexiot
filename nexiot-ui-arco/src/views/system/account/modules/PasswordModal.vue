<template>
  <a-modal v-model:visible="visible" title="修改密码" width="min(500px, calc(100vw - 32px))" :mask-closable="false" align-center @before-ok="save" @close="close">
    <a-alert type="warning" class="password-modal__alert">修改后请使用新密码登录，其他已登录终端可能需要重新认证。</a-alert>
    <a-form ref="formRef" :model="form" auto-label-width>
      <a-form-item field="oldPassword" label="当前密码" required :rules="[{ required: true, message: '请输入当前密码' }]">
        <a-input-password v-model="form.oldPassword" autocomplete="current-password" :max-length="30" allow-clear placeholder="请输入当前密码" />
      </a-form-item>
      <a-form-item field="newPassword" label="新密码" required :rules="passwordRules">
        <a-input-password v-model="form.newPassword" autocomplete="new-password" :max-length="30" allow-clear placeholder="8-30 位强密码" />
      </a-form-item>
      <a-form-item field="confirmPassword" label="确认密码" required :rules="confirmRules">
        <a-input-password v-model="form.confirmPassword" autocomplete="new-password" :max-length="30" allow-clear placeholder="再次输入新密码" />
      </a-form-item>
    </a-form>
    <div class="password-modal__rules">
      <span :class="{ passed: checks.length }"><icon-check-circle-fill />8-30 个字符</span>
      <span :class="{ passed: checks.letter }"><icon-check-circle-fill />包含字母</span>
      <span :class="{ passed: checks.number }"><icon-check-circle-fill />包含数字</span>
      <span :class="{ passed: checks.special }"><icon-check-circle-fill />包含特殊字符</span>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import type { FieldRule, FormInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { updateAccountPassword } from '../api'

defineOptions({ name: 'AccountPasswordModal' })
const emit = defineEmits<{ success: [] }>()

const PASSWORD_PATTERN = /^(?=.*\d)(?=.*[a-z])(?=.*[^a-z\d]).{8,30}$/i
const LETTER_RE = /[a-z]/i
const NUMBER_RE = /\d/
const SPECIAL_RE = /[^a-z\d]/i
const formRef = ref<FormInstance | null>(null)
const visible = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules: FieldRule[] = [
  { required: true, message: '请输入新密码' },
  { match: PASSWORD_PATTERN, message: '密码必须包含字母、数字和特殊字符，长度 8-30 位' },
  {
    validator: (value, callback) => {
      if (value && value === form.oldPassword) callback('新密码不能与当前密码相同')
      else callback()
    }
  }
]
const confirmRules: FieldRule[] = [{
  validator: (value, callback) => {
    if (!value) callback('请再次输入新密码')
    else if (value !== form.newPassword) callback('两次输入的密码不一致')
    else callback()
  }
}]
const checks = computed(() => ({
  length: form.newPassword.length >= 8 && form.newPassword.length <= 30,
  letter: LETTER_RE.test(form.newPassword),
  number: NUMBER_RE.test(form.newPassword),
  special: SPECIAL_RE.test(form.newPassword)
}))

function open() {
  close()
  visible.value = true
}

function close() {
  Object.assign(form, { oldPassword: '', newPassword: '', confirmPassword: '' })
  formRef.value?.clearValidate()
}

async function save() {
  if (await formRef.value?.validate()) return false
  await updateAccountPassword(form.oldPassword, form.newPassword)
  Message.success('密码修改成功')
  emit('success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.password-modal {
  &__alert {
    margin-bottom: 20px;
  }

  &__rules {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px 16px;
    padding: 12px 16px;
    color: var(--color-text-3);
    background: var(--color-fill-1);

    span {
      display: flex;
      gap: 6px;
      align-items: center;
    }

    .passed {
      color: rgb(var(--success-6));
    }
  }
}
</style>
