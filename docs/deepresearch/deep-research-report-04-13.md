# 我开发的六爻断卦AI系统（liuyao 仓库）深度研究报告

## 执行摘要

本仓库是一套“规则引擎 + 知识库检索 + 大模型表达”的六爻断卦系统雏形，整体采用前后端分离与多进程协同：后端为基于 Spring Boot 的“六爻骨架项目”，含起卦排盘、规则命中、结构化评分、案例留痕等模块；同时提供 Python Worker，用于将书籍/文本解析为分块并生成向量，写入 PostgreSQL（含 pgvector）以支持知识检索；规则资源以版本化 JSON 管理，并镜像到运行时 resources 目录，便于审阅与回滚。citeturn45view0turn31view0turn21view0turn16view0

在推理流程上，系统先用确定性算法构建卦盘（本卦/变卦、世应、六神、纳甲、六亲等），再由规则引擎产出 RuleHits 与结构化评分（含冲突摘要、有效规则/被抑制规则），最后由分析服务生成“机械分段文本”，并可选调用 LLM 对表达进行润色（LLM 输出非空则覆盖机械文本）。citeturn38view2turn43view0turn45view0

关键短板集中在三类：其一，前端工程与生产化部署信息不完整（脚本展示了前端 dev server 启动，但仓库前端目录呈现为仅有 src 的“片段式提交”，缺少可复现的构建依赖说明，部署与发布链路“未说明”）；其二，LLM 输出缺少结构化约束与安全护栏，存在提示注入/幻觉覆盖机械结果的风险；其三，缺少可量化的系统级评估闭环（虽已有部分回归测试与“案例留痕”，但对“断卦质量”尚未形成可持续迭代指标体系）。citeturn2view5turn43view0turn45view0turn8view0

本报告给出一条“1 个月内可落地”的 P0 路线：建立卦例回放评估与回归基线、将 LLM 输出改为结构化且可验证、补齐一键部署与接口契约（OpenAPI/类型生成）以打通前后端与运维；并给出 1–6 个月、6 个月以上的扩展计划：规则配置化/可视化编辑、数据标注与主动学习、RAG 引用证据与可追溯解释、以及更全面的隐私合规策略。citeturn20view0turn45view0turn21view0

## 代码与架构审查

### 仓库顶层结构与主要模块

从启动脚本可以直接读出系统的“多组件编排”逻辑：先启动 PostgreSQL（docker compose），再启动 Java 后端（8080），再启动 Python Worker（后台任务/知识提取），最后启动 H5 前端 dev server（5173）。citeturn2view5turn4view0

| 层级 | 目录/组件 | 主要职责 | 技术栈/依赖线索 | 是否清晰说明 |
|---|---|---|---|---|
| 规则引擎后端 | `liuyao-app/` | 起卦输入→排盘构建→规则评估→结构化结果→LLM 表达→案例留痕 | Spring Boot、JPA、Flyway、PostgreSQL；规则资源 version=v1；可配置 LLM（OpenAI 兼容 base-url，默认 qwen-plus） | 说明较完整（README/配置/接口示例）citeturn45view0turn31view0turn38view2 |
| 知识处理 Worker | `liuyao-worker/` | 书籍/文本解析→清洗→切分→Embedding→写入 pgvector；另含“规则抽取任务”把 chunk 提交给 LLM 生成候选规则 | Python；配置 embedding_provider/embedding_model 等；RuleExtractPipeline 调用 LLM | 主要通过代码可推断（架构意图明确，运行参数/默认值部分“未说明”）citeturn16view0turn19view0 |
| 规则资源 | `rules/`、`rules/v1/*.json` | 维护 JSON 规则快照；并镜像到 `liuyao-app/src/main/resources/rules/v1/` 作为运行时资源 | 版本化治理：manifest/metadata/rule bundles | 说明清晰citeturn21view0turn22view0turn23view0turn25view0 |
| 前端 | `liuyao-h5/` | H5 交互（脚本按 Vite dev server 启动） | 启动脚本调用 `npm run dev`，但目录展示仅 `src/` | 构建与依赖“未说明/不可复现风险”citeturn2view5turn8view0 |
| 设计文档与任务 | `docs/` | 规则引擎详细设计、用神定位设计、迁移状态、gap 任务等 | Markdown 形式，体现规划与演进 | 文件存在，内容本次未逐篇解析（但可作为 project plan 依据）citeturn46view0 |
| 工程规则说明 | `六爻用神选择规则表_工程版.md` | 明确“用神选择”第一阶段固定规则、后续配置化与统计优化路线 | 同时列出建议代码包与 `use_god_rules.json` | 说明非常关键（相当于产品/算法路线图）citeturn20view0 |

### 后端关键数据流与推理链路

后端主入口为 `POST /api/divinations/analyze`（`DivinationController` 转发给 `DivinationService#analyze`）。citeturn35view0turn45view0turn38view2

`DivinationService#analyze`的核心链路（按代码逻辑顺序）：

1) DTO→领域输入：`DivinationInput input = divinationMapper.toInput(request)`  
2) 排盘构建：`ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input)`  
3) 规则评估：`ruleEngineService.evaluateResult(chartSnapshot)` → `RuleHit` 列表 + 结构化结果  
4) 上下文构建：`analysisContextFactory.create(...)`，并注入 `knowledgeSearchService.suggestKnowledgeSnippets(..., 6)`  
5) 分析生成：`analysisService.analyze(analysisContext)` 先机械分段再可选 LLM 润色  
6) 案例落库：`caseCenterService.recordAnalysis(...)`  
7) 响应：返回 `chartSnapshot`、`ruleHits`、`analysis`、`analysisContext`、`structuredResult` citeturn38view2turn45view0

下面用一张 mermaid 图，把“后端推理 + 知识库 + worker”串起来（其中某些组件的实现细节在仓库中可见，但生产部署形态未给出，因此以逻辑关系展示）：

```mermaid
flowchart LR
  U[用户/H5] -->|POST /api/divinations/analyze| API[liuyao-app\nSpring Boot :8080]

  subgraph API[liuyao-app]
    C[ChartBuilderService\n排盘构建]
    R[RuleEngineService\n规则评估]
    AC[AnalysisContextFactory\n上下文]
    K[KnowledgeSearchService\n检索片段]
    AS[AnalysisService\n机械分段 + LLM润色]
    CC[CaseCenterService\n案例留痕]
  end

  API --> C --> R --> AC --> AS --> CC --> U
  AC --> K

  subgraph DB[(PostgreSQL + pgvector)]
    T1[case_* 表\n卦例/分析结果]
    T2[book_chunk 表\n文本分块+向量]
    T3[task_* / rule_candidate\n任务与候选规则]
  end

  CC --> DB
  K --> DB

  subgraph W[liuyao-worker\nPython Worker]
    BP[BookPipeline\n解析/清洗/切分/Embedding]
    RP[RuleExtractPipeline\nLLM抽取规则候选]
    WR[Worker.run_forever\n轮询任务]
  end

  W -->|读任务/写结果| DB
  WR --> BP --> DB
  WR --> RP --> DB
```

图中的关键点都能在代码或迁移脚本中找到对应：`DivinationService` 调用规则引擎、分析服务与知识检索；数据库迁移包含案例/分析/知识引用、book chunk 与任务锁、规则候选表等；Worker 以“claim task→pipeline→写回”的方式工作。citeturn38view2turn32view0turn16view0turn19view0

### 规则资源与“用神”机制

规则资源治理明确采用“版本化 bundle + 镜像运行时资源”的策略：`rules/v1/`为维护源，运行时资源在 `liuyao-app/src/main/resources/rules/v1/`，两者镜像以便审阅。citeturn21view0turn22view0turn30view0

“用神选择”在工程层面走的是“第一阶段固定规则”，并明确未来演进：固定规则 → 配置化 → 结合卦例反馈统计优化 → AI 辅助发现模式但不直接改规则。citeturn20view0

`use_god_rules.json` 中已实现部分 intent→用神映射（示例：求职/收入/出行等），并带有场景与注释说明。citeturn24view0turn24view2turn23view1

### LLM 与模型类型、训练/推理流程

后端 `application.yml` 显示 LLM 为“可开关组件”，并采用 OpenAI 兼容 `base-url` 形式；`api-key` 允许从 `LIUYAO_LLM_API_KEY` 或 `DASHSCOPE_API_KEY` 或 `OPENAI_API_KEY` 注入；默认 `model=qwen-plus`，`timeout-ms=8000`。citeturn31view0

从 `AnalysisService` 的实现可知：系统先生成“机械分段文本”（规则驱动、可控），再交给 `LlmExpressionClient.refine(mechanicalText, question)` 做表达润色；LLM 返回非空就覆盖，否则回退机械文本。citeturn43view0

这意味着当前“LLM 的职责定位”更接近 **NLG 表达层**（润色/组织语言），而非直接替代规则推理；但由于实现上允许 LLM 覆盖全文，实际仍存在“表达层反客为主”的风险（详见后文不足分析）。citeturn43view0

训练流程：仓库未出现针对自有模型的训练代码与数据集管理逻辑；更接近“调用外部预训练 LLM + 外部 embedding 服务 + 本地规则”的组合，因此训练部分应标注为“未说明/未实现”。citeturn31view0turn19view0turn20view0

### 部署方式与运行形态

目前仓库提供的是“本地开发/联调型启动方式”：  
- PostgreSQL：`docker compose up -d`（在 `liuyao-app/docker-compose.yml`）citeturn45view0turn4view0  
- 后端：`mvn spring-boot:run`，默认 8080；并有 `mvn test`、指定 controller/规则回归测试等命令说明citeturn45view0  
- Worker：`python app/worker.py`（启动脚本中以后台运行方式执行）citeturn2view5turn16view0  
- 前端：脚本尝试 `npm run dev -- --host 0.0.0.0 --port 5173`，但前端依赖与构建配置在仓库层面“未说明”。citeturn2view5turn8view0  

生产化部署（镜像发布、反向代理、配置中心、灰度/回滚、可观测性）在仓库中未形成“明确标准形态”，应标注为“未说明”。citeturn2view5turn45view0

### 代码片段证据

片段一：顶层一键启动脚本展示了组件编排顺序与端口（本地联调定位非常清晰）

```bash
cd "$(dirname "$0")"

cd liuyao-app
docker-compose up -d
mvn spring-boot:run &

cd ../liuyao-worker
python app/worker.py &

cd ../liuyao-h5
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

citeturn2view5

片段二：后端主推理链路（排盘→规则→知识检索→LLM→落库）

```java
DivinationInput input = divinationMapper.toInput(request);
ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);

RuleEvaluationResult evaluationResult = ruleEngineService.evaluateResult(chartSnapshot);
List<RuleHit> ruleHits = evaluationResult.getHits();

AnalysisContextDTO analysisContext = buildAnalysisContext(
  request.getQuestionText(), chartSnapshot, ruleHits, structuredResult);

String analysis = analysisService.analyze(analysisContext);
caseCenterService.recordAnalysis(request, chartSnapshot, ruleHits, analysisContext, structuredResult, analysis);
```

citeturn38view2

片段三：分析服务“机械文本 + 可选 LLM 覆盖”的实现（当前安全与可控性讨论的核心依据）

```java
String mechanicalText = sectionComposer.compose(context);
if (llmExpressionClient != null) {
  String refinedText = llmExpressionClient.refine(mechanicalText, question);
  if (refinedText != null && !refinedText.isBlank()) {
    return refinedText;
  }
}
return mechanicalText;
```

citeturn43view0

## 需求定位与目标用户

### 基于仓库材料的“强证据定位”

从 `liuyao-app` README 的模块描述与接口示例，可以确定本项目至少覆盖三个核心能力：  
1) **六爻排盘与结构化要素提取**：本卦/变卦、世应、六神、纳甲、六亲、历法字段等；citeturn45view0  
2) **规则命中与结构化评分**：返回 `ruleHits`、`structuredResult`（含冲突摘要、有效规则/被抑制规则、分阶段统计等）；citeturn45view0turn38view2  
3) **可解释文本输出**：先工程化分段输出，再用 LLM 做表达润色；citeturn43view0turn31view0  
4) **案例留痕/复盘基础设施**：`casecenter` 模块与对应迁移脚本、测试命令显示出“卦例留存与回放”的工程意图。citeturn45view0turn32view0  

据此，本系统最合理的目标用户可归为两类：

- **面向易学从业者/深度爱好者的“可复盘断卦工具”**：强调排盘准确、要素齐全、可记录、可检索、可复盘（类似“天时六爻”这类工具的专业定位）。citeturn45view0turn44search7  
- **面向 AI 产品/开发者的“规则引擎适配器 + 解释层服务”**：启动脚本中对后端的描述为“规则引擎适配器”；同时存在知识入库 worker 与候选规则抽取 pipeline，明显偏向“可工程化迭代”的平台型产品思路。citeturn2view5turn16view0turn46view0  

### 假设与可选替代定位（仓库未完全明确部分）

由于仓库未提供清晰的产品化文案、商业化策略与用户画像（例如：是否做小程序、是否做付费咨询聚合、是否做社区卦例库等），以下定位属于“基于现有工程形态的推断”，需要后续用 README/PRD 补强：

| 定位选项 | 核心卖点 | 必要补齐的信息 | 风险 |
|---|---|---|---|
| “断卦助手 API（B2B/B2D）” | 提供稳定接口与可解释结构化输出，易嵌入智能体/工作流 | 认证鉴权、配额计费、SLA、OpenAPI 文档 | 需要强运维与成本控制（LLM/RAG）citeturn31view0turn45view0 |
| “个人专业排盘+复盘工具（B2C）” | 体验、记录、搜索、案例库与学习体系 | 完整前端/移动端、备份同步、隐私策略 | 需要持续内容与用户增长 |
| “规则引擎研究平台（开源/共创）” | 规则版本治理、候选规则抽取、回归测试 | 贡献指南、规则 DSL/编辑器、基准数据集 | 规则争议与质量控制成本高citeturn21view0turn20view0turn46view0 |

## 竞品与生态对比

下表选取了“国内在线六爻排盘/解卦站点、移动端工具、开源 AI 命理项目、海外 I Ching 平台（相关）”共 8 个样本。由于多数商业/网站产品不公开算法与数据来源，“未说明”字段按要求标注；可信度评价以“信息透明度、是否能保存复盘、是否明确方法/免责声明”等维度做工程视角估计。

| 样本 | 类型 | 主要功能（公开描述） | 算法/模型披露 | 交互形态 | 付费模式线索 | 数据来源与可信度（工程视角） |
|---|---|---|---|---|---|---|
| China95 六爻在线排盘 citeturn44search0 | 在线网页 | 排盘结果展示卦/爻辞，并给出电脑自动解卦；可按占测范围输出相关结果 citeturn44search0 | 未说明 | 表单式网页 | 未说明 | 以“卦辞爻辞+自动解卦”呈现，方法披露有限；更像工具站（可用但不易复盘） |
| 易安居 六爻在线排盘 citeturn44search8 | 在线网页 | 电脑铜钱随机数、可自动排盘或按时间起卦；可选择预测范围自动给用神与强弱衰旺说明 citeturn44search8 | 规则/随机数说明较多，但“断语质量”算法未说明 citeturn44search8 | 网页交互 | 未说明 | 对起卦程序的随机与成卦规则描述相对透明；但解卦依据与素材来源不透明 |
| 卜易居（移动端站点） entity["organization","卜易居","chinese astrology site"] | 综合命理/算卦门户 | 页面展示“算卦”等栏目，并有免责声明“仅供娱乐参考” citeturn44search1 | 未说明 | 移动端导航站 | 未说明 | 有明确免责声明；但六爻细节能力需进一步核验（本次仅依据站点入口与免责声明）citeturn44search1 |
| 天时六爻（App Store） entity["company","Apple","app store operator"] | 移动端排盘工具 | 多种起卦方式（正时/手工/卦名/报数/铜钱等）、记录回溯搜索、分享扫码保存、卦例库反馈复盘；并提供“智能解卦参考”覆盖事业/经商/婚恋等 citeturn44search7 | “智能解读”机制未说明（可能规则模板/统计/AI） citeturn44search7 | App（记录/复盘/分享） | 标注“免费无广告”citeturn44search7 | 复盘与案例库能力强（和本仓库 casecenter 方向相似）；但解卦算法不透明、无法审阅规则 |
| MingAI（开源 AI 命理工具）citeturn44search5turn44search13 | 开源项目生态 | 覆盖多命理体系（含六爻），并支持 MCP/Skills 等形态citeturn44search5turn44search13 | 具体六爻模块推理方式未在摘要中披露（需读仓库 README 才能判定，本文受工具调用限制未展开）citeturn44search5 | 作为工具/服务接口形态 | 未说明 | 代码可审阅是优势；但“六爻断卦可解释度/规则透明”需进一步逐仓库分析 |
| castiching.com citeturn44search3 | 海外 I Ching 在线占卜 | 3 coins / yarrow 两种方式；强调“严格算法复刻古法数学”，即时出结果；不需登录 citeturn44search3 | 对方法学与随机算法自述较明确citeturn44search3 | 网页交互 | “100% free, no sign-up”citeturn44search3 | 对“起卦算法”透明度较高；但“解释文本来源”依赖其自有内容体系（非六爻纳甲体系） |
| aiching.app citeturn44search6 | 海外 I Ching 在线占卜 | 免费、无登录；展示 64 卦与解读；强调大量咨询量 citeturn44search6 | 未说明是否 AI/规则/人工撰写 | 网页交互 | 免费、无登录citeturn44search6 | 适合作为“海外解释层”参考；但与六爻纳甲不完全同构 |
| iching-ai.com citeturn44search10 | 海外 AI 解读站 | “AI-powered interpretation” 的 I Ching 卦象解读 citeturn44search10 | 未披露模型与数据 | 网页交互 | 未说明 | 作为“AI 解读形态”的对照样本；透明度较低，工程复现与可信度评估困难 |

### 与本系统的差异化机会（从工程可控性出发）

与上述样本相比，本仓库的核心优势更偏“可工程化迭代”：

- **规则透明且可版本治理**：规则快照与运行时镜像策略，可做 code review 与回滚，避免“黑盒断语”。citeturn21view0turn22view0  
- **结构化结果与冲突解释**：响应体不仅给文本，还给 `structuredResult`（有效规则、冲突摘要、分阶段统计），适合做评估与可视化。citeturn45view0  
- **知识库与候选规则抽取管线**：Python Worker 形成“书籍→chunk→embedding→检索”，并可用 LLM 从 chunk 中抽取规则候选，支撑“规则扩展”的生产力工具链。citeturn16view0turn19view0  
- **复盘闭环的工程基础**：已存在 casecenter 模块与迁移脚本，且 README 给出回归测试/规则回归测试命令，利于形成持续集成质量门槛。citeturn45view0turn32view0  

## 不足与风险分析

本节按“代码质量、架构扩展、模型与数据、体验、安全合规、可维护性”逐项指出不足，并尽量给出模块/行号定位（以 GitHub 浏览器行号为准）。

### 代码质量与鲁棒性

第一，LLM 输出覆盖机制缺少“结构化约束 + 验证 + 回退原因记录”。`AnalysisService#analyze(AnalysisContextDTO)` 只要 LLM 返回非空文本就直接替换机械文本；但没有 JSON schema 校验、没有“必须包含哪些段落”的检查、也没有把 LLM 失败原因写入上下文/案例表。定位：`liuyao-app/.../analysis/service/AnalysisService.java` 388–406 行。citeturn43view0

第二，主流程对异常与超时的“用户可感知策略”未在主链路体现。`DivinationService#analyze` 无论 LLM 成功与否都会返回（因为 AnalysisService 有 fallback），这很好；但对“知识检索失败/数据库失败/规则引擎失败”等情况，主流程没有表现出分级降级策略（例如跳过知识而继续、输出降级原因等）。定位：`liuyao-app/.../divination/service/DivinationService.java` 814–875 行。citeturn38view2

第三，Worker 采用轮询式 `run_forever`，当无任务时 `sleep(poll_interval_seconds)`，没有指数退避、无多 worker 协同冲突策略说明（虽有 claim 机制，但锁实现要看 repository）。定位：`liuyao-worker/app/task_runner/worker.py` 541–549 行。citeturn16view0

### 架构可扩展性与工程化缺口

第一，前端工程可复现性存在明显缺口：启动脚本要求 `liuyao-h5` 下 `npm install`、`npm run dev`，但目录结构展示仅有 `src/`，缺少 `package.json` 等构建元数据时，第三方无法复现，也无法进行 CI 构建与发布。该问题会直接阻断“可部署产品形态”。citeturn2view5turn8view0

第二，生产部署形态未形成统一方案。目前 `docker-compose.yml`只覆盖 PostgreSQL；后端与 worker 未容器化，缺少 healthcheck、日志/指标采集、反向代理与安全 headers 的标准配置。因此“上线/迁移/扩容”的工程成本会迅速上升。citeturn4view0turn2view5turn45view0

第三，规则资源虽已版本治理，但**规则编辑/验证链路仍偏工程手工**。`rules/README`描述了快照与镜像策略，但未见“规则 DSL 校验器/可视化编辑器/规则单测生成器”的明确实现入口（可能在 `docs/`规划中）。这会导致规则扩展依赖核心开发者，降低社区协作效率。citeturn21view0turn46view0

### 模型性能、数据与标注

第一，仓库文档明确指出：当前缺少足够高质量卦例数据，因此用神选择先走“固定规则”，并警惕“自主进化学歪”。这说明数据与标注体系仍在早期。定位：`六爻用神选择规则表_工程版.md` “设计原则/原因”段落。citeturn20view0

第二，结构化评分虽已输出（effectiveScore、冲突决策等），但缺少公开的评估指标定义与基准集管理方式：例如“用神准确率”“规则命中是否与人类断语一致”“LLM 润色是否引入事实错误”等。虽然 README 提到了 `RuleEngineScenarioRegressionTest`，但这更像规则回归的工程测试，而不是产品质量评估体系。citeturn45view0

第三，知识库 RAG 的“证据可追溯性”在当前响应体中尚不突出：`analysisContext` 有 `knowledgeSnippets`（由 `KnowledgeSearchService.suggestKnowledgeSnippets` 注入），但最终 `analysis` 文本是否引用这些片段、是否返回引用来源（book id / chunk id）并形成可追溯证据链，在接口示例中未体现为强约束。citeturn38view2turn45view0

### 用户体验与可解释性

第一，问题意图与类目体系可能造成“用户输入摩擦”。工程文档强调“问工作不能直接写死官鬼，应拆意图”，并建议“方式一：前端先传细分类别更稳”。这意味着如果前端不能很好地引导“意图细分”，后端仅靠关键词兜底会不稳定，影响断卦体验一致性。citeturn20view0turn24view2

第二，当前分析文本示例呈现为“概览+用神判断+规则命中数”，但缺少对“为什么命中这些规则”“规则冲突如何裁决”“哪些知识证据支撑结论”的强解释模板（虽然 structuredResult 有冲突摘要，但需要前端把它转化为可读解释）。citeturn45view0

### 安全与合规风险

第一，LLM 配置默认启用，且 base-url 默认指向 OpenAI 兼容接口；在未做鉴权/配额/敏感信息治理时，存在 API Key 泄露与滥用风险（尤其在前端调试、日志输出、错误栈回传等场景）。定位：`application.yml` 中 `liuyao.llm.enabled/api-key/model`。citeturn31view0

第二，断卦问题天然可能涉及健康、法律、财务等高敏感领域；外部竞品普遍在页面显著位置给出“仅供娱乐参考”免责声明（例如卜易居站点明确展示免责声明）。本系统虽可在前端/接口层加入免责声明，但仓库当前未体现标准化的合规文案与拒答策略。citeturn44search1turn45view0

### 可维护性与团队协作

第一，`docs/` 中存在多份“任务计划/迁移状态/详细设计”文档，说明项目在快速演进；但若缺少统一的“架构决策记录（ADR）+ 版本里程碑 + 变更日志”，很容易导致文档与代码漂移。当前仅能确认这些文档存在，是否与代码对齐需进一步审阅。citeturn46view0

第二，多语言栈（Java + Python + Node）叠加外部模型调用，会放大工程协作成本，尤其是在缺少统一 CI（lint/test/typecheck/security scan）时。仓库已提供部分 Maven 测试命令，但前端与 worker 的测试基线尚不清晰。citeturn45view0turn2view5turn16view0

## 改进路线图与资源建议

本节按短期（≤1个月）、中期（1–6个月）、长期（≥6个月）给出可执行计划，并覆盖：模型改进、数据增强、评估指标、接口与前端、部署运维、隐私合规。时间与人力为“保守估算”，实际需结合代码量与团队熟练度调整。

### 短期路线图（一个月内可完成）

| 优先级 | 改进项 | 目标与技术方案 | 人力/周期（估算） | 风险与依赖 |
|---|---|---|---|---|
| P0 | 建立“卦例回放评估 + 回归基线” | 以 `casecenter` 落库数据与 README 提到的回归测试为基础，新增“可重放案例集（golden set）”；对规则命中、structuredResult、LLM 输出做快照对比；CI 中加入回归门禁 | 1–2人，2–3周 | 需要定义评价口径；需要稳定的 LLM mock/录制回放策略citeturn45view0turn32view0turn43view0 |
| P0 | LLM 输出结构化与安全护栏 | 将 `LlmExpressionClient` 由“返回自由文本”升级为“返回 JSON（sections、risk_level、citations）”；服务端用 schema 校验，不通过则回退机械文本并记录原因；加入提示注入防护（把用户问题作为 data，不让其改写系统指令） | 1–2人，2–4周 | 需要调整 prompt 与输出解析；需要前端配合展示 structured sectionsciteturn43view0turn31view0 |
| P0 | 前端工程补齐 + API 契约 | 补齐 `liuyao-h5` 构建元数据（package.json、锁文件、env 示例）；引入 OpenAPI 生成 TS client；把 `questionCategory/intent` 做成可选列表引导，减少用神误选 | 1–2人，2–4周 | 当前前端目录不完整，需先恢复工程；依赖后端提供稳定 schemaciteturn8view0turn45view0turn20view0 |
| P1 | 一键部署（全栈 docker-compose） | 把后端/worker 容器化；加 healthcheck；统一 env 管理；支持本地与生产同构 | 1人，1–2周 | 需要明确生产依赖（域名、证书、代理、存储）citeturn4view0turn2view5 |

### 中期路线图（一到六个月）

中期的重点是“把规则与数据迭代纳入平台化闭环”，并降低人工维护成本：

1) **规则配置化与可视化治理**：在现有 `rules/v1` 基础上引入 rule schema 校验、规则编辑器（至少是管理后台表单+版本发布+回滚）；并把 `rule_definitions.json` 的 bundle 元信息与 UI 绑定，形成可审阅的变更历史。citeturn23view0turn21view0turn25view0

2) **卦例标注体系与主动学习**：利用 `casecenter` 的“结果反馈/复盘”字段（迁移脚本已体现案例相关表的持续扩展），定义最小标注集：意图（QuestionIntent）、用神（UseGod）、结论等级（GOOD/NEUTRAL/BAD）、验证结果（对/错/不确定）等；结合“候选规则抽取 pipeline”把书籍规则转成候选项，交给人工审核后入库为新版本规则。citeturn32view0turn16view0turn20view0

3) **RAG 证据链与可追溯输出**：让 `analysisContext.knowledgeSnippets` 参与最终输出的强约束：每个关键判断必须附引用（book/chunk id），前端可点击展开来源文本。这样既提升可信度，也能形成“用户纠错→反向改进知识库”的闭环。citeturn38view2turn19view0

4) **运维与成本控制**：对 LLM 调用引入缓存（同一卦例同一上下文返回一致）、限流、分级模型（轻量润色模型 vs 高质量分析模型可切换），并补齐指标（延迟、命中率、失败率）。配置层面可继续沿用 `application.yml` 环境变量注入方式，但要增加 secret 管理与审计。citeturn31view0turn43view0

### 长期路线图（六个月以上）

长期目标是从“能断”走向“断得准、可验证、可规模化迭代”：

- **从固定规则走向“统计校准 + 人审守门”的混合智能**：沿用文档提出的路线（固定→配置化→统计优化→AI 辅助发现但不自动改规则），在规则层之上建立校准器：让历史卦例统计为规则提供权重调优建议，但最终发布仍需要人工审核与回归测试门禁。citeturn20view0turn45view0  
- **标准化评测集与可公开基准**：沉淀可脱敏的公开卦例子集，定义跨版本可比的指标（用神准确率、规则冲突率、LLM 覆盖率、用户反馈一致性等），并形成“版本发布报告”。  
- **多端产品化与生态接口**：在 API 稳定后扩展小程序/桌面端；或提供 MCP/插件形态接入各种智能体平台（对照 MingAI 的生态做法）。citeturn44search5turn44search13turn45view0  

### 资源建议（数据集、开源模型、工具链）

由于仓库当前强调“卦例数据不足、先固定规则”，资源建议以“可落地、可审计、可逐步闭环”为原则：citeturn20view0

- 数据来源方向：优先使用“公版古籍+现代可授权教材”构建知识库（由 `liuyao-worker` 的书籍解析与向量入库能力支撑），再用 `RuleExtractPipeline` 抽取候选规则，形成“可追溯到原文”的规则资产。citeturn19view0turn16view0  
- 开源模型方向：若继续使用 OpenAI 兼容调用，优先选择中文能力强、成本可控、支持工具/函数调用的模型；并保持模型与 base-url 可配置（当前已具备）。citeturn31view0  
- 工具链方向：  
  - 后端：OpenAPI 生成文档与 TS client；DB 层结合迁移脚本与回放测试做 schema 演进门禁。citeturn32view0turn45view0  
  - 知识库：pgvector + 统一 embedding 维度；为 chunk 建立去重、版本与来源字段（便于引用）。citeturn19view0turn32view0  
  - 测试：在已有 `RuleEngineScenarioRegressionTest` 基础上，扩展成“端到端卦例回放”。citeturn45view0  

## 最优先三项改进实施细化

以下三项按“对系统质量提升幅度/阻断性/可在 1 个月内闭环”排序，给出实施步骤、工具建议、示例伪代码与验收标准。

### 卦例回放评估与回归基线（P0）

**目标**：把当前“能跑通”升级为“每次改规则/改提示词都不会把系统弄坏”，并可量化对比规则版本与模型版本的收益。该方向与仓库已有 `casecenter`、迁移脚本、回归测试命令高度契合。citeturn45view0turn32view0

**实施步骤**

1) 定义最小“golden case”格式：保存请求体 + 期望的关键字段快照（主/变卦编码、palace、useGod、effectiveRuleCodes、structuredResult 决策等）。接口示例已给出字段清单，可直接裁剪为断言点。citeturn45view0  
2) 在测试中新增“端到端回放”：  
   - 调用 `DivinationService#analyze`（或 controller）得到响应  
   - 对结构化字段做稳定断言  
   - 对文本 `analysis` 采用“弱断言”策略（例如必须包含卦名/用神/关键规则码；或对机械文本阶段先固定）。citeturn38view2turn43view0  
3) 引入 LLM mock：把 `LlmExpressionClient` 替换为测试桩，确保回归可重复（例如固定返回 null，让系统回退机械文本；或固定返回预置 JSON）。citeturn43view0  
4) CI 门禁：任何规则文件（`rules/v1/*.json`）、分析模板（`AnalysisSectionComposer`）、规则引擎核心逻辑变更，必须通过回放集。citeturn21view0turn45view0turn43view2  

**示例伪代码**

```java
@Test
void replayCases_shouldMatchGoldenSnapshots() {
  for (GoldenCase gc : loadGoldenCases()) {
    DivinationAnalyzeResponse resp = divinationService.analyze(gc.request());

    assertEquals(gc.expectMainCode(), resp.getChartSnapshot().getMainHexagramCode());
    assertEquals(gc.expectUseGod(), resp.getChartSnapshot().getUseGod());
    assertEquals(gc.expectEffectiveRules(), resp.getStructuredResult().getEffectiveRuleCodes());

    // 文本弱断言：至少包含卦名与用神
    assertTrue(resp.getAnalysis().contains(resp.getChartSnapshot().getMainHexagram()));
    assertTrue(resp.getAnalysis().contains(resp.getChartSnapshot().getUseGod()));
  }
}
```

**测试方法与验收标准**

- 新增 ≥30 条覆盖不同类目/动爻组合/世应边界条件的回放用例；  
- 对结构化字段：版本间对比差异必须可解释（例如规则版本更新导致 useGod 变化，要有变更记录）；  
- 在 LLM mock 下回归必须 100% 稳定；在真实 LLM 下可选跑“非门禁”的 nightly job 观察漂移。citeturn45view0turn43view0

### LLM 输出结构化、可验证与安全护栏（P0）

**目标**：避免 LLM “随意改写结论/引入幻觉”，把 LLM 的角色锁定为“表达与结构化补全”，并允许失败时可追踪原因而不是“悄悄回退”。当前实现是“LLM 返回非空即覆盖”，风险敞口较大。citeturn43view0turn31view0

**实施步骤**

1) 设计 LLM 输出 JSON schema（强约束）：  
   - sections：概览、用神、动爻、世应、综合结论、建议  
   - evidence：引用的 ruleCodes + knowledgeSnippetIds（如果有）  
   - safety：是否包含医疗/法律/赌博等敏感建议，命中则降级为免责声明模板  
2) 在服务端对 JSON 做严格校验：解析失败/字段缺失 → 记录失败原因 → 回退机械文本，并把“LLM 覆盖失败原因”写入 `analysisContext` 或案例表（迁移脚本已有 analysis_context 的演进轨迹，可继续扩展字段）。citeturn32view0turn43view0  
3) Prompt 防注入：把用户问题与机械文本作为 data 输入，不允许它们覆盖系统指令；并要求输出只在 schema 内。  
4) 输出二次检查：  
   - 必须包含 mainHexagram、changedHexagram、useGod（来自 `chartSnapshot`，不得被 LLM 改写）  
   - 若 LLM 输出与结构化结果冲突（例如 useGod 不一致），直接回退机械文本并写日志。citeturn38view2turn45view0turn43view0  

**示例伪代码**

```java
RefinedResult rr = llm.refineToJson(mechanicalText, question);

if (!schemaValidator.isValid(rr)) return mechanicalText;

if (!rr.useGod().equals(context.getUseGod())) return mechanicalText;

// 将 rr.sections 拼成最终展示文本，或直接返回结构化对象给前端
return renderer.render(rr);
```

**测试方法与验收标准**

- 单测：schema 校验覆盖（缺字段、非法 JSON、超长文本、注入样例）；  
- 回放测试：在固定 LLM stub 下，确保输出结构化字段不破坏现有结构化结果；  
- 安全用例：输入“健康/官司/投资”等高风险问题，输出必须包含免责声明与风险提示；对比外部站点常见“娱乐参考”免责声明作为基线。citeturn44search1turn43view0turn31view0  

### 前端工程补齐与接口契约化（P0）

**目标**：让外部贡献者/部署者可以“从零 clone → 一键启动 → 可用 UI”，并通过契约减少“类目/意图”输入摩擦。因为用神规则文档强调“前端传细分类别最稳”，这项工作能直接降低断卦质量波动。citeturn20view0turn2view5turn8view0

**实施步骤**

1) 补齐 `liuyao-h5` 为可构建工程：提交 `package.json`、锁文件、README、env.example；并明确后端地址（dev/prod）。citeturn8view0turn2view5  
2) 契约化：  
   - 生成 OpenAPI（或至少在后端输出 swagger.json）  
   - 前端用生成的 TS client，避免字段拼写错误  
   - 把 `questionCategory` 与 `QuestionIntent` 做成枚举/字典（后端兜底关键词识别仍保留，但前端优先引导）。citeturn20view0turn45view0  
3) UI 体验：  
   - 起卦方式：手工/时间/数字/铜钱（可参考“天时六爻”App 对多起卦方式的产品化经验）citeturn44search7  
   - 输出结构：卦盘要素、ruleHits、冲突摘要、证据片段（knowledgeSnippets）、最终结论  
4) 端到端联调：把当前 README 的请求示例固化为“前端 demo 数据”，支持一键回放。citeturn45view0  

**示例代码（前端请求伪代码）**

```ts
// 伪代码：使用生成的 client
const resp = await divinationsApi.analyze({
  questionText,
  questionCategory,   // UI 引导选择
  divinationMethod,
  divinationTime,
  rawLines,
  movingLines,
});

renderChart(resp.data.chartSnapshot);
renderRules(resp.data.ruleHits);
renderAnalysis(resp.data.analysis);
```

**测试方法与验收标准**

- CI：前端 build 必须通过；  
- 本地：运行启动脚本或等价命令后，访问前端页面可完成一次 analyze 请求并正确渲染核心字段；  
- 体验：用户不填写细分类别时，系统应提示“建议选择更细意图以提高用神准确性”；与用神规则文档保持一致。citeturn20view0turn2view5turn45view0  

## 结论

该仓库已经具备一个“可迭代的六爻断卦 AI 系统内核”：排盘构建与规则引擎形成确定性主干，结构化结果与冲突摘要为评估与解释提供了良好接口；案例留痕与数据库迁移脚本让“复盘闭环”具备落地基座；Python Worker 的知识入库与规则候选抽取管线，为后续“规则扩展与证据引用”提供了生产力工具链。citeturn45view0turn38view2turn32view0turn16view0turn21view0

要把它从“工程雏形”推进到“可对外稳定交付”的产品/平台，最关键的工程抓手是：用回放评估与回归基线锁住正确性；用结构化与校验机制把 LLM 从“黑盒覆盖”收束为“可控表达层”；用契约化接口与可复现前端/部署链路解决交付与协作的阻断点。上述三项在 1 个月内具备高可行性，并且能为中长期的规则配置化、数据标注与统计优化、RAG 证据链、以及更完善的合规策略建立坚实地基。citeturn20view0turn43view0turn2view5turn45view0