<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :width="modalWidth"
    :modal-style="{ maxWidth: 'calc(100vw - 24px)' }"
    :body-style="{ maxHeight: '76vh', overflowY: 'auto' }"
    :ok-text="isDefinitionMode ? (definitionId ? '更新作业' : '保存作业') : '立即创建'"
    :ok-loading="submitting"
    :mask-closable="false"
    unmount-on-close
    @before-ok="submit"
    @close="reset"
  >
    <a-spin :loading="loading" class="job-modal-loading">
      <a-steps :current="currentStep" small class="job-steps">
        <a-step title="选择产品" />
        <a-step title="圈定设备" />
        <a-step title="配置指令" />
        <a-step v-if="isDefinitionMode" title="调度设置" />
      </a-steps>

      <section class="job-section">
        <header><span>1</span><div><strong>选择产品</strong><small>设备范围和物模型功能均由产品决定</small></div></header>
        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="产品" required>
              <a-select v-model="form.productKey" :options="productOptions" allow-search placeholder="请选择产品" :disabled="Boolean(definitionId)" @change="handleProductChange" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="任务名称">
              <a-input v-model.trim="form.taskName" allow-clear placeholder="不填则由系统自动命名" :max-length="128" show-word-limit />
            </a-form-item>
          </a-col>
        </a-row>
        <div v-if="product" class="product-context">
          <div><span>产品名称</span><strong>{{ product.name || '-' }}</strong></div>
          <div><span>ProductKey</span><code>{{ product.productKey }}</code></div>
          <div><span>设备类型</span><a-tag color="arcoblue">{{ deviceNodeLabel(product.deviceNode) }}</a-tag></div>
          <div><span>可用功能</span><strong>{{ functions.length }}</strong></div>
        </div>
      </section>

      <section class="job-section">
        <header><span>2</span><div><strong>圈定设备范围</strong><small>可精确勾选，或让任务按筛选条件覆盖全部匹配设备</small></div></header>
        <DeviceTargetPicker
          v-model:application="form.application"
          v-model:group-id="form.devGroupId"
          v-model:keyword="form.keyword"
          v-model:scope-type="form.scopeType"
          v-model:selected-ids="form.selectedIds"
          v-model:matched-count="matchedCount"
          :product-key="form.productKey"
        />
      </section>

      <section class="job-section">
        <header><span>3</span><div><strong>配置执行指令</strong><small>支持多条设备功能按顺序或并行执行</small></div></header>
        <a-alert v-if="form.productKey && !functions.length" type="warning">该产品暂无功能物模型，无法创建设备作业。</a-alert>
        <DeviceCommandEditor ref="commandEditorRef" v-model:commands="commands" v-model:execution-mode="form.commandExecutionMode" :functions="functions" :product-source="product?.thirdPlatform" />
      </section>

      <section v-if="isDefinitionMode" class="job-section">
        <header><span>4</span><div><strong>调度设置</strong><small>作业定义可按需手动执行，也可由 Quartz 定时触发</small></div></header>
        <div class="schedule-grid">
          <a-form-item label="调度方式">
            <a-radio-group v-model="form.scheduleType" type="button">
              <a-radio value="MANUAL">手动触发</a-radio>
              <a-radio value="CRON">定时调度</a-radio>
            </a-radio-group>
          </a-form-item>
          <template v-if="form.scheduleType === 'CRON'">
            <a-form-item label="调度状态">
              <a-radio-group v-model="form.scheduleStatus" type="button">
                <a-radio value="ENABLED">启用</a-radio>
                <a-radio value="PAUSED">暂停</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="错过执行策略">
              <a-select v-model="form.misfirePolicy" :options="misfireOptions" />
            </a-form-item>
            <a-form-item label="Cron 表达式" required class="cron-field">
              <a-input v-model.trim="form.cronExpression" placeholder="例如 0 0/5 * * * ?">
                <template #prepend>
                  <a-select v-model="cronPreset" :options="cronPresets" placeholder="常用周期" style="width: 140px" @change="applyCronPreset" />
                </template>
              </a-input>
            </a-form-item>
          </template>
        </div>
      </section>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { DeviceJobPayload, ScheduleStatus, ScheduleType, TaskDefinition } from '../api'
import type { ProductItem, ProductMetadataItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { getProductByKey, getProductList } from '@/apis/iot/product'
import { createDeviceJobDefinition, createDeviceJobTask, getTaskDefinition, updateDeviceJobDefinition } from '../api'
import { normalizeFunctions, parseRecord } from '../shared'
import DeviceCommandEditor from './DeviceCommandEditor.vue'
import DeviceTargetPicker from './DeviceTargetPicker.vue'

defineOptions({ name: 'TaskCenterDeviceJobModal' })
const emit = defineEmits<{ success: [result?: unknown] }>()
const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const definitionId = ref('')
const mode = ref<'DEFINE' | 'EXECUTE'>('DEFINE')
const products = ref<ProductItem[]>([])
const product = ref<ProductItem>()
const functions = ref<ProductMetadataItem[]>([])
const commands = ref<any[]>([])
const matchedCount = ref(0)
const cronPreset = ref('')
const commandEditorRef = useTemplateRef<InstanceType<typeof DeviceCommandEditor>>('commandEditorRef')
const { width } = useWindowSize()
const modalWidth = computed(() => Math.min(1120, Math.max(300, width.value - 24)))
const isDefinitionMode = computed(() => mode.value === 'DEFINE')
const title = computed(() => isDefinitionMode.value ? definitionId.value ? '编辑设备作业' : '新建设备作业' : '创建设备作业')
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const form = reactive({
  taskName: '',
  productKey: '',
  application: '',
  devGroupId: '',
  keyword: '',
  scopeType: 'SELECTED' as 'SELECTED' | 'FILTERED',
  selectedIds: [] as string[],
  commandExecutionMode: 'SERIAL' as 'SERIAL' | 'PARALLEL',
  scheduleType: 'MANUAL' as ScheduleType,
  scheduleStatus: 'ENABLED' as ScheduleStatus,
  cronExpression: '',
  misfirePolicy: '2'
})
const targetReady = computed(() => form.scopeType === 'FILTERED' ? matchedCount.value > 0 : form.selectedIds.length > 0)
const currentStep = computed(() => !form.productKey ? 1 : !targetReady.value ? 2 : !commands.value.every((item) => item.functionId) ? 3 : isDefinitionMode.value ? 4 : 3)
const misfireOptions = [
  { label: '默认策略', value: '0' },
  { label: '立即补偿执行', value: '1' },
  { label: '补偿触发一次', value: '2' },
  { label: '跳过本次补偿', value: '3' }
]
const cronPresets = [
  { label: '每 5 分钟', value: '0 0/5 * * * ?' },
  { label: '每 10 分钟', value: '0 0/10 * * * ?' },
  { label: '每 30 分钟', value: '0 0/30 * * * ?' },
  { label: '每小时', value: '0 0 0/1 * * ?' },
  { label: '每天 00:00', value: '0 0 0 * * ?' },
  { label: '每天 06:00', value: '0 0 6 * * ?' },
  { label: '每周一 09:00', value: '0 0 9 ? * MON' },
  { label: '每月 1 日', value: '0 0 0 1 * ?' }
]

async function loadProducts() {
  if (products.value.length) return
  const response = await getProductList({ page: 1, size: 1000 })
  products.value = response.data.records || []
}

async function add(options: { mode?: 'DEFINE' | 'EXECUTE', productKey?: string, selectedIds?: string[] } = {}) {
  reset()
  mode.value = options.mode || 'DEFINE'
  loading.value = true
  try {
    await loadProducts()
    if (options.productKey) {
      form.productKey = options.productKey
      form.selectedIds = options.selectedIds || []
      await loadProduct(options.productKey)
    }
    visible.value = true
  } finally {
    loading.value = false
  }
}

async function edit(id: string) {
  reset()
  mode.value = 'DEFINE'
  definitionId.value = id
  loading.value = true
  try {
    const [definitionRes] = await Promise.all([getTaskDefinition(id), loadProducts()])
    visible.value = true
    await nextTick()
    await applyDefinition(definitionRes.data)
    visible.value = true
  } finally {
    loading.value = false
  }
}

async function applyDefinition(definition: TaskDefinition) {
  const snapshot = parseRecord(definition.requestSnapshot)
  Object.assign(form, {
    taskName: definition.taskName || '',
    productKey: String(snapshot.productKey || definition.productKey || ''),
    application: String(snapshot.application || ''),
    devGroupId: snapshot.devGroupId ? String(snapshot.devGroupId) : '',
    keyword: String(snapshot.keyword || ''),
    scopeType: snapshot.chooseAll ? 'FILTERED' : 'SELECTED',
    selectedIds: Array.isArray(snapshot.ids) ? snapshot.ids.map(String) : [],
    commandExecutionMode: snapshot.commandExecutionMode === 'PARALLEL' ? 'PARALLEL' : 'SERIAL',
    scheduleType: definition.scheduleType === 'CRON' ? 'CRON' : 'MANUAL',
    scheduleStatus: definition.scheduleStatus === 'PAUSED' ? 'PAUSED' : 'ENABLED',
    cronExpression: definition.cronExpression || '',
    misfirePolicy: definition.misfirePolicy || '2'
  })
  await loadProduct(form.productKey)
  const snapshots = Array.isArray(snapshot.commands) && snapshot.commands.length
    ? snapshot.commands
    : snapshot.functionId ? [{ commandId: 'CMD_01', functionId: snapshot.functionId, source: snapshot.source, serviceType: snapshot.serviceType, functionArgsSpec: snapshot.functionArgsSpec, functionData: snapshot.functionData }] : []
  await nextTick()
  commandEditorRef.value?.applySnapshot(snapshots)
}

async function handleProductChange(value: string | number | boolean | Record<string, any>) {
  form.productKey = String(value || '')
  Object.assign(form, { application: '', devGroupId: '', keyword: '', scopeType: 'SELECTED', selectedIds: [] })
  matchedCount.value = 0
  commands.value = []
  await loadProduct(form.productKey)
}

async function loadProduct(productKey: string) {
  if (!productKey) {
    product.value = undefined
    functions.value = []
    return
  }
  const response = await getProductByKey(productKey)
  product.value = response.data
  functions.value = normalizeFunctions(response.data?.metadata)
}

function applyCronPreset(value: string | number | boolean | Record<string, any>) {
  form.cronExpression = String(value || '')
}

async function submit() {
  if (!form.productKey) {
    Message.warning('请选择产品')
    return false
  }
  if (!targetReady.value) {
    Message.warning(form.scopeType === 'FILTERED' ? '当前筛选条件没有匹配设备' : '请至少勾选一台设备')
    return false
  }
  if (isDefinitionMode.value && form.scheduleType === 'CRON' && !form.cronExpression.trim()) {
    Message.warning('请输入 Cron 表达式')
    return false
  }
  const commandPayload = commandEditorRef.value?.validateAndBuild()
  if (!commandPayload?.length) return false
  const first = commandPayload[0]
  const payload: DeviceJobPayload = {
    taskName: form.taskName.trim() || undefined,
    productKey: form.productKey,
    application: form.application || undefined,
    devGroupId: form.devGroupId || undefined,
    keyword: form.keyword.trim() || undefined,
    chooseAll: form.scopeType === 'FILTERED',
    ids: form.scopeType === 'SELECTED' ? form.selectedIds : [],
    commandExecutionMode: form.commandExecutionMode,
    commands: commandPayload,
    functionId: first.functionId,
    source: first.source,
    serviceType: first.serviceType,
    functionArgsSpec: first.functionArgsSpec,
    functionData: first.functionData
  }
  if (isDefinitionMode.value) {
    payload.scheduleType = form.scheduleType
    payload.scheduleStatus = form.scheduleType === 'CRON' ? form.scheduleStatus : 'PAUSED'
    payload.cronExpression = form.scheduleType === 'CRON' ? form.cronExpression.trim() : undefined
    payload.misfirePolicy = form.scheduleType === 'CRON' ? form.misfirePolicy : undefined
  }
  submitting.value = true
  try {
    const response = isDefinitionMode.value
      ? definitionId.value ? await updateDeviceJobDefinition(definitionId.value, payload) : await createDeviceJobDefinition(payload)
      : await createDeviceJobTask(payload)
    Message.success(response.message || (isDefinitionMode.value ? '设备作业已保存' : '设备作业已创建'))
    emit('success', response.data)
    return true
  } finally {
    submitting.value = false
  }
}

function deviceNodeLabel(value?: string) {
  return ({ DEVICE: '直连设备', GATEWAY: '网关设备', GATEWAY_SUB_DEVICE: '网关子设备', VIDEO_DEVICE: '视频设备' } as Record<string, string>)[value || ''] || value || '-'
}

function reset() {
  definitionId.value = ''
  product.value = undefined
  functions.value = []
  commands.value = []
  matchedCount.value = 0
  cronPreset.value = ''
  Object.assign(form, { taskName: '', productKey: '', application: '', devGroupId: '', keyword: '', scopeType: 'SELECTED', selectedIds: [], commandExecutionMode: 'SERIAL', scheduleType: 'MANUAL', scheduleStatus: 'ENABLED', cronExpression: '', misfirePolicy: '2' })
}

defineExpose({ add, edit })
</script>

<style scoped lang="scss" src="./DeviceJobModal.scss"></style>
