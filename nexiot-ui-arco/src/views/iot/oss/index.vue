<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-upload :show-file-list="false" :custom-request="handleUpload" accept="*">
          <template #upload-button>
            <a-button type="primary" :loading="uploading">
              <template #icon><icon-upload /></template>
              上传文件
            </a-button>
          </template>
        </a-upload>
        <a-space>
          <a-badge status="processing" />
          <span>当前存储</span>
          <a-tag color="green">{{ getProviderLabel(currentStorage) }}</a-tag>
        </a-space>
      </a-space>
      <a-space wrap>
        <a-input v-model="query.originalName" placeholder="文件名" allow-clear />
        <a-select v-model="query.service" :options="providers" placeholder="服务商" allow-clear />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <a-table class="g-table" row-key="ossId" :data="tableData" :columns="columns" :loading="loading || uploading" :pagination="pagination" :bordered="{ cell: true }" :scroll="{ x: '100%', y: '100%', minWidth: 1250 }" />

    <a-modal v-model:visible="previewVisible" :title="previewRecord.originalName || previewRecord.fileName" width="780px" :footer="false">
      <img v-if="isImage(previewRecord)" class="oss-file__preview-image" :src="previewRecord.url" alt="文件预览" />
      <a-result v-else status="info" title="该文件不支持在线预览" subtitle="可使用下载按钮保存到本地查看">
        <template #extra><a-button type="primary" @click="downloadOssFile(previewRecord)">下载文件</a-button></template>
      </a-result>
      <a-typography-paragraph copyable class="oss-file__preview-url">{{ previewRecord.url }}</a-typography-paragraph>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { RequestOption } from '@arco-design/web-vue/es/upload/interfaces'
import type { OssFileItem, OssProviderOption } from '@/apis/iot/oss'
import { Button, Image, Message, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconCopy, IconDownload, IconEye } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { downloadOssFile, getOssConfig, getOssFileList, uploadOssFile } from '@/apis/iot/oss'
import { useTable } from '@/hooks'
import { copyText } from '@/utils'

defineOptions({ name: 'IotOssFile' })

const query = reactive({ originalName: '', service: undefined as string | undefined })
const providers = ref<OssProviderOption[]>([])
const currentStorage = ref('')
const uploading = ref(false)
const previewVisible = ref(false)
const previewRecord = ref<OssFileItem>({} as OssFileItem)

const { loading, tableData, pagination, search, refresh } = useTable<OssFileItem>({
  listAPI: (page) => getOssFileList({ ...page, originalName: query.originalName.trim() || undefined, service: query.service }),
  rowKey: 'ossId'
})

function reset() {
  Object.assign(query, { originalName: '', service: undefined })
  search()
}

function isImage(record: Partial<OssFileItem>) {
  return ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg'].includes((record.fileSuffix || '').toLowerCase())
}

function getProviderLabel(provider: string) {
  return providers.value.find((item) => item.value === provider)?.label || provider || '-'
}

function openPreview(record: OssFileItem) {
  previewRecord.value = record
  previewVisible.value = true
}

async function copyUrl(url: string) {
  try {
    await copyText(url)
    Message.success('文件地址已复制')
  } catch { Message.error('复制失败') }
}

function handleUpload(option: RequestOption) {
  const file = option.fileItem.file
  if (!file) {
    option.onError(new Error('未读取到上传文件'))
    return {}
  }
  if (file.size >= 100 * 1024 * 1024) {
    Message.error('上传文件大小不能超过 100 MB')
    option.onError(new Error('文件过大'))
    return {}
  }
  uploading.value = true
  uploadOssFile(file)
    .then((res) => {
      option.onSuccess(res.data)
      Message.success('文件上传成功')
      refresh()
    })
    .catch(option.onError)
    .finally(() => { uploading.value = false })
  return {}
}

async function loadConfig() {
  const { data } = await getOssConfig()
  providers.value = data.providers
  currentStorage.value = data.current
}

const columns: TableColumnData[] = [
  {
    title: '预览',
    width: 84,
    align: 'center',
    render: ({ record }) => isImage(record as OssFileItem) ? <Image width={48} height={48} fit="cover" src={record.url} preview={false} onClick={() => openPreview(record as OssFileItem)} /> : <icon-file-text size="28" />
  },
  {
    title: '文件',
    width: 260,
    render: ({ record }) => (
      <div class="oss-file__name">
        <Tooltip content={record.originalName || record.fileName}><strong>{record.originalName || record.fileName}</strong></Tooltip>
        <small>{record.fileName}</small>
      </div>
    )
  },
  { title: '后缀', dataIndex: 'fileSuffix', width: 90, align: 'center' },
  { title: '服务商', width: 110, align: 'center', render: ({ record }) => <Tag color={record.service === currentStorage.value ? 'green' : 'blue'}>{getProviderLabel(record.service)}</Tag> },
  { title: '访问地址', dataIndex: 'url', minWidth: 280, ellipsis: true, tooltip: true },
  { title: '上传人', dataIndex: 'createBy', width: 140, ellipsis: true, tooltip: true },
  { title: '上传时间', width: 180, render: ({ record }) => record.createTime ? Dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' },
  {
    title: '操作',
    width: 132,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        <Tooltip content="查看"><Button type="text" size="small" shape="circle" onClick={() => openPreview(record as OssFileItem)}><IconEye /></Button></Tooltip>
        <Tooltip content="下载"><Button type="text" size="small" shape="circle" onClick={() => downloadOssFile(record as OssFileItem)}><IconDownload /></Button></Tooltip>
        <Tooltip content="复制地址"><Button type="text" size="small" shape="circle" onClick={() => copyUrl(record.url)}><IconCopy /></Button></Tooltip>
      </Space>
    )
  }
]

loadConfig()
</script>

<style lang="scss" scoped>
.oss-file {
  &__preview-image {
    display: block;
    max-width: 100%;
    max-height: 560px;
    margin: 0 auto;
    object-fit: contain;
  }

  &__preview-url {
    margin-top: 16px;
    word-break: break-all;
  }
}

:deep(.oss-file__name) {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;

  strong,
  small {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    margin-top: 4px;
    color: var(--color-text-3);
  }
}
</style>
