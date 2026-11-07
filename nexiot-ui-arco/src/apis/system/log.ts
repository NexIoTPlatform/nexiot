import http from '@/utils/http'

export interface LoginLogItem {
  infoId: string
  userName: string
  status: string
  ipaddr: string
  loginLocation: string
  browser: string
  os: string
  msg: string
  loginTime: string
}

export interface OperLogItem {
  operId: string
  title: string
  businessType: number
  method: string
  requestMethod: string
  operatorType: number
  operName: string
  deptName: string
  operUrl: string
  operIp: string
  operLocation: string
  operParam: string
  jsonResult: string
  status: number
  errorMsg: string
  operTime: string
}

export interface LogQuery {
  beginTime?: string
  endTime?: string
}

export interface LoginLogQuery extends LogQuery {
  userName?: string
  ipaddr?: string
  status?: string
}

export interface OperLogQuery extends LogQuery {
  title?: string
  operName?: string
  businessType?: number
  status?: number
}

function toDateParams(query: LogQuery) {
  return {
    'params[beginTime]': query.beginTime,
    'params[endTime]': query.endTime
  }
}

export async function getLoginLogList(params: { page: number, size: number } & LoginLogQuery) {
  const res = await http.get<PageRes<LoginLogItem[]>>('/admin/monitor/logininfor/list', {
    pageNum: params.page,
    pageSize: params.size,
    userName: params.userName,
    ipaddr: params.ipaddr,
    status: params.status,
    ...toDateParams(params)
  })
  return { ...res, data: { records: res.data.records.map((item) => ({ ...item, infoId: String(item.infoId) })), total: res.data.total } }
}

export function deleteLoginLogs(ids: string[]) {
  return http.delete<unknown>(`/admin/monitor/logininfor/${ids.join(',')}`)
}

export function cleanLoginLogs() {
  return http.delete<unknown>('/admin/monitor/logininfor/clean')
}

export function exportLoginLogs(query: LoginLogQuery) {
  return http.download('/admin/monitor/logininfor/export', {
    method: 'post',
    params: { ...query, ...toDateParams(query) },
    fileName: `登录日志_${Date.now()}.xlsx`
  })
}

export async function getOperLogList(params: { page: number, size: number } & OperLogQuery) {
  const res = await http.get<PageRes<OperLogItem[]>>('/admin/monitor/operlog/list', {
    pageNum: params.page,
    pageSize: params.size,
    title: params.title,
    operName: params.operName,
    businessType: params.businessType,
    status: params.status,
    ...toDateParams(params)
  })
  return { ...res, data: { records: res.data.records.map((item) => ({ ...item, operId: String(item.operId) })), total: res.data.total } }
}

export function exportOperLogs(query: OperLogQuery) {
  return http.download('/admin/monitor/operlog/export', {
    method: 'post',
    params: { ...query, ...toDateParams(query) },
    fileName: `操作日志_${Date.now()}.xlsx`
  })
}
