import http from '@/utils/http'

export type TaskType = 'DEVICE_CREDENTIAL_GENERATE' | 'DEVICE_IMPORT' | 'DEVICE_FUNCTION_JOB'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'PARTIAL_SUCCESS' | 'FAILED' | 'CANCELED'
export type TaskItemStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
export type ScheduleType = 'MANUAL' | 'CRON'
export type ScheduleStatus = 'ENABLED' | 'PAUSED'
export type ParamMode = 'FIXED' | 'AUTO' | 'TEMPLATE'

export interface TaskCenterItem {
  id: string
  taskNo?: string
  taskType?: TaskType | string
  taskName?: string
  bizModule?: string
  bizKey?: string
  productKey?: string
  productName?: string
  definitionId?: string
  triggerType?: string
  status?: TaskStatus | string
  totalCount?: number
  successCount?: number
  failCount?: number
  creatorId?: string
  creatorName?: string
  requestSnapshot?: string | Record<string, unknown>
  resultSnapshot?: string | Record<string, unknown>
  extJson?: string | Record<string, unknown>
  errorMessage?: string
  startTime?: string
  endTime?: string
  createTime?: string
  updateTime?: string
}

export interface TaskDefinition {
  id: string
  definitionNo?: string
  taskType?: TaskType | string
  taskName?: string
  productKey?: string
  productName?: string
  scheduleType?: ScheduleType | string
  scheduleStatus?: ScheduleStatus | string
  cronExpression?: string
  misfirePolicy?: string
  lastTaskId?: string
  lastExecuteTime?: string
  nextExecuteTime?: string
  creatorName?: string
  requestSnapshot?: string | Record<string, unknown>
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface TaskDetailItem {
  id: string
  taskId?: string
  itemNo?: string
  itemKey?: string
  itemName?: string
  rowNo?: number
  status?: TaskItemStatus | string
  message?: string
  rawData?: string | Record<string, unknown>
  resultData?: string | Record<string, unknown>
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface TaskLog {
  id: string
  taskId?: string
  logLevel?: string
  logType?: string
  content?: string
  extJson?: string | Record<string, unknown>
  createTime?: string
}

export interface DeviceJobParamSpec {
  mode: ParamMode
  value?: unknown
  template?: string
}

export interface DeviceJobCommand {
  commandId: string
  functionId: string
  serviceType?: string
  source?: string
  functionArgsSpec: Record<string, DeviceJobParamSpec>
  functionData: Record<string, unknown>
}

export interface DeviceJobPayload {
  taskName?: string
  productKey: string
  application?: string
  devGroupId?: string
  keyword?: string
  ids: string[]
  chooseAll: boolean
  functionId?: string
  serviceType?: string
  source?: string
  functionArgsSpec?: Record<string, DeviceJobParamSpec>
  functionData?: Record<string, unknown>
  commandExecutionMode: 'SERIAL' | 'PARALLEL'
  commands: DeviceJobCommand[]
  scheduleType?: ScheduleType
  scheduleStatus?: ScheduleStatus
  cronExpression?: string
  misfirePolicy?: string
}

export interface CredentialTaskPayload {
  productKey: string
  taskName?: string
  generateMode: 'AUTO' | 'LIST'
  count?: number
  deviceIdPrefix?: string
  deviceIdStartNo?: number
  deviceIdLength?: number
  deviceNamePrefix?: string
  deviceIds?: string[]
  deviceIdsText?: string
  application?: string
  deptId?: string
  detail?: string
}

interface PageQuery {
  page: number
  size: number
}

function pageParams<T extends PageQuery>(params: T) {
  return { ...params, pageNum: params.page, pageSize: params.size, page: undefined, size: undefined }
}

export function getTaskDefinitions(params: PageQuery & Partial<Pick<TaskDefinition, 'taskType' | 'taskName' | 'productKey' | 'scheduleType' | 'scheduleStatus'>>) {
  return http.get<PageRes<TaskDefinition[]>>('/admin/v1/task-center/definitions/list', pageParams(params))
}

export function getTaskDefinition(id: string) {
  return http.get<TaskDefinition>(`/admin/v1/task-center/definitions/${id}`)
}

export function createDeviceJobDefinition(data: DeviceJobPayload) {
  return http.post<TaskDefinition>('/admin/v1/task-center/definitions/device-job', data)
}

export function updateDeviceJobDefinition(id: string, data: DeviceJobPayload) {
  return http.put<TaskDefinition>(`/admin/v1/task-center/definitions/device-job/${id}`, data)
}

export function runTaskDefinition(id: string) {
  return http.put<{ taskId?: string }>(`/admin/v1/task-center/definitions/run/${id}`)
}

export function updateTaskDefinitionSchedule(id: string, status: ScheduleStatus) {
  return http.put<unknown>(`/admin/v1/task-center/definitions/schedule-status/${id}/${status}`)
}

export function deleteTaskDefinitions(ids: string[]) {
  return http.delete<unknown>(`/admin/v1/task-center/definitions/${ids.join(',')}`)
}

export function getTaskCenterList(params: PageQuery & Partial<Pick<TaskCenterItem, 'taskType' | 'taskName' | 'productKey' | 'status'>>) {
  return http.get<PageRes<TaskCenterItem[]>>('/admin/v1/task-center/list', pageParams(params))
}

export function getTaskCenter(id: string) {
  return http.get<TaskCenterItem>(`/admin/v1/task-center/${id}`)
}

export function getTaskItems(taskId: string, params: PageQuery & { keyword?: string, status?: string }) {
  return http.get<PageRes<TaskDetailItem[]>>(`/admin/v1/task-center/items/${taskId}`, pageParams(params))
}

export function getTaskLogs(taskId: string) {
  return http.get<TaskLog[]>(`/admin/v1/task-center/logs/${taskId}`)
}

export function getDeviceJobDevices(params: PageQuery & { productKey: string, application?: string, devGroupId?: string, keyword?: string }) {
  return http.get<PageRes<Record<string, unknown>[]>>('/admin/v1/task-center/device-job/devices', pageParams(params))
}

export function createDeviceJobTask(data: DeviceJobPayload) {
  return http.post<{ taskId?: string }>('/admin/v1/task-center/device-job', data)
}

export function getCredentialTasks(params: PageQuery & { taskName?: string, productKey?: string, status?: string }) {
  return http.get<PageRes<TaskCenterItem[]>>('/admin/v1/task-center/device-credential/list', pageParams(params))
}

export function createCredentialTask(data: CredentialTaskPayload) {
  return http.post<{ taskId?: string }>('/admin/v1/task-center/device-credential', data)
}

export function getCredentialTask(id: string) {
  return http.get<TaskCenterItem>(`/admin/v1/task-center/device-credential/${id}`)
}

export function getCredentialTaskItems(taskId: string, params: PageQuery & { keyword?: string, status?: string }) {
  return http.get<PageRes<TaskDetailItem[]>>(`/admin/v1/task-center/device-credential/items/${taskId}`, pageParams(params))
}

export function getCredentialTaskLogs(taskId: string) {
  return http.get<TaskLog[]>(`/admin/v1/task-center/device-credential/logs/${taskId}`)
}

export function exportFailedItems(taskId: string, taskNo = 'task') {
  return http.download(`/admin/v1/task-center/export-failed/${taskId}`, { method: 'post', fileName: `${taskNo}_failed_items.xlsx` })
}

export function exportCredentials(taskId: string, taskNo = 'credential') {
  return http.download(`/admin/v1/task-center/export-credentials/${taskId}`, { method: 'post', fileName: `${taskNo}_credentials.xlsx` })
}
