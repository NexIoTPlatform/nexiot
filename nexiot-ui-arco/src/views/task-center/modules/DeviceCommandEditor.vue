<template>
  <div class="command-editor">
    <div class="command-toolbar">
      <div>
        <strong>指令序列</strong>
        <a-radio-group v-model="executionMode" type="button" size="small">
          <a-radio value="SERIAL">串行执行</a-radio>
          <a-radio value="PARALLEL">并行执行</a-radio>
        </a-radio-group>
      </div>
      <a-button type="primary" size="small" @click="addCommand"><template #icon><icon-plus /></template>添加指令</a-button>
    </div>

    <div class="command-tabs">
      <button v-for="(command, index) in commands" :key="command.commandId" type="button" :class="{ active: command.commandId === activeId }" @click="activeId = command.commandId">
        <span>指令 {{ index + 1 }}</span>
        <small>{{ functionLabel(command.functionId) }}</small>
        <icon-close v-if="commands.length > 1" class="remove-command" @click.stop="removeCommand(command.commandId)" />
      </button>
    </div>

    <div v-if="activeCommand" class="command-workspace">
      <aside>
        <a-input v-model.trim="functionKeyword" allow-clear placeholder="搜索功能" size="small"><template #prefix><icon-search /></template></a-input>
        <a-scrollbar class="function-list">
          <button v-for="item in filteredFunctions" :key="item.id" type="button" :class="{ active: activeCommand.functionId === item.id }" @click="selectFunction(item)">
            <span>{{ item.name || item.id }}</span><code>{{ item.id }}</code>
          </button>
          <a-empty v-if="!filteredFunctions.length" description="当前产品暂无可用功能" />
        </a-scrollbar>
      </aside>

      <main>
        <template v-if="activeFunction">
          <header>
            <div><strong>{{ activeFunction.name || activeFunction.id }}</strong><code>{{ activeFunction.id }}</code></div>
            <a-input v-if="showServiceType" v-model.trim="activeCommand.serviceType" size="small" placeholder="serviceType" />
          </header>
          <a-alert type="info">参数支持固定值、设备运行时自动取值和变量模板。自动取值会按参数名从设备标识、配置与影子中解析。</a-alert>
          <div v-if="activeCommand.fields.length" class="param-list">
            <section v-for="field in activeCommand.fields" :key="field.id" class="param-row">
              <div class="param-label">
                <strong>{{ field.name || field.id }}<em v-if="field.required">*</em></strong>
                <code>{{ field.id }}</code>
                <span v-if="field.description">{{ field.description }}</span>
              </div>
              <div class="param-editor">
                <a-radio-group :model-value="activeCommand.specs[field.id]?.mode || 'FIXED'" type="button" size="mini" @change="(value) => changeMode(field, String(value) as ParamMode)">
                  <a-radio value="FIXED">固定值</a-radio>
                  <a-radio value="AUTO" :disabled="isComplex(field)">自动取值</a-radio>
                  <a-radio value="TEMPLATE" :disabled="isComplex(field)">变量模板</a-radio>
                </a-radio-group>
                <MetadataParamInput v-if="modeOf(field) === 'FIXED'" :model-value="activeCommand.specs[field.id]?.value" :field="field" @update:model-value="(value) => setFixedValue(field.id, value)" />
                <a-input v-else-if="modeOf(field) === 'TEMPLATE'" :model-value="activeCommand.specs[field.id]?.template" placeholder="例如 {{deviceId}} 或 {{configuration.meterNo}}" @update:model-value="(value) => setTemplate(field.id, String(value))" />
                <a-typography-text v-else type="secondary">执行时按参数标识自动匹配每台设备的数据</a-typography-text>
              </div>
            </section>
          </div>
          <a-empty v-else description="该功能无需输入参数" />
        </template>
        <a-empty v-else description="请从左侧选择要执行的功能" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DeviceJobCommand, ParamMode } from '../api'
import type { ProductMetadataItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { normalizeFunctionInputs, parseRecord } from '../shared'
import MetadataParamInput from './MetadataParamInput.vue'

interface CommandDraft {
  commandId: string
  functionId: string
  source?: string
  serviceType?: string
  fields: (ProductMetadataItem & { required?: boolean })[]
  specs: Record<string, { mode: ParamMode, value?: unknown, template?: string }>
}

const commands = defineModel<CommandDraft[]>('commands', { default: () => [] })
const executionMode = defineModel<'SERIAL' | 'PARALLEL'>('executionMode', { default: 'SERIAL' })
const props = defineProps<{ functions: ProductMetadataItem[], productSource?: string }>()
const activeId = ref('')
const functionKeyword = ref('')
let seed = 0
const activeCommand = computed(() => commands.value.find((item) => item.commandId === activeId.value) || commands.value[0])
const activeFunction = computed(() => props.functions.find((item) => item.id === activeCommand.value?.functionId))
const filteredFunctions = computed(() => {
  const keyword = functionKeyword.value.toLowerCase()
  return keyword ? props.functions.filter((item) => `${item.name || ''} ${item.id}`.toLowerCase().includes(keyword)) : props.functions
})
const showServiceType = computed(() => [activeCommand.value?.source, activeFunction.value?.source, props.productSource].some((value) => String(value || '').toLowerCase() === 'ctaiot'))

function newCommand(): CommandDraft {
  return { commandId: `CMD_${Date.now()}_${++seed}`, functionId: '', fields: [], specs: {} }
}

function ensureCommand() {
  if (!commands.value.length) {
    const command = newCommand()
    commands.value = [command]
    activeId.value = command.commandId
    return
  }
  if (!activeId.value) activeId.value = commands.value[0].commandId
}

function addCommand() {
  const command = newCommand()
  commands.value = [...commands.value, command]
  activeId.value = command.commandId
}

function removeCommand(id: string) {
  if (commands.value.length <= 1) return
  commands.value = commands.value.filter((item) => item.commandId !== id)
  if (activeId.value === id) activeId.value = commands.value[0].commandId
}

function functionLabel(id: string) {
  const item = props.functions.find((entry) => entry.id === id)
  return item ? item.name || item.id : '未选择功能'
}

function selectFunction(item: ProductMetadataItem) {
  if (!activeCommand.value) return
  const fields = normalizeFunctionInputs(item.inputs, item.id !== 'setDeviceDesiredProperties') as CommandDraft['fields']
  const specs = fields.reduce<CommandDraft['specs']>((result, field) => {
    const suggestedAuto = ['deviceId', 'childDeviceId', 'deviceCode', 'meterNo', 'imei', 'imsi', 'iccid', 'slaveAddress', 'serialNo'].includes(field.id) && !isComplex(field)
    result[field.id] = { mode: suggestedAuto ? 'AUTO' : 'FIXED', template: `{{${field.id}}}` }
    return result
  }, {})
  patchActive({ functionId: item.id, source: item.source, serviceType: '', fields, specs })
}

function patchActive(patch: Partial<CommandDraft>) {
  commands.value = commands.value.map((item) => item.commandId === activeCommand.value?.commandId ? { ...item, ...patch } : item)
}

function patchSpec(fieldId: string, patch: Record<string, unknown>) {
  if (!activeCommand.value) return
  patchActive({ specs: { ...activeCommand.value.specs, [fieldId]: { ...activeCommand.value.specs[fieldId], ...patch } } })
}

function changeMode(field: ProductMetadataItem, mode: ParamMode) {
  patchSpec(field.id, { mode })
}

function setFixedValue(fieldId: string, value: unknown) {
  patchSpec(fieldId, { value })
}

function setTemplate(fieldId: string, template: string) {
  patchSpec(fieldId, { template })
}

function modeOf(field: ProductMetadataItem) {
  return activeCommand.value?.specs[field.id]?.mode || 'FIXED'
}

function isComplex(field: ProductMetadataItem) {
  return ['object', 'array', 'geopoint', 'geoshape'].includes(String(parseRecord(field.valueType).type || '').toLowerCase())
}

function serializeFixed(field: ProductMetadataItem, value: unknown) {
  const type = String(parseRecord(field.valueType).type || field.type || 'string').toLowerCase()
  if (value === undefined || value === null || value === '') return undefined
  if (['object', 'array', 'geopoint', 'geoshape'].includes(type)) {
    try {
      return typeof value === 'string' ? JSON.parse(value) : value
    } catch {
      throw new Error(`${field.name || field.id} 不是合法 JSON`)
    }
  }
  if (['int', 'integer', 'long', 'float', 'double', 'number'].includes(type)) return Number(value)
  if (type === 'boolean') return value === true || value === 1 || value === '1' || value === 'true'
  return value
}

function validateAndBuild(): DeviceJobCommand[] | undefined {
  for (let index = 0; index < commands.value.length; index += 1) {
    const command = commands.value[index]
    if (!command.functionId) {
      activeId.value = command.commandId
      Message.warning(`请选择指令 ${index + 1} 的执行功能`)
      return
    }
    for (const field of command.fields) {
      const spec = command.specs[field.id] || { mode: 'FIXED' as ParamMode }
      if (spec.mode === 'TEMPLATE' && !String(spec.template || '').trim()) {
        activeId.value = command.commandId
        Message.warning(`指令 ${index + 1} 的参数“${field.name || field.id}”缺少模板`)
        return
      }
      if (spec.mode === 'FIXED' && field.required && (spec.value === undefined || spec.value === '')) {
        activeId.value = command.commandId
        Message.warning(`请填写指令 ${index + 1} 的参数“${field.name || field.id}”`)
        return
      }
    }
  }

  try {
    return commands.value.map((command) => {
      const functionArgsSpec: DeviceJobCommand['functionArgsSpec'] = {}
      const functionData: Record<string, unknown> = {}
      command.fields.forEach((field) => {
        const spec = command.specs[field.id] || { mode: 'FIXED' as ParamMode }
        if (spec.mode === 'AUTO') {
          functionArgsSpec[field.id] = { mode: 'AUTO' }
          functionData[field.id] = `{{auto.${field.id}}}`
        } else if (spec.mode === 'TEMPLATE') {
          functionArgsSpec[field.id] = { mode: 'TEMPLATE', template: String(spec.template || '').trim() }
          functionData[field.id] = String(spec.template || '').trim()
        } else {
          const value = serializeFixed(field, spec.value)
          functionArgsSpec[field.id] = { mode: 'FIXED', value }
          if (value !== undefined) functionData[field.id] = value
        }
      })
      return { commandId: command.commandId, functionId: command.functionId, source: command.source, serviceType: command.serviceType || undefined, functionArgsSpec, functionData }
    })
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '指令参数格式错误')
  }
}

function applySnapshot(snapshotCommands: Record<string, any>[]) {
  commands.value = snapshotCommands.map((snapshot) => {
    const fn = props.functions.find((item) => item.id === snapshot.functionId)
    const fields = normalizeFunctionInputs(fn?.inputs, fn?.id !== 'setDeviceDesiredProperties') as CommandDraft['fields']
    const specs = fields.reduce<CommandDraft['specs']>((result, field) => {
      const incoming = snapshot.functionArgsSpec?.[field.id] || {}
      result[field.id] = { mode: incoming.mode || 'FIXED', value: incoming.value ?? snapshot.functionData?.[field.id], template: incoming.template || '' }
      return result
    }, {})
    return { commandId: snapshot.commandId || `CMD_${Date.now()}_${++seed}`, functionId: snapshot.functionId || '', source: snapshot.source || fn?.source, serviceType: snapshot.serviceType || '', fields, specs }
  })
  ensureCommand()
  activeId.value = commands.value[0].commandId
}

watch(() => props.functions, ensureCommand, { immediate: true })
defineExpose({ validateAndBuild, applySnapshot })
</script>

<style scoped lang="scss" src="./DeviceCommandEditor.scss"></style>

<style scoped lang="scss">
.command-workspace main > header :deep(.arco-input-wrapper) {
  width: 180px;
}
</style>
