# Data Model: 测试夹具

**Feature**: 001-competition-test-suite
**Date**: 2026-11-01
**Input**: [spec.md](./spec.md)、[plan.md](./plan.md)、[research.md](./research.md)

## 通用规则

- 所有夹具通过 `StudentTest` 内的具名静态工厂构造，禁止在测试方法中直接写出长位置参数列表。
- 每个夹具提供一个“标准样本”工厂和若干“变体”工厂；变体名称表达业务差异，不表达参数位置。
- 字符串标识使用非空、非空白值；集合字段允许传入 `null`，但构造后会变成不可变空集合。
- 测试只断言可观察行为与不可变性，不依赖 `Map`、`Set` 的迭代顺序。
- 标准样本用于“快乐路径 + 字段传递”测试；变体用于边界、异常与分支覆盖。

## AnswerResult（19 字段）

**工厂命名**：`answerStandard()`、`answerNumeric(...)`、`answerChoice(...)`、`answerMultiChoice(...)`、`answerEssay(...)`、`answerProgramming(...)`、`answerUnanswered(...)`、`answerWrong(...)`、`answerWithinTolerance(...)`、`answerInvalidNumeric()`。

**标准样本**：`q1` / NUMERIC / `math` / 满分 10.00 / 必修 / 已作答 / 精确匹配 / 无选项 / 误差 0 / 容差 0.01 / rubric 0 / 编程计数 0 / 已编译 / 无抄袭 / 未超时 / 无需人工复核。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| questionId | 非空字符串 | `q1`；空白非法 | `answerStandard`、`answerUnanswered` |
| type | QuestionType 七个常量 | 七种题型各一 | `answerChoice`、`answerProgramming`、`answerEssay` |
| section | 非空字符串 | `math`；空白非法 | `answerStandard`、`answerWithSection` |
| maximumPoints | BigDecimal，> 0，两位小数 | 0.01、1.00、10.00 | `answerStandard`、`answerMaxOne` |
| mandatory | boolean | true / false | `answerStandard`、`answerOptional` |
| answered | boolean | true / false | `answerUnanswered` |
| exactMatch | boolean | true / false | `answerExact`、`answerWrong` |
| correctOptionCount | int，>= 0 | 0、1、4 | `answerMultiChoice` |
| selectedCorrectOptions | int，0 到 correctOptionCount | 0、1、correctOptionCount | `answerMultiChoicePartial` |
| selectedIncorrectOptions | int，>= 0 | 0、1 | `answerMultiChoiceOnlyIncorrect` |
| numericError | BigDecimal，>= 0 或 null | null、0、容差、容差 + 0.01 | `answerWithinTolerance`、`answerWrong` |
| numericTolerance | BigDecimal，>= 0 或 null | null、0、0.01、10 倍误差 | `answerMissingTolerance`、`answerWideTolerance` |
| rubricPercent | int，0 到 100 | 0、50、100 | `answerEssay`、`answerFillBlankPartial` |
| passedTests | int，>= 0 | 0、1、totalTests | `answerProgrammingPartial` |
| totalTests | int，>= 0 | 0、1、3 | `answerProgrammingNoTests`、`answerProgrammingAllPass` |
| compilationSucceeded | boolean | true / false | `answerProgrammingNotCompiled` |
| plagiarismFlag | boolean | true / false | `answerProgrammingPlagiarized` |
| timedOut | boolean | true / false | `answerTimedOut` |
| manualReviewRequired | boolean | true / false | `answerEssayManualReview` |

**变体清单**：

- 未作答：`answerUnanswered()`，`answered=false`，`exactMatch=false`，误差与容差为空。
- 容差内：`answerWithinTolerance()`，误差等于容差，`exactMatch=false`。
- 超差：`answerWrong()`，误差大于容差。
- 多选部分正确、仅选错误项、无正确选项三种变体。
- 填空 rubric 0、50、100 三种变体。
- 数值题缺少容差、容差内、10 倍容差内、超限四种变体。
- 编程题未编译、无测试、全过、部分过、超时、抄袭六种变体。

## ExamSubmission（18 字段）

**工厂命名**：`submissionStandard()`、`submissionWithAnswers(...)`、`submissionEmpty()`、`submissionLate(...)`、`submissionOvertime(...)`、`submissionIntegrity(...)`、`submissionWithOverride(...)`、`submissionWithSections(...)`、`submissionAttemptAtLimit()`、`submissionAttemptOverLimit()`。

**标准样本**：`exam-1` / `student-1` / 单个数值答案 / 基准时间提交 / 无截止时间 / 允许 3600 秒 / 第 1 次尝试 / 身份已验证 / 已接受条款 / 未停用 / 未断连 / IP 未变更 / 全屏退出 0 / 复制粘贴 0 / 加分 0 / 无教师覆写 / 无分区要求。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| examId | 非空字符串 | `exam-1`；空白非法 | `submissionStandard` |
| studentId | 非空字符串 | `student-1`；空白非法 | `submissionStandard` |
| answers | List，非 null；null 变为空列表 | 空、1、N；重复 questionId 非法 | `submissionEmpty`、`submissionWithAnswers` |
| startedAt | Instant，非 null | 基准时刻 | `submissionStandard` |
| submittedAt | Instant，非 null，且不早于 startedAt | 基准时刻、基准 + 3600 秒 | `submissionOvertime` |
| deadline | Instant 或 null | null、基准、基准 + 1 秒、基准 - 1 秒 | `submissionLate` |
| allowedDurationSeconds | int，>= 0 | 0、60、300、3600 | `submissionOvertime`、`submissionNoDurationLimit` |
| attemptNumber | int，> 0 | 1、上限、上限 + 1 | `submissionAttemptAtLimit`、`submissionAttemptOverLimit` |
| identityVerified | boolean | true / false | `submissionIdentityMissing` |
| termsAccepted | boolean | true / false | `submissionTermsMissing` |
| suspendedAccount | boolean | true / false | `submissionSuspended` |
| proctorDisconnected | boolean | true / false | `submissionProctorLost` |
| ipAddressChanged | boolean | true / false | `submissionIpChanged` |
| fullscreenExitCount | int，>= 0 | 0、1、2、3 | `submissionFullscreen(1/2/3)` |
| copyPasteEventCount | int，>= 0 | 0、4、5 | `submissionCopyPaste(4/5)` |
| bonusPoints | BigDecimal，>= 0 | 0.00、1.00 | `submissionWithBonus` |
| instructorOverridePercent | Integer 或 null | null、0、100；越界非法 | `submissionWithOverride(0/100)` |
| sectionPassPercentages | Map<String,Integer>，null 变为空 Map | 空、单分区、多分区；键空白或值越界非法 | `submissionWithSections` |

**变体清单**：

- 空答卷、单题答、多题答。
- 迟到 1 秒、59 分、60 分、61 分。
- 超时 60 秒、300 秒、301 秒。
- attempt 等于上限、超过上限。
- 身份未验证 + 策略要求/不要求。
- 全屏 1/2/3、复制粘贴 4/5、监考断连、IP 变更。
- 教师覆写 0/100、加分、分区缺考与低于线。

## GradingPolicy（14 字段）

**工厂命名**：`policyStandard()`、`policyStrict()`、`policyLenient()`、`policyPartialOff()`、`policyNegativeOn()`、`policyUnansweredPenalty()`、`policyIdentityRequired()`、`policyCapOff()`、`policyLatePenalty(...)`、`policyProgrammingRequired()`。

**标准样本**：及格 60 / 部分给分开启 / 负分标记关闭 / 错答罚率 0.25 / 未答罚率 0 / 每小时迟交扣 5 分 / 迟交封顶 50 分 / 分区最低 0 / 所有必修题必答 / 不要求身份验证 / 最多 3 次尝试 / 诚信违规阻断 / 百分比封顶 / 编程必须编译。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| passPercentage | int，0 到 100 | 0、60、100 | `policyStandard`、`policyStrict` |
| partialCreditEnabled | boolean | true / false | `policyPartialOff` |
| negativeMarkingEnabled | boolean | true / false | `policyNegativeOn` |
| wrongAnswerPenaltyRate | BigDecimal，0 到 1 | 0、0.25、1 | `policyNegativeOn` |
| unansweredPenaltyRate | BigDecimal，0 到 1 | 0、0.10、1 | `policyUnansweredPenalty` |
| latePenaltyPercentPerHour | int，>= 0 | 0、5、100 | `policyLatePenalty` |
| maximumLatePenaltyPercent | int，0 到 100 | 0、50、100 | `policyLatePenalty` |
| minimumSectionPercentage | int，0 到 100 | 0、60、100 | `policyStrict` |
| allMandatoryQuestionsRequired | boolean | true / false | `policyLenient` |
| identityVerificationRequired | boolean | true / false | `policyIdentityRequired` |
| maximumAttempts | int，> 0 | 1、3、5 | `policyStandard` |
| failOnIntegrityViolation | boolean | true / false | `policyLenient` |
| capScoreAtOneHundred | boolean | true / false | `policyCapOff` |
| programmingCompilationRequired | boolean | true / false | `policyProgrammingRequired` |

**变体清单**：严格策略、宽松策略、部分给分关闭、负分开启、未答惩罚开启、身份验证开启、封顶关闭、迟交 100% 封顶。

## ExpressionQuestion（6 字段）

**工厂命名**：`questionStandard()`、`questionWithTolerance(...)`、`questionZeroTolerance()`、`questionInvalidCorrectExpression()`、`questionNonMandatory()`、`questionMaxOne()`。

**标准样本**：`q1` / `math` / `(2+3)*4` / 满分 10.00 / 容差 0.00 / 必修。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| questionId | 非空字符串 | `q1`；空白非法 | `questionStandard` |
| section | 非空字符串 | `math`；空白非法 | `questionStandard` |
| correctExpression | 非空且可求值 | `(2+3)*4`；`1+` 非法 | `questionInvalidCorrectExpression` |
| maximumPoints | BigDecimal，> 0 | 0.01、10.00 | `questionMaxOne` |
| tolerance | BigDecimal，>= 0 | 0、0.01 | `questionWithTolerance` |
| mandatory | boolean | true / false | `questionNonMandatory` |

**变体清单**：零容差、带容差、最大分 1、非必修、正确表达式不可求值。

## ExpressionAnswerSubmission（2 字段）

**工厂命名**：`expressionSubmission(...)`、`blankExpressionSubmission()`、`nullExpressionSubmission()`、`invalidExpressionSubmission()`、`withinToleranceSubmission()`。

**标准样本**：`questionStandard()` + `10+10`。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| question | ExpressionQuestion，非 null | 标准题、带容差题、非法题 | `expressionSubmission` |
| submittedExpression | 可空字符串 | `10+10`、空串、空白、null、`1+`、不可解析文本 | `blankExpressionSubmission`、`invalidExpressionSubmission` |

**变体清单**：空串、空白、null、非法表达式、容差内答案、错误答案。

## ExpressionExamSubmission（18 字段）

**工厂命名**：`expressionExamStandard()`、`expressionExamEmpty()`、`expressionExamWithAnswers(...)`、`expressionExamLate(...)`、`expressionExamOvertime(...)`、`expressionExamIntegrity(...)`、`expressionExamWithOverride(...)`、`expressionExamWithSections(...)`。

**标准样本**：`exam-1` / `student-1` / 单个表达式答案 / 基准时间提交 / 无截止时间 / 允许 3600 秒 / 第 1 次尝试 / 身份已验证 / 已接受条款 / 未停用 / 未断连 / IP 未变更 / 全屏退出 0 / 复制粘贴 0 / 加分 0 / 无教师覆写 / 无分区要求。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| examId | 非空字符串 | `exam-1` | `expressionExamStandard` |
| studentId | 非空字符串 | `student-1` | `expressionExamStandard` |
| answers | List<ExpressionAnswerSubmission>，null 变为空列表 | 空、1、N | `expressionExamEmpty`、`expressionExamWithAnswers` |
| startedAt | Instant，非 null | 基准时刻 | `expressionExamStandard` |
| submittedAt | Instant，非 null，且不早于 startedAt | 基准时刻、基准 + 3600 秒 | `expressionExamOvertime` |
| deadline | Instant 或 null | null、基准、基准 + 1 秒 | `expressionExamLate` |
| allowedDurationSeconds | int，>= 0 | 0、60、300、3600 | `expressionExamOvertime` |
| attemptNumber | int，> 0 | 1、上限、上限 + 1 | `expressionExamAttemptLimit` |
| identityVerified | boolean | true / false | `expressionExamIdentityMissing` |
| termsAccepted | boolean | true / false | `expressionExamTermsMissing` |
| suspendedAccount | boolean | true / false | `expressionExamSuspended` |
| proctorDisconnected | boolean | true / false | `expressionExamProctorLost` |
| ipAddressChanged | boolean | true / false | `expressionExamIpChanged` |
| fullscreenExitCount | int，>= 0 | 0、1、2、3 | `expressionExamFullscreen(1/2/3)` |
| copyPasteEventCount | int，>= 0 | 0、4、5 | `expressionExamCopyPaste(4/5)` |
| bonusPoints | BigDecimal，>= 0 | 0.00、1.00 | `expressionExamWithBonus` |
| instructorOverridePercent | Integer 或 null | null、0、100 | `expressionExamWithOverride` |
| sectionPassPercentages | Map<String,Integer>，null 变为空 Map | 空、单分区、多分区 | `expressionExamWithSections` |

**变体清单**：与 ExamSubmission 对应；额外验证 `toExamSubmission(answers)` 把表达式答案转换为评分答案列表，并保留全部提交字段。

## LessonProgress（14 字段）

**工厂命名**：`lessonStandard()`、`videoLesson(...)`、`articleLesson(...)`、`quizLesson(...)`、`assignmentLesson(...)`、`liveLesson(...)`、`projectLesson(...)`、`lockedLesson()`、`lessonAtAttemptLimit()`、`lessonWithPlagiarism()`。

**标准样本**：`lesson-1` / VIDEO / 必修 / 顺序 1 / 完成度 100 / 分数 100 / 已尝试 1 / 最多 3 / 已提交 / 已通过 / 无抄袭 / 出席 0 分钟 / 要求 0 分钟 / 已解锁。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| lessonId | 非空字符串，课程内唯一 | `lesson-1`；重复非法 | `lessonStandard` |
| type | LessonType 六个常量 | 六种课时各一 | `videoLesson`、`quizLesson`、`projectLesson` |
| mandatory | boolean | true / false | `lessonStandard` |
| order | int，>= 0，课程内唯一 | 0、1、N；重复非法 | `lessonStandard` |
| completionPercent | int，0 到 100 | 0、50、70、90、100 | `videoLesson(70/90/100)` |
| score | int，0 到 100 | 0、59、60、100 | `quizLesson(score)` |
| attempts | int，>= 0 | 0、上限 | `lessonAtAttemptLimit` |
| maximumAttempts | int，>= 0 | 0、3 | `lessonAtAttemptLimit` |
| submitted | boolean | true / false | `quizLesson`、`assignmentLesson` |
| approved | boolean | true / false | `assignmentLesson`、`projectLesson` |
| plagiarismFlag | boolean | true / false | `lessonWithPlagiarism` |
| attendedMinutes | int，>= 0 | 0、requiredMinutes | `liveLesson` |
| requiredMinutes | int，>= 0 | 0、80 | `liveLesson` |
| unlocked | boolean | true / false | `lockedLesson` |

**变体清单**：六种课时类型、VIDEO 70–90 告警、QUIZ 未提交/低于线、ASSIGNMENT 未提交/未通过、LIVE 要求 0 与非 0、PROJECT 抄袭、尝试次数耗尽、锁定有进度。

## CourseProgressSnapshot（18 字段）

**工厂命名**：`snapshotStandard()`、`snapshotEmpty()`、`snapshotExpired()`、`snapshotExpiringSoon()`、`snapshotFutureEnrollment()`、`snapshotWithLessons(...)`、`snapshotSequential(...)`、`snapshotCertificateEdge(...)`、`snapshotWithBlockers(...)`、`snapshotFinalProjectRequired()`。

**标准样本**：`student-1` / `course-1` / 一个完成视频课时 / 非顺序学习 / 要求 60% / 测验 60 分 / 身份已验证 / 已接受准则 / 订阅有效 / 账号正常 / 课程已发布 / 30 天前报名 / 30 天后到期 / 证书开启 / 证书名 `Alice` / 无最终项目 / 无欠款 / 无教师审批要求。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| studentId | 非空字符串 | `student-1` | `snapshotStandard` |
| courseId | 非空字符串 | `course-1` | `snapshotStandard` |
| lessons | List<LessonProgress>，null 变为空列表 | 空、1、N | `snapshotEmpty`、`snapshotWithLessons` |
| sequentialLearning | boolean | true / false | `snapshotSequential` |
| requiredOverallPercent | int，0 到 100 | 0、60、100 | `snapshotStandard` |
| requiredQuizScore | int，0 到 100 | 0、60、100 | `snapshotStandard` |
| identityVerified | boolean | true / false | `snapshotCertificateEdge` |
| codeOfConductAccepted | boolean | true / false | `snapshotWithBlockers` |
| subscriptionActive | boolean | true / false | `snapshotWithBlockers` |
| accountSuspended | boolean | true / false | `snapshotWithBlockers` |
| coursePublished | boolean | true / false | `snapshotWithBlockers` |
| enrolledAt | Instant 或 null | 基准、基准 + 1 秒 | `snapshotFutureEnrollment` |
| accessExpiresAt | Instant 或 null | 基准 - 1 秒、基准、基准 + 7 天、基准 + 8 天 | `snapshotExpired`、`snapshotExpiringSoon` |
| certificateEnabled | boolean | true / false | `snapshotCertificateEdge` |
| certificateName | String 或 null | null、空串、1 字符、2 字符、80 字符、81 字符 | `snapshotCertificateEdge` |
| finalProjectRequired | boolean | true / false | `snapshotFinalProjectRequired` |
| outstandingBalance | boolean | true / false | `snapshotCertificateEdge` |
| instructorApprovalRequired | boolean | true / false | `snapshotWithBlockers` |

**变体清单**：空课程、已过期、7 天内到期、报名在未来、课程未发布、账号停用、订阅失效、准则未接受、证书名边界、最终项目未通过、教师审批缺失。

## GradeReport（10 字段）

**工厂命名**：`reportStandard()`、`reportFailed()`、`reportManualReview()`、`reportCapped()`、`reportUncapped()`、`reportWithSections(...)`、`reportWithCodes(...)`。

**标准样本**：原始分 10.00 / 调整分 10.00 / 满分 10.00 / 百分比 100 / 等级 A+ / 通过 / 无需人工复核 / 分区 `math:100` / 无失败码 / 无告警码。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| rawPoints | BigDecimal，>= 0，两位小数 | 0.00、10.00 | `reportStandard` |
| finalPoints | BigDecimal，>= 0，两位小数 | 0.00、10.00、超过满分 | `reportCapped`、`reportUncapped` |
| maximumPoints | BigDecimal，>= 0，两位小数 | 0.00、10.00 | `reportStandard` |
| percentage | int，0 到 100，封顶关闭时可超过 100 | 0、59、60、95、100、101 | `reportUncapped` |
| letterGrade | 字符串，A+ 到 F | 60/65/70/75/80/85/90/95 边界 | `reportBoundary(...)` |
| passed | boolean | true / false | `reportFailed` |
| manualReview | boolean | true / false | `reportManualReview` |
| sectionPercentages | Map<String,Integer>，null 变为空 Map | 空、单分区、多分区 | `reportWithSections` |
| failures | List<String>，null 变为空列表 | 空、稳定失败码 | `reportWithCodes` |
| warnings | List<String>，null 变为空列表 | 空、稳定告警码 | `reportWithCodes` |

**变体清单**：通过/不通过、手动复核、封顶/不封顶、9 个等级边界、分区缺考、稳定码列表、空集合。

## ProgressEvaluation（8 字段）

**工厂命名**：`progressStandard()`、`progressInProgress()`、`progressBlocked()`、`progressCertified()`、`progressEmptyCourse()`。

**标准样本**：总体 100% / 已完成 1 / 总数 1 / 课程完成 / 证书可用 / 无下一课时 / 无阻断 / 无告警。

| 字段 | 合法域 | 典型边界 | 工厂命名 / 变体 |
|------|--------|----------|----------------|
| overallPercent | int，0 到 100 | 0、59、60、100 | `progressInProgress`、`progressStandard` |
| completedLessons | int，>= 0 | 0、totalLessons | `progressEmptyCourse` |
| totalLessons | int，>= 0 | 0、N | `progressEmptyCourse` |
| courseCompleted | boolean | true / false | `progressStandard`、`progressBlocked` |
| certificateEligible | boolean | true / false | `progressCertified` |
| nextLessonId | String 或 null | null、`lesson-2` | `progressInProgress` |
| blockers | List<String>，null 变为空列表 | 空、稳定阻断码 | `progressBlocked` |
| warnings | List<String>，null 变为空列表 | 空、稳定告警码 | `progressBlocked` |

**变体清单**：进行中、被阻断、证书可用、空课程、下一课时存在。

## 不可变契约断言

- `ExamSubmission`、`GradeReport`、`ExpressionExamGrade`、`CourseProgressSnapshot`、`ProgressEvaluation` 对 `null` 集合回退为不可变空集合。
- 对传入的可变集合做防御性拷贝：修改原始集合不得影响夹具或结果。
- 测试应断言集合的不可变性（例如添加元素抛出 `UnsupportedOperationException`），而不是断言内部实现类。
- 测试不得依赖 `Map` 或 `Set` 的迭代顺序；只断言键值内容、大小与重复键冲突。
