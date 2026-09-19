USE movie_rec;

-- 适用于已经存在用户数据的数据库。执行前请先备份数据库。
ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色: USER/ADMIN' AFTER `password`;

CREATE TABLE IF NOT EXISTS `comment` (
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

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `comment_id`  BIGINT       NOT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
    KEY `idx_like_comment_id` (`comment_id`),
    KEY `idx_like_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- 只将没有角色的历史账号视为普通用户。管理员请按实际账号修改。
UPDATE `user` SET `role` = 'USER' WHERE `role` IS NULL OR `role` = '';
-- 示例：UPDATE `user` SET `role` = 'ADMIN' WHERE `username` = '你的管理员用户名';
