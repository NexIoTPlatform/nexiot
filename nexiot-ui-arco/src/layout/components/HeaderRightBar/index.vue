<!--
  @file HeaderRightBar 组件
  @description 头部右侧工具栏组件，包含项目配置、消息通知、全屏切换、主题切换和用户菜单等功能
-->
<template>
  <a-row justify="end" align="center" class="header-right-bar">
    <a-space size="medium">
      <!-- 项目配置按钮 -->
      <a-tooltip content="项目配置" position="bl">
        <a-button size="mini" class="g-hover-btn" @click="handleOpenSettings">
          <template #icon>
            <icon-settings :size="18" />
          </template>
        </a-button>
      </a-tooltip>

      <!-- 消息通知下拉框 -->
      <a-popover v-model:popup-visible="noticeVisible" position="br" trigger="click">
        <a-badge :count="websocketStore.unreadCount" :max-count="99">
          <a-button size="mini" class="g-hover-btn" :aria-label="`消息中心，${websocketStore.unreadCount} 条未读`">
            <template #icon>
              <icon-notification :size="18" />
            </template>
          </a-button>
        </a-badge>
        <template #content>
          <Notice :visible="noticeVisible" @navigate="noticeVisible = false"></Notice>
        </template>
      </a-popover>

      <!-- 全屏切换按钮 -->
      <a-tooltip v-if="!['xs', 'sm'].includes(breakpoint)" content="全屏切换" position="bottom">
        <a-button size="mini" class="g-hover-btn" @click="toggle">
          <template #icon>
            <icon-fullscreen v-if="!isFullscreen" :size="18" />
            <icon-fullscreen-exit v-else :size="18" />
          </template>
        </a-button>
      </a-tooltip>

      <!-- 暗黑模式切换 -->
      <a-tooltip content="主题切换" position="bottom">
        <GiThemeBtn></GiThemeBtn>
      </a-tooltip>

      <!-- 当前账户 -->
      <a-dropdown trigger="click" position="br">
        <a-row align="center" :wrap="false" class="header-right-bar__user">
          <a-avatar :size="32" :image-url="userStore.avatar">
            {{ userInitial }}
          </a-avatar>
          <span class="header-right-bar__username">{{ userStore.name }}</span>
          <icon-down />
        </a-row>

        <template #content>
          <a-doption v-for="item in USER_MENUS" :key="item.key" @click="item.onClick">
            <template #icon>
              <GiIconBox :color="item.iconColor">
                <component :is="item.icon" />
              </GiIconBox>
            </template>
            <span>{{ item.label }}</span>
          </a-doption>
          <a-divider :margin="0" />
          <a-doption @click="handleLogout">
            <template #icon>
              <GiIconBox color="warning">
                <icon-export />
              </GiIconBox>
            </template>
            <span>退出登录</span>
          </a-doption>
        </template>
      </a-dropdown>
    </a-space>
  </a-row>
  <PasswordModal ref="passwordModalRef" />
</template>

<script setup lang="ts">
import { Button, Drawer, Message, Modal } from '@arco-design/web-vue'
import { useFullscreen } from '@vueuse/core'
import { useBreakpoint, useTheme } from '@/hooks'
import { useAppStore, useUserStore, useWebSocketPushStore } from '@/stores'
import PasswordModal from '@/views/system/account/modules/PasswordModal.vue'
import Notice from './Notice.vue'
import SettingDrawerPanel from './SettingDrawerPanel.vue'

/** 组件名称 */
defineOptions({ name: 'HeaderRight' })

/** 路由实例 */
const router = useRouter()

/** 状态管理 */
const userStore = useUserStore()
const appStore = useAppStore()
const websocketStore = useWebSocketPushStore()
const noticeVisible = ref(false)
const passwordModalRef = ref<{ open: () => void } | null>(null)
const userInitial = computed(() => (userStore.name || '?').slice(0, 1).toUpperCase())
const { setThemeColor } = useTheme()

/** 响应式断点 */
const { breakpoint } = useBreakpoint()

/** 全屏控制 */
const { isFullscreen, toggle } = useFullscreen()

/** 用户菜单配置 */
const USER_MENUS = [
  {
    key: 'user',
    label: '个人中心',
    icon: 'icon-user',
    iconColor: 'primary',
    onClick: () => router.push({ path: '/system/account', query: { tab: 'profile' } })
  },
  {
    key: 'password',
    label: '修改密码',
    icon: 'icon-unlock',
    iconColor: 'warning',
    onClick: () => passwordModalRef.value?.open()
  },
  {
    key: 'push',
    label: '推送设置',
    icon: 'icon-notification',
    iconColor: 'primary',
    onClick: () => router.push({ path: '/system/account', query: { tab: 'push' } })
  },
  {
    key: 'preferences',
    label: '偏好设置',
    icon: 'icon-settings',
    iconColor: 'success',
    onClick: handleOpenSettings
  }
]

/** 恢复默认项目配置 */
const handleResetSettings = () => {
  appStore.resetSettings()
  setThemeColor(appStore.themeColor)
  Message.success('已恢复默认配置')
}

/** 打开设置抽屉 */
function handleOpenSettings() {
  Drawer.open({
    title: '项目配置',
    width: 300,
    content: () => h(SettingDrawerPanel),
    footer: () => h(Button, { type: 'primary', long: true, style: { margin: 0 }, onClick: handleResetSettings }, { default: () => '恢复默认配置' })
  })
}

/** 处理退出登录 */
const handleLogout = () => {
  Modal.warning({
    title: '提示',
    content: '确认退出登录？',
    hideCancel: false,
    closable: true,
    onBeforeOk: async () => {
      try {
        await userStore.logout()
        websocketStore.reset()
        router.replace('/login')
        return true
      } catch {
        Message.error('退出登录失败')
        return false
      }
    }
  })
}

onMounted(() => websocketStore.initialize())
</script>

<style lang="scss" scoped>
.arco-dropdown-open .arco-icon-down {
  transform: rotate(180deg);
}

.header-right-bar {
  &__user {
    color: var(--color-text-1);
    cursor: pointer;
  }

  &__username {
    margin-left: 10px;
    white-space: nowrap;

    .arco-icon-down {
      margin-left: 2px;
      transition: all 0.3s;
    }
  }
}

@media (width <= 640px) {
  .header-right-bar {
    &__username,
    &__user>.arco-icon-down {
      display: none;
    }
  }
}
</style>
