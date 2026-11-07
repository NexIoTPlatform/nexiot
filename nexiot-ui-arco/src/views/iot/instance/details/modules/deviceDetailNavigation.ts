export function normalizeDeviceDetailTab(value: unknown) {
  const tab = Array.isArray(value) ? value[0] : value
  if (tab === 'sip') return 'commands'
  return typeof tab === 'string' && tab ? tab : 'overview'
}
