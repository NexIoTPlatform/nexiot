<template>
  <GiPageLayout :body-style="{ padding: 0 }">
    <a-spin :loading="loading" class="detail-page">
      <template v-if="product">
        <header class="detail-header">
          <a-button type="text" shape="circle" aria-label="返回产品列表" @click="goBack"><icon-left /></a-button>
          <div class="detail-header__identity">
            <div><h1>{{ product.name }}</h1><a-tag :color="stateInfo.color">{{ stateInfo.label }}</a-tag></div>
            <span>{{ product.productKey }}</span>
          </div>
          <a-space wrap>
            <a-tag color="arcoblue">{{ product.thirdPlatform || '未配置接入方式' }}</a-tag>
            <a-tag>{{ product.transportProtocol || '未配置传输协议' }}</a-tag>
            <a-tag>{{ deviceCount }} 台设备</a-tag>
            <a-button @click="loadProduct"><template #icon><icon-refresh /></template>刷新</a-button>
          </a-space>
        </header>

        <nav class="detail-tabs">
          <a-tabs v-model:active-key="activeTab" type="line">
            <a-tab-pane key="overview" title="产品信息" />
            <a-tab-pane key="configuration" title="产品配置" />
            <a-tab-pane v-if="hasOperations" key="operations" title="产品操作" />
            <a-tab-pane key="metadata" title="物模型" />
            <a-tab-pane key="devices" title="设备管理" />
            <a-tab-pane v-if="hasConnection" key="connection" title="连接信息" />
            <a-tab-pane key="ota" title="OTA 升级" />
            <a-tab-pane v-if="hasCertificate" key="certificate" title="证书管理" />
          </a-tabs>
        </nav>

        <main class="detail-content">
          
          <ProductOverviewPanel v-if="activeTab === 'overview'" :product="product" :device-count="deviceCount" @edit="formModalRef?.edit(product.id)" @refresh="loadProduct" />
          <ProductConfigurationWorkspace v-else-if="activeTab === 'configuration'" :product="product" @refresh="loadProduct" />
          <ProductOperationsPanel v-else-if="activeTab === 'operations'" :product-id="product.id" />
          <ProductMetadataPanel v-else-if="activeTab === 'metadata'" :product-id="product.id" :product-key="product.productKey" />
          <ProductDevicesPanel v-else-if="activeTab === 'devices'" :product="product" />
          <ProductOtaPanel v-else-if="activeTab === 'ota'" :product="product" />
          <ProductConnectionPanel v-else-if="activeTab === 'connection'" :product="product" @refresh="loadProduct" />
          <ProductCertificatePanel v-else-if="activeTab === 'certificate'" :product="product" @refresh="loadProduct" />
        </main>
      </template>
      <a-result v-else-if="!loading" status="404" title="产品不存在"><template #extra><a-button type="primary" @click="goBack">返回产品列表</a-button></template></a-result>
    </a-spin>
    <ProductFormModal ref="formModalRef" @save-success="loadProduct" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { ProductItem } from '@/apis/iot/product'
import { useRoute, useRouter } from 'vue-router'
import { getProduct, getProductDeviceCounts, getProductOperations } from '@/apis/iot/product'
import { hasPerm } from '@/utils/permission'
import ProductFormModal from '../ProductFormModal.vue'
import ProductCertificatePanel from './ProductCertificatePanel.vue'
import ProductConfigurationWorkspace from './ProductConfigurationWorkspace.vue'
import ProductConnectionPanel from './ProductConnectionPanel.vue'
import ProductDevicesPanel from './ProductDevicesPanel.vue'
import ProductMetadataPanel from './ProductMetadataPanel.vue'
import ProductOperationsPanel from './ProductOperationsPanel.vue'
import ProductOtaPanel from './ProductOtaPanel.vue'

import ProductOverviewPanel from './ProductOverviewPanel.vue'

defineOptions({ name: 'IotProductDetail' })

const route = useRoute()
const router = useRouter()
const formModalRef = useTemplateRef<InstanceType<typeof ProductFormModal>>('formModalRef')
const loading = ref(false)
const activeTab = ref(normalizeTab(route.query.tab))
const product = ref<ProductItem>()
const deviceCount = ref(0)
const hasOperations = ref(false)
const stateInfo = computed(() => [{ label: '已发布', color: 'green' }, { label: '审核中', color: 'orange' }, { label: '已停用', color: 'gray' }][product.value?.state ?? 0] || { label: '未知', color: 'gray' })
const hasConnection = computed(() => ['mqtt'].includes(String(product.value?.thirdPlatform || '').toLowerCase()))
const hasCertificate = computed(() => ['mqtt'].includes(String(product.value?.thirdPlatform || '').toLowerCase()))

async function loadProduct() {
  loading.value = true
  try {
    const id = String(route.params.id)
    const [detailRes, countRes, operationRes] = await Promise.all([getProduct(id, true), getProductDeviceCounts(), getProductOperations(id).catch(() => ({ data: [] }))])
    product.value = detailRes.data
    deviceCount.value = countRes.data?.[detailRes.data.productKey] || Number(detailRes.data.instance || 0)
    hasOperations.value = Array.isArray(operationRes.data) && operationRes.data.length > 0
    if (activeTab.value === 'operations' && !hasOperations.value) activeTab.value = 'overview'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/product/list')
}
function normalizeTab(tab: unknown) {
  return ['overview', 'configuration', 'operations', 'metadata', 'devices', 'ota', 'connection', 'certificate'].includes(String(tab)) ? String(tab) : 'overview'
}
watch(activeTab, (tab) => router.replace({ query: { ...route.query, tab } }))
watch(() => route.query.tab, (tab) => {
  activeTab.value = normalizeTab(tab)
  if (tab === 'algorithm') router.replace({ query: { ...route.query, tab: 'overview' } })
}, { immediate: true })
watch(() => route.params.id, loadProduct, { immediate: true })
</script>

<style lang="scss" scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
  background: var(--color-bg-1);
}

.detail-header {
  display: grid;
  grid-template-columns: auto minmax(220px, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 16px 20px 12px;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border-2);
}

.detail-header__identity > div {
  display: flex;
  gap: 10px;
  align-items: center;
}

.detail-header__identity h1 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0;
}

.detail-header__identity > span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
}

.detail-tabs {
  padding: 0 20px;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border-2);
}

.detail-tabs :deep(.arco-tabs-nav::before) {
  display: none;
}

.detail-content {
  flex: 1;
  min-height: 0;
  padding: 16px 20px 24px;
  overflow: auto;

  > .network-guide {
    margin-bottom: 16px;
  }
}

@media (width <= 900px) {
  .detail-header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .detail-header > .arco-space {
    grid-column: 1 / -1;
    padding-left: 46px;
  }
}
</style>
