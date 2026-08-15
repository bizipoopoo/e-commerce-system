#!/usr/bin/env node

const baseUrl = (process.env.SMOKE_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')
const adminEmail = process.env.DEMO_ADMIN_EMAIL ?? 'admin@aurora.local'
const adminPassword = process.env.DEMO_ADMIN_PASSWORD ?? 'Aurora@2026'
const runId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

function step(message) {
  process.stdout.write(`✓ ${message}\n`)
}

async function request(path, { token, expectedStatus = 200, ...init } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  })
  const raw = await response.text()
  let payload
  try {
    payload = JSON.parse(raw)
  } catch {
    throw new Error(`${init.method ?? 'GET'} ${path} returned non-JSON (${response.status}): ${raw.slice(0, 160)}`)
  }
  if (response.status !== expectedStatus || (path.startsWith('/api/') && payload.code !== 'OK')) {
    throw new Error(`${init.method ?? 'GET'} ${path} failed (${response.status}): ${raw}`)
  }
  return path.startsWith('/api/') ? payload.data : payload
}

function post(path, body, token, headers = {}) {
  return request(path, { method: 'POST', body: JSON.stringify(body), token, headers })
}

try {
  const health = await request('/actuator/health')
  if (health.status !== 'UP') throw new Error(`Backend health is ${health.status}`)
  step('后端健康检查')

  const home = await request('/api/v1/home')
  const product = home.featuredProducts.find((item) => item.defaultSkuId) ?? home.newArrivals.find((item) => item.defaultSkuId)
  if (!product) throw new Error('No sellable demo product found')
  step(`首页与商品数据（${product.name}）`)

  const customer = await post('/api/v1/auth/register', {
    email: `smoke-${runId}@aurora.local`,
    password: 'Smoke@2026',
    displayName: '自动化验收用户',
  })
  const customerToken = customer.accessToken
  step('客户注册与 JWT 签发')

  await post(`/api/v1/favorites/${product.id}`, {}, customerToken)
  await post('/api/v1/cart/items', { skuId: product.defaultSkuId, quantity: 1 }, customerToken)
  const preview = await post('/api/v1/checkout/preview', {}, customerToken)
  if (preview.items.length !== 1 || Number(preview.payableAmount) <= 0) throw new Error('Checkout preview is invalid')
  step('收藏、购物车与结算预览')

  const order = await post('/api/v1/orders', {
    receiverName: 'Aurora 验收员',
    receiverPhone: '13800138000',
    addressLine: '上海市浦东新区演示路 7 号',
    customerNote: `Stage 07 smoke ${runId}`,
    userCouponId: null,
  }, customerToken, { 'Idempotency-Key': `smoke-order-${runId}` })
  const payment = await post('/api/v1/payments', { orderNo: order.orderNo, channel: 'MOCK' }, customerToken)
  await post(`/api/v1/payments/mock/${payment.paymentNo}/complete`, {}, customerToken)
  step(`下单与模拟支付（${order.orderNo}）`)

  const admin = await post('/api/v1/auth/login', { email: adminEmail, password: adminPassword })
  if (admin.user.role !== 'ADMIN') throw new Error('Configured demo account is not an administrator')
  const adminToken = admin.accessToken
  await post(`/api/v1/admin/orders/${order.orderNo}/ship`, {
    carrier: 'Aurora Express',
    trackingNo: `AUTO-${runId}`,
  }, adminToken)
  step('管理员登录与订单发货')

  await request(`/api/v1/shipments/${order.orderNo}`, { token: customerToken })
  const completed = await post(`/api/v1/orders/${order.orderNo}/confirm-receipt`, {}, customerToken)
  if (completed.status !== 'COMPLETED') throw new Error(`Order status is ${completed.status}`)
  await post('/api/v1/reviews', {
    orderNo: order.orderNo,
    orderItemId: completed.items[0].id,
    rating: 5,
    content: 'Stage 07 自动化回归：交易、履约与评价链路正常。',
    imageUrls: null,
  }, customerToken)
  step('物流查询、确认收货与真实购买评价')

  const dashboard = await request('/api/v1/admin/dashboard', { token: adminToken })
  if (dashboard.completedOrderCount < 1) throw new Error('Dashboard did not aggregate the completed order')
  step('运营看板聚合')
  process.stdout.write(`\nAurora Commerce smoke test passed via ${baseUrl}.\n`)
} catch (error) {
  process.stderr.write(`\nSmoke test failed: ${error instanceof Error ? error.message : String(error)}\n`)
  process.exitCode = 1
}
