export const amapConfig = {
  key: import.meta.env.VITE_AMAP_KEY || '',
  securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || ''
}

export function applyAMapSecurityConfig() {
  if (!amapConfig.securityJsCode) return
  const targetWindow = window as typeof window & {
    _AMapSecurityConfig?: { securityJsCode: string }
  }
  targetWindow._AMapSecurityConfig = {
    securityJsCode: amapConfig.securityJsCode
  }
}
