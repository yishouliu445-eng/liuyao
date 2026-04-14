package com.yishou.liuyao.ops.job.repository;

import com.yishou.liuyao.ops.job.domain.JobLease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobLeaseRepository extends JpaRepository<JobLease, Long> {

    Optional<JobLease> findByJobName(String jobName);
}
