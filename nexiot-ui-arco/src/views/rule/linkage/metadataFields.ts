import type { ProductMetadataItem } from '@/apis/iot/product'

interface ValueType {
  type?: string
  properties?: ProductMetadataItem[]
  elementType?: ValueType
}

export function metadataFieldOptions(items: ProductMetadataItem[], prefix = '', depth = 0): { label: string, value: string }[] {
  if (depth > 6) return []
  return items.flatMap((item) => {
    const path = prefix ? `${prefix}.${item.id}` : item.id
    const type = (item.valueType || {}) as ValueType
    const children = type.type === 'array'
      ? [{ id: '0', name: `${item.name || item.id}首项`, valueType: type.elementType }]
      : type.properties || []
    return [{ label: `${item.name || item.id} (${path})`, value: path }, ...metadataFieldOptions(children, path, depth + 1)]
  })
}
