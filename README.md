# 智能电影推荐系统

## 当前基线

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

## 重要说明

- `backend/douban_movies.csv`、`backend/douban_users.csv`、`backend/posters/` 属于本地数据或生成物，默认不会提交。
- 生产环境必须通过环境变量提供数据库密码和 JWT 密钥。
- 当前项目已使用 Spring Boot 3 和 Java 17，并已完成 DTO、Bean Validation、REST v1、统一异常、OpenAPI 和 CORS 接口底座。


## 基线与敏感数据

- 当前基线记录见 `docs/baseline/2026-09-18-baseline.md`。
- 数据库导出、原始 CSV、海报和本地备份默认不进入 Git；需要共享时只提交脱敏的小样本或恢复说明。
- 不要把密码、JWT Secret、LLM API Key、完整 Token 或包含用户隐私的日志提交到仓库、Issue、PR 或截图中。
