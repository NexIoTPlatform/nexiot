const TENANT_DOMAIN_SEPARATOR_RE = /[,;，；\n\r\t ]+/

export function splitTenantDomains(value?: string | string[]) {
  const source = Array.isArray(value) ? value.join(',') : value || ''
  return source.split(TENANT_DOMAIN_SEPARATOR_RE).map((item) => item.trim()).filter(Boolean)
}

export function normalizeTenantDomain(value: string) {
  const text = value.trim()
  if (!text) return ''
  try {
    const url = new URL(text.includes('://') ? text : `http://${text}`)
    return url.host
  } catch {
    return ''
  }
}

export function normalizeTenantDomains(values: string[]) {
  return [...new Set(values.map(normalizeTenantDomain).filter(Boolean))]
}
