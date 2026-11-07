/** 接口返回数据格式 */
interface ApiRes<T> {
  code: number
  data: T
  message: string
  success: boolean
}

/** NexIoT 旧接口返回格式，由 HTTP 层统一转换为 ApiRes */
interface NexIoTRes<T = unknown> {
  code: number
  data?: T
  rows?: T
  total?: number
  msg?: string
}

/** 分页返回的数据格式 */
interface PageRes<T> {
  records: T
  total: number
}
