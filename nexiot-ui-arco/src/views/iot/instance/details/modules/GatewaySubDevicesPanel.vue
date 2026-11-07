<template>
  <section class="sub-device-panel">
    <header class="sub-device-panel__toolbar">
      <div>
        <h2>网关拓扑</h2>
        <p>{{ product?.name || device.productName || '网关设备' }} · {{ device.deviceId }}</p>
      </div>
      <a-space wrap>
        <a-button v-if="activeView === 'list' && hasPerm('system:instance:add')" type="primary" @click="formDrawerRef?.add()">
          <template #icon><icon-plus /></template>
          新增子设备
        </a-button>
        <a-button :loading="topologyLoading || loading" @click="refreshAll">
          <template #icon><icon-refresh /></template>
          刷新
        </a-button>
      </a-space>
    </header>

    <a-tabs v-model:active-key="activeView" type="line" class="sub-device-panel__tabs">
      <a-tab-pane key="topology" title="拓扑关系">
        <div class="topology-toolbar">
          <a-space wrap>
            <a-radio-group v-model="topologyState" type="button" size="small">
              <a-radio value="all">全部 {{ topologyDevices.length }}</a-radio>
              <a-radio value="online">在线 {{ onlineCount }}</a-radio>
              <a-radio value="offline">离线 {{ offlineCount }}</a-radio>
            </a-radio-group>
            <a-input v-model="topologyKeyword" allow-clear placeholder="搜索子设备" style="width: 220px">
              <template #prefix><icon-search /></template>
            </a-input>
          </a-space>
          <span class="topology-toolbar__hint">子设备通过网关统一接入</span>
        </div>

        <div class="topology-canvas">
          <article class="gateway-node" @click="openDetail(device)">
            <div class="node-icon node-icon--gateway"><img v-if="gatewayImage" :src="gatewayImage" :alt="device.deviceName || '网关设备'" /><icon-branch v-else /></div>
            <div class="node-copy">
              <strong>{{ device.deviceName || device.deviceId }}</strong>
              <code>{{ device.deviceId }}</code>
              <span>{{ product?.thirdPlatform || product?.transportProtocol || '网关设备' }}</span>
            </div>
            <a-tag :color="isOnline(device) ? 'green' : 'red'">
              <i class="status-dot" :class="isOnline(device) ? 'status-dot--online' : 'status-dot--offline'"></i>
              {{ isOnline(device) ? '在线' : '离线' }}
            </a-tag>
          </article>

          <div class="topology-trunk" aria-hidden="true"><span></span></div>

          <div v-if="topologyLoading" class="topology-empty"><a-spin dot /><span>正在加载拓扑关系</span></div>
          <a-empty v-else-if="!filteredTopologyDevices.length" description="暂无匹配的子设备" />
          <div v-else class="sub-device-scroll">
            <div class="sub-device-bus">
              <div v-for="item in filteredTopologyDevices" :key="`${item.productKey}:${item.deviceId}`" class="child-branch">
                <article class="child-node" :class="{ 'child-node--offline': !isOnline(item) }">
                  <div class="child-node__head">
                    <div class="node-icon node-icon--child"><img v-if="imageUrl(item.photoUrl)" :src="imageUrl(item.photoUrl)" :alt="item.deviceName || '子设备'" /><icon-common v-else /></div>
                    <a-tag size="small" color="arcoblue">子设备</a-tag>
                  </div>
                  <strong>{{ item.deviceName || item.productName || '未命名设备' }}</strong>
                  <code>{{ item.deviceId }}</code>
                  <span class="child-node__product">{{ item.productName || item.productKey || '未知产品' }}</span>
                  <div class="child-node__status">
                    <i class="status-dot" :class="isOnline(item) ? 'status-dot--online' : 'status-dot--offline'"></i>
                    {{ isOnline(item) ? '在线' : '离线' }}
                    <small v-if="item.onlineTime">· {{ fromNow(item.onlineTime) }}</small>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="list" title="子设备列表">
        <div class="sub-device-panel__filters">
          <a-input v-model="query.deviceId" class="sub-device-panel__search" placeholder="DeviceId" allow-clear @press-enter="search" />
          <a-input v-model="query.deviceName" class="sub-device-panel__search" placeholder="设备名称" allow-clear @press-enter="search" />
          <a-input v-model="query.iotId" class="sub-device-panel__iot-search" placeholder="IoT ID" allow-clear @press-enter="search" />
          <a-space wrap>
            <GiButton type="search" @click="search" />
            <GiButton type="reset" @click="reset" />
          </a-space>
        </div>

        <a-table
          row-key="id"
          :data="rows"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          :bordered="{ cell: true }"
          :scroll="{ x: '100%', minWidth: 1095 }"
          class="sub-device-panel__table"
        />
      </a-tab-pane>
    </a-tabs>

    <DeviceFormDrawer ref="formDrawerRef" @save-success="refreshAll" />

    <a-modal
      v-model:visible="gatewayVisible"
      title="更换所属网关"
      width="560px"
      :mask-closable="false"
      :ok-loading="gatewaySaving"
      @before-ok="saveGatewayChange"
      @close="resetGatewayDialog"
    >
      <a-form :model="{ target: gatewayTarget }" layout="vertical">
        <a-form-item label="子设备"><a-input :model-value="selectedDevice ? `${selectedDevice.deviceName || selectedDevice.deviceId} (${selectedDevice.deviceId})` : ''" disabled /></a-form-item>
        <a-form-item label="当前网关"><a-input :model-value="`${device.deviceName || device.deviceId} (${device.deviceId})`" disabled /></a-form-item>
        <a-form-item label="目标网关" required><a-select v-model="gatewayTarget" :options="gatewayOptions" :loading="gatewayLoading" allow-search placeholder="请选择目标网关" /></a-form-item>
      </a-form>
    </a-modal>
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceItem, GatewayDeviceCandidate, GatewaySubDeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconEdit, IconEye, IconSwap } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { getDeviceList, getGatewaySubDeviceRelation, getSubDeviceGatewayCandidates, updateSubDeviceGateway } from '@/apis/iot/device'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import DeviceFormDrawer from '../../modules/DeviceFormDrawer.vue'

defineOptions({ name: 'IotGatewaySubDevicesPanel' })

const props = defineProps<{ device: DeviceItem, product?: ProductItem }>()
const TIMESTAMP_RE = /^\d{10,13}$/
const HTTP_URL_RE = /^https?:\/\//
const router = useRouter()
const formDrawerRef = useTemplateRef<InstanceType<typeof DeviceFormDrawer>>('formDrawerRef')
const activeView = ref('topology')
const topologyLoading = ref(false)
const topologyDevices = ref<GatewaySubDeviceItem[]>([])
const topologyKeyword = ref('')
const topologyState = ref<'all' | 'online' | 'offline'>('all')
const query = reactive({ deviceId: '', deviceName: '', iotId: '' })
const gatewayVisible = ref(false)
const gatewayLoading = ref(false)
const gatewaySaving = ref(false)
const gatewayTarget = ref('')
const selectedDevice = ref<DeviceItem>()
const gatewayCandidates = ref<GatewayDeviceCandidate[]>([])
const gatewayOptions = computed(() => {
  const candidates: GatewayDeviceCandidate[] = [props.device, ...gatewayCandidates.value]
  const unique = new Map<string, GatewayDeviceCandidate>()
  candidates.forEach((item) => unique.set(gatewayValue(item), item))
  return Array.from(unique.values(), (item) => ({ label: `${item.deviceName || item.deviceId} / ${item.productName || item.productKey} (${item.deviceId})`, value: gatewayValue(item) }))
})

const { loading, tableData: rows, pagination, search, refresh } = useTable<DeviceItem>({
  listAPI: (page) => getDeviceList({ ...page, gwProductKey: props.device.productKey, extDeviceId: props.device.deviceId, deviceId: query.deviceId.trim() || undefined, deviceName: query.deviceName.trim() || undefined, iotId: query.iotId.trim() || undefined }),
  immediate: true
})

const onlineCount = computed(() => topologyDevices.value.filter(isOnline).length)
const offlineCount = computed(() => topologyDevices.value.length - onlineCount.value)
const gatewayImage = computed(() => imageUrl(props.product?.photoUrl))
const filteredTopologyDevices = computed(() => topologyDevices.value.filter((item) => {
  const keyword = topologyKeyword.value.trim().toLowerCase()
  const matchesText = !keyword || [item.deviceName, item.deviceId, item.productName, item.productKey].some((value) => String(value || '').toLowerCase().includes(keyword))
  const matchesState = topologyState.value === 'all' || (topologyState.value === 'online' ? isOnline(item) : !isOnline(item))
  return matchesText && matchesState
}))

function gatewayValue(item: Pick<GatewayDeviceCandidate, 'productKey' | 'deviceId'>) {
  return JSON.stringify({ productKey: item.productKey, deviceId: item.deviceId })
}
function isOnline(record: { state?: boolean | number | string }) {
  return record.state === true || record.state === 1 || record.state === '1'
}
function formatTime(value?: string | number) {
  if (!value || value === '0') return '-'
  const text = String(value)
  const date = Dayjs(TIMESTAMP_RE.test(text) ? Number(text) * (text.length === 10 ? 1000 : 1) : text)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text
}
function fromNow(value?: string | number) {
  if (!value || value === '0') return '从未通信'
  const text = String(value)
  const timestamp = TIMESTAMP_RE.test(text) ? Number(text) * (text.length === 10 ? 1000 : 1) : Dayjs(text).valueOf()
  const diff = Date.now() - timestamp
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 2_592_000_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return `${Math.floor(diff / 2_592_000_000)} 个月前`
}
function parseObject(value?: string) {
  try {
    const data = JSON.parse(value || '{}')
    return data && typeof data === 'object' ? data as Record<string, unknown> : {}
  } catch {
    return {}
  }
}
function imageUrl(value?: string) {
  if (!value) return ''
  if (HTTP_URL_RE.test(value)) return value
  try {
    const data = JSON.parse(value) as { img?: string, url?: string }
    return data.img || data.url || ''
  } catch {
    return ''
  }
}
function openDetail(record: Pick<DeviceItem, 'id'>) {
  if (record.id) router.push(`/system/instance/instance-details/${record.id}`)
}
function reset() {
  Object.assign(query, { deviceId: '', deviceName: '', iotId: '' })
  search()
}

async function loadTopology() {
  if (!props.device.productKey || !props.device.deviceId) return
  topologyLoading.value = true
  try {
    topologyDevices.value = (await getGatewaySubDeviceRelation(props.device.productKey, props.device.deviceId)).data || []
  } finally {
    topologyLoading.value = false
  }
}
function refreshAll() {
  refresh()
  loadTopology()
}

async function openGatewayDialog(record: DeviceItem) {
  selectedDevice.value = record
  gatewayTarget.value = gatewayValue(props.device)
  gatewayCandidates.value = []
  gatewayVisible.value = true
  gatewayLoading.value = true
  try {
    gatewayCandidates.value = (await getSubDeviceGatewayCandidates(record.id)).data || []
  } finally {
    gatewayLoading.value = false
  }
}
function resetGatewayDialog() {
  selectedDevice.value = undefined
  gatewayTarget.value = ''
  gatewayCandidates.value = []
}
async function saveGatewayChange() {
  if (!selectedDevice.value || !gatewayTarget.value) {
    Message.warning('请选择目标网关')
    return false
  }
  const value = JSON.parse(gatewayTarget.value) as { productKey: string, deviceId: string }
  gatewaySaving.value = true
  try {
    await updateSubDeviceGateway(selectedDevice.value.id, { gwProductKey: value.productKey, gwDeviceId: value.deviceId })
    Message.success('已更换所属网关')
    await loadTopology()
    refresh()
    return true
  } finally {
    gatewaySaving.value = false
  }
}

const columns: TableColumnData[] = [
  { title: '设备名称 / DeviceId', width: 240, fixed: 'left', render: ({ record }) => (
    <div class="sub-device-cell">
      <Button type="text" onClick={() => openDetail(record as DeviceItem)}><strong>{record.deviceName || '未命名设备'}</strong></Button>
      <code>{record.deviceId}</code>
    </div>
  ) },
  { title: '所属产品 / ProductKey', width: 210, render: ({ record }) => (
    <div class="sub-device-product-cell">
      <strong>{record.productName || '未知产品'}</strong>
      <div>
        <Tag size="small" color="arcoblue">子</Tag>
        <code>{record.productKey}</code>
      </div>
    </div>
  ) },
  { title: 'IoT ID', width: 180, render: ({ record }) => <code class="sub-device-iot-id">{record.iotId || '-'}</code> },
  { title: '设备状态', width: 90, align: 'center', render: ({ record }) => (
    <Tag color={isOnline(record as DeviceItem) ? 'green' : 'red'}>
      <span class="table-status-dot" />
      {isOnline(record as DeviceItem) ? '在线' : '离线'}
    </Tag>
  ) },
  { title: '最近通信', width: 165, render: ({ record }) => (
    <div class="sub-device-communication">
      <strong>{formatTime(record.onlineTime as string | number | undefined)}</strong>
      <span>
        <i class={isOnline(record as DeviceItem) ? 'online' : 'offline'} />
        {fromNow(record.onlineTime as string | number | undefined)}
      </span>
    </div>
  ) },
  { title: '操作', width: 120, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="设备详情"><Button type="text" shape="circle" aria-label="设备详情" onClick={() => openDetail(record as DeviceItem)}><IconEye /></Button></Tooltip>
      {hasPerm('system:instance:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑子设备" onClick={() => formDrawerRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
      {hasPerm('system:instance:edit') && <Tooltip content="更换网关"><Button type="text" shape="circle" aria-label="更换网关" onClick={() => openGatewayDialog(record as DeviceItem)}><IconSwap /></Button></Tooltip>}
    </Space>
  ) }
]

watch(() => [props.device.productKey, props.device.deviceId], loadTopology, { immediate: true })
</script>

<style lang="scss" scoped>
.sub-device-panel {
  width: 100%;
  min-width: 0;
}

.sub-device-panel__toolbar {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 0 0 18px;
  border-bottom: 1px solid var(--color-border-2);

  h2 {
    margin: 0;
    font-size: 18px;
  }

  p {
    margin: 5px 0 0;
    font-family: var(--font-family-code);
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.sub-device-panel__tabs {
  margin-top: 18px;
}

.sub-device-panel__tabs :deep(.arco-tabs-content) {
  border: 0;
}

.topology-toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.topology-toolbar__hint {
  font-size: 12px;
  color: var(--color-text-3);
}

.topology-canvas {
  min-height: 380px;
  padding: 28px clamp(14px, 3vw, 40px) 36px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
}

.gateway-node {
  display: flex;
  gap: 14px;
  align-items: center;
  max-width: 470px;
  min-height: 84px;
  padding: 16px 18px;
  margin: 0 auto;
  cursor: pointer;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-3);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgb(var(--gray-10) / 6%);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.gateway-node:hover {
  border-color: var(--color-border-4);
  box-shadow: 0 4px 12px rgb(var(--gray-10) / 9%);
}

.node-icon {
  display: grid;
  flex: 0 0 44px;
  place-items: center;
  width: 44px;
  height: 44px;
  font-size: 22px;
  border-radius: 8px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: inherit;
  }
}

.node-icon--gateway {
  color: var(--color-text-2);
  background: var(--color-fill-2);
}

.node-icon--child {
  color: rgb(var(--primary-6));
  background: rgb(var(--primary-1));
}

.node-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.node-copy strong,
.child-node strong {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  white-space: nowrap;
}

.node-copy code,
.child-node code {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 11px;
  color: var(--color-text-3);
  white-space: nowrap;
}

.node-copy span {
  font-size: 12px;
  color: var(--color-text-2);
}

.topology-trunk {
  position: relative;
  width: 100%;
  height: 36px;

  &::before {
    position: absolute;
    top: 0;
    left: 50%;
    width: 1px;
    height: 100%;
    content: '';
    background: var(--color-border-3);
  }

  span {
    position: absolute;
    bottom: -2px;
    left: 50%;
    z-index: 1;
    width: 5px;
    height: 5px;
    background: var(--color-text-4);
    border-radius: 50%;
    transform: translateX(-2px);
  }
}

.sub-device-scroll {
  width: 100%;
  padding-bottom: 4px;
  overflow-x: auto;
}

.sub-device-bus {
  --child-node-width: clamp(200px, 13.8vw, 244px);

  position: relative;
  display: flex;
  gap: 16px;
  width: max-content;
  padding-top: 24px;
  margin: 0 auto;

  &::before {
    position: absolute;
    top: 0;
    right: calc(var(--child-node-width) / 2);
    left: calc(var(--child-node-width) / 2);
    height: 1px;
    content: '';
    background: var(--color-border-3);
  }
}

.child-branch {
  position: relative;
  box-sizing: border-box;
  flex: 0 0 var(--child-node-width);
  width: var(--child-node-width);

  &::before {
    position: absolute;
    top: -24px;
    left: 50%;
    width: 1px;
    height: 24px;
    content: '';
    background: var(--color-border-3);
  }
}

.child-node {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  padding: 14px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.child-node--offline {
  background: var(--color-bg-2);
}

.child-node__head {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  .node-icon {
    flex-basis: 34px;
    width: 34px;
    height: 34px;
    font-size: 18px;
  }
}

.child-node > strong,
.child-node > code {
  display: block;
}

.child-node__product {
  display: block;
  margin-top: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--color-text-3);
  white-space: nowrap;
}

.child-node__status {
  display: flex;
  align-items: center;
  margin-top: 14px;
  font-size: 12px;
  color: var(--color-text-2);

  small {
    margin-left: 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    color: var(--color-text-3);
    white-space: nowrap;
  }
}

.status-dot {
  width: 7px;
  height: 7px;
  margin-right: 6px;
  border-radius: 50%;
}

.status-dot--online {
  background: rgb(var(--green-6));
}

.status-dot--offline {
  background: rgb(var(--red-5));
}

.topology-empty {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: var(--color-text-3);
}

.sub-device-panel__filters {
  display: grid;
  grid-template-columns: 180px 200px minmax(240px, 1fr) 150px auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  margin-bottom: 14px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-1);
  border-radius: 6px;
}

.sub-device-panel__search,
.sub-device-panel__iot-search,
.sub-device-panel__address {
  width: 100%;
}

.sub-device-panel__table {
  width: 100%;
}

:deep(.sub-device-cell),
:deep(.sub-device-product-cell) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 14px;
    white-space: nowrap;
  }

  code {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--color-text-3);
    white-space: nowrap;
  }
}

:deep(.sub-device-cell) {
  align-items: flex-start;

  .arco-btn {
    max-width: 100%;
    height: auto;
    padding: 0;
  }
}

:deep(.sub-device-product-cell > div) {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

:deep(.sub-device-iot-id) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--color-text-2);
  white-space: nowrap;
}

:deep(.table-status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

:deep(.sub-device-communication) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-1);
  }

  span {
    display: flex;
    gap: 6px;
    align-items: center;
    font-size: 12px;
    color: var(--color-text-3);
  }

  i {
    flex: 0 0 6px;
    width: 6px;
    height: 6px;
    background: var(--color-text-4);
    border-radius: 50%;

    &.online {
      background: rgb(var(--green-6));
    }

    &.offline {
      background: var(--color-text-4);
    }
  }
}

:deep(.sub-device-panel__table .arco-table-th) {
  height: 44px;
  color: var(--color-text-2);
  background: var(--color-fill-1);
}

:deep(.sub-device-panel__table .arco-table-td) {
  height: 66px;
}

@media (width <= 1100px) {
  .sub-device-panel__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width <= 760px) {
  .sub-device-panel__toolbar,
  .topology-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .sub-device-panel__filters {
    grid-template-columns: 1fr;
  }
}
</style>
