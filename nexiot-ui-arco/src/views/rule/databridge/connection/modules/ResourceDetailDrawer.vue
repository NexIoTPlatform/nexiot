<template>
  <a-drawer v-model:visible="visible" title="连接详情" width="min(560px, calc(100vw - 24px))" :footer="false">
    <a-descriptions v-if="record" :column="1" bordered>
      <a-descriptions-item label="连接名称">{{ record.name }}</a-descriptions-item>
      <a-descriptions-item label="资源类型"><a-tag :color="getResourceTypeColor(record.type)">{{ record.type }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="插件类型">{{ record.pluginType || '-' }}</a-descriptions-item>
      <a-descriptions-item label="数据方向"><a-tag :color="getDirectionColor(record.dataDirection)">{{ getDirectionLabel(record.dataDirection) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="连接地址">{{ getResourceAddress(record) }}</a-descriptions-item>
      <a-descriptions-item label="用户名">{{ record.username || '-' }}</a-descriptions-item>
      <a-descriptions-item label="密码">{{ record.password ? '********' : '-' }}</a-descriptions-item>
      <a-descriptions-item label="数据库">{{ record.databaseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="record.status === 1 ? 'green' : 'gray'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建信息">{{ record.createBy || '-' }} / {{ record.createTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="描述">{{ record.description || '-' }}</a-descriptions-item>
      <a-descriptions-item label="扩展配置"><pre>{{ sanitizedConfig }}</pre></a-descriptions-item>
    </a-descriptions>
  </a-drawer>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { ResourceConnection } from '../../api'
import { getDirectionColor, getDirectionLabel, getResourceAddress, getResourceTypeColor, parseJsonObject } from '../../shared'

const visible = ref(false)
const record = ref<ResourceConnection>()
const SENSITIVE_CONFIG_KEY_RE = /password|secret|token|accesskey/i
const sanitizedConfig = computed(() => {
  const config = parseJsonObject(record.value?.extraConfig)
  const safe = Object.fromEntries(Object.entries(config).map(([key, value]) => [key, SENSITIVE_CONFIG_KEY_RE.test(key) && value ? '********' : value]))
  return JSON.stringify(safe, null, 2)
})
function open(value: ResourceConnection) { record.value = value; visible.value = true }
defineExpose({ open })
</script>

<style scoped>
/* stylelint-disable declaration-block-single-line-max-declarations */
pre { max-height: 300px; padding: 10px; overflow: auto; font-size: 12px; word-break: break-all; white-space: pre-wrap; background: var(--color-fill-2); }
</style>
