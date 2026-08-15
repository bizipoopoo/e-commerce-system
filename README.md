# Aurora Commerce

Aurora Commerce 是一套面向 Web 的全功能电商演示系统。项目采用“核心交易真实闭环、外围能力可演示、模块边界可扩展”的建设策略，用一周形成可运行、可讲解、可继续演进的产品基座。

## 当前基线

- 后端：Java 21、Spring Boot、Spring Modulith、Spring Security、JPA、Flyway
- 用户端：Vue 3、TypeScript、Vite、Pinia
- 运营端：Vue 3、TypeScript、Vite
- 本地默认：H2 内存数据库，无需 Docker
- 演示部署：MySQL 8.4、Docker Compose、Nginx 同源反向代理

## 工程结构

```text
backend/             Spring Boot 模块化单体
apps/storefront/     用户商城 Web
apps/admin-web/      运营管理 Web
docs/                PRD、架构、数据模型与 API 设计
```

## 快速启动

```bash
# 后端
cd backend
mvn spring-boot:run

# 安装前端依赖（项目根目录）
npm install

# 商城用户端
npm run dev:storefront

# 运营后台
npm run dev:admin
```

默认地址：

- 后端 API：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- 商城用户端：`http://localhost:5173`
- 运营后台：`http://localhost:5174`

本地默认使用 `local` Profile，并自动加载演示商品及管理员：

- 邮箱：`admin@aurora.local`
- 密码：`Aurora@2026`

### Docker 一键演示

```bash
cp .env.example .env
# 编辑 .env，替换数据库密码和 JWT_SECRET
docker compose up --build -d
npm run smoke
```

容器启动后访问：

- 商城：`http://localhost:3000`
- 运营后台：`http://localhost:3001`
- 后端健康检查：`http://localhost:8080/actuator/health`

Docker 演示环境默认管理员仍为 `admin@aurora.local` / `Aurora@2026`。详见[部署手册](docs/deployment.md)。生产环境必须仅启用 `prod` Profile，并提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和不少于 32 字节的随机 `JWT_SECRET`；生产 Profile 不创建演示管理员，也不加载演示数据。

## 质量门禁

```bash
cd backend && mvn test
cd .. && npm ci && npm run build
npm run smoke  # 需先启动本地后端或 Compose 环境
```

GitHub Actions 对每次 `main` 推送和 Pull Request 自动执行后端测试、双端生产构建、Compose 配置校验和核心交易冒烟。

版本发布使用语义化标签，例如 `v0.2.0`。`Release Images` 会向 GHCR 发布后端、商城和运营端多架构镜像；`Deploy Environment` 负责 staging/production 审批、digest 固定、远端健康检查与失败回滚。启用步骤见 [CI/CD 自动化实施方案](docs/cicd-implementation.md)。

## 产品与技术文档

- [产品需求文档](docs/01-prd.md)
- [系统架构](docs/02-architecture.md)
- [数据模型](docs/03-data-model.md)
- [API 设计](docs/04-api-design.md)
- [一周交付计划](docs/05-delivery-plan.md)
- [阶段研发与 Code Review 流程](docs/06-engineering-workflow.md)
- [部署手册](docs/deployment.md)
- [10 分钟演示脚本](docs/demo-script.md)
- [最终回归报告](docs/final-regression-report.md)
- [CI/CD 自动化实施方案](docs/cicd-implementation.md)
- [Stage 02：身份、商品与首页数据化](docs/stages/stage-02-identity-catalog.md)
- [Stage 03：库存、购物车与结算预览](docs/stages/stage-03-inventory-cart-checkout.md)
- [Stage 04：订单、支付与履约](docs/stages/stage-04-orders-payments-fulfillment.md)
- [Stage 05：营销、内容、推荐与消息](docs/stages/stage-05-marketing-content-recommendation-notifications.md)
- [Stage 06：售后、互动与运营工作台](docs/stages/stage-06-after-sales-engagement-operations.md)
- [Stage 07：部署、演示与交付](docs/stages/stage-07-delivery-deployment-demo.md)
- [Stage 08：CI/CD 自动化研究与实施](docs/stages/stage-08-cicd-automation.md)

Day 1–Day 7 的 Demo 计划功能已实现，Stage 08 在此基础上补充版本镜像、环境审批、自动部署与回滚；各阶段 Code Review 记录位于 [`docs/reviews`](docs/reviews)。
