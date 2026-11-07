<template>
  <a-spin :loading="loading" class="driver-history">
    <a-alert v-if="error" type="error">历史加载失败。<a-link @click="load">重试</a-link></a-alert>
    <a-empty v-else-if="!revisions.length && !loading" description="暂无历史记录。首次保存时会保留修改前后的源码。" />
    <template v-else-if="revisions.length">
      <div class="driver-history__toolbar">
        <a-select v-model="selectedId" size="small" :options="options" class="driver-history__select" />
        <a-popconfirm content="用此版本替换当前草稿？载入后仍需保存并应用才会生效。" @ok="restore"><a-button type="primary" size="small" :disabled="disabled">载入草稿</a-button></a-popconfirm>
      </div>
      <div class="driver-history__hint"><span>左侧：历史源码 · 右侧：当前草稿 · 最近保留 30 份源码</span><a-space size="mini"><a-tooltip content="上一处改动"><a-button aria-label="上一处改动" size="mini" type="text" @click="diffRef?.goToDiff('previous')"><template #icon><icon-up /></template></a-button></a-tooltip><a-tooltip content="下一处改动"><a-button aria-label="下一处改动" size="mini" type="text" @click="diffRef?.goToDiff('next')"><template #icon><icon-down /></template></a-button></a-tooltip></a-space></div>
      <div v-if="selected && active" class="driver-history__diff"><MagicEditor :key="selected.id" ref="diffRef" :theme="dark ? 'dark' : 'light'" :original="selected.source" :model-value="source" readonly /></div>
    </template>
  </a-spin>
</template>

<script setup lang="ts">
import type { DriverRevision } from '../api/debugger'
import { getDriverHistory } from '../api/debugger'
import MagicEditor from './MagicEditor.vue'

defineOptions({ name: 'DriverHistory' })
const props = defineProps<{ protocolId: string, source: string, savedSource: string, dark: boolean, active: boolean, disabled: boolean }>()
const emit = defineEmits<{ (e: 'restore', revision: DriverRevision): void }>()
const diffRef = useTemplateRef<InstanceType<typeof MagicEditor>>('diffRef')
const loading = ref(false)
const error = ref(false)
const revisions = ref<DriverRevision[]>([])
const selectedId = ref('')
let generation = 0
const selected = computed(() => revisions.value.find((item) => item.id === selectedId.value))
const options = computed(() => revisions.value.map((item) => ({ value: item.id, label: `${new Date(item.createdAt).toLocaleString()} · ${item.sourceHash.slice(0, 7)} · ${item.version || '未标版本'} · ${item.savedBy}` })))
async function load() {
  const current = ++generation
  loading.value = true
  error.value = false
  try {
    const data = (await getDriverHistory(props.protocolId)).data
    if (current !== generation) return
    revisions.value = data
    if (!data.some((item) => item.id === selectedId.value)) selectedId.value = data.find((item) => item.source !== props.source)?.id || data[0]?.id || ''
  } catch {
    if (current === generation) error.value = true
  } finally {
    if (current === generation) loading.value = false
  }
}
function restore() {
  if (selected.value && !props.disabled) emit('restore', selected.value)
}
watch(() => [props.active, props.protocolId, props.savedSource], () => {
  if (props.active) void load()
}, { immediate: true })
onBeforeUnmount(() => generation++)
</script>

<style lang="scss" scoped>
.driver-history {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  &__toolbar {
    display: flex;
    flex-shrink: 0;
    gap: 12px;
    padding: 8px 12px 4px;
  }

  &__select {
    flex: 1;
    min-width: 0;
  }

  &__hint {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 4px 12px 8px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__diff {
    flex: 1;
    min-height: 100px;
    border-top: 1px solid var(--color-border-2);
  }
}
</style>
