<script setup lang="ts">
const navigation = [
  { icon: '▦', label: '经营概览', active: true },
  { icon: '◇', label: '商品中心' },
  { icon: '▤', label: '订单管理', badge: '12' },
  { icon: '▥', label: '库存中心', badge: '3' },
  { icon: '♙', label: '用户中心' },
  { icon: '％', label: '营销活动' },
  { icon: '◫', label: '内容运营' },
  { icon: '♧', label: '推荐与消息' },
  { icon: '◉', label: '数据分析' },
]

const metrics = [
  { label: '今日交易额', value: '¥ 128,460', change: '+18.6%', tone: 'green', detail: '昨日 ¥108,320' },
  { label: '支付订单', value: '1,284', change: '+12.4%', tone: 'green', detail: '昨日 1,142' },
  { label: '访客数', value: '18,692', change: '+8.2%', tone: 'green', detail: '转化率 6.87%' },
  { label: '售后金额', value: '¥ 3,860', change: '-2.1%', tone: 'orange', detail: '售后率 1.03%' },
]

const orders = [
  { no: 'AU202608150042', user: '林女士', amount: '¥2,499.00', status: '待发货', time: '10:32' },
  { no: 'AU202608150041', user: '陈先生', amount: '¥998.00', status: '已支付', time: '10:28' },
  { no: 'AU202608150040', user: '周女士', amount: '¥569.00', status: '售后中', time: '10:21' },
  { no: 'AU202608150039', user: '王先生', amount: '¥1,328.00', status: '已完成', time: '10:16' },
]

const bars = [44, 58, 46, 72, 63, 79, 68, 83, 76, 92, 87, 100, 84, 95]
</script>

<template>
  <div class="console">
    <aside>
      <div class="console-brand">A<span>AURORA</span></div>
      <div class="workspace"><small>当前空间</small><strong>Aurora 品牌商城</strong><span>⌄</span></div>
      <nav>
        <a v-for="item in navigation" :key="item.label" :class="{ active: item.active }" href="#">
          <i>{{ item.icon }}</i><span>{{ item.label }}</span><b v-if="item.badge">{{ item.badge }}</b>
        </a>
      </nav>
      <div class="aside-bottom">
        <a href="#">⚙ <span>系统设置</span></a>
        <a href="#">? <span>帮助中心</span></a>
      </div>
    </aside>

    <section class="main-area">
      <header>
        <div class="search">⌕<input placeholder="搜索订单、商品、用户" /><kbd>⌘ K</kbd></div>
        <div class="header-tools"><button>◌<b></b></button><span></span><div class="avatar">CW</div><div><strong>陈伟</strong><small>超级管理员</small></div><button>⌄</button></div>
      </header>

      <main>
        <div class="welcome">
          <div><p>2026 年 8 月 15 日 · 星期六</p><h1>上午好，陈伟</h1><span>这里是 Aurora 今日的经营情况，整体表现不错。</span></div>
          <div class="welcome-actions"><button>下载日报</button><button class="solid">＋ 创建活动</button></div>
        </div>

        <div class="metrics">
          <article v-for="metric in metrics" :key="metric.label">
            <div><span>{{ metric.label }}</span><button>···</button></div>
            <strong>{{ metric.value }}</strong>
            <p><b :class="metric.tone">{{ metric.change }}</b>{{ metric.detail }}</p>
          </article>
        </div>

        <div class="dashboard-grid">
          <article class="trend panel">
            <div class="panel-title"><div><h2>交易趋势</h2><p>近 14 天支付金额与订单变化</p></div><div class="tabs"><button>交易额</button><button>订单数</button><select><option>近 14 天</option></select></div></div>
            <div class="chart">
              <div class="axis"><span>15万</span><span>10万</span><span>5万</span><span>0</span></div>
              <div class="bars">
                <div v-for="(bar, index) in bars" :key="index" class="bar-wrap"><div class="bar" :style="{ height: `${bar}%` }"></div><span>{{ index + 2 }}日</span></div>
              </div>
            </div>
          </article>

          <article class="funnel panel">
            <div class="panel-title"><div><h2>今日转化</h2><p>实时用户转化漏斗</p></div><button>···</button></div>
            <div class="funnel-list">
              <div><span><i style="--w:100%"></i></span><p><b>18,692</b>访客</p></div>
              <div><span><i style="--w:66%"></i></span><p><b>6,428</b>商品详情</p></div>
              <div><span><i style="--w:43%"></i></span><p><b>2,106</b>加购物车</p></div>
              <div><span><i style="--w:28%"></i></span><p><b>1,284</b>支付订单</p></div>
            </div>
            <div class="conversion"><strong>6.87%</strong><span>整体支付转化率<br /><b>较昨日 +0.52%</b></span></div>
          </article>

          <article class="orders panel">
            <div class="panel-title"><div><h2>最新订单</h2><p>实时更新的交易订单</p></div><a href="#">查看全部 →</a></div>
            <table>
              <thead><tr><th>订单编号</th><th>客户</th><th>订单金额</th><th>状态</th><th>下单时间</th><th></th></tr></thead>
              <tbody><tr v-for="order in orders" :key="order.no"><td><strong>{{ order.no }}</strong></td><td>{{ order.user }}</td><td>{{ order.amount }}</td><td><span :class="['status', order.status]">{{ order.status }}</span></td><td>{{ order.time }}</td><td>•••</td></tr></tbody>
            </table>
          </article>

          <article class="todo panel">
            <div class="panel-title"><div><h2>待办事项</h2><p>需要及时处理</p></div></div>
            <div class="todo-grid"><a href="#"><i>▤</i><strong>12</strong><span>待发货订单</span></a><a href="#"><i>↩</i><strong>5</strong><span>待处理售后</span></a><a href="#"><i>!</i><strong>3</strong><span>库存预警</span></a><a href="#"><i>✎</i><strong>8</strong><span>待审核评价</span></a></div>
          </article>
        </div>
      </main>
    </section>
  </div>
</template>

