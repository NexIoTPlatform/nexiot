<template>
  <GiPageLayout margin>
    <a-row justify="space-between" align="center" class="g-row-tool">
      <a-space>
        <a-badge status="processing" />
        <span>当前上传存储</span>
        <a-tag color="green">{{ getProviderLabel(summary.current) }}</a-tag>
      </a-space>
      <a-button :loading="loading" @click="loadConfig">
        <template #icon><icon-refresh /></template>
        刷新
      </a-button>
    </a-row>

    <a-table
      class="g-table"
      row-key="provider"
      :data="configRows"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      :bordered="{ cell: true }"
      :scroll="{ x: '100%', y: '100%', minWidth: 1180 }"
    />

    <OssConfigModal ref="configModalRef" @save-success="loadConfig" />
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { OssConfigSummary, OssStorageConfig } from '@/apis/iot/oss'
import { Button, Message, Modal, Space, Tag } from '@arco-design/web-vue'
import { IconEdit, IconLink, IconSwap } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { getOssConfig, switchOssStorage, testOssStorage } from '@/apis/iot/oss'
import OssConfigModal from './OssConfigModal.vue'

defineOptions({ name: 'IotOssConfig' })

interface ConfigRow extends OssStorageConfig {
  label: string
}

const configModalRef = useTemplateRef<InstanceType<typeof OssConfigModal>>('configModalRef')
const loading = ref(false)
const switching = ref('')
const testing = ref('')
const summary = reactive<OssConfigSummary>({ current: '', providers: [], configs: [] })
const configRows = computed<ConfigRow[]>(() => summary.providers.map((provider) => ({
  provider: provider.value,
  label: provider.label,
  endpoint: '',
  prefix: '',
  accessKey: '',
  secretKey: '',
  bucketName: '',
  isHttps: false,
  ...(summary.configs.find((item) => item.provider === provider.value) || {})
})))

function getProviderLabel(provider: string) {
  return summary.providers.find((item) => item.value === provider)?.label || provider || '-'
}

async function loadConfig() {
  loading.value = true
  try {
    Object.assign(summary, (await getOssConfig()).data)
  } finally {
    loading.value = false
  }
}

function handleSwitch(provider: string) {
  Modal.warning({
    title: '切换上传存储',
    content: `确定将新上传文件切换到“${getProviderLabel(provider)}”吗？已有文件不会迁移。`,
    hideCancel: false,
    onBeforeOk: async () => {
      switching.value = provider
      try {
        await switchOssStorage(provider)
        Message.success('上传存储切换成功')
        await loadConfig()
        return true
      } finally {
        switching.value = ''
      }
    }
  })
}

async function handleTest(provider: string) {
  testing.value = provider
  try {
    const { data } = await testOssStorage(provider)
    Message.success(`连接成功：${data.endpoint || getProviderLabel(provider)}`)
  } finally {
    testing.value = ''
  }
}

const columns: TableColumnData[] = [
  { title: '服务商', dataIndex: 'label', width: 150 },
  { title: '类型', dataIndex: 'provider', width: 110, align: 'center', render: ({ record }) => <Tag>{record.provider}</Tag> },
  { title: '状态', width: 100, align: 'center', render: ({ record }) => <Tag color={record.provider === summary.current ? 'green' : 'gray'}>{record.provider === summary.current ? '已启用' : '未启用'}</Tag> },
  { title: '存储桶', dataIndex: 'bucketName', width: 160, ellipsis: true, tooltip: true },
  { title: '区域', dataIndex: 'region', width: 130, render: ({ record }) => record.region || '-' },
  { title: '服务端地址', dataIndex: 'endpoint', minWidth: 230, ellipsis: true, tooltip: true },
  { title: '公网地址 / 域名', minWidth: 230, ellipsis: true, tooltip: true, render: ({ record }) => record.domain || record.publicEndpoint || '-' },
  { title: '更新时间', dataIndex: 'updateTime', width: 180, render: ({ record }) => record.updateTime ? Dayjs(record.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-' },
  {
    title: '操作',
    width: 240,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        <Button type="text" size="mini" disabled={record.provider === summary.current} loading={switching.value === record.provider} onClick={() => handleSwitch(record.provider)}>
          <IconSwap />
          启用
        </Button>
        <Button type="text" size="mini" loading={testing.value === record.provider} onClick={() => handleTest(record.provider)}>
          <IconLink />
          测试
        </Button>
        <Button type="text" size="mini" onClick={() => configModalRef.value?.open(record.provider, record.label)}>
          <IconEdit />
          配置
        </Button>
      </Space>
    )
  }
]

loadConfig()
</script>

<style lang="scss" scoped></style>
