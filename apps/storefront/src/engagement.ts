import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  ApiError,
  apiRequest,
  type AfterSaleItem,
  type FavoriteItem,
  type ReviewItem,
} from './api'
import { useSessionStore } from './session'

export const useEngagementStore = defineStore('engagement', () => {
  const session = useSessionStore()
  const favorites = ref<FavoriteItem[]>([])
  const afterSales = ref<AfterSaleItem[]>([])
  const reviews = ref<ReviewItem[]>([])
  const loading = ref(false)
  const error = ref('')

  async function load() {
    if (!session.accessToken) return
    await run(async () => {
      const [favoriteData, afterSaleData, reviewData] = await Promise.all([
        authorizedRequest<FavoriteItem[]>('/favorites'),
        authorizedRequest<AfterSaleItem[]>('/after-sales'),
        authorizedRequest<ReviewItem[]>('/me/reviews'),
      ])
      favorites.value = favoriteData
      afterSales.value = afterSaleData
      reviews.value = reviewData
    })
  }

  function isFavorite(productId: number) {
    return favorites.value.some(item => item.productId === productId)
  }

  async function toggleFavorite(productId: number) {
    await run(async () => {
      if (isFavorite(productId)) {
        await authorizedRequest<void>(`/favorites/${productId}`, { method: 'DELETE' })
        favorites.value = favorites.value.filter(item => item.productId !== productId)
      } else {
        const favorite = await authorizedRequest<FavoriteItem>(`/favorites/${productId}`, { method: 'POST' })
        favorites.value.unshift(favorite)
      }
    })
  }

  async function applyAfterSale(
    orderNo: string,
    type: 'REFUND_ONLY' | 'RETURN_REFUND',
    reasonCode: string,
    description: string,
  ) {
    await run(async () => {
      const item = await authorizedRequest<AfterSaleItem>('/after-sales', {
        method: 'POST',
        body: JSON.stringify({ orderNo, type, reasonCode, description }),
      })
      afterSales.value = [item, ...afterSales.value.filter(current => current.orderNo !== orderNo)]
    })
  }

  async function submitReturn(afterSaleNo: string, carrier: string, trackingNo: string) {
    await run(async () => {
      const updated = await authorizedRequest<AfterSaleItem>(`/after-sales/${afterSaleNo}/return`, {
        method: 'POST',
        body: JSON.stringify({ carrier, trackingNo }),
      })
      afterSales.value = afterSales.value.map(item => item.afterSaleNo === afterSaleNo ? updated : item)
    })
  }

  async function createReview(
    orderNo: string,
    orderItemId: number,
    rating: number,
    content: string,
  ) {
    await run(async () => {
      const review = await authorizedRequest<ReviewItem>('/reviews', {
        method: 'POST',
        body: JSON.stringify({ orderNo, orderItemId, rating, content, imageUrls: null }),
      })
      reviews.value = [review, ...reviews.value.filter(item => item.orderItemId !== orderItemId)]
    })
  }

  function afterSaleForOrder(orderNo: string) {
    return afterSales.value.find(item => item.orderNo === orderNo) ?? null
  }

  function reviewed(orderItemId: number) {
    return reviews.value.some(item => item.orderItemId === orderItemId)
  }

  function reset() {
    favorites.value = []
    afterSales.value = []
    reviews.value = []
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
      error.value = caught instanceof Error ? caught.message : '操作失败，请稍后再试。'
      throw caught
    } finally {
      loading.value = false
    }
  }

  return {
    favorites, afterSales, reviews, loading, error, load, isFavorite, toggleFavorite,
    applyAfterSale, submitReturn, createReview, afterSaleForOrder, reviewed, reset,
  }
})
