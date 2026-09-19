package com.movierec.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private Long id;

    private String username;

    private String role;

    private String preferences;

    private LocalDateTime createTime;
}
