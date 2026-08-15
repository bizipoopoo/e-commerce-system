# Aurora Commerce

Aurora Commerce 是一套面向 Web 的全功能电商演示系统。项目采用“核心交易真实闭环、外围能力可演示、模块边界可扩展”的建设策略，用一周形成可运行、可讲解、可继续演进的产品基座。

## 当前基线

- 后端：Java 21、Spring Boot、Spring Modulith、Spring Security、JPA、Flyway
- 用户端：Vue 3、TypeScript、Vite
- 运营端：Vue 3、TypeScript、Vite
- 本地默认：H2 内存数据库，无需 Docker
- 完整环境：MySQL、Redis，使用 Docker Compose

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

## 产品与技术文档

- [产品需求文档](docs/01-prd.md)
- [系统架构](docs/02-architecture.md)
- [数据模型](docs/03-data-model.md)
- [API 设计](docs/04-api-design.md)
- [一周交付计划](docs/05-delivery-plan.md)
- [阶段研发与 Code Review 流程](docs/06-engineering-workflow.md)
