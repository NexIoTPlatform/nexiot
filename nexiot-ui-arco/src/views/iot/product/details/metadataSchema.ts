import type { ProductMetadataItem } from '@/apis/iot/product'
import { deepClone } from '@/utils'

export type FieldType = 'properties' | 'events' | 'functions'
export interface ValueTypeForm {
  [key: string]: unknown
  type: string
  unit?: string
  min?: number
  max?: number
  step?: number
  precision?: number
  trueValue?: string
  trueText?: string
  falseValue?: string
  falseText?: string
  pattern?: string
  elements: { value: string, text: string, description?: string }[]
  properties: ObjectPropertyForm[]
  elementType: ValueTypeForm
  bodyType?: string
  latProperty?: string
  lonProperty?: string
  format?: string
  expands: { maxLength?: number, minItems?: number, maxItems?: number, accept?: string, maxSize?: number, defaultShapeType?: string }
}
export interface ObjectPropertyForm {
  [key: string]: unknown
  id: string
  name: string
  description: string
  expands: { required?: boolean }
  valueType: ValueTypeForm
}

export const IDENTIFIER_RE = /^[A-Z]\w*$/i

export function createValueType(type = 'string'): ValueTypeForm {
  const normalizedType = type === 'bool' ? 'boolean' : type === 'struct' ? 'object' : type
  return {
    type: normalizedType,
    elements: normalizedType === 'enum' ? [{ value: '', text: '', description: '' }] : [],
    properties: [],
    elementType: normalizedType === 'array' ? createValueType('string') : undefined as unknown as ValueTypeForm,
    bodyType: normalizedType === 'file' ? 'url' : undefined,
    latProperty: normalizedType === 'geoPoint' ? 'lat' : undefined,
    lonProperty: normalizedType === 'geoPoint' ? 'lon' : undefined,
    format: normalizedType === 'date' ? 'timestamp' : undefined,
    expands: normalizedType === 'string' ? { maxLength: 50 } : normalizedType === 'text' ? { maxLength: 500 } : normalizedType === 'geoShape' ? { defaultShapeType: 'Point' } : {}
  }
}

export function normalizeValueType(source: unknown): ValueTypeForm {
  const raw = source && typeof source === 'object' ? deepClone(source as Record<string, unknown>) : { type: source || 'string' }
  const result = Object.assign(createValueType(String(raw.type || 'string')), raw) as ValueTypeForm
  result.type = result.type === 'bool' ? 'boolean' : result.type === 'struct' ? 'object' : result.type
  result.expands = { ...(raw.expands as ValueTypeForm['expands'] || {}) }
  if (raw.maxLength !== undefined && result.expands.maxLength === undefined) result.expands.maxLength = Number(raw.maxLength)
  const enumSource = Array.isArray(raw.elements) ? raw.elements : Array.isArray(raw.enum) ? raw.enum : []
  result.elements = enumSource.map((item) => ({ ...item, value: String(item?.value ?? ''), text: String(item?.text ?? ''), description: String(item?.description ?? '') }))
  result.properties = (Array.isArray(raw.properties) ? raw.properties : []).map((item) => {
    const field = (item || {}) as Record<string, unknown>
    return { ...field, id: String(field.id || ''), name: String(field.name || ''), description: String(field.description || ''), expands: { ...(field.expands as { required?: boolean } || {}) }, valueType: normalizeValueType(field.valueType || field.type || 'string') }
  })
  for (const key of ['trueValue', 'falseValue', 'trueText', 'falseText'] as const) {
    if (raw[key] !== undefined && raw[key] !== null) result[key] = String(raw[key])
  }
  if (result.type === 'array') result.elementType = normalizeValueType(raw.elementType || 'string')
  return result
}

export function buildValueType(source: ValueTypeForm): Record<string, unknown> {
  const result: Record<string, unknown> = deepClone(source)
  for (const key of ['elements', 'enum', 'properties', 'elementType', 'maxLength']) delete result[key]
  for (const key of ['unit', 'min', 'max', 'step', 'precision', 'pattern', 'bodyType', 'latProperty', 'lonProperty', 'format', 'trueValue', 'trueText', 'falseValue', 'falseText'] as const) {
    if (source[key] === undefined || source[key] === '') delete result[key]
  }
  const expands = Object.fromEntries(Object.entries(source.expands || {}).filter(([, value]) => value !== undefined && value !== ''))
  delete result.expands
  if (Object.keys(expands).length) result.expands = expands
  if (source.type === 'enum') result.elements = source.elements.map((item) => ({ ...item, value: item.value.trim(), text: item.text.trim() }))
  if (source.type === 'object') {
    result.properties = source.properties.map((item) => {
      const { type: _type, ...property } = item
      return { ...property, id: item.id.trim(), name: item.name.trim(), description: item.description.trim(), expands: { ...item.expands }, valueType: buildValueType(item.valueType) }
    })
  }
  if (source.type === 'array') result.elementType = buildValueType(source.elementType)
  return result
}

export function validateValueType(source: ValueTypeForm, path = '类型定义'): string | undefined {
  if (['int', 'long', 'float', 'double'].includes(source.type) && source.min !== undefined && source.max !== undefined && source.min > source.max) return `${path}的最小值不能大于最大值`
  if (source.type === 'enum') {
    if (!source.elements.length) return `${path}至少需要一个完整枚举项`
    const values = new Set<string>()
    for (const item of source.elements) {
      const value = item.value.trim()
      if (!value || !item.text.trim()) return `${path}请完整填写枚举值和显示名称`
      if (values.has(value)) return `${path}存在重复枚举值 ${value}`
      values.add(value)
    }
  }
  if (source.step !== undefined && source.step <= 0) return `${path}的步长必须大于 0`
  if (source.pattern) {
    try {
      new RegExp(source.pattern)
    } catch {
      return `${path}的正则表达式无效`
    }
  }
  if (source.type === 'array' && source.expands.minItems !== undefined && source.expands.maxItems !== undefined && source.expands.minItems > source.expands.maxItems) return `${path}的最少项数不能大于最多项数`
  if (source.type === 'boolean' && source.trueValue !== undefined && source.falseValue !== undefined && source.trueValue === source.falseValue) return `${path}的真值和假值不能相同`
  if (source.type === 'object') {
    if (!source.properties.length) return `${path}至少需要一个结构字段`
    const ids = new Set<string>()
    for (const [index, property] of source.properties.entries()) {
      const fieldPath = `${path} / 字段 ${index + 1}`
      if (!property.id.trim() || !property.name.trim()) return `${fieldPath}的标识和名称不能为空`
      if (!IDENTIFIER_RE.test(property.id.trim())) return `${fieldPath}标识必须以字母开头，仅支持字母、数字和下划线`
      if (ids.has(property.id.trim())) return `${path}存在重复字段标识 ${property.id}`
      ids.add(property.id.trim())
      const nestedError = validateValueType(property.valueType, fieldPath)
      if (nestedError) return nestedError
    }
  }
  if (source.type === 'array') return validateValueType(source.elementType, `${path} / 数组元素`)
  return undefined
}

export type MetadataDocument = Record<FieldType, ProductMetadataItem[]> & Record<string, unknown>

export function parseMetadata(value: string): MetadataDocument {
  const data = JSON.parse(value || '{"properties":[],"events":[],"functions":[]}')
  if (!data || typeof data !== 'object' || Array.isArray(data)) throw new Error('物模型必须是 JSON 对象')
  for (const section of ['properties', 'events', 'functions']) {
    if (data[section] === undefined) data[section] = []
    if (!Array.isArray(data[section])) throw new Error('物模型必须包含 properties、events、functions 三个数组')
  }
  return data
}

export function validateMetadataItems(items: ProductMetadataItem[], type: FieldType, path: string): string | undefined {
  const ids = new Set<string>()
  for (const [index, item] of items.entries()) {
    const label = `${path} / 第 ${index + 1} 项`
    if (!item || typeof item.id !== 'string' || typeof item.name !== 'string' || !item.id.trim() || !item.name.trim()) return `${label}的标识和名称不能为空`
    const id = item.id.trim()
    if (!IDENTIFIER_RE.test(id)) return `${label}标识必须以字母开头，仅支持字母、数字和下划线`
    if (ids.has(id)) return `${path}存在重复标识 ${id}`
    ids.add(id)
    if (type === 'functions' && item.inputs !== undefined && !Array.isArray(item.inputs)) return `${label}的输入参数必须是数组`
    const error = type === 'functions'
      ? validateMetadataItems(item.inputs || [], 'properties', `${label} / 输入参数`)
      : validateValueType(normalizeValueType(item.valueType || item.type || 'string'), label)
    if (error) return error
  }
}

export function validateMetadata(data: MetadataDocument): string | undefined {
  for (const [type, label] of [['properties', '属性'], ['events', '事件'], ['functions', '功能']] as const) {
    const error = validateMetadataItems(data[type], type, label)
    if (error) return error
  }
}
