<script setup lang="ts">
const categories = [
  { icon: '◌', name: '今日上新', caption: 'NEW ARRIVALS' },
  { icon: '⌂', name: '居家生活', caption: 'LIVING' },
  { icon: '◇', name: '数码精选', caption: 'DIGITAL' },
  { icon: '✦', name: '美妆个护', caption: 'BEAUTY' },
  { icon: '♧', name: '户外运动', caption: 'OUTDOOR' },
  { icon: '◐', name: '服饰穿搭', caption: 'STYLE' },
]

const products = [
  {
    name: '云感人体工学休闲椅',
    category: 'AURORA HOME',
    price: '2,499',
    original: '2,899',
    tag: '设计师精选',
    image: 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?auto=format&fit=crop&w=1000&q=85',
  },
  {
    name: '晨雾手冲咖啡套装',
    category: 'DAILY RITUAL',
    price: '429',
    original: '529',
    tag: '本周热销',
    image: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1000&q=85',
  },
  {
    name: '原木无线氛围音箱',
    category: 'SOUND & LIGHT',
    price: '899',
    original: '1,099',
    tag: '新品首发',
    image: 'https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=1000&q=85',
  },
  {
    name: '山系轻量城市双肩包',
    category: 'URBAN OUTDOOR',
    price: '569',
    original: '699',
    tag: '会员专享',
    image: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=85',
  },
]

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
        <button aria-label="个人中心">♙</button>
        <button class="cart" aria-label="购物车">购物袋 <b>2</b></button>
      </div>
    </header>

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
          <img src="https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=1400&q=88" alt="温暖现代的家居空间" />
          <div class="floating-card">
            <span>EDITOR'S PICK</span>
            <strong>原木与光</strong>
            <small>打造会呼吸的客厅</small>
          </div>
          <div class="hero-index">01 <i></i> 04</div>
        </div>
      </section>

      <section class="category-section container" id="new">
        <div v-for="item in categories" :key="item.name" class="category-item">
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
          <article v-for="product in products" :key="product.name" class="product-card">
            <div class="product-image">
              <img :src="product.image" :alt="product.name" />
              <span class="product-tag">{{ product.tag }}</span>
              <button class="favorite" aria-label="收藏">♡</button>
              <button class="quick-add">快速加入购物袋</button>
            </div>
            <p>{{ product.category }}</p>
            <h3>{{ product.name }}</h3>
            <div class="price"><strong>¥{{ product.price }}</strong><del>¥{{ product.original }}</del></div>
          </article>
        </div>
      </section>

      <section class="feature-banner container">
        <div class="feature-image"></div>
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
  </div>
</template>

