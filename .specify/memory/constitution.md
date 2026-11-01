<!--
Sync Impact Report
==================
- Version change: 1.0.0 → 2.0.0（MAJOR：治理目标由“可运行业务系统”重定义为“测试竞赛交付”）
- Modified principles:
  - I. 测试交付优先 → I. 覆盖率即一等指标
- Added principles:
  - II. 变异可杀死性
  - III. 单文件可读性
  - IV. 运行效率
  - V. 确定性与隔离（NON-NEGOTIABLE）
  - VI. 只测不改的交付边界
- Removed principles:
  - II. 十进制精确算术（降级并入“技术约束”的受测契约）
  - III. 不可变领域模型与边界校验（降级并入“技术约束”的受测契约）
  - IV. 可注入依赖与测试可替换性（降级并入“技术约束”的受测契约）
  - V. 质量门禁（由 II–VI 与“工作流与质量门禁”承接）
- Modified sections:
  - 技术约束（重写并扩展：交付物与评分模型、技术栈、覆盖率基线、受测契约、环境边界）
  - 开发工作流与质量门禁 → 工作流与质量门禁（重命名并重写）
- Added sections: 无
- Removed sections:
  - 原“技术约束”中的依赖治理与打包结构条款
  - 原“开发工作流与质量门禁”中的开发顺序与提交要求条款
- Follow-up TODOs: 无

说明：本报告为临时审阅材料，不属于治理内容，提交前应移除。
-->

# Online Math Expression Grading 测试竞赛交付宪法

## Core Principles

### I. 覆盖率即一等指标

分支覆盖率占总分 40%，是本项目的第一验收指标。

- 本条中的“分支条件”指 JaCoCo 计入 `BRANCH` 的 `if`、`switch`、三元表达式与布尔短路条件。
- 每个被评分的可达分支条件必须同时具备真、假两个方向的用例；取值型条件必须覆盖边界值、
  边界 ±1，以及空值或缺省值分支（当该分支可达时）。禁止只覆盖快乐路径。
- 覆盖率必须以 JaCoCo 的 `BRANCH_COVERED / (BRANCH_MISSED + BRANCH_COVERED)` 计算，
  不得以行覆盖率、断言数量或测试方法数量替代。
- 新增或修改的每个测试必须写明它覆盖的类与分支条件；无法对应到具体分支的测试不计入
  覆盖率贡献。
- 分支缺口必须按 `target/site/jacoco/jacoco.csv` 的分支总数从大到小处理；基线见
  “技术约束”中的“覆盖率基线”。

理由：40% 的评分权重直接由 JaCoCo 分支计数结算，覆盖率是唯一客观的分数来源。

### II. 变异可杀死性

变异杀死率占总分 30%，断言强度以“能否杀死变异体”判定。

- 断言必须落在行为契约上：具体返回值、具体异常类型、具体枚举常量、具体失败码或告警码。
  禁止以 `assertNotNull`、`assertTrue(x != null)`、`assertDoesNotThrow` 作为唯一断言；
  它们只能作为附加断言。
- 数值比较必须使用 `compareTo(...) == 0` 或 AssertJ `isEqualByComparingTo`；禁止用
  `BigDecimal.equals` 或 JUnit `assertEquals(BigDecimal, BigDecimal)` 判定数值相等，
  因为二者受 scale 影响。
- 枚举、状态与稳定字符串码必须使用精确相等；只有当异常消息文本不稳定时，才可以使用
  `startsWith` 或 `contains`。
- 每个存活变异体必须归类为“缺输入”“缺断言”“等价变异”之一。前两类必须补测；第三类
  必须逐条记录等价理由。
- 变异结果以 `target/pit-reports` 为准；工具配置固定为 PIT 1.15.3、
  `pitest-junit5-plugin` 1.2.1、`targetClasses=net.mooctest.*`、`mutators=DEFAULTS`。

理由：30% 的评分权重由变异杀死率结算，弱断言无法杀死变异体。

### III. 单文件可读性

可读性与可维护性占总分 20%，全部测试必须集中在一个文件中。

- 全部测试必须写在唯一文件 `src/test/java/net/mooctest/StudentTest.java` 中，并按被测类
  组织为 `@Nested` 内部类；每个 `@Nested` 类必须以其被测类命名。
- 测试方法名必须读作一句行为描述，例如 `returnsUnansweredWhenSubmittedExpressionIsBlank`；
  禁止 `test1`、`testCaseA` 之类编号命名。
- 魔法数字与魔法字符串必须提取为具名常量或具名工厂方法；同一个值在文件中出现两次以上
  必须提取。
- 领域对象必须通过 `StudentTest` 内的 `private static` 工厂方法构造，例如 `answer(...)`、
  `submission(...)`、`lesson(...)`；禁止直接写出带 19 个位置参数的 `new`。
- 注释只解释“为什么这样测”或“这个边界来自哪里”，不得复述代码；删除注释后测试的语义
  必须保持不变。

理由：20% 的评分权重由单文件可读性与可维护性结算，结构性重复和裸构造会直接扣分。

### IV. 运行效率

脚本运行效率占总分 10%，测试套件必须快速且不引入外部资源。

- 禁止 `@SpringBootTest` 或任何 Spring 上下文启动；禁止 `Thread.sleep`；禁止网络、
  文件系统、数据库 IO；禁止构造大规模笛卡尔积参数化。
- 全套 `mvn test` 必须 ≤ 15 秒（硬上限）；单个测试方法的执行时间必须 ≤ 300 毫秒。
- 同一行为的多组边界输入必须使用 `@ParameterizedTest` 表达；单个参数化方法的数据行
  必须 ≤ 12；禁止使用 `@RepeatedTest` 刷覆盖率。
- 理由：10% 的评分权重由脚本运行效率结算，慢测试同时会掩盖分支缺口。

### V. 确定性与隔离（NON-NEGOTIABLE）

测试结果必须可复现，且不得受执行顺序或静态状态影响。

- 时间必须通过 `Clock.fixed` 注入，禁止在测试中裸调用 `Instant.now()`、
  `LocalDateTime.now()` 或 `new Date()`。
- 禁止使用随机数（`Random`、`ThreadLocalRandom`、`Math.random()`），禁止依赖测试执行
  顺序，禁止依赖静态状态的初始值。
- 已知全局状态必须显式隔离：
  - `Mathematical_Expression.Options` 的 `useCache`、`useBigDecimal` 与两个静态缓存
    Map 必须在每个测试后通过 `setUseCache(false)` 复位；该调用是当前唯一可用的清空入口。
  - `CalculationManagement` 是静态注册表且没有清除 API；测试必须使用唯一组件名实现
    隔离，并必须显式固定“同名复用”与“同名类型冲突抛 `ExtractException`”两种语义。
- 安全禁令：`CumulativeCalculation` 的步长必须大于 0；禁止传入 0 或负数（会导致
  死循环）；此类输入不得进入测试套件。
- 理由：静态缓存与静态注册表会跨测试泄漏状态，确定性是覆盖率与变异结果可复现的前提。

### VI. 只测不改的交付边界

唯一评分对象是 `StudentTest.java`，任何源码或工程修改都会使交付无效。

- 不得修改 `src/main` 下任何文件、`pom.xml`、依赖或插件配置；不得新增运行时或测试依赖；
  不得新增测试文件。辅助类型只能是 `StudentTest` 内的静态嵌套类或私有静态方法。
- 包名 `net.mooctest` 与顶层类名 `StudentTest` 不可更改；不得使用 `@Disabled` 或
  `@Ignore`；不得提交只打印不断言的“测试”。
- 发现 `src/main` 的可疑行为（例如空字符串触发 `StringIndexOutOfBounds`、不可达
  `default` 分支）时，只允许按现状钉住行为（`assertThrows` 或断言当前返回值），不得
  顺手修复；修复不属于本赛题。
- 每次提交必须附证据：分支覆盖率、变异杀死率、`mvn test` 套件耗时三个数字，以及
  “未覆盖/未杀死”清单。
- 理由：评分只读取 `StudentTest.java` 中的测试方法，修改源码或工程会改变评分基线。

## 技术约束

**交付物与评分模型（只读事实）**

- 本项目的交付物不是产品功能；唯一评分对象是
  `src/test/java/net/mooctest/StudentTest.java` 一个文件中的 JUnit 5 测试方法。
- 评分权重固定为：分支覆盖率 40% + 变异杀死率 30% + 可读性与可维护性 20% +
  脚本运行效率 10%。
- 只读边界：不得修改 `src/main` 下任何文件、`pom.xml`、依赖与插件配置；不得新增运行时
  或测试依赖；不得新增测试文件。
- 命名契约：package `net.mooctest` 与顶层类名 `StudentTest` 不可更改；不得使用
  `@Disabled`/`@Ignore`；不得提交只打印不断言的“测试”。

**技术栈（只读事实）**

- Java 21；Maven 3.9.6 及以上；Spring Boot 3.5.16 父工程仅作为依赖版本来源。
- JUnit Jupiter 5.10.2；Mockito 5.12.0。
- JaCoCo 0.8.11；PIT 1.15.3（`pitest-junit5-plugin` 1.2.1，
  `targetClasses=net.mooctest.*`，`DEFAULTS` mutators）。
- Clover 4.5.2（仅生成 XML）；Checkstyle 3.3.1（仅扫描 `src/test/java`）。
- 测试只能使用 pom 既有依赖；不得通过新增依赖引入断言库、测试框架或工具。

**覆盖率基线（只读事实）**

- 基线来源：`target/site/jacoco/jacoco.csv`。
- 全局分支：1,072 个，已覆盖 43 个（4.0%）。
- 全局指令：7,287 条，已覆盖 633 条（8.7%）。
- 业务服务分支总数：`net.mooctest.grading.service.ExamGradingService` 344；
  `net.mooctest.progress.service.LearningProgressService` 263；
  `net.mooctest.progress.service.ExpressionExamProgressService` 20。
- 引擎分支总数：`net.mooctest.expression.engine.PrefixExpression` 76；
  `PrefixExpressionOperation` 48；`NumberUtils` 46；`StrUtils` 42；
  `ArithmeticExpression` 38（已覆盖 27）。
- 上述数字是每次提交计算增量的唯一基线，不得用其他统计口径替换。

**受测契约（继承自 v1.0.0 且仍然成立）**

以下条款只描述必须被测试钉住的既有可观察行为，不是可修改的实现要求。

- 十进制精确算术：分数、容差与表达式结果以 `BigDecimal` 呈现；等值判定必须使用
  `compareTo(...) == 0`，容差判定必须使用 `abs().compareTo(tolerance) <= 0`；不得用
  `equals` 或 JUnit `assertEquals` 判定数值。
- 不可变领域模型：`grading.domain` 与 `progress.domain` 的 record 对 `null` 集合回退为
  `List.of()`/`Map.of()`，并以 `List.copyOf`/`Map.copyOf` 做防御性拷贝；测试必须断言
  这一可观察行为，不得要求修改实现。
- 构造函数注入：`ExpressionAnswerGradingService(ExpressionEvaluator)`、
  `LearningProgressService(Clock)`、`ExpressionExamProgressService(LearningProgressService)`
  是既有的测试替身入口；测试必须通过注入桩 `ExpressionEvaluator` 与 `Clock.fixed`
  驱动分支，不得要求新增注入点或修改构造器。
- 结果表达：评分结果使用 `ExpressionAnswerStatus` 枚举与稳定的 failures/warnings 码，
  例如 `MANDATORY_QUESTION_UNANSWERED:<id>`、`INVALID_STUDENT_EXPRESSION`；测试必须以
  精确相等断言这些值。
- 表达式引擎：`net.mooctest.expression.engine` 是只读上游；适配层位于
  `net.mooctest.grading.expression`，不得修改引擎行为。

**环境与边界**

- `@Service` 注解存在于 `ExamGradingService` 与 `LearningProgressService`，但测试禁止
  启动 Spring 上下文；必须直接实例化服务。
- 源码中的 `Instant.now(clock)` 与 `Clock` 构造参数是固定测试时间的唯一入口。
- 源码中的不可达分支或不可稳定断言的行为按 VI 处理，不得修改源码。

## 工作流与质量门禁

**工作流（每次变更按顺序执行）**

1. 按被测类列出分支清单：从 `target/site/jacoco/jacoco.csv` 读取分支总数与已覆盖数，
   从源码定位分支条件，记录“类 → 分支条件 → 对应用例”。
2. 写失败或缺失的测试，确认其因覆盖不足或断言不足而失败。
3. 将断言驱动到“变异可杀”：使用具体返回值、异常类型、枚举常量与稳定码。
4. 运行 JaCoCo 与 PIT，读取 `target/site/jacoco/jacoco.csv` 与 `target/pit-reports`，
   记录新增覆盖与存活变异体。
5. 对“缺输入”“缺断言”类存活变异体补测；对“等价变异”逐条记录理由。
6. 在提交说明中记录分支覆盖率、变异杀死率、套件耗时与“未覆盖/未杀死”清单。

**固定验证命令**

```bash
mvn -q test
mvn -q org.jacoco:jacoco-maven-plugin:report
mvn -q org.pitest:pitest-maven:mutationCoverage
```

- 覆盖率证据从 `target/site/jacoco/jacoco.csv` 读取。
- 变异证据从 `target/pit-reports` 读取。
- 不得使用其他命令替代上述三项验证。

**质量门禁（全部满足才可交付）**

- `mvn test` 通过，且不得通过删除或禁用测试使门禁通过。
- 新增或修改的每个可达分支条件都有对应用例。
- 没有以 `assertNotNull`、`assertTrue(x != null)` 或 `assertDoesNotThrow` 作为唯一断言
  的测试。
- 没有 `@Disabled`/`@Ignore`，没有只打印不断言的测试。
- 全套 `mvn test` 耗时 ≤ 15 秒；单个测试方法耗时 ≤ 300 毫秒。
- `src/test/java` 通过 Checkstyle。
- 提交证据包含分支覆盖率、变异杀死率、套件耗时与“未覆盖/未杀死”清单。

## Governance

本宪法优先于个人偏好、既有习惯与临时约定。

修正程序：任何修正必须提交书面提案，包含变更条款、理由、对评分基线的影响与迁移计划；
经至少一名维护者批准后生效。生效时必须同步更新版本号、`Ratified` 与 `Last Amended`
日期，并在 Sync Impact Report 中记录变更。

版本策略：采用 `MAJOR.MINOR.PATCH` 语义化版本。

- `MAJOR`：重新定义治理目标、评分权重、唯一评分对象或交付边界。
- `MINOR`：新增或删除原则、新增章节、对既有条款作实质性扩展。
- `PATCH`：不改变语义的措辞澄清、错别字修正与格式调整。

合规评审：每次 spec、plan、tasks 与评审都必须执行 Constitution Check；无法解释的违反
阻断交付。所有例外必须书面记录理由、风险与移除时限。

Ratified 日期保持 2026-11-01，不因本次修订改变。

**Version**: 2.0.0 | **Ratified**: 2026-11-01 | **Last Amended**: 2026-11-01
