<template>
  <a-card title="图标选择器" :bordered="true">
    <GiIconSelector v-model="icon" style="max-width: 380px"></GiIconSelector>
  </a-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'GiIconSelectorDemo' })

const icon = ref('')
</script>
