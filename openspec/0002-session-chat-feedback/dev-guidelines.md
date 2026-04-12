# v2.0 开发规范与执行注意事项

> **适用范围**：openspec/0002-session-chat-feedback 全部 Chunk  
> **最后更新**：2026-04-12

---

## 一、开发顺序：先纵向切片，再横向扩展

### 1.1 为什么顺序很重要

这个项目有明确的依赖链，乱序开发会导致大量返工：

```
Phase 0: 基础设施 ← 先做，后面全受益
    │
Phase 1: 后端核心 → 前端跟进
    │         │
    │    ┌────┴────────────────┐
    │    ↓                     ↓
    │  Chunk 2: DB Migration  Chunk 3: LLM重构
    │  + Session数据模型        + Prompt模板
    │    ↓                     ↓
    │  Chunk 4: SessionService ← 依赖 Chunk 2 + 3
    │    ↓
    │  Chunk 5: 前端对话UI ← 依赖 Chunk 4 的API
    │
Phase 2: Chunk 6 应验闭环 ← 依赖 Chunk 4 的 Session 概念
    │
Phase 3: Chunk 7 质量基建 + Chunk 8 RAG升级
```

### 1.2 第一个里程碑

> **"用户发一个问题 → 后端创建Session + 排盘 + 规则 + MockLLM分析 → 返回JSON → 前端渲染一个对话气泡"**

哪怕 Prompt 很粗糙、UI 很简陋都没关系——先证明数据链路是通的。不要在 Prompt 质量上花时间优化，直到链路通了再说。

---

## 二、核心开发规范

### 2.1 Feature Flag 渐进切换

**绝对不要一刀切替换旧代码。** 用 Feature Flag 控制新旧路径：

```yaml
# application.yml
liuyao:
  feature:
    session-api-enabled: ${SESSION_API_ENABLED:false}
    orchestrated-analysis: ${ORCHESTRATED_ANALYSIS:false}
```

旧的 `POST /api/divinations/analyze` 始终保持可用。新 API 开发完一个验证一个，验证通过后翻 Flag 切流量。这样任何时候都有一个能跑的系统。

### 2.2 LLM Mock 先行

开发 SessionService 和对话 UI 时，**全程用 MockLlmClient**：

| 为什么 | 说明 |
|--------|------|
| 速度 | 真 LLM 每次 3-8 秒，Mock 瞬间返回，开发循环快 10 倍 |
| 确定性 | 真 LLM 输出不稳定，Mock 固定返回同一 JSON，方便 debug |
| 成本 | 开发阶段可能调用数百次，Mock 零成本 |
| 可测试 | 单元测试和集成测试必须用 Mock 才能可重复 |

**只在 Prompt 调优阶段接真 LLM。** 切换方式：`spring.profiles.active` 去掉 `test` 即可。

### 2.3 DB Migration 纪律

| 规则 | 说明 |
|------|------|
| **不改历史** | V1~V13 已有的 migration 文件内容不能修改（Flyway checksum 校验） |
| **单一职责** | 每个 migration 只做一件事（建表和加列分两个文件） |
| **先验证** | 每写完一个 migration，先在本地干净库跑一遍确认无冲突 |
| **可回滚** | 对于 ALTER TABLE，写之前想清楚如果回滚怎么办 |
| **命名规范** | `V{N}__{动词}_{对象}.sql`，如 `V14__create_chat_session_and_message.sql` |

### 2.4 前后端并行契约

前后端**不要互相等**。约定好 API 契约后各自开发：

```
后端：先写 Controller 返回 hardcoded JSON → 部署 → 供前端对接
前端：用 src/api/mock.ts 返回本地 Mock 数据 → 开发 UI → 后端好了再切
```

只要 Request/Response 的 JSON 结构定死了，两边可以完全并行。

### 2.5 Prompt 开发的实操节奏

别花太多时间在纸上设计 Prompt。Prompt 的效果**只能靠实际跑卦例来验证**：

```
1. 写一个最简版 System Prompt（30分钟，别追求完美）
2. 用 3 个代表性卦例跑一下，看输出
3. 发现问题 → 针对性修 Prompt → 再跑
4. 循环 5-10 轮后稳定
5. 补 Few-Shot 示例
6. 最后建黄金数据集做回归
```

---

## 三、每个 Chunk 的验证检查点

每完成一个 Chunk，**必须**先通过验证再进入下一个：

| Chunk | 验证命令/方式 | 通过标准 |
|-------|-------------|---------|
| 1. 基础设施 | `docker-compose up postgres` + `mvn test` | PG启动成功；现有测试全过 |
| 2. 数据模型 | `mvn spring-boot:run` + 检查DB表结构 | 应用启动无报错；新表存在 |
| 3. LLM重构 | `mvn test -Dtest=LlmClientTest,ContextWindowBuilderTest` | 新增测试全过 |
| 4. Session服务 | `mvn test -Dtest=SessionServiceTest,SessionApiIntegrationTest` | 创建→追问→关闭链路通 |
| 5. 前端UI | 浏览器手动验证 | 起卦→进入对话页→可追问→可看历史 |
| 6. 应验日历 | API + 前端手动验证 | 事件创建→日历展示→反馈可提交 |
| 7. 质量DevOps | `docker-compose up` + CI绿灯 | 全部服务一键启动；CI全过 |
| 8. RAG升级 | Worker测试 + 手动检查检索质量 | 卦例不被切碎；混合检索有结果 |

---

## 四、容易踩的坑

### 4.1 JSON Schema 输出不稳定

即使设置了 `response_format: json_object`，LLM 仍可能：
- 在 JSON 外面包一层 markdown（```json...```）
- 输出多余字段
- 个别字段类型不对

**应对**：`LlmClient` 内置 JSON 清洗逻辑（去掉 markdown 包裹）+ Schema 校验 + 重试 1 次 + 降级。

### 4.2 Token 溢出

追问到第 7-8 轮时，如果不做裁剪，context 会超限导致 API 报 400。

**应对**：`ContextWindowBuilder` 必须从第 1 天就实现 Token 计数和裁剪，不能"先不管以后再加"。

### 4.3 Session 超时的并发问题

用户可能在 Session 即将超时的瞬间发送了追问。

**应对**：`addMessage()` 方法内先刷新 `lastActiveAt`，再做业务逻辑。使用乐观锁或 `status = 'ACTIVE'` 的 WHERE 条件防止操作已关闭的 Session。

### 4.4 前端状态管理

对话页面的消息列表可能很长，频繁 re-render 会卡。

**应对**：用 `React.memo` 包裹 ChatBubble；消息列表用 `useRef` + 手动 scroll 而非依赖 state 变化。

### 4.5 数据库密码

当前 docker-compose 里 `POSTGRES_PASSWORD: postgres` 是硬编码的。

**应对**：新 docker-compose 必须从 `.env` 读取，`.env.example` 提供模板，`.env` 加入 `.gitignore`。

---

## 五、代码组织约定

### 5.1 新增模块的包结构

```
com.yishou.liuyao.session/
├── controller/
│   └── SessionController.java
├── service/
│   ├── SessionService.java
│   └── SessionPersistenceService.java
├── domain/
│   ├── ChatSession.java
│   └── ChatMessage.java
├── repository/
│   ├── ChatSessionRepository.java
│   └── ChatMessageRepository.java
├── dto/
│   ├── SessionCreateRequest.java
│   ├── SessionCreateResponse.java
│   ├── MessageRequest.java
│   └── MessageResponse.java
└── mapper/
    └── SessionMapper.java
```

每个模块保持与 0001 一致的 `controller/service/domain/repository/dto/mapper` 分层。

### 5.2 命名约定

| 类别 | 约定 | 示例 |
|------|------|------|
| Entity | 名词 | `ChatSession`, `VerificationEvent` |
| Service | 名词 + Service | `SessionService`, `VerificationEventService` |
| Controller | 名词 + Controller | `SessionController` |
| DTO | 动词/名词 + Request/Response/DTO | `SessionCreateRequest`, `AnalysisOutputDTO` |
| 测试 | 被测类名 + Test | `SessionServiceTest` |

### 5.3 错误码规范

```java
// 新 Session 模块的错误码段：2xxxx
SESSION_NOT_FOUND(20401, "Session不存在"),
SESSION_CLOSED(20901, "该会话已关闭，请开启新会话"),
SESSION_MESSAGE_LIMIT(20902, "单次会话消息数已达上限"),

// 新 Calendar 模块的错误码段：3xxxx
VERIFICATION_EVENT_NOT_FOUND(30401, "应验事件不存在"),
FEEDBACK_ALREADY_SUBMITTED(30902, "反馈已提交，不可重复提交"),

// 限流错误码
RATE_LIMIT_EXCEEDED(42901, "今日请求次数已达上限"),
```

---

## 六、Git 分支策略

```
main ─────────────────────────────────────── 稳定可发布
 │
 └── feature/v2-session ──────────────────── 所有 v2.0 开发
      │
      ├── chunk/infrastructure ────────────── Phase 0 基础设施
      ├── chunk/db-migration ──────────────── Chunk 2 数据模型
      ├── chunk/llm-refactor ──────────────── Chunk 3 LLM重构
      ├── chunk/session-service ────────────── Chunk 4 Session服务
      ├── chunk/frontend-chat ─────────────── Chunk 5 前端对话
      ├── chunk/verification ──────────────── Chunk 6 应验日历
      ├── chunk/devops ────────────────────── Chunk 7 质量DevOps
      └── chunk/rag-upgrade ───────────────── Chunk 8 RAG升级
```

**规则**：
- 每个 Chunk 一个子分支，从 `feature/v2-session` 开出
- Chunk 验证通过后合入 `feature/v2-session`
- 所有 Chunk 完成后，`feature/v2-session` 合入 `main`
- 任何时候 `main` 上跑的都是可工作的 v1.0 系统
