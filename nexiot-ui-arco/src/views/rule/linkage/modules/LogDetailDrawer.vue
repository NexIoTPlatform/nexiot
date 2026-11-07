<template>
  <a-drawer v-model:visible="visible" title="执行日志详情" width="min(860px, calc(100vw - 24px))" :footer="false" unmount-on-close>
    <template v-if="log">
      <a-descriptions :column="{ xs: 1, sm: 2 }" bordered>
        <a-descriptions-item label="场景名称">{{ log.cname || log.sceneName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="执行状态"><a-tag :color="getStatusColor(log.cstatus)">{{ getStatusLabel(log.cstatus) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="触发方式"><a-tag :color="getTriggerColor(log.triggerType)">{{ getTriggerLabel(log.triggerType) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="执行时间">{{ formatDateTime(log.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="业务 ID">{{ log.cid || '-' }}</a-descriptions-item>
        <a-descriptions-item label="执行人">{{ log.createBy || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-tabs default-active-key="actions" class="log-detail__tabs">
        <a-tab-pane key="actions" title="动作结果">
          <a-empty v-if="!actions.length" description="没有动作结果" />
          <div v-else class="log-detail__actions">
            <article v-for="(item, index) in actions" :key="index">
              <header><a-space><a-tag color="arcoblue">{{ getActionLabel(item.trigger) }}</a-tag><strong>{{ item.targetName || item.target || `动作 ${index + 1}` }}</strong></a-space><a-tag :color="item.success ? 'green' : 'red'">{{ item.success ? '成功' : '失败' }}</a-tag></header>
              <div v-if="item.param" class="log-detail__params"><span>参数</span><code>{{ formatResult(item.param) }}</code></div>
              <div class="log-detail__result"><a-button type="text" shape="circle" class="log-detail__copy" @click="copy(formatResult(item.result))"><template #icon><icon-copy /></template></a-button><pre>{{ formatResult(item.result) }}</pre></div>
            </article>
          </div>
        </a-tab-pane>
        <a-tab-pane key="trigger" title="设备触发">
          <a-empty v-if="!deviceMeta" description="本条日志没有设备触发上下文" />
          <template v-else>
            <a-descriptions :column="{ xs: 1, sm: 2 }" bordered>
              <a-descriptions-item label="产品">{{ deviceMeta.productKey || '-' }}</a-descriptions-item>
              <a-descriptions-item label="设备">{{ deviceMeta.deviceName || deviceMeta.deviceId || '-' }}</a-descriptions-item>
              <a-descriptions-item label="消息类型">{{ deviceMeta.messageType || '-' }}</a-descriptions-item>
              <a-descriptions-item label="场景">{{ deviceMeta.sceneName || log.cname || '-' }}</a-descriptions-item>
            </a-descriptions>
            <div v-for="(condition, index) in deviceConditions" :key="index" class="log-detail__condition">
              <strong>{{ condition.deviceName || condition.deviceId || `条件 ${index + 1}` }}</strong>
              <div v-for="filter in condition.filters || []" :key="filter.key" class="log-detail__filter">
                <span>{{ filter.key }} {{ getOperatorLabel(filter.operator) }} {{ filter.value }}</span>
                <a-tag :color="filterMatched(filter, condition) ? 'green' : 'red'">触发值 {{ formatResult(actualValue(filter, condition)) }} / {{ filterMatched(filter, condition) ? '满足' : '不满足' }}</a-tag>
              </div>
            </div>
          </template>
        </a-tab-pane>
        <a-tab-pane key="raw" title="原始上下文"><pre class="log-detail__raw">{{ rawContext }}</pre></a-tab-pane>
      </a-tabs>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { SceneAction, SceneFilter, SceneLog, SceneTrigger } from '../api'
import { Message } from '@arco-design/web-vue'
import { copyText } from '@/utils'
import { formatDateTime, formatResult, getActionLabel, getOperatorLabel, getStatusColor, getStatusLabel, getTriggerColor, getTriggerLabel, isFilterMatched, parseDeviceMeta, parseList, parseLogActions, triggerFieldValue } from '../shared'

const visible = ref(false)
const log = ref<SceneLog>()
const actions = computed(() => parseLogActions(log.value))
const deviceMeta = computed(() => parseDeviceMeta(log.value?.cdeviceMeta))
const deviceConditions = computed(() => deviceMeta.value?.triggerConditions || [])
const rawContext = computed(() => formatResult({ deviceMeta: deviceMeta.value, triggerCondition: parseList<SceneTrigger>(log.value?.triggerCondition), execAction: parseList<SceneAction>(log.value?.execAction) }))
function actualValue(filter: SceneFilter, trigger: SceneTrigger) { return triggerFieldValue(deviceMeta.value?.actualDeviceData || {}, trigger, filter.key) }
function filterMatched(filter: SceneFilter, trigger: SceneTrigger) { return isFilterMatched(filter, actualValue(filter, trigger)) }
async function copy(text: string) {
  try { await copyText(text); Message.success('已复制') } catch { Message.error('复制失败') }
}
function open(value: SceneLog) { log.value = value; visible.value = true }
defineExpose({ open })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.log-detail__tabs { margin-top: 18px; }
.log-detail__actions { display: grid; gap: 12px; }
.log-detail__actions article { padding: 12px; border: 1px solid var(--color-border-2); }
.log-detail__actions header { display: flex; gap: 10px; align-items: center; justify-content: space-between; }
.log-detail__params { display: grid; grid-template-columns: 60px 1fr; gap: 8px; margin-top: 10px; color: var(--color-text-3); }
.log-detail__params code { color: var(--color-text-2); word-break: break-all; white-space: pre-wrap; }
.log-detail__result { position: relative; margin-top: 10px; }
.log-detail__result pre, .log-detail__raw { max-height: 420px; padding: 12px; margin: 0; overflow: auto; font-family: Monaco, Menlo, Consolas, monospace; word-break: break-all; white-space: pre-wrap; background: var(--color-fill-2); }
.log-detail__copy { position: absolute; top: 4px; right: 4px; z-index: 1; }
.log-detail__condition { display: grid; gap: 8px; padding: 12px; margin-top: 12px; border: 1px solid var(--color-border-2); }
.log-detail__filter { display: flex; gap: 8px; align-items: center; justify-content: space-between; }

@media (width <= 576px) { .log-detail__actions header, .log-detail__filter { flex-direction: column; align-items: flex-start; } }
</style>
