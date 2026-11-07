// 来自 SceneLinkageService → SenceIoTDeviceDownService 的通知上下文，以及 UPRequest / Request。
// 仅提供标量字段；trigger、action、properties、data 等对象不能直接作为微信字段值。
export const wechatVariableGroups = [
  {
    label: '场景联动 · 各类触发均可用',
    options: [
      { key: 'sceneName', label: '场景名称', aliases: ['规则名称', '联动名称'], description: '当前执行的场景 / 规则名称' },
      { key: 'sceneId', label: '场景 ID', aliases: ['规则ID', '场景编号'], description: '当前场景的唯一编号' },
      { key: 'execTime', label: '执行时间', aliases: ['通知时间', '触发时间'], description: '通知执行时间，格式：2026-09-14 15:30:00' }
    ]
  },
  {
    label: '触发设备 · 设备上报时可用',
    options: [
      { key: 'deviceName', label: '设备名称', aliases: [], description: '触发场景的上报设备名称' },
      { key: 'deviceId', label: '设备编号', aliases: ['设备ID'], description: '上报设备的设备 ID' },
      { key: 'iotId', label: '平台设备 ID', aliases: [], description: '上报设备在平台内的唯一标识' },
      { key: 'productKey', label: '产品标识', aliases: ['产品Key'], description: '上报设备所属产品的 productKey' },
      { key: 'deviceNode', label: '设备节点类型', aliases: [], description: '上报设备的节点类型，需报文提供' },
      { key: 'childDeviceId', label: '子设备编号', aliases: [], description: '子设备上报时提供的设备 ID' }
    ]
  },
  {
    label: '上报消息 · 按报文内容提供',
    options: [
      { key: 'messageType', label: '消息类型', aliases: [], description: '上报消息的类型，如 PROPERTIES、EVENT' },
      { key: 'eventName', label: '事件名称', aliases: [], description: '事件上报时的事件名称，需报文提供' },
      { key: 'event', label: '事件标识', aliases: ['事件ID'], description: '事件上报时的事件编码' },
      { key: 'time', label: '上报时间戳', aliases: [], description: '上报消息中的原始数值时间戳；可读通知时间请选「执行时间」' },
      { key: 'requestId', label: '请求 ID', aliases: [], description: '上报消息的请求标识，需报文提供' }
    ]
  }
]

export const wechatVariables = wechatVariableGroups.flatMap((group) => group.options.map((option) => ({
  ...option,
  value: `#{${option.key}}`
})))

export function findWechatVariable(value: string) {
  return wechatVariables.find((option) => option.value === value)
}

export function suggestWechatValue(key: string, label: string) {
  const normalize = (text: string) => text.replace(/\s/g, '').toLowerCase()
  const exact = wechatVariables.find((option) => option.key === key)
  const matched = exact || wechatVariables.find((option) => [option.label, ...option.aliases]
    .some((alias) => normalize(alias) === normalize(label)))
  return matched ? matched.value : ''
}

export interface WechatField { key: string, label: string, value: string }
export interface WechatConfig { templateId: string, url: string, miniProgramAppId: string, miniProgramPage: string, fields: WechatField[] }
interface TemplateSource { channelType?: string, content?: string, wechat?: WechatConfig | null }
const FIELD_RE = /([^{}]*)\{\{\s*([a-zA-Z]\w*)\.DATA\s*\}\}/g
const LABEL_RE = /[:：]\s*$/
const PARAM_RE = /#\{([\w.]+)\}/g
export function emptyWechat(): WechatConfig {
  return { templateId: '', url: '', miniProgramAppId: '', miniProgramPage: '', fields: [] }
}
export function identifyFields(content: string, previous: WechatField[] = []) {
  const fields = new Map<string, WechatField>()
  for (const match of (content || '').matchAll(FIELD_RE)) {
    const key = match[2]
    const existing = previous.find((field) => field.key === key)
    if (fields.has(key)) continue
    const label = match[1].trim().split(/\r?\n/).pop()?.replace(LABEL_RE, '').trim() || key
    fields.set(key, existing || { key, label, value: suggestWechatValue(key, label) })
  }
  return Array.from(fields.values())
}
export function noticeVariables(template: TemplateSource) {
  const config = template.channelType === 'wechat' ? template.wechat : null
  const content = config ? [...config.fields.map((field) => field.value), config.url, config.miniProgramPage].join('\n') : template.content || ''
  return [...new Set(Array.from(content.matchAll(PARAM_RE), (match) => match[1]))]
}
export function previewWechat(template: TemplateSource, params: Record<string, unknown> = {}) {
  return (template.wechat?.fields || []).map((field) => ({
    key: field.key,
    label: field.label || field.key,
    value: field.value.replace(PARAM_RE, (_, key) => String(params[key] ?? `待填写 ${key}`))
  }))
}
