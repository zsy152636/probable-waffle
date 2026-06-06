-- 初始化数据

-- 默认管理员用户（密码: admin123，BCrypt 加密）
INSERT IGNORE INTO sys_user (id, username, password, real_name, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 1);

-- 默认角色
INSERT IGNORE INTO sys_role (id, role_name, role_code, description) VALUES
(1, '超级管理员', 'ADMIN', '拥有所有权限');

-- 用户角色关联
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 菜单数据
-- 一级目录
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(1,  0, '主页',     '/home',          'home/HomeView',            'HomeFilled', 1, NULL,           0, 1),
(10, 0, '系统管理',  NULL,             NULL,                       'Setting',    0, NULL,           1, 1),
(20, 0, '应用',      NULL,             NULL,                       'Document',   0, NULL,           2, 1);

-- 系统管理子菜单
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(11, 10, '用户管理', '/system/user',  'system/UserManageView',   'User',      1, 'user:list',     1, 1),
(12, 10, '权限管理', '/system/role',  'system/RoleManageView',   'Lock',      1, 'role:list',     2, 1),
(13, 10, 'AI配置',   '/system/ai',    'system/AIConfigView',      'Cpu',       1, 'ai:list',       3, 1);

-- 应用子菜单
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(21, 20, '文档列表', '/app/document',      'app/DocumentListView',      'Files',       1, 'doc:list',      1, 1),
(22, 20, '试题列表', '/app/test-paper',    'app/TestPaperListView',     'Tickets',     1, 'paper:list',    2, 1),
(23, 20, '答题记录', '/app/quiz-results',  'app/QuizResultListView',    'DataAnalysis',1, 'quiz:take',     3, 1);

-- 按钮权限 - 用户管理
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(111, 11, '新增用户', NULL, NULL, NULL, 2, 'user:add',       1, 1),
(112, 11, '修改用户', NULL, NULL, NULL, 2, 'user:update',    2, 1),
(113, 11, '删除用户', NULL, NULL, NULL, 2, 'user:delete',    3, 1);

-- 按钮权限 - 角色管理
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(121, 12, '新增角色', NULL, NULL, NULL, 2, 'role:add',        1, 1),
(122, 12, '修改角色', NULL, NULL, NULL, 2, 'role:update',     2, 1),
(123, 12, '删除角色', NULL, NULL, NULL, 2, 'role:delete',     3, 1),
(124, 12, '分配权限', NULL, NULL, NULL, 2, 'role:assignPerm', 4, 1);

-- 按钮权限 - AI配置
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(141, 13, '新增配置', NULL, NULL, NULL, 2, 'ai:add',    1, 1),
(142, 13, '修改配置', NULL, NULL, NULL, 2, 'ai:update', 2, 1),
(143, 13, '删除配置', NULL, NULL, NULL, 2, 'ai:delete', 3, 1),
(144, 13, '测试连接', NULL, NULL, NULL, 2, 'ai:test',   4, 1);

-- 按钮权限 - 文档列表
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(211, 21, '上传文档', NULL, NULL, NULL, 2, 'doc:upload',       1, 1),
(212, 21, '修改文档', NULL, NULL, NULL, 2, 'doc:update',       2, 1),
(213, 21, '删除文档', NULL, NULL, NULL, 2, 'doc:delete',       3, 1),
(214, 21, '生成题目', NULL, NULL, NULL, 2, 'doc:genQuestion',  4, 1),
(215, 21, '确认题目', NULL, NULL, NULL, 2, 'question:confirm', 5, 1);

-- 按钮权限 - 试题列表
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(221, 22, '查看试题', NULL, NULL, NULL, 2, 'paper:list',  1, 1),
(222, 22, '删除试题', NULL, NULL, NULL, 2, 'paper:delete',2, 1),
(223, 22, '参加测试', NULL, NULL, NULL, 2, 'quiz:take',   3, 1);

-- 按钮权限 - 答题记录
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, icon, type, permission, sort, `visible`) VALUES
(231, 23, '查看记录', NULL, NULL, NULL, 2, 'quiz:take',   1, 1),
(232, 23, '删除记录', NULL, NULL, NULL, 2, 'quiz:delete', 2, 1);

-- 给 ADMIN 角色分配所有菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;
