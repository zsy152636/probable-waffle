-- AI 智能答题系统 - 建表脚本

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`      VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    `real_name`     VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `role_name`     VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `role_code`     VARCHAR(50)  NOT NULL COMMENT '角色编码',
    `description`   VARCHAR(200) DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_name`     VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    `path`          VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    `component`     VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
    `icon`          VARCHAR(50)  DEFAULT NULL COMMENT '图标',
    `type`          TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：0-目录 1-菜单 2-按钮',
    `permission`    VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `role_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联';

CREATE TABLE IF NOT EXISTS `ai_config` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `provider_name` VARCHAR(100) NOT NULL COMMENT '服务厂商名称',
    `api_url`       VARCHAR(500) NOT NULL COMMENT 'API 访问地址',
    `api_token`     VARCHAR(500) NOT NULL COMMENT 'API Token / Key',
    `model_name`    VARCHAR(100) NOT NULL COMMENT '模型名称',
    `max_tokens`    INT          NOT NULL DEFAULT 4096 COMMENT '最大输出 Token',
    `temperature`   DECIMAL(3,2) NOT NULL DEFAULT 0.7 COMMENT '温度参数',
    `is_active`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 服务配置';

CREATE TABLE IF NOT EXISTS `doc_document` (
    `id`                 BIGINT       NOT NULL COMMENT '主键',
    `doc_no`             VARCHAR(32)  NOT NULL COMMENT '文档编号（WD+雪花ID）',
    `doc_name`           VARCHAR(200) NOT NULL COMMENT '文档名称',
    `doc_type`           VARCHAR(20)  NOT NULL COMMENT '文档类型：TXT/WORD/PDF',
    `file_path`          VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size`          BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小',
    `content_text`       LONGTEXT     DEFAULT NULL COMMENT '解析后的纯文本',
    `question_count`     INT          NOT NULL DEFAULT 0 COMMENT '生成的题目数量',
    `question_gen_time`  DATETIME     DEFAULT NULL COMMENT '最近一次题目生成时间',
    `upload_user_id`     BIGINT       NOT NULL COMMENT '上传用户ID',
    `upload_user_name`   VARCHAR(50)  NOT NULL COMMENT '上传用户名',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_no` (`doc_no`),
    KEY `idx_upload_user` (`upload_user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

CREATE TABLE IF NOT EXISTS `doc_question` (
    `id`              BIGINT       NOT NULL COMMENT '主键',
    `document_id`     BIGINT       NOT NULL COMMENT '所属文档ID',
    `question_type`   VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE/MULTI/JUDGE/FILL/SHORT',
    `question_title`  TEXT         NOT NULL COMMENT '题干',
    `analysis`        TEXT         DEFAULT NULL COMMENT '答案解析',
    `difficulty`      TINYINT      NOT NULL DEFAULT 1 COMMENT '难度：1-简单 2-中等 3-困难',
    `score`           INT          NOT NULL DEFAULT 5 COMMENT '分值',
    `sort`            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待确认 1-已确认',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

CREATE TABLE IF NOT EXISTS `doc_question_option` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `question_id`   BIGINT       NOT NULL COMMENT '题目ID',
    `option_label`  VARCHAR(10)  NOT NULL COMMENT '选项标签（A/B/C/D）',
    `option_text`   TEXT         NOT NULL COMMENT '选项内容',
    `is_correct`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否正确答案',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

CREATE TABLE IF NOT EXISTS `test_paper` (
    `id`              BIGINT       NOT NULL COMMENT '主键',
    `paper_no`        VARCHAR(32)  NOT NULL COMMENT '试题编号（TP+雪花ID）',
    `paper_name`      VARCHAR(200) NOT NULL COMMENT '试题名称',
    `document_id`     BIGINT       NOT NULL COMMENT '来源文档ID',
    `doc_no`          VARCHAR(32)  NOT NULL COMMENT '来源文档编号',
    `total_count`     INT          NOT NULL DEFAULT 0 COMMENT '题目总数',
    `single_count`    INT          NOT NULL DEFAULT 0 COMMENT '单选题数量',
    `multi_count`     INT          NOT NULL DEFAULT 0 COMMENT '多选题数量',
    `judge_count`     INT          NOT NULL DEFAULT 0 COMMENT '判断题数量',
    `fill_count`      INT          NOT NULL DEFAULT 0 COMMENT '填空题数量',
    `total_score`     INT          NOT NULL DEFAULT 100 COMMENT '试卷总分',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
    `create_user_id`  BIGINT       NOT NULL COMMENT '创建人ID',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_no` (`paper_no`),
    KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题表';

CREATE TABLE IF NOT EXISTS `test_paper_answer` (
    `id`              BIGINT       NOT NULL COMMENT '主键',
    `test_paper_id`   BIGINT       NOT NULL COMMENT '试卷ID',
    `question_id`     BIGINT       NOT NULL COMMENT '题目ID',
    `question_type`   VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE/MULTI/JUDGE/FILL/SHORT',
    `correct_answer`  TEXT         NOT NULL COMMENT '正确答案（选项label数组JSON，如["B"]、["A","C"]、["正确"]）',
    `score`           INT          NOT NULL DEFAULT 5 COMMENT '分值',
    `sort`            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_test_paper_id` (`test_paper_id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷答案表（确认题目时写入，交卷评分时读取）';

CREATE TABLE IF NOT EXISTS `quiz_result` (
    `id`                BIGINT       NOT NULL COMMENT '主键',
    `test_paper_id`     BIGINT       NOT NULL COMMENT '关联试题ID',
    `document_id`       BIGINT       NOT NULL COMMENT '关联文档ID',
    `user_id`           BIGINT       NOT NULL COMMENT '答题用户ID',
    `total_score`       INT          NOT NULL DEFAULT 0 COMMENT '总分',
    `user_score`        INT          NOT NULL DEFAULT 0 COMMENT '用户得分',
    `correct_count`     INT          NOT NULL DEFAULT 0 COMMENT '答对题数',
    `total_count`       INT          NOT NULL DEFAULT 0 COMMENT '总题数',
    `answers_json`      JSON         DEFAULT NULL COMMENT '用户提交的答案',
    `start_time`        DATETIME     NOT NULL COMMENT '开始答题时间',
    `submit_time`       DATETIME     DEFAULT NULL COMMENT '提交时间',
    `duration_seconds`  INT          DEFAULT NULL COMMENT '答题耗时（秒）',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录/成绩表';
