import http from '@/utils/http'

export type StandardFieldType = 'property' | 'event' | 'function'

export interface StandardFieldItem {
  id: string
  fieldType: StandardFieldType
  fieldId: string
  fieldName: string
  categoryId?: string
  categoryName?: string
  dataType?: string
  unit?: string
  mode?: string
  description?: string
  valueTypeConfig?: string
  metadataConfig?: string
  searchKeywords?: string
  useCount?: number
  recommendPriority?: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface StandardFieldQuery {
  page: number
  size: number
  fieldId?: string
  fieldName?: string
  fieldType?: StandardFieldType
}

export function getStandardFieldList(params: StandardFieldQuery) {
  return http.get<PageRes<StandardFieldItem[]>>('/admin/v1/standard/field/list', {
    pageNum: params.page,
    pageSize: params.size,
    fieldId: params.fieldId,
    fieldName: params.fieldName,
    fieldType: params.fieldType
  })
}

export function getStandardField(id: string) {
  return http.get<StandardFieldItem>(`/admin/v1/standard/field/${id}`)
}

export function searchStandardFields(keyword: string, fieldType?: StandardFieldType, limit = 10) {
  return http.get<StandardFieldItem[]>('/admin/v1/standard/field/search', { keyword, fieldType, limit })
}

export function addStandardField(data: Partial<StandardFieldItem>) {
  return http.post<unknown>('/admin/v1/standard/field', data)
}

export function updateStandardField(data: Partial<StandardFieldItem>) {
  return http.put<unknown>('/admin/v1/standard/field', data)
}

export function deleteStandardFields(ids: string[]) {
  const path = ids.length === 1 ? `/admin/v1/standard/field/${ids[0]}` : `/admin/v1/standard/field/batch/${ids.join(',')}`
  return http.delete<unknown>(path)
}
