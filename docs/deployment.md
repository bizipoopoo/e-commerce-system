# Aurora Commerce 部署手册

## 1. 适用范围

本文用于单机 Demo、项目验收和后续开发基座部署。Compose 拓扑包含 MySQL、Spring Boot 后端、商城 Nginx 和运营后台 Nginx。两个前端均通过同源 `/api` 代理后端，浏览器不需要额外跨域配置。

生产上线还需补充 HTTPS、域名、密钥托管、数据库备份、日志采集和监控告警；当前配置不应直接作为公网生产方案。

## 2. 前置条件

- Docker Engine 24+，并包含 Docker Compose v2
- 至少 4 GB 可用内存
- 本机端口 `3000`、`3001`、`8080` 可用，或在 `.env` 中修改映射
- 仅运行回归脚本时需要 Node.js 20+

## 3. 一键启动

```bash
cp .env.example .env
```

编辑 `.env`，至少替换：

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`：不少于 32 个随机字节
- 对外分享环境时同时替换 `DEMO_ADMIN_PASSWORD`

然后执行：

```bash
docker compose up --build -d
docker compose ps
```

服务在健康检查通过后按依赖顺序启动。首次构建需要下载 Maven、npm 和镜像依赖，耗时取决于网络。

## 4. 验证与访问

```bash
curl http://localhost:8080/actuator/health
npm run smoke
```

若修改了映射端口，可指定测试入口，例如：

```bash
SMOKE_BASE_URL=http://localhost:3000 npm run smoke
```

默认入口：

| 服务 | 地址 | 说明 |
|---|---|---|
| 商城 | `http://localhost:3000` | 客户浏览、交易、售后与互动 |
| 运营后台 | `http://localhost:3001` | 管理员登录与运营工作台 |
| 后端 | `http://localhost:8080` | API 与健康检查 |

演示管理员：`admin@aurora.local` / `Aurora@2026`，实际值以 `.env` 为准。

## 5. 日常运维

查看状态与日志：

```bash
docker compose ps
docker compose logs -f backend
```

停止并保留数据库：

```bash
docker compose down
```

重新启动：

```bash
docker compose up -d
```

仅当确认要清空所有 Demo 订单、用户和业务数据时，才执行 `docker compose down -v`。该命令会删除 Compose 管理的 MySQL 数据卷，无法从本项目恢复。

## 6. Profile 与数据隔离

- `local`：H2 内存库，加载演示数据，适合本地开发。
- `prod,demo`：Compose 默认组合，使用 MySQL，并显式加载演示数据和演示管理员。
- `prod`：生产基线，只运行正式 Flyway migration，不加载演示数据或管理员。

正式部署应移除 `demo` Profile，使用受管 MySQL 和密钥服务，并通过环境变量提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`。

## 7. 常见问题

- 后端一直等待：先用 `docker compose logs mysql` 检查数据库健康状态和密码配置。
- 前端出现 502：用 `docker compose ps` 确认 backend 已为 healthy，再看后端日志。
- 端口冲突：修改 `.env` 中 `BACKEND_PORT`、`STOREFRONT_PORT`、`ADMIN_PORT`。
- 修改了数据库凭据后无法启动旧数据卷：恢复原凭据，或在确认无需数据后删除数据卷并重建。
