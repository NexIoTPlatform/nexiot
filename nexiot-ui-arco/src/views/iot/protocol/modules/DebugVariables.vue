<template>
  <div class="debug-variables">
    <div class="debug-variables__toolbar">
      <span class="debug-variables__hint">{{ paused ? `暂停于第 ${line} 行 · 执行此行前的变量` : variables.length ? '上次断点快照' : '点击行号左侧添加断点，运行后查看变量' }}</span>
      <a-space size="mini">
        <a-tooltip content="收起全部变量"><a-button aria-label="收起全部变量" size="mini" type="text" :disabled="!variables.length" @click="expandedKeys = []"><template #icon><icon-shrink /></template></a-button></a-tooltip>
        <a-tooltip content="停止执行"><a-button aria-label="停止执行" size="mini" type="text" status="danger" :disabled="!active || pending" @click="$emit('stop')"><template #icon><icon-record-stop /></template></a-button></a-tooltip>
        <a-tooltip content="单步执行 · F10"><a-button aria-label="单步执行" size="mini" type="text" :disabled="!paused || pending" @click="$emit('step')"><template #icon><icon-skip-next /></template></a-button></a-tooltip>
        <a-tooltip content="继续执行 · F5"><a-button aria-label="继续执行" size="mini" type="text" :disabled="!paused || pending" @click="$emit('continue')"><template #icon><icon-play-arrow /></template></a-button></a-tooltip>
      </a-space>
    </div>
    <div class="debug-variables__body">
      <a-tree v-if="variables.length" v-model:expanded-keys="expandedKeys" :data="nodes" :selectable="false" block-node size="mini">
        <template #title="node">
          <span class="debug-variables__name">{{ node.variable.name }}</span>
          <span> = </span>
          <span class="debug-variables__value" :class="`debug-variables__value--${node.variable.kind || 'value'}`">{{ display(node.variable) }}</span>
          <span v-if="node.variable.type" class="debug-variables__type"> ({{ node.variable.type }})</span>
          <span v-if="node.variable.notice" class="debug-variables__notice"> · {{ node.variable.notice }}</span>
        </template>
      </a-tree>
      <a-empty v-else description="命中断点后显示当前变量" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DebugVariable } from '../api/debugger'

defineOptions({ name: 'DebugVariables' })
const props = defineProps<{ variables: DebugVariable[], paused: boolean, active: boolean, pending: boolean, line: number | null | undefined }>()
defineEmits<{ (e: 'continue'): void, (e: 'step'): void, (e: 'stop'): void }>()
interface VariableNode {
  key: string
  title: string
  variable: DebugVariable
  children?: VariableNode[]
}
const expandedKeys = ref<string[]>([])
function toNodes(variables: DebugVariable[], path = ''): VariableNode[] {
  return variables.map((variable, index) => {
    const key = path ? `${path}/${index}:${variable.name}` : variable.name
    return { key, title: variable.name, variable, children: variable.children ? toNodes(variable.children, key) : undefined }
  })
}
const nodes = computed(() => toNodes(props.variables))
function display(variable: DebugVariable) {
  return variable.kind === 'string' ? JSON.stringify(variable.value) : variable.value
}
</script>

<style lang="scss" scoped>
.debug-variables {
  display: flex;
  flex-direction: column;
  height: 100%;

  &__toolbar {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    padding: 4px 10px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__hint {
    flex: 1;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__body {
    flex: 1;
    min-height: 0;
    padding: 6px 12px;
    overflow: auto;
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
  }

  &__name {
    color: var(--ide-variable);
  }

  &__value {
    color: var(--color-text-1);
  }

  &__value--string, &__value--bytes {
    color: var(--ide-value);
  }

  &__value--class {
    color: var(--ide-class);
  }

  &__type, &__notice {
    color: var(--color-text-3);
  }

  :deep(.arco-tree-node-title) {
    padding: 1px 4px;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }

  :deep(.arco-tree-node-title-text) {
    user-select: text;
  }
}
</style>
