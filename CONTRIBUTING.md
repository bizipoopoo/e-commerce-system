# Contributing to Aurora Commerce

感谢你参与 Aurora Commerce。提交代码前，请先搜索现有 Issue 和 Pull Request，避免重复工作；较大的功能或架构调整建议先创建 Discussion 或 Issue 对齐范围。

## 本地开发

环境要求：Java 21、Maven 3.9+、Node.js 20+、npm 10+。完整启动方式见 [README](README.md)。

提交前请运行：

```bash
cd backend && mvn test
cd .. && npm ci && npm run build
```

涉及部署流程时，还需运行：

```bash
bash scripts/test-deploy.sh
docker compose -f compose.yml config --quiet
docker compose -f deployment/compose.release.yml --env-file deployment/release.env.example config --quiet
```

## 提交变更

1. 从最新的 `main` 创建功能分支。
2. 一个 Pull Request 聚焦一个主题，并补充或更新对应测试和文档。
3. 使用清晰的 Conventional Commits 风格提交信息，例如 `feat(cart): add quantity limit`。
4. 完整填写 Pull Request 模板，说明影响范围、验证结果和安全/数据迁移影响。
5. 确认 GitHub Actions 全部通过并处理 Review 意见。

## 工程约定

- 后端保持模块化单体边界，跨模块通过公开 API 或领域事件协作。
- API 变更同步更新 `docs/04-api-design.md`，数据模型变更同步更新 `docs/03-data-model.md`。
- 不提交真实凭据、`.env`、生产数据或可识别个人身份的信息。
- 新依赖应说明用途并优先选择维护活跃、许可证兼容的组件。
- UI 变更应同时检查商城与运营端的桌面 Web 体验。

提交贡献即表示你同意按本项目的 [Apache License 2.0](LICENSE) 授权该贡献。

参与本项目时请遵守 [行为准则](CODE_OF_CONDUCT.md)。安全问题请勿提交公开 Issue，参见 [安全政策](SECURITY.md)。
