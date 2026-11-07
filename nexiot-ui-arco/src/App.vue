<template>
  <a-config-provider update-at-scroll>
    <router-view></router-view>
    <!-- <template #loading>
      <img src="/static/images/loading.gif" class="loading-icon" />
    </template> -->
  </a-config-provider>
</template>

<script setup lang="ts">
import { useTheme } from '@/hooks'

defineOptions({ name: 'App' })

const { initThemeColor } = useTheme()
initThemeColor()
</script>

<style lang="scss" scoped>
.loading-icon {
  width: 30px;
}
</style>
