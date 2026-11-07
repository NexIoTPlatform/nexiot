import type { DeviceItem } from '@/apis/iot/device'

export const MAX_COMPARE_DEVICES = 6

export function canAddCompareDevice(current: DeviceItem, selected: DeviceItem[], candidate: DeviceItem) {
  return candidate.productKey === current.productKey && !!candidate.iotId
    && selected.length < MAX_COMPARE_DEVICES && !selected.some((device) => String(device.id) === String(candidate.id))
}

export async function runCompareQueries<T>(items: T[], task: (item: T) => Promise<void>, signal: AbortSignal) {
  let cursor = 0
  async function worker() {
    while (!signal.aborted && cursor < items.length) {
      const item = items[cursor++]
      await task(item)
    }
  }
  await Promise.all(Array.from({ length: Math.min(2, items.length) }, worker))
}
