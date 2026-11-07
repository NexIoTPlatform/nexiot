<template>
  <GiPageLayout margin size="280px" collapsed class="device-list-scope">
    <template #left>
      <div class="group-page__tree-header">
        <div><h3>设备群组</h3><span>{{ flatGroups.length }} 个分组</span></div>
        <a-space :size="2">
          <a-tooltip content="新增根分组"><a-button type="text" shape="circle" aria-label="新增根分组" @click="formModalRef?.addRoot()"><icon-plus /></a-button></a-tooltip>
          <a-tooltip content="刷新分组"><a-button type="text" shape="circle" aria-label="刷新分组" @click="loadGroups"><icon-refresh /></a-button></a-tooltip>
        </a-space>
      </div>
      <a-input-search v-model="groupKeyword" class="group-page__tree-search" placeholder="搜索分组" allow-clear />
      <a-tree v-model:expanded-keys="expandedGroupKeys" block-node :data="filteredGroups" :field-names="{ key: 'id', title: 'groupName', children: 'children' }" :selected-keys="selectedGroupId ? [selectedGroupId] : []" class="group-page__tree" @select="handleGroupSelect">
        <template #title="nodeData"><span class="group-page__tree-title"><icon-folder />{{ nodeData.groupName }}<a-tag v-if="nodeData.deviceCount" size="small">{{ nodeData.deviceCount }}</a-tag></span></template>
      </a-tree>
    </template>

    <section class="group-page__content">
      <header class="group-page__header">
        <div>
          <a-space align="center"><h2>{{ selectedGroup?.groupName || '设备列表' }}</h2><a-tag v-if="selectedGroup" color="arcoblue">ID {{ selectedGroup.id }}</a-tag></a-space>
          <p>{{ selectedGroup?.groupDescribe || '从左侧选择分组后管理其设备' }}</p>
        </div>
        <a-space v-if="selectedGroup && selectedGroup.id !== '0'" :size="4">
          <a-tooltip content="新增子分组"><a-button type="text" shape="circle" aria-label="新增子分组" @click="formModalRef?.addChild(selectedGroup.id)"><icon-folder-add /></a-button></a-tooltip>
          <a-tooltip content="编辑分组"><a-button type="text" shape="circle" aria-label="编辑分组" @click="formModalRef?.edit(selectedGroup.id)"><icon-edit /></a-button></a-tooltip>
          <a-popconfirm content="确定删除当前分组吗？包含子分组或设备时将无法删除。" @before-ok="removeGroup">
            <a-tooltip content="删除分组"><a-button type="text" status="danger" shape="circle" aria-label="删除分组"><icon-delete /></a-button></a-tooltip>
          </a-popconfirm>
        </a-space>
      </header>

      <a-row justify="space-between" class="g-row-tool">
        <a-space wrap>
          <a-button type="primary" :disabled="!canManageDevices" @click="bindDrawerRef?.open(selectedGroupId)"><template #icon><IconLink /></template>绑定设备</a-button>
          <a-popconfirm content="确定批量解绑选中的设备吗？" @before-ok="batchUnbind">
            <a-button status="danger" :disabled="!selectedKeys.length"><template #icon><icon-unlink /></template>批量解绑</a-button>
          </a-popconfirm>
        </a-space>
        <a-space wrap>
          <a-input v-model="query.deviceName" class="device-list__search" placeholder="设备名称" allow-clear @press-enter="search" />
          <a-input v-model="query.deviceId" class="device-list__search" placeholder="DeviceId" allow-clear @press-enter="search" />
          <GiButton type="search" :disabled="!canManageDevices" @click="search" />
          <GiButton type="reset" :disabled="!canManageDevices" @click="resetQuery" />
        </a-space>
      </a-row>

      <GiTable title="分组设备" row-key="id" :data="deviceList" :columns="columns" :loading="loading" :pagination="pagination" :scroll="{ x: '100%', y: '100%', minWidth: 1220 }" :row-selection="{ type: 'checkbox', showCheckedAll: true }" :selected-keys="selectedKeys" @select="select" @select-all="selectAll" @refresh="refresh">
        <template #custom-title><a-space><strong>设备列表</strong><a-tag>{{ pagination.total }} 台</a-tag></a-space></template>
      </GiTable>
    </section>

    <DeviceGroupFormModal ref="formModalRef" :tree-data="groupTree" @save-success="handleGroupSaved" />
    <DeviceGroupBindDrawer ref="bindDrawerRef" @bind-success="refresh" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { DeviceGroupItem, DeviceItem } from '@/apis/iot/device'
import { Button, Message, Popconfirm, Space, Tooltip } from '@arco-design/web-vue'
import { IconEye, IconLink } from '@arco-design/web-vue/es/icon'
import { useRouter } from 'vue-router'
import { deleteDeviceGroup, getDeviceGroup, getDeviceGroups, getGroupDeviceList, unbindDevicesFromGroup } from '@/apis/iot/device'
import { useTable } from '@/hooks'
import DeviceGroupBindDrawer from '../modules/DeviceGroupBindDrawer.vue'
import DeviceGroupFormModal from '../modules/DeviceGroupFormModal.vue'
import { createDeviceListColumns } from '../modules/deviceListColumns'

defineOptions({ name: 'IotDeviceGroup' })

const router = useRouter()
const formModalRef = useTemplateRef<InstanceType<typeof DeviceGroupFormModal>>('formModalRef')
const bindDrawerRef = useTemplateRef<InstanceType<typeof DeviceGroupBindDrawer>>('bindDrawerRef')
const groupTree = ref<DeviceGroupItem[]>([])
const expandedGroupKeys = ref<(string | number)[]>([])
const selectedGroupId = ref('')
const selectedGroup = ref<DeviceGroupItem>()
const groupKeyword = ref('')
const query = reactive({ deviceName: '', deviceId: '' })
const canManageDevices = computed(() => Boolean(selectedGroupId.value && selectedGroupId.value !== '0'))

function filterTree(nodes: DeviceGroupItem[], keyword: string): DeviceGroupItem[] {
  return nodes.flatMap((node) => {
    const children = filterTree(node.children || [], keyword)
    return node.groupName.toLowerCase().includes(keyword) || children.length ? [{ ...node, children }] : []
  })
}

function flatten(nodes: DeviceGroupItem[]): DeviceGroupItem[] {
  return nodes.flatMap((node) => [node, ...flatten(node.children || [])])
}

const flatGroups = computed(() => flatten(groupTree.value).filter((item) => item.id !== '0'))
const filteredGroups = computed(() => groupKeyword.value.trim() ? filterTree(groupTree.value, groupKeyword.value.trim().toLowerCase()) : groupTree.value)

const { loading, tableData: deviceList, pagination, selectedKeys, search, refresh, select, selectAll } = useTable<DeviceItem>({
  listAPI: (page) => getGroupDeviceList({ ...page, groupId: selectedGroupId.value, deviceId: query.deviceId.trim() || undefined, deviceName: query.deviceName.trim() || undefined }),
  immediate: false
})

async function loadGroups() {
  groupTree.value = (await getDeviceGroups()).data || []
  expandedGroupKeys.value = flatten(groupTree.value).map((item) => item.id)
  if (!selectedGroupId.value && groupTree.value.length === 1 && groupTree.value[0].id !== '0') await selectGroup(groupTree.value[0].id)
}

async function selectGroup(id: string) {
  selectedGroupId.value = String(id)
  selectedKeys.value = []
  if (id === '0') {
    selectedGroup.value = groupTree.value.find((item) => item.id === '0')
    deviceList.value = []
    pagination.total = 0
    return
  }
  selectedGroup.value = (await getDeviceGroup(id)).data
  search()
}

function handleGroupSelect(keys: (string | number)[]) {
  const id = keys[0]
  if (id !== undefined) selectGroup(String(id))
}

function resetQuery() {
  Object.assign(query, { deviceName: '', deviceId: '' })
  search()
}

async function removeGroup() {
  if (!selectedGroupId.value) return false
  await deleteDeviceGroup(selectedGroupId.value)
  Message.success('分组删除成功')
  selectedGroupId.value = ''
  selectedGroup.value = undefined
  deviceList.value = []
  await loadGroups()
  return true
}

async function unbindRows(rows: DeviceItem[]) {
  const iotIds = rows.map((item) => item.iotId).filter(Boolean) as string[]
  await unbindDevicesFromGroup(selectedGroupId.value, iotIds)
  Message.success(`已解绑 ${iotIds.length} 台设备`)
  refresh()
  return true
}

function batchUnbind() {
  return unbindRows(deviceList.value.filter((item) => selectedKeys.value.includes(item.id)))
}

async function handleGroupSaved() {
  await loadGroups()
  if (selectedGroupId.value) await selectGroup(selectedGroupId.value)
}

function openDetail(record: DeviceItem) {
  router.push(`/system/instance/instance-details/${record.id}`)
}

const columns: TableColumnData[] = [
  ...createDeviceListColumns(openDetail),
  { title: '操作', width: 110, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="详情"><Button type="text" shape="circle" aria-label="设备详情" onClick={() => openDetail(record as DeviceItem)}><IconEye /></Button></Tooltip>
      <Popconfirm content="确定从当前分组解绑该设备吗？" onBeforeOk={() => unbindRows([record as DeviceItem])}><Tooltip content="解绑"><Button type="text" status="danger" shape="circle" aria-label="解绑设备"><IconLink /></Button></Tooltip></Popconfirm>
    </Space>
  ) }
]

watch(groupKeyword, () => {
  expandedGroupKeys.value = flatten(filteredGroups.value).map((item) => item.id)
})

loadGroups()
</script>

<style lang="scss" scoped>
.group-page {
  &__tree-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    h3 {
      margin: 0;
      font-size: 16px;
    }

    span {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__tree-search {
    margin-bottom: 12px;
  }

  &__tree {
    flex: 1;
    overflow: auto;
  }

  &__tree-title {
    display: flex;
    gap: 6px;
    align-items: center;
  }

  &__content {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
  }

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 2px 0 14px;
    margin-bottom: 14px;
    border-bottom: 1px solid var(--color-border-1);

    h2,
    p {
      margin: 0;
    }

    h2 {
      font-size: 18px;
    }

    p {
      margin-top: 6px;
      font-size: 13px;
      color: var(--color-text-3);
    }
  }
}
</style>

<!-- 与设备列表共享 TSX 单元格样式，限定在 device-list-scope 内。 -->
<style lang="scss" src="@/views/iot/instance/modules/deviceList.scss"></style>
