import type { ProductMetadataItem } from '@/apis/iot/product'
import type { StandardFieldItem } from '@/apis/iot/standardField'
import { buildValueType, normalizeValueType } from './metadataSchema'

function parseConfig(value: unknown, label: string): Record<string, unknown> {
  if (value === undefined || value === null || value === '') return {}
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed as Record<string, unknown>
  } catch {
    throw new Error(`${label} JSON 格式错误`)
  }
  throw new Error(`${label}必须是 JSON 对象`)
}

function parseInputs(value: unknown): ProductMetadataItem[] {
  if (value === undefined || value === null || value === '') return []
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (Array.isArray(parsed)) return parsed
  } catch {
    throw new Error('标准功能输入参数 JSON 格式错误')
  }
  throw new Error('标准功能输入参数必须是数组')
}

export function standardFieldToMetadata(item: StandardFieldItem): ProductMetadataItem {
  const metadata = parseConfig(item.metadataConfig, '标准字段完整配置')
  const result = {
    ...metadata,
    id: item.fieldId || String(metadata.id || ''),
    name: item.fieldName || String(metadata.name || ''),
    description: item.description ?? metadata.description
  } as ProductMetadataItem
  if (item.fieldType === 'function') {
    result.inputs = parseInputs(metadata.inputs ?? metadata.inputData).map((input) => {
      const { type, ...field } = input
      return { ...field, valueType: buildValueType(normalizeValueType(input.valueType || type || 'string')) }
    })
    result.config = Boolean(metadata.config ?? metadata.isConfig)
    delete (result as unknown as Record<string, unknown>).inputData
  } else {
    let valueSource = metadata.valueType || metadata.type
    if (!valueSource) {
      const valueConfig = parseConfig(item.valueTypeConfig, '标准字段类型配置')
      valueSource = Object.keys(valueConfig).length ? valueConfig : { type: item.dataType || 'string' }
    }
    const valueType = normalizeValueType(valueSource)
    if (!valueType.unit && item.unit) valueType.unit = item.unit
    result.valueType = buildValueType(valueType)
    delete result.type
    if (item.fieldType === 'property') result.mode = item.mode || result.mode || 'rw'
  }
  return result
}
