/**
 * Generate a UUID without requiring a secure browser context.
 *
 * `crypto.randomUUID()` is unavailable on HTTP pages in some browsers, while
 * `getRandomValues()` is broadly supported there. The final fallback keeps
 * the UI functional in older browsers that expose neither API.
 */
export function createUuid(): string {
  const cryptoApi = typeof globalThis !== 'undefined' ? globalThis.crypto : undefined

  if (typeof cryptoApi?.randomUUID === 'function') {
    try {
      return cryptoApi.randomUUID()
    } catch {
      // Some browsers expose randomUUID but throw when the page is not secure.
    }
  }

  if (typeof cryptoApi?.getRandomValues === 'function') {
    try {
      const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
      bytes[6] = (bytes[6] & 0x0F) | 0x40
      bytes[8] = (bytes[8] & 0x3F) | 0x80
      return formatUuidBytes(bytes)
    } catch {
      // Fall through to the non-cryptographic fallback.
    }
  }

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const random = Math.floor(Math.random() * 16)
    const value = character === 'x' ? random : (random & 0x3) | 0x8
    return value.toString(16)
  })
}

function formatUuidBytes(bytes: Uint8Array): string {
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
