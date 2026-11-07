import http from '@/utils/http'

export interface CertificateItem {
  id: string
  name: string
  sslKey: string
  expireTime?: string
  certInfo?: string | Record<string, unknown>
  certContent?: string
  keyContent?: string
  certPassword?: string
  keyPassword?: string
  certificateType?: 'GENERIC' | 'CLIENT' | 'CA' | 'SERVER' | 'OPCUA_BUNDLE'
  trustMode?: 'PINNED' | 'CA'
  trustCertContent?: string
  fingerprintSha256?: string
  trustFingerprintSha256?: string
  remark?: string
  createUser?: string
  createTime?: string
  updateTime?: string
  disabled?: boolean
}

export interface CertificateListParams {
  page: number
  size: number
  name?: string
  sslKey?: string
  expireStart?: string
  expireEnd?: string
}

export function getCertificateList(params: CertificateListParams) {
  return http.get<PageRes<CertificateItem[]>>('/admin/v1/certificate/list', {
    pageNum: params.page,
    pageSize: params.size,
    name: params.name,
    sslKey: params.sslKey,
    expireStart: params.expireStart,
    expireEnd: params.expireEnd
  })
}

export function getCertificate(sslKey: string) {
  return http.get<CertificateItem>('/admin/v1/certificate/get', { sslKey })
}

export function addCertificate(data: Partial<CertificateItem>) {
  return http.post<unknown>('/admin/v1/certificate/add', data)
}

export function updateCertificate(data: Partial<CertificateItem>) {
  return http.post<unknown>('/admin/v1/certificate/update', data)
}

export function deleteCertificate(id: string) {
  return http.post<unknown>('/admin/v1/certificate/delete', undefined, {
    params: { id }
  })
}

export function uploadCertificateFile(data: FormData) {
  return http.post<unknown>('/admin/v1/certificate/uploadFile', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadCertificateContent(data: {
  sslKey: string
  certContent: string
  keyContent: string
  name?: string
}) {
  const form = new FormData()
  Object.entries(data).forEach(([key, value]) => form.append(key, value || ''))
  return http.post<unknown>('/admin/v1/certificate/uploadContent', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadOpcuaCertificateBundle(data: FormData) {
  return http.post<unknown>('/admin/v1/certificate/uploadOpcuaBundle', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function bindProductCertificate(productKey: string, sslKey: string) {
  return http.post<unknown>('/admin/v1/product/bindCertificate', undefined, {
    params: { productKey, sslKey }
  })
}

export function unbindProductCertificate(productKey: string) {
  return http.post<unknown>('/admin/v1/product/unbindCertificate', undefined, {
    params: { productKey }
  })
}
