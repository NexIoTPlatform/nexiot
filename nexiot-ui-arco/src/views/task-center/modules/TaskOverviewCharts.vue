<template>
  <section class="task-charts">
    <div>
      <header><strong>执行状态</strong><span>当前页 {{ rows.length }} 条记录</span></header>
      <VCharts :option="statusOption" autoresize />
    </div>
    <div>
      <header><strong>任务构成</strong><span>按任务类型统计</span></header>
      <VCharts :option="typeOption" autoresize />
    </div>
  </section>
</template>

<script setup lang="ts">
import type { TaskCenterItem } from '../api'
import { useMediaQuery } from '@vueuse/core'
import VCharts from 'vue-echarts'
import { taskStatusLabel, taskTypeLabel } from '../shared'

const props = defineProps<{ rows: TaskCenterItem[] }>()
const compact = useMediaQuery('(max-width: 768px)')

function countBy(values: string[]) {
  return values.reduce<Record<string, number>>((result, value) => {
    result[value] = (result[value] || 0) + 1
    return result
  }, {})
}

const statusColors: Record<string, string> = {
  待执行: '#86909c',
  进行中: '#165dff',
  已完成: '#00b42a',
  部分成功: '#ff7d00',
  失败: '#f53f3f',
  已取消: '#c9cdd4'
}

const typeColors: Record<string, string> = {
  设备作业: '#165dff',
  设备导入: '#00b42a',
  三元组生成: '#ff7d00'
}

const statusOption = computed(() => {
  const entries = Object.entries(countBy(props.rows.map((item) => taskStatusLabel(item.status))))
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
    series: [{
      type: 'pie',
      radius: compact.value ? ['38%', '65%'] : ['42%', '68%'],
      center: ['50%', '45%'],
      label: { show: !compact.value, formatter: '{b}\n{c}' },
      data: entries.map(([name, value]) => ({ name, value, itemStyle: { color: statusColors[name] || '#86909c' } }))
    }]
  }
})

const typeOption = computed(() => {
  const entries = Object.entries(countBy(props.rows.map((item) => taskTypeLabel(item.taskType))))
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 18, top: 20, bottom: 48 },
    xAxis: { type: 'category', data: entries.map(([name]) => name), axisLabel: { interval: 0 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e5e6eb', type: 'dashed' } } },
    series: [{
      type: 'bar',
      barMaxWidth: 42,
      data: entries.map(([name, value]) => ({ value, itemStyle: { color: typeColors[name] || '#86909c', borderRadius: [4, 4, 0, 0] } }))
    }]
  }
})
</script>

<style scoped lang="scss">
.task-charts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.task-charts > div {
  min-width: 0;
  padding: 14px 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-1);
  border-radius: 6px;
}

.task-charts header {
  display: flex;
  gap: 8px;
  align-items: baseline;
  justify-content: space-between;
}

.task-charts header strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
}

.task-charts header span {
  font-size: 12px;
  color: var(--color-text-3);
}

.task-charts :deep(.echarts) {
  width: 100%;
  height: 210px;
}

@media (width <= 768px) {
  .task-charts {
    grid-template-columns: 1fr;
  }
}
</style>
