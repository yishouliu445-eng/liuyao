const state = {
  selectedCaseId: null,
};

const els = {
  analyzeForm: document.getElementById("analyze-form"),
  analyzeStatus: document.getElementById("analyze-status"),
  analysisSummary: document.getElementById("analysis-summary"),
  analysisRules: document.getElementById("analysis-rules"),
  caseList: document.getElementById("case-list"),
  caseDetail: document.getElementById("case-detail"),
  searchCategory: document.getElementById("search-category"),
  refreshCases: document.getElementById("refresh-cases"),
  presetIncome: document.getElementById("preset-income"),
};

function toJson(response) {
  return response.json().then((body) => {
    if (!response.ok || body.success === false) {
      const message = body && body.message ? body.message : "请求失败";
      throw new Error(message);
    }
    return body.data;
  });
}

function parseCsv(value, mapper) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .map(mapper);
}

function buildAnalyzePayload() {
  return {
    questionText: document.getElementById("questionText").value.trim(),
    questionCategory: document.getElementById("questionCategory").value,
    divinationMethod: document.getElementById("divinationMethod").value.trim(),
    divinationTime: document.getElementById("divinationTime").value,
    rawLines: parseCsv(document.getElementById("rawLines").value, (item) => item),
    movingLines: parseCsv(document.getElementById("movingLines").value, (item) => Number(item)),
  };
}

function updateStatus(message, type = "") {
  els.analyzeStatus.className = `status-text ${type}`.trim();
  els.analyzeStatus.textContent = message;
}

function renderSummary(data) {
  const snapshot = data.chartSnapshot;
  const context = data.analysisContext || {};
  els.analysisSummary.className = "summary-card";
  els.analysisSummary.innerHTML = `
    <strong>${snapshot.mainHexagram} → ${snapshot.changedHexagram}</strong>
    <p class="meta-line">上下卦：${snapshot.mainUpperTrigram}/${snapshot.mainLowerTrigram} → ${snapshot.changedUpperTrigram}/${snapshot.changedLowerTrigram}</p>
    <div class="summary-grid">
      <div class="metric"><span class="metric-label">用神</span><span class="metric-value">${snapshot.useGod || "-"}</span></div>
      <div class="metric"><span class="metric-label">卦宫</span><span class="metric-value">${snapshot.palace || "-"} / ${snapshot.palaceWuXing || "-"}</span></div>
      <div class="metric"><span class="metric-label">世应</span><span class="metric-value">世 ${snapshot.shi || "-"} / 应 ${snapshot.ying || "-"}</span></div>
      <div class="metric"><span class="metric-label">规则命中</span><span class="metric-value">${context.ruleCount || data.ruleHits.length || 0}</span></div>
    </div>
    <div class="detail-tags">
      <span class="tag">日辰 ${snapshot.riChen || "-"}</span>
      <span class="tag">月建 ${snapshot.yueJian || "-"}</span>
      <span class="tag">空亡 ${(snapshot.kongWang || []).join("、") || "-"}</span>
      <span class="tag">版本 ${snapshot.snapshotVersion || "-"}</span>
    </div>
    <pre>${data.analysis || "暂无分析文本"}</pre>
  `;
}

function renderRules(ruleHits) {
  if (!ruleHits || ruleHits.length === 0) {
    els.analysisRules.innerHTML = '<div class="empty-state">当前没有规则命中。</div>';
    return;
  }

  els.analysisRules.innerHTML = ruleHits
    .map((hit) => {
      const evidence = hit.evidence || {};
      const targets = Array.isArray(evidence.targetSummary) ? evidence.targetSummary : [];
      const targetText = targets.length
        ? targets.map((item) => `${item.lineIndex}爻 ${item.liuQin || "-"} ${item.branch || "-"}`).join(" / ")
        : "无目标爻摘要";
      return `
        <article class="rule-item">
          <strong>${hit.ruleName} · ${hit.ruleCode}</strong>
          <p class="rule-meta">${hit.hitReason || "无命中说明"}</p>
          <div class="rule-tags">
            <span class="tag">影响 ${hit.impactLevel || "-"}</span>
            <span class="tag">用神 ${evidence.useGod || "-"}</span>
            <span class="tag">目标数 ${evidence.targetCount ?? 0}</span>
          </div>
          <p class="detail-text">${targetText}</p>
        </article>
      `;
    })
    .join("");
}

function renderCaseList(response) {
  if (!response.items || response.items.length === 0) {
    els.caseList.innerHTML = '<div class="empty-state">当前筛选条件下还没有案例。</div>';
    return;
  }

  els.caseList.innerHTML = response.items
    .map((item) => `
      <article class="case-item ${state.selectedCaseId === item.caseId ? "active" : ""}" data-case-id="${item.caseId}">
        <strong>${item.questionText}</strong>
        <p class="meta-line">${item.questionCategory || "未分类"} · ${item.status}</p>
        <p class="detail-text">${item.mainHexagram || "-"} → ${item.changedHexagram || "-"}</p>
        <div class="rule-tags">
          <span class="tag">卦宫 ${item.palace || "-"}</span>
          <span class="tag">用神 ${item.useGod || "-"}</span>
        </div>
      </article>
    `)
    .join("");

  els.caseList.querySelectorAll(".case-item").forEach((node) => {
    node.addEventListener("click", () => {
      const caseId = Number(node.dataset.caseId);
      loadCaseDetail(caseId);
    });
  });
}

function renderCaseDetail(detail) {
  const snapshot = detail.chartSnapshot || {};
  const context = detail.analysisContext || {};
  els.caseDetail.className = "detail-card";
  els.caseDetail.innerHTML = `
    <strong>${detail.questionText}</strong>
    <p class="detail-meta">${detail.questionCategory || "未分类"} · ${detail.status || "-"}</p>
    <div class="detail-grid">
      <div class="metric"><span class="metric-label">本卦 / 变卦</span><span class="metric-value">${snapshot.mainHexagram || "-"} → ${snapshot.changedHexagram || "-"}</span></div>
      <div class="metric"><span class="metric-label">上下卦</span><span class="metric-value">${snapshot.mainUpperTrigram || "-"}/${snapshot.mainLowerTrigram || "-"} → ${snapshot.changedUpperTrigram || "-"}/${snapshot.changedLowerTrigram || "-"}</span></div>
      <div class="metric"><span class="metric-label">用神</span><span class="metric-value">${snapshot.useGod || "-"}</span></div>
      <div class="metric"><span class="metric-label">分析上下文</span><span class="metric-value">${context.contextVersion || "-"} / ${context.ruleCount || 0} 条规则</span></div>
    </div>
    <div class="detail-tags">
      <span class="tag">世 ${snapshot.shi || "-"}</span>
      <span class="tag">应 ${snapshot.ying || "-"}</span>
      <span class="tag">日辰 ${snapshot.riChen || "-"}</span>
      <span class="tag">月建 ${snapshot.yueJian || "-"}</span>
    </div>
    <pre>${detail.analysis || "暂无分析结果"}</pre>
  `;
}

async function analyze() {
  updateStatus("正在调用分析接口...");
  try {
    const data = await fetch("/api/divinations/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(buildAnalyzePayload()),
    }).then(toJson);

    renderSummary(data);
    renderRules(data.ruleHits || []);
    updateStatus(`分析完成：${data.chartSnapshot.mainHexagram} → ${data.chartSnapshot.changedHexagram}`, "success-text");
    await loadCases();
  } catch (error) {
    updateStatus(`分析失败：${error.message}`, "error-text");
  }
}

async function loadCases() {
  const category = els.searchCategory.value;
  const query = new URLSearchParams({ page: "1", size: "8" });
  if (category) {
    query.set("questionCategory", category);
  }
  const data = await fetch(`/api/cases/search?${query.toString()}`).then(toJson);
  renderCaseList(data);
  if (!state.selectedCaseId && data.items && data.items.length > 0) {
    await loadCaseDetail(data.items[0].caseId);
  }
}

async function loadCaseDetail(caseId) {
  state.selectedCaseId = caseId;
  const detail = await fetch(`/api/cases/${caseId}`).then(toJson);
  renderCaseDetail(detail);
  await loadCasesWithoutReset();
}

async function loadCasesWithoutReset() {
  const category = els.searchCategory.value;
  const query = new URLSearchParams({ page: "1", size: "8" });
  if (category) {
    query.set("questionCategory", category);
  }
  const data = await fetch(`/api/cases/search?${query.toString()}`).then(toJson);
  renderCaseList(data);
}

function fillIncomePreset() {
  document.getElementById("questionText").value = "我下个月工资会不会上涨";
  document.getElementById("questionCategory").value = "收入";
  document.getElementById("divinationTime").value = "2026-04-06T10:00";
  document.getElementById("rawLines").value = "老阳,少阴,少阳,少阴,老阴,少阳";
  document.getElementById("movingLines").value = "1,5";
  updateStatus("已切换到收入固定盘。");
}

els.analyzeForm.addEventListener("submit", (event) => {
  event.preventDefault();
  analyze();
});

els.refreshCases.addEventListener("click", () => {
  loadCases().catch((error) => {
    els.caseList.innerHTML = `<div class="empty-state error-text">案例加载失败：${error.message}</div>`;
  });
});

els.searchCategory.addEventListener("change", () => {
  state.selectedCaseId = null;
  loadCases().catch((error) => {
    els.caseList.innerHTML = `<div class="empty-state error-text">案例加载失败：${error.message}</div>`;
  });
});

els.presetIncome.addEventListener("click", fillIncomePreset);

loadCases().catch(() => {
  els.caseList.innerHTML = '<div class="empty-state">当前还没有案例，先运行一次固定盘。</div>';
});
