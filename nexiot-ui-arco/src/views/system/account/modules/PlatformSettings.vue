<template>
  <div class="platform-settings">
    <section class="platform-settings__form">
      <div class="platform-settings__section-title">
        <GiIconBox color="primary"><icon-settings /></GiIconBox>
        <div><h3>平台品牌</h3><span>当前租户</span></div>
      </div>

      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item field="siteName" label="平台名称" :rules="[{ required: true, message: '请输入平台名称' }]">
          <a-input v-model.trim="form.siteName" :max-length="50" show-word-limit />
        </a-form-item>

        <a-form-item label="平台 Logo">
          <div class="platform-settings__logo-field">
            <div class="platform-settings__logo-preview">
              <img v-if="logoPreview" :src="logoPreview" :alt="form.siteName" />
              <icon-image v-else />
            </div>
            <div class="platform-settings__logo-actions">
              <input ref="logoInputRef" type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" @change="handleLogoChange" />
              <a-space wrap>
                <a-button :loading="uploading" @click="logoInputRef?.click()"><template #icon><icon-upload /></template>上传 Logo</a-button>
                <a-button v-if="form.siteLogo" type="text" status="danger" @click="removeLogo"><template #icon><icon-delete /></template>移除</a-button>
              </a-space>
              <span>PNG、JPG、WebP 或 SVG，文件不超过 5 MB</span>
            </div>
          </div>
        </a-form-item>

        <a-divider />
        <div class="platform-settings__watermark-heading">
          <div><strong>页面水印</strong><span>应用于当前租户的工作区页面</span></div>
          <a-switch v-model="form.watermarkEnabled" />
        </div>
        <a-form-item v-if="form.watermarkEnabled" field="watermarkText" label="水印文字">
          <a-input v-model.trim="form.watermarkText" :max-length="50" show-word-limit :placeholder="form.siteName" />
        </a-form-item>

        <a-space>
          <a-button type="primary" :loading="saving" @click="save"><template #icon><icon-save /></template>保存设置</a-button>
          <a-button @click="resetForm"><template #icon><icon-refresh /></template>恢复</a-button>
        </a-space>
      </a-form>
    </section>

    <aside class="platform-settings__preview">
      <div class="platform-settings__section-title">
        <GiIconBox color="success"><icon-eye /></GiIconBox>
        <div><h3>效果预览</h3><span>导航与页面水印</span></div>
      </div>
      <div class="brand-preview">
        <header>
          <img v-if="logoPreview" :src="logoPreview" :alt="form.siteName" />
          <icon-image v-else />
          <strong>{{ form.siteName || '平台名称' }}</strong>
        </header>
        <a-watermark :content="form.watermarkEnabled ? form.watermarkText || form.siteName : ''" class="brand-preview__body">
          <div class="brand-preview__skeleton"><i /><i /><i /><i /></div>
        </a-watermark>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance } from '@arco-design/web-vue'
import type { TenantBranding } from '../api'
import { Message } from '@arco-design/web-vue'
import { uploadOssFile } from '@/apis/iot/oss'
import { useTenantBrandingStore } from '@/stores'
import { resolveApiResourceUrl } from '@/utils'
import { updateTenantBranding } from '../api'

defineOptions({ name: 'AccountPlatformSettings' })

const formRef = useTemplateRef<FormInstance>('formRef')
const logoInputRef = useTemplateRef<HTMLInputElement>('logoInputRef')
const brandingStore = useTenantBrandingStore()
const uploading = ref(false)
const saving = ref(false)
const form = reactive<TenantBranding>({
  siteName: '',
  siteLogo: '',
  siteIcon: '',
  watermarkEnabled: false,
  watermarkText: ''
})
const logoPreview = computed(() => resolveApiResourceUrl(form.siteLogo))

function resetForm() {
  Object.assign(form, brandingStore.branding)
  formRef.value?.clearValidate()
}

async function handleLogoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    Message.error('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    Message.error('Logo 文件不能超过 5 MB')
    return
  }
  uploading.value = true
  try {
    const url = (await uploadOssFile(file)).data.url
    form.siteLogo = url
    form.siteIcon = url
    Message.success('Logo 上传成功')
  } finally {
    uploading.value = false
  }
}

function removeLogo() {
  form.siteLogo = ''
  form.siteIcon = ''
}

async function save() {
  if (await formRef.value?.validate()) return
  saving.value = true
  try {
    const payload = {
      ...form,
      siteIcon: form.siteIcon || form.siteLogo,
      watermarkText: form.watermarkText?.trim() || form.siteName
    }
    //演示环境有叼毛老换名字，暂时注释
    Message.success('请你别改了，这里代码注释了')
    // await updateTenantBranding(payload)
    // await brandingStore.fetchBranding(true)
    // resetForm()
    // Message.success('平台设置已更新')
  } finally {
    saving.value = false
  }
}

watch(() => brandingStore.loaded, (loaded) => loaded && resetForm(), { immediate: true })
onMounted(async () => {
  if (!brandingStore.loaded) await brandingStore.fetchBranding()
  resetForm()
})
</script>

<style lang="scss" scoped>
.platform-settings {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 24px;
  max-width: 1060px;

  &__form,
  &__preview {
    min-width: 0;
  }

  &__section-title {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-bottom: 22px;

    h3,
    span {
      display: block;
      margin: 0;
    }

    h3 {
      margin-bottom: 3px;
      font-size: 16px;
    }

    span {
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__logo-field {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  &__logo-preview {
    display: grid;
    flex: 0 0 72px;
    place-items: center;
    width: 72px;
    height: 72px;
    overflow: hidden;
    font-size: 28px;
    color: var(--color-text-3);
    background: var(--color-fill-2);
    border: 1px solid var(--color-border-2);
    border-radius: 6px;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__logo-actions {
    min-width: 0;

    input {
      display: none;
    }

    > span {
      display: block;
      margin-top: 8px;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }

  &__watermark-heading {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    strong,
    span {
      display: block;
    }

    span {
      margin-top: 4px;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}

.brand-preview {
  overflow: hidden;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  header {
    display: flex;
    gap: 10px;
    align-items: center;
    height: 52px;
    padding: 0 14px;
    border-bottom: 1px solid var(--color-border-2);

    img,
    svg {
      width: 28px;
      height: 28px;
      object-fit: contain;
    }

    strong {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__body {
    height: 230px;
    padding: 18px;
    background: var(--color-fill-1);
  }

  &__skeleton {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;

    i {
      height: 82px;
      background: var(--color-bg-2);
      border: 1px solid var(--color-border-1);
      border-radius: 4px;
    }
  }
}

@media (width <= 900px) {
  .platform-settings {
    grid-template-columns: 1fr;
  }
}

@media (width <= 520px) {
  .platform-settings__logo-field {
    align-items: flex-start;
  }
}
</style>
