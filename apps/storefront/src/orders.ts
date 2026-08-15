import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  ApiError,
  apiRequest,
  type CustomerOrder,
  type PaymentOrder,
  type Shipment,
} from './api'
import { useSessionStore } from './session'

export interface CreateOrderInput {
  receiverName: string
  receiverPhone: string
  addressLine: string
  customerNote: string
}

export const useOrderStore = defineStore('orders', () => {
  const session = useSessionStore()
  const data = ref<CustomerOrder[]>([])
  const active = ref<CustomerOrder | null>(null)
  const payment = ref<PaymentOrder | null>(null)
  const shipment = ref<Shipment | null>(null)
  const drawerOpen = ref(false)
  const checkoutOpen = ref(false)
  const paymentOpen = ref(false)
  const loading = ref(false)
  const error = ref('')
  const checkoutKey = ref<string | null>(null)

  async function load() {
    await run(async () => {
      data.value = await authorizedRequest<CustomerOrder[]>('/orders')
      if (active.value) {
        active.value = data.value.find(order => order.orderNo === active.value?.orderNo) ?? active.value
      }
    })
  }

  function beginCheckout() {
    checkoutKey.value ??= createIdempotencyKey()
    checkoutOpen.value = true
    error.value = ''
  }

  async function create(input: CreateOrderInput) {
    checkoutKey.value ??= createIdempotencyKey()
    await run(async () => {
      active.value = await authorizedRequest<CustomerOrder>('/orders', {
        method: 'POST',
        headers: { 'Idempotency-Key': checkoutKey.value! },
        body: JSON.stringify(input),
      })
      checkoutKey.value = null
      checkoutOpen.value = false
      payment.value = null
      paymentOpen.value = true
      data.value = [active.value, ...data.value.filter(order => order.orderNo !== active.value?.orderNo)]
    })
  }

  async function pay() {
    if (!active.value) return
    await run(async () => {
      payment.value = await authorizedRequest<PaymentOrder>('/payments', {
        method: 'POST',
        body: JSON.stringify({ orderNo: active.value!.orderNo, channel: 'MOCK' }),
      })
      payment.value = await authorizedRequest<PaymentOrder>(`/payments/mock/${payment.value.paymentNo}/complete`, {
        method: 'POST',
      })
      active.value = await authorizedRequest<CustomerOrder>(`/orders/${active.value!.orderNo}`)
      data.value = data.value.map(order => order.orderNo === active.value?.orderNo ? active.value! : order)
    })
  }

  async function cancel(orderNo: string) {
    await run(async () => {
      active.value = await authorizedRequest<CustomerOrder>(`/orders/${orderNo}/cancel`, { method: 'POST' })
      data.value = data.value.map(order => order.orderNo === orderNo ? active.value! : order)
    })
  }

  async function confirmReceipt(orderNo: string) {
    await run(async () => {
      active.value = await authorizedRequest<CustomerOrder>(`/orders/${orderNo}/confirm-receipt`, { method: 'POST' })
      data.value = data.value.map(order => order.orderNo === orderNo ? active.value! : order)
      await loadShipment(orderNo)
    })
  }

  async function select(order: CustomerOrder) {
    active.value = order
    shipment.value = null
    if (order.status === 'SHIPPED' || order.status === 'COMPLETED') {
      await run(async () => loadShipment(order.orderNo))
    }
  }

  async function loadShipment(orderNo: string) {
    shipment.value = await authorizedRequest<Shipment>(`/shipments/${orderNo}`)
  }

  function reset() {
    data.value = []
    active.value = null
    payment.value = null
    shipment.value = null
    drawerOpen.value = false
    checkoutOpen.value = false
    paymentOpen.value = false
    error.value = ''
    checkoutKey.value = null
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
      error.value = caught instanceof Error ? caught.message : '订单操作失败，请稍后再试。'
      throw caught
    } finally {
      loading.value = false
    }
  }

  return {
    data, active, payment, shipment, drawerOpen, checkoutOpen, paymentOpen, loading, error,
    load, beginCheckout, create, pay, cancel, confirmReceipt, select, reset,
  }
})

function createIdempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `order-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
