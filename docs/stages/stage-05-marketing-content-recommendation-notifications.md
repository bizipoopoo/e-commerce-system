# Stage 05：营销、内容、推荐与消息

## 范围

- 优惠券活动、用户领取、结算预览、下单锁定、支付核销和取消释放
- 订单优惠快照及按订单商品金额比例分摊
- 内容文章创建、发布、频道信息流和文章详情
- 用户商品浏览、点击、加购和购买行为事件
- 基于类目偏好、商品热度和运营精选的可解释推荐流
- 订单、支付、物流、营销和系统站内消息
- 鉴权 SSE 实时消息流、未读计数和已读状态
- 商城发现频道、会员礼遇、推荐卡片、文章阅读器和消息中心

## 非目标

- 复杂叠加促销、阶梯满减、秒杀和动态定价
- 外部短信、邮件、App Push 和第三方消息渠道
- 离线机器学习训练、向量召回和实时特征平台
- 收藏、评价、晒单和售后互动，统一在 Stage 06 实现
- 运营后台完整营销与内容工作台，统一在 Stage 06 数据化

## 优惠券状态

```text
UNCLAIMED -> AVAILABLE -> LOCKED -> USED
                        |
                        +-> AVAILABLE（订单取消或超时）
```

## 推荐规则

1. 未登录用户使用运营精选和全站销量热度。
2. 登录用户最近 100 条商品行为按 `VIEW=1`、`CLICK=2`、`ADD_TO_CART=4`、`PURCHASE=6` 加权。
3. 相同类目偏好提高候选商品得分，运营精选提供额外权重。
4. 每个推荐结果返回 `reason`，用于解释“为何推荐”。
5. 行为采集失败不能阻断加购、结算等核心交易流程。

## 消息规则

1. 订单创建、支付、发货、收货、取消、超时和优惠券领取均生成站内消息。
2. 消息记录与业务操作处于同一数据库事务。
3. SSE 只在事务成功提交后推送，避免客户端收到已回滚状态。
4. SSE 连接必须携带登录令牌；断线时消息列表 API 仍可完整恢复状态。

## 数据结构

- `coupons`, `user_coupons`, `order_discounts`
- `content_articles`
- `behavior_events`
- `notifications`

## 主要 API

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/v1/coupons` | 用户 | 可领取优惠券 |
| POST | `/api/v1/coupons/{id}/claim` | 用户 | 领取优惠券 |
| GET | `/api/v1/me/coupons` | 用户 | 我的优惠券 |
| POST | `/api/v1/coupons/preview` | 用户 | 优惠试算 |
| POST | `/api/v1/admin/coupons` | ADMIN | 创建优惠券活动 |
| GET | `/api/v1/content/feed` | 公开 | 内容频道信息流 |
| GET | `/api/v1/content/articles/{slug}` | 公开 | 文章详情 |
| POST/PATCH | `/api/v1/admin/content/articles/**` | ADMIN | 创建和发布文章 |
| GET | `/api/v1/recommendations` | 公开/用户 | 可解释推荐流 |
| POST | `/api/v1/behaviors` | 用户 | 上报商品行为 |
| GET | `/api/v1/notifications` | 用户 | 消息列表与未读数 |
| PATCH | `/api/v1/notifications/{id}/read` | 用户 | 标记已读 |
| GET | `/api/v1/notifications/stream` | 用户 | SSE 实时消息 |
| POST | `/api/v1/admin/notifications` | ADMIN | 发送系统消息 |

## 验收条件

1. 用户可领取优惠券，下单金额、订单优惠和商品优惠分摊正确。
2. 优惠券在订单创建时锁定，支付后核销，取消或超时后恢复可用。
3. 发现频道同时展示会员礼遇、推荐商品、推荐理由和已发布内容。
4. 用户行为会改变推荐理由或排序，未登录用户仍有稳定推荐结果。
5. 业务节点生成站内消息，未读数和已读状态正确。
6. SSE 建连成功，实时消息仅在事务提交后发送。
7. Stage 05 Code Review 的 P0/P1 清零并推送 GitHub。
