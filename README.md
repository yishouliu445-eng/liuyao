# liuyao v2.0

六爻 AI 断卦系统正在从“单次起卦工具”升级为“多轮对话 + 应验反馈平台”。

当前仓库包含 3 个核心子项目：

- `liuyao-app`：Spring Boot 后端，负责起卦、Session 会话、多轮分析、案例留痕、应验日历 API
- `liuyao-h5`：React + Vite 前端，负责起卦页、对话页、历史会话页、应验日历页
- `liuyao-worker`：Python worker，负责知识库文档处理、分块、向量写入等离线任务

## 新架构

- `Session`：把单次分析升级为可追问、可关闭、可回看的会话
- `OrchestratedAnalysisService`：新的编排式分析入口，统一 Prompt 模板、多轮上下文与结构化输出
- `VerificationEventService`：从 `predictedTimeline` 自动创建应验事件，支持反馈、时间线和提醒
- 旧接口 `POST /api/divinations/analyze` 保留，但已经代理到新的 Session 流程

## 本地启动

### 1. 纯本地开发

后端：

```bash
cd liuyao-app
../apache-maven-3.9.6/bin/mvn spring-boot:run
```

前端：

```bash
cd liuyao-h5
npm install
npm run dev
```

Worker：

```bash
cd liuyao-worker
python3 -m app.main --once
```

### 2. Docker Compose

先准备环境变量：

```bash
cp .env.example .env
```

最少需要填写：

- `DB_PASSWORD`
- `LLM_API_KEY` 或 `DASHSCOPE_API_KEY`

启动全部服务：

```bash
docker compose up -d --build
```

默认端口：

- PostgreSQL：`5433`
- 后端：`8080`
- H5：`3000`

如果本机已有 `8080` 占用，可临时覆盖：

```bash
APP_PORT=8081 docker compose up -d --build
```

## 核心页面与接口

页面：

- `/`：起卦首页
- `/session/:id`：多轮对话页
- `/history`：历史会话页
- `/calendar`：应验日历页

主要 API：

- `POST /api/sessions`
- `POST /api/sessions/{id}/messages`
- `GET /api/sessions/{id}`
- `GET /api/sessions`
- `DELETE /api/sessions/{id}`
- `GET /api/calendar/events`
- `GET /api/calendar/timeline`
- `POST /api/calendar/events/{id}/feedback`

更详细接口说明见：

- [liuyao-app/docs/api.md](/Users/liuyishou/wordspace/liuyao/liuyao-app/docs/api.md)

## 测试与验证

后端：

```bash
cd liuyao-app
../apache-maven-3.9.6/bin/mvn test
```

前端：

```bash
cd liuyao-h5
npm run build
```

Worker 如需基础校验，可先做语法检查：

```bash
python3 -m py_compile liuyao-worker/app/task_runner/worker.py
```
