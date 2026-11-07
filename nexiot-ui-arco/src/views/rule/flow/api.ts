import type { ApplicationItem } from '@/apis/application/application'
import type { DeviceGroupItem, DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import http from '@/utils/http'

export interface RuleTarget {
  id: string
  type: 'http'
  url: string
}

export interface ForwardRuleConfig {
  appId: string
  fields: string
  condition?: string
  targets: RuleTarget[]
}

export interface ForwardRule {
  id?: string
  ruleName: string
  description?: string
  dataLevel: 'product' | 'group' | 'device'
  productKey?: string
  groupId?: string
  relationIds: string[]
  status: 'run' | 'stop'
  payload?: string
  config: ForwardRuleConfig
  createTime?: string
  updateTime?: string
}

export function getForwardRules(params: { page: number, size: number, ruleName?: string, status?: 'run' | 'stop' }) {
  return http.get<PageRes<ForwardRule[]>>('/admin/v1/rule/list', {
    pageNum: params.page,
    pageSize: params.size,
    ruleName: params.ruleName,
    status: params.status
  })
}

export function getForwardRule(id: string) {
  return http.get<ForwardRule>('/admin/v1/rule', { id })
}

export function addForwardRule(data: ForwardRule) {
  return http.post<string>('/admin/v1/rule', data)
}

export function updateForwardRule(data: ForwardRule) {
  return http.put<unknown>('/admin/v1/rule', data)
}

export function updateForwardRuleTargets(data: ForwardRule) {
  return http.put<unknown>('/admin/v1/rule/targets', data)
}

export function updateForwardRuleStatus(data: ForwardRule) {
  return http.post<unknown>('/admin/v1/rule/status', data)
}

export function deleteForwardRule(id: string) {
  return http.delete<unknown>(`/admin/v1/rule/${id}`)
}

export function testForwardSql(data: ForwardRule) {
  return http.post<unknown>('/admin/v1/rule/sql/test', data)
}

export function testForwardTarget(data: { param: string, ruleTarget: RuleTarget }) {
  return http.post<unknown>('/admin/v1/rule/target/test', data)
}

export function getForwardProducts() {
  return http.get<PageRes<ProductItem[]>>('/admin/v1/product/v4/list', { pageNum: 1, pageSize: 99999, state: 0 })
}

export function getForwardApplications() {
  return http.get<PageRes<ApplicationItem[]>>('/admin/v1/apply/list', { pageNum: 1, pageSize: 99999 })
}

export function getForwardGroups() {
  return http.get<DeviceGroupItem[]>('/admin/v1/instance/group/list')
}

export function getForwardDevices(productKey: string) {
  return http.get<PageRes<DeviceItem[]>>('/admin/v1/device/list', { pageNum: 1, pageSize: 99999, productKey })
}
