<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 20px)" :mask-closable="false"
    :modal-style="{ maxWidth: '600px' }" :body-style="{ maxHeight: '70vh' }" @before-ok="save" @close="close">
    <a-alert v-if="!isEdit && canManageUsers" :type="createMode === 'TENANT_ADMIN' ? 'info' : 'success'" class="user-form__notice">
      {{ createMode === 'TENANT_ADMIN'
        ? '将创建租户管理员，系统会自动创建租户、根部门并绑定套餐管理员角色。'
        : '将创建当前租户成员，必须选择当前租户的部门和角色。' }}
    </a-alert>
    <a-alert v-if="roleManagedBySystem" type="warning" class="user-form__notice">
      租户管理员角色由系统按租户套餐维护，不支持手工修改。
    </a-alert>
    <GiForm ref="GiFormRef" :model-value="form" :columns="formColumns"
      :grid-item-props="{ span: { xs: 24, sm: 24, md: 12, lg: 12, xl: 12, xxl: 12 } }"
      @update:model-value="Object.assign(form, $event)" />
  </a-modal>
</template>

<script setup lang="ts">
import type { FormColumnItem } from '@/components/index'
import { Message } from '@arco-design/web-vue'
import { getDeptOptions } from '@/apis/system/dept'
import { addUser, getUserContext, updateUser } from '@/apis/system/user'
import { GiForm } from '@/components/index'
import { useResetReactive } from '@/hooks'
import * as Regexp from '@/utils/regexp'

defineOptions({ name: 'UserFormModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const GiFormRef = useTemplateRef<InstanceType<typeof GiForm>>('GiFormRef')
const detailId = ref('')
const isEdit = computed(() => !!detailId.value)
const createMode = ref<'TENANT_ADMIN' | 'TENANT_MEMBER'>('TENANT_MEMBER')
const canManageUsers = ref(false)
const currentTenantId = ref('')
const deptList = ref<Awaited<ReturnType<typeof getDeptOptions>>['data']>([])
const roleOptions = ref<Array<{ label: string, value: string }>>([])
const title = computed(() => isEdit.value ? '编辑用户' : createMode.value === 'TENANT_ADMIN' ? '新增租户管理员' : '新增租户成员')
const visible = ref(false)

const [form, resetForm] = useResetReactive({
  id: '',
  unionId: '',
  username: '', // 用户名
  password: '', // 密码，仅新增时必填
  nickname: '', // 昵称
  phone: '', // 手机号
  email: '', // 邮箱
  deptId: '', // 部门
  tenantId: '',
  tenantRole: '',
  roleIds: [] as string[], // 角色(可能多个)
  description: '', // 描述
  status: '1' as Status, // 状态 与字典 STATUS 一致 '0'禁用 '1'启用
  type: 2 as 1 | 2, // 类型 1系统内置(admin是系统内置) 2自定义
  disabled: false // 如果 type===1 这为 true, 主要作用是列表复选框禁用状态
})

const roleManagedBySystem = computed(() => canManageUsers.value && (isEdit.value ? form.tenantRole === 'ADMIN' : createMode.value === 'TENANT_ADMIN'))
const showDeptSelector = computed(() => canManageUsers.value && (isEdit.value ? form.tenantRole !== 'ADMIN' : createMode.value !== 'TENANT_ADMIN'))
const showRoleSelector = computed(() => canManageUsers.value && !roleManagedBySystem.value)

const formColumns = computed<FormColumnItem[]>(() => [
  {
    type: 'input',
    label: '用户名',
    field: 'username',
    required: true,
    rules: [
      { min: 2, max: 30, message: '长度在 2 - 30 个字符' }
    ],
    props: { maxLength: 30, disabled: isEdit.value }
  },
  {
    type: 'input',
    label: '密码',
    field: 'password',
    required: !isEdit.value,
    hide: isEdit.value,
    rules: [{ match: /^(?=.*[a-z])(?=.*\d)(?=.*[^a-z\d]).{8,30}$/i, message: '密码必须包含字母、数字和特殊字符，长度 8-30 位' }],
    props: { type: 'password', maxLength: 30 }
  },
  {
    type: 'input',
    label: '昵称',
    field: 'nickname',
    required: true,
    rules: [
      { min: 2, max: 30, message: '长度在 2 - 30 个字符' }
    ],
    props: { maxLength: 30 }
  },
  {
    type: 'input',
    label: '手机号码',
    field: 'phone',
    required: true,
    rules: [{ match: Regexp.Phone, message: '手机号格式不正确' }],
    props: { maxLength: 11 }
  },
  {
    type: 'input',
    label: '邮箱',
    field: 'email',
    rules: [{ match: Regexp.Email, message: '邮箱格式不正确' }],
    props: { maxLength: 30 }
  },
  {
    type: 'tree-select',
    label: '所属部门',
    field: 'deptId',
    required: true,
    hide: !showDeptSelector.value,
    props: {
      data: deptList.value,
      fieldNames: { key: 'id', title: 'name' },
      allowSearch: true,
      disabled: form.disabled
    },
    span: 24
  },
  {
    type: 'select',
    label: '角色',
    field: 'roleIds',
    required: true,
    hide: !showRoleSelector.value,
    props: {
      options: roleOptions.value,
      multiple: true,
      allowSearch: { retainInputValue: true },
      disabled: form.disabled
    },
    span: 24
  },
  {
    type: 'textarea',
    label: '描述',
    field: 'description',
    props: {
      maxLength: 200,
      autoSize: { minRows: 3 },
      showWordLimit: true
    },
    span: 24
  },
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

async function loadContext(id?: string) {
  const res = await getUserContext(id)
  createMode.value = res.data.createMode
  canManageUsers.value = res.data.canManageUsers
  currentTenantId.value = res.data.currentTenantId
  roleOptions.value = res.data.roles.map((role) => ({ label: role.name, value: role.id }))
  if (res.data.user) Object.assign(form, res.data.user)
  if (showDeptSelector.value) {
    const tenantId = form.tenantId || currentTenantId.value
    deptList.value = (await getDeptOptions(tenantId)).data
    if (!form.deptId && deptList.value.length) form.deptId = deptList.value[0].id
  } else {
    deptList.value = []
    form.deptId = ''
    form.roleIds = []
  }
}

const add = async () => {
  resetForm()
  detailId.value = ''
  visible.value = true
  await loadContext()
}

const edit = async (id: string) => {
  resetForm()
  visible.value = true
  detailId.value = id
  await loadContext(id)
}

const close = () => {
  GiFormRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await GiFormRef.value?.formRef?.validate()
    if (valid) return false
    await (isEdit.value ? updateUser(form) : addUser(form))
    Message.success(isEdit.value ? '修改成功' : '新增成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.user-form {
  &__notice {
    margin-bottom: 16px;
  }
}
</style>
