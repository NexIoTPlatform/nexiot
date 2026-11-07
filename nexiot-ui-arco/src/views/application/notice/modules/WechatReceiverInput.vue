<template>
  <div class="wechat-receiver">
    <a-input :model-value="modelValue" placeholder="OpenID 或 user:绑定标识，多个用逗号分隔" @update:model-value="emit('update:modelValue', $event)" />
    <a-button class="wechat-receiver__scan" :disabled="!channelId" @click="visible = true">
      <template #icon><IconWechat /></template>扫码获取 OpenID
    </a-button>
    <p class="wechat-receiver__hint">选择公众号渠道后扫码，获取的 OpenID 会自动添加到接收人。</p>
    <WechatBinding v-if="visible && channelId" :key="channelId" :channel="{ id: channelId, name: channelName || '当前模板公众号' }" select-receiver @bound="handleBound" @close="visible = false" />
  </div>
</template>

<script setup lang="ts">
import type { WechatBinding as BindingResult } from '@/apis/application/notice'
import { Message } from '@arco-design/web-vue'
import { IconWechat } from '@arco-design/web-vue/es/icon'
import WechatBinding from './WechatBinding.vue'

const props = defineProps<{ modelValue?: string, channelId?: string, channelName?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()
const visible = ref(false)
function handleBound(result: BindingResult) {
  if (!result.openid) return
  const receivers = [...(props.modelValue || '').split(','), result.openid].map(value => value.trim()).filter(Boolean)
  emit('update:modelValue', [...new Set(receivers)].join(','))
  visible.value = false
  Message.success('OpenID 已添加到接收人')
}
watch(() => props.channelId, () => { visible.value = false })
</script>

<style lang="scss" scoped>
.wechat-receiver {
  width: 100%;

  &__scan {
    margin-top: 8px;
  }

  &__hint {
    margin-bottom: 0;
    color: var(--color-text-3);
  }
}
</style>
