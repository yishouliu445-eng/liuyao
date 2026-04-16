# Homepage Entry And Intent Resolution Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework the homepage into `问事 / 手工起卦`, add server-backed问题方向 resolution with conflict confirmation, and keep the analysis pipeline from being silently biased by mistaken user selections.

**Architecture:** Keep storage-compatible rollout for the first implementation slice: introduce explicit direction-resolution DTOs at the API edge, compute `finalDirection` on the backend, and continue persisting that final value through the existing `questionCategory` column until a later schema migration is justified. On the frontend, split the current monolithic form into two entry modes so beginners land in `问事` while熟手 users can switch to `手工起卦` without losing existing capabilities.

**Tech Stack:** React 18 + TypeScript + Vite, Spring Boot 3.3, JUnit 5, Mockito, existing CSS modules in `src/styles/components.css`

---

## Chunk 1: Backend Direction Resolution And Session Contract

### Task 1: Add a dedicated direction-resolution model and tests

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionDirectionResolution.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionDirectionResolutionService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionIntentResolver.java`
- Test: `liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/QuestionIntentResolverTest.java`
- Test: `liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/QuestionDirectionResolutionServiceTest.java`

- [ ] **Step 1: Write failing resolver tests for conflict and fallback cases**

```java
@Test
void shouldPreferTextDirectionWhenUserDidNotSelectOne() {
    QuestionDirectionResolution result = service.resolve("我和前任还有机会复合吗", null, null);
    assertEquals("感情", result.finalDirection());
    assertFalse(result.requiresConfirmation());
}

@Test
void shouldFlagConflictWhenSelectionDisagreesWithDetectedDirection() {
    QuestionDirectionResolution result = service.resolve("我和前任还有机会复合吗", "出行", null);
    assertTrue(result.requiresConfirmation());
    assertEquals("感情", result.detectedDirection());
    assertEquals("出行", result.userSelectedDirection());
}
```

- [ ] **Step 2: Run targeted tests to verify they fail**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=QuestionIntentResolverTest,QuestionDirectionResolutionServiceTest test`

Expected: FAIL because the new service and conflict behavior do not exist yet.

- [ ] **Step 3: Implement the resolution result model**

```java
public record QuestionDirectionResolution(
        String questionText,
        String detectedDirection,
        String userSelectedDirection,
        String finalDirection,
        boolean requiresConfirmation,
        String suggestedDirection,
        String source,
        double confidence
) {}
```

- [ ] **Step 4: Implement the resolution service**

```java
public QuestionDirectionResolution resolve(String questionText, String userSelectedDirection, String confirmedDirection) {
    String detected = normalizeDetectedDirection(questionText);
    String selected = normalizer.normalize(userSelectedDirection);
    if (selected == null || selected.isBlank()) {
        return resolved(detected, selected, detected, false, detected, "detected", confidenceOf(detected));
    }
    if (detected.equals(selected) || detected.isBlank()) {
        return resolved(detected, selected, selected, false, selected, "user_selected", confidenceOf(detected));
    }
    if (selected.equals(confirmedDirection) || detected.equals(confirmedDirection)) {
        return resolved(detected, selected, confirmedDirection, false, detected, "confirmed", confidenceOf(detected));
    }
    return resolved(detected, selected, selected, true, detected, "conflict", confidenceOf(detected));
}
```

- [ ] **Step 5: Refactor `QuestionIntentResolver` to expose a reusable text-to-direction mapping**

Implementation notes:
- Keep `resolve(questionText, questionCategory)` for existing rule-engine callers.
- Extract text parsing into a method the new service can call without duplicating keyword logic.
- Keep normalization centralized through `QuestionCategoryNormalizer`.

- [ ] **Step 6: Run the targeted tests again**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=QuestionIntentResolverTest,QuestionDirectionResolutionServiceTest test`

Expected: PASS.

- [ ] **Step 7: Commit the backend resolution slice**

```bash
cd /Users/liuyishou/wordspace/liuyao
git add liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionDirectionResolution.java \
        liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionDirectionResolutionService.java \
        liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionIntentResolver.java \
        liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/QuestionIntentResolverTest.java \
        liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/QuestionDirectionResolutionServiceTest.java
git commit -m "feat: add question direction resolution service"
```

### Task 2: Expose resolution data through session APIs without breaking storage

**Files:**
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/session/dto/SessionCreateRequest.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/session/dto/SessionCreateResponse.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/session/service/SessionService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/dto/DivinationAnalyzeRequest.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/mapper/DivinationMapper.java`
- Modify: `liuyao-app/src/test/java/com/yishou/liuyao/divination/mapper/DivinationMapperTest.java`
- Modify: `liuyao-app/src/test/java/com/yishou/liuyao/session/service/SessionServiceTest.java`

- [ ] **Step 1: Write failing tests for request-to-finalDirection mapping**

```java
@Test
void shouldMapFinalDirectionIntoAnalyzeRequest() {
    SessionCreateRequest request = new SessionCreateRequest();
    request.setQuestionText("我和前任还有机会复合吗");
    request.setUserSelectedDirection("出行");
    request.setFinalDirection("感情");

    DivinationAnalyzeRequest analyze = service.toAnalyzeRequest(request);

    assertEquals("感情", analyze.getQuestionCategory());
}
```

- [ ] **Step 2: Run focused tests to confirm failure**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=DivinationMapperTest,SessionServiceTest test`

Expected: FAIL because `userSelectedDirection` and `finalDirection` do not exist yet.

- [ ] **Step 3: Extend session and analysis request DTOs with edge-facing direction fields**

Implementation notes:
- Add `userSelectedDirection`, `finalDirection`, `detectedDirection`, `requiresDirectionConfirmation`, and `suggestedDirection` where each is appropriate.
- Keep `questionCategory` in responses for backward compatibility during rollout.
- Do not add a database migration in this slice.

- [ ] **Step 4: Update `SessionService.createSession` to resolve and enforce final direction**

```java
QuestionDirectionResolution resolution = directionResolutionService.resolve(
        request.getQuestionText(),
        request.getUserSelectedDirection(),
        request.getFinalDirection()
);
if (resolution.requiresConfirmation()) {
    throw new BusinessException(ErrorCode.BAD_REQUEST, buildDirectionConflictMessage(resolution));
}
DivinationAnalyzeRequest divRequest = toAnalyzeRequest(request, resolution.finalDirection());
```

Implementation notes:
- Persist `resolution.finalDirection()` through the existing `questionCategory` path for now.
- Return the detected and final direction metadata in `SessionCreateResponse` so the frontend can stay synchronized.

- [ ] **Step 5: Update `DivinationMapper` to always normalize the final direction that enters the domain layer**

```java
input.setQuestionCategory(questionCategoryNormalizer.normalize(request.getQuestionCategory()));
```

Change source of `request.getQuestionCategory()` so it now represents the backend-approved final direction.

- [ ] **Step 6: Re-run focused tests**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=DivinationMapperTest,SessionServiceTest test`

Expected: PASS.

- [ ] **Step 7: Commit the API contract slice**

```bash
cd /Users/liuyishou/wordspace/liuyao
git add liuyao-app/src/main/java/com/yishou/liuyao/session/dto/SessionCreateRequest.java \
        liuyao-app/src/main/java/com/yishou/liuyao/session/dto/SessionCreateResponse.java \
        liuyao-app/src/main/java/com/yishou/liuyao/session/service/SessionService.java \
        liuyao-app/src/main/java/com/yishou/liuyao/divination/dto/DivinationAnalyzeRequest.java \
        liuyao-app/src/main/java/com/yishou/liuyao/divination/mapper/DivinationMapper.java \
        liuyao-app/src/test/java/com/yishou/liuyao/divination/mapper/DivinationMapperTest.java \
        liuyao-app/src/test/java/com/yishou/liuyao/session/service/SessionServiceTest.java
git commit -m "feat: resolve final direction during session creation"
```

## Chunk 2: Frontend Homepage Split Into 问事 / 手工起卦

### Task 3: Replace the single form with a two-mode homepage shell

**Files:**
- Modify: `liuyao-h5/src/pages/HomePage.tsx`
- Create: `liuyao-h5/src/components/input/EntryModeTabs.tsx`
- Create: `liuyao-h5/src/components/input/AskedMatterForm.tsx`
- Create: `liuyao-h5/src/components/input/ManualDivinationForm.tsx`
- Modify: `liuyao-h5/src/components/input/QuestionForm.tsx`
- Modify: `liuyao-h5/src/components/input/CategoryTags.tsx`
- Modify: `liuyao-h5/src/components/input/LineInput.tsx`
- Modify: `liuyao-h5/src/constants/categories.ts`
- Modify: `liuyao-h5/src/styles/components.css`
- Test: `liuyao-h5/src/__tests__/home-page.contract.ts`

- [ ] **Step 1: Add a failing homepage contract file that imports the new shell pieces**

```ts
import HomePage from '../pages/HomePage';
import EntryModeTabs from '../components/input/EntryModeTabs';
import AskedMatterForm from '../components/input/AskedMatterForm';
import ManualDivinationForm from '../components/input/ManualDivinationForm';

void HomePage;
void EntryModeTabs;
void AskedMatterForm;
void ManualDivinationForm;
```

- [ ] **Step 2: Run the frontend build to verify the new imports fail**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: FAIL because the new components do not exist yet.

- [ ] **Step 3: Build the two-mode homepage shell**

Implementation notes:
- `HomePage.tsx` owns the active tab state: `问事` by default, `手工起卦` secondary.
- `EntryModeTabs.tsx` renders the segmented control with accessible buttons and the readable selected/unselected color pairs from the spec.
- Keep the page-level loading/error behavior in `HomePage` so both forms share it.

- [ ] **Step 4: Split the current form into beginner and expert paths**

Implementation notes:
- `AskedMatterForm.tsx` contains: title, helper copy, question input, system-resolved direction row, 问事时间, optional direction editor, and `进入问事`.
- `ManualDivinationForm.tsx` contains: question input, optional direction selector, six-line input, 起卦时间, 起卦方式, and `开始起卦`.
- Leave `QuestionForm.tsx` either as a thin compatibility wrapper or remove its responsibilities entirely once both new forms compile.

- [ ] **Step 5: Rename and restyle categories into directions**

Implementation notes:
- Update visible label copy from `问题分类` to `问题方向`.
- Remove the hard-coded default `出行`.
- Add an empty state such as `待识别` or `未改定`.
- Adjust pill styling so selected tabs use a light gold background with dark text; unselected tabs stay dark with light text.

- [ ] **Step 6: Keep six-line interaction intact while polishing copy**

Implementation notes:
- Preserve click-to-toggle yin/yang and double-click/long-press moving-line behavior.
- Rename visible labels: `所问何事`, `问事时间`, `问题方向`, `进入问事`, `开始起卦`.

- [ ] **Step 7: Re-run the frontend build**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: PASS.

- [ ] **Step 8: Commit the homepage shell slice**

```bash
cd /Users/liuyishou/wordspace/liuyao
git add liuyao-h5/src/pages/HomePage.tsx \
        liuyao-h5/src/components/input/EntryModeTabs.tsx \
        liuyao-h5/src/components/input/AskedMatterForm.tsx \
        liuyao-h5/src/components/input/ManualDivinationForm.tsx \
        liuyao-h5/src/components/input/QuestionForm.tsx \
        liuyao-h5/src/components/input/CategoryTags.tsx \
        liuyao-h5/src/components/input/LineInput.tsx \
        liuyao-h5/src/constants/categories.ts \
        liuyao-h5/src/styles/components.css \
        liuyao-h5/src/__tests__/home-page.contract.ts
git commit -m "feat: add asked-matter and manual-divination homepage modes"
```

### Task 4: Wire the frontend to backend direction resolution and confirmation flow

**Files:**
- Create: `liuyao-h5/src/components/input/DirectionConfirmationDialog.tsx`
- Create: `liuyao-h5/src/components/input/useDirectionResolution.ts`
- Modify: `liuyao-h5/src/api/sessions.ts`
- Modify: `liuyao-h5/src/types/divination.ts`
- Modify: `liuyao-h5/src/types/session.ts`
- Modify: `liuyao-h5/src/components/input/AskedMatterForm.tsx`
- Modify: `liuyao-h5/src/components/input/ManualDivinationForm.tsx`
- Modify: `liuyao-h5/src/styles/components.css`

- [ ] **Step 1: Add type-level failing usage for resolution metadata**

```ts
const req: SessionCreateRequestDTO = {
  questionText: '我和前任还有机会复合吗',
  userSelectedDirection: '出行',
  finalDirection: '感情',
  divinationMethod: '问事',
  divinationTime: '2026-04-16T09:19:00',
  rawLines: [],
  movingLines: [],
};
```

- [ ] **Step 2: Run frontend build to confirm the DTO mismatch fails**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: FAIL because the DTO and response normalizers do not support the new fields yet.

- [ ] **Step 3: Extend the frontend session API types**

Implementation notes:
- Add `userSelectedDirection`, `detectedDirection`, `finalDirection`, `suggestedDirection`, `requiresDirectionConfirmation`.
- Normalize both legacy `questionCategory` and new direction fields so old responses still render.

- [ ] **Step 4: Add a resolution hook for local page state**

Implementation notes:
- `useDirectionResolution.ts` owns the current question text, user-selected direction, detected direction metadata from the backend response, and pending conflict state.
- Do not create a second global state store.

- [ ] **Step 5: Show light confirmation only when the backend reports a conflict**

Implementation notes:
- Submit once with the current draft.
- If backend rejects with a direction-conflict payload, open `DirectionConfirmationDialog`.
- Retry with `finalDirection` set to the user-confirmed choice.

- [ ] **Step 6: Re-run frontend build**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: PASS.

- [ ] **Step 7: Commit the frontend resolution slice**

```bash
cd /Users/liuyishou/wordspace/liuyao
git add liuyao-h5/src/components/input/DirectionConfirmationDialog.tsx \
        liuyao-h5/src/components/input/useDirectionResolution.ts \
        liuyao-h5/src/api/sessions.ts \
        liuyao-h5/src/types/divination.ts \
        liuyao-h5/src/types/session.ts \
        liuyao-h5/src/components/input/AskedMatterForm.tsx \
        liuyao-h5/src/components/input/ManualDivinationForm.tsx \
        liuyao-h5/src/styles/components.css
git commit -m "feat: add direction conflict confirmation flow"
```

## Chunk 3: Integration Polish, Display Compatibility, And Verification

### Task 5: Keep downstream pages stable while surfacing the new wording

**Files:**
- Modify: `liuyao-h5/src/pages/SessionPage.tsx`
- Modify: `liuyao-h5/src/pages/CasesPage.tsx`
- Modify: `liuyao-h5/src/pages/CaseDetailPage.tsx`
- Modify: `liuyao-h5/src/pages/CalendarPage.tsx`
- Modify: `liuyao-h5/src/components/history/SessionCard.tsx`
- Modify: `liuyao-h5/src/components/chart/CollapsibleChart.tsx`
- Modify: `liuyao-h5/src/api/cases.ts`
- Modify: `liuyao-h5/src/api/calendar.ts`

- [ ] **Step 1: Add a failing display expectation via build-safe type updates**

Implementation notes:
- Change session and case consumers to prefer `finalDirection` display, then fall back to legacy `questionCategory`.
- The initial build should fail until all affected views compile.

- [ ] **Step 2: Run frontend build and observe failures**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: FAIL on the first incomplete set of view updates.

- [ ] **Step 3: Update views to use the new wording**

Implementation notes:
- Replace visible `未分类` language with `未改定` or `未识别` where it fits the new UX.
- Keep filter APIs backward compatible by continuing to query the existing server-side `questionCategory` parameter until server list endpoints are expanded.

- [ ] **Step 4: Re-run frontend build**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: PASS.

- [ ] **Step 5: Commit the compatibility slice**

```bash
cd /Users/liuyishou/wordspace/liuyao
git add liuyao-h5/src/pages/SessionPage.tsx \
        liuyao-h5/src/pages/CasesPage.tsx \
        liuyao-h5/src/pages/CaseDetailPage.tsx \
        liuyao-h5/src/pages/CalendarPage.tsx \
        liuyao-h5/src/components/history/SessionCard.tsx \
        liuyao-h5/src/components/chart/CollapsibleChart.tsx \
        liuyao-h5/src/api/cases.ts \
        liuyao-h5/src/api/calendar.ts
git commit -m "feat: align downstream views with direction terminology"
```

### Task 6: Run end-to-end verification and capture rollout notes

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-16-homepage-entry-intent-design.md`

- [ ] **Step 1: Run backend focused tests**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=QuestionIntentResolverTest,QuestionDirectionResolutionServiceTest,DivinationMapperTest,SessionServiceTest test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run: `cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build`

Expected: PASS.

- [ ] **Step 3: Run the full local smoke flow**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao
curl -s http://localhost:8080/api/sessions >/tmp/sessions-smoke.json && cat /tmp/sessions-smoke.json
```

Expected: `200` response with a valid JSON envelope.

Manual checks:
- Homepage defaults to `问事`
- Switching to `手工起卦` preserves question text if already entered
- Selecting a conflicting direction triggers a lightweight confirmation
- Selected entry tabs are readable against the dark background

- [ ] **Step 4: Update docs with the shipped naming and rollout caveat**

Implementation notes:
- README should mention the new dual-entry homepage briefly.
- Spec note should record that the first slice persists `finalDirection` in the legacy `questionCategory` field to avoid a broad migration.

- [ ] **Step 5: Commit verification and docs updates**

```bash
cd /Users/liuyishou/wordspace/liuyao
git add README.md docs/superpowers/specs/2026-04-16-homepage-entry-intent-design.md
git commit -m "docs: record homepage entry rollout notes"
```

## Notes For The Implementer

- Keep `start.sh` untouched; it already has unrelated local changes in the worktree.
- Keep `.superpowers/` untracked.
- Do not widen scope into case-center schema changes on the first implementation slice.
- If direction confidence becomes noisy, lower-risk fallback is to require confirmation only when the backend has both a detected direction and a non-empty conflicting user selection.

## Plan Review Note

Subagent-based plan review was not run here because delegation is not available in this session policy. Review this plan manually before execution and tighten any file list that changes during implementation discovery.
