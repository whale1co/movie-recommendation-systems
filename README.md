# 智能电影推荐系统

## 项目概览

- Backend: Java 17、Spring Boot 3.3、MyBatis-Plus、Spring Security、Flyway、MySQL
- Frontend: Vue 3、TypeScript、Vite、Pinia、Axios、Element Plus

## 本地运行

1. 安装 JDK 17、Maven、Node.js LTS、MySQL 8 和 Git。
2. 创建 `movie_rec` 数据库，并准备有权限的数据库账号。
3. 复制 `.env.example` 为本地配置参考，填写数据库密码和随机 JWT 密钥。Spring Boot 不会自动读取任意目录下的 `.env` 文件，请通过 IDE Run Configuration、PowerShell 环境变量或启动脚本注入这些变量。
4. 必须设置 `JWT_SECRET`，且至少包含 32 个 UTF-8 字节；项目不再提供不安全的默认密钥。
5. 在 `backend` 目录执行 `mvn test`，再执行 `mvn spring-boot:run` 或 `mvn package` 后运行 JAR。
6. 在 `frontend` 目录执行 `npm ci` 和 `npm run dev`。

Flyway 会在应用启动时执行 `backend/src/main/resources/db/migration` 中的版本迁移。已有数据库使用 `baseline-on-migrate` 建立基线；后续结构变更必须新增版本脚本，不要直接修改已执行的迁移。

## REST API

- 业务接口统一使用 `/api/v1` 前缀，静态海报继续使用 `/api/posters`。
- OpenAPI JSON 位于 `/v3/api-docs`，Swagger UI 位于 `/swagger-ui.html`。
- API 响应包含 `code`、`message`、`data`、`requestId` 和 `timestamp`；HTTP 状态码与响应中的 `code` 保持一致。
- 开发环境默认只允许 `http://localhost:5173` 和 `http://127.0.0.1:5173` 跨域访问。生产环境必须显式设置 `CORS_ALLOWED_ORIGINS`，不接受 `*`。
- 可通过 `--spring.profiles.active=dev|test|prod` 选择环境配置。生产配置只包含环境变量占位符，不包含真实密钥。

## AI 选片顾问

- `POST /api/v1/ai/advisor` 接收问题字段，要求登录且拥有 `ai:chat` 权限。
- 默认 `LLM_PROVIDER=fake`，无需网络即可验证完整 RAG、安全检查、引用回查和降级链路。
- 连接 OpenAI 兼容 API 或 Ollama 时设置 `LLM_PROVIDER=openai-compatible`、`LLM_BASE_URL`、`LLM_MODEL`，需要鉴权的服务另设 `LLM_API_KEY`。
- 后端先解析片长、类型、评分和年份约束，再从本地电影库做文本与 User-CF 融合检索；模型只能解释候选电影，响应中的 `movieId` 均经后端候选集回查。
- 模型不可用、超时、非法 JSON 或返回虚构 ID 时，接口仍返回本地排序结果，并将 `degraded` 设为 `true`。审计记录不包含原始问题、提示词、Token 或 API Key。

## 重要说明

- `backend/douban_movies.csv` 和 `backend/douban_users.csv` 可作为电影与用户数据导入文件使用，`backend/posters/` 可用于存放电影海报资源；是否随项目部署或提交，按照实际运行环境和交付要求安排。
- 生产环境必须通过环境变量提供数据库密码和 JWT 密钥。
- 当前项目已使用 Spring Boot 3 和 Java 17，并已完成 DTO、Bean Validation、REST v1、统一异常、OpenAPI 和 CORS 接口底座。

## Git 协作与 CI

- 从 `develop` 创建 `feature/*` 或 `fix/*` 分支，通过 Pull Request 合并；`main` 只接收通过验收的版本。
- PR 必须通过后端测试/打包、前端依赖审计/测试/构建、Gitleaks Secret 扫描和依赖变更审查。
- 本地提交前运行：`cd backend && mvn test`，以及 `cd frontend && npm ci && npm test && npm run build`。
- 仓库维护者应为 `main` 和 `develop` 启用分支保护，要求 PR、至少 1 次人工批准、全部对话已解决和所需 CI 检查通过。
- PR、Issue、Review 和截图中不得包含密码、密钥、Token、Cookie、真实用户数据或未脱敏日志。

## 配置与数据说明

- 数据库连接、JWT、跨域和 AI 服务参数通过环境变量配置，示例见 `.env.example`。
- 不要把密码、JWT Secret、LLM API Key、完整 Token 或包含用户隐私的日志提交到仓库、Issue、PR 或截图中。
