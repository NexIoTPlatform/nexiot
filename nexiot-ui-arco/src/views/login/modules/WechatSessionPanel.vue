<template>
  <div class="wechat-session" :class="{ 'wechat-session--login': appearance === 'login' }">
    <a-spin :loading="loading || busy">
      <div v-if="session" class="wechat-session__content">
        <div class="wechat-session__qr" :class="{ 'wechat-session__qr--expired': expired }">
          <img v-if="qrSource && !imageFailed" :src="qrSource" :alt="accountName ? `${accountName}二维码` : '微信公众号二维码'" @error="imageFailed = true" />
          <span v-else class="wechat-session__hint">二维码暂不可用</span>
          <button v-if="expired" type="button" class="wechat-session__overlay" :aria-label="`${statusText}，点击刷新二维码`" :disabled="loading || busy" @click="refresh">
            <icon-refresh class="wechat-session__overlay-icon" />
            <span>{{ statusText }}</span>
            <span class="wechat-session__overlay-hint">点击刷新</span>
          </button>
        </div>
        <div class="wechat-session__verification" :class="{ 'wechat-session__verification--expired': expired }">
          <p v-if="appearance === 'login'" class="wechat-session__lead">微信扫一扫</p>
          <template v-if="session.verificationCode">
            <p class="wechat-session__hint">发送下方 6 位数字</p>
            <code class="wechat-session__code" :class="{ 'wechat-session__code--expired': expired }" :aria-label="expired ? '微信验证数字已失效' : '微信验证数字'">{{ session.verificationCode }}</code>
          </template>
          <p v-else class="wechat-session__hint">{{ purpose === 'bind' ? '扫码后确认绑定' : '扫码后登录' }}</p>
        </div>
        <div v-if="!expired && appearance !== 'login'" class="wechat-session__status" :class="{ 'wechat-session__status--scanned': status === 'SCANNED' }">
          <span role="status">{{ statusText }}</span>
          <span class="wechat-session__countdown">{{ remaining }} 秒</span>
        </div>
        <a-button v-if="purpose === 'bind' && status === 'SCANNED'" type="primary" :loading="busy" @click="emit('confirm')">确认绑定</a-button>
      </div>
      <div v-else class="wechat-session__placeholder"><span class="wechat-session__hint">{{ loading ? '加载中' : '点击刷新获取二维码' }}</span></div>
    </a-spin>
    <p v-if="message && expired && status !== 'EXPIRED' && !error" class="wechat-session__hint" role="status">{{ message }}</p>
    <p v-if="error" class="wechat-session__error" role="alert">{{ errorText }}</p>
    <div v-if="!session || !expired" class="wechat-session__footer" :class="{ 'wechat-session__footer--idle': !session }">
      <div v-if="appearance === 'login' && session && !expired" class="wechat-session__status" :class="{ 'wechat-session__status--scanned': status === 'SCANNED' }">
        <span role="status">{{ statusText }}</span>
        <span class="wechat-session__countdown">{{ remaining }} 秒</span>
      </div>
      <a-button class="wechat-session__refresh" type="text" size="small" :disabled="loading || busy" @click="refresh"><template #icon><icon-refresh /></template>{{ !session ? '重新获取' : '刷新二维码' }}</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { WechatAuthSession, WechatAuthStatus } from '../api'
import { resolveApiResourceUrl } from '@/utils'

defineOptions({ name: 'WechatSessionPanel' })

const props = withDefaults(defineProps<{
  session: WechatAuthSession | null
  status: WechatAuthStatus
  remaining: number
  accountName?: string
  purpose?: 'login' | 'bind'
  appearance?: 'default' | 'login'
  loading?: boolean
  busy?: boolean
  message?: string
  error?: string
}>(), { purpose: 'login', appearance: 'default', loading: false, busy: false, accountName: '', message: '', error: '' })
const emit = defineEmits<{
  (event: 'refresh'): void
  (event: 'confirm'): void
}>()

const imageFailed = ref(false)
const qrSource = computed(() => resolveApiResourceUrl(props.session?.qrUrl))
const errorText = computed(() => /[\u3400-\u9FFF]/u.test(props.error) ? props.error : '暂时无法连接，请重试')
const expired = computed(() => ['EXPIRED', 'CANCELLED', 'FAILED', 'UNBOUND', 'DENIED', 'CONSUMED'].includes(props.status))
const statusText = computed(() => ({
  WAITING: props.session?.verificationCode ? '等待验证' : '等待扫码',
  SCANNED: props.purpose === 'bind' ? '等待确认' : '已扫码',
  READY: '正在登录',
  BINDING: '正在绑定',
  BOUND: '绑定成功',
  EXPIRED: '二维码已过期',
  CANCELLED: '已取消',
  FAILED: '验证失败',
  UNBOUND: '微信未绑定',
  DENIED: '暂无登录权限',
  CONSUMED: '会话已结束'
})[props.status])
function refresh() {
  if (props.loading || props.busy) return
  emit('refresh')
}
watch([qrSource, () => props.session?.sessionId], () => {
  imageFailed.value = false
})
</script>

<style lang="scss" scoped>
.wechat-session {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  text-align: center;

  &__content {
    display: flex;
    flex-direction: column;
    gap: 14px;
    align-items: center;

    p {
      margin: 0;
      line-height: 1.6;
    }
  }

  &__qr {
    position: relative;
    box-sizing: border-box;
    display: grid;
    place-items: center;
    width: 200px;
    height: 200px;
    padding: 12px;
    background: #fff;
    border: 1px solid var(--color-border-2);
    border-radius: 10px;

    img {
      display: block;
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }

    .wechat-session__hint {
      color: #6b7280;
    }
  }

  &__qr--expired img {
    opacity: 0.32;
    filter: grayscale(1);
  }

  &__overlay {
    position: absolute;
    inset: 0;
    display: grid;
    gap: 6px;
    align-content: center;
    justify-items: center;
    padding: 12px;
    font-family: inherit;
    font-size: 12px;
    font-weight: 500;
    line-height: 20px;
    color: #4e5969;
    cursor: pointer;
    background: rgb(255 255 255 / 38%);
    border: 0;
    border-radius: inherit;

    &:hover {
      color: #272e3b;
      background: rgb(255 255 255 / 50%);
    }

    &:focus-visible {
      outline: 2px solid rgb(var(--primary-6));
      outline-offset: 4px;
    }

    &:disabled {
      cursor: default;
    }
  }

  &__overlay-icon {
    margin-bottom: 2px;
    font-size: 24px;
  }

  &__overlay-hint {
    font-weight: 400;
    color: #6b7280;
  }

  &__code {
    padding: 0;
    font-family: ui-monospace, monospace;
    font-size: 24px;
    font-weight: 600;
    line-height: 32px;
    color: var(--color-text-1);
    letter-spacing: 4px;
    user-select: all;
  }

  &__verification {
    display: flex;
    flex-direction: column;
    gap: 14px;
    align-items: center;
  }

  &__verification--expired {
    .wechat-session__lead,
    .wechat-session__hint {
      color: var(--color-text-4);
    }
  }

  &__code--expired {
    color: var(--color-text-4);
    text-decoration: line-through;
    text-decoration-thickness: 1px;
    user-select: none;
  }

  &__lead {
    font-size: 15px;
    font-weight: 500;
    color: var(--color-text-1);
  }

  &__hint {
    font-size: 12px;
    line-height: 1.6;
    color: var(--color-text-3);
  }

  &__placeholder {
    box-sizing: border-box;
    display: grid;
    place-items: center;
    width: 200px;
    min-height: 280px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-2);
    border-radius: 10px;
  }

  &__status {
    display: flex;
    gap: 10px;
    align-items: center;
    font-size: 12px;
    line-height: 20px;
    color: var(--color-text-3);
  }

  &__status--scanned {
    color: rgb(var(--success-6));
  }

  &__countdown {
    padding-left: 10px;
    font-variant-numeric: tabular-nums;
    color: var(--color-text-3);
    border-left: 1px solid var(--color-border-2);
  }

  &__error {
    max-width: 100%;
    margin: 0;
    font-size: 12px;
    line-height: 1.6;
    color: rgb(var(--warning-6));
    overflow-wrap: anywhere;
  }

  &__refresh {
    font-size: 12px;
    color: var(--color-text-3);

    &:hover {
      color: rgb(var(--primary-6));
    }
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &--login {
    gap: 18px;
    align-items: stretch;

    :deep(.arco-spin) {
      width: 100%;
    }

    .wechat-session__content {
      display: grid;
      grid-template-columns: 164px minmax(0, 1fr);
      gap: 16px;
      align-items: center;
    }

    .wechat-session__qr {
      width: 164px;
      height: 164px;
      padding: 0;
      border: 0;
      border-radius: 0;
    }

    .wechat-session__verification {
      gap: 8px;
      align-items: flex-start;
      text-align: left;
    }

    .wechat-session__lead {
      margin-bottom: 2px;
      font-size: 14px;
    }

    .wechat-session__code {
      font-size: 21px;
      font-weight: 500;
      line-height: 32px;
      letter-spacing: 1px;
    }

    .wechat-session__placeholder {
      width: 100%;
      min-height: 164px;
      background: transparent;
      border: 0;
      border-radius: 0;
    }

    .wechat-session__footer {
      justify-content: space-between;
      padding-top: 12px;
      border-top: 1px solid var(--color-border-1);
    }

    .wechat-session__footer--idle {
      justify-content: center;
    }

    .wechat-session__refresh {
      padding-right: 0;

      &:hover {
        color: var(--color-text-1);
        background: transparent;
      }
    }
  }
}

@media (width <= 480px) {
  .wechat-session--login {
    .wechat-session__content {
      grid-template-columns: minmax(0, 1fr);
      gap: 14px;
      justify-items: center;
    }

    .wechat-session__verification {
      gap: 6px;
      align-items: center;
      text-align: center;
    }

    .wechat-session__qr {
      width: 176px;
      height: 176px;
    }

    .wechat-session__lead {
      display: none;
    }

    .wechat-session__placeholder {
      min-height: 240px;
    }
  }
}
</style>
