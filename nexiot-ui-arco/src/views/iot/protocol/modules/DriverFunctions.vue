<template>
  <div class="driver-functions">
    <aside class="driver-functions__nav">
      <div class="driver-functions__search">
        <a-input v-model="query" placeholder="搜索函数名、参数或说明" allow-clear size="small"><template #prefix><icon-search /></template></a-input>
        <a-select v-model="group" :options="groups" size="small" aria-label="函数分类" />
        <span>{{ filtered.length }} 个函数 · 按所属类分类</span>
      </div>
      <a-alert v-if="error" type="warning">加载失败。<a-link @click="$emit('reload')">重试</a-link></a-alert>
      <div class="driver-functions__list">
        <button v-for="fn in filtered" :key="fn.key" :class="{ 'driver-functions__entry--active': selected?.key === fn.key }" class="driver-functions__entry" @click="selectedKey = fn.key"><strong>{{ fn.name }}</strong><span>{{ fn.description || fn.group }}</span></button>
        <a-empty v-if="!filtered.length" description="未找到匹配函数" />
      </div>
    </aside>
    <article v-if="selected" class="driver-functions__article">
      <div class="driver-functions__breadcrumb">{{ groupLabel(selected.group) }} <span>{{ selected.group }}</span></div>
      <h2>{{ selected.name }} <a-tag v-if="selected.deprecated" color="orange" size="small">已弃用</a-tag></h2>
      <p>{{ selected.description || '此函数尚未提供说明。' }}</p>
      <pre>{{ selected.signature }} → {{ selected.returnType }}</pre>
      <h3>参数</h3>
      <table v-if="selected.params.length"><thead><tr><th>名称</th><th>类型</th><th>说明</th></tr></thead><tbody><tr v-for="param in selected.params" :key="param.name"><td><code>{{ param.name }}</code></td><td><code>{{ param.type }}</code></td><td>{{ param.description || '—' }}</td></tr></tbody></table>
      <p v-else>无需参数。</p>
      <h3>返回值</h3><code>{{ selected.returnType }}</code>
      <div class="driver-functions__footer"><a-button size="small" type="primary" :disabled="disabled" @click="$emit('insert', selected)"><template #icon><icon-code /></template>插入代码</a-button><span>也可在编辑器输入两个字母，直接选择函数。</span></div>
    </article>
    <a-empty v-else class="driver-functions__empty" description="选择左侧函数查看文档" />
  </div>
</template>

<script setup lang="ts">
import type { MagicFunction } from '../api/debugger'

defineOptions({ name: 'DriverFunctions' })
const props = defineProps<{ functions: MagicFunction[], error: boolean, disabled: boolean }>()
defineEmits<{ (e: 'reload'): void, (e: 'insert', fn: MagicFunction): void }>()
const query = ref('')
const group = ref('all')
const selectedKey = ref('')
const labels: Record<string, string> = { HexFunctions: 'HEX 与 DL/T645', ModbusFunctions: 'Modbus 协议', ProtocolFunctions: '协议校验', ConvertFunctions: '数据转换', TypeCheckFunctions: '类型判断', TempAESFunctions: 'AES 加解密', UnivFunctions: '通用工具', ShadowFunctions: '设备影子', MagicScriptFunctions: '脚本与日志', CollectionFunctions: '集合', StringFunctions: '字符串', MathFunctions: '数学', ArrayFunctions: '数组', AggregationFunctions: '聚合', LinqFunctions: '查询' }
function groupLabel(value: string) {
  return labels[value] || value
}
const groups = computed(() => [{ label: '全部分类', value: 'all' }, ...[...new Set(props.functions.map((fn) => fn.group))].sort().map((value) => ({ value, label: groupLabel(value) }))])
const filtered = computed(() => props.functions.filter((fn) => (group.value === 'all' || fn.group === group.value) && `${fn.name} ${fn.description} ${fn.group} ${groupLabel(fn.group)} ${fn.params.map((p) => `${p.name} ${p.type} ${p.description}`).join(' ')}`.toLowerCase().includes(query.value.trim().toLowerCase())))
const selected = computed(() => filtered.value.find((fn) => fn.key === selectedKey.value) || filtered.value[0])
</script>

<style lang="scss" scoped>
.driver-functions {
  display: flex;
  height: 100%;
  font-size: 12px;

  &__nav {
    display: flex;
    flex: 0 0 270px;
    flex-direction: column;
    min-width: 0;
    min-height: 0;
    border-right: 1px solid var(--color-border-2);
  }

  &__search {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 14px;
    color: var(--color-text-3);
    border-bottom: 1px solid var(--color-border-2);
  }

  &__list {
    flex: 1;
    min-height: 0;
    overflow: auto;
  }

  &__entry {
    display: flex;
    flex-direction: column;
    gap: 5px;
    width: 100%;
    padding: 10px 14px;
    font: inherit;
    color: var(--color-text-1);
    text-align: left;
    cursor: pointer;
    background: transparent;
    border: 0;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__entry strong {
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
    font-weight: 500;
    overflow-wrap: anywhere;
  }

  &__entry span {
    display: block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    color: var(--color-text-3);
    white-space: nowrap;
  }

  &__entry:hover, &__entry--active {
    color: var(--ide-accent);
    background: var(--color-fill-2);
  }

  &__article {
    flex: 1;
    min-width: 0;
    padding: 24px 30px;
    overflow: auto;
  }

  &__breadcrumb {
    color: var(--ide-accent);
  }

  &__breadcrumb span {
    margin-left: 10px;
    color: var(--color-text-3);
  }

  &__article h2 {
    margin: 14px 0;
    font-family: Menlo, Consolas, monospace;
    font-size: 22px;
    overflow-wrap: anywhere;
  }

  &__article h3 {
    margin: 26px 0 12px;
    font-size: 13px;
    font-weight: 500;
  }

  &__article p {
    line-height: 1.8;
  }

  &__article pre {
    padding: 14px;
    line-height: 1.8;
    color: var(--ide-variable);
    overflow-wrap: anywhere;
    white-space: pre-wrap;
    background: var(--ide-toolbar);
    border: 1px solid var(--color-border-2);
    border-radius: 4px;
  }

  &__article table {
    width: 100%;
    border-collapse: collapse;
  }

  &__article th, &__article td {
    padding: 10px;
    line-height: 1.6;
    text-align: left;
    overflow-wrap: anywhere;
    border-bottom: 1px solid var(--color-border-2);
  }

  &__article th {
    font-weight: 500;
    color: var(--color-text-3);
    background: var(--ide-toolbar);
  }

  &__footer {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    margin-top: 30px;
    color: var(--color-text-3);
  }

  &__empty {
    margin: auto;
  }
}

@media (width <= 700px) {
  .driver-functions__nav {
    flex-basis: 180px;
  }

  .driver-functions__article {
    padding: 16px;
  }

  .driver-functions__article h2 {
    font-size: 16px;
  }
}
</style>
