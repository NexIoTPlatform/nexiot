<template>
  <section class="rule-review">
    <div class="rule-review__status">
      <div class="rule-review__icon" :class="[{ 'is-running': rule.status === 'run' }]"><icon-play-arrow-fill v-if="rule.status === 'run'" /><icon-pause-circle-fill v-else /></div>
      <div><strong>{{ rule.status === 'run' ? '规则将在保存后运行' : '规则将在保存后保持停止' }}</strong><span>可在列表中随时切换运行状态</span></div>
      <a-switch :model-value="running" checked-text="运行" unchecked-text="停止" @change="changeRunning" />
    </div>
    <a-descriptions :column="{ xs: 1, sm: 2 }" bordered>
      <a-descriptions-item label="规则名称">{{ rule.ruleName }}</a-descriptions-item>
      <a-descriptions-item label="数据级别">{{ levelLabel }}</a-descriptions-item>
      <a-descriptions-item label="产品/群组">{{ scopeLabel }}</a-descriptions-item>
      <a-descriptions-item label="设备数量">{{ rule.relationIds?.length || 0 }}</a-descriptions-item>
      <a-descriptions-item label="来源应用">{{ rule.config.appId }}</a-descriptions-item>
      <a-descriptions-item label="转发目标">{{ rule.config.targets.length }} 个</a-descriptions-item>
      <a-descriptions-item label="SQL" :span="2"><pre>{{ sqlPreview }}</pre></a-descriptions-item>
      <a-descriptions-item label="描述" :span="2">{{ rule.description || '-' }}</a-descriptions-item>
    </a-descriptions>
  </section>
</template>

<script setup lang="ts">
import type { ForwardRule } from '../api'

const props = defineProps<{ rule: ForwardRule, productName?: string, groupName?: string }>()
const emit = defineEmits<{ (e: 'status-change', value: 'run' | 'stop'): void }>()
const running = computed(() => props.rule.status === 'run')
function changeRunning(value: string | number | boolean) {
  emit('status-change', value ? 'run' : 'stop')
}
const levelLabel = computed(() => ({ product: '产品', group: '设备群组', device: '指定设备' }[props.rule.dataLevel]))
const scopeLabel = computed(() => props.rule.dataLevel === 'group' ? props.groupName || props.rule.groupId || '-' : props.productName || props.rule.productKey || '-')
const sqlPreview = computed(() => `SELECT ${props.rule.config.fields || '*'}\nFROM ${props.rule.config.appId || '*'}${props.rule.config.condition ? `\nWHERE ${props.rule.config.condition}` : ''}`)
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.rule-review { display: grid; gap: 18px; }
.rule-review__status { display: grid; grid-template-columns: 52px minmax(0, 1fr) auto; gap: 14px; align-items: center; padding: 16px; background: var(--color-fill-1); border: 1px solid var(--color-border-2); }
.rule-review__status > div:nth-child(2) { display: flex; flex-direction: column; gap: 4px; }
.rule-review__status span { font-size: 12px; color: var(--color-text-3); }
.rule-review__icon { display: grid; place-items: center; width: 48px; height: 48px; font-size: 25px; color: rgb(var(--gray-6)); background: var(--color-fill-3); }
.rule-review__icon.is-running { color: rgb(var(--success-6)); background: rgb(var(--success-1)); }
pre { margin: 0; overflow: auto; font-family: Monaco, Menlo, Consolas, monospace; color: rgb(var(--primary-6)); white-space: pre-wrap; }

@media (width <= 576px) { .rule-review__status { grid-template-columns: 44px minmax(0, 1fr); } .rule-review__status :deep(.arco-switch) { grid-column: 1 / -1; } }
</style>
