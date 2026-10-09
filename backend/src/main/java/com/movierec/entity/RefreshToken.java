package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("refresh_token")
public class RefreshToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private Long replacedByTokenId;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
