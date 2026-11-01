# Quickstart: 单文件竞赛测试套件

**Feature**: 001-competition-test-suite
**Date**: 2026-11-01
**Input**: [spec.md](./spec.md)、[plan.md](./plan.md)、[research.md](./research.md)、[contracts/test-matrix.md](./contracts/test-matrix.md)

## 目的

本指南用于在本地或评分环境验证测试套件是否满足交付标准：测试全部通过、分支覆盖率达到门槛、PIT 变异杀死率达到门槛、运行时间受控、结果可复跑，并且 `src/main` 与 `pom.xml` 没有改动。

## 前置检查

1. 确认 Java 与 Maven 版本：`java -version` 为 21，`mvn -version` 为 3.9.6 或更高。
2. 确认只存在一个测试文件：`src/test/java/net/mooctest/StudentTest.java`；不得新增其他测试文件。
3. 确认没有修改 `pom.xml`、`src/main`、依赖或插件配置。
4. 确认测试没有使用 `@Disabled`、`Thread.sleep`、网络、文件 IO、随机数或 Spring 上下文。

## 验证步骤

### 1. 运行测试

```bash
mvn -q test
```

期望结果：

- 失败 0、错误 0、跳过 0。
- Surefire 报告显示测试方法总数不超过 160。
- 全套耗时不超过 15 秒；目标不超过 10 秒。

### 2. 生成并读取覆盖率

```bash
mvn -q org.jacoco:jacoco-maven-plugin:report
```

读取 `target/site/jacoco/jacoco.csv`。每一行的分支覆盖率为：

```text
BRANCH_COVERED / (BRANCH_MISSED + BRANCH_COVERED)
```

全局覆盖率为所有行求和后的同一公式；不要使用行覆盖率或测试方法数替代。

### 3. 运行 PIT 变异测试

```bash
mvn -q org.pitest:pitest-maven:mutationCoverage
```

读取 `target/pit-reports/mutation.xml`，统计 `status="KILLED"` 与 `status="SURVIVED"` 的变异体数量：

```text
kill rate = killed / (killed + survived)
```

同时读取 `target/pit-reports/index.html` 作为人工复核入口。若 PIT 因继承 Surefire 的 JaCoCo agent 而报错，可在命令行覆盖：

```bash
mvn -q org.pitest:pitest-maven:mutationCoverage -DargLine=
```

不得为此修改 `pom.xml`。

### 4. 连续两次运行

按顺序重复第 1 至第 3 步两次，比较：

- 测试失败数、错误数、跳过数是否相同。
- `jacoco.csv` 的分支覆盖总数是否相同。
- `mutation.xml` 的 killed 与 survived 数量是否相同。
- 全套耗时是否在 15 秒以内且没有数量级波动。

### 5. 验证零源码改动

如果目录有 git 元数据：

```bash
git diff -- src/main pom.xml
```

期望输出为空。如果没有 git 元数据，使用基线校验和对比 `src/main` 与 `pom.xml`，期望所有文件的校验和与基线一致。

### 6. Checkstyle（仅在评分执行时）

```bash
mvn -q checkstyle:check
```

若默认规则对测试源码报错，只调整 `StudentTest.java` 的格式；不得修改 `pom.xml`、Checkstyle 配置或 `src/main`。如果评分环境不执行 Checkstyle，不把它作为本地阻断条件。

## 覆盖率对照记录表

| 指标 | 改前 | 目标 | 实际 | 是否达标 |
|------|------|------|------|----------|
| 分支已覆盖 | 43 | 912 | 待填写 | 待填写 |
| 分支总数 | 1,072 | 1,072 | 待填写 | 待填写 |
| 分支覆盖率 | 4.0% | 85%（目标 90%） | 待填写 | 待填写 |
| 指令已覆盖 | 633 | 仅作参考 | 待填写 | 不单独设门槛 |
| 指令总数 | 7,287 | 仅作参考 | 待填写 | 不单独设门槛 |

按类记录模板：

| 类 | 改前已覆盖 / 总数 | 目标已覆盖 | 改后已覆盖 / 总数 | 增量 |
|----|-------------------|------------|-------------------|------|
| `ExamGradingService` | 0 / 344 | 310 | 待填写 | 待填写 |
| `LearningProgressService` | 0 / 263 | 231 | 待填写 | 待填写 |
| `ExpressionExamProgressService` | 0 / 20 | 20 | 待填写 | 待填写 |
| `PrefixExpression` | 0 / 76 | 65 | 待填写 | 待填写 |
| `PrefixExpressionOperation` | 1 / 47 | 40 | 待填写 | 待填写 |
| `NumberUtils` | 0 / 46 | 44 | 待填写 | 待填写 |
| `StrUtils` | 0 / 42 | 40 | 待填写 | 待填写 |
| `ArithmeticExpression` | 27 / 38 | 38 | 待填写 | 待填写 |
| `BracketsExpression` | 0 / 22 | 20 | 待填写 | 待填写 |
| `BracketsCalculation2` | 1 / 19 | 17 | 待填写 | 待填写 |
| `CumulativeCalculation` | 0 / 18 | 14 | 待填写 | 待填写 |
| `BooleanCalculation2` | 0 / 12 | 11 | 待填写 | 待填写 |
| `NumberCalculation` | 0 / 31 | 28 | 待填写 | 待填写 |
| 零散类 | — / 94 | 75 | 待填写 | 待填写 |

说明：表格沿用规格基线；当前本地 `jacoco.csv` 对 `PrefixExpressionOperation` 与 `BracketsCalculation2` 可能显示 48 与 20，最终以实际报告分母为准，按同比例调整绝对目标。

## 变异结果记录表

| 指标 | 数值 |
|------|------|
| Killed | 待填写 |
| Survived | 待填写 |
| No coverage | 待填写 |
| 变异杀死率 | 待填写 |
| 目标 | 75%（目标 85%） |
| 存活变异体已分类比例 | 必须为 100% |

逐类记录模板：

| 类 | Killed | Survived | No coverage | 主要存活原因 | 处理状态 |
|----|--------|----------|-------------|--------------|----------|
| `ExamGradingService` | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| `LearningProgressService` | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| `ExpressionExamProgressService` | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| 引擎类 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

存活变异体记录模板：

| 类 | Mutator | 行号 | 分类 | 原因 | 对应测试或登记 |
|----|---------|------|------|------|----------------|
| 待填写 | 待填写 | 待填写 | 缺输入 / 缺断言 / 等价 | 待填写 | 待填写 |

## 稳定性检查

- 连续两次 `mvn test` 的失败、错误、跳过数相同。
- 连续两次 `jacoco.csv` 的分支覆盖总数相同。
- 连续两次 PIT 的 killed 与 survived 数量相同。
- 没有测试依赖执行顺序；打乱测试顺序或单独运行任一测试，结果不变。
- 没有测试读取真实系统时间；所有时间敏感测试都使用固定时钟。
- 没有测试依赖 `Map`、`Set` 的迭代顺序。

## 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| PIT 长时间不结束 | 区间累加步长为 0 或负数；存在无限循环；测试方法过多 | 立即移除非法步长输入；检查循环边界；把方法数控制在 160 以内 |
| 覆盖率不增长 | 测试只走快乐路径；缓存命中掩盖了未命中分支；断言未执行到目标分支 | 检查 `jacoco.csv` 的 `BRANCH_MISSED`；显式覆盖缓存开/关、precise/approximate 路径 |
| 测试偶发失败 | 全局静态状态未复位；真实时钟；随机；依赖执行顺序 | 按 `setUseCache(false)` → `setUseBigDecimal(false)` 复位；使用固定时钟；移除随机与顺序假设 |
| PIT 报 bytecode 或 agent 错误 | JaCoCo agent 与 PIT 同 JVM 冲突 | 分开运行 JaCoCo 与 PIT；必要时使用 `-DargLine=` 覆盖 |
| Checkstyle 报测试源码 | 默认规则与测试文件格式冲突 | 只修 `StudentTest.java` 的格式；不得改 `pom.xml` 或规则配置 |
| 栈溢出或超时 | 阶乘输入过大；大参数化笛卡尔积 | 阶乘只测 0/1/5/10 与负数；参数化数据行不超过 12 |
| `git diff` 不可用 | 环境没有 git 元数据 | 使用基线文件校验和对比替代 |

## 完成判定

- [ ] `mvn test` 失败 0、错误 0、跳过 0。
- [ ] 分支覆盖率 ≥ 85%（目标 90%）。
- [ ] PIT 变异杀死率 ≥ 75%（目标 85%）。
- [ ] 所有存活变异体已归类并登记。
- [ ] 全套耗时 ≤ 15 秒（目标 ≤ 10 秒）；单个测试 ≤ 300 毫秒。
- [ ] 没有 `@Disabled`、TODO、FIXME、只打印不断言的测试。
- [ ] `src/main` 与 `pom.xml` 改动为零。
- [ ] 连续两次运行结果一致。
- [ ] 覆盖率与变异记录表已填写完整。
