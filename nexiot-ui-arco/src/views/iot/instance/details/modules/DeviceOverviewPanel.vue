<template>
  <div class="device-overview">
    <!-- 顶部状态栏与快捷操作 -->
    <section class="device-overview__header-card">
      <div class="device-overview__header-left">
        <div class="device-overview__header-title">
          <span class="device-overview__name">{{ device.deviceName || '未命名设备' }}</span>
          <a-tag :color="online ? 'green' : 'gray'" size="small">
            <template #icon>
              <span class="device-overview__status-dot" :class="{ 'is-online': online }" />
            </template>
            {{ online ? '在线' : '离线' }}
          </a-tag>
        </div>
        <div class="device-overview__header-tags">
          <a-tooltip v-if="deviceNodeDisplay.raw" :content="`节点代码: ${deviceNodeDisplay.raw}`">
            <a-tag color="blue" size="small">{{ deviceNodeDisplay.label }}</a-tag>
          </a-tooltip>
          <a-tooltip v-if="platformDisplay.label" :content="platformDisplay.tip || `接入方式: ${platformDisplay.raw}`">
            <a-tag :color="platformDisplay.color || 'arcoblue'" size="small">{{ platformDisplay.label }}</a-tag>
          </a-tooltip>
          <a-tooltip v-if="transportDisplay.label" :content="`通信协议: ${transportDisplay.raw}`">
            <a-tag color="purple" size="small">{{ transportDisplay.label }}</a-tag>
          </a-tooltip>
          <a-tag v-for="group in device.devGroupName || []" :key="group" color="green" size="small">
            <template #icon><icon-folder /></template>
            {{ group }}
          </a-tag>
        </div>
      </div>
      <a-space class="device-overview__header-actions">
        <a-button type="primary" size="small" @click="emit('edit')">
          <template #icon><icon-edit /></template>
          编辑设备
        </a-button>
        <a-button type="outline" size="small" @click="ownerVisible = true">
          <template #icon><icon-user /></template>
          修改归属
        </a-button>
      </a-space>
    </section>

    <!-- 基本信息 -->
    <section class="device-overview__card">
      <div class="device-overview__card-title">
        <icon-info-circle class="device-overview__card-icon" />
        <span>基本信息</span>
      </div>
      <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" bordered size="medium" class="device-overview__descriptions">
        <a-descriptions-item v-for="item in basicFields" :key="item.label" :label="item.label">
          <div class="device-overview__desc-content">
            <code v-if="item.code" class="device-overview__code">{{ item.value || '-' }}</code>
            <span v-else class="device-overview__text" :class="{ 'is-empty': !item.value || item.value === '-' }">
              {{ item.value || '-' }}
            </span>
            <a-tooltip v-if="item.copy && item.value" content="复制">
              <a-button type="text" size="mini" class="device-overview__copy-btn" aria-label="复制" @click="copy(item.copy)">
                <template #icon><icon-copy /></template>
              </a-button>
            </a-tooltip>
            <span v-if="item.badge" class="device-overview__badge-text">{{ item.badge }}</span>
          </div>
        </a-descriptions-item>
      </a-descriptions>
    </section>

    <!-- 运维与归属信息 -->
    <section class="device-overview__card">
      <div class="device-overview__card-title">
        <icon-settings class="device-overview__card-icon" />
        <span>运维与位置</span>
      </div>
      <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" bordered size="medium" class="device-overview__descriptions">
        <a-descriptions-item v-for="item in operationFields" :key="item.label" :label="item.label" :span="item.full ? 3 : 1">
          <div class="device-overview__desc-content">
            <code v-if="item.code" class="device-overview__code">{{ item.value || '-' }}</code>
            <span v-else class="device-overview__text" :class="{ 'is-empty': !item.value || item.value === '-' }">
              {{ item.value || '-' }}
            </span>
            <a-tooltip v-if="item.copy && item.value" content="复制">
              <a-button type="text" size="mini" class="device-overview__copy-btn" aria-label="复制" @click="copy(item.copy)">
                <template #icon><icon-copy /></template>
              </a-button>
            </a-tooltip>
          </div>
        </a-descriptions-item>
      </a-descriptions>
    </section>

    <!-- 连接信息 -->
    <section v-if="connectionItems.length" class="device-overview__card">
      <div class="device-overview__card-title">
        <icon-wifi class="device-overview__card-icon" />
        <span>连接信息</span>
        <span class="device-overview__card-subtitle">设备接入平台所需的网络地址与安全认证凭据</span>
      </div>
      <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" bordered size="medium" class="device-overview__descriptions">
        <a-descriptions-item v-for="item in connectionItems" :key="item.label" :label="item.label">
          <div class="device-overview__desc-content">
            <code v-if="item.code || item.secret" class="device-overview__code">
              {{ item.secret && !showSecret ? mask(item.value) : item.value || '-' }}
            </code>
            <span v-else class="device-overview__text">{{ item.value || '-' }}</span>
            <a-tooltip v-if="item.secret" :content="showSecret ? '隐藏密钥' : '显示密钥'">
              <a-button type="text" size="mini" class="device-overview__copy-btn" @click="showSecret = !showSecret">
                <template #icon><icon-eye v-if="!showSecret" /><icon-eye-invisible v-else /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="item.value" content="复制">
              <a-button type="text" size="mini" class="device-overview__copy-btn" @click="copy(item.value)">
                <template #icon><icon-copy /></template>
              </a-button>
            </a-tooltip>
          </div>
        </a-descriptions-item>
      </a-descriptions>
    </section>

    <DeviceAccessParameters :device="device" :product="product" @edit="emit('edit')" />

    <!-- 修改归属弹窗 -->
    <a-modal
      v-model:visible="ownerVisible"
      title="修改设备归属"
      :mask-closable="false"
      width="460px"
      @before-ok="saveOwner"
    >
      <a-form :model="ownerForm" layout="vertical">
        <a-form-item label="归属账号" required>
          <a-select
            v-model="ownerForm.unionId"
            :options="ownerOptions"
            allow-search
            placeholder="请选择新的归属账号"
            @popup-visible-change="loadOwners"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import type { ListItem as UserItem } from '@/apis/system/user'
import { Message } from '@arco-design/web-vue'
import Dayjs from 'dayjs'
import { updateDeviceOwner } from '@/apis/iot/device'
import { getProductConfigDicts, getProductConnectInfo } from '@/apis/iot/product'
import { getUserList } from '@/apis/system/user'
import { copyText } from '@/utils'
import DeviceAccessParameters from './DeviceAccessParameters.vue'

defineOptions({ name: 'IotDeviceOverviewPanel' })

const props = defineProps<{
  device: DeviceItem
  product: ProductItem
}>()

const emit = defineEmits<{
  (e: 'edit'): void
  (e: 'refresh'): void
}>()

const REGEX_PLATFORM_KEY = /[\s_/-]/g
const REGEX_TIMESTAMP = /^\d{10,13}$/

const ownerVisible = ref(false)
const ownerForm = reactive({ unionId: '' })
const owners = ref<UserItem[]>([])
const connection = ref<Record<string, unknown>>({})
const showSecret = ref(false)

const dictNodeOptions = ref<{ label: string, value: string }[]>([])
const dictTransportOptions = ref<{ label: string, value: string }[]>([])

// 节点类型本地兜底字典
const LOCAL_NODE_MAP: Record<string, string> = {
  DEVICE: '直连设备',
  GATEWAY: '网关设备',
  GATEWAY_SUB_DEVICE: '网关子设备',
  EDGE_CONTROLLER: '边缘控制器'
}

// 接入方式平台中文映射
const PLATFORM_MAP: Record<string, { label: string, color: string, tip?: string }> = {
  mqtt: { label: 'MQTT 接入', color: 'arcoblue', tip: '标准 MQTT 物联网通信' },
  http: { label: 'HTTP 接入', color: 'cyan', tip: 'HTTP/HTTPS 报文接入' }
}
const LOCAL_TRANSPORT_MAP: Record<string, string> = { MQTT: 'MQTT 协议', HTTP: 'HTTP 协议' }
const online = computed(() => props.device.state === true || props.device.state === 1 || props.device.state === '1')

const ownerOptions = computed(() => owners.value.map((item) => ({
  label: `${item.nickname || item.username} (${item.username})`,
  value: item.unionId || item.username
})))

// 节点类型展示
const deviceNodeDisplay = computed(() => {
  const raw = String(props.device.deviceNode || props.product.deviceNode || '').trim()
  if (!raw) return { label: '-', raw: '' }
  const dict = dictNodeOptions.value.find((item) => item.value === raw)
  const label = dict?.label || LOCAL_NODE_MAP[raw.toUpperCase()] || raw
  return { label, raw }
})

// 接入方式平台展示
const platformDisplay = computed(() => {
  const raw = String(props.device.thirdPlatform || props.product.thirdPlatform || '').trim()
  if (!raw) return { label: '', raw: '', color: 'gray' }
  const key = raw.toLowerCase().replace(REGEX_PLATFORM_KEY, '')
  const item = PLATFORM_MAP[key]
  return {
    label: item ? item.label : raw,
    raw,
    color: item?.color || 'arcoblue',
    tip: item?.tip
  }
})

// 传输协议展示
const transportDisplay = computed(() => {
  const raw = String(props.product.transportProtocol || '').trim()
  if (!raw) return { label: '', raw: '' }
  const dict = dictTransportOptions.value.find((item) => item.value === raw)
  const label = dict?.label || LOCAL_TRANSPORT_MAP[raw.toUpperCase()] || raw
  return { label, raw }
})

// 基础标识信息
const basicFields = computed(() => [
  { label: '所属产品', value: props.device.productName || props.product.name, copy: props.device.productName || props.product.name },
  { label: 'ProductKey', value: props.device.productKey, copy: props.device.productKey, code: true },
  { label: 'DeviceId', value: props.device.deviceId, copy: props.device.deviceId, code: true },
  { label: 'IoT ID', value: props.device.iotId, copy: props.device.iotId, code: true },
  { label: '节点类型', value: deviceNodeDisplay.value.label, badge: deviceNodeDisplay.value.raw !== deviceNodeDisplay.value.label ? deviceNodeDisplay.value.raw : '' },
  { label: '接入方式', value: platformDisplay.value.label || '-' },
  { label: '传输协议', value: transportDisplay.value.label || '-' },
  { label: '厂商简称', value: props.product.companyNo || '-' }
])

// 运维与位置信息
const operationFields = computed(() => [
  { label: '归属账号', value: props.device.creatorName || props.device.creatorId || '未分配' },
  { label: '所属网关', value: props.device.gwDeviceInfo || props.device.gwDeviceId || '-', copy: props.device.gwDeviceInfo || props.device.gwDeviceId, code: Boolean(props.device.gwDeviceId) },
  { label: '设备位置 (经纬度)', value: formatLocation(props.device.longitude, props.device.latitude) },
  { label: '注册时间', value: formatTime(props.device.registryTime) },
  { label: '最后通信时间', value: formatTime(props.device.onlineTime) },
  { label: '创建时间', value: formatTime(props.device.createTime) },
  { label: '备注说明', value: props.device.detail || '-', full: true }
])

// 连接信息
const connectionItems = computed(() => {
  const info = parseObject(connection.value)
  const mqtt = parseObject(info.mqtt)
  const network = parseObject(info.network)
  return [
    { label: '接入地址', value: stringValue(mqtt.host || network.host || info.host), code: true },
    { label: '通信端口', value: stringValue(mqtt.port || network.port || info.port), code: true },
    { label: 'Client ID', value: stringValue(mqtt.clientId || info.clientId), code: true },
    { label: '接入账号', value: stringValue(mqtt.username || info.username) },
    { label: '设备密钥', value: props.device.secretKey || '', secret: true, code: true }
  ].filter((item) => item.value)
})

function parseObject(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? '' : String(value)
}

function formatLocation(lon?: string | number, lat?: string | number): string {
  if (lat === null || lon === null || lon === undefined || lat === undefined || lon === '' || lat === '') return '-'
  return `${lon},${lat}`
}

function formatTime(value?: string | number): string {
  if (!value) return '-'
  const text = String(value)
  const date = REGEX_TIMESTAMP.test(text) ? Dayjs(Number(text) * (text.length === 10 ? 1000 : 1)) : Dayjs(text)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}

function mask(value: string): string {
  if (!value) return '-'
  return value.length <= 4 ? '******' : `${value.slice(0, 2)}******${value.slice(-2)}`
}

async function copy(value?: string) {
  if (!value) return
  try {
    await copyText(value)
    Message.success('已复制到剪贴板')
  } catch {
    Message.error('复制失败')
  }
}

async function loadOwners(open: boolean) {
  if (!open || owners.value.length) return
  owners.value = (await getUserList({ page: 1, size: 500 })).data.records || []
}

async function saveOwner() {
  if (!ownerForm.unionId) {
    Message.warning('请选择归属账号')
    return false
  }
  await updateDeviceOwner(props.device.id, { unionId: ownerForm.unionId })
  Message.success('设备归属修改成功')
  emit('refresh')
  return true
}

// 异步加载连接信息与字典
getProductConnectInfo(props.device.productKey).then((res) => {
  connection.value = res.data as Record<string, unknown>
}).catch(() => {
  connection.value = {}
})

getProductConfigDicts(['instance_node', 'transport_protocol']).then((res) => {
  if (res.data) {
    dictNodeOptions.value = (res.data.instance_node || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    dictTransportOptions.value = (res.data.transport_protocol || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
  }
}).catch(() => {})
</script>

<style lang="scss" scoped>
.device-overview {
  display: flex;
  flex-direction: column;
  gap: 14px;

  &__header-card {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: var(--color-bg-2);
    border: 1px solid var(--color-border-2);
    border-radius: 6px;
  }

  &__header-left {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 16px;
    align-items: center;
  }

  &__header-title {
    display: inline-flex;
    gap: 8px;
    align-items: center;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-1);
  }

  &__status-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    margin-right: 4px;
    background-color: var(--color-text-4);
    border-radius: 50%;

    &.is-online {
      background-color: rgb(var(--success-6));
      box-shadow: 0 0 0 2px rgb(var(--success-6) / 20%);
    }
  }

  &__header-tags {
    display: inline-flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
  }

  &__header-actions {
    margin-left: auto;
  }

  &__card {
    padding: 14px 16px;
    background: var(--color-bg-2);
    border: 1px solid var(--color-border-2);
    border-radius: 6px;
  }

  &__card-title {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.4;
    color: var(--color-text-1);
  }

  &__card-icon {
    font-size: 15px;
    color: rgb(var(--primary-6));
  }

  &__card-subtitle {
    margin-left: 4px;
    font-size: 12px;
    font-weight: normal;
    color: var(--color-text-3);
  }

  &__text {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.is-empty {
      color: var(--color-text-4);
    }
  }

  &__code {
    min-width: 0;
    max-width: 100%;
    padding: 1px 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: var(--font-mono, Consolas, Monaco, monospace);
    font-size: 12px;
    white-space: nowrap;
    background: var(--color-fill-2);
    border: 1px solid var(--color-border-2);
    border-radius: 3px;
  }

  &__copy-btn {
    flex-shrink: 0;
    color: var(--color-text-3);

    &:hover {
      color: rgb(var(--primary-6));
    }
  }

  &__badge-text {
    flex-shrink: 0;
    margin-left: 4px;
    font-size: 11px;
    color: var(--color-text-4);
  }

  &__descriptions {
    :deep(.arco-descriptions-item-label) {
      width: 130px;
      font-size: 12px;
      color: var(--color-text-3);
      background-color: var(--color-fill-1);
    }

    :deep(.arco-descriptions-item-value) {
      font-size: 13px;
      color: var(--color-text-1);
    }
  }

  &__desc-content {
    display: flex;
    gap: 6px;
    align-items: center;
  }

}

@media (width <= 640px) {
  .device-overview__header-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .device-overview__header-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
