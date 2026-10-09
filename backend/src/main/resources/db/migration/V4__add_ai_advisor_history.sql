CREATE TABLE `ai_advisor_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `question` VARCHAR(1000) NOT NULL,
    `answer` TEXT NOT NULL,
    `result_snapshot` LONGTEXT NOT NULL,
    `recommendation_count` INT NOT NULL DEFAULT 0,
    `ai_generated` TINYINT(1) NOT NULL DEFAULT 0,
    `degraded` TINYINT(1) NOT NULL DEFAULT 0,
    `provider` VARCHAR(100) DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    KEY `idx_ai_history_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_ai_history_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
