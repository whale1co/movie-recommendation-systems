package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_task_error")
public class AdminTaskError {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer rowNumber;
    private String targetKey;
    private String errorType;
    private String errorMessage;
    private String rawData;
    private LocalDateTime createdAt;
}
