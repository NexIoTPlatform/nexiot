<template>
  <GiPageLayout margin>
    <a-row justify="space-between" class="g-row-tool">
      <a-space wrap>
        <a-button v-hasPerm="['notice:template:add']" type="primary" @click="openForm()"><template #icon><icon-plus /></template>新增模板</a-button>
        <a-button v-hasPerm="['notice:template:edit']" :disabled="!selectedKeys.length" @click="setStatus(true)">批量启用</a-button>
        <a-button v-hasPerm="['notice:template:edit']" status="warning" :disabled="!selectedKeys.length" @click="setStatus(false)">批量停用</a-button>
      </a-space>
      <a-space wrap>
        <a-input v-model.trim="query.name" class="notice-template__search" placeholder="模板名称" allow-clear @press-enter="search" />
        <a-select v-model="query.channelType" class="notice-template__type" :options="channelTypeOptions" placeholder="渠道类型" allow-clear>
          <template #label="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template>
          <template #option="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template>
        </a-select>
        <GiButton type="search" @click="search" />
        <GiButton type="reset" @click="reset" />
      </a-space>
    </a-row>

    <GiTable
      title="通知模板列表"
      row-key="id"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: '100%', y: '100%', minWidth: 1280 }"
      :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      :selected-keys="selectedKeys"
      @select="select"
      @select-all="selectAll"
      @refresh="refresh"
    />

    <a-modal
      v-model:visible="formVisible"
      :title="form.id ? '编辑通知模板' : '新增通知模板'"
      width="min(820px, calc(100vw - 32px))"
      :mask-closable="false"
      :body-style="modalBodyStyle"
      align-center
      unmount-on-close
      >
<a-form :model="form" layout="vertical"
        >
<a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16"
          >
<a-form-item label="模板名称" required
            >
<a-input v-model.trim="form.name" />
</a-form-item
          ><a-form-item label="通知渠道" required
            >
<a-select
              v-model="form.channelType"
              :options="channelTypeOptions"
              @change="loadChannels">
<template #label="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template><template #option="{ data }"><a-space><NoticeChannelIcon :type="String(data.value)" />{{ data.label }}</a-space></template>
</a-select>
</a-form-item
          ><a-form-item label="渠道配置" required
            >
<a-select
              v-model="form.channelId"
              :options="channelOptions"
              placeholder="请选择渠道">
<template #label="{ data }"><a-space><NoticeChannelIcon :type="form.channelType" />{{ data.label }}</a-space></template><template #option="{ data }"><a-space><NoticeChannelIcon :type="form.channelType" />{{ data.label }}</a-space></template>
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
        ><WechatTemplateEditor v-if="form.channelType === 'wechat' && form.wechat" v-model="form.wechat" v-model:content="form.content" /><a-form-item v-else label="模板内容" required
          >
<a-textarea
            v-model="form.content"
            :auto-size="{ minRows: 8, maxRows: 16 }"
            placeholder="支持 #{variable} 占位符" />
          <div class="notice-template__variables">
            <a-input v-model.trim="variableName" placeholder="变量名，如 deviceName" @press-enter="insertVariable" />
            <a-button :disabled="!variableName" @click="insertVariable"><template #icon><icon-plus /></template>插入变量</a-button>
          </div>
          <a-space v-if="contentVariables.length" wrap><a-tag v-for="item in contentVariables" :key="item" color="arcoblue">{{ item }}</a-tag></a-space>
</a-form-item
        ><a-form-item label="默认接收人"
          >
<WechatReceiverInput v-if="form.channelType === 'wechat'" v-model="form.receivers" :channel-id="form.channelId" :channel-name="channelMap[form.channelId || '']" />
<a-input v-else
            v-model.trim="form.receivers"
            :placeholder="form.channelType === 'wechat' ? 'OpenID 或 user:绑定标识，多个用逗号分隔' : '多个接收人用逗号分隔'" />
</a-form-item
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
    ><a-modal
      v-model:visible="sendVisible"
      :title="sendMode === 'test' ? '测试模板' : '发送通知'"
      width="min(620px, calc(100vw - 32px))"
      :body-style="modalBodyStyle"
      align-center
      unmount-on-close
      @before-ok="send"
      >
<a-form :model="sendForm" layout="vertical"
        >
<a-form-item label="接收人" required
          >
<WechatReceiverInput v-if="sendTemplate?.channelType === 'wechat'" v-model="sendForm.receivers" :channel-id="sendTemplate.channelId" />
<a-input v-else
            v-model.trim="sendForm.receivers"
            placeholder="多个接收人用逗号分隔" />
</a-form-item
        ><NoticeParameterForm v-if="sendTemplate" :key="sendTemplate.id" v-model="sendForm.params" :template="sendTemplate" />
</a-form>
</a-modal
    ><a-modal v-model:visible="detailVisible" title="通知模板详情" width="min(820px, calc(100vw - 32px))" :body-style="modalBodyStyle" align-center unmount-on-close
      >
<a-descriptions v-if="currentTemplate" :column="{ xs: 1, sm: 2 }" bordered
        >
<a-descriptions-item label="模板名称">{{ currentTemplate.name }}</a-descriptions-item
        ><a-descriptions-item label="渠道类型"><a-space><NoticeChannelIcon :type="currentTemplate.channelType" />{{ getChannelTypeLabel(currentTemplate.channelType) }}</a-space></a-descriptions-item
        ><a-descriptions-item label="状态"><a-tag :color="currentTemplate.status === 1 ? 'green' : 'gray'">{{ currentTemplate.status === 1 ? '启用' : '停用' }}</a-tag></a-descriptions-item
        ><a-descriptions-item label="渠道配置">{{ getChannelName(currentTemplate.channelId) }}</a-descriptions-item
        ><a-descriptions-item label="模板内容" :span="2"><pre class="notice-template__content">{{ currentTemplate.content }}</pre></a-descriptions-item
        ><a-descriptions-item label="内容变量" :span="2"><a-space wrap><a-tag v-for="item in noticeVariables(currentTemplate)" :key="item" color="arcoblue">{{ item }}</a-tag><span v-if="!noticeVariables(currentTemplate).length">无</span></a-space></a-descriptions-item
        ><a-descriptions-item v-if="currentTemplate.wechat" label="微信字段映射" :span="2"><div>微信模板 ID：{{ currentTemplate.wechat.templateId }}</div><div v-for="field in currentTemplate.wechat.fields" :key="field.key">{{ field.label }} / {{ field.key }} → {{ field.value }}</div></a-descriptions-item><a-descriptions-item label="默认接收人" :span="2">{{ currentTemplate.receivers || '-' }}</a-descriptions-item
        ><a-descriptions-item label="备注" :span="2">{{ currentTemplate.remark || '-' }}</a-descriptions-item
      >
</a-descriptions
      ><template #footer><a-space><a-button v-if="hasPerm('notice:template:edit')" @click="detailVisible = false; currentTemplate && openSend(currentTemplate, 'test')"><template #icon><IconSend /></template>测试</a-button><a-button v-if="hasPerm('notice:template:edit')" type="primary" @click="detailVisible = false; currentTemplate && openForm(currentTemplate)"><template #icon><IconEdit /></template>编辑</a-button></a-space></template
    >
</a-modal
  >
</GiPageLayout>
</template>

<script setup lang="tsx">
import type { TableColumnData } from '@arco-design/web-vue'
import type { NoticeChannel, NoticeTemplate } from '@/apis/application/notice'
import {
  Button,
  Message,
  Popconfirm,
  Space,
  Tag,
  Tooltip
} from '@arco-design/web-vue'
import { IconDelete, IconEdit, IconEye, IconSend } from '@arco-design/web-vue/es/icon'
import Dayjs from 'dayjs'
import {
  deleteNoticeTemplates,
  disableNoticeTemplates,
  enableNoticeTemplates,
  getNoticeChannels,
  getNoticeTemplate,
  getNoticeTemplates,
  saveNoticeTemplate,
  sendNotice,
  testNoticeTemplate
} from '@/apis/application/notice'
import { useTable } from '@/hooks'
import { hasPerm } from '@/utils/permission'
import { channelTypeOptions } from './modules/channelConfig'
import NoticeChannelIcon from './modules/NoticeChannelIcon.vue'
import NoticeParameterForm from './modules/NoticeParameterForm.vue'
import { emptyWechat, noticeVariables } from './modules/wechat'
import WechatReceiverInput from './modules/WechatReceiverInput.vue'
import WechatTemplateEditor from './modules/WechatTemplateEditor.vue'

defineOptions({ name: 'ApplicationNoticeTemplate' })

const query = reactive({ name: '', channelType: '' })
const { loading, tableData, pagination, selectedKeys, search, refresh, select, selectAll } = useTable<NoticeTemplate>({
  listAPI: (page) =>
    getNoticeTemplates({
      ...page,
      name: query.name || undefined,
      channelType: query.channelType || undefined
    }),
  immediate: true
})
const formVisible = ref(false)
const modalBodyStyle = { maxHeight: 'min(72vh, 700px)', overflowY: 'auto' as const }
const saving = ref(false)
const form = reactive<NoticeTemplate>({
  name: '',
  channelType: 'webhook',
  channelId: '',
  content: '',
  wechat: emptyWechat(),
  receivers: '',
  status: 1,
  remark: ''
})
const enabled = computed({
  get: () => form.status === 1,
  set: (value) => {
    form.status = value ? 1 : 0
  }
})
const channelOptions = ref<{ label: string, value: string }[]>([])
const sendVisible = ref(false)
const sendMode = ref<'test' | 'send'>('test')
const sendTemplate = ref<NoticeTemplate>()
const sendForm = reactive({ receivers: '', params: {} as Record<string, unknown> })
const detailVisible = ref(false)
const currentTemplate = ref<NoticeTemplate>()
const variableName = ref('')
const channelMap = ref<Record<string, string>>({})
const VARIABLE_RE = /(?:#|\$)\{\s*([a-z_$][\w$.-]*)\s*\}|\{\{\s*([a-z_$][\w$.-]*)\s*\}\}/gi
const EMAIL_RE = /^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/
const PHONE_RE = /^\+?[0-9-]{6,20}$/
const contentVariables = computed(() => extractVariables(form.content))
function resetForm() {
  Object.assign(form, {
    id: '',
    name: '',
    channelType: 'webhook',
    channelId: '',
    content: '',
    wechat: emptyWechat(),
    receivers: '',
    status: 1,
    remark: ''
  })
}
async function loadChannels() {
  const res = await getNoticeChannels({
    page: 1,
    size: 200,
    channelType: form.channelType,
    status: 0
  })
  channelOptions.value = (res.data.records || []).map(
    (item: NoticeChannel) => ({ label: item.name, value: String(item.id) })
  )
  channelMap.value = Object.fromEntries(channelOptions.value.map((item) => [item.value, item.label]))
  if (!channelOptions.value.some((item) => item.value === form.channelId))
    form.channelId = ''
}
function extractVariables(content = '') {
  return [
    ...new Set(
      Array.from(content.matchAll(VARIABLE_RE), (match) => match[1] || match[2])
    )
  ]
}
function insertVariable() {
  if (!variableName.value) return
  const token = `#{${variableName.value}}`
  form.content = `${form.content}${form.content ? '\n' : ''}${token}`
  variableName.value = ''
}
function formatDateTime(value?: string | number) {
  if (!value) return '-'
  const date = Dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
function getChannelTypeLabel(type: string) {
  return channelTypeOptions.find((item) => item.value === type)?.label || type
}
function getChannelName(id?: string) {
  return id ? channelMap.value[id] || id : '-'
}
async function openDetail(record: NoticeTemplate) {
  currentTemplate.value = (await getNoticeTemplate(String(record.id))).data
  form.channelType = currentTemplate.value.channelType
  await loadChannels()
  detailVisible.value = true
}
async function openForm(record?: NoticeTemplate) {
  if (!record) {
    resetForm()
    await loadChannels()
    formVisible.value = true
    return
  }
  resetForm()
  Object.assign(form, (await getNoticeTemplate(String(record.id))).data)
  form.wechat ||= emptyWechat()
  form.channelId = String(form.channelId || '')
  await loadChannels()
  formVisible.value = true
}
function reset() {
  Object.assign(query, { name: '', channelType: '' })
  search()
}
async function save() {
  if (!form.name || !form.content || !form.channelId) {
    Message.warning('请填写模板名称、渠道和内容')
    return
  }
  const invalidReceiver = validateReceivers(form.receivers || '', form.channelType)
  if (invalidReceiver) {
    Message.warning(invalidReceiver)
    return
  }
  saving.value = true
  try {
    await saveNoticeTemplate(form)
    Message.success('保存成功')
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}
function validateReceivers(receivers: string, channelType: string) {
  if (!receivers) return ''
  const values = receivers.split(',').map((item) => item.trim()).filter(Boolean)
  if (channelType === 'email' && values.some((item) => !EMAIL_RE.test(item))) return '邮箱接收人格式不正确'
  if ((channelType.startsWith('sms_') || channelType.startsWith('audio_')) && values.some((item) => !PHONE_RE.test(item))) return '手机号接收人格式不正确'
  return ''
}
async function setStatus(enabled: boolean) {
  if (!selectedKeys.value.length) return
  await (enabled
    ? enableNoticeTemplates(selectedKeys.value.map(String))
    : disableNoticeTemplates(selectedKeys.value.map(String)))
  Message.success(enabled ? '已启用' : '已停用')
  selectedKeys.value = []
  search()
}
async function remove(record: NoticeTemplate) {
  await deleteNoticeTemplates([String(record.id)])
  search()
  return true
}
function openSend(template: NoticeTemplate, mode: 'test' | 'send') {
  sendTemplate.value = template
  sendMode.value = mode
  Object.assign(sendForm, {
    receivers: template.receivers || '',
    params: Object.fromEntries(noticeVariables(template).map((key) => [key, '']))
  })
  sendVisible.value = true
}
async function send() {
  if (!sendTemplate.value || !sendForm.receivers) {
    Message.warning('请输入接收人')
    return false
  }
  const params = sendForm.params
  await (sendMode.value === 'test'
    ? testNoticeTemplate({
        templateId: String(sendTemplate.value.id),
        receivers: sendForm.receivers,
        params
      })
    : sendNotice({
        templateId: String(sendTemplate.value.id),
        receivers: sendForm.receivers,
        params
      }))
  Message.success(sendTemplate.value.channelType === 'wechat' ? '微信已受理，请核对微信端卡片内容' : '通知已发送')
  return true
}
const columns: TableColumnData[] = [
  {
    title: '模板名称 / ID',
    dataIndex: 'name',
    width: 260,
    fixed: 'left',
    render: ({ record }) => (
      <div class="notice-template__identity">
        {hasPerm('notice:template:detail')
          ? <Button type="text" onClick={() => openDetail(record as NoticeTemplate)}><strong>{record.name || '未命名模板'}</strong></Button>
          : <strong>{record.name || '未命名模板'}</strong>}
        <code>{record.id || '-'}</code>
      </div>
    )
  },
  {
    title: '渠道类型',
    width: 130,
    render: ({ record }) => (
      <Space>
        <NoticeChannelIcon type={record.channelType} />
        {getChannelTypeLabel(record.channelType)}
      </Space>
    )
  },
  { title: '模板内容', dataIndex: 'content', ellipsis: true, tooltip: true },
  {
    title: '默认接收人',
    dataIndex: 'receivers',
    width: 200,
    ellipsis: true,
    tooltip: true
  },
  {
    title: '状态',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag color={record.status === 1 ? 'green' : 'gray'}>
        <span class="notice-template__status-dot" />
        {record.status === 1 ? '启用' : '停用'}
      </Tag>
    )
  },
  {
    title: '创建时间',
    width: 180,
    render: ({ record }) => formatDateTime(record.createTime)
  },
  {
    title: '操作',
    width: 190,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space size={2}>
        {hasPerm('notice:template:detail') && (
          <Tooltip content="详情">
            <Button type="text" shape="circle" aria-label="查看详情" onClick={() => openDetail(record as NoticeTemplate)}>
              <IconEye />
            </Button>
          </Tooltip>
        )}
        {hasPerm('notice:template:edit') && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              aria-label="编辑"
              onClick={() => openForm(record as NoticeTemplate)}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {hasPerm('notice:template:edit') && (
          <Tooltip content="测试">
            <Button
              type="text"
              shape="circle"
              status="success"
              aria-label="测试模板"
              onClick={() => openSend(record as NoticeTemplate, 'test')}
            >
              <IconSend />
            </Button>
          </Tooltip>
        )}
        {hasPerm('notice:template:edit') && (
          <Tooltip content="发送">
            <Button
              type="text"
              shape="circle"
              aria-label="发送通知"
              onClick={() => openSend(record as NoticeTemplate, 'send')}
            >
              <IconSend />
            </Button>
          </Tooltip>
        )}
        {hasPerm('notice:template:del') && (
          <Popconfirm
            content="确定删除该模板吗？"
            onBeforeOk={() => remove(record as NoticeTemplate)}
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
.notice-template {
  &__search {
    width: 150px;
  }

  &__type {
    width: 180px;
  }

  &__variables {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 8px;
    margin-top: 8px;
  }

  &__content {
    max-height: 320px;
    margin: 0;
    overflow: auto;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }
}

:deep(.notice-template__identity) {
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

:deep(.notice-template__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}
</style>
