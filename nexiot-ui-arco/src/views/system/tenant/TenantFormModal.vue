<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    width="720px"
    :mask-closable="false"
    :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
    @before-ok="save"
    @close="close"
  >
    <a-spin :loading="loading" class="tenant-form__spin">
      <a-form ref="formRef" :model="form" auto-label-width>
        <a-grid :cols="2" :col-gap="16" :row-gap="4">
          <a-form-item field="tenantName" label="租户名称" required>
            <a-input v-model="form.tenantName" :max-length="50" />
          </a-form-item>
          <a-form-item field="tenantCode" label="租户编码" required>
            <a-input v-model="form.tenantCode" :disabled="isEdit" :max-length="30" />
          </a-form-item>
          <a-form-item field="packageId" label="租户套餐" required>
            <a-select v-model="form.packageId" :options="packageOptions" allow-search />
          </a-form-item>
          <a-form-item field="accountCount" label="账号上限" required>
            <a-input-number v-model="form.accountCount" :min="1" />
          </a-form-item>
          <a-form-item field="status" label="状态" required>
            <a-radio-group v-model="form.status" type="button">
              <a-radio :value="0">正常</a-radio>
              <a-radio :value="1">停用</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item field="expireTime" label="到期时间">
            <a-date-picker
              v-model="form.expireTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm:ss"
            />
          </a-form-item>
          <a-grid-item :span="2"><a-divider orientation="left">租户管理员</a-divider></a-grid-item>
          <a-form-item field="adminAlias" label="管理员昵称" required>
            <a-input v-model="form.adminAlias" :max-length="30" />
          </a-form-item>
          <a-form-item field="adminUsername" label="管理员账号" required>
            <a-input v-model="form.adminUsername" :disabled="isEdit" :max-length="30" />
          </a-form-item>
          <a-form-item field="adminMobile" label="管理员手机号" required>
            <a-input v-model="form.adminMobile" :max-length="20" />
          </a-form-item>
          <a-form-item
            field="adminPassword"
            label="管理员密码"
            :required="!isEdit"
            :rules="passwordRules"
          >
            <a-input-password v-model="form.adminPassword" :max-length="30" />
          </a-form-item>
          <a-grid-item :span="2"><a-divider orientation="left">登录配置</a-divider></a-grid-item>
          <a-form-item field="domain" label="登录域名" :rules="domainRules" :span="2">
            <a-input-tag
              v-model="form.domain"
              allow-clear
              placeholder="输入域名、URL 或 IP 后回车"
              @blur="normalizeDomains"
            />
          </a-form-item>
          <a-form-item field="remark" label="备注" :span="2">
            <a-textarea v-model="form.remark" :max-length="200" show-word-limit />
          </a-form-item>
        </a-grid>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { FieldRule, FormInstance, SelectOptionData } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import {
  addTenant,
  getTenant,
  getTenantPackageOptions,
  updateTenant
} from '@/apis/system/tenant'
import { useResetReactive } from '@/hooks'
import { normalizeTenantDomains, splitTenantDomains } from './utils'

defineOptions({ name: 'TenantFormModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const TENANT_ADMIN_PASSWORD_RE = /^(?=.*[a-z])(?=.*\d)(?=.*[^a-z\d]).{8,30}$/i

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const loading = ref(false)
const packageOptions = ref<SelectOptionData[]>([])

const [form, resetForm] = useResetReactive({
  tenantId: '',
  tenantName: '',
  tenantCode: '',
  packageId: '',
  accountCount: 10,
  status: 0 as 0 | 1,
  expireTime: '',
  cfg: '',
  domain: [] as string[],
  adminAlias: '',
  adminUsername: '',
  adminMobile: '',
  adminPassword: '',
  remark: ''
})

const isEdit = computed(() => Boolean(form.tenantId))
const title = computed(() => isEdit.value ? '编辑租户' : '新增租户')

const passwordRules = computed<FieldRule[]>(() => [
  {
    validator: (value, callback) => {
      if (!isEdit.value && !value) return callback('请输入管理员密码')
      if (value && !TENANT_ADMIN_PASSWORD_RE.test(value))
        return callback('密码必须包含字母、数字和特殊字符，长度 8-30 位')
      callback()
    }
  }
])

const domainRules: FieldRule[] = [{
  validator: (value, callback) => {
    const domains = splitTenantDomains(value as string[])
    if (domains.length !== normalizeTenantDomains(domains).length)
      return callback('请输入合法且不重复的域名、URL 或 IP')
    callback()
  }
}]

async function loadPackageOptions() {
  const res = await getTenantPackageOptions()
  packageOptions.value = res.data.map((item) => ({ label: item.packageName, value: item.packageId }))
}

function normalizeDomains() {
  form.domain = normalizeTenantDomains(form.domain)
}

async function add() {
  resetForm()
  loading.value = true
  visible.value = true
  try {
    await loadPackageOptions()
  } finally {
    loading.value = false
  }
}

async function edit(tenantId: string) {
  resetForm()
  loading.value = true
  visible.value = true
  try {
    const [, detail] = await Promise.all([loadPackageOptions(), getTenant(tenantId)])
    const { loginTemplate: _loginTemplate, ...tenant } = detail.data
    Object.assign(form, tenant, {
      domain: splitTenantDomains(detail.data.domain),
      adminPassword: ''
    })
  } finally {
    loading.value = false
  }
}

function close() {
  formRef.value?.resetFields()
  resetForm()
}

async function save() {
  const errors = await formRef.value?.validate()
  if (errors) return false
  normalizeDomains()
  const payload: Record<string, unknown> = {
    ...form,
    domain: form.domain.join(',')
  }
  if (!payload.adminPassword) delete payload.adminPassword
  await (isEdit.value ? updateTenant(payload) : addTenant(payload))
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.tenant-form__spin {
  width: 100%;
}
</style>
