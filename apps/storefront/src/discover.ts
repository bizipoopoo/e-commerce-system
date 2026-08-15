import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  ApiError,
  apiRequest,
  type ContentArticle,
  type Coupon,
  type NotificationCenter,
  type NotificationItem,
  type Recommendation,
} from './api'
import { useSessionStore } from './session'

export const useDiscoverStore = defineStore('discover', () => {
  const session = useSessionStore()
  const articles = ref<ContentArticle[]>([])
  const recommendations = ref<Recommendation[]>([])
  const coupons = ref<Coupon[]>([])
  const myCoupons = ref<Coupon[]>([])
  const notifications = ref<NotificationItem[]>([])
  const unreadCount = ref(0)
  const activeArticle = ref<ContentArticle | null>(null)
  const discoverOpen = ref(false)
  const messagesOpen = ref(false)
  const loading = ref(false)
  const error = ref('')
  let streamAbort: AbortController | null = null

  async function loadDiscover() {
    await run(async () => {
      const headers = session.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : undefined
      const [articleData, recommendationData] = await Promise.all([
        apiRequest<ContentArticle[]>('/content/feed'),
        apiRequest<Recommendation[]>('/recommendations', { headers }),
      ])
      articles.value = articleData
      recommendations.value = recommendationData
      if (session.accessToken) await loadCouponsInternal()
    })
  }

  async function loadCoupons() {
    if (!session.accessToken) return
    await run(loadCouponsInternal)
  }

  async function loadCouponsInternal() {
    const [available, mine] = await Promise.all([
      authorizedRequest<Coupon[]>('/coupons'),
      authorizedRequest<Coupon[]>('/me/coupons'),
    ])
    coupons.value = available
    myCoupons.value = mine
  }

  async function claim(couponId: number) {
    await run(async () => {
      await authorizedRequest<Coupon>(`/coupons/${couponId}/claim`, { method: 'POST' })
      await loadCouponsInternal()
      await loadNotificationsInternal()
    })
  }

  async function record(eventType: string, productId: number) {
    if (!session.accessToken) return
    try {
      await authorizedRequest<void>('/behaviors', {
        method: 'POST',
        body: JSON.stringify({ eventType, targetType: 'PRODUCT', targetId: productId }),
      })
    } catch {
      // Behavior collection must never block the shopping flow.
    }
  }

  async function loadNotifications() {
    if (!session.accessToken) return
    await run(loadNotificationsInternal)
  }

  async function loadNotificationsInternal() {
    const center = await authorizedRequest<NotificationCenter>('/notifications')
    notifications.value = center.items
    unreadCount.value = center.unreadCount
  }

  async function markRead(item: NotificationItem) {
    if (item.readAt) return
    await run(async () => {
      const updated = await authorizedRequest<NotificationItem>(`/notifications/${item.id}/read`, {
        method: 'PATCH',
      })
      notifications.value = notifications.value.map(current => current.id === updated.id ? updated : current)
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    })
  }

  function connectNotifications() {
    disconnectNotifications()
    if (!session.accessToken) return
    streamAbort = new AbortController()
    void consumeStreamLoop(streamAbort.signal)
  }

  function disconnectNotifications() {
    streamAbort?.abort()
    streamAbort = null
  }

  async function consumeStreamLoop(signal: AbortSignal) {
    let retryDelay = 1_000
    while (!signal.aborted) {
      try {
        await loadNotificationsInternal()
        await consumeStream(signal)
        retryDelay = 1_000
      } catch (caught) {
        if (signal.aborted || (caught instanceof DOMException && caught.name === 'AbortError')) return
      }
      if (signal.aborted) return
      await waitForRetry(retryDelay, signal)
      retryDelay = Math.min(retryDelay * 2, 30_000)
    }
  }

  function waitForRetry(delay: number, signal: AbortSignal) {
    return new Promise<void>(resolve => {
      const timeout = window.setTimeout(done, delay)
      signal.addEventListener('abort', done, { once: true })
      function done() {
        window.clearTimeout(timeout)
        signal.removeEventListener('abort', done)
        resolve()
      }
    })
  }

  async function consumeStream(signal: AbortSignal) {
    const response = await fetch('/api/v1/notifications/stream', {
      headers: { Authorization: `Bearer ${session.accessToken}` },
      signal,
    })
    if (!response.ok || !response.body) throw new Error('实时消息连接失败')
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (!signal.aborted) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() ?? ''
      frames.forEach(frame => {
        if (!frame.includes('event:notification')) return
        const data = frame.split(/\r?\n/).find(line => line.startsWith('data:'))?.slice(5)
        if (!data) return
        const notification = JSON.parse(data) as NotificationItem
        if (!notifications.value.some(item => item.id === notification.id)) {
          notifications.value.unshift(notification)
          unreadCount.value += 1
        }
      })
    }
  }

  function reset() {
    disconnectNotifications()
    coupons.value = []
    myCoupons.value = []
    notifications.value = []
    unreadCount.value = 0
    discoverOpen.value = false
    messagesOpen.value = false
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
      error.value = caught instanceof Error ? caught.message : '内容加载失败，请稍后再试。'
      throw caught
    } finally {
      loading.value = false
    }
  }

  return {
    articles, recommendations, coupons, myCoupons, notifications, unreadCount, activeArticle,
    discoverOpen, messagesOpen, loading, error, loadDiscover, loadCoupons, claim, record,
    loadNotifications, markRead, connectNotifications, disconnectNotifications, reset,
  }
})
