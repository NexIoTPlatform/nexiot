/// <reference types="vite/client" />

/** 声明环境变量的类型 */
interface ImportMetaEnv {
  readonly VITE_API_PREFIX: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_BASE: string
  readonly VITE_LOGIN_PLATFORM_NAME?: string
  readonly VITE_LOGIN_GRANT: 'password' | 'captcha'
  readonly VITE_SIGN_SECRET: string
  readonly VITE_WS_URL?: string
  readonly VITE_AMAP_KEY?: string
  readonly VITE_AMAP_SECURITY_CODE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
