import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { Message, Notification } from '@arco-design/web-vue'
import axios from 'axios'
import CryptoJS from 'crypto-js'
import NProgress from 'nprogress'
import qs from 'qs'
import router from '@/router'
import { clearAuthTokens, getRefreshToken, getToken, setAuthTokens } from '@/utils/auth'
import 'nprogress/nprogress.css'

// 配置 NProgress
NProgress.configure({ showSpinner: false })

/** 状态码消息映射 */
const StatusCodeMessage: Record<number, string> = {
  200: '服务器成功返回请求的数据',
  201: '新建或修改数据成功',
  202: '一个请求已经进入后台排队（异步任务）',
  204: '删除数据成功',
  400: '请求错误(400)',
  401: '未授权，请重新登录(401)',
  403: '拒绝访问(403)',
  404: '请求出错(404)',
  408: '请求超时(408)',
  500: '服务器错误(500)',
  501: '服务未实现(501)',
  502: '网络错误(502)',
  503: '服务不可用(503)',
  504: '网络超时(504)'
}
const UTF8_FILE_NAME_PATTERN = /filename\*=UTF-8''([^;]+)/i
const FILE_NAME_PATTERN = /filename="?([^";]+)"?/i

/** 创建 axios 实例 */
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_PREFIX,
  timeout: 30 * 1000
})

const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_PREFIX,
  timeout: 30 * 1000
})

interface RetryConfig extends AxiosRequestConfig {
  _retry?: boolean
}

let refreshPromise: Promise<string> | null = null

async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new Error('登录状态已过期')
  const form = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: 'web',
    client_secret: 'web'
  })
  const response = await refreshClient.post<{
    access_token: string
    refresh_token?: string
    expires_in?: number
  }>('/oauth/token', form, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
  setAuthTokens(response.data)
  return response.data.access_token
}

async function retryAfterRefresh(config: RetryConfig) {
  if (config._retry || config.url?.includes('/oauth/token')) throw new Error('登录状态已过期')
  config._retry = true
  refreshPromise ||= refreshAccessToken().finally(() => {
    refreshPromise = null
  })
  const token = await refreshPromise
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${token}`
  config.headers.token = token
  return http.request(config)
}

function redirectToLogin() {
  clearAuthTokens()
  if (router.currentRoute.value.path !== '/login') {
    router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

function generateNonce(length = 16) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  const webCrypto = typeof window !== 'undefined' ? window.crypto : undefined
  if (webCrypto?.getRandomValues) {
    try {
      const bytes = new Uint8Array(length)
      webCrypto.getRandomValues(bytes)
      return Array.from(bytes, (value) => chars[value % chars.length]).join('')
    } catch {
      // Fall back to Math.random when Web Crypto is blocked by the browser.
    }
  }
  // Web Crypto is unavailable on plain HTTP IP origins. The nonce still only
  // needs to be unique for the server-side replay cache in that environment.
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

async function createSignature(payload: string) {
  const secret = import.meta.env.VITE_SIGN_SECRET || 'nexiot'
  const subtle = typeof window !== 'undefined' ? window.crypto?.subtle : undefined
  if (subtle && typeof TextEncoder !== 'undefined') {
    const encoder = new TextEncoder()
    const key = await subtle.importKey(
      'raw',
      encoder.encode(secret),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    )
    const signature = await subtle.sign('HMAC', key, encoder.encode(payload))
    return Array.from(new Uint8Array(signature), (value) => value.toString(16).padStart(2, '0')).join('').toUpperCase()
  }
  return CryptoJS.HmacSHA256(payload, secret).toString(CryptoJS.enc.Hex).toUpperCase()
}

async function signRequest(config: AxiosRequestConfig) {
  const rawUrl = config.url || ''
  const parts = rawUrl.split('?')
  const path = parts[0] || ''
  const paramsQuery = config.params instanceof URLSearchParams
    ? config.params.toString()
    : config.params ? qs.stringify(config.params, { indices: false }) : ''
  const query = [parts[1], paramsQuery].filter(Boolean).join('&')
  if (query) {
    config.url = `${path}?${query}`
    config.params = undefined
  }
  const timestamp = Math.floor(Date.now() / 1000).toString()
  const nonce = generateNonce()
  // 与服务端 URLDecoder 一致：原始 + 表示空格，%2B 解码后仍为字面加号。
  const decodedQuery = decodeURIComponent(query.replace(/\+/g, ' '))
  const payload = [(config.method || 'GET').toUpperCase(), path, decodedQuery, timestamp, nonce].join('\n')
  config.headers = config.headers || {}
  config.headers['X-Sign-Timestamp'] = timestamp
  config.headers['X-Sign-Nonce'] = nonce
  config.headers['X-Signature'] = await createSignature(payload)
  return config
}

// 请求拦截器
http.interceptors.request.use(
  async (config: AxiosRequestConfig) => {
    NProgress.start()
    const token = getToken()
    if (token) {
      if (!config.headers) {
        config.headers = {}
      }
      config.headers.token = token
      config.headers.Authorization = `Bearer ${token}`
    }
    return signRequest(config)
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  async (response: AxiosResponse) => {
    if (response.data instanceof Blob) {
      const contentType = response.headers['content-type'] || response.data.type
      if (!String(contentType).includes('application/json')) {
        NProgress.done()
        return response
      }
      const json = JSON.parse(await response.data.text())
      // JSON 导出文件也使用 application/json；只有接口响应信封才进入业务状态处理。
      const isApiResponse = json && !Array.isArray(json)
        && (typeof json.code === 'number' || typeof json.success === 'boolean')
      if (!isApiResponse) {
        NProgress.done()
        return response
      }
      response.data = json
    }

    const body = response.data as ApiRes<unknown> & NexIoTRes<unknown[]> & { access_token?: string }
    const isNexIoTResponse = typeof body.success !== 'boolean' && typeof body.code === 'number'
    const isOAuthResponse = typeof body.access_token === 'string'

    if (isOAuthResponse) {
      response.data = { code: 0, data: body, message: '', success: true }
    } else if (isNexIoTResponse) {
      const { code: _code, msg: _msg, rows: _rows, total: _total, data: bodyData, ...extraData } = body
      const hasExtraData = Object.keys(extraData).length > 0
      response.data = {
        code: body.code,
        data: Array.isArray(body.rows)
          ? { records: body.rows, total: Number(body.total ?? body.rows.length) }
          : hasExtraData
            ? { ...extraData, ...(bodyData === undefined ? {} : { data: bodyData }) }
            : bodyData,
        message: body.msg || '',
        success: body.code === 0
      }
    } else if (typeof (body as ApiRes<unknown>)?.success !== 'boolean') {
      // Some legacy endpoints return a raw PageBean/list/object with HTTP 200.
      response.data = { code: 0, data: body, message: '', success: true }
    }

    const { message: responseMessage, success, code } = response.data as ApiRes<unknown>
    const message = responseMessage || body.msg || ''

    // token失效
    if (code === 401) {
      NProgress.done()
      try {
        return await retryAfterRefresh(response.config)
      } catch {
        redirectToLogin()
        return Promise.reject(new Error(message))
      }
    }

    if (!success) {
      NProgress.done()
      // 如果错误信息长度过长，使用 Notification 进行提示
      if (message.length <= 15) {
        Message.error(message || '服务器端错误')
      } else {
        Notification.error(message || '服务器端错误')
      }
      return Promise.reject(new Error(message || '服务器端错误'))
    }

    NProgress.done()
    return response
  },
  async (error) => {
    NProgress.done()
    // 主动取消历史/轨迹请求时保留取消信号，不把正常切换显示成系统异常。
    if (axios.isCancel(error)) return Promise.reject(error)
    if (error.response?.status === 401) {
      try {
        return await retryAfterRefresh(error.config)
      } catch {
        redirectToLogin()
        return Promise.reject(new Error('登录状态已过期'))
      }
    }
    Message.clear()
    const response = Object.assign({}, error.response)
    const responseData = response?.data
    const message = responseData?.error_description
      || responseData?.msg
      || responseData?.message
      || StatusCodeMessage[response?.status]
      || '系统异常, 请检查网络或联系管理员！'
    Message.error(message)
    return Promise.reject(new Error(message))
  }
)

/** 封装请求方法 */
const request = <T = unknown>(config: AxiosRequestConfig): Promise<ApiRes<T>> => {
  return new Promise((resolve, reject) => {
    http
      .request<T>(config)
      .then((res: AxiosResponse) => resolve(res.data))
      .catch((err: { message: string }) => reject(err))
  })
}

/** GET 请求 */
const get = <T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<ApiRes<T>> => {
  return request({
    method: 'get',
    url,
    params,
    ...config
  })
}

/** POST 请求 */
const post = <T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<ApiRes<T>> => {
  return request({
    method: 'post',
    url,
    data: params,
    ...config
  })
}

/** PUT 请求 */
const put = <T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<ApiRes<T>> => {
  return request({ method: 'put', url, data: params, ...config })
}

/** DELETE 请求 */
const del = <T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<ApiRes<T>> => {
  return request({ method: 'delete', url, params, ...config })
}

interface DownloadOptions {
  method?: 'get' | 'post'
  params?: object
  data?: object
  fileName?: string
  chooseSaveLocation?: boolean
}

interface DownloadFileHandle {
  createWritable: () => Promise<{ write: (data: Blob) => Promise<void>, close: () => Promise<void>, abort: () => Promise<void> }>
}

function getDownloadFileName(contentDisposition?: string) {
  if (!contentDisposition) return ''
  const utf8Match = contentDisposition.match(UTF8_FILE_NAME_PATTERN)
  const plainMatch = contentDisposition.match(FILE_NAME_PATTERN)
  const encodedName = utf8Match?.[1] || plainMatch?.[1] || ''
  try {
    return decodeURIComponent(encodedName)
  } catch {
    return encodedName
  }
}

/** 下载需要鉴权和签名的后端文件。 */
const download = async (url: string, options: DownloadOptions = {}) => {
  let fileHandle: DownloadFileHandle | undefined
  const picker = (window as Window & {
    showSaveFilePicker?: (options: { suggestedName?: string }) => Promise<DownloadFileHandle>
  }).showSaveFilePicker
  // 在点击事件内先选择保存位置，避免异步请求结束后丢失浏览器用户手势。
  if (options.chooseSaveLocation && picker) {
    try {
      fileHandle = await picker.call(window, { suggestedName: options.fileName })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return 'cancelled'
      // 非安全上下文或内嵌浏览器可能禁止文件选择器，继续使用标准下载方式。
      if (!(error instanceof DOMException) || !['SecurityError', 'NotAllowedError'].includes(error.name)) throw error
    }
  }
  const response = await http.request<Blob>({
    method: options.method || 'post',
    url,
    params: options.params,
    data: options.data,
    responseType: 'blob'
  })
  if (!(response.data instanceof Blob)) {
    Message.error('导出接口未返回文件，请重试')
    throw new Error('导出接口未返回文件')
  }
  if (fileHandle) {
    const writable = await fileHandle.createWritable()
    try {
      await writable.write(response.data)
      await writable.close()
      return 'saved'
    } catch (error) {
      await writable.abort().catch(() => {})
      throw error
    }
  }
  const objectUrl = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = options.fileName || getDownloadFileName(response.headers['content-disposition']) || 'download'
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
  return 'requested'
}

export default { get, post, put, delete: del, request, download }
