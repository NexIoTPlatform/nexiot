import type { TenantBranding } from '@/views/system/account/api'
import { defineStore } from 'pinia'
import defaultLogo from '@/assets/images/logo.gif'
import { resolveApiResourceUrl } from '@/utils'
import { getTenantBranding } from '@/views/system/account/api'

const DEFAULT_SITE_NAME = 'NexIoT'

function resolveBrandingResourceUrl(path?: string) {
  if (!path) return ''
  return path === defaultLogo ? defaultLogo : resolveApiResourceUrl(path)
}

function updateDocumentBranding(branding: TenantBranding) {
  document.title = branding.siteName || DEFAULT_SITE_NAME
  const iconUrl = resolveBrandingResourceUrl(branding.siteIcon || branding.siteLogo)
  if (!iconUrl) return
  let favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (!favicon) {
    favicon = document.createElement('link')
    favicon.rel = 'icon'
    document.head.append(favicon)
  }
  favicon.href = iconUrl
}

export const useTenantBrandingStore = defineStore('tenant-branding', () => {
  const branding = reactive<TenantBranding>({
    siteName: DEFAULT_SITE_NAME,
    siteLogo: defaultLogo,
    siteIcon: '',
    watermarkEnabled: false,
    watermarkText: ''
  })
  const loading = ref(false)
  const loaded = ref(false)
  const logoUrl = computed(() => resolveBrandingResourceUrl(branding.siteLogo) || defaultLogo)
  const watermarkContent = computed(() => branding.watermarkEnabled
    ? branding.watermarkText?.trim() || branding.siteName
    : '')

  function reset() {
    Object.assign(branding, {
      siteName: DEFAULT_SITE_NAME,
      siteLogo: defaultLogo,
      siteIcon: '',
      watermarkEnabled: false,
      watermarkText: ''
    })
    loaded.value = false
    updateDocumentBranding(branding)
  }

  async function fetchBranding(force = false) {
    if (loading.value || (loaded.value && !force)) return
    loading.value = true
    try {
      const response = await getTenantBranding()
      Object.assign(branding, response.data, {
        siteName: response.data.siteName || DEFAULT_SITE_NAME,
        watermarkEnabled: Boolean(response.data.watermarkEnabled)
      })
      loaded.value = true
      updateDocumentBranding(branding)
    } finally {
      loading.value = false
    }
  }

  return { branding, loading, loaded, logoUrl, watermarkContent, fetchBranding, reset }
})
