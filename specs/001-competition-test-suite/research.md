# Research: 单文件竞赛测试套件

**Feature**: 001-competition-test-suite
**Date**: 2026-11-01
**Input**: [spec.md](./spec.md)、[plan.md](./plan.md)

All eight planning questions are resolved below. The decisions also define the branch budget, the unreachable-branch policy, and the mutation-survivor workflow.

## R1. Single-File Organization

**Decision**: Use one top-level `StudentTest` with one `@Nested` group per production class, plus a second nested level only for rule families that exceed 20 methods. Keep static factories and named constants at the outer level. Do not use any other test file.

- Group names: `ExpressionAnswerGradingServiceTest`, `ExamGradingServiceTest`, `LearningProgressServiceTest`, `ExpressionExamProgressServiceTest`, `ExpressionEngineTest`, `EndToEndTest`.
- `ExamGradingServiceTest` second-level groups: `ValidationTest`, `BinaryQuestionTest`, `MultipleChoiceTest`, `FillBlankTest`, `NumericQuestionTest`, `EssayTest`, `ProgrammingTest`, `SubmissionRulesTest`, `AggregationTest`, `IntegrityTest`.
- Method names are behavior sentences: `returnsUnansweredWhenSubmittedExpressionIsBlank`, `capsFinalPointsWhenCapIsEnabled`.
- Constants are named domain values: `BASE_TIME`, `QUESTION_ID`, `SECTION`, `TOLERANCE`, `MAX_ATTEMPTS`, `PASS_PERCENTAGE`.
- Factories live in the outer class and return immutable fixtures; nested groups do not own mutable fields.

**Rationale**: The competition reads exactly one file. A per-SUT nested layout keeps navigation local while preventing a single giant inner class; the 20-method ceiling keeps any one group readable. Named factories avoid the 19-argument positional constructor trap.

**Alternatives considered**:

- One flat class with all methods: rejected because 150+ flat methods destroy the readability score and make navigation impractical.
- Multiple test files: rejected because FR-001 forbids them.
- One giant nested class: rejected because it recreates the flat-file problem inside a wrapper.

## R2. Static State Isolation

**Decision**: Use a single outer `@AfterEach` that resets global state in a fixed order: first `Mathematical_Expression.Options.setUseCache(false)` (which clears both static caches), then `Mathematical_Expression.Options.setUseBigDecimal(false)`. Do not rely on a `@BeforeEach` cleanup or on test ordering. For `CalculationManagement`, use unique component names per test and add a dedicated registry test that pins the registration and type-conflict semantics.

- Reset order is fixed so a failure report is reproducible: `setUseCache(false)` → `setUseBigDecimal(false)`.
- Cache tests must explicitly exercise both the miss path and the hit path, then reset.
- Registry tests use unique names such as `StudentTest-registry-01`; never reuse `PREFIX_EXPRESSION_OPERATION_NAME` or `BRACKETS_CALCULATION_2_NAME` for mutation tests.
- The registry has no clear API. Tests must not assert global emptiness or global size; they assert only per-name state.
- Dedicated registry assertions: `register(calc, true)` on an existing name returns `false` and keeps the original instance; `register(calc, false)` overwrites and returns `true`; `getInstance` on the same name and type returns the same instance; `getInstance` on the same name with a different type throws `ExtractException`.

**Rationale**: The two caches and the registry persist for the JVM lifetime. Without a fixed reset order, a cache-hit test can silently satisfy a later cache-miss test, and PIT can produce different results across runs.

**Alternatives considered**:

- Reflection to clear the private maps: rejected as brittle and contrary to the “test observable behavior” rule.
- Fork a JVM per test: rejected as too slow for a 15-second suite and hostile to PIT.
- Rely on test execution order: rejected because order dependence is explicitly forbidden by FR-006 and the constitution.

## R3. Fixed Clock

**Decision**: Inject `Clock.fixed(BASE, ZoneOffset.UTC)` where `BASE = 2026-01-15T10:00:00Z` into `LearningProgressService`. Use the same base instant for every time-sensitive fixture.

| Boundary | Fixture value | Expected behavior |
|----------|---------------|-------------------|
| Enrollment in the future | `enrolledAt = BASE + 1 second` | `ENROLLMENT_DATE_IN_FUTURE` blocker |
| Enrollment exactly now | `enrolledAt = BASE` | No future-enrollment blocker |
| Access already expired | `accessExpiresAt = BASE - 1 second` | `COURSE_ACCESS_EXPIRED` blocker |
| Access expires exactly now | `accessExpiresAt = BASE` | Not expired; `COURSE_ACCESS_EXPIRES_SOON` warning |
| Access within 7 days | `accessExpiresAt = BASE + 7 days` | `COURSE_ACCESS_EXPIRES_SOON` warning |
| Access just beyond 7 full days | `accessExpiresAt = BASE + 8 days` | No expiry warning |
| No expiry | `accessExpiresAt = null` | No expiry blocker or warning |

- Exam timestamps are direct `Instant` inputs, not clock reads; they are varied through `startedAt`, `submittedAt`, and `deadline`.
- No other test needs wall-clock time.

**Rationale**: Fixed time makes certificate, expiry, and enrollment branches deterministic and repeatable. The UTC zone removes the machine timezone from the result.

**Alternatives considered**:

- `Clock.systemUTC()`: rejected because the result changes with the real date.
- Mockito-mocked `Clock`: viable but unnecessary; a fixed clock is simpler and does not require a mock.
- Injecting a lambda instead of a `Clock`: rejected because the constructor accepts `Clock` and the production contract should be exercised as-is.

## R4. Mockito Boundary

**Decision**: Default to hand-written stubs and lambdas. `ExpressionEvaluator` is a functional interface, so tests pass deterministic lambdas that return a value or throw `ExpressionEvaluationException`. Use Mockito only when the test must verify collaboration count or captured arguments, principally `ExpressionExamGradingService` wrapping `ExamGradingService` and `ExpressionAnswerGradingService`.

- Use Mockito to verify that `ExamGradingService.grade` is called once and that the captured `ExamSubmission` carries the per-answer results produced by the answer service.
- Do not mock records such as `ExpressionQuestion`, `ExpressionAnswerSubmission`, `AnswerResult`, `GradeReport`, or `ProgressEvaluation`.
- Do not mock final types such as `ExpressionExamProgressService`; instantiate it with a real or stubbed `LearningProgressService`.
- Prefer real `MathematicalExpressionEvaluator` when P1 needs a realistic evaluation path; use lambda stubs for isolated grading branches.

**Rationale**: Lambdas keep branch tests fast and readable. Mockito is reserved for the one relationship where call count and argument transformation are the behavior under test. Records are value objects, so mocking them hides the very data shape the suite must verify.

**Alternatives considered**:

- Mockito for every collaborator: rejected because it slows the suite and makes behavior harder to read.
- No Mockito at all: rejected because the wrapper delegation cannot be verified without a spy or a mock.
- Mock final classes: rejected because the default Mockito setup does not support final mocking without an extra configuration.

## R5. PIT Compatibility

**Decision**: Run coverage and mutation as two separate Maven invocations. Keep the suite free of `@Disabled`, order dependence, `System.exit`, infinite loops, oversized parameterization, randomness, sleeping, and external IO.

- JaCoCo is attached to Surefire through `argLine`; it instruments classes for `mvn test`.
- PIT runs its own forked JVM and does not consume Surefire’s `argLine` by default. If the environment causes PIT to inherit a JaCoCo agent, pass `-DargLine=` on the PIT command line only; do not edit `pom.xml`.
- PIT configuration remains `targetClasses=net.mooctest.*`, `targetTests=net.mooctest.*`, `DEFAULTS`, HTML+XML, `timestampedReports=false`.
- Run `mvn -q test` and `mvn -q org.pitest:pitest-maven:mutationCoverage` separately and record both artifacts.
- Re-run both commands a second time; identical failure counts, coverage counts, and mutation counts are required for acceptance.

**Rationale**: Double instrumentation is the main source of PIT slowness or class-format errors. Separate invocations keep the JaCoCo coverage run and the mutation run independent and reproducible. PIT’s repeated forks also punish any test with hidden global state or infinite-loop input.

**Alternatives considered**:

- Run PIT with the JaCoCo agent enabled: rejected because it adds overhead and can conflict with bytecode mutation.
- Disable JaCoCo to simplify PIT: rejected because coverage is 40% of the score and `pom.xml` must not change.
- Extend PIT timeouts via pom: rejected; the suite must be fast enough as delivered.

## R6. Assertion Strategy

**Decision**: Assert behavior contracts, not incidental representation.

- `BigDecimal`: compare with `compareTo(...) == 0`; if AssertJ is already on the existing test classpath, `isEqualByComparingTo` is acceptable, but no dependency is added. Never use `BigDecimal.equals` or JUnit `assertEquals(BigDecimal, BigDecimal)` for numeric equality.
- Exceptions: use `assertThrows(SpecificException.class, ...)`; assert the exception type even when the message is also checked.
- Stable business codes: exact equality for codes such as `ANSWER_NOT_PROVIDED`, `INVALID_STUDENT_EXPRESSION`, `SUBSCRIPTION_INACTIVE`; use `startsWith` for prefixed codes such as `MANDATORY_QUESTION_UNANSWERED:`.
- Engine messages: Chinese diagnostic text is unstable; assert only the exception type for engine-level format errors.
- `toString` and `equals`: assert only invariant properties, never exact diagnostic wording, unless the type documents the string as a contract.
- Weak assertions (`assertNotNull`, `assertTrue(x != null)`, `assertDoesNotThrow`) may accompany a strong assertion but may not stand alone.

**Rationale**: PIT mutates return values, conditions, and constants. A strong assertion on the returned value, exception class, enum constant, or stable code kills those mutants; a null check does not. Scale-sensitive equality also causes false negatives on otherwise correct BigDecimal results.

**Alternatives considered**:

- `assertEquals` on BigDecimal: rejected because `10.0` and `10.00` are unequal under `equals`.
- Message-text equality for engine errors: rejected because the engine uses long bilingual messages and may change wording.
- Snapshot or whole-object `toString` assertions: rejected because they couple tests to formatting rather than behavior.

## R7. Branch Budget Allocation

**Decision**: Allocate per-class branch targets as shown in the table, with a global target of 953 covered branches out of 1,072 (88.9%). Prioritize the largest pools (`ExamGradingService`, `LearningProgressService`, engine classes) and treat the listed unreachable or low-value branches as out of scope.

**Rationale**: The largest branch pools produce the most score per test method. Per-class targets make progress attributable and prevent a single easy class from masking gaps in the high-weight services. Chasing unreachable branches would waste the 160-method budget and add no mutation value.

**Alternatives considered**:

- Uniform 85% for every class: rejected because small classes with generated methods and legacy engine branches make uniform targets inefficient.
- Chase every branch to 100%: rejected because equivalent mutants, unreachable enum defaults, and third-party formatter paths make it impossible and wasteful.
- Test only the top two service classes: rejected because global coverage would remain well below 85%.

The table uses the specification’s baseline counts. Targets are planning targets; the actual denominator is always `BRANCH_MISSED + BRANCH_COVERED` from `target/site/jacoco/jacoco.csv`.

| Class / Group | Branch Total | Target Coverage | Target Covered | Priority | Notes |
|---------------|--------------|-----------------|----------------|----------|-------|
| `ExamGradingService` | 344 | 90% | 310 | P1 | 题型、提交规则、诚信、汇总 |
| `LearningProgressService` | 263 | 88% | 231 | P2 | 课时类型、顺序、证书、账户 |
| `ExpressionExamProgressService` | 20 | 100% | 20 | P2 | 回写与拒绝分支 |
| `PrefixExpression` | 76 | 85% | 65 | P3 | 编译、缓存、精度、栈状态 |
| `PrefixExpressionOperation` | 47 | 85% | 40 | P3 | 格式检查、格式化、双精度分支 |
| `NumberUtils` | 46 | 95% | 44 | P3 | 运算、优先级、比较、区间、阶乘 |
| `StrUtils` | 42 | 95% | 40 | P3 | 拆分、去空、数值解析、缓存 |
| `ArithmeticExpression` | 38 | 100% | 38 | P3 | 简易解析器全部可达分支 |
| `BracketsExpression` | 22 | 90% | 20 | P3 | 括号编译与缓存 |
| `BracketsCalculation2` | 19 | 90% | 17 | P3 | 双精度路径与注册 |
| `CumulativeCalculation` | 18 | 80% | 14 | P3 | 安全区间边界，非法步长不测 |
| `BooleanCalculation2` | 12 | 90% | 11 | P3 | 比较与 null 字面量 |
| `NumberCalculation` | 31 | 90% | 28 | P3 | 通过具体子类覆盖 check 与计算委托 |
| Scattered classes | 94 | 80% | 75 | P3/P4 | 结果类型、优化运算、NameExpression、PackExpression、LogResults 等 |
| **Total** | **1,072** | **88.9%** | **953** | — | 高于 85% 硬目标，接近 90% 目标 |

- The specification baseline lists `PrefixExpressionOperation` as 47 and `BracketsCalculation2` as 19. The current local `jacoco.csv` shows 48 and 20 for these classes. The final run uses the actual denominator; absolute targets scale proportionally.
- If the final global target is 90%, the remaining gap is allocated to `ExamGradingService` and `LearningProgressService`, because they have the largest branch pools and the highest scoring weight.

### Unreachable or Low-Value Branches

| Branch / Area | Decision | Reason |
|---------------|----------|--------|
| `LessonType` switch `default` in `assessLesson` | Do not chase; record as unreachable/equivalent | The enum has six constants and the switch is exhaustive; a new constant cannot be created through normal test input |
| `QuestionType` switch | Cover all seven reachable constants | The switch has no default and all enum values are constructible |
| `LogResults.explain(VarFormatter)` | Low priority; cover only if a real formatter is already available | The path delegates to the third-party varFormatter API, which the tests are told not to depend on |
| `BracketsCalculation.calculation2` | One happy path and one no-operator error | The method is `@Deprecated` and no longer used by the main path |
| `CalculationResults.explain` default | Optional single test | It is a default method that always throws `UnsupportedOperationException` |
| `CumulativeCalculation` with step ≤ 0 | Never test | The loop would not terminate; FR-010 bans the input |
| `NumberUtils.factorial` with very large input | Test 0, 1, 5, 10, and a negative value only | Deep recursion and `double` overflow make “very large” a stack/perf hazard, not a meaningful branch |
| `ArithmeticExpression.supports` exhaustive regex fuzzing | Sample the representative character classes | Exhaustive regex enumeration adds little branch value and bloats the file |
| `Map.copyOf` / `Set` iteration order | Do not assert order | The immutable-copy contract does not guarantee order; assert contents and size |

## R8. Efficiency Implementation Details

**Decision**: Keep the suite under 160 methods, avoid Spring context startup, reuse immutable fixtures and the fixed clock, and use parameterized tests for boundary sequences.

| Budget Item | Allocation |
|-------------|------------|
| Surefire / JVM startup | 1.5–2.5 seconds |
| 158 test methods × 40 ms average | 6.3 seconds |
| Fixture and teardown overhead | 0.5 seconds |
| Reporting and class loading | 0.3 seconds |
| **Expected total** | **8.6–9.6 seconds** |
| Hard ceiling | 15 seconds |
| Per-method hard ceiling | 300 milliseconds |
| Per-method target | ≤ 45 milliseconds |

- Method budget: P1 70, P2 48, P3 34, P4 6; total 158.
- Parameterized tests count as one method; keep each to ≤ 12 rows.
- Do not construct heavyweight objects in `@BeforeEach`; build fixtures once in static factories or static final constants and return immutable records.
- Do not use `@RepeatedTest`; it inflates runtime and adds no branch value.
- Avoid cartesian products: one parameterized method per boundary family, not one per combination.
- PIT runs the suite repeatedly; an extra 100 ms per test becomes minutes across mutants. Any test above 100 ms must be simplified before merge.

**Rationale**: The suite must fit inside both the 15-second competition budget and PIT’s repeated-fork budget. Average cost, not just total cost, determines whether mutation analysis completes in a usable time.

**Alternatives considered**:

- Fewer, larger test methods: rejected because one failing method hides multiple behaviors and weakens the mutation signal.
- More, smaller methods: rejected because method count and startup overhead grow past the 160 ceiling.
- Parallel test execution: rejected because the project does not configure it and static state isolation would become harder.

## Mutation Triad Workflow

1. Run `mvn -q org.pitest:pitest-maven:mutationCoverage`.
2. Read `target/pit-reports/mutation.xml` (and the HTML index) and export every `SURVIVED` mutant by class, mutator, and line.
3. Classify each survivor into exactly one of three buckets:
   - **Missing input**: the branch exists but no test drives it. Add a focused input case.
   - **Missing assertion**: the branch runs but the assertion is too weak. Strengthen the existing assertion.
   - **Equivalent mutant**: the mutation cannot change observable behavior. Record the reason and do not add a test.
4. For the first two buckets, add or strengthen a test; for the third, write a one-line ledger entry.
5. Re-run PIT and compare killed/survived counts per class.
6. Stop when SC-002 is met and every surviving mutant has a ledger entry; otherwise continue the loop.

**Survivor ledger fields**: class, mutator, line, bucket, reason, test method, and re-check date.

**Exit criteria**: killed / (killed + survived) ≥ 75% (target 85%); 100% of survivors classified; two consecutive PIT runs produce the same survivor count.
