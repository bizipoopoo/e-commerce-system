export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId: string
}

export interface Category {
  id: number
  parentId: number | null
  name: string
  slug: string
  icon: string
}

export interface ProductCard {
  id: number
  defaultSkuId: number | null
  name: string
  subtitle: string
  coverImageUrl: string
  salePrice: number
  marketPrice: number
  rating: number
  salesCount: number
  categoryName: string
  brandName: string
}

export interface Banner {
  id: number
  title: string
  subtitle: string
  imageUrl: string
  linkUrl: string
}

export interface HomeData {
  heroBanners: Banner[]
  featureBanners: Banner[]
  categories: Category[]
  featuredProducts: ProductCard[]
  newArrivals: ProductCard[]
}

export interface User {
  id: number
  email: string
  displayName: string
  role: 'CUSTOMER' | 'ADMIN'
}

export interface AuthResult {
  accessToken: string
  expiresIn: number
  user: User
}

export interface CartItem {
  id: number
  skuId: number
  productId: number | null
  productName: string
  skuName: string
  imageUrl: string
  unitPrice: number
  quantity: number
  subtotal: number
  selected: boolean
  available: boolean
  availableQuantity: number
}

export interface CartData {
  items: CartItem[]
  totalQuantity: number
  selectedQuantity: number
  selectedAmount: number
}

export interface CheckoutPreview {
  items: Array<{
    cartItemId: number
    skuId: number
    productId: number
    productName: string
    skuName: string
    imageUrl: string
    unitPrice: number
    quantity: number
    subtotal: number
  }>
  goodsAmount: number
  discountAmount: number
  shippingAmount: number
  payableAmount: number
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'REFUNDING' | 'REFUNDED' | 'CLOSED'

export interface OrderItem {
  id: number
  productId: number
  skuId: number
  productName: string
  skuName: string
  imageUrl: string
  unitPrice: number
  quantity: number
  discountAmount: number
  payableAmount: number
}

export interface OrderTimeline {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  operatorType: string
  remark: string
  createdAt: string
}

export interface CustomerOrder {
  orderNo: string
  status: OrderStatus
  items: OrderItem[]
  goodsAmount: number
  discountAmount: number
  shippingAmount: number
  payableAmount: number
  receiverName: string
  receiverPhone: string
  addressLine: string
  customerNote: string | null
  expireAt: string
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  closedAt: string | null
  refundedAt: string | null
  createdAt: string
  timeline: OrderTimeline[]
}

export interface PaymentOrder {
  paymentNo: string
  orderNo: string
  status: 'PENDING' | 'SUCCESS'
  channel: string
  amount: number
  providerTradeNo: string | null
  paidAt: string | null
  createdAt: string
}

export interface Shipment {
  shipmentNo: string
  orderNo: string
  status: 'IN_TRANSIT' | 'DELIVERED'
  carrier: string
  trackingNo: string
  shippedAt: string
  deliveredAt: string | null
  tracks: Array<{ description: string; occurredAt: string }>
}

export interface Coupon {
  couponId: number
  userCouponId: number | null
  code: string
  name: string
  description: string
  discountAmount: number
  thresholdAmount: number
  status: 'UNCLAIMED' | 'AVAILABLE' | 'LOCKED' | 'USED' | 'EXPIRED'
  startsAt: string
  endsAt: string
}

export interface ContentArticle {
  id: number
  slug: string
  title: string
  summary: string
  coverImageUrl: string
  contentText: string
  channelCode: string
  status: 'DRAFT' | 'PUBLISHED'
  featured: boolean
  publishedAt: string
}

export interface Recommendation {
  productId: number
  defaultSkuId: number
  productName: string
  subtitle: string
  imageUrl: string
  salePrice: number
  categoryName: string
  reason: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  referenceType: string | null
  referenceNo: string | null
  readAt: string | null
  createdAt: string
}

export interface NotificationCenter {
  items: NotificationItem[]
  unreadCount: number
}

export interface FavoriteItem {
  productId: number
  defaultSkuId: number | null
  productName: string
  imageUrl: string
  salePrice: number
  createdAt: string
}

export type AfterSaleStatus = 'PENDING_REVIEW' | 'APPROVED' | 'WAITING_RETURN' | 'RETURNED' | 'REJECTED' | 'REFUNDED'

export interface AfterSaleItem {
  afterSaleNo: string
  orderNo: string
  userId: number
  type: 'REFUND_ONLY' | 'RETURN_REFUND'
  sourceOrderStatus: OrderStatus
  status: AfterSaleStatus
  reasonCode: string
  description: string
  refundAmount: number
  returnCarrier: string | null
  returnTrackingNo: string | null
  adminNote: string | null
  reviewedAt: string | null
  returnedAt: string | null
  refundedAt: string | null
  createdAt: string
  refundNo: string | null
  timeline: Array<{
    fromStatus: AfterSaleStatus | null
    toStatus: AfterSaleStatus
    operatorType: string
    remark: string
    createdAt: string
  }>
}

export interface ReviewItem {
  id: number
  userId: number
  orderItemId: number
  productId: number
  rating: number
  content: string
  imageUrls: string | null
  status: 'PUBLISHED' | 'HIDDEN'
  adminReply: string | null
  repliedAt: string | null
  createdAt: string
}

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number,
  ) {
    super(message)
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers,
    },
  })
  const rawBody = await response.text()
  let payload: ApiResponse<T>
  try {
    payload = JSON.parse(rawBody) as ApiResponse<T>
  } catch {
    throw new ApiError(
      'INVALID_SERVER_RESPONSE',
      response.ok ? '服务返回了无法解析的数据' : '服务暂时不可用，请稍后再试',
      response.status,
    )
  }
  if (!response.ok) {
    throw new ApiError(payload.code, payload.message, response.status)
  }
  return payload.data
}

export function getHome(): Promise<HomeData> {
  return apiRequest<HomeData>('/home')
}
