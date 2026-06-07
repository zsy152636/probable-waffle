-- 题目生成配置系统 - 增量迁移脚本（主子表模式）
-- 适用于已有数据库

-- 1. test_paper 表新增统计字段（如已存在请忽略报错）
ALTER TABLE `test_paper`
    ADD COLUMN `short_count` INT NOT NULL DEFAULT 0 COMMENT '简答题数量' AFTER `fill_count`;
ALTER TABLE `test_paper`
    ADD COLUMN `code_count` INT NOT NULL DEFAULT 0 COMMENT '代码题数量' AFTER `short_count`;
ALTER TABLE `test_paper`
    ADD COLUMN `calc_count` INT NOT NULL DEFAULT 0 COMMENT '计算大题数量' AFTER `code_count`;

-- 2. 处理旧表 type_config 列（如果存在则删除）
-- ALTER TABLE `question_gen_config` DROP COLUMN IF EXISTS `type_config`;
-- MySQL 不支持 DROP COLUMN IF EXISTS，用存储过程兜底

-- 2. 新建/更新题目生成配置主表
CREATE TABLE IF NOT EXISTS `question_gen_config` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `name`          VARCHAR(100) NOT NULL COMMENT '配置名称',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
    `total_count`   INT          NOT NULL DEFAULT 0 COMMENT '总题数',
    `need_passage`  TINYINT      NOT NULL DEFAULT 0 COMMENT '选择题是否强制附带阅读材料',
    `direction`     TEXT         DEFAULT NULL COMMENT '出题方向/额外提示词',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `is_active`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目生成配置表';

-- 3. 新建题目生成配置明细表
CREATE TABLE IF NOT EXISTS `question_gen_config_type` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `config_id`     BIGINT       NOT NULL COMMENT '配置ID',
    `question_type` VARCHAR(20)  NOT NULL COMMENT '题型',
    `count`         INT          NOT NULL DEFAULT 0 COMMENT '题目数量',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目生成配置题型明细表';

-- 4. 预置默认配置
INSERT INTO `question_gen_config` (`id`, `name`, `description`, `total_count`, `need_passage`, `direction`, `sort`, `is_active`, `create_time`, `update_time`) VALUES
(1, '通用标准配置', '包含选择、多选、判断、简答的均衡配置', 25, 0, '覆盖文档核心知识点，难度均匀分布', 1, 1, NOW(), NOW()),
(2, '政考配置', '不含多选题和判断题，选择题需附带阅读材料', 20, 1, '选择题需附带约200字阅读材料，侧重政策理解与分析能力', 2, 1, NOW(), NOW()),
(3, '编程语言试卷配置', '包含代码题和计算大题', 30, 0, '侧重编程语言核心概念、算法思维和实际编码能力', 3, 1, NOW(), NOW());

INSERT INTO `question_gen_config_type` (`id`, `config_id`, `question_type`, `count`, `sort`) VALUES
-- 通用标准配置
(101, 1, 'SINGLE', 10, 1),
(102, 1, 'MULTI',   5, 2),
(103, 1, 'JUDGE',   5, 3),
(104, 1, 'SHORT',   5, 4),
-- 政考配置
(201, 2, 'SINGLE', 15, 1),
(202, 2, 'SHORT',   5, 2),
-- 编程语言试卷配置
(301, 3, 'SINGLE',       10, 1),
(302, 3, 'MULTI',         5, 2),
(303, 3, 'JUDGE',         5, 3),
(304, 3, 'FILL',          3, 4),
(305, 3, 'CODE',          5, 5),
(306, 3, 'CALCULATION',   2, 6);
