<template>
  <div class="overview-panel">
    <section class="product-profile">
      <div class="product-profile__primary">
        <div class="product-photo">
          <img v-if="imageUrl" :src="imageUrl" :alt="product.name" />
          <icon-cube v-else />
          <input ref="imageInputRef" class="product-photo__input" type="file" accept="image/png,image/jpeg" @change="handleImageChange" />
        </div>
        <div class="product-summary">
          <div class="product-summary__name"><h2>{{ product.name }}</h2><a-tag :color="stateInfo.color">{{ stateInfo.label }}</a-tag></div>
          <a-space wrap><a-tag color="arcoblue">{{ product.thirdPlatform || '未配置接入方式' }}</a-tag><a-tag>{{ deviceNodeLabel }}</a-tag><a-tag v-if="product.transportProtocol">{{ product.transportProtocol }}</a-tag></a-space>
          <div class="product-key">
            <span>ProductKey</span><code>{{ product.productKey }}</code>
            <a-tooltip content="复制"><a-typography-text copyable :copy-text="product.productKey" /></a-tooltip>
            <a-tooltip :content="deviceCount ? '产品下存在设备，不能修改 ProductKey' : '修改 ProductKey'">
              <a-button v-hasPerm="['system:product:edit']" type="text" shape="circle" :disabled="deviceCount > 0" aria-label="修改 ProductKey" @click="openRename"><icon-edit /></a-button>
            </a-tooltip>
          </div>
          <p>{{ product.describe || '暂无产品描述' }}</p>
        </div>
        <a-space class="product-actions" direction="vertical">
          <a-button v-hasPerm="['system:product:edit']" type="primary" @click="emit('edit')"><template #icon><icon-edit /></template>编辑产品</a-button>
          <a-button v-hasPerm="['system:product:edit']" :loading="imageUploading" @click="imageInputRef?.click()"><template #icon><icon-upload /></template>{{ imageUrl ? '更换图片' : '上传图片' }}</a-button>
          <a-button v-if="imageUrl" type="text" @click="imagePreviewVisible = true"><template #icon><icon-eye /></template>查看图片</a-button>
        </a-space>
      </div>

      <div class="product-profile__section-title"><icon-info-circle /><span>基本信息</span></div>
      <div class="detail-grid">
        <div><span>产品 ID</span><strong>{{ product.productId || product.id }}</strong></div>
        <div><span>厂商简称</span><strong>{{ product.companyNo || '-' }}</strong></div>
        <div><span>所属分类</span><strong>{{ product.classifiedName || '-' }}</strong></div>
        <div><span>创建人</span><strong>{{ product.creatorId || '-' }}</strong></div>
        <div><span>设备实例</span><strong>{{ deviceCount }} 个</strong></div>
        <div><span>日志存储</span><strong>{{ product.storePolicy || '默认策略' }}</strong></div>
        <div><span>创建时间</span><strong>{{ product.createTime || '-' }}</strong></div>
        <div><span>更新时间</span><strong>{{ product.updateTime || '-' }}</strong></div>
      </div>

      <div class="product-profile__tags">
        <div class="product-profile__section-title">
          <icon-tags /><span>设备标签</span>
          <a-button v-hasPerm="['system:product:edit']" type="text" size="small" @click="toggleTags"><template #icon><icon-edit /></template>{{ tagEditing ? '取消' : '编辑' }}</a-button>
        </div>
        <template v-if="tagEditing">
          <div class="tag-editor">
            <a-select v-model="tagForm" multiple allow-clear allow-search :options="tagOptions" placeholder="请选择设备标签" />
            <a-button type="primary" :loading="tagSaving" @click="saveTags"><template #icon><icon-save /></template>保存标签</a-button>
          </div>
        </template>
        <div v-else class="tag-list"><a-tag v-for="tag in displayTags" :key="tag.value" color="arcoblue">{{ tag.label }}</a-tag><a-empty v-if="!displayTags.length" description="暂无标签" /></div>
      </div>
    </section>

    <ProductGatewayPanel v-if="product.deviceNode === 'GATEWAY_SUB_DEVICE'" :product="product" @refresh="emit('refresh')" />

    <a-modal v-model:visible="renameVisible" title="修改 ProductKey" :mask-closable="false" :ok-loading="renameLoading" @before-ok="saveProductKey">
      <a-alert>仅产品下没有设备时允许修改。新的 ProductKey 必须唯一，且长度不超过 20 个字符。</a-alert>
      <a-input v-model="productKeyForm" class="rename-input" :max-length="20" show-word-limit placeholder="请输入新的 ProductKey" />
    </a-modal>
    <a-modal v-model:visible="imagePreviewVisible" title="产品图片" :footer="false" width="560px">
      <img class="image-preview" :src="imageUrl" :alt="product.name" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { ProductItem } from '@/apis/iot/product'
import { Message } from '@arco-design/web-vue'
import { getProductConfigDicts, renameProductKey, updateProduct, uploadProductImage } from '@/apis/iot/product'
import ProductGatewayPanel from './ProductGatewayPanel.vue'

defineOptions({ name: 'ProductOverviewPanel' })

const props = defineProps<{ product: ProductItem, deviceCount: number }>()
const emit = defineEmits<{ (e: 'edit'): void, (e: 'refresh'): void }>()
const imageInputRef = useTemplateRef<HTMLInputElement>('imageInputRef')
const imageUploading = ref(false)
const imagePreviewVisible = ref(false)
const renameVisible = ref(false)
const renameLoading = ref(false)
const productKeyForm = ref('')
const tagEditing = ref(false)
const tagSaving = ref(false)
const tagForm = ref<string[]>([])
const tagOptions = ref<{ label: string, value: string }[]>([])
const stateInfo = computed(() => [{ label: '已发布', color: 'green' }, { label: '审核中', color: 'orange' }, { label: '已停用', color: 'gray' }][props.product.state] || { label: '未知', color: 'gray' })
const tags = computed<string[]>(() => (Array.isArray(props.product.tags) ? props.product.tags : String(props.product.tags || '').split(',')).filter(Boolean))
const displayTags = computed(() => tags.value.map((value) => ({ value, label: tagOptions.value.find((item) => item.value === value)?.label || value })))
const deviceNodeLabel = computed(() => ({ DEVICE: '直连设备', GATEWAY: '网关设备', GATEWAY_SUB_DEVICE: '网关子设备' }[props.product.deviceNode || ''] || props.product.deviceNode || '-'))
const imageUrl = computed(() => {
  if (!props.product.photoUrl) return ''
  if (/^https?:\/\//.test(props.product.photoUrl)) return props.product.photoUrl
  try {
    const data = JSON.parse(props.product.photoUrl)
    return data.img || data.url || ''
  } catch {
    return ''
  }
})

function openRename() {
  productKeyForm.value = props.product.productKey
  renameVisible.value = true
}

async function saveProductKey() {
  const productKey = productKeyForm.value.trim()
  if (!productKey) {
    Message.error('请输入 ProductKey')
    return false
  }
  if (!/^[\w-]+$/.test(productKey)) {
    Message.error('ProductKey 仅支持字母、数字、下划线和短横线')
    return false
  }
  if (productKey === props.product.productKey) {
    Message.warning('ProductKey 未发生变化')
    return false
  }
  renameLoading.value = true
  try {
    await renameProductKey({ id: props.product.id, productKey })
    Message.success('ProductKey 修改成功')
    emit('refresh')
    return true
  } finally {
    renameLoading.value = false
  }
}

function toggleTags() {
  tagEditing.value = !tagEditing.value
  tagForm.value = [...tags.value]
}

async function saveTags() {
  tagSaving.value = true
  try {
    await updateProduct({ id: props.product.id, tags: tagForm.value.join(',') })
    Message.success('设备标签保存成功')
    tagEditing.value = false
    emit('refresh')
  } finally {
    tagSaving.value = false
  }
}

async function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    Message.error('仅支持 PNG 或 JPG 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    Message.error('图片大小不能超过 5MB')
    return
  }
  imageUploading.value = true
  try {
    await uploadProductImage(props.product.id, file)
    Message.success('产品图片上传成功')
    emit('refresh')
  } finally {
    imageUploading.value = false
  }
}

async function loadTagOptions() {
  const res = await getProductConfigDicts(['product_tags'])
  tagOptions.value = (res.data.product_tags || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
}

loadTagOptions()
</script>

<style lang="scss" scoped>
.overview-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-profile {
  padding: 16px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
}

.product-profile__primary {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
}

.product-profile__section-title {
  display: flex;
  gap: 7px;
  align-items: center;
  margin-top: 18px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
}

.product-profile__tags {
  padding-top: 14px;
  margin-top: 16px;
  border-top: 1px solid var(--color-border-2);
}

.product-profile__tags .product-profile__section-title {
  margin: 0 0 10px;
}

.product-profile__tags .product-profile__section-title .arco-btn {
  margin-left: auto;
}

.product-photo {
  display: grid;
  place-items: center;
  width: 104px;
  height: 104px;
  overflow: hidden;
  font-size: 40px;
  color: rgb(var(--primary-6));
  background: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
}

.product-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-photo__input {
  display: none;
}

.product-summary__name {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.product-summary h2 {
  margin: 0;
  font-size: 22px;
  letter-spacing: 0;
}

.product-summary p {
  margin: 12px 0 0;
  line-height: 1.7;
  color: var(--color-text-2);
}

.product-key {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 12px;
}

.product-key span {
  font-size: 12px;
  color: var(--color-text-3);
}

.product-key code {
  padding: 3px 8px;
  background: var(--color-fill-2);
  border-radius: 3px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px 24px;
  padding: 14px 16px;
  background: var(--color-fill-1);
  border-radius: 4px;
}

.detail-grid span {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  color: var(--color-text-3);
}

.detail-grid strong {
  font-weight: 500;
  overflow-wrap: anywhere;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 24px;
}

.tag-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.rename-input {
  margin-top: 14px;
}

.image-preview {
  display: block;
  max-width: 100%;
  max-height: 65vh;
  margin: 0 auto;
  object-fit: contain;
}

@media (width <= 900px) {
  .product-profile__primary {
    grid-template-columns: 80px minmax(0, 1fr);
  }

  .product-actions {
    grid-column: 1 / -1;
    align-items: flex-start;
  }

  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-photo {
    width: 80px;
    height: 80px;
  }
}

@media (width <= 600px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .tag-editor {
    grid-template-columns: 1fr;
  }
}
</style>
