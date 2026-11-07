<template>
  <div class="mqtt-topic">
    <section class="mqtt-topic__section">
      <div class="mqtt-topic__header">
        <div>
          <h3>已绑定产品（{{ boundProducts.length }}）</h3>
          <p>关联产品留空时，按标准 Topic 识别，或从自定义 Topic 中识别唯一已绑定产品 Key；Topic 只有设备号时请选择产品。</p>
        </div>
        <a-button
          v-hasPerm="['network:mqtt:edit']"
          type="primary"
          @click="openBindingModal"
        >
          <template #icon><icon-link /></template>
          管理绑定
        </a-button>
      </div>
      <div v-if="boundProducts.length" class="mqtt-topic__products">
        <div
          v-for="product in boundProducts"
          :key="product.productKey"
          class="mqtt-topic__product-row"
        >
          <strong :title="product.name || product.productKey">
            {{ product.name || product.productKey }}
          </strong>
          <code>{{ product.productKey }}</code>
        </div>
      </div>
      <a-empty v-else description="暂无绑定产品" />
    </section>

    <section class="mqtt-topic__section">
      <div class="mqtt-topic__header">
        <div>
          <h3>主题模式测试</h3>
          <p>输入真实 Topic，检查当前表单中的首条命中规则、产品绑定和数据类型；不会发送设备消息。</p>
        </div>
      </div>
      <a-input-search
        v-model="testTopic"
        search-button
        button-text="测试匹配"
        placeholder="例如：$thing/up/property/产品Key/设备号"
        @search="testTopicMatch"
        @press-enter="testTopicMatch"
      />
      <a-alert
        v-if="testResult"
        class="mqtt-topic__test-result"
        :type="testResult.matched ? 'success' : 'warning'"
      >
        {{ testResult.message }}
      </a-alert>
    </section>

    <section class="mqtt-topic__section">
      <div class="mqtt-topic__header">
        <div>
          <h3>主题映射配置</h3>
          <p>支持 <code>+</code> 单层通配符和位于末尾的 <code>#</code> 多层通配符。</p>
        </div>
        <a-space wrap>
          <template v-if="editing">
            <a-button @click="cancelEdit">取消</a-button>
            <a-button @click="addMapping">
              <template #icon><icon-plus /></template>
              添加主题
            </a-button>
            <a-button
              v-hasPerm="['network:mqtt:edit']"
              type="primary"
              :loading="saving"
              @click="saveMappings"
            >
              <template #icon><icon-save /></template>
              保存配置
            </a-button>
          </template>
          <a-button
            v-else
            v-hasPerm="['network:mqtt:edit']"
            type="primary"
            @click="startEdit"
          >
            <template #icon><icon-edit /></template>
            编辑配置
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="_key"
        :data="mappingRows"
        :pagination="false"
        :bordered="{ cell: true }"
        :scroll="{ x: '100%', minWidth: 950 }"
      >
        <template #columns>
          <a-table-column title="订阅主题" data-index="topicPattern" :width="260">
            <template #cell="{ record }">
              <a-input
                v-if="editing"
                v-model.trim="record.topicPattern"
                placeholder="例如：nexiot/+/+/#"
                :error="Boolean(record.topicError)"
                @blur="validateMapping(record)"
              />
              <div v-if="record.topicError" class="mqtt-topic__field-error">
                {{ record.topicError }}
              </div>
              <code v-if="!editing">{{ record.topicPattern }}</code>
            </template>
          </a-table-column>
          <a-table-column title="数据类型" data-index="topicCategory" :width="140">
            <template #cell="{ record }">
              <a-select
                v-if="editing"
                v-model="record.topicCategory"
                :options="categoryOptions"
                placeholder="请选择"
              />
              <a-tag v-else :color="record.topicCategory === 'PASSTHROUGH' ? 'green' : 'arcoblue'">
                {{ categoryLabel(record.topicCategory) }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="QoS" data-index="qos" :width="130">
            <template #cell="{ record }">
              <a-select v-if="editing" v-model="record.qos" :options="qosOptions" />
              <a-tag v-else :color="qosColor(record.qos)">QoS {{ record.qos }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="关联产品" data-index="productKey" :width="220">
            <template #cell="{ record }">
              <a-select
                v-if="editing"
                v-model="record.productKey"
                :options="boundProductOptions"
                placeholder="按 Topic 自动识别"
                allow-clear
                allow-search
              />
              <span v-else>{{ productLabel(record.productKey) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="状态" data-index="enabled" :width="100" align="center">
            <template #cell="{ record }">
              <a-switch v-if="editing" v-model="record.enabled" />
              <a-tag v-else :color="record.enabled ? 'green' : 'gray'">
                {{ record.enabled ? '启用' : '禁用' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="70" align="center">
            <template #cell="{ rowIndex }">
              <a-button
                v-if="editing"
                type="text"
                status="danger"
                aria-label="删除主题"
                @click="removeMapping(rowIndex)"
              >
                <template #icon><icon-delete /></template>
              </a-button>
            </template>
          </a-table-column>
        </template>
      </a-table>
      <a-empty v-if="!mappingRows.length" description="暂无主题映射">
        <a-button v-if="editing" type="outline" @click="addMapping">
          添加第一个主题
        </a-button>
      </a-empty>
    </section>

    <a-modal
      v-model:visible="bindingVisible"
      title="管理绑定产品"
      width="min(680px, calc(100vw - 32px))"
      :mask-closable="false"
      :body-style="{ maxHeight: 'min(68vh, 620px)', overflowY: 'auto' }"
      align-center
      unmount-on-close
      @before-ok="saveBindings"
    >
      <a-alert class="mqtt-topic__binding-alert">
        产品绑定变更会直接影响该 MQTT 组件可识别的产品范围。
      </a-alert>
      <a-select
        v-model="selectedProductKeys"
        :options="productOptions"
        :loading="productLoading"
        placeholder="请选择 MQTT 产品"
        multiple
        :max-tag-count="3"
        allow-clear
        allow-search
      />
      <div class="mqtt-topic__selected-count">
        已选择 {{ selectedProductKeys.length }} 个产品
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { SelectOptionData } from '@arco-design/web-vue'
import type {
  MqttBoundProduct,
  MqttNetworkItem,
  MqttTopicMapping
} from '@/apis/iot/mqtt'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import {
  parseMqttConfiguration,
  parseMqttTopicMappings,
  updateMqttNetwork,
  updateMqttProductBinding
} from '@/apis/iot/mqtt'
import { getProductByKey, getProductList } from '@/apis/iot/product'
import { deepClone } from '@/utils'
import { previewTopicRoute, validateTopicPattern } from '@/views/iot/network/mqtt/topicRouting'

defineOptions({ name: 'MqttTopicSubscriptionPanel' })

const props = defineProps<{
  network: MqttNetworkItem
}>()
const emit = defineEmits<{
  (e: 'updated'): void
}>()

interface EditableMapping extends MqttTopicMapping {
  _key: string
  topicError?: string
}

const categoryOptions = [
  { label: '物模型', value: 'THING_MODEL' },
  { label: '透传', value: 'PASSTHROUGH' }
]
const qosOptions = [0, 1, 2].map((value) => ({
  label: `QoS ${value}`,
  value
}))

const editing = ref(false)
const saving = ref(false)
const mappingRows = ref<EditableMapping[]>([])
const backupRows = ref<EditableMapping[]>([])
const testTopic = ref('')
const testResult = ref<{ matched: boolean, message: string }>()
const bindingVisible = ref(false)
const productLoading = ref(false)
const productList = ref<ProductItem[]>([])
const selectedProductKeys = ref<string[]>([])

const boundProducts = computed(() => props.network.bindProducts || [])
const boundProductOptions = computed<SelectOptionData[]>(() =>
  boundProducts.value.map((item) => ({
    label: `${item.name || item.productKey}（${item.productKey}）`,
    value: item.productKey
  }))
)
const productOptions = computed<SelectOptionData[]>(() => {
  const products = new Map<string, ProductItem | MqttBoundProduct>()
  boundProducts.value.forEach((item) => products.set(item.productKey, item))
  productList.value.forEach((item) => products.set(item.productKey, item))
  return Array.from(products.values()).map((item) => ({
    label: `${item.name || item.productKey}（${item.productKey}）${item.networkUnionId && item.networkUnionId !== props.network.unionId ? ' - 已绑定其他组件' : ''}`,
    value: item.productKey,
    disabled: Boolean(
      item.networkUnionId && item.networkUnionId !== props.network.unionId
    )
  }))
})

function createKey() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function loadMappings() {
  mappingRows.value = parseMqttTopicMappings(
    parseMqttConfiguration(props.network)
  ).map((item) => ({ ...item, _key: createKey() }))
  backupRows.value = deepClone(mappingRows.value)
}

function startEdit() {
  backupRows.value = deepClone(mappingRows.value)
  editing.value = true
}

function cancelEdit() {
  mappingRows.value = deepClone(backupRows.value)
  editing.value = false
}

function addMapping() {
  mappingRows.value.push({
    _key: createKey(),
    topicPattern: '',
    topicCategory: undefined,
    qos: 1,
    productKey: undefined,
    enabled: true
  })
}

function removeMapping(index: number) {
  mappingRows.value.splice(index, 1)
}

function validateMapping(record: EditableMapping) {
  record.topicError = validateTopicPattern(record.topicPattern)
  return !record.topicError
}

function validateMappings() {
  for (let index = 0; index < mappingRows.value.length; index += 1) {
    const item = mappingRows.value[index]
    if (!validateMapping(item)) {
      Message.warning(`第 ${index + 1} 行主题格式不正确`)
      return false
    }
    if (!item.topicCategory) {
      Message.warning(`第 ${index + 1} 行请选择数据类型`)
      return false
    }
    if (item.enabled && item.productKey && !boundProducts.value.some((product) => product.productKey === item.productKey)) {
      Message.warning(`第 ${index + 1} 行关联产品未绑定当前网络，请先管理绑定`)
      return false
    }
    // 固定 Topic 可在保存时直接验证；通配符规则通过真实 Topic 测试识别结果。
    if (item.enabled && !item.productKey && !item.topicPattern.includes('+') && !item.topicPattern.includes('#')) {
      const topic = item.topicPattern.startsWith('$share/') ? item.topicPattern.split('/').slice(2).join('/') : item.topicPattern
      const result = previewTopicRoute(topic, [item], boundProducts.value.map((product) => product.productKey))
      if (!result.matched) {
        Message.warning(`第 ${index + 1} 行：${result.message}`)
        return false
      }
    }
  }
  return true
}

async function saveMappings() {
  if (!validateMappings()) return
  saving.value = true
  try {
    const config = parseMqttConfiguration(props.network)
    config.subscribeTopics = mappingRows.value.map((item) => ({
      topicPattern: item.topicPattern.trim(),
      topicCategory: item.topicCategory,
      qos: item.qos,
      productKey: item.productKey || undefined,
      enabled: item.enabled
    }))
    await updateMqttNetwork({
      id: props.network.id,
      type: props.network.type,
      unionId: props.network.unionId,
      productKey: props.network.productKey,
      name: props.network.name,
      description: props.network.description,
      state: props.network.state,
      configuration: JSON.stringify(config)
    })
    editing.value = false
    backupRows.value = deepClone(mappingRows.value)
    Message.success('主题订阅配置已保存')
    emit('updated')
  } finally {
    saving.value = false
  }
}

function testTopicMatch() {
  const topic = testTopic.value.trim()
  if (!topic) {
    Message.warning('请输入要测试的 Topic')
    return
  }
  if (topic.includes('+') || topic.includes('#')) {
    Message.warning('真实 Topic 不能包含通配符')
    return
  }
  const result = previewTopicRoute(topic, mappingRows.value, boundProducts.value.map((item) => item.productKey))
  testResult.value = result.matched
    ? { ...result, message: `${result.message}；关联产品：${productLabel(result.productKey)}；数据类型：${categoryLabel(result.topicCategory)}` }
    : result
}

function categoryLabel(value?: string) {
  if (value === 'THING_MODEL') return '物模型'
  if (value === 'PASSTHROUGH') return '透传'
  if (value === 'SYSTEM_LEVEL') return '系统 / OTA'
  return '未配置'
}

function qosColor(qos: number) {
  return qos === 0 ? 'orange' : qos === 1 ? 'arcoblue' : 'green'
}

function productLabel(productKey?: string) {
  if (!productKey) return '按 Topic 自动识别'
  const product = boundProducts.value.find(
    (item) => item.productKey === productKey
  )
  return product ? `${product.name}（${productKey}）` : productKey
}

async function openBindingModal() {
  bindingVisible.value = true
  selectedProductKeys.value = boundProducts.value.map((item) => item.productKey)
  productLoading.value = true
  try {
    const { data } = await getProductList({ page: 1, size: 1000, state: 0 })
    productList.value = data.records.filter(
      (item) =>
        String(item.thirdPlatform || '').toLowerCase() === 'mqtt'
        && ['DEVICE', 'GATEWAY'].includes(String(item.deviceNode || '').toUpperCase())
    )
  } finally {
    productLoading.value = false
  }
}

async function saveBindings() {
  const products = new Map<string, ProductItem | MqttBoundProduct>()
  boundProducts.value.forEach((item) => products.set(item.productKey, item))
  productList.value.forEach((item) => products.set(item.productKey, item))
  const before = new Set(boundProducts.value.map((item) => item.productKey))
  const after = new Set(selectedProductKeys.value)
  const changes = Array.from(products.values()).filter(
    (item) => before.has(item.productKey) !== after.has(item.productKey)
  )
  await Promise.all(
    changes.map(async (item) => {
      const product = item.id ? item : (await getProductByKey(item.productKey)).data
      if (!product.id) throw new Error(`产品 ${item.productKey} 缺少 ID，无法更新绑定`)
      return updateMqttProductBinding(
        String(product.id),
        after.has(item.productKey) ? props.network.unionId : undefined
      )
    })
  )
  Message.success('产品绑定已更新')
  emit('updated')
  return true
}

watch([testTopic, mappingRows, boundProducts], () => {
  testResult.value = undefined
}, { deep: true })

watch(() => [props.network.id, props.network.configuration], loadMappings, {
  immediate: true
})
</script>

<style lang="scss" scoped>
.mqtt-topic {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__section {
    padding-bottom: 16px;
    border-bottom: 1px solid var(--color-border-2);

    &:last-of-type {
      padding-bottom: 0;
      border-bottom: 0;
    }
  }

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 14px;

    h3 {
      margin: 0;
      font-size: 15px;
      line-height: 24px;
      color: var(--color-text-1);
    }

    p {
      margin: 3px 0 0;
      font-size: 12px;
      line-height: 20px;
      color: var(--color-text-3);
    }
  }

  &__test-result,
  &__binding-alert {
    margin-top: 12px;
  }

  &__products {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 20px;
    width: 100%;
    max-height: 132px;
    overflow-y: auto;
    border-top: 1px solid var(--color-border-1);
    border-bottom: 1px solid var(--color-border-1);
  }

  &__product-row {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
    min-height: 40px;
    border-bottom: 1px solid var(--color-border-1);

    strong {
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 13px;
      white-space: nowrap;
    }

    code {
      flex: 0 1 46%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__selected-count {
    margin-top: 8px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__field-error {
    margin-top: 4px;
    font-size: 12px;
    color: rgb(var(--danger-6));
  }
}

code {
  padding: 2px 5px;
  color: rgb(var(--primary-6));
  background: var(--color-fill-2);
  border-radius: 3px;
}

@media (width <= 768px) {
  .mqtt-topic__header {
    flex-direction: column;
  }

  .mqtt-topic__products {
    grid-template-columns: 1fr;
  }
}
</style>
