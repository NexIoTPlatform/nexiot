<template>
  <a-modal :title="selectReceiver ? '扫码获取 OpenID' : '微信扫码绑定'" :visible="true" :footer="false" @cancel="close">
    <a-alert>使用微信扫一扫完成绑定。服务号需配置网页授权域名；接收模板通知前请关注该服务号。</a-alert>
    <p>公众号渠道：{{ channel.name }}</p>
    <a-form-item v-if="!selectReceiver" label="绑定标识（可选）"><a-input v-model.trim="userKey" placeholder="留空自动生成，也可填写 employee001" :disabled="!!sessionId" /></a-form-item>
    <p v-else>扫码成功后，OpenID 会自动添加到接收人输入框。</p>
    <a-button type="primary" :loading="loading" @click="create">{{ sessionId ? '重新生成二维码' : '生成绑定二维码' }}</a-button>
    <div v-if="qr" style="text-align: center; margin: 16px 0"><img :src="qr" alt="微信授权绑定二维码" width="220"><p>二维码 5 分钟有效</p></div>
    <a-alert v-if="error" type="error" style="margin-top: 12px">{{ error }}</a-alert>
    <a-alert v-if="result?.status === 'BOUND'" type="success">绑定成功</a-alert>
    <div v-if="result?.status === 'BOUND'">
      <a-form-item label="OpenID"><a-input :model-value="result.openid" readonly /></a-form-item>
      <a-form-item label="通知接收人"><a-input :model-value="`user:${result.userKey}`" readonly /></a-form-item>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import type { NoticeChannel, WechatBinding } from '@/apis/application/notice'
import QRCode from 'qrcode'
import { createWechatBinding, getWechatBinding } from '@/apis/application/notice'

const props = defineProps<{ channel: Pick<NoticeChannel, 'id' | 'name'>, selectReceiver?: boolean }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'bound', value: WechatBinding): void
}>()
const userKey = ref('')
const sessionId = ref('')
const qr = ref('')
const loading = ref(false)
const error = ref('')
const result = ref<WechatBinding>()
let timer: ReturnType<typeof setTimeout> | undefined
let deadline = 0
let closed = false
let generation = 0
function close() {
  closed = true
  generation++
  clearTimeout(timer)
  emit('close')
}
onMounted(() => { if (props.selectReceiver) create() })
onBeforeUnmount(() => {
  closed = true
  generation++
  clearTimeout(timer)
})
async function create() {
  const currentGeneration = ++generation
  qr.value = ''
  clearTimeout(timer)
  error.value = ''
  result.value = undefined
  loading.value = true
  try {
    const response = await createWechatBinding({ channelId: String(props.channel.id), userKey: userKey.value })
    if (closed || currentGeneration !== generation) return
    sessionId.value = response.data.sessionId
    qr.value = await QRCode.toDataURL(response.data.url, { width: 220, margin: 1 })
    deadline = Date.now() + response.data.expiresIn * 1000
    if (!closed && currentGeneration === generation) timer = setTimeout(poll, 2000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '生成二维码失败'
  } finally {
    loading.value = false
  }
}
async function poll() {
  const currentGeneration = generation
  if (closed) return
  if (Date.now() >= deadline) {
    error.value = '二维码已过期，请重新生成'
    return
  }
  try {
    const response = await getWechatBinding(sessionId.value)
    if (closed || currentGeneration !== generation) return
    result.value = response.data
    if (result.value.status === 'WAITING') timer = setTimeout(poll, 2000)
    else if (result.value.status === 'BOUND' && result.value.openid) emit('bound', result.value)
    else if (result.value.status === 'FAILED') error.value = result.value.message
  } catch (e) {
    error.value = e instanceof Error ? e.message : '查询绑定结果失败'
  }
}
</script>
