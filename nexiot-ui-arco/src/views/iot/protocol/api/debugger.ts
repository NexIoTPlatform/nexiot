import http from '@/utils/http'

export interface MagicFunction {
  key: string
  group: string
  name: string
  signature: string
  description: string
  returnType: string
  deprecated: boolean
  params: { name: string, type: string, description: string, varArgs: boolean }[]
}
export interface DebugVariable {
  name: string
  type: string
  value: string
  kind?: 'value' | 'string' | 'class' | 'bytes'
  members?: number
  notice?: string
  children?: DebugVariable[]
}
export interface DebugRun {
  id: string
  state: 'QUEUED' | 'RUNNING' | 'PAUSED' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'
  line: number | null
  variables: DebugVariable[]
  logs: string[]
  result: string | null
  error: string | null
  elapsedMs: number
}
export interface RunInput {
  source: string
  payload: string
  context: string | null
  method: string
  debug: boolean
  breakpoints: number[]
}
export interface DriverRevision {
  id: string
  source: string
  sourceHash: string
  version: string
  savedBy: string
  createdAt: string
}
const base = (id: string) => `/admin/v1/protocol/${encodeURIComponent(id)}/debugger`
export const getMagicFunctions = () => http.get<MagicFunction[]>('/admin/v1/protocol/magic/functions')
export const getDebugSource = (id: string) => http.get<{ source: string, sourceHash: string }>(base(id))
export const runDriver = (id: string, input: RunInput) => http.post<DebugRun>(`${base(id)}/runs`, input)
export const pollDriver = (id: string, runId: string) => http.get<DebugRun>(`${base(id)}/runs/${runId}`)
export const stopDriver = (id: string, runId: string) => http.delete(`${base(id)}/runs/${runId}`)
export const commandDriver = (id: string, runId: string, action: 'continue' | 'step' | 'breakpoints', breakpoints: number[]) => http.post<DebugRun>(`${base(id)}/runs/${runId}/command`, { action, breakpoints })
export const getDriverHistory = (id: string) => http.get<DriverRevision[]>(`${base(id)}/history`)
export const applyDriver = (id: string, source: string, expectedSourceHash: string) => http.put<{ sourceHash: string }>(base(id), { source, expectedSourceHash })
