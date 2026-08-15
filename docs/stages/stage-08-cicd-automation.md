# Stage 08：CI/CD 自动化研究与实施

## 阶段目标

在 Stage 07 可部署 Demo 基础上，建立从代码验证、版本镜像、环境审批到远端部署和失败回滚的自动化基线，使发布过程可重复、可审计、可晋级。

## 交付范围

- CI concurrency、job timeout、失败日志归档和回滚编排测试。
- 基于语义化 Git 标签的三镜像 GHCR 发布。
- amd64/arm64 镜像、commit SHA 标签、部署时 digest 固定、SBOM 和 provenance。
- staging 自动部署开关与 production Environment 审批。
- SSH known-host 校验、发布前镜像存在性验证和最小权限。
- staging/production 目录、Compose project、容器和数据卷隔离。
- 远端部署锁、同源链路健康检查、部署历史和失败自动回滚。
- GitHub Actions、Maven、npm 和 Docker 基础镜像 Dependabot。
- 服务器初始化、Secrets、版本发布、晋级和数据库回滚边界文档。

## 验收条件

- 所有 workflow 通过 `actionlint` 与 YAML 解析。
- 部署与测试脚本通过 `bash -n`。
- 自动化测试证明成功部署、健康失败、回滚成功和非法标签拒绝。
- 常规后端测试、前端构建和 Compose smoke 不回归。
- Code Review P0/P1 清零。
- `main` 推送后 Quality Gate 全部成功。
- 未配置服务器时 release/deploy workflow 不会被普通 `main` 推送误触发。

## 启用条件

代码侧 CD 已就绪，但真实环境部署需要项目方提供或确认：

1. staging/production Linux 主机与域名。
2. GitHub Environments、审批人和环境 Secrets。
3. 服务器只读 GHCR 凭据。
4. HTTPS 入口与运营后台访问控制策略。
5. production 数据库备份与恢复 Runbook。

在这些外部条件完成前，本阶段交付的是可验证的持续交付能力，不宣称生产环境已上线。
