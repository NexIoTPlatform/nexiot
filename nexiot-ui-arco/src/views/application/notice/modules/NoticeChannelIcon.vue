<template>
  <span v-if="icon" class="notice-channel-icon" :class="{ 'notice-channel-icon--wechat': type === 'wechat' }" aria-hidden="true" v-html="icon" />
</template>

<script setup lang="ts">
import { getChannelIcon } from './channelConfig'

const props = defineProps<{ type: string }>()
// SVG 仅来自本地渠道常量，不渲染接口或用户传入的 HTML。
const icon = computed(() => getChannelIcon(props.type))
</script>

<style scoped lang="scss">
.notice-channel-icon {
  display: inline-flex;
  flex: 0 0 18px;
  align-items: center;
  width: 18px;
  height: 18px;
  vertical-align: middle;

  &--wechat {
    color: #07c160;
  }

  :deep(svg) {
    width: 100%;
    height: 100%;
  }
}
</style>
