import type { Component } from 'vue'
import type { DeviceItem } from '@/apis/iot/device'
import type { DeviceCustomField } from '@/views/iot/instance/modules/deviceCustomFields'

export interface ProtocolDeviceAccessPlugin {
  protocol: string
  fields: DeviceCustomField[]
  guide?: Component
  normalize?: (configuration: Record<string, unknown>, device: DeviceItem) => Record<string, unknown>
}

const plugins: Record<string, ProtocolDeviceAccessPlugin> = {}

export function registerProtocolDeviceAccess(plugin: ProtocolDeviceAccessPlugin) {
  plugins[plugin.protocol.toLowerCase()] = plugin
  return plugin
}

export function getProtocolDeviceAccess(protocol?: string) {
  return protocol ? plugins[protocol.toLowerCase()] : undefined
}

