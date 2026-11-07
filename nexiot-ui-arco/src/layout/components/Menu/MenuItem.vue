<template>
  <a-menu-item v-if="!item.children?.length" :key="item?.path">
    <template v-if="hasIcon" #icon>
      <MenuIcon :icon="item?.meta?.icon"></MenuIcon>
    </template>
    <a-row v-if="menuBadgeStore.badgeMap[item.path] !== undefined" justify="space-between" align="center"
      style="flex-wrap: nowrap;">
      <div class="g-line-1">{{ item?.meta?.title }}</div>
      <a-badge class="g-ml" :count="menuBadgeStore.badgeMap[item.path]"
        v-bind="menuBadgeStore.BadgePropsMap[item.path]" />
    </a-row>
    <template v-else>{{ item?.meta?.title }}</template>
  </a-menu-item>

  <a-sub-menu v-else :key="item.path" :title="item?.meta?.title">
    <template v-if="hasIcon" #icon>
      <MenuIcon :icon="item?.meta?.icon"></MenuIcon>
    </template>
    <MenuItem v-for="child in item.children" :key="child.path" :item="child">
    </MenuItem>
  </a-sub-menu>
</template>

<script lang="ts" setup>
import type { RouteRecordRaw } from 'vue-router'
import { useMenuBadgeStore } from '@/stores'
import { resolveMenuIcon } from '@/utils/menuIcon'
import MenuIcon from './MenuIcon.vue'

interface Props {
  item: RouteRecordRaw
}

defineOptions({ name: 'MenuItem' })

const props = defineProps<Props>()

const menuBadgeStore = useMenuBadgeStore()
const hasIcon = computed(() => resolveMenuIcon(props.item.meta?.icon).kind !== 'none')
</script>

<style lang="scss" scoped>
:deep(.arco-badge) {
  display: inline-flex;
}
</style>
