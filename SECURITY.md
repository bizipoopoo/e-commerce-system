# Security Policy

## Supported versions

Aurora Commerce 当前处于 Demo/MVP 阶段，仅维护 `main` 分支的最新版本。历史提交和旧标签不提供安全更新承诺。

## Reporting a vulnerability

请使用 GitHub 仓库 Security 页面中的 **Report a vulnerability** 私密报告安全问题，不要创建公开 Issue、Discussion 或 Pull Request，也不要在报告中包含真实用户数据或生产凭据。

报告建议包含：

- 受影响的提交、版本、模块或接口；
- 可复现步骤和最小化验证样例；
- 潜在影响与利用前提；
- 建议修复方案（如有）。

维护者会尽量在 5 个工作日内确认收到报告，并在完成影响评估后同步修复计划。请在修复发布前避免公开漏洞细节。

## Deployment responsibility

仓库中的默认账号、密码和密钥只用于本地演示或 CI，不得用于生产环境。部署方必须更换所有凭据、仅启用 `prod` Profile、启用 HTTPS，并自行承担生产环境的配置加固、备份、监控和合规责任。
