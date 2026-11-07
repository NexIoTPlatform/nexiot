<template>
  <section class="configuration-workspace">
    <div class="configuration-workspace__navigation">
      <a-tabs v-model:active-key="activeSection" type="rounded">
        <a-tab-pane key="access" title="接入配置" />
        <a-tab-pane key="storage" title="数据留存" />
        <a-tab-pane key="thirdParty" title="第三方参数" />
      </a-tabs>
    </div>

    <div class="configuration-workspace__content">
      <ProductConfigurationPanel v-if="activeSection === 'access'" :product="product" @refresh="emit('refresh')" />
      <ProductStoragePanel v-else-if="activeSection === 'storage'" :product="product" @refresh="emit('refresh')" />
      <ProductOtherConfigurationPanel v-else :product="product" @refresh="emit('refresh')" />
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ProductItem } from '@/apis/iot/product'
import ProductConfigurationPanel from './ProductConfigurationPanel.vue'
import ProductOtherConfigurationPanel from './ProductOtherConfigurationPanel.vue'
import ProductStoragePanel from './ProductStoragePanel.vue'

defineOptions({ name: 'ProductConfigurationWorkspace' })

defineProps<{ product: ProductItem }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()
const activeSection = ref('access')
</script>

<style lang="scss" scoped>
.configuration-workspace {
  min-width: 0;

  &__navigation {
    padding: 8px 12px 0;
    background: var(--color-bg-2);
    border: 1px solid var(--color-border-2);
    border-bottom: 0;
    border-radius: 6px 6px 0 0;
  }

  &__navigation :deep(.arco-tabs-content) {
    display: none;
  }

  &__content :deep(> section),
  &__content :deep(> div > section) {
    border-radius: 0 0 6px 6px;
  }
}

@media (width <= 600px) {
  .configuration-workspace__navigation {
    overflow-x: auto;
  }
}
</style>
