<template>
  <a-drawer v-model:visible="visible" :title="isEdit ? '编辑设备' : '新增设备'" width="560px" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item v-if="product.deviceNode === 'GATEWAY_SUB_DEVICE'" field="extDeviceId" label="所属网关设备" :rules="[{ required: true, message: '请选择所属网关设备' }]">
        <a-select v-model="form.extDeviceId" :options="gatewayOptions" allow-search placeholder="请选择所属网关设备" />
      </a-form-item>
      <a-grid :cols="2" :col-gap="12">
        <a-form-item field="deviceId" label="设备名称 / DeviceId" :rules="deviceIdRules"><a-input v-model="form.deviceId" :disabled="isEdit" placeholder="请输入设备序列号" /></a-form-item>
        <a-form-item field="deviceName" label="设备名称" :rules="[{ required: true, message: '请输入设备名称' }]"><a-input v-model="form.deviceName" /></a-form-item>
      </a-grid>
      <a-form-item label="设备密钥"><a-input-password v-model="form.secretKey" :disabled="isEdit" placeholder="按接入方式要求填写" /></a-form-item>
      <a-form-item label="所属部门"><a-tree-select v-model="form.deptId" :data="deptOptions" :field-names="{ key: 'id', title: 'name', children: 'children' }" allow-search allow-clear placeholder="不选择时使用当前账号部门" /></a-form-item>

      <section v-if="customFields.length" class="device-config">
        <h4>设备接入参数</h4>
        <a-form-item v-for="field in customFields" :key="field.id" :label="field.name" :required="field.required" :extra="field.helpText || field.description">
          <a-input-number v-if="field.type === 'number'" :model-value="getNumber(field.id)" :min="field.min" :max="field.max" @update:model-value="setCustom(field.id, $event)" />
          <a-select v-else-if="field.type === 'select'" :model-value="getSelect(field.id)" :options="field.options" allow-clear @update:model-value="setCustom(field.id, $event)" />
          <a-switch v-else-if="field.type === 'boolean'" :model-value="Boolean(form.customValues[field.id])" @update:model-value="setCustom(field.id, $event)" />
          <a-input-password v-else-if="field.type === 'password'" :model-value="getText(field.id)" :disabled="field.disabled" :max-length="field.maxLength" :placeholder="field.placeholder" @update:model-value="setCustom(field.id, $event)" />
          <a-textarea v-else-if="field.type === 'textarea'" :model-value="getText(field.id)" :disabled="field.disabled" :max-length="field.maxLength" :auto-size="{ minRows: 3, maxRows: 6 }" :placeholder="field.placeholder" @update:model-value="setCustom(field.id, $event)" />
          <a-input v-else :model-value="getText(field.id)" :disabled="field.disabled" :min-length="field.minLength" :max-length="field.maxLength" :placeholder="field.placeholder" @update:model-value="setCustom(field.id, $event)" />
        </a-form-item>
      </section>

      <a-grid :cols="2" :col-gap="12"><a-form-item label="经度"><a-input-number v-model="form.longitude" :min="-180" :max="180" /></a-form-item><a-form-item label="纬度"><a-input-number v-model="form.latitude" :min="-90" :max="90" /></a-form-item></a-grid>
      <a-form-item label="备注"><a-textarea v-model="form.detail" :auto-size="{ minRows: 3, maxRows: 6 }" /></a-form-item>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { ProductDeviceItem, ProductItem } from '@/apis/iot/product'
import type { ListItem as DeptItem } from '@/apis/system/dept'
import type { DeviceCustomField } from '@/views/iot/instance/modules/deviceCustomFields'
import { Message } from '@arco-design/web-vue'
import { addProductDevice, getProductDevice, getProductGatewayDevices, updateProductDevice } from '@/apis/iot/product'
import { getDeptOptions } from '@/apis/system/dept'
import {
  normalizeDeviceCustomValues,
  visibleDeviceCustomFields
} from '@/views/iot/instance/modules/deviceCustomFields'

defineOptions({ name: 'ProductDeviceFormDrawer' })

const props = defineProps<{ product: ProductItem }>()

const emit = defineEmits<{ (e: 'save-success'): void }>()

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const deptOptions = ref<DeptItem[]>([])
const gatewayOptions = ref<{ label: string, value: string }[]>([])
const form = reactive({ id: '', deviceId: '', deviceName: '', productKey: '', secretKey: '', deptId: '', extDeviceId: '', gwDeviceId: '', longitude: undefined as number | undefined, latitude: undefined as number | undefined, detail: '', customValues: {} as Record<string, unknown> })
const isEdit = computed(() => Boolean(form.id))
const deviceIdRules = [{ required: true, message: '请输入设备序列号' }, { match: /^[\w-]+$/, message: '仅支持字母、数字、下划线和短横线' }]
const allCustomFields = computed<DeviceCustomField[]>(() => {
  try {
    const data = JSON.parse(props.product.thirdConfiguration || '{}') as { customField?: DeviceCustomField[] }
    return data.customField || []
  } catch {
    return []
  }
})
const customFields = computed(() => visibleDeviceCustomFields(allCustomFields.value, form.customValues))

function parseObject(value?: string) {
  try {
    const data = JSON.parse(value || '{}')
    return data && typeof data === 'object' ? data : {}
  } catch {
    return {}
  }
}

function normalizeCustomValues(source: Record<string, unknown> = {}) {
  return normalizeDeviceCustomValues(allCustomFields.value, source)
}

function reset() {
  Object.assign(form, { id: '', deviceId: '', deviceName: '', productKey: props.product.productKey, secretKey: '', deptId: '', extDeviceId: '', gwDeviceId: '', longitude: undefined, latitude: undefined, detail: '', customValues: normalizeCustomValues() })
  formRef.value?.resetFields()
}

async function loadOptions() {
  deptOptions.value = (await getDeptOptions()).data
  if (props.product.deviceNode === 'GATEWAY_SUB_DEVICE') {
    gatewayOptions.value = ((await getProductGatewayDevices(props.product.productKey)).data || []).map((item) => ({ label: `${item.deviceName || item.deviceId} - ${item.deviceId}`, value: item.deviceId }))
  }
}

async function add() {
  reset()
  await loadOptions()
  visible.value = true
}

async function edit(id: string) {
  reset()
  await loadOptions()
  const data = (await getProductDevice(id)).data
  const values = { ...parseObject(data.configuration), ...(data.otherParams || {}) }
  const customValues = normalizeCustomValues(values)
  Object.assign(form, data, { id: String(data.id), longitude: data.longitude === undefined ? undefined : Number(data.longitude), latitude: data.latitude === undefined ? undefined : Number(data.latitude), customValues })
  visible.value = true
}

function getText(id: string) {
  const value = form.customValues[id]
  return value === undefined || value === null ? '' : String(value)
}

function getNumber(id: string) {
  const value = form.customValues[id]
  return value === undefined || value === '' ? undefined : Number(value)
}

function getSelect(id: string) {
  const value = form.customValues[id]
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? value : undefined
}

function setCustom(id: string, value: unknown) {
  form.customValues[id] = value
  form.customValues = normalizeCustomValues(form.customValues)
}

async function save() {
  if (await formRef.value?.validate()) return false
  form.customValues = normalizeCustomValues(form.customValues)
  const missing = customFields.value.find((field) => field.required && (form.customValues[field.id] === undefined || form.customValues[field.id] === ''))
  if (missing) {
    Message.error(`请填写${missing.name}`)
    return false
  }
  const payload: Partial<ProductDeviceItem> = {
    ...form,
    productKey: props.product.productKey,
    gwDeviceId: form.extDeviceId || undefined,
    configuration: JSON.stringify(form.customValues),
    otherParams: form.customValues
  }
  delete (payload as Partial<ProductDeviceItem> & { customValues?: Record<string, unknown> }).customValues
  await (isEdit.value ? updateProductDevice(payload) : addProductDevice(payload))
  Message.success(isEdit.value ? '设备保存成功' : '设备添加成功')
  emit('save-success')
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.device-config {
  padding: 14px;
  margin-bottom: 16px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-1);
  border-radius: 6px;

  h4 {
    margin: 0 0 14px;
    font-size: 14px;
  }

  :deep(.arco-input-number) {
    width: 100%;
  }
}
</style>
