# Stage 09：开源化与社区协作基线

## 阶段目标

将 Aurora Commerce 从私有演示仓库转换为可合法使用、可安全协作、可持续维护的公开开源项目，同时避免在公开操作中泄露历史凭据。

## 交付范围

- 采用 Apache License 2.0，并在 Maven、npm 元数据和 README 中保持一致。
- 提供贡献指南、行为准则与安全漏洞私密披露政策。
- 提供结构化 Bug、Feature Issue 模板和 Pull Request 检查清单。
- 对全部 Git 历史执行敏感信息特征扫描，区分 Demo/CI 固定值与真实凭据。
- 将 GitHub 仓库设为 Public，启用 Private vulnerability reporting。
- 完成 Code Review、全量质量门禁、推送与 GitHub Actions 验收。

## 开源决策

选择 Apache-2.0 的主要原因：

- 允许个人和企业使用、修改、再分发及商业化；
- 提供明确的贡献者专利授权与专利诉讼终止条款；
- 与 Spring 生态常见的宽松许可证模型相符；
- 保留版权、许可证和变更说明义务，适合后续基于本项目继续演进。

根 `package.json` 的 `private: true` 继续保留。它只用于阻止 npm 误发布工作区根包，不影响 GitHub 仓库或源代码的 Apache-2.0 开源状态。

## 安全公开流程

1. 检查工作区和全部 Git 提交中的私钥、GitHub Token、云访问密钥与常见 API Key 特征。
2. 核对 `.env`、证书、密钥库等高风险文件是否曾进入提交历史。
3. 只保留明确用于本地 Demo/CI 的固定值；生产部署继续要求外部注入强凭据。
4. 推送开源协作基线并等待 CI 成功。
5. 切换仓库可见性为 Public。
6. 启用 GitHub Private vulnerability reporting，并回读仓库、许可证和安全设置状态。

## 社区协作入口

- 普通缺陷：Bug report 模板。
- 产品建议：Feature request 模板。
- 代码贡献：Pull Request 模板和 `CONTRIBUTING.md`。
- 安全漏洞：Security 页面中的私密报告入口，禁止使用公开 Issue。
- 社区行为：`CODE_OF_CONDUCT.md`。

## 后续维护建议

- `main` 启用 Pull Request、至少一名 Reviewer 和必需 CI 检查的分支保护。
- 每次发布继续沿用 Stage 08 的 tag → GHCR → staging → production 流程。
- 定期处理 Dependabot PR，并在引入新依赖时检查许可证兼容性。
- 首次接收外部贡献后，根据维护规模决定是否引入 DCO 或 CLA；当前 Apache-2.0 第 5 条已覆盖默认贡献授权。
