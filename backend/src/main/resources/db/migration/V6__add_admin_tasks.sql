CREATE TABLE IF NOT EXISTS `admin_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_type` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `params_json` TEXT DEFAULT NULL,
    `total_count` INT NOT NULL DEFAULT 0,
    `success_count` INT NOT NULL DEFAULT 0,
    `failed_count` INT NOT NULL DEFAULT 0,
    `error_message` TEXT DEFAULT NULL,
    `created_by` BIGINT NOT NULL,
    `started_at` DATETIME DEFAULT NULL,
    `finished_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_admin_task_status_type` (`status`, `task_type`),
    KEY `idx_admin_task_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_task_error` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `row_number` INT DEFAULT NULL,
    `target_key` VARCHAR(255) DEFAULT NULL,
    `error_type` VARCHAR(50) DEFAULT NULL,
    `error_message` TEXT NOT NULL,
    `raw_data` TEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_admin_task_error_task_id` (`task_id`),
    CONSTRAINT `fk_admin_task_error_task` FOREIGN KEY (`task_id`) REFERENCES `admin_task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
