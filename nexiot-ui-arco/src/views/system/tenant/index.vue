<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <GiButton v-hasPerm="['system:tenant:add']" type="add" @click="handleAdd" />
        <GiButton v-hasPerm="['system:tenant:remove']" type="delete" @click="onBatchDelete" />
      </a-space>
      <a-space wrap>
        <a-input v-model="query.tenantName" placeholder="租户名称" allow-clear />
        <a-input v-model="query.tenantCode" placeholder="租户编码" allow-clear />
        <a-input v-model="query.domain" placeholder="登录域名" allow-clear />
        <a-select v-model="query.status" :options="statusOptions" placeholder="状态" allow-clear />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <a-table
      class="g-table"
      row-key="tenantId"
      :loading="loading"
      :data="tenantList"
      :columns="columns"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1700 }"
      :pagination="pagination"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
    />

    <TenantFormModal ref="tenantFormModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { TenantListItem } from '@/apis/system/tenant'
import { Message, Space, Switch, Tag } from '@arco-design/web-vue'
import { changeTenantStatus, deleteTenant, getTenantList } from '@/apis/system/tenant'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import TenantFormModal from './TenantFormModal.vue'
import { splitTenantDomains } from './utils'

defineOptions({ name: 'SystemTenant' })

const tenantFormModalRef = useTemplateRef<InstanceType<typeof TenantFormModal>>('tenantFormModalRef')
const statusOptions = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 }
]
const query = reactive({ tenantName: '', tenantCode: '', domain: '', status: undefined as number | undefined })

const {
  loading,
  tableData: tenantList,
  pagination,
  selectedKeys,
  search,
  select,
  selectAll,
  fixed,
  onBatchDelete,
  onDelete
} = useTable<TenantListItem>({
  listAPI: (page) => getTenantList({
    ...page,
    tenantName: query.tenantName.trim() || undefined,
    tenantCode: query.tenantCode.trim() || undefined,
    domain: query.domain.trim() || undefined,
    status: query.status
  }),
  deleteAPI: deleteTenant,
  rowKey: 'tenantId',
  immediate: true
})

function reset() {
  Object.assign(query, { tenantName: '', tenantCode: '', domain: '', status: undefined })
  search()
}

function handleAdd() {
  tenantFormModalRef.value?.add()
}

function handleEdit(record: TenantListItem) {
  tenantFormModalRef.value?.edit(record.tenantId)
}

async function handleStatusChange(record: TenantListItem, status: 0 | 1) {
  const previous = record.status
  record.status = status
  try {
    await changeTenantStatus({ tenantId: record.tenantId, status })
    Message.success(status === 0 ? '启用成功' : '停用成功')
  } catch {
    record.status = previous
  }
}

const columns: TableColumnData[] = [
  { title: '租户名称', dataIndex: 'tenantName', width: 180, ellipsis: true, tooltip: true },
  { title: '租户编码', dataIndex: 'tenantCode', width: 140 },
  {
    title: '登录域名',
    width: 260,
    render: ({ record }) => (
      <Space wrap>
        {splitTenantDomains(record.domain).map((domain) => <Tag key={domain}>{domain}</Tag>)}
        {!splitTenantDomains(record.domain).length && <span>-</span>}
      </Space>
    )
  },
  { title: '套餐', dataIndex: 'packageName', width: 140 },
  { title: '账号上限', dataIndex: 'accountCount', width: 100, align: 'center' },
  { title: '当前账号数', dataIndex: 'userCount', width: 110, align: 'center' },
  { title: '管理员昵称', dataIndex: 'adminAlias', width: 130 },
  { title: '管理员账号', dataIndex: 'adminUsername', width: 140 },
  { title: '到期时间', dataIndex: 'expireTime', width: 180 },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Switch
        modelValue={record.status}
        checkedValue={0}
        uncheckedValue={1}
        checkedText="正常"
        uncheckedText="停用"
        disabled={!hasPerm('system:tenant:changeStatus')}
        onUpdate:modelValue={(value) => handleStatusChange(record as TenantListItem, value as 0 | 1)}
      />
    )
  },
  {
    title: '操作',
    width: 92,
    align: 'center',
    fixed: fixed.value,
    render: ({ record }) => (
      <GiTableActions actions={[
        { key: 'edit', label: '编辑', icon: 'edit', visible: hasPerm('system:tenant:edit'), onClick: () => handleEdit(record as TenantListItem) },
        { key: 'delete', label: '删除', icon: 'delete', status: 'danger', visible: hasPerm('system:tenant:remove'), confirm: '确定删除该租户吗?', onClick: () => onDelete(record as TenantListItem) }
      ]}
      />
    )
  }
]
</script>

<style lang="scss" scoped></style>
