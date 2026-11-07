<template>
  <a-modal
    v-model:visible="visible"
    title="修改头像"
    width="min(720px, calc(100vw - 32px))"
    :body-style="{ maxHeight: 'calc(100vh - 160px)', overflowY: 'auto' }"
    :mask-closable="false"
    align-center
    @before-ok="save"
    @close="close"
  >
    <div class="avatar-crop">
      <div class="avatar-crop__stage">
        <canvas
          ref="canvasRef"
          width="360"
          height="360"
          :class="{ 'avatar-crop__canvas--ready': hasImage }"
          @pointerdown="handlePointerDown"
          @pointermove="handlePointerMove"
          @pointerup="handlePointerUp"
          @pointercancel="handlePointerUp"
        ></canvas>
        <div v-if="!hasImage" class="avatar-crop__empty">
          <icon-image :size="42" />
          <strong>选择一张头像图片</strong>
          <span>支持 JPG、PNG，文件不超过 5 MB</span>
        </div>
        <div v-else class="avatar-crop__mask"></div>
      </div>

      <div class="avatar-crop__controls">
        <input ref="fileInputRef" type="file" accept="image/jpeg,image/png" @change="handleFileChange" />
        <a-button type="primary" @click="fileInputRef?.click()">
          <template #icon><icon-upload /></template>
          选择图片
        </a-button>
        <div class="avatar-crop__zoom">
          <icon-zoom-out />
          <a-slider v-model="zoom" :min="1" :max="3" :step="0.05" :disabled="!hasImage" />
          <icon-zoom-in />
        </div>
        <a-space>
          <a-tooltip content="向左旋转">
            <a-button shape="circle" :disabled="!hasImage" @click="rotate(-1)"><icon-undo /></a-button>
          </a-tooltip>
          <a-tooltip content="向右旋转">
            <a-button shape="circle" :disabled="!hasImage" @click="rotate(1)"><icon-redo /></a-button>
          </a-tooltip>
          <a-tooltip content="重置位置">
            <a-button shape="circle" :disabled="!hasImage" @click="resetTransform"><icon-refresh /></a-button>
          </a-tooltip>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { uploadAccountAvatar } from '../api'
import { useAvatarCropper } from './useAvatarCropper'

defineOptions({ name: 'AccountAvatarCropModal' })
const emit = defineEmits<{ success: [url: string] }>()

const FILE_EXTENSION_RE = /\.[^.]+$/
const visible = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const fileName = ref('avatar.jpg')
const {
  canvasRef,
  hasImage,
  zoom,
  load,
  clear,
  rotate,
  resetTransform,
  handlePointerDown,
  handlePointerMove,
  handlePointerUp,
  toBlob
} = useAvatarCropper()

function open() {
  clear()
  visible.value = true
}

function close() {
  clear()
  if (fileInputRef.value) fileInputRef.value.value = ''
}

async function handleFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    Message.warning('仅支持 JPG、PNG 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    Message.warning('图片不能超过 5 MB')
    return
  }
  fileName.value = `${file.name.replace(FILE_EXTENSION_RE, '') || 'avatar'}.jpg`
  try {
    await load(file)
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '图片读取失败')
  }
}

async function save() {
  if (!hasImage.value) {
    Message.warning('请先选择头像图片')
    return false
  }
  const blob = await toBlob()
  const res = await uploadAccountAvatar(blob, fileName.value)
  Message.success('头像已更新')
  emit('success', res.data.imgUrl)
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.avatar-crop {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(220px, 1fr);
  gap: 24px;
  align-items: center;

  &__stage {
    position: relative;
    width: 100%;
    aspect-ratio: 1;
    overflow: hidden;
    background: var(--color-fill-2);
    border: 1px solid var(--color-border-2);
  }

  canvas {
    display: block;
    width: 100%;
    height: 100%;
    touch-action: none;
    cursor: default;
  }

  &__canvas--ready {
    cursor: grab !important;
  }

  &__canvas--ready:active {
    cursor: grabbing !important;
  }

  &__empty {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: center;
    justify-content: center;
    color: var(--color-text-3);
    text-align: center;

    strong {
      font-size: 15px;
      color: var(--color-text-1);
    }
  }

  &__mask {
    position: absolute;
    inset: 10%;
    pointer-events: none;
    border: 2px solid rgb(var(--primary-6));
    border-radius: 50%;
    box-shadow: 0 0 0 100vmax rgb(0 0 0 / 45%);
  }

  &__controls {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  &__controls>input {
    display: none;
  }

  &__zoom {
    display: grid;
    grid-template-columns: 20px minmax(120px, 1fr) 20px;
    gap: 12px;
    align-items: center;
  }
}

@media (width <= 640px) {
  .avatar-crop {
    grid-template-columns: minmax(0, 1fr);

    &__controls {
      gap: 16px;
    }
  }
}
</style>
