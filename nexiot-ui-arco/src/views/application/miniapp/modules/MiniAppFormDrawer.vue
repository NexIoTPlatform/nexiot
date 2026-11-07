<template>
  <a-modal
    v-model:visible="visible"
    :title="isEdit ? '编辑微信小程序' : '新增微信小程序'"
    width="min(900px, calc(100vw - 32px))"
    align-center
    :mask-closable="false"
    :body-style="modalBodyStyle"
    unmount-on-close
  >
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-divider orientation="left">基础信息</a-divider>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
        >
<a-form-item
          label="小程序名称"
          field="name"
          :rules="[{ required: true, message: '请输入名称' }]"
          >
<a-input v-model.trim="form.name" />
</a-form-item
        ><a-form-item
          label="小程序 APPID"
          field="appId"
          :rules="[{ required: true, message: '请输入 APPID' }]"
          >
<a-input v-model.trim="form.appId" :disabled="isEdit" />
</a-form-item
        ><a-form-item
          label="所属应用"
          field="linkedAppId"
          :rules="[{ required: true, message: '请选择关联应用' }]"
          >
<a-select
            v-model="form.linkedAppId"
            :options="applicationOptions"
            :loading="applicationLoading"
            allow-search
            :filter-option="false"
            placeholder="请选择或搜索 IoT 应用"
            @search="loadApplications"
            @dropdown-visible-change="(open) => open && loadApplications()" />
</a-form-item
        ><a-form-item label="主题色"
          >
<a-color-picker v-model="form.primaryColor" />
</a-form-item
        ><a-form-item label="客服电话"
          >
<a-input v-model.trim="form.contactPhone" />
</a-form-item
        ><a-form-item label="联系邮箱"
          >
<a-input v-model.trim="form.contactEmail" />
</a-form-item
      >
</a-grid>
      <a-form-item label="小程序密钥">
        <a-input-password
          v-model="form.appSecret"
          placeholder="新增时填写，编辑留空表示不修改"
        />
      </a-form-item>
      <a-divider orientation="left">展示与访问</a-divider>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item label="Logo 地址">
          <a-input v-model.trim="form.logo" />
        </a-form-item>
        <a-form-item label="二维码地址">
          <a-input v-model.trim="form.qrcodeImage" />
        </a-form-item>
        <a-form-item label="访问链接" :span="2">
          <a-input v-model.trim="form.accessUrl" />
        </a-form-item>
        <a-form-item label="应用描述">
          <a-textarea
            v-model="form.description"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item label="欢迎语">
          <a-textarea
            v-model="form.welcomeText"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
      </a-grid>
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
import type { MiniAppItem } from '@/apis/application/miniapp'
import { Message } from '@arco-design/web-vue'
import { getApplicationList } from '@/apis/application/application'
import {
  createMiniApp,
  getMiniApp,
  updateMiniApp
} from '@/apis/application/miniapp'

defineOptions({ name: 'MiniAppFormDrawer' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const applicationLoading = ref(false)
const applicationOptions = ref<{ label: string, value: string }[]>([])
const modalBodyStyle = {
  maxHeight: 'min(72vh, 720px)',
  overflowY: 'auto' as const
}
const form = reactive<Partial<MiniAppItem>>({
  name: '',
  appId: '',
  appSecret: '',
  linkedAppId: '',
  logo: '',
  qrcodeImage: '',
  accessUrl: '',
  primaryColor: '#165DFF',
  contactPhone: '',
  contactEmail: '',
  description: '',
  welcomeText: ''
})
function reset() {
  Object.assign(form, {
    name: '',
    appId: '',
    appSecret: '',
    linkedAppId: '',
    logo: '',
    qrcodeImage: '',
    accessUrl: '',
    primaryColor: '#165DFF',
    contactPhone: '',
    contactEmail: '',
    description: '',
    welcomeText: ''
  })
  isEdit.value = false
  formRef.value?.clearValidate()
}
function add() {
  reset()
  loadApplications()
  visible.value = true
}
async function edit(id: string) {
  reset()
  Object.assign(form, (await getMiniApp(id)).data)
  form.appSecret = ''
  await loadApplications()
  if (form.linkedAppId && !applicationOptions.value.some((item) => item.value === form.linkedAppId)) {
    applicationOptions.value.unshift({ label: form.appName || form.linkedAppId, value: form.linkedAppId })
  }
  isEdit.value = true
  visible.value = true
}
async function loadApplications(keyword = '') {
  applicationLoading.value = true
  try {
    const res = await getApplicationList({ page: 1, size: 50, appName: keyword || undefined })
    applicationOptions.value = (res.data.records || []).map((item) => ({ label: `${item.appName} (${item.appId})`, value: item.appUniqueId }))
  } finally {
    applicationLoading.value = false
  }
}
async function save() {
  if (await formRef.value?.validate()) return
  saving.value = true
  try {
    await (isEdit.value ? updateMiniApp(form) : createMiniApp(form))
    Message.success(isEdit.value ? '修改成功' : '创建成功')
    visible.value = false
    emit('save-success')
  } finally {
    saving.value = false
  }
}
defineExpose({ add, edit })
</script>
