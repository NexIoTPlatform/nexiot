<template>
  <GiPageLayout margin>
    <div class="ota-package__filters">
      <div class="ota-package__field ota-package__field--product">
        <a-select v-model="query.productKey" :options="productOptions" :loading="productSearchLoading" allow-search allow-clear placeholder="全部产品" @search="searchProducts" @dropdown-visible-change="refreshProducts" />
      </div>
      <div class="ota-package__field ota-package__field--search">
        <a-input v-model="query.version" placeholder="搜索版本号" allow-clear @press-enter="search" />
      </div>
      <div class="ota-package__field">
        <a-select v-model="query.packageType" :options="packageTypes" allow-clear placeholder="全部类型" />
      </div>
      <div class="ota-package__field">
        <a-select v-model="query.status" :options="statusOptions" allow-clear placeholder="全部状态" />
      </div>
      <a-space>
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </div>
    <GiTable
      title="升级包列表"
      row-key="id"
      :data="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :table-layout-fixed="true"
      :scroll="{ x: '100%', y: '100%', minWidth: 1060 }"
      @refresh="refresh"
    >
      <template #custom-title>
        <a-space wrap>
          <a-button type="primary" @click="formDrawerRef?.open()"><template #icon><icon-plus /></template>新增升级包</a-button>
          <a-button @click="tagManagerRef?.open()"><template #icon><icon-tags /></template>升级标签</a-button>
          <a-button @click="profileModalRef?.open(query.productKey || undefined)"><template #icon><icon-settings /></template>厂家适配</a-button>
        </a-space>
      </template>
    </GiTable>
    <OtaPackageFormDrawer ref="formDrawerRef" :products="availableProducts" :tags="tags" @save-success="search" />
    <OtaTagManagerModal ref="tagManagerRef" @change="tags = $event" />
    <OtaTransferProfileModal ref="profileModalRef" :products="availableProducts" />
    <a-drawer v-model:visible="detailVisible" title="升级包详情" width="min(560px, 100%)"><a-descriptions v-if="detail" :column="1" bordered><a-descriptions-item v-for="item in detailItems" :key="item.label" :label="item.label"><span class="ota-package__detail-value">{{ item.value || '-' }}</span></a-descriptions-item></a-descriptions></a-drawer>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
/* eslint-disable style/max-statements-per-line */
import type { OtaPackageItem, OtaTagItem } from '@/apis/iot/ota'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Tag, Tooltip } from '@arco-design/web-vue'
import { deleteOtaPackage, getOtaPackage, getOtaPackageList, getOtaTagList, updateOtaPackageStatus } from '@/apis/iot/ota'
import { getProductList } from '@/apis/iot/product'
import GiTableActions from '@/components/GiTableActions/GiTableActions.vue'
import { useTable } from '@/hooks'
import { formatFileSize } from '@/utils'
import OtaPackageFormDrawer from '../modules/OtaPackageFormDrawer.vue'
import OtaTagManagerModal from '../modules/OtaTagManagerModal.vue'
import OtaTransferProfileModal from '../modules/OtaTransferProfileModal.vue'
import { useOtaProducts } from '../useOtaProducts'

defineOptions({ name: 'IotOtaPackage' })
const products = ref<ProductItem[]>([])
const tags = ref<OtaTagItem[]>([])
const formDrawerRef = useTemplateRef<InstanceType<typeof OtaPackageFormDrawer>>('formDrawerRef')
const tagManagerRef = useTemplateRef<InstanceType<typeof OtaTagManagerModal>>('tagManagerRef')
const profileModalRef = useTemplateRef<InstanceType<typeof OtaTransferProfileModal>>('profileModalRef')
const detailVisible = ref(false)
const detail = ref<OtaPackageItem>()
const query = reactive({ productKey: '', version: '', packageType: '', status: '' })
const { availableProducts, productOptions, productSearchLoading, searchProducts, refreshProducts } = useOtaProducts(() => products.value)
const packageTypes = [{ label: '固件', value: 'FIRMWARE' }, { label: '软件', value: 'SOFTWARE' }, { label: '配置', value: 'CONFIG' }, { label: '资源', value: 'RESOURCE' }]
const statusOptions = [{ label: '草稿', value: 'DRAFT' }, { label: '已发布', value: 'PUBLISHED' }]
const { loading, tableData: rows, pagination, search, refresh } = useTable<OtaPackageItem>({ listAPI: (page) => getOtaPackageList({ ...page, productKey: query.productKey || undefined, version: query.version.trim() || undefined, packageType: query.packageType || undefined, status: query.status || undefined }), immediate: true })
function reset() {
  Object.assign(query, { productKey: '', version: '', packageType: '', status: '' })
  search()
}
function parseArray(value: unknown) {
  if (Array.isArray(value)) return value.map(String)
  const text = String(value || '').trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) return parsed.map(String)
  } catch { /* Legacy data. */ } return text.split(',').map((item) => item.trim()).filter(Boolean)
}
function resolveTags(row: OtaPackageItem) {
  const ids = parseArray(row.tagIds?.length ? row.tagIds : row.tagIdsText)
  const names = parseArray(row.tagNames?.length ? row.tagNames : row.tagNamesText)
  return tags.value.filter((tag) => ids.includes(tag.id) || names.includes(tag.tagName))
}
function packageSummary(row: OtaPackageItem) {
  const tagNames = resolveTags(row).map((tag) => tag.tagName).join('、')
  return `${row.packageName || '未命名升级包'} · ID: ${row.id}${tagNames ? ` · 标签: ${tagNames}` : ''}`
}
async function remove(row: OtaPackageItem) {
  await deleteOtaPackage(row.id)
  Message.success('升级包删除成功')
  search()
  return true
}
async function publish(row: OtaPackageItem) {
  await updateOtaPackageStatus(row.id, row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED')
  Message.success(row.status === 'PUBLISHED' ? '升级包已撤回' : '升级包已发布')
  search()
}
async function showDetail(row: OtaPackageItem) {
  detail.value = (await getOtaPackage(row.id)).data
  detailVisible.value = true
}
const detailItems = computed(() => detail.value ? [{ label: '升级包 ID', value: detail.value.id }, { label: '产品', value: `${detail.value.productName || '-'} / ${detail.value.productKey}` }, { label: '升级包名称', value: detail.value.packageName }, { label: '版本', value: detail.value.version }, { label: '类型', value: packageTypes.find((item) => item.value === detail.value?.packageType)?.label || detail.value.packageType }, { label: '标签', value: resolveTags(detail.value).map((tag) => tag.tagName).join('、') }, { label: '文件', value: detail.value.fileName || detail.value.fileUrl }, { label: '文件大小', value: detail.value.fileSize == null ? '-' : formatFileSize(detail.value.fileSize) }, { label: '校验', value: `${detail.value.checksumAlgorithm || ''} ${detail.value.checksum || ''}` }, { label: '状态', value: detail.value.status === 'PUBLISHED' ? '已发布' : '草稿' }, { label: '说明', value: detail.value.releaseNotes || detail.value.description }] : [])
const columns: TableColumnData[] = [
  { title: '升级包 / 版本', dataIndex: 'packageName', width: 250, fixed: 'left', render: ({ record }) => (
    <div class="ota-package__cell">
      <Tooltip content={packageSummary(record as OtaPackageItem)}><Button type="text" class="ota-package__name" onClick={() => showDetail(record as OtaPackageItem)}><span class="ota-package__ellipsis">{record.packageName || '未命名升级包'}</span></Button></Tooltip>
      <span class="ota-package__meta">
        <Tooltip content={`版本：${record.version || '-'}`}><code class="ota-package__version">{record.version || '-'}</code></Tooltip>
        <span>{packageTypes.find((item) => item.value === record.packageType)?.label || record.packageType || '-'}</span>
      </span>
    </div>
  ) },
  { title: '所属产品', dataIndex: 'productKey', width: 190, render: ({ record }) => (
    <Tooltip content={`${record.productName || '-'} · ProductKey: ${record.productKey}`}><span class="ota-package__ellipsis">{record.productName || record.productKey || '-'}</span></Tooltip>
  ) },
  { title: '升级文件', dataIndex: 'fileName', width: 180, render: ({ record }) => (
    <div class="ota-package__cell">
      <Tooltip content={record.fileName || record.fileUrl || '-'}><span class="ota-package__ellipsis">{record.fileName || '-'}</span></Tooltip>
      <span class="ota-package__secondary">{record.fileSize == null ? '-' : formatFileSize(record.fileSize)}</span>
    </div>
  ) },
  { title: '发布状态', dataIndex: 'status', width: 94, align: 'center', render: ({ record }) => (
    <Tag color={record.status === 'PUBLISHED' ? 'green' : 'gray'}>{record.status === 'PUBLISHED' ? '已发布' : '草稿'}</Tag>
  ) },
  { title: '创建时间', dataIndex: 'createTime', width: 156, ellipsis: true, tooltip: true },
  { title: '操作', width: 138, align: 'center', fixed: 'right', render: ({ record }) => (
    <GiTableActions actions={[
      { key: 'edit', label: '编辑', icon: 'edit', onClick: () => formDrawerRef.value?.open(record as OtaPackageItem) },
      { key: 'publish', label: record.status === 'PUBLISHED' ? '撤回' : '发布', icon: record.status === 'PUBLISHED' ? 'withdraw' : 'publish', status: record.status === 'PUBLISHED' ? 'warning' : 'success', onClick: () => publish(record as OtaPackageItem) },
      { key: 'delete', label: '删除', icon: 'delete', status: 'danger', confirm: '确定删除该升级包吗？关联任务的升级包不能删除。', onClick: () => remove(record as OtaPackageItem) }
    ]}
    />
  ) }
]
Promise.all([getProductList({ page: 1, size: 500 }), getOtaTagList({ page: 1, size: 500 })]).then(([productRes, tagRes]) => {
  products.value = productRes.data.records || []
  tags.value = tagRes.data.records || []
})
</script>

<style lang="scss" scoped>
.ota-package {
  &__filters {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    padding-bottom: 14px;
    margin-bottom: 14px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__field {
    flex: 0 0 160px;
    width: 160px;
    min-width: 0;
    max-width: 100%;

    &--search {
      flex-basis: 220px;
      width: 220px;
    }

    &--product {
      flex-basis: 280px;
      width: 280px;
    }
  }

  &__detail-value {
    overflow-wrap: anywhere;
  }
}

:deep(.ota-package__cell) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

:deep(.ota-package__ellipsis),
:deep(.ota-package__version) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ota-package__meta) {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  font-size: 12px;
  color: var(--color-text-3);
}

:deep(.ota-package__version) {
  max-width: 150px;
  padding: 1px 6px;
  color: var(--color-text-2);
  background: var(--color-fill-2);
  border-radius: 3px;
}

:deep(.ota-package__secondary) {
  font-size: 12px;
  color: var(--color-text-3);
}

:deep(.ota-package__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;
  overflow: hidden;
  font-weight: 500;
  text-align: left;

  .ota-package__ellipsis {
    display: block;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
