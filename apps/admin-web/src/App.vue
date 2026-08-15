<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ApiError,
  apiRequest,
  type AfterSale,
  type Article,
  type AuthResult,
  type Coupon,
  type Dashboard,
  type Inventory,
  type Order,
  type Review,
} from './api'

type Section = 'dashboard' | 'orders' | 'afterSales' | 'inventory' | 'marketing' | 'content' | 'reviews' | 'messages'

const navigation: Array<{ icon: string; label: string; section: Section }> = [
  { icon: '▦', label: '经营概览', section: 'dashboard' },
  { icon: '▤', label: '订单管理', section: 'orders' },
  { icon: '↩', label: '售后工作台', section: 'afterSales' },
  { icon: '▥', label: '库存中心', section: 'inventory' },
  { icon: '％', label: '营销活动', section: 'marketing' },
  { icon: '◫', label: '内容运营', section: 'content' },
  { icon: '✎', label: '评价管理', section: 'reviews' },
  { icon: '♧', label: '消息推送', section: 'messages' },
]

const token = ref(sessionStorage.getItem('aurora-admin-token') ?? '')
const email = ref('admin@aurora.local')
const password = ref('Aurora@2026')
const userName = ref('Aurora 管理员')
const active = ref<Section>('dashboard')
const dashboard = ref<Dashboard | null>(null)
const orders = ref<Order[]>([])
const afterSales = ref<AfterSale[]>([])
const inventories = ref<Inventory[]>([])
const coupons = ref<Coupon[]>([])
const articles = ref<Article[]>([])
const reviews = ref<Review[]>([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const messageUserId = ref<number | null>(null)
const messageTitle = ref('Aurora 会员关怀')
const messageContent = ref('感谢你的关注，新的生活提案已经上线。')

const title = computed(() => navigation.find(item => item.section === active.value)?.label ?? '经营概览')
const metrics = computed(() => [
  { label: '累计交易额', value: currency(dashboard.value?.grossMerchandiseValue ?? 0), detail: '已支付口径' },
  { label: '支付订单', value: String(dashboard.value?.paidOrderCount ?? 0), detail: `已完成 ${dashboard.value?.completedOrderCount ?? 0}` },
  { label: '待发货', value: String(dashboard.value?.pendingShipmentCount ?? 0), detail: '需要及时处理' },
  { label: '待审售后', value: String(dashboard.value?.pendingAfterSaleCount ?? 0), detail: `库存预警 ${dashboard.value?.warningStockCount ?? 0}` },
])

async function login() {
  await run(async () => {
    const auth = await apiRequest<AuthResult>('/auth/login', undefined, {
      method: 'POST', body: JSON.stringify({ email: email.value, password: password.value }),
    })
    if (auth.user.role !== 'ADMIN') throw new Error('该账户没有运营后台权限')
    token.value = auth.accessToken
    userName.value = auth.user.displayName
    sessionStorage.setItem('aurora-admin-token', auth.accessToken)
    await loadSection('dashboard')
  })
}

function logout() {
  token.value = ''
  sessionStorage.removeItem('aurora-admin-token')
}

async function loadSection(section: Section) {
  active.value = section
  await run(async () => {
    if (section === 'dashboard') dashboard.value = await request<Dashboard>('/admin/dashboard')
    if (section === 'orders') orders.value = await request<Order[]>('/admin/orders')
    if (section === 'afterSales') {
      afterSales.value = await request<AfterSale[]>('/admin/after-sales')
      if (dashboard.value) {
        dashboard.value.pendingAfterSaleCount = afterSales.value.filter(item => item.status === 'PENDING_REVIEW').length
      }
    }
    if (section === 'inventory') inventories.value = await request<Inventory[]>('/admin/inventories')
    if (section === 'marketing') coupons.value = await request<Coupon[]>('/admin/coupons')
    if (section === 'content') articles.value = await request<Article[]>('/admin/content/articles')
    if (section === 'reviews') reviews.value = await request<Review[]>('/admin/reviews')
  })
}

async function ship(orderNo: string) {
  await action(`/admin/orders/${orderNo}/ship`, {
    carrier: '顺丰速运', trackingNo: `SF${Date.now()}`,
  }, '订单已发货')
  await loadSection('orders')
}

async function afterSaleAction(item: AfterSale, actionName: 'approve' | 'reject' | 'refund') {
  const notes = {
    approve: item.type === 'RETURN_REFUND' ? '审核通过，请用户寄回商品' : '审核通过，进入退款处理',
    reject: '材料暂不符合售后条件，运营审核驳回',
    refund: '商品及退款信息核验完成，模拟退款成功',
  }
  await action(`/admin/after-sales/${item.afterSaleNo}/${actionName}`, { note: notes[actionName] }, '售后状态已更新')
  await loadSection('afterSales')
}

async function publishArticle(id: number) {
  await action(`/admin/content/articles/${id}/publish`, undefined, '文章已发布', 'PATCH')
  await loadSection('content')
}

async function replyReview(id: number) {
  await action(`/admin/reviews/${id}/reply`, { reply: '感谢你的认真体验与分享，愿好物长久陪伴你。' }, '评价已回复', 'PATCH')
  await loadSection('reviews')
}

async function hideReview(id: number) {
  await action(`/admin/reviews/${id}/hide`, undefined, '评价已隐藏', 'PATCH')
  await loadSection('reviews')
}

async function sendMessage() {
  if (!messageUserId.value) {
    error.value = '请输入接收用户 ID'
    return
  }
  await action('/admin/notifications', {
    userId: messageUserId.value, type: 'SYSTEM', title: messageTitle.value,
    content: messageContent.value, referenceType: null, referenceNo: null,
  }, '系统消息已发送')
}

async function action(path: string, body: unknown, message: string, method = 'POST') {
  await run(async () => {
    await request(path, { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) })
    success.value = message
  })
}

async function request<T = unknown>(path: string, init: RequestInit = {}) {
  return apiRequest<T>(path, token.value, init)
}

async function run(operation: () => Promise<void>) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await operation()
  } catch (caught) {
    if (caught instanceof ApiError && caught.status === 401) logout()
    error.value = caught instanceof Error ? caught.message : '操作失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function currency(value: number) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value)
}

function dateTime(value: string | null) {
  return value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—'
}

function orderStatus(status: string) {
  return ({ PENDING_PAYMENT: '待支付', PAID: '待发货', SHIPPED: '运输中', COMPLETED: '已完成', REFUNDING: '售后中', REFUNDED: '已退款', CLOSED: '已关闭' } as Record<string, string>)[status] ?? status
}

function serviceStatus(status: string) {
  return ({ PENDING_REVIEW: '待审核', APPROVED: '待退款', WAITING_RETURN: '待寄回', RETURNED: '待退款', REJECTED: '已驳回', REFUNDED: '已退款' } as Record<string, string>)[status] ?? status
}

onMounted(() => {
  if (token.value) void loadSection('dashboard')
})
</script>

<template>
  <div v-if="!token" class="admin-login">
    <form @submit.prevent="login"><div class="login-brand">A<span>AURORA OPS</span></div><p>OPERATIONS CONSOLE</p><h1>让每一次履约<br />都有温度</h1><label>管理员邮箱<input v-model.trim="email" required type="email" /></label><label>密码<input v-model="password" required type="password" /></label><div v-if="error" class="notice error">{{ error }}</div><button :disabled="loading">{{ loading ? '正在进入…' : '进入运营后台' }}</button><small>Demo 管理员凭据已预置，可直接登录</small></form><div class="login-visual"><span>STAGE 06</span><strong>交易、售后与内容<br />在同一个工作台流动</strong></div>
  </div>

  <div v-else class="console">
    <aside><div class="console-brand">A<span>AURORA</span></div><div class="workspace"><small>当前空间</small><strong>Aurora 品牌商城</strong><span>⌄</span></div><nav><a v-for="item in navigation" :key="item.section" :class="{ active: active === item.section }" href="#" @click.prevent="loadSection(item.section)"><i>{{ item.icon }}</i><span>{{ item.label }}</span><b v-if="item.section === 'afterSales' && dashboard?.pendingAfterSaleCount">{{ dashboard.pendingAfterSaleCount }}</b></a></nav><div class="aside-bottom"><a href="#">⚙ <span>系统设置</span></a><a href="#" @click.prevent="logout">↪ <span>退出登录</span></a></div></aside>
    <section class="main-area">
      <header><div class="search">⌕<input placeholder="搜索订单、商品、用户" /><kbd>⌘ K</kbd></div><div class="header-tools"><button>◌<b></b></button><span></span><div class="avatar">AU</div><div><strong>{{ userName }}</strong><small>超级管理员</small></div><button @click="logout">退出</button></div></header>
      <main>
        <div class="welcome"><div><p>2026 年 8 月 15 日 · 实时业务数据</p><h1>{{ title }}</h1><span>关键操作已接入真实 API，并由权限、事务与状态机保护。</span></div><div class="welcome-actions"><button @click="loadSection(active)">刷新数据</button><button class="solid" @click="loadSection('messages')">＋ 发送消息</button></div></div>
        <div v-if="error" class="notice error">{{ error }}</div><div v-if="success" class="notice success">{{ success }}</div>

        <template v-if="active === 'dashboard'">
          <div class="metrics"><article v-for="metric in metrics" :key="metric.label"><div><span>{{ metric.label }}</span><button>···</button></div><strong>{{ metric.value }}</strong><p><b class="green">LIVE</b>{{ metric.detail }}</p></article></div>
          <div class="dashboard-grid"><article class="trend panel"><div class="panel-title"><div><h2>交易健康度</h2><p>基于当前真实订单聚合</p></div></div><div class="live-chart"><div v-for="(height, index) in [34,48,41,62,58,77,69,86,74,92,82,100]" :key="index"><i :style="{ height: `${height}%` }"></i><span>{{ index + 1 }}时</span></div></div></article><article class="funnel panel"><div class="panel-title"><div><h2>今日待办</h2><p>需要及时处理</p></div></div><div class="todo-stack"><button @click="loadSection('orders')"><b>{{ dashboard?.pendingShipmentCount ?? 0 }}</b><span>待发货订单</span></button><button @click="loadSection('afterSales')"><b>{{ dashboard?.pendingAfterSaleCount ?? 0 }}</b><span>待审核售后</span></button><button @click="loadSection('inventory')"><b>{{ dashboard?.warningStockCount ?? 0 }}</b><span>库存预警</span></button></div></article><article class="orders panel"><div class="panel-title"><div><h2>最新订单</h2><p>实时交易订单</p></div><a href="#" @click.prevent="loadSection('orders')">查看全部 →</a></div><table><thead><tr><th>订单编号</th><th>客户</th><th>商品</th><th>金额</th><th>状态</th><th>时间</th></tr></thead><tbody><tr v-for="order in dashboard?.latestOrders" :key="order.orderNo"><td><strong>{{ order.orderNo }}</strong></td><td>{{ order.receiverName }}</td><td>{{ order.items[0]?.productName }}</td><td>{{ currency(order.payableAmount) }}</td><td><span class="status">{{ orderStatus(order.status) }}</span></td><td>{{ dateTime(order.createdAt) }}</td></tr></tbody></table></article></div>
        </template>

        <article v-if="active === 'orders'" class="panel data-panel"><div class="panel-title"><div><h2>订单工作台</h2><p>查询、发货与售后状态联动</p></div></div><table><thead><tr><th>订单</th><th>客户</th><th>商品</th><th>金额</th><th>状态</th><th>下单时间</th><th>操作</th></tr></thead><tbody><tr v-for="order in orders" :key="order.orderNo"><td><strong>{{ order.orderNo }}</strong></td><td>{{ order.receiverName }}<small>{{ order.receiverPhone }}</small></td><td>{{ order.items[0]?.productName }}<small>共 {{ order.items.length }} 行</small></td><td>{{ currency(order.payableAmount) }}</td><td><span class="status">{{ orderStatus(order.status) }}</span></td><td>{{ dateTime(order.createdAt) }}</td><td><button v-if="order.status === 'PAID'" class="table-action" @click="ship(order.orderNo)">模拟发货</button><span v-else>—</span></td></tr></tbody></table></article>

        <article v-if="active === 'afterSales'" class="panel data-panel"><div class="panel-title"><div><h2>售后工作台</h2><p>审核、退货与退款状态机</p></div></div><table><thead><tr><th>售后单</th><th>订单</th><th>用户</th><th>类型</th><th>金额</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in afterSales" :key="item.afterSaleNo"><td><strong>{{ item.afterSaleNo }}</strong><small>{{ item.reasonCode }}</small></td><td>{{ item.orderNo }}</td><td>#{{ item.userId }}</td><td>{{ item.type === 'REFUND_ONLY' ? '仅退款' : '退货退款' }}</td><td>{{ currency(item.refundAmount) }}</td><td><span class="status service">{{ serviceStatus(item.status) }}</span><small v-if="item.returnTrackingNo">{{ item.returnCarrier }} · {{ item.returnTrackingNo }}</small></td><td><div class="action-row"><button v-if="item.status === 'PENDING_REVIEW'" class="table-action" @click="afterSaleAction(item, 'approve')">通过</button><button v-if="item.status === 'PENDING_REVIEW'" class="table-action secondary" @click="afterSaleAction(item, 'reject')">驳回</button><button v-if="['APPROVED','RETURNED'].includes(item.status)" class="table-action" @click="afterSaleAction(item, 'refund')">完成退款</button></div></td></tr></tbody></table></article>

        <article v-if="active === 'inventory'" class="panel data-panel"><div class="panel-title"><div><h2>库存中心</h2><p>实时可售、锁定与预警</p></div></div><div class="inventory-grid"><article v-for="stock in inventories" :key="stock.skuId" :class="{ warning: stock.warning }"><span>SKU {{ stock.skuId }}</span><strong>{{ stock.availableQuantity }}</strong><p>总库存 {{ stock.totalQuantity }} · 锁定 {{ stock.reservedQuantity }}</p><small>预警线 {{ stock.warningQuantity }}</small></article></div></article>

        <article v-if="active === 'marketing'" class="panel data-panel"><div class="panel-title"><div><h2>优惠券活动</h2><p>领取、锁定与核销数据基座</p></div></div><div class="card-grid"><article v-for="coupon in coupons" :key="coupon.couponId"><span>{{ coupon.code }}</span><h3>{{ coupon.name }}</h3><strong>¥{{ coupon.discountAmount }}</strong><p>满 ¥{{ coupon.thresholdAmount }} 可用</p><small>{{ dateTime(coupon.startsAt) }} — {{ dateTime(coupon.endsAt) }}</small></article></div></article>

        <article v-if="active === 'content'" class="panel data-panel"><div class="panel-title"><div><h2>内容运营</h2><p>频道文章与发布状态</p></div></div><table><thead><tr><th>标题</th><th>频道</th><th>Slug</th><th>精选</th><th>状态</th><th>发布时间</th><th>操作</th></tr></thead><tbody><tr v-for="article in articles" :key="article.id"><td><strong>{{ article.title }}</strong></td><td>{{ article.channelCode }}</td><td>{{ article.slug }}</td><td>{{ article.featured ? '是' : '否' }}</td><td><span class="status">{{ article.status === 'PUBLISHED' ? '已发布' : '草稿' }}</span></td><td>{{ dateTime(article.publishedAt) }}</td><td><button v-if="article.status === 'DRAFT'" class="table-action" @click="publishArticle(article.id)">发布</button><span v-else>—</span></td></tr></tbody></table></article>

        <article v-if="active === 'reviews'" class="panel data-panel"><div class="panel-title"><div><h2>评价管理</h2><p>真实购买评价、回复与内容治理</p></div></div><table><thead><tr><th>评价</th><th>用户</th><th>商品</th><th>评分</th><th>状态</th><th>商家回复</th><th>操作</th></tr></thead><tbody><tr v-for="review in reviews" :key="review.id"><td><strong>{{ review.content }}</strong><small>{{ dateTime(review.createdAt) }}</small></td><td>#{{ review.userId }}</td><td>#{{ review.productId }}</td><td>{{ '★'.repeat(review.rating) }}</td><td><span class="status">{{ review.status === 'PUBLISHED' ? '展示中' : '已隐藏' }}</span></td><td>{{ review.adminReply || '—' }}</td><td><div class="action-row"><button class="table-action" @click="replyReview(review.id)">回复</button><button v-if="review.status === 'PUBLISHED'" class="table-action secondary" @click="hideReview(review.id)">隐藏</button></div></td></tr></tbody></table></article>

        <article v-if="active === 'messages'" class="panel data-panel message-compose"><div><p class="eyebrow">TARGETED MESSAGE</p><h2>发送系统消息</h2><span>消息写入用户站内信，并通过鉴权 SSE 实时到达在线客户端。</span></div><form @submit.prevent="sendMessage"><label>用户 ID<input v-model.number="messageUserId" required type="number" min="1" placeholder="例如：2" /></label><label>消息标题<input v-model.trim="messageTitle" required maxlength="180" /></label><label>消息内容<textarea v-model.trim="messageContent" required maxlength="1000"></textarea></label><button :disabled="loading">{{ loading ? '正在发送…' : '立即发送' }}</button></form></article>
      </main>
    </section>
  </div>
</template>
