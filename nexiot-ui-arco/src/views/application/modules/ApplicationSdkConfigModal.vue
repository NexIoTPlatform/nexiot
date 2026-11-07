<template>
  <a-modal
    v-model:visible="visible"
    title="SDK 配置示例"
    width="min(820px, calc(100vw - 32px))"
    :body-style="modalBodyStyle"
    :footer="false"
    align-center
    unmount-on-close
  >
    <a-alert type="info" class="sdk-config__tip">
      建议使用 client_credentials 授权模式。配置文件放在
      classpath:resources/config/nexiot.properties。
    </a-alert>
    <section class="sdk-config__section">
      <div class="sdk-config__header">
        <strong>Maven 依赖</strong>
        <a-button type="text" size="small" @click="copy(mavenConfig)">
          <template #icon><icon-copy /></template>复制
        </a-button>
      </div>
      <pre>{{ mavenConfig }}</pre>
    </section>
    <section class="sdk-config__section">
      <div class="sdk-config__header">
        <strong>Spring Boot 配置</strong>
        <a-button type="text" size="small" @click="copy(propertiesConfig)">
          <template #icon><icon-copy /></template>复制
        </a-button>
      </div>
      <pre>{{ propertiesConfig }}</pre>
    </section>
    <a-link href="https://docs.nexiot.cc/iot/http/java-sdk.html" target="_blank">
      打开 SDK 文档
    </a-link>
  </a-modal>
</template>

<script setup lang="ts">
import type { ApplicationItem } from '@/apis/application/application'
import { Message } from '@arco-design/web-vue'
import { copyText } from '@/utils'

defineOptions({ name: 'ApplicationSdkConfigModal' })
const visible = ref(false)
const application = ref<ApplicationItem>()
const revealSecret = ref(false)
const modalBodyStyle = { maxHeight: 'min(74vh, 700px)', overflowY: 'auto' as const }
const mavenConfig = `<dependency>
    <groupId>cc.nexiot</groupId>
    <artifactId>nexiot-openapi-sdk</artifactId>
    <version>1.0.0</version>
</dependency>`
const propertiesConfig = computed(() => `# 生产环境建议启用 HTTPS
iot.sdk.enable.https=false

# 服务器地址（不包含 http/https）
universal.iot.host=${window.location.host}

# OAuth2 客户端凭证
universal.iot.clientId=${application.value?.appId || ''}
universal.iot.clientSecret=${revealSecret.value ? application.value?.appSecret || '' : '请先在应用信息中显示密钥'}

universal.iot.grantType=client_credentials`)

function open(app: ApplicationItem, showSecret: boolean) {
  application.value = app
  revealSecret.value = showSecret
  visible.value = true
}

async function copy(value: string) {
  try {
    await copyText(value)
    Message.success('已复制到剪贴板')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '复制失败')
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.sdk-config {
  &__tip {
    margin-bottom: 16px;
  }

  &__section {
    margin-bottom: 18px;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
}

pre {
  max-height: 360px;
  padding: 14px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-1);
  background: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
}
</style>
