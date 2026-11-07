<template>
  <GiPageLayout margin>
    <a-result status="info" :title="title" subtitle="该菜单已纳入 Vue 3 迁移计划，当前尚未完成实现。">
      <template #extra>
        <a-button type="primary" @click="router.push('/index')">返回工作台</a-button>
      </template>
    </a-result>
  </GiPageLayout>
</template>

<script setup lang="ts">
defineOptions({ name: 'MigrationPending' })

const route = useRoute()
const router = useRouter()
const title = computed(() => String(route.meta.title || '功能迁移中'))
</script>

<style lang="scss" scoped></style>
