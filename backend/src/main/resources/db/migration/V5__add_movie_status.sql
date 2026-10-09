ALTER TABLE `movie`
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '电影状态: ACTIVE/DELETED';

CREATE INDEX `idx_movie_status` ON `movie` (`status`);
