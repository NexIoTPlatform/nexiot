<template>
  <a-row :justify="hasActions ? 'space-between' : 'end'" align="center" class="g-row-tool rule-page-toolbar" :class="{ 'has-actions': hasActions }">
    <a-space v-if="hasActions" wrap class="rule-page-toolbar__actions">
      <slot name="actions" />
    </a-space>
    <a-space wrap class="rule-page-toolbar__filters">
      <slot />
    </a-space>
  </a-row>
</template>

<script setup lang="ts">
const slots = useSlots()
const hasActions = computed(() => Boolean(slots.actions))
</script>

<style scoped lang="scss">
.rule-page-toolbar {
  gap: 12px;

  &__filters {
    :deep(.arco-input-wrapper),
    :deep(.arco-select-view) {
      width: 180px;
    }

    :deep(.arco-picker) {
      width: 290px;
    }
  }
}

@media (width <= 768px) {
  .rule-page-toolbar {
    &.has-actions {
      flex-direction: column;
      align-items: flex-start;
    }

    &__filters {
      width: 100%;

      :deep(.arco-input-wrapper),
      :deep(.arco-select-view),
      :deep(.arco-picker) {
        width: 100%;
      }
    }
  }
}
</style>
