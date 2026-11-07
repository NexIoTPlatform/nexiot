<template>
  <div class="driver-toolbar">
    <div class="driver-toolbar__context">
      <a-tooltip content="编辑运行参数"><button class="driver-toolbar__method" @click="$emit('input')">{{ method }}<icon-down /></button></a-tooltip>
      <a-tag v-if="run" :color="stateColor" size="small">{{ stateLabel }}</a-tag>
      <span v-else class="driver-toolbar__hint">{{ breakpoints ? `${breakpoints} 个断点` : '就绪' }}</span>
    </div>
    <div class="driver-toolbar__actions">
      <a-tooltip content="运行参数"><a-button aria-label="运行参数" :aria-pressed="inputOpen" :class="{ 'driver-toolbar__selected': inputOpen }" type="text" size="small" @click="$emit('input')"><template #icon><icon-settings /></template></a-button></a-tooltip>
      <a-tooltip content="历史版本与源码对比"><a-button aria-label="历史版本" :class="{ 'driver-toolbar__selected': view === 'history' }" type="text" size="small" @click="$emit('history')"><template #icon><icon-history /></template></a-button></a-tooltip>
      <a-tooltip content="函数文档"><a-button aria-label="函数文档" :class="{ 'driver-toolbar__selected': view === 'functions' }" type="text" size="small" @click="$emit('functions')"><template #icon><icon-book /></template></a-button></a-tooltip>
      <span class="driver-toolbar__separator" />
      <a-dropdown @select="changeTheme">
        <a-tooltip content="外观：跟随平台 / 浅色 / 深色"><a-button aria-label="外观" size="small" type="text"><template #icon><icon-skin /></template></a-button></a-tooltip>
        <template #content><a-doption value="auto">{{ appearance === 'auto' ? '✓ ' : '' }}跟随平台</a-doption><a-doption value="light">{{ appearance === 'light' ? '✓ ' : '' }}浅色</a-doption><a-doption value="dark">{{ appearance === 'dark' ? '✓ ' : '' }}深色</a-doption></template>
      </a-dropdown>
      <a-tooltip :content="fullscreen ? '退出全屏' : '全屏'"><a-button :aria-label="fullscreen ? '退出全屏' : '全屏'" type="text" size="small" @click="$emit('fullscreen')"><template #icon><icon-fullscreen-exit v-if="fullscreen" /><icon-fullscreen v-else /></template></a-button></a-tooltip>
      <a-tooltip content="保存并应用 · Ctrl / ⌘ + S"><a-button v-hasPerm="['protocol:protocol:edit']" aria-label="保存并应用" type="text" size="small" :disabled="saveDisabled" :loading="saving" @click="$emit('save')"><template #icon><icon-save /></template></a-button></a-tooltip>
      <span class="driver-toolbar__separator" />
      <a-tooltip v-if="active" content="停止执行"><a-button aria-label="停止执行" type="text" size="small" status="danger" :disabled="pending" @click="$emit('stop')"><template #icon><icon-record-stop /></template></a-button></a-tooltip>
      <a-tooltip v-if="run?.state === 'PAUSED'" content="单步执行 · F10"><a-button aria-label="单步执行" type="text" size="small" :disabled="pending" @click="$emit('step')"><template #icon><icon-skip-next /></template></a-button></a-tooltip>
      <a-tooltip :content="runHint"><a-button :aria-label="run?.state === 'PAUSED' ? '继续执行' : '运行'" class="driver-toolbar__run" type="text" size="small" :disabled="runDisabled" :loading="pending" @click="$emit('run')"><template #icon><icon-play-arrow /></template></a-button></a-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DebugRun } from '../api/debugger'

defineOptions({ name: 'DriverToolbar' })
const props = defineProps<{ method: string, run?: DebugRun, stateLabel: string, stateColor: string, breakpoints: number, inputOpen: boolean, view: string, appearance: string, fullscreen: boolean, saving: boolean, active: boolean, pending: boolean, saveDisabled: boolean, runDisabled: boolean }>()
const emit = defineEmits<{
  (e: 'input'): void
  (e: 'history'): void
  (e: 'functions'): void
  (e: 'theme', value: 'auto' | 'light' | 'dark'): void
  (e: 'fullscreen'): void
  (e: 'save'): void
  (e: 'run'): void
  (e: 'step'): void
  (e: 'stop'): void
}>()
const runHint = computed(() => props.run?.state === 'PAUSED' ? '继续执行 · F5' : props.breakpoints ? '运行（自动断点调试）· F5' : '运行 · F5 / Ctrl / ⌘ + Enter')
function changeTheme(value: string | number | Record<string, unknown> | undefined) {
  if (value === 'auto' || value === 'light' || value === 'dark') emit('theme', value)
}
</script>

<style lang="scss" scoped>
.driver-toolbar {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  padding: 5px 12px;
  background: var(--ide-toolbar);
  border-bottom: 1px solid var(--color-border-2);

  &__context, &__actions {
    display: flex;
    gap: 4px;
    align-items: center;
  }

  &__actions {
    margin-left: auto;
  }

  &__method {
    display: flex;
    gap: 6px;
    align-items: center;
    padding: 4px 8px;
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
    color: var(--color-text-2);
    cursor: pointer;
    background: transparent;
    border: 0;
    border-radius: 3px;
  }

  &__method:hover {
    background: var(--color-fill-2);
  }

  &__hint {
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__separator {
    width: 1px;
    height: 16px;
    margin: 0 5px;
    background: var(--color-border-2);
  }

  &__actions :deep(.arco-btn) {
    width: 30px;
    padding: 0;
    color: var(--color-text-2);
  }

  &__actions :deep(.arco-btn:hover), &__actions :deep(.driver-toolbar__selected) {
    color: var(--ide-accent);
    background: var(--color-fill-2);
  }

  &__actions :deep(.driver-toolbar__run:not(:disabled)) {
    color: var(--ide-run);
  }
}
</style>
