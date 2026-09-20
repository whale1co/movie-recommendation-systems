# 电影推荐系统阶段 4 真实 AI 联调交接文档

> 项目路径：`D:/个人项目/movie_recommendation_systems`
> 联调日期：2026-09-20
> 主题：DeepSeek OpenAI 兼容接口真实调用验收

## 1. 本轮结论

阶段 4 的真实 AI 接口联调已成功完成。后端使用 DeepSeek OpenAI 兼容 API 完成了登录、额度检查、RAG 检索、模型推荐解释、电影引用回查和临时用户清理的完整链路。

真实调用结果：

```text
AI_CODE=200
PROVIDER=openai-compatible:deepseek-v4-pro
AI_GENERATED=True
DEGRADED=False
COUNT=5
ALL_REFERENCES_VALID=True
TEMP_USER_CLEANED=True
```

本轮实际返回并回查成功的电影 ID：

```text
4, 16, 17, 64, 12
```

API Key、数据库密码和 JWT Secret 均未写入仓库、日志或交接文档。用于启动本轮后端的两个 DPAPI 临时凭据文件已删除。

## 2. 真实调用配置

```text
LLM_PROVIDER=openai-compatible
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-v4-pro
LLM_API_KEY=通过进程环境变量注入，未写入文件
```

数据库使用本地环境变量注入的 `DB_PASSWORD`。后端实例当前运行在 `http://localhost:8080`，健康接口 `GET /api/v1/health` 返回 HTTP 200。

AI 接口：

```text
POST /api/v1/ai/advisor
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "question": "想找一部适合周末和父母一起看的电影，两个小时以内，不要恐怖片，节奏轻松，评分高一些。"
}
```

## 3. 已完成的真实链路

```text
创建临时用户
  -> 登录获取 Access Token
  -> 调用 /api/v1/ai/advisor
  -> DeepSeek 真实模型调用
  -> 后端校验结构化推荐
  -> 逐个调用电影详情接口回查 movieId
  -> 删除临时用户
```

本轮确认：

- AI 接口返回 HTTP 200。
- `aiGenerated=true`，不是本地降级结果。
- `degraded=false`，模型调用和输出校验均成功。
- 返回 5 部电影，全部能从本地数据库查询到。
- 临时用户已通过 `DELETE /api/v1/users/me` 注销。

## 4. 本轮代码调整

- OpenAI 兼容客户端支持 `message.content` 为文本或数组。
- 推理模型返回 `message.reasoning_content` 时可以读取该字段。
- 响应带 Markdown 代码围栏或额外说明时，后端提取 JSON 对象再解析。
- LLM 默认最大输出 token 从 800 提高到 2048。
- AI 审计增加脱敏降级原因，不记录模型原文、用户问题或密钥。
- 明确标注 `AiUsageLimitService` 和 `LoginAttemptService` 的 Spring 注入构造器。

## 5. 自动化验证

```text
mvn.cmd -q test
41 tests, 0 failures, 0 errors

mvn.cmd -q -DskipTests package
成功生成 target/movie-rec-backend-1.0.0.jar
```

覆盖提示注入、超长输入、Fake LLM、非法 JSON、超时重试、幻觉 ID 过滤、模型降级、401/403、完整 Spring 上下文，以及阶段 1~3 回归。

## 6. 安全状态

- API Key 没有写入项目文件。
- `.env.example` 只包含空的 `LLM_API_KEY` 占位符。
- `%TEMP%/movierec-llm-key.dpapi` 已删除。
- `%TEMP%/movierec-db-password.dpapi` 已删除。
- 临时测试用户已注销。
- 未保存完整 Access Token、Refresh Token、Cookie 或数据库密码。

## 7. 新对话继续方式

新对话请先阅读：

```text
docs/阶段3完成交接文档.md
docs/阶段4完成交接文档.md
docs/阶段4真实AI联调交接文档.md
```

下一阶段建议进入阶段 5：

1. 实现 AI 选片顾问前端页面和 API 模块。
2. 展示回答、结构化意图、推荐理由和真实电影引用卡片。
3. 支持详情跳转、收藏和评分入口。
4. 处理加载、取消、重试、429、503 和降级状态。
5. 模型内容只按纯文本渲染，不使用未经处理的 `v-html`。
6. 分别验证 Fake 和 DeepSeek 真实模式。

## 8. 重要边界

- 当前 AI 顾问是单轮接口，没有持久化 AI 会话。
- 当前 RAG 使用 SQL 过滤、电影文本匹配、User-CF 和评分/热度融合，未引入独立向量数据库。
- 多实例部署时，内存限流应迁移到 Redis。
- 生产环境必须使用 HTTPS，并设置 `JWT_REFRESH_COOKIE_SECURE=true`。
- 不要把真实密钥、数据库密码或临时 DPAPI 文件提交到 Git。
