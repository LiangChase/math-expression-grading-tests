# Feature Specification: 单文件竞赛测试套件（评分最大化）

**Feature Branch**: `001-competition-test-suite`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "为单文件 JUnit 5 测试套件（竞赛评分最大化）生成特性规范；本特性不改产品代码，只交付测试。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 评分主链路判定正确 (Priority: P1)

参赛者提交一份表达式考试答卷后，评分方需要看到每题的五种判定状态，并把单题结果汇总为整场成绩报告。测试套件作为交付物，负责验证这条主链路的每个判定与汇总字段。

**Why this priority**: 这条链路覆盖 `grading.expression` 与 `ExamGradingService` 的 344+ 个分支，直接决定分支覆盖率 40% 与变异杀死率 30% 的主要得分。

**Independent Test**: 只交付 P1 的测试组，即可独立运行并产出“状态映射、整场汇总字段、失败/告警码”三类证据；无需 P2 的课程进度数据或 P4 的端到端串联。

**Acceptance Scenarios**:

1. **Given** 题目正确答案可求值且容差为 0，**When** 学生提交等价表达式，**Then** 单题状态为 EXACT，整场报告按满分计入该题。
2. **Given** 题目容差大于 0，**When** 学生答案与正确值的绝对误差恰好等于容差，**Then** 状态为 WITHIN_TOLERANCE；误差为容差加 0.01 时状态为 INCORRECT。
3. **Given** 学生提交空字符串、空白字符串或 null，**When** 评分，**Then** 状态为 UNANSWERED，answered=false，且不产生数值误差。
4. **Given** 学生提交语法非法或求值失败的表达式，**When** 评分，**Then** 状态为 INVALID_EXPRESSION，expectedValue 保留，actualValue 与 absoluteError 为空，并带有稳定错误码。
5. **Given** 题目配置的正确表达式本身不可求值，**When** 评分，**Then** 抛出表示“题目配置无效”的异常，而不是把学生答案判为错误。
6. **Given** 一组覆盖全部题型（单选、多选、判断、填空、数值、问答、编程）的作答，**When** 汇总为整场成绩，**Then** rawPoints、finalPoints、maximumPoints、percentage、letterGrade、passed、manualReview、sectionPercentages、failures、warnings 相互一致。
7. **Given** 百分比落在 60/65/70/75/80/85/90/95 分档边界，**When** 生成报告，**Then** 字母等级按固定分档映射，且 passed 仍由总分、分区、必修题与诚信规则共同决定。
8. **Given** 迟到、超时、attempt 超限、教师覆写或封顶开关被触发，**When** 汇总成绩，**Then** 调整分、百分比、失败码与告警码符合边界规则。

---

### User Story 2 - 学习进度与通过性判定 (Priority: P2)

课程运营者或学生需要用课程快照得到总体进度、完成课时数、课程完成、证书资格、下一课时、阻断项与告警项；并在表达式考试成绩定稿后，把成绩回写到对应 QUIZ 课时并重新评估。

**Why this priority**: 覆盖 `LearningProgressService` 263 个分支与 `ExpressionExamProgressService` 20 个分支，规则密集，且与 P1 共享成绩报告数据。

**Independent Test**: 只交付 P2 的测试组，即可用固定时钟和合成课程快照独立验证进度指标、证书资格与 QUIZ 回写；不依赖真实考试评分链路。

**Acceptance Scenarios**:

1. **Given** 包含视频、文章、测验、作业、直播、项目的课程快照，**When** 评估进度，**Then** 每种课时的完成判定、权重与已得权重都产生预期值，completedLessons 只统计已完成课时。
2. **Given** 顺序学习开启且前一必修课时未完成，**When** 后续课时已解锁，**Then** 产生顺序规则被绕过的告警，并把下一课时定位到第一个可访问且未完成的课时。
3. **Given** 必修课未完成、抄袭标记、尝试次数耗尽、锁定课程有进度或空课程，**Then** blockers 与 warnings 分别包含对应稳定码，courseCompleted 为 false。
4. **Given** 证书开关、身份验证、余额、证书名长度与最终项目状态的不同组合，**When** 评估证书资格，**Then** certificateEligible 与 blockers 按规则变化。
5. **Given** 一份没有 manualReview 的定稿成绩，**When** 回写到存在的 QUIZ 课时，**Then** 该课时完成度为 100、分数等于报告百分比、attempts 加一、submitted=true，并用更新后的快照重新评估。
6. **Given** 成绩仍处于 manualReview、课时不存在、同一 QUIZ 重复出现或目标课时不是 QUIZ，**When** 回写，**Then** 请求被拒绝并给出可区分的错误。
7. **Given** 成绩的 failures 含 INTEGRITY 失败，**When** 回写，**Then** 目标课时被标记为抄袭，重新评估时产生抄袭阻断且课程不能完成。

---

### User Story 3 - 表达式引擎数值与格式契约 (Priority: P3)

测试套件需要验证只读表达式引擎对括号、优先级、正负号、阶乘、取余、幂、比较、区间累加、格式化与缓存的行为，确保 P1 使用的真实求值器可信。

**Why this priority**: 引擎侧约 400 个分支，是分支覆盖率的第二大来源；同时为 P1 提供真实求值路径，避免评分测试只依赖桩实现。

**Independent Test**: 只交付 P3 的测试组，即可直接对引擎的数值与格式契约取证；不涉及考试评分与课程进度。

**Acceptance Scenarios**:

1. **Given** 带括号、嵌套括号、不同优先级与同优先级的表达式，**When** 求值，**Then** 结果符合数学优先级与左结合规则。
2. **Given** 正负号、阶乘、取余、幂等运算，**When** 求值，**Then** 结果与边界行为符合引擎契约；负数阶乘被拒绝。
3. **Given** 比较表达式（小于、大于、小于等于、大于等于、等于、不等于、null 字面量），**When** 求值，**Then** 返回正确的布尔结果。
4. **Given** 区间累加表达式且步长大于 0，**When** 求值，**Then** 结果等于区间内各项之和；步长为 0 或负数的输入不得进入套件。
5. **Given** 缓存开关或 BigDecimal 开关打开或关闭，**When** 重复求值同一表达式，**Then** 数值结果保持一致，且全局状态在测试之间被复位。
6. **Given** 同一组件名重复注册、同名不同类型注册或强制覆盖注册，**When** 访问注册表，**Then** 返回既有实例或抛出类型冲突异常，注册结果符合复用与冲突契约。
7. **Given** 空、空白、非法或括号不匹配的表达式，**When** 经过格式检查或求值，**Then** 被拒绝并产生对应异常，不返回数值。

---

### User Story 4 - 端到端与一致性 (Priority: P4)

评分方需要确认从“表达式题目 + 学生作答”到“成绩报告 + 进度快照”的整条路径在跨服务边界时仍然一致，且重复运行得到相同结果。

**Why this priority**: 端到端测试把 P1 到 P3 的行为组合起来，能发现单服务测试看不到的数据传递、不变量与状态泄漏问题。

**Independent Test**: 只交付 P4 的测试组，即可用真实求值器、固定时钟和合成课程快照完成一次完整旅程，并断言最终报告与进度结果。

**Acceptance Scenarios**:

1. **Given** 一份包含正确、容差内、错误、未作答与非法作答的完整考试，**When** 端到端评分，**Then** 报告中的每题结果与独立评分结果一致，分区百分比与总分自洽。
2. **Given** 一份已定稿且无 manualReview 的成绩，**When** 回写到 QUIZ 并重新评估进度，**Then** 课时分数、完成状态、下一课时与阻断项随成绩变化。
3. **Given** 成绩含诚信违规，**When** 端到端回写并评估，**Then** 课时被标记抄袭，课程完成与证书资格按规则被阻断。
4. **Given** 固定时钟与相同输入，**When** 连续两次执行完整旅程，**Then** 两次报告与进度结果逐字段相同。

---

### Edge Cases

**表达式评分与作答**

- 空字符串、空白字符串或 null 作答：状态为 UNANSWERED，answered=false，message 为 ANSWER_NOT_PROVIDED，expectedValue 与 actualValue 为空。
- null 提交或 null 评分策略：立即抛出 NullPointerException，不产生部分报告。
- 题目正确答案不可求值：抛出 IllegalStateException，异常原因指向题目配置错误；不得把学生答案判为 INCORRECT。
- 学生表达式语法错误或求值失败：状态为 INVALID_EXPRESSION，expectedValue 保留，actualValue 与 absoluteError 为空，并带 INVALID_STUDENT_EXPRESSION 码。
- 除零：double 路径按浮点语义得到 Infinity 或 NaN；BigDecimal 路径抛出 ArithmeticException；经求值器统一表现为 ExpressionEvaluationException，作为学生作答时映射为 INVALID_EXPRESSION。
- 负数阶乘：直接调用引擎抛出 UnsupportedOperationException；经求值器表现为 ExpressionEvaluationException；当前实现中 1! 返回 1，0! 返回 0，属于必须按现状钉住的行为。
- 括号不匹配：格式检查路径抛出 WrongFormat；简易十进制解析路径抛出 IllegalArgumentException；两者都不得返回数值。
- 多余运算符：格式检查路径抛出 WrongFormat；简易十进制解析路径抛出 IllegalArgumentException。
- 同优先级与不同优先级：同优先级按左结合；乘、除、取余、幂的优先级高于加、减；幂与乘除同级并按左结合处理。
- 重复 questionId：评分入口抛出 IllegalArgumentException。
- 分区名空白：评分入口抛出 IllegalArgumentException。
- 分区百分比小于 0 或大于 100：评分入口或策略校验抛出 IllegalArgumentException。
- 满分等于 0 或为负：题目构造或评分校验阶段抛出 IllegalArgumentException，不得进入评分。
- 成绩封顶开关打开：百分比不超过 100，调整分不超过满分；开关关闭：百分比与调整分可以超过 100 和满分。
- 教师覆写百分比为 0、100 或越界：0 与 100 合法并分别得到 0 分与满分；越界抛出 IllegalArgumentException；覆写会追加 INSTRUCTOR_OVERRIDE_APPLIED 告警并参与通过判定。
- 迟到 1 秒、59 分、60 分：均至少按 1 小时计；61 分按 2 小时计；扣分达到 100% 时记录 SUBMISSION_TOO_LATE 失败。
- 考试时长宽限 60 秒与 300 秒：超时不超过 60 秒只告警；超过 60 秒且不超过 300 秒按 0.95 计分；超过 300 秒按 0.80 计分并记录 EXAM_DURATION_EXCEEDED；允许时长为 0 时不做时长检查。
- attempt 等于上限：记录 FINAL_ALLOWED_ATTEMPT 告警；attempt 超过上限：记录 MAXIMUM_ATTEMPTS_EXCEEDED 失败且调整分为 0。
- 身份未验证且策略要求：记录 IDENTITY_NOT_VERIFIED 失败；策略不要求：记录同名告警。
- 诚信违规与 failOnIntegrityViolation：开关打开时诚信失败阻断通过；关闭时诚信失败不阻断通过，但抄袭仍触发 manualReview。
- 全屏退出 1 次、2 次、3 次及以上：前两者为告警；3 次及以上为失败并触发 manualReview。
- 复制粘贴 4 次、5 次及以上：4 次为告警；5 次及以上为失败并触发 manualReview；0 次不产生记录。
- 监考断连：记录 PROCTOR_CONNECTION_LOST 告警并触发 manualReview。
- 未作答惩罚：惩罚率大于 0 时扣为负分并记录 UNANSWERED_PENALTY；惩罚率为 0 时为 0 分；该惩罚不受负分标记开关影响（现状契约）。
- 部分给分开关：开启时多选、填空、数值、编程按既定比例给分；关闭时未完全正确的作答为 0 分，开启负分标记时按罚分率扣为负分。
- 负分标记开关：开启时错误作答可为负分；关闭时已作答的负分被截断为 0，未作答惩罚路径按上一条执行。

**学习进度与课程状态**

- 课程未发布、账号停用、订阅失效、行为准则未接受：分别产生 COURSE_NOT_PUBLISHED、ACCOUNT_SUSPENDED、SUBSCRIPTION_INACTIVE、CODE_OF_CONDUCT_NOT_ACCEPTED 阻断，courseCompleted 为 false。
- 报名时间在未来：产生 ENROLLMENT_DATE_IN_FUTURE 阻断；等于当前时刻不算未来。
- 访问已过期：产生 COURSE_ACCESS_EXPIRED 阻断；未过期且剩余整日数不超过 7 天：产生 COURSE_ACCESS_EXPIRES_SOON 告警；等于到期时刻不算过期但会触发即将到期告警。
- 空课程：产生 COURSE_HAS_NO_LESSONS 阻断，overallPercent 为 0，courseCompleted 为 false。
- 必修课未完成：产生 MANDATORY_LESSON_INCOMPLETE:<id> 阻断。
- 抄袭标记：产生 PLAGIARISM_REVIEW:<id> 阻断，课程不能完成。
- 尝试次数耗尽：当最大尝试次数大于 0 且已用次数达到上限且课时未完成时，产生 ATTEMPT_LIMIT_REACHED:<id> 阻断。
- 锁定课程有进度：产生 LOCKED_LESSON_HAS_PROGRESS:<id> 告警。
- 顺序学习被绕过：当前一必修课时未完成而后继课时已解锁时，产生 SEQUENCE_RULE_BYPASSED:<id> 告警。
- 证书名缺失或长度非法：缺失或空白产生 CERTIFICATE_NAME_REQUIRED；长度小于 2 或大于 80 产生 CERTIFICATE_NAME_LENGTH_INVALID；certificateEligible 为 false。
- 最终项目未通过：产生 FINAL_PROJECT_NOT_APPROVED 阻断，courseCompleted 为 false。
- 教师审批缺失：产生 INSTRUCTOR_APPROVAL_REQUIRED 阻断，courseCompleted 为 false。

**QUIZ 回写**

- 成绩仍处于 manualReview：回写被拒绝并抛出 IllegalArgumentException。
- 课时不存在：回写被拒绝并抛出 IllegalArgumentException。
- 同一 QUIZ 课时重复出现：回写被拒绝并抛出 IllegalArgumentException。
- 目标课时类型不是 QUIZ：回写被拒绝并抛出 IllegalArgumentException。
- 成绩含 INTEGRITY 失败：目标课时 plagiarismFlag 置为 true，重新评估产生 PLAGIARISM_REVIEW 阻断且课程不能完成。

**表达式引擎与全局状态**

- 空字符串、空白字符串或 null 表达式：求值器入口抛出 ExpressionEvaluationException；简易解析器入口抛出 IllegalArgumentException；格式检查入口按现状抛出 WrongFormat 或 StringIndexOutOfBoundsException。
- 缓存开关打开或关闭：同一表达式重复求值结果一致；关闭缓存时清空两个静态缓存。
- BigDecimal 开关打开或关闭：同一表达式在 double 与 BigDecimal 路径下对可精确表示的值结果一致。
- 计算组件注册表同名复用：相同名称返回同一实例；同名不同类型抛出 ExtractException；重复注册且检查重复时返回 false 并保留既有实例，强制注册时覆盖既有实例并返回 true。
- 区间累加步长为 0 或负数：会死循环，属于禁止输入，不得进入测试套件；只测试步长大于 0 的区间。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 测试必须且只能存在于 `src/test/java/net/mooctest/StudentTest.java`；辅助类型必须位于该文件内部的静态嵌套类或私有静态方法；不得新增任何测试文件。
- **FR-002**: 测试不得修改 `src/main`、`pom.xml`、依赖或插件配置；发现可疑行为时必须按现状钉住，不得修复源码。
- **FR-003**: 每条功能需求必须映射到至少一个测试；每个测试必须至少包含一条行为级断言，断言对象为具体值、具体异常类型、具体状态枚举或具体失败/告警码；弱断言不得作为唯一断言。
- **FR-004**: 每个条件分支必须覆盖真、假两侧；数值边界必须覆盖临界值及正负 1，至少包括 60/65/70/75/80/85/90/95 分档、迟到 60/300 秒、7 天到期窗口、attempt 等于或超过上限。
- **FR-005**: 异常路径必须断言异常类型；只有当消息文本属于稳定契约（如 `MANDATORY_QUESTION_UNANSWERED:<id>`）时才断言文本。
- **FR-006**: 涉及时间的测试必须注入固定时钟；禁止随机、sleep、网络或文件 IO，禁止依赖测试执行顺序。
- **FR-007**: 测试必须隔离并显式覆盖全局静态状态，包括表达式求值的缓存开关、BigDecimal 开关，以及计算组件注册表的重复注册与类型冲突语义；测试之间不得互相污染。
- **FR-008**: 测试必须覆盖表达式求值器的两条路径（简易十进制解析器路径与通用编译器路径）和两条精度路径（double 与 BigDecimal），并覆盖空、空白、非法表达式。
- **FR-009**: 单文件结构必须可读：按被测类组织嵌套测试分组，测试方法名为行为描述，领域对象通过具名工厂构造，魔法值提取为常量，相关字段的断言聚合为一次整体校验。
- **FR-010**: 套件必须能在 PIT（DEFAULTS mutators、`targetClasses=net.mooctest.*`）下反复执行而不超时、不挂起；不得包含可能死循环的输入，区间累加步长必须大于 0。
- **FR-011**: 每个测试必须可解释其价值，说明服务的用户故事、覆盖的类与分支；禁止为凑数而写的重复测试。
- **FR-012**: 不得使用 `@Disabled`，不得遗留 TODO/FIXME，不得吞异常后不断言。

### Key Entities *(include if feature involves data)*

- **Test Case（测试用例）**: 一个可独立识别的行为验证单元，记录所属用户故事、被测类、覆盖的分支条件、预期行为与证据。
- **Test Suite（测试套件）**: 唯一测试文件内的测试集合，按被测类分组，受只读边界与命名契约约束。
- **Coverage Evidence（覆盖率证据）**: 每个类的分支总数、已覆盖数、改前与改后差值，用于证明覆盖率提升可归因。
- **Mutation Evidence（变异证据）**: 存活与杀死的变异体清单、每个存活变异体的分类与理由。
- **Grade Snapshot（评分快照）**: 单题与整场的评分结果，含状态、原始分、调整分、百分比、等级、通过标记、手工复核标记、分区百分比、失败码与告警码。
- **Progress Snapshot（进度快照）**: 课程与课时的完成度、证书资格、下一课时、阻断项与告警项。
- **Expression Contract（表达式契约）**: 引擎对括号、优先级、正负号、阶乘、取余、幂、比较、区间累加、格式化、缓存与注册表的预期行为。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 分支覆盖率达到 85% 以上，目标 90% 以上，并以 JaCoCo 的分支计数为唯一口径。
- **SC-002**: PIT 变异杀死率达到 75% 以上，目标 85% 以上；存活变异体必须 100% 归类为“缺输入”“缺断言”或“等价变异”。
- **SC-003**: 全套 `mvn test` 通过且耗时不超过 15 秒；不得出现超时、挂起或随机失败。
- **SC-004**: 测试结果为失败 0、错误 0、跳过 0；连续两次运行结果相同，CI 与本地结果一致。
- **SC-005**: 新读者能在 10 分钟内说出每个嵌套测试组测什么、具名工厂为什么这样设计；文件中不存在复制粘贴的大段脚手架。
- **SC-006**: `src/main` 与 `pom.xml` 的差异为空；若环境提供 git 元数据，以 `git diff` 证明，否则以基线文件校验和对比证明。
- **SC-007**: 覆盖率提升可按类归因，能够给出每个主要类的改前与改后分支覆盖数字。

## Assumptions

- 仓库为 `net.mooctest:online-math-expression-grading`，Java 21，Maven 单模块；`src/main` 共 53 个类，分为 `expression.engine`（只读上游数学引擎）、`grading.*`（考试评分）、`progress.*`（学习进度）。
- 交付物是唯一文件 `src/test/java/net/mooctest/StudentTest.java`（package `net.mooctest`，顶层类 `StudentTest`），目前只有 1 个测试方法。
- 评分权重固定为：分支覆盖率 40% + 变异杀死率 30% + 可读性与可维护性 20% + 脚本运行效率 10%。
- 基线：分支 1,072 个仅覆盖约 4%；指令 7,287 个仅覆盖约 8%。
- 分支分布用于排优先级：`ExamGradingService` 344、`LearningProgressService` 263、`ExpressionExamProgressService` 20、`PrefixExpression` 76、`PrefixExpressionOperation` 47、`NumberUtils` 46、`StrUtils` 42、`ArithmeticExpression` 38、`BracketsExpression` 22、`BracketsCalculation2` 19、`CumulativeCalculation` 18、`BooleanCalculation2` 12、`NumberCalculation` 31，其余零散。
- 评分环境离线；只有 pom 已有的 JUnit 5.10.2 与 Mockito 5.12.0 可用，不新增依赖。
- 覆盖率统计对象是 `src/main`（含 engine 包），不统计测试代码。
- PIT 使用 DEFAULTS mutators；record 与 enum 自动生成方法可能产生等价变异体，允许记录而不强杀。
- 可读性由人工或模型评审，偏好“结构清晰 + 断言有力”，不追求覆盖率数字的极端化。
- 现有 `StudentTest.java` 的示例测试可以被替换或保留，但类名、包名、文件名不变。
- 当前工作目录可能没有 git 元数据；此时 SC-006 以基线文件校验和对比作为等价证据，若评分环境提供 git 则以 `git diff` 为准。
