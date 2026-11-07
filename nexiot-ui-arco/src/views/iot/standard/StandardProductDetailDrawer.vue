<template>
  <a-drawer v-model:visible="visible" :title="`标准产品详情 - ${product?.productName || ''}`" width="min(1080px, 100vw)" unmount-on-close>
    <a-descriptions v-if="product" :column="2" bordered size="large">
      <a-descriptions-item label="产品名称">{{ product.productName }}</a-descriptions-item>
      <a-descriptions-item label="产品标识"><a-typography-text copyable>{{ product.productKey }}</a-typography-text></a-descriptions-item>
      <a-descriptions-item label="分类路径">{{ product.categoryPath || product.categoryName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="product.status === 1 ? 'green' : 'gray'">{{ product.status === 1 ? '启用' : '禁用' }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="描述" :span="2">{{ product.description || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ product.createTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ product.updateTime || '-' }}</a-descriptions-item>
    </a-descriptions>
    <a-divider>物模型概览</a-divider>
    <ProductMetadataPanel v-if="visible" :model-value="metadata" draft readonly />
  </a-drawer>
</template>

<script setup lang="ts">
import type { StandardProductItem } from '@/apis/iot/standardModel'
import type { MetadataDocument } from '@/views/iot/product/details/metadataSchema'
import { getStandardProduct } from '@/apis/iot/standardModel'

import { parseMetadata } from '@/views/iot/product/details/metadataSchema'

defineOptions({ name: 'StandardProductDetailDrawer' })
const ProductMetadataPanel = defineAsyncComponent(() => import('@/views/iot/product/details/ProductMetadataPanel.vue'))
const visible = ref(false)
const product = ref<StandardProductItem>()
const metadata = ref<MetadataDocument>({ properties: [], events: [], functions: [] })

async function open(id: string) {
  const data = (await getStandardProduct(id)).data
  product.value = data
  metadata.value = parseMetadata(data.metadata || '')
  visible.value = true
}
defineExpose({ open })
</script>
