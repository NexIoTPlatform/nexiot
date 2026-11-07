export interface DeviceCustomFieldOption {
  label: string
  value: string | number | boolean
}

export interface DeviceCustomFieldCondition {
  field?: string
  key?: string
  dependsOn?: string
  exists?: boolean
  in?: unknown | unknown[]
  values?: unknown | unknown[]
  oneOf?: unknown | unknown[]
  notIn?: unknown | unknown[]
  eq?: unknown
  value?: unknown
  equals?: unknown
  ne?: unknown
  not?: unknown
  anyOf?: DeviceCustomFieldCondition[]
  or?: DeviceCustomFieldCondition[]
  allOf?: DeviceCustomFieldCondition[]
  and?: DeviceCustomFieldCondition[]
}

export interface DeviceCustomField {
  id: string
  name: string
  type: string
  required?: boolean
  placeholder?: string
  description?: string
  helpText?: string
  min?: number
  max?: number
  minLength?: number
  maxLength?: number
  disabled?: boolean
  options?: DeviceCustomFieldOption[]
  optionUrl?: string
  optionsUrl?: string
  hidden?: boolean
  defaultValue?: unknown
  visibleWhen?: DeviceCustomFieldCondition | DeviceCustomFieldCondition[]
}

function valuesOf<T>(value?: T | T[]): T[] {
  if (Array.isArray(value)) return value
  return value === undefined || value === null ? [] : [value]
}

function isBlank(value: unknown) {
  return value === undefined || value === null || value === ''
}

function normalized(value: unknown) {
  const token = String(value ?? '').trim().toLowerCase().replace(/[_/\s]+/g, '-')
  const aliases: Record<string, string> = {
    'tcp': 'tcp-client',
    'modbus-tcp': 'tcp-client',
    'modbus-tcp-client': 'tcp-client',
    'modbus-rtu-tcp': 'rtu-tcp',
    'modbus-rtu-over-tcp': 'rtu-tcp',
    'rtu-over-tcp': 'rtu-tcp',
    'serial': 'rtu-serial',
    'modbus-serial': 'rtu-serial',
    'modbus-rtu': 'rtu-serial',
    'modbus-rtu-serial': 'rtu-serial',
    'modbus-rtu-over-serial': 'rtu-serial',
    'rtu-over-serial': 'rtu-serial',
    'serial-rtu': 'rtu-serial',
    'modbus-ascii': 'ascii-serial',
    'modbus-ascii-serial': 'ascii-serial',
    'modbus-ascii-over-serial': 'ascii-serial',
    'ascii-over-serial': 'ascii-serial',
    'serial-ascii': 'ascii-serial'
  }
  return aliases[token] || token
}

function fieldValue(fieldId: string, values: Record<string, unknown>, fields: DeviceCustomField[]) {
  if (fieldId in values && !isBlank(values[fieldId])) return values[fieldId]
  const field = fields.find((item) => item.id === fieldId)
  if (field?.defaultValue !== undefined && field.defaultValue !== null) return field.defaultValue
  if (fieldId === 'transportType') return 'tcp-client'
  return values[fieldId]
}

function contains(expected: unknown | unknown[], actual: unknown) {
  return valuesOf(expected).some((item) => normalized(item) === normalized(actual))
}

function matches(
  condition: DeviceCustomFieldCondition,
  values: Record<string, unknown>,
  fields: DeviceCustomField[]
): boolean {
  const anyOf = condition.anyOf || condition.or
  if (anyOf) return anyOf.some((item) => matches(item, values, fields))
  const allOf = condition.allOf || condition.and
  if (allOf) return allOf.every((item) => matches(item, values, fields))
  const fieldId = condition.field || condition.key || condition.dependsOn
  if (!fieldId) return true
  const actual = fieldValue(fieldId, values, fields)
  if (condition.exists !== undefined) return condition.exists ? !isBlank(actual) : isBlank(actual)
  if (condition.in !== undefined || condition.values !== undefined || condition.oneOf !== undefined) {
    return contains(condition.in ?? condition.values ?? condition.oneOf ?? [], actual)
  }
  if (condition.notIn !== undefined) return !contains(condition.notIn, actual)
  if (condition.eq !== undefined || condition.value !== undefined || condition.equals !== undefined) {
    return contains(condition.eq ?? condition.value ?? condition.equals, actual)
  }
  if (condition.ne !== undefined || condition.not !== undefined) {
    return !contains(condition.ne ?? condition.not, actual)
  }
  return true
}

export function isDeviceCustomFieldVisible(
  field: DeviceCustomField,
  values: Record<string, unknown>,
  fields: DeviceCustomField[]
) {
  if (field.hidden) return false
  return !field.visibleWhen || valuesOf(field.visibleWhen).every((condition) => matches(condition, values, fields))
}

export function visibleDeviceCustomFields(fields: DeviceCustomField[], values: Record<string, unknown>) {
  return fields.filter((field) => isDeviceCustomFieldVisible(field, values, fields))
}


export function normalizeDeviceCustomValues(
  fields: DeviceCustomField[],
  source: Record<string, unknown> = {}
) {
  const values = { ...source }
  for (let pass = 0; pass < Math.max(fields.length, 1); pass += 1) {
    let changed = false
    visibleDeviceCustomFields(fields, values).forEach((field) => {
      if (field.defaultValue !== undefined && (values[field.id] === undefined || values[field.id] === null)) {
        values[field.id] = field.defaultValue
        changed = true
      }
    })
    if (!changed) break
  }
  fields.forEach((field) => {
    if (field.visibleWhen && !isDeviceCustomFieldVisible(field, values, fields)) delete values[field.id]
  })
  return values
}

