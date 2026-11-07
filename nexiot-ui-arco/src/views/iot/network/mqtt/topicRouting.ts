import type { MqttTopicMapping } from '@/apis/iot/mqtt'

export function validateTopicPattern(pattern: string) {
  let value = pattern.trim()
  if (!value) return '主题模式不能为空'
  if (value.includes('\0')) return '主题模式不能包含空字符'
  if (value.startsWith('$share/')) {
    const [, group, ...filters] = value.split('/')
    if (!group || group.includes('+') || group.includes('#') || !filters.join('/')) {
      return '共享订阅格式为 $share/组名/主题模式，组名不能包含通配符'
    }
    value = filters.join('/')
  }
  const levels = value.split('/')
  for (let index = 0; index < levels.length; index += 1) {
    const level = levels[index]
    if (level.includes('#') && (level !== '#' || index !== levels.length - 1)) {
      return '# 必须独占最后一级'
    }
    if (level.includes('+') && level !== '+') return '+ 必须独占一级'
  }
  return ''
}

export function topicMatches(topic: string, pattern: string) {
  if (!topic || topic.includes('\0') || topic.includes('+') || topic.includes('#') || validateTopicPattern(pattern)) return false
  const filter = pattern.startsWith('$share/') ? pattern.split('/').slice(2).join('/') : pattern
  const topicLevels = topic.split('/')
  const patternLevels = filter.split('/')
  if (topic.startsWith('$') && ['+', '#'].includes(patternLevels[0])) return false
  for (let index = 0; index < patternLevels.length; index += 1) {
    const level = patternLevels[index]
    if (level === '#') return true
    if (topicLevels[index] === undefined) return false
    if (level !== '+' && level !== topicLevels[index]) return false
  }
  return topicLevels.length === patternLevels.length
}

// 与后端 MQTTTopicManager 的标准 Topic 结构一致，固定位置优先于通配符位置。
const STANDARD_TOPICS = [
  { pattern: /^\$[^/]+\/up\/(?:property|event)\/([^/]+)\/([^/]+)$/, category: 'THING_MODEL' },
  { pattern: /^\$[^/]+\/down\/([^/]+)\/([^/]+)$/, category: 'THING_MODEL' },
  { pattern: /^\$[^/]+\/up\/([^/]+)\/([^/]+)$/, category: 'PASSTHROUGH' },
  { pattern: /^\$[^/]+\/(?:report|update)\/([^/]+)\/([^/]+)$/, category: 'SYSTEM_LEVEL' }
] as const

export interface TopicMatchResult {
  matched: boolean
  message: string
  productKey?: string
  topicCategory?: string
}

/** 预览当前表单规则；只有主题、产品绑定及分类都可解析时才显示成功。 */
export function previewTopicRoute(topic: string, mappings: MqttTopicMapping[], boundKeys: string[]): TopicMatchResult {
  const mapping = mappings.find((item) => item.enabled && topicMatches(topic, item.topicPattern))
  if (!mapping) return { matched: false, message: '未匹配任何已启用的主题模式' }
  const standard = STANDARD_TOPICS.find((item) => item.pattern.test(topic))
  let productKey = mapping.productKey || standard?.pattern.exec(topic)?.[1]
  if (!productKey) {
    const candidates = [...new Set(topic.split('/').filter((level) => boundKeys.includes(level)))]
    if (candidates.length > 1) {
      return { matched: false, message: `已命中 ${mapping.topicPattern}，但 Topic 包含多个已绑定产品 Key，请明确选择关联产品` }
    }
    productKey = candidates[0]
  }
  if (!productKey) {
    return { matched: false, message: `已命中 ${mapping.topicPattern}，但无法自动识别产品。自定义 Topic 需包含唯一已绑定产品 Key，否则请选择关联产品` }
  }
  if (!boundKeys.includes(productKey)) {
    return { matched: false, productKey, message: `已识别产品 ${productKey}，但该产品未绑定当前网络组件` }
  }
  const topicCategory = mapping.topicCategory || standard?.category
  if (!topicCategory) {
    return { matched: false, productKey, message: `已识别产品 ${productKey}，但无法确定数据类型，请选择物模型或透传` }
  }
  return { matched: true, productKey, topicCategory, message: `匹配成功：${mapping.topicPattern}` }
}
