CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色: USER/ADMIN',
    `preferences` VARCHAR(255) DEFAULT NULL COMMENT '偏好类型，逗号分隔',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `movie` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `douban_id` VARCHAR(20) DEFAULT NULL,
    `title` VARCHAR(200) NOT NULL,
    `director` VARCHAR(200) DEFAULT NULL,
    `actors` TEXT DEFAULT NULL,
    `genre` VARCHAR(255) DEFAULT NULL,
    `release_date` DATE DEFAULT NULL,
    `runtime` INT DEFAULT NULL,
    `summary` TEXT DEFAULT NULL,
    `poster_url` VARCHAR(500) DEFAULT NULL,
    `douban_rating` DECIMAL(3,1) DEFAULT NULL,
    `avg_rating` DECIMAL(3,1) DEFAULT NULL,
    `rating_count` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_douban_id` (`douban_id`),
    KEY `idx_genre` (`genre`(191)), KEY `idx_rating_count` (`rating_count`, `avg_rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `movie_id` BIGINT NOT NULL,
    `score` TINYINT NOT NULL, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_user_movie` (`user_id`, `movie_id`),
    KEY `idx_movie_id` (`movie_id`), KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `movie_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_user_movie` (`user_id`, `movie_id`), KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `movie_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL, `like_count` INT DEFAULT 0, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_comment_movie_id` (`movie_id`), KEY `idx_comment_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `comment_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
    KEY `idx_like_comment_id` (`comment_id`), KEY `idx_like_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crawl_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `task_type` VARCHAR(50) NOT NULL, `status` VARCHAR(20) NOT NULL,
    `message` TEXT DEFAULT NULL, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time` DATETIME DEFAULT NULL, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
