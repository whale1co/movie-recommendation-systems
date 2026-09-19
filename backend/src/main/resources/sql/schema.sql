CREATE DATABASE IF NOT EXISTS movie_rec DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE movie_rec;

CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL,
    `password`    VARCHAR(100) NOT NULL,
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '用户角色: USER/ADMIN',
    `preferences` VARCHAR(255) DEFAULT NULL COMMENT '偏好类型，逗号分隔',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `movie` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `douban_id`     VARCHAR(20)   DEFAULT NULL COMMENT '豆瓣ID',
    `title`         VARCHAR(200)  NOT NULL,
    `director`      VARCHAR(200)  DEFAULT NULL,
    `actors`        TEXT          DEFAULT NULL COMMENT '演员列表，逗号分隔',
    `genre`         VARCHAR(255)  DEFAULT NULL COMMENT '类型，逗号分隔',
    `release_date`  DATE          DEFAULT NULL,
    `runtime`       INT           DEFAULT NULL COMMENT '时长(分钟)',
    `summary`       TEXT          DEFAULT NULL,
    `poster_url`    VARCHAR(500)  DEFAULT NULL,
    `douban_rating` DECIMAL(3,1)  DEFAULT NULL COMMENT '豆瓣评分',
    `avg_rating`    DECIMAL(3,1)  DEFAULT NULL COMMENT '本站均分',
    `rating_count`  INT           NOT NULL DEFAULT 0 COMMENT '评分人数',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_douban_id` (`douban_id`),
    KEY `idx_genre` (`genre`(191)),
    KEY `idx_rating_count` (`rating_count`, `avg_rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影表';

CREATE TABLE `rating` (
    `id`          BIGINT     NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT     NOT NULL,
    `movie_id`    BIGINT     NOT NULL,
    `score`       TINYINT    NOT NULL COMMENT '评分1-5',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_movie` (`user_id`, `movie_id`),
    KEY `idx_movie_id` (`movie_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

CREATE TABLE `favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT   NOT NULL,
    `movie_id`    BIGINT   NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_movie` (`user_id`, `movie_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE `comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `movie_id`    BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `like_count`  INT          DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_comment_movie_id` (`movie_id`),
    KEY `idx_comment_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE `comment_like` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `comment_id`  BIGINT       NOT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
    KEY `idx_like_comment_id` (`comment_id`),
    KEY `idx_like_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

CREATE TABLE `crawl_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `task_type`   VARCHAR(50)  NOT NULL COMMENT '任务类型',
    `status`      VARCHAR(20)  NOT NULL COMMENT '状态: RUNNING/SUCCESS/FAILED',
    `message`     TEXT         DEFAULT NULL COMMENT '日志信息',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time`    DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='爬虫日志表';
