<template>
  <section class="polling-panel">
    <header class="polling-panel__header">
      <div>
        <a-space align="center"><h2>{{ taskTitle }}</h2><a-tag :color="form.enabled ? 'green' : 'gray'">{{ form.enabled ? '已启用' : '未启用' }}</a-tag></a-space>
        <p>{{ device.deviceName || device.deviceId }} / {{ device.deviceId }}</p>
      </div>
    </header>

    <a-alert type="info" class="polling-panel__notice">轮询由服务端调度运行，指令按列表顺序执行。</a-alert>

    <section class="polling-panel__settings">
      <div class="polling-panel__section-title">
        <div><h3>任务参数</h3><span>调度频率、超时和重试策略</span></div>
      </div>
      <a-form :model="form" layout="vertical">
        <a-grid :cols="{ xs: 1, sm: 2, lg: 5 }" :col-gap="14" :row-gap="4">
          <a-form-item label="启用任务"><a-switch v-model="form.enabled" /></a-form-item>
          <a-form-item label="轮询周期"><a-select v-model="form.intervalSeconds" :options="intervalOptions" /></a-form-item>
          <a-form-item label="超时时间 (秒)"><a-input-number v-model="form.timeoutSeconds" :min="5" :max="60" /></a-form-item>
          <a-form-item label="重试次数"><a-input-number v-model="form.retryTimes" :min="0" :max="5" /></a-form-item>
          <a-form-item label="指令间隔 (毫秒)"><a-input-number v-model="form.commandIntervalMs" :min="0" :max="60000" /></a-form-item>
        </a-grid>
      </a-form>
    </section>

    <section class="polling-panel__commands">
      <div class="polling-panel__section-title">
        <div><h3>{{ commandSectionTitle }}</h3><span>{{ commands.length }} 条指令，保存时自动转换为后端配置</span></div>
        <a-button type="primary" @click="addCommand"><template #icon><icon-plus /></template>{{ addButtonText }}</a-button>
      </div>
      <a-empty v-if="!commands.length" description="暂无轮询指令，请先添加一条指令" />
      <a-table v-else row-key="rowKey" :data="commands" :columns="commandColumns" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 900 }">
        <template #order="{ rowIndex }">
          <a-space :size="2">
            <a-button type="text" shape="circle" size="small" :disabled="rowIndex === 0" aria-label="上移指令" @click="moveCommand(rowIndex, -1)"><icon-up /></a-button>
            <a-button type="text" shape="circle" size="small" :disabled="rowIndex === commands.length - 1" aria-label="下移指令" @click="moveCommand(rowIndex, 1)"><icon-down /></a-button>
            <span>{{ rowIndex + 1 }}</span>
          </a-space>
        </template>
        <template #commandName="{ record }"><strong>{{ record.commandName || '未命名指令' }}</strong></template>
        <template #target="{ record }"><code>{{ record.slaveDeviceId || '-' }}</code></template>
        <template #content="{ record }"><code class="polling-panel__content-cell" :title="String(record.commandHex || '')">{{ record.commandHex || '-' }}</code></template>
        <template #status="{ record }"><a-tag :color="record.enabled ? 'green' : 'gray'">{{ record.enabled ? '启用' : '禁用' }}</a-tag></template>
        <template #action="{ record, rowIndex }">
          <a-space :size="4">
            <a-button type="text" size="small" @click="editCommand(record, rowIndex)"><template #icon><icon-edit /></template>编辑</a-button>
            <a-popconfirm content="确定删除这条指令吗？" @before-ok="removeCommand(rowIndex)"><a-button type="text" status="danger" size="small"><template #icon><icon-delete /></template>删除</a-button></a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </section>

    <footer class="polling-panel__actions">
      <a-space wrap>
        <a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存配置</a-button>
        <a-button :loading="testing" :disabled="!canTest" @click="test"><template #icon><icon-play-arrow /></template>立即测试</a-button>
        <a-popconfirm content="确定删除当前轮询配置吗？" @before-ok="removeConfig"><a-button status="danger" :disabled="!form.id"><template #icon><icon-delete /></template>删除配置</a-button></a-popconfirm>
      </a-space>
      <span v-if="!form.id">当前任务尚未保存</span>
    </footer>

    <a-collapse v-if="testResult" class="polling-panel__result">
      <a-collapse-item key="result" header="最近测试结果"><pre>{{ testResult }}</pre></a-collapse-item>
    </a-collapse>

    <GatewayPollingCommandModal
      v-model:visible="commandModalVisible"
      :task-type="taskType"
      :command="editingCommand"
      @save="handleCommandSave"
    />
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem, GatewayPollingConfig } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { deleteGatewayPollingConfig, getGatewayPollingConfig, saveGatewayPollingConfig, testGatewayPolling } from '@/apis/iot/device'
import GatewayPollingCommandModal from './GatewayPollingCommandModal.vue'

defineOptions({ name: 'IotGatewayPollingPanel' })

const props = defineProps<{ device: DeviceItem, product: ProductItem }>()

type CommandPayload = {
  commandName: string
  commandHex: string
  taskType: GatewayPollingConfig['taskType']
  commandType: string
  slaveDeviceId: string
  propertyMapping: string
  protocolParams: string
  dataParserScript: string
  enabled: boolean
  executionOrder?: number
}

type PollingCommand = CommandPayload & Record<string, unknown> & {
  rowKey: string
}
const taskType = ref<GatewayPollingConfig['taskType']>('CLOUD_POLLING')
const form = reactive(createForm('CLOUD_POLLING'))
const commands = ref<PollingCommand[]>([])
const commandModalVisible = ref(false)
const editingCommand = ref<Record<string, unknown>>()
const editingIndex = ref(-1)
const saving = ref(false)
const testing = ref(false)
const testResult = ref('')
const intervalOptions = [
  { label: '30 秒 / 高频', value: 30 },
  { label: '60 秒', value: 60 },
  { label: '120 秒 / 推荐', value: 120 },
  { label: '300 秒', value: 300 },
  { label: '600 秒 / 低频', value: 600 }
]
const taskTitle = '云端轮询任务'
const commandSectionTitle = '透传指令'
const addButtonText = '添加透传指令'
const canTest = computed(() => Boolean(form.id && form.enabled && commands.value.length))
const commandColumns = computed<TableColumnData[]>(() => [
  { title: '顺序', width: 112, align: 'center', slotName: 'order' },
  { title: '指令名称', width: 190, slotName: 'commandName' },
  { title: '子设备 ID', width: 180, slotName: 'target' },
  { title: '透传内容', dataIndex: 'commandHex', slotName: 'content', ellipsis: true, tooltip: true },
  { title: '状态', width: 90, align: 'center', slotName: 'status' },
  { title: '操作', width: 170, align: 'center', fixed: 'right', slotName: 'action' }
])

function createForm(type: GatewayPollingConfig['taskType']): GatewayPollingConfig {
  return { productKey: props.device.productKey, deviceId: props.device.deviceId, taskType: type, enabled: false, intervalSeconds: 60, timeoutSeconds: 10, retryTimes: 1, commandIntervalMs: 500, commands: [] }
}

function normalizeCommand(source: Record<string, unknown>, index: number): PollingCommand {
  return {
    ...source,
    rowKey: `${String(source.id || 'command')}_${index}_${String(source.executionOrder ?? index)}`,
    commandName: String(source.commandName || ''),
    commandHex: String(source.commandHex || ''),
    taskType: 'CLOUD_POLLING',
    commandType: String(source.commandType || ''),
    slaveDeviceId: String(source.slaveDeviceId || ''),
    propertyMapping: String(source.propertyMapping || '[]'),
    protocolParams: String(source.protocolParams || '{}'),
    dataParserScript: String(source.dataParserScript || ''),
    enabled: source.enabled !== false && source.enabled !== 0 && source.enabled !== '0',
    executionOrder: index
  }
}

async function load() {
  Object.assign(form, createForm(taskType.value))
  commands.value = []
  testResult.value = ''
  try {
    const res = await getGatewayPollingConfig(props.device.productKey, props.device.deviceId, taskType.value)
    if (res.data) {
      Object.assign(form, res.data, { taskType: taskType.value })
      commands.value = (res.data.commands || []).map((item, index) => normalizeCommand(item, index))
    }
  } catch { /* Empty configuration is a valid state. */ }
}

function addCommand() {
  editingIndex.value = -1
  editingCommand.value = undefined
  commandModalVisible.value = true
}

function editCommand(record: PollingCommand, index: number) {
  editingIndex.value = index
  editingCommand.value = { ...record }
  commandModalVisible.value = true
}

function handleCommandSave(command: CommandPayload) {
  const next = normalizeCommand({ ...command }, editingIndex.value >= 0 ? editingIndex.value : commands.value.length)
  if (editingIndex.value >= 0) commands.value.splice(editingIndex.value, 1, next)
  else commands.value.push(next)
  commands.value.forEach((item, index) => {
    item.executionOrder = index
  })
  commandModalVisible.value = false
}

function removeCommand(index: number) {
  commands.value.splice(index, 1)
  commands.value.forEach((item, order) => {
    item.executionOrder = order
  })
  return true
}

function moveCommand(index: number, direction: -1 | 1) {
  const target = index + direction
  if (target < 0 || target >= commands.value.length) return
  const current = commands.value[index]
  commands.value[index] = commands.value[target]
  commands.value[target] = current
  commands.value.forEach((item, order) => {
    item.executionOrder = order
  })
}

async function save() {
  saving.value = true
  try {
    await saveGatewayPollingConfig({ ...form, taskType: taskType.value, commands: commands.value.map(({ rowKey, ...command }) => command) })
    Message.success('轮询配置保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

async function test() {
  testing.value = true
  try {
    const res = await testGatewayPolling(props.device.productKey, props.device.deviceId, taskType.value)
    testResult.value = JSON.stringify(res.data || {}, null, 2)
    Message.success('测试完成')
  } finally {
    testing.value = false
  }
}

async function removeConfig() {
  await deleteGatewayPollingConfig(props.device.productKey, props.device.deviceId, taskType.value)
  Message.success('轮询配置已删除')
  await load()
  return true
}

watch([() => props.device.productKey, () => props.device.deviceId], () => {
  load()
})
load()
</script>

<style lang="scss" scoped>
.polling-panel {
  width: 100%;
  min-width: 0;

  &__header {
    display: flex;
    gap: 20px;
    align-items: flex-start;
    justify-content: space-between;
    padding-bottom: 14px;

    h2,
    p {
      margin: 0;
      letter-spacing: 0;
    }

    h2 {
      font-size: 18px;
    }

    p {
      margin-top: 5px;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__modes {
    min-width: 0;
  }

  &__modes :deep(.arco-tabs-content) {
    display: none;
  }

  &__notice {
    margin-bottom: 14px;
  }

  &__settings,
  &__commands {
    padding: 16px;
    margin-bottom: 14px;
    background: var(--color-bg-2);
    border: 1px solid var(--color-border-2);
    border-radius: 6px;
  }

  &__settings :deep(.arco-form-item) {
    margin-bottom: 0;
  }

  &__section-title {
    display: flex;
    gap: 14px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    h3,
    span {
      margin: 0;
    }

    h3 {
      font-size: 15px;
      letter-spacing: 0;
    }

    span {
      display: block;
      margin-top: 3px;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__content-cell {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__actions {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    background: var(--color-fill-1);
    border-top: 1px solid var(--color-border-2);

    > span {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__result {
    margin-top: 14px;

    pre {
      overflow: auto;
      font-size: 12px;
      white-space: pre-wrap;
    }
  }
}

@media (width <= 760px) {
  .polling-panel {
    &__header,
    &__actions,
    &__section-title {
      flex-direction: column;
      align-items: flex-start;
    }
  }
}
</style>
