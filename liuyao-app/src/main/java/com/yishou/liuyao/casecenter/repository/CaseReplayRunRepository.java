package com.yishou.liuyao.casecenter.repository;

import com.yishou.liuyao.casecenter.domain.CaseReplayRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseReplayRunRepository extends JpaRepository<CaseReplayRun, Long> {

    List<CaseReplayRun> findByCaseIdOrderByIdDesc(Long caseId);

    Page<CaseReplayRun> findAllByOrderByIdDesc(Pageable pageable);

    Page<CaseReplayRun> findByRecommendPersistReplayOrderByIdDesc(Boolean recommendPersistReplay, Pageable pageable);

    Page<CaseReplayRun> findByQuestionCategoryOrderByIdDesc(String questionCategory, Pageable pageable);

    Page<CaseReplayRun> findByQuestionCategoryAndRecommendPersistReplayOrderByIdDesc(String questionCategory,
                                                                                     Boolean recommendPersistReplay,
                                                                                     Pageable pageable);
}
