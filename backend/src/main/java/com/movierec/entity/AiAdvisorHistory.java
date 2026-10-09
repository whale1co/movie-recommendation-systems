package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_advisor_history")
public class AiAdvisorHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String question;
    private String answer;
    private String resultSnapshot;
    private Integer recommendationCount;
    private Boolean aiGenerated;
    private Boolean degraded;
    private String provider;
    private LocalDateTime createdAt;
}
