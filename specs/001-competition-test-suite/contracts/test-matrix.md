# Test Matrix: 单文件竞赛测试套件

**Feature**: 001-competition-test-suite
**Date**: 2026-11-01
**Input**: [spec.md](../spec.md)、[plan.md](../plan.md)、[research.md](../research.md)、[data-model.md](../data-model.md)

## 使用方式

- 每一行是一个“分支族”，不是一个具体测试方法；参数化方法可以把多行合并为一个方法。
- “需覆盖的分支对”描述必须同时出现的真/假两侧；“断言点”描述该行至少需要的行为级断言。
- 预估测试数用于方法预算控制，合计 158，低于 160 上限。
- 所有入口都必须直接实例化，不得启动 Spring 上下文。

## 预算汇总

| 被测组 | 预估测试数 |
|--------|------------|
| `ExpressionAnswerGradingService` | 12 |
| `ExamGradingService` | 58 |
| `LearningProgressService` | 38 |
| `ExpressionExamProgressService` | 10 |
| 表达式引擎 | 34 |
| 端到端 | 6 |
| **合计** | **158** |

## A. ExpressionAnswerGradingService

| SUT | 公开入口或可达入口 | 需覆盖的分支对 | 断言点 | 优先级 | 预估测试数 |
|-----|--------------------|----------------|--------|--------|------------|
| null 提交 | `evaluate(null)` | null / 非 null | NullPointerException | P1 | 1 |
| 空作答 | `evaluate(submission)`，提交表达式为 null、空串、空白 | 空白 / 非空白；null / 非 null | `UNANSWERED`、`answered=false`、`exactMatch=false`、expected/actual/error 为 null、`ANSWER_NOT_PROVIDED` | P1 | 1 |
| 题目配置无效 | 正确表达式不可求值 | 正确表达式可求值 / 不可求值 | `IllegalStateException`，原因链含 `ExpressionEvaluationException` | P1 | 1 |
| 学生表达式非法 | 提交语法错误或求值失败 | 可求值 / 不可求值 | `INVALID_EXPRESSION`、expectedValue 保留、actual/error 为 null、`INVALID_STUDENT_EXPRESSION` | P1 | 1 |
| 误差判定 | 误差 0、等于容差、大于容差 | 误差 = 0 / 0 < 误差 ≤ 容差 / 误差 > 容差 | `EXACT`、`WITHIN_TOLERANCE`、`INCORRECT`；容差等号归入容差内 | P1 | 1 |
| 结果标记传递 | `AnswerResult` 的 answered、exactMatch、numericError、numericTolerance | true / false；null / 非 null | 对应字段精确值；误差与容差原样传递 | P1 | 2 |
| 容差边界 | 容差 0 与非 0；负容差在题目构造时拒绝 | 0 / > 0；合法 / 非法 | `ExpressionQuestion` 构造异常；零容差只接受精确值 | P1 | 1 |
| 状态枚举 | 五种 `ExpressionAnswerStatus` | 五个常量 | 每个状态精确相等，不靠字符串推断 | P1 | 1 |
| 错误码稳定性 | `INVALID_STUDENT_EXPRESSION`、`ANSWER_NOT_PROVIDED` | 命中 / 未命中 | 精确相等；不以异常消息全文作为唯一断言 | P1 | 1 |
| 多答案独立 | 同一题目多次评分 | 独立 / 污染 | 每次评分不改变题目或前一次结果 | P1 | 1 |
| 不可变输入 | 提交对象为 record | 正常 / null 字段 | 不因评分修改原始记录 | P1 | 1 |

## B. ExamGradingService

| SUT | 公开入口或可达入口 | 需覆盖的分支对 | 断言点 | 优先级 | 预估测试数 |
|-----|--------------------|----------------|--------|--------|------------|
| 入参校验 | `grade(submission, policy)` | null / 非 null；空白 / 非空白；时间顺序；负数；越界；重复键；非法枚举；非法计数 | 对应 `NullPointerException` 或 `IllegalArgumentException`；错误在评分前发生 | P1 | 7 |
| 单选题与判断题 | 两种 `QuestionType` 的评分路径 | 正确 / 错误；负分开 / 关；超时且得分 > 0 / 不得分；封顶 / 不封顶 | 满分、0、负分、0.80 折扣、满分裁剪 | P1 | 5 |
| 多选题 | 多选评分路径 | 完全正确 / 非完全正确；无正确选项 / 有；部分给分开 / 关；仅选错误项 / 混合 | 满分、比例分、0、负分、only-incorrect 告警、错误项扣减 | P1 | 7 |
| 填空题 | 填空评分路径 | 精确 / 非精确；rubric > 0 / = 0；部分给分开 / 关；负分开 / 关 | rubric 比例分、0、负分、告警 | P1 | 4 |
| 数值题 | 数值评分路径 | 精确 / 容差内 / 10 倍容差内 / 超限；容差缺失 / 存在；部分给分开 / 关；负分开 / 关 | 满分、比例分、0、`NUMERIC_TOLERANCE_DATA_MISSING`、负分 | P1 | 5 |
| 问答题 | 作文评分路径 | 精确 / 非精确；rubric = 0 / > 0；manualReviewRequired / 否 | rubric 比例分、0、`ESSAY_SCORE_IS_PROVISIONAL` | P1 | 3 |
| 编程题 | 编程评分路径 | 未编译 / 已编译；无测试 / 有测试；全过 / 部分过；部分给分开 / 关；超时 / 不超时；抄袭 / 不抄袭 | `PROGRAM_DID_NOT_COMPILE`、`PROGRAM_HAS_NO_AUTOMATED_TESTS`、命中率分、0.75 超时折扣、抄袭 0 分 | P1 | 6 |
| 未作答 | 未作答评分分支 | 惩罚率 0 / > 0；负分开 / 关；必修 / 非必修 | 0 分或负分、`UNANSWERED_PENALTY`、`MANDATORY_QUESTION_UNANSWERED`、`MANDATORY_QUESTION_NO_CREDIT` | P1 | 2 |
| 提交规则 | 迟到、时长、尝试、覆写、封顶、加分 | 迟到 1 秒 / 59 分 / 60 分 / 61 分；超时 60 / 300 / 301 秒；允许时长 0 / 非 0；attempt == / > 上限；覆写 0 / 100 / 越界；封顶开 / 关 | `LATE_SUBMISSION_PENALTY`、`SUBMISSION_TOO_LATE`、`EXAM_GRACE_PERIOD_USED`、`EXAM_DURATION_MINOR_OVERRUN`、`EXAM_DURATION_EXCEEDED`、`FINAL_ALLOWED_ATTEMPT`、`MAXIMUM_ATTEMPTS_EXCEEDED`、`INSTRUCTOR_OVERRIDE_APPLIED` | P1 | 7 |
| 分区与汇总 | 分区百分比、总分、百分比、等级、通过、手动复核 | 分区缺失 / 存在；低于线 / 达标；满分 0 / 非 0；9 个等级边界；四合一通过条件 | `sectionPercentages`、`SECTION_BELOW_PASS_MARK`、`REQUIRED_SECTION_MISSING`、`OVERALL_SCORE_BELOW_PASS_MARK`、`letterGrade`、`passed`、`manualReview` | P1 | 5 |
| 教务诚信 | 条款、账号、身份、全屏、复制粘贴、IP、监考、抄袭 | 条款接受 / 未接受；停用 / 正常；身份要求 / 不要求 × 已验证 / 未验证；全屏 1 / 2 / 3；复制粘贴 4 / 5；IP 变更 / 未变更；断连 / 正常；抄袭 / 不抄袭 | `EXAM_TERMS_NOT_ACCEPTED`、`ACCOUNT_SUSPENDED`、`IDENTITY_NOT_VERIFIED`、`FULLSCREEN_EXIT_RECORDED`、`MULTIPLE_FULLSCREEN_EXITS`、`EXCESSIVE_FULLSCREEN_EXITS`、`COPY_PASTE_EVENTS_RECORDED`、`EXCESSIVE_COPY_PASTE_EVENTS`、`IP_ADDRESS_CHANGED`、`PROCTOR_CONNECTION_LOST`、`ANSWER_INTEGRITY_VIOLATION` | P1 | 5 |
| 报告一致性 | `GradeReport` 十个字段 | 0 / 非 0；封顶 / 不封顶；有 / 无失败与告警 | 金额两位小数、百分比公式、0 与 100 clamp、failures/warnings 内容稳定 | P1 | 2 |

## C. LearningProgressService

| SUT | 公开入口或可达入口 | 需覆盖的分支对 | 断言点 | 优先级 | 预估测试数 |
|-----|--------------------|----------------|--------|--------|------------|
| VIDEO 课时 | `evaluate(snapshot)` | 完成度 ≥ 90 / 70–89 / < 70；必修 / 非必修 | 完成判定、权重 15、earnedWeight、`VIDEO_NEAR_COMPLETION` | P2 | 2 |
| ARTICLE 课时 | 同上 | 完成度 = 100 / < 100 | 完成判定、权重 10、比例分 | P2 | 2 |
| QUIZ 课时 | 同上 | 已提交 / 未提交；分数 ≥ 线 / < 线 | 完成判定、权重 20、`QUIZ_SCORE_TOO_LOW` | P2 | 2 |
| ASSIGNMENT 课时 | 同上 | 未提交 / 已提交未通过 / 已通过 | 完成判定、权重 25、半分或 0、`ASSIGNMENT_AWAITING_REVISION` | P2 | 2 |
| LIVE_SESSION 课时 | 同上 | requiredMinutes = 0 / > 0；出席 ≥ 80% / < 80% | 完成判定、权重 20、`LIVE_ATTENDANCE_TOO_SHORT` | P2 | 2 |
| PROJECT 课时 | 同上 | 已提交且通过且无抄袭 / 抄袭 / 未提交 | 完成判定、权重 30、`PROJECT_HELD_FOR_INTEGRITY_REVIEW` | P2 | 2 |
| 权重与总体进度 | 同上 | 总权重 0 / 非 0；overallPercent 0 / 中间 / 100 | 权重表、overallPercent 取整与 clamp、completedLessons | P2 | 4 |
| 必修与尝试 | 同上 | 必修完成 / 未完成；最大尝试 0 / > 0；已用 < / = / > 上限 | `MANDATORY_LESSON_INCOMPLETE`、`ATTEMPT_LIMIT_REACHED` | P2 | 2 |
| 锁定与顺序 | 同上 | 锁定有进度 / 无进度；顺序开启 / 关闭；前一必修完成 / 未完成 | `LOCKED_LESSON_HAS_PROGRESS`、`SEQUENCE_RULE_BYPASSED`、nextLessonId | P2 | 2 |
| 下一课时 | 同上 | 无可访问下一课 / 有多个候选 / 首个未完成 | nextLessonId 精确值；完成与未完成时 `COMPLETION_STATE_CONFLICT` / `NO_ACCESSIBLE_NEXT_LESSON` | P2 | 2 |
| 账户与访问 | 同上 | 课程发布 / 未发布；账号正常 / 停用；订阅有效 / 失效；准则接受 / 未接受 | 四个阻断码分别出现；courseCompleted=false | P2 | 4 |
| 时间边界 | 同上 | 报名未来 / 现在；访问已过期 / 等于到期 / 7 天内 / 8 天 | `ENROLLMENT_DATE_IN_FUTURE`、`COURSE_ACCESS_EXPIRED`、`COURSE_ACCESS_EXPIRES_SOON` | P2 | 3 |
| 证书资格 | 同上 | 证书开 / 关；完成 / 未完成；身份验证 / 未验证；欠款 / 无欠款；名称缺失 / 1 字符 / 2 字符 / 80 字符 / 81 字符 | `CERTIFICATE_DISABLED`、`CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION`、`CERTIFICATE_BLOCKED_BY_BALANCE`、`CERTIFICATE_NAME_REQUIRED`、`CERTIFICATE_NAME_LENGTH_INVALID`、certificateEligible | P2 | 4 |
| 最终项目与审批 | 同上 | 要求 / 不要求；已通过 / 未通过；审批满足 / 缺失 | `FINAL_PROJECT_NOT_APPROVED`、`INSTRUCTOR_APPROVAL_REQUIRED`、courseCompleted=false | P2 | 2 |
| 空课程 | 同上 | 空 / 非空 | `COURSE_HAS_NO_LESSONS`、overallPercent=0、courseCompleted=false | P2 | 1 |
| 入参校验 | 同上 | 13 类非法输入 | 对应 `NullPointerException` 或 `IllegalArgumentException` | P2 | 2 |

## D. ExpressionExamProgressService

| SUT | 公开入口或可达入口 | 需覆盖的分支对 | 断言点 | 优先级 | 预估测试数 |
|-----|--------------------|----------------|--------|--------|------------|
| null 输入与空白 lessonId | `evaluate(snapshot, lessonId, grade)` | null / 非 null；空白 / 非空白 | `NullPointerException` 或 `IllegalArgumentException` | P2 | 1 |
| manualReview 拒绝 | 同上 | manualReview true / false | true 时抛出 `IllegalArgumentException`，false 时继续 | P2 | 1 |
| 课时不存在 | 同上 | 找到 / 未找到 | `IllegalArgumentException`，不产生部分快照 | P2 | 1 |
| 课时重复 | 同上 | 唯一 / 重复 | `IllegalArgumentException`，不更新任何课时 | P2 | 1 |
| 类型非 QUIZ | 同上 | QUIZ / 非 QUIZ | `IllegalArgumentException`，不改动课时 | P2 | 1 |
| 百分比 clamp | 同上 | 报告百分比 < 0 / 0–100 / > 100 | 更新后分数为 0 或 100；不越界 | P2 | 2 |
| INTEGRITY 失败 | 同上 | 有 / 无 INTEGRITY 失败 | 目标课时 `plagiarismFlag=true` | P2 | 1 |
| 快照重建 | 同上 | 单课时 / 多课时；不可变列表 | 非目标课时原样保留；更新后的快照字段完整；列表不可变 | P2 | 2 |

## E. 表达式引擎

| SUT | 公开入口或可达入口 | 需覆盖的分支对 | 断言点 | 优先级 | 预估测试数 |
|-----|--------------------|----------------|--------|--------|------------|
| `BracketsCalculation2` | `getInstance`、`calculation`、`compile`、`compileBigDecimal` | 新名称 / 已存在 / 同名不同类型；useBigDecimal 开 / 关；缓存开 / 关；formatRequired 真 / 假；同时通过该入口覆盖 `NumberCalculation.check` 的格式分支 | 同一名称复用、`ExtractException`、数值结果、缓存命中、格式异常 | P3 | 4 |
| `BracketsExpression` | `compile`、`compileBigDecimal` | 有括号 / 无括号 / 嵌套；缓存命中 / 未命中；double / BigDecimal | 结果值、缓存返回同一对象、表达式字符串 | P3 | 3 |
| `PrefixExpression` | `compile`、`compileBigDecimal`、`calculation`、`calculationBigDecimals`、栈访问器 | double 缓存命中 / 未命中；BigDecimal 缓存命中 / 未命中；isBigDecimal / isUnBigDecimal；isCopy=true / false；available 状态；精度转换 | 结果、栈副本、`UnsupportedOperationException`、`getExpressionStr` 不变量 | P3 | 5 |
| `PrefixExpressionOperation` | `check`、`formatStr`、`calculation`、`explain` | 7 类格式错误；`+-` 规整；useBigDecimal 开 / 关 | `WrongFormat`、`StringIndexOutOfBoundsException`（空串现状）、格式化字符串、数值结果 | P3 | 4 |
| `NumberUtils` | 静态方法 | 6 种运算 + 非法；4 种优先级组合 + 非优先；7 种比较 + 非法；阶乘 0 / 1 / 负数 / 中等大数；区间 start==end / step==1 / step≠1；累乘 start==end / 正步长 | 运算值、`AbnormalOperation`、布尔结果、阶乘值、区间字符串 | P3 | 5 |
| `StrUtils` | 静态方法 | 首/尾/连续分隔符；去空；阶乘 / 负数阶乘 / 缓存命中；BigDecimal 缓存 instanceof 命中 / 未命中；IsANumber 空串 / 越界字符 | 列表内容、字符串、数值、异常、缓存结果 | P3 | 4 |
| `ArithmeticExpression` | `supports`、`evaluate` | 合法字符 / 非法字符；空串 / null；嵌套括号；缺右括号；多余 token；一元 +/-；空格 | 支持判定、数值、`IllegalArgumentException` | P3 | 3 |
| `CalculationManagement` | `register`、`isRegister`、`getCalculationByName` | 重复检查开 / 关；新名称 / 既有名称；存在 / 不存在 | 返回值、原实例保留 / 覆盖、`isRegister` 真假 | P3 | 2 |
| `CumulativeCalculation` | `check`、`formatStr` | 区间段数错误 / 逗号数错误；start > end；step > 0 | `WrongFormat`、`StringIndexOutOfBoundsException`（start > end 现状）、正确展开结果 | P3 | 2 |
| 零散类型 | `CalculationOptimized`、`BooleanCalculation`、`BooleanCalculation2`、`CalculationNumberResults`、`CalculationBooleanResults`、`NameExpression`、`PackExpression`、`LogResults` | 合法 / 非法运算符；formatStr；null 字面量；7 种比较；compareTo null / 大 / 小 / 相等；toString 不变量；缓存与可用性 | 数值、`AbnormalOperation`、布尔结果、比较符号、字符串不变量 | P3 | 2 |

## F. 端到端

| SUT | 公开入口或可达入口 | 需覆盖的分支对 | 断言点 | 优先级 | 预估测试数 |
|-----|--------------------|----------------|--------|--------|------------|
| 完整评分旅程 | `ExpressionExamGradingService.grade` | 正确 / 容差内 / 错误 / 未作答 / 非法 | 每题评估与报告字段自洽；分区百分比与总分一致 | P4 | 2 |
| 成绩回写进度 | `ExpressionExamProgressService.evaluate` | 无 manualReview / 有；QUIZ 存在 / 不存在 | 课时完成度、分数、attempts、submitted、重新评估后的 nextLessonId | P4 | 1 |
| 诚信端到端 | 同上 | 有 / 无 INTEGRITY 失败 | 课时抄袭标记、阻断码、课程不能完成 | P4 | 1 |
| 确定性 | 同一输入连续两次 | 首次 / 第二次 | 报告与进度逐字段相同 | P4 | 1 |
| 分区与必修 | 同上 | 分区达标 / 不达标；必修完成 / 未完成 | `passed` 四合一、`SECTION_BELOW_PASS_MARK`、`MANDATORY_QUESTION_UNANSWERED` | P4 | 1 |
