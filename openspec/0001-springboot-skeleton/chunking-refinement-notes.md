# 书籍切分规则细化说明

## 目标

基于已经完成的首批真实导入结果，判断当前切分规则是否需要细化，并给出后续落地顺序。

这份说明只讨论：

- 当前规则实际表现
- 哪些书已经表现良好
- 哪些书需要专项优化
- 是否值得修改切分代码

## 当前规则实现位置

- 通用切分器：[classic_text_chunker.py](/Users/liuyishou/wordspace/liuyao/liuyao-worker/app/chunker/classic_text_chunker.py)
- 当前书籍切分入口：[zengshan_buyi_chunker.py](/Users/liuyishou/wordspace/liuyao/liuyao-worker/app/chunker/zengshan_buyi_chunker.py)
- 文本清洗：[text_cleaner.py](/Users/liuyishou/wordspace/liuyao/liuyao-worker/app/cleaner/text_cleaner.py)

当前实际状态：

- `zengshan_buyi_chunker` 还没有特化逻辑
- 目前所有 `txt / pdf` 实际上都在共用 `ClassicTextChunker`

## 当前已导入样本

### PDF

- [黎光－六爻预测学.pdf](/Users/liuyishou/wordspace/liuyao/黎光－六爻预测学.pdf)

### TXT

- [增删卜易--野鹤老人.txt](/Users/liuyishou/wordspace/liuyao/增删卜易--野鹤老人.txt)
- [卜筮正宗-清-王洪绪.txt](/Users/liuyishou/wordspace/liuyao/卜筮正宗-清-王洪绪.txt)
- [黄金策-明-刘基.txt](/Users/liuyishou/wordspace/liuyao/黄金策-明-刘基.txt)
- [火珠林-宋-麻衣道者.txt](/Users/liuyishou/wordspace/liuyao/火珠林-宋-麻衣道者.txt)

## 真实导入结果

### PDF 导入结果

- `六爻预测学`：`421` chunks
- `untagged`：`303`
- 已打标签占比偏低

主题分布：

- `用神 62`
- `动爻 28`
- `世应 11`
- `空亡 8`
- `六亲 5`
- `月破 4`

结论：

- PDF 链路可用
- OCR 后仍有较多脏数据
- 章节标题质量不稳定
- `untagged` 比例偏高

### TXT 导入结果

- `增删卜易`：`637` chunks，`untagged = 0`
- `卜筮正宗`：`372` chunks，`untagged = 0`
- `黄金策`：`661` chunks，`untagged = 0`
- `火珠林`：`77` chunks，`untagged = 0`

全部都已使用：

- `embedding_provider = dashscope`
- `embedding_model = text-embedding-v4`
- `embedding_dim = 1024`

## 当前质量判断

### 对 TXT 的判断

当前通用规则对古籍 `txt` 的效果明显好于扫描版 `pdf`。

从已导入样本看：

- 能稳定识别 `用神 / 世应 / 动爻 / 空亡 / 六亲 / 月破 / 日破`
- 基本不会出现 `untagged`
- 对理论段、断语段、卦例段已经有一定召回能力

结论：

- 当前规则对 `txt` 已经达到“可继续扩大导入”的水平
- 不需要在导入前先停下来大改代码

### 对 PDF 的判断

当前规则对扫描版 `pdf` 只能算“链路已打通，质量待提升”。

主要问题：

- OCR 错字仍偏多
- 目录页和页眉页脚虽然已过滤一部分，但还不彻底
- 标题抽取质量不稳定
- 很多本可分类内容仍落入 `untagged`

结论：

- PDF 需要专项优化
- 但优化应放在 `txt` 批量导入之后

## 是否需要修改切分规则代码

结论：

- **需要**
- 但不是“马上大改”
- 应该按“先导入更多高质量 TXT，再做分层细化”的方式推进

原因：

1. 当前 `txt` 已经能稳定导入，不值得为了追求完美先打断资料积累
2. 目前最明显的问题集中在：
   - 古籍结构识别还不够细
   - PDF/OCR 噪声处理还不够强
3. 现在就可以先让知识库继续变厚，再在真实样本基础上调规则

## 建议的细化方向

### 第一层：保留通用规则

继续保留当前能力：

- 空行粗切
- 触发词切分
- 标点细切
- 标题识别
- 主题识别
- OCR 清洗

### 第二层：新增古籍 TXT 专项规则

优先增强：

- `卷 / 篇 / 章 / 节` 识别
- `歌曰 / 诀曰 / 断曰 / 占曰 / 野鹤曰` 识别
- 卦例起始格式识别：
  - `占...`
  - `某占...`
  - `例...`
  - `某人占...`

目标：

- 把理论段、歌诀段、卦例段、断语段拆得更清楚

### 第三层：新增 PDF / OCR 专项规则

优先增强：

- 页眉页脚剔除
- 页码剔除
- 目录样式块继续过滤
- OCR 错词词典扩充
- 标题行纠偏

目标：

- 降低 `untagged`
- 提高章节标题质量

### 第四层：少量重点书特化

建议优先特化两本：

- [增删卜易--野鹤老人.txt](/Users/liuyishou/wordspace/liuyao/增删卜易--野鹤老人.txt)
- [黎光－六爻预测学.pdf](/Users/liuyishou/wordspace/liuyao/黎光－六爻预测学.pdf)

原因：

- 一古一今
- 一本文本质量高，一本 OCR 噪声高
- 最能代表后续规则改动的收益

## 推荐执行顺序

### 先做

- 继续导入剩余高质量 `txt`
- 用这些 `txt` 继续喂知识库和分析模块

### 再做

- 为古籍 `txt` 增加专项切分规则
- 为 `pdf` 增加 OCR 专项清洗规则

### 最后做

- 对重点书再做少量特化

## 当前结论

一句话总结：

- 当前切分规则已经足够支撑 `txt` 批量导入
- 当前切分规则还不足以让扫描版 `pdf` 达到高质量
- 所以后续应该是：
  - 先继续导 `txt`
  - 再细化切分代码
  - 再继续优化 `pdf`
