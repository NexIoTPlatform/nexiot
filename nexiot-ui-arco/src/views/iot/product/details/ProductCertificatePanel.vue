<template>
  <section class="certificate-panel">
    <div class="certificate-panel__header"><div><h2>证书绑定</h2><p>为产品的 TLS 接入绑定服务端证书。</p></div><a-space><a-button v-if="boundCertificate" status="danger" @click="unbind"><template #icon><icon-link-unlink /></template>解绑</a-button><a-button type="primary" @click="openSelector"><template #icon><icon-link /></template>{{ boundCertificate ? '更换证书' : '绑定证书' }}</a-button></a-space></div>
    <a-empty v-if="!boundCertificate" description="当前产品未绑定证书" />
    <a-descriptions v-else :column="2" bordered>
      <a-descriptions-item label="证书名称">{{ boundCertificate.name }}</a-descriptions-item><a-descriptions-item label="证书标识">{{ boundCertificate.sslKey }}</a-descriptions-item>
      <a-descriptions-item label="证书主题">{{ certInfo.subject || '-' }}</a-descriptions-item><a-descriptions-item label="签发机构">{{ certInfo.issuer || '-' }}</a-descriptions-item>
      <a-descriptions-item label="有效期起始">{{ certInfo.notBefore || '-' }}</a-descriptions-item><a-descriptions-item label="有效期截止">{{ certInfo.notAfter || boundCertificate.expireTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="序列号">{{ certInfo.serialNumber || '-' }}</a-descriptions-item><a-descriptions-item label="签名算法">{{ certInfo.signatureAlgorithm || '-' }}</a-descriptions-item>
    </a-descriptions>
    <a-modal v-model:visible="visible" title="选择证书" width="720px" :mask-closable="false" @before-ok="bind">
      <a-table row-key="id" :data="certificates" :columns="columns" :loading="loading" :pagination="pagination" :row-selection="{ type: 'radio' }" :selected-keys="selectedId ? [selectedId] : []" @select="handleSelect" />
    </a-modal>
  </section>
</template>

<script setup lang="ts">
import type { TableColumnData } from '@arco-design/web-vue'
import type { CertificateItem } from '@/apis/iot/certificate'
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { bindProductCertificate, getCertificateList, unbindProductCertificate } from '@/apis/iot/certificate'

defineOptions({ name: 'ProductCertificatePanel' })

const props = defineProps<{ product: ProductItem }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()
const visible = ref(false)
const loading = ref(false)
const certificates = ref<CertificateItem[]>([])
const boundCertificate = ref<CertificateItem>()
const selectedId = ref('')
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const columns: TableColumnData[] = [{ title: '证书名称', dataIndex: 'name' }, { title: '证书标识', dataIndex: 'sslKey', width: 220 }, { title: '到期时间', dataIndex: 'expireTime', width: 180 }]
const certInfo = computed<Record<string, unknown>>(() => {
  const value = boundCertificate.value?.certInfo
  if (value && typeof value === 'object') return value
  try {
    return JSON.parse(value || '{}')
  } catch {
    return {}
  }
})
const sslKey = computed(() => {
  try {
    return String(JSON.parse(props.product.configuration || '{}').sslKey || '')
  } catch {
    return ''
  }
})

async function loadBound() {
  if (!sslKey.value) {
    boundCertificate.value = undefined
    return
  }
  const res = await getCertificateList({ page: 1, size: 10, sslKey: sslKey.value })
  boundCertificate.value = res.data.records?.[0]
}
async function loadList() {
  loading.value = true
  try {
    const res = await getCertificateList({ page: pagination.current, size: pagination.pageSize })
    certificates.value = (res.data.records || []).filter((item) => item.sslKey !== 'default-tcp')
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}
async function openSelector() {
  selectedId.value = boundCertificate.value?.id || ''
  visible.value = true
  await loadList()
}
function handleSelect(keys: (string | number)[]) {
  selectedId.value = String(keys[0] || '')
}
async function bind() {
  const cert = certificates.value.find((item) => item.id === selectedId.value)
  if (!cert) {
    Message.error('请选择证书')
    return false
  }
  await bindProductCertificate(props.product.productKey, cert.sslKey)
  Message.success('证书绑定成功')
  emit('refresh')
  await loadBound()
  return true
}
async function unbind() {
  await unbindProductCertificate(props.product.productKey)
  Message.success('证书已解绑')
  boundCertificate.value = undefined
  emit('refresh')
}

watch(sslKey, loadBound, { immediate: true })
</script>

<style lang="scss" scoped>
.certificate-panel {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &__header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;

    h2 {
      margin: 0;
      font-size: 18px;
      letter-spacing: 0;
    }

    p {
      margin: 5px 0 0;
      color: var(--color-text-3);
    }
  }
}
</style>
