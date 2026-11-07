<template>
  <GiPageLayout margin>
    <div class="standard-layout">
      <aside class="category-pane">
        <div class="pane-header">
          <strong>公共物模型分类</strong>
          <a-tooltip content="新增一级分类">
            <a-button v-hasPerm="['standard:category:add']" type="primary" shape="circle" aria-label="新增一级分类" @click="categoryModalRef?.add()"><icon-plus /></a-button>
          </a-tooltip>
        </div>
        <a-input-search v-model="treeKeyword" placeholder="搜索分类" allow-clear class="tree-search" />
        <div class="tree-scroll">
          <a-spin :loading="treeLoading" class="tree-spin">
            <a-tree
              v-if="filteredTree.length"
              v-model:selected-keys="selectedCategoryKeys"
              :data="filteredTree"
              :field-names="{ key: 'id', title: 'categoryName', children: 'children' }"
              block-node
              show-line
              default-expand-all
              @select="selectCategory"
            >
              <template #title="nodeData">
                <StandardCategoryTreeTitle :category="nodeData" :remove="removeCategory" @add="categoryModalRef?.add($event)" @edit="categoryModalRef?.edit($event)" />
              </template>
            </a-tree>
            <a-empty v-else description="暂无分类" />
          </a-spin>
        </div>
      </aside>

      <main class="product-pane">
        <a-row justify="space-between" class="g-row-tool">
          <a-space wrap>
            <a-button v-hasPerm="['standard:product:add']" type="primary" @click="productModalRef?.add(currentCategory)">
              <template #icon><icon-plus /></template>
              新增标准产品
            </a-button>
            <a-popconfirm content="确定删除选中的标准产品吗？" @before-ok="batchDelete"><a-button v-hasPerm="['standard:product:remove']" status="danger" :disabled="!selectedKeys.length"><template #icon><IconDelete /></template>批量删除</a-button></a-popconfirm>
          </a-space>
          <a-space wrap>
            <a-input v-model="query.productName" placeholder="产品名称" allow-clear @press-enter="search" />
            <a-input v-model="query.productKey" placeholder="产品标识" allow-clear @press-enter="search" />
            <GiButton type="search" @click="search" />
            <GiButton type="reset" @click="resetQuery" />
          </a-space>
        </a-row>
        <div class="category-context"><icon-folder /> {{ currentCategory?.categoryPath || currentCategory?.categoryName || '全部标准产品' }}<a-tag v-if="currentCategory" size="small">含子分类</a-tag></div>
        <a-table
          class="g-table"
          row-key="id"
          :data="productList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          :bordered="{ cell: true }"
          :scroll="{ x: '100%', y: '100%', minWidth: 1100 }"
          :row-selection="{ type: 'checkbox', showCheckedAll: true }"
          :selected-keys="selectedKeys"
          @select="select"
          @select-all="selectAll"
        />
      </main>
    </div>
    <StandardCategoryFormModal ref="categoryModalRef" @save-success="refreshTree" />
    <StandardProductFormModal ref="productModalRef" @save-success="search" />
    <StandardProductDetailDrawer ref="detailDrawerRef" />
    <MetadataEditorModal ref="metadataEditorRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { StandardCategoryItem, StandardCategoryTreeNode, StandardProductItem } from '@/apis/iot/standardModel'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconCode, IconDelete, IconEdit, IconEye, IconStar, IconStarFill } from '@arco-design/web-vue/es/icon'
import {
  deleteStandardCategory,
  deleteStandardProducts,
  getStandardCategoryTree,
  getStandardProductList,
  setStandardProductTemplate,
  toStandardCategoryTree
} from '@/apis/iot/standardModel'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import MetadataEditorModal from './MetadataEditorModal.vue'
import StandardCategoryFormModal from './StandardCategoryFormModal.vue'
import StandardCategoryTreeTitle from './StandardCategoryTreeTitle.vue'
import StandardProductDetailDrawer from './StandardProductDetailDrawer.vue'
import StandardProductFormModal from './StandardProductFormModal.vue'

defineOptions({ name: 'ProductStandardModelCategory' })
const categoryModalRef = useTemplateRef<InstanceType<typeof StandardCategoryFormModal>>('categoryModalRef')
const productModalRef = useTemplateRef<InstanceType<typeof StandardProductFormModal>>('productModalRef')
const detailDrawerRef = useTemplateRef<InstanceType<typeof StandardProductDetailDrawer>>('detailDrawerRef')
const metadataEditorRef = useTemplateRef<InstanceType<typeof MetadataEditorModal>>('metadataEditorRef')
const treeLoading = ref(false)
const categoryTree = ref<StandardCategoryTreeNode[]>([])
const treeKeyword = ref('')
const selectedCategoryKeys = ref<string[]>([])
const currentCategory = ref<StandardCategoryItem>()
const query = reactive({ productName: '', productKey: '' })

function filterTree(nodes: StandardCategoryTreeNode[], keyword: string): StandardCategoryTreeNode[] {
  if (!keyword) return nodes
  return nodes.reduce<StandardCategoryTreeNode[]>((result, node) => {
    const children = filterTree(node.children || [], keyword)
    if (node.categoryName.toLowerCase().includes(keyword.toLowerCase()) || children.length) result.push({ ...node, children })
    return result
  }, [])
}
const filteredTree = computed(() => filterTree(categoryTree.value, treeKeyword.value.trim()))

const { loading, tableData: productList, pagination, selectedKeys, search, select, selectAll, onDelete } = useTable<StandardProductItem>({
  listAPI: (page) => getStandardProductList({ ...page, productName: query.productName.trim() || undefined, productKey: query.productKey.trim() || undefined, categoryId: currentCategory.value?.id }),
  deleteAPI: deleteStandardProducts,
  immediate: true
})

async function refreshTree() {
  treeLoading.value = true
  try {
    categoryTree.value = toStandardCategoryTree((await getStandardCategoryTree()).data || [])
  } finally {
    treeLoading.value = false
  }
}

function findCategory(nodes: StandardCategoryTreeNode[], id: string, parentPath = ''): StandardCategoryItem | undefined {
  for (const node of nodes) {
    const categoryPath = parentPath ? `${parentPath}/${node.categoryName}` : node.categoryName
    if (String(node.id) === id) return { ...node, id: String(node.id), categoryPath }
    const child = findCategory(node.children || [], id, categoryPath)
    if (child) return child
  }
}

function selectCategory(keys: (string | number)[]) {
  currentCategory.value = keys.length ? findCategory(categoryTree.value, String(keys[0])) : undefined
  search()
}

async function removeCategory(id: string) {
  await deleteStandardCategory(id)
  Message.success('分类删除成功')
  if (currentCategory.value?.id === id) {
    currentCategory.value = undefined
    selectedCategoryKeys.value = []
    search()
  }
  refreshTree()
  return true
}

function resetQuery() {
  Object.assign(query, { productName: '', productKey: '' })
  search()
}
async function batchDelete() {
  await deleteStandardProducts(selectedKeys.value.map(String))
  Message.success('删除成功')
  search()
  return true
}
async function toggleTemplate(record: StandardProductItem) {
  await setStandardProductTemplate(record.id, record.isTemplate !== 1)
  Message.success(record.isTemplate === 1 ? '已取消模板' : '已设为模板')
  search()
}

const columns: TableColumnData[] = [
  { title: '产品名称', width: 200, render: ({ record }) => (
    <Space>
      <Button type="text" onClick={() => detailDrawerRef.value?.open(String(record.id))}>{record.productName}</Button>
      {record.isTemplate === 1 && <Tag color="gold">模板</Tag>}
    </Space>
  ) },
  { title: '产品标识', dataIndex: 'productKey', width: 190, ellipsis: true, tooltip: true },
  { title: '分类路径', dataIndex: 'categoryPath', ellipsis: true, tooltip: true },
  { title: '状态', width: 90, align: 'center', render: ({ record }) => <Tag color={record.status === 1 ? 'green' : 'gray'}>{record.status === 1 ? '启用' : '禁用'}</Tag> },
  { title: '使用次数', dataIndex: 'useCount', width: 100, align: 'center' },
  { title: '更新时间', dataIndex: 'updateTime', width: 180 },
  {
    title: '操作',
    width: 200,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        <Tooltip content="查看"><Button type="text" shape="circle" aria-label="查看" onClick={() => detailDrawerRef.value?.open(String(record.id))}><IconEye /></Button></Tooltip>
        {hasPerm('standard:product:edit') && <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => productModalRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>}
        {hasPerm('standard:product:edit') && <Tooltip content="编辑物模型"><Button type="text" shape="circle" aria-label="编辑物模型" onClick={() => metadataEditorRef.value?.open(String(record.id))}><IconCode /></Button></Tooltip>}
        {hasPerm('standard:product:edit') && <Tooltip content={record.isTemplate === 1 ? '取消模板' : '设为模板'}><Button type="text" shape="circle" aria-label="切换模板" onClick={() => toggleTemplate(record as StandardProductItem)}>{record.isTemplate === 1 ? <IconStarFill /> : <IconStar />}</Button></Tooltip>}
        {hasPerm('standard:product:remove') && <Popconfirm content="确定删除该标准产品吗？" onBeforeOk={() => onDelete(record as StandardProductItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip></Popconfirm>}
      </Space>
    )
  }
]

refreshTree()
</script>

<style lang="scss" scoped>
.standard-layout {
  display: grid;
  grid-template-columns: minmax(280px, 330px) minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.category-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-right: 12px;
  border-right: 1px solid var(--color-border-2);
}

.pane-header, .category-context {
  display: flex;
  align-items: center;
}

.pane-header {
  justify-content: space-between;
  min-height: 34px;
}

.tree-search {
  margin: 12px 0;
}

.tree-scroll {
  flex: 1;
  min-height: 0;
  padding-right: 4px;
  overflow: auto;
}

.tree-spin {
  width: 100%;
}

.product-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.category-context {
  gap: 6px;
  min-height: 32px;
  margin-bottom: 8px;
  color: var(--color-text-2);
}

@media (width <= 960px) {
  .standard-layout {
    grid-template-rows: minmax(220px, 35vh) minmax(0, 1fr);
    grid-template-columns: 1fr;
  }

  .category-pane {
    padding: 0 0 12px;
    border-right: 0;
    border-bottom: 1px solid var(--color-border-2);
  }
}
</style>
