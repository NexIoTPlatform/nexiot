import CustomIcons from '@/icons/custom-icons.json'

export type ResolvedMenuIcon
  = | { kind: 'arco', value: string }
    | { kind: 'iconify', value: string }
    | { kind: 'none', value: '' }

const CUSTOM_ICON_NAMES = new Set(Object.keys(CustomIcons.icons))

const LEGACY_ANT_ICON_MAP: Record<string, string> = {
  'api': 'icon-command',
  'appstore': 'icon-apps',
  'area-chart': 'icon-bar-chart',
  'bars': 'icon-menu',
  'box-plot': 'icon-bar-chart',
  'build': 'icon-settings',
  'button': 'icon-apps',
  'bxAnalyse': 'icon-bar-chart',
  'cascader': 'icon-mind-mapping',
  'cloud-server': 'icon-storage',
  'cloud-upload': 'icon-upload',
  'cluster': 'icon-mind-mapping',
  'color': 'icon-palette',
  'control': 'icon-settings',
  'database': 'icon-storage',
  'deployment-unit': 'icon-relation',
  'dict': 'icon-book',
  'documentation': 'icon-book',
  'drag': 'icon-drag-dot-vertical',
  'eyeOpen': 'icon-eye',
  'file-sync': 'icon-sync',
  'fund': 'icon-bar-chart',
  'gateway': 'icon-cloud',
  'guide': 'icon-compass',
  'inbox': 'icon-archive',
  'key': 'icon-lock',
  'line-chart': 'icon-bar-chart',
  'peoples': 'icon-user-group',
  'profile': 'icon-idcard',
  'radio': 'icon-record',
  'rate': 'icon-star',
  'redis': 'icon-storage',
  'team': 'icon-user-group',
  'tree': 'icon-mind-mapping',
  'youtube': 'icon-live-broadcast'
}

const DIRECT_ARCO_ICONS = new Set([
  'bug',
  'cloud',
  'code',
  'dashboard',
  'edit',
  'experiment',
  'export',
  'file',
  'folder',
  'layout',
  'link',
  'phone',
  'play-circle',
  'robot',
  'swap',
  'thunderbolt',
  'unordered-list',
  'user',
  'video-camera',
  'wifi'
])

const ARCO_COMPONENT_RE = /^Icon[A-Z]/

export function resolveMenuIcon(icon?: string): ResolvedMenuIcon {
  const value = String(icon || '').trim()
  if (!value || value === '#') return { kind: 'none', value: '' }
  // The old dashboard entry used a WPS document icon (`file-wps`).
  // Keep the database value compatible while rendering the semantic Arco dashboard icon.
  if (value === 'file-wps') return { kind: 'arco', value: 'icon-dashboard' }
  if (value.startsWith('custom:') || value.startsWith('icon-park-outline:')) {
    return { kind: 'iconify', value }
  }
  if (CUSTOM_ICON_NAMES.has(value)) {
    return { kind: 'iconify', value: `custom:${value}` }
  }
  if (value.includes(':')) return { kind: 'iconify', value }
  if (ARCO_COMPONENT_RE.test(value) || value.startsWith('icon-')) {
    return { kind: 'arco', value }
  }
  const legacyIcon = LEGACY_ANT_ICON_MAP[value]
  if (legacyIcon) return { kind: 'arco', value: legacyIcon }
  if (DIRECT_ARCO_ICONS.has(value)) return { kind: 'arco', value: `icon-${value}` }
  return { kind: 'arco', value: 'icon-apps' }
}
