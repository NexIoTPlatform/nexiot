<template>
  <a-drawer v-model:visible="visible" :title="form.id ? '编辑数据转发规则' : '新增数据转发规则'" width="min(920px, calc(100vw - 24px))" :mask-closable="false" :footer="false" unmount-on-close>
    <a-steps :current="step" small class="rule-form__steps">
      <a-step title="设置转发数据" />
      <a-step title="设置转发目标" />
      <a-step title="确认并启动" />
    </a-steps>
    <a-form ref="formRef" :model="form" layout="vertical">
      <section v-show="step === 1" class="rule-form__section">
        <a-alert>规则引擎使用 SQL 从应用消息中筛选字段与条件，再把结果分发到配置的 HTTP 目标。</a-alert>
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <a-form-item field="ruleName" label="规则名称" :rules="[{ required: true, message: '请输入规则名称' }]"><a-input v-model.trim="form.ruleName" :max-length="100" show-word-limit /></a-form-item>
          <a-form-item field="dataLevel" label="数据级别" :rules="[{ required: true, message: '请选择数据级别' }]"><a-select v-model="form.dataLevel" :options="levelOptions" @change="handleLevelChange" /></a-form-item>
          <a-form-item v-if="form.dataLevel === 'product' || form.dataLevel === 'device'" label="产品" required><a-select v-model="form.productKey" :options="productOptions" allow-search @change="handleProductChange" /></a-form-item>
          <a-form-item v-if="form.dataLevel === 'group'" label="设备群组" required><a-tree-select v-model="form.groupId" :data="groupOptions" allow-search /></a-form-item>
          <a-form-item v-if="form.dataLevel === 'device' && form.productKey" label="设备" required><a-select v-model="form.relationIds" :options="deviceOptions" multiple allow-search :loading="deviceLoading" :max-tag-count="3" /></a-form-item>
        </a-grid>
        <a-form-item label="描述"><a-textarea v-model="form.description" :max-length="255" :auto-size="{ minRows: 2, maxRows: 4 }" show-word-limit /></a-form-item>
        <a-divider orientation="left">数据过滤 SQL</a-divider>
        <div class="rule-form__sql-grid">
          <a-form-item label="SELECT" required><a-textarea v-model="form.config.fields" :auto-size="{ minRows: 3, maxRows: 6 }" placeholder="* 或 properties.temperature AS temperature" /></a-form-item>
          <a-form-item label="FROM" required><a-select v-model="form.config.appId" :options="applicationOptions" allow-search /></a-form-item>
          <a-form-item label="WHERE"><a-textarea v-model="form.config.condition" :auto-size="{ minRows: 3, maxRows: 6 }" placeholder="可选，例如 properties.temperature > 30" /></a-form-item>
          <pre class="rule-form__sql-preview">{{ sqlPreview }}</pre>
        </div>
        <a-collapse :default-active-key="[]">
          <a-collapse-item key="test" header="SQL 测试">
            <a-form-item label="测试 Payload"><a-textarea v-model="form.payload" class="rule-form__code" :auto-size="{ minRows: 12, maxRows: 18 }" /></a-form-item>
            <a-button type="primary" :loading="testingSql" @click="runSqlTest"><template #icon><icon-play-arrow /></template>执行 SQL 测试</a-button>
            <pre v-if="sqlResult" class="rule-form__test-result">{{ sqlResult }}</pre>
          </a-collapse-item>
        </a-collapse>
      </section>
      <section v-show="step === 2" class="rule-form__section"><RuleTargetEditor v-model="form.config.targets" /></section>
      <section v-show="step === 3" class="rule-form__section"><RuleReview :rule="form" :product-name="selectedProductName" :group-name="selectedGroupName" @status-change="form.status = $event" /></section>
    </a-form>
    <div class="rule-form__footer">
      <a-button @click="visible = false">取消</a-button>
      <a-space>
        <a-button v-if="step > 1" @click="step--"><template #icon><icon-left /></template>上一步</a-button>
        <a-button v-if="step < 3" type="primary" @click="next"><template #icon><icon-right /></template>下一步</a-button>
        <a-button v-else type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存规则</a-button>
      </a-space>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { FormInstance, TreeNodeData } from '@arco-design/web-vue'
import type { ForwardRule } from '../api'
import type { ApplicationItem } from '@/apis/application/application'
import type { DeviceGroupItem, DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { addForwardRule, getForwardApplications, getForwardDevices, getForwardGroups, getForwardProducts, getForwardRule, testForwardSql, updateForwardRule, updateForwardRuleTargets } from '../api'
import RuleReview from './RuleReview.vue'
import RuleTargetEditor from './RuleTargetEditor.vue'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const visible = ref(false)
const step = ref(1)
const saving = ref(false)
const testingSql = ref(false)
const deviceLoading = ref(false)
const sqlResult = ref('')
const formRef = useTemplateRef<FormInstance>('formRef')
const products = ref<ProductItem[]>([])
const groups = ref<DeviceGroupItem[]>([])
const devices = ref<DeviceItem[]>([])
const applications = ref<ApplicationItem[]>([])
const form = reactive<ForwardRule>(createDefault())
const levelOptions = [{ label: '产品', value: 'product' }, { label: '设备群组', value: 'group' }, { label: '指定设备', value: 'device' }]

function createDefault(): ForwardRule {
  return { ruleName: '', description: '', dataLevel: 'product', productKey: undefined, groupId: undefined, relationIds: [], status: 'stop', payload: '{\n  "time": 1644303132765,\n  "deviceId": "device-001",\n  "deviceName": "示例设备",\n  "productKey": "product-key",\n  "properties": {\n    "temperature": 30\n  },\n  "messageType": "PROPERTIES"\n}', config: { appId: '*', fields: '*', condition: '', targets: [] } }
}
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const applicationOptions = computed(() => [{ label: '全部应用', value: '*' }, ...applications.value.map((item) => ({ label: `${item.appName} (${item.appId})`, value: item.appId }))])
const deviceOptions = computed(() => devices.value.map((item) => ({ label: `${item.deviceName || item.deviceId} (${item.deviceId})`, value: item.iotId || item.id })))
function mapGroups(items: DeviceGroupItem[]): TreeNodeData[] { return items.map((item) => ({ key: item.id, title: item.groupName, children: mapGroups(item.children || []) })) }
const groupOptions = computed(() => mapGroups(groups.value[0]?.children || groups.value))
const sqlPreview = computed(() => `SELECT ${form.config.fields || '*'}\nFROM ${form.config.appId || '*'}${form.config.condition ? `\nWHERE ${form.config.condition}` : ''}`)
const selectedProductName = computed(() => products.value.find((item) => item.productKey === form.productKey)?.name)
function findGroupName(items: DeviceGroupItem[]): string | undefined { for (const item of items) { if (item.id === form.groupId) return item.groupName; const child = findGroupName(item.children || []); if (child) return child } }
const selectedGroupName = computed(() => findGroupName(groups.value))
function handleLevelChange() { form.productKey = undefined; form.groupId = undefined; form.relationIds = []; devices.value = [] }
async function handleProductChange() { form.relationIds = []; devices.value = []; if (form.dataLevel !== 'device' || !form.productKey) return; deviceLoading.value = true; try { devices.value = (await getForwardDevices(form.productKey)).data.records || [] } finally { deviceLoading.value = false } }
function validateBase() {
  if (!form.config.fields.trim()) { Message.warning('SELECT 字段不能为空'); return false }
  if (!form.config.appId) { Message.warning('请选择来源应用'); return false }
  if ((form.dataLevel === 'product' || form.dataLevel === 'device') && !form.productKey) { Message.warning('请选择产品'); return false }
  if (form.dataLevel === 'group' && !form.groupId) { Message.warning('请选择设备群组'); return false }
  if (form.dataLevel === 'device' && !form.relationIds.length) { Message.warning('请选择至少一个设备'); return false }
  return true
}
async function next() { if (step.value === 1 && (!form.ruleName.trim() || !validateBase())) { if (!form.ruleName.trim()) Message.warning('请输入规则名称'); return } step.value++ }
async function runSqlTest() {
  if (!validateBase()) return
  try { JSON.parse(form.payload || '') } catch { Message.warning('测试 Payload 不是有效 JSON'); return }
  testingSql.value = true
  try { const res = await testForwardSql(form); sqlResult.value = JSON.stringify(res.data, null, 2) || res.message || '测试成功' } finally { testingSql.value = false }
}
async function save() {
  saving.value = true
  try {
    let id = form.id
    if (id) {
      await updateForwardRule(form)
    } else { const res = await addForwardRule(form); id = String(res.data); form.id = id }
    await updateForwardRuleTargets(form)
    Message.success('数据转发规则保存成功')
    visible.value = false
    emit('save-success')
  } finally { saving.value = false }
}
async function loadDependencies() {
  const [productRes, groupRes, applicationRes] = await Promise.all([getForwardProducts(), getForwardGroups(), getForwardApplications()])
  products.value = productRes.data.records || []
  groups.value = groupRes.data || []
  applications.value = applicationRes.data.records || []
}
async function open(record?: ForwardRule) {
  Object.assign(form, createDefault())
  step.value = 1
  sqlResult.value = ''
  visible.value = true
  await loadDependencies()
  if (record?.id) Object.assign(form, (await getForwardRule(record.id)).data)
  form.relationIds ||= []
  form.config ||= { appId: '*', fields: '*', targets: [] }
  form.config.targets ||= []
  if (form.dataLevel === 'device' && form.productKey) {
    const relationIds = [...(form.relationIds || [])]
    await handleProductChange()
    form.relationIds = relationIds
  }
}
defineExpose({ open })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.rule-form__steps { margin-bottom: 20px; }
.rule-form__section { display: grid; gap: 14px; }
.rule-form__sql-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(280px, .8fr); gap: 0 16px; }
.rule-form__sql-preview { grid-row: 1 / span 3; grid-column: 2; min-height: 180px; padding: 14px; margin: 0; overflow: auto; font-family: Monaco, Menlo, Consolas, monospace; color: rgb(var(--primary-6)); white-space: pre-wrap; background: var(--color-fill-2); }
.rule-form__code, .rule-form__test-result { font-family: Monaco, Menlo, Consolas, monospace; }
.rule-form__test-result { max-height: 300px; padding: 12px; overflow: auto; word-break: break-all; white-space: pre-wrap; background: var(--color-fill-2); }
.rule-form__footer { position: sticky; bottom: 0; display: flex; justify-content: space-between; padding: 14px 20px; margin: 24px -20px -20px; background: var(--color-bg-2); border-top: 1px solid var(--color-border-2); }

@media (width <= 768px) { .rule-form__sql-grid { grid-template-columns: 1fr; } .rule-form__sql-preview { grid-row: auto; grid-column: 1; min-height: 140px; } }
</style>
