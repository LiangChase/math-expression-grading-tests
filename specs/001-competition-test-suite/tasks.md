# Tasks: 单文件 JUnit 5 测试套件（评分最大化）

**Input**: Design documents from `/specs/001-competition-test-suite/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/test-matrix.md, quickstart.md

**Tests**: 本特性中“测试即实现”；测试套件就是交付物。没有产品代码任务，也没有“先写测试、再写实现”的 TDD 顺序。

**Organization**: 任务按用户故事分阶段；所有任务只修改唯一文件 `src/test/java/net/mooctest/StudentTest.java`。

**Concurrency**: 并发性因单文件约束被显式放弃；所有任务串行执行，不使用并行标记。

## Format: `[ID] [Story?] Description`

- 每条任务以 `- [ ]` 复选框开头，使用顺序 ID。
- 用户故事阶段使用 `[US1]` 到 `[US4]`；Setup、Foundational、Polish 不使用故事标签。
- 每条任务都写明文件路径与 `@Nested` 类名。
- 每条任务包含四要素：被测类与入口、要覆盖的行为/分支、断言要求、完成判据。
- “新增测试数”按行为用例计；同一任务的同类边界用参数化方法合并，阶段测量任务核对实际方法数不超过 160。
- 单文件约束下不存在可并行任务；任何并行执行都会造成同一文件冲突。

## Path Conventions

- 唯一可写代码文件：`src/test/java/net/mooctest/StudentTest.java`
- 只读被测对象：`src/main/java/net/mooctest/expression/engine`、`grading`、`progress`
- 只读构建配置：`pom.xml`
- 文档产物：`specs/001-competition-test-suite/`
- 禁止新增测试文件、禁止修改 `src/main`、禁止修改 `pom.xml`、禁止修改依赖或插件配置。

## Coverage Budget

| 类 / 组 | 实测分支 | 计划吃掉 | 阶段 | 验收任务 |
|---------|----------|----------|------|----------|
| `ExamGradingService` | 344 | 310 | Phase 3 | T022 |
| `ExpressionAnswerGradingService` | 10 | 10 | Phase 3 | T022 |
| `ExpressionQuestion` | 8 | 8 | Phase 3 | T022 |
| `LearningProgressService` | 263 | 231 | Phase 4 | T035 |
| `ExpressionExamProgressService` | 20 | 20 | Phase 4 | T035 |
| `PrefixExpression` | 76 | 65 | Phase 5 | T050 |
| `PrefixExpressionOperation` | 47 | 40 | Phase 5 | T050 |
| `NumberUtils` | 46 | 44 | Phase 5 | T050 |
| `StrUtils` | 42 | 40 | Phase 5 | T050 |
| `ArithmeticExpression` | 38 | 38 | Phase 5 | T050 |
| `BracketsExpression` | 22 | 20 | Phase 5 | T050 |
| `BracketsCalculation2` | 19 | 17 | Phase 5 | T050 |
| `CumulativeCalculation` | 18 | 14 | Phase 5 | T050 |
| `BooleanCalculation2` | 12 | 11 | Phase 5 | T050 |
| `NumberCalculation` | 31 | 28 | Phase 5 | T050 |
| 其余零散类 | 76 | 57 | Phase 5 | T050 / T057 |
| **合计** | **1,072** | **953（88.9%）** | — | T061 |

阶段预算：

| 阶段 | 计划新增覆盖分支 | 累计目标 |
|------|------------------|----------|
| Phase 2 基线 | 16（冒烟与夹具） | 约 5% |
| Phase 3 评分 | 328 | ≥ 30% |
| Phase 4 进度 | 251 | ≥ 55% |
| Phase 5 引擎 | 374 | ≥ 80% |
| Phase 6 端到端 | 0（复用并验证） | ≥ 85% |
| Phase 7 收口 | 0（仅存活变异体补测，不新增覆盖预算） | ≥ 85%（目标 90%） |

说明：本表采用规格基线数字；当前 `jacoco.csv` 对 `PrefixExpressionOperation` 与 `BracketsCalculation2` 可能显示 48 与 20，最终以实际报告分母为准，按同比例调整绝对目标。

## Template Deviations

- **单文件 → 全部任务串行**：所有任务写同一个文件，不能并行；不使用并行标记，不安排多人同时编辑。
- **测试即实现 → 没有实现代码任务**：不产生服务实现、数据模型或 Spring 配置类任务，也不改依赖；夹具只需在 `StudentTest` 内用内部静态工厂构造。
- **测试是产物 → 没有“先红后绿”的 TDD 顺序**：每条任务按“先读被测源码 → 写测试 → `mvn -q test` 全绿 → 记录覆盖”执行，而不是先写失败测试再实现产品代码。
- **任务按 `@Nested` 组织**：一个 `@Nested` 类承载 6–14 个行为用例；同类边界用参数化方法合并，实际方法数由阶段测量任务核对。
- **禁止修改唯一文件以外的东西**：任何任务都不得修改 `src/main`、`pom.xml`、依赖或插件配置；发现源码可疑行为只允许按现状钉住。

## Dependency Graph

```text
Phase 1 (Setup)
    └── Phase 2 (Foundational)
            ├── Phase 3 [US1] 评分主链路
            ├── Phase 4 [US2] 学习进度
            └── Phase 5 [US3] 表达式引擎
                    └── Phase 6 [US4] 端到端与不变量
                            └── Phase 7 Polish
```

- Phase 3、Phase 4、Phase 5 之间没有硬依赖，可以按 P1 → P2 → P3 顺序执行，也可以交换顺序；但它们都依赖 Phase 1–2。
- Phase 6 依赖 Phase 3–5 的测试资产；Phase 7 依赖前四阶段全部完成。
- 同一阶段内的任务按 ID 顺序执行；每个任务都以 `mvn -q test` 全绿作为完成前提。
- 测量任务依赖本阶段所有测试任务完成；测量任务本身不新增行为用例。

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 建立唯一的测试文件、夹具工厂、断言约定与静态状态隔离层。

- [X] T001 建立 StudentTest 骨架与 @Nested 规划（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：无，仅在该文件内用注释列出后续嵌套组）。 (a) 被测/入口：无生产入口，搭建测试外壳； (b) 覆盖：保留 `package net.mooctest` 与顶层类 `StudentTest`，注释列出 `ExpressionAnswerGradingServiceTest`、`ExamGradingServiceTest`、`LearningProgressServiceTest`、`ExpressionExamProgressServiceTest`、`ExpressionEngineTest`、`EndToEndTest`，不写空 @Nested 类； (c) 断言：无行为断言，只检查编译与结构； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 预期吃掉 0 个分支。

- [X] T002 用内部静态工厂构造评分夹具（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`FixtureSupportTest.GradingFixtures`）。 (a) 被测/入口：不以生产方法为入口，构造 `AnswerResult`（19 字段）、`ExamSubmission`（18 字段）、`GradingPolicy`（14 字段）的标准样本与变体； (b) 覆盖：标准样本 + 未作答 / 迟到 / 超时 / 诚信 / 覆写 / 多选 / 编程 / 作文变体；定义 `BASE_TIME`、`QUESTION_ID`、`SECTION`、`TOLERANCE`、`MAX_ATTEMPTS`、`PASS_PERCENTAGE` 常量； (c) 断言：标准样本字段精确值、变体间差异、禁止裸 `new` 长位置参数列表； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 0 个分支（夹具层）。

- [X] T003 用内部静态工厂构造进度与表达式夹具（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`FixtureSupportTest.ProgressAndExpressionFixtures`）。 (a) 被测/入口：构造 `ExpressionQuestion`（6 字段）、`ExpressionAnswerSubmission`（2 字段）、`ExpressionExamSubmission`（18 字段）、`LessonProgress`（14 字段）、`CourseProgressSnapshot`（18 字段）、`GradeReport`（10 字段）、`ProgressEvaluation`（8 字段）的标准样本与变体； (b) 覆盖：标准样本 + 空集合 / 空作答 / 证书名边界 / 访问到期 / 课时类型 / 成绩码 / 进度阻断变体； (c) 断言：字段值精确、`null` 集合回退为空、标准样本与变体可区分； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 0 个分支（夹具层）。

- [X] T004 建立断言与隔离约定（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`IsolationTest`）。 (a) 被测/入口：断言工具与 `@AfterEach` 生命周期； (b) 覆盖：BigDecimal 采用 `compareTo(...) == 0`；异常采用 `assertThrows(具体类型)`；稳定码精确相等或 `startsWith`；`@AfterEach` 先 `Mathematical_Expression.Options.setUseCache(false)` 再 `setUseBigDecimal(false)`；注册表使用唯一组件名； (c) 断言：复位后 `isUseCache=false`、`isUseBigDecimal=false`；注册表测试不依赖全局大小； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 0 个分支；同时记录 Phase 1 结束时的测试方法数与分支覆盖基线。

**Checkpoint**: 测试外壳、夹具、断言与隔离层就绪；可以开始 Phase 2。

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 固定时钟、冒烟链路与夹具完整性，是所有用户故事的共同前提。

**Critical**: Phase 2 完成前不得开始 Phase 3–6。

- [X] T005 固定时钟与时间边界夹具（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.ClockFixtureTest`）。 (a) 被测/入口：`LearningProgressService(Clock)`，使用 `Clock.fixed(2026-01-15T10:00:00Z, UTC)`； (b) 覆盖：`enrolledAt` 为将来 / 现在；`accessExpiresAt` 已过期 / 等于现在 / 7 天内 / 8 天； (c) 断言：对应阻断码 `ENROLLMENT_DATE_IN_FUTURE`、`COURSE_ACCESS_EXPIRED` 与告警码 `COURSE_ACCESS_EXPIRES_SOON`； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 0 个分支（时间夹具层）。

- [X] T006 冒烟端到端与基线测量（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`EndToEndTest.SmokeTest`）。 (a) 被测/入口：`ExpressionAnswerGradingService.evaluate` 与 `MathematicalExpressionEvaluator`； (b) 覆盖：表达式题目 + 学生作答 → 单题判定，证明不需要 Spring 上下文；运行 `mvn -q test` 并读取 `target/site/jacoco/jacoco.csv` 记录初始分支数； (c) 断言：状态为 `EXACT`，expectedValue 与 actualValue 精确相等； (d) 完成：`mvn -q test` 全绿 + 新增 1 个行为用例 + 预期吃掉 8 个分支；记录基线 43/1,072（约 4%）。

- [X] T007 夹具完整性与不可变性（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`FixtureSupportTest.ImmutabilityTest`）。 (a) 被测/入口：10 类夹具的静态工厂与 record 紧凑构造器； (b) 覆盖：`null` 集合回退为空、`List.copyOf` / `Map.copyOf` 防御性拷贝、标准样本字段、变体差异； (c) 断言：修改原始集合不影响夹具；对结果集合添加元素抛 `UnsupportedOperationException`；不依赖 `Map` 迭代顺序； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 8 个分支。

**Checkpoint**: 固定时钟与夹具基础完成；可以开始任一用户故事阶段。

---

## Phase 3: User Story 1 - 评分主链路判定正确 (Priority: P1) 🎯 MVP

**Goal**: 验证单题五种判定状态与整场成绩报告，覆盖 `ExamGradingService` 的题型、提交规则、汇总与诚信分支。

**Independent Test**: 只执行 Phase 3 的测试组，即可验证 `ExpressionAnswerGradingService` 与 `ExamGradingService` 的状态、分数、失败码、告警码与报告字段；不依赖进度与引擎测试。

- [X] T008 [US1] 单题状态与题目校验（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionAnswerGradingServiceTest.StatusMappingTest`）。 (a) 被测/入口：`ExpressionAnswerGradingService.evaluate(ExpressionAnswerSubmission)`、`ExpressionQuestion` 构造器； (b) 覆盖：空 / 空白 / null 作答 → `UNANSWERED`；正确表达式不可求值 → `IllegalStateException`；学生表达式非法 → `INVALID_EXPRESSION`；误差 0 → `EXACT`；误差 ≤ 容差 → `WITHIN_TOLERANCE`；误差 > 容差 → `INCORRECT`；题目空白字段、满分 ≤ 0、负容差； (c) 断言：状态枚举精确相等；expectedValue / actualValue / absoluteError 精确值；`ANSWER_NOT_PROVIDED`、`INVALID_STUDENT_EXPRESSION`；异常类型； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 18 个分支。

- [X] T009 [US1] validate 的 12 类非法输入（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.ValidationTest`）。 (a) 被测/入口：`ExamGradingService.grade(ExamSubmission, GradingPolicy)`； (b) 覆盖：null 提交 / null 策略；空白 examId / studentId；null 时间戳；提交早于开始；负数允许时长；attempt ≤ 0；负数诚信计数；null 或负加分；覆写越界；重复 questionId；空白分区名；分区百分比越界；策略百分比 / 罚率 / 尝试上限越界；答案 null / 空白 ID / null 类型 / 空白分区 / 满分 ≤ 0 / 选项计数非法 / rubric 越界 / 编程计数非法； (c) 断言：`NullPointerException` 或 `IllegalArgumentException` 精确类型；校验在评分前发生，不产生部分报告； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 26 个分支。

- [X] T010 [US1] 未作答与未作答惩罚（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.UnansweredTest`）。 (a) 被测/入口：`ExamGradingService.grade`； (b) 覆盖：未作答惩罚率 0 / > 0；负分标记开 / 关；必修题未作答 / 非必修；必修题 0 分告警； (c) 断言：0 分或负分；`UNANSWERED_PENALTY:<id>`、`MANDATORY_QUESTION_UNANSWERED:<id>`、`MANDATORY_QUESTION_NO_CREDIT:<id>`；passed 在这些条件下为 false； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 15 个分支。

- [X] T011 [US1] 单选与判断（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.BinaryQuestionTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的 SINGLE_CHOICE 与 TRUE_FALSE 路径； (b) 覆盖：正确 / 错误；负分标记开 / 关；超时且得分 > 0 的 0.80 折扣；得分上限裁剪；负分关闭时截断为 0； (c) 断言：满分、0、负分、折扣后分数、裁剪后分数；`QUESTION_TIMED_OUT:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 25 个分支。

- [X] T012 [US1] 多选评分（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.MultipleChoiceTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的 MULTIPLE_CHOICE 路径； (b) 覆盖：完全正确；`correctOptionCount=0`；partialCredit 开 / 关；部分正确 + 错误选项；仅选错误项；负分标记开 / 关；错误项扣减 25% 或罚分率；得分上限； (c) 断言：精确分数；`MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:<id>`、`ONLY_INCORRECT_OPTIONS_SELECTED:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 34 个分支。

- [X] T013 [US1] 填空评分（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.FillBlankTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的 FILL_BLANK 路径； (b) 覆盖：精确 / 非精确；rubric 0 / 50 / 100；partialCredit 开 / 关；负分标记开 / 关； (c) 断言：满分、半比例分、0、负分；rubric 边界； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 15 个分支。

- [X] T014 [US1] 数值评分（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.NumericQuestionTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的 NUMERIC 路径； (b) 覆盖：精确；tolerance 缺失；容差内；10 倍容差内线性给分；超限；partialCredit 开 / 关；负分标记开 / 关； (c) 断言：满分、线性比例分、0、负分；`NUMERIC_TOLERANCE_DATA_MISSING:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 25 个分支。

- [X] T015 [US1] 作文评分（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.EssayTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的 ESSAY 路径； (b) 覆盖：精确；rubric = 0；rubric > 0；manualReviewRequired 开 / 关； (c) 断言：满分、0、rubric 比例分；`ESSAY_SCORE_IS_PROVISIONAL:<id>`；manualReview 标记； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 12 个分支。

- [X] T016 [US1] 编程评分（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.ProgrammingTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的 PROGRAMMING 路径； (b) 覆盖：未编译 + `programmingCompilationRequired` 开 / 关；无测试 + manualReview 开 / 关；全过；部分过；partialCredit 开 / 关；超时 0.75 折扣；抄袭清零； (c) 断言：0、10%、20%、满分、部分分、0.75 折扣、0；`PROGRAM_DID_NOT_COMPILE:<id>`、`PROGRAM_HAS_NO_AUTOMATED_TESTS:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 34 个分支。

- [X] T017 [US1] 加分与迟交（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.LateSubmissionTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的提交规则； (b) 覆盖：加分 > 0；迟到 1 秒 / 59 分 / 60 分 / 61 分；迟交扣分封顶；penalty ≥ 100 时追加失败；deadline 为空 / 提交时间等于 deadline； (c) 断言：调整分精确值；`BONUS_POINTS_APPLIED`、`LATE_SUBMISSION_PENALTY:<percent>`、`SUBMISSION_TOO_LATE`； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 20 个分支。

- [X] T018 [US1] 考试时长与尝试次数（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.DurationAndAttemptTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的时长与尝试规则； (b) 覆盖：允许时长 0 → 不检查；超时 ≤ 60 秒 → 宽限；61–300 秒 → 0.95；> 300 秒 → 0.80 + 失败；attempt == 上限 → 告警；attempt > 上限 → 失败且调整分 0； (c) 断言：调整分精确值；`EXAM_GRACE_PERIOD_USED`、`EXAM_DURATION_MINOR_OVERRUN`、`EXAM_DURATION_EXCEEDED`、`FINAL_ALLOWED_ATTEMPT`、`MAXIMUM_ATTEMPTS_EXCEEDED`； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 15 个分支。

- [X] T019 [US1] 分区与总分汇总（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.AggregationTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的汇总逻辑； (b) 覆盖：分区百分比；缺失必需分区；分区低于线；满分 0 / 非 0；百分比四舍五入；0 与 100 clamp；封顶开 / 关；通过条件四合一（总分、分区、必修、诚信）； (c) 断言：`sectionPercentages` 精确值；`SECTION_BELOW_PASS_MARK:<id>`、`REQUIRED_SECTION_MISSING:<id>`、`OVERALL_SCORE_BELOW_PASS_MARK`；passed 组合； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 32 个分支。

- [X] T020 [US1] 等级九档与百分比取整（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.LetterGradeTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的百分比与 `letterGrade`； (b) 覆盖：60 / 65 / 70 / 75 / 80 / 85 / 90 / 95 分档；< 60 的 F；≥ 95 的 A+；百分比四舍五入； (c) 断言：九个等级边界精确字符串；passed 与等级不互相反推； (d) 完成：`mvn -q test` 全绿 + 新增 9 个行为用例 + 预期吃掉 12 个分支。

- [X] T021 [US1] 诚信事件与教师覆写（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.IntegrityAndOverrideTest`）。 (a) 被测/入口：`ExamGradingService.grade` 的诚信与覆写分支； (b) 覆盖：条款未接受；账号停用；身份要求 / 不要求 × 已验证 / 未验证；全屏 1 / 2 / 3；复制粘贴 4 / 5；IP 变更；监考断连；抄袭；manualReviewRequired；覆写 0 / 100 / 越界； (c) 断言：`EXAM_TERMS_NOT_ACCEPTED`、`ACCOUNT_SUSPENDED`、`IDENTITY_NOT_VERIFIED`、`FULLSCREEN_EXIT_RECORDED`、`MULTIPLE_FULLSCREEN_EXITS`、`EXCESSIVE_FULLSCREEN_EXITS`、`COPY_PASTE_EVENTS_RECORDED`、`EXCESSIVE_COPY_PASTE_EVENTS`、`IP_ADDRESS_CHANGED`、`PROCTOR_CONNECTION_LOST`、`ANSWER_INTEGRITY_VIOLATION`、`INSTRUCTOR_OVERRIDE_APPLIED`；manualReview 与 passed； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 45 个分支。

- [X] T022 [US1] Phase 3 覆盖测量（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：`mvn -q test` 与 `target/site/jacoco/jacoco.csv`； (b) 覆盖：记录 `ExamGradingService`、`ExpressionAnswerGradingService`、`ExpressionQuestion` 的 `BRANCH_MISSED` / `BRANCH_COVERED`； (c) 断言：本阶段新增覆盖 ≥ 328 个分支，累计分支覆盖率 ≥ 30%；测试方法总数阶段核对； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 记录本阶段分支增量。

**Checkpoint**: US1 可独立验证；单题状态与整场报告字段应全部可断言。

---

## Phase 4: User Story 2 - 学习进度与通过性判定 (Priority: P2)

**Goal**: 验证六类课时的完成规则、顺序学习、证书资格、账户访问与 QUIZ 回写。

**Independent Test**: 只执行 Phase 4 的测试组，即可用固定时钟与合成快照验证进度指标、阻断项、证书资格与回写；不依赖评分或引擎测试。

- [X] T023 [US2] VIDEO 课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.VideoLessonTest`）。 (a) 被测/入口：`LearningProgressService.evaluate(CourseProgressSnapshot)`； (b) 覆盖：完成度 ≥ 90 / 70–89 / < 70；必修 / 非必修；权重 15；已得权重； (c) 断言：complete 与 earned 精确值；`VIDEO_NEAR_COMPLETION:<id>`；completedLessons 与 overallPercent； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 18 个分支。

- [X] T024 [US2] ARTICLE 课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.ArticleLessonTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：完成度 = 100 / < 100；权重 10；比例分； (c) 断言：complete、earned、overallPercent； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 12 个分支。

- [X] T025 [US2] QUIZ 课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.QuizLessonTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：已提交 / 未提交；分数 ≥ 要求 / < 要求；权重 20； (c) 断言：complete、earned；`QUIZ_SCORE_TOO_LOW:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 18 个分支。

- [X] T026 [US2] ASSIGNMENT 课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.AssignmentLessonTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：未提交 / 已提交未通过 / 已通过；权重 25；半分或 0； (c) 断言：complete、earned；`ASSIGNMENT_AWAITING_REVISION:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 18 个分支。

- [X] T027 [US2] LIVE_SESSION 课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.LiveSessionTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：`requiredMinutes=0` 且有 / 无出席；`requiredMinutes>0` 且出席 ≥ 80% / < 80%；权重 20； (c) 断言：complete、earned；`LIVE_ATTENDANCE_TOO_SHORT:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 18 个分支。

- [X] T028 [US2] PROJECT 课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.ProjectLessonTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：已提交且通过且无抄袭 / 抄袭 / 未提交；权重 30；三分之一比例分； (c) 断言：complete、earned；`PROJECT_HELD_FOR_INTEGRITY_REVIEW:<id>`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 18 个分支。

- [X] T029 [US2] 账户与访问（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.AccountAccessTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：课程未发布 / 已发布；账号停用 / 正常；订阅失效 / 有效；准则未接受 / 接受；报名在未来 / 现在；访问已过期 / 等于到期 / 7 天内 / 8 天； (c) 断言：`COURSE_NOT_PUBLISHED`、`ACCOUNT_SUSPENDED`、`SUBSCRIPTION_INACTIVE`、`CODE_OF_CONDUCT_NOT_ACCEPTED`、`ENROLLMENT_DATE_IN_FUTURE`、`COURSE_ACCESS_EXPIRED`、`COURSE_ACCESS_EXPIRES_SOON`；courseCompleted； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 30 个分支。

- [X] T030 [US2] 顺序学习与下一课时（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.SequentialLearningTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：顺序开 / 关；前一必修完成 / 未完成；后继解锁；下一课时为首个可访问且未完成；完成状态下仍有下一课；无课可访问； (c) 断言：`SEQUENCE_RULE_BYPASSED:<id>`、`COMPLETION_STATE_CONFLICT`、`NO_ACCESSIBLE_NEXT_LESSON`；nextLessonId 精确值； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 22 个分支。

- [X] T031 [US2] 必修、尝试上限与锁定课程（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.MandatoryAndAttemptTest`）。 (a) 被测/入口：`LearningProgressService.evaluate`； (b) 覆盖：必修完成 / 未完成；最大尝试 0 / > 0；已用 < / = / > 上限；锁定有进度 / 无进度； (c) 断言：`MANDATORY_LESSON_INCOMPLETE:<id>`、`ATTEMPT_LIMIT_REACHED:<id>`、`LOCKED_LESSON_HAS_PROGRESS:<id>`；courseCompleted； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 24 个分支。

- [X] T032 [US2] 证书资格（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.CertificateTest`）。 (a) 被测/入口：`LearningProgressService.evaluate` 的证书分支； (b) 覆盖：证书关闭 / 开启；课程未完成 / 完成；身份未验证 / 已验证；有欠款 / 无欠款；证书名缺失 / 空白 / 1 字符 / 2 字符 / 80 字符 / 81 字符； (c) 断言：`CERTIFICATE_DISABLED`、`CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION`、`CERTIFICATE_BLOCKED_BY_BALANCE`、`CERTIFICATE_NAME_REQUIRED`、`CERTIFICATE_NAME_LENGTH_INVALID`；certificateEligible； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 22 个分支。

- [X] T033 [US2] 权重、总体进度、空课程与快照校验（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`LearningProgressServiceTest.WeightsAndValidationTest`）。 (a) 被测/入口：`LearningProgressService.evaluate` 的权重与校验； (b) 覆盖：总权重 0 / 非 0；overallPercent 0 / 中间 / 100；空课程；必修与尝试的组合；快照 13 类非法输入（空白 ID、重复课时 ID、重复 order、null 类型、负数 order / 完成度 / 分数 / 尝试 / 出席分钟、尝试超过上限、百分比越界等）； (c) 断言：overallPercent 取整与 clamp、`COURSE_HAS_NO_LESSONS`；`NullPointerException` 或 `IllegalArgumentException` 精确类型； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 31 个分支。

- [X] T034 [US2] QUIZ 回写（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionExamProgressServiceTest.WritebackTest`）。 (a) 被测/入口：`ExpressionExamProgressService.evaluate(snapshot, quizLessonId, grade)`； (b) 覆盖：manualReview 拒绝；lessonId 空白；课时不存在；同一 QUIZ 重复；目标类型非 QUIZ；报告百分比 < 0 / 0–100 / > 100 的 clamp；failures 含 INTEGRITY → `plagiarismFlag`；快照重建与非目标课时保留； (c) 断言：`IllegalArgumentException`；更新后 completion=100、score=百分比、attempts+1、submitted=true；`PLAGIARISM_REVIEW:<id>`；列表不可变； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 20 个分支。

- [X] T035 [US2] Phase 4 覆盖测量（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：`mvn -q test` 与 `target/site/jacoco/jacoco.csv`； (b) 覆盖：记录 `LearningProgressService` 与 `ExpressionExamProgressService` 的分支增量； (c) 断言：本阶段新增覆盖 ≥ 251 个分支，累计覆盖率 ≥ 55%；测试方法总数阶段核对； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 记录本阶段分支增量。

**Checkpoint**: US2 可独立验证；进度、证书与回写规则应全部可断言。

---

## Phase 5: User Story 3 - 表达式引擎数值与格式契约 (Priority: P3)

**Goal**: 验证引擎的数值、格式、缓存、精度与注册表契约，为 P1 提供真实求值路径。

**Independent Test**: 只执行 Phase 5 的测试组，即可直接验证引擎公开入口；不依赖评分与进度测试。

**Safety**: 区间累加步长只能大于 0；禁止 0 或负数，否则死循环。不得依赖测试执行顺序。只使用 public 入口；`getInstance` 是唯一构造途径，不得使用反射。

- [X] T036 [US3] ArithmeticExpression（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.ArithmeticExpressionTest`）。 (a) 被测/入口：`ArithmeticExpression.supports(String)`、`evaluate(String)`； (b) 覆盖：合法 / 非法字符；空串 / null；嵌套括号；缺失右括号；多余 token；一元 + / −；空格； (c) 断言：BigDecimal 精确值；`IllegalArgumentException`；supports 真假； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 38 个分支。

- [X] T037 [US3] BracketsCalculation2（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.BracketsCalculation2Test`）。 (a) 被测/入口：`getInstance`、`calculation`、`compile`、`compileBigDecimal`； (b) 覆盖：新名称 / 已存在 / 同名不同类型；useBigDecimal 开 / 关；缓存开 / 关；formatRequired 真 / 假；通过该入口覆盖 `NumberCalculation.check` 的格式分支； (c) 断言：同一名称返回同一实例；`ExtractException`；数值结果；缓存命中；`WrongFormat`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 19 个分支。

- [X] T038 [US3] BracketsExpression（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.BracketsExpressionTest`）。 (a) 被测/入口：`BracketsExpression.compile`、`compileBigDecimal`； (b) 覆盖：有括号 / 无括号 / 嵌套；缓存命中 / 未命中；double / BigDecimal；括号内递归计算； (c) 断言：数值结果；缓存返回同一对象；表达式字符串不变量； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 22 个分支。

- [X] T039 [US3] PrefixExpression 编译与精度（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.PrefixExpressionCompileTest`）。 (a) 被测/入口：`compile`、`compileBigDecimal`、`calculation`、`calculationBigDecimals`、`convertToMultiPrecisionSupported`； (b) 覆盖：double 缓存命中 / 未命中；BigDecimal 缓存命中 / 未命中；`isBigDecimal` / `isUnBigDecimal`；精度双向转换；错误精度调用抛 `UnsupportedOperationException`；`getExpressionStr`； (c) 断言：数值结果；精度标志；异常类型；字符串不变量； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 45 个分支。

- [X] T040 [US3] PrefixExpression 栈与可用性（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.PrefixExpressionStateTest`）。 (a) 被测/入口：`getDoubleStack`、`getBigDecimalsR`、`getCharacterStack`、`isAvailable`、`calculation(false)`； (b) 覆盖：isCopy=true / false；`calculation(false)` 后访问栈抛 `UnsupportedOperationException`；available 状态；栈副本独立； (c) 断言：副本内容相等但修改互不影响；异常类型；isAvailable 变化； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 20 个分支。

- [X] T041 [US3] PrefixExpressionOperation（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.PrefixExpressionOperationTest`）。 (a) 被测/入口：`check`、`formatStr`、`calculation`； (b) 覆盖：7 类格式错误（空串、空白、括号、未知符号、缺失数值、连续运算符、末字符非数值）；`+-` 规整为 `-` 并追加 `+0`；useBigDecimal 开 / 关； (c) 断言：`WrongFormat`；空串按现状抛 `StringIndexOutOfBoundsException`；格式化字符串；数值结果； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 45 个分支。

- [X] T042 [US3] NumberUtils（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.NumberUtilsTest`）。 (a) 被测/入口：`calculation`、`PriorityComparison`、`ComparisonOperation`、`factorial`、`sumOfRangeString`、`MultiplyOfRange`； (b) 覆盖：6 种运算 + 非法运算符；4 种高优先级组合 + 非优先；7 种比较 + 非法；阶乘 0 / 1 / 5 / 10 / 负数；区间 start==end / step==1 / step≠1；累乘 start==end / 正步长； (c) 断言：数值精确；`AbnormalOperation`；布尔结果；0! 现状为 0； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 46 个分支。

- [X] T043 [US3] StrUtils（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.StrUtilsTest`）。 (a) 被测/入口：`splitByChar`、`removeEmpty`、`stringToDouble`、`stringToBigDecimal`、`IsANumber`； (b) 覆盖：首 / 尾 / 连续分隔符；去空；阶乘 / 负数阶乘 / 缓存命中；BigDecimal 缓存 `instanceof` 命中 / 未命中；空串与越界字符； (c) 断言：列表内容；字符串；数值；`UnsupportedOperationException`；`IsANumber("")` 现状为 true； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 42 个分支。

- [X] T044 [US3] CalculationManagement（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.CalculationManagementTest`）。 (a) 被测/入口：`register`、`isRegister`、`getCalculationByName`； (b) 覆盖：新名称；重复注册且检查重复 → 返回 false 并保留原实例；强制注册 → 覆盖并返回 true；未知名称 → null / false； (c) 断言：返回值、原实例保留 / 覆盖、`isRegister` 真假； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 3 个分支。

- [X] T045 [US3] CumulativeCalculation（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.CumulativeCalculationTest`）。 (a) 被测/入口：`getInstance`、`check`、`formatStr`； (b) 覆盖：区间段数错误 / 逗号数错误；start > end；step > 0；正确展开； (c) 断言：`WrongFormat`；start > end 按现状抛 `StringIndexOutOfBoundsException`；数值结果； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 18 个分支。**安全：只使用 step > 0；禁止 0 或负数。**

- [X] T046 [US3] BooleanCalculation2 与 BooleanCalculation（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.BooleanCalculationTest`）。 (a) 被测/入口：`getInstance`、`check`、`calculation`、`formatStr`； (b) 覆盖：左 null / 右 null / 双 null；7 种比较运算符；非法运算符；空白格式化；`setCalculation` 自定义数值组件； (c) 断言：布尔结果；左右值；`AbnormalOperation`；`WrongFormat`； (d) 完成：`mvn -q test` 全绿 + 新增 8 个行为用例 + 预期吃掉 18 个分支。

- [X] T047 [US3] CalculationOptimized（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.CalculationOptimizedTest`）。 (a) 被测/入口：`CalculationOptimized.calculation(char, BigDecimal, BigDecimal)`； (b) 覆盖：加、减、乘、除、取余、幂；非法运算符； (c) 断言：BigDecimal 精确值；`AbnormalOperation`； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 10 个分支。

- [X] T048 [US3] 两个 Results 类（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.CalculationResultsTest`）。 (a) 被测/入口：`CalculationNumberResults` 与 `CalculationBooleanResults` 的构造器、`getResult`、`getResultLayers`、`getCalculationSourceName`、`getBigDecimalResult`、`compareTo`、`compare`、`toString`； (b) 覆盖：null / 大 / 小 / 相等；true / false；source 设置； (c) 断言：数值与布尔值；比较符号；字符串不变量； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 12 个分支。

- [X] T049 [US3] NumberCalculation、BracketsCalculation 与零散类型（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExpressionEngineTest.NumberCalculationAndScatteredTest`）。 (a) 被测/入口：具体子类的 `check`、`formatStr`、`calculation2`；`NameExpression`、`PackExpression`、`LogResults` 的公开方法； (b) 覆盖：空串 / 末字符非数值 / 括号数量 / 未知符号 / 连续运算符；格式去空白；`calculation2` 正常与无运算符；缓存与 available；`dismantling`；`LogResults` 结果读写； (c) 断言：`WrongFormat`；`AbnormalOperation`；结果与字符串；`UnsupportedOperationException`； (d) 完成：`mvn -q test` 全绿 + 新增 10 个行为用例 + 预期吃掉 36 个分支。

- [X] T050 [US3] Phase 5 覆盖测量（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：`mvn -q test` 与 `target/site/jacoco/jacoco.csv`； (b) 覆盖：记录引擎各类的分支增量； (c) 断言：本阶段新增覆盖 ≥ 374 个分支，累计覆盖率 ≥ 80%；测试方法总数阶段核对； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 记录本阶段分支增量。

- [X] T051 [US3] PIT 基线导出（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：`mvn -q org.pitest:pitest-maven:mutationCoverage`； (b) 覆盖：读取 `target/pit-reports/mutation.xml`，导出 killed / survived / no coverage； (c) 断言：记录每类杀死率；存活变异体按“缺输入 / 缺断言 / 等价”初步分类； (d) 完成：PIT 运行结束 + 新增 0 个行为用例 + 产出存活变异体清单。

**Checkpoint**: US3 可独立验证；引擎公开入口的数值、格式、缓存、精度与注册表契约应全部可断言。

---

## Phase 6: User Story 4 - 端到端与一致性 (Priority: P4)

**Goal**: 验证从表达式作答到成绩报告、再到进度回写的完整链路与不变量。

**Independent Test**: 只执行 Phase 6 的测试组，即可用真实求值器、固定时钟和合成快照完成一次完整旅程；不依赖其他测试的中间状态。

- [X] T052 [US4] 完整评分旅程（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`EndToEndTest.GradingJourneyTest`）。 (a) 被测/入口：`ExpressionExamGradingService.grade(ExpressionExamSubmission, GradingPolicy)`； (b) 覆盖：正确 / 容差内 / 错误 / 未作答 / 非法作答；报告字段与单题评估一一对应；分区百分比与总分自洽； (c) 断言：每题状态；rawPoints / finalPoints / maximumPoints / percentage / letterGrade / passed / manualReview；sectionPercentages；failures / warnings； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 0 个新分支（复用 P1–P3 路径）。

- [X] T053 [US4] 成绩回写旅程（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`EndToEndTest.WritebackJourneyTest`）。 (a) 被测/入口：`ExpressionExamGradingService.grade` → `ExpressionExamProgressService.evaluate`； (b) 覆盖：无 manualReview 的定稿成绩回写到存在的 QUIZ；更新后重新评估；nextLessonId 与 blockers 随成绩变化； (c) 断言：completion=100、score=报告百分比、attempts+1、submitted=true；重新评估后的 overallPercent、nextLessonId； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 0 个新分支。

- [X] T054 [US4] 诚信与不可变性不变量（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`EndToEndTest.IntegrityInvariantTest`）。 (a) 被测/入口：完整评分 → 回写 → 进度评估； (b) 覆盖：成绩含 INTEGRITY 失败；课时被标记抄袭；`PLAGIARISM_REVIEW` 阻断；总分 / 等级 / 通过 / blockers 组合不变量；`List.copyOf` 后的集合修改； (c) 断言：blockers 精确码；courseCompleted 与 certificateEligible；对集合添加元素抛 `UnsupportedOperationException`； (d) 完成：`mvn -q test` 全绿 + 新增 6 个行为用例 + 预期吃掉 0 个新分支。

- [X] T055 [US4] Phase 6 测量与确定性（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：`mvn -q test`、`target/site/jacoco/jacoco.csv`、连续两次运行； (b) 覆盖：记录 Phase 6 分支增量；比较两次运行的失败 / 错误 / 跳过数； (c) 断言：累计覆盖率 ≥ 85%；两次结果一致；无随机失败； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 记录本阶段结果。

**Checkpoint**: US4 可独立验证；完整链路的报告与进度不变量应全部可断言。

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 用 PIT 结果补测、记录等价变异、收口效率与可读性、产出交付证据。

- [X] T056 全量 PIT 与存活变异体导出（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：`mvn -q org.pitest:pitest-maven:mutationCoverage`； (b) 覆盖：读取 `target/pit-reports/mutation.xml` 与 `index.html`，按类、mutator、行号导出 survived； (c) 断言：计算 killed / (killed + survived)；每个存活变异体按“缺输入 / 缺断言 / 等价”分类； (d) 完成：PIT 运行结束 + 新增 0 个行为用例 + 产出三分类清单。

- [X] T057 针对缺输入与缺断言的批量补测（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest.SupplementaryTest`、`LearningProgressServiceTest.SupplementaryTest`、`ExpressionEngineTest.SupplementaryTest`）。 (a) 被测/入口：按 T056 清单中“缺输入”“缺断言”的类与入口； (b) 覆盖：按类批量补测，不按单个变异体拆分；优先补边界值与行为断言； (c) 断言：具体返回值、异常类型、状态枚举、失败码；不得新增弱断言唯一测试； (d) 完成：`mvn -q test` 全绿 + 新增 12 个行为用例 + 预期吃掉 0 个新覆盖分支（以杀死变异体为目标）。

- [X] T058 等价变异体清单（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅记录证据）。 (a) 被测/入口：T056/T057 后仍存活的变异体； (b) 覆盖：record / enum 自动生成方法；不可达 `LessonType` switch `default`；第三方 varFormatter 路径； (c) 断言：每条记录类名、mutator、行号、等价理由；不得为等价变异强行补测； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 清单 100% 覆盖剩余存活体。

- [X] T059 效率收口（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：`ExamGradingServiceTest`、`LearningProgressServiceTest`、`ExpressionEngineTest` 的既有分组）。 (a) 被测/入口：`target/surefire-reports` 与全套 `mvn -q test`； (b) 覆盖：统计方法数与耗时；超过 15 秒则压缩参数化行数、复用不可变夹具、移除重复构造； (c) 断言：全套 ≤ 15 秒（目标 ≤ 10 秒）；单测 ≤ 300 毫秒；方法总数 ≤ 160； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 记录耗时与方法数。

- [X] T060 可读性收口（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：全部既有分组）。 (a) 被测/入口：全部 `@Nested` 分组与静态工厂； (b) 覆盖：`@Nested` 命名统一为被测类；方法名读作行为描述；`@DisplayName` 只补充分组意图；魔法值提为常量；删除重复脚手架； (c) 断言：不改变测试语义；每个分组能一句话说明测什么；工厂调用不使用长位置参数； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 可读性检查通过。

- [X] T061 连续两次运行与交付证据（路径：`src/test/java/net/mooctest/StudentTest.java`；@Nested：不新增，仅运行测量命令）。 (a) 被测/入口：连续两次 `mvn -q test`、JaCoCo 报告、PIT 报告、`git diff -- src/main pom.xml` 或基线校验和； (b) 覆盖：失败 / 错误 / 跳过数；分支覆盖；变异杀死率；套件耗时；源码与 pom 差异； (c) 断言：两次结果一致；覆盖率 ≥ 85%（目标 90%）；杀死率 ≥ 75%（目标 85%）；耗时 ≤ 15 秒；`src/main` 与 `pom.xml` 差异为 0； (d) 完成：`mvn -q test` 全绿 + 新增 0 个行为用例 + 交付证据表填写完整。

## 交付证据表模板

| 指标 | 改前 | 改后 | 目标 | 证据来源 |
|------|------|------|------|----------|
| 分支覆盖 | 43 / 1,072（4.0%） | 待填写 | ≥ 85%（目标 90%） | `target/site/jacoco/jacoco.csv` |
| 变异杀死率 | 待填写 | 待填写 | ≥ 75%（目标 85%） | `target/pit-reports/mutation.xml` |
| 套件耗时 | 待填写 | 待填写 | ≤ 15 秒（目标 10 秒） | Surefire 报告 |
| 方法总数 | 待填写 | 待填写 | ≤ 160 | Surefire 报告 |
| `src/main` 与 `pom.xml` 差异 | — | 0 | 0 | `git diff` 或基线校验和 |
| 存活变异体分类率 | — | 100% | 100% | 等价变异体清单 |

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**：无依赖，立即开始。
- **Phase 2 Foundational**：依赖 Phase 1；阻塞所有用户故事。
- **Phase 3 [US1]**：依赖 Phase 2；与 Phase 4、Phase 5 无硬依赖。
- **Phase 4 [US2]**：依赖 Phase 2；与 Phase 3、Phase 5 无硬依赖。
- **Phase 5 [US3]**：依赖 Phase 2；与 Phase 3、Phase 4 无硬依赖。
- **Phase 6 [US4]**：依赖 Phase 3、Phase 4、Phase 5。
- **Phase 7 Polish**：依赖 Phase 3–6 全部完成。

### Within Each Phase

- 任务按 ID 顺序串行执行；同一文件不允许并行编辑。
- 每个任务完成后必须运行 `mvn -q test`，全绿后才能进入下一条。
- 阶段测量任务必须在本阶段所有测试任务完成后执行。
- 发现测试失败时，先修复当前任务，不得跳过后面的任务。

### User Story Dependencies

- **US1 (P1)**：评分主链路，是 MVP；不依赖 US2–US4。
- **US2 (P2)**：进度与回写；可独立于 US1 验证，但 QUIZ 回写使用 US1 的 `GradeReport` 形状。
- **US3 (P3)**：引擎契约；可独立于 US1/US2 验证，并为 US1 提供真实求值器。
- **US4 (P4)**：端到端；依赖 US1–US3 的测试资产。

### Parallel Opportunities

- 无。单文件约束下，所有任务串行；任何并行执行都会导致同一文件冲突。

## Implementation Strategy

### MVP First

1. 完成 Phase 1–2。
2. 完成 Phase 3（US1）。
3. 在 Phase 3 的 T022 停止并验证：`mvn -q test` 全绿，覆盖率 ≥ 30%，报告字段可断言。
4. 此时已可交付“评分主链路”最小测试套件。

### Incremental Delivery

1. Phase 1–2 → 基础夹具与冒烟链路。
2. Phase 3 → 评分主链路；测量并记录。
3. Phase 4 → 学习进度与回写；测量并记录。
4. Phase 5 → 表达式引擎；测量、PIT 基线。
5. Phase 6 → 端到端与不变量。
6. Phase 7 → PIT 补测、等价变异、效率与可读性收口、最终证据。

### Notes

- 所有任务只改 `StudentTest.java`；不修改 `src/main`、`pom.xml`、依赖或插件。
- 只使用 public 入口；引擎通过 `getInstance` 获取，不使用反射。
- 区间累加步长只能大于 0；禁止 0 或负数。
- 不依赖测试执行顺序；每个测试前后按约定复位静态状态。
- 每个阶段结束时必须记录分支覆盖增量；T061 负责最终的证据表与两次运行一致性。
- 任务描述中的“预期吃掉分支数”是计划值；实际以 `jacoco.csv` 为准，允许在同一阶段内调剂。

## Phase 8: Convergence

- [X] T062 补 ExamGradingService 的边界/条件变异体覆盖并完成等价分类 per SC-002/T058 (partial). 目标：kill ≥10/25，25 个存活体全部归类为缺输入/缺断言/等价；branch +0–4。行为清单：line 136 未答罚率边界、151/154/157 超时/封顶/负分裁剪、191/200/206 多选 partial/扣分/only-incorrect、242/246/247 数值容差与 10 倍线性、317 加分零边界、336/338/341/344 时长三段与允许时长边界、348/351 attempt 边界、377 分区阈值等号、420/423 复制粘贴 0/5、438 必修策略关闭、447–451 各诚信码单独命中、455 等级 60 且 passed=false、529/536/540/541/545/546/549/550/551/554/557 校验 0/100 边界。
- [X] T063 补 LearningProgressService 的边界/条件变异体覆盖并完成等价分类 per SC-002/T058 (partial). 目标：kill ≥6/11，全部归类；branch +2。行为清单：line 70 nextLesson 选择条件、147 ARTICLE 完成三元、171 LIVE 告警边界、246/250 证书名缺失/长度非法返回 false、268–271 noAcademicBlocker 的四个阻断码分别命中；将 LessonType switch default 记为不可达等价变异并写明理由。
- [X] T064 补 PrefixExpression 与 PrefixExpressionOperation 的变异体覆盖 per SC-002/T058 (partial). 目标：kill ≥10/23，全部归类；branch +0。行为清单：PrefixExpression 的 available/栈/精度/表达式字符串缓存分支；PrefixExpressionOperation 的 getInstance null 返回分支、check 边界、formatStr 与 explain 日志分支。
- [X] T065 补 NumberUtils 与 ArithmeticExpression 的变异体覆盖 per SC-002/T058/R7 (partial). 目标：kill ≥10/22，NumberUtils branch 41/46 → ≥44，全部归类。行为清单：ComparisonOperation 各比较符的 false 分支、范围字符串 MathMutator 边界、ArithmeticExpression skipSpaces 与减法/括号条件分支。
- [X] T066 补 CumulativeCalculation、BooleanCalculation2 与 BracketsCalculation2 的变异体覆盖 per SC-002/T058/R7 (partial). 目标：kill ≥8/15，Cumulative branch 13/18 → ≥14，全部归类。行为清单：getInstance 既有/冲突返回、check/formatStr 的正步长与 start>end 边界、BooleanCalculation2 比较与 null 字面量、BracketsCalculation2 explain 日志与缓存；将 BracketsCalculation.calculation2 的 @Deprecated 分支记为低价值/等价并写明理由。
- [X] T067 补 ExpressionExamProgressService、StrUtils、NumberCalculation、MathematicalExpressionEvaluator 与零散类的变异体覆盖 per SC-002/T058/R7 (partial). 目标：kill ≥10/29，ExpressionExamProgressService branch 18/20 → 20，全部归类。行为清单：回写 attempts+1、非 INTEGRITY 失败不置 plagiarismFlag、StrUtils 边界字符、NumberCalculation 剩余 check 条件、Evaluator null/blank/非法输入、record/enum 生成方法等价登记。
- [X] T068 强化弱断言并集中魔法常量 per FR-003/FR-009/Constitution II/III (contradicts). 目标：0 个弱断言唯一测试；把 60/100/300/0.80/0.75/0.25/1.60 抽为具名常量；不新增重复脚手架。必须修复：packExpressionDismantlingReturnsUnderlying 仅 assertNotNull；checkAcceptsValidExpressions、validationAcceptsBoundaryValues、snapshotValidationAcceptsBoundaryValues 仅 assertDoesNotThrow 的测试；为这些测试补具体返回值/状态码/报告字段断言。
- [X] T069 压缩测试方法数至 ≤160 并保持覆盖与变异率 per plan R8/T059 (partial). 目标：方法数 242 → ≤160；branch ≥92%；PIT kill ≥85%；套件 ≤15s。按被测类合并相邻同类行为，优先参数化与 assertAll，不删除行为用例、不降低现有断言强度。
