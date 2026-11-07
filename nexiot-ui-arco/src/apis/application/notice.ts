import type { WechatConfig } from '@/views/application/notice/modules/wechat'
import http from '@/utils/http'

export interface NoticeChannel {
  id?: string
  name: string
  channelType: string
  status?: number
  config: Record<string, string>
  remark?: string
  tenantId?: string
  deptId?: number
  creator?: string
  createTime?: string
  updateTime?: string
}
export interface NoticeTemplate {
  wechat?: WechatConfig | null
  id?: string
  name: string
  channelType: string
  channelId?: string
  content: string
  receivers?: string
  status?: number
  remark?: string
  tenantId?: string
  deptId?: number
  creator?: string
  createTime?: string
  updateTime?: string
}
export async function getNoticeChannels(params: {
  page: number
  size: number
  name?: string
  channelType?: string
  status?: number
}) {
  const response = await http.get<PageRes<NoticeChannel[]>>('/admin/v1/notice/channel/list', {
    pageNum: params.page,
    pageSize: params.size,
    name: params.name,
    channelType: params.channelType,
    status: params.status
  })
  response.data.records = (response.data.records || []).map(normalizeNoticeChannel)
  return response
}
export async function getNoticeChannel(id: string) {
  const response = await http.get<NoticeChannel>('/admin/v1/notice/channel/get', { id })
  response.data = normalizeNoticeChannel(response.data)
  return response
}
export function saveNoticeChannel(data: NoticeChannel) {
  return http.post<unknown>('/admin/v1/notice/channel/save', data)
}
export function deleteNoticeChannels(ids: string[]) {
  return http.post<unknown>('/admin/v1/notice/channel/delete', ids)
}
export async function getNoticeTemplates(params: {
  page: number
  size: number
  name?: string
  channelType?: string
  status?: number
}) {
  const response = await http.get<PageRes<NoticeTemplate[]>>('/admin/v1/notice/template/list', {
    pageNum: params.page,
    pageSize: params.size,
    name: params.name,
    channelType: params.channelType,
    status: params.status
  })
  response.data.records = (response.data.records || []).map(normalizeNoticeTemplate)
  return response
}
export async function getNoticeTemplate(id: string) {
  const response = await http.get<NoticeTemplate>('/admin/v1/notice/template/get', { id })
  response.data = normalizeNoticeTemplate(response.data)
  return response
}
export function saveNoticeTemplate(data: NoticeTemplate) {
  return http.post<unknown>('/admin/v1/notice/template/save', data)
}
export function deleteNoticeTemplates(ids: string[]) {
  return http.post<unknown>('/admin/v1/notice/template/delete', ids)
}
export function enableNoticeTemplates(ids: string[]) {
  return http.post<unknown>('/admin/v1/notice/template/enable', ids)
}
export function disableNoticeTemplates(ids: string[]) {
  return http.post<unknown>('/admin/v1/notice/template/disable', ids)
}
export function testNoticeTemplate(data: {
  templateId: string
  receivers: string
  params?: Record<string, unknown>
}) {
  return http.post<unknown>('/admin/v1/notice/template/test', data)
}
export function sendNotice(data: {
  templateId: string
  receivers: string
  params?: Record<string, unknown>
}) {
  return http.post<unknown>('/admin/v1/notice/send', data)
}

export function getWechatOpenIdUrl(params: { channelId: string, redirectUri: string, state?: string }) {
  return http.get<{ url: string }>('/open-api/v1/notice/wechat/openid/url', params)
}

export function exchangeWechatOpenId(params: { channelId: string, code: string }) {
  return http.get<{ openid: string }>('/open-api/v1/notice/wechat/openid/exchange', params)
}

export function sendOpenNotice(data: {
  templateId: string
  receivers: string
  params?: Record<string, unknown>
}) {
  return http.post<{ success: boolean, message: string }>('/open-api/v1/notice/send', data)
}

export function listNoticeChannelFields(dictType = 'notice_wechat_field') {
  return http.get<Array<{ key: string, label: string, meta: string }>>('/admin/v1/notice/meta/fields', { dictType })
}

export interface NoticePreview {
  channelType: string
  content?: string
  payload?: { template_id: string, data: Record<string, { value: string }>, url?: string, miniprogram?: { appid: string, pagepath: string } }
}
export function previewNoticeTemplate(data: { templateId: string, params: Record<string, unknown> }) {
  return http.post<NoticePreview>('/admin/v1/notice/template/preview', data)
}

export interface WechatBinding { status: 'WAITING' | 'BOUND' | 'FAILED', openid: string, userKey: string, message: string }
export function createWechatBinding(data: { channelId: string, userKey: string }) {
  return http.post<{ sessionId: string, url: string, expiresIn: number }>('/admin/v1/notice/wechat/bind/session', data)
}
export function getWechatBinding(sessionId: string) {
  return http.get<WechatBinding>(`/admin/v1/notice/wechat/bind/session/${sessionId}`)
}

function normalizeNoticeChannel(channel: NoticeChannel): NoticeChannel {
  return { ...channel, id: String(channel.id), status: Number(channel.status) }
}
export function normalizeNoticeTemplate(template: NoticeTemplate): NoticeTemplate {
  return { ...template, id: String(template.id), channelId: String(template.channelId), status: Number(template.status) }
}
