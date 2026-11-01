# Implementation Plan: 单文件竞赛测试套件（评分最大化）

**Branch**: `001-competition-test-suite` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-competition-test-suite/spec.md`

**Note**: This plan produces design artifacts only. Test code is implemented later through `/speckit-tasks` and `/speckit-implement`.

## Summary

交付一个只包含一个文件的 JUnit 5 测试套件，用于在竞赛评分中最大化分支覆盖率（40%）、变异杀死率（30%）、可读性（20%）与运行效率（10%）。测试全部收敛到 `src/test/java/net/mooctest/StudentTest.java`，按被测类组织嵌套测试组，使用具名工厂与具名常量构造夹具，依赖固定时钟与显式静态状态复位保证确定性。实施顺序为 P1 评分主链路、P2 学习进度、P3 表达式引擎、P4 端到端；计划 158 个测试方法，分支覆盖率目标 85% 以上，PIT 变异杀死率目标 75% 以上。

## Technical Context

**Language/Version**: Java 21（`maven.compiler.source/target=21`，UTF-8）

**Primary Dependencies**: 仅 pom 既有——JUnit Jupiter 5.10.2、Mockito 5.12.0（可选）、Spring Boot 3.5.16 parent（仅依赖管理，禁止启动 Spring 上下文）、varFormatter 与 algorithmStar（引擎内部使用，测试不直接依赖其 API）

**Storage**: 无存储

**Testing**: Maven Surefire 3.2.5（`failIfNoTests=true`，`argLine` 已挂 JaCoCo agent）、JaCoCo 0.8.11（report 绑 `test` 阶段，产物 `target/jacoco.exec` 与 `target/site/jacoco/jacoco.csv`）、PIT 1.15.3 + `pitest-junit5-plugin` 1.2.1（`targetClasses=net.mooctest.*`、`targetTests=net.mooctest.*`、`DEFAULTS` mutators、HTML+XML、`timestampedReports=false`）、Checkstyle 3.3.1（`sourceDirectories=src/test/java`）

**Target Platform**: JDK 21；Windows 与 Linux 均可

**Project Type**: 单模块 Maven 库，测试即交付物

**Performance Goals**: 全套 `mvn test` ≤ 15 秒（目标 ≤ 10 秒）；单个测试方法 ≤ 300 毫秒；测试方法总数 ≤ 160；单个参数化方法数据行 ≤ 12；必须能在 PIT 多变异体循环中重复执行

**Constraints**: 唯一代码产物 `src/test/java/net/mooctest/StudentTest.java`；不得修改 `src/main`、`pom.xml`、依赖或插件配置；离线；确定性；不得使用 sleep、网络或文件 IO、Spring 上下文

**Scale/Scope**: `src/main` 53 个类；基线 1,072 个分支、7,287 条指令；4 个用户故事；计划 158 个测试方法，覆盖评分、进度、引擎与端到端四层

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Verdict | Evidence |
|-----------|---------|----------|
| I. 覆盖率即一等指标 | PASS | 规格 SC-001；本计划 Branch Budget 以 `target/site/jacoco/jacoco.csv` 的 `BRANCH_COVERED / (BRANCH_MISSED + BRANCH_COVERED)` 为唯一口径，逐类分配 85% 以上目标 |
| II. 变异可杀死性 | PASS | 规格 SC-002；PIT 配置固定为 DEFAULTS、`targetClasses=net.mooctest.*`；research R6 规定行为级断言与存活变异体三角分类 |
| III. 单文件可读性 | PASS | 规格 FR-001 与 FR-009；本计划 Structure Decision 规定唯一测试文件、按被测类嵌套分组、具名工厂与具名常量；data-model.md 给出工厂契约 |
| IV. 运行效率 | PASS | 规格 FR-010 与 SC-003；本计划 Performance Goals 与 research R8 给出方法数 × 平均耗时预算，禁止上下文启动与 IO |
| V. 确定性与隔离 | PASS | 规格 FR-006 与 FR-007；research R2/R3 规定 `@AfterEach` 复位顺序、唯一组件名、固定时钟与禁止随机/sleep/顺序依赖 |
| VI. 只测不改的交付边界 | PASS | 规格 FR-002 与 SC-006；本计划只新增 `specs/001-competition-test-suite/` 下文档，代码阶段只写 `StudentTest.java`，不触碰 `src/main` 与 `pom.xml` |

## Project Structure

### Documentation (this feature)

```text
specs/001-competition-test-suite/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/
│   └── test-matrix.md   # Phase 1 output (/speckit-plan command)
├── checklists/
│   └── requirements.md  # Existing requirements-quality checklist
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
src/
├── main/
│   └── java/net/mooctest/
│       ├── expression/engine/   # 只读上游数学引擎
│       ├── grading/             # 考试评分（domain / expression / service）
│       └── progress/            # 学习进度（domain / service）
└── test/
    └── java/net/mooctest/
        └── StudentTest.java     # 唯一可写代码产物
```

**Structure Decision**: 采用“文档产物在 `specs/001-competition-test-suite/`，代码产物仅 `src/test/java/net/mooctest/StudentTest.java`”的双轨结构。所有测试夹具、嵌套测试组、具名工厂与常量都收敛到该文件内部；不新增测试文件，不修改 `src/main`、`pom.xml`、依赖或插件配置。`src/main` 仅作为只读被测对象，`src/test/java/net/mooctest/StudentTest.java` 是本特性唯一允许写入的代码文件。

## Complexity Tracking

No constitution violations were found before Phase 0 research. No complexity exceptions are requested; the table is intentionally empty because the single-file constraint already enforces the simplest viable structure.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | None | None |

## Milestones & Coverage Targets

| Milestone | Scope | Method Budget | Cumulative Branch Coverage | Cumulative Mutation Kill Rate |
|-----------|-------|---------------|----------------------------|------------------------------|
| P1 评分主链路 | `ExpressionAnswerGradingService`、`ExamGradingService`、评分领域夹具 | 70 | ≥ 30% | ≥ 65%（覆盖到的类） |
| P2 学习进度 | `LearningProgressService`、`ExpressionExamProgressService`、课程夹具 | 48 | ≥ 55% | ≥ 70%（覆盖到的类） |
| P3 表达式引擎 | `expression.engine` 的公开可达入口 | 34 | ≥ 80% | ≥ 75%（覆盖到的类） |
| P4 端到端 | 评分到进度回写的完整旅程 | 6 | ≥ 85%（目标 90%） | ≥ 75%（目标 85%） |

- 方法预算合计 158，低于 160 上限；参数化方法按 1 个方法计，数据行上限 12。
- 每阶段结束必须产出：JaCoCo 分支覆盖数、PIT 杀死率、套件耗时、未覆盖与未杀死清单。
- 阶段目标按全局 1,072 个分支计算；若实际 `jacoco.csv` 分母变化，按同名类同比例调整，但总目标不得低于 85%。

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| 静态缓存导致测试顺序依赖 | 偶发失败、PIT 结果漂移 | 每个测试后按 `setUseCache(false)` → `setUseBigDecimal(false)` 顺序复位；缓存相关测试显式验证命中与未命中 |
| 区间累加步长为 0 或负数 | 测试挂起、PIT 超时 | 测试矩阵禁止非法步长；只使用正步长与 `start == end` 等安全边界 |
| PIT 对 record/enum 生成方法产生等价变异 | 变异率无法进一步提升 | 三角分类为“等价变异”并逐条登记；不为自动生成方法强行补测 |
| Checkstyle 默认规则对测试源码报错 | 评分阶段门禁失败 | 只在测试文件内按规则调整格式；不得修改 `pom.xml` 或 Checkstyle 配置 |
| 单文件膨胀到不可读 | 可读性 20% 失分 | 按被测类 `@Nested` 分组；每组不超过 20 个方法；具名工厂与常量抑制重复 |
| PIT 与 Surefire 的 JaCoCo agent 冲突 | PIT 运行失败或变慢 | JaCoCo 与 PIT 分开执行；若 PIT 继承 agent，用命令行 `-DargLine=` 覆盖，不改 pom |
| 断言依赖不稳定消息文本 | 引擎升级或消息变化导致误报 | 引擎中文消息只断言异常类型；稳定业务码才做精确或前缀断言 |
| `Map.copyOf` 迭代顺序不确定 | 断言顺序导致偶发失败 | 只断言集合内容与大小，不假设 `Map` 或 `Set` 的迭代顺序 |

## Constitution Check (Post-Design)

*Re-check after Phase 1 design artifacts are generated.*

| Principle | Verdict | Evidence |
|-----------|---------|----------|
| I. 覆盖率即一等指标 | PASS | research.md 的 Branch Budget 逐类分配目标；contracts/test-matrix.md 为每个 SUT 给出分支对与预估测试数；quickstart.md 给出改前/改后对照表 |
| II. 变异可杀死性 | PASS | research.md R6 与“变异体三角处理流程”规定行为级断言、存活体分类与复跑闭环；contracts/test-matrix.md 为每行给出断言点 |
| III. 单文件可读性 | PASS | research.md R1 给出嵌套命名与分层约定；data-model.md 给出 10 类夹具的具名工厂、标准样本与变体；所有测试资产限定在 `StudentTest.java` |
| IV. 运行效率 | PASS | research.md R8 给出启动、方法数与平均耗时预算；quickstart.md 把 15 秒与 300 毫秒设为验收项；计划禁止 Spring 上下文、sleep 与 IO |
| V. 确定性与隔离 | PASS | research.md R2 规定复位顺序与唯一组件名；R3 规定固定时钟与时间边界；R5 禁止随机、顺序依赖与无限循环 |
| VI. 只测不改的交付边界 | PASS | 本计划文件清单只新增 `specs/001-competition-test-suite/` 下文档；后续代码阶段只写 `StudentTest.java`；quickstart.md 用 `git diff` 或基线校验和验证零源码改动 |

## Files to Create or Modify

| Path | Action | Purpose |
|------|--------|---------|
| `specs/001-competition-test-suite/plan.md` | Create/Replace | 本计划 |
| `specs/001-competition-test-suite/research.md` | Create | Phase 0 的 8 个决策、分支预算与变异体三角流程 |
| `specs/001-competition-test-suite/data-model.md` | Create | 10 类测试夹具的字段、合法域、边界与具名工厂 |
| `specs/001-competition-test-suite/contracts/test-matrix.md` | Create | 逐 SUT 的分支对、断言点、优先级与预估测试数 |
| `specs/001-competition-test-suite/quickstart.md` | Create | 验证命令、覆盖率与变异率计算、稳定性检查 |
| `specs/001-competition-test-suite/tasks.md` | Future | 由 `/speckit-tasks` 生成 |
| `src/test/java/net/mooctest/StudentTest.java` | Future | 由 `/speckit-implement` 生成或更新 |
| `src/main/**`、`pom.xml` | No change | 只读被测对象与构建配置 |
