import type { Component } from 'vue'

export interface ProtocolConfigurationField {
  key: string
  label: string
  type: 'string' | 'int' | 'boolean' | 'select' | 'json' | 'password'
  placeholder?: string
  help?: string
  options?: { label: string, value: string | number | boolean }[]
  sensitive?: boolean
}

export interface ProtocolConfigurationPlugin<T extends Record<string, unknown> = Record<string, unknown>> {
  protocol: string
  fields: ProtocolConfigurationField[]
  component: Component
  createModel: (configuration: unknown) => T
  merge: (base: unknown, model: T) => Record<string, unknown>
  validate?: (model: T, editing: boolean) => string
}

const plugins: Record<string, ProtocolConfigurationPlugin> = {}

export function registerProtocolConfiguration(plugin: ProtocolConfigurationPlugin) {
  plugins[plugin.protocol.toLowerCase()] = plugin
  return plugin
}

export function getProtocolConfigurationPlugin(protocol?: string) {
  return protocol ? plugins[protocol.toLowerCase()] : undefined
}

