package com.yishou.liuyao.calendar.repository;

import com.yishou.liuyao.calendar.domain.VerificationEvent;
import com.yishou.liuyao.calendar.domain.VerificationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationFeedbackRepository extends JpaRepository<VerificationFeedback, UUID> {

    Optional<VerificationFeedback> findByEvent(VerificationEvent event);

    Optional<VerificationFeedback> findByEvent_Id(UUID eventId);

    boolean existsByEvent_Id(UUID eventId);
}
