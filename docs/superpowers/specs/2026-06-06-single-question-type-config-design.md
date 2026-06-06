# 题目生成配置支持单题型选择

Date: 2026-06-06
Status: approved

## 背景

当前题目生成配置中，题型比例滑块约束为 min:10 / max:80，用户无法将某一题型比例设为 0% 或 100%，即无法选择只生成单一题型（如全部选择题）。

## 设计目标

允许用户在题目生成配置中选择只生成一种题型（如全部单选题），也支持任意比例组合。

## 方案概述

保留现有比例滑块布局，将滑块范围从 [10, 80] 改为 [0, 100]，使用户可以将单一题型拖到 100%，其余题型归零。

## 文件变更

### 1. 前端 — `ai-quiz-web/src/views/app/DocumentListView.vue`

**滑块约束修改：**

| 位置 | 当前值 | 改为 |
|------|--------|------|
| 单选题 slider `:min` | `10` | `0` |
| 单选题 slider `:max` | `80` | `100` |
| 多选题 slider `:min` | `10` | `0` |
| 多选题 slider `:max` | `80` | `100` |
| 判断题 slider `:min` | `10` | `0` |
| 判断题 slider `:max` | `80` | `100` |

**互调逻辑：** 经审查，`onSingleChange` / `onMultiChange` / `onJudgeChange` 三个函数已能正确处理边界情况：
- 当某类型拖到 100% 时：`remain=0`，其余类型计算为 0
- 当某类型从 0% 拖起时：`remain=100, ratio=0` 走 else 分支均分余数

**无需修改互调逻辑。**

### 2. 后端 — `ai-quiz-ai/.../service/AIService.java`

**现状：** 后端接收任意比例值，计算各题型实际数量。当某题型数量为 0 时，prompt 中会写明 `"单选题(SINGLE)：0 道"`，AI 可能仍然生成该题型。

**修改：** 在 prompt 中增加约束，跳过数量为 0 的题型：

```java
// 构建题型要求行时，跳过 count=0 的题型
// 在 prompt 末尾追加："【重要】只生成上述列出的题型，数量为0的题型不要生成。"
```

### 不变更的内容

- `GenerateConfig` TypeScript 类型结构不变
- `QuestionController` / `QuestionService` 不变（透传参数）
- 不增加新 UI 组件
- `includeFill` 填空题逻辑不变

## 边界情况

| 场景 | 预期行为 |
|------|---------|
| 全部单选题 (single=100, multi=0, judge=0) | 20道全部为单选 |
| 全部多选题 (single=0, multi=100, judge=0) | 20道全部为多选 |
| 判断题+单选题混合 (single=60, multi=0, judge=40) | 12道单选 + 8道判断 |
| 三个都是 0% | 不会发生（slider 互调确保 sum=100） |
| 包含填空题 + 单题型 | includeFill 从总数中扣减 fillCount |

## AI Prompt 约束

当前 prompt 模板（推断）格式类似：
```
题型要求：
单选题(SINGLE)：X 道
多选题(MULTI)：Y 道
判断题(JUDGE)：Z 道
...
```

修改后，数量为 0 的题型行不输出，并追加：
```
【重要】只生成上述列出的题型，数量为0的题型不要生成。
```
