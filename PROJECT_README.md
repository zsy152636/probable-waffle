# AI 智能答题系统 — 项目说明文档

## 一、项目简介

**AI 智能答题系统**是一套基于大语言模型的在线考试与答题生成平台。用户上传学习文档（TXT / Word / PDF / Markdown），系统调用可配置的 AI 服务对文档内容进行语义分析，自动提取知识点并生成结构化题库。生成的题目经用户确认后发布为正式试卷，供考生在线作答，系统自动评分并生成统计分析。

### 核心流程

```
上传文档 → AI 自动出题 → 人工确认/编辑 → 生成试卷 → 在线答题 → 自动评分 → 数据统计
```

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | 3.4+ |
| 构建工具 | Vite | 5.x |
| UI 组件库 | Element Plus | 2.6+ |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP 客户端 | Axios | 1.6+ |
| 图表 | ECharts + vue-echarts | 6.x |
| 后端框架 | Spring Boot | 3.2.5 |
| JDK | OpenJDK | 17 |
| 持久层 | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis (Spring Data Redis) | 7.x |
| 安全框架 | Spring Security + JWT (jjwt 0.12.5) | — |
| 文档解析 | Apache POI (Word) + Apache PDFBox (PDF) | — |
| AI 调用 | OkHttp 动态代理（用户自定义 endpoint） | 4.12 |

## 三、系统架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                       前端 (Vue 3 + TypeScript)                       │
│   登录页 → 主布局(侧边栏+顶栏) → 主页 / 系统管理 / 文档 / 试题 / 答题   │
│                          │                                          │
│                    Axios + JWT Token                                 │
└──────────────────────────┬──────────────────────────────────────────┘
                           │  HTTP (前端 9020 → 后端 9010)
┌──────────────────────────▼──────────────────────────────────────────┐
│                     ai-quiz-admin (启动模块 9010)                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  @ComponentScan 扫描 sys / ai / dome 全部组件                  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ ai-quiz-sys  │  │ ai-quiz-ai   │  │   ai-quiz-dome           │  │
│  │ (系统基础)    │  │ (AI大模型)    │  │   (领域业务)              │  │
│  │              │  │              │  │                          │  │
│  │ 认证/JWT     │  │ AI配置管理    │  │ 文档上传/解析(TXT/Word/PDF)│  │
│  │ 用户/角色权限 │  │ OkHttp动态客户端│  AI 题目生成               │  │
│  │ 公共工具/异常  │  │ Prompt引擎    │  │ 题目确认/编辑/删除        │  │
│  │ Redis/MP配置  │  │ 连通性测试    │  │ 试题发布/列表             │  │
│  └──────┬───────┘  └──────┬───────┘  │ 在线答题/自动评分         │  │
│         │                 │          │ 答题结果/历史记录          │  │
│         │                 │          │ 数据统计/图表分析          │  │
│         │                 │          └──────────────────────────┘  │
│         └─────────────────┴───────────────────────┘                 │
│                           │                                         │
│                ┌──────────┴──────────┐                             │
│                │                     │                             │
│           ┌────▼────┐          ┌─────▼─────┐                       │
│           │  MySQL   │          │   Redis    │                       │
│           └─────────┘          └───────────┘                       │
│                           │                                         │
│                ┌──────────▼──────────┐                             │
│                │  第三方 AI API       │                             │
│                │ (OpenAI 兼容接口)    │                             │
│                └─────────────────────┘                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 后端模块结构

项目采用 Maven 多模块架构，按职责拆分为四个模块：

```
ai-quiz/                                  # 父工程
├── pom.xml                               # 统一依赖版本管理
├── ai-quiz-sys/                          # 【系统基础模块】
│   ├── config/                           #   Security、Redis、MyBatis-Plus、WebMvc 配置
│   ├── security/                         #   JWT Token 提供者、鉴权过滤器、UserDetails
│   ├── controller/                       #   AuthController（登录/登出）、UserController、RoleController
│   ├── service/                          #   AuthService、UserService、RoleService
│   ├── entity/                           #   User、Role、Menu
│   ├── common/                           #   Result 统一返回体、PageResult、BusinessException
│   └── util/                             #   SnowflakeIdGenerator 雪花 ID 生成器
│
├── ai-quiz-ai/                           # 【AI 大模型接口模块】
│   ├── config/AIClientFactory.java       #   OkHttp 动态客户端工厂
│   ├── controller/AIConfigController.java #   AI 配置 CRUD + 连通性测试
│   ├── service/AIService.java            #   动态 Prompt 构建 + AI 调用 + JSON 解析
│   ├── service/AIConfigService.java      #   配置管理服务
│   └── entity/AIConfig.java              #   配置实体
│
├── ai-quiz-dome/                         # 【领域业务模块】
│   ├── controller/                       #   Document/Question/TestPaper/Quiz/StatsController
│   ├── service/                          #   文档、题目、试卷、答题、统计服务
│   ├── entity/                           #   Document/Question/QuestionOption/TestPaper/QuizResult
│   ├── mapper/                           #   MyBatis-Plus Mapper 接口
│   └── util/DocumentParser.java          #   TXT / Word / PDF 文档解析器
│
└── ai-quiz-admin/                        # 【启动 & 装配模块】
    ├── QuizApplication.java              #   @SpringBootApplication 主启动类
    └── resources/                        #   application.yml + application-dev.yml + SQL 脚本
```

### 3.3 前端项目结构

```
ai-quiz-web/
├── index.html
├── package.json / pnpm-lock.yaml
├── vite.config.ts                       # Vite 配置（代理 /api → 9010）
├── tsconfig.json
├── src/
│   ├── main.ts                          # Vue 应用入口
│   ├── App.vue                          # 根组件
│   ├── api/                             # API 接口层
│   │   ├── request.ts                   #   Axios 实例（拦截器注入 JWT Token）
│   │   ├── auth.ts                      #   登录/登出/用户信息
│   │   ├── user.ts / role.ts            #   用户/角色管理
│   │   ├── aiConfig.ts                  #   AI 配置 CRUD
│   │   ├── document.ts                  #   文档上传/列表
│   │   ├── question.ts                  #   题目生成/确认/编辑
│   │   ├── questionGenConfig.ts         #   题目生成配置模板
│   │   ├── quiz.ts                      #   在线答题/提交/结果
│   │   ├── testPaper.ts                 #   试题列表
│   │   └── stats.ts                     #   数据统计
│   ├── router/index.ts                  #   路由 + 导航守卫（权限控制）
│   ├── stores/
│   │   ├── user.ts                      #   用户信息 + Token + 权限
│   │   └── app.ts                       #   全局状态（侧边栏折叠等）
│   ├── views/
│   │   ├── login/LoginView.vue          #   登录页
│   │   ├── layout/MainLayout.vue        #   主布局（侧边栏 + 顶栏）
│   │   ├── home/HomeView.vue            #   主页仪表盘（ECharts 统计）
│   │   ├── system/                      #   系统管理
│   │   │   ├── UserManageView.vue       #     用户管理
│   │   │   ├── RoleManageView.vue       #     权限角色管理
│   │   │   ├── AIConfigView.vue         #     AI 服务配置
│   │   │   └── QuestionGenConfigView.vue#     题目生成配置模板
│   │   └── app/                         #   业务功能
│   │       ├── DocumentListView.vue     #     文档列表
│   │       ├── QuestionConfirmView.vue  #     题目确认/编辑
│   │       ├── TestPaperListView.vue    #     试题列表
│   │       ├── QuizTakeView.vue         #     在线答题/成绩查看
│   │       └── QuizResultListView.vue   #     答题记录
│   ├── components/                      #   可复用组件
│   │   ├── QuestionCard.vue             #     题目展示卡（支持所有题型）
│   │   ├── QuestionForm.vue             #     题目编辑表单
│   │   └── UploadDocument.vue           #     文档上传弹窗
│   ├── types/index.ts                   #   TypeScript 类型定义
│   ├── utils/index.ts                   #   工具函数
│   └── styles/theme.css                 #   全局主题样式
```

## 四、数据库设计

### 4.1 数据表总览

| 表名 | 说明 | 所属模块 |
|------|------|---------|
| `sys_user` | 系统用户 | 系统基础 |
| `sys_role` | 角色 | 系统基础 |
| `sys_menu` | 菜单/按钮权限 | 系统基础 |
| `sys_role_menu` | 角色-菜单关联 | 系统基础 |
| `sys_user_role` | 用户-角色关联 | 系统基础 |
| `ai_config` | AI 服务配置 | AI 模块 |
| `doc_document` | 上传文档 | 领域业务 |
| `doc_question` | 题目（含 AI 生成+人工确认） | 领域业务 |
| `doc_question_option` | 题目选项 | 领域业务 |
| `question_gen_config` | 题目生成配置模板 | 领域业务 |
| `question_gen_config_type` | 配置模板题型明细 | 领域业务 |
| `test_paper` | 已发布的试卷 | 领域业务 |
| `test_paper_answer` | 试题答案记录 | 领域业务 |
| `quiz_result` | 答题结果 | 领域业务 |

### 4.2 核心表关系

```
sys_user ──1:N──→ doc_document ──1:N──→ doc_question ──1:N──→ doc_question_option
  │                                          │
  │                                          │ (status: 0=待确认, 1=已确认)
  │                                          ↓
  │                                     test_paper
  │                                          │
  └───────────1:N──→ quiz_result ←────N:1───┘
```

## 五、核心功能列表

### 5.1 系统管理

| 功能 | 描述 |
|------|------|
| 用户管理 | 用户的增删改查、启用/禁用、分配角色、搜索分页 |
| 角色/权限管理 | 角色的增删改查、菜单/按钮级权限绑定 |
| AI 配置管理 | 配置 AI 服务商（API 地址、Token、模型、参数）、启用/禁用、连通性测试 |
| 题目生成配置模板 | 预设题目生成参数（题型组合、各题型数量、出题方向），一键应用到文档出题 |

### 5.2 文档与出题

| 功能 | 描述 |
|------|------|
| 文档上传 | 支持 TXT / DOCX / PDF / Markdown 格式，单文件 ≤ 20MB |
| 文档管理 | 文档列表展示、类型筛选、关键字搜索、编辑名称、删除 |
| AI 题目生成 | 基于文档内容调用 AI 自动生成 7 种题型（单选/多选/判断/填空/简答/代码/计算） |
| 出题参数配置 | 选择配置模板或自定义题型数量、强制阅读材料、出题方向提示 |

### 5.3 题目确认

| 功能 | 描述 |
|------|------|
| 题目列表 | 按题型分组展示 AI 生成的题目，支持状态筛选、题型筛选、分页 |
| 单题操作 | 查看/编辑题目内容、选项、答案、解析、分值、难度 |
| 批量操作 | 批量确认、批量删除、全选/取消全选 |
| 重新生成 | 清空当前题目并重新调用 AI 生成 |
| 确认生成试卷 | 确认题目后自动生成正式试卷 |

### 5.4 在线答题

| 功能 | 描述 |
|------|------|
| 试题列表 | 所有已生成的试卷，按题型统计数量，支持搜索 |
| 在线答题 | 按题型分组展示试题，支持 7 种题型作答 |
| 自动计时 | 计时器 + 剩余时间不足 5 分钟报警 |
| 提交评分 | 自动批改客观题（单选/多选/判断/填空），主观题记录答案 |
| 成绩展示 | 得分、答对数量、详细答题情况、题目解析 |

### 5.5 数据统计

| 功能 | 描述 |
|------|------|
| 概览仪表盘 | 文档总数、试题总数、答题次数、正确率（含动画计数） |
| 图表分析 | 答题正确率饼图、分数段分布柱状图、近期得分趋势折线图 |
| 答题记录 | 历史答题列表，支持查看详情和删除 |

## 六、接口 API 概览

### 6.1 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/info` | 获取当前用户信息（含权限、菜单） |

### 6.2 系统管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/api/users/**` | 用户 CRUD |
| GET/POST/PUT/DELETE | `/api/roles/**` | 角色 CRUD + 分配菜单权限 |
| GET/POST/PUT/DELETE | `/api/ai-configs/**` | AI 配置 CRUD + 启用/禁用/测试连接 |
| GET/POST/PUT/DELETE | `/api/question-gen-configs/**` | 生成配置模板 CRUD |

### 6.3 业务接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/documents` | 文档列表（分页+搜索+类型筛选） |
| POST | `/api/documents/upload` | 文档上传 |
| PUT | `/api/documents/{id}` | 修改文档名称 |
| DELETE | `/api/documents/{id}` | 删除文档 |
| POST | `/api/documents/{id}/generate-questions` | AI 生成题目 |
| DELETE | `/api/documents/{id}/questions` | 取消生成（清空题目） |
| GET | `/api/documents/{id}/questions` | 获取题目列表 |
| PUT | `/api/questions/{id}` | 更新单题 |
| DELETE | `/api/questions/{id}` | 删除单题 |
| POST | `/api/questions/batch-confirm` | 批量确认题目并生成试卷 |
| POST | `/api/questions/batch-delete` | 批量删除题目 |
| GET | `/api/test-papers` | 试题列表（分页+搜索） |
| DELETE | `/api/test-papers/{id}` | 删除试题 |
| GET | `/api/quiz/{paperId}/paper` | 获取试卷题目 |
| POST | `/api/quiz/{paperId}/submit` | 提交答案 |
| GET | `/api/quiz/results` | 答题记录（分页） |
| GET | `/api/quiz/results/{id}` | 答题结果详情 |
| DELETE | `/api/quiz/results/{id}` | 删除答题记录 |
| GET | `/api/stats/overview` | 统计概览数据 |

## 七、环境要求

| 环境 | 要求 |
|------|------|
| JDK | 17+ |
| Node.js | 18+（推荐 20+） |
| 包管理器 | pnpm（推荐）或 npm |
| MySQL | 8.0+ |
| Redis | 7.x |
| 操作系统 | Windows / Linux / macOS 均可 |

## 八、快速启动

### 8.1 后端启动

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_quiz DEFAULT CHARSET utf8mb4;"

# 2. 修改数据库配置
#    编辑 ai-quiz/ai-quiz-admin/src/main/resources/application-dev.yml
#    修改 datasource 的 username 和 password

# 3. 启动 Redis（默认端口 6379）

# 4. 编译运行
cd ai-quiz
mvn clean install -DskipTests
mvn spring-boot:run -pl ai-quiz-admin

# 启动后自动执行 schema.sql + data.sql 初始化表结构和测试数据
```

### 8.2 前端启动

```bash
cd ai-quiz-web
pnpm install
pnpm dev
# 访问 http://localhost:9020
```

默认开发环境前端运行在 `9020` 端口，通过 Vite proxy 将 `/api` 请求转发到后端 `9010` 端口。

## 九、题型说明

系统支持以下 7 种题型：

| 题型标识 | 中文名称 | 作答方式 | 评分方式 |
|---------|---------|---------|---------|
| `SINGLE` | 单选题 | 单选 Radio | 自动 |
| `MULTI` | 多选题 | 多选 Checkbox | 自动（全对得分） |
| `JUDGE` | 判断题 | 选择"正确"/"错误" | 自动 |
| `FILL` | 填空题 | 文本输入 | 自动（关键字匹配） |
| `SHORT` | 简答题 | 文本域输入 | 记录答案，待批改 |
| `CODE` | 代码题 | 代码文本域（等宽字体） | 记录答案，待批改 |
| `CALCULATION` | 计算大题 | 文本域输入 | 记录答案，待批改 |

## 十、权限说明

系统基于 RBAC（Role-Based Access Control）模型，内置 `ADMIN` 角色拥有全部权限。按钮级权限控制示例：

| 权限标识 | 说明 |
|---------|------|
| `user:list` | 查看用户列表 |
| `user:add` | 新增用户 |
| `role:list` | 查看角色列表 |
| `ai:list` | 查看 AI 配置 |
| `ai:test` | 测试 AI 连接 |
| `doc:list` | 查看文档列表 |
| `doc:upload` | 上传文档 |
| `doc:genQuestion` | AI 生成题目 |
| `question:confirm` | 确认题目 |
| `paper:list` | 查看试题列表 |
| `paper:delete` | 删除试题 |
| `quiz:take` | 在线答题 |
