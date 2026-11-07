<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['protocol:protocol:add']" type="primary" @click="formDrawerRef?.add()"><template #icon><icon-plus /></template>新增驱动</a-button>
        <a-button v-hasPerm="['protocol:protocol:codec']" @click="goToMagicWeb"><template #icon><IconBug /></template>前往调试器</a-button>
        <a-popconfirm content="确定删除选中的驱动吗？" @before-ok="batchDelete"><a-button v-hasPerm="['protocol:protocol:remove']" status="danger" :disabled="!selectedKeys.length"><template #icon><IconDelete /></template>批量删除</a-button></a-popconfirm>
      </a-space>
      <a-space wrap>
        <a-input v-model="query.id" class="protocol-list__search" placeholder="ProductKey" allow-clear @press-enter="search" />
        <a-input v-model="query.name" class="protocol-list__search" placeholder="驱动名称" allow-clear @press-enter="search" />
        <a-select v-model="query.state" class="protocol-list__state" :options="stateOptions" placeholder="发布状态" allow-clear />
        <GiButton type="search" @click="search" /><GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>
    <GiTable
      title="驱动列表"
      row-key="id"
      :data="protocolList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1220 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
      @refresh="refresh"
    />
    <ProtocolFormDrawer ref="formDrawerRef" @save-success="search" />
    <DriverDebugger v-if="debuggerMounted" ref="debuggerRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ProtocolItem } from '@/apis/iot/protocol'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconBug, IconDelete, IconEdit } from '@arco-design/web-vue/es/icon'
import { getProductByKey } from '@/apis/iot/product'
import { deleteProtocols, getProtocolList } from '@/apis/iot/protocol'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import ProtocolFormDrawer from './ProtocolFormDrawer.vue'

defineOptions({ name: 'IotProtocol' })

const DriverDebugger = defineAsyncComponent(() => import('./modules/DriverDebugger.vue'))

const formDrawerRef = useTemplateRef<InstanceType<typeof ProtocolFormDrawer>>('formDrawerRef')
const debuggerMounted = ref(false)
const debuggerRef = useTemplateRef<InstanceType<typeof DriverDebugger>>('debuggerRef')
const pendingDriver = shallowRef<ProtocolItem>()
function goToMagicWeb() {
  window.open('/magic/debug/index.html', '_blank', 'noopener,noreferrer')
}

async function openSource(record: ProtocolItem) {
  if (record.type !== 'magic') {
    formDrawerRef.value?.edit(String(record.id))
    return
  }
  debuggerMounted.value = true
  await nextTick()
  // 异步组件首次加载后，由 watchPostEffect 打开当前请求。
  pendingDriver.value = record
}
watchPostEffect(() => {
  if (debuggerRef.value && pendingDriver.value) {
    const record = pendingDriver.value
    pendingDriver.value = undefined
    debuggerRef.value.open(String(record.id), record.name || '未命名驱动')
  }
})
const query = reactive({ id: '', name: '', state: undefined as number | undefined })
const productNameMap = ref<Record<string, string>>({})
const stateOptions = [{ label: '已发布', value: 1 }, { label: '未发布', value: 0 }]
const typeLabels: Record<string, string> = { 'jar': 'JAR', 'jscript': 'JavaScript', 'magic': 'MagicScript', 'magic-script': 'MagicScript', 'spring': 'Spring Bean' }
const { loading, tableData: protocolList, pagination, selectedKeys, search, refresh, select, selectAll, onDelete } = useTable<ProtocolItem>({ listAPI: (page) => getProtocolList({ ...page, id: query.id.trim() || undefined, name: query.name.trim() || undefined, state: query.state }), deleteAPI: deleteProtocols, immediate: true })
watch(protocolList, (list) => {
  const keys = list.map((item) => String(item.id || '')).filter((key) => key && !productNameMap.value[key])
  if (!keys.length) return
  Promise.all(keys.map(async (key) => {
    try {
      const product = (await getProductByKey(key)).data
      return [key, product?.name || key] as const
    } catch {
      return [key, key] as const
    }
  })).then((entries) => {
    productNameMap.value = { ...productNameMap.value, ...Object.fromEntries(entries) }
  })
}, { immediate: true })
function getProductName(record: ProtocolItem) {
  return record.productName || productNameMap.value[String(record.id)] || record.id || '-'
}
function reset() {
  Object.assign(query, { id: '', name: '', state: undefined })
  search()
}
async function batchDelete() {
  await deleteProtocols(selectedKeys.value.map(String))
  Message.success('删除成功')
  search()
  return true
}
const columns: TableColumnData[] = [
  { title: '驱动名称 / 版本', dataIndex: 'name', width: 240, fixed: 'left', render: ({ record }) => (
    <div class="protocol-list__cell">
      {hasPerm('protocol:protocol:codec')
        ? <Button type="text" class="protocol-list__name" onClick={() => openSource(record as ProtocolItem)}><strong title={record.name || '未命名驱动'}>{record.name || '未命名驱动'}</strong></Button>
        : <strong title={record.name || '未命名驱动'}>{record.name || '未命名驱动'}</strong>}
      <code>{`版本 ${record.version || '-'}`}</code>
    </div>
  ) },
  { title: '所属产品 / ProductKey', dataIndex: 'id', width: 260, render: ({ record }) => (
    <div class="protocol-list__cell">
      <strong title={String(getProductName(record as ProtocolItem))}>{getProductName(record as ProtocolItem)}</strong>
      <code title={record.id}>{record.id}</code>
    </div>
  ) },
  { title: '类型', dataIndex: 'type', width: 130, render: ({ record }) => <Tag size="small" color="arcoblue">{typeLabels[record.type] || record.type}</Tag> },
  { title: '描述', dataIndex: 'description', ellipsis: true, tooltip: true },
  { title: '发布状态', dataIndex: 'state', width: 110, align: 'center', render: ({ record }) => (
    <Tag color={record.state === 1 ? 'green' : 'gray'}>
      <span class="protocol-list__status-dot" />
      {record.state === 1 ? '已发布' : '未发布'}
    </Tag>
  ) },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', width: 138, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('protocol:protocol:codec') && (
        <Tooltip content={record.type === 'magic' ? '调试驱动' : '查看源码'}>
          <Button type="text" shape="circle" aria-label={record.type === 'magic' ? '调试驱动' : '查看源码'} onClick={() => openSource(record as ProtocolItem)}>
            <IconBug />
          </Button>
        </Tooltip>
      )}
      {hasPerm('protocol:protocol:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => formDrawerRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
      {hasPerm('protocol:protocol:remove') && <Popconfirm content="确定删除该驱动吗？" onBeforeOk={() => onDelete(record as ProtocolItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]
</script>

<style lang="scss" scoped>
.protocol-list {
  &__search {
    width: 150px;
  }

  &__state {
    width: 130px;
  }
}

:deep(.protocol-list__cell) {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong,
  code {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  code {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

:deep(.protocol-list__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;

  .arco-btn-content {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

:deep(.protocol-list__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

@media (width <= 768px) {
  .protocol-list {
    &__search,
    &__state {
      width: 100%;
    }
  }
}
</style>
