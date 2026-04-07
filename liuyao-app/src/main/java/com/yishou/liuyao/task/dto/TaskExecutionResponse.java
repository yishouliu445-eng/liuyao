package com.yishou.liuyao.task.dto;

public class TaskExecutionResponse {

    private Long taskId;
    private String status;
    private Integer createdReferenceCount;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatedReferenceCount() {
        return createdReferenceCount;
    }

    public void setCreatedReferenceCount(Integer createdReferenceCount) {
        this.createdReferenceCount = createdReferenceCount;
    }
}
