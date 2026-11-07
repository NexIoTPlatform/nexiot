import http from '@/utils/http'

export interface ProductCategoryItem {
  id: string
  parentId: string
  hasChild?: number
  identification: string
  classifiedName: string
  description?: string
  createTime?: string
  createBy?: string
  children?: ProductCategoryItem[]
}

export interface ProductCategoryQuery {
  identification?: string
  classifiedName?: string
}

function normalizeList(data: PageRes<ProductCategoryItem[]> | ProductCategoryItem[]) {
  return Array.isArray(data) ? data : data.records
}

export async function getProductCategoryList(params: ProductCategoryQuery = {}) {
  const res = await http.get<PageRes<ProductCategoryItem[]>>('/admin/sort/list', params)
  return { ...res, data: normalizeList(res.data) }
}

export async function getProductCategoryTree() {
  const res = await http.get<ProductCategoryItem[]>('/admin/sort/treeselect')
  return { ...res, data: normalizeList(res.data) }
}

export function getProductCategory(id: string) {
  return http.get<ProductCategoryItem>(`/admin/sort/${id}`)
}

export function addProductCategory(data: Partial<ProductCategoryItem>) {
  return http.post<unknown>('/admin/sort', data)
}

export function updateProductCategory(data: Partial<ProductCategoryItem>) {
  return http.put<unknown>('/admin/sort', data)
}

export function deleteProductCategory(id: string) {
  return http.delete<unknown>(`/admin/sort/${id}`)
}
