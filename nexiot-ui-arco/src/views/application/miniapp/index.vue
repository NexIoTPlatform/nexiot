<template>
  <GiPageLayout margin>
    <a-tabs v-model:active-key="activeTab" hide-content class="miniapp__tabs">
      <a-tab-pane key="apps" title="小程序应用" />
      <a-tab-pane key="users" title="小程序用户" />
    </a-tabs>
    <template v-if="activeTab === 'apps'">
        <a-row justify="space-between" class="g-row-tool">
          <a-space wrap>
            <a-button v-hasPerm="['application:miniapp:add']" type="primary" @click="appFormRef?.add()">
              <template #icon><icon-plus /></template>新增小程序
            </a-button>
            <a-popconfirm v-hasPerm="['application:miniapp:add']" content="确定同步全部关联应用的密钥吗？" @before-ok="syncSecrets">
              <a-button><template #icon><IconRefresh /></template>同步密钥</a-button>
            </a-popconfirm>
          </a-space>
          <a-space wrap>
            <a-input v-model.trim="appQuery.name" class="miniapp__search" placeholder="小程序名称" allow-clear @press-enter="submitAppSearch" />
            <a-select v-model="appQuery.status" class="miniapp__status" :options="appStatusOptions" placeholder="状态" allow-clear />
            <GiButton type="search" @click="submitAppSearch" />
            <GiButton type="reset" @click="resetApps" />
          </a-space>
        </a-row>
        <GiTable
          title="小程序列表"
          row-key="appId"
          :loading="appLoading"
          :data="apps"
          :columns="appColumns"
          :pagination="appPagination"
          :scroll="{ x: '100%', y: '100%', minWidth: 1120 }"
          @page-change="handleAppPageChange"
          @page-size-change="handleAppPageSizeChange"
          @refresh="searchApps"
        />
    </template>

    <template v-else>
        <a-row justify="space-between" class="g-row-tool">
          <a-button v-hasPerm="['application:miniapp:user:add']" type="primary" :disabled="!selectedAppId" @click="openUserForm">
            <template #icon><icon-plus /></template>新增用户
          </a-button>
          <a-space wrap>
            <a-select
              v-model="selectedAppId"
              class="miniapp__app-filter"
              :options="miniAppOptions"
              :loading="miniAppOptionLoading"
              placeholder="选择小程序"
              allow-search
              :filter-option="false"
              @search="loadMiniAppOptions"
              @dropdown-visible-change="(open) => open && loadMiniAppOptions()"
              @change="changeUserApp"
            />
            <a-input v-model.trim="userQuery.phone" class="miniapp__search" placeholder="手机号" allow-clear @press-enter="submitUserSearch" />
            <a-input v-model.trim="userQuery.nickname" class="miniapp__search" placeholder="昵称" allow-clear @press-enter="submitUserSearch" />
            <a-select v-model="userQuery.status" class="miniapp__status" :options="userStatusOptions" placeholder="状态" allow-clear />
            <GiButton type="search" @click="submitUserSearch" />
            <GiButton type="reset" @click="resetUsers" />
          </a-space>
        </a-row>
        <GiTable
          v-if="selectedAppId"
          title="小程序用户列表"
          row-key="id"
          :loading="userLoading"
          :data="users"
          :columns="userColumns"
          :pagination="userPagination"
          :scroll="{ x: '100%', y: '100%', minWidth: 1100 }"
          @page-change="handleUserPageChange"
          @page-size-change="handleUserPageSizeChange"
          @refresh="searchUsers"
        />
        <a-empty v-else description="请选择小程序后管理用户" />
    </template>

    <MiniAppFormDrawer ref="appFormRef" @save-success="handleAppSaved" />

    <a-modal
      v-model:visible="userFormVisible"
      title="新增小程序用户"
      width="min(720px, calc(100vw - 32px))"
      align-center
      :mask-closable="false"
      :body-style="modalBodyStyle"
      @before-ok="saveUser"
    >
      <a-form ref="userFormRef" :model="userForm" :rules="userFormRules" layout="vertical">
        <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
          <a-form-item label="用户名" field="username"><a-input v-model.trim="userForm.username" placeholder="字母、数字或下划线" /></a-form-item>
          <a-form-item label="密码" field="password"><a-input-password v-model="userForm.password" placeholder="设置用户名时必填，6-32 位" /></a-form-item>
          <a-form-item label="昵称"><a-input v-model.trim="userForm.nickname" /></a-form-item>
          <a-form-item label="手机号" field="phone"><a-input v-model.trim="userForm.phone" placeholder="可使用手机号单独创建" /></a-form-item>
          <a-form-item label="真实姓名"><a-input v-model.trim="userForm.realName" /></a-form-item>
          <a-form-item label="初始权限"><a-select v-model="userForm.permissions" :options="permissionOptions" multiple /></a-form-item>
        </a-grid>
        <a-form-item label="备注"><a-textarea v-model="userForm.remark" :auto-size="{ minRows: 3, maxRows: 5 }" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="permissionVisible"
      title="用户权限与设备"
      width="min(960px, calc(100vw - 32px))"
      align-center
      :mask-closable="false"
      :body-style="managerBodyStyle"
      unmount-on-close
    >
      <div v-if="managedUser" class="miniapp-manager__summary">
        <a-avatar :size="40" :image-url="managedUser.avatar">{{ (managedUser.nickname || managedUser.username || '?').slice(0, 1) }}</a-avatar>
        <div class="miniapp-manager__identity">
          <strong>{{ managedUser.nickname || managedUser.username || '未命名用户' }}</strong>
          <span>{{ managedUser.username || '-' }} · {{ managedUser.phone || '未填写手机号' }}</span>
        </div>
        <a-tag :color="managedUser.status === 2 ? 'red' : managedUser.status === 1 ? 'green' : 'orange'">{{ ['待绑定', '已绑定', '禁用'][managedUser.status || 0] }}</a-tag>
      </div>
      <a-tabs v-model:active-key="managerTab">
        <a-tab-pane key="permissions" title="权限">
          <div class="miniapp-manager__section-head">
            <div><strong>功能权限</strong><span>勾选该用户在小程序内可使用的能力</span></div>
            <a-tag>{{ editingPermissions.length }} 项</a-tag>
          </div>
          <a-spin :loading="permissionLoading" class="miniapp-manager__permission-spin">
            <a-checkbox-group v-model="editingPermissions" class="miniapp-manager__permissions">
              <a-checkbox v-for="option in permissionOptions" :key="option.value" :value="option.value">{{ option.label }}</a-checkbox>
            </a-checkbox-group>
          </a-spin>
        </a-tab-pane>
        <a-tab-pane key="devices" title="设备授权">
          <div class="miniapp-manager__device-toolbar">
            <div><strong>可访问设备</strong><span>已选择 {{ editingDeviceKeys.length }} 台</span></div>
            <a-input v-model.trim="deviceKeyword" allow-clear placeholder="搜索设备名称、DeviceId 或产品" class="miniapp-manager__search" @press-enter="searchDevices">
              <template #prefix><icon-search /></template>
            </a-input>
            <GiButton type="search" @click="searchDevices" />
          </div>
          <a-table
            row-key="iotId"
            :data="authorizedDevices"
            :columns="deviceColumns"
            :loading="deviceLoading"
            :pagination="devicePagination"
            :scroll="{ x: '100%', y: 320, minWidth: 760 }"
            :row-selection="{ type: 'checkbox', showCheckedAll: true, onlyCurrent: false }"
            :selected-keys="editingDeviceKeys"
            @select="handleDeviceSelect"
            @select-all="handleDeviceSelectAll"
            @page-change="handleDevicePageChange"
            @page-size-change="handleDevicePageSizeChange"
          />
        </a-tab-pane>
      </a-tabs>
      <template #footer>
        <a-space>
          <a-button @click="permissionVisible = false">关闭</a-button>
          <a-button
            type="primary"
            :loading="managerSaving"
            :disabled="managerTab === 'permissions' ? !permissionReady : !deviceSelectionReady"
            @click="saveManagedAccess"
          >
            <template #icon><icon-save /></template>{{ managerTab === 'permissions' ? '保存权限' : '保存设备授权' }}
          </a-button>
        </a-space>
      </template>
    </a-modal>

    <a-modal
      v-model:visible="detailVisible"
      title="小程序详情"
      width="min(880px, calc(100vw - 32px))"
      align-center
      :body-style="modalBodyStyle"
      unmount-on-close
    >
      <template v-if="currentApp">
        <div class="miniapp-detail__hero">
          <a-avatar :size="52" :image-url="currentApp.logo">{{ currentApp.name?.slice(0, 1) }}</a-avatar>
          <div class="miniapp-detail__identity">
            <div><strong>{{ currentApp.name }}</strong><a-tag :color="currentApp.status === 1 ? 'green' : 'gray'">{{ currentApp.status === 1 ? '启用' : '禁用' }}</a-tag></div>
            <a-typography-text copyable>{{ currentApp.appId }}</a-typography-text>
          </div>
          <div class="miniapp-detail__stats">
            <div><strong>{{ currentApp.userCount || 0 }}</strong><span>用户</span></div>
            <div><strong>{{ currentApp.deviceCount || 0 }}</strong><span>设备</span></div>
            <div><strong>{{ currentApp.onlineDeviceCount || 0 }}</strong><span>在线</span></div>
          </div>
        </div>
        <a-descriptions :column="{ xs: 1, sm: 2 }" bordered>
          <a-descriptions-item label="关联应用">{{ currentApp.appName || currentApp.linkedAppId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="主题色"><span class="miniapp-detail__swatch" :style="{ backgroundColor: currentApp.primaryColor || '#165DFF' }" />{{ currentApp.primaryColor || '#165DFF' }}</a-descriptions-item>
          <a-descriptions-item label="客服电话">{{ currentApp.contactPhone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="联系邮箱">{{ currentApp.contactEmail || '-' }}</a-descriptions-item>
          <a-descriptions-item label="访问链接" :span="2"><a-typography-paragraph v-if="currentApp.accessUrl" copyable>{{ currentApp.accessUrl }}</a-typography-paragraph><span v-else>-</span></a-descriptions-item>
          <a-descriptions-item label="描述" :span="2">{{ currentApp.description || '-' }}</a-descriptions-item>
          <a-descriptions-item label="欢迎语" :span="2">{{ currentApp.welcomeText || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div v-if="currentApp.qrcodeImage" class="miniapp-detail__qr"><span>访问二维码</span><a-image :src="currentApp.qrcodeImage" width="144" /></div>
      </template>
      <template #footer>
        <a-space>
          <a-button @click="detailVisible = false">关闭</a-button>
          <a-button v-if="hasPerm('application:miniapp:edit')" @click="manageAppUsers(currentApp)"><template #icon><IconUserGroup /></template>用户管理</a-button>
          <a-button v-if="hasPerm('application:miniapp:edit')" type="primary" @click="currentApp && appFormRef?.edit(currentApp.appId); detailVisible = false"><template #icon><IconEdit /></template>编辑</a-button>
        </a-space>
      </template>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import type { FormInstance, TableColumnData } from '@arco-design/web-vue'
import type {
  MiniAppItem,
  MiniDeviceItem,
  MiniUserItem
} from '@/apis/application/miniapp'
import {
  Avatar,
  Button,
  Message,
  Popconfirm,
  Space,
  Tag,
  Tooltip
} from '@arco-design/web-vue'
import {
  IconDelete,
  IconEdit,
  IconEye,
  IconPause,
  IconPlayArrow,
  IconRefresh,
  IconSettings,
  IconUserGroup
} from '@arco-design/web-vue/es/icon'
import {
  createMiniUser,
  deleteMiniApp,
  deleteMiniUser,
  disableMiniApp,
  disableMiniUser,
  enableMiniApp,
  enableMiniUser,
  getMiniApp,
  getMiniAppDevices,
  getMiniAppList,
  getMiniUserDevices,
  getMiniUserList,
  getMiniUserPermissions,
  refreshMiniApp,
  syncMiniAppSecrets,
  updateMiniUserDevices,
  updateMiniUserPermissions
} from '@/apis/application/miniapp'
import { hasPerm } from '@/utils/permission'
import MiniAppFormDrawer from './modules/MiniAppFormDrawer.vue'

defineOptions({ name: 'ApplicationMiniapp' })
type MiniDeviceRecord = MiniDeviceItem
const activeTab = ref('apps')
const appFormRef
  = useTemplateRef<InstanceType<typeof MiniAppFormDrawer>>('appFormRef')
const apps = ref<MiniAppItem[]>([])
const appLoading = ref(false)
const appQuery = reactive({
  name: '',
  status: undefined as number | undefined
})
const appPagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true })
const selectedAppId = ref('')
const miniAppOptionLoading = ref(false)
const miniAppOptions = ref<{ label: string, value: string }[]>([])
const users = ref<MiniUserItem[]>([])
const userLoading = ref(false)
const userQuery = reactive({
  phone: '',
  nickname: '',
  status: undefined as number | undefined
})
const userPagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true })
const userFormVisible = ref(false)
const userFormRef = useTemplateRef<FormInstance>('userFormRef')
const userForm = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  realName: '',
  remark: '',
  permissions: [] as string[]
})
const userFormRules = {
  username: [{ match: /^\w*$/, message: '用户名只允许字母、数字和下划线' }],
  password: [{ minLength: 6, maxLength: 32, message: '密码长度为 6-32 位' }],
  phone: [{ match: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号' }]
}
const permissionVisible = ref(false)
const managerTab = ref('permissions')
const managedUser = ref<MiniUserItem>()
const editingPermissions = ref<string[]>([])
const permissionLoading = ref(false)
const permissionReady = ref(false)
const authorizedDevices = ref<MiniDeviceRecord[]>([])
const editingDeviceKeys = ref<string[]>([])
const deviceLoading = ref(false)
const deviceSelectionReady = ref(false)
const deviceKeyword = ref('')
const devicePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: true,
  showPageSize: true
})
const managerSaving = ref(false)
const detailVisible = ref(false)
const currentApp = ref<MiniAppItem>()
const modalBodyStyle = {
  maxHeight: 'min(72vh, 720px)',
  overflowY: 'auto' as const
}
const managerBodyStyle = {
  maxHeight: 'min(74vh, 760px)',
  overflowY: 'auto' as const,
  paddingTop: '8px'
}
const appStatusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]
const userStatusOptions = [
  { label: '待绑定', value: 0 },
  { label: '已绑定', value: 1 },
  { label: '禁用', value: 2 }
]
const permissionOptions = [
  { label: '查看设备', value: 'DEVICE_VIEW' },
  { label: '查看所有设备（管理员）', value: 'DEVICE_VIEW_ALL' },
  { label: '控制设备', value: 'FUNCTION_CALL' },
  { label: '设备添加', value: 'DEVICE_ADD' },
  { label: '设备解绑', value: 'DEVICE_UNBIND' },
  { label: '用户管理', value: 'USER_MANAGE' }
]
function deviceRowKey(record: MiniDeviceRecord) {
  return record.iotId || record.id || ''
}
function handleDeviceSelect(keys: (string | number)[]) {
  const pageKeys = new Set(authorizedDevices.value.map(deviceRowKey))
  const selectedOnPage = keys.map(String).filter((key) => pageKeys.has(key))
  const selectedOutsidePage = editingDeviceKeys.value.filter((key) => !pageKeys.has(key))
  editingDeviceKeys.value = [...new Set([...selectedOutsidePage, ...selectedOnPage])]
}
function handleDeviceSelectAll(checked: boolean) {
  const visibleKeys = authorizedDevices.value.map(deviceRowKey)
  const selected = new Set(editingDeviceKeys.value)
  visibleKeys.forEach((key) => checked ? selected.add(key) : selected.delete(key))
  editingDeviceKeys.value = [...selected]
}
async function searchApps() {
  appLoading.value = true
  try {
    const res = await getMiniAppList({
      page: appPagination.current,
      size: appPagination.pageSize,
      ...appQuery
    })
    apps.value = res.data.records || []
    appPagination.total = res.data.total || 0
  } finally {
    appLoading.value = false
  }
}
function submitAppSearch() {
  appPagination.current = 1
  searchApps()
}
function resetApps() {
  Object.assign(appQuery, { name: '', status: undefined })
  appPagination.current = 1
  searchApps()
}
function handleAppPageChange(page: number) {
  appPagination.current = page
  searchApps()
}
function handleAppPageSizeChange(size: number) {
  appPagination.current = 1
  appPagination.pageSize = size
  searchApps()
}
async function appStatus(item: MiniAppItem, enabled: boolean) {
  await (enabled ? enableMiniApp(item.appId) : disableMiniApp(item.appId))
  await searchApps()
  return true
}
async function removeApp(item: MiniAppItem) {
  await deleteMiniApp(item.appId)
  await Promise.all([searchApps(), loadMiniAppOptions()])
  return true
}
async function refreshApp(item: MiniAppItem) {
  await refreshMiniApp(item.appId)
  Message.success('统计已刷新')
  await searchApps()
}
async function openAppDetail(item: MiniAppItem) {
  currentApp.value = (await getMiniApp(item.appId)).data
  detailVisible.value = true
}
function manageAppUsers(item?: MiniAppItem) {
  if (!item) return
  if (!miniAppOptions.value.some((option) => option.value === item.appId)) {
    miniAppOptions.value.unshift({ label: `${item.name} (${item.appId})`, value: item.appId })
  }
  selectedAppId.value = item.appId
  activeTab.value = 'users'
  detailVisible.value = false
  searchUsers()
}
async function loadMiniAppOptions(keyword = '') {
  miniAppOptionLoading.value = true
  try {
    const pageSize = 200
    const firstPage = await getMiniAppList({
      page: 1,
      size: pageSize,
      name: keyword || undefined
    })
    const records = [...(firstPage.data.records || [])]
    const pageCount = Math.ceil(Number(firstPage.data.total || records.length) / pageSize)
    for (let page = 2; page <= pageCount; page += 1) {
      const res = await getMiniAppList({ page, size: pageSize, name: keyword || undefined })
      records.push(...(res.data.records || []))
    }
    const options = records.map((item) => ({
      label: `${item.name} (${item.appId})`,
      value: item.appId
    }))
    const selected = miniAppOptions.value.find((item) => item.value === selectedAppId.value)
    if (selected && !options.some((item) => item.value === selected.value)) options.unshift(selected)
    miniAppOptions.value = options
  } finally {
    miniAppOptionLoading.value = false
  }
}
async function handleAppSaved() {
  await Promise.all([searchApps(), loadMiniAppOptions()])
}
async function syncSecrets() {
  await syncMiniAppSecrets()
  Message.success('密钥同步任务已提交')
  return true
}
async function searchUsers() {
  if (!selectedAppId.value) {
    users.value = []
    return
  }
  userLoading.value = true
  try {
    const res = await getMiniUserList({
      page: userPagination.current,
      size: userPagination.pageSize,
      appId: selectedAppId.value,
      ...userQuery
    })
    users.value = res.data.records || []
    userPagination.total = res.data.total || 0
  } finally {
    userLoading.value = false
  }
}
function submitUserSearch() {
  userPagination.current = 1
  searchUsers()
}
function changeUserApp() {
  userPagination.current = 1
  searchUsers()
}
function resetUsers() {
  Object.assign(userQuery, { phone: '', nickname: '', status: undefined })
  userPagination.current = 1
  searchUsers()
}
function handleUserPageChange(page: number) {
  userPagination.current = page
  searchUsers()
}
function handleUserPageSizeChange(size: number) {
  userPagination.current = 1
  userPagination.pageSize = size
  searchUsers()
}
function openUserForm() {
  Object.assign(userForm, {
    username: '',
    password: '',
    nickname: '',
    phone: '',
    realName: '',
    remark: '',
    permissions: ['DEVICE_VIEW']
  })
  userFormRef.value?.clearValidate()
  userFormVisible.value = true
}
async function saveUser() {
  if (await userFormRef.value?.validate()) return false
  if (!userForm.username && !userForm.phone) {
    Message.warning('用户名和手机号至少填写一个')
    return false
  }
  if (userForm.username && !userForm.password) {
    Message.warning('设置用户名时需要同时设置密码')
    return false
  }
  await createMiniUser({ ...userForm, appId: selectedAppId.value })
  userFormVisible.value = false
  searchUsers()
  return true
}
async function userStatus(item: MiniUserItem, enabled: boolean) {
  await (enabled
    ? enableMiniUser(String(item.id))
    : disableMiniUser(String(item.id)))
  searchUsers()
  return true
}
async function removeUser(item: MiniUserItem) {
  await deleteMiniUser(String(item.id))
  searchUsers()
  return true
}
async function manageUser(user: MiniUserItem) {
  if (!selectedAppId.value) return
  managedUser.value = user
  managerTab.value = 'permissions'
  deviceKeyword.value = ''
  devicePagination.current = 1
  permissionVisible.value = true
  editingPermissions.value = []
  editingDeviceKeys.value = []
  authorizedDevices.value = []
  permissionReady.value = false
  deviceSelectionReady.value = false
  permissionLoading.value = true
  const permissionRequest = getMiniUserPermissions(String(user.id), selectedAppId.value)
    .then((res) => {
      editingPermissions.value = res.data || []
      permissionReady.value = true
    })
    .finally(() => {
      permissionLoading.value = false
    })
  const selectedDeviceRequest = getMiniUserDevices(String(user.id), selectedAppId.value)
    .then((res) => {
      editingDeviceKeys.value = res.data || []
      deviceSelectionReady.value = true
    })
  await Promise.allSettled([
    permissionRequest,
    selectedDeviceRequest,
    loadAuthorizedDevices()
  ])
}
async function loadAuthorizedDevices() {
  if (!selectedAppId.value) return
  deviceLoading.value = true
  try {
    const res = await getMiniAppDevices(selectedAppId.value, {
      page: devicePagination.current,
      size: devicePagination.pageSize,
      keyword: deviceKeyword.value || undefined
    })
    authorizedDevices.value = res.data.records || []
    devicePagination.total = res.data.total || 0
  } finally {
    deviceLoading.value = false
  }
}
function searchDevices() {
  devicePagination.current = 1
  loadAuthorizedDevices()
}
function handleDevicePageChange(page: number) {
  devicePagination.current = page
  loadAuthorizedDevices()
}
function handleDevicePageSizeChange(size: number) {
  devicePagination.current = 1
  devicePagination.pageSize = size
  loadAuthorizedDevices()
}
async function savePermissions() {
  if (!managedUser.value) return
  await updateMiniUserPermissions(
    String(managedUser.value.id),
    selectedAppId.value,
    editingPermissions.value
  )
  Message.success('权限已保存')
}
async function saveDevices() {
  if (!managedUser.value) return
  await updateMiniUserDevices(
    String(managedUser.value.id),
    selectedAppId.value,
    editingDeviceKeys.value
  )
  Message.success('设备授权已保存')
}
async function saveManagedAccess() {
  managerSaving.value = true
  try {
    await (managerTab.value === 'permissions' ? savePermissions() : saveDevices())
  } finally {
    managerSaving.value = false
  }
}
const appColumns: TableColumnData[] = [
  {
    title: '小程序名称 / AppId',
    dataIndex: 'name',
    width: 280,
    fixed: 'left',
    render: ({ record }) => (
      <div class="miniapp__identity">
        <Avatar size={36} imageUrl={record.logo}>{record.name?.slice(0, 1)}</Avatar>
        <div class="miniapp__cell">
          <Button type="text" class="miniapp__name" onClick={() => openAppDetail(record as MiniAppItem)}><strong title={record.name}>{record.name || '未命名小程序'}</strong></Button>
          <code title={record.appId}>{record.appId}</code>
        </div>
      </div>
    )
  },
  { title: '关联应用', dataIndex: 'appName', width: 180, ellipsis: true, tooltip: true },
  { title: '用户数', dataIndex: 'userCount', width: 90, align: 'center' },
  {
    title: '设备 / 在线',
    dataIndex: 'deviceCount',
    width: 120,
    align: 'center',
    render: ({ record }) =>
      `${record.deviceCount || 0} / ${record.onlineDeviceCount || 0}`
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag color={record.status === 1 ? 'green' : 'gray'}>
        <span class="miniapp__status-dot" />
        {record.status === 1 ? '启用' : '禁用'}
      </Tag>
    )
  },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  {
    title: '操作',
    width: 232,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space size={2}>
        <Tooltip content="详情">
          <Button type="text" shape="circle" aria-label="详情" onClick={() => openAppDetail(record as MiniAppItem)}>
            <IconEye />
          </Button>
        </Tooltip>
        {hasPerm('application:miniapp:edit') && (
          <Tooltip content="用户管理">
            <Button type="text" shape="circle" aria-label="用户管理" onClick={() => manageAppUsers(record as MiniAppItem)}>
              <IconUserGroup />
            </Button>
          </Tooltip>
        )}
        {hasPerm('application:miniapp:edit') && (
          <Tooltip content="编辑">
            <Button
              type="text"
              shape="circle"
              aria-label="编辑"
              onClick={() => appFormRef.value?.edit(record.appId)}
            >
              <IconEdit />
            </Button>
          </Tooltip>
        )}
        {hasPerm('application:miniapp:edit') && (
          <Tooltip content="刷新统计">
            <Button
              type="text"
              shape="circle"
              aria-label="刷新统计"
              onClick={() => refreshApp(record as MiniAppItem)}
            >
              <IconRefresh />
            </Button>
          </Tooltip>
        )}
        {hasPerm('application:miniapp:edit') && (
          <Popconfirm
            content={`确定${record.status === 1 ? '禁用' : '启用'}该小程序吗？`}
            onBeforeOk={() =>
              appStatus(record as MiniAppItem, record.status !== 1)}
          >
            <Tooltip content={record.status === 1 ? '禁用' : '启用'}>
              <Button type="text" shape="circle" status={record.status === 1 ? 'warning' : 'success'} aria-label={record.status === 1 ? '禁用' : '启用'}>
                {record.status === 1 ? <IconPause /> : <IconPlayArrow />}
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
        {hasPerm('application:miniapp:del') && (
          <Popconfirm
            content="确定删除该小程序吗？"
            onBeforeOk={() => removeApp(record as MiniAppItem)}
          >
            <Tooltip content="删除">
              <Button type="text" status="danger" shape="circle" aria-label="删除">
                <IconDelete />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]
const userColumns: TableColumnData[] = [
  { title: '用户 / 用户名', dataIndex: 'username', width: 260, fixed: 'left', render: ({ record }) => (
    <div class="miniapp__identity">
      <Avatar size={36} imageUrl={record.avatar}>{(record.nickname || record.username || '?').slice(0, 1)}</Avatar>
      <div class="miniapp__cell">
        <strong title={record.nickname || record.username}>{record.nickname || record.username || '未命名用户'}</strong>
        <code title={record.username}>{record.username || '-'}</code>
      </div>
    </div>
  ) },
  { title: '手机号', dataIndex: 'phone', width: 140 },
  {
    title: '登录方式',
    dataIndex: 'openId',
    width: 110,
    render: ({ record }) => record.openId ? '微信' : record.username ? '账号密码' : '手机号'
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    align: 'center',
    render: ({ record }) => (
      <Tag
        color={
          record.status === 2 ? 'red' : record.status === 1 ? 'green' : 'orange'
        }
      >
        <span class="miniapp__status-dot" />
        {['待绑定', '已绑定', '禁用'][record.status || 0]}
      </Tag>
    )
  },
  { title: '最后登录', dataIndex: 'lastLoginTime', width: 180 },
  {
    title: '操作',
    width: 138,
    align: 'center',
    fixed: 'right',
    render: ({ record }) => (
      <Space size={2}>
        {hasPerm('application:miniapp:user:edit') && (
          <Tooltip content="权限和设备">
            <Button
              type="text"
              shape="circle"
              aria-label="权限和设备"
              onClick={() => manageUser(record as MiniUserItem)}
            >
              <IconSettings />
            </Button>
          </Tooltip>
        )}
        {hasPerm('application:miniapp:user:edit') && (
          <Popconfirm
            content={`确定${record.status === 2 ? '启用' : '禁用'}该用户吗？`}
            onBeforeOk={() =>
              userStatus(record as MiniUserItem, record.status === 2)}
          >
            <Tooltip content={record.status === 2 ? '启用' : '禁用'}>
              <Button type="text" shape="circle" status={record.status === 2 ? 'success' : 'warning'} aria-label={record.status === 2 ? '启用' : '禁用'}>
                {record.status === 2 ? <IconPlayArrow /> : <IconPause />}
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
        {hasPerm('application:miniapp:user:del') && (
          <Popconfirm
            content="确定删除该用户吗？"
            onBeforeOk={() => removeUser(record as MiniUserItem)}
          >
            <Tooltip content="删除">
              <Button type="text" status="danger" shape="circle" aria-label="删除">
                <IconDelete />
              </Button>
            </Tooltip>
          </Popconfirm>
        )}
      </Space>
    )
  }
]
const deviceColumns: TableColumnData[] = [
  { title: '设备名称', dataIndex: 'deviceName', width: 160, ellipsis: true, tooltip: true },
  { title: 'DeviceId', dataIndex: 'deviceId', width: 170, ellipsis: true, tooltip: true },
  { title: '产品', dataIndex: 'productName', width: 150, ellipsis: true, tooltip: true },
  { title: 'ProductKey', dataIndex: 'productKey', width: 150, ellipsis: true, tooltip: true },
  { title: '最后在线', dataIndex: 'lastOnlineTime', width: 170 },
  {
    title: '在线状态',
    width: 100,
    render: ({ record }) => (
      <Tag color={record.onlineStatus ? 'green' : 'gray'}>
        {record.onlineStatus ? '在线' : '离线'}
      </Tag>
    )
  }
]
onMounted(() => {
  searchApps()
  loadMiniAppOptions()
})
</script>

<style lang="scss" scoped>
.miniapp {
  &__tabs {
    flex-shrink: 0;
    margin-bottom: 12px;
  }

  &__search {
    width: 150px;
  }

  &__status {
    width: 130px;
  }

  &__app-filter {
    width: 220px;
  }
}

:deep(.miniapp__identity) {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;

  .arco-avatar {
    flex-shrink: 0;
  }
}

:deep(.miniapp__cell) {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong,
  code {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  code {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

:deep(.miniapp__name) {
  justify-content: flex-start;
  width: 100%;
  height: auto;
  padding: 0;

  .arco-btn-content {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

:deep(.miniapp__status-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  background: currentcolor;
  border-radius: 50%;
}

@media (width <= 768px) {
  .miniapp {
    &__search,
    &__status,
    &__app-filter {
      width: 100%;
    }
  }
}

.miniapp-manager {
  &__summary {
    display: flex;
    gap: 12px;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--color-border-1);
  }

  &__identity {
    flex: 1;
    min-width: 0;
  }

  &__identity strong,
  &__identity span,
  &__section-head strong,
  &__section-head span,
  &__device-toolbar strong,
  &__device-toolbar span {
    display: block;
  }

  &__identity span,
  &__section-head span,
  &__device-toolbar span {
    margin-top: 3px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__section-head,
  &__device-toolbar {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }

  &__permissions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px 24px;
    padding: 14px 0;
  }

  &__search {
    width: 300px;
  }
}

.miniapp-detail {
  &__hero {
    display: flex;
    gap: 14px;
    align-items: center;
    padding-bottom: 16px;
  }

  &__identity {
    flex: 1;
    min-width: 0;
  }

  &__identity > div {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-bottom: 4px;
  }

  &__identity strong {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 16px;
    white-space: nowrap;
  }

  &__stats {
    display: flex;
    gap: 22px;
  }

  &__stats > div {
    min-width: 42px;
    text-align: center;
  }

  &__stats strong,
  &__stats span {
    display: block;
  }

  &__stats strong {
    font-size: 18px;
  }

  &__stats span {
    margin-top: 2px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__swatch {
    display: inline-block;
    width: 14px;
    height: 14px;
    margin-right: 7px;
    vertical-align: -2px;
    border: 1px solid var(--color-border-2);
    border-radius: 3px;
  }

  &__qr {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
    margin-top: 16px;
  }

  &__qr > span {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

@media (width <= 640px) {
  .miniapp-manager {
    &__section-head,
    &__device-toolbar {
      flex-direction: column;
      gap: 10px;
      align-items: stretch;
    }

    &__search {
      width: 100%;
    }

    &__permissions {
      grid-template-columns: 1fr;
    }
  }

  .miniapp-detail {
    &__hero {
      flex-wrap: wrap;
      align-items: flex-start;
    }

    &__stats {
      justify-content: space-around;
      width: 100%;
      padding-top: 10px;
      border-top: 1px solid var(--color-border-1);
    }
  }
}
</style>
