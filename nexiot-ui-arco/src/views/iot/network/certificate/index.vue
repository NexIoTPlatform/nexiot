<template>
  <GiPageLayout margin :body-style="{ overflowY: 'auto' }">
    <a-row justify="space-between" class="g-row-tool">
      <a-space>
      <a-button v-hasPerm="['network:cert:add']" type="primary" @click="formDrawerRef?.add()"
        >
<template #icon><icon-plus /></template>新增证书
</a-button
      >
      <a-popconfirm content="确定删除选中的证书吗？默认证书不会被选择。" @before-ok="batchDelete">
        <a-button v-hasPerm="['network:cert:delete']" status="danger" :disabled="!selectedKeys.length"><template #icon><IconDelete /></template>批量删除</a-button>
      </a-popconfirm>
      </a-space>
      <a-space wrap>
        <a-input
          v-model.trim="query.name"
          placeholder="证书名称"
          allow-clear
          @press-enter="search"
        />
        <a-input
          v-model.trim="query.sslKey"
          placeholder="证书标识"
          allow-clear
          @press-enter="search"
        />
        <a-range-picker v-model="expireRange" value-format="YYYY-MM-DD" />
        <GiButton type="search" @click="search" /><GiButton
          type="reset"
          @click="reset"
        />
      </a-space>
    </a-row>
    <a-table
      class="g-table"
      row-key="id"
      :loading="loading"
      :data="tableData"
      :columns="columns"
      :pagination="pagination"
      :bordered="{ cell: true }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true, onlyCurrent: false }"
      :selected-keys="selectedKeys"
      :scroll="{ x: '100%', y: '100%', minWidth: 1100 }"
      @select="select"
      @select-all="selectAll"
    />
    <CertificateFormDrawer ref="formDrawerRef" @save-success="search" />
    <a-modal
      v-model:visible="detailVisible"
      title="证书详情"
      width="min(900px, calc(100vw - 32px))"
      align-center
      :body-style="modalBodyStyle"
      unmount-on-close
    >
      <template v-if="current">
      <div class="certificate-page__header"><div><h3>{{ current.name || '未命名证书' }}</h3><a-typography-text copyable>{{ current.sslKey }}</a-typography-text></div><a-tag :color="getExpireStatus(current.expireTime).color">{{ getExpireStatus(current.expireTime).text }}</a-tag></div>
      <a-descriptions :column="descriptionColumns" bordered>
        <a-descriptions-item label="证书名称">
{{
          current.name
        }}
</a-descriptions-item
        ><a-descriptions-item label="到期时间">
{{
          formatDate(current.expireTime)
        }}
</a-descriptions-item
        ><a-descriptions-item label="证书类型">{{ certificateTypeLabel(current.certificateType) }}</a-descriptions-item
        ><a-descriptions-item label="信任模式">{{ current.trustMode || '-' }}</a-descriptions-item
        ><a-descriptions-item label="客户端证书指纹" :span="2">{{ current.fingerprintSha256 || '-' }}</a-descriptions-item
        ><a-descriptions-item label="信任证书指纹" :span="2">{{ current.trustFingerprintSha256 || '-' }}</a-descriptions-item
        ><a-descriptions-item label="创建人">
{{
          current.createUser || "-"
        }}
</a-descriptions-item
        ><a-descriptions-item label="创建时间">{{ formatDate(current.createTime) }}</a-descriptions-item
        ><a-descriptions-item label="更新时间">{{ formatDate(current.updateTime) }}</a-descriptions-item
        ><a-descriptions-item label="证书主题">{{ certInfo.subject || '-' }}</a-descriptions-item
        ><a-descriptions-item label="签发机构">{{ certInfo.issuer || '-' }}</a-descriptions-item
        ><a-descriptions-item label="有效期起始">{{ formatDate(certInfo.notBefore) }}</a-descriptions-item
        ><a-descriptions-item label="有效期截止">{{ formatDate(certInfo.notAfter) }}</a-descriptions-item
        ><a-descriptions-item label="序列号">{{ certInfo.serialNumber || '-' }}</a-descriptions-item
        ><a-descriptions-item label="签名算法">{{ certInfo.signatureAlgorithm || '-' }}</a-descriptions-item
        ><a-descriptions-item label="备注" :span="2">
{{
          current.remark || "-"
        }}
</a-descriptions-item>
      </a-descriptions>
      </template>
      <template #footer><a-button @click="detailVisible = false">关闭</a-button></template>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { CertificateItem } from '@/apis/iot/certificate'
import { Button, Message, Popconfirm, Space, Tag, Tooltip } from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye } from '@arco-design/web-vue/es/icon'
import { useMediaQuery } from '@vueuse/core'
import Dayjs from 'dayjs'
import {
  deleteCertificate,
  getCertificate,
  getCertificateList
} from '@/apis/iot/certificate'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import CertificateFormDrawer from './CertificateFormDrawer.vue'

defineOptions({ name: 'IotNetworkCertificate' })
const formDrawerRef
  = useTemplateRef<InstanceType<typeof CertificateFormDrawer>>('formDrawerRef')
const query = reactive({ name: '', sslKey: '' })
const expireRange = ref<string[]>([])
const detailVisible = ref(false)
const current = ref<CertificateItem>()
const modalBodyStyle = {
  maxHeight: 'min(72vh, 720px)',
  overflowY: 'auto' as const
}
const isNarrow = useMediaQuery('(max-width: 720px)')
const descriptionColumns = computed(() => isNarrow.value ? 1 : 2)
const { loading, tableData, pagination, selectedKeys, search, select, selectAll } = useTable<CertificateItem>({
  listAPI: (page) =>
    getCertificateList({
      ...page,
      name: query.name || undefined,
      sslKey: query.sslKey || undefined,
      expireStart: expireRange.value?.[0],
      expireEnd: expireRange.value?.[1]
    }),
  formatResult: (rows) => rows.map((item) => ({ ...item, disabled: item.sslKey === 'default-tcp' })),
  immediate: true
})
function reset() {
  Object.assign(query, { name: '', sslKey: '' })
  expireRange.value = []
  search()
}
function getExpireStatus(expireTime?: string) {
  if (!expireTime) return { text: '长期有效', color: 'blue' }
  const days = (new Date(expireTime).getTime() - Date.now()) / 86400000
  return days < 0
    ? { text: '已过期', color: 'red' }
    : days < 30
      ? { text: '即将到期', color: 'orange' }
      : { text: '有效', color: 'green' }
}
async function openDetail(item: CertificateItem) {
  current.value = (await getCertificate(item.sslKey)).data
  detailVisible.value = true
}
async function remove(item: CertificateItem) {
  await deleteCertificate(item.id)
  search()
  return true
}
const certInfo = computed<Record<string, unknown>>(() => {
  const value = current.value?.certInfo
  if (!value) return {}
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
})
function formatDate(value: unknown) {
  if (!value) return '-'
  const date = Dayjs(value as string | number)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
function certificateTypeLabel(value?: string) {
  return value === 'OPCUA_BUNDLE' ? 'OPC UA 证书包' : value || '通用 TLS 证书'
}
async function batchDelete() {
  for (const id of selectedKeys.value) await deleteCertificate(String(id))
  Message.success('删除成功')
  search()
  return true
}
const columns: TableColumnData[] = [
  {
    title: '证书名称',
    width: 220,
    render: ({ record }) => (
      <div>
        <strong>{record.name}</strong>
        <div class="certificate-page__subtitle">{record.sslKey}</div>
      </div>
    )
  },
  {
    title: '证书类型',
    width: 150,
    render: ({ record }) => <Tag color={record.certificateType === 'OPCUA_BUNDLE' ? 'arcoblue' : 'gray'}>{certificateTypeLabel(record.certificateType)}</Tag>
  },
  {
    title: '到期时间',
    width: 180,
    render: ({ record }) => formatDate(record.expireTime)
  },
  {
    title: '状态',
    width: 110,
    align: 'center',
    render: ({ record }) => {
      const status = getExpireStatus(record.expireTime)
      return <Tag color={status.color}>{status.text}</Tag>
    }
  },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '创建人', dataIndex: 'createUser', width: 150 },
  {
    title: '操作',
    width: 150,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space>
        {hasPerm('network:cert:query') && (
          <Tooltip content="详情">
            <Button
              type="text"
              shape="circle"
              aria-label="详情"
              onClick={() => openDetail(record as CertificateItem)}
            >
              <IconEye />
            </Button>
          </Tooltip>
        )}
        {hasPerm('network:cert:edit') && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              aria-label="编辑"
              onClick={() => formDrawerRef.value?.edit(record.sslKey)}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {hasPerm('network:cert:delete') && record.sslKey !== 'default-tcp' && (
          <Popconfirm
            content="确定删除该证书吗？"
            onBeforeOk={() => remove(record as CertificateItem)}
          >
            <Tooltip content="删除">
              <Button
                type="text"
                status="danger"
                shape="circle"
                aria-label="删除"
              >
                <IconDelete />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]
</script>

<style lang="scss" scoped>
.certificate-page {
  &__subtitle {
    margin-top: 3px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__info {
    max-height: 360px;
    margin: 0;
    overflow: auto;
    word-break: break-all;
    white-space: pre-wrap;
  }

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &__header h3 {
    margin: 0 0 4px;
  }

  &__content {
    margin-top: 16px;
  }

  &__content-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    margin-bottom: 8px;
  }
}
</style>
