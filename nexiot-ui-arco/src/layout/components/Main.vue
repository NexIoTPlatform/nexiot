<!--
  @file Main 组件
  @description 主内容区组件，支持路由切换动画、页面缓存和组件重载功能
-->
<template>
  <a-layout class="main">
    <a-watermark :content="brandingStore.watermarkContent" class="main__watermark">
      <router-view v-slot="{ Component, route }">
        <transition :name="appStore.pageTransitionName" mode="out-in" appear>
          <keep-alive :include="(tabsStore.cacheList as string[])">
            <component :is="Component" v-if="tabsStore.reloadFlag" :key="route.path" />
          </keep-alive>
        </transition>
      </router-view>
    </a-watermark>
  </a-layout>
</template>

<script setup lang="ts">
import { useAppStore, useTabsStore, useTenantBrandingStore } from '@/stores'

/** 组件名称 */
defineOptions({ name: 'AppMain' })

/** 状态管理 */
const appStore = useAppStore()
const tabsStore = useTabsStore()
const brandingStore = useTenantBrandingStore()
</script>

<style lang="scss" scoped>
.main {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: hidden;

  &__watermark {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
    min-height: 0;
  }
}
</style>
