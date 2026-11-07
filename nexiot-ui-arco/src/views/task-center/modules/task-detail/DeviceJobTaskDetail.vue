<template>
  <div class="job-detail">
    <a-descriptions :column="descriptionColumns" bordered size="small" class="job-summary">
      <a-descriptions-item label="执行指令">{{ commandSummary }}</a-descriptions-item>
      <a-descriptions-item label="执行方式">{{ snapshot.commandExecutionMode === 'PARALLEL' ? '并行' : '串行' }}</a-descriptions-item>
      <a-descriptions-item label="设备范围">{{ snapshot.chooseAll ? '当前筛选结果全部设备' : '手动勾选设备' }}</a-descriptions-item>
      <a-descriptions-item label="筛选条件">{{ filterText }}</a-descriptions-item>
    </a-descriptions>
    <div v-if="commands.length" class="command-rules">
      <article v-for="(command, index) in commands" :key="command.commandId || command.functionId || index">
        <header>
          <div><strong>指令 {{ index + 1 }} · {{ command.functionName || command.functionId || '-' }}</strong><code>{{ command.functionId || '-' }}</code></div>
          <a-space><a-tag v-if="command.source">{{ command.source }}</a-tag><a-tag v-if="command.serviceType" color="purple">{{ command.serviceType }}</a-tag></a-space>
        </header>
        <div v-if="commandRules(command).length" class="rule-list">
          <div v-for="rule in commandRules(command)" :key="rule.id"><span>{{ rule.name }}</span><code>{{ rule.id }}</code><a-tag :color="rule.color">{{ rule.mode }}</a-tag><strong>{{ rule.value }}</strong></div>
        </div>
        <a-typography-text v-else type="secondary">该指令无需输入参数</a-typography-text>
      </article>
    </div>
    <a-table row-key="id" :data="tableRows" :columns="columns" :loading="loading" :pagination="false" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 1200 }" />
  </div>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TaskCenterItem, TaskDetailItem } from '../../api'
import type { ApplicationItem } from '@/apis/application/application'
import { Popover, Tag } from '@arco-design/web-vue'
import { useMediaQuery } from '@vueuse/core'
import { getApplicationList } from '@/apis/application/application'
import { formatJson, itemStatusColor, itemStatusLabel, parseRecord } from '../../shared'

const props = defineProps<{ task: TaskCenterItem, items: TaskDetailItem[], loading: boolean }>()
const compact = useMediaQuery('(max-width: 640px)')
const applications = ref<ApplicationItem[]>([])
const applicationMap = computed(() => applications.value.reduce<Record<string, string>>((result, item) => {
  result[item.appUniqueId || item.appId] = item.appName
  result[item.appId] = item.appName
  return result
}, {}))
const descriptionColumns = computed(() => compact.value ? 1 : 2)
const snapshot = computed(() => parseRecord(props.task.requestSnapshot))
const commands = computed(() => {
  if (Array.isArray(snapshot.value.commands) && snapshot.value.commands.length) return snapshot.value.commands
  return snapshot.value.functionId ? [{ functionId: snapshot.value.functionId, functionName: snapshot.value.functionName, functionData: snapshot.value.functionData }] : []
})
const commandSummary = computed(() => commands.value.map((item: Record<string, unknown>) => item.functionName || item.functionId).filter(Boolean).join('、') || '-')
const filterText = computed(() => {
  const application = snapshot.value.application
  const values = [application && `应用：${applicationMap.value[application] || application}`, snapshot.value.devGroupId && `分组：${snapshot.value.devGroupId}`, snapshot.value.keyword && `关键字：${snapshot.value.keyword}`].filter(Boolean)
  return values.join('；') || '未额外筛选'
})
const tableRows = computed(() => props.items.map((item) => {
  const raw = parseRecord(item.rawData)
  const result = parseRecord(item.resultData)
  const commandResults = Array.isArray(result.commandResults) ? result.commandResults : []
  return {
    ...item,
    deviceId: result.deviceId || raw.deviceId || item.itemKey || '-',
    deviceName: result.deviceName || raw.deviceName || item.itemName || '-',
    application: applicationMap.value[raw.application || snapshot.value.application] || raw.application || snapshot.value.application || '-',
    functionName: commandResults.length ? commandResults.map((entry: Record<string, unknown>) => entry.functionName || entry.functionId).join('、') : result.functionName || snapshot.value.functionName || snapshot.value.functionId || '-',
    resolvedData: commandResults.length ? commandResults : result.resolvedData || {}
  }
}))

function commandRules(command: Record<string, any>) {
  const definitionInputs: Record<string, unknown>[] = Array.isArray(command.functionDefinition?.inputs) ? command.functionDefinition.inputs : []
  const inputMap = definitionInputs.reduce<Record<string, Record<string, unknown>>>((result, input: Record<string, unknown>) => {
    if (input.id) result[String(input.id)] = input
    return result
  }, {})
  return Object.entries(command.functionArgsSpec || {}).map(([id, raw]) => {
    const spec = parseRecord(raw)
    const mode = String(spec.mode || 'FIXED')
    const value = mode === 'AUTO' ? '执行时逐台解析' : mode === 'TEMPLATE' ? String(spec.template || '-') : formatJson(spec.value, true)
    return { id, name: String(inputMap[id]?.name || id), mode: ({ FIXED: '固定值', AUTO: '自动取值', TEMPLATE: '变量模板' } as Record<string, string>)[mode] || mode, color: ({ FIXED: 'arcoblue', AUTO: 'green', TEMPLATE: 'orange' } as Record<string, string>)[mode] || 'gray', value }
  })
}

const columns: TableColumnData[] = [
  { title: '设备', width: 220, render: ({ record }) => (
    <div class="device-cell">
      <strong>{record.deviceName}</strong>
      <code>{record.deviceId}</code>
    </div>
  ) },
  { title: '归属应用', dataIndex: 'application', width: 180, ellipsis: true, tooltip: true },
  { title: '执行功能', dataIndex: 'functionName', width: 200, ellipsis: true, tooltip: true },
  { title: '状态', width: 100, render: ({ record }) => <Tag color={itemStatusColor(record.status)}>{itemStatusLabel(record.status)}</Tag> },
  { title: '最终参数', width: 260, render: ({ record }) => (
    <Popover
      position="lt"
      content-style={{ maxWidth: '520px' }}
      v-slots={{ content: () => <pre class="json-preview">{formatJson(record.resolvedData)}</pre> }}
    >
      <div class="json-inline">{formatJson(record.resolvedData, true)}</div>
    </Popover>
  ) },
  { title: '结果说明', dataIndex: 'message', minWidth: 220, ellipsis: true, tooltip: true }
]

getApplicationList({ page: 1, size: 500 }).then((response) => applications.value = response.data.records || [])
</script>

<style scoped lang="scss">
.job-summary {
  margin-bottom: 12px;
}

.command-rules {
  display: grid;
  gap: 10px;
  margin-bottom: 12px;
}

.command-rules article {
  padding: 12px;
  background: var(--color-fill-1);
  border-radius: 6px;
}

.command-rules header,
.command-rules header > div,
.rule-list > div {
  display: flex;
  gap: 8px;
  align-items: center;
}

.command-rules header {
  justify-content: space-between;
}

.command-rules header > div {
  align-items: baseline;
}

.command-rules code,
.rule-list span {
  font-size: 12px;
  color: var(--color-text-3);
}

.rule-list {
  display: grid;
  gap: 6px;
  margin-top: 10px;
}

.rule-list > div {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(100px, 0.7fr) 80px minmax(140px, 1.2fr);
}

.rule-list strong {
  overflow-wrap: anywhere;
}

@media (width <= 640px) {
  .command-rules header {
    align-items: flex-start;
  }

  .command-rules header,
  .command-rules header > div {
    flex-direction: column;
  }

  .rule-list > div {
    grid-template-columns: 1fr 1fr;
  }
}

.device-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.device-cell code {
  font-size: 12px;
  color: var(--color-text-3);
}

.json-inline {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: monospace;
  font-size: 12px;
  color: var(--color-text-2);
  white-space: nowrap;
  cursor: help;
}

.json-preview {
  max-width: 480px;
  max-height: 320px;
  margin: 0;
  overflow: auto;
  font-size: 12px;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
