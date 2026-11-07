<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    width="min(1120px, calc(100vw - 40px))"
    :mask-closable="false"
    :body-style="{ padding: '0' }"
    unmount-on-close
    @close="reset"
  >
    <a-spin :loading="loading" class="protocol-form__spin">
      <a-form ref="formRef" :model="form" layout="vertical" class="protocol-form">
        <section class="protocol-form__info">
          <div class="protocol-form__section-title"><icon-settings /><div><strong>驱动信息</strong><span>{{ editing ? form.id : '关联产品与运行配置' }}</span></div></div>
          <a-form-item label="关联产品" field="id" :rules="[{ required: true, message: '请选择关联产品' }]">
            <a-select
              v-model="form.id"
              :options="productOptions"
              :loading="productLoading"
              allow-search
              :filter-option="false"
              :disabled="editing"
              placeholder="请选择或搜索产品"
              @search="handleProductSearch"
              @dropdown-reach-bottom="loadMoreProducts"
            />
          </a-form-item>
          <a-form-item label="驱动名称" field="name" :rules="[{ required: true, message: '请输入驱动名称' }]">
            <a-input v-model="form.name" :max-length="80" />
          </a-form-item>
          <a-grid :cols="2" :col-gap="12">
            <a-form-item label="版本号" field="version" :rules="[{ required: true, message: '请输入版本号' }]">
              <a-input v-model="form.version" placeholder="例如 1.0.0" />
            </a-form-item>
            <a-form-item label="发布状态">
              <a-switch v-model="published" checked-text="已发布" unchecked-text="未发布" />
            </a-form-item>
          </a-grid>
          <a-form-item label="驱动类型" field="type" :rules="[{ required: true, message: '请选择驱动类型' }]">
            <a-select v-model="form.type" :options="typeOptions" />
          </a-form-item>
          <a-form-item label="连接协议">
            <a-input v-model="form.connectionProtocol" placeholder="可选" />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea v-model="form.description" :max-length="300" show-word-limit :auto-size="{ minRows: 3, maxRows: 5 }" />
          </a-form-item>
<!--          <a-collapse :default-active-key="[]" class="protocol-form__advanced"> -->
<!--            <a-collapse-item key="config" header="高级配置 JSON"> -->
<!--              <a-form-item field="configuration" :rules="[{ validator: validateJson }]" hide-label> -->
<!--                <ProtocolCodeEditor v-model="form.configuration" compact placeholder="请输入配置 JSON" /> -->
<!--              </a-form-item> -->
<!--            </a-collapse-item> -->
<!--          </a-collapse> -->
        </section>

        <section class="protocol-form__source">
          <div class="protocol-form__source-header">
            <div class="protocol-form__section-title"><icon-code /><div><strong>插件源码</strong><span>{{ sourceHint }}</span></div></div>
            <a-tag color="arcoblue">{{ typeLabel }}</a-tag>
          </div>

          <a-form-item v-if="scriptDriver" field="jscript" :rules="[{ required: true, message: '请输入插件源码' }]" hide-label class="protocol-form__code-field">
            <ProtocolCodeEditor v-model="form.jscript" placeholder="请输入编解码脚本" />
          </a-form-item>

          <div v-else-if="form.type === 'spring'" class="protocol-form__source-fields">
            <a-result status="info" title="Spring Bean 驱动" subtitle="选择后端已注册的 Codec Bean，无需维护脚本源码。" />
            <a-form-item label="Codec Bean" field="beanName" :rules="[{ required: true, message: '请选择 Codec Bean' }]">
              <a-select v-model="form.beanName" :options="beanOptions" allow-search :loading="beanLoading" />
            </a-form-item>
          </div>

          <div v-else class="protocol-form__source-fields">
            <a-result status="info" title="JAR 插件驱动" subtitle="上传后自动识别可用驱动类，无需手动填写文件地址。" />
            <a-form-item label="JAR 文件" field="url" :rules="[{ required: true, message: '请上传 JAR 文件' }]">
              <div class="protocol-form__jar-upload">
                <a-input v-model="form.url" readonly placeholder="请选择 JAR 文件" />
                <a-upload :show-file-list="false" :custom-request="handleJarUpload" accept=".jar,application/java-archive" :disabled="uploading">
                  <template #upload-button>
                    <a-button type="primary" :loading="uploading"><template #icon><icon-upload /></template>{{ form.url ? '重新上传' : '选择文件' }}</a-button>
                  </template>
                </a-upload>
              </div>
            </a-form-item>
            <a-form-item label="驱动类" field="fileName" :rules="[{ required: true, message: '请选择或输入驱动类' }]">
              <a-select v-if="codecCandidates.length" v-model="form.fileName" :options="codecOptions" allow-search placeholder="请选择 JAR 中的驱动类" />
              <a-input v-else v-model="form.fileName" :disabled="uploading" placeholder="上传后自动识别，识别不到时可手动输入全限定类名" />
            </a-form-item>
          </div>
        </section>
      </a-form>
    </a-spin>
    <template #footer>
      <a-space><a-button @click="visible = false">取消</a-button><a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存</a-button></a-space>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { RequestOption } from '@arco-design/web-vue/es/upload/interfaces'
import type { ProtocolCodecCandidate } from '@/apis/iot/protocol'
import { Message } from '@arco-design/web-vue'
import { getProductByKey } from '@/apis/iot/product'
import { addProtocol, getAvailableCodecBeans, getProductsWithoutProtocol, getProtocol, updateProtocol, uploadProtocolJar } from '@/apis/iot/protocol'
import ProtocolCodeEditor from './ProtocolCodeEditor.vue'

defineOptions({ name: 'ProtocolFormDrawer' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const PRODUCT_PAGE_SIZE = 20

const formRef = useTemplateRef<FormInstance>('formRef')
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const productLoading = ref(false)
const beanLoading = ref(false)
const uploading = ref(false)
const productOptions = ref<{ label: string, value: string }[]>([])
const productPage = ref(0)
const productHasMore = ref(true)
const productSearchKey = ref('')
const beanOptions = ref<{ label: string, value: string, disabled?: boolean }[]>([])
const codecCandidates = ref<ProtocolCodecCandidate[]>([])
let productRequestId = 0
let productSearchTimer: ReturnType<typeof setTimeout> | undefined
const form = reactive(createForm())
const title = computed(() => editing.value ? '编辑产品驱动' : '新增产品驱动')
const published = computed({
  get: () => form.state === 1,
  set: (value) => form.state = value ? 1 : 0
})
const scriptDriver = computed(() => ['jscript', 'magic', 'magic-script'].includes(form.type))
const typeOptions = [
  { label: 'MagicScript', value: 'magic' },
  { label: 'JavaScript', value: 'jscript' },
  { label: 'Spring Bean', value: 'spring' },
  { label: 'JAR 插件', value: 'jar' }
]
const typeLabel = computed(() => typeOptions.find((item) => item.value === form.type)?.label || form.type)
const sourceHint = computed(() => scriptDriver.value ? '支持语法高亮、行号与代码缩进' : '当前驱动类型不使用内嵌源码')
const codecOptions = computed(() => codecCandidates.value.map((item) => ({
  label: item.simpleName ? `${item.simpleName} (${item.className})` : item.className,
  value: item.className
})))

function createForm() {
  return { id: '', name: '', description: '', state: 0, type: 'magic', configuration: '{}', expectedSourceHash: undefined as string | undefined, version: '1.0.0', fileName: '', url: '', jscript: '', beanName: '', connectionProtocol: '' }
}

function _validateJson(value: string, callback: (error?: string) => void) {
  try {
    JSON.parse(value || '{}')
    callback()
  } catch {
    callback('请输入有效的 JSON')
  }
}

function reset() {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productRequestId += 1
  Object.assign(form, createForm())
  editing.value = false
  productLoading.value = false
  productOptions.value = []
  productPage.value = 0
  productHasMore.value = true
  productSearchKey.value = ''
  codecCandidates.value = []
  formRef.value?.clearValidate()
}

function validateJar(file: File) {
  if (!file.name.toLowerCase().endsWith('.jar')) {
    Message.error('请选择 .jar 格式的驱动文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    Message.error('JAR 文件大小不能超过 10 MB')
    return false
  }
  return true
}

function handleJarUpload(option: RequestOption) {
  const file = option.fileItem.file
  if (!file || !validateJar(file)) {
    option.onError(new Error('JAR 文件校验失败'))
    return {}
  }
  uploading.value = true
  uploadProtocolJar(file)
    .then((res) => {
      const data = res.data
      form.url = data.url
      codecCandidates.value = Array.isArray(data.codecCandidates) ? data.codecCandidates : []
      if (codecCandidates.value.length === 1) form.fileName = codecCandidates.value[0].className
      else if (data.packageName) form.fileName = data.packageName
      else if (codecCandidates.value.length && !codecCandidates.value.some((item) => item.className === form.fileName)) form.fileName = ''
      option.onSuccess(data)
      Message.success(codecCandidates.value.length ? `上传成功，识别到 ${codecCandidates.value.length} 个驱动类` : 'JAR 上传成功')
    })
    .catch(option.onError)
    .finally(() => uploading.value = false)
  return {}
}

async function loadProducts(keyword = '', page = 1, append = false) {
  if (append && (productLoading.value || !productHasMore.value)) return
  if (!append) {
    productSearchKey.value = keyword
    productPage.value = 0
    productHasMore.value = true
    productRequestId += 1
  }
  const requestId = productRequestId
  productLoading.value = true
  try {
    const result = (await getProductsWithoutProtocol({
      searchKey: productSearchKey.value || undefined,
      pageNum: page,
      pageSize: PRODUCT_PAGE_SIZE
    })).data
    if (requestId !== productRequestId) return
    const rows = Array.isArray(result)
      ? result
      : (Array.isArray(result.records) ? result.records : [])
    const options = rows
      .map((item) => ({ label: item.name, value: String(item.productKey || item.id || '') }))
      .filter((item) => item.value)
    if (append) {
      const optionMap = new Map(productOptions.value.map((item) => [item.value, item]))
      options.forEach((item) => optionMap.set(item.value, item))
      productOptions.value = Array.from(optionMap.values())
    } else {
      productOptions.value = options
    }
    productPage.value = page
    const total = Array.isArray(result) ? result.length : Number(result.total || 0)
    productHasMore.value = productOptions.value.length < total
  } finally {
    if (requestId === productRequestId) productLoading.value = false
  }
}

function handleProductSearch(keyword: string) {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productSearchTimer = setTimeout(loadProducts, 300, keyword)
}

function loadMoreProducts() {
  loadProducts(productSearchKey.value, productPage.value + 1, true)
}

async function loadBeans() {
  beanLoading.value = true
  try {
    beanOptions.value = ((await getAvailableCodecBeans()).data || []).map((item) => ({ label: `${item.beanName} (${item.className})`, value: item.beanName, disabled: item.isRegistered }))
  } finally {
    beanLoading.value = false
  }
}

async function add() {
  reset()
  visible.value = true
  loading.value = true
  try {
    await loadProducts()
  } finally {
    loading.value = false
  }
}

async function edit(id: string) {
  reset()
  editing.value = true
  visible.value = true
  loading.value = true
  try {
    const data = (await getProtocol(id)).data
    Object.assign(form, data, { id: String(data.id), configuration: data.configuration || '{}', jscript: data.jscript || '' })
    const productKey = String(data.id)
    let productName = productKey
    try {
      const product = (await getProductByKey(productKey)).data
      productName = product?.name || productKey
    } catch {
      // 历史产品被删除时仍显示 ProductKey，保证协议可以继续维护
    }
    productOptions.value = [{ label: productName, value: productKey }]
    if (data.type === 'spring') await loadBeans()
  } finally {
    loading.value = false
  }
}

async function save() {
  if (await formRef.value?.validate()) return
  saving.value = true
  try {
    await (editing.value ? updateProtocol(form) : addProtocol(form))
    Message.success(editing.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('save-success')
  } finally {
    saving.value = false
  }
}

watch(() => form.type, (type) => {
  if (type === 'spring' && !beanOptions.value.length) loadBeans()
  if (type !== 'jar') codecCandidates.value = []
})

defineExpose({ add, edit })
</script>

<style lang="scss" scoped>
.protocol-form {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  height: min(680px, 72vh);

  &__spin {
    display: block;
  }

  &__info,
  &__source {
    box-sizing: border-box;
    min-width: 0;
    padding: 20px;
    overflow-y: auto;
  }

  &__info {
    background: var(--color-fill-1);
    border-right: 1px solid var(--color-border-2);
  }

  &__section-title {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-bottom: 18px;
    font-size: 18px;

    div,
    strong,
    span {
      display: block;
      min-width: 0;
    }

    strong {
      font-size: 15px;
    }

    span {
      margin-top: 3px;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 12px;
      font-weight: 400;
      color: var(--color-text-3);
      white-space: nowrap;
    }
  }

  &__source-header {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;

    .protocol-form__section-title {
      margin-bottom: 14px;
    }
  }

  &__code-field {
    margin-bottom: 0;
  }

  &__source-fields {
    max-width: 640px;
    margin: 20px auto 0;
  }

  &__jar-upload {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 8px;
  }

  &__advanced {
    margin-top: 4px;
    background: var(--color-bg-2);
  }
}

@media (width <= 820px) {
  .protocol-form {
    display: block;
    height: 72vh;
    overflow-y: auto;

    &__info,
    &__source {
      overflow: visible;
    }

    &__info {
      border-right: 0;
      border-bottom: 1px solid var(--color-border-2);
    }
  }
}
</style>
