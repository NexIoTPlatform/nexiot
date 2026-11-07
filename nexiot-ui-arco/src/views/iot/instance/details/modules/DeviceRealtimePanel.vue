<template>
  <section class="realtime-panel">
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-radio-group v-model="activeType" type="button"><a-radio value="properties">属性 {{ properties.length }}</a-radio><a-radio value="events">事件 {{ events.length }}</a-radio></a-radio-group>
      </a-space>
      <a-space wrap>
        <span class="realtime-panel__updated">影子版本 {{ shadow.version || '-' }} · {{ formatTime(shadow.lastTime) }}</span>
        <a-select v-model="refreshSeconds" class="realtime-panel__interval" :options="refreshOptions" />
        <a-button :loading="activeType === 'events' ? eventsLoading : loading" @click="refreshData"><template #icon><icon-refresh /></template>刷新</a-button>
      </a-space>
    </a-row>

    <a-alert v-if="activeType === 'events' && eventsError" type="warning">{{ eventsError }}</a-alert>
    <a-spin :loading="activeType === 'events' ? eventsLoading && !eventTotals.length : loading" class="realtime-panel__content">
      <div v-if="activeType === 'properties' && properties.length" class="realtime-panel__cards">
        <article v-for="field in properties" :key="field.id" class="status-card property-card" :class="{ 'property-card--with-desired': hasDifferentDesired(field) }">
          <div class="card-header">
            <div class="card-title" :title="`${field.name}(${field.id})`">
              {{ field.name || field.id }}
              <span class="property-key">({{ field.id }})</span>
            </div>
            <div class="card-header-actions">
              <a-tooltip v-if="isWritableProperty(field)" content="设置设备期望值">
                <div class="icon-action edit-action" @click.stop="openDesired(field)">
                  <icon-edit />
                </div>
              </a-tooltip>
              <div class="card-badge badge-property">
                <span class="badge-dot" :class="{ 'badge-dot--desired': hasDifferentDesired(field) }">●</span>
                <span class="badge-text">属性</span>
              </div>
            </div>
          </div>

          <div class="card-value property-value" :class="{ 'property-value--dual': hasDifferentDesired(field), 'property-value--single': !hasDifferentDesired(field), 'property-value--empty': !hasDisplayValue(field.reportedValue) && !hasDisplayValue(field.desiredValue) }">
            <template v-if="hasDifferentDesired(field)">
              <div class="property-dual-layout">
                <div class="property-column">
                  <a-tooltip v-if="hasDisplayValue(field.reportedValue)" content="上报值">
                    <div class="property-column-value property-column-value--reported">{{ formatFieldValue(field, field.reportedValue) }}</div>
                  </a-tooltip>
                  <div v-else class="property-column-value property-column-value--empty">--</div>
                </div>
                <div class="property-divider"></div>
                <div class="property-column">
                  <a-tooltip content="期望值">
                    <div class="property-column-value property-column-value--desired">{{ formatFieldValue(field, field.desiredValue) }}</div>
                  </a-tooltip>
                </div>
              </div>
            </template>
            <template v-else-if="hasDisplayValue(field.reportedValue)">
              <a-tooltip :content="formatFieldValue(field, field.reportedValue)">
                <div class="property-main-value">{{ formatFieldValue(field, field.reportedValue) }}</div>
              </a-tooltip>
            </template>
            <div v-else class="property-empty-text">暂无数据</div>
          </div>

          <div class="card-footer">
            <div class="card-time">
              <icon-clock-circle />
              {{ formatTime(shadow.lastTime) || '暂无时间' }}
            </div>
            <div class="card-actions">
              <div v-if="isTrackField(field)" class="history-btn track-btn" title="查看轨迹地图" @click="openTrack(field)">
                <icon-location />
                <span class="history-text">轨迹</span>
              </div>
              <div class="history-btn" title="查看历史数据" @click="openHistory(field, 'PROPERTIES')">
                <icon-history />
                <span class="history-text">历史</span>
              </div>
            </div>
          </div>
        </article>
      </div>
      <div v-else-if="activeType === 'events' && events.length" class="realtime-panel__cards">
        <article v-for="field in events" :key="field.id" class="status-card event-card">
          <div class="card-header">
            <div class="card-title" :title="`${field.name}(${field.id})`">
              {{ field.name || field.id }}
              <span class="event-key">({{ field.id }})</span>
            </div>
            <div class="card-badge badge-event" :class="eventLevelColor(field) === 'red' || eventLevelColor(field) === 'orange' ? 'badge-warning' : 'badge-info'">
              <span class="badge-dot">●</span>
              {{ eventLevelLabel(field) }}
            </div>
          </div>

          <div class="card-value event-value" :title="field.description || '暂无事件描述'">
            <div class="event-count">{{ field.qty || 0 }}次</div>
            <div class="event-description">{{ field.description || '暂无事件描述' }}</div>
          </div>

          <div class="card-footer">
            <div class="card-time">
              <icon-clock-circle />
              {{ formatTime(field.time) || '暂无时间' }}
            </div>
            <div class="card-actions">
              <div class="history-btn" title="查看历史数据" @click="openHistory(field, 'EVENT')">
                <icon-history />
                <span class="history-text">历史</span>
              </div>
            </div>
          </div>
        </article>
      </div>
      <a-empty v-else :description="activeType === 'properties' ? '该设备暂无属性数据' : eventsError ? '事件统计暂不可用' : eventsLoading ? '正在加载事件统计' : '该设备暂无事件数据'" />
    </a-spin>

    <a-modal v-model:visible="desiredVisible" title="设置设备期望值" :mask-closable="false" :ok-loading="submitting" @before-ok="submitDesired" @close="resetDesired">
      <a-alert type="warning" class="realtime-panel__notice">期望值会作为设备控制指令下发，请确认设备当前可安全执行。</a-alert>
      <a-descriptions v-if="desiredField" :column="1" bordered class="realtime-panel__desired-info">
        <a-descriptions-item label="属性">{{ desiredField.name }} ({{ desiredField.id }})</a-descriptions-item>
        <a-descriptions-item label="当前上报">{{ formatFieldValue(desiredField, reported[desiredField.id]) }}</a-descriptions-item>
        <a-descriptions-item label="当前期望">{{ formatFieldValue(desiredField, desired[desiredField.id]) }}</a-descriptions-item>
      </a-descriptions>
      <a-form-item v-if="desiredField" label="新期望值" required>
        <a-switch v-if="fieldType(desiredField) === 'boolean'" v-model="desiredBoolean" />
        <a-select v-else-if="fieldType(desiredField) === 'enum'" v-model="desiredEnum" :options="enumOptions(desiredField)" />
        <a-input-number v-else-if="['int', 'long', 'float', 'double'].includes(fieldType(desiredField))" v-model="desiredNumber" />
        <a-textarea v-else-if="['object', 'array'].includes(fieldType(desiredField))" v-model="desiredText" :auto-size="{ minRows: 5, maxRows: 10 }" placeholder="请输入合法 JSON" />
        <a-input v-else v-model="desiredString" />
      </a-form-item>
    </a-modal>

    <DeviceHistoryModal v-model:visible="historyVisible" :device="device" :field="activeField" :message-type="historyType" />
    <DeviceTrackModal v-model:visible="trackVisible" :device="device" :field="activeField" />
  </section>
</template>

<script setup lang="tsx">
import type { DeviceEventTotalItem, DeviceItem, DeviceShadow } from '@/apis/iot/device'
import type { ProductItem, ProductMetadataItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import Dayjs from 'dayjs'
import { batchGetDeviceShadows, getDeviceEventTotals, sendDeviceCommand } from '@/apis/iot/device'
import { getProductEvents, getProductProperties } from '@/apis/iot/product'
import { useUserStore } from '@/stores'
import DeviceHistoryModal from './DeviceHistoryModal.vue'
import { isTrackField } from './deviceTrack'
import DeviceTrackModal from './DeviceTrackModal.vue'

defineOptions({ name: 'IotDeviceRealtimePanel' })

const props = defineProps<{ device: DeviceItem, product: ProductItem }>()

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const activeType = ref<'properties' | 'events'>('properties')
const refreshSeconds = ref(15)
const shadow = reactive<DeviceShadow>({})
const reported = reactive<Record<string, unknown>>({})
const desired = reactive<Record<string, unknown>>({})
const propertyDefinitions = ref<ProductMetadataItem[]>([])
const eventDefinitions = ref<ProductMetadataItem[]>([])
const eventTotals = ref<DeviceEventTotalItem[]>([])
const eventsLoading = ref(false)
const eventsError = ref('')
let lastEventsAttempt = 0
const EVENTS_REFRESH_INTERVAL_MS = 30_000
const desiredVisible = ref(false)
const desiredField = ref<ProductMetadataItem>()
const desiredBoolean = ref(false)
const desiredNumber = ref<number>()
const desiredEnum = ref<string | number>()
const desiredString = ref('')
const desiredText = ref('')
const historyVisible = ref(false)
const trackVisible = ref(false)
const activeField = ref<ProductMetadataItem>()
const historyType = ref<'PROPERTIES' | 'EVENT'>('PROPERTIES')
let timer: ReturnType<typeof setInterval> | undefined
const UNSAFE_REQUEST_PART_PATTERN = /[^\w.:-]/g
const refreshOptions = [{ label: '手动刷新', value: 0 }, { label: '每 5 秒', value: 5 }, { label: '每 15 秒', value: 15 }, { label: '每 30 秒', value: 30 }]
const properties = computed(() => propertyDefinitions.value
  .map((field) => ({ ...field, reportedValue: reported[field.id], desiredValue: desired[field.id] }))
  .filter((field) => hasDisplayValue(field.reportedValue) || hasDisplayValue(field.desiredValue)))
const events = computed(() => {
  const definitions = new Map(eventDefinitions.value.map((field) => [field.id, field]))
  return eventTotals.value.map((event) => {
    const definition = definitions.get(event.id)
    return {
      ...definition,
      ...event,
      id: event.id,
      name: event.name || definition?.name || event.id,
      description: event.description || definition?.description,
      valueType: event.valueType || definition?.valueType,
      expands: { ...parseObject(definition?.expands), ...parseObject(event.expands) }
    }
  })
})

function parseObject(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function extractBranches(metadata: unknown) {
  const root = parseObject(metadata)
  const state = parseObject(root.state)
  const nextReported = Object.keys(parseObject(state.reported)).length ? parseObject(state.reported) : Object.keys(parseObject(root.reported)).length ? parseObject(root.reported) : root
  const nextDesired = Object.keys(parseObject(state.desired)).length ? parseObject(state.desired) : parseObject(root.desired)
  Object.keys(reported).forEach((key) => delete reported[key])
  Object.keys(desired).forEach((key) => delete desired[key])
  Object.assign(reported, nextReported)
  Object.assign(desired, nextDesired)
}

async function loadData() {
  if (!props.device.iotId || loading.value) return
  loading.value = true
  try {
    const [propertyRes, eventRes, shadowRes] = await Promise.all([
      getProductProperties(props.product.id || props.product.productId || ''),
      getProductEvents(props.product.id || props.product.productId || ''),
      batchGetDeviceShadows([props.device.iotId])
    ])
    propertyDefinitions.value = propertyRes.data || []
    eventDefinitions.value = eventRes.data || []
    const current = shadowRes.data?.[0] || {}
    Object.assign(shadow, current)
    extractBranches(current.metadata)
  } finally {
    loading.value = false
  }
}

async function loadEventTotals(force = false) {
  if (activeType.value !== 'events' || !props.device.iotId || eventsLoading.value) return
  if (!force && Date.now() - lastEventsAttempt < EVENTS_REFRESH_INTERVAL_MS) return
  lastEventsAttempt = Date.now()
  eventsLoading.value = true
  eventsError.value = ''
  try {
    const response = await getDeviceEventTotals(props.device.productKey, props.device.iotId)
    eventTotals.value = response.data?.records || []
  } catch {
    eventsError.value = '事件统计暂不可用，请稍后刷新重试；已有统计仅供参考'
  } finally {
    eventsLoading.value = false
  }
}

function refreshData() {
  void loadData()
  void loadEventTotals(true)
}

function fieldType(field: ProductMetadataItem) {
  const valueType = parseObject(field.valueType)
  return String(valueType.type || field.type || 'string')
}

function enumOptions(field: ProductMetadataItem) {
  const elements = parseObject(field.valueType).elements
  return Array.isArray(elements)
    ? elements.map((item) => {
        const value = parseObject(item)
        return { label: String(value.text || value.label || value.value), value: value.value as string | number }
      })
    : []
}

function formatFieldValue(field: ProductMetadataItem, value: unknown) {
  if (value === undefined || value === null || value === '') return '--'
  if (fieldType(field) === 'boolean') return value === true || value === 1 || value === '1' || value === 'true' ? '是' : '否'
  if (fieldType(field) === 'enum') return enumOptions(field).find((item) => String(item.value) === String(value))?.label || String(value)
  const text = typeof value === 'object' ? JSON.stringify(value) : String(value)
  const unit = parseObject(field.valueType).unit
  return unit ? `${text} ${unit}` : text
}

function hasDisplayValue(value: unknown) {
  return value !== undefined && value !== null && value !== ''
}

function isWritableProperty(field: ProductMetadataItem) {
  return String(field.mode || '').toLowerCase() === 'rw'
}

function eventLevel(field: ProductMetadataItem & { level?: string }) {
  return String(field.level || parseObject(field.expands).level || 'info').toLowerCase()
}

function eventLevelLabel(field: ProductMetadataItem & { level?: string }) {
  const level = eventLevel(field)
  if (['urgent', 'critical', 'fatal'].includes(level)) return '紧急'
  if (['warn', 'warning', 'error'].includes(level)) return '告警'
  if (level === 'info') return '信息'
  return field.level || String(parseObject(field.expands).level || '普通')
}

function eventLevelColor(field: ProductMetadataItem & { level?: string }) {
  const level = eventLevel(field)
  if (['urgent', 'critical', 'fatal'].includes(level)) return 'red'
  if (['warn', 'warning', 'error'].includes(level)) return 'orange'
  return 'arcoblue'
}

function hasDifferentDesired(field: ProductMetadataItem & { reportedValue?: unknown, desiredValue?: unknown }) {
  return field.desiredValue !== undefined && field.desiredValue !== field.reportedValue
}

function openHistory(field: ProductMetadataItem, type: 'PROPERTIES' | 'EVENT') {
  activeField.value = field
  historyType.value = type
  historyVisible.value = true
}

function openTrack(field: ProductMetadataItem) {
  activeField.value = field
  trackVisible.value = true
}

function formatTime(value?: string | number) {
  if (!value) return '尚未同步'
  const text = String(value)
  const date = /^\d{10,13}$/.test(text) ? Dayjs(Number(text) * (text.length === 10 ? 1000 : 1)) : Dayjs(text)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

async function openDesired(field: ProductMetadataItem) {
  desiredField.value = field
  const current = desired[field.id] ?? reported[field.id]
  desiredBoolean.value = current === true || current === 1 || current === '1' || current === 'true'
  desiredNumber.value = current === undefined || current === '' ? undefined : Number(current)
  desiredEnum.value = typeof current === 'string' || typeof current === 'number' ? current : undefined
  desiredString.value = current === undefined || current === null ? '' : String(current)
  desiredText.value = typeof current === 'object' ? JSON.stringify(current, null, 2) : ''
  desiredVisible.value = true
}

function resetDesired() {
  desiredField.value = undefined
  desiredBoolean.value = false
  desiredNumber.value = undefined
  desiredEnum.value = undefined
  desiredString.value = ''
  desiredText.value = ''
}

async function submitDesired() {
  if (!desiredField.value) return false
  const type = fieldType(desiredField.value)
  let value: unknown = type === 'boolean' ? desiredBoolean.value : type === 'enum' ? desiredEnum.value : ['int', 'long', 'float', 'double'].includes(type) ? desiredNumber.value : desiredString.value
  if (['object', 'array'].includes(type)) {
    try {
      value = JSON.parse(desiredText.value)
    } catch {
      Message.error('请输入合法 JSON')
      return false
    }
  }
  submitting.value = true
  try {
    await sendDeviceCommand(props.device.productKey, {
      appUnionId: userStore.name,
      productKey: props.device.productKey,
      deviceId: props.device.deviceId,
      cmd: 'DEV_FUNCTION',
      function: { messageType: 'FUNCTIONS', function: 'setDeviceDesiredProperties', data: { [desiredField.value.id]: value } }
    })
    desired[desiredField.value.id] = value
    Message.success('期望值下发成功')
    return true
  } finally {
    submitting.value = false
  }
}

function resetTimer() {
  if (timer) clearInterval(timer)
  timer = refreshSeconds.value > 0
    ? setInterval(() => {
        void loadData()
        void loadEventTotals()
      }, refreshSeconds.value * 1000)
    : undefined
}

watch(refreshSeconds, resetTimer)
watch(activeType, () => {
  void loadEventTotals()
})
onMounted(() => {
  loadData()
  resetTimer()
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.realtime-panel {
  &__content {
    display: block;
    min-height: 260px;
  }

  &__cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
    margin-top: 16px;
  }

  &__updated {
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__interval {
    width: 120px;
  }

  &__notice,
  &__desired-info {
    margin-bottom: 16px;
  }
}

.status-card {
  display: flex;
  flex-direction: column;
  min-height: 160px;
  padding: 14px;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  background: var(--color-bg-2);
  transition: all 0.2s ease;
}

.status-card:hover {
  border-color: var(--color-border-3);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.property-card--with-desired {
  border-color: rgba(var(--success-6), 0.5);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.card-title {
  flex: 1;
  color: var(--color-text-1);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  word-break: break-word;
}

.card-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.property-key,
.event-key {
  color: var(--color-text-3);
  font-size: 11px;
  font-weight: 400;
}

.card-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 500;
  white-space: nowrap;
}

.badge-property {
  border: 1px solid rgba(var(--primary-6), 0.3);
  background: rgba(var(--primary-6), 0.1);
  color: rgb(var(--primary-6));
}

.badge-event {
  border: 1px solid rgba(var(--warning-6), 0.3);
  background: rgba(var(--warning-6), 0.1);
  color: rgb(var(--warning-6));
}

.badge-warning {
  border: 1px solid rgba(var(--danger-6), 0.3);
  background: rgba(var(--danger-6), 0.1);
  color: rgb(var(--danger-6));
}

.badge-info {
  color: rgb(var(--warning-6));
}

.badge-dot {
  margin-right: 4px;
}

.badge-dot--desired {
  color: rgb(var(--success-6));
}

.card-value {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
  min-height: 60px;
  margin: 0 0 16px;
  color: var(--color-text-1);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
  text-align: center;
  word-break: break-word;
}

.property-value {
  color: rgb(var(--primary-6));
}

.property-value--single {
  justify-content: center;
  text-align: center;
}

.property-value--dual {
  justify-content: center;
  padding: 0;
}

.property-value--empty {
  color: var(--color-text-4);
}

.property-dual-layout {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 60px;
}

.property-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px;
}

.property-column-value {
  color: var(--color-text-1);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
  word-break: break-all;
  text-align: center;
}

.property-column-value--reported {
  color: rgb(var(--primary-6));
}

.property-column-value--desired {
  color: rgb(var(--success-6));
}

.property-column-value--empty {
  color: var(--color-text-4);
  font-size: 14px;
}

.property-divider {
  width: 1px;
  background: var(--color-border-2);
  align-self: stretch;
  margin: 8px 0;
}

.property-main-value {
  max-width: 100%;
  color: rgb(var(--primary-6));
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
  text-align: center;
  word-break: break-all;
}

.property-empty-text {
  color: var(--color-text-4);
  font-size: 13px;
  font-weight: 500;
}

.event-value {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.event-count {
  color: rgb(var(--warning-6));
  font-size: 20px;
  font-weight: 700;
}

.event-description {
  color: var(--color-text-3);
  font-size: 12px;
  line-height: 1.4;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-2);
}

.card-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-3);
  font-size: 11px;
  font-weight: 500;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  background: rgb(var(--primary-6));
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-btn:hover {
  background: rgb(var(--primary-5));
  transform: translateY(-1px);
}

.track-btn {
  background: rgb(var(--success-6));
}

.track-btn:hover {
  background: rgb(var(--success-5));
}

.history-text {
  font-size: 10px;
  line-height: 1;
  font-weight: 600;
}

.icon-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
}

.edit-action:hover {
  background: rgba(var(--success-6), 0.1);
  color: rgb(var(--success-6));
}

:deep(.arco-input-number) {
  width: 100%;
}
</style>
