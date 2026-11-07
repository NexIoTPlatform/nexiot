<template>
  <a-modal v-model:visible="visible" :title="`${providerLabel}配置`" width="760px" :mask-closable="false" @before-ok="save" @close="close">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-grid :cols="2" :col-gap="16" :row-gap="4">
        <a-form-item field="endpoint" label="服务端地址"><a-input v-model="form.endpoint" placeholder="服务端连接地址" /></a-form-item>
        <a-form-item field="bucketName" label="存储桶"><a-input v-model="form.bucketName" placeholder="Bucket 名称" /></a-form-item>
        <a-form-item field="prefix" label="对象前缀"><a-input v-model="form.prefix" placeholder="对象前缀" /></a-form-item>
        <a-form-item field="region" label="区域"><a-input v-model="form.region" placeholder="区域，可选" /></a-form-item>
        <a-form-item field="accessKey" label="访问密钥" :span="2"><a-input v-model="form.accessKey" autocomplete="off" /></a-form-item>
        <a-form-item field="secretKey" label="加密密钥" :span="2">
          <a-input-password v-model="form.secretKey" autocomplete="new-password" placeholder="保存后自动脱敏" />
        </a-form-item>
        <a-form-item field="publicEndpoint" label="公网地址"><a-input v-model="form.publicEndpoint" placeholder="可选" /></a-form-item>
        <a-form-item field="domain" label="绑定域名"><a-input v-model="form.domain" placeholder="可选" /></a-form-item>
        <a-form-item field="isHttps" label="HTTPS"><a-switch v-model="form.isHttps" /></a-form-item>
        <a-form-item field="remark" label="备注" :span="2"><a-textarea v-model="form.remark" :max-length="300" show-word-limit /></a-form-item>
      </a-grid>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { OssStorageConfig } from '@/apis/iot/oss'
import { Message } from '@arco-design/web-vue'
import { getOssStorageConfig, saveOssStorageConfig } from '@/apis/iot/oss'

defineOptions({ name: 'OssConfigModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const visible = ref(false)
const providerLabel = ref('对象存储')
const formRef = useTemplateRef<FormInstance>('formRef')
const createForm = (): OssStorageConfig => ({
  provider: '',
  endpoint: '',
  publicEndpoint: '',
  domain: '',
  prefix: '',
  accessKey: '',
  secretKey: '',
  bucketName: '',
  region: '',
  isHttps: false,
  remark: ''
})
const form = reactive<OssStorageConfig>(createForm())
const rules = {
  endpoint: [{ required: true, message: '请输入服务端地址' }],
  bucketName: [{ required: true, message: '请输入存储桶' }],
  accessKey: [{ required: true, message: '请输入访问密钥' }],
  secretKey: [{ required: true, message: '请输入加密密钥' }]
}

async function open(provider: string, label: string) {
  Object.assign(form, createForm())
  providerLabel.value = label
  visible.value = true
  Object.assign(form, (await getOssStorageConfig(provider)).data)
}

function close() {
  formRef.value?.resetFields()
  Object.assign(form, createForm())
}

async function save() {
  if (await formRef.value?.validate()) return false
  await saveOssStorageConfig({ ...form })
  Message.success('存储配置保存成功')
  emit('save-success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped></style>
