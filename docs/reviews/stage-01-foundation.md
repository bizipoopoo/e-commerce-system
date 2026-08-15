# Stage 01 工程基线 Code Review

## 审查范围

- 提交：`b1dea8b chore: initialize Aurora commerce platform`
- 后端 Spring Boot / Spring Modulith 骨架
- 用户商城与运营后台首页
- PRD、架构、数据模型、API 和交付计划
- 本地运行、依赖锁定与构建配置

## Review 发现

| 级别 | 发现 | 处理结果 |
|---|---|---|
| P1 | Security 配置放行了全部 `/api/v1/**`，后续新增管理接口时可能被意外公开 | 改为仅匿名放行健康检查和系统概览 |
| P1 | Vue、Vite 和插件使用 `latest`，干净安装可能获得不兼容版本 | 固定为已验证版本并保留 lockfile |
| P2 | README 声明 Pinia/ECharts 已在技术栈中，但工程尚未安装 | README 改为当前真实依赖，功能引入时再补充 |
| P2 | CORS 来源硬编码在 Java 中，部署到其他域名必须改代码 | 改为配置属性并支持环境变量覆盖 |
| P2 | 未认证的 REST 请求返回 403，无法区分“未登录”和“无权限” | 配置认证入口点，未认证统一返回 401 |
| P2 | 阶段完成缺少仓库级自动化门禁和 Review 留痕 | 新增 GitHub Actions 与阶段研发流程 |

## 验证结果

- `cd backend && mvn test`：通过，4 个测试，包含模块边界和认证授权回归。
- `npm ci`：通过，依赖审计 0 个漏洞。
- `npm run build`：通过，商城端和运营端均完成生产构建。
- `git diff --check`：通过。
- 远端校验：在本报告随代码推送后执行并记录到阶段完成汇报。

## 剩余风险

- 当前前端商品图片和字体依赖外部 CDN；正式演示前应转为本地静态资产。
- 当前页面以静态演示数据为主，Stage 02 开始接入真实 API。
- 身份认证尚未实现，除公开概览外的 API 默认拒绝访问。
