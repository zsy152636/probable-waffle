-- ============================================================================
-- AI 智能答题系统 — 完整初始化 SQL 脚本
-- 适用数据库：MySQL 8.0+
-- 说明：一键初始化整个系统的数据库表结构与默认数据
-- 使用方法： mysql -u root -p < init.sql
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 创建数据库（如不存在）
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `ai_quiz` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ai_quiz`;

-- ============================================================================
-- 1. 系统基础表：用户、角色、菜单、关联关系
-- ============================================================================

-- 1.1 系统用户
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`      VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    `real_name`     VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 1.2 角色
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `role_name`     VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `role_code`     VARCHAR(50)  NOT NULL COMMENT '角色编码',
    `description`   VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- 1.3 菜单/权限（支持层级目录、菜单、按钮三级）
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID（0表示顶级）',
    `menu_name`     VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    `path`          VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    `component`     VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
    `icon`          VARCHAR(50)  DEFAULT NULL COMMENT '图标名称',
    `type`          TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：0-目录 1-菜单 2-按钮',
    `permission`    VARCHAR(100) DEFAULT NULL COMMENT '权限标识（如 user:list）',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `visible`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限表';

-- 1.4 用户-角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- 1.5 角色-菜单/权限关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单/权限ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单/权限关联表';

-- ============================================================================
-- 2. AI 配置表
-- ============================================================================

DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `provider_name` VARCHAR(100) NOT NULL COMMENT '服务厂商名称（如 OpenAI、Anthropic、DeepSeek）',
    `api_url`       VARCHAR(500) NOT NULL COMMENT 'API 访问地址',
    `api_token`     VARCHAR(500) NOT NULL COMMENT 'API Token / Key',
    `model_name`    VARCHAR(100) NOT NULL COMMENT '模型名称（如 gpt-4o、deepseek-chat）',
    `max_tokens`    INT          NOT NULL DEFAULT 4096 COMMENT '最大输出 Token 数',
    `temperature`   DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '温度参数（0~2）',
    `is_active`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用：0-禁用 1-启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 服务配置表';

-- ============================================================================
-- 3. 领域业务表：文档、题目、选项、试卷、答案、答题结果
-- ============================================================================

-- 3.1 文档表
DROP TABLE IF EXISTS `doc_document`;
CREATE TABLE `doc_document` (
    `id`                 BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `doc_no`             VARCHAR(32)  NOT NULL COMMENT '文档编号（格式：WD+雪花ID）',
    `doc_name`           VARCHAR(200) NOT NULL COMMENT '文档名称',
    `doc_type`           VARCHAR(20)  NOT NULL COMMENT '文档类型：TXT / WORD / PDF',
    `file_path`          VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size`          BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `content_text`       LONGTEXT     DEFAULT NULL COMMENT '解析后的纯文本内容',
    `question_count`     INT          NOT NULL DEFAULT 0 COMMENT '已生成的题目数量',
    `question_gen_time`  DATETIME     DEFAULT NULL COMMENT '最近一次题目生成时间',
    `upload_user_id`     BIGINT       NOT NULL COMMENT '上传用户ID',
    `upload_user_name`   VARCHAR(50)  NOT NULL COMMENT '上传用户名',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_no` (`doc_no`),
    KEY `idx_upload_user` (`upload_user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 3.2 题目表
DROP TABLE IF EXISTS `doc_question`;
CREATE TABLE `doc_question` (
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `document_id`     BIGINT       NOT NULL COMMENT '所属文档ID',
    `question_type`   VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE/MULTI/JUDGE/FILL/SHORT/CODE/CALCULATION',
    `question_title`  TEXT         NOT NULL COMMENT '题干内容',
    `passage`         TEXT         DEFAULT NULL COMMENT '阅读材料/代码上下文/背景信息',
    `analysis`        TEXT         DEFAULT NULL COMMENT '答案解析',
    `difficulty`      TINYINT      NOT NULL DEFAULT 1 COMMENT '难度：1-简单 2-中等 3-困难',
    `score`           INT          NOT NULL DEFAULT 5 COMMENT '分值',
    `sort`            INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待确认 1-已确认',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_question_type` (`question_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 3.3 题目选项表
DROP TABLE IF EXISTS `doc_question_option`;
CREATE TABLE `doc_question_option` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `question_id`   BIGINT       NOT NULL COMMENT '题目ID',
    `option_label`  VARCHAR(10)  NOT NULL COMMENT '选项标签（如 A/B/C/D）',
    `option_text`   TEXT         NOT NULL COMMENT '选项内容',
    `is_correct`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否为正确答案：0-否 1-是',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

-- 3.4 试卷表（确认题目后自动生成）
DROP TABLE IF EXISTS `test_paper`;
CREATE TABLE `test_paper` (
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `paper_no`        VARCHAR(32)  NOT NULL COMMENT '试题编号（格式：TP+雪花ID）',
    `paper_name`      VARCHAR(200) NOT NULL COMMENT '试题名称（取自文档名称）',
    `document_id`     BIGINT       NOT NULL COMMENT '来源文档ID',
    `doc_no`          VARCHAR(32)  NOT NULL COMMENT '来源文档编号',
    `total_count`     INT          NOT NULL DEFAULT 0 COMMENT '题目总数',
    `single_count`    INT          NOT NULL DEFAULT 0 COMMENT '单选题数量',
    `multi_count`     INT          NOT NULL DEFAULT 0 COMMENT '多选题数量',
    `judge_count`     INT          NOT NULL DEFAULT 0 COMMENT '判断题数量',
    `fill_count`      INT          NOT NULL DEFAULT 0 COMMENT '填空题数量',
    `short_count`     INT          NOT NULL DEFAULT 0 COMMENT '简答题数量',
    `code_count`      INT          NOT NULL DEFAULT 0 COMMENT '代码题数量',
    `calc_count`      INT          NOT NULL DEFAULT 0 COMMENT '计算大题数量',
    `total_score`     INT          NOT NULL DEFAULT 100 COMMENT '试卷总分',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常',
    `create_user_id`  BIGINT       NOT NULL COMMENT '创建人ID',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_no` (`paper_no`),
    KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 3.5 试卷答案表（确认题目时写入，交卷评分时使用）
DROP TABLE IF EXISTS `test_paper_answer`;
CREATE TABLE `test_paper_answer` (
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `test_paper_id`   BIGINT       NOT NULL COMMENT '试卷ID',
    `question_id`     BIGINT       NOT NULL COMMENT '题目ID',
    `question_type`   VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE/MULTI/JUDGE/FILL/SHORT/CODE/CALCULATION',
    `correct_answer`  TEXT         NOT NULL COMMENT '正确答案（JSON 数组，如 ["B"]、["A","C"]、["正确"]）',
    `score`           INT          NOT NULL DEFAULT 5 COMMENT '分值',
    `sort`            INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_test_paper_id` (`test_paper_id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷答案表';

-- 3.6 答题记录/成绩表
DROP TABLE IF EXISTS `quiz_result`;
CREATE TABLE `quiz_result` (
    `id`                BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `test_paper_id`     BIGINT       NOT NULL COMMENT '关联试卷ID',
    `document_id`       BIGINT       NOT NULL COMMENT '关联文档ID',
    `user_id`           BIGINT       NOT NULL COMMENT '答题用户ID',
    `total_score`       INT          NOT NULL DEFAULT 0 COMMENT '试卷总分',
    `user_score`        INT          NOT NULL DEFAULT 0 COMMENT '用户得分',
    `correct_count`     INT          NOT NULL DEFAULT 0 COMMENT '答对题数',
    `total_count`       INT          NOT NULL DEFAULT 0 COMMENT '题目总数',
    `answers_json`      JSON         DEFAULT NULL COMMENT '用户提交的答案（JSON）',
    `start_time`        DATETIME     NOT NULL COMMENT '开始答题时间',
    `submit_time`       DATETIME     DEFAULT NULL COMMENT '提交时间',
    `duration_seconds`  INT          DEFAULT NULL COMMENT '答题耗时（秒）',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_test_paper_id` (`test_paper_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录/成绩表';

-- ============================================================================
-- 4. 题目生成配置表（配置模板系统）
-- ============================================================================

-- 4.1 题目生成配置主表
DROP TABLE IF EXISTS `question_gen_config`;
CREATE TABLE `question_gen_config` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `name`          VARCHAR(100) NOT NULL COMMENT '配置名称（如"通用标准配置"、"政考配置"）',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
    `total_count`   INT          NOT NULL DEFAULT 0 COMMENT '总题数',
    `need_passage`  TINYINT      NOT NULL DEFAULT 0 COMMENT '选择题是否强制附带阅读材料：0-否 1-是',
    `direction`     TEXT         DEFAULT NULL COMMENT '出题方向/额外提示词',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `is_active`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目生成配置主表';

-- 4.2 题目生成配置题型明细表
DROP TABLE IF EXISTS `question_gen_config_type`;
CREATE TABLE `question_gen_config_type` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `config_id`     BIGINT       NOT NULL COMMENT '所属配置ID',
    `question_type` VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE/MULTI/JUDGE/FILL/SHORT/CODE/CALCULATION',
    `count`         INT          NOT NULL DEFAULT 0 COMMENT '该题型生成数量',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目生成配置题型明细表';

-- ============================================================================
-- 5. 默认数据初始化
-- ============================================================================

-- 5.1 默认管理员用户
-- 密码：admin123（BCrypt 加密）
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@example.com', '13800000000', 1);

-- 5.2 默认超级管理员角色
INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '超级管理员', 'ADMIN', '拥有系统所有权限');

-- 5.3 用户-角色关联
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 5.4 菜单与权限数据
-- 一级目录
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `component`, `icon`, `type`, `permission`, `sort`, `visible`) VALUES
(1,  0, '主页',     '/home',          'home/HomeView',            'HomeFilled', 1, NULL,           0, 1),
(10, 0, '系统管理',  NULL,             NULL,                       'Setting',    0, NULL,           1, 1),
(20, 0, '应用',      NULL,             NULL,                       'Document',   0, NULL,           2, 1);

-- 系统管理子菜单
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `component`, `icon`, `type`, `permission`, `sort`, `visible`) VALUES
(11, 10, '用户管理',         '/system/user',            'system/UserManageView',           'User',          1, 'user:list',        1, 1),
(12, 10, '权限管理',         '/system/role',            'system/RoleManageView',           'Lock',          1, 'role:list',        2, 1),
(13, 10, 'AI配置',           '/system/ai',              'system/AIConfigView',             'Cpu',           1, 'ai:list',          3, 1),
(14, 10, '题目生成配置',     '/system/question-config',  'system/QuestionGenConfigView',    'Edit',          1, 'doc:genQuestion',  4, 1);

-- 应用子菜单
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `component`, `icon`, `type`, `permission`, `sort`, `visible`) VALUES
(21, 20, '文档列表', '/app/document',      'app/DocumentListView',      'Files',          1, 'doc:list',      1, 1),
(22, 20, '试题列表', '/app/test-paper',    'app/TestPaperListView',     'Tickets',        1, 'paper:list',    2, 1),
(23, 20, '答题记录', '/app/quiz-results',  'app/QuizResultListView',    'DataAnalysis',   1, 'quiz:take',     3, 1);

-- 按钮权限 - 用户管理
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `type`, `permission`, `sort`, `visible`) VALUES
(111, 11, '新增用户', 2, 'user:add',    1, 1),
(112, 11, '修改用户', 2, 'user:update', 2, 1),
(113, 11, '删除用户', 2, 'user:delete', 3, 1);

-- 按钮权限 - 角色管理
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `type`, `permission`, `sort`, `visible`) VALUES
(121, 12, '新增角色', 2, 'role:add',        1, 1),
(122, 12, '修改角色', 2, 'role:update',     2, 1),
(123, 12, '删除角色', 2, 'role:delete',     3, 1),
(124, 12, '分配权限', 2, 'role:assignPerm', 4, 1);

-- 按钮权限 - AI配置
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `type`, `permission`, `sort`, `visible`) VALUES
(141, 13, '新增配置', 2, 'ai:add',    1, 1),
(142, 13, '修改配置', 2, 'ai:update', 2, 1),
(143, 13, '删除配置', 2, 'ai:delete', 3, 1),
(144, 13, '测试连接', 2, 'ai:test',   4, 1);

-- 按钮权限 - 文档列表
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `type`, `permission`, `sort`, `visible`) VALUES
(211, 21, '上传文档', 2, 'doc:upload',       1, 1),
(212, 21, '修改文档', 2, 'doc:update',       2, 1),
(213, 21, '删除文档', 2, 'doc:delete',       3, 1),
(214, 21, '生成题目', 2, 'doc:genQuestion',  4, 1),
(215, 21, '确认题目', 2, 'question:confirm', 5, 1);

-- 按钮权限 - 试题列表
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `type`, `permission`, `sort`, `visible`) VALUES
(221, 22, '查看试题', 2, 'paper:list',  1, 1),
(222, 22, '删除试题', 2, 'paper:delete',2, 1),
(223, 22, '参加测试', 2, 'quiz:take',   3, 1);

-- 按钮权限 - 答题记录
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `type`, `permission`, `sort`, `visible`) VALUES
(231, 23, '查看记录', 2, 'quiz:take',   1, 1),
(232, 23, '删除记录', 2, 'quiz:delete', 2, 1);

-- 5.5 给超级管理员角色分配所有菜单/权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;

-- ============================================================================
-- 6. 预置题目生成配置模板
-- ============================================================================

INSERT IGNORE INTO `question_gen_config` (`id`, `name`, `description`, `total_count`, `need_passage`, `direction`, `sort`, `is_active`) VALUES
(1, '通用标准配置',    '包含单选、多选、判断、简答的均衡配置',     25, 0, '覆盖文档核心知识点，难度均匀分布',                              1, 1),
(2, '政考配置',        '不含多选题和判断题，选择题需附带阅读材料', 20, 1, '选择题需附带约200字阅读材料，侧重政策理解与分析能力',          2, 1),
(3, '编程语言试卷配置', '包含代码题和计算大题',                   30, 0, '侧重编程语言核心概念、算法思维和实际编码能力',                  3, 1);

INSERT IGNORE INTO `question_gen_config_type` (`id`, `config_id`, `question_type`, `count`, `sort`) VALUES
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

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 初始化完成
-- 默认管理员：admin / admin123
-- ============================================================================
