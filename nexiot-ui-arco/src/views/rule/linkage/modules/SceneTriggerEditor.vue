<template>
  <section class="trigger-editor">
    <a-grid :cols="{ xs: 1, sm: 2, md: 4 }" :col-gap="12" :row-gap="12">
      <a-select v-model="model.trigger" :options="triggerOptions" placeholder="触发器类型" @change="changeTrigger" />
      <template v-if="model.trigger === 'timer'">
        <a-select v-model="model.cron" :options="cronPresets" allow-create allow-search placeholder="Quartz Cron 表达式" />
      </template>
      <template v-if="model.trigger === 'device'">
        <a-button class="trigger-editor__device" @click="pickerRef?.open(model.productKey)"><template #icon><icon-storage /></template>{{ model.deviceName || '选择设备' }}</a-button>
        <a-select v-model="model.type" :options="messageTypeOptions" placeholder="属性或事件" @change="changeMessageType" />
        <a-select v-if="model.type === 'event'" v-model="model.modelId" :options="eventOptions" placeholder="物模型事件" allow-search allow-create @change="changeEvent" />
      </template>
    </a-grid>
    <template v-if="model.trigger === 'device' && (model.type === 'properties' || model.type === 'event')">
      <div v-for="(filter, index) in model.filters" :key="index" class="trigger-editor__filter">
        <a-select v-model="filter.key" :options="fieldOptions" allow-search allow-create :placeholder="model.type === 'event' ? '事件参数或 data 字段路径' : '物模型属性'" />
        <a-select v-model="filter.operator" :options="operatorOptions" placeholder="操作符" />
        <a-input :model-value="filter.value == null ? '' : String(filter.value)" placeholder="条件值" @update:model-value="filter.value = $event" />
        <a-tooltip content="删除条件"><a-button type="text" status="danger" shape="circle" @click="removeFilter(index)"><template #icon><icon-delete /></template></a-button></a-tooltip>
      </div>
      <a-button type="text" :disabled="model.type === 'event' && !model.modelId" @click="addFilter"><template #icon><icon-plus /></template>{{ model.type === 'event' ? '添加事件参数条件' : '添加属性条件' }}</a-button>
      <a-alert v-if="model.type === 'event'" type="info">不添加条件时，收到所选事件即触发。添加后需同时满足全部条件；可选择事件参数，或输入 data.channelId、data.alarmState、data.images.0.url 等字段路径。</a-alert>
    </template>
    <a-alert v-if="model.trigger === 'manual'">该场景只会通过“手动触发”命令执行。</a-alert>
    <a-alert v-else-if="model.trigger === 'timer'">使用 Quartz Cron，例如 `0 0/5 * * * ?` 表示每 5 分钟执行。</a-alert>
    <DevicePickerModal ref="pickerRef" @select="selectDevice" />
  </section>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { SceneTrigger } from '../api'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'
import { getSceneProductMetadata } from '../api'
import { metadataFieldOptions } from '../metadataFields'
import DevicePickerModal from './DevicePickerModal.vue'

const model = defineModel<SceneTrigger>({ required: true })
const props = defineProps<{ deviceOnly?: boolean }>()
const pickerRef = useTemplateRef<InstanceType<typeof DevicePickerModal>>('pickerRef')
const properties = ref<ProductMetadataItem[]>([])
const events = ref<ProductMetadataItem[]>([])
const triggerOptions = computed(() => props.deviceOnly ? [{ label: '设备触发', value: 'device' }] : [{ label: '设备触发', value: 'device' }, { label: '定时触发', value: 'timer' }, { label: '手动触发', value: 'manual' }])
const messageTypeOptions = [{ label: '属性', value: 'properties' }, { label: '事件', value: 'event' }]
const operatorOptions = [{ label: '=', value: 'eq' }, { label: '!=', value: 'ne' }, { label: '>', value: 'gt' }, { label: '<', value: 'lt' }, { label: '>=', value: 'gte' }, { label: '<=', value: 'lte' }, { label: '包含', value: 'in' }, { label: '不包含', value: 'nin' }, { label: '模糊匹配', value: 'like' }]
const cronPresets = [{ label: '每分钟', value: '0 * * * * ?' }, { label: '每 5 分钟', value: '0 0/5 * * * ?' }, { label: '每小时', value: '0 0 * * * ?' }, { label: '每天 00:00', value: '0 0 0 * * ?' }]
const fieldOptions = computed(() => {
  if (model.value.type !== 'event') return metadataFieldOptions(properties.value)
  const event = events.value.find((item) => item.id === model.value.modelId)
  const type = event?.valueType as { properties?: ProductMetadataItem[] } | undefined
  return metadataFieldOptions(type?.properties || [], 'data')
})
const eventOptions = computed(() => events.value.map((item) => ({ label: `${item.name} (${item.id})`, value: item.id })))
function changeTrigger() { Object.assign(model.value, { cron: undefined, deviceId: undefined, deviceName: undefined, productKey: undefined, type: undefined, modelId: undefined, filters: [] }); properties.value = []; events.value = [] }
function changeMessageType() { model.value.modelId = undefined; model.value.filters = model.value.type === 'properties' ? [{}] : [] }
function changeEvent() { model.value.filters = [] }
function addFilter() { (model.value.filters ||= []).push({}) }
function removeFilter(index: number) { model.value.filters?.splice(index, 1) }
async function loadMetadata() {
  const productKey = model.value.productKey
  properties.value = []
  events.value = []
  if (!productKey) return
  const data = (await getSceneProductMetadata(productKey)).data
  if (productKey !== model.value.productKey) return
  properties.value = data.properties || []
  events.value = data.events || []
}
function selectDevice(device: DeviceItem) {
  Object.assign(model.value, { deviceId: device.deviceId, deviceName: device.deviceName || device.deviceId, productKey: device.productKey, type: undefined, modelId: undefined, filters: [] })
}
watch(() => model.value.productKey, loadMetadata, { immediate: true })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.trigger-editor { display: grid; gap: 12px; }
.trigger-editor__device { justify-content: flex-start; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.trigger-editor__filter { display: grid; grid-template-columns: minmax(180px, 1.2fr) minmax(110px, .7fr) minmax(160px, 1fr) 32px; gap: 8px; }

@media (width <= 768px) { .trigger-editor__filter { grid-template-columns: 1fr 1fr; } }

@media (width <= 480px) { .trigger-editor__filter { grid-template-columns: 1fr; } }
</style>
