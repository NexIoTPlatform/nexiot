<template>
  <GiPageLayout margin :body-style="{ overflowY: 'auto' }">
    <a-row justify="space-between" class="g-row-tool mqtt-page__toolbar">
      <a-space wrap>
        <a-button
          v-hasPerm="['network:mqtt:add']"
          type="primary"
          @click="formDrawerRef?.add()"
        >
          <template #icon><icon-plus /></template>
          新增 MQTT 组件
        </a-button>
        <a-popconfirm
          content="确定删除选中的 MQTT 组件吗？"
          @before-ok="batchDelete"
        >
        <a-button
          v-if="viewMode === 'list'"
          v-hasPerm="['network:mqtt:del']"
            status="danger"
            :disabled="!selectedKeys.length"
          >
            <template #icon><IconDelete /></template>
            批量删除
          </a-button>
        </a-popconfirm>
      </a-space>
      <a-space wrap>
        <a-input
          v-model.trim="query.name"
          placeholder="组件名称"
          allow-clear
          @press-enter="search"
        />
        <a-input
          v-model.trim="query.productKey"
          placeholder="ProductKey"
          allow-clear
          @press-enter="search"
        />
        <a-select
          v-model="query.type"
          :options="typeOptions"
          placeholder="MQTT 类型"
          allow-clear
        />
        <a-select
          v-model="query.state"
          :options="stateOptions"
          placeholder="启用状态"
          allow-clear
        />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="resetQuery" />
        <a-radio-group v-model="viewMode" type="button">
          <a-radio value="list">
            <a-tooltip content="列表视图"><icon-list /></a-tooltip>
          </a-radio>
          <a-radio value="card">
            <a-tooltip content="卡片视图"><icon-apps /></a-tooltip>
          </a-radio>
        </a-radio-group>
      </a-space>
    </a-row>

    <a-spin
      v-if="viewMode === 'card'"
      :loading="loading"
      class="mqtt-page__view"
    >
      <div v-if="tableData.length" class="mqtt-page__grid">
        <article
          v-for="item in tableData"
          :key="item.id"
          class="mqtt-card"
          tabindex="0"
          @click="openDetail(item)"
          @keydown.enter="openDetail(item)"
        >
          <header class="mqtt-card__header">
            <div class="mqtt-card__title-row">
              <a-tooltip :content="item.name || '未命名组件'">
                <h3 class="mqtt-card__title">{{ item.name || '未命名组件' }}</h3>
              </a-tooltip>
              <a-tag size="small" :color="item.running ? 'green' : 'gray'">
                {{ item.running ? '运行中' : configured(item) ? '已停止' : '未配置' }}
              </a-tag>
            </div>
            <div class="mqtt-card__meta-row">
              <a-tag size="small" color="arcoblue">{{ typeLabel(item.type) }}</a-tag>
              <code v-if="item.type" class="mqtt-card__type-code">{{ item.type }}</code>
            </div>
          </header>

          <div class="mqtt-card__facts">
            <div>
              <span>Broker 地址</span>
              <strong :title="brokerAddress(item)">{{ brokerAddress(item) }}</strong>
            </div>
            <div>
              <span>主题配置</span>
              <strong class="mqtt-card__fact-action" @click.stop="openTopics(item)">
                {{ topicCount(item) }} 条 <icon-right />
              </strong>
            </div>
            <div>
              <span>关联产品</span>
              <strong>{{ productCount(item) ? `${productCount(item)} 个` : '未绑定' }}</strong>
            </div>
          </div>

          <div class="mqtt-card__product-bar" @click.stop="handleProductPreview(item)">
            <span class="mqtt-card__product-label"><icon-link /> 绑定产品</span>
            <span class="mqtt-card__product-name" :title="primaryMqttProductName(item)">
              {{ primaryMqttProductName(item) }}
            </span>
            <span v-if="productCount(item) > 1" class="mqtt-card__product-extra">
              +{{ productCount(item) - 1 }}
            </span>
            <icon-right class="mqtt-card__product-arrow" />
          </div>

          <footer class="mqtt-card__footer">
            <div class="mqtt-card__status">
              <span class="mqtt-card__status-dot" :class="item.state === 1 ? 'is-enabled' : 'is-disabled'" />
              <span>{{ item.state === 1 ? '已启用' : '已停用' }}</span>
            </div>
            <a-space :size="2">
              <a-popconfirm
                v-if="item.running"
                content="确定停止该组件吗？"
                @before-ok="() => changeRuntime(item, 'stop')"
              >
                <a-button
                  v-hasPerm="['network:mqtt:stop']"
                  type="text"
                  size="small"
                  status="warning"
                  shape="circle"
                  aria-label="停止"
                  @click.stop
                >
                  <IconPause />
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                v-else
                content="确定启动该组件吗？"
                @before-ok="() => changeRuntime(item, 'start')"
              >
                <a-button
                  v-hasPerm="['network:mqtt:start']"
                  type="text"
                  size="small"
                  status="success"
                  shape="circle"
                  aria-label="启动"
                  :disabled="item.state !== 1 || !configured(item)"
                  @click.stop
                >
                  <IconPlayArrow />
                </a-button>
              </a-popconfirm>
              <a-tooltip content="编辑基础配置">
                <a-button
                  v-hasPerm="['network:mqtt:edit']"
                  type="text"
                  size="small"
                  shape="circle"
                  aria-label="编辑基础配置"
                  @click.stop="formDrawerRef?.edit(item.id)"
                >
                  <template #icon><IconEdit /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm
                v-hasPerm="['network:mqtt:restart']"
                content="确定重启该组件吗？"
                @before-ok="() => changeRuntime(item, 'restart')"
              >
                <a-tooltip :content="item.state === 1 ? '重启' : '组件已停用'">
                  <a-button type="text" size="small" shape="circle" aria-label="重启" :disabled="item.state !== 1" @click.stop>
                    <IconRefresh />
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
              <a-popconfirm
                content="确定删除该组件吗？"
                @before-ok="() => onDelete(item)"
              >
                <a-tooltip content="删除组件">
                  <a-button
                    v-hasPerm="['network:mqtt:del']"
                    type="text"
                    size="small"
                    shape="circle"
                    status="danger"
                    aria-label="删除组件"
                    @click.stop
                  >
                    <template #icon><IconDelete /></template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </a-space>
          </footer>
        </article>
      </div>
      <a-empty v-else description="暂无 MQTT 组件" />
      <a-pagination v-bind="pagination" class="mqtt-page__pagination" />
    </a-spin>

    <a-table
      v-else
      class="g-table"
      row-key="id"
      :loading="loading"
      :data="tableData"
      :columns="columns"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1300 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
    />

    <MqttFormDrawer
      ref="formDrawerRef"
      @save-success="refresh"
    />
    <MqttDetailDrawer
      ref="detailDrawerRef"
      @edit="handleDetailEdit"
      @updated="refresh"
    />
    <NetworkBoundProductsModal ref="boundProductsRef" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type {
  MqttNetworkItem,
  MqttNetworkType
} from '@/apis/iot/mqtt'
import {
  Button,
  Message,
  Popconfirm,
  Space,
  Tag,
  Tooltip
} from '@arco-design/web-vue'
import {
  IconDelete,
  IconEdit,
  IconEye,
  IconPause,
  IconPlayArrow,
  IconRefresh
} from '@arco-design/web-vue/es/icon'
import { useStorage } from '@vueuse/core'
import {
  deleteMqttNetworks,
  getMqttNetwork,
  getMqttNetworkList,
  parseMqttConfiguration,
  parseMqttTopicMappings,
  restartMqttNetwork,
  startMqttNetwork,
  stopMqttNetwork
} from '@/apis/iot/mqtt'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import NetworkBoundProductsModal from '../modules/NetworkBoundProductsModal.vue'
import MqttDetailDrawer from './modules/MqttDetailDrawer.vue'
import MqttFormDrawer from './modules/MqttFormDrawer.vue'

defineOptions({ name: 'IotNetworkMqtt' })

const formDrawerRef
  = useTemplateRef<InstanceType<typeof MqttFormDrawer>>('formDrawerRef')
const detailDrawerRef
  = useTemplateRef<InstanceType<typeof MqttDetailDrawer>>('detailDrawerRef')
const boundProductsRef
  = useTemplateRef<InstanceType<typeof NetworkBoundProductsModal>>('boundProductsRef')
const productsLoadingId = ref('')
const viewMode = useStorage<'card' | 'list'>('nexiot-network-mqtt-view-mode', 'list')
const query = reactive({
  name: '',
  productKey: '',
  type: undefined as MqttNetworkType | undefined,
  state: undefined as number | undefined
})
const typeOptions = [
  { label: 'MQTT 服务端', value: 'MQTT_SERVER' },
  { label: 'MQTT 客户端', value: 'MQTT_CLIENT' }
]
const stateOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

function listMqttNetworks(page: { page: number, size: number }) {
  return getMqttNetworkList({
    ...page,
    name: query.name || undefined,
    productKey: query.productKey || undefined,
    type: query.type,
    state: query.state
  })
}

const {
  loading,
  tableData,
  pagination,
  selectedKeys,
  search,
  refresh,
  select,
  selectAll,
  onDelete
} = useTable<MqttNetworkItem>({
  listAPI: listMqttNetworks,
  deleteAPI: deleteMqttNetworks,
  immediate: true,
  defaultPageSize: 12
})
let statusSyncTimer: number | undefined
let statusSyncing = false

async function syncRuntimeStatus() {
  if (document.hidden || statusSyncing) return
  statusSyncing = true
  try {
    const { data } = await listMqttNetworks({
      page: pagination.current,
      size: pagination.pageSize
    })
    tableData.value = data.records
  } catch {
    // The regular table request remains responsible for user-facing errors.
  } finally {
    statusSyncing = false
  }
}

onMounted(() => {
  statusSyncTimer = window.setInterval(syncRuntimeStatus, 15000)
})

onUnmounted(() => {
  if (statusSyncTimer) window.clearInterval(statusSyncTimer)
})

function resetQuery() {
  Object.assign(query, { name: '', productKey: '', type: undefined, state: undefined })
  search()
}

async function batchDelete() {
  await deleteMqttNetworks(selectedKeys.value.map(String))
  Message.success('删除成功')
  search()
  return true
}

function typeLabel(type: MqttNetworkType) {
  return type === 'MQTT_CLIENT' ? 'MQTT 客户端' : 'MQTT 服务端'
}

function configured(item: MqttNetworkItem) {
  return Boolean(parseMqttConfiguration(item).host)
}

function brokerAddress(item: MqttNetworkItem) {
  const config = parseMqttConfiguration(item)
  if (!config.host && !config.port) return '未配置'
  if (config.host && config.port && !config.host.includes(`:${config.port}`)) {
    return `${config.host}:${config.port}`
  }
  return config.host || `:${config.port}`
}

function topicCount(item: MqttNetworkItem) {
  return parseMqttTopicMappings(parseMqttConfiguration(item)).length
}

function productCount(item: MqttNetworkItem) {
  return Math.max(
    item.bindProductCount || 0,
    item.bindProducts?.length || 0
  )
}

function mqttProducts(item: MqttNetworkItem) {
  return item.bindProducts || []
}

function openDetail(item: MqttNetworkItem) {
  detailDrawerRef.value?.open(String(item.id))
}

function openTopics(item: MqttNetworkItem) {
  detailDrawerRef.value?.open(String(item.id), 'topics')
}

async function openProducts(item: MqttNetworkItem) {
  productsLoadingId.value = item.id
  try {
    const current = (await getMqttNetwork(item.id)).data
    boundProductsRef.value?.open(
      current.name || item.name,
      current.bindProducts || [],
      productCount(current)
    )
  } finally {
    productsLoadingId.value = ''
  }
}

function primaryMqttProductName(item: MqttNetworkItem) {
  const products = mqttProducts(item)
  return products[0]?.name || products[0]?.productKey || (productCount(item) ? `${productCount(item)} 个产品` : '未绑定产品')
}

function handleProductPreview(item: MqttNetworkItem) {
  if (productCount(item)) openProducts(item)
  else openTopics(item)
}

function handleDetailEdit(id: string) {
  formDrawerRef.value?.edit(id)
}

async function changeRuntime(
  item: MqttNetworkItem,
  action: 'start' | 'stop' | 'restart'
) {
  const actions = {
    start: startMqttNetwork,
    stop: stopMqttNetwork,
    restart: restartMqttNetwork
  }
  await actions[action](String(item.id))
  Message.success('运行操作已提交，正在同步状态')
  await pollRuntime(item.id, action !== 'stop')
  return true
}

function pollRuntime(id: string, expectedRunning: boolean, remaining = 8) {
  if (remaining <= 0) return Promise.resolve()
  return new Promise<void>((resolve) => {
    window.setTimeout(async () => {
      await refresh()
      const current = tableData.value.find((item) => String(item.id) === String(id))
      if (current && Boolean(current.running) === expectedRunning) {
        Message.success(expectedRunning ? '组件已连接' : '组件已停止')
        resolve()
        return
      }
      await pollRuntime(id, expectedRunning, remaining - 1)
      resolve()
    }, 1000)
  })
}

const columns: TableColumnData[] = [
  {
    title: '组件名称',
    width: 220,
    render: ({ record }) => (
      <Button type="text" onClick={() => openDetail(record as MqttNetworkItem)}>
        <strong>{record.name}</strong>
      </Button>
    )
  },
  {
    title: '类型',
    width: 140,
    render: ({ record }) => (
      <Tag color="arcoblue">{typeLabel(record.type)}</Tag>
    )
  },
  {
    title: 'Broker 地址',
    width: 220,
    ellipsis: true,
    tooltip: true,
    render: ({ record }) => brokerAddress(record as MqttNetworkItem)
  },
  {
    title: '主题映射',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Button type="text" onClick={() => openTopics(record as MqttNetworkItem)}>
        {`${topicCount(record as MqttNetworkItem)} 条`}
      </Button>
    )
  },
  {
    title: '绑定产品',
    width: 100,
    align: 'center',
    render: ({ record }) => productCount(record as MqttNetworkItem)
      ? (
          <Button type="text" onClick={() => openProducts(record as MqttNetworkItem)}>
            {`${productCount(record as MqttNetworkItem)} 个`}
          </Button>
        )
      : <Button type="text" onClick={() => openTopics(record as MqttNetworkItem)}>去绑定</Button>
  },
  {
    title: '启用状态',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag color={record.state === 1 ? 'green' : 'gray'}>
        {record.state === 1 ? '启用' : '停用'}
      </Tag>
    )
  },
  {
    title: '运行状态',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag color={record.running ? 'green' : 'gray'}>
        {record.running ? '运行中' : configured(record as MqttNetworkItem) ? '已停止' : '未配置'}
      </Tag>
    )
  },
  {
    title: '创建时间',
    width: 180,
    render: ({ record }) => record.createDateStr || record.createDate || '-'
  },
  {
    title: '操作',
    width: 250,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        <Tooltip content="详情">
          <Button
            type="text"
            shape="circle"
            aria-label="详情"
            onClick={() => openDetail(record as MqttNetworkItem)}
          >
            <IconEye />
          </Button>
        </Tooltip>
        {hasPerm('network:mqtt:edit') && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              aria-label="编辑"
              onClick={() => formDrawerRef.value?.edit(String(record.id))}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {record.running
          ? hasPerm('network:mqtt:stop') && (
            <Popconfirm
              content="确定停止该组件吗？"
              onBeforeOk={() => changeRuntime(record as MqttNetworkItem, 'stop')}
            >
              <Tooltip content="停止">
                <Button type="text" status="warning" shape="circle" aria-label="停止">
                  <IconPause />
                </Button>
              </Tooltip>
            </Popconfirm>
          )
          : hasPerm('network:mqtt:start') && (
            <Popconfirm
              content="确定启动该组件吗？"
              onBeforeOk={() => changeRuntime(record as MqttNetworkItem, 'start')}
            >
              <Tooltip content="启动">
                <Button
                  type="text"
                  status="success"
                  shape="circle"
                  aria-label="启动"
                  disabled={record.state !== 1 || !configured(record as MqttNetworkItem)}
                >
                  <IconPlayArrow />
                </Button>
              </Tooltip>
            </Popconfirm>
          )}
        {hasPerm('network:mqtt:restart') && (
          <Popconfirm
            content="确定重启该组件吗？"
            onBeforeOk={() => changeRuntime(record as MqttNetworkItem, 'restart')}
          >
            <Tooltip content="重启">
              <Button type="text" shape="circle" aria-label="重启" disabled={record.state !== 1}>
                <IconRefresh />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
        {hasPerm('network:mqtt:del') && (
          <Popconfirm
            content="确定删除该组件吗？"
            onBeforeOk={() => onDelete(record as MqttNetworkItem)}
          >
            <Tooltip content="删除">
              <Button type="text" status="danger" shape="circle" aria-label="删除">
                <IconDelete />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]
</script>

<style lang="scss" scoped>
.mqtt-page {
  &__toolbar {
    gap: 12px;
  }

  &__view {
    display: block;
    padding: 2px 4px 4px 0;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 14px;
    align-items: stretch;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  &__subtitle {
    padding-left: 15px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.mqtt-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 14px 14px 12px;
  cursor: pointer;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  box-shadow: 0 1px 4px 0 rgb(0 0 0 / 4%), 0 2px 6px rgb(0 0 0 / 2%);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover,
  &:focus-visible {
    outline: none;
    border-color: rgb(var(--primary-5));
    box-shadow: 0 6px 18px -2px rgb(var(--primary-6) / 12%), 0 3px 8px rgb(0 0 0 / 4%);
    transform: translateY(-2px);

    .mqtt-card__title {
      color: rgb(var(--primary-6));
    }
  }

  &__header {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
  }

  &__title-row {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
  }

  &__title {
    flex: 1;
    min-width: 0;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 15px;
    font-weight: 600;
    line-height: 22px;
    color: var(--color-text-1);
    white-space: nowrap;
    transition: color 0.15s ease;
  }

  &__meta-row {
    display: flex;
    gap: 6px;
    align-items: center;
    min-width: 0;
  }

  &__type-code {
    display: inline-block;
    max-width: 100%;
    padding: 0 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: var(--font-family-code, monospace);
    font-size: 11px;
    line-height: 18px;
    color: var(--color-text-3);
    white-space: nowrap;
    background: var(--color-fill-2);
    border-radius: 4px;
  }

  &__facts {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px 8px;
    padding: 8px 10px;
    margin: 10px 0 8px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-fill-2);
    border-radius: 6px;

    div {
      min-width: 0;
    }

    span {
      display: block;
      margin-bottom: 2px;
      font-size: 11px;
      line-height: 16px;
      color: var(--color-text-3);
    }

    strong {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 12px;
      font-weight: 500;
      line-height: 18px;
      color: var(--color-text-1);
      white-space: nowrap;
    }
  }

  &__fact-action {
    display: inline-flex;
    gap: 2px;
    align-items: center;
    color: rgb(var(--primary-6)) !important;
    cursor: pointer;
    transition: opacity 0.15s ease;

    &:hover {
      opacity: 0.8;
    }

    svg {
      flex-shrink: 0;
      font-size: 10px;
    }
  }

  &__product-bar {
    display: flex;
    gap: 8px;
    align-items: center;
    padding: 6px 10px;
    margin-bottom: 8px;
    font-size: 12px;
    cursor: pointer;
    background: var(--color-fill-1);
    border: 1px solid var(--color-fill-2);
    border-radius: 6px;
    transition: all 0.2s;

    &:hover {
      background: var(--color-fill-2);
      border-color: var(--color-border-2);
    }
  }

  &__product-label {
    display: inline-flex;
    flex-shrink: 0;
    gap: 4px;
    align-items: center;
    font-size: 11px;
    color: var(--color-text-3);
  }

  &__product-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-weight: 500;
    color: var(--color-text-1);
    white-space: nowrap;
  }

  &__product-extra {
    display: inline-flex;
    flex-shrink: 0;
    align-items: center;
    height: 16px;
    padding: 0 4px;
    font-size: 10px;
    font-weight: 600;
    color: rgb(var(--primary-6));
    background: var(--color-primary-light-1);
    border-radius: 8px;
  }

  &__product-arrow {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__footer {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
    padding-top: 8px;
    margin-top: auto;
    border-top: 1px solid var(--color-border-1);
  }

  &__status {
    display: inline-flex;
    gap: 6px;
    align-items: center;
    font-size: 12px;
    color: var(--color-text-2);
  }

  &__status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;

    &.is-enabled {
      background-color: rgb(var(--success-6));
    }

    &.is-disabled {
      background-color: var(--color-text-4);
    }
  }
}

@media (width <= 720px) {
  .mqtt-page__grid {
    grid-template-columns: 1fr;
  }
}

@media (width > 720px) and (width <= 1200px) {
  .mqtt-page__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width > 1200px) and (width <= 1560px) {
  .mqtt-page__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
