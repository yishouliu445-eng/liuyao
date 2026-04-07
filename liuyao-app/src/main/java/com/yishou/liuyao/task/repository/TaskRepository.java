package com.yishou.liuyao.task.repository;

import com.yishou.liuyao.task.domain.DocProcessTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<DocProcessTask, Long> {

    List<DocProcessTask> findTop20ByOrderByIdDesc();
}
