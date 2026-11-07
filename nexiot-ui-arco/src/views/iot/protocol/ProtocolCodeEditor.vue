<template>
  <div class="protocol-code-editor" :class="{ 'protocol-code-editor--compact': compact, 'protocol-code-editor--fullscreen': fullscreen }">
    <div class="protocol-code-editor__toolbar">
      <span v-if="fullscreen">代码编辑器</span>
      <a-tooltip :content="fullscreen ? '退出全屏' : '全屏编辑'">
        <a-button type="text" shape="circle" :aria-label="fullscreen ? '退出全屏' : '全屏编辑'" @click="fullscreen = !fullscreen">
          <icon-fullscreen-exit v-if="fullscreen" />
          <icon-fullscreen v-else />
        </a-button>
      </a-tooltip>
    </div>
    <CodeMirror
      :model-value="modelValue"
      :extensions="extensions"
      :tab-size="2"
      :dark="true"
      :basic="true"
      :readonly="readonly"
      :placeholder="placeholder"
      @update:model-value="handleUpdate"
    />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable vue/custom-event-name-casing -- Vue v-model requires the update:modelValue event name. */
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import CodeMirror from 'vue-codemirror6'

defineOptions({ name: 'ProtocolCodeEditor' })

withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  readonly?: boolean
  compact?: boolean
}>(), {
  modelValue: '',
  placeholder: '',
  readonly: false,
  compact: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const extensions = [javascript(), oneDark]
const fullscreen = ref(false)

function handleUpdate(value?: unknown) {
  emit('update:modelValue', String(value || ''))
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape' || !fullscreen.value) return
  event.preventDefault()
  event.stopImmediatePropagation()
  fullscreen.value = false
}

onMounted(() => window.addEventListener('keydown', handleKeydown, true))
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown, true))
</script>

<style lang="scss" scoped>
.protocol-code-editor {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  background: #282c34;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    min-height: 38px;
    padding: 0 8px 0 12px;
    color: var(--color-text-1);
    background: var(--color-bg-2);
    border-bottom: 1px solid var(--color-border-2);

    span {
      margin-right: auto;
      font-size: 14px;
      font-weight: 600;
    }
  }

  :deep(.cm-editor) {
    width: 100%;
    height: 480px;
  }

  :deep(.cm-scroller) {
    overflow: auto;
    font-family: SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 13px;
    line-height: 1.65;
  }

  &--compact :deep(.cm-editor) {
    height: 180px;
  }

  &--fullscreen {
    position: fixed;
    inset: 0;
    z-index: 2001;
    border: 0;
    border-radius: 0;

    :deep(.cm-editor) {
      height: calc(100vh - 39px);
    }
  }
}
</style>
