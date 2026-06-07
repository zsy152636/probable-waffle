# 题目生成配置管理系统 — 设计文档

**日期:** 2026-06-07  
**状态:** 已确认

---

## 1. 概述

在现有 AI 智能答题系统中新增"题目生成配置"功能，允许管理员预定义多种题型组合方案（如政考配置、编程试卷配置等），用户在生成题目时可直接选择已保存的配置，无需每次手动设置题型参数。

同时扩展题型体系，新增代码题（CODE）和计算大题（CALCULATION），支持 7 种题型自定义组合。

---

## 2. 题型体系

### 2.1 全部题型（7种）

| 编码 | 名称 | 类型 | 选项结构 |
|------|------|------|------|
| SINGLE | 选择题 | 客观题 | A/B/C/D 四选项，标记正确答案 |
| MULTI | 多选题 | 客观题 | A/B/C/D 四选项，标记多个正确答案 |
| JUDGE | 判断题 | 客观题 | 正确/错误 |
| FILL | 填空题 | 客观题 | `options[0].text` = 正确答案 |
| SHORT | 简答题 | 主观题 | `options[0].text` = 参考答案 |
| **CODE** | **代码题** | **主观大题** | `options[0].text` = 参考代码 |
| **CALCULATION** | **计算大题** | **主观大题** | `options[0].text` = 所需公式 |

### 2.2 题型分类

- **有选项型**（SINGLE/MULTI/JUDGE）：前端展示 ABC 选项列表
- **无选项型**（FILL/SHORT/CODE/CALCULATION）：前端展示 `options[0].text` 作为参考答案/代码/公式

---

## 3. 数据库设计

### 3.1 新表 `question_gen_config`

```sql
CREATE TABLE question_gen_config (
    id            BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    name          VARCHAR(100) NOT NULL COMMENT '配置名称',
    description   VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
    type_config   JSON         NOT NULL COMMENT '题型组合：{"types":[{"type":"SINGLE","count":10,"label":"选择题"},...]}',
    total_count   INT          NOT NULL DEFAULT 0 COMMENT '总题数（type_config 中 count 求和）',
    need_passage  TINYINT      NOT NULL DEFAULT 0 COMMENT '选择题是否强制附带阅读材料 0-否 1-是',
    direction     TEXT         DEFAULT NULL COMMENT '出题方向/额外提示词',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    is_active     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目生成配置表';
```

### 3.2 `type_config` JSON 格式

```json
{
  "types": [
    { "type": "SINGLE",       "count": 10, "label": "选择题" },
    { "type": "MULTI",        "count": 5,  "label": "多选题" },
    { "type": "JUDGE",        "count": 5,  "label": "判断题" },
    { "type": "CODE",         "count": 3,  "label": "代码题" },
    { "type": "CALCULATION",  "count": 2,  "label": "计算大题" }
  ]
}
```

- `count` = 用户填写的该题型数量
- `total_count` = 所有 `count` 之和（冗余字段，方便查询和排序）
- 未启用的题型不出现在 `types` 数组中

### 3.3 现有表变更

| 表 | 变更内容 |
|---|---------|
| `doc_question` | `question_type` 字段值域扩展为 `SINGLE/MULTI/JUDGE/FILL/SHORT/CODE/CALCULATION` |
| `test_paper` | 新增 `code_count INT DEFAULT 0`、`calc_count INT DEFAULT 0` |

---

## 4. 后端 API 设计

### 4.1 配置管理 CRUD

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/question-gen-configs` | 登录即可 | 配置列表（分页） |
| GET | `/api/question-gen-configs/{id}` | 登录即可 | 配置详情 |
| POST | `/api/question-gen-configs` | 管理员 | 新增配置 |
| PUT | `/api/question-gen-configs/{id}` | 管理员 | 编辑配置 |
| DELETE | `/api/question-gen-configs/{id}` | 管理员 | 删除配置 |

### 4.2 生成接口改造

`POST /api/documents/{docId}/generate-questions`

请求体新增可选字段：

```json
{
  "configId": 123456,           // 【新增】配置ID，传了则使用配置
  "totalCount": 20,             // 兼容旧逻辑
  "includeFill": false,         // 兼容旧逻辑
  "typeRatios": {               // 兼容旧逻辑
    "single": 50, "multi": 30, "judge": 20
  },
  "direction": "侧重知识点辨析"  // 会与配置中的 direction 合并
}
```

**后端处理逻辑：**
- 若 `configId` 存在 → 读配置，用 `type_config` 中的 `types[].count` 构建 Prompt
- 若 `configId` 不存在 → 走旧逻辑（百分比计算），向下兼容
- `direction` 字段：请求体中的 `direction` 与配置中的 `direction` 合并拼接
- `need_passage=1` 时，在 Prompt 中强制要求所有 SINGLE 题型包含 reading passage

### 4.3 AI Prompt 扩展

Prompt 模板新增代码题和计算大题规范：

```
- 代码题(CODE)：questionTitle 为题目描述，passage 可选提供代码上下文，
  options[0] 的 text 为参考代码实现，label 固定为"参考代码"
- 计算大题(CALCULATION)：questionTitle 为题目描述，passage 可选提供背景，
  options[0] 的 text 为所需公式（不提供完整解题过程），label 固定为"所需公式"
```

---

## 5. 前端设计

### 5.1 配置管理页面

- **路由**: `/admin/question-config`
- **入口**: 管理后台左侧菜单新增"题目生成配置"
- **列表页**:
  - 表格列：名称、题型标签（彩色 Tag）、总题数、阅读材料标识、启用状态、操作
  - 操作：编辑、删除、启用/禁用
  - 顶部新增按钮
- **新增/编辑弹窗**（复用或独立组件）:
  - 配置名称输入框
  - 描述输入框
  - 7 种题型 checkbox + 数量输入（勾选后启用数量输入，取消勾选归零禁用）
  - 总题数自动求和显示
  - 选择题强制阅读材料开关
  - 出题方向 textarea

### 5.2 生成弹窗改造

在 `QuestionConfirmView.vue`「重新生成」弹窗中：
- 顶部新增 `el-select`："选择配置模板"
- 选中配置后自动填充：题型组合、数量、出题方向
- 用户可在自动填充后手动微调

### 5.3 QuestionCard 展示扩展

- 代码题：题目描述正常展示 + 参考代码使用 `<pre><code>` 代码块（后续可加语法高亮）
- 计算大题：题目描述正常展示 + 公式区使用等宽字体展示

### 5.4 QuestionForm 编辑扩展

- 代码题：题目描述 textarea + 参考代码 `<el-input type="textarea">` 大文本区
- 计算大题：题目描述 textarea + 公式 `<el-input type="textarea">`

### 5.5 TypeScript 类型扩展

```typescript
export type QuestionType = 'SINGLE' | 'MULTI' | 'JUDGE' | 'FILL' | 'SHORT' | 'CODE' | 'CALCULATION'

export interface GenConfigTypeItem {
  type: QuestionType
  count: number
  label: string
}

export interface GenConfig {
  id?: string
  name: string
  description: string
  typeConfig: GenConfigTypeItem[]
  totalCount: number
  needPassage: number
  direction: string
  sort: number
  isActive: number
}
```

---

## 6. 实现顺序

| 阶段 | 内容 | 涉及文件 |
|------|------|------|
| 1 | 数据库：新表建表 + 现有表字段扩展 | `.sql` 文件 |
| 2 | 后端：Entity + Mapper + Service + Controller | `QuestionGenConfig.java` 等 |
| 3 | 后端：改造生成接口 + AI Prompt | `QuestionController.java`、`AIService.java` |
| 4 | 前端：配置管理页面 CRUD | 新页面组件 |
| 5 | 前端：生成弹窗改造 | `QuestionConfirmView.vue` |
| 6 | 前端：QuestionCard/Form 扩展 | `QuestionCard.vue`、`QuestionForm.vue` |
| 7 | 联调测试 | — |

---

## 7. 兼容性说明

- 旧的 `totalCount + includeFill + typeRatios` 参数**完全保留**，不传 `configId` 时行为不变
- 现有 5 种题型的前端展示和编辑逻辑不受影响
- `test_paper` 新字段有默认值 0，旧数据自动兼容
