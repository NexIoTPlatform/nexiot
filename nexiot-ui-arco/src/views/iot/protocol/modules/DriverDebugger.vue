<template>
  <a-modal
    v-model:visible="visible" :fullscreen="fullscreen" width="min(1500px, 96vw)"
    :mask-closable="false" :esc-to-close="false" :on-before-cancel="handleCancel"
    :footer="false" :body-style="{ padding: 0 }" :modal-class="['driver-debugger-shell', { 'driver-debugger-shell--dark': dark }]" unmount-on-close
  >
    <template #title><div class="driver-debugger__title"><icon-code /> {{ name }} <span>驱动调试</span><a-tag v-if="dirty" color="orange" size="small">未保存</a-tag></div></template>
    <a-spin :loading="loading" class="driver-debugger" :class="{ 'driver-debugger--full': fullscreen }" @keydown.f5.prevent="handleRun" @keydown.f10.prevent="proceed('step')">
      <DriverToolbar :method="method" :run="run" :state-label="stateLabel" :state-color="stateColor" :breakpoints="breakpoints.length" :input-open="inputOpen && view === 'code'" :view="view" :appearance="appearance" :fullscreen="fullscreen" :saving="saving" :active="active" :pending="pending" :save-disabled="busy || !dirty || loading" :run-disabled="runDisabled" @input="toggleInput" @history="view = view === 'history' ? 'code' : 'history'" @functions="view = view === 'functions' ? 'code' : 'functions'" @theme="appearance = $event" @fullscreen="fullscreen = !fullscreen" @save="confirmSave" @run="handleRun" @step="proceed('step')" @stop="stop" />
      <a-alert v-if="connectionLost" type="warning">调试连接已中断，代码草稿仍保留。<a-link @click="reconnect">重新连接</a-link></a-alert>
      <a-alert v-if="breakpointSyncError" type="warning">断点同步失败，当前会话可能仍使用旧断点。<a-link @click="syncBreakpoints">重试同步</a-link></a-alert>
      <div v-if="!loading" class="driver-debugger__workspace">
        <div v-show="view === 'code'" class="driver-debugger__coding">
          <div class="driver-debugger__workbench">
            <a-split v-model:size="outputSplit" direction="vertical" :min="0.25" :max="0.85" class="driver-debugger__split">
              <template #first>
                <main class="driver-debugger__main">
                  <div class="driver-debugger__file"><span class="driver-debugger__file-tab"><icon-code /> {{ id }}.ms <i v-if="dirty" /></span><span class="driver-debugger__file-hint">{{ active ? '可继续增删断点 · F9 切换' : '点击行号左侧设置断点 · F9 切换' }}</span></div>
                  <div class="driver-debugger__editor"><MagicEditor ref="editorRef" v-model="source" v-model:breakpoints="breakpoints" :theme="dark ? 'dark' : 'light'" :functions="functions" :line="run?.line" :error="run?.state === 'FAILED' ? run.error : null" :readonly="busy" :breakpoints-disabled="saving || loading" @run="handleRun" @step="proceed('step')" @save="confirmSave" /></div>
                </main>
              </template>
              <template #second><DriverOutput v-model:tab="outputTab" :run="run" :stale="resultStale" :active="active" :pending="pending" @continue="proceed('continue')" @step="proceed('step')" @stop="stop" /></template>
            </a-split>
          </div>
          <aside v-if="inputOpen" class="driver-debugger__input">
            <div class="driver-debugger__panel-heading"><span><icon-settings /> 运行参数</span><a-tooltip content="收起运行参数"><a-button aria-label="收起运行参数" type="text" size="mini" @click="inputOpen = false"><template #icon><icon-close /></template></a-button></a-tooltip></div>
            <DriverInput v-model:method="method" v-model:payload="payload" v-model:context="context" :disabled="busy" />
          </aside>
        </div>
        <section v-if="view !== 'code'" class="driver-debugger__document">
          <div class="driver-debugger__document-heading"><a-button size="small" type="text" @click="view = 'code'"><template #icon><icon-arrow-left /></template>返回代码</a-button><strong>{{ view === 'history' ? '历史版本 · 源码对比' : 'MagicScript 函数文档' }}</strong></div>
          <div class="driver-debugger__document-body">
            <DriverHistory v-if="view === 'history'" active :dark="dark" :protocol-id="id" :source="source" :saved-source="savedSource" :disabled="busy" @restore="restoreRevision" />
            <DriverFunctions v-else :functions="functions" :error="catalogError" :disabled="busy" @reload="loadFunctions" @insert="insertFunction" />
          </div>
        </section>
      </div>
      <div class="driver-debugger__status"><span>{{ run ? stateLabel : '就绪' }} <span class="driver-debugger__status-gap">{{ breakpoints.length }} 个断点{{ breakpoints.length ? ' · 运行时自动调试' : '' }}</span></span><span>{{ dirty ? '未保存草稿' : '已同步' }} · UTF-8 · MagicScript</span></div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { DriverRevision, MagicFunction } from '../api/debugger'
import { Modal } from '@arco-design/web-vue'
import { useLocalStorage, useMutationObserver } from '@vueuse/core'
import { hasPerm } from '@/utils/permission'
import DriverFunctions from './DriverFunctions.vue'
import DriverHistory from './DriverHistory.vue'
import DriverInput from './DriverInput.vue'
import DriverOutput from './DriverOutput.vue'
import DriverToolbar from './DriverToolbar.vue'
import MagicEditor from './MagicEditor.vue'
import { useDriverDebugger } from './useDriverDebugger'
import './debuggerTheme.scss'

defineOptions({ name: 'DriverDebugger' })
const emit = defineEmits<{ (e: 'save-success'): void }>()
const { visible, loading, saving, pending, id, name, source, savedSource, payload, context, method, breakpoints, functions, catalogError, run, resultStale, connectionLost, breakpointSyncError, syncBreakpoints, dirty, active, busy, open, start, proceed, stop, save, beforeClose, loadFunctions, reconnect, restore } = useDriverDebugger(() => emit('save-success'))
const editorRef = useTemplateRef<InstanceType<typeof MagicEditor>>('editorRef')
const fullscreen = ref(false)
const view = ref<'code' | 'history' | 'functions'>('code')
const inputOpen = ref(false)
const appearance = useLocalStorage<'auto' | 'light' | 'dark'>('nexiot-driver-appearance', 'auto')
const platformDark = ref(document.body.getAttribute('arco-theme') === 'dark')
useMutationObserver(document.body, () => {
  platformDark.value = document.body.getAttribute('arco-theme') === 'dark'
}, { attributes: true, attributeFilter: ['arco-theme'] })
const dark = computed(() => appearance.value === 'dark' || (appearance.value === 'auto' && platformDark.value))
const outputSplit = ref(0.66)
const outputTab = ref('result')
const runDisabled = computed(() => loading.value || saving.value || pending.value || (active.value && run.value?.state !== 'PAUSED'))
const stateLabel = computed(() => ({ QUEUED: '等待执行', RUNNING: '运行中', PAUSED: '断点暂停', SUCCEEDED: '运行成功', FAILED: '运行失败', CANCELLED: '已停止' })[run.value?.state || 'QUEUED'])
const stateColor = computed(() => ({ SUCCEEDED: 'green', FAILED: 'red', PAUSED: 'orange', RUNNING: 'arcoblue', QUEUED: 'gray', CANCELLED: 'gray' })[run.value?.state || 'QUEUED'])
function toggleInput() {
  inputOpen.value = view.value === 'code' ? !inputOpen.value : true
  view.value = 'code'
}
function handleRun() {
  if (runDisabled.value) return
  view.value = 'code'
  if (window.innerWidth <= 900) inputOpen.value = false
  if (run.value?.state === 'PAUSED') {
    void proceed('continue')
  } else {
    outputTab.value = breakpoints.value.length ? 'debug' : 'result'
    void start()
  }
}
function restoreRevision(revision: DriverRevision) {
  restore(revision)
  view.value = 'code'
}
async function insertFunction(fn: MagicFunction) {
  view.value = 'code'
  await nextTick()
  editorRef.value?.insertFunction(fn)
}
function handleCancel() {
  void beforeClose().then((close) => {
    if (close) visible.value = false
  })
  return false
}
function confirmSave() {
  if (!hasPerm('protocol:protocol:edit') || busy.value || !dirty.value) return
  Modal.confirm({ title: '保存并应用', content: '保存后将立即应用到此产品驱动，确认继续？', onBeforeOk: save })
}
watch(() => run.value?.state, (state) => {
  if (state === 'PAUSED') outputTab.value = 'debug'
  if (state === 'FAILED' || state === 'SUCCEEDED' || state === 'CANCELLED') outputTab.value = 'result'
})
watch(visible, (value) => {
  if (value) {
    view.value = 'code'
    outputTab.value = 'result'
    inputOpen.value = window.innerWidth > 900
  }
})
defineExpose({ open })
</script>

<style lang="scss" scoped>
.driver-debugger {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: min(900px, 86vh);
  color: var(--color-text-1);
  background: var(--ide-editor);

  &--full {
    height: calc(100vh - 42px);
  }

  &__title {
    display: flex;
    gap: 10px;
    align-items: center;
    font-size: 13px;
  }

  &__title span {
    font-size: 12px;
    font-weight: 400;
    color: var(--color-text-3);
  }

  &__workspace {
    position: relative;
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  &__coding {
    display: flex;
    height: 100%;
  }

  &__workbench {
    flex: 1;
    min-width: 0;
    height: 100%;
  }

  &__split, &__main {
    height: 100%;
  }

  &__main {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  &__file {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: space-between;
    height: 34px;
    font-size: 12px;
    color: var(--color-text-2);
    background: var(--ide-toolbar);
  }

  &__file-tab {
    display: flex;
    gap: 8px;
    align-items: center;
    height: 100%;
    padding: 0 18px;
    background: var(--ide-editor);
    border-top: 1px solid var(--ide-accent);
    border-right: 1px solid var(--color-border-2);
  }

  &__file-tab .arco-icon {
    color: var(--ide-file);
  }

  &__file-tab i {
    width: 7px;
    height: 7px;
    margin-left: 10px;
    background: var(--color-text-3);
    border-radius: 50%;
  }

  &__file-hint {
    padding-right: 14px;
    color: var(--color-text-3);
  }

  &__editor {
    flex: 1;
    min-height: 0;
  }

  &__input {
    display: flex;
    flex: 0 0 300px;
    flex-direction: column;
    min-height: 0;
    background: var(--ide-editor);
    border-left: 1px solid var(--color-border-2);
  }

  &__panel-heading {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: space-between;
    height: 34px;
    padding: 0 12px;
    font-size: 12px;
    background: var(--ide-toolbar);
    border-bottom: 1px solid var(--color-border-2);
  }

  &__document {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  &__document-heading {
    display: flex;
    flex-shrink: 0;
    gap: 18px;
    align-items: center;
    padding: 6px 12px;
    border-bottom: 1px solid var(--color-border-2);
  }

  &__document-heading strong {
    font-size: 13px;
    font-weight: 500;
  }

  &__document-body {
    flex: 1;
    min-height: 0;
  }

  &__status {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: space-between;
    padding: 4px 12px;
    font-size: 11px;
    color: var(--ide-status-text);
    background: var(--ide-status);
  }

  &__status-gap {
    margin-left: 18px;
  }
}

@media (width <= 900px) {
  .driver-debugger__file-hint {
    display: none;
  }

  .driver-debugger__input {
    position: absolute;
    inset: 0 0 0 auto;
    z-index: 5;
    width: min(320px, 90%);
    box-shadow: -8px 0 24px #0002;
  }
}
</style>
