# Stage 04：订单、支付与履约

## 范围

- 从已勾选购物车创建订单，保存商品、价格、地址与金额快照
- `Idempotency-Key` 防重复下单，订单创建后清理对应购物车行
- 下单预占库存，模拟支付成功后确认扣减库存
- 待支付订单取消和 30 分钟超时关闭，自动释放库存
- 模拟支付单、支付回调记录与回调幂等
- 用户订单列表、订单详情、取消、支付和确认收货
- 管理员订单查询、模拟发货、物流单和物流轨迹
- 商城结算地址弹窗、模拟收银台和“我的订单”抽屉

## 非目标

- 真实微信、支付宝或银行卡支付渠道
- 真实物流服务商 API、电子面单和轨迹订阅
- 退款、退货和售后状态，统一在 Stage 06 实现
- 优惠券和活动优惠分摊，统一在 Stage 05 实现
- 运营后台完整订单工作台，统一在 Stage 06 数据化

## 状态模型

```text
PENDING_PAYMENT -> PAID -> SHIPPED -> COMPLETED
       |
       +-> CLOSED
```

支付单：`PENDING -> SUCCESS`。

物流单：`IN_TRANSIT -> DELIVERED`。

## 核心规则

1. 创建订单必须携带 1–100 个字符的 `Idempotency-Key`，同一用户重复提交返回原订单。
2. 订单商品名称、SKU、图片、单价、数量和金额在创建时保存为不可变快照。
3. 下单使用订单号作为库存业务键预占库存；任一 SKU 失败时整单事务回滚。
4. 模拟支付成功在同一事务内写入回调、确认扣库并推进订单状态。
5. 支付和发货操作支持重复调用，同一业务对象不会重复扣库或重复建单。
6. 只有待支付订单允许取消；只有已支付订单允许发货；只有已发货订单允许确认收货。
7. 用户只能访问自己的订单和物流，管理员接口由 `ADMIN` 角色保护。

## 数据结构

- `customer_orders`：订单主表、金额、地址快照、状态和关键时间
- `order_items`：订单商品不可变快照
- `order_status_logs`：完整状态变更轨迹
- `payment_orders`：模拟支付单
- `payment_callbacks`：支付回调幂等记录
- `shipments`：物流单
- `shipment_tracks`：物流轨迹
- `inventory_reservations.confirmed_at`：库存预占确认时间

## 主要 API

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/orders` | 用户 | 创建订单，要求 `Idempotency-Key` |
| GET | `/api/v1/orders` | 用户 | 我的订单 |
| GET | `/api/v1/orders/{orderNo}` | 用户 | 订单详情 |
| POST | `/api/v1/orders/{orderNo}/cancel` | 用户 | 取消待支付订单 |
| POST | `/api/v1/payments` | 用户 | 创建或返回模拟支付单 |
| POST | `/api/v1/payments/mock/{paymentNo}/complete` | 用户 | 完成模拟支付 |
| GET | `/api/v1/shipments/{orderNo}` | 用户 | 查询物流 |
| POST | `/api/v1/orders/{orderNo}/confirm-receipt` | 用户 | 确认收货 |
| GET | `/api/v1/admin/orders` | ADMIN | 查询订单 |
| POST | `/api/v1/admin/orders/{orderNo}/ship` | ADMIN | 模拟发货 |

## 验收条件

1. 用户可从真实购物车完成地址填写、下单和模拟支付，订单金额与结算预览一致。
2. 重复下单和重复支付不会创建重复业务数据或重复扣减库存。
3. 取消及超时订单会释放库存，支付成功会将预占库存转为实际扣减。
4. 管理员可以对已支付订单发货，用户可以查看轨迹并确认收货。
5. 订单、物流和管理接口具备正确的用户隔离及角色权限。
6. 商城提供订单空状态、错误状态、状态时间线、收银台和物流信息。
7. Stage 04 Code Review 的 P0/P1 清零并推送 GitHub。
