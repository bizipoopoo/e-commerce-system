import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ApiError, apiRequest, type CartData, type CheckoutPreview } from './api'
import { useSessionStore } from './session'

const emptyCart = (): CartData => ({
  items: [],
  totalQuantity: 0,
  selectedQuantity: 0,
  selectedAmount: 0,
})

export const useCartStore = defineStore('cart', () => {
  const session = useSessionStore()
  const data = ref<CartData>(emptyCart())
  const checkout = ref<CheckoutPreview | null>(null)
  const drawerOpen = ref(false)
  const loading = ref(false)
  const error = ref('')

  async function load() {
    if (!session.accessToken) {
      reset()
      return
    }
    await run(async () => {
      data.value = await authorizedRequest<CartData>('/cart')
    })
  }

  async function add(skuId: number, quantity = 1) {
    await run(async () => {
      data.value = await authorizedRequest<CartData>('/cart/items', {
        method: 'POST',
        body: JSON.stringify({ skuId, quantity }),
      })
      drawerOpen.value = true
      checkout.value = null
    })
  }

  async function update(itemId: number, input: { quantity?: number; selected?: boolean }) {
    await run(async () => {
      data.value = await authorizedRequest<CartData>(`/cart/items/${itemId}`, {
        method: 'PATCH',
        body: JSON.stringify(input),
      })
      checkout.value = null
    })
  }

  async function remove(itemId: number) {
    await run(async () => {
      data.value = await authorizedRequest<CartData>(`/cart/items/${itemId}`, { method: 'DELETE' })
      checkout.value = null
    })
  }

  async function previewCheckout() {
    await run(async () => {
      checkout.value = await authorizedRequest<CheckoutPreview>('/checkout/preview', { method: 'POST' })
    })
  }

  function reset() {
    data.value = emptyCart()
    checkout.value = null
    drawerOpen.value = false
    error.value = ''
  }

  async function authorizedRequest<T>(path: string, init: RequestInit = {}) {
    if (!session.accessToken) throw new Error('需要登录')
    return apiRequest<T>(path, {
      ...init,
      headers: { ...init.headers, Authorization: `Bearer ${session.accessToken}` },
    })
  }

  async function run(action: () => Promise<void>) {
    loading.value = true
    error.value = ''
    try {
      await action()
    } catch (caught) {
      if (caught instanceof ApiError && caught.code === 'UNAUTHORIZED') {
        session.logout()
        reset()
      }
      error.value = caught instanceof Error ? caught.message : '购物车操作失败，请稍后再试。'
      throw caught
    } finally {
      loading.value = false
    }
  }

  return { data, checkout, drawerOpen, loading, error, load, add, update, remove, previewCheckout, reset }
})
