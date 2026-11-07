<template>
  <section class="target-editor">
    <a-row justify="space-between" align="center">
      <a-space><strong>转发目标</strong><a-tag>{{ model.length }}/10</a-tag></a-space>
      <a-button type="primary" :disabled="model.length >= 10" @click="openForm()"><template #icon><icon-plus /></template>添加目标</a-button>
    </a-row>
    <a-alert>HTTP 目标使用 POST 请求发送 `application/json` 数据，最多配置 10 个。</a-alert>
    <div v-if="model.length" class="target-list">
      <div v-for="item in model" :key="item.id" class="target-list__item">
        <div><a-tag color="arcoblue">HTTP POST</a-tag><strong>{{ item.url }}</strong></div>
        <a-space size="mini">
          <a-tooltip content="连通性测试"><a-button type="text" shape="circle" @click="openTest(item)"><template #icon><icon-thunderbolt /></template></a-button></a-tooltip>
          <a-tooltip content="编辑"><a-button type="text" shape="circle" @click="openForm(item)"><template #icon><icon-edit /></template></a-button></a-tooltip>
          <a-popconfirm content="确定删除该转发目标吗？" @ok="remove(item.id)"><a-tooltip content="删除"><a-button type="text" status="danger" shape="circle"><template #icon><icon-delete /></template></a-button></a-tooltip></a-popconfirm>
        </a-space>
      </div>
    </div>
    <a-empty v-else description="尚未配置转发目标" />

    <a-modal v-model:visible="formVisible" :title="targetForm.id ? '编辑转发目标' : '添加转发目标'" width="min(560px, calc(100vw - 32px))" align-center :mask-closable="false" @before-ok="saveTarget">
      <a-form :model="targetForm" layout="vertical">
        <a-form-item label="目标类型"><a-select v-model="targetForm.type" :options="[{ label: '第三方应用服务（HTTP 推送）', value: 'http' }]" /></a-form-item>
        <a-form-item label="推送 URL" required extra="必须是可访问的 HTTP 或 HTTPS 地址。"><a-input v-model.trim="targetForm.url" placeholder="https://example.com/webhook" /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:visible="testVisible" title="目标连通性测试" width="min(620px, calc(100vw - 24px))" :footer="false">
      <a-form :model="{ payload: testPayload }" layout="vertical">
        <a-form-item label="目标地址"><a-input :model-value="testingTarget?.url" readonly /></a-form-item>
        <a-form-item label="测试 JSON"><a-textarea v-model="testPayload" class="target-editor__code" :auto-size="{ minRows: 10, maxRows: 16 }" /></a-form-item>
        <a-form-item label="测试结果"><pre class="target-editor__result">{{ testResult || '尚未执行测试' }}</pre></a-form-item>
      </a-form>
      <a-button type="primary" :loading="testing" @click="runTest"><template #icon><icon-thunderbolt /></template>执行测试</a-button>
    </a-drawer>
  </section>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { RuleTarget } from '../api'
import { Message } from '@arco-design/web-vue'
import { createUuid } from '@/utils'
import { testForwardTarget } from '../api'

const model = defineModel<RuleTarget[]>({ required: true })
const formVisible = ref(false)
const testVisible = ref(false)
const testing = ref(false)
const testingTarget = ref<RuleTarget>()
const testPayload = ref('{\n  "data": "连接测试"\n}')
const testResult = ref('')
const targetForm = reactive<RuleTarget>({ id: '', type: 'http', url: '' })
function createId() { return createUuid() }
function openForm(item?: RuleTarget) { Object.assign(targetForm, item || { id: '', type: 'http', url: '' }); formVisible.value = true }
function isValidUrl(value: string) { try { const url = new URL(value); return ['http:', 'https:'].includes(url.protocol) } catch { return false } }
function saveTarget(done: (closed: boolean) => void) {
  if (!targetForm.url || !isValidUrl(targetForm.url)) { Message.warning('请输入正确的 HTTP/HTTPS 地址'); done(false); return }
  const target = { ...targetForm, id: targetForm.id || createId() }
  const index = model.value.findIndex((item) => item.id === target.id)
  if (index >= 0) model.value.splice(index, 1, target)
  else model.value.push(target)
  done(true)
}
function remove(id: string) { model.value = model.value.filter((item) => item.id !== id) }
function openTest(item: RuleTarget) { testingTarget.value = item; testResult.value = ''; testVisible.value = true }
async function runTest() {
  if (!testingTarget.value) return
  try { JSON.parse(testPayload.value) } catch { Message.warning('测试 JSON 格式错误'); return }
  testing.value = true
  try {
    const res = await testForwardTarget({ param: testPayload.value, ruleTarget: testingTarget.value })
    testResult.value = res.message || JSON.stringify(res.data, null, 2) || '测试成功'
  } catch (error) { testResult.value = error instanceof Error ? error.message : '测试失败' } finally { testing.value = false }
}
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.target-editor { display: grid; gap: 14px; }
.target-list { border: 1px solid var(--color-border-2); }
.target-list__item { display: flex; gap: 12px; align-items: center; justify-content: space-between; min-height: 52px; padding: 8px 12px; border-bottom: 1px solid var(--color-border-2); }
.target-list__item:last-child { border-bottom: 0; }
.target-list__item > div { display: flex; gap: 8px; align-items: center; min-width: 0; }
.target-list__item strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.target-editor__code, .target-editor__result { font-family: Monaco, Menlo, Consolas, monospace; }
.target-editor__result { min-height: 120px; max-height: 300px; padding: 12px; overflow: auto; word-break: break-all; white-space: pre-wrap; background: var(--color-fill-2); }

@media (width <= 576px) { .target-list__item { flex-direction: column; align-items: flex-start; } }
</style>
