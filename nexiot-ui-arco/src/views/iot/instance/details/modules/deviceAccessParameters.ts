import type { DeviceCustomField } from '@/views/iot/instance/modules/deviceCustomFields'
import { parseRecord } from './deviceCommand'

export interface DeviceAccessParameter {
  key: string
  name: string
  displayValue: string
  rawValue: string
  description: string
  secret: boolean
}

export interface DeviceAccessValuePresentation {
  kind: 'scalar' | 'object' | 'array' | 'long-text'
  previewValue: string
  detailValue: string
  expandable: boolean
}

const SENSITIVE_KEY_PATTERN = /password|secret|token|credential/i
const MAX_PREVIEW_LENGTH = 80

// 常用工业与网络参数中文名称与说明字典
const COMMON_CONFIG_PARAM_MAP: Record<string, { name: string, description?: string }> = {
  slaveId: { name: '从机地址', description: 'Modbus 从站设备编号 (1~247)' },
  slaveAddress: { name: '从机地址', description: '工业通信设备物理/逻辑站号' },
  transportType: { name: '通信传输方式', description: '数据传输链路与封装方式' },
  pollingInterval: { name: '采集轮询周期', description: '主动采集命令的轮询执行间隔(毫秒)' },
  pollInterval: { name: '采集周期', description: '命令轮询调度周期(毫秒)' },
  timeout: { name: '通信超时时间', description: '单次交互等待设备响应的最大毫秒数' },
  retryCount: { name: '失败重试次数', description: '通信未收到应答时的最大尝试次数' },
  retry: { name: '重试次数', description: '通信重试次数' },
  host: { name: '主机地址/IP', description: '目标通信服务器或网关网络地址' },
  serverIp: { name: '服务器 IP', description: '设备远程接入的主机 IP 地址' },
  ip: { name: '设备 IP', description: '设备本地或局域网 IP' },
  port: { name: '通信端口', description: '网络连接对应的远程服务端口' },
  baudRate: { name: '波特率', description: '串口通信波特率(bps)' },
  serialPort: { name: '采集主机串口', description: '运行后端采集服务的主机上的串口设备标识' },
  dataBits: { name: '数据位', description: '串口数据传输有效位数' },
  stopBits: { name: '停止位', description: '串口帧同步停止位' },
  parity: { name: '校验方式', description: '串口传输奇偶校验规则' },
  flowControl: { name: '流控方式', description: '串口硬件或软件流控制' },
  imei: { name: '设备 IMEI 码', description: '蜂窝通信设备国际移动设备识别码' },
  imsi: { name: 'IMSI 卡标识', description: 'SIM 卡国际移动用户识别码' },
  iccid: { name: 'ICCID 卡号', description: 'SIM 卡集成电路卡识别码' },
  username: { name: '接入用户名', description: '认证授权用户名' },
  password: { name: '接入密码', description: '认证密码' },
  clientId: { name: '客户端标识', description: 'MQTT 唯一客户端标识符' },
  keepAlive: { name: '心跳间隔 (秒)', description: '客户端维持连接的心跳保活周期' },
  subTopic: { name: '订阅主题', description: '设备消息下发/订阅主题' },
  pubTopic: { name: '发布主题', description: '设备数据上报主题' },
  qos: { name: '服务质量等级', description: 'MQTT 消息投递 QoS 等级 (0/1/2)' },
  cleanSession: { name: '清除会话', description: '客户端断开后是否清理会话记录' },
  connectionMode: { name: '连接模式', description: '网关共享连接或设备独立直连模式' }
}

// 常见参数枚举值的中文提示
const COMMON_CONFIG_VALUE_MAP: Record<string, Record<string, string>> = {
  transportType: {
    'tcp-client': 'TCP 客户端 (tcp-client)',
    'rtu-tcp': 'Modbus RTU over TCP (rtu-tcp)',
    'rtu-serial': 'Modbus RTU 串口 (rtu-serial)',
    'ascii-serial': 'Modbus ASCII 串口 (ascii-serial)'
  },
  parity: {
    none: '无校验 (None)',
    even: '偶校验 (Even)',
    odd: '奇校验 (Odd)',
    mark: '标志校验 (Mark)',
    space: '空格校验 (Space)'
  },
  connectionMode: {
    SHARED: '网关共享连接 (SHARED)',
    INDEPENDENT: '独立直连模式 (INDEPENDENT)'
  }
}

export function buildDeviceAccessParameters(configuration: unknown, fields: DeviceCustomField[] = []): DeviceAccessParameter[] {
  return Object.entries(parseRecord(configuration)).map(([key, rawValue]) => {
    const customField = fields.find((field) => field.id === key)
    const commonParam = COMMON_CONFIG_PARAM_MAP[key]
    const secret = customField?.type === 'password' || SENSITIVE_KEY_PATTERN.test(key)
    const enumOption = customField?.options?.find((option) => String(option.value) === String(rawValue))
    let displayValue = formatAccessValue(rawValue)
    if (enumOption) displayValue = `${enumOption.label} (${rawValue})`
    else if (COMMON_CONFIG_VALUE_MAP[key]?.[String(rawValue)]) displayValue = COMMON_CONFIG_VALUE_MAP[key]![String(rawValue)]!
    if (secret) displayValue = rawValue === undefined || rawValue === null || rawValue === '' ? '未配置' : '已设置（不回显）'
    return {
      key,
      name: customField?.name || commonParam?.name || key,
      displayValue,
      rawValue: secret ? '' : typeof rawValue === 'object' ? JSON.stringify(rawValue) : String(rawValue ?? ''),
      description: customField?.description || customField?.helpText || commonParam?.description || '',
      secret
    }
  })
}

export function getDeviceAccessValuePresentation(item: DeviceAccessParameter): DeviceAccessValuePresentation {
  const scalar: DeviceAccessValuePresentation = {
    kind: 'scalar',
    previewValue: item.displayValue,
    detailValue: item.displayValue,
    expandable: false
  }
  if (item.secret) return scalar

  // 仅解析结构化 JSON，数字字符串可能是标识符或超出安全整数精度。
  const source = item.rawValue.trim()
  if (source.startsWith('{') || source.startsWith('[')) {
    try {
      const value: unknown = JSON.parse(source)
      if (value && typeof value === 'object') {
        const array = Array.isArray(value)
        return {
          kind: array ? 'array' : 'object',
          previewValue: array ? `数组 · ${value.length} 项` : `对象 · ${Object.keys(value).length} 个字段`,
          detailValue: formatAccessJson(source),
          expandable: true
        }
      }
    } catch {
      // 非法 JSON 保留原文，继续支持查看和复制。
    }
  }

  if (item.displayValue.length > MAX_PREVIEW_LENGTH || /[\r\n]/.test(item.displayValue)) {
    const singleLine = item.displayValue.replace(/\s+/g, ' ').trim()
    return {
      kind: 'long-text',
      previewValue: singleLine.length > MAX_PREVIEW_LENGTH ? `${singleLine.slice(0, MAX_PREVIEW_LENGTH - 1)}…` : singleLine,
      detailValue: item.displayValue,
      expandable: true
    }
  }

  return scalar
}

function formatAccessJson(source: string): string {
  // 按原文排版，保留大整数、指数形式和字符串转义的准确内容。
  const tokens = source.match(/"(?:\\.|[^"\\])*"|[{}[\],:]|[^\s{}[\],:]+/g) || []
  const formatted: string[] = []
  let depth = 0
  for (const [index, token] of tokens.entries()) {
    if (token === '{' || token === '[') {
      formatted.push(token)
      depth += 1
      const next = tokens[index + 1]
      if (next !== '}' && next !== ']') formatted.push(`\n${'  '.repeat(depth)}`)
    } else if (token === '}' || token === ']') {
      depth -= 1
      const previous = tokens[index - 1]
      if (previous !== '{' && previous !== '[') formatted.push(`\n${'  '.repeat(depth)}`)
      formatted.push(token)
    } else if (token === ',') {
      formatted.push(`,\n${'  '.repeat(depth)}`)
    } else if (token === ':') {
      formatted.push(': ')
    } else {
      formatted.push(token)
    }
  }
  return formatted.join('')
}

function formatAccessValue(value: unknown): string {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  if (typeof value === 'boolean') return value ? '是 (true)' : '否 (false)'
  return String(value)
}
