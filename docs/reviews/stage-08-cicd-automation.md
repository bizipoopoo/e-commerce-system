# Stage 08 CI/CD 自动化 Code Review

## 审查范围

- PR/main Quality Gate 的并发、超时、失败诊断和回归覆盖
- 语义化标签发布、main 来源约束、GHCR 权限和三镜像版本一致性
- 多架构镜像、BuildKit cache、SBOM 与 provenance
- reusable deployment、GitHub Environment、Secrets 作用域和 production 审批
- SSH known-host、参数校验、远端目录权限和命令注入风险
- staging/production 的目录、Compose project、容器、端口和数据卷隔离
- registry 标签可变性、digest 固定、部署状态、并发锁、健康检查与回滚
- Flyway migration 与镜像回滚边界
- Dependabot、服务器 Runbook 和首次启用流程

## Review 结论

- P0/P1：修复后无遗留问题。
- 普通 PR/main 不会发布镜像或访问部署 Secrets；只有版本标签触发发布，部署必须经 Environment。
- production 不接受仅凭标签重新解析镜像，必须输入 staging 摘要中的三个 digest，保证同一内容晋级。
- 自动回滚恢复上一组三个 digest，但仍返回失败状态，不会用“回滚成功”掩盖本次发布故障。

## Review 发现

| 级别 | 发现 | 处理结果 |
|---|---|---|
| P1 | 初版 staging 与 production 共用 `/opt/aurora-commerce` 和固定 Compose project，同主机会互相替换容器和数据卷 | 环境名经白名单校验后贯穿独立目录、Compose project、workflow concurrency 与部署脚本；两套环境可同机隔离运行 |
| P1 | `sha-<commit>` 是可追踪标签但 Registry 标签仍可移动，不能作为真正不可变发布物；标签变化还会影响回滚 | workflow 将三个镜像分别解析为 manifest digest，远端 Compose 只运行 `image@sha256`；`.release-state` 保存上一组三个 digest，production 强制复用 staging 摘要中的 digest |
| P1 | 只校验标签格式仍允许从未合入的临时分支创建版本并发布 | Release checkout 完整历史并验证 tag commit 必须是 `origin/main` 的 ancestor；文档同时要求用 ruleset 保护 `v*` 标签 |
| P2 | reusable workflow 初版使用 `secrets: inherit`，会传递调用方可见的全部 Secrets | 移除继承；SSH Secrets 只由 deploy job 绑定的目标 GitHub Environment 注入 |
| P2 | 发布专用 Compose 只有通用 YAML 解析，缺少 Docker Compose 对变量和 schema 的真实展开校验 | 常规 Quality Gate 增加带占位 digest/Secrets 的 `docker compose -f deployment/compose.release.yml config --quiet` |
| P2 | 部署失败自动回滚属于关键发布控制，初版仅能靠真实服务器验证 | 增加 fake-Docker 自动化测试，覆盖部署成功、健康失败、精确回滚、历史状态、非法标签/环境和可移动镜像引用拒绝 |
| P2 | SSH 配置错误时可能尝试额外身份或等待交互式认证，失败反馈不够快 | 连接前验证全部 Environment Secrets 和端口范围；SSH/SCP 强制 `BatchMode`、`IdentitiesOnly` 与 15 秒连接超时 |
| P2 | 首次推送后 Dependabot 立即创建 9 个 PR，其中包含 Spring Boot 4、Node 26、TypeScript 7 等 major 升级；根目录 Docker 扫描还产生失败运行 | 所有生态忽略 semver major 并设置 PR 上限；移除不兼容的根 Docker 扫描，major 升级改为季度迁移评估 |
| P3 | macOS 默认没有 Linux `flock`，本地直接运行远端部署脚本会在锁阶段失败 | 测试显式模拟锁；实施手册明确目标 Linux 必须安装 `util-linux/flock`，该脚本不作为 macOS 部署入口 |

## 验证结果

- `actionlint v1.7.12`：全部 GitHub Actions workflow 通过。
- YAML：CI、Release、Deploy、Dependabot 和 release Compose 解析通过。
- Shell：`deployment/deploy.sh`、`scripts/test-deploy.sh` 通过 `bash -n`。
- 部署测试：成功、失败回滚和输入拒绝场景通过。
- 后端：20/20 测试通过；npm 安装 0 漏洞；商城和运营端生产构建通过。
- Compose smoke 与最终 GitHub Actions 结果在推送后执行并记录。

## 已知限制

- 尚未配置真实 staging/production 主机、域名、Environment Secrets 和审批人，因此本阶段不宣称公网环境已上线。
- 未主动创建 `v*` 标签，避免在项目方确认首个版本号前向 GHCR 发布正式包；Release workflow 将在首次标签时端到端验证。
- 当前目标仍是单机 Compose；多节点调度、零停机滚动、流量切换和跨区容灾需要 Kubernetes/云部署适配器。
- 镜像构建已生成 SBOM/provenance，但漏洞阈值扫描与签名准入策略尚未启用。
- Actions 使用官方主版本标签并由 Dependabot 维护；更高供应链等级可进一步固定到完整 commit SHA。
- Flyway 只向前迁移，破坏性 schema 变更不能依赖镜像自动回滚，必须使用 expand/contract 和数据库恢复 Runbook。
