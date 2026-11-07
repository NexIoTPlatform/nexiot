/** Copy text in secure contexts and fall back to the legacy API on HTTP pages. */
export async function copyText(text: string): Promise<void> {
  if (typeof navigator !== 'undefined' && typeof navigator.clipboard?.writeText === 'function') {
    try {
      await navigator.clipboard.writeText(text)
      return
    } catch {
      // Clipboard API can reject when the page is not a secure context.
    }
  }

  if (typeof document === 'undefined' || typeof document.execCommand !== 'function') {
    throw new TypeError('当前浏览器不支持复制到剪贴板')
  }

  const input = document.createElement('textarea')
  input.value = text
  input.setAttribute('readonly', '')
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  const copied = document.execCommand('copy')
  input.remove()
  if (!copied) throw new TypeError('当前浏览器不支持复制到剪贴板')
}
