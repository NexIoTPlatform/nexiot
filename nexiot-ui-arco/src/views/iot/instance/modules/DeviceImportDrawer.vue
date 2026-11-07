<template>
  <a-drawer v-model:visible="visible" title="批量导入设备" width="min(520px, 100%)" :mask-closable="false" @before-ok="submit" @close="reset">
    <!-- <a-alert type="info" class="device-import__notice">先选择产品并下载对应模板。导入任务提交后可继续使用页面，列表会自动刷新。</a-alert> -->
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item field="productKey" label="所属产品" :rules="[{ required: true, message: '请选择所属产品' }]">
        <a-select v-model="form.productKey" :options="productOptions" allow-search placeholder="请选择产品" />
      </a-form-item>
      <a-form-item label="导入模板">
        <a-button :disabled="!form.productKey" @click="downloadTemplate"><template #icon><icon-download /></template>下载当前产品模板</a-button>
      </a-form-item>
      <a-form-item field="file" label="设备文件" :rules="[{ required: true, message: '请选择设备文件' }]">
        <a-upload :file-list="fileList" :auto-upload="false" :limit="1" accept=".xlsx,.xls,.csv" draggable @change="handleFileChange" />
      </a-form-item>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import type { FileItem, FormInstance } from '@arco-design/web-vue'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { downloadDeviceTemplate, importDevices } from '@/apis/iot/device'
import { getProductList } from '@/apis/iot/product'

defineOptions({ name: 'IotDeviceImportDrawer' })

const emit = defineEmits<{
  (e: 'import-success'): void
}>()

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const products = ref<ProductItem[]>([])
const fileList = ref<FileItem[]>([])
const form = reactive({ productKey: '', file: undefined as File | undefined })
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
function reset() {
  Object.assign(form, { productKey: '', file: undefined })
  fileList.value = []
  formRef.value?.resetFields()
}

async function open(productKey?: string) {
  reset()
  products.value = (await getProductList({ page: 1, size: 500 })).data.records || []
  form.productKey = productKey || ''
  visible.value = true
}

function handleFileChange(files: FileItem[]) {
  fileList.value = files.slice(-1)
  form.file = fileList.value[0]?.file
}

async function downloadTemplate() {
  await downloadDeviceTemplate(form.productKey)
  Message.success('模板下载完成')
}

async function submit() {
  if (await formRef.value?.validate()) return false
  if (!form.file) return false
  await importDevices(form.file, { productKey: form.productKey })
  Message.success('设备导入任务已提交')
  emit('import-success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.device-import {
  &__notice {
    margin-bottom: 16px;
  }
}
</style>
