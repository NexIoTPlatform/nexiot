<template>
  <button type="button" class="rule-name-cell" :aria-label="ariaLabel || `查看${title}`" @click="emit('click')">
    <span class="rule-name-cell__title" :title="title">{{ title || fallback }}</span>
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  fallback?: string
  ariaLabel?: string
}>(), {
  title: '',
  fallback: '未命名',
  ariaLabel: ''
})

const emit = defineEmits<{ click: [] }>()
</script>

<style scoped lang="scss">
.rule-name-cell {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  height: auto;
  padding: 0;
  font: inherit;
  color: rgb(var(--primary-6));
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 2px;

  &:hover {
    color: rgb(var(--primary-5));
  }

  &:focus-visible {
    outline: 2px solid rgb(var(--primary-5));
    outline-offset: 1px;
  }

  &__title {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
    white-space: nowrap;
  }
}
</style>
