import type { TransformOverview, TransformPreview, TransformRule } from '@/apis/application/application'
import http from '@/utils/http'

export function updateApplicationPush(data: Record<string, unknown>) {
  return http.put<unknown>('/admin/v1/apply', data)
}

export function updatePushEnabled(appUniqueId: string, pushType: string, enable: boolean) {
  return http.post<unknown>(`/admin/v1/apply/${appUniqueId}/${pushType}/${enable}`)
}

export function testPushConnection(appUniqueId: string, pushType: string, data: Record<string, unknown>) {
  return http.post<unknown>(`/admin/v1/apply/${appUniqueId}/push/${pushType}/test`, data)
}

export function getApplicationTransform(appUniqueId: string) {
  return http.get<TransformOverview>(`/admin/v1/apply/transform/${appUniqueId}`)
}

export function saveApplicationTransform(appUniqueId: string, data: TransformRule) {
  return http.put<TransformRule>(`/admin/v1/apply/transform/${appUniqueId}/rule`, data)
}

export function updateApplicationTransform(appUniqueId: string, data: TransformOverview['config']) {
  return http.put<unknown>(`/admin/v1/apply/transform/${appUniqueId}/config`, data)
}

export function previewApplicationTransform(data: {
  appUniqueId: string
  ruleType: string
  presetCode?: string
  content?: string
  options?: string
  sampleInput: string
}) {
  return http.post<TransformPreview>('/admin/v1/apply/transform/preview', data)
}
