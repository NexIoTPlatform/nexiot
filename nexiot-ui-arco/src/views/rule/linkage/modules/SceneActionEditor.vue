<template>
  <section class="action-editor">
    <a-grid :cols="{ xs: 1, sm: 2, md: 4 }" :col-gap="12" :row-gap="12">
      <a-select v-model="model.trigger" :options="actionOptions" placeholder="动作类型" @change="changeAction" />
      <template v-if="model.trigger === 'device'">
        <a-button class="action-editor__device" @click="pickerRef?.open(model.productKey)"><template #icon><icon-storage /></template>{{ model.deviceName || '选择设备' }}</a-button>
        <a-select v-model="model.type" :options="[{ label: '功能调用', value: 'functions' }]" placeholder="调用类型" @change="model.modelId = undefined" />
        <a-select v-if="model.type === 'functions'" v-model="model.modelId" :options="functionOptions" placeholder="物模型功能" allow-search @change="selectFunction" />
      </template>
      <template v-if="model.trigger === 'notice'">
        <a-select v-model="model.noticeTemplateId" :options="templateOptions" :loading="templateLoading" allow-search placeholder="通知模板" @change="selectTemplate" />
        <a-input v-model.trim="model.receivers" placeholder="手机号、邮箱、微信 OpenID 或 user:绑定标识，逗号分隔" />
      </template>
    </a-grid>
    <div v-if="model.trigger === 'device' && model.execData?.length" class="action-editor__params">
      <MetadataValueInput v-for="(item, index) in model.execData" :key="item.id" v-model="model.execData[index]" />
    </div>
    <div v-if="selectedWechat?.wechat" class="wechat-rule-mapping"><b>微信字段映射</b><div v-for="field in selectedWechat.wechat.fields" :key="field.key">{{ field.key }} → {{ field.value }}</div><div>触发消息需提供：{{ noticeVariables(selectedWechat).join('、') }}</div></div>
    <DevicePickerModal ref="pickerRef" @select="selectDevice" />
  </section>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { SceneAction, SceneActionParam } from '../api'
import type { NoticeTemplate } from '@/apis/application/notice'
import type { DeviceItem, DeviceShadow } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import type { RuntimeField } from '@/views/iot/instance/details/modules/deviceCommand'
import { getDeviceShadow } from '@/apis/iot/device'
import { noticeVariables } from '@/views/application/notice/modules/wechat'
import { autoFillCommandFields, coerceRuntimeValue, fieldSchema, normalizeInputs } from '@/views/iot/instance/details/modules/deviceCommand'
import { getSceneDevices, getSceneNoticeTemplates, getSceneProductMetadata } from '../api'
import DevicePickerModal from './DevicePickerModal.vue'
import MetadataValueInput from './MetadataValueInput.vue'

const model = defineModel<SceneAction>({ required: true })
const DESIRED_FUNCTION_ID = 'setDeviceDesiredProperties'
const pickerRef = useTemplateRef<InstanceType<typeof DevicePickerModal>>('pickerRef')
const functions = ref<ProductMetadataItem[]>([])
const templates = ref<NoticeTemplate[]>([])
const templateLoading = ref(false)
const selectedDevice = ref<DeviceItem>()
const deviceShadow = ref<DeviceShadow>()
const selectedWechat = computed(() => templates.value.find((item) => String(item.id) === String(model.value.noticeTemplateId)))
const actionOptions = [{ label: '设备输出', value: 'device' }, { label: '通知发送', value: 'notice' }]
const functionOptions = computed(() => functions.value.map((item) => ({ label: `${item.name} (${item.id})`, value: item.id })))
const templateOptions = computed(() => templates.value.map((item) => ({ label: `${item.name} / ${item.channelType}`, value: item.id! })))
function cloneParams(items?: ProductMetadataItem[]) { return normalizeInputs(structuredClone(toRaw(items || []))) as SceneActionParam[] }
function changeAction() { Object.assign(model.value, { type: undefined, deviceId: undefined, deviceName: undefined, productKey: undefined, modelId: undefined, execData: [], noticeTemplateId: undefined, noticeTemplateName: undefined, receivers: undefined }); if (model.value.trigger === 'notice') loadTemplates() }
function selectTemplate(value: unknown) { const item = templates.value.find((template) => template.id === String(value)); model.value.noticeTemplateName = item?.name }
function fillRuntimeParams(params: SceneActionParam[], overwrite: boolean) {
  const normalized = params.map((item) => {
    const type = String(fieldSchema(item as RuntimeField).type || item.type || '').toLowerCase()
    if (!['int', 'integer', 'short', 'byte', 'long', 'float', 'double', 'decimal', 'number', 'boolean', 'bool', 'enum', 'string', 'text', 'password'].includes(type)) return item
    const value = coerceRuntimeValue(item as RuntimeField, item.params)
    return { ...item, params: value === undefined ? item.params : value }
  })
  if (!selectedDevice.value || model.value.modelId === DESIRED_FUNCTION_ID) return normalized
  const result = autoFillCommandFields(params as RuntimeField[], selectedDevice.value, deviceShadow.value)
  return normalized.map((item) => {
    if (!result.sources[item.id] || (!overwrite && item.params !== undefined && item.params !== '')) return item
    return { ...item, params: result.values[item.id] }
  })
}
function selectFunction(value: unknown) {
  const fn = functions.value.find((item) => item.id === String(value))
  model.value.execData = fillRuntimeParams(cloneParams(fn?.inputs), true)
}
async function loadMetadata() {
  const productKey = model.value.productKey
  if (!productKey) return
  const metadata = (await getSceneProductMetadata(productKey)).data
  if (model.value.productKey !== productKey) return
  functions.value = metadata.functions || []
  const fn = functions.value.find((item) => item.id === model.value.modelId)
  if (!fn || !model.value.execData?.length) return
  model.value.execData = fillRuntimeParams(model.value.execData.map((item) => {
    const source = fn.inputs?.find((input) => input.id === item.id)
    return source ? { ...source, ...item, valueType: source.valueType } : item
  }), false)
}
async function loadDeviceContext(device: DeviceItem) {
  selectedDevice.value = device
  deviceShadow.value = undefined
  if (!device.iotId) return
  try {
    const shadow = (await getDeviceShadow(device.iotId)).data || undefined
    if (selectedDevice.value?.iotId === device.iotId) deviceShadow.value = shadow
  } catch { if (selectedDevice.value?.iotId === device.iotId) deviceShadow.value = undefined }
}
async function restoreDeviceContext() {
  if (!model.value.deviceId) return
  const res = await getSceneDevices({ page: 1, size: 10, productKey: model.value.productKey, deviceId: model.value.deviceId })
  const device = (res.data.records || []).find((item) => item.deviceId === model.value.deviceId)
  if (device) await loadDeviceContext(device)
}
async function selectDevice(device: DeviceItem) {
  Object.assign(model.value, { deviceId: device.deviceId, deviceName: device.deviceName || device.deviceId, productKey: device.productKey, type: 'functions', modelId: undefined, execData: [] })
  await Promise.all([loadMetadata(), loadDeviceContext(device)])
  if (model.value.deviceId === device.deviceId && model.value.productKey === device.productKey && model.value.execData?.length) model.value.execData = fillRuntimeParams(model.value.execData, false)
}
async function loadTemplates() { if (templates.value.length) return; templateLoading.value = true; try { templates.value = (await getSceneNoticeTemplates({ page: 1, size: 500 })).data.records || [] } finally { templateLoading.value = false } }
watch(() => model.value, async () => {
  functions.value = []
  selectedDevice.value = undefined
  deviceShadow.value = undefined
  if (model.value.trigger === 'device') {
    await Promise.all([loadMetadata(), restoreDeviceContext()])
    if (model.value.execData?.length) model.value.execData = fillRuntimeParams(model.value.execData, false)
  }
  if (model.value.trigger === 'notice') await loadTemplates()
}, { immediate: true })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.action-editor { display: grid; gap: 12px; }
.action-editor__device { justify-content: flex-start; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.action-editor__params { border-top: 1px solid var(--color-border-2); }
</style>
