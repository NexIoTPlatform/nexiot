<template>
  <GiMenuIcon :icon="icon" :size="20" />
</template>

<script setup lang="ts">
import GiMenuIcon from '@/components/GiMenuIcon/GiMenuIcon.vue'

defineOptions({ name: 'MenuIcon' })

withDefaults(defineProps<{
  icon?: string
}>(), {
  icon: ''
})
</script>

<style lang="scss" scoped></style>
