<template>
  <a-modal
    v-model:visible="visible"
    :title="isEdit ? '编辑证书' : '新增证书'"
    width="min(900px, calc(100vw - 32px))"
    align-center
    :mask-closable="false"
    :body-style="modalBodyStyle"
    unmount-on-close
  >
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item label="证书名称" field="name" :rules="[{ required: true, message: '请输入证书名称' }]">
          <a-input v-model.trim="form.name" />
        </a-form-item>
        <a-form-item label="证书标识" field="sslKey" :rules="[{ required: true, message: '请输入证书标识' }]">
          <a-input v-model.trim="form.sslKey" :disabled="isEdit" placeholder="用于 TLS 配置引用" />
        </a-form-item>
        <a-form-item label="证书类型" field="certificateType" :rules="[{ required: true, message: '请选择证书类型' }]">
          <a-select v-model="form.certificateType" :options="certificateTypeOptions" :disabled="isEdit" />
        </a-form-item>
        <a-form-item v-if="isOpcuaBundle" label="服务端信任模式" field="trustMode" :rules="[{ required: true, message: '请选择服务端信任模式' }]">
          <a-select v-model="form.trustMode" :options="trustModeOptions" />
        </a-form-item>
      </a-grid>

      <a-form-item v-if="!isEdit" label="录入方式">
        <a-radio-group v-model="inputMode" type="button">
          <a-radio value="file"><icon-upload /> 上传 PEM/KEY</a-radio>
          <a-radio value="content"><icon-code /> 粘贴内容</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-alert v-else type="info" class="certificate-form__edit-notice">证书材料不会回显；留空表示保持现有材料不变。</a-alert>
      <a-form-item v-if="inputMode === 'file' && !isEdit && !isOpcuaBundle" label="证书与私钥文件" required>
        <a-upload
          :auto-upload="false"
          :file-list="fileList"
          :limit="2"
          multiple
          accept=".pem,.key"
          draggable
          @change="handleFiles"
        >
          <template #upload-button><div class="certificate-form__drop"><icon-upload /><strong>选择或拖入证书和私钥</strong><span>请同时提供 PEM/CRT 证书与 KEY 私钥文件</span></div></template>
        </a-upload>
      </a-form-item>

      <a-grid v-if="inputMode === 'file' && !isEdit && isOpcuaBundle" :cols="{ xs: 1, sm: 3 }" :col-gap="12">
        <a-form-item label="客户端证书" required><a-upload :auto-upload="false" :file-list="clientCertificateFiles" :limit="1" accept=".pem,.crt,.cer" @change="files => handleOpcuaFile(files, 'certContent')"><template #upload-button><a-button long><template #icon><icon-upload /></template>选择证书</a-button></template></a-upload></a-form-item>
        <a-form-item label="客户端私钥" required><a-upload :auto-upload="false" :file-list="clientPrivateKeyFiles" :limit="1" accept=".key,.pem" @change="files => handleOpcuaFile(files, 'keyContent')"><template #upload-button><a-button long><template #icon><icon-lock /></template>选择私钥</a-button></template></a-upload></a-form-item>
        <a-form-item label="服务端信任证书" required><a-upload :auto-upload="false" :file-list="trustCertificateFiles" :limit="1" accept=".pem,.crt,.cer" @change="files => handleOpcuaFile(files, 'trustCertContent')"><template #upload-button><a-button long><template #icon><icon-safe /></template>选择信任证书</a-button></template></a-upload></a-form-item>
      </a-grid>

      <a-form-item v-if="inputMode === 'content' || isEdit" :label="isOpcuaBundle ? '客户端证书内容' : '证书内容'" field="certContent" :rules="[{ required: !isEdit, message: '请输入证书内容' }]">
        <a-textarea v-model="form.certContent" class="certificate-form__code" :auto-size="{ minRows: 7, maxRows: 14 }" placeholder="-----BEGIN CERTIFICATE-----" />
      </a-form-item>
      <a-form-item v-if="inputMode === 'content' || isEdit" label="私钥内容" field="keyContent" :rules="[{ required: !isEdit, message: '请输入私钥内容' }]">
        <a-textarea v-model="form.keyContent" class="certificate-form__code" :auto-size="{ minRows: 7, maxRows: 14 }" placeholder="-----BEGIN PRIVATE KEY-----" />
      </a-form-item>
      <a-form-item v-if="isOpcuaBundle && (inputMode === 'content' || isEdit)" label="服务端信任证书" field="trustCertContent" :rules="[{ required: !isEdit, message: '请输入服务端信任证书' }]">
        <a-textarea v-model="form.trustCertContent" class="certificate-form__code" :auto-size="{ minRows: 7, maxRows: 14 }" placeholder="-----BEGIN CERTIFICATE-----" />
      </a-form-item>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item label="证书密码"><a-input-password v-model="form.certPassword" placeholder="未设置请留空" /></a-form-item>
        <a-form-item label="私钥密码"><a-input-password v-model="form.keyPassword" placeholder="未设置请留空" /></a-form-item>
      </a-grid>
      <a-form-item label="备注"><a-textarea v-model="form.remark" :max-length="300" show-word-limit /></a-form-item>
    </a-form>
    <template #footer>
      <a-space><a-button @click="visible = false">取消</a-button><a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>{{ inputMode === 'file' && !isEdit ? '上传并创建' : '保存' }}</a-button></a-space>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import type { FileItem, FormInstance } from '@arco-design/web-vue'
import type { CertificateItem } from '@/apis/iot/certificate'
import { Message } from '@arco-design/web-vue'
import { addCertificate, getCertificate, updateCertificate, uploadOpcuaCertificateBundle } from '@/apis/iot/certificate'
import { createUuid } from '@/utils'

defineOptions({ name: 'CertificateFormDrawer' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const inputMode = ref<'file' | 'content'>('file')
const fileList = ref<FileItem[]>([])
const clientCertificateFiles = ref<FileItem[]>([])
const clientPrivateKeyFiles = ref<FileItem[]>([])
const trustCertificateFiles = ref<FileItem[]>([])
const modalBodyStyle = {
  maxHeight: 'min(72vh, 720px)',
  overflowY: 'auto' as const
}
const form = reactive<Partial<CertificateItem>>({ name: '', sslKey: '', certificateType: 'GENERIC', trustMode: undefined, certContent: '', keyContent: '', trustCertContent: '', certPassword: '', keyPassword: '', remark: '' })
const certificateTypeOptions = [{ label: '通用 TLS 证书', value: 'GENERIC' }, { label: 'OPC UA 证书包', value: 'OPCUA_BUNDLE' }]
const trustModeOptions = [{ label: '固定服务端证书', value: 'PINNED' }, { label: 'CA 信任链', value: 'CA' }]
const isOpcuaBundle = computed(() => form.certificateType === 'OPCUA_BUNDLE')

function createSslKey() {
  return `cert-${Date.now().toString(36)}-${createUuid().slice(0, 8)}`
}
function reset() {
  Object.assign(form, { id: '', name: '', sslKey: createSslKey(), certificateType: 'GENERIC', trustMode: undefined, certContent: '', keyContent: '', trustCertContent: '', certPassword: '', keyPassword: '', remark: '' })
  fileList.value = []
  clientCertificateFiles.value = []
  clientPrivateKeyFiles.value = []
  trustCertificateFiles.value = []
  inputMode.value = 'file'
  isEdit.value = false
  formRef.value?.clearValidate()
}
function add() {
  reset()
  visible.value = true
}
async function edit(sslKey: string) {
  reset()
  Object.assign(form, (await getCertificate(sslKey)).data, { certPassword: '', keyPassword: '' })
  inputMode.value = 'content'
  isEdit.value = true
  visible.value = true
}
async function handleFiles(files: FileItem[]) {
  fileList.value = files.slice(-2)
  form.certContent = ''
  form.keyContent = ''
  for (const item of fileList.value) {
    const file = item.file
    if (!file) continue
    const content = await file.text()
    if (!content.trim().startsWith('-----BEGIN')) {
      Message.error(`${file.name} 不是有效的 PEM/KEY 文件`)
      continue
    }
    if (file.name.toLowerCase().endsWith('.key') || content.includes('PRIVATE KEY')) form.keyContent = content
    else form.certContent = content
  }
}
async function handleOpcuaFile(files: FileItem[], field: 'certContent' | 'keyContent' | 'trustCertContent') {
  const next = files.slice(-1)
  if (field === 'certContent') clientCertificateFiles.value = next
  else if (field === 'keyContent') clientPrivateKeyFiles.value = next
  else trustCertificateFiles.value = next
  const file = next[0]?.file
  form[field] = file ? await file.text() : ''
}
async function createFromGenericFiles() {
  const files = fileList.value.map((item) => item.file).filter((file): file is File => Boolean(file))
  if (files.length < 2 || !form.certContent || !form.keyContent) {
    Message.warning('请同时选择证书文件和私钥文件')
    return false
  }
  await addCertificate(form)
  return true
}
async function createFromOpcuaFiles() {
  const clientCertificate = clientCertificateFiles.value[0]?.file
  const clientPrivateKey = clientPrivateKeyFiles.value[0]?.file
  const trustCertificate = trustCertificateFiles.value[0]?.file
  if (!clientCertificate || !clientPrivateKey || !trustCertificate) {
    Message.warning('请完整选择客户端证书、客户端私钥和服务端信任证书')
    return false
  }
  const data = new FormData()
  data.append('sslKey', form.sslKey || '')
  data.append('name', form.name || '')
  data.append('trustMode', form.trustMode || 'PINNED')
  data.append('clientCertificate', clientCertificate)
  data.append('clientPrivateKey', clientPrivateKey)
  data.append('trustCertificate', trustCertificate)
  if (form.keyPassword) data.append('keyPassword', form.keyPassword)
  await uploadOpcuaCertificateBundle(data)
  await updateCertificate({ sslKey: form.sslKey, name: form.name, remark: form.remark, certificateType: 'OPCUA_BUNDLE', trustMode: form.trustMode })
  return true
}
function updatePayload() {
  const payload = { ...form }
  ;(['certContent', 'keyContent', 'trustCertContent', 'certPassword', 'keyPassword'] as const).forEach((key) => {
    if (!payload[key]) delete payload[key]
  })
  return payload
}
async function save() {
  if (await formRef.value?.validate()) return
  saving.value = true
  try {
    if (!isEdit.value && inputMode.value === 'file') {
      if (!await (isOpcuaBundle.value ? createFromOpcuaFiles() : createFromGenericFiles())) return
    } else {
      if (!isEdit.value && (!form.certContent?.trim().startsWith('-----BEGIN') || !form.keyContent?.trim().startsWith('-----BEGIN'))) {
        Message.error('证书和私钥内容必须是 PEM 格式')
        return
      }
      if (!isEdit.value && isOpcuaBundle.value && !form.trustCertContent?.trim().startsWith('-----BEGIN')) {
        Message.error('服务端信任证书必须是 PEM 格式')
        return
      }
      await (isEdit.value ? updateCertificate(updatePayload()) : addCertificate(form))
    }
    Message.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('save-success')
  } finally { saving.value = false }
}
defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.certificate-form {
  &__code {
    font-family: Monaco, Consolas, monospace;
  }

  &__drop {
    display: grid;
    place-items: center;
    min-height: 116px;
    color: var(--color-text-2);
  }

  &__drop > svg {
    font-size: 28px;
    color: rgb(var(--primary-6));
  }

  &__drop span {
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__edit-notice {
    margin-bottom: 14px;
  }

}
</style>
