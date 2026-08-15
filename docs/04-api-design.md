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

## 3. Stage 02 已实现接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/auth/register` | 公开 | 邮箱注册并签发访问令牌 |
| POST | `/auth/login` | 公开 | 登录并签发访问令牌 |
| GET | `/me` | 登录用户 | 获取当前用户 |
| GET | `/home` | 公开 | 首页 Banner、类目和商品聚合 |
| GET | `/categories` | 公开 | 启用类目列表 |
| GET | `/products` | 公开 | 商品关键词、类目筛选和分页 |
| GET | `/products/{id}` | 公开 | 已上架商品详情与 SKU |
| POST | `/admin/products` | ADMIN | 创建草稿商品与 SKU |
| PUT | `/admin/products/{id}` | ADMIN | 编辑商品基础资料 |
| PATCH | `/admin/products/{id}/publish` | ADMIN | 上架商品 |
| PATCH | `/admin/products/{id}/archive` | ADMIN | 下架归档商品 |

## 4. 错误码

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
| `EMAIL_ALREADY_REGISTERED` | 邮箱已经注册 |
| `INVALID_CREDENTIALS` | 登录凭据错误 |
| `PRODUCT_NOT_FOUND` | 商品不存在或不可见 |
| `INVALID_SKU_PRICE` | SKU 市场价低于销售价 |
| `DATA_CONFLICT` | 唯一键或数据引用冲突 |
