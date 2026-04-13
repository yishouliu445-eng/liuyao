package com.yishou.liuyao.calendar.repository;

import com.yishou.liuyao.calendar.domain.VerificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationEventRepository extends JpaRepository<VerificationEvent, UUID> {

    List<VerificationEvent> findBySessionIdOrderByPredictedDateDescCreatedAtDesc(UUID sessionId);

    List<VerificationEvent> findByUserIdOrderByPredictedDateDescCreatedAtDesc(Long userId);

    List<VerificationEvent> findByStatusOrderByPredictedDateAscCreatedAtAsc(String status);

    List<VerificationEvent> findByStatusAndPredictedDateLessThanEqualOrderByPredictedDateAscCreatedAtAsc(
            String status, LocalDate predictedDate);

    Page<VerificationEvent> findByPredictedDateBetweenOrderByPredictedDateAscCreatedAtAsc(
            LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<VerificationEvent> findByUserIdAndPredictedDateBetweenOrderByPredictedDateAscCreatedAtAsc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<VerificationEvent> findByUserIdOrderByPredictedDateDescCreatedAtDesc(Long userId, Pageable pageable);

    Page<VerificationEvent> findAllByOrderByPredictedDateDescCreatedAtDesc(Pageable pageable);

    Optional<VerificationEvent> findBySessionIdAndPredictedDate(UUID sessionId, LocalDate predictedDate);
}
