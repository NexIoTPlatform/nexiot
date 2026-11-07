<template>
  <section class="gateway-panel">
    <header class="gateway-panel__header">
      <div><strong><icon-branch /> 推荐网关</strong><p>添加网关子设备时优先推荐兼容的网关产品。</p></div>
      <a-space>
        <a-button v-if="product.gwProductKey" v-hasPerm="['system:product:edit']" type="text" status="danger" @click="removeGateway"><template #icon><icon-delete /></template>移除</a-button>
        <a-button v-hasPerm="['system:product:edit']" type="text" @click="openSelector"><template #icon><icon-swap /></template>{{ product.gwProductKey ? '更换推荐' : '添加推荐' }}</a-button>
      </a-space>
    </header>
    <div v-if="product.gwProductKey" class="gateway-product">
      <div class="gateway-product__image"><img v-if="gatewayImage" :src="gatewayImage" :alt="product.gwName" /><icon-branch v-else /></div>
      <div><strong>{{ product.gwName || product.gwProductKey }}</strong><code>{{ product.gwProductKey }}</code></div>
    </div>
    <a-empty v-else description="暂未设置推荐网关产品" />

    <a-modal v-model:visible="visible" title="选择推荐网关" width="680px" :mask-closable="false" @before-ok="saveGateway">
      <a-input-search v-model="keyword" placeholder="搜索产品名称或 ProductKey" allow-clear />
      <a-radio-group v-model="selectedKey" class="gateway-options">
        <a-radio v-for="item in filteredGateways" :key="item.productKey" :value="item.productKey">
          <span class="gateway-option"><strong>{{ item.name }}</strong><code>{{ item.productKey }}</code></span>
        </a-radio>
      </a-radio-group>
      <a-empty v-if="!filteredGateways.length" description="暂无可用网关产品" />
    </a-modal>
  </section>
</template>

<script setup lang="ts">
import type { ProductItem } from '@/apis/iot/product'
import { Message, Modal } from '@arco-design/web-vue'
import { getGatewayProducts, updateProductGateway } from '@/apis/iot/product'

defineOptions({ name: 'ProductGatewayPanel' })

const props = defineProps<{ product: ProductItem }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()
const visible = ref(false)
const keyword = ref('')
const selectedKey = ref('')
const gateways = ref<ProductItem[]>([])
const filteredGateways = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return text ? gateways.value.filter((item) => `${item.name} ${item.productKey}`.toLowerCase().includes(text)) : gateways.value
})
const gatewayImage = computed(() => {
  if (!props.product.gwPhotoUrl) return ''
  try {
    const data = JSON.parse(props.product.gwPhotoUrl)
    return data.img || data.url || ''
  } catch {
    return props.product.gwPhotoUrl
  }
})

async function openSelector() {
  gateways.value = (await getGatewayProducts()).data || []
  selectedKey.value = props.product.gwProductKey || ''
  keyword.value = ''
  visible.value = true
}

async function saveGateway() {
  if (!selectedKey.value) {
    Message.error('请选择网关产品')
    return false
  }
  await updateProductGateway(props.product.id, selectedKey.value)
  Message.success('推荐网关保存成功')
  emit('refresh')
  return true
}

function removeGateway() {
  Modal.warning({
    title: '移除推荐网关',
    content: '确定移除当前推荐网关吗？',
    hideCancel: false,
    onOk: async () => {
      await updateProductGateway(props.product.id)
      Message.success('推荐网关已移除')
      emit('refresh')
    }
  })
}
</script>

<style lang="scss" scoped>
.gateway-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 14px;

    strong {
      display: flex;
      gap: 7px;
      align-items: center;
      font-size: 15px;
    }

    p {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}

.gateway-product {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: var(--color-fill-1);
  border-radius: 6px;

  &__image {
    display: grid;
    place-items: center;
    width: 48px;
    height: 48px;
    overflow: hidden;
    font-size: 24px;
    border: 1px solid var(--color-border-2);
    border-radius: 6px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  strong,
  code {
    display: block;
  }

  code {
    margin-top: 5px;
    color: var(--color-text-3);
  }
}

.gateway-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 420px;
  margin-top: 12px;
  overflow: auto;
}

.gateway-option {
  display: inline-flex;
  gap: 12px;
  align-items: center;

  code {
    color: var(--color-text-3);
  }
}
</style>
