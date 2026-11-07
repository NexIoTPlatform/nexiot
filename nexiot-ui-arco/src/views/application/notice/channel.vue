<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['notice:channel:add']" type="primary" @click="openForm()"><template #icon><icon-plus /></template>新增渠道</a-button>
      </a-space>
      <a-space wrap>
        <a-input v-model.trim="query.name" class="notice-channel__search" placeholder="渠道名称" allow-clear @press-enter="search" />
        <a-select v-model="query.channelType" class="notice-channel__type" :options="channelTypeOptions" placeholder="渠道类型" allow-clear>
          <template #label="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template>
          <template #option="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template>
        </a-select>
        <a-select v-model="query.status" class="notice-channel__status" :options="statusOptions" placeholder="状态" allow-clear />
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <GiTable
      title="通知渠道列表"
      row-key="id"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1180 }"
      @refresh="refresh"
    />

    <a-modal
      v-model:visible="formVisible"
      :title="form.id ? '编辑通知渠道' : '新增通知渠道'"
      width="min(760px, calc(100vw - 32px))"
      :mask-closable="false"
      :body-style="modalBodyStyle"
      align-center
      unmount-on-close
      >
<a-form :model="form" layout="vertical"
        >
<a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
          >
<a-form-item label="渠道名称" required
            >
<a-input v-model.trim="form.name" />
</a-form-item
          ><a-form-item label="渠道类型" required
            >
<a-select
              v-model="form.channelType"
              :options="channelTypeOptions"
              :disabled="Boolean(form.id)"
              @change="resetConfig">
<template #label="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template><template #option="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template>
</a-select>
</a-form-item
          ><a-form-item label="状态"
            >
<a-switch
              v-model="enabled"
              checked-text="启用"
              unchecked-text="停用" />
</a-form-item>
</a-grid
        ><a-alert
          v-if="channelDefinition.tip"
          type="info"
          class="notice-channel__tip"
          >
{{ channelDefinition.tip }}
</a-alert
        ><a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
          >
<a-form-item
            v-for="field in channelDefinition.fields"
            :key="field.key"
            :label="field.label"
            :required="field.required"
            >
<a-input-password
              v-if="field.secret"
              v-model="form.config[field.key]"
              :placeholder="field.placeholder" /><a-input
              v-else
              v-model.trim="form.config[field.key]"
              :placeholder="field.placeholder" />
</a-form-item>
</a-grid
        ><a-form-item label="备注"
          >
<a-textarea v-model="form.remark" />
</a-form-item>
</a-form
      ><template #footer
        >
<a-space
          >
<a-button @click="formVisible = false">取消</a-button
          ><a-button type="primary" :loading="saving" @click="save"
            >
<template #icon><icon-save /></template>保存
</a-button
          >
</a-space
        >
</template
      >
</a-modal
    ><a-modal v-model:visible="detailVisible" title="通知渠道详情" width="min(760px, calc(100vw - 32px))" :body-style="modalBodyStyle" align-center unmount-on-close
      >
<a-descriptions v-if="currentChannel" :column="{ xs: 1, sm: 2 }" bordered
        >
<a-descriptions-item label="渠道名称">{{ currentChannel.name }}</a-descriptions-item
        ><a-descriptions-item label="渠道类型"><a-space><NoticeChannelIcon :type="currentChannel.channelType" />{{ getChannelTypeLabel(currentChannel.channelType) }}</a-space></a-descriptions-item
        ><a-descriptions-item label="状态"><a-tag :color="currentChannel.status === 0 ? 'green' : 'gray'">{{ currentChannel.status === 0 ? '启用' : '停用' }}</a-tag></a-descriptions-item
        ><a-descriptions-item label="创建人">{{ currentChannel.creator || '-' }}</a-descriptions-item
        ><a-descriptions-item label="创建时间" :span="2">{{ formatDateTime(currentChannel.createTime) }}</a-descriptions-item
        ><a-descriptions-item label="配置" :span="2"><div class="notice-channel__config-list"><div v-for="(value, key) in currentChannel.config" :key="key" class="notice-channel__config-item"><span>{{ configFieldLabel(currentChannel.channelType, String(key)) }}</span><code>{{ displayConfigValue(currentChannel.channelType, String(key), value) }}</code></div></div></a-descriptions-item
        ><a-descriptions-item label="备注" :span="2">{{ currentChannel.remark || '-' }}</a-descriptions-item
      >
</a-descriptions
      ><template #footer><a-button v-if="hasPerm('notice:channel:edit')" type="primary" @click="currentChannel && openForm(currentChannel); detailVisible = false"><template #icon><IconEdit /></template>编辑</a-button></template
    >
</a-modal
    >
<WechatBinding v-if="bindingChannel" :channel="bindingChannel" @close="bindingChannel = undefined" />
</GiPageLayout
  >
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { NoticeChannel } from '@/apis/application/notice'
import {
  Button,
  Message,
  Popconfirm,
  Space,
  Tag,
  Tooltip
} from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye, IconLink } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import { onMounted } from 'vue'
import {
  deleteNoticeChannels,
  getNoticeChannel,
  getNoticeChannels,
  listNoticeChannelFields,
  saveNoticeChannel
} from '@/apis/application/notice'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import { channelTypeOptions } from './modules/channelConfig'
import NoticeChannelIcon from './modules/NoticeChannelIcon.vue'
import WechatBinding from './modules/WechatBinding.vue'

defineOptions({ name: 'ApplicationNoticeChannel' })
const SENSITIVE_CONFIG_KEY_RE = /password|secret|token|accesskey/i

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]
function formatDateTime(value?: string | number) {
  if (!value) return '-'
  const date = Dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
const definitions = reactive<Record<
  string,
  {
    tip: string
    fields: {
      key: string
      label: string
      placeholder: string
      required?: boolean
      secret?: boolean
    }[]
  }
>>({
  wechat: {
    tip: '配置服务号凭据。微信模板 ID、字段映射和跳转地址请在通知模板中分别设置。',
    fields: [
      { key: 'appId', label: '公众号 AppID', placeholder: 'wx...', required: true },
      { key: 'appSecret', label: '公众号 AppSecret', placeholder: '请输入', required: true, secret: true },
      { key: 'oauthCallbackUrl', label: '微信扫码回调地址', placeholder: 'https://公网域名/api/open-api/v1/notice/wechat/callback' }

    ]
  },
  webhook: {
    tip: 'Webhook 将使用 JSON 请求体发送通知。',
    fields: [
      {
        key: 'webhook',
        label: 'Webhook 地址',
        placeholder: 'https://...',
        required: true
      },
      {
        key: 'AuthTokenKey',
        label: '认证头名称',
        placeholder: 'Authorization'
      },
      { key: 'TokenValue', label: '认证值', placeholder: '可选', secret: true }
    ]
  },
  dingTalk: {
    tip: '配置钉钉机器人 Webhook 和可选签名密钥。',
    fields: [
      {
        key: 'webhook',
        label: 'Webhook 地址',
        placeholder: 'https://...',
        required: true
      },
      { key: 'secret', label: '签名密钥', placeholder: '可选', secret: true }
    ]
  },
  feishu: {
    tip: '配置飞书机器人 Webhook。',
    fields: [
      {
        key: 'webhook',
        label: 'Webhook 地址',
        placeholder: 'https://...',
        required: true
      }
    ]
  },
  email: {
    tip: '配置 SMTP 服务。敏感字段编辑时留空会保留已保存的值。',
    fields: [
      {
        key: 'smtpHost',
        label: 'SMTP 服务',
        placeholder: 'smtp.example.com',
        required: true
      },
      {
        key: 'smtpPort',
        label: 'SMTP 端口',
        placeholder: '587',
        required: true
      },
      {
        key: 'username',
        label: '用户名',
        placeholder: '邮箱地址',
        required: true
      },
      {
        key: 'password',
        label: '密码/授权码',
        placeholder: '编辑时可留空',
        secret: true
      },
      {
        key: 'fromEmail',
        label: '发件邮箱',
        placeholder: 'name@example.com',
        required: true
      }
    ]
  },
  sms_ali: {
    tip: '配置阿里云短信的访问凭证、签名和模板。',
    fields: [
      {
        key: 'accessKey',
        label: 'AccessKey ID',
        placeholder: '',
        required: true
      },
      {
        key: 'secretKey',
        label: 'AccessKey Secret',
        placeholder: '编辑时可留空',
        secret: true
      },
      { key: 'signName', label: '短信签名', placeholder: '', required: true },
      {
        key: 'templateCode',
        label: '模板代码',
        placeholder: '',
        required: true
      }
    ]
  },
  sms_tencent: {
    tip: '配置腾讯云短信的访问凭证、签名和模板。',
    fields: [
      { key: 'secretId', label: 'SecretId', placeholder: '', required: true },
      {
        key: 'secretKey',
        label: 'SecretKey',
        placeholder: '编辑时可留空',
        secret: true
      },
      { key: 'signName', label: '短信签名', placeholder: '', required: true },
      {
        key: 'templateCode',
        label: '模板代码',
        placeholder: '',
        required: true
      }
    ]
  },
  audio_ali: {
    tip: '配置阿里云语音通知。',
    fields: [
      {
        key: 'accessKey',
        label: 'AccessKey ID',
        placeholder: '',
        required: true
      },
      {
        key: 'secretKey',
        label: 'AccessKey Secret',
        placeholder: '编辑时可留空',
        secret: true
      },
      { key: 'signName', label: '语音签名', placeholder: '', required: true },
      {
        key: 'templateCode',
        label: '模板代码',
        placeholder: '',
        required: true
      }
    ]
  },
  audio_tencent: {
    tip: '配置腾讯云语音通知。',
    fields: [
      { key: 'secretId', label: 'SecretId', placeholder: '', required: true },
      {
        key: 'secretKey',
        label: 'SecretKey',
        placeholder: '编辑时可留空',
        secret: true
      },
      { key: 'signName', label: '语音签名', placeholder: '', required: true },
      {
        key: 'templateCode',
        label: '模板代码',
        placeholder: '',
        required: true
      }
    ]
  }
})
const query = reactive({
  name: '',
  channelType: '',
  status: undefined as number | undefined
})
const { loading, tableData, pagination, search, refresh } = useTable<NoticeChannel>({
  listAPI: (page) =>
    getNoticeChannels({
      ...page,
      name: query.name || undefined,
      channelType: query.channelType || undefined,
      status: query.status
    }),
  immediate: true
})
const formVisible = ref(false)
const modalBodyStyle = { maxHeight: 'min(72vh, 700px)', overflowY: 'auto' as const }
const saving = ref(false)
const bindingChannel = ref<NoticeChannel>()
const detailVisible = ref(false)
const currentChannel = ref<NoticeChannel>()
const form = reactive<NoticeChannel>({
  name: '',
  channelType: 'webhook',
  status: 0,
  config: {},
  remark: ''
})
const enabled = computed({
  get: () => form.status === 0,
  set: (value) => {
    form.status = value ? 0 : 1
  }
})
const channelDefinition = computed(
  () => definitions[form.channelType] || { tip: '', fields: [] }
)
onMounted(async () => {
  try {
    const fields = (await listNoticeChannelFields()).data || []
    if (fields.length) {
      definitions.wechat.fields = fields.filter((item) => item.key && item.label).map((item) => {
        const [type, required, placeholder] = (item.meta || 'input|optional|').split('|')
        return { key: item.key, label: item.label, placeholder: placeholder || '', required: required === 'required', secret: type === 'password' }
      })
    }
  } catch {
    // 字典不可用时保留内置字段，保证渠道页面仍可配置
  }
})
function resetConfig() {
  form.config = {}
}
function resetForm() {
  Object.assign(form, {
    id: '',
    name: '',
    channelType: 'webhook',
    status: 0,
    config: {},
    remark: ''
  })
}
function openForm(record?: NoticeChannel) {
  if (!record) {
    resetForm()
    formVisible.value = true
    return
  }
  getNoticeChannel(String(record.id)).then((res) => {
    Object.assign(form, res.data, { config: res.data.config || {} })
    formVisible.value = true
  })
}
function reset() {
  Object.assign(query, { name: '', channelType: '', status: undefined })
  search()
}
function getChannelTypeLabel(type: string) {
  return channelTypeOptions.find((item) => item.value === type)?.label || type
}
function configField(type: string, key: string) {
  return definitions[type]?.fields.find((item) => item.key === key)
}
function configFieldLabel(type: string, key: string) {
  return configField(type, key)?.label || key
}
function displayConfigValue(type: string, key: string, value: unknown) {
  if (!value) return '-'
  const sensitive = configField(type, key)?.secret || SENSITIVE_CONFIG_KEY_RE.test(key)
  return sensitive ? '********************************' : String(value)
}
async function openDetail(record: NoticeChannel) {
  currentChannel.value = (await getNoticeChannel(String(record.id))).data
  detailVisible.value = true
}
async function save() {
  if (!form.name) {
    Message.warning('请输入渠道名称')
    return
  }
  saving.value = true
  try {
    await saveNoticeChannel(form)
    Message.success('保存成功')
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}
async function remove(record: NoticeChannel) {
  await deleteNoticeChannels([String(record.id)])
  search()
  return true
}
const columns: TableColumnData[] = [
  {
    title: '渠道名称 / ID',
    dataIndex: 'name',
    width: 260,
    fixed: 'left',
    render: ({ record }) => (
      <div class="notice-channel__identity">
        {hasPerm('notice:channel:detail')
          ? <Button type="text" onClick={() => openDetail(record as NoticeChannel)}><strong>{record.name || '未命名渠道'}</strong></Button>
          : <strong>{record.name || '未命名渠道'}</strong>}
        <code>{record.id || '-'}</code>
      </div>
    )
  },
  {
    title: '渠道类型',
    width: 150,
    render: ({ record }) => (
      <Space>
        <NoticeChannelIcon type={record.channelType} />
        {getChannelTypeLabel(record.channelType)}
      </Space>
    )
  },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag color={record.status === 0 ? 'green' : 'gray'}>
        <span class="notice-channel__status-dot" />
        {record.status === 0 ? '启用' : '停用'}
      </Tag>
    )
  },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '创建人', dataIndex: 'creator', width: 150 },
  {
    title: '创建时间',
    width: 180,
    render: ({ record }) => formatDateTime(record.createTime)
  },
  {
    title: '操作',
    width: 160,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space size={2}>
        {record.channelType === 'wechat' && hasPerm('notice:channel:edit') && <Tooltip content="微信绑定"><Button type="text" shape="circle" aria-label="微信绑定" onClick={() => { bindingChannel.value = record as NoticeChannel }}><IconLink /></Button></Tooltip>}
        {hasPerm('notice:channel:detail') && (
          <Tooltip content="详情">
            <Button type="text" shape="circle" aria-label="查看详情" onClick={() => openDetail(record as NoticeChannel)}>
              <IconEye />
            </Button>
          </Tooltip>
        )}
        {hasPerm('notice:channel:edit') && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              aria-label="编辑"
              onClick={() => openForm(record as NoticeChannel)}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {hasPerm('notice:channel:del') && (
          <Popconfirm
            content="确定删除该渠道吗？已被模板引用或已有微信绑定的渠道不能直接删除。"
            onBeforeOk={() => remove(record as NoticeChannel)}
          >
            <Tooltip content="删除">
              <Button type="text" shape="circle" status="danger" aria-label="删除">
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
.notice-channel {
  &__search {
    width: 150px;
  }

  &__type {
    width: 180px;
  }

  &__status {
    width: 120px;
  }

  &__tip {
    margin-bottom: 16px;
  }

  &__config-list {
    display: grid;
    gap: 8px;
  }

  &__config-item {
    display: grid;
    grid-template-columns: minmax(120px, 0.35fr) minmax(0, 1fr);
    gap: 12px;
    align-items: center;
    padding: 8px 10px;
    background: var(--color-fill-1);
  }

  &__config-item span {
    color: var(--color-text-3);
  }

  &__config-item code {
    overflow-wrap: anywhere;
  }
}

:deep(.notice-channel__identity) {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  .arco-btn {
    justify-content: flex-start;
    height: auto;
    padding: 0;
  }

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  code {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--color-text-3);
    white-space: nowrap;
  }
}

:deep(.notice-channel__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}
</style>
