<template>
  <a-modal v-model:visible="visible" :title="title" width="800px" :mask-closable="false" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item field="thirdPlatform" label="接入方式" :rules="requiredRule('请选择接入方式')">
        <a-select v-model="form.thirdPlatform" :loading="referenceLoading" :disabled="isEdit" placeholder="请选择接入方式" allow-search @change="handleAccessChange">
          <a-option v-for="item in visibleAccessProtocols" :key="item.code" :value="item.code">
            <div class="access-option"><span><strong>{{ item.name }}</strong><small>{{ item.description }}</small></span><a-tag v-if="item.status" :color="item.available ? 'green' : 'orange'">{{ item.status }}</a-tag></div>
          </a-option>
        </a-select>
      </a-form-item>

      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <a-form-item field="classifiedId" label="产品分类" :rules="requiredRule('请选择产品分类')">
          <a-tree-select v-model="form.classifiedId" :data="categoryTree" :field-names="{ key: 'id', title: 'classifiedName', children: 'children' }" allow-search placeholder="请选择叶子分类" />
        </a-form-item>
        <a-form-item v-if="isEdit" label="ProductKey"><a-input v-model="form.productKey" disabled /></a-form-item>
      </a-grid>

      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="16">
        <a-form-item field="name" label="产品名称" :rules="requiredRule('请输入产品名称')"><a-input v-model="form.name" :max-length="80" placeholder="请输入产品名称" /></a-form-item>
        <a-form-item field="companyNo" label="厂商简称" :rules="requiredRule('请输入厂商简称')"><a-input v-model="form.companyNo" placeholder="请输入厂商简称" /></a-form-item>
        <a-form-item field="productId" label="设备型号" :rules="requiredRule('请输入设备型号')"><a-input v-model="form.productId" :disabled="isEdit" placeholder="请输入设备型号" /></a-form-item>
        <a-form-item field="deviceNode" label="设备节点" :rules="requiredRule('请选择设备节点')"><a-select v-model="form.deviceNode" :disabled="isEdit" :options="deviceNodeOptions" placeholder="请选择设备节点" @change="handleDeviceNodeChange" /></a-form-item>
        <a-form-item v-if="showTransportProtocol" field="transportProtocol" label="数据协议" :rules="requiredRule('请选择数据协议')"><a-select v-model="form.transportProtocol" :disabled="isEdit" :options="gatewayTransportOptions" placeholder="请选择数据协议" /></a-form-item>
      </a-grid>

      <a-form-item v-if="form.thirdPlatform === 'http'" label="南向云对接地址"><a-input v-model="form.southboundCloudDockingAddress" placeholder="用于第三方云下发指令的 URL" /></a-form-item>
      <a-form-item label="产品标签"><a-select v-model="tagValues" multiple allow-clear allow-search :options="tagOptions" placeholder="请选择产品标签" /></a-form-item>
      <a-form-item v-if="isEdit" label="产品状态"><a-radio-group v-model="form.state" type="button"><a-radio :value="0">已发布</a-radio><a-radio :value="1">审核中</a-radio><a-radio :value="2">已停用</a-radio></a-radio-group></a-form-item>
      <a-form-item label="产品描述"><a-textarea v-model="form.describe" :max-length="500" show-word-limit :auto-size="{ minRows: 3, maxRows: 6 }" placeholder="请输入产品描述" /></a-form-item>
    </a-form>

  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { ProductAccessProtocol, ProductItem } from '@/apis/iot/product'
import type { ProductCategoryItem } from '@/apis/iot/productCategory'
import { Message } from '@arco-design/web-vue'
import { addProduct, getProduct, getProductAccessProtocols, getProductConfigDicts, updateProduct } from '@/apis/iot/product'
import { getProductCategoryList } from '@/apis/iot/productCategory'

defineOptions({ name: 'ProductFormModal' })

const emit = defineEmits<{ (e: 'save-success'): void, (e: 'created', product: Pick<ProductItem, 'id' | 'productKey'>): void }>()

interface CategoryNode extends ProductCategoryItem {
  disabled?: boolean
  children?: CategoryNode[]
}

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const referenceLoading = ref(false)
const categoryTree = ref<CategoryNode[]>([])
const accessProtocols = ref<ProductAccessProtocol[]>([])
const deviceNodeOptions = ref<{ label: string, value: string }[]>([])
const transportOptions = ref<{ label: string, value: string }[]>([])
const tagOptions = ref<{ label: string, value: string }[]>([])
const form = reactive(createForm())

const isEdit = computed(() => Boolean(form.id))
const visibleAccessProtocols = computed(() => accessProtocols.value.filter((item) => ['mqtt', 'http'].includes(item.code.toLowerCase()) && (isEdit.value || (item.available && item.isManualCreatable !== false))))
const title = computed(() => isEdit.value ? '编辑产品' : '新增产品')
const isGatewayNode = computed(() => ['GATEWAY', 'GATEWAY_SUB_DEVICE'].includes(form.deviceNode))
const gatewayTransportOptions = computed(() => transportOptions.value.filter((item) => ['json', 'string', 'binary'].includes(item.value.toLowerCase())))
const showTransportProtocol = computed(() => isGatewayNode.value && gatewayTransportOptions.value.length > 0)
const tagValues = computed({
  get: () => form.tags ? String(form.tags).split(',').filter(Boolean) : [],
  set: (value: string[]) => { form.tags = value.join(',') }
})
const requiredRule = (message: string) => [{ required: true, message }]
function createForm(): Partial<ProductItem> & { id: string, name: string, productId: string, productKey: string, classifiedId: string, companyNo: string, deviceNode: string, thirdPlatform: string, transportProtocol: string, state: number, tags: string, describe: string, configuration: string, tupIsThrough?: number, payloadFormat?: number, powerModel?: number, lwm2mEdrxTime: string, southboundCloudDockingAddress: string } {
  return { id: '', name: '', productId: '', productKey: '', classifiedId: '', companyNo: '', deviceNode: '', thirdPlatform: '', transportProtocol: '', state: 0, tags: '', describe: '', configuration: '', tupIsThrough: undefined, payloadFormat: undefined, powerModel: undefined, lwm2mEdrxTime: '', southboundCloudDockingAddress: '' }
}

function buildTree(list: ProductCategoryItem[], parentId = '0'): CategoryNode[] {
  return list.filter((item) => String(item.parentId || '0') === parentId).map((item) => {
    const children = buildTree(list, String(item.id))
    return { ...item, children, disabled: children.length > 0 }
  })
}

async function loadReferences() {
  referenceLoading.value = true
  try {
    const [categoryRes, dictRes] = await Promise.all([
      getProductCategoryList(),
      getProductConfigDicts(['instance_node', 'transport_protocol', 'product_tags'])
    ])
    const protocolRes = await getProductAccessProtocols().catch(() => undefined)
    categoryTree.value = buildTree(categoryRes.data)
    deviceNodeOptions.value = (dictRes.data.instance_node || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    transportOptions.value = (dictRes.data.transport_protocol || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    tagOptions.value = (dictRes.data.product_tags || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    accessProtocols.value = protocolRes ? Object.values(protocolRes.data.protocols || {}) : fallbackAccessProtocols()
  } finally {
    referenceLoading.value = false
  }
}

function fallbackAccessProtocols(): ProductAccessProtocol[] {
  return [{ code: 'mqtt', name: 'MQTT', available: true }, { code: 'http', name: 'HTTP', available: true }]
}

function reset() {
  Object.assign(form, createForm())
  formRef.value?.clearValidate()
}

function handleAccessChange() {
  if (isEdit.value) return
  Object.assign(form, { deviceNode: '', transportProtocol: '', tupIsThrough: undefined, payloadFormat: undefined, powerModel: undefined, lwm2mEdrxTime: '', southboundCloudDockingAddress: '' })
}

function handleDeviceNodeChange() {
  form.transportProtocol = ''
}

async function add() {
  reset()
  await loadReferences()
  visible.value = true
}

function parseConfig(value?: string) {
  try {
    return JSON.parse(value || '{}') as Record<string, unknown>
  } catch {
    return {}
  }
}

async function edit(id: string) {
  reset()
  const [, detailRes] = await Promise.all([loadReferences(), getProduct(id)])
  const data = detailRes.data
  const config = parseConfig(data.configuration)
  Object.assign(form, data, {
    id: String(data.id),
    tags: Array.isArray(data.tags) ? data.tags.join(',') : data.tags || '',
    tupIsThrough: data.tupIsThrough ?? config.tupIsThrough,
    payloadFormat: data.payloadFormat ?? config.payloadFormat,
    powerModel: data.powerModel ?? config.powerModel,
    lwm2mEdrxTime: data.lwm2mEdrxTime ?? config.lwm2mEdrxTime ?? '',
    southboundCloudDockingAddress: data.southboundCloudDockingAddress ?? config.thirdDownUrl ?? ''
  })
  visible.value = true
}

function flatten(item: CategoryNode): CategoryNode[] {
  return [item, ...(item.children || []).flatMap(flatten)]
}

function buildPayload() {
  const selectedCategory = categoryTree.value.flatMap(flatten).find((item) => String(item.id) === String(form.classifiedId))
  const payload = { ...form, classifiedName: selectedCategory?.classifiedName }
  const configuration = parseConfig(form.configuration)
  if (form.thirdPlatform === 'http') {
    if (form.southboundCloudDockingAddress) {
      Object.assign(configuration, { thirdDownSupport: true, thirdDownUrl: form.southboundCloudDockingAddress })
    } else {
      delete configuration.thirdDownSupport
      delete configuration.thirdDownUrl
    }
  }
  payload.configuration = Object.keys(configuration).length ? JSON.stringify(configuration) : ''
  return payload
}

async function save() {
  if (await formRef.value?.validate()) return false
  const payload = buildPayload()
  let created: Pick<ProductItem, 'id' | 'productKey'> | undefined
  if (isEdit.value) await updateProduct(payload)
  else created = (await addProduct(payload)).data
  Message.success(isEdit.value ? '修改成功' : '新增成功')
  emit('save-success')
  if (created?.id) emit('created', created)
  return true
}

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.access-option {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;

  span,
  small {
    display: block;
  }

  small {
    margin-top: 2px;
    color: var(--color-text-3);
  }
}

.form-section {
  padding: 12px;
  margin-bottom: 16px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
  }
}

.public-product-selection {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.public-product-toolbar {
  gap: 12px;
  margin-bottom: 12px;

  .arco-input-wrapper {
    max-width: 360px;
  }
}

@media (width <= 600px) {
  .form-section__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
