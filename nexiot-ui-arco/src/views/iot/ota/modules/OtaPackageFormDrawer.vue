<template>
  <a-drawer v-model:visible="visible" :title="form.id ? '编辑升级包' : '上传升级包'" width="min(660px, 100%)" :mask-closable="false" :ok-button-props="{ disabled: uploading }" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-alert class="ota-package-form__notice">上传完整文件即可；分包传输时，平台按产品的厂家适配自动切包。</a-alert>
      <a-form-item field="productKey" label="所属产品" :rules="required('请选择所属产品')"><a-select v-model="form.productKey" :options="productOptions" :loading="productSearchLoading" :disabled="locked" allow-search placeholder="请选择产品" @search="searchProducts" @dropdown-visible-change="refreshProducts" @change="handleProductChange" /></a-form-item>
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12">
        <a-form-item field="packageName" label="升级包名称" :rules="required('请输入升级包名称')"><a-input v-model.trim="form.packageName" placeholder="如：温控器固件" /></a-form-item>
        <a-form-item field="version" label="目标版本" :rules="required('请输入版本号')"><a-input v-model.trim="form.version" :disabled="locked" placeholder="如 1.2.0" /></a-form-item>
      </a-grid>
      <a-form-item field="fileUrl" label="完整升级文件" :rules="required('请上传升级包文件或填写下载地址')">
        <div class="ota-package-form__file">
          <a-input v-model="form.fileUrl" :disabled="locked || uploading" placeholder="上传文件，或填写完整下载地址" @input="handleUrlChange" />
          <a-button :loading="uploading" :disabled="locked" @click="fileInputRef?.click()"><template #icon><icon-upload /></template>上传文件</a-button>
          <input ref="fileInputRef" type="file" @change="handleFile" />
        </div>
        <div v-if="form.fileName" class="ota-package-form__file-info">{{ form.fileName }} · {{ form.fileSize == null ? '大小未知' : formatFileSize(form.fileSize) }}</div>
        <div v-if="form.checksum" class="ota-package-form__file-info">已填写 {{ form.checksumAlgorithm }} 校验值</div>
      </a-form-item>
      <a-form-item field="fileSize" label="文件大小（字节）" extra="上传文件自动填写；使用下载地址时，发布前需填写实际字节数。分包升级请使用上传文件。">
        <a-input-number v-model="form.fileSize" :min="1" :max="Number.MAX_SAFE_INTEGER" :precision="0" :disabled="locked || uploading" placeholder="如：3257" />
      </a-form-item>
      <a-alert v-if="hashSkipped" type="warning" class="ota-package-form__notice">本次上传未自动计算校验值。请在高级设置中填入厂家提供的校验值，再保存。</a-alert>
      <a-alert v-if="legacyChecksum && !locked" type="warning" class="ota-package-form__notice">该升级包使用历史校验算法，发布前请改用 SHA256 或 SHA512，并填写对应校验值。</a-alert>
      <a-form-item label="版本说明"><a-textarea v-model="form.releaseNotes" placeholder="简单说明这次升级的变化" :auto-size="{ minRows: 2, maxRows: 5 }" /></a-form-item>
      <a-collapse v-model:active-key="advancedKeys">
        <a-collapse-item key="advanced" header="高级设置（通常无需修改）">
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="12">
            <a-form-item field="packageType" label="升级包类型" :rules="required('请选择升级包类型')"><a-select v-model="form.packageType" :options="packageTypeOptions" :disabled="locked" /></a-form-item>
            <a-form-item label="模块"><a-input v-model.trim="form.module" :disabled="locked" placeholder="APPLICATION" /></a-form-item>
            <a-form-item label="校验算法"><a-select v-model="form.checksumAlgorithm" :options="checksumOptions" :disabled="locked" @change="handleAlgorithmChange" /></a-form-item>
            <a-form-item label="校验值" required extra="SHA256 为 64 位、SHA512 为 128 位十六进制字符。"><a-input v-model.trim="form.checksum" :disabled="locked" placeholder="上传后自动填写，也可填写厂家校验值" /></a-form-item>
          </a-grid>
          <a-form-item label="升级标签"><a-select v-model="form.tagIds" :options="tagOptions" multiple allow-search allow-clear placeholder="用于区分稳定版、测试版或设备批次" /></a-form-item>
          <a-grid :cols="{ xs: 1, sm: 3 }" :col-gap="12"><a-form-item label="最低软件版本"><a-input v-model.trim="form.minSoftwareVersion" /></a-form-item><a-form-item label="最低硬件版本"><a-input v-model.trim="form.minHardwareVersion" /></a-form-item><a-form-item label="最高硬件版本"><a-input v-model.trim="form.maxHardwareVersion" /></a-form-item></a-grid>
          <a-form-item label="签名文件地址"><a-input v-model.trim="form.signatureUrl" /></a-form-item>
          <a-form-item label="强制升级"><a-switch v-model="form.forceUpgrade" /></a-form-item>
          <a-form-item label="备注"><a-textarea v-model="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" /></a-form-item>
        </a-collapse-item>
      </a-collapse>
      <a-alert v-if="locked" type="warning" class="ota-package-form__notice">升级包已发布，版本、文件和校验信息不可修改。</a-alert>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { OtaPackageItem, OtaTagItem } from '@/apis/iot/ota'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { addOtaPackage, getOtaPackage, updateOtaPackage, uploadOtaPackage } from '@/apis/iot/ota'
import { formatFileSize } from '@/utils'

import { useOtaProducts } from '../useOtaProducts'

defineOptions({ name: 'IotOtaPackageFormDrawer' })
const props = defineProps<{ products: ProductItem[], tags: OtaTagItem[] }>()
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const fileInputRef = useTemplateRef<HTMLInputElement>('fileInputRef')
const visible = ref(false)
const uploading = ref(false)
const hashSkipped = ref(false)
const advancedKeys = ref<string[]>([])
const hashes = ref<Record<string, string>>({})
const form = reactive(createForm())
const packageTypeOptions = [{ label: '固件', value: 'FIRMWARE' }, { label: '软件', value: 'SOFTWARE' }, { label: '配置', value: 'CONFIG' }, { label: '资源', value: 'RESOURCE' }]
const checksumAlgorithms = ['SHA256', 'SHA512']
const legacyChecksum = computed(() => !checksumAlgorithms.includes(form.checksumAlgorithm))
const checksumOptions = computed(() => {
  const options = checksumAlgorithms.map((value) => ({ label: value, value, disabled: false }))
  if (legacyChecksum.value) options.push({ label: `${form.checksumAlgorithm}（历史算法）`, value: form.checksumAlgorithm, disabled: true })
  return options
})
const { availableProducts, productOptions, productSearchLoading, searchProducts, refreshProducts } = useOtaProducts(() => props.products)
const tagOptions = computed(() => props.tags.map((item) => ({ label: item.tagName, value: item.id })))
const locked = computed(() => Boolean(form.id && form.status === 'PUBLISHED'))
const required = (message: string) => [{ required: true, message }]
let uploadId = 0

function createForm() {
  return { id: '', productId: '', productKey: '', packageName: '', version: '', packageType: 'FIRMWARE', module: 'APPLICATION', description: '', fileUrl: '', fileName: '', fileSize: undefined as number | undefined, checksumAlgorithm: 'SHA256', checksum: '', signatureUrl: '', minSoftwareVersion: '', minHardwareVersion: '', maxHardwareVersion: '', forceUpgrade: false, tagIds: [] as string[], releaseNotes: '', status: 'DRAFT' }
}
function reset() {
  uploadId++
  uploading.value = false
  hashSkipped.value = false
  advancedKeys.value = []
  hashes.value = {}
  Object.assign(form, createForm())
  formRef.value?.clearValidate()
}
function handleProductChange(value: unknown) {
  const product = availableProducts.value.find((item) => item.productKey === String(value || ''))
  form.productId = product?.id || ''
}
function handleAlgorithmChange() {
  form.checksum = hashes.value[form.checksumAlgorithm] || ''
}
function handleUrlChange() {
  hashes.value = {}
  hashSkipped.value = false
  Object.assign(form, { checksum: '', fileName: '', fileSize: undefined })
}
async function open(record?: OtaPackageItem, productKey?: string) {
  reset()
  if (record) {
    const detail = (await getOtaPackage(record.id)).data
    Object.assign(form, detail, { id: String(detail.id), fileSize: detail.fileSize == null ? undefined : Number(detail.fileSize), tagIds: parseArray(detail.tagIds?.length ? detail.tagIds : detail.tagIdsText) })
    hashes.value = { [form.checksumAlgorithm]: form.checksum }
  } else if (productKey) {
    form.productKey = productKey
    handleProductChange(productKey)
  }
  visible.value = true
}
function parseArray(value: unknown) {
  if (Array.isArray(value)) return value.map(String)
  const text = String(value || '').trim()
  if (!text) return []
  try {
    const result = JSON.parse(text)
    if (Array.isArray(result)) return result.map(String)
  } catch { /* 兼容历史逗号分隔标签。 */ }
  return text.split(',').map((item) => item.trim()).filter(Boolean)
}
async function handleFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const currentUpload = ++uploadId
  uploading.value = true
  try {
    const data = (await uploadOtaPackage(file)).data
    if (currentUpload !== uploadId) return
    hashes.value = { SHA256: data.sha256 || '', SHA512: data.sha512 || '' }
    if (legacyChecksum.value) form.checksumAlgorithm = 'SHA256'
    hashSkipped.value = Boolean(data.hashSkipped)
    Object.assign(form, { fileUrl: data.url || '', fileName: data.fileName || file.name, fileSize: Number(data.fileSize ?? file.size), checksum: hashes.value[form.checksumAlgorithm] || '' })
    if (!form.packageName) form.packageName = file.name.replace(/\.[^.]+$/, '')
    if (!form.checksum) advancedKeys.value = ['advanced']
    Message.success('升级包上传成功')
  } finally {
    if (currentUpload === uploadId) uploading.value = false
  }
}
async function save() {
  if (uploading.value || await formRef.value?.validate()) return false
  if ((form.status === 'PUBLISHED' || form.fileSize != null) && (!Number.isSafeInteger(form.fileSize) || !form.fileSize || form.fileSize < 1)) {
    Message.warning('请填写大于 0 的文件实际字节数')
    return false
  }
  if (legacyChecksum.value && form.status === 'PUBLISHED') {
    advancedKeys.value = ['advanced']
    Message.warning('请选择 SHA256 或 SHA512，并填写对应校验值')
    return false
  }
  if (!form.checksum) {
    advancedKeys.value = ['advanced']
    Message.warning('请在高级设置中填写所选算法对应的校验值')
    return false
  }
  const checksumLength = form.checksumAlgorithm === 'SHA512' ? 128 : 64
  if (form.status === 'PUBLISHED' && !new RegExp(`^[0-9a-f]{${checksumLength}}$`, 'i').test(form.checksum)) {
    advancedKeys.value = ['advanced']
    Message.warning(`${form.checksumAlgorithm} 校验值应为 ${checksumLength} 位十六进制字符`)
    return false
  }
  await (form.id ? updateOtaPackage(form) : addOtaPackage(form))
  Message.success(form.id ? '升级包保存成功' : '升级包创建成功，请发布后创建升级计划')
  emit('save-success')
  return true
}
defineExpose({ open })
</script>

<style lang="scss" scoped>
.ota-package-form {
  &__notice {
    margin-bottom: 16px;
  }

  &__file {
    display: flex;
    gap: 8px;
    width: 100%;

    input[type='file'] {
      display: none;
    }
  }

  &__file-info {
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
