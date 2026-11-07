<template>
  <section class="device-ota">
    <a-spin :loading="loading">
      <header class="device-ota__header">
        <h3>设备升级</h3>
        <a-space wrap>
          <a-button type="text" :loading="loading" @click="loadData"><template #icon><icon-refresh /></template>刷新</a-button>
          <a-button v-hasPerm="['system:ota:task:add']" type="primary" @click="emit('open-product-ota')"><template #icon><icon-cloud-upload /></template>创建升级计划</a-button>
        </a-space>
      </header>
      <a-descriptions :column="{ xs: 1, sm: 2 }" bordered>
        <a-descriptions-item label="当前版本">{{ currentVersion || '-' }}</a-descriptions-item>
        <a-descriptions-item label="目标版本">{{ activeTask?.targetVersion || recommendedPackage?.version || '-' }}</a-descriptions-item>
        <a-descriptions-item label="升级状态">{{ activeTask ? taskStatus(activeTask.status) : '暂无进行中的升级' }}</a-descriptions-item>
        <a-descriptions-item label="分包传输">{{ activeTransfer.chunks }}</a-descriptions-item>
      </a-descriptions>
      <a-alert v-if="activeTask && activeTransfer.phase" class="device-ota__notice">{{ activeTransfer.phase }}。文件传完后，还需设备安装并上报升级结果。</a-alert>
      <section class="device-ota__records">
        <h3>最近升级记录</h3>
        <a-timeline v-if="recentRecords.length" mode="left">
          <a-timeline-item v-for="item in recentRecords" :key="item.id" :dot-color="item.status === 'FAILED' ? 'red' : item.status === 'SUCCESS' ? 'green' : 'orange'">
            <strong>{{ item.taskName || item.packageName || '升级计划' }}</strong>
            <span>{{ taskStatus(item.status) }} · {{ item.currentVersion || '-' }} → {{ item.targetVersion || '-' }}</span>
            <span v-if="item.transferState">{{ describeOtaTransfer(item.transferState, item.status).chunks }} · {{ describeOtaTransfer(item.transferState, item.status).phase }}</span>
            <small>最近更新：{{ recordTime(item) }}</small>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="该设备暂无 OTA 升级记录" />
      </section>
    </a-spin>
  </section>
</template>

<script setup lang="ts">
import type { DeviceItem } from '@/apis/iot/device'
import type { OtaDeviceSummary, OtaPackageItem, OtaTaskDeviceItem } from '@/apis/iot/ota'
import { getOtaDeviceSummary, getOtaDeviceVersion } from '@/apis/iot/ota'
import { describeOtaTransfer, otaDeviceStatusLabels } from '@/views/iot/ota/utils'

interface DeviceOtaRecord extends OtaTaskDeviceItem {
  taskName?: string
  packageName?: string
}

defineOptions({ name: 'IotDeviceOtaPanel' })
const props = defineProps<{ device: DeviceItem }>()
const emit = defineEmits<{ (e: 'open-product-ota'): void }>()
const loading = ref(false)
const summary = ref<OtaDeviceSummary>({})
const version = ref<Record<string, unknown>>({})
const currentVersion = computed(() => {
  const summaryVersion = summary.value.currentVersion
  return String(version.value.version || version.value.currentVersion || (typeof summaryVersion === 'string' ? summaryVersion : summaryVersion?.currentVersion) || '')
})
const activeTask = computed(() => summary.value.activeTask as DeviceOtaRecord | undefined)
const activeTransfer = computed(() => describeOtaTransfer(activeTask.value?.transferState, activeTask.value?.status))
const recommendedPackage = computed(() => summary.value.recommendedPackage as OtaPackageItem | undefined)
const recentRecords = computed(() => Array.isArray(summary.value.recentRecords) ? summary.value.recentRecords as DeviceOtaRecord[] : [])
function taskStatus(status?: string) {
  return otaDeviceStatusLabels[status || ''] || status || '未知'
}
function recordTime(record: DeviceOtaRecord) {
  return record.updateTime || record.successTime || record.lastReportTime || record.pushedTime || record.createTime || '-'
}
async function loadData() {
  loading.value = true
  try {
    const [summaryRes, versionRes] = await Promise.all([getOtaDeviceSummary(props.device.productKey, props.device.deviceId), getOtaDeviceVersion(props.device.productKey, props.device.deviceId)])
    summary.value = summaryRes.data || {}
    version.value = versionRes.data || {}
  } finally {
    loading.value = false
  }
}
loadData()
</script>

<style lang="scss" scoped>
.device-ota {
  width: 100%;
  padding: 24px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  > :deep(.arco-spin) {
    display: block;
    width: 100%;
  }

  &__header {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  h3 {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-1);
  }

  &__header h3 {
    margin: 0;
  }

  &__notice,
  &__records {
    margin-top: 20px;
  }

  small,
  :deep(.arco-timeline-item-content span) {
    display: block;
    margin-top: 6px;
    font-size: 13px;
    color: var(--color-text-3);
  }
}

@media (width <= 720px) {
  .device-ota__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
