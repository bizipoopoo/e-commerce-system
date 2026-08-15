export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface AuthResult {
  accessToken: string
  user: { id: number; email: string; displayName: string; role: string }
}

export interface OrderItem {
  id: number
  productName: string
  quantity: number
  payableAmount: number
}

export interface Order {
  orderNo: string
  status: string
  items: OrderItem[]
  payableAmount: number
  receiverName: string
  receiverPhone: string
  createdAt: string
}

export interface Dashboard {
  grossMerchandiseValue: number
  paidOrderCount: number
  pendingShipmentCount: number
  completedOrderCount: number
  pendingAfterSaleCount: number
  warningStockCount: number
  latestOrders: Order[]
}

export interface AfterSale {
  afterSaleNo: string
  orderNo: string
  userId: number
  type: string
  status: string
  reasonCode: string
  description: string
  refundAmount: number
  returnCarrier: string | null
  returnTrackingNo: string | null
  adminNote: string | null
  createdAt: string
}

export interface Inventory {
  skuId: number
  totalQuantity: number
  reservedQuantity: number
  availableQuantity: number
  warningQuantity: number
  warning: boolean
}

export interface Coupon {
  couponId: number
  code: string
  name: string
  discountAmount: number
  thresholdAmount: number
  status: string
  startsAt: string
  endsAt: string
}

export interface Article {
  id: number
  slug: string
  title: string
  channelCode: string
  status: 'DRAFT' | 'PUBLISHED'
  featured: boolean
  publishedAt: string | null
}

export interface Review {
  id: number
  userId: number
  productId: number
  rating: number
  content: string
  status: 'PUBLISHED' | 'HIDDEN'
  adminReply: string | null
  createdAt: string
}

export class ApiError extends Error {
  constructor(public code: string, message: string, public status: number) {
    super(message)
  }
}

export async function apiRequest<T>(path: string, token?: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok) throw new ApiError(payload.code, payload.message, response.status)
  return payload.data
}
