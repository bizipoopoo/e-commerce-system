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
