<template>
  <section class="security-settings">
    <div class="security-settings__heading">
      <h2>安全设置</h2>
      <p>管理登录密码并检查当前账户的基础安全状态。</p>
    </div>
    <a-alert type="success" show-icon>平台已启用 8-30 位强密码策略，密码必须同时包含字母、数字和特殊字符。</a-alert>
    <a-list :bordered="false" class="security-settings__list">
      <a-list-item>
        <a-list-item-meta title="账户密码" description="建议定期更新密码，且不要与其他系统共用。">
          <template #avatar><GiIconBox color="warning"><icon-lock /></GiIconBox></template>
        </a-list-item-meta>
        <template #actions><a-button type="primary" @click="emit('password')"><template #icon><icon-edit /></template>修改密码</a-button></template>
      </a-list-item>
      <a-list-item>
        <a-list-item-meta title="最近登录" :description="lastLogin">
          <template #avatar><GiIconBox color="primary"><icon-history /></GiIconBox></template>
        </a-list-item-meta>
        <template #actions><a-tag color="green">正常</a-tag></template>
      </a-list-item>
      <a-list-item>
        <a-list-item-meta title="账户状态" :description="accountDescription">
          <template #avatar><GiIconBox color="success"><icon-safe /></GiIconBox></template>
        </a-list-item-meta>
        <template #actions><a-badge status="success" text="已启用" /></template>
      </a-list-item>
    </a-list>
    <WechatAccountBinding />
  </section>
</template>

<script setup lang="ts">
import type { AccountProfile } from '../api'
import { formatAccountDate } from '../api'
import WechatAccountBinding from './WechatAccountBinding.vue'

defineOptions({ name: 'AccountSecuritySettings' })

const props = defineProps<{ profile: AccountProfile }>()
const emit = defineEmits<{ (event: 'password'): void }>()
const lastLogin = computed(() => {
  const parts = [formatAccountDate(props.profile.user.loginDate), props.profile.user.loginIp].filter(Boolean)
  return parts.length ? parts.join(' · ') : '暂无最近登录记录'
})
const accountDescription = computed(() => props.profile.user.tenantRole === 'TENANT_ADMIN'
  ? '当前账户为租户管理员，请妥善保管凭据。'
  : '当前账户已通过平台身份认证。')
</script>

<style lang="scss" scoped>
.security-settings {
  max-width: 880px;

  &__heading {
    margin-bottom: 20px;

    h2,
    p {
      margin: 0;
    }

    h2 {
      margin-bottom: 6px;
      font-size: 18px;
    }

    p {
      color: var(--color-text-3);
    }
  }

  &__list {
    margin-top: 16px;
  }

  :deep(.arco-list-item) {
    padding: 20px 0;
  }
}
</style>
