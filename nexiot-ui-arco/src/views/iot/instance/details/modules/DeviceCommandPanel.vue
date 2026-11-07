<template>
  <section class="command-panel">
    <div v-if="loading" class="command-panel__empty">
      <a-spin tip="正在加载功能与设备影子" />
    </div>
    <div v-else-if="!functions.length" class="command-panel__empty">
      <a-empty description="当前产品未定义可下发功能" />
    </div>

    <div v-else class="command-panel__workspace">
      <aside class="command-panel__functions">
        <header class="command-panel__section-head">
          <strong>功能列表</strong>
          <a-tag color="arcoblue">{{ functions.length }} 项</a-tag>
        </header>
        <div class="command-panel__search">
          <a-input v-model="functionSearch" allow-clear placeholder="搜索名称或标识"><template #prefix><icon-search /></template></a-input>
        </div>
        <nav class="command-panel__function-list" aria-label="设备功能列表">
          <button
            v-for="item in filteredFunctions"
            :key="item.id"
            type="button"
            class="command-panel__function-item"
            :class="{ 'command-panel__function-item--active': selectedId === item.id }"
            @click="selectedId = item.id"
          >
            <strong>{{ item.name || item.id }}</strong>
            <code>{{ item.id }}</code>
          </button>
          <a-empty v-if="!filteredFunctions.length" description="没有匹配的功能" />
        </nav>
      </aside>

      <main v-if="selectedFunction" class="command-panel__main">
        <header class="command-panel__command-head">
          <div>
            <h3>{{ selectedFunction.name || selectedFunction.id }}</h3>
            <code>{{ selectedFunction.id }}</code>
          </div>
          <a-space wrap>
            <a-tag>{{ functionParams.length }} 个参数</a-tag>
            <a-tag v-if="filledCount" color="green">已自动填充 {{ filledCount }} 项</a-tag>
          </a-space>
        </header>

        <div class="command-panel__form-scroll">
          <a-form ref="formRef" :model="form" layout="vertical">
            <div class="command-panel__form-grid">
              <a-form-item v-if="showServiceType" field="serviceType" label="serviceType">
                <a-input v-model="serviceType" placeholder="CTAIOT 服务类型（可选）" />
              </a-form-item>

              <a-form-item
                v-for="field in functionParams"
                :key="field.id"
                :field="field.id"
                :rules="field.required && !availableSources[field.id] ? [{ required: true, message: `请填写${field.name || field.id}` }] : undefined"
                :extra="field.description"
              >
                <template #label>
                  <div class="command-panel__field-label">
                    <span>{{ field.name || field.id }}</span>
                    <code v-if="field.name">{{ field.id }}</code>
                    <a-tag v-if="autoFillSources[field.id]" size="small" :color="autoFillSourceColor(autoFillSources[field.id])">
                      {{ autoFillSourceLabel(autoFillSources[field.id]) }}
                    </a-tag>
                  </div>
                </template>

                <a-select
                  v-if="fieldType(field) === 'boolean'"
                  :model-value="form[field.id]"
                  :options="[{ label: '开启', value: true }, { label: '关闭', value: false }]"
                  allow-clear
                  placeholder="留空由后台补齐"
                  @update:model-value="setValue(field.id, $event)"
                />
                <div v-else-if="fieldType(field) === 'enum'" class="command-panel__enum">
                  <button
                    v-for="option in enumOptions(field)"
                    :key="String(option.value)"
                    type="button"
                    class="command-panel__enum-chip"
                    :class="{ 'command-panel__enum-chip--active': form[field.id] === option.value }"
                    @click="setValue(field.id, form[field.id] === option.value ? undefined : option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
                <a-input-number
                  v-else-if="isNumberField(field)"
                  :model-value="getNumber(field.id)"
                  :min="numberOption(field, 'min')"
                  :max="numberOption(field, 'max')"
                  :step="numberOption(field, 'step') || (['float', 'double'].includes(fieldType(field)) ? 0.1 : 1)"
                  placeholder="请输入数值"
                  @update:model-value="setValue(field.id, $event)"
                >
                  <template v-if="fieldSchema(field).unit" #suffix>{{ fieldSchema(field).unit }}</template>
                </a-input-number>
                <a-date-picker
                  v-else-if="['date', 'datetime'].includes(fieldType(field))"
                  :model-value="getText(field.id)"
                  :show-time="fieldType(field) === 'datetime'"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @update:model-value="setValue(field.id, $event)"
                />
                <a-input-password
                  v-else-if="fieldType(field) === 'password'"
                  :model-value="getText(field.id)"
                  :placeholder="field.placeholder || `请输入${field.name || field.id}`"
                  @update:model-value="setValue(field.id, $event)"
                />
                <a-textarea
                  v-else-if="['object', 'array'].includes(fieldType(field))"
                  :model-value="getText(field.id)"
                  :auto-size="{ minRows: 4, maxRows: 8 }"
                  placeholder="请输入合法 JSON"
                  @update:model-value="setValue(field.id, $event)"
                />
                <a-textarea
                  v-else-if="fieldType(field) === 'text'"
                  :model-value="getText(field.id)"
                  :auto-size="{ minRows: 3, maxRows: 6 }"
                  :placeholder="field.placeholder || `请输入${field.name || field.id}`"
                  @update:model-value="setValue(field.id, $event)"
                />
                <a-input
                  v-else
                  :model-value="getText(field.id)"
                  :placeholder="field.placeholder || `请输入${field.name || field.id}`"
                  @update:model-value="setValue(field.id, $event)"
                >
                  <template v-if="fieldSchema(field).unit" #suffix>{{ fieldSchema(field).unit }}</template>
                </a-input>
              </a-form-item>
            </div>
            <div v-if="!functionParams.length && !showServiceType" class="command-panel__no-params">该功能无需参数，可直接发送。</div>
          </a-form>
        </div>

        <footer class="command-panel__footer">
          <a-space>
            <a-button :loading="syncing" @click="syncRuntimeValues(true)"><template #icon><icon-refresh /></template>重新同步参数</a-button>
            <a-button type="primary" :loading="submitting" @click="send"><template #icon><icon-send /></template>发送指令</a-button>
          </a-space>
        </footer>
      </main>

      <main v-else class="command-panel__main">
        <a-empty description="请选择左侧功能" />
      </main>

      <aside class="command-panel__logs">
        <header class="command-panel__section-head">
          <strong>会话日志</strong>
          <a-button type="text" size="mini" :disabled="!logs.length" @click="logs = []">清空</a-button>
        </header>
        <div class="command-panel__log-list">
          <article v-for="log in logs" :key="log.time" class="command-panel__log-item">
            <button type="button" class="command-panel__log-head" @click="log.collapsed = !log.collapsed">
              <icon-right v-if="log.collapsed" /><icon-down v-else />
              <span>{{ log.name }}</span>
              <time>{{ log.time }}</time>
            </button>
            <div v-show="!log.collapsed" class="command-panel__log-detail">
              <label>请求</label><pre>{{ formatJson(log.request) }}</pre>
              <label>响应</label><pre>{{ formatJson(log.response) }}</pre>
              <a-button v-if="responseFileUrl(log.response)" type="text" size="mini" @click="openResponseFile(log.response)"><template #icon><icon-download /></template>打开响应文件</a-button>
            </div>
          </article>
          <div v-if="!logs.length" class="command-panel__log-empty">本次会话暂无下发记录</div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { AutoFillSource, DeviceFunction, RuntimeField, RuntimeValue } from './deviceCommand'
import type { DeviceItem, DeviceShadow } from '@/apis/iot/device'
import { Message } from '@arco-design/web-vue'
import { getDeviceShadow, sendDeviceCommand } from '@/apis/iot/device'
import { getProductFunctions } from '@/apis/iot/product'
import { useUserStore } from '@/stores'
import {
  autoFillCommandFields,
  autoFillSourceColor,
  autoFillSourceLabel,
  coerceRuntimeValue,
  enumOptions,
  fieldSchema,
  fieldType,
  isNumberField,
  normalizeInputs,
  parseRecord
} from './deviceCommand'

defineOptions({ name: 'IotDeviceCommandPanel' })

const props = defineProps<{ device: DeviceItem, productId: string, source?: string, shadow?: DeviceShadow }>()
const router = useRouter()
const userStore = useUserStore()
const formRef = useTemplateRef<FormInstance>('formRef')
const functions = ref<DeviceFunction[]>([])
const selectedId = ref('')
const functionSearch = ref('')
const form = reactive<Record<string, RuntimeValue>>({})
const autoFillSources = reactive<Record<string, AutoFillSource>>({})
const serviceType = ref('')
const loading = ref(true)
const submitting = ref(false)
const syncing = ref(false)
const localShadow = ref<DeviceShadow>()
const logs = ref<{ name: string, time: string, request: unknown, response: unknown, collapsed: boolean }[]>([])
const selectedFunction = computed(() => functions.value.find((item) => item.id === selectedId.value))
const desiredFunction = computed(() => selectedFunction.value?.id === 'setDeviceDesiredProperties')
const functionParams = computed(() => normalizeInputs(selectedFunction.value?.inputs, !desiredFunction.value))
const filteredFunctions = computed(() => {
  const keyword = functionSearch.value.trim().toLowerCase()
  return keyword ? functions.value.filter((item) => `${item.name || ''} ${item.id}`.toLowerCase().includes(keyword)) : functions.value
})
const filledCount = computed(() => Object.keys(autoFillSources).length)
const availableSources = computed(() => autoFillCommandFields(functionParams.value, props.device, localShadow.value, !desiredFunction.value).sources)
const showServiceType = computed(() => [props.source, selectedFunction.value?.source].some((value) => String(value || '').toLowerCase() === 'ctaiot'))

function clearReactive(record: Record<string, unknown>) {
  Object.keys(record).forEach((key) => delete record[key])
}

function applyRuntimeValues() {
  clearReactive(form)
  clearReactive(autoFillSources)
  const result = autoFillCommandFields(functionParams.value, props.device, localShadow.value, !desiredFunction.value)
  Object.assign(form, result.values)
  Object.assign(autoFillSources, result.sources)
  serviceType.value = ''
  nextTick(() => formRef.value?.clearValidate())
}

async function syncRuntimeValues(refreshShadow = false) {
  syncing.value = true
  try {
    if (refreshShadow && props.device.iotId) localShadow.value = (await getDeviceShadow(props.device.iotId)).data || {}
    applyRuntimeValues()
    if (refreshShadow) Message.success(`已同步 ${filledCount.value} 个参数`)
  } finally {
    syncing.value = false
  }
}

function getNumber(id: string) {
  const value = form[id]
  return value === undefined || value === '' ? undefined : Number(value)
}

function getText(id: string) {
  return form[id] === undefined ? '' : String(form[id])
}

function setValue(id: string, value: unknown) {
  form[id] = typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || value === undefined ? value : String(value)
  delete autoFillSources[id]
}

function numberOption(field: RuntimeField, key: 'min' | 'max' | 'step') {
  const value = fieldSchema(field)[key]
  return value === undefined || value === null || value === '' ? undefined : Number(value)
}

function serializeFieldValue(field: RuntimeField) {
  const value = form[field.id]
  if (value === undefined || value === '') return undefined
  if (['object', 'array'].includes(fieldType(field))) {
    try {
      return JSON.parse(String(value))
    } catch {
      throw new Error(`${field.name || field.id} 不是合法 JSON`)
    }
  }
  if (fieldType(field) === 'enum') return value
  return coerceRuntimeValue(field, value)
}

async function send() {
  if (!selectedFunction.value || await formRef.value?.validate()) return
  const data: Record<string, unknown> = {}
  try {
    for (const field of functionParams.value) {
      const value = serializeFieldValue(field)
      if (value !== undefined) data[field.id] = value
    }
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '参数格式错误')
    return
  }
  const command = {
    messageType: 'FUNCTIONS',
    function: selectedFunction.value.id,
    data,
    source: selectedFunction.value.source,
    serviceType: serviceType.value || undefined
  }
  const payload = { appUnionId: userStore.name, productKey: props.device.productKey, deviceId: props.device.deviceId, cmd: 'DEV_FUNCTION', function: command }
  submitting.value = true
  try {
    const response = await sendDeviceCommand(props.device.productKey, payload)
    addLog(selectedFunction.value.name || selectedFunction.value.id, command, response.data, true)
    Message.success('指令已发送')
  } catch (error) {
    addLog(selectedFunction.value.name || selectedFunction.value.id, command, { error: error instanceof Error ? error.message : '下发失败' }, false)
  } finally {
    submitting.value = false
  }
}

function addLog(name: string, request: unknown, response: unknown, success: boolean) {
  logs.value.forEach((item) => item.collapsed = true)
  logs.value.unshift({ name: `${success ? '成功' : '失败'} · ${name}`, time: new Date().toLocaleTimeString(), request, response, collapsed: false })
}

function formatJson(value: unknown) {
  try {
    return JSON.stringify(value ?? {}, null, 2)
  } catch {
    return String(value ?? '')
  }
}

function responseFileUrl(value: unknown) {
  const data = parseRecord(value)
  return String(data.fileUrl || data.url || '')
}

function openResponseFile(value: unknown) {
  const url = responseFileUrl(value)
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

watch(selectedFunction, applyRuntimeValues)
watch(() => props.shadow, (value) => localShadow.value = value, { deep: true, immediate: true })

async function initialize() {
  loading.value = true
  try {
    const shadowPromise = props.device.iotId ? getDeviceShadow(props.device.iotId) : Promise.resolve({ data: props.shadow || {} })
    const [functionRes, shadowRes] = await Promise.all([getProductFunctions(props.productId), shadowPromise])
    functions.value = (functionRes.data || []) as DeviceFunction[]
    localShadow.value = shadowRes.data || props.shadow
    if (functions.value.length) selectedId.value = functions.value[0].id
  } finally {
    loading.value = false
  }
}

initialize()
</script>

<style lang="scss" scoped src="./DeviceCommandPanel.scss"></style>

<style lang="scss" scoped>
:deep(.arco-input-number),
:deep(.arco-picker) {
  width: 100%;
}

:deep(.command-panel__form-grid .arco-form-item) {
  margin-bottom: 8px;
}

:deep(.command-panel__form-grid .arco-form-item-label-col) {
  margin-bottom: 4px;
}

:deep(.command-panel__form-grid .arco-form-item-label) {
  display: flex;
  gap: 4px;
  align-items: center;
}

.command-panel__enum {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.command-panel__enum-chip {
  padding: 4px 14px;
  font-size: 13px;
  line-height: 22px;
  color: var(--color-text-2);
  white-space: nowrap;
  cursor: pointer;
  background: var(--color-fill-2);
  border: 1px solid transparent;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s, border-color 0.15s;

  &:hover {
    color: rgb(var(--primary-6));
    background: var(--color-fill-3);
  }

  &--active {
    color: rgb(var(--primary-6));
    background: var(--color-primary-light-1);
    border-color: rgb(var(--primary-6));
  }
}
</style>
