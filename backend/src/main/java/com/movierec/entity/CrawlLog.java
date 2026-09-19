package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crawl_log")
public class CrawlLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskType;

    private String status;

    private String message;

    private LocalDateTime createTime;

    private LocalDateTime endTime;
}
