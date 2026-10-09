package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_task")
public class AdminTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private String status;
    private String paramsJson;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
