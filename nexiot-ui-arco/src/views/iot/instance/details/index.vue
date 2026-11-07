  <template>
  <GiPageLayout :header-bordered="false" :header-style="{ padding: 0 }" :body-style="{ padding: 0 }">
    <template #header>
      <div class="device-detail__header">
        <a-button type="text" shape="circle" aria-label="返回设备列表" @click="router.back()"><icon-left /></a-button>
        <div class="device-detail__identity"><h1>{{ device?.deviceName || '设备详情' }}</h1><code>{{ device?.deviceId || route.params.id }}</code></div>
        <a-space v-if="device" wrap><a-tag :color="online ? 'green' : 'gray'">{{ online ? '在线' : '离线' }}</a-tag><a-tag color="arcoblue">{{ device.productName || product?.name }}</a-tag><a-button @click="loadData"><template #icon><icon-refresh /></template>刷新</a-button></a-space>
      </div>
      <a-tabs v-if="device && product" v-model:active-key="activeTab" type="line" class="device-detail__tabs">
        <a-tab-pane v-for="tab in visibleTabs" :key="tab.key" :title="tab.title" />
      </a-tabs>
    </template>

    <a-spin :loading="loading" class="device-detail__body">
      <a-result v-if="!loading && !device" status="404" title="设备不存在" subtitle="设备可能已被删除或当前账号无权访问"><template #extra><a-button type="primary" @click="router.push('/iot/device')">返回设备列表</a-button></template></a-result>
      <template v-else-if="device && product">
        <DeviceOverviewPanel v-if="activeTab === 'overview'" :device="device" :product="product" @edit="formDrawerRef?.edit(device.id)" @refresh="loadData" />
        <DeviceRealtimePanel v-else-if="activeTab === 'realtime'" :device="device" :product="product" />
        <ProductMetadataPanel v-else-if="activeTab === 'metadata'" :product-id="product.id" :product-key="product.productKey" readonly />
        <DeviceCommandPanel v-else-if="activeTab === 'commands'" :device="device" :product-id="product.id" :source="product.thirdPlatform" :shadow="shadow" />
        <section v-else-if="activeTab === 'shadow'" class="device-detail__shadow"><a-row justify="space-between" align="center"><a-space><h3>设备影子</h3><a-tag>版本 {{ shadow.version || '-' }}</a-tag><span>{{ formatTime(shadow.lastTime) }}</span></a-space><a-button @click="loadShadow"><template #icon><icon-refresh /></template>刷新</a-button></a-row><pre v-html="shadowHtml" /></section>
        <DeviceLogsPanel v-else-if="activeTab === 'logs'" :device="device" :product="product" />
        <GatewaySubDevicesPanel v-else-if="activeTab === 'children'" :device="device" :product="product" />
        <GatewayPollingPanel v-else-if="activeTab === 'polling'" :device="device" :product="product" />
        <DeviceOtaPanel v-else-if="activeTab === 'ota'" :device="device" @open-product-ota="openOtaTask" />
        <DeviceSubscriptionsPanel v-else-if="activeTab === 'subscriptions'" :device="device" />

      </template>
    </a-spin>

    <DeviceFormDrawer ref="formDrawerRef" @save-success="loadData" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { DeviceItem, DeviceShadow } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import Dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import { getDevice, getDeviceShadow } from '@/apis/iot/device'
import { getProductByKey } from '@/apis/iot/product'
import { hasPerm } from '@/utils/permission'
import ProductMetadataPanel from '@/views/iot/product/details/ProductMetadataPanel.vue'
import DeviceFormDrawer from '../modules/DeviceFormDrawer.vue'
import DeviceCommandPanel from './modules/DeviceCommandPanel.vue'
import { normalizeDeviceDetailTab } from './modules/deviceDetailNavigation'
import DeviceLogsPanel from './modules/DeviceLogsPanel.vue'
import DeviceOtaPanel from './modules/DeviceOtaPanel.vue'
import DeviceOverviewPanel from './modules/DeviceOverviewPanel.vue'
import DeviceRealtimePanel from './modules/DeviceRealtimePanel.vue'
import DeviceSubscriptionsPanel from './modules/DeviceSubscriptionsPanel.vue'
import GatewayPollingPanel from './modules/GatewayPollingPanel.vue'
import GatewaySubDevicesPanel from './modules/GatewaySubDevicesPanel.vue'

defineOptions({ name: 'IotDeviceDetail' })

const route = useRoute()
const router = useRouter()
const formDrawerRef = useTemplateRef<InstanceType<typeof DeviceFormDrawer>>('formDrawerRef')
const loading = ref(false)
const device = ref<DeviceItem>()
const product = ref<ProductItem>()
const shadow = reactive<DeviceShadow>({})
const activeTab = ref(normalizeDeviceDetailTab(route.query.tab))
const online = computed(() => device.value?.state === true || device.value?.state === 1 || device.value?.state === '1')
const deviceNode = computed(() => String(product.value?.deviceNode || device.value?.deviceNode || '').toUpperCase())
const normalizedThirdPlatform = computed(() => String(product.value?.thirdPlatform || device.value?.thirdPlatform || '').trim().toLowerCase().replace(/[\s_/-]/g, ''))
function openOtaTask() {
  if (!device.value) return
  void router.push({ name: 'IotOtaTask', query: { create: '1', productKey: device.value.productKey, deviceId: device.value.deviceId } })
}
const tabs = computed(() => [
  { key: 'overview', title: '设备信息', show: true },
  { key: 'realtime', title: '实时状态', show: true },
  { key: 'metadata', title: '物模型', show: true },
  { key: 'commands', title: '功能调用', show: true },
  { key: 'shadow', title: '设备影子', show: true },
  { key: 'logs', title: '日志查看', show: true },
  { key: 'subscriptions', title: '消息订阅', show: true },
  { key: 'children', title: '子设备', show: deviceNode.value === 'GATEWAY' },
  { key: 'polling', title: '网关任务', show: deviceNode.value === 'GATEWAY' && normalizedThirdPlatform.value === 'mqtt' },
  { key: 'ota', title: 'OTA 升级', show: hasPerm('system:ota:list') }
])
const visibleTabs = computed(() => tabs.value.filter((item) => item.show))
const shadowText = computed(() => {
  try {
    const value = typeof shadow.metadata === 'string' ? JSON.parse(shadow.metadata) : shadow.metadata || {}
    return JSON.stringify(value, null, 2)
  } catch {
    return String(shadow.metadata || '{}')
  }
})
const shadowHtml = computed(() => highlightJson(shadowText.value))

function escapeHtml(value: string) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function highlightJson(value: string) {
  const pattern = /"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|\b(?:true|false|null)\b/g
  let result = ''
  let cursor = 0
  for (const match of value.matchAll(pattern)) {
    const index = match.index || 0
    const token = match[0]
    result += escapeHtml(value.slice(cursor, index))
    const isKey = token.startsWith('"') && /^\s*:/.test(value.slice(index + token.length))
    result += `<span class="device-detail__shadow-${isKey ? 'key' : 'value'}">${escapeHtml(token)}</span>`
    cursor = index + token.length
  }
  return result + escapeHtml(value.slice(cursor))
}

async function loadShadow() {
  if (!device.value?.iotId) return
  Object.assign(shadow, (await getDeviceShadow(device.value.iotId)).data || {})
}

async function loadData() {
  loading.value = true
  try {
    device.value = (await getDevice(String(route.params.id))).data
    product.value = (await getProductByKey(device.value.productKey)).data
    await loadShadow()
    if (!visibleTabs.value.some((item) => item.key === activeTab.value)) activeTab.value = 'overview'
  } catch {
    device.value = undefined
    product.value = undefined
  } finally {
    loading.value = false
  }
}

function formatTime(value?: string | number) {
  if (!value) return '尚未同步'
  const text = String(value)
  const date = /^\d{10,13}$/.test(text) ? Dayjs(Number(text) * (text.length === 10 ? 1000 : 1)) : Dayjs(text)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

watch(activeTab, (tab) => {
  if (route.query.tab !== tab) void router.replace({ query: { ...route.query, tab } })
})
watch(() => route.query.tab, (value) => {
  const tab = normalizeDeviceDetailTab(value)
  activeTab.value = device.value && product.value && !visibleTabs.value.some((item) => item.key === tab) ? 'overview' : tab
  if (value && value !== activeTab.value) void router.replace({ query: { ...route.query, tab: activeTab.value } })
}, { immediate: true })
watch(visibleTabs, (tabs) => {
  if (device.value && product.value && !tabs.some((item) => item.key === activeTab.value)) activeTab.value = 'overview'
})
watch(() => route.params.id, loadData)
loadData()
</script>

<style lang="scss" scoped>
.device-detail {
  &__header {
    display: flex;
    gap: 12px;
    align-items: center;
    padding: 16px 20px 12px;
    background: var(--color-bg-2);
  }

  &__identity {
    flex: 1;
    min-width: 0;

    h1 {
      margin: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 18px;
      white-space: nowrap;
    }

    code {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__tabs {
    padding: 0 20px;
    background: var(--color-bg-2);
    border-bottom: 1px solid var(--color-border-2);

    :deep(.arco-tabs-nav::before) {
      display: none;
    }
  }

  &__body {
    box-sizing: border-box;
    flex: 1;
    width: 100%;
    min-height: 0;
    padding: 16px 20px 24px;
    overflow: auto;
  }

  &__shadow {
    h3 {
      margin: 0;
    }

    span {
      font-size: 12px;
      color: var(--color-text-3);
    }

    pre {
      min-height: 360px;
      padding: 16px;
      overflow: auto;
      font-size: 12px;
      line-height: 1.6;
      background: var(--color-fill-1);
      border: 1px solid var(--color-border-1);
      border-radius: 6px;
    }

    :deep(.device-detail__shadow-key) {
      color: var(--color-text-2);
    }

    :deep(.device-detail__shadow-value) {
      color: rgb(var(--primary-6));
    }
  }

  &__entry {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 360px;
    color: var(--color-text-2);

    h3 {
      margin: 14px 0 6px;
    }

    p {
      margin: 0 0 18px;
      color: var(--color-text-3);
    }
  }
}

@media (width <= 640px) {
  .device-detail__header {
    align-items: flex-start;
  }

  .device-detail__body {
    padding: 12px;
  }
}
</style>
