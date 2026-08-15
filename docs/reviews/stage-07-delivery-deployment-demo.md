# Stage 07 部署、演示与交付 Code Review

## 审查范围

- Spring Boot、商城和运营端多阶段 Dockerfile
- MySQL、后端与双前端 Docker Compose 编排、健康检查和持久化
- `prod`/`demo` Profile 隔离、演示管理员与演示数据加载
- Nginx SPA 托管、API/Actuator 反向代理和 SSE 代理行为
- Node 核心交易冒烟脚本的真实性、失败检测、隔离性和可重复性
- GitHub Actions Compose 交付门禁
- README、部署手册、10 分钟演示脚本与最终回归报告
- 商城首页、发现频道和运营看板的运行态浏览器验收

## Review 结论

- P0/P1：修复后无遗留问题。
- Demo Profile 必须显式启用；只启用 `prod` 不会创建演示管理员或加载演示数据。
- 冒烟使用真实 API 和状态机，不通过数据库直写或 Mock 响应伪造成功。
- 运行态 Compose 验证已纳入 GitHub Actions，覆盖从镜像构建、MySQL migration、容器健康到 Nginx 同源代理的完整交付面。

## Review 发现

| 级别 | 发现 | 处理结果 |
|---|---|---|
| P1 | `DemoAdminInitializer` 仅标注 `local` Profile，Compose 使用 `prod,demo` 时不会创建演示管理员，导致运营登录和 smoke 失败 | Profile 改为 `local` 或 `demo`；以 `local,demo` 真实启动并成功执行管理员登录、完整 smoke 和浏览器运营看板验收 |
| P2 | 初版 CI 仅执行 `docker compose config`，核心 smoke 仍运行在本地 H2 进程，不能证明 Dockerfile、MySQL、Profile 与 Nginx 能协同 | CI 改为真实 `docker compose up --build --detach`，经商城 `:3000` 代理执行 smoke，失败输出全量容器状态/日志，结束后清理数据卷 |

## 验证结果

- 后端：`mvn --batch-mode test` 通过，共 20 个测试。
- 前端：`npm ci` 为 0 漏洞；商城与运营端 TypeScript 检查和生产构建通过。
- 冒烟：本地连续两次完整通过；覆盖注册、收藏、购物车、结算、订单、支付、发货、收货、评价和看板。
- Profile：使用 `local,demo` 启动，日志确认两个 Profile 生效且演示管理员创建成功。
- 浏览器：商城首页/发现频道完整；运营端登录成功；看板读取 smoke 订单并显示 ¥429 GMV、支付订单 1、已完成 1。
- 配置：smoke 脚本语法、Compose/Actions/application-demo YAML 静态解析和 `git diff --check` 通过。
- Docker：当前开发机无 Docker CLI，完整镜像构建与 Compose 运行态由推送后的 GitHub Actions 验证。

## 已知限制

- Compose 是单机 Demo 拓扑，没有 HTTPS、负载均衡、集中日志、备份任务和服务级监控告警。
- 演示管理员和演示业务数据属于 `demo` Profile，正式环境不得启用该 Profile。
- Compose 首次构建需从 Maven Central、npm registry 和镜像仓库下载依赖，需要可用网络。
- smoke 会写入独立客户、订单和评价，仅应对本地/验收 Demo 环境执行，不应指向生产环境。
