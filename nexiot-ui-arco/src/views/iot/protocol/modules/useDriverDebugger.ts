import type { DebugRun, DriverRevision, MagicFunction } from '../api/debugger'
import { Message, Modal } from '@arco-design/web-vue'
import { applyDriver, commandDriver, getDebugSource, getMagicFunctions, pollDriver, runDriver, stopDriver } from '../api/debugger'

export function useDriverDebugger(onSaved: () => void) {
  const visible = ref(false)
  const loading = ref(false)
  const saving = ref(false)
  const pending = ref(false)
  const id = ref('')
  const name = ref('')
  const source = ref('')
  const savedSource = ref('')
  const sourceHash = ref('')
  const payload = ref('')
  const context = ref('')
  const method = ref('decode')
  const breakpoints = ref<number[]>([])
  const functions = ref<MagicFunction[]>([])
  const catalogError = ref(false)
  const run = ref<DebugRun>()
  const executedSource = ref('')
  const resultStale = computed(() => !!run.value && source.value !== executedSource.value)
  const connectionLost = ref(false)
  const breakpointSyncError = ref(false)
  let commands: Promise<unknown> = Promise.resolve()
  const dirty = computed(() => source.value !== savedSource.value)
  const active = computed(() => !!run.value && ['QUEUED', 'RUNNING', 'PAUSED'].includes(run.value.state))
  const busy = computed(() => active.value || pending.value || saving.value)
  let timer: ReturnType<typeof setTimeout> | undefined
  let generation = 0

  async function loadFunctions() {
    catalogError.value = false
    try {
      functions.value = (await getMagicFunctions()).data
    } catch {
      catalogError.value = true
    }
  }
  async function open(protocolId: string, protocolName: string) {
    const current = ++generation
    clearTimeout(timer)
    id.value = protocolId
    name.value = protocolName
    run.value = undefined
    breakpointSyncError.value = false
    breakpoints.value = []
    payload.value = ''
    context.value = ''
    method.value = 'decode'
    source.value = ''
    savedSource.value = ''
    connectionLost.value = false
    visible.value = true
    loading.value = true
    void loadFunctions()
    try {
      const data = (await getDebugSource(protocolId)).data
      if (generation !== current) return
      source.value = data.source
      savedSource.value = data.source
      sourceHash.value = data.sourceHash
    } catch {
      visible.value = false
    } finally {
      if (generation === current) loading.value = false
    }
  }
  async function poll(current: number) {
    const runId = run.value?.id
    if (!runId || generation !== current || !visible.value) return
    try {
      const data = (await pollDriver(id.value, runId)).data
      if (generation !== current || run.value?.id !== runId) return
      run.value = data
      connectionLost.value = false
      if (active.value) timer = setTimeout(poll, 600, current)
    } catch {
      if (generation === current) connectionLost.value = true
    }
  }
  function reconnect() {
    clearTimeout(timer)
    void poll(generation)
  }
  async function start() {
    if (busy.value || loading.value) return
    if (!source.value.trim()) {
      Message.warning('请先输入驱动代码')
      return
    }
    const debug = breakpoints.value.length > 0
    clearTimeout(timer)
    const current = generation
    const protocolId = id.value
    pending.value = true
    connectionLost.value = false
    try {
      const data = (await runDriver(protocolId, { source: source.value, payload: payload.value, context: context.value || null, method: method.value, debug, breakpoints: breakpoints.value })).data
      if (generation !== current) {
        await stopDriver(protocolId, data.id)
        return
      }
      run.value = data
      executedSource.value = source.value
      if (active.value) timer = setTimeout(poll, 300, current)
    } finally { pending.value = false }
  }
  function enqueueCommand(action: 'continue' | 'step' | 'breakpoints') {
    const current = generation
    const runId = run.value?.id
    const protocolId = id.value
    const lines = [...breakpoints.value]
    const operation = commands.catch(() => {}).then(async () => {
      if (generation !== current || !runId || run.value?.id !== runId || !active.value) return
      const data = (await commandDriver(protocolId, runId, action, lines)).data
      if (generation === current && run.value?.id === runId) return data
    })
    commands = operation
    return operation
  }
  async function syncBreakpoints() {
    if (!active.value) return
    const current = generation
    const runId = run.value?.id
    try {
      await enqueueCommand('breakpoints')
      if (generation === current && run.value?.id === runId) breakpointSyncError.value = false
    } catch {
      if (generation === current && run.value?.id === runId) breakpointSyncError.value = true
    }
  }
  // 断点与源码只读状态分离，运行和暂停期间均可同步；命令串行，避免旧断点覆盖新断点。
  watch([breakpoints, () => run.value?.id], () => {
    void syncBreakpoints()
  }, { deep: true })
  async function proceed(action: 'continue' | 'step') {
    if (run.value?.state !== 'PAUSED' || pending.value) return
    pending.value = true
    clearTimeout(timer)
    try {
      const data = await enqueueCommand(action)
      if (data) run.value = data
    } finally {
      pending.value = false
      if (active.value) timer = setTimeout(poll, 300, generation)
    }
  }
  async function stop() {
    if (!run.value || pending.value) return
    pending.value = true
    clearTimeout(timer)
    try {
      await stopDriver(id.value, run.value.id)
      run.value = (await pollDriver(id.value, run.value.id)).data
      connectionLost.value = false
    } finally { pending.value = false }
  }
  async function save() {
    if (busy.value || !dirty.value) return
    saving.value = true
    try {
      sourceHash.value = (await applyDriver(id.value, source.value, sourceHash.value)).data.sourceHash
      savedSource.value = source.value
      Message.success('驱动已保存并应用')
      onSaved()
    } finally { saving.value = false }
  }
  function restore(revision: DriverRevision) {
    if (busy.value) return
    source.value = revision.source
    breakpoints.value = []
    run.value = undefined
    Message.info('历史源码已载入草稿，调试通过后可保存并应用')
  }
  function confirmClose(): Promise<boolean> {
    if (saving.value || pending.value) return Promise.resolve(false)
    if (!dirty.value && !active.value) return Promise.resolve(true)
    return new Promise((resolve) => Modal.confirm({
      title: '关闭驱动调试',
      content: [dirty.value ? '当前有未保存的代码，关闭后将丢弃草稿。' : '', active.value ? '正在进行的调试也会停止。' : ''].join(''),
      okText: '关闭',
      cancelText: '继续编辑',
      onOk: () => resolve(true),
      onCancel: () => resolve(false)
    }))
  }
  async function beforeClose() {
    if (!(await confirmClose())) return false
    if (active.value) {
      try {
        await stop()
      } catch {
        return false
      }
    }
    generation++
    clearTimeout(timer)
    return true
  }
  function unload(event: BeforeUnloadEvent) {
    if (dirty.value || active.value) event.preventDefault()
  }
  onMounted(() => window.addEventListener('beforeunload', unload))
  onBeforeUnmount(() => {
    generation++
    clearTimeout(timer)
    window.removeEventListener('beforeunload', unload)
    if (active.value && run.value) void stopDriver(id.value, run.value.id).catch(() => {})
  })
  return { visible, loading, saving, pending, id, name, source, savedSource, payload, context, method, breakpoints, functions, catalogError, run, resultStale, connectionLost, breakpointSyncError, syncBreakpoints, dirty, active, busy, open, start, proceed, stop, save, beforeClose, loadFunctions, reconnect, restore }
}
