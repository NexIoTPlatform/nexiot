import http from '@/utils/http'

export type OtaTransferMode = 'URL' | 'PUSH' | 'PULL'

export interface OtaTransferProfile {
  mode: OtaTransferMode
  chunkSize: number
  ackTimeoutSeconds: number
  chunkMaxRetries: number
  dataEncoding: 'BASE64' | 'HEX'
  indexBase: 0 | 1
  templateName: string
}

export const transferModeOptions = [
  { value: 'URL', label: '整包下载', description: '设备收到下载地址，自行下载完整文件。' },
  { value: 'PUSH', label: '平台逐包发送', description: '平台切包并逐包发送，设备确认后继续，超时自动补发。' },
  { value: 'PULL', label: '设备按需索包', description: '设备确认收到后，再请求下一分包；中断后从已确认进度继续。' }
] as const

export function createTransferProfile(): OtaTransferProfile {
  return { mode: 'URL', chunkSize: 1024, ackTimeoutSeconds: 10, chunkMaxRetries: 3, dataEncoding: 'BASE64', indexBase: 0, templateName: '' }
}

export function getOtaTransferProfile(productKey: string) {
  return http.get<OtaTransferProfile>(`/admin/v1/ota/profile/${encodeURIComponent(productKey)}`)
}

export function updateOtaTransferProfile(productKey: string, profile: OtaTransferProfile) {
  return http.put<OtaTransferProfile>(`/admin/v1/ota/profile/${encodeURIComponent(productKey)}`, profile)
}
