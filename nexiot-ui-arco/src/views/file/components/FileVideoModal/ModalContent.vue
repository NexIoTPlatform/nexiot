<template>
  <video :src="data.src" controls autoplay width="100%" />
</template>

<script setup lang="ts">
import type { FileItem } from '@/apis/file'
defineOptions({ name: 'FileVideoModalContent' })
defineProps<{ data: FileItem }>()
</script>
