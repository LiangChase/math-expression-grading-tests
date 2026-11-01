# MoocTest 数学表达式评分 — 单文件 JUnit 5 测试套件

本项目是 **MoocTest 软件测试大赛**题目的参赛实现。被测系统是一套在线数学表达式考试评分服务，包含数学表达式引擎、考试评分与学习进度三个部分。

赛题的核心要求是：**不修改产品代码，只在唯一文件 `src/test/java/net/mooctest/StudentTest.java` 中交付 JUnit 5 测试套件**，并用分支覆盖率、变异杀死率、可读性与运行效率四个维度评分。

## 评分目标

| 维度 | 权重 | 我们的目标 |
|------|------|------------|
| 分支覆盖率 | 40% | 最大化可达分支覆盖 |
| 变异杀死率 | 30% | 用行为级断言杀死变异体 |
| 可读性与可维护性 | 20% | 按被测类组织、具名工厂、集中常量 |
| 脚本运行效率 | 10% | 全套测试远低于 15 秒 |

## 我们做了什么

1. **只读分析生产代码**：通读 `src/main` 中的表达式引擎、评分服务、进度服务与领域模型，逐类确认公开入口、分支条件和异常路径。
2. **只改一个测试文件**：所有测试集中在 `src/test/java/net/mooctest/StudentTest.java`，未修改 `src/main`、`pom.xml`、依赖或插件配置。
3. **按被测类组织测试**：使用 58 个 `@Nested` 测试组，按 `ExpressionAnswerGradingService`、`ExamGradingService`、`LearningProgressService`、`ExpressionExamProgressService`、表达式引擎与端到端场景分组。
4. **具名夹具与 Builder**：所有长参数 record 通过 `Builder`/具名工厂构造，避免 19 个位置参数的裸 `new`；金额、阈值、时钟、卡号样例等收敛为具名常量。
5. **确定性优先**：时间相关测试使用 `Clock.fixed`；每个行为前重置静态缓存与开关；不使用 `Thread.sleep`、随机数、网络或文件 IO。
6. **用 JaCoCo/PIT 迭代**：先建立基线，再按分支缺口补测；用 PIT 导出存活变异体，按“缺输入 / 缺断言 / 等价”分类并记录在测试文件末尾。
7. **Spec Kit 过程留痕**：从 `spec.md`、`plan.md`、`research.md`、`data-model.md`、`contracts/`、`tasks.md` 到 `converge` 收敛，完整保留了需求、设计、任务与验收证据。

## 实测结果

**基线**：分支 43 / 1,072（4.0%）  
**最终**：分支 1,010 / 1,072（94.22%）

| 指标 | 基线 | 最终 |
|------|------|------|
| 分支覆盖 | 43 / 1,072（4.0%） | **1,010 / 1,072（94.22%）** |
| 指令覆盖 | 633 / 7,287（8.7%） | **7,099 / 7,287（97.42%）** |
| PIT killed | — | **919** |
| PIT survived | — | **52** |
| PIT no coverage | — | **18** |
| PIT timed out | — | **3** |
| PIT 杀死率（killed / (killed + survived)） | — | **94.6%** |
| 测试执行 | 1 个示例 | **59 个用例，0 失败 / 0 错误 / 0 跳过** |
| 测试方法数 | 1 | **51** |
| 全套耗时 | — | **约 8.7 秒** |

### 核心模块覆盖

| 类 | 已覆盖分支 / 分支总数 | 覆盖率 |
|----|------------------------|--------|
| `ExamGradingService` | 323 / 344 | 93.90% |
| `LearningProgressService` | 248 / 263 | 94.30% |
| `ExpressionExamProgressService` | 20 / 20 | 100.00% |
| `PrefixExpression` | 74 / 76 | 97.37% |
| `PrefixExpressionOperation` | 46 / 48 | 95.83% |
| `NumberUtils` | 45 / 46 | 97.83% |
| `StrUtils` | 42 / 42 | 100.00% |
| `ArithmeticExpression` | 38 / 38 | 100.00% |
| `BracketsExpression` | 21 / 22 | 95.45% |
| `BracketsCalculation2` | 17 / 20 | 85.00% |
| `CumulativeCalculation` | 14 / 18 | 77.78% |

> 全局覆盖率与服务层三大类均达到 85% 以上；`CumulativeCalculation` 低于单类 85%，但其任务预算目标已达成，且总分不受单类阈值阻断。

## 变异测试

PIT 使用 `DEFAULTS` mutators，`targetClasses=net.mooctest.*`、`targetTests=net.mooctest.*`、HTML+XML 报告，`timestampedReports=false`。

- 生成：**992** 个变异体
- 杀死：**919**
- 存活：**52**
- 无覆盖：**18**
- 超时：**3**（`MathMutator` 单次变异执行超时；测试套件本身无挂起）
- 严格杀死率：**94.6%**
- 按生成数计算的杀死率：**92.6%**

剩余 52 个存活变异体已在 `StudentTest.java` 末尾的 survivor ledger 中登记并分类：

- **37 个**：等价 / 不可达 / 低价值，例如冗余 `skipSpaces`、`StringBuilder` 容量计算、错误消息文本、日志元数据、不可达的精度转换分支、边界同值条件。
- **15 个**：缺输入 / 缺断言，作为超出 85% 目标后的已接受残留。
- `LessonType` switch default、record/enum 生成方法、`BracketsCalculation.calculation2` 的 `@Deprecated` 分支已注明不计入目标。

## 测试套件结构

- **58 个 `@Nested` 测试组**：按被测类与规则簇组织。
- **51 个测试方法**：原有测试用例通过 `allBehaviors()` 聚合器合并为 48 个聚合方法；聚合器调用私有行为方法，并在每个行为前执行 `resetStaticState()`，保持静态缓存与开关隔离。
- **具名 Builder**：`AnswerBuilder`、`SubmissionBuilder`、`PolicyBuilder`、`LessonBuilder`、`SnapshotBuilder`、`ExprExamBuilder` 等，统一处理长参数夹具。
- **集中常量**：金额阈值、百分比、迟到/宽限秒数、折扣因子、固定时钟等。
- **survivor ledger**：文件末尾记录所有存活变异体的分类与理由。

## 复现方式

```bash
# 1. 运行全部测试
mvn -q test

# 2. 生成并读取 JaCoCo 分支覆盖
mvn -q org.jacoco:jacoco-maven-plugin:report
# 读取 target/site/jacoco/jacoco.csv

# 3. 运行 PIT 变异测试
mvn -q org.pitest:pitest-maven:mutationCoverage
# 读取 target/pit-reports/mutation.xml 与 index.html
```

连续两次运行应保持一致：**59 个用例，0 失败 / 0 错误 / 0 跳过**，耗时约 **8.6–8.7 秒**。

## 仓库结构

```text
src/
├── main/java/net/mooctest/          # 只读被测系统（未修改）
│   ├── expression/engine/           # 数学表达式引擎
│   ├── grading/                     # 考试评分
│   └── progress/                    # 学习进度
└── test/java/net/mooctest/
    └── StudentTest.java             # 唯一测试交付物

specs/001-competition-test-suite/    # 需求、计划、研究、数据模型、契约、任务
.specify/memory/constitution.md      # 项目宪法
README.md                            # 本说明
```

## 已知限制

- **Checkstyle**：默认规则要求单文件不超过 2,000 行，且需要 `package-info.java`；当前测试文件为 5,349 行。满足这些规则需要拆分单文件或新建文件，均与赛题硬约束冲突，因此未修改 `pom.xml` 或 Checkstyle 配置。若评分执行 Checkstyle，需要评审方放宽该规则。
- **git 校验**：本仓库不包含 `src/main` 的修改；测试开发过程中未写入 `src/main`、`pom.xml`、依赖或插件。若需要 `git diff` 证据，请以仓库中 `src/main` 的提交历史为准。
- **PIT 超时**：3 个 `MathMutator` 在单次变异执行中超时；这不是测试套件挂起，套件本身连续运行稳定。

## 说明

本仓库中的测试代码用于 MoocTest 软件测试大赛的评分交付；生产代码保持只读。测试套件的目标是尽可能提高分支覆盖、变异杀死率、可读性与运行效率，同时严格保持“单文件、无新依赖、确定性执行”的约束。
