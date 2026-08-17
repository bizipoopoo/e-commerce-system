# Stage 09 开源化与社区协作 Code Review

## 审查范围

- Apache-2.0 许可证正文及 Maven、npm、README 元数据一致性
- 公开仓库前的工作区与 Git 历史敏感信息检查
- 贡献指南、行为准则、安全披露政策及 GitHub 社区模板
- Demo/CI 固定凭据与生产凭据边界说明
- 仓库公开、Private vulnerability reporting 和 GitHub 许可证识别验收

## Review 结论

- P0/P1：无遗留问题。
- 仓库具备合法复用、外部贡献、结构化反馈和私密漏洞披露的基础文件。
- 扫描未发现私钥、GitHub Token、云访问密钥、通用 API Key 或提交进历史的 `.env`/证书/密钥库。
- 命中的密码和 JWT 值均为带有 Demo、CI 或 replace-with 语义的非生产固定值；文档与 `prod` 配置明确要求生产环境外部注入凭据。

## Review 发现

| 级别 | 发现 | 处理结果 |
|---|---|---|
| P2 | 初版只在根 `package.json` 增加许可证和仓库地址，`package-lock.json` 根包元数据未同步，依赖审计工具可能读取到不一致信息 | 同步锁文件中的 SPDX `Apache-2.0` 和 repository 元数据，并使用 `npm ci` 验证锁文件一致性 |
| P2 | 仅添加 `SECURITY.md` 不会自动产生私密报告入口，若公开后未启用 Private vulnerability reporting，模板中的安全链接将不可用 | 将 GitHub 安全设置列为公开流程的强制验收项，公开后通过 API 启用并回读状态 |
| P3 | 根包保留 `private: true` 容易被误解为代码仍是私有授权 | 在阶段文档明确该字段只阻止 npm 工作区根包误发布，不影响仓库可见性与 Apache-2.0 授权 |

## 验证结果

- 后端测试：20/20 通过。
- Web：`npm ci` 成功、0 漏洞，商城和运营端生产构建通过。
- Maven effective POM：正确包含 Apache License 2.0。
- npm/package-lock：许可证和 repository 元数据一致。
- GitHub Issue 模板：全部 YAML 解析通过。
- Shell：部署脚本与测试脚本通过 `bash -n`。
- 部署编排：成功、失败回滚和非法标签验证测试通过。
- Git 差异：`git diff --check` 通过。
- 本机未安装 Docker，Compose 解析与完整 smoke 由推送后的 GitHub Actions Docker runner 执行并作为最终门禁。

## 已知限制

- 当前安全扫描为敏感文件名与高置信度凭据特征扫描，不能替代持续 secret scanning；公开后应持续使用 GitHub 的安全能力。
- 项目仍处于 Demo/MVP 阶段，只承诺维护 `main` 最新版本，不承诺历史标签的长期安全支持。
- 当前未要求贡献者签署 CLA 或 DCO；若未来出现多组织协作、商标授权或双重许可需求，应重新评估贡献治理。
- 行为准则事件暂由仓库所有者通过 GitHub 私密联系方式处理；维护团队扩大后应建立独立的社区治理邮箱或表单。
