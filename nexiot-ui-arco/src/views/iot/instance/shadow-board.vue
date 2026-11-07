<template>
  <GiPageLayout margin class="shadow-board">
    <div class="shadow-board__filters">
      <div class="shadow-board__product">
        <a-select
          v-model="query.productKey"
          :options="productOptions"
          :loading="productLoading"
          :filter-option="false"
          :fallback-option="selectedProductOption"
          :show-extra-options="false"
          allow-search
          placeholder="搜索产品名称 / ProductKey"
          aria-label="选择产品"
          @search="searchProducts"
          @popup-visible-change="handleProductPopupChange"
          @change="handleProductSelectChange"
        >
          <template #label="{ data }"><span class="shadow-board__product-label" :title="data.label">{{ data.name }}</span></template>
          <template #option="{ data }">
            <div class="shadow-board__product-option">
              <span class="shadow-board__product-info"><strong :title="data.name">{{ data.name }}</strong><code>{{ data.value }}</code></span>
            </div>
          </template>
          <template #empty>
            <div class="shadow-board__product-empty">
              <span>{{ productLoading ? '正在搜索产品...' : productLoadFailed ? '产品加载失败' : '未找到匹配的产品' }}</span>
              <a-button v-if="productLoadFailed" type="text" size="small" @click="retryProducts">重试</a-button>
            </div>
          </template>
        </a-select>
      </div>
      <a-input v-model="query.deviceName" placeholder="设备名称" aria-label="设备名称" allow-clear :disabled="!query.productKey" @press-enter="search" />
      <a-input v-model="query.deviceId" placeholder="设备编号 DeviceId" aria-label="设备编号" allow-clear :disabled="!query.productKey" @press-enter="search" />
      <div><a-select v-model="query.state" :options="stateOptions" allow-clear placeholder="在线状态" aria-label="在线状态" :disabled="!query.productKey" @change="search" /></div>
      <a-space class="shadow-board__search-actions">
        <GiButton type="search" :disabled="!query.productKey" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </div>

    <div class="shadow-board__toolbar">
      <div class="shadow-board__result">
        <strong>设备数据</strong>
        <span>共 {{ pagination.total }} 台</span>
      </div>
      <div class="shadow-board__tools">
        <a-button :disabled="!query.productKey" @click="fieldVisible = true">
          <template #icon><icon-settings /></template>显示字段 <span class="shadow-board__field-count">{{ selectedFields.length }}</span>
        </a-button>
        <div class="shadow-board__interval"><a-select v-model="refreshSeconds" :options="refreshOptions" aria-label="自动刷新间隔" :disabled="!query.productKey" /></div>
        <a-radio-group v-model="viewMode" type="button" aria-label="显示方式">
          <a-radio value="table" aria-label="列表视图"><icon-list /></a-radio>
          <a-radio value="card" aria-label="卡片视图"><icon-apps /></a-radio>
        </a-radio-group>
        <a-tooltip content="刷新数据">
          <a-button :loading="loading || shadowLoading" :disabled="!query.productKey" aria-label="刷新数据" @click="refreshPage"><template #icon><icon-refresh /></template></a-button>
        </a-tooltip>
      </div>
    </div>

    <template v-if="query.productKey">
      <a-table
        v-if="viewMode === 'table'"
        class="g-table shadow-board__table"
        row-key="id"
        :data="displayRows"
        :columns="columns"
        :loading="loading || shadowLoading"
        :pagination="pagination"
        :scroll="{ x: '100%', y: '100%', minWidth: tableMinWidth }"
        table-layout-fixed
        stripe
      >
        <template #device="{ record }">
          <div class="shadow-device">
            <button type="button" class="shadow-device__name" :title="record.deviceName || '未命名设备'" @click="openDetail(record)">{{ record.deviceName || '未命名设备' }}</button>
            <code class="shadow-device__id" :title="record.deviceId">{{ record.deviceId }}</code>
          </div>
        </template>
        <template #state="{ record }"><a-tag size="small" :color="isOnline(record) ? 'green' : 'gray'">{{ isOnline(record) ? '在线' : '离线' }}</a-tag></template>
        <template v-for="field in selectedFields" :key="field.id" #[`field-${field.id}`]="{ record }">
          <div class="shadow-board__value-cell">
            <strong class="shadow-value" :class="{ 'shadow-value--empty': isEmptyValue(getReported(record, field.id)) }" :title="formatField(field, getReported(record, field.id))">{{ formatField(field, getReported(record, field.id)) }}</strong>
            <span v-if="getDesired(record, field.id) !== undefined" class="shadow-board__desired" :title="formatField(field, getDesired(record, field.id))">期望 {{ formatField(field, getDesired(record, field.id)) }}</span>
          </div>
        </template>
        <template #updated="{ record }"><span class="shadow-board__time">{{ formatTime(record.shadowLastTime) }}</span></template>
      </a-table>
      <a-spin v-else :loading="loading || shadowLoading" class="shadow-board__card-view">
        <a-empty v-if="!displayRows.length" description="当前条件下暂无设备" />
        <div v-else class="shadow-board__cards">
          <article v-for="row in displayRows" :key="row.id" class="shadow-card">
            <header class="shadow-card__header">
              <div class="shadow-card__identity">
                <button type="button" class="shadow-card__name" :title="row.deviceName || '未命名设备'" @click="openDetail(row)">{{ row.deviceName || '未命名设备' }}</button>
                <code :title="row.deviceId">{{ row.deviceId }}</code>
              </div>
              <a-tag size="small" :color="isOnline(row) ? 'green' : 'gray'">{{ isOnline(row) ? '在线' : '离线' }}</a-tag>
            </header>
            <div v-if="selectedFields.length" class="shadow-card__fields">
              <div v-for="field in selectedFields" :key="field.id" class="shadow-card__field">
                <span :title="field.name || field.id">{{ field.name || field.id }}</span>
                <strong class="shadow-value" :class="{ 'shadow-value--empty': isEmptyValue(getReported(row, field.id)) }" :title="formatField(field, getReported(row, field.id))">{{ formatField(field, getReported(row, field.id)) }}</strong>
                <small v-if="getDesired(row, field.id) !== undefined" :title="formatField(field, getDesired(row, field.id))">期望 {{ formatField(field, getDesired(row, field.id)) }}</small>
              </div>
            </div>
            <p v-else class="shadow-card__no-fields">暂未选择显示字段</p>
            <footer class="shadow-card__footer"><span>更新 {{ formatTime(row.shadowLastTime) }}</span><span :title="`影子版本 ${row.shadowVersion || '-'}`">v{{ row.shadowVersion || '-' }}</span></footer>
          </article>
        </div>
        <div v-if="displayRows.length" class="shadow-board__pagination"><a-pagination v-bind="pagination" /></div>
      </a-spin>
    </template>
    <div v-else class="shadow-board__empty">
      <a-empty :description="emptyDescription" />
      <a-button v-if="productLoadFailed" type="text" @click="loadProducts">重新加载产品</a-button>
    </div>

    <a-drawer v-model:visible="fieldVisible" title="配置显示字段" width="min(480px, 100%)" :mask-closable="false" @before-ok="applyFields" @open="prepareFields">
      <a-input-search v-model="fieldKeyword" placeholder="搜索属性名称或标识" allow-clear />
      <a-row justify="space-between" class="shadow-board__field-tools"><span>已选 {{ fieldDraft.length }} 项</span><a-space><a-button type="text" @click="selectDefaultFields">恢复默认</a-button><a-button type="text" @click="fieldDraft = []">清空</a-button></a-space></a-row>
      <a-checkbox-group v-model="fieldDraft" direction="vertical" class="shadow-board__field-list"><a-checkbox v-for="field in filteredFields" :key="field.id" :value="field.id"><div><strong>{{ field.name || field.id }}</strong><code>{{ field.id }}</code><span>{{ fieldType(field) }}</span></div></a-checkbox></a-checkbox-group>
    </a-drawer>
  </GiPageLayout>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem, DeviceShadow } from '@/apis/iot/device'
import type { ProductMetadataItem, ProductOptionItem } from '@/apis/iot/product'
import { useStorage } from '@vueuse/core'
import Dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { batchGetDeviceShadows, getDeviceList } from '@/apis/iot/device'
import { getProductOptions, getProductProperties } from '@/apis/iot/product'
import { useTable } from '@/hooks'

defineOptions({ name: 'IotDeviceShadowBoard' })

interface ShadowDevice extends DeviceItem {
  shadowMetadata?: Record<string, unknown>
  shadowVersion?: string | number
  shadowLastTime?: string | number
}

const router = useRouter()
const products = ref<ProductOptionItem[]>([])
const selectedProduct = ref<ProductOptionItem>()
const productKeyword = ref('')
const fields = ref<ProductMetadataItem[]>([])
const shadows = reactive<Record<string, DeviceShadow>>({})
const productLoading = ref(false)
const productLoadFailed = ref(false)
const shadowLoading = ref(false)
const fieldVisible = ref(false)
const fieldKeyword = ref('')
const fieldDraft = ref<string[]>([])
const refreshSeconds = ref(15)
const viewMode = useStorage<'table' | 'card'>('nexiot-device-shadow-view', 'table')
const lastProductKey = useStorage('nexiot-device-shadow-product', '')
const fieldPreferences = useStorage<Record<string, string[]>>('nexiot-device-shadow-fields-by-product', {})
const selectedFieldIds = ref<string[]>([])
const query = reactive({ productKey: '', deviceName: '', deviceId: '', state: undefined as number | undefined })
let timer: ReturnType<typeof setInterval> | undefined
let productChangeToken = 0
let productSearchToken = 0
let productSearchTimer: ReturnType<typeof setTimeout> | undefined
const stateOptions = [{ label: '在线', value: 1 }, { label: '离线', value: 0 }]
const refreshOptions = [{ label: '手动刷新', value: 0 }, { label: '每 5 秒', value: 5 }, { label: '每 15 秒', value: 15 }, { label: '每 30 秒', value: 30 }]
function toProductOption(item: ProductOptionItem) {
  return { label: `${item.name || item.productKey} ${item.productKey}`, name: item.name || item.productKey, value: item.productKey }
}
const productOptions = computed(() => products.value.map(toProductOption))
function selectedProductOption(value: unknown) {
  return selectedProduct.value && selectedProduct.value.productKey === value
    ? toProductOption(selectedProduct.value)
    : { label: String(value), name: String(value), value: String(value) }
}
const selectedFields = computed(() => selectedFieldIds.value.map((id) => fields.value.find((field) => field.id === id)).filter(Boolean) as ProductMetadataItem[])
const emptyDescription = computed(() => productLoading.value ? '正在加载产品...' : productLoadFailed.value ? '产品加载失败，请稍后重试' : productKeyword.value ? '未找到匹配的产品，请更换名称或 ProductKey 搜索' : products.value.length ? '请选择产品，设备状态字段将按该产品物模型展示' : '暂无可用产品，请先创建产品')
const filteredFields = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase()
  return keyword ? fields.value.filter((field) => `${field.name} ${field.id}`.toLowerCase().includes(keyword)) : fields.value
})
const { loading, tableData: deviceList, pagination, search, refresh } = useTable<DeviceItem>({
  listAPI: (page) => getDeviceList({ ...page, productKey: query.productKey, deviceName: query.deviceName.trim() || undefined, deviceId: query.deviceId.trim() || undefined, state: query.state }),
  immediate: false
})
const displayRows = computed<ShadowDevice[]>(() => deviceList.value.map((device) => {
  const shadow = device.iotId ? shadows[device.iotId] : undefined
  return { ...device, shadowMetadata: parseObject(shadow?.metadata), shadowVersion: shadow?.version, shadowLastTime: shadow?.lastTime }
}))
const tableMinWidth = computed(() => 548 + selectedFields.value.length * 156)

function parseObject(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch { return {} }
}
function branches(row: ShadowDevice) {
  const root = row.shadowMetadata || {}
  const state = parseObject(root.state)
  const reported = Object.keys(parseObject(state.reported)).length ? parseObject(state.reported) : Object.keys(parseObject(root.reported)).length ? parseObject(root.reported) : root
  const desired = Object.keys(parseObject(state.desired)).length ? parseObject(state.desired) : parseObject(root.desired)
  return { reported, desired }
}
function getReported(row: ShadowDevice, id: string) { return branches(row).reported[id] }
function getDesired(row: ShadowDevice, id: string) { return branches(row).desired[id] }
function fieldType(field: ProductMetadataItem) { return String(parseObject(field.valueType).type || field.type || 'string') }
function enumOptions(field: ProductMetadataItem) {
  const elements = parseObject(field.valueType).elements
  return Array.isArray(elements) ? elements.map(parseObject) : []
}
function isEmptyValue(value: unknown) { return value === undefined || value === null || value === '' }
function formatField(field: ProductMetadataItem, value: unknown) {
  if (isEmptyValue(value)) return '--'
  if (fieldType(field) === 'boolean') return value === true || value === 1 || value === '1' ? '是' : '否'
  if (fieldType(field) === 'enum') {
    const item = enumOptions(field).find((option) => String(option.value) === String(value))
    if (item) return String(item.text || item.label || value)
  } const text = typeof value === 'object' ? JSON.stringify(value) : String(value)
  const unit = parseObject(field.valueType).unit
  return unit ? `${text} ${unit}` : text
}
function formatTime(value?: string | number) {
  if (!value) return '尚未同步'
  const text = String(value)
  const date = /^\d{10,13}$/.test(text) ? Dayjs(Number(text) * (text.length === 10 ? 1000 : 1)) : Dayjs(text)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}
function isOnline(row: DeviceItem) { return row.state === true || row.state === 1 || row.state === '1' }
function openDetail(row: DeviceItem) { router.push(`/system/instance/instance-details/${row.id}`) }
function clearShadows() { Object.keys(shadows).forEach((key) => delete shadows[key]) }
function clearBoard() {
  fields.value = []
  selectedFieldIds.value = []
  deviceList.value = []
  pagination.total = 0
  shadowLoading.value = false
  clearShadows()
}
async function loadShadows() {
  const activeProductKey = query.productKey
  const ids = deviceList.value.map((item) => item.iotId).filter(Boolean) as string[]
  if (!ids.length) return
  shadowLoading.value = true
  try {
    const data = (await batchGetDeviceShadows(ids)).data || []
    if (query.productKey !== activeProductKey) return
    data.forEach((item) => { if (item.iotId) shadows[item.iotId] = item })
  } finally {
    if (query.productKey === activeProductKey) shadowLoading.value = false
  }
}
function saveFieldPreference() {
  if (!query.productKey) return
  fieldPreferences.value = { ...fieldPreferences.value, [query.productKey]: [...selectedFieldIds.value] }
}
function restoreFieldPreference(productKey: string) {
  const availableIds = new Set(fields.value.map((field) => field.id))
  const storedIds = (fieldPreferences.value[productKey] || []).filter((id) => availableIds.has(id))
  const hasStoredPreference = Object.keys(fieldPreferences.value).includes(productKey)
  selectedFieldIds.value = hasStoredPreference ? storedIds : fields.value.slice(0, 6).map((item) => item.id)
  saveFieldPreference()
}
async function handleProductChange(productKey = query.productKey) {
  const token = ++productChangeToken
  query.productKey = productKey || ''
  clearBoard()
  if (!query.productKey) {
    lastProductKey.value = ''
    resetTimer()
    return
  }
  const product = products.value.find((item) => item.productKey === query.productKey) || (selectedProduct.value?.productKey === query.productKey ? selectedProduct.value : undefined)
  if (!product) {
    query.productKey = ''
    lastProductKey.value = ''
    resetTimer()
    return
  }
  selectedProduct.value = product
  lastProductKey.value = product.productKey
  try {
    const result = await getProductProperties(product.id)
    if (token !== productChangeToken || query.productKey !== product.productKey) return
    fields.value = result.data || []
    restoreFieldPreference(product.productKey)
  } catch {
    if (token === productChangeToken && query.productKey === product.productKey) fields.value = []
  } finally {
    if (token === productChangeToken && query.productKey === product.productKey) {
      search()
      resetTimer()
    }
  }
}
function handleProductSelectChange(value: unknown) {
  handleProductChange(typeof value === 'string' ? value : '')
}
async function loadProducts() {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  const token = ++productSearchToken
  const savedKey = lastProductKey.value
  productKeyword.value = ''
  productLoading.value = true
  productLoadFailed.value = false
  const [initial, restored] = await Promise.allSettled([
    getProductOptions(),
    savedKey ? getProductOptions({ productKey: savedKey }) : Promise.resolve(undefined)
  ])
  if (token !== productSearchToken) return
  products.value = initial.status === 'fulfilled' ? initial.value.data.records || [] : []
  const restoredProduct = restored.status === 'fulfilled' ? restored.value?.data.records?.[0] : undefined
  const product = restoredProduct || products.value[0]
  productLoading.value = false
  productLoadFailed.value = initial.status === 'rejected'
  if (product) {
    selectedProduct.value = product
    await handleProductChange(product.productKey)
  } else {
    selectedProduct.value = undefined
    query.productKey = ''
    clearBoard()
    resetTimer()
    if (initial.status === 'fulfilled' && restored.status === 'fulfilled') lastProductKey.value = ''
  }
}
function searchProducts(value: string) {
  const keyword = value.trim()
  if (keyword === productKeyword.value && !productLoadFailed.value) return
  productKeyword.value = keyword
  const token = ++productSearchToken
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productLoading.value = true
  productLoadFailed.value = false
  productSearchTimer = setTimeout(() => fetchProductOptions(keyword, token), 250)
}
async function fetchProductOptions(keyword: string, token: number) {
  try {
    const result = await getProductOptions({ name: keyword || undefined })
    if (token !== productSearchToken) return
    products.value = result.data.records || []
  } catch {
    if (token !== productSearchToken) return
    products.value = []
    productLoadFailed.value = true
  } finally {
    if (token === productSearchToken) productLoading.value = false
  }
}
function retryProducts() {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productLoading.value = true
  productLoadFailed.value = false
  fetchProductOptions(productKeyword.value, ++productSearchToken)
}
function handleProductPopupChange(visible: boolean) {
  if (visible && productKeyword.value) searchProducts('')
  else if (visible && productLoadFailed.value) retryProducts()
}
function reset() {
  Object.assign(query, { deviceName: '', deviceId: '', state: undefined })
  if (query.productKey) search()
}
function selectDefaultFields() {
  fieldDraft.value = fields.value.slice(0, 6).map((item) => item.id)
  if (!fieldVisible.value) {
    selectedFieldIds.value = [...fieldDraft.value]
    saveFieldPreference()
  }
}
function prepareFields() {
  fieldDraft.value = [...selectedFieldIds.value]
  fieldKeyword.value = ''
}
function applyFields() {
  selectedFieldIds.value = fieldDraft.value.filter((id) => fields.value.some((field) => field.id === id))
  saveFieldPreference()
  return true
}
function refreshPage() { if (query.productKey) refresh() }
function resetTimer() {
  if (timer) clearInterval(timer)
  timer = refreshSeconds.value > 0 && query.productKey ? setInterval(refreshPage, refreshSeconds.value * 1000) : undefined
}
function stopTimer() {
  if (timer) clearInterval(timer)
  timer = undefined
}

const columns = computed<TableColumnData[]>(() => [
  { title: '设备名称 / 编号', width: 280, fixed: 'left', slotName: 'device' },
  { title: '状态', width: 88, align: 'center', slotName: 'state' },
  ...selectedFields.value.map((field) => ({ title: field.name || field.id, width: 156, ellipsis: true, tooltip: true, slotName: `field-${field.id}` })),
  { title: '最近更新', width: 180, ellipsis: true, tooltip: true, slotName: 'updated' }
])

watch(deviceList, loadShadows)
watch(refreshSeconds, resetTimer)
onMounted(loadProducts)
onActivated(resetTimer)
onDeactivated(stopTimer)
onBeforeUnmount(() => {
  stopTimer()
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productSearchToken++
  productChangeToken++
})
</script>

<style lang="scss" scoped>
.shadow-board {
  &__filters {
    display: grid;
    flex-shrink: 0;
    grid-template-columns: minmax(240px, 1.7fr) minmax(130px, 1fr) minmax(160px, 1fr) 120px auto;
    gap: 8px;
    margin-bottom: 12px;

    > * {
      min-width: 0;
    }
  }

  &__product {
    width: 100%;
  }

  &__product-label {
    display: block;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__product-empty {
    display: flex;
    gap: 4px;
    align-items: center;
    justify-content: center;
    padding: 16px;
    color: var(--color-text-3);
  }

  &__product-option {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    padding: 4px 0;
  }

  &__product-info {
    display: flex;
    flex-direction: column;
    min-width: 0;

    strong, code {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      font-weight: 500;
    }

    code {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__toolbar {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 8px 16px;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 12px;
  }

  &__result, &__tools {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &__result {
    gap: 12px;
    white-space: nowrap;

    strong {
      font-size: 14px;
      font-weight: 600;
    }

    span {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__tools {
    flex-wrap: wrap;
  }

  &__field-count {
    margin-left: 4px;
    color: var(--color-text-3);
  }

  &__interval {
    flex: 0 0 122px;
    width: 122px;
  }

  &__table {
    min-height: 0;

    :deep(.arco-table-td) {
      padding: 0;
    }

    :deep(.arco-table-td .arco-table-cell) {
      padding: 10px 16px;
    }

    :deep(.arco-table-th) {
      font-size: 12px;
      color: var(--color-text-2);
    }
  }

  &__value-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;

    > * {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__desired {
    font-size: 12px;
    color: rgb(var(--orange-6));
  }

  &__time {
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--color-text-3);
  }

  &__card-view {
    flex: 1;
    min-height: 0;
    overflow: auto;
  }

  &__cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
    gap: 12px;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    padding-top: 14px;
  }

  &__empty {
    margin: auto;
    text-align: center;
  }

  &__field-tools {
    margin: 14px 0;
    color: var(--color-text-3);
  }

  &__field-list {
    width: 100%;

    :deep(.arco-checkbox) {
      width: 100%;
      padding: 10px;
      border-bottom: 1px solid var(--color-border-1);
    }

    div {
      display: flex;
      gap: 8px;
      align-items: center;
    }

    code, span {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}

.shadow-device {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;

  &__name, &__id {
    display: block;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__name {
    padding: 0;
    font: inherit;
    font-weight: 500;
    color: rgb(var(--primary-6));
    text-align: left;
    cursor: pointer;
    background: transparent;
    border: 0;

    &:hover {
      text-decoration: underline;
    }
  }

  &__id {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.shadow-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;

  &__header {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
    align-items: start;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__identity {
    min-width: 0;

    code {
      display: block;
      margin-top: 5px;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 12px;
      color: var(--color-text-3);
      white-space: nowrap;
    }
  }

  &__name {
    display: -webkit-box;
    width: 100%;
    padding: 0;
    overflow: hidden;
    -webkit-line-clamp: 2;
    font: inherit;
    font-weight: 600;
    line-height: 20px;
    color: var(--color-text-1);
    text-align: left;
    overflow-wrap: anywhere;
    cursor: pointer;
    background: transparent;
    border: 0;
    -webkit-box-orient: vertical;

    &:hover {
      color: rgb(var(--primary-6));
    }
  }

  &__fields {
    display: grid;
    flex: 1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 16px;
    padding: 14px 0;
  }

  &__field {
    min-width: 0;

    span, strong, small {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    span, small {
      font-size: 12px;
      color: var(--color-text-3);
    }

    strong {
      margin-top: 4px;
      font-size: 17px;
    }

    small {
      margin-top: 3px;
      color: rgb(var(--orange-6));
    }
  }

  &__no-fields {
    flex: 1;
    color: var(--color-text-3);
  }

  &__footer {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
    padding-top: 10px;
    margin-top: auto;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--color-text-3);
    border-top: 1px solid var(--color-border-1);

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.shadow-value {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-1);

  &--empty {
    font-weight: 400;
    color: var(--color-text-4);
  }
}

@media (width <= 1200px) {
  .shadow-board__filters {
    grid-template-columns: minmax(220px, 1.5fr) minmax(130px, 1fr) minmax(150px, 1fr);
  }
}

@media (width <= 640px) {
  .shadow-board__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shadow-board__product {
    grid-column: 1 / -1;
  }

  .shadow-board__search-actions {
    justify-self: end;
  }
}
</style>
