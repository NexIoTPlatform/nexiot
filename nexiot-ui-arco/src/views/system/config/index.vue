<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton v-hasPerm="['system:config:add']" type="add" @click="formModalRef?.add()" />
        <GiButton v-hasPerm="['system:config:remove']" type="delete" @click="onBatchDelete" />
        <a-popconfirm content="确定刷新参数缓存吗?" @before-ok="handleRefreshCache">
          <a-button v-hasPerm="['system:config:remove']">
            <template #icon><icon-refresh /></template>
            刷新缓存
          </a-button>
        </a-popconfirm>
      </a-space>
      <a-space wrap>
        <a-input v-model="query.name" placeholder="参数名称" allow-clear />
        <a-input v-model="query.key" placeholder="参数键名" allow-clear />
        <a-select v-model="query.system" :options="systemOptions" placeholder="参数类型" allow-clear />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <a-table
      class="g-table"
      row-key="id"
      :data="configList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1200 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
    />

    <ConfigFormModal ref="formModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ConfigItem } from '@/apis/system/config'
import { Message, Tag } from '@arco-design/web-vue'
import { deleteConfig, getConfigList, refreshConfigCache } from '@/apis/system/config'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import ConfigFormModal from './ConfigFormModal.vue'

defineOptions({ name: 'SystemConfig' })

const formModalRef = useTemplateRef<InstanceType<typeof ConfigFormModal>>('formModalRef')
const systemOptions = [{ label: '是', value: true }, { label: '否', value: false }]
const query = reactive({ name: '', key: '', system: undefined as boolean | undefined })

const { loading, tableData: configList, pagination, selectedKeys, search, select, selectAll, fixed, onDelete, onBatchDelete } = useTable<ConfigItem>({
  listAPI: (page) => getConfigList({
    ...page,
    name: query.name.trim() || undefined,
    key: query.key.trim() || undefined,
    system: query.system
  }),
  deleteAPI: deleteConfig,
  immediate: true
})

function reset() {
  Object.assign(query, { name: '', key: '', system: undefined })
  search()
}

async function handleRefreshCache() {
  await refreshConfigCache()
  Message.success('参数缓存刷新成功')
  return true
}

const columns: TableColumnData[] = [
  { title: '参数名称', dataIndex: 'name', width: 180, ellipsis: true, tooltip: true },
  { title: '参数键名', dataIndex: 'key', width: 220, ellipsis: true, tooltip: true },
  { title: '参数键值', dataIndex: 'value', width: 220, ellipsis: true, tooltip: true },
  {
    title: '系统内置',
    width: 100,
    align: 'center',
    render: ({ record }) => record.system ? <Tag color="blue">是</Tag> : <Tag>否</Tag>
  },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  {
    title: '操作',
    width: 92,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => (
      <GiTableActions actions={[
        { key: 'edit', label: '编辑', icon: 'edit', visible: hasPerm('system:config:edit'), onClick: () => formModalRef.value?.edit(String(record.id)) },
        { key: 'delete', label: '删除', icon: 'delete', status: 'danger', visible: hasPerm('system:config:remove'), confirm: '确定删除该参数吗?', onClick: () => onDelete(record as ConfigItem) }
      ]}
      />
    )
  }
]
</script>

<style lang="scss" scoped></style>
