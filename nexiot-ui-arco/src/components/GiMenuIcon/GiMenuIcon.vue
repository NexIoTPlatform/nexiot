<template>
  <Icon v-if="resolved.kind === 'iconify'" :icon="resolved.value" :width="size" :height="size" />
  <component :is="resolved.value" v-else-if="resolved.kind === 'arco'" :size="size" />
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { resolveMenuIcon } from '@/utils/menuIcon'

defineOptions({ name: 'GiMenuIcon' })

const props = withDefaults(defineProps<{
  icon?: string
  size?: number
}>(), {
  icon: '',
  size: 20
})

const resolved = computed(() => resolveMenuIcon(props.icon))
</script>

<style lang="scss" scoped></style>
