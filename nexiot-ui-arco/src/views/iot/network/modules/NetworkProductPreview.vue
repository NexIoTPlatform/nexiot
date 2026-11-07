<template>
  <button
    type="button"
    class="network-product-preview"
    :class="{ 'network-product-preview--empty': !primaryProduct, 'network-product-preview--disabled': disabled }"
    :disabled="disabled"
    @click="emit('click')"
  >
    <span class="network-product-preview__avatar">
      <img
        v-if="productImage(primaryProduct)"
        :src="productImage(primaryProduct)"
        :alt="primaryProduct?.name || primaryProduct?.productKey"
      >
      <span v-else>{{ productInitial(primaryProduct) }}</span>
    </span>

    <span class="network-product-preview__content">
      <span class="network-product-preview__heading">
        <span>绑定产品</span>
        <span class="network-product-preview__count">{{ total }}</span>
      </span>
      <strong :title="primaryProduct?.name || primaryProduct?.productKey || emptyTitle">
        {{ primaryProduct?.name || primaryProduct?.productKey || emptyTitle }}
      </strong>
      <span class="network-product-preview__meta">
        {{ previewMeta }}
      </span>
    </span>

    <icon-right class="network-product-preview__arrow" />
  </button>
</template>

<script setup lang="ts">
interface ProductPreviewItem {
  productKey: string
  name?: string
  deviceNode?: string
  companyNo?: string
  thirdPlatform?: string
  photoUrl?: unknown
}

const props = withDefaults(defineProps<{
  products?: ProductPreviewItem[]
  total?: number
  emptyTitle?: string
  emptyDescription?: string
  disabled?: boolean
}>(), {
  products: () => [],
  total: 0,
  emptyTitle: '暂未绑定产品',
  emptyDescription: '进入配置后选择产品',
  disabled: false
})

const emit = defineEmits<{ (e: 'click'): void }>()
const primaryProduct = computed(() => props.products[0])

const previewMeta = computed(() => {
  const product = primaryProduct.value
  if (!product) return props.emptyDescription
  const node = deviceNodeLabel(product.deviceNode)
  if (props.total > 1) return `${node} · 另有 ${props.total - 1} 个产品`
  return [node, product.companyNo].filter(Boolean).join(' · ')
})

function deviceNodeLabel(value?: string) {
  if (value === 'GATEWAY') return '网关设备'
  if (value === 'DEVICE') return '直连设备'
  if (value === 'VIDEO_DEVICE') return '视频设备'
  if (value === 'GATEWAY_SUB_DEVICE') return '网关子设备'
  return value || '产品接入'
}

function productImage(product?: ProductPreviewItem) {
  if (!product?.photoUrl) return ''
  if (typeof product.photoUrl === 'object') {
    return String((product.photoUrl as { img?: unknown }).img || '')
  }
  if (typeof product.photoUrl !== 'string') return ''
  try {
    const parsed = JSON.parse(product.photoUrl) as { img?: unknown }
    return String(parsed.img || '')
  } catch {
    return product.photoUrl
  }
}

function productInitial(product?: ProductPreviewItem) {
  return (product?.name || product?.productKey || '产').slice(0, 1).toUpperCase()
}
</script>

<style lang="scss" scoped>
.network-product-preview {
  box-sizing: border-box;
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-width: 0;
  padding: 8px 10px;
  color: var(--color-text-1);
  text-align: left;
  cursor: pointer;
  background: var(--color-fill-1);
  border: 1px solid var(--color-fill-2);
  border-radius: 6px;
  transition: all .2s;

  &:hover:not(:disabled) {
    background: var(--color-fill-2);
    border-color: var(--color-border-2);
  }

  &--disabled {
    cursor: default;
  }

  &__avatar {
    display: flex;
    flex: 0 0 32px;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    overflow: hidden;
    font-size: 13px;
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

  &__content {
    display: block;
    flex: 1;
    min-width: 0;
  }

  &__heading {
    display: flex;
    gap: 6px;
    align-items: center;
    margin-bottom: 2px;
    font-size: 11px;
    color: var(--color-text-3);
  }

  &__count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    font-size: 10px;
    font-weight: 600;
    color: rgb(var(--primary-6));
    background: var(--color-primary-light-1);
    border-radius: 8px;
  }

  strong,
  &__meta {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    font-size: 13px;
    font-weight: 500;
    line-height: 1.3;
  }

  &__meta {
    margin-top: 1px;
    font-size: 11px;
    color: var(--color-text-3);
  }

  &__arrow {
    flex: 0 0 auto;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &--empty &__avatar {
    color: var(--color-text-3);
  }
}
</style>
