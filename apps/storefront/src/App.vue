<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError, getHome, type Category, type ProductCard } from './api'
import { useCartStore } from './cart'
import { useOrderStore } from './orders'
import { useSessionStore } from './session'

const session = useSessionStore()
const cart = useCartStore()
const orders = useOrderStore()
const categories = ref<Array<Category & { caption: string }>>([])
const products = ref<Array<ProductCard & { tag: string }>>([])
const heroImage = ref('')
const featureImage = ref('')
const loading = ref(true)
const loadError = ref('')
const authOpen = ref(false)
const authMode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const displayName = ref('')
const authError = ref('')
const authSubmitting = ref(false)
const accountMenuOpen = ref(false)
const receiverName = ref('')
const receiverPhone = ref('')
const addressLine = ref('')
const customerNote = ref('')

const authTitle = computed(() => authMode.value === 'login' ? '欢迎回来' : '加入 Aurora')

const tags = ['设计师精选', '本周热销', '新品首发', '会员专享']

async function loadHome() {
  loading.value = true
  loadError.value = ''
  try {
    const home = await getHome()
    categories.value = home.categories.map(item => ({
      ...item,
      caption: item.slug.replaceAll('-', ' ').toUpperCase(),
    }))
    products.value = home.featuredProducts.map((item, index) => ({
      ...item,
      tag: tags[index % tags.length],
    }))
    heroImage.value = home.heroBanners[0]?.imageUrl ?? ''
    featureImage.value = home.featureBanners[0]?.imageUrl ?? ''
  } catch {
    loadError.value = '首页数据暂时无法加载，请确认后端服务已经启动。'
  } finally {
    loading.value = false
  }
}

function openAccount() {
  if (session.authenticated) {
    accountMenuOpen.value = !accountMenuOpen.value
    return
  }
  authOpen.value = true
}

function logout() {
  session.logout()
  cart.reset()
  orders.reset()
  accountMenuOpen.value = false
}

async function submitAuth() {
  authSubmitting.value = true
  authError.value = ''
  try {
    if (authMode.value === 'login') {
      await session.login(email.value, password.value)
    } else {
      await session.register(email.value, password.value, displayName.value)
    }
    authOpen.value = false
    password.value = ''
    void cart.load().catch(() => undefined)
  } catch (error) {
    authError.value = error instanceof ApiError
      ? error.message
      : error instanceof Error
        ? `会话处理失败：${error.message}`
        : '暂时无法完成操作，请稍后再试。'
  } finally {
    authSubmitting.value = false
  }
}

function switchAuthMode() {
  authMode.value = authMode.value === 'login' ? 'register' : 'login'
  authError.value = ''
}

async function addToCart(skuId: number | null) {
  if (!session.authenticated) {
    authOpen.value = true
    return
  }
  if (skuId == null) return
  try {
    await cart.add(skuId)
  } catch {
    cart.drawerOpen = true
  }
}

async function openCart() {
  if (!session.authenticated) {
    authOpen.value = true
    return
  }
  cart.drawerOpen = true
  try {
    await cart.load()
  } catch {
    // The cart drawer presents the actionable error.
  }
}

async function changeQuantity(itemId: number, current: number, delta: number) {
  const next = current + delta
  if (next < 1 || next > 99) return
  try {
    await cart.update(itemId, { quantity: next })
  } catch {
    // The cart drawer presents the actionable error.
  }
}

async function setSelected(itemId: number, selected: boolean) {
  try {
    await cart.update(itemId, { selected })
  } catch {
    // The cart drawer presents the actionable error.
  }
}

async function removeCartItem(itemId: number) {
  try {
    await cart.remove(itemId)
  } catch {
    // The cart drawer presents the actionable error.
  }
}

async function previewCheckout() {
  if (cart.checkout) {
    orders.beginCheckout()
    cart.drawerOpen = false
    return
  }
  try {
    await cart.previewCheckout()
  } catch {
    // The cart drawer presents the actionable error.
  }
}

async function submitOrder() {
  try {
    await orders.create({
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      addressLine: addressLine.value,
      customerNote: customerNote.value,
    })
    await cart.load()
  } catch {
    // The checkout dialog presents the actionable error.
  }
}

async function mockPay() {
  try {
    await orders.pay()
  } catch {
    // The payment dialog presents the actionable error.
  }
}

async function openOrders() {
  accountMenuOpen.value = false
  orders.drawerOpen = true
  try {
    await orders.load()
  } catch {
    // The order drawer presents the actionable error.
  }
}

async function selectOrder(order: (typeof orders.data)[number]) {
  try {
    await orders.select(order)
  } catch {
    // The order drawer presents the actionable error.
  }
}

async function cancelOrder(orderNo: string) {
  try {
    await orders.cancel(orderNo)
  } catch {
    // The order drawer presents the actionable error.
  }
}

async function confirmOrder(orderNo: string) {
  try {
    await orders.confirmReceipt(orderNo)
  } catch {
    // The order drawer presents the actionable error.
  }
}

function orderStatus(status: string) {
  return ({
    PENDING_PAYMENT: '待支付', PAID: '待发货', SHIPPED: '运输中', COMPLETED: '已完成', CLOSED: '已关闭',
  } as Record<string, string>)[status] ?? status
}

function formatTime(value: string | null) {
  return value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : ''
}

onMounted(() => {
  void loadHome()
  void session.restore().then(() => cart.load()).catch(() => undefined)
})

const stories = [
  { number: '01', title: '让家，成为恢复能量的地方', meta: '空间灵感 · 8 MIN READ' },
  { number: '02', title: '一杯好咖啡的晨间仪式', meta: '生活方式 · 5 MIN READ' },
  { number: '03', title: '轻装出发，去城市边缘呼吸', meta: '户外指南 · 6 MIN READ' },
]
</script>

<template>
  <div class="site-shell">
    <div class="announcement">新会员首单立减 ¥50 · 全场满 ¥299 免运费</div>

    <header class="header container">
      <a class="brand" href="#">AURORA<span>生活有光</span></a>
      <nav>
        <a class="active" href="#">首页</a>
        <a href="#new">新品</a>
        <a href="#selection">精选</a>
        <a href="#inspiration">灵感</a>
        <a href="#member">会员</a>
      </nav>
      <div class="header-actions">
        <button aria-label="搜索">⌕</button>
        <button class="account-button" aria-label="个人中心" @click="openAccount">
          {{ session.user?.displayName || '登录' }}
        </button>
        <div v-if="accountMenuOpen && session.user" class="account-menu">
          <strong>{{ session.user.displayName }}</strong>
          <span>{{ session.user.email }}</span>
          <button @click="openOrders">我的订单</button>
          <button @click="logout">退出登录</button>
        </div>
        <button class="cart" aria-label="购物车" @click="openCart">购物袋 <b>{{ cart.data.totalQuantity }}</b></button>
      </div>
    </header>

    <div v-if="loadError" class="load-error container">
      <span>{{ loadError }}</span><button @click="loadHome">重新加载</button>
    </div>

    <main>
      <section class="hero container">
        <div class="hero-copy">
          <p class="eyebrow">AURORA SELECTION · 2026</p>
          <h1>把好生活<br /><em>带回家</em></h1>
          <p class="hero-text">我们从设计、质感与真实使用体验出发，挑选值得长久陪伴的日常好物。</p>
          <div class="hero-buttons">
            <button class="primary">探索本季精选 <span>→</span></button>
            <button class="text-button">了解我们的选品标准</button>
          </div>
          <div class="hero-proof">
            <div><strong>4.9</strong><span>用户综合评分</span></div>
            <div><strong>30 天</strong><span>无忧退换</span></div>
            <div><strong>100%</strong><span>正品保障</span></div>
          </div>
        </div>
        <div class="hero-visual">
          <div v-if="loading" class="hero-placeholder"></div>
          <img v-else-if="heroImage" :src="heroImage" alt="温暖现代的家居空间" />
          <div class="floating-card">
            <span>EDITOR'S PICK</span>
            <strong>原木与光</strong>
            <small>打造会呼吸的客厅</small>
          </div>
          <div class="hero-index">01 <i></i> 04</div>
        </div>
      </section>

      <section class="category-section container" id="new">
        <div v-for="item in categories" :key="item.id" class="category-item">
          <div class="category-icon">{{ item.icon }}</div>
          <strong>{{ item.name }}</strong>
          <span>{{ item.caption }}</span>
        </div>
      </section>

      <section class="product-section container" id="selection">
        <div class="section-heading">
          <div><p class="eyebrow">CURATED FOR YOU</p><h2>本周精选</h2></div>
          <a href="#">查看全部商品 <span>→</span></a>
        </div>
        <div class="product-grid">
          <article v-for="product in products" :key="product.id" class="product-card">
            <div class="product-image">
              <img :src="product.coverImageUrl" :alt="product.name" />
              <span class="product-tag">{{ product.tag }}</span>
              <button class="favorite" aria-label="收藏">♡</button>
              <button class="quick-add" :disabled="product.defaultSkuId == null" @click="addToCart(product.defaultSkuId)">快速加入购物袋</button>
            </div>
            <p>{{ product.brandName }}</p>
            <h3>{{ product.name }}</h3>
            <div class="price"><strong>¥{{ product.salePrice.toLocaleString() }}</strong><del>¥{{ product.marketPrice.toLocaleString() }}</del></div>
          </article>
          <div v-if="loading" v-for="index in 4" :key="`skeleton-${index}`" class="product-skeleton"></div>
        </div>
      </section>

      <section class="feature-banner container">
        <div class="feature-image" :style="featureImage ? { backgroundImage: `linear-gradient(90deg, transparent 70%, rgba(29,74,53,.2)), url('${featureImage}')` } : undefined"></div>
        <div class="feature-copy">
          <p class="eyebrow">A BETTER EVERYDAY</p>
          <h2>不是更多，<br />而是更好的选择</h2>
          <p>每件商品都经过真实体验与多维评估。我们关注它如何被制造，也关心它能陪伴你多久。</p>
          <a href="#">认识 Aurora 的选品哲学 <span>↗</span></a>
        </div>
      </section>

      <section class="stories container" id="inspiration">
        <div class="section-heading">
          <div><p class="eyebrow">JOURNAL & INSPIRATION</p><h2>生活灵感</h2></div>
        </div>
        <div class="story-list">
          <article v-for="story in stories" :key="story.number">
            <span>{{ story.number }}</span>
            <div><p>{{ story.meta }}</p><h3>{{ story.title }}</h3></div>
            <button>阅读文章 →</button>
          </article>
        </div>
      </section>
    </main>

    <footer>
      <div class="container footer-inner">
        <div><a class="brand light" href="#">AURORA<span>生活有光</span></a><p>认真选择每一件，值得留在生活里的东西。</p></div>
        <div class="footer-links"><a href="#">购物帮助</a><a href="#">配送与退换</a><a href="#">关于我们</a><a href="#">联系我们</a></div>
        <p class="copyright">© 2026 AURORA COMMERCE</p>
      </div>
    </footer>

    <div v-if="authOpen" class="auth-overlay" @click.self="authOpen = false">
      <form class="auth-dialog" role="dialog" aria-modal="true" :aria-label="authTitle" @submit.prevent="submitAuth">
        <button class="auth-close" type="button" aria-label="关闭" @click="authOpen = false">×</button>
        <p class="eyebrow">AURORA MEMBER</p>
        <h2>{{ authTitle }}</h2>
        <p>{{ authMode === 'login' ? '登录后同步你的收藏、订单和专属推荐。' : '创建账户，开始更懂你的生活提案。' }}</p>
        <label v-if="authMode === 'register'">称呼<input v-model.trim="displayName" required maxlength="80" placeholder="你的称呼" /></label>
        <label>邮箱<input v-model.trim="email" required type="email" autocomplete="email" placeholder="name@example.com" /></label>
        <label>密码<input v-model="password" required type="password" minlength="8" :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'" placeholder="至少 8 位字符" /></label>
        <div v-if="authError" class="auth-error">{{ authError }}</div>
        <button class="auth-submit" :disabled="authSubmitting">{{ authSubmitting ? '请稍候…' : authMode === 'login' ? '登录' : '创建账户' }}</button>
        <button class="auth-switch" type="button" @click="switchAuthMode">
          {{ authMode === 'login' ? '还没有账户？立即注册' : '已有账户？返回登录' }}
        </button>
      </form>
    </div>

    <div v-if="cart.drawerOpen" class="cart-overlay" @click.self="cart.drawerOpen = false">
      <aside class="cart-drawer" aria-label="购物袋">
        <header>
          <div><p class="eyebrow">YOUR SELECTION</p><h2>购物袋 <span>{{ cart.data.totalQuantity }}</span></h2></div>
          <button aria-label="关闭购物袋" @click="cart.drawerOpen = false">×</button>
        </header>
        <div v-if="cart.error" class="cart-error">{{ cart.error }}</div>
        <div v-if="cart.data.items.length === 0" class="empty-cart">
          <strong>购物袋还是空的</strong><p>去挑选一些值得带回家的好物吧。</p>
          <button @click="cart.drawerOpen = false">继续逛逛</button>
        </div>
        <div v-else class="cart-items">
          <article v-for="item in cart.data.items" :key="item.id" :class="{ unavailable: !item.available }">
            <input :checked="item.selected" :disabled="!item.available || cart.loading" type="checkbox" :aria-label="`选择${item.productName}`" @change="setSelected(item.id, ($event.target as HTMLInputElement).checked)" />
            <img :src="item.imageUrl" :alt="item.productName" />
            <div class="cart-item-copy">
              <h3>{{ item.productName }}</h3><p>{{ item.skuName }}</p>
              <strong>¥{{ item.unitPrice.toLocaleString() }}</strong>
              <span v-if="!item.available">库存不足或商品已失效</span>
              <div class="quantity-control">
                <button :disabled="cart.loading || item.quantity <= 1" @click="changeQuantity(item.id, item.quantity, -1)">−</button>
                <b>{{ item.quantity }}</b>
                <button :disabled="cart.loading || item.quantity >= item.availableQuantity" @click="changeQuantity(item.id, item.quantity, 1)">＋</button>
              </div>
            </div>
            <button class="remove-item" aria-label="删除商品" @click="removeCartItem(item.id)">×</button>
          </article>
        </div>
        <footer v-if="cart.data.items.length">
          <div><span>已选 {{ cart.data.selectedQuantity }} 件</span><strong>¥{{ cart.data.selectedAmount.toLocaleString() }}</strong></div>
          <div v-if="cart.checkout" class="checkout-breakdown">
            <p><span>商品金额</span><b>¥{{ cart.checkout.goodsAmount.toLocaleString() }}</b></p>
            <p><span>运费</span><b>{{ cart.checkout.shippingAmount === 0 ? '免运费' : `¥${cart.checkout.shippingAmount}` }}</b></p>
            <p><span>应付金额</span><strong>¥{{ cart.checkout.payableAmount.toLocaleString() }}</strong></p>
          </div>
          <button class="checkout-button" :disabled="cart.loading || cart.data.selectedQuantity === 0" @click="previewCheckout">
            {{ cart.loading ? '正在计算…' : cart.checkout ? '填写配送信息' : '结算预览' }}
          </button>
          <small>满 ¥299 免运费 · 下单后 30 分钟内完成支付</small>
        </footer>
      </aside>
    </div>

    <div v-if="orders.checkoutOpen" class="auth-overlay trade-overlay" @click.self="orders.checkoutOpen = false">
      <form class="auth-dialog checkout-dialog" role="dialog" aria-modal="true" aria-label="确认订单" @submit.prevent="submitOrder">
        <button class="auth-close" type="button" aria-label="关闭结算" @click="orders.checkoutOpen = false">×</button>
        <p class="eyebrow">SECURE CHECKOUT</p>
        <h2>确认配送信息</h2>
        <p>价格和库存会在提交时再次校验，订单创建后将为你锁定 30 分钟。</p>
        <div class="checkout-form-grid">
          <label>收货人<input v-model.trim="receiverName" required maxlength="80" placeholder="收货人姓名" /></label>
          <label>联系电话<input v-model.trim="receiverPhone" required maxlength="30" pattern="[0-9+() -]{6,30}" placeholder="138 0000 0000" /></label>
        </div>
        <label>详细地址<input v-model.trim="addressLine" required maxlength="300" placeholder="省 / 市 / 区 / 街道及门牌号" /></label>
        <label>订单备注<input v-model.trim="customerNote" maxlength="300" placeholder="选填，例如工作日配送" /></label>
        <div class="checkout-order-total">
          <span>本次应付</span><strong>¥{{ cart.checkout?.payableAmount.toLocaleString() }}</strong>
        </div>
        <div v-if="orders.error" class="auth-error">{{ orders.error }}</div>
        <button class="auth-submit" :disabled="orders.loading">{{ orders.loading ? '正在锁定库存…' : '提交订单并前往支付' }}</button>
      </form>
    </div>

    <div v-if="orders.paymentOpen && orders.active" class="auth-overlay trade-overlay" @click.self="orders.paymentOpen = false">
      <section class="auth-dialog payment-dialog" role="dialog" aria-modal="true" aria-label="模拟收银台">
        <button class="auth-close" type="button" aria-label="关闭收银台" @click="orders.paymentOpen = false">×</button>
        <p class="eyebrow">AURORA PAY</p>
        <div v-if="orders.active.status === 'PENDING_PAYMENT'">
          <h2>订单已为你保留</h2>
          <p>订单 {{ orders.active.orderNo }}<br />请在 {{ formatTime(orders.active.expireAt) }} 前完成支付。</p>
          <div class="payment-amount"><span>支付金额</span><strong>¥{{ orders.active.payableAmount.toLocaleString() }}</strong></div>
          <div class="mock-channel"><i>◆</i><div><strong>Aurora 模拟支付</strong><span>用于 Demo 演示，不会产生真实扣款</span></div><b>已选择</b></div>
          <div v-if="orders.error" class="auth-error">{{ orders.error }}</div>
          <button class="auth-submit" :disabled="orders.loading" @click="mockPay">{{ orders.loading ? '支付处理中…' : '确认模拟支付' }}</button>
        </div>
        <div v-else class="payment-success">
          <i>✓</i><h2>支付成功</h2><p>库存已正式扣减，订单正在等待商家发货。</p>
          <button class="auth-submit" @click="orders.paymentOpen = false; openOrders()">查看我的订单</button>
        </div>
      </section>
    </div>

    <div v-if="orders.drawerOpen" class="cart-overlay order-overlay" @click.self="orders.drawerOpen = false">
      <aside class="order-drawer" aria-label="我的订单">
        <header>
          <div><p class="eyebrow">MY AURORA</p><h2>我的订单</h2></div>
          <button aria-label="关闭订单" @click="orders.drawerOpen = false">×</button>
        </header>
        <div v-if="orders.error" class="cart-error">{{ orders.error }}</div>
        <div class="order-workspace">
          <div class="order-list">
            <div v-if="!orders.data.length" class="empty-orders"><strong>还没有订单</strong><span>从一件喜欢的商品开始吧。</span></div>
            <button v-for="order in orders.data" :key="order.orderNo" :class="{ active: orders.active?.orderNo === order.orderNo }" @click="selectOrder(order)">
              <span><b>{{ orderStatus(order.status) }}</b>{{ formatTime(order.createdAt) }}</span>
              <strong>{{ order.items[0]?.productName }}<small v-if="order.items.length > 1">等 {{ order.items.length }} 件</small></strong>
              <em>¥{{ order.payableAmount.toLocaleString() }}</em>
            </button>
          </div>
          <section v-if="orders.active" class="order-detail">
            <div class="order-detail-head"><span>订单 {{ orders.active.orderNo }}</span><b>{{ orderStatus(orders.active.status) }}</b></div>
            <article v-for="item in orders.active.items" :key="item.id">
              <img :src="item.imageUrl" :alt="item.productName" /><div><strong>{{ item.productName }}</strong><span>{{ item.skuName }} · ×{{ item.quantity }}</span></div><b>¥{{ item.payableAmount.toLocaleString() }}</b>
            </article>
            <div class="order-address"><span>配送至</span><strong>{{ orders.active.receiverName }} · {{ orders.active.receiverPhone }}</strong><p>{{ orders.active.addressLine }}</p></div>
            <div v-if="orders.shipment" class="shipment-card">
              <span>{{ orders.shipment.carrier }} · {{ orders.shipment.trackingNo }}</span>
              <p v-for="track in orders.shipment.tracks" :key="track.occurredAt"><i></i><b>{{ track.description }}</b><small>{{ formatTime(track.occurredAt) }}</small></p>
            </div>
            <div class="order-timeline">
              <p v-for="log in orders.active.timeline" :key="log.createdAt"><i></i><span><b>{{ orderStatus(log.toStatus) }}</b>{{ log.remark }}</span><small>{{ formatTime(log.createdAt) }}</small></p>
            </div>
            <div class="order-actions">
              <button v-if="orders.active.status === 'PENDING_PAYMENT'" @click="orders.paymentOpen = true">立即支付</button>
              <button v-if="orders.active.status === 'PENDING_PAYMENT'" class="secondary" @click="cancelOrder(orders.active.orderNo)">取消订单</button>
              <button v-if="orders.active.status === 'SHIPPED'" @click="confirmOrder(orders.active.orderNo)">确认收货</button>
            </div>
          </section>
        </div>
      </aside>
    </div>
  </div>
</template>
