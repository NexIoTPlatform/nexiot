const CROP_SIZE = 360

export function useAvatarCropper() {
  const canvasRef = ref<HTMLCanvasElement | null>(null)
  const image = ref<HTMLImageElement | null>(null)
  const zoom = ref(1)
  const rotation = ref(0)
  const offset = reactive({ x: 0, y: 0 })
  const pointer = reactive({ dragging: false, x: 0, y: 0 })

  function dimensions() {
    if (!image.value) return { width: 0, height: 0 }
    const quarterTurn = Math.abs(rotation.value / 90) % 2 === 1
    return quarterTurn
      ? { width: image.value.naturalHeight, height: image.value.naturalWidth }
      : { width: image.value.naturalWidth, height: image.value.naturalHeight }
  }

  function scale() {
    const { width, height } = dimensions()
    return width && height ? Math.max(CROP_SIZE / width, CROP_SIZE / height) * zoom.value : 1
  }

  function clampOffset() {
    const { width, height } = dimensions()
    const currentScale = scale()
    const maxX = Math.max(0, (width * currentScale - CROP_SIZE) / 2)
    const maxY = Math.max(0, (height * currentScale - CROP_SIZE) / 2)
    offset.x = Math.min(maxX, Math.max(-maxX, offset.x))
    offset.y = Math.min(maxY, Math.max(-maxY, offset.y))
  }

  function render() {
    const canvas = canvasRef.value
    const source = image.value
    if (!canvas || !source) return
    clampOffset()
    const context = canvas.getContext('2d')
    if (!context) return
    context.clearRect(0, 0, CROP_SIZE, CROP_SIZE)
    context.save()
    context.translate(CROP_SIZE / 2 + offset.x, CROP_SIZE / 2 + offset.y)
    context.rotate(rotation.value * Math.PI / 180)
    const currentScale = scale()
    context.scale(currentScale, currentScale)
    context.drawImage(source, -source.naturalWidth / 2, -source.naturalHeight / 2)
    context.restore()
  }

  function load(file: File) {
    return new Promise<void>((resolve, reject) => {
      const url = URL.createObjectURL(file)
      const source = new Image()
      source.onload = () => {
        URL.revokeObjectURL(url)
        image.value = source
        resetTransform()
        resolve()
      }
      source.onerror = () => {
        URL.revokeObjectURL(url)
        reject(new Error('图片读取失败'))
      }
      source.src = url
    })
  }

  function resetTransform() {
    zoom.value = 1
    rotation.value = 0
    offset.x = 0
    offset.y = 0
    nextTick(render)
  }

  function clear() {
    image.value = null
    resetTransform()
    const context = canvasRef.value?.getContext('2d')
    context?.clearRect(0, 0, CROP_SIZE, CROP_SIZE)
  }

  function rotate(direction: -1 | 1) {
    rotation.value = (rotation.value + direction * 90 + 360) % 360
    offset.x = 0
    offset.y = 0
    nextTick(render)
  }

  function handlePointerDown(event: PointerEvent) {
    if (!image.value) return
    const rect = canvasRef.value?.getBoundingClientRect()
    if (!rect) return
    const ratio = CROP_SIZE / rect.width
    pointer.dragging = true
    pointer.x = (event.clientX - rect.left) * ratio - offset.x
    pointer.y = (event.clientY - rect.top) * ratio - offset.y
    canvasRef.value?.setPointerCapture(event.pointerId)
  }

  function handlePointerMove(event: PointerEvent) {
    if (!pointer.dragging) return
    const rect = canvasRef.value?.getBoundingClientRect()
    if (!rect) return
    const ratio = CROP_SIZE / rect.width
    offset.x = (event.clientX - rect.left) * ratio - pointer.x
    offset.y = (event.clientY - rect.top) * ratio - pointer.y
    render()
  }

  function handlePointerUp(event: PointerEvent) {
    pointer.dragging = false
    canvasRef.value?.releasePointerCapture(event.pointerId)
  }

  function toBlob() {
    return new Promise<Blob>((resolve, reject) => {
      canvasRef.value?.toBlob((blob) => {
        if (blob) resolve(blob)
        else reject(new Error('头像裁剪失败'))
      }, 'image/jpeg', 0.9)
    })
  }

  watch(zoom, render)

  return {
    canvasRef,
    hasImage: computed(() => Boolean(image.value)),
    zoom,
    load,
    clear,
    render,
    rotate,
    resetTransform,
    handlePointerDown,
    handlePointerMove,
    handlePointerUp,
    toBlob
  }
}
