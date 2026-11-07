import type { ProductItem } from '@/apis/iot/product'
import { useDebounceFn } from '@vueuse/core'
import { getProductList } from '@/apis/iot/product'

/** Keep selected products available while discovering products created after page load. */
export function useOtaProducts(initialProducts: () => ProductItem[]) {
  const discoveredProducts = ref<ProductItem[]>([])
  const productSearchLoading = ref(false)
  let requestId = 0
  const availableProducts = computed(() => {
    const items = new Map(initialProducts().map((item) => [item.productKey, item]))
    discoveredProducts.value.forEach((item) => items.set(item.productKey, item))
    return [...items.values()]
  })
  const productOptions = computed(() => availableProducts.value.map((item) => ({ label: `${item.name} (${item.productKey})`, value: item.productKey })))

  async function loadProducts(keyword = '') {
    const id = ++requestId
    const value = keyword.trim()
    productSearchLoading.value = true
    try {
      const params = { page: 1, size: 50 }
      const responses = await Promise.all(value
        ? [getProductList({ ...params, name: value }), getProductList({ ...params, productKey: value })]
        : [getProductList(params)])
      if (id !== requestId) return
      const items = new Map(discoveredProducts.value.map((item) => [item.productKey, item]))
      responses.forEach(({ data }) => data.records?.forEach((item) => items.set(item.productKey, item)))
      discoveredProducts.value = [...items.values()]
    } finally {
      if (id === requestId) productSearchLoading.value = false
    }
  }

  const searchProducts = useDebounceFn((value: string) => loadProducts(value).catch(() => {}), 250)
  function refreshProducts(visible: boolean) {
    if (visible) loadProducts().catch(() => {})
  }
  return { availableProducts, productOptions, productSearchLoading, searchProducts, refreshProducts }
}
