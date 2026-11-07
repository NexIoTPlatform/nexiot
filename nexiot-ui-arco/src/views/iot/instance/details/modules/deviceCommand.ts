import type { DeviceItem, DeviceShadow } from '@/apis/iot/device'
import type { ProductMetadataItem } from '@/apis/iot/product'

export type RuntimeValue = string | number | boolean | undefined
export type AutoFillSource = 'device' | 'reported' | 'desired' | 'configuration' | 'default'

export interface RuntimeField extends Omit<ProductMetadataItem, 'expands'> {
  required?: boolean
  placeholder?: string
  defaultValue?: RuntimeValue
  default?: RuntimeValue
  expands?: ProductMetadataItem['expands'] & { required?: boolean, autoFill?: boolean } | string
}

export interface DeviceFunction extends Omit<ProductMetadataItem, 'inputs'> {
  inputs?: RuntimeField[] | string
}

export interface AutoFillResult {
  values: Record<string, RuntimeValue>
  sources: Record<string, AutoFillSource>
}

const DEVICE_FIELDS = new Set(['deviceId', 'iotId', 'deviceName', 'productKey', 'extDeviceId'])
const NUMBER_TYPES = new Set(['int', 'integer', 'short', 'byte', 'long', 'float', 'double', 'decimal', 'number'])

export function parseRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>
  try {
    const parsed = JSON.parse(String(value || '{}'))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

export function normalizeInputs(value: DeviceFunction['inputs'], defaultRequired = true): RuntimeField[] {
  const raw = Array.isArray(value) ? value : typeof value === 'string' ? parseArray(value) : []
  return raw.map((item) => normalizeField(item, defaultRequired))
}

function parseArray(value: string) {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function normalizeField(value: unknown, defaultRequired: boolean): RuntimeField {
  const field = parseRecord(value) as unknown as RuntimeField
  const valueType = fieldSchema(field)
  const expands = parseRecord(field.expands)
  return {
    ...field,
    valueType,
    placeholder: String(field.placeholder ?? expands.placeholder ?? ''),
    required: Boolean(field.required ?? expands.required ?? defaultRequired)
  }
}

export function fieldSchema(field: RuntimeField) {
  if (typeof field.valueType === 'string' && !field.valueType.trim().startsWith('{')) return { type: field.valueType }
  return parseRecord(field.valueType)
}

export function fieldType(field: RuntimeField) {
  return String(fieldSchema(field).type || field.type || 'string').toLowerCase()
}

export function isNumberField(field: RuntimeField) {
  return NUMBER_TYPES.has(fieldType(field))
}

export function enumOptions(field: RuntimeField) {
  const elements = fieldSchema(field).elements
  return Array.isArray(elements)
    ? elements.map((item) => {
        const data = parseRecord(item)
        return { label: String(data.text ?? data.label ?? data.value), value: coerceRuntimeValue(field, data.value) as string | number }
      })
    : []
}

export function autoFillCommandFields(fields: RuntimeField[], device: DeviceItem, shadow: DeviceShadow | undefined, includeRuntime = true): AutoFillResult {
  const values: Record<string, RuntimeValue> = {}
  const sources: Record<string, AutoFillSource> = {}
  // Device custom values live in devConfiguration. Older list/detail responses exposed
  // them as configuration, so keep that field as a compatibility fallback.
  const configuration = parseRecord(device.devConfiguration ?? device.configuration)
  const shadowRoot = parseRecord(shadow?.metadata)
  const state = parseRecord(shadowRoot.state)
  const reported = Object.keys(parseRecord(state.reported)).length ? parseRecord(state.reported) : parseRecord(shadowRoot.reported)
  const desired = Object.keys(parseRecord(state.desired)).length ? parseRecord(state.desired) : parseRecord(shadowRoot.desired)

  for (const field of fields) {
    if (!includeRuntime || String(parseRecord(field.expands).autoFill).toLowerCase() === 'false') {
      values[field.id] = undefined
      continue
    }
    const runtime = includeRuntime ? resolveRuntimeValue(field.id, device, configuration, reported, desired) : undefined
    if (runtime) {
      values[field.id] = coerceRuntimeValue(field, runtime.value)
      sources[field.id] = runtime.source
      continue
    }
    const schema = fieldSchema(field)
    const expands = parseRecord(field.expands)
    const defaultValue = [field.defaultValue, field.default, expands.defaultValue, expands.default, schema.defaultValue, schema.default].find(isSimpleValue)
    if (isSimpleValue(defaultValue)) {
      values[field.id] = coerceRuntimeValue(field, defaultValue)
      sources[field.id] = 'default'
    } else {
      values[field.id] = emptyFieldValue(field)
    }
  }
  return { values, sources }
}

function resolveRuntimeValue(
  fieldId: string,
  device: DeviceItem,
  configuration: Record<string, unknown>,
  reported: Record<string, unknown>,
  desired: Record<string, unknown>
): { value: string | number | boolean, source: AutoFillSource } | undefined {
  if (isSimpleValue(reported[fieldId])) return { value: reported[fieldId], source: 'reported' }
  if (isSimpleValue(desired[fieldId])) return { value: desired[fieldId], source: 'desired' }
  if (isSimpleValue(configuration[fieldId])) return { value: configuration[fieldId], source: 'configuration' }
  const deviceValue = device[fieldId]
  if (DEVICE_FIELDS.has(fieldId) && isSimpleValue(deviceValue)) return { value: deviceValue, source: 'device' }
  return undefined
}

function isSimpleValue(value: unknown): value is string | number | boolean {
  return (typeof value === 'string' && value.trim() !== '') || typeof value === 'number' || typeof value === 'boolean'
}

export function coerceRuntimeValue(field: RuntimeField, value: unknown): RuntimeValue {
  const type = fieldType(field)
  if (value === undefined || value === null || (typeof value === 'string' && !value.trim())) return undefined
  if (NUMBER_TYPES.has(type)) {
    const number = Number(value)
    if (!Number.isFinite(number)) return undefined
    if (['int', 'integer', 'short', 'byte', 'long'].includes(type) && !Number.isSafeInteger(number)) return undefined
    return number
  }
  if (type === 'boolean' || type === 'bool') {
    const text = String(value).trim().toLowerCase()
    if (text === 'true' || text === '1') return true
    if (text === 'false' || text === '0') return false
    return undefined
  }
  if (type === 'enum' && (typeof value === 'string' || typeof value === 'number')) {
    const schema = fieldSchema(field)
    const underlying = schema.valueType ?? schema.dataType
    const underlyingField = { ...field, valueType: underlying as RuntimeField['valueType'] }
    if (underlying && fieldType(underlyingField) !== 'enum') return coerceRuntimeValue(underlyingField, value)
    const element = Array.isArray(schema.elements) ? schema.elements.map(parseRecord).find((item) => String(item.value) === String(value)) : undefined
    return (element?.value ?? value) as string | number
  }
  return typeof value === 'string' ? value : String(value)
}

export function emptyFieldValue(field: RuntimeField): RuntimeValue {
  const type = fieldType(field)
  if (type === 'boolean') return undefined
  if (type === 'object') return '{}'
  if (type === 'array') return '[]'
  return undefined
}

export function autoFillSourceLabel(source: AutoFillSource) {
  return ({
    device: '设备标识',
    reported: '设备上报',
    desired: '影子期望',
    configuration: '设备配置',
    default: '默认值'
  } as Record<AutoFillSource, string>)[source]
}

export function autoFillSourceColor(source: AutoFillSource) {
  return ({ device: 'arcoblue', reported: 'green', desired: 'orange', configuration: 'purple', default: 'gray' } as Record<AutoFillSource, string>)[source]
}
