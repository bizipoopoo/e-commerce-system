# API 设计基线

## 1. 约定

- 基础路径：`/api/v1`
- JSON 字段：camelCase
- 时间：ISO 8601，服务端统一存储 UTC
- 分页：`page`, `size`, `sort`
- 写操作支持请求头 `Idempotency-Key`
- 鉴权：`Authorization: Bearer <token>`

统一响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "requestId": "01J..."
}
```

## 2. 第一阶段接口清单

| 领域 | 主要接口 |
|---|---|
| 认证 | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh` |
| 首页 | `GET /home`, `GET /recommendations` |
| 商品 | `GET /categories`, `GET /products`, `GET /products/{id}` |
| 搜索 | `GET /search`, `GET /search/suggestions` |
| 购物车 | `GET/POST /cart/items`, `PATCH/DELETE /cart/items/{id}` |
| 结算订单 | `POST /checkout/preview`, `POST /orders`, `GET /orders/{orderNo}` |
| 支付 | `POST /payments`, `POST /payments/mock/{paymentNo}/complete` |
| 履约售后 | `GET /shipments/{orderNo}`, `POST /after-sales` |
| 互动 | `POST /favorites`, `GET /browse-history`, `POST /reviews` |
| 消息 | `GET /notifications`, `GET /notifications/stream` |
| 管理端 | `/admin/products`, `/admin/inventories`, `/admin/orders`, `/admin/content` |

## 3. 错误码

| 错误码 | 含义 |
|---|---|
| `VALIDATION_FAILED` | 请求参数错误 |
| `UNAUTHORIZED` | 未登录或令牌失效 |
| `FORBIDDEN` | 无权限 |
| `PRODUCT_NOT_AVAILABLE` | 商品不可售 |
| `INSUFFICIENT_STOCK` | 库存不足 |
| `PRICE_CHANGED` | 结算期间价格变化 |
| `ORDER_STATE_CONFLICT` | 当前订单状态不允许操作 |
| `DUPLICATE_REQUEST` | 重复请求 |

