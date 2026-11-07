<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['system:sort:add']" type="primary" @click="formModalRef?.add()">
          <template #icon><IconPlus /></template>
          新增分类
        </a-button>
      </a-space>
      <a-space wrap>
        <a-input v-model="query.identification" placeholder="分类标识" allow-clear @press-enter="loadData" />
        <a-input v-model="query.classifiedName" placeholder="分类名称" allow-clear @press-enter="loadData" />
        <GiButton type="search" @click="loadData" />
        <GiButton type="reset" @click="reset" />
        <a-tooltip content="刷新">
          <a-button shape="circle" aria-label="刷新" :loading="loading" @click="loadData"><icon-refresh /></a-button>
        </a-tooltip>
      </a-space>
    </a-row>

    <a-table
      class="g-table"
      row-key="id"
      :data="categoryList"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 900 }"
      default-expand-all-rows
    />

    <ProductCategoryFormModal ref="formModalRef" @save-success="loadData" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { ProductCategoryItem } from '@/apis/iot/productCategory'
import { Button, Message, Popconfirm, Space, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconPlus } from '@arco-design/web-vue/es/icon'
import { deleteProductCategory, getProductCategoryList } from '@/apis/iot/productCategory'
import { hasPerm } from '@/utils/permission'
import ProductCategoryFormModal from './ProductCategoryFormModal.vue'

defineOptions({ name: 'IotProductCategory' })

const formModalRef = useTemplateRef<InstanceType<typeof ProductCategoryFormModal>>('formModalRef')
const loading = ref(false)
const categoryList = ref<ProductCategoryItem[]>([])
const query = reactive({ identification: '', classifiedName: '' })

function buildTree(list: ProductCategoryItem[], parentId = '0'): ProductCategoryItem[] {
  return list
    .filter((item) => String(item.parentId || '0') === parentId)
    .map((item) => ({ ...item, children: buildTree(list, String(item.id)) }))
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProductCategoryList({
      identification: query.identification.trim() || undefined,
      classifiedName: query.classifiedName.trim() || undefined
    })
    categoryList.value = buildTree(res.data)
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { identification: '', classifiedName: '' })
  loadData()
}

async function remove(record: ProductCategoryItem) {
  await deleteProductCategory(record.id)
  Message.success('删除成功')
  loadData()
}

const columns: TableColumnData[] = [
  { title: '分类名称', dataIndex: 'classifiedName', width: 240, ellipsis: true, tooltip: true },
  { title: '分类标识', dataIndex: 'identification', width: 200, ellipsis: true, tooltip: true },
  { title: '分类 ID', dataIndex: 'id', width: 170, ellipsis: true, tooltip: true },
  { title: '说明', dataIndex: 'description', ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  {
    title: '操作',
    width: 150,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        {hasPerm('system:sort:add') && String(record.id).length < 9 && (
          <Tooltip content="新增子分类"><Button type="text" shape="circle" aria-label="新增子分类" onClick={() => formModalRef.value?.add(record as ProductCategoryItem)}><IconPlus /></Button></Tooltip>
        )}
        {hasPerm('system:sort:edit') && (
          <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑" onClick={() => formModalRef.value?.edit(String(record.id))}><IconEdit /></Button></Tooltip>
        )}
        {hasPerm('system:sort:remove') && (
          <Popconfirm content="确定删除该分类吗？子分类或已关联产品会导致删除失败。" onBeforeOk={() => remove(record as ProductCategoryItem)}>
            <Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除"><IconDelete /></Button></Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]

loadData()
</script>

<style lang="scss" scoped></style>
