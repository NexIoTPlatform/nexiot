<template>
  <aside class="account-summary">
    <div class="account-summary__identity">
      <a-avatar :size="84" :image-url="avatarUrl" class="account-summary__avatar" @click="emit('avatar')">
        {{ initials }}
        <template #trigger-icon><icon-camera /></template>
      </a-avatar>
      <strong>{{ profile.user.alias || profile.user.username }}</strong>
      <span>@{{ profile.user.username }}</span>
      <a-space wrap :size="6">
        <a-tag v-for="role in roles" :key="role" color="arcoblue">{{ role }}</a-tag>
      </a-space>
    </div>

    <a-divider />

    <div class="account-summary__details">
      <div><GiIconBox color="primary"><icon-safe /></GiIconBox><span>账户类型</span><strong>{{ accountType }}</strong></div>
      <div><GiIconBox color="success"><icon-storage /></GiIconBox><span>所属租户</span><strong>{{ tenantName }}</strong></div>
      <div><GiIconBox color="warning"><icon-location /></GiIconBox><span>最近登录</span><strong>{{ profile.user.loginIp || '暂无 IP' }}</strong></div>
    </div>

    <a-divider />

    <section class="account-summary__completion">
      <div>
        <strong>资料完整度</strong>
        <span>{{ profileCompletion }}% 已完成，补全资料有助于账户识别和通知触达</span>
      </div>
      <VCharts :option="option" :theme="theme" autoresize />
    </section>

    <a-button long @click="emit('profile')"><template #icon><icon-edit /></template>编辑个人资料</a-button>
  </aside>
</template>

<script setup lang="ts">
import type { AccountProfile } from '../api'
import VCharts from 'vue-echarts'
import { useChart } from '@/hooks'
import { useUserStore } from '@/stores'
import { resolveApiResourceUrl } from '@/utils'
import { normalizeRoleGroup } from '../api'

defineOptions({ name: 'AccountSummary' })

const props = defineProps<{ profile: AccountProfile }>()
const emit = defineEmits<{ avatar: [], profile: [] }>()
const userStore = useUserStore()
const avatarUrl = computed(() => resolveApiResourceUrl(props.profile.user.avatar) || userStore.avatar)
const initials = computed(() => (props.profile.user.alias || props.profile.user.username || '?').slice(0, 1).toUpperCase())
const roles = computed(() => normalizeRoleGroup(props.profile.roleGroup).slice(0, 3))
const tenantName = computed(() => userStore.userInfo.tenantName || props.profile.user.tenantId || '平台租户')
const accountType = computed(() => {
  if (props.profile.user.identity === 0) return '平台管理员'
  if (props.profile.user.tenantRole === 'TENANT_ADMIN') return '租户管理员'
  return '租户成员'
})
const profileFields = computed(() => [
  { name: '昵称', completed: Boolean(props.profile.user.alias) },
  { name: '手机', completed: Boolean(props.profile.user.mobile) },
  { name: '邮箱', completed: Boolean(props.profile.user.email) },
  { name: '头像', completed: Boolean(props.profile.user.avatar) }
])
const profileCompletion = computed(() => {
  return Math.round(profileFields.value.filter((item) => item.completed).length / profileFields.value.length * 100)
})
const { option, theme } = useChart((isDark) => ({
  backgroundColor: 'transparent',
  animationDuration: 350,
  grid: { left: 36, right: 44, top: 4, bottom: 4 },
  xAxis: { type: 'value', min: 0, max: 100, show: false },
  yAxis: {
    type: 'category',
    data: profileFields.value.map((item) => item.name),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: isDark ? '#c9cdd4' : '#4e5969', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    barWidth: 8,
    showBackground: true,
    backgroundStyle: { color: isDark ? '#3b3f46' : '#e5e6eb' },
    silent: true,
    label: {
      show: true,
      position: 'right',
      color: isDark ? '#c9cdd4' : '#4e5969',
      fontSize: 10,
      formatter: ({ value }) => Number(value) === 100 ? '完成' : '待补'
    },
    data: profileFields.value.map((item) => ({ value: item.completed ? 100 : 0, itemStyle: { color: item.completed ? '#00b42a' : '#f53f3f' } }))
  }]
}))
</script>

<style lang="scss" scoped>
.account-summary {
  box-sizing: border-box;
  height: 100%;
  padding: 8px 4px;
  overflow-y: auto;

  &__identity {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: center;
    text-align: center;

    >strong {
      font-size: 18px;
    }

    >span {
      color: var(--color-text-3);
    }
  }

  &__avatar {
    font-size: 28px;
    color: var(--color-white);
    cursor: pointer;
    background: rgb(var(--primary-6));
  }

  &__details {
    display: flex;
    flex-direction: column;
    gap: 16px;

    >div {
      display: grid;
      grid-template-columns: 30px 72px minmax(0, 1fr);
      gap: 8px;
      align-items: center;
    }

    span {
      color: var(--color-text-3);
    }

    strong {
      overflow: hidden;
      text-overflow: ellipsis;
      font-weight: 500;
      text-align: right;
      white-space: nowrap;
    }
  }

  &__completion {
    margin-bottom: 20px;

    >div:first-child {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    span {
      font-size: 12px;
      line-height: 20px;
      color: var(--color-text-3);
    }

    :deep(.echarts) {
      width: 100%;
      height: 120px;
    }
  }
}
</style>
