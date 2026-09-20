package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("security_audit_event")
public class SecurityAuditEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long actorUserId;
    private String eventType;
    private String outcome;
    private String subject;
    private String ipAddress;
    private String requestId;
    private String metadata;
    private LocalDateTime createdAt;
}
