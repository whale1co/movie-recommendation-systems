# 实验三：Git 协作与持续集成

> 完成日期：2026-09-20
> 仓库：`https://github.com/whale1co/movie-recommendation-systems`
> 目标：使用真实分支、Pull Request、人工 Review、冲突解决和 GitHub Actions 建立可审计的协作闭环。

## 1. 分支与提交约定

长期分支：

- `main`：可演示、可发布版本。
- `develop`：阶段成果集成分支。

短期分支：

- `feature/*`：功能或工程能力。
- `fix/*`：缺陷和安全修复。

提交信息使用 `feat:`、`fix:`、`test:`、`ci:`、`docs:`、`chore:` 等前缀。每个提交只表达一个可解释、可回退的目标。禁止直接向受保护的 `main` 或 `develop` 推送业务改动。

## 2. Pull Request 流程

1. 从最新 `develop` 创建功能分支。
2. 在本地完成测试、构建和敏感信息自查。
3. 推送功能分支，创建目标为 `develop` 的 Pull Request。
4. 填写变更、验证、风险、回滚和 Review 重点，关联 Issue。
5. 等待 GitHub Actions 全绿，并由另一位真实人员完成至少一次 Review。
6. 处理 Review 意见和全部讨论，再合并 PR。
7. 阶段验收后由 `develop` 向 `main` 提交发布 PR。

AI 可以辅助检查代码和整理修改建议，但不能冒充人工批准。PR 作者不能用自己的评论代替独立 Review。

## 3. CI 门禁

工作流文件：`.github/workflows/ci.yml`。

| 检查 | 内容 | 失败处理 |
| --- | --- | --- |
| Backend test and package | Java 17、MySQL 8、`mvn test`、`mvn -DskipTests package` | 修复编译、测试或迁移问题 |
| Frontend test and build | Node 20、`npm ci`、`npm audit`、Vitest、Vue 类型检查和 Vite 构建 | 修复依赖、测试、类型或构建问题 |
| Secret scan | Gitleaks 扫描完整 Git 历史 | 撤销并轮换真实密钥，再清理历史 |
| Dependency review | Trivy 扫描 Maven/npm 依赖中的 HIGH/CRITICAL 漏洞 | 升级、替换或移除依赖 |

后端固定使用 `LLM_PROVIDER=fake`，CI 不访问真实模型，也不需要 `LLM_API_KEY`。数据库密码和 JWT Secret 是工作流内仅供隔离 CI 服务使用的非生产值，不使用本地或生产凭据。

## 4. 本地验证结果

阶段 6 实施时的结果：

```text
mvn.cmd --batch-mode --no-transfer-progress test
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0

npm test
Test Files: 2 passed
Tests: 4 passed

npm run build
vue-tsc passed
Vite production build passed

npm audit --audit-level=high
found 0 vulnerabilities
```

Windows 本机的后端 `package` 步骤因正在运行的后端进程占用 `target/movie-rec-backend-1.0.0.jar` 而无法重命名旧 JAR。测试和编译均已通过；GitHub Actions 使用全新 Ubuntu Runner，不存在该文件占用。需要本机复验时，先安全停止旧后端进程，再运行打包命令。

## 5. 分支保护设置

在 GitHub 仓库的 `Settings -> Branches` 或 Rulesets 中分别为 `main`、`develop` 配置：

- Require a pull request before merging。
- Require at least 1 approval。
- Dismiss stale approvals when new commits are pushed。
- Require conversation resolution before merging。
- Require status checks to pass before merging。
- Require branches to be up to date before merging。
- 禁止 force push 和删除受保护分支。

所需状态检查选择：

```text
Backend test and package
Frontend test and build
Secret scan
Dependency review
```

状态检查通常要在工作流首次运行后才出现在选择列表中。本仓库为私有仓库，未启用 GitHub Advanced Security，官方 Dependency Review Action 不可用，因此改用不依赖该付费能力的 Trivy 文件系统依赖扫描；前端 job 仍额外执行 `npm audit`。

## 6. 无害冲突演示

冲突只允许发生在专用演示文件或文档行，不在业务代码、迁移脚本或配置中制造。推荐过程：

1. 从同一基线创建 `demo/conflict-source` 和 `demo/conflict-target`。
2. 两个分支分别修改同一演示文件的同一行并提交。
3. 合并时确认 Git 真实产生冲突，截图保留冲突标记和分支名。
4. 人工选择最终内容，删除 `<<<<<<<`、`=======`、`>>>>>>>` 后提交合并。
5. 再运行 `git diff --check` 和必要测试，并保存提交图截图。

不得手写一段冲突标记后声称解决过冲突。课程证据应包含真实分支、两个父提交和冲突解决提交。

## 7. 证据清单

以下证据必须来自真实 GitHub 页面，并在内容脱敏后截图：

- 功能分支和提交历史。
- PR 描述及关联 Issue。
- 四项 CI 检查结果。
- 另一位人员的 Review 意见或批准。
- Review 意见对应的修复提交。
- 分支保护规则。
- 无害冲突发生、解决后的提交图和最终文件。

本地文件、模板或 AI 建议不能替代 PR、人工 Review、Actions 运行和平台分支保护证据。
