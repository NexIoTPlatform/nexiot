<!--
  @file Logo 组件
  @description 系统 Logo 组件，支持折叠状态和首页导航功能
-->
<template>
  <section class="system-logo" :class="{ collapsed: props.collapsed }" @click="handleHomeNavigation">
    <img class="logo" :src="brandingStore.logoUrl" :alt="brandingStore.branding.siteName" />
    <span v-if="!props.hideName" class="system-name g-line-1">{{ brandingStore.branding.siteName }}</span>
  </section>
</template>

<script setup lang="ts">
import { useTenantBrandingStore, useUserStore } from '@/stores'

/** 组件名称 */
defineOptions({ name: 'Logo' })

/** Props 默认值 */
const props = withDefaults(defineProps<Props>(), {
  collapsed: false,
  hideName: false
})

/** Props 类型定义 */
interface Props {
  /** 是否折叠状态 */
  collapsed?: boolean
  /** 是否隐藏名称 */
  hideName?: boolean
}

/** 路由实例 */
const router = useRouter()
const userStore = useUserStore()
const brandingStore = useTenantBrandingStore()

/** 处理首页导航 */
const handleHomeNavigation = () => {
  router.push('/')
}

watch(
  () => [userStore.userInfo.tenantId, userStore.userInfo.currentAdmin] as const,
  ([tenantId, currentAdmin]) => {
    if (tenantId && !currentAdmin) {
      brandingStore.fetchBranding().catch(() => brandingStore.reset())
    } else {
      brandingStore.reset()
    }
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.system-logo {
  box-sizing: border-box;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  height: 56px;
  padding: 0 12px;
  font-size: 20px;
  line-height: 1;
  color: var(--color-text-1);
  cursor: pointer;
  user-select: none;
  transition: padding 0.2s;

  // 折叠状态样式
  &.collapsed {
    justify-content: center;
    padding: 0;

    .system-name {
      display: none;
    }
  }

  // Logo 图片样式
  .logo {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    overflow: hidden;
    border-radius: 6px;
    transition: all 0.2s;
  }

  // 系统名称样式
  .system-name {
    padding-left: 10px;
    white-space: nowrap;
    transition: color 0.3s;

    &:hover {
      color: rgb(var(--primary-6));
    }
  }
}
</style>
