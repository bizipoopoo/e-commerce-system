# Stage 06：售后、互动与运营工作台

## 范围

- 已支付、已发货和已完成订单的全额退款或退货退款申请
- 售后审核、驳回、用户寄回、退款完成和完整状态日志
- 退款完成后的订单状态推进、库存回补、退款记录和站内消息
- 商品收藏、我的收藏、已完成订单评价、晒单文字和运营回复/隐藏
- 运营看板真实指标、最新订单、库存预警和待处理售后
- 运营端登录、订单发货、售后处理、优惠券、内容发布和消息推送工作台
- 商城订单售后申请、寄回信息、售后进度、收藏和评价入口

## 非目标

- 第三方支付渠道真实退款、原路退回回调和资金对账
- 真实逆向物流 API、质检入库和部分商品/部分金额退款
- 多级客服工单、仲裁、举证和自动风控
- 复杂 RBAC、数据权限和多租户运营体系
- 最终云部署、演示脚本和交付回归，统一在 Stage 07 完成

## 售后状态

```text
PENDING_REVIEW -> APPROVED -> REFUNDED
       |              ^
       |              |
       +-> WAITING_RETURN -> RETURNED
       |
       +-> REJECTED
```

- `REFUND_ONLY`：仅允许已支付且未发货订单，审核通过后进入 `APPROVED`。
- `RETURN_REFUND`：允许已发货或已完成订单，审核通过后进入 `WAITING_RETURN`。
- 驳回后订单恢复申请前状态；退款完成后订单进入 `REFUNDED`。
- 退款完成时按订单商品数量幂等回补库存并生成退款记录。

## 数据结构

- `after_sales`, `after_sale_logs`, `refund_records`
- `product_favorites`, `product_reviews`
- `customer_orders.refunded_at`

## 主要 API

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/after-sales` | 用户 | 申请售后 |
| GET | `/api/v1/after-sales` | 用户 | 我的售后 |
| POST | `/api/v1/after-sales/{no}/return` | 用户 | 提交寄回物流 |
| GET | `/api/v1/admin/after-sales` | ADMIN | 售后工作台 |
| POST | `/api/v1/admin/after-sales/{no}/approve` | ADMIN | 审核通过 |
| POST | `/api/v1/admin/after-sales/{no}/reject` | ADMIN | 审核驳回 |
| POST | `/api/v1/admin/after-sales/{no}/refund` | ADMIN | 完成模拟退款 |
| GET/POST/DELETE | `/api/v1/favorites/**` | 用户 | 收藏管理 |
| GET/POST | `/api/v1/products/{id}/reviews`, `/api/v1/reviews` | 公开/用户 | 评价列表与发表评价 |
| GET/PATCH | `/api/v1/admin/reviews/**` | ADMIN | 评价管理 |
| GET | `/api/v1/admin/dashboard` | ADMIN | 经营看板 |

## 验收条件

1. 不同订单状态只能创建对应类型售后，跨用户订单不可申请。
2. 审核、驳回、寄回和退款严格遵守状态机，重复操作不会重复退款或回补库存。
3. 驳回后订单恢复原状态，退款后订单、退款记录、库存和消息保持一致。
4. 已完成订单可以评价一次，收藏具备幂等增加和删除能力。
5. 运营端全部关键数据来自真实 API，可完成发货、售后审核与退款、内容发布等操作。
6. 商城可申请售后、查看进度、提交寄回单号并发表评价。
7. Stage 06 Code Review 的 P0/P1 清零并推送 GitHub。
