<template>
  <section class="profile-settings">
    <div class="profile-settings__heading">
      <div>
        <h2>基本资料</h2>
        <p>用于平台内显示、通知触达和账户识别。</p>
      </div>
      <a-space wrap>
        <a-button @click="emit('avatar')"><template #icon><icon-camera /></template>更换头像</a-button>
        <a-button :loading="loading" @click="reset"><template #icon><icon-refresh /></template>恢复</a-button>
      </a-space>
    </div>

    <a-descriptions :column="descriptionColumns" bordered size="large" class="profile-settings__identity">
      <a-descriptions-item label="登录账号">{{ profile.user.username }}</a-descriptions-item>
      <a-descriptions-item label="用户标识">{{ profile.user.unionId }}</a-descriptions-item>
      <a-descriptions-item label="租户标识">{{ profile.user.tenantId || '平台账户' }}</a-descriptions-item>
      <a-descriptions-item label="最近登录">{{ formatAccountDate(profile.user.loginDate) }}</a-descriptions-item>
    </a-descriptions>

    <a-form ref="formRef" :model="form" layout="vertical" class="profile-settings__form">
      <a-form-item field="alias" label="昵称" required :rules="[{ required: true, message: '请输入昵称' }]">
        <a-input v-model.trim="form.alias" :max-length="30" show-word-limit allow-clear placeholder="请输入昵称" />
      </a-form-item>
      <a-form-item field="mobile" label="手机号码" required :rules="mobileRules">
        <a-input v-model.trim="form.mobile" :max-length="11" allow-clear placeholder="请输入手机号码" />
        <template #extra>脱敏号码保持不变即可；需要换绑时请输入完整的新号码。</template>
      </a-form-item>
      <a-form-item field="email" label="电子邮箱" required :rules="emailRules">
        <a-input v-model.trim="form.email" :max-length="80" allow-clear placeholder="请输入电子邮箱" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea :model-value="profile.user.remark || ''" :max-length="200" disabled auto-size />
        <template #extra>账户备注由管理员维护。</template>
      </a-form-item>
    </a-form>

    <a-space>
      <a-button type="primary" :loading="loading" :disabled="!dirty" @click="save">
        <template #icon><icon-save /></template>
        保存资料
      </a-button>
      <a-button :disabled="!dirty || loading" @click="reset">取消修改</a-button>
    </a-space>
  </section>
</template>

<script setup lang="ts">
import type { FieldRule, FormInstance } from '@arco-design/web-vue'
import type { AccountProfile, ProfilePayload } from '../api'
import { Message } from '@arco-design/web-vue'
import { useBreakpoint } from '@/hooks'
import { formatAccountDate, updateAccountProfile } from '../api'

defineOptions({ name: 'AccountProfileSettings' })
const props = defineProps<{ profile: AccountProfile }>()
const emit = defineEmits<{ success: [], avatar: [] }>()

const MOBILE_RE = /^1[3-9]\d{9}$/
const { breakpoint } = useBreakpoint()
const descriptionColumns = computed(() => ['xs', 'sm'].includes(breakpoint.value) ? 1 : 2)
const formRef = ref<FormInstance | null>(null)
const loading = ref(false)
const form = reactive<ProfilePayload>({ alias: '', mobile: '', email: '', cfg: '' })
const snapshot = ref('')
const dirty = computed(() => JSON.stringify(form) !== snapshot.value)
const emailRules: FieldRule[] = [
  { required: true, message: '请输入电子邮箱' },
  { type: 'email', message: '请输入正确的电子邮箱' }
]
const mobileRules: FieldRule[] = [{
  validator: (value, callback) => {
    if (!value) callback('请输入手机号码')
    else if (MOBILE_RE.test(value)) callback()
    else if (value === props.profile.user.mobile && value.includes('*')) callback()
    else callback('请输入正确的 11 位手机号码')
  }
}]

function reset() {
  Object.assign(form, {
    alias: props.profile.user.alias || props.profile.user.username,
    mobile: props.profile.user.mobile || '',
    email: props.profile.user.email || '',
    cfg: props.profile.user.cfg || ''
  })
  snapshot.value = JSON.stringify(form)
  nextTick(() => formRef.value?.clearValidate())
}

async function save() {
  if (await formRef.value?.validate()) return
  loading.value = true
  try {
    await updateAccountProfile({ ...form })
    Message.success('个人资料已保存')
    snapshot.value = JSON.stringify(form)
    emit('success')
  } finally {
    loading.value = false
  }
}

watch(() => props.profile, reset, { immediate: true, deep: true })
</script>

<style lang="scss" scoped>
.profile-settings {
  max-width: 880px;

  &__heading {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 20px;

    h2,
    p {
      margin: 0;
    }

    h2 {
      margin-bottom: 6px;
      font-size: 18px;
    }

    p {
      color: var(--color-text-3);
    }
  }

  &__identity {
    margin-bottom: 24px;
  }

  &__form {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 24px;
  }
}

@media (width <= 640px) {
  .profile-settings__form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
