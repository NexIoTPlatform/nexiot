<template>
  <a-spin :loading="loading">
    <a-alert type="info" class="transform-config__tip">
      规则可先保存为草稿并在线预览，只有发布后的规则会参与真实推送。产品级规则优先于全局规则。
    </a-alert>
    <a-tabs v-model:active-key="activeTab" type="line">
      <a-tab-pane key="rules" title="规则列表">
        <a-row justify="space-between" class="g-row-tool">
          <a-space>
            <a-button v-hasPerm="['application:application:transform:add']" type="primary" @click="openRule()"><template #icon><icon-plus /></template>创建规则</a-button>
            <a-button v-hasPerm="['application:application:transform:query']" @click="load"><template #icon><icon-refresh /></template>刷新</a-button>
          </a-space>
          <span class="transform-config__muted">共 {{ rules.length }} 条规则</span>
        </a-row>
        <a-table
          row-key="id"
          :data="rules"
          :columns="columns"
          :pagination="false"
          :bordered="{ cell: true }"
          :scroll="{ x: '100%', minWidth: 1120 }"
        />
      </a-tab-pane>
      <a-tab-pane key="config" title="应用配置">
        <a-form :model="config" layout="vertical" class="transform-config__form">
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="20" :row-gap="8">
            <a-form-item label="启用数据转换">
              <a-switch v-model="config.enable" />
              <div class="transform-config__help">关闭后直接推送平台原始报文。</div>
            </a-form-item>
            <a-form-item label="转换失败时">
              <a-select v-model="config.fallbackMode" :options="fallbackOptions" />
              <div class="transform-config__help">模板、脚本或内置格式执行失败时使用。</div>
            </a-form-item>
          </a-grid>
          <a-form-item label="默认全局规则">
            <a-select
              v-model="config.defaultRuleId"
              :options="publishedGlobalOptions"
              allow-clear
              placeholder="留空时自动使用最近发布的全局规则"
            />
          </a-form-item>
          <a-button v-hasPerm="['application:application:transform:config']" type="primary" :loading="configSaving" @click="saveConfig">
            <template #icon><icon-save /></template>保存应用配置
          </a-button>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="presets" title="内置格式">
        <a-row :gutter="16">
          <a-col v-for="preset in sortedPresets" :key="preset.code" :xs="24" :md="12" :xl="8">
            <a-card class="transform-config__preset" :title="preset.name">
              <template #extra>
                <a-tag :color="preset.implemented === false ? 'gray' : 'green'">
                  {{ preset.implemented === false ? "预留扩展" : "可用" }}
                </a-tag>
              </template>
              <a-typography-paragraph code copyable>{{ preset.code }}</a-typography-paragraph>
              <p>{{ preset.description || "暂无说明" }}</p>
              <a-button v-hasPerm="['application:application:transform:add']" type="primary" size="small" :disabled="preset.implemented === false" @click="openRule(undefined, preset.code)">
                使用此格式
              </a-button>
            </a-card>
          </a-col>
        </a-row>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:visible="ruleVisible"
      :title="editing.id ? '编辑转换规则' : '创建转换规则'"
      width="min(1040px, calc(100vw - 32px))"
      :body-style="ruleModalBodyStyle"
      :mask-closable="false"
      align-center
      unmount-on-close
    >
      <a-steps :current="ruleStep" small class="transform-config__steps">
        <a-step description="范围与名称">基础信息</a-step>
        <a-step description="格式与内容">转换内容</a-step>
        <a-step description="验证输出">在线预览</a-step>
      </a-steps>

      <a-form :model="editing" layout="vertical">
        <div v-show="ruleStep === 1">
          <a-form-item label="规则名称" required>
            <a-input v-model.trim="editing.ruleName" :max-length="80" placeholder="例如：客户 A 属性上报格式" />
          </a-form-item>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="20">
            <a-form-item label="作用范围" required>
              <a-radio-group v-model="editing.scopeType" type="button">
                <a-radio value="GLOBAL">整个应用</a-radio>
                <a-radio value="PRODUCT">指定产品</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item v-if="editing.scopeType === 'PRODUCT'" label="产品" required>
              <a-select v-model="editing.productKey" :options="productOptions" allow-search placeholder="选择 ProductKey" />
            </a-form-item>
          </a-grid>
        </div>

        <div v-show="ruleStep === 2">
          <a-form-item label="规则类型" required>
            <a-radio-group v-model="editing.ruleType" type="button" @change="changeRuleType">
              <a-radio value="PRESET">内置格式</a-radio>
              <a-radio value="TEMPLATE">JSON 模板</a-radio>
              <a-radio value="SCRIPT">脚本</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-if="editing.ruleType === 'PRESET'" label="目标平台格式" required>
            <a-select v-model="editing.presetCode" :options="presetOptions" />
          </a-form-item>
          <a-form-item v-if="editing.ruleType === 'PRESET'" label="附加配置（JSON）">
            <template #extra><a-button type="text" size="mini" @click="formatJson('options')">格式化 JSON</a-button></template>
            <a-textarea v-model="editing.options" class="transform-config__editor" :auto-size="{ minRows: 6, maxRows: 14 }" />
          </a-form-item>
          <a-form-item v-else :label="editing.ruleType === 'SCRIPT' ? '脚本内容' : '模板内容'" required>
            <template #extra><a-button type="text" size="mini" @click="fillExample">填充示例</a-button></template>
            <a-textarea v-model="editing.content" class="transform-config__editor" :auto-size="{ minRows: 12, maxRows: 24 }" />
          </a-form-item>
        </div>

        <div v-show="ruleStep === 3">
          <a-row justify="space-between" class="g-row-tool">
            <a-select v-model="sampleKey" :options="sampleOptions" style="width: 220px" @change="applySample" />
            <a-space>
              <a-button @click="formatJson('sampleInput')">格式化输入</a-button>
              <a-button type="primary" :loading="previewLoading" @click="preview"><template #icon><IconPlayArrow /></template>执行预览</a-button>
            </a-space>
          </a-row>
          <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
            <a-form-item label="输入样例">
              <a-textarea v-model="editing.sampleInput" class="transform-config__editor" :auto-size="{ minRows: 14, maxRows: 24 }" />
            </a-form-item>
            <a-form-item label="转换后报文">
              <a-textarea v-model="previewResult.outputJson" readonly class="transform-config__editor" :auto-size="{ minRows: 14, maxRows: 24 }" />
            </a-form-item>
          </a-grid>
          <a-alert :type="previewResult.success === false ? 'error' : 'info'">{{ previewResult.message || "等待执行预览" }}</a-alert>
        </div>
      </a-form>

      <template #footer>
        <a-row justify="space-between" class="transform-config__footer">
          <a-space wrap>
            <a-button v-if="ruleStep > 1" @click="ruleStep--">上一步</a-button>
            <a-button v-if="ruleStep < 3" type="primary" @click="nextStep">下一步</a-button>
          </a-space>
          <a-space wrap>
            <a-button @click="ruleVisible = false">取消</a-button>
            <a-button v-if="canSaveRule" :loading="saving" @click="save(false)">保存草稿</a-button>
            <a-button v-if="ruleStep === 3 && hasPerm('application:application:transform:publish')" type="primary" :loading="publishing" @click="save(true)">保存并发布</a-button>
          </a-space>
        </a-row>
      </template>
    </a-modal>
  </a-spin>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TransformOverview, TransformPreview, TransformRule } from '@/apis/application/application'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconPlayArrow } from '@arco-design/web-vue/es/icon'
import {
  addTransformRule,
  deleteTransformRule,
  getTransformOverview,
  getTransformRule,
  previewTransform,
  publishTransformRule,
  updateTransformConfig,
  updateTransformRule
} from '@/apis/application/application'
import { getProductList } from '@/apis/iot/product'
import { hasPerm } from '@/utils/permission'

defineOptions({ name: 'ApplicationTransformConfig' })
const props = defineProps<{ appUniqueId: string }>()
const activeTab = ref('rules')
const loading = ref(false)
const configSaving = ref(false)
const saving = ref(false)
const publishing = ref(false)
const previewLoading = ref(false)
const ruleModalBodyStyle = { maxHeight: 'min(76vh, 760px)', overflowY: 'auto' as const }
const rules = ref<TransformRule[]>([])
const presets = ref<NonNullable<TransformOverview['presets']>>([])
const config = reactive<NonNullable<TransformOverview['config']>>({ enable: false, support: true, fallbackMode: 'ORIGINAL' })
const ruleVisible = ref(false)
const ruleStep = ref(1)
const sampleKey = ref('properties')
const previewResult = reactive<TransformPreview>({ message: '等待执行预览', outputJson: '' })
const productOptions = ref<{ label: string, value: string }[]>([])
const fallbackOptions = [{ label: '继续推送原始报文', value: 'ORIGINAL' }, { label: '阻断本次推送', value: 'BLOCK' }]
const DEFAULT_TEMPLATE = `{
  "deviceId": "\${deviceId}",
  "productKey": "\${productKey}",
  "messageType": "\${messageType}",
  "timestamp": "\${time}",
  "payload": \${properties}
}`
const DEFAULT_SCRIPT = `var transform = (ctx, source, request, requestMap) => {
  return {
    deviceId: ctx.deviceId,
    productKey: ctx.productKey,
    messageType: ctx.messageType,
    timestamp: ctx.time,
    body: ctx.properties || ctx.data || {}
  };
};
return transform(ctx, source, request, requestMap);`
const samples = {
  properties: JSON.stringify({ messageType: 'PROPERTIES', properties: { temperature: 23.5, humidity: 56 }, time: Date.now(), productKey: 'demoProductKey', deviceId: 'demoDevice01', deviceName: '演示设备' }, null, 2),
  event: JSON.stringify({ messageType: 'EVENT', event: 'alarm', data: { level: 2, text: '温度过高' }, time: Date.now(), productKey: 'demoProductKey', deviceId: 'demoDevice01' }, null, 2)
}
const sampleOptions = [{ label: '属性上报样例', value: 'properties' }, { label: '设备事件样例', value: 'event' }]
const editing = reactive<TransformRule>(defaultRule())

function defaultRule(): TransformRule {
  return { appUniqueId: props.appUniqueId, ruleName: '', scopeType: 'GLOBAL', productKey: '', ruleType: 'PRESET', presetCode: '', options: '{}', content: '', sampleInput: samples.properties, sampleOutput: '', status: 0, version: 1 }
}

const sortedPresets = computed(() => [...presets.value].sort((a, b) => Number(b.implemented !== false) - Number(a.implemented !== false)))
const presetOptions = computed(() => sortedPresets.value.map((item) => ({ label: `${item.name} (${item.code})`, value: item.code, disabled: item.implemented === false })))
const publishedGlobalOptions = computed(() => rules.value.filter((item) => item.scopeType === 'GLOBAL' && Number(item.status) === 1).map((item) => ({ label: `${item.ruleName} (v${item.version || 1})`, value: item.id })))
const canSaveRule = computed(() => hasPerm(editing.id ? 'application:application:transform:edit' : 'application:application:transform:add'))

async function load() {
  if (!props.appUniqueId) return
  if (!hasPerm('application:application:transform:query')) return
  loading.value = true
  try {
    const data = (await getTransformOverview(props.appUniqueId)).data || {}
    rules.value = data.rules || []
    presets.value = data.presets || []
    Object.assign(config, { enable: false, support: true, defaultRuleId: undefined, fallbackMode: 'ORIGINAL', ...(data.config || {}) })
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  configSaving.value = true
  try {
    await updateTransformConfig(props.appUniqueId, { ...config, support: true })
    Message.success('转换配置已保存并刷新集群缓存')
    await load()
  } finally {
    configSaving.value = false
  }
}

async function openRule(rule?: TransformRule, presetCode = '') {
  const detail = rule?.id ? (await getTransformRule(rule.id)).data : defaultRule()
  Object.assign(editing, defaultRule(), detail, { appUniqueId: props.appUniqueId })
  if (!rule?.id) editing.presetCode = presetCode || sortedPresets.value.find((item) => item.implemented !== false)?.code || ''
  sampleKey.value = 'properties'
  Object.assign(previewResult, { success: undefined, message: editing.sampleOutput ? '已载入上次调试输出' : '等待执行预览', outputJson: editing.sampleOutput || '' })
  ruleStep.value = rule?.id || presetCode ? 2 : 1
  ruleVisible.value = true
}

function changeRuleType(value: unknown) {
  value = String(value)
  if (value === 'PRESET' && !editing.presetCode) editing.presetCode = sortedPresets.value.find((item) => item.implemented !== false)?.code || ''
  if (value === 'TEMPLATE' && !editing.content) editing.content = DEFAULT_TEMPLATE
  if (value === 'SCRIPT' && !editing.content) editing.content = DEFAULT_SCRIPT
}

function fillExample() {
  editing.content = editing.ruleType === 'SCRIPT' ? DEFAULT_SCRIPT : DEFAULT_TEMPLATE
}

function formatJson(field: 'options' | 'sampleInput') {
  try {
    editing[field] = JSON.stringify(JSON.parse(editing[field] || '{}'), null, 2)
  } catch {
    Message.warning('JSON 格式不正确')
  }
}

function applySample(value: unknown) {
  editing.sampleInput = samples[String(value) as keyof typeof samples]
}

function validate(step = ruleStep.value) {
  if (!editing.ruleName) return '请输入规则名称'
  if (editing.scopeType === 'PRODUCT' && !editing.productKey) return '请选择产品'
  if (step >= 2 && editing.ruleType === 'PRESET' && !editing.presetCode) return '请选择内置格式'
  if (step >= 2 && editing.ruleType !== 'PRESET' && !editing.content) return '请输入转换内容'
  if (step >= 3 && !editing.sampleInput) return '请输入调试样例'
  return ''
}

function nextStep() {
  const message = validate()
  if (message) return Message.warning(message)
  ruleStep.value++
}

async function preview() {
  const message = validate(3)
  if (message) return Message.warning(message)
  previewLoading.value = true
  try {
    const data = (await previewTransform({ appUniqueId: props.appUniqueId, ruleType: editing.ruleType, presetCode: editing.ruleType === 'PRESET' ? editing.presetCode : '', content: editing.ruleType === 'PRESET' ? '' : editing.content, options: editing.options || '{}', sampleInput: editing.sampleInput || '' })).data
    Object.assign(previewResult, data)
    editing.sampleOutput = data.outputJson || ''
    Message.success('预览完成')
  } finally {
    previewLoading.value = false
  }
}

function payload(): TransformRule {
  return { ...editing, appUniqueId: props.appUniqueId, productKey: editing.scopeType === 'PRODUCT' ? editing.productKey : '', presetCode: editing.ruleType === 'PRESET' ? editing.presetCode : '', content: editing.ruleType === 'PRESET' ? '' : editing.content, options: editing.options || '{}', sampleOutput: editing.sampleOutput || previewResult.outputJson || '' }
}

async function save(publish: boolean) {
  const message = validate(publish ? 3 : 2)
  if (message) return Message.warning(message)
  const state = publish ? publishing : saving
  state.value = true
  try {
    const saved = await (editing.id ? updateTransformRule(payload()) : addTransformRule(payload()))
    if (publish && saved.data.id) await publishTransformRule(saved.data.id)
    Message.success(publish ? '规则已保存并发布' : '规则草稿已保存')
    ruleVisible.value = false
    await load()
  } finally {
    state.value = false
  }
}

async function publishRule(rule: TransformRule) {
  if (!rule.id) return false
  await publishTransformRule(rule.id)
  Message.success('规则已发布')
  await load()
  return true
}

async function removeRule(rule: TransformRule) {
  if (!rule.id) return false
  await deleteTransformRule(String(rule.id))
  Message.success('规则已删除')
  await load()
  return true
}

const columns: TableColumnData[] = [
  { title: '规则名称', dataIndex: 'ruleName', width: 200, ellipsis: true, tooltip: true },
  { title: '范围', width: 110, render: ({ record }) => <Tag color={record.scopeType === 'PRODUCT' ? 'orange' : 'blue'}>{record.scopeType === 'PRODUCT' ? '指定产品' : '整个应用'}</Tag> },
  { title: 'ProductKey', dataIndex: 'productKey', width: 170, ellipsis: true, tooltip: true },
  { title: '类型', dataIndex: 'ruleType', width: 100 },
  { title: '内置格式', dataIndex: 'presetCode', width: 160, ellipsis: true, tooltip: true },
  { title: '状态', width: 90, render: ({ record }) => <Tag color={Number(record.status) === 1 ? 'green' : 'gray'}>{Number(record.status) === 1 ? '已发布' : '草稿'}</Tag> },
  { title: '版本', dataIndex: 'version', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', width: 140, fixed: 'right', render: ({ record }) => (
    <Space>
      {hasPerm('application:application:transform:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" onClick={() => openRule(record as TransformRule)}><IconEdit /></Button></Tooltip>}
      {Number(record.status) !== 1 && hasPerm('application:application:transform:publish') && <Popconfirm content="发布后将立即生效，确定继续吗？" onBeforeOk={() => publishRule(record as TransformRule)}><Tooltip content="发布"><Button type="text" status="success" shape="circle"><IconPlayArrow /></Button></Tooltip></Popconfirm>}
      {hasPerm('application:application:transform:remove') && <Popconfirm content="确定删除该规则吗？" onBeforeOk={() => removeRule(record as TransformRule)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]

async function loadProducts() {
  const res = await getProductList({ page: 1, size: 100, state: 0 })
  productOptions.value = (res.data.records || []).map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey }))
}

watch(() => props.appUniqueId, (value) => value && load(), { immediate: true })
onMounted(loadProducts)
defineExpose({ load })
</script>

<style lang="scss" scoped>
.transform-config {
  &__tip {
    margin-bottom: 12px;
  }

  &__muted,
  &__help {
    color: var(--color-text-3);
  }

  &__help {
    margin-top: 8px;
    font-size: 12px;
  }

  &__form {
    max-width: 760px;
    padding-top: 12px;
  }

  &__preset {
    min-height: 210px;
    margin-bottom: 16px;
  }

  &__steps {
    margin: 4px 0 24px;
  }

  &__footer {
    width: 100%;
  }

  &__editor :deep(textarea) {
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
    line-height: 1.6;
  }
}
</style>
