<template>
  <section class="ota-panel">
    <OtaQuickGuide />
    <a-tabs v-model:active-key="activeTab" type="rounded">
      <a-tab-pane key="packages" title="升级包管理">
        <a-row justify="space-between" class="g-row-tool">
          <a-space wrap>
            <a-button v-hasPerm="['system:ota:package:add']" type="primary" @click="packageDrawerRef?.open(undefined, product.productKey)"><template #icon><icon-plus /></template>上传升级包</a-button>
            <a-button v-hasPerm="['system:ota:tag:add', 'system:ota:tag:edit', 'system:ota:tag:remove']" @click="tagManagerRef?.open()"><template #icon><icon-tags /></template>升级标签</a-button>
            <a-button @click="profileModalRef?.open(product.productKey)"><template #icon><icon-settings /></template>厂家适配</a-button>
          </a-space>
          <a-space wrap><a-input v-model="packageQuery.version" placeholder="版本号" allow-clear /><a-select v-model="packageQuery.packageType" :options="packageTypeOptions" placeholder="升级包类型" allow-clear /><GiButton type="search" @click="searchPackages" /><GiButton type="reset" @click="resetPackageQuery" /></a-space>
        </a-row>
        <a-table row-key="id" :data="packages" :columns="packageColumns" :loading="packageLoading" :pagination="packagePagination" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 1100 }" />
      </a-tab-pane>
      <a-tab-pane key="tasks" title="任务中心">
        <a-row justify="space-between" class="g-row-tool">
          <a-button v-hasPerm="['system:ota:task:add']" type="primary" @click="taskDrawerRef?.open(product.productKey)"><template #icon><icon-plus /></template>创建升级计划</a-button>
          <a-space wrap><a-input v-model="taskQuery.taskName" placeholder="计划名称" allow-clear /><a-select v-model="taskQuery.status" :options="taskStatusOptions" placeholder="计划状态" allow-clear /><GiButton type="search" @click="searchTasks" /><GiButton type="reset" @click="resetTaskQuery" /></a-space>
        </a-row>
        <a-table row-key="id" :data="tasks" :columns="taskColumns" :loading="taskLoading" :pagination="taskPagination" :bordered="{ cell: true }" :scroll="{ x: '100%', minWidth: 1250 }" />
      </a-tab-pane>
    </a-tabs>
    <OtaPackageFormDrawer ref="packageDrawerRef" :products="[product]" :tags="tags" @save-success="searchPackages" />
    <OtaTaskFormDrawer ref="taskDrawerRef" :products="[product]" @save-success="handleTaskSaved" />
    <OtaTagManagerModal ref="tagManagerRef" @change="tags = $event" />
    <OtaTransferProfileModal ref="profileModalRef" :products="[product]" />
    <a-drawer v-model:visible="detailVisible" :title="detailType === 'package' ? '升级包详情' : '计划详情'" width="min(620px, 100%)">
      <a-descriptions v-if="detailRecord" :column="1" bordered>
        <a-descriptions-item v-for="item in detailItems" :key="item.label" :label="item.label">{{ item.value || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-button v-if="detailType === 'task'" class="ota-panel__detail-action" type="primary" @click="router.push({ name: 'IotOtaTask', query: { productKey: product.productKey } })">前往任务中心查看设备传输进度</a-button>
    </a-drawer>
  </section>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { OtaPackageItem, OtaTagItem, OtaTaskItem } from '@/apis/iot/ota'
import type { ProductItem } from '@/apis/iot/product'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye, IconPlayArrow, IconStop } from '@arco-design/web-vue/es/icon'
import { cancelOtaTask, deleteOtaPackage, dispatchOtaTask, getOtaPackage, getOtaPackageList, getOtaTagList, getOtaTaskList, updateOtaPackageStatus } from '@/apis/iot/ota'
import { useTable } from '@/hooks'
import OtaPackageFormDrawer from '@/views/iot/ota/modules/OtaPackageFormDrawer.vue'
import OtaQuickGuide from '@/views/iot/ota/modules/OtaQuickGuide.vue'
import OtaTagManagerModal from '@/views/iot/ota/modules/OtaTagManagerModal.vue'
import OtaTaskFormDrawer from '@/views/iot/ota/modules/OtaTaskFormDrawer.vue'
import OtaTransferProfileModal from '@/views/iot/ota/modules/OtaTransferProfileModal.vue'

defineOptions({ name: 'IotProductOtaPanel' })
const props = defineProps<{ product: ProductItem }>()
const router = useRouter()
const packageDrawerRef = useTemplateRef<InstanceType<typeof OtaPackageFormDrawer>>('packageDrawerRef')
const taskDrawerRef = useTemplateRef<InstanceType<typeof OtaTaskFormDrawer>>('taskDrawerRef')
const tagManagerRef = useTemplateRef<InstanceType<typeof OtaTagManagerModal>>('tagManagerRef')
const profileModalRef = useTemplateRef<InstanceType<typeof OtaTransferProfileModal>>('profileModalRef')
const activeTab = ref('packages')
const detailVisible = ref(false)
const detailType = ref<'package' | 'task'>('package')
const detailRecord = ref<OtaPackageItem | OtaTaskItem>()
const tags = ref<OtaTagItem[]>([])
const packageQuery = reactive({ version: '', packageType: '' })
const taskQuery = reactive({ taskName: '', status: '' })
const packageTypeOptions = [{ label: '固件', value: 'FIRMWARE' }, { label: '软件', value: 'SOFTWARE' }, { label: '配置', value: 'CONFIG' }, { label: '资源', value: 'RESOURCE' }]
const taskStatusLabels: Record<string, string> = { DRAFT: '草稿', PENDING: '待执行', RUNNING: '执行中', COMPLETED: '已完成', SUCCESS: '已完成', PARTIAL_SUCCESS: '部分成功', FAILED: '失败', CANCELED: '已取消' }
const rolloutLabels: Record<string, string> = { ALL: '全部设备', SPECIFIED: '指定设备', GRAY: '灰度发布' }
const taskStatusOptions = Object.entries(taskStatusLabels).filter(([value]) => value !== 'SUCCESS').map(([value, label]) => ({ label, value }))
const { loading: packageLoading, tableData: packages, pagination: packagePagination, search: searchPackages } = useTable<OtaPackageItem>({ listAPI: (page) => getOtaPackageList({ ...page, productKey: props.product.productKey, version: packageQuery.version || undefined, packageType: packageQuery.packageType || undefined }), immediate: true })
const { loading: taskLoading, tableData: tasks, pagination: taskPagination, search: searchTasks } = useTable<OtaTaskItem>({ listAPI: (page) => getOtaTaskList({ ...page, productKey: props.product.productKey, taskName: taskQuery.taskName || undefined, status: taskQuery.status || undefined }), immediate: true })
const detailItems = computed(() => detailType.value === 'package' ? buildPackageDetails(detailRecord.value as OtaPackageItem) : buildTaskDetails(detailRecord.value as OtaTaskItem))

function resetPackageQuery() {
  Object.assign(packageQuery, { version: '', packageType: '' })
  searchPackages()
}
function resetTaskQuery() {
  Object.assign(taskQuery, { taskName: '', status: '' })
  searchTasks()
}
function handleTaskSaved() {
  searchTasks()
  activeTab.value = 'tasks'
}
function parseArray(value: unknown) {
  if (Array.isArray(value)) return value.map(String)
  const text = String(value || '').trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) return parsed.map(String)
  } catch { /* 兼容历史逗号分隔标签。 */ }
  return text.split(',').map((item) => item.trim()).filter(Boolean)
}
function resolvePackageTags(record: OtaPackageItem) {
  const ids = parseArray(record.tagIds?.length ? record.tagIds : record.tagIdsText)
  const names = parseArray(record.tagNames?.length ? record.tagNames : record.tagNamesText)
  return tags.value.filter((item) => ids.includes(String(item.id)) || names.includes(item.tagName))
}
async function removePackage(record: OtaPackageItem) {
  await deleteOtaPackage(record.id)
  Message.success('升级包已删除')
  searchPackages()
  return true
}
async function publishPackage(record: OtaPackageItem) {
  await updateOtaPackageStatus(record.id, record.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED')
  Message.success('升级包状态已更新')
  searchPackages()
}
async function dispatch(record: OtaTaskItem) {
  await dispatchOtaTask(record.id)
  Message.success('计划已开始执行')
  searchTasks()
}
async function cancel(record: OtaTaskItem) {
  await cancelOtaTask(record.id)
  Message.success('计划已取消')
  searchTasks()
}
async function showDetail(type: 'package' | 'task', record: OtaPackageItem | OtaTaskItem) {
  detailType.value = type
  detailRecord.value = type === 'package' ? (await getOtaPackage(record.id)).data : record
  detailVisible.value = true
}
function buildPackageDetails(record?: OtaPackageItem) {
  if (!record) return []
  return [{ label: '升级包名称', value: record.packageName }, { label: '版本', value: record.version }, { label: '类型', value: packageTypeOptions.find((item) => item.value === record.packageType)?.label }, { label: '标签', value: resolvePackageTags(record).map((item) => item.tagName).join('、') }, { label: '状态', value: record.status === 'PUBLISHED' ? '已发布' : '草稿' }, { label: '文件', value: record.fileName || record.fileUrl }, { label: '校验', value: `${record.checksumAlgorithm || ''} ${record.checksum || ''}` }, { label: '说明', value: record.releaseNotes || record.description }]
}
function buildTaskDetails(record?: OtaTaskItem) {
  if (!record) return []
  return [{ label: '计划名称', value: record.taskName }, { label: '计划编号', value: record.taskNo }, { label: '升级包', value: record.packageName }, { label: '目标版本', value: record.targetVersion }, { label: '升级范围', value: rolloutLabels[record.rolloutMode] || record.rolloutMode }, { label: '状态', value: taskStatusLabels[record.status || ''] || record.status }, { label: '设备统计', value: describeTaskCounts(record) }, { label: '备注', value: record.description }]
}
function describeTaskCounts(record: OtaTaskItem) {
  return `${record.successCount || 0} 成功 / ${record.failedCount ?? record.failureCount ?? 0} 失败 / ${record.deviceCount ?? record.totalCount ?? 0} 总数`
}
const packageColumns: TableColumnData[] = [
  { title: '升级包 / 版本', width: 240, render: ({ record }) => (
    <div>
      <strong>{record.packageName}</strong>
      <div class="ota-panel__sub-text">{record.version}</div>
    </div>
  ) },
  { title: '类型', width: 100, render: ({ record }) => <Tag>{packageTypeOptions.find((item) => item.value === record.packageType)?.label || record.packageType}</Tag> },
  { title: '标签', width: 200, render: ({ record }) => {
    const packageTags = resolvePackageTags(record as OtaPackageItem)
    return packageTags.length ? <Space wrap size={4}>{packageTags.map((item) => <Tag key={item.id} color={item.color || 'arcoblue'}>{item.tagName}</Tag>)}</Space> : '-'
  } },
  { title: '模块', dataIndex: 'module', width: 130 },
  { title: '文件', dataIndex: 'fileName', ellipsis: true, tooltip: true },
  { title: '状态', width: 110, render: ({ record }) => <Tag color={record.status === 'PUBLISHED' ? 'green' : 'gray'}>{record.status === 'PUBLISHED' ? '已发布' : '草稿'}</Tag> },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', width: 170, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="详情"><Button type="text" shape="circle" aria-label="升级包详情" onClick={() => showDetail('package', record as OtaPackageItem)}><IconEye /></Button></Tooltip>
      <Tooltip content="编辑"><Button type="text" shape="circle" aria-label="编辑升级包" onClick={() => packageDrawerRef.value?.open(record as OtaPackageItem)}><IconEdit /></Button></Tooltip>
      <Tooltip content={record.status === 'PUBLISHED' ? '撤回发布' : '发布'}><Button type="text" shape="circle" aria-label="切换发布状态" onClick={() => publishPackage(record as OtaPackageItem)}><IconPlayArrow /></Button></Tooltip>
      <Popconfirm content="确定删除升级包吗？关联任务的升级包不能删除。" onBeforeOk={() => removePackage(record as OtaPackageItem)}><Tooltip content="删除"><Button type="text" status="danger" shape="circle" aria-label="删除升级包"><IconDelete /></Button></Tooltip></Popconfirm>
    </Space>
  ) }
]
const taskColumns: TableColumnData[] = [
  { title: '计划', width: 240, render: ({ record }) => (
    <div>
      <strong>{record.taskName}</strong>
      <div class="ota-panel__sub-text">{record.taskNo || record.id}</div>
    </div>
  ) },
  { title: '升级包 / 版本', width: 220, render: ({ record }) => `${record.packageName || '-'} / ${record.targetVersion || '-'}` },
  { title: '升级范围', width: 120, render: ({ record }) => rolloutLabels[record.rolloutMode] || record.rolloutMode },
  { title: '状态', width: 110, render: ({ record }) => <Tag color={['COMPLETED', 'SUCCESS'].includes(record.status) ? 'green' : record.status === 'FAILED' ? 'red' : record.status === 'PARTIAL_SUCCESS' ? 'orange' : 'arcoblue'}>{taskStatusLabels[record.status] || record.status || '-'}</Tag> },
  { title: '设备统计', width: 180, render: ({ record }) => describeTaskCounts(record as OtaTaskItem) },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', width: 132, align: 'center', fixed: 'right', render: ({ record }) => (
    <Space size={2}>
      <Tooltip content="详情"><Button type="text" shape="circle" aria-label="计划详情" onClick={() => showDetail('task', record as OtaTaskItem)}><IconEye /></Button></Tooltip>
      {['DRAFT', 'PENDING'].includes(record.status) && <Tooltip content="执行"><Button type="text" shape="circle" aria-label="执行升级计划" onClick={() => dispatch(record as OtaTaskItem)}><IconPlayArrow /></Button></Tooltip>}
      {['PENDING', 'RUNNING'].includes(record.status) && <Tooltip content="取消"><Button type="text" status="danger" shape="circle" aria-label="取消升级计划" onClick={() => cancel(record as OtaTaskItem)}><IconStop /></Button></Tooltip>}
    </Space>
  ) }
]
getOtaTagList({ page: 1, size: 500 }).then((res) => {
  tags.value = res.data.records || []
})
</script>

<style lang="scss" scoped>
.ota-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__detail-action {
    margin-top: 16px;
  }
}

:deep(.ota-panel__sub-text) {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
