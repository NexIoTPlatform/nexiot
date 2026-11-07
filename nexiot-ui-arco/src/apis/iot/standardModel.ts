import http from '@/utils/http'

export interface StandardCategoryItem {
  id: string
  parentId: string
  categoryKey: string
  categoryName: string
  level: number
  listOrder?: number
  description?: string
  icon?: string
  status: number
  categoryPath?: string
  children?: StandardCategoryItem[]
}

export type StandardCategoryTreeNode = Omit<StandardCategoryItem, 'icon' | 'children'> & {
  iconName?: string
  children?: StandardCategoryTreeNode[]
}

export function toStandardCategoryTree(nodes: StandardCategoryItem[]): StandardCategoryTreeNode[] {
  return nodes.map(({ icon, children, ...node }) => ({
    ...node,
    iconName: icon,
    children: children ? toStandardCategoryTree(children) : []
  }))
}

export interface StandardProductItem {
  id: string
  categoryId: string
  productKey: string
  productName: string
  description?: string
  photoUrl?: string
  metadata?: string
  status: number
  isTemplate: number
  useCount?: number
  categoryName?: string
  categoryPath?: string
  createTime?: string
  updateTime?: string
}

export function getStandardCategoryTree() {
  return http.get<StandardCategoryItem[]>('/admin/v1/standard/category/tree')
}

export function getStandardCategory(id: string) {
  return http.get<StandardCategoryItem>(`/admin/v1/standard/category/${id}`)
}

export function addStandardCategory(data: Partial<StandardCategoryItem>) {
  return http.post<unknown>('/admin/v1/standard/category', data)
}

export function updateStandardCategory(data: Partial<StandardCategoryItem>) {
  return http.put<unknown>('/admin/v1/standard/category', data)
}

export function deleteStandardCategory(id: string) {
  return http.delete<unknown>(`/admin/v1/standard/category/${id}`)
}

export function getStandardProductList(params: { page: number, size: number, productName?: string, productKey?: string, categoryId?: string }) {
  return http.get<PageRes<StandardProductItem[]>>('/admin/v1/standard/product/list', {
    pageNum: params.page,
    pageSize: params.size,
    productName: params.productName,
    productKey: params.productKey,
    categoryId: params.categoryId
  })
}

export function getStandardProduct(id: string) {
  return http.get<StandardProductItem>(`/admin/v1/standard/product/${id}`)
}

export function addStandardProduct(data: Partial<StandardProductItem>) {
  return http.post<unknown>('/admin/v1/standard/product', data)
}

export function updateStandardProduct(data: Partial<StandardProductItem>) {
  return http.put<unknown>('/admin/v1/standard/product', data)
}

export function updateStandardProductMetadata(id: string, metadata: string) {
  return http.request<unknown>({ method: 'put', url: `/admin/v1/standard/product/metadata/${id}`, data: metadata, headers: { 'Content-Type': 'application/json' } })
}

export function setStandardProductTemplate(id: string, isTemplate: boolean) {
  return http.put<unknown>(`/admin/v1/standard/product/template/${id}`, undefined, { params: { isTemplate } })
}

export function deleteStandardProducts(ids: string[]) {
  const path = ids.length === 1 ? `/admin/v1/standard/product/${ids[0]}` : `/admin/v1/standard/product/batch/${ids.join(',')}`
  return http.delete<unknown>(path)
}
