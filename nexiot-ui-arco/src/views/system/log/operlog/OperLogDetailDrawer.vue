<template>
  <a-drawer v-model:visible="visible" title="操作日志详情" width="680px" :footer="false">
    <a-descriptions :column="2" bordered size="large">
      <a-descriptions-item label="操作模块">{{ data.title || '-' }}</a-descriptions-item>
      <a-descriptions-item label="操作状态">
        <a-tag :color="data.status === 0 ? 'green' : 'red'">{{ data.status === 0 ? '正常' : '异常' }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="操作人员">{{ data.operName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="部门">{{ data.deptName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="请求方式">{{ data.requestMethod || '-' }}</a-descriptions-item>
      <a-descriptions-item label="操作 IP">{{ data.operIp || '-' }}</a-descriptions-item>
      <a-descriptions-item label="操作地点" :span="2">{{ data.operLocation || '-' }}</a-descriptions-item>
      <a-descriptions-item label="请求地址" :span="2">{{ data.operUrl || '-' }}</a-descriptions-item>
      <a-descriptions-item label="操作方法" :span="2">{{ data.method || '-' }}</a-descriptions-item>
      <a-descriptions-item label="请求参数" :span="2"><pre class="oper-log-detail__code">{{ formatContent(data.operParam) }}</pre></a-descriptions-item>
      <a-descriptions-item label="返回参数" :span="2"><pre class="oper-log-detail__code">{{ formatContent(data.jsonResult) }}</pre></a-descriptions-item>
      <a-descriptions-item v-if="data.errorMsg" label="异常信息" :span="2"><pre class="oper-log-detail__error">{{ data.errorMsg }}</pre></a-descriptions-item>
      <a-descriptions-item label="操作时间" :span="2">{{ data.operTime || '-' }}</a-descriptions-item>
    </a-descriptions>
  </a-drawer>
</template>

<script setup lang="ts">
import type { OperLogItem } from '@/apis/system/log'

defineOptions({ name: 'OperLogDetailDrawer' })

const visible = ref(false)
const data = ref<Partial<OperLogItem>>({})

function formatContent(value?: string) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function open(record: OperLogItem) {
  data.value = record
  visible.value = true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.oper-log-detail {
  &__code,
  &__error {
    max-height: 240px;
    margin: 0;
    overflow: auto;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    line-height: 1.6;
    word-break: break-all;
    white-space: pre-wrap;
  }

  &__error {
    color: rgb(var(--red-6));
  }
}
</style>
