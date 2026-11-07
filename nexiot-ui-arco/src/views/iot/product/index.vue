<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['system:product:add']" type="primary" @click="formModalRef?.add()"><template #icon><icon-plus /></template>新增产品</a-button>
        <a-button v-hasPerm="['system:product:import']" @click="importModalRef?.open()"><template #icon><icon-import /></template>导入</a-button>
        <a-button v-hasPerm="['system:product:export']" :loading="exporting" @click="handleExport()"><template #icon><IconExport /></template>导出</a-button>
        <a-popconfirm content="确定删除选中的产品吗？已关联设备的产品不能删除。" @before-ok="batchDelete">
          <a-button v-if="viewMode === 'table'" v-hasPerm="['system:product:remove']" status="danger" :disabled="!selectedKeys.length"><template #icon><IconDelete /></template>批量删除</a-button>
        </a-popconfirm>
      </a-space>
      <a-space wrap>
        <a-select v-model="query.productKey" class="product-search" :options="productOptions" allow-search allow-create allow-clear placeholder="产品名称 / ProductKey" />
        <a-input v-model="query.companyNo" placeholder="厂商名称" allow-clear @press-enter="search" />
        <a-input v-model="query.classifiedName" placeholder="产品分类" allow-clear @press-enter="search" />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
        <a-radio-group v-model="viewMode" type="button" class="view-switch">
          <a-radio value="card"><icon-apps /> 卡片</a-radio>
          <a-radio value="table"><icon-list /> 列表</a-radio>
        </a-radio-group>
      </a-space>
    </a-row>

    <a-spin v-if="viewMode === 'card'" :loading="loading" class="product-view">
      <a-empty v-if="!loading && !productList.length" description="暂无产品" />
      <div v-else class="product-grid">
        <article v-for="product in productList" :key="product.id" class="product-card" tabindex="0" @click="openDetail(product)" @keydown.enter="openDetail(product)">
          <div class="product-card__main">
            <div class="product-card__image">
              <img v-if="getProductImage(product)" :src="getProductImage(product)" :alt="product.name" width="44" height="44" />
              <icon-common v-else />
            </div>
            <div class="product-card__identity">
              <a-tooltip :content="product.name"><h3>{{ product.name || '未命名产品' }}</h3></a-tooltip>
              <code>{{ product.productKey }}</code>
            </div>
            <a-tag size="small" :color="getState(product).color">{{ getState(product).label }}</a-tag>
          </div>

          <div class="product-card__facts">
            <div><span>设备节点</span><strong>{{ getDeviceNode(product.deviceNode) }}</strong></div>
            <div><span>接入方式</span><strong>{{ product.thirdPlatform || '-' }}</strong></div>
            <div><span>产品分类</span><strong>{{ product.classifiedName || '-' }}</strong></div>
          </div>

          <div class="product-card__metadata">
            <span>物模型</span>
            <a-space :size="4">
              <a-tag size="small">{{ getMetadataCounts(product).properties }} 属性</a-tag>
              <a-tag size="small">{{ getMetadataCounts(product).events }} 事件</a-tag>
              <a-tag size="small">{{ getMetadataCounts(product).functions }} 功能</a-tag>
            </a-space>
          </div>

          <footer class="product-card__footer">
            <a-space :size="4" class="product-card__tags">
              <a-tag v-for="tag in visibleTags(product, 2)" :key="tag.value" size="small" color="arcoblue">{{ tag.label }}</a-tag>
              <a-tooltip v-if="hiddenTagCount(product, 2)" :content="tagTooltip(product)">
                <a-tag size="small">+{{ hiddenTagCount(product, 2) }}</a-tag>
              </a-tooltip>
            </a-space>
            <a-space :size="2">
              <a-tooltip content="设备数量"><span class="device-count"><icon-common />{{ deviceCounts[product.productKey] || 0 }}</span></a-tooltip>
              <a-tooltip content="编辑"><a-button v-hasPerm="['system:product:edit']" size="small" type="text" shape="circle" aria-label="编辑" @click.stop="formModalRef?.edit(String(product.id))"><IconEdit /></a-button></a-tooltip>
              <a-tooltip content="导出产品"><a-button v-hasPerm="['system:product:export']" size="small" type="text" shape="circle" aria-label="导出产品" :disabled="exporting" @click.stop="handleExport(product)"><IconExport /></a-button></a-tooltip>
              <a-popconfirm content="确定删除该产品吗？" @before-ok="() => onDelete(product)">
                <a-tooltip content="删除"><a-button v-hasPerm="['system:product:remove']" size="small" type="text" status="danger" shape="circle" aria-label="删除" @click.stop><IconDelete /></a-button></a-tooltip>
              </a-popconfirm>
            </a-space>
          </footer>
        </article>
      </div>
      <div v-if="productList.length" class="card-pagination"><a-pagination v-bind="pagination" /></div>
    </a-spin>

    <a-table
      v-else
      class="g-table"
      row-key="id"
      :data="productList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1900 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
    />

    <ProductFormModal ref="formModalRef" @save-success="search" @created="openDetail" />
    <ProductImportModal ref="importModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Modal, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconExport, IconEye } from '@arco-design/web-vue/es/icon'
import { useStorage } from '@vueuse/core'
import { useRouter } from 'vue-router'
import { deleteProducts, exportProducts, getProductConfigDicts, getProductDeviceCounts, getProductList } from '@/apis/iot/product'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import ProductFormModal from './ProductFormModal.vue'
import ProductImportModal from './ProductImportModal.vue'

defineOptions({ name: 'IotProduct' })

const router = useRouter()
const formModalRef = useTemplateRef<InstanceType<typeof ProductFormModal>>('formModalRef')
const importModalRef = useTemplateRef<InstanceType<typeof ProductImportModal>>('importModalRef')
const query = reactive({ productKey: '', companyNo: '', classifiedName: '' })
const viewMode = useStorage<'card' | 'table'>('nexiot-product-view', 'table')
const deviceCounts = ref<Record<string, number>>({})
const tagLabels = ref<Record<string, string>>({})
const exporting = ref(false)
const products = ref<ProductItem[]>([])
const productOptions = computed(() => products.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))
const stateMap = [{ label: '已发布', color: 'green' }, { label: '审核中', color: 'orange' }, { label: '已停用', color: 'gray' }]
const HTTP_URL_RE = /^https?:\/\//

const { loading, tableData: productList, pagination, selectedKeys, search, select, selectAll, onDelete } = useTable<ProductItem>({
  listAPI: (page) => getProductList({ ...page, productKey: (query.productKey || '').trim() || undefined, companyNo: query.companyNo.trim() || undefined, classifiedName: query.classifiedName.trim() || undefined }),
  deleteAPI: deleteProducts,
  defaultPageSize: 12,
  immediate: true
})

function reset() {
  Object.assign(query, { productKey: '', companyNo: '', classifiedName: '' })
  search()
}
async function batchDelete() {
  await deleteProducts(selectedKeys.value.map(String))
  Message.success('删除成功')
  search()
  return true
}
function handleExport(product?: ProductItem) {
  Modal.confirm({
    title: product ? `导出产品「${product.name}」` : '导出产品',
    content: `${product ? '将导出该产品' : '将导出当前筛选条件下所有有权限导出的产品'}的完整配置，包括协议、网络和物模型。`,
    onBeforeOk: async () => {
      exporting.value = true
      try {
        const params = product
          ? { id: String(product.id) }
          : { productKey: (query.productKey || '').trim() || undefined, companyNo: query.companyNo.trim() || undefined, classifiedName: query.classifiedName.trim() || undefined }
        const result = await exportProducts(params, product ? `${product.productKey}_${Date.now()}_export.json` : undefined)
        if (result === 'cancelled') return false
        if (result === 'saved') Message.success('产品 JSON 已保存')
        else Message.info('已发起下载，请查看浏览器下载列表')
        return true
      } finally {
        exporting.value = false
      }
    }
  })
}
function getTags(record: ProductItem) {
  return (Array.isArray(record.tags) ? record.tags : String(record.tags || '').split(','))
    .map((tag) => String(tag).trim())
    .filter(Boolean)
}
function getTagItems(record: ProductItem) {
  return getTags(record).map((value) => ({ value, label: tagLabels.value[value] || value }))
}
function visibleTags(record: ProductItem, limit: number) {
  return getTagItems(record).slice(0, limit)
}
function hiddenTagCount(record: ProductItem, limit: number) {
  return Math.max(0, getTagItems(record).length - limit)
}
function tagTooltip(record: ProductItem) {
  return getTagItems(record).map((item) => item.label).join('、')
}
function getState(record: ProductItem) {
  return stateMap[record.state] || { label: '未知', color: 'gray' }
}
function getDeviceNode(value?: string) {
  return ({ VIDEO_DEVICE: '视频设备', DEVICE: '直连设备', GATEWAY: '网关设备', GATEWAY_SUB_DEVICE: '网关子设备' } as Record<string, string>)[value || ''] || value || '-'
}
function getProductImage(record: ProductItem) {
  if (!record.photoUrl) return ''
  if (HTTP_URL_RE.test(record.photoUrl)) return record.photoUrl
  try {
    const data = JSON.parse(record.photoUrl)
    return data.img || data.url || ''
  } catch {
    return ''
  }
}
function getMetadataCounts(record: ProductItem) {
  try {
    const data = typeof record.metadata === 'string' ? JSON.parse(record.metadata) : record.metadata || {}
    return { properties: data.properties?.length || 0, events: data.events?.length || 0, functions: data.functions?.length || 0 }
  } catch {
    return { properties: 0, events: 0, functions: 0 }
  }
}
function openDetail(record: Pick<ProductItem, 'id'>) {
  router.push(`/product/details/${record.id}`)
}

const columns: TableColumnData[] = [
  { title: '产品', width: 290, fixed: 'left', render: ({ record }) => (
    <div class="product-cell">
      <div class="product-cell__image">{getProductImage(record as ProductItem) ? <img src={getProductImage(record as ProductItem)} alt={record.name} width="40" height="40" /> : <span>--</span>}</div>
      <div class="product-cell__identity">
        <Tooltip content={record.name || '未命名产品'}>
          <Button class="product-cell__name" type="text" onClick={() => openDetail(record as ProductItem)}>{record.name || '未命名产品'}</Button>
        </Tooltip>
        <Tooltip content={record.productKey}>
          <code class="product-cell__key">{record.productKey}</code>
        </Tooltip>
      </div>
    </div>
  ) },
  { title: '设备型号', dataIndex: 'productId', width: 140, ellipsis: true, tooltip: true },
  { title: '产品分类', dataIndex: 'classifiedName', width: 160, ellipsis: true, tooltip: true },
  { title: '厂商', dataIndex: 'companyNo', width: 150, ellipsis: true, tooltip: true },
  { title: '设备节点', width: 130, render: ({ record }) => getDeviceNode(record.deviceNode) },
  { title: '接入方式', dataIndex: 'thirdPlatform', width: 130 },
  { title: '传输协议', dataIndex: 'transportProtocol', width: 120 },
  { title: '物模型', width: 220, render: ({ record }) => {
    const counts = getMetadataCounts(record as ProductItem)
    return (
      <Space size={4}>
        <Tag>
          {counts.properties}
          {' '}
          属性
        </Tag>
        <Tag>
          {counts.events}
          {' '}
          事件
        </Tag>
        <Tag>
          {counts.functions}
          {' '}
          功能
        </Tag>
      </Space>
    )
  } },
  { title: '标签', width: 250, render: ({ record }) => {
    const product = record as ProductItem
    const items = visibleTags(product, 4)
    const hidden = hiddenTagCount(product, 4)
    return (
      <Space wrap size={4}>
        {items.map((tag) => <Tag key={tag.value}>{tag.label}</Tag>)}
        {hidden > 0 && (
          <Tooltip content={tagTooltip(product)}>
            <Tag>
              +
              {hidden}
            </Tag>
          </Tooltip>
        )}
      </Space>
    )
  } },
  { title: '设备数', width: 90, align: 'center', render: ({ record }) => deviceCounts.value[record.productKey] || 0 },
  { title: '状态', width: 100, align: 'center', render: ({ record }) => <Tag color={getState(record as ProductItem).color}>{getState(record as ProductItem).label}</Tag> },
  { title: '创建人', dataIndex: 'creatorId', width: 120, ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '更新时间', dataIndex: 'updateTime', width: 180 },
  { title: '描述', dataIndex: 'describe', width: 220, ellipsis: true, tooltip: true },
  { title: '操作', width: 174, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      {hasPerm('system:product:query') && <Tooltip content="详情"><Button type="text" shape="circle" aria-label="详情" onClick={() => openDetail(record as ProductItem)}><IconEye /></Button></Tooltip>}
      {hasPerm('system:product:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => formModalRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
      {hasPerm('system:product:export') && <Tooltip content="导出产品"><Button type="text" shape="circle" aria-label="导出产品" disabled={exporting.value} onClick={() => handleExport(record as ProductItem)}><IconExport /></Button></Tooltip>}
      {hasPerm('system:product:remove') && <Popconfirm content="确定删除该产品吗？" onBeforeOk={() => onDelete(record as ProductItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
    </Space>
  ) }
]

getProductList({ page: 1, size: 500 }).then((res) => {
  products.value = res.data.records || []
})
getProductDeviceCounts().then((res) => {
  deviceCounts.value = res.data || {}
})
getProductConfigDicts(['product_tags']).then((res) => {
  tagLabels.value = Object.fromEntries((res.data.product_tags || []).map((item) => [item.dictValue, item.dictLabel]))
})
</script>

<style lang="scss" scoped>
.product-search {
  width: 240px;
}

.product-view {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  align-content: start;
  padding: 2px 2px 14px;
  overflow: auto;
}

.product-card {
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

    .product-card__identity h3 {
      color: rgb(var(--primary-6));
    }
  }

  &__main {
    display: grid;
    grid-template-columns: 44px minmax(0, 1fr) auto;
    gap: 10px;
    align-items: start;
  }

  &__image {
    position: relative;
    display: grid;
    flex: 0 0 44px;
    place-items: center;
    width: 44px;
    min-width: 44px;
    max-width: 44px;
    height: 44px;
    min-height: 44px;
    max-height: 44px;
    overflow: hidden;
    font-size: 22px;
    color: rgb(var(--primary-6));
    background: var(--color-fill-2);
    border: 1px solid var(--color-border-2);
    border-radius: 8px;

    img {
      position: absolute;
      inset: 0;
      display: block;
      width: 100%;
      min-width: 44px;
      max-width: 44px;
      height: 100%;
      min-height: 44px;
      max-height: 44px;
      object-fit: cover;
    }
  }

  &__identity {
    min-width: 0;

    h3 {
      margin: 0 0 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 14px;
      font-weight: 600;
      line-height: 20px;
      color: var(--color-text-1);
      white-space: nowrap;
      transition: color 0.15s ease;
    }

    code {
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

  &__metadata {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
    padding: 6px 0;
    font-size: 12px;
    color: var(--color-text-3);
    border-top: 1px dashed var(--color-border-2);
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

  &__tags {
    display: flex;
    flex: 1;
    flex-wrap: nowrap;
    min-width: 0;
    overflow: hidden;

    :deep(.arco-tag) {
      max-width: 88px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.device-count {
  display: inline-flex;
  gap: 3px;
  align-items: center;
  height: 22px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
  color: var(--color-text-2);
  background: var(--color-fill-2);
  border-radius: 4px;
}

.card-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

:deep(.product-cell) {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  width: 100%;
  min-width: 0;
}

:deep(.product-cell__image) {
  position: relative;
  display: grid;
  place-items: center;
  width: 40px;
  min-width: 40px;
  max-width: 40px;
  height: 40px;
  min-height: 40px;
  max-height: 40px;
  overflow: hidden;
  font-size: 10px;
  color: var(--color-text-4);
  background: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
}

:deep(.product-cell__image img) {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  min-width: 40px;
  max-width: 40px;
  height: 100%;
  min-height: 40px;
  max-height: 40px;
  object-fit: cover;
}

:deep(.product-cell__identity) {
  display: grid;
  gap: 3px;
  min-width: 0;
}

:deep(.product-cell__name.arco-btn) {
  display: block;
  width: 100%;
  height: 20px;
  padding: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
  line-height: 20px;
  text-align: left;
  white-space: nowrap;
}

:deep(.product-cell__key) {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  line-height: 18px;
  color: var(--color-text-3);
  white-space: nowrap;
}

@media (width <= 720px) {
  .product-grid {
    grid-template-columns: 1fr;
  }

  .product-card__facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width > 720px) and (width <= 1200px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width > 1200px) and (width <= 1560px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
