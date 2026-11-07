<template>
  <a-modal v-model:visible="visible" :title="title" width="calc(100% - 20px)" :mask-closable="false"
    :modal-style="{ maxWidth: '520px' }" @before-ok="save" @close="close">
    <a-spin :loading="loading" class="w-full">
      <a-alert v-if="isWechatReply" class="g-mb" type="info">
        {{ isKeywordReply
          ? '关键词与用户消息或事件键完整匹配，忽略首尾空格和英文大小写；数值越小的排序越优先。回复内容可直接换行、填写 emoji。'
          : 'default 是未匹配时的默认回复，login_success 是登录成功回复。回复内容可直接换行、填写 emoji；留空或禁用时使用程序内置文案。' }}
      </a-alert>
      <GiForm ref="GiFormRef" :model-value="form" :columns="formColumns" :grid-item-props="{ span: 24 }"
        @update:model-value="Object.assign(form, $event)">
      </GiForm>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormColumnItem } from '@/components/index'
import { Message } from '@arco-design/web-vue'
import { addDictData, getDictDataDetail, updateDictData } from '@/apis/system/dict'
import { GiForm } from '@/components/index'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'DictDataFormModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const GiFormRef = useTemplateRef<InstanceType<typeof GiForm>>('GiFormRef')
const dictDataId = ref('')
const dictTypeCode = ref('')
const isKeywordReply = computed(() => dictTypeCode.value === 'wechat_keyword_reply')
const isWechatReply = computed(() => isKeywordReply.value || dictTypeCode.value === 'wechat_reply')
const isEdit = computed(() => !!dictDataId.value)
const title = computed(() => {
  const t = isEdit.value ? '编辑-字典数据' : '新增-字典数据'
  return dictTypeCode.value ? `${t} (${dictTypeCode.value})` : t
})
const visible = ref(false)
const loading = ref(false)

const [form, resetForm] = useResetReactive({
  name: '',
  value: '',
  sort: 0,
  status: '1' as Status,
  color: '',
  remark: ''
})

const COLOR_OPTIONS = [
  { label: '默认', value: '' },
  { label: 'red', value: 'red' },
  { label: 'orangered', value: 'orangered' },
  { label: 'orange', value: 'orange' },
  { label: 'gold', value: 'gold' },
  { label: 'lime', value: 'lime' },
  { label: 'green', value: 'green' },
  { label: 'cyan', value: 'cyan' },
  { label: 'blue', value: 'blue' },
  { label: 'arcoblue', value: 'arcoblue' },
  { label: 'purple', value: 'purple' },
  { label: 'pinkpurple', value: 'pinkpurple' },
  { label: 'magenta', value: 'magenta' },
  { label: 'gray', value: 'gray' }
]

const formColumns = computed<FormColumnItem[]>(() => [
  {
    type: 'input',
    label: isKeywordReply.value ? '关键词' : '数据标签',
    field: 'name',
    required: true,
    rules: [{ required: true, message: '请输入数据标签' }],
    props: {
      maxLength: isWechatReply.value ? 100 : 10,
      placeholder: isKeywordReply.value ? '例如：帮助、登录、绑定；一项配置一个关键词' : '请输入数据标签'
    }
  },
  {
    type: 'input',
    label: '数据键值',
    field: 'value',
    required: true,
    rules: [
      { match: /^\w*$/, message: '格式不对！只能包含英文数字下划线' }
    ],
    props: { maxLength: isWechatReply.value ? 100 : 10 }
  },
  {
    type: 'input-number',
    label: '排序',
    field: 'sort',
    props: { min: 0, style: { width: '120px' } },
    span: 12
  },
  {
    type: 'switch',
    label: '状态',
    field: 'status',
    required: true,
    props: {
      type: 'round',
      checkedValue: '1',
      uncheckedValue: '0',
      checkedText: '正常',
      uncheckedText: '禁用'
    },
    span: 12
  },
  {
    type: 'select',
    label: '颜色',
    field: 'color',
    props: {
      allowCreate: true,
      allowClear: true,
      placeholder: '请选择（支持hex）',
      options: COLOR_OPTIONS
    },
    span: 24
  },
  {
    type: 'textarea',
    label: isWechatReply.value ? '回复内容' : '备注',
    field: 'remark',
    props: {
      maxLength: isWechatReply.value ? 500 : 100,
      placeholder: isWechatReply.value ? '直接填写回复正文，支持换行和 emoji' : '请输入备注'
    }
  }
])

const add = (code: string) => {
  dictTypeCode.value = code
  dictDataId.value = ''
  visible.value = true
}

const edit = async (data: { id: string, code: string }) => {
  dictTypeCode.value = data.code
  visible.value = true
  dictDataId.value = data.id
  loading.value = true
  const res = await getDictDataDetail({ id: data.id })
  Object.assign(form, res.data)
  form.value = res.data.value != null ? String(res.data.value) : ''
  loading.value = false
}

const close = () => {
  GiFormRef.value?.formRef?.resetFields()
  resetForm()
  dictTypeCode.value = ''
}

const save = async () => {
  try {
    const valid = await GiFormRef.value?.formRef?.validate()
    if (valid) return false
    const data = { ...form, id: dictDataId.value, code: dictTypeCode.value }
    await (isEdit.value ? updateDictData(data) : addDictData(data))
    Message.success(isEdit.value ? '修改成功' : '新增成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

defineExpose({ add, edit })
</script>
