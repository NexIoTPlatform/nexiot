<template>
  <GiPageLayout title="工作台">
    <a-row justify="space-between" align="center" class="g-row-tool"><h2>NexIoT 设备概览</h2><a-button :loading="loading" @click="loadOverview"><template #icon><icon-refresh /></template>刷新</a-button></a-row>
    <a-spin :loading="loading">
      <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" bordered>
        <a-descriptions-item label="产品总数">{{ overview.productStats?.totalProducts || 0 }}</a-descriptions-item>
        <a-descriptions-item label="设备总数">{{ overview.deviceStats?.totalDevices || 0 }}</a-descriptions-item>
        <a-descriptions-item label="在线设备">{{ overview.deviceStats?.onlineDevices || 0 }}</a-descriptions-item>
        <a-descriptions-item label="离线设备">{{ overview.deviceStats?.offlineDevices || 0 }}</a-descriptions-item>
        <a-descriptions-item label="在线率">{{ overview.deviceStats?.onlineRate || 0 }}%</a-descriptions-item>
        <a-descriptions-item label="应用总数">{{ overview.applicationStats?.totalApplications || 0 }}</a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { DashboardOverview } from './types'
import { getDashboardOverview } from './api'

defineOptions({ name: 'Home' })
const overview = ref<DashboardOverview>({})
const loading = ref(false)
async function loadOverview() {
  loading.value = true
  try {
    overview.value = (await getDashboardOverview()).data || {}
  } finally {
    loading.value = false
  }
}
onMounted(loadOverview)
</script>
