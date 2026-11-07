<template>
  <div ref="container" class="magic-editor" />
</template>

<script setup lang="ts">
import type { MagicFunction } from '../api/debugger'
import { monaco, registerMagicIntelligence, setupMagicLanguage } from './magicLanguage'

defineOptions({ name: 'MagicEditor' })
const props = withDefaults(defineProps<{
  modelValue: string
  theme?: 'auto' | 'light' | 'dark'
  functions?: MagicFunction[]
  breakpoints?: number[]
  line?: number | null
  error?: string | null
  readonly?: boolean
  breakpointsDisabled?: boolean
  original?: string
}>(), { functions: () => [], breakpoints: () => [], line: null, error: null, original: undefined, theme: 'auto' })
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:breakpoints', value: number[]): void
  (e: 'run'): void
  (e: 'save'): void
  (e: 'step'): void
}>()
const container = useTemplateRef<HTMLDivElement>('container')
let editor: monaco.editor.IStandaloneCodeEditor | undefined
let diff: monaco.editor.IStandaloneDiffEditor | undefined
let model: monaco.editor.ITextModel | undefined
let original: monaco.editor.ITextModel | undefined
let marks: monaco.editor.IEditorDecorationsCollection | undefined
let pauseMark: monaco.editor.IEditorDecorationsCollection | undefined
let syncing = false
const disposables: monaco.IDisposable[] = []
let observer: MutationObserver | undefined

function decorate() {
  if (!editor || !model) return
  marks?.set(props.breakpoints.filter((line) => line <= model!.getLineCount()).map((line) => ({
    range: new monaco.Range(line, 1, line, 1),
    options: { glyphMarginClassName: 'magic-editor__breakpoint', glyphMarginHoverMessage: { value: '点击取消断点' }, stickiness: monaco.editor.TrackedRangeStickiness.NeverGrowsWhenTypingAtEdges }
  })))
  const line = props.line && props.line <= model.getLineCount() ? props.line : null
  pauseMark?.set(line ? [{ range: new monaco.Range(line, 1, line, 1), options: { isWholeLine: true, className: 'magic-editor__current', linesDecorationsClassName: 'magic-editor__arrow' } }] : [])
  monaco.editor.setModelMarkers(model, 'driver', line && props.error ? [{ startLineNumber: line, endLineNumber: line, startColumn: 1, endColumn: model.getLineMaxColumn(line), message: props.error, severity: monaco.MarkerSeverity.Error }] : [])
  if (line) editor.revealLineInCenterIfOutsideViewport(line)
}
function insertFunction(fn: MagicFunction) {
  editor?.focus()
  editor?.trigger('function-panel', 'type', { text: `${fn.name}(${fn.params.map((p) => p.name).join(', ')})` })
  editor?.trigger('function-panel', 'editor.action.triggerParameterHints', {})
}
function theme() {
  monaco.editor.setTheme(props.theme === 'dark' || (props.theme === 'auto' && document.body.getAttribute('arco-theme') === 'dark') ? 'nexiot-magic-dark' : 'vs')
}
onMounted(() => {
  if (!container.value) return
  setupMagicLanguage()
  theme()
  model = monaco.editor.createModel(props.modelValue, 'nexiot-magic')
  const options: monaco.editor.IStandaloneEditorConstructionOptions = {
    automaticLayout: true,
    readOnly: props.readonly,
    glyphMargin: props.original === undefined,
    minimap: { enabled: false },
    fontSize: 13,
    quickSuggestions: { other: true, comments: false, strings: false },
    quickSuggestionsDelay: 100,
    suggestOnTriggerCharacters: true,
    wordBasedSuggestions: 'off',
    lineHeight: 22,
    scrollBeyondLastLine: false,
    padding: { top: 12 },
    tabSize: 2,
    fixedOverflowWidgets: true,
    wordWrap: 'on',
    renderLineHighlight: 'line',
    overviewRulerLanes: 0
  }
  if (props.original !== undefined) {
    original = monaco.editor.createModel(props.original, 'nexiot-magic')
    diff = monaco.editor.createDiffEditor(container.value, { ...options, readOnly: true, renderSideBySide: true })
    diff.setModel({ original, modified: model })
    editor = diff.getModifiedEditor()
    let revealFirst = true
    disposables.push(diff.onDidUpdateDiff(() => {
      if (revealFirst && diff?.getLineChanges()?.length) {
        diff.goToDiff('next')
        revealFirst = false
      }
    }))
  } else {
    editor = monaco.editor.create(container.value, { ...options, model })
  }
  marks = editor.createDecorationsCollection()
  pauseMark = editor.createDecorationsCollection()
  disposables.push(...registerMagicIntelligence(model, () => props.functions))
  disposables.push(editor.onDidChangeModelContent(() => {
    if (syncing) return
    emit('update:modelValue', model!.getValue())
    emit('update:breakpoints', marks?.getRanges().map((r) => r.startLineNumber) || [])
  }))
  disposables.push(editor.onMouseDown((event) => {
    if (props.original !== undefined || props.breakpointsDisabled || event.target.type !== monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN) return
    const line = event.target.position?.lineNumber
    if (line) emit('update:breakpoints', props.breakpoints.includes(line) ? props.breakpoints.filter((v) => v !== line) : [...props.breakpoints, line].sort((a, b) => a - b))
  }))
  disposables.push(editor.addAction({
    id: 'driver-breakpoint',
    label: '切换断点',
    keybindings: [monaco.KeyCode.F9],
    run: () => {
      const line = editor?.getPosition()?.lineNumber
      if (line && props.original === undefined && !props.breakpointsDisabled) emit('update:breakpoints', props.breakpoints.includes(line) ? props.breakpoints.filter((value) => value !== line) : [...props.breakpoints, line])
    }
  }))
  disposables.push(editor.addAction({ id: 'driver-run', label: '运行驱动', keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, monaco.KeyCode.F5], run: () => emit('run') }))
  disposables.push(editor.addAction({ id: 'driver-step', label: '单步执行', keybindings: [monaco.KeyCode.F10], run: () => emit('step') }))
  disposables.push(editor.addAction({ id: 'driver-save', label: '保存并应用', keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS], run: () => emit('save') }))
  observer = new MutationObserver(theme)
  observer.observe(document.body, { attributes: true, attributeFilter: ['arco-theme'] })
  decorate()
})
watch(() => props.modelValue, (value) => {
  if (model && value !== model.getValue()) {
    syncing = true
    model.setValue(value)
    syncing = false
    decorate()
  }
})
watch(() => props.original, (value) => {
  if (original && value !== undefined) original.setValue(value)
})
watch(() => props.theme, theme)
watch(() => props.readonly, (value) => editor?.updateOptions({ readOnly: value }))
watch(() => [props.breakpoints, props.line, props.error], decorate, { deep: true })
onBeforeUnmount(() => {
  observer?.disconnect()
  disposables.forEach((d) => d.dispose())
  if (diff) diff.dispose()
  else editor?.dispose()
  model?.dispose()
  original?.dispose()
})
function goToDiff(direction: 'next' | 'previous') {
  diff?.goToDiff(direction)
}
defineExpose({ insertFunction, goToDiff })
</script>

<style lang="scss" scoped>
.magic-editor {
  width: 100%;
  height: 100%;
  min-height: 80px;

  :deep(.magic-editor__breakpoint) {
    cursor: pointer;
    background: radial-gradient(circle, rgb(var(--danger-6)) 0 5px, transparent 6px);
  }

  :deep(.magic-editor__current) {
    background: var(--ide-line);
  }

  :deep(.magic-editor__arrow) {
    border-left: 3px solid rgb(var(--warning-6));
  }
}
</style>
