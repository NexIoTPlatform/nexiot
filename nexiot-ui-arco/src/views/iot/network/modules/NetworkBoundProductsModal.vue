<template>
  <a-modal
    v-model:visible="visible"
    title="绑定产品"
    width="min(680px, calc(100vw - 32px))"
    :body-style="modalBodyStyle"
    :footer="false"
    align-center
    unmount-on-close
  >
    <div class="bound-products__summary">
      <div>
        <strong>{{ componentName || '通信组件' }}</strong>
        <span>当前绑定关系</span>
      </div>
      <a-tag color="arcoblue">{{ total }} 个产品</a-tag>
    </div>

    <div v-if="products.length" class="bound-products__list">
      <div v-for="product in products" :key="product.productKey" class="bound-products__item">
        <div class="bound-products__avatar">
          <img v-if="productImage(product)" :src="productImage(product)" :alt="product.name || product.productKey">
          <span v-else>{{ productInitial(product) }}</span>
        </div>
        <div class="bound-products__identity">
          <a-space class="bound-products__name-row">
            <a-link v-if="product.id" :title="product.name || product.productKey" @click="openProduct(product)">
              <strong>{{ product.name || product.productKey }}</strong>
            </a-link>
            <strong v-else :title="product.name || product.productKey">{{ product.name || product.productKey }}</strong>
            <a-tag v-if="product.deviceNode" size="small">{{ deviceNodeLabel(product.deviceNode) }}</a-tag>
          </a-space>
          <span>{{ [deviceNodeLabel(product.deviceNode), product.companyNo].filter(Boolean).join(' · ') }}</span>
        </div>
        <a-typography-text copyable class="bound-products__key">
          {{ product.productKey }}
        </a-typography-text>
      </div>
    </div>
    <a-empty v-else description="暂无绑定产品" />

    <a-alert v-if="total > products.length" type="warning" class="bound-products__notice">
      绑定总数为 {{ total }} 个，当前接口返回了 {{ products.length }} 个产品摘要。
    </a-alert>
  </a-modal>
</template>

<script setup lang="ts">
import type { ProductItem } from '@/apis/iot/product'

type BoundProductSummary = Partial<Pick<ProductItem, 'id' | 'name' | 'deviceNode' | 'photoUrl' | 'companyNo' | 'thirdPlatform'>> & Pick<ProductItem, 'productKey'>

defineOptions({ name: 'NetworkBoundProductsModal' })

const visible = ref(false)
const componentName = ref('')
const total = ref(0)
const products = ref<BoundProductSummary[]>([])
const modalBodyStyle = { maxHeight: 'min(70vh, 640px)', overflowY: 'auto' as const }
const router = useRouter()

function deviceNodeLabel(value?: string) {
  return value === 'GATEWAY' ? '网关设备' : value === 'VIDEO_DEVICE' ? '视频设备': value === 'DEVICE' ? '直连设备' : value === 'GATEWAY_SUB_DEVICE' ? '网关子设备' : value || '产品接入'
}

function productImage(product: BoundProductSummary) {
  if (!product.photoUrl) return ''
  if (typeof product.photoUrl !== 'string') return ''
  try {
    const parsed = JSON.parse(product.photoUrl) as { img?: unknown }
    return String(parsed.img || '')
  } catch {
    return product.photoUrl
  }
}

function productInitial(product: BoundProductSummary) {
  return (product.name || product.productKey || '产').slice(0, 1).toUpperCase()
}

function open(name: string, source: BoundProductSummary[], count?: number) {
  componentName.value = name
  products.value = [...new Map(source.filter((item) => item.productKey).map((item) => [item.productKey, item])).values()]
  total.value = Math.max(count || 0, products.value.length)
  visible.value = true
}

function openProduct(product: BoundProductSummary) {
  if (!product.id) return
  visible.value = false
  router.push(`/product/details/${product.id}`)
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.bound-products {
  &__summary {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__summary > div {
    min-width: 0;
  }

  &__summary strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__summary strong,
  &__summary span,
  &__identity strong,
  &__identity span {
    display: block;
  }

  &__summary span,
  &__identity span {
    margin-top: 3px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__list {
    display: grid;
    gap: 8px;
    margin-top: 14px;
  }

  &__item {
    display: grid;
    grid-template-columns: 46px minmax(140px, .8fr) minmax(0, 1.2fr);
    gap: 12px;
    align-items: center;
    padding: 12px;
    background: var(--color-fill-1);
    border-radius: 4px;
  }

  &__avatar {
    display: grid;
    place-items: center;
    width: 46px;
    height: 46px;
    overflow: hidden;
    font-weight: 600;
    color: rgb(var(--primary-6));
    background: var(--color-bg-2);
    border-radius: 6px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__identity,
  &__key {
    min-width: 0;
  }

  &__name-row {
    display: flex;
    min-width: 0;
  }

  &__name-row :deep(.arco-space-item:first-child),
  &__name-row :deep(.arco-link) {
    min-width: 0;
    overflow: hidden;
  }

  &__identity strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__key {
    overflow-wrap: anywhere;
  }

  &__notice {
    margin-top: 14px;
  }
}

@media (width <= 560px) {
  .bound-products__item {
    grid-template-columns: 46px minmax(0, 1fr);
    gap: 8px;
  }

  .bound-products__key {
    grid-column: 2;
  }
}
</style>
