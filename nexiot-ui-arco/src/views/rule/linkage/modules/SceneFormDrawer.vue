<template>
  <a-drawer v-model:visible="visible" :title="form.id ? '编辑场景联动' : '新增场景联动'" width="min(980px, calc(100vw - 24px))" :mask-closable="false" :footer="false" unmount-on-close>
    <a-form :model="form" layout="vertical">
      <a-grid :cols="{ xs: 1, sm: 3 }" :col-gap="16">
        <a-form-item label="场景名称" required><a-input v-model.trim="form.sceneName" :max-length="100" show-word-limit /></a-form-item>
        <a-form-item label="沉默周期（秒）" required><a-input-number v-model="form.sleepCycle" :min="0" :max="86400" style="width: 100%" /></a-form-item>
        <a-form-item label="状态"><a-switch v-model="enabled" checked-text="启用" unchecked-text="停用" /></a-form-item>
      </a-grid>
      <a-divider orientation="left">触发条件</a-divider>
      <a-form-item label="条件关系">
        <a-radio-group v-model="form.touch" type="button" @change="changeTouch">
          <a-radio value="one">满足任意一个</a-radio><a-radio value="all">满足全部</a-radio>
        </a-radio-group>
        <template #extra>“满足全部”要求同一设备、同一消息类型的一次上报满足全部条件；未上报的属性不视为满足。</template>
      </a-form-item>
      <div class="scene-form__list">
        <article v-for="(item, index) in triggers" :key="index" class="scene-form__item">
          <header><strong>触发器 {{ index + 1 }}</strong><a-tooltip content="删除触发器"><a-button type="text" status="danger" shape="circle" :disabled="triggers.length === 1" @click="triggers.splice(index, 1)"><template #icon><icon-delete /></template></a-button></a-tooltip></header>
          <SceneTriggerEditor v-model="triggers[index]" :device-only="form.touch === 'all'" />
        </article>
      </div>
      <a-button type="text" @click="triggers.push({ filters: [] })"><template #icon><icon-plus /></template>新增触发器</a-button>

      <a-divider orientation="left">执行动作</a-divider>
      <div class="scene-form__list">
        <article v-for="(item, index) in actions" :key="index" class="scene-form__item">
          <header><strong>动作 {{ index + 1 }}</strong><a-tooltip content="删除动作"><a-button type="text" status="danger" shape="circle" :disabled="actions.length === 1" @click="actions.splice(index, 1)"><template #icon><icon-delete /></template></a-button></a-tooltip></header>
          <SceneActionEditor v-model="actions[index]" />
        </article>
      </div>
      <a-button type="text" @click="actions.push({ filters: [] })"><template #icon><icon-plus /></template>新增动作</a-button>
    </a-form>
    <div class="scene-form__footer"><a-button @click="visible = false">取消</a-button><a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存场景</a-button></div>
  </a-drawer>
</template>

<script setup lang="ts">
/* eslint-disable style/max-statements-per-line */
import type { SceneAction, SceneLinkage, SceneTrigger } from '../api'
import { Message } from '@arco-design/web-vue'
import { addSceneLinkage, getSceneLinkage, updateSceneLinkage } from '../api'
import { isValidAction, isValidTrigger, parseList } from '../shared'
import SceneActionEditor from './SceneActionEditor.vue'
import SceneTriggerEditor from './SceneTriggerEditor.vue'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const visible = ref(false)
const saving = ref(false)
const triggers = ref<SceneTrigger[]>([{ filters: [] }])
const actions = ref<SceneAction[]>([{ filters: [] }])
const form = reactive<SceneLinkage>(createDefault())
function createDefault(): SceneLinkage { return { sceneName: '', touch: 'one', triggerCondition: [], execAction: [], sleepCycle: 0, status: 0 } }
const enabled = computed({ get: () => form.status === 0, set: (value) => { form.status = value ? 0 : 1 } })
function changeTouch() {
  if (form.touch !== 'all') return
  const deviceTriggers = triggers.value.filter((item) => !item.trigger || item.trigger === 'device')
  if (deviceTriggers.length !== triggers.value.length) Message.warning('“满足全部”只保留设备触发器')
  triggers.value = deviceTriggers.length ? deviceTriggers : [{ trigger: 'device', filters: [] }]
}
async function save() {
  if (!form.sceneName.trim()) { Message.warning('请输入场景名称'); return }
  if (!Number.isInteger(form.sleepCycle) || form.sleepCycle < 0) { Message.warning('沉默周期必须是 0 或正整数'); return }
  const invalidTrigger = triggers.value.findIndex((item) => !isValidTrigger(item))
  if (invalidTrigger >= 0) { Message.warning(`请完整配置触发器 ${invalidTrigger + 1}`); return }
  if (form.touch === 'all') {
    const first = triggers.value[0]
    if (!first || triggers.value.some((item) => item.trigger !== 'device' || item.deviceId !== first.deviceId || item.productKey !== first.productKey || item.type !== first.type)) {
      Message.warning('满足全部仅支持同一设备、同一消息类型；多个设备请使用满足任意一个')
      return
    }
  }
  const invalidAction = actions.value.findIndex((item) => !isValidAction(item))
  if (invalidAction >= 0) { Message.warning(`请完整配置动作 ${invalidAction + 1}`); return }
  const payload: SceneLinkage = { ...form, triggerCondition: triggers.value, execAction: actions.value }
  saving.value = true
  try {
    if (payload.id) await updateSceneLinkage(payload)
    else await addSceneLinkage(payload)
    Message.success('场景联动保存成功')
    visible.value = false
    emit('save-success')
  } finally { saving.value = false }
}
async function open(record?: SceneLinkage) {
  Object.assign(form, createDefault(), { id: undefined })
  triggers.value = [{ filters: [] }]
  actions.value = [{ filters: [] }]
  visible.value = true
  if (!record?.id) return
  const detail = (await getSceneLinkage(record.id)).data
  Object.assign(form, detail)
  triggers.value = parseList<SceneTrigger>(detail.triggerCondition)
  actions.value = parseList<SceneAction>(detail.execAction)
  if (!triggers.value.length) triggers.value = [{ filters: [] }]
  if (!actions.value.length) actions.value = [{ filters: [] }]
}
defineExpose({ open })
</script>

<style scoped lang="scss">
/* stylelint-disable declaration-block-single-line-max-declarations */
.scene-form__list { display: grid; gap: 12px; }
.scene-form__item { padding: 12px; background: var(--color-fill-1); border: 1px solid var(--color-border-2); }
.scene-form__item header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.scene-form__footer { position: sticky; bottom: 0; display: flex; gap: 8px; justify-content: flex-end; padding: 14px 20px; margin: 24px -20px -20px; background: var(--color-bg-2); border-top: 1px solid var(--color-border-2); }
</style>
