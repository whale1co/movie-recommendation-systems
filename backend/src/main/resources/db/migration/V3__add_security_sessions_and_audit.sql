CREATE TABLE `refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `session_id` VARCHAR(36) NOT NULL,
    `token_hash` CHAR(64) NOT NULL,
    `expires_at` DATETIME(6) NOT NULL,
    `revoked_at` DATETIME(6) DEFAULT NULL,
    `replaced_by_token_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `last_used_at` DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    UNIQUE KEY `uk_refresh_token_session` (`session_id`),
    KEY `idx_refresh_token_user` (`user_id`, `revoked_at`),
    CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `security_audit_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `actor_user_id` BIGINT DEFAULT NULL,
    `event_type` VARCHAR(60) NOT NULL,
    `outcome` VARCHAR(20) NOT NULL,
    `subject` VARCHAR(100) DEFAULT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `request_id` VARCHAR(100) DEFAULT NULL,
    `metadata` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    KEY `idx_audit_created_at` (`created_at`),
    KEY `idx_audit_actor_created` (`actor_user_id`, `created_at`),
    KEY `idx_audit_type_created` (`event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `permission` (`code`, `name`) VALUES ('ai:chat', '使用 AI 选片顾问');
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
    SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p
    WHERE r.code IN ('USER', 'ADMIN') AND p.code = 'ai:chat';
