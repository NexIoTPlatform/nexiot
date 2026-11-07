<template>
  <a-modal v-model:visible="visible" title="厂家 OTA 适配" width="min(660px, 100%)" :mask-closable="false" :ok-loading="saving" :ok-button-props="{ disabled: loading || !loaded }" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item label="所属产品" required>
        <a-select v-model="productKey" :options="productOptions" :loading="productSearchLoading" :disabled="saving" allow-search placeholder="选择需要适配的产品" @search="searchProducts" @dropdown-visible-change="refreshProducts" @change="loadProfile" />
      </a-form-item>
      <a-spin :loading="loading" class="ota-profile__content">
        <template v-if="loaded">
          <a-form-item label="设备支持的升级方式">
            <a-radio-group v-model="form.mode" direction="vertical" @change="handleModeChange">
              <a-radio v-for="item in transferModeOptions" :key="item.value" :value="item.value">
                <strong>{{ item.label }}</strong><div class="ota-profile__description">{{ item.description }}</div>
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item field="templateName" label="适配名称"><a-input v-model.trim="form.templateName" placeholder="如：厂家 A · MQTT 固件升级" :max-length="100" /></a-form-item>
          <a-collapse v-if="form.mode !== 'URL'" :default-active-key="['chunk']">
            <a-collapse-item key="chunk" header="分包设置（按厂家协议填写）">
              <a-form-item field="chunkSize" label="每包字节数" :rules="[{ required: true, message: '请填写每包字节数' }]"><a-input-number v-model="form.chunkSize" :min="128" :max="32768" :precision="0" /></a-form-item>
              <a-collapse>
                <a-collapse-item key="advanced" header="高级参数">
                  <a-grid :cols="2" :col-gap="12">
                    <a-form-item field="ackTimeoutSeconds" label="确认超时（秒）" :rules="[{ required: true, message: '请填写确认超时' }]"><a-input-number v-model="form.ackTimeoutSeconds" :min="2" :max="300" :precision="0" /></a-form-item>
                    <a-form-item field="chunkMaxRetries" label="每包补发次数" :rules="[{ required: true, message: '请填写补发次数' }]"><a-input-number v-model="form.chunkMaxRetries" :min="0" :max="10" :precision="0" /></a-form-item>
                    <a-form-item label="分片内容编码"><a-select v-model="form.dataEncoding" :options="['BASE64', 'HEX']" /></a-form-item>
                    <a-form-item label="分片编号起点"><a-select v-model="form.indexBase" :options="[{ label: '从 0 开始', value: 0 }, { label: '从 1 开始', value: 1 }]" /></a-form-item>
                  </a-grid>
                </a-collapse-item>
              </a-collapse>
            </a-collapse-item>
          </a-collapse>
          <a-alert type="info" class="ota-profile__notice">这里保存传输规则。厂家升级指令、分片确认和结果报文，仍需技术人员在产品协议脚本中适配一次，并用真实设备验证。</a-alert>
          <div class="ota-profile__description ota-profile__notice">保存后用于新建计划；已创建的计划继续使用创建时的配置。</div>
        </template>
        <a-alert v-else-if="!loading && productKey" type="warning">适配配置未能加载，请检查后端是否已升级。<a-link @click="loadProfile">重新加载</a-link></a-alert>
        <a-empty v-else-if="!loading" description="请选择产品加载当前适配" />
      </a-spin>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { createTransferProfile, getOtaTransferProfile, transferModeOptions, updateOtaTransferProfile } from '../api'

import { useOtaProducts } from '../useOtaProducts'

defineOptions({ name: 'IotOtaTransferProfileModal' })
const props = defineProps<{ products: ProductItem[] }>()
const emit = defineEmits<{ (e: 'save-success'): void }>()
const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const loading = ref(false)
const loaded = ref(false)
const saving = ref(false)
const productKey = ref('')
const form = reactive(createTransferProfile())
const { productOptions, productSearchLoading, searchProducts, refreshProducts } = useOtaProducts(() => props.products)
let requestId = 0

function reset() {
  requestId++
  loading.value = false
  loaded.value = false
  productKey.value = ''
  Object.assign(form, createTransferProfile())
  formRef.value?.clearValidate()
}

function handleModeChange() {
  formRef.value?.clearValidate()
}

async function loadProfile() {
  const currentRequest = ++requestId
  loaded.value = false
  if (!productKey.value) return
  loading.value = true
  try {
    const { data } = await getOtaTransferProfile(productKey.value)
    if (currentRequest !== requestId) return
    Object.assign(form, createTransferProfile(), data)
    loaded.value = true
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

async function open(key?: string) {
  reset()
  visible.value = true
  if (key) {
    productKey.value = key
    await loadProfile()
  }
}

async function save() {
  if (!productKey.value || !loaded.value || loading.value || await formRef.value?.validate()) return false
  saving.value = true
  try {
    await updateOtaTransferProfile(productKey.value, { ...form })
    Message.success('厂家适配已保存，新建升级计划时生效')
    emit('save-success')
    return true
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.ota-profile {
  &__content {
    display: block;
    width: 100%;
  }

  &__description {
    font-size: 12px;
    line-height: 1.7;
    color: var(--color-text-3);
  }

  &__notice {
    margin-top: 16px;
  }
}
</style>
