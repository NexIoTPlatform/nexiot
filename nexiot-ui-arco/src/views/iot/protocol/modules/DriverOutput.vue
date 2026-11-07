<template>
  <a-tabs v-model:active-key="tab" position="bottom" size="small" :animation="false" class="driver-output">
    <a-tab-pane key="result">
      <template #title><icon-play-arrow /> 执行结果</template>
      <div class="driver-output__content">
        <a-alert v-if="stale" type="warning">代码已修改，下方结果来自上次运行。</a-alert>
        <div v-if="!run" class="driver-output__welcome"><icon-code-block /><strong>等待运行</strong><span>填写运行参数，点击右上角 <icon-play-arrow /> 查看输出</span></div>
        <template v-else>
          <a-alert v-if="run.error" :type="run.state === 'FAILED' ? 'error' : 'warning'">{{ run.line ? `第 ${run.line} 行：` : '' }}{{ run.error }}</a-alert>
          <pre v-else-if="run.state === 'SUCCEEDED'">{{ formatResult(run.result) }}</pre>
          <p v-else>{{ run.state === 'PAUSED' ? `已暂停在第 ${run.line} 行，在调试信息中查看变量或继续执行。` : '正在执行…' }}</p>
        </template>
      </div>
    </a-tab-pane>
    <a-tab-pane key="debug"><template #title><icon-bug /> 调试信息<span v-if="run?.state === 'PAUSED'" class="driver-output__paused-dot" /></template><DebugVariables :variables="run?.variables || []" :paused="run?.state === 'PAUSED'" :active="active" :pending="pending" :line="run?.line" @continue="$emit('continue')" @step="$emit('step')" @stop="$emit('stop')" /></a-tab-pane>
    <a-tab-pane key="logs"><template #title><icon-file /> 运行日志 <span v-if="run?.logs.length" class="driver-output__count">{{ run.logs.length }}</span></template><div class="driver-output__content"><pre v-if="run?.logs.length">{{ run.logs.join('\n') }}</pre><a-empty v-else description="暂无输出，支持 print、println 和 log" /></div></a-tab-pane>
  </a-tabs>
</template>

<script setup lang="ts">
import type { DebugRun } from '../api/debugger'
import DebugVariables from './DebugVariables.vue'

defineOptions({ name: 'DriverOutput' })
const tab = defineModel<string>('tab', { required: true })
defineProps<{ run?: DebugRun, stale: boolean, active: boolean, pending: boolean }>()
defineEmits<{ (e: 'continue'): void, (e: 'step'): void, (e: 'stop'): void }>()
function formatResult(value: string | null) {
  if (!value) return '执行成功，返回值为空'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}
</script>

<style lang="scss" scoped>
.driver-output {
  display: flex;
  flex-direction: column;
  height: 100%;

  :deep(.arco-tabs-content) {
    flex: 1;
    min-height: 0;
    padding: 0;
    overflow: clip;
  }

  :deep(.arco-tabs-pane), :deep(.arco-tabs-content-list) {
    height: 100%;
  }

  :deep(.arco-tabs-content-item) {
    height: 100%;
    overflow: auto;
  }

  :deep(.arco-tabs-content-item:not(.arco-tabs-content-item-active)) {
    visibility: hidden;
  }

  :deep(.arco-tabs-nav) {
    flex-shrink: 0;
    background: var(--ide-toolbar);
  }

  :deep(.arco-tabs-tab) {
    padding: 9px 0;
    margin: 0 10px;
    font-size: 12px;
  }

  &__content {
    padding: 12px 16px;
  }

  &__content pre {
    margin: 0;
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
    line-height: 1.7;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }

  &__welcome {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__welcome strong {
    font-weight: 400;
    color: var(--color-text-2);
  }

  &__paused-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    margin-left: 5px;
    background: rgb(var(--warning-6));
    border-radius: 50%;
  }

  &__count {
    padding: 0 4px;
    margin-left: 3px;
    font-size: 11px;
    background: var(--color-fill-3);
    border-radius: 3px;
  }
}
</style>
