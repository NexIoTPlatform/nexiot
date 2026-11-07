<template>
  <a-modal v-model:visible="visibleProxy" :title="command ? '编辑透传指令' : '添加透传指令'" width="min(760px, 100%)" :mask-closable="false" :ok-loading="saving" @before-ok="save" @close="reset">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, sm: 2 }" :col-gap="16">
        <a-form-item field="commandName" label="指令名称" :rules="[{ required: true, message: '请输入指令名称' }]"><a-input v-model.trim="form.commandName" placeholder="例如：唤醒上报、读取状态" /></a-form-item>
        <a-form-item field="slaveDeviceId" label="子设备 ID"><a-input v-model.trim="form.slaveDeviceId" placeholder="可选，透传目标子设备" /></a-form-item>
      </a-grid>
      <a-form-item label="启用指令"><a-switch v-model="form.enabled" /></a-form-item>
      <a-form-item field="commandHex" label="透传内容" :rules="[{ required: true, message: '请输入透传内容' }]"><a-textarea v-model="form.commandHex" :auto-size="{ minRows: 7, maxRows: 16 }" placeholder="例如 HEX、JSON 或普通字符串" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { GatewayPollingConfig } from '@/apis/iot/device'

defineOptions({ name: 'GatewayPollingCommandModal' })
const props = defineProps<{ visible: boolean, taskType: GatewayPollingConfig['taskType'], command?: Record<string, unknown> }>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', value: ReturnType<typeof createCommand>): void
}>()
const formRef = useTemplateRef<FormInstance>('formRef')
const saving = ref(false)
const visibleProxy = computed({ get: () => props.visible, set: (value: boolean) => emit('update:visible', value) })
const form = reactive(createCommand())
function createCommand(source?: Record<string, unknown>) {
  return {
    commandName: String(source?.commandName || ''),
    commandHex: String(source?.commandHex || ''),
    taskType: props.taskType,
    commandType: 'CUSTOM',
    slaveDeviceId: String(source?.slaveDeviceId || ''),
    propertyMapping: '[]',
    protocolParams: '{}',
    dataParserScript: '',
    enabled: source?.enabled !== false && source?.enabled !== 0 && source?.enabled !== '0',
    executionOrder: typeof source?.executionOrder === 'number' ? source.executionOrder : undefined
  }
}
function reset() {
  Object.assign(form, createCommand())
  saving.value = false
  formRef.value?.clearValidate()
}
async function save() {
  if (await formRef.value?.validate()) return false
  saving.value = true
  try {
    emit('save', { ...form })
    return true
  } finally {
    saving.value = false
  }
}
watch([() => props.visible, () => props.command], ([visible]) => {
  if (visible) Object.assign(form, createCommand(props.command))
}, { immediate: true })
</script>
