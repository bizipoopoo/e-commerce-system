# Aurora Commerce 最终回归报告

## 回归结论

截至 2026-08-15，Day 1–Day 7 计划内的 Demo 能力已完成开发与本地回归。后端测试、双端生产构建、核心交易冒烟、商城/运营端浏览器验收、配置语法和 Diff 检查均通过；Stage 07 Review 发现的问题已修复，P0/P1 遗留为 0。

Docker 运行时未安装在当前开发机，因此本地无法执行镜像构建；[GitHub Actions Quality Gate](https://github.com/bizipoopoo/e-commerce-system/actions/workflows/ci.yml) 的 `Compose delivery smoke test` 会在 Linux Docker 环境真实构建四服务栈，并通过商城 Nginx 入口再次执行同一套 smoke。最终交付只在最新 `main` 的三个 job 全部成功后成立。

## 自动化回归矩阵

| 门禁 | 命令/环境 | 结果 |
|---|---|---|
| 后端测试 | `cd backend && mvn --batch-mode test` | 20/20，通过；0 失败、0 错误、0 跳过 |
| 依赖安装 | `npm ci` | 通过；59 个包，0 漏洞 |
| 双端生产构建 | `npm run build` | 商城与运营后台均通过 TypeScript 检查及 Vite 构建 |
| 核心交易冒烟 | `npm run smoke` | 连续两次通过；第二次使用 `local,demo` Profile |
| 脚本语法 | `node --check scripts/smoke-test.mjs` | 通过 |
| YAML 静态解析 | Compose、Actions、demo Profile | 通过 |
| Git Diff | `git diff --check` | 通过 |
| Compose 运行态 | GitHub-hosted Ubuntu + Docker | 由最新 `main` Quality Gate 构建四服务并执行同源代理 smoke |

## 冒烟覆盖

自动化脚本每次生成唯一用户、幂等键和物流单号，覆盖：

1. Actuator 健康检查。
2. 首页与可售商品读取。
3. 客户注册与 JWT。
4. 商品收藏、加入购物车、结算预览。
5. 订单创建、支付单创建与模拟支付完成。
6. 管理员登录与订单发货。
7. 客户查询物流、确认收货、真实购买评价。
8. 运营看板对已完成订单的实时聚合。

本次可观测业务结果：订单金额 ¥429，订单最终状态 `COMPLETED`，运营看板显示 GMV ¥429、支付订单 1、已完成 1。

## 浏览器验收

| 页面/链路 | 验收结果 |
|---|---|
| 商城首页 | Hero、分类、商品卡、选品理念、内容卡及页脚完整渲染，无明显布局错位 |
| 发现频道 | 推荐理由、六个商品候选、三篇真实内容文章正常展示 |
| 运营登录 | `demo` Profile 管理员可登录，证明 Review 修复生效 |
| 经营看板 | smoke 完成后刷新即可读取真实 GMV、订单数量和最新订单 |
| 双端视觉 | 统一品牌色与信息层级清晰，适合桌面 Web 演示 |

## 功能覆盖结论

- 身份与权限：客户注册/登录、管理员登录、JWT、管理接口授权。
- 商品与库存：首页、分类、搜索、SKU、库存预占/扣减/释放和预警。
- 交易与履约：购物车、结算、优惠、订单状态机、模拟支付、发货、物流、确认收货。
- 营销与增长：优惠券、内容流、可解释推荐、行为采集、收藏和评价。
- 信息推流：站内通知、未读状态、SSE 实时消息与断线补偿。
- 售后：仅退款、退货退款、运营审核/驳回、退款记录、库存幂等回补。
- 运营工作台：看板、订单、售后、库存、营销、内容、评价和消息。
- 交付工程：Flyway、模块边界测试、Docker/Compose、部署手册、演示脚本、CI 门禁。

## 已知限制与生产化边界

- 支付、物流、退款和消息发送均为 Demo 级 Mock，不连接真实供应商。
- 单机 Nginx + Spring Boot + MySQL 拓扑适合演示和小规模验收，不包含高可用和水平扩缩容。
- SSE 连接状态保存在单实例内存；多实例需接入 Redis Pub/Sub、消息队列或推送网关。
- 图片来自外部 Unsplash，离线或受限网络下可能加载较慢，但不影响交易数据。
- 正式上线需补齐 HTTPS、密钥服务、数据库备份、可观测性、限流、防刷、细粒度 RBAC、真实支付合规与容量压测。
