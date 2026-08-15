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
