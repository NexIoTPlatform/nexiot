import http from '@/utils/http'

export interface ProductOptionItem {
  id: string
  productKey: string
  name: string
}

export interface ProductItem {
  id: string
  productId?: string
  productKey: string
  name: string
  devNum?: number
  tags?: string | string[]
  thirdPlatform?: string
  companyNo?: string
  classifiedId?: string
  classifiedName?: string
  deviceNode?: string
  messageProtocol?: string
  transportProtocol?: string
  state: number
  describe?: string
  storePolicy?: string
  photoUrl?: string
  configuration?: string
  storePolicyConfiguration?: string
  metadata?: string
  createTime?: string
  updateTime?: string
  creatorId?: string
  networkUnionId?: string
  gwProductKey?: string
  gwName?: string
  gwPhotoUrl?: string
  thirdConfiguration?: string
  instance?: string
  publicProductID?: string
  payloadFormat?: number
  powerModel?: number
  tupIsThrough?: number
  lwm2mEdrxTime?: string
  southboundCloudDockingAddress?: string
}

export interface ProductAccessProtocol {
  code: string
  name: string
  available: boolean
  status?: string
  description?: string
  isManualCreatable?: boolean
  notCreatableReason?: string
}

export interface ProductMetadataItem {
  id: string
  name: string
  description?: string
  mode?: string
  type?: string
  valueType?: unknown
  inputs?: ProductMetadataItem[]
  expands?: { level?: string }
  source?: string
  config?: boolean
}

export interface ProductConfigDictItem {
  dictLabel: string
  dictValue: string
  remark?: string
}

export interface ProductStorePolicy {
  label?: string
  value?: string
  policyName?: string
  policyCode?: string
  [key: string]: unknown
}

function parseMetadataRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function parseMetadataArray(value: unknown): unknown[] {
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(String(value || '[]'))
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function normalizeValueType(value: unknown) {
  const objectValue = parseMetadataRecord(value)
  if (Object.keys(objectValue).length) return objectValue
  return typeof value === 'string' && value.trim() ? { type: value.trim() } : undefined
}

function normalizeMetadataItem(value: unknown): ProductMetadataItem {
  const record = parseMetadataRecord(value)
  const item = {
    ...record,
    id: String(record.id || ''),
    name: String(record.name || ''),
    valueType: normalizeValueType(record.valueType),
    expands: parseMetadataRecord(record.expands),
    inputs: parseMetadataArray(record.inputs).map(normalizeMetadataItem)
  }
  return item as ProductMetadataItem
}

function normalizeMetadataItems(value: unknown) {
  return (Array.isArray(value) ? value : []).map(normalizeMetadataItem)
}

export interface ProductOperationField {
  key: string
  label: string
  type?: 'select' | 'boolean' | 'int' | 'textarea' | 'string'
  placeholder?: string
  description?: string
  required?: boolean
  defaultValue?: string | number | boolean
  advanced?: boolean
  options?: { label: string, value: string | number | boolean }[]
}

export interface ProductOperation {
  code: string
  name: string
  description?: string
  enabled: boolean
  disabledReason?: string
  confirmText?: string
  group?: string
  groupName?: string
  fields?: ProductOperationField[]
}

export interface ProductDeviceItem {
  id: string
  deviceId: string
  deviceName?: string
  iotId?: string
  productKey: string
  productName?: string
  state?: boolean | number | string
  creatorId?: string
  creatorName?: string
  pendingOwner?: boolean
  onlineTime?: string | number
  registryTime?: string | number
  deviceNode?: string
  deptId?: string
  detail?: string
  longitude?: string | number
  latitude?: string | number
  secretKey?: string
  extDeviceId?: string
  gwDeviceId?: string
  configuration?: string
  otherParams?: Record<string, unknown>
}

export interface ProductConnectInfo {
  network?: Record<string, unknown>
  mqtt?: Record<string, unknown>
  topics?: Record<string, unknown>
  [key: string]: unknown
}

export function getProductList(params: { page: number, size: number, productKey?: string, name?: string, companyNo?: string, classifiedName?: string, state?: number }) {
  return http.get<PageRes<ProductItem[]>>('/admin/v1/product/v4/list', { pageNum: params.page, pageSize: params.size, ...params })
}

export function getProductOptions(params: { name?: string, productKey?: string } = {}) {
  return http.get<PageRes<ProductOptionItem[]>>('/admin/v1/product/options', { pageNum: 1, pageSize: 20, ...params })
}
export function getProduct(id: string, desensitizeConfig = false) {
  return http.get<ProductItem>(`/admin/v1/product/${id}`, { desensitizeConfig })
}

export function getProductByKey(productKey: string) {
  return http.get<ProductItem>(`/admin/v1/product/get/pro/${productKey}`)
}

export function addProduct(data: Partial<ProductItem>) {
  return http.post<Pick<ProductItem, 'id' | 'productKey'>>('/admin/v1/product', data)
}

export function getProductAccessProtocols() {
  return http.get<{ protocols: Record<string, ProductAccessProtocol> }>('/admin/protocol/status')
}




export function updateProduct(data: Partial<ProductItem>) {
  return http.put<unknown>('/admin/v1/product', data)
}

export function updateProductConfig(data: { id: string, configuration: string }) {
  return http.put<unknown>('/admin/v1/product/config', data)
}

export function updateProductStoreConfig(data: { id: string, storePolicyConfiguration: string }) {
  return http.put<unknown>('/admin/v1/product/storeConfig', data)
}

export function updateProductOtherConfig(data: { productId: string, customField: object[] }) {
  return http.put<unknown>('/admin/v1/product/otherConfig', data)
}

export function getProductConfigDicts(types: string[]) {
  return http.get<Record<string, ProductConfigDictItem[]>>(`/admin/system/dict/data/types/${types.join(',')}`)
}

export function getEnabledStorePolicies() {
  return http.get<(ProductStorePolicy | string)[]>('/admin/v1/product/storePolicies/enabled')
}

export function uploadProductImage(id: string, file: File) {
  const data = new FormData()
  data.append('id', id)
  data.append('file', file)
  return http.request<{ url?: string }>({ method: 'post', url: '/admin/v1/product/uploadImage', data })
}

export function getGatewayProducts() {
  return http.get<ProductItem[]>('/admin/v1/product/gateway/list')
}

export function updateProductGateway(id: string, gwProductKey?: string) {
  return http.put<unknown>('/admin/v1/product/gateway', { id, gwProductKey: gwProductKey || '' })
}

export function deleteProducts(ids: string[]) {
  return http.delete<unknown>(`/admin/v1/product/${ids.join(',')}`)
}

export function getProductDeviceCounts() {
  return http.get<Record<string, number>>('/admin/v1/product/device/count')
}

export async function getProductProperties(id: string) {
  const res = await http.get<unknown[]>(`/admin/v1/product/properties/${id}`)
  return { ...res, data: normalizeMetadataItems(res.data) }
}

export async function getProductEvents(id: string) {
  const res = await http.get<unknown[]>(`/admin/v1/product/events/${id}`)
  return { ...res, data: normalizeMetadataItems(res.data) }
}

export async function getProductFunctions(id: string) {
  const res = await http.get<unknown[]>(`/admin/v1/product/functions/${id}`)
  return { ...res, data: normalizeMetadataItems(res.data) }
}

export function updateProductMetadata(data: { productKey: string, mode: 'replace' | 'merge', properties: ProductMetadataItem[], events: ProductMetadataItem[], functions: ProductMetadataItem[], standardProductId?: string, metadata?: string }) {
  return http.post<unknown>('/admin/v1/product/metadata/batch', data)
}

export function addProductMetadata(data: { productKey: string, type: string, ioTProductMetadataBO: ProductMetadataItem }) {
  return http.post<unknown>('/admin/v1/product/metadata/add', data)
}

export function editProductMetadata(data: { productKey: string, type: string, oldId: string, ioTProductMetadataBO: ProductMetadataItem }) {
  return http.post<unknown>('/admin/v1/product/metadata/update', data)
}

export function deleteProductMetadata(data: { productKey: string, type: string, oldId: string }) {
  return http.post<unknown>('/admin/v1/product/metadata/delete', data)
}

export function getProductOperations(id: string) {
  return http.get<ProductOperation[]>(`/admin/v1/product/operations/${id}`)
}

export function executeProductOperation(id: string, operationCode: string, params: Record<string, unknown>) {
  return http.post<unknown>(`/admin/v1/product/operations/${id}/${operationCode}`, { params })
}

export function getProductDeviceList(params: { page: number, size: number, productKey: string, deviceId?: string, deviceName?: string, iotId?: string, deviceTag?: string, state?: number }) {
  return http.get<PageRes<ProductDeviceItem[]>>('/admin/v1/device/list', {
    pageNum: params.page,
    pageSize: params.size,
    productKey: params.productKey,
    deviceId: params.deviceId,
    deviceName: params.deviceName,
    iotId: params.iotId,
    deviceTag: params.deviceTag,
    state: params.state
  })
}

export function getProductDevice(id: string) {
  return http.get<ProductDeviceItem>(`/admin/v1/device/${id}`)
}

export function addProductDevice(data: Partial<ProductDeviceItem>) {
  return http.post<unknown>('/admin/v1/device', data)
}

export function updateProductDevice(data: Partial<ProductDeviceItem>) {
  return http.put<unknown>('/admin/v1/device', data)
}

export function deleteProductDevices(ids: string[]) {
  return http.delete<unknown>(`/admin/v1/device/${ids.join(',')}`)
}

export function importProductDevices(file: File, productKey: string) {
  const data = new FormData()
  data.append('file', file)
  return http.request<unknown>({ method: 'post', url: '/admin/v1/device/import', params: { productKey }, data })
}

export function exportProductDevices(params: { productKey: string, deviceId?: string, deviceName?: string, iotId?: string, deviceTag?: string, state?: number }) {
  return http.download('/admin/v1/device/export', { method: 'post', params, fileName: `devices_${Date.now()}.xlsx` })
}

export function getProductGatewayDevices(productKey: string) {
  return http.get<ProductDeviceItem[]>(`/admin/v1/device/gateway/${productKey}`)
}

/** Returns the child products explicitly bound to a gateway product. */
export function getGatewaySubProducts(productKey: string) {
  return http.get<ProductItem[]>(`/admin/v1/product/sub/device/${productKey}`)
}

export function getProductConnectInfo(productKey: string) {
  return http.post<ProductConnectInfo>('/admin/v1/product/connect/info', undefined, { params: { productKey } })
}

export function renameProductKey(data: { id: string, productKey: string }) {
  return http.put<unknown>('/admin/v1/product/productKey', data)
}

export function exportProducts(params: { id?: string, productKey?: string, companyNo?: string, classifiedName?: string } = {}, fileName = `product_packages_${Date.now()}.json`) {
  return http.download('/admin/v1/product/transfer/export', { method: 'post', params, fileName, chooseSaveLocation: true })
}

export interface ProductImportResult {
  productCount: number
  protocolCount: number
  networkCount: number
  metadataVersionCount: number
}

export function importProducts(file: File) {
  const data = new FormData()
  data.append('file', file)
  return http.request<ProductImportResult>({ method: 'post', url: '/admin/v1/product/transfer/import', data })
}
