ALTER TABLE `user` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态: ACTIVE/DISABLED/DELETED';

CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(100) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `role_permission` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `role` (`code`, `name`) VALUES ('USER', '普通用户'), ('ADMIN', '管理员');
INSERT IGNORE INTO `permission` (`code`, `name`) VALUES
    ('movie:read', '查看电影'), ('rating:write', '管理评分'), ('favorite:write', '管理收藏'),
    ('comment:write', '发布评论'), ('user:read', '查看用户'), ('user:write', '管理用户'),
    ('movie:write', '管理电影'), ('crawl:execute', '执行爬取'), ('audit:read', '查看审计');
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
    SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p WHERE r.code = 'USER' AND p.code IN ('movie:read','rating:write','favorite:write','comment:write');
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
    SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p WHERE r.code = 'ADMIN';
INSERT IGNORE INTO `user_role` (`user_id`, `role_id`)
    SELECT u.id, r.id FROM `user` u JOIN `role` r ON r.code = COALESCE(NULLIF(u.role, ''), 'USER');