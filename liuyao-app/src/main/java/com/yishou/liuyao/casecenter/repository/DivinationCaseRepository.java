package com.yishou.liuyao.casecenter.repository;

import com.yishou.liuyao.casecenter.domain.DivinationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DivinationCaseRepository extends JpaRepository<DivinationCase, Long> {

    List<DivinationCase> findTop20ByOrderByDivinationTimeDescIdDesc();
}
