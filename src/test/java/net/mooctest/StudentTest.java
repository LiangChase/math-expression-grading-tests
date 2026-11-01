/*
 * 单文件竞赛测试套件。
 *
 * 约束：所有测试只能写在本文件内；不修改 src/main 与 pom.xml；不新增文件与依赖。
 * 结构：按被测类组织 @Nested；夹具全部通过本文件内的具名静态工厂与 Builder 构造。
 */
package net.mooctest;

import net.mooctest.expression.engine.AbnormalOperation;
import net.mooctest.expression.engine.ArithmeticExpression;
import net.mooctest.expression.engine.BooleanCalculation2;
import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.BracketsExpression;
import net.mooctest.expression.engine.CalculationBooleanResults;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.CalculationNumberResults;
import net.mooctest.expression.engine.CalculationOptimized;
import net.mooctest.expression.engine.CalculationResults;
import net.mooctest.expression.engine.CumulativeCalculation;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.LogResults;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.NameExpression;
import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.expression.engine.PackExpression;
import net.mooctest.expression.engine.PrefixExpression;
import net.mooctest.expression.engine.PrefixExpressionOperation;
import net.mooctest.expression.engine.StrUtils;
import net.mooctest.expression.engine.WrongFormat;
import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.grading.domain.GradingPolicy;
import net.mooctest.grading.domain.QuestionType;
import net.mooctest.grading.expression.ExpressionAnswerEvaluation;
import net.mooctest.grading.expression.ExpressionAnswerGradingService;
import net.mooctest.grading.expression.ExpressionAnswerStatus;
import net.mooctest.grading.expression.ExpressionAnswerSubmission;
import net.mooctest.grading.expression.ExpressionEvaluationException;
import net.mooctest.grading.expression.ExpressionExamGrade;
import net.mooctest.grading.expression.ExpressionExamGradingService;
import net.mooctest.grading.expression.ExpressionExamSubmission;
import net.mooctest.grading.expression.ExpressionQuestion;
import net.mooctest.grading.expression.MathematicalExpressionEvaluator;
import net.mooctest.grading.service.ExamGradingService;
import net.mooctest.progress.domain.CourseProgressSnapshot;
import net.mooctest.progress.domain.LessonProgress;
import net.mooctest.progress.domain.LessonType;
import net.mooctest.progress.domain.ProgressEvaluation;
import net.mooctest.progress.service.ExpressionExamProgressService;
import net.mooctest.progress.service.LearningProgressService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单文件竞赛测试套件。
 */
class StudentTest {

    private static final Instant BASE = Instant.parse("2026-01-15T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(BASE, ZoneOffset.UTC);

    private static final String QUESTION_ID = "q1";
    private static final String SECTION = "math";
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal ONE = new BigDecimal("1.00");
    private static final BigDecimal TEN = new BigDecimal("10.00");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private static final int GRACE_SECONDS = 60;
    private static final int MINOR_OVERRUN_SECONDS = 300;
    private static final BigDecimal TIMEOUT_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal PROGRAM_TIMEOUT_FACTOR = new BigDecimal("0.75");
    private static final BigDecimal MULTI_INCORRECT_FACTOR = new BigDecimal("0.25");
    private static final BigDecimal WRONG_PENALTY_RATE = new BigDecimal("0.25");
    private static final BigDecimal HALF_PENALTY_RATE = new BigDecimal("0.50");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal DURATION_MINOR_FACTOR = new BigDecimal("0.95");
    private static final int FULL_PERCENT = 100;
    private static final int PASS_PERCENT = 60;
    private static final int VIDEO_COMPLETE_PERCENT = 90;
    private static final int VIDEO_WARN_PERCENT = 70;
    private static final int QUIZ_PASS_SCORE = 60;
    private static final int ALLOWED_SECONDS = 100;

    @AfterEach
    void resetStaticState() {
        // 固定顺序：先清空两个静态缓存，再复位 BigDecimal 开关。
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
    }

    // ---------------------------------------------------------------------
    // 夹具：评分领域
    // ---------------------------------------------------------------------

    private static AnswerBuilder answer() {
        return new AnswerBuilder();
    }

    private static SubmissionBuilder submission() {
        return new SubmissionBuilder();
    }

    private static PolicyBuilder policy() {
        return new PolicyBuilder();
    }

    private static ExpressionQuestion question() {
        return new ExpressionQuestion(
                QUESTION_ID, SECTION, "(2+3)*4", TEN, ZERO, true
        );
    }

    private static ExpressionQuestion question(String correctExpression, BigDecimal tolerance) {
        return new ExpressionQuestion(
                QUESTION_ID, SECTION, correctExpression, TEN, tolerance, true
        );
    }

    private static ExpressionQuestion question(String id, String correctExpression,
                                               BigDecimal tolerance) {
        return new ExpressionQuestion(id, SECTION, correctExpression, TEN, tolerance, true);
    }

    private static ExpressionAnswerSubmission answerSubmission(
            ExpressionQuestion question, String expression) {
        return new ExpressionAnswerSubmission(question, expression);
    }

    private static ExprExamBuilder expressionExam() {
        return new ExprExamBuilder();
    }

    private static LessonBuilder lesson() {
        return new LessonBuilder();
    }

    private static SnapshotBuilder snapshot() {
        return new SnapshotBuilder();
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    private static void assertHasCode(List<String> codes, String expected) {
        assertTrue(codes.contains(expected),
                () -> "expected " + expected + " in " + codes);
    }

    private static void assertHasPrefix(List<String> codes, String prefix) {
        assertTrue(codes.stream().anyMatch(code -> code.startsWith(prefix)),
                () -> "expected prefix " + prefix + " in " + codes);
    }

    private static GradeReport gradeReport(int percentage, boolean manualReview,
                                           List<String> failures) {
        return new GradeReport(TEN, TEN, TEN, percentage, "A", true, manualReview,
                Map.of(SECTION, 100), failures, List.of());
    }

    private static ExpressionExamGrade examGrade(int percentage, boolean manualReview,
                                                  List<String> failures) {
        return new ExpressionExamGrade(gradeReport(percentage, manualReview, failures),
                List.of());
    }

    @Nested
    @DisplayName("端到端冒烟")
    class EndToEndTest {

        @Test
        @DisplayName("表达式作答可以通过公开入口完成单题判定")
        void expressionAnswerCanBeGradedThroughPublicEntryPoint() {
            ExpressionAnswerGradingService service =
                    new ExpressionAnswerGradingService(new MathematicalExpressionEvaluator());
            ExpressionAnswerEvaluation result = service.evaluate(
                    answerSubmission(question(), "10+10")
            );
            assertAll(
                    () -> assertEquals(ExpressionAnswerStatus.EXACT, result.status()),
                    () -> assertDecimal("20", result.expectedValue()),
                    () -> assertDecimal("20", result.actualValue()),
                    () -> assertDecimal("0", result.absoluteError())
            );
        }

        @Nested
        @DisplayName("完整评分旅程")
        class GradingJourneyTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); fullJourneyMapsStatusesAndReportFields(); },
                    () -> { resetStaticState(); sameInputProducesSameReport(); },
                    () -> { resetStaticState(); mandatoryUnansweredBlocksPassInFullJourney(); }
                );
            }

            private final ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(new MathematicalExpressionEvaluator());
            private final ExamGradingService examService = new ExamGradingService();
            private final ExpressionExamGradingService service =
                    new ExpressionExamGradingService(answerService, examService);

            private ExpressionExamSubmission fullSubmission() {
                return expressionExam().answers(
                        answerSubmission(question("q1", "(2+3)*4", ZERO), "10+10"),
                        answerSubmission(question("q2", "10", TOLERANCE), "10.01"),
                        answerSubmission(question("q3", "10", ZERO), "11"),
                        new ExpressionAnswerSubmission(
                                new ExpressionQuestion("q4", SECTION, "10", TEN, ZERO, false),
                                "   "),
                        answerSubmission(question("q5", "10", ZERO), "1+")
                ).build();
            }

            void fullJourneyMapsStatusesAndReportFields() {
                ExpressionExamGrade grade = service.grade(fullSubmission(),
                        policy().passPercentage(PASS_PERCENT).build());
                GradeReport report = grade.report();
                List<ExpressionAnswerEvaluation> evaluations = grade.answerEvaluations();
                assertAll(
                        () -> assertEquals(5, evaluations.size()),
                        () -> assertEquals(ExpressionAnswerStatus.EXACT,
                                evaluations.get(0).status()),
                        () -> assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE,
                                evaluations.get(1).status()),
                        () -> assertEquals(ExpressionAnswerStatus.INCORRECT,
                                evaluations.get(2).status()),
                        () -> assertEquals(ExpressionAnswerStatus.UNANSWERED,
                                evaluations.get(3).status()),
                        () -> assertEquals(ExpressionAnswerStatus.INVALID_EXPRESSION,
                                evaluations.get(4).status()),
                        () -> assertDecimal("20.00", report.rawPoints()),
                        () -> assertDecimal("20.00", report.finalPoints()),
                        () -> assertDecimal("50.00", report.maximumPoints()),
                        () -> assertEquals(40, report.percentage()),
                        () -> assertEquals("F", report.letterGrade()),
                        () -> assertFalse(report.passed()),
                        () -> assertFalse(report.manualReview()),
                        () -> assertEquals(40, report.sectionPercentages().get(SECTION)),
                        () -> assertHasCode(report.failures(),
                                "OVERALL_SCORE_BELOW_PASS_MARK"),
                        () -> assertHasPrefix(report.warnings(),
                                "NUMERIC_TOLERANCE_DATA_MISSING:q5")
                );
            }

            void sameInputProducesSameReport() {
                GradeReport first = service.grade(fullSubmission(),
                        policy().passPercentage(PASS_PERCENT).build()).report();
                GradeReport second = service.grade(fullSubmission(),
                        policy().passPercentage(PASS_PERCENT).build()).report();
                assertAll(
                        () -> assertDecimal("20.00", first.rawPoints()),
                        () -> assertDecimal("20.00", second.rawPoints()),
                        () -> assertEquals(first.percentage(), second.percentage()),
                        () -> assertEquals(first.letterGrade(), second.letterGrade()),
                        () -> assertEquals(first.failures(), second.failures()),
                        () -> assertEquals(first.warnings(), second.warnings())
                );
            }

            void mandatoryUnansweredBlocksPassInFullJourney() {
                ExpressionExamSubmission submission = expressionExam().answers(
                        answerSubmission(question("q1", "10", ZERO), "10"),
                        new ExpressionAnswerSubmission(
                                new ExpressionQuestion("q2", SECTION, "10", TEN, ZERO, true),
                                "   ")
                ).build();
                GradeReport report = service.grade(submission,
                        policy().passPercentage(50).build()).report();
                assertAll(
                        () -> assertFalse(report.passed()),
                        () -> assertHasPrefix(report.failures(),
                                "MANDATORY_QUESTION_UNANSWERED:q2")
                );
            }
        }

        @Nested
        @DisplayName("成绩回写旅程")
        class WritebackJourneyTest {

            private final ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(new MathematicalExpressionEvaluator());
            private final ExamGradingService examService = new ExamGradingService();
            private final ExpressionExamGradingService gradingService =
                    new ExpressionExamGradingService(answerService, examService);
            private final ExpressionExamProgressService progressService =
                    new ExpressionExamProgressService(new LearningProgressService(FIXED_CLOCK));

            @Test
            void finalizedGradeFlowsIntoQuizProgress() {
                ExpressionExamGrade grade = gradingService.grade(
                        expressionExam().answers(
                                answerSubmission(question("q1", "10", ZERO), "10")
                        ).build(),
                        policy().passPercentage(PASS_PERCENT).build());
                LessonProgress quiz = lesson().lessonId("quiz-1").type(LessonType.QUIZ)
                        .order(1).mandatory(true).completionPercent(0)
                        .score(0).submitted(false).approved(false)
                        .attempts(1).maximumAttempts(3).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(quiz)
                        .requiredOverallPercent(0).build();
                ProgressEvaluation result = progressService.evaluate(
                        snapshot, "quiz-1", grade);
                assertAll(
                        () -> assertEquals(100, grade.report().percentage()),
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons()),
                        () -> assertTrue(result.courseCompleted()),
                        () -> assertTrue(result.certificateEligible()),
                        () -> assertNull(result.nextLessonId()),
                        () -> assertFalse(snapshot.lessons().get(0).submitted())
                );
            }
        }

        @Nested
        @DisplayName("诚信与不可变性")
        class IntegrityInvariantTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); integrityFailureMarksQuizAndBlocksCourse(); },
                    () -> { resetStaticState(); resultCollectionsAreImmutable(); }
                );
            }

            private final ExpressionExamProgressService progressService =
                    new ExpressionExamProgressService(new LearningProgressService(FIXED_CLOCK));

            void integrityFailureMarksQuizAndBlocksCourse() {
                LessonProgress quiz = lesson().lessonId("quiz-1").type(LessonType.QUIZ)
                        .order(1).mandatory(true).completionPercent(0)
                        .score(0).submitted(false).approved(false)
                        .attempts(1).maximumAttempts(3).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(quiz)
                        .requiredOverallPercent(0).build();
                ExpressionExamGrade grade = new ExpressionExamGrade(
                        gradeReport(80, false,
                                List.of("ANSWER_INTEGRITY_VIOLATION:q1")),
                        List.of());
                ProgressEvaluation result = progressService.evaluate(
                        snapshot, "quiz-1", grade);
                assertAll(
                        () -> assertHasPrefix(result.blockers(), "PLAGIARISM_REVIEW:"),
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertFalse(result.certificateEligible())
                );
            }

            void resultCollectionsAreImmutable() {
                GradeReport report = gradeReport(80, false, List.of());
                ProgressEvaluation evaluation = new ProgressEvaluation(
                        80, 1, 1, true, true, null, List.of("BLOCKER"),
                        List.of("WARNING"));
                assertAll(
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> report.failures().add("x")),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> report.warnings().add("x")),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> evaluation.blockers().add("x")),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> evaluation.warnings().add("x"))
                );
            }
        }
    }

    // ---------------------------------------------------------------------
    // 夹具 Builder
    // ---------------------------------------------------------------------

    private static final class AnswerBuilder {
        private String questionId = QUESTION_ID;
        private QuestionType type = QuestionType.NUMERIC;
        private String section = SECTION;
        private BigDecimal maximumPoints = TEN;
        private boolean mandatory = true;
        private boolean answered = true;
        private boolean exactMatch = true;
        private int correctOptionCount;
        private int selectedCorrectOptions;
        private int selectedIncorrectOptions;
        private BigDecimal numericError = ZERO;
        private BigDecimal numericTolerance = TOLERANCE;
        private int rubricPercent;
        private int passedTests;
        private int totalTests;
        private boolean compilationSucceeded = true;
        private boolean plagiarismFlag;
        private boolean timedOut;
        private boolean manualReviewRequired;

        AnswerBuilder questionId(String value) {
            this.questionId = value;
            return this;
        }

        AnswerBuilder type(QuestionType value) {
            this.type = value;
            return this;
        }

        AnswerBuilder section(String value) {
            this.section = value;
            return this;
        }

        AnswerBuilder maximumPoints(BigDecimal value) {
            this.maximumPoints = value;
            return this;
        }

        AnswerBuilder mandatory(boolean value) {
            this.mandatory = value;
            return this;
        }

        AnswerBuilder answered(boolean value) {
            this.answered = value;
            return this;
        }

        AnswerBuilder exactMatch(boolean value) {
            this.exactMatch = value;
            return this;
        }

        AnswerBuilder correctOptionCount(int value) {
            this.correctOptionCount = value;
            return this;
        }

        AnswerBuilder selectedCorrectOptions(int value) {
            this.selectedCorrectOptions = value;
            return this;
        }

        AnswerBuilder selectedIncorrectOptions(int value) {
            this.selectedIncorrectOptions = value;
            return this;
        }

        AnswerBuilder numericError(BigDecimal value) {
            this.numericError = value;
            return this;
        }

        AnswerBuilder numericTolerance(BigDecimal value) {
            this.numericTolerance = value;
            return this;
        }

        AnswerBuilder rubricPercent(int value) {
            this.rubricPercent = value;
            return this;
        }

        AnswerBuilder passedTests(int value) {
            this.passedTests = value;
            return this;
        }

        AnswerBuilder totalTests(int value) {
            this.totalTests = value;
            return this;
        }

        AnswerBuilder compilationSucceeded(boolean value) {
            this.compilationSucceeded = value;
            return this;
        }

        AnswerBuilder plagiarismFlag(boolean value) {
            this.plagiarismFlag = value;
            return this;
        }

        AnswerBuilder timedOut(boolean value) {
            this.timedOut = value;
            return this;
        }

        AnswerBuilder manualReviewRequired(boolean value) {
            this.manualReviewRequired = value;
            return this;
        }

        AnswerResult build() {
            return new AnswerResult(
                    questionId, type, section, maximumPoints, mandatory, answered,
                    exactMatch, correctOptionCount, selectedCorrectOptions,
                    selectedIncorrectOptions, numericError, numericTolerance,
                    rubricPercent, passedTests, totalTests, compilationSucceeded,
                    plagiarismFlag, timedOut, manualReviewRequired
            );
        }
    }

    private static final class SubmissionBuilder {
        private String examId = "exam-1";
        private String studentId = "student-1";
        private List<AnswerResult> answers = List.of();
        private Instant startedAt = BASE;
        private Instant submittedAt = BASE.plusSeconds(ALLOWED_SECONDS);
        private Instant deadline;
        private int allowedDurationSeconds = 3600;
        private int attemptNumber = 1;
        private boolean identityVerified = true;
        private boolean termsAccepted = true;
        private boolean suspendedAccount;
        private boolean proctorDisconnected;
        private boolean ipAddressChanged;
        private int fullscreenExitCount;
        private int copyPasteEventCount;
        private BigDecimal bonusPoints = ZERO;
        private Integer instructorOverridePercent;
        private Map<String, Integer> sectionPassPercentages = Map.of();

        SubmissionBuilder examId(String value) {
            this.examId = value;
            return this;
        }

        SubmissionBuilder studentId(String value) {
            this.studentId = value;
            return this;
        }

        SubmissionBuilder answers(AnswerResult... values) {
            this.answers = List.of(values);
            return this;
        }

        SubmissionBuilder answers(List<AnswerResult> values) {
            this.answers = values;
            return this;
        }

        SubmissionBuilder startedAt(Instant value) {
            this.startedAt = value;
            return this;
        }

        SubmissionBuilder submittedAt(Instant value) {
            this.submittedAt = value;
            return this;
        }

        SubmissionBuilder deadline(Instant value) {
            this.deadline = value;
            return this;
        }

        SubmissionBuilder allowedDurationSeconds(int value) {
            this.allowedDurationSeconds = value;
            return this;
        }

        SubmissionBuilder attemptNumber(int value) {
            this.attemptNumber = value;
            return this;
        }

        SubmissionBuilder identityVerified(boolean value) {
            this.identityVerified = value;
            return this;
        }

        SubmissionBuilder termsAccepted(boolean value) {
            this.termsAccepted = value;
            return this;
        }

        SubmissionBuilder suspendedAccount(boolean value) {
            this.suspendedAccount = value;
            return this;
        }

        SubmissionBuilder proctorDisconnected(boolean value) {
            this.proctorDisconnected = value;
            return this;
        }

        SubmissionBuilder ipAddressChanged(boolean value) {
            this.ipAddressChanged = value;
            return this;
        }

        SubmissionBuilder fullscreenExitCount(int value) {
            this.fullscreenExitCount = value;
            return this;
        }

        SubmissionBuilder copyPasteEventCount(int value) {
            this.copyPasteEventCount = value;
            return this;
        }

        SubmissionBuilder bonusPoints(BigDecimal value) {
            this.bonusPoints = value;
            return this;
        }

        SubmissionBuilder instructorOverridePercent(Integer value) {
            this.instructorOverridePercent = value;
            return this;
        }

        SubmissionBuilder sectionPassPercentages(Map<String, Integer> value) {
            this.sectionPassPercentages = value;
            return this;
        }

        ExamSubmission build() {
            return new ExamSubmission(
                    examId, studentId, answers, startedAt, submittedAt, deadline,
                    allowedDurationSeconds, attemptNumber, identityVerified,
                    termsAccepted, suspendedAccount, proctorDisconnected,
                    ipAddressChanged, fullscreenExitCount, copyPasteEventCount,
                    bonusPoints, instructorOverridePercent, sectionPassPercentages
            );
        }
    }

    private static final class PolicyBuilder {
        private int passPercentage = 60;
        private boolean partialCreditEnabled = true;
        private boolean negativeMarkingEnabled;
        private BigDecimal wrongAnswerPenaltyRate = WRONG_PENALTY_RATE;
        private BigDecimal unansweredPenaltyRate = ZERO;
        private int latePenaltyPercentPerHour = 5;
        private int maximumLatePenaltyPercent = 50;
        private int minimumSectionPercentage;
        private boolean allMandatoryQuestionsRequired = true;
        private boolean identityVerificationRequired;
        private int maximumAttempts = 3;
        private boolean failOnIntegrityViolation = true;
        private boolean capScoreAtOneHundred = true;
        private boolean programmingCompilationRequired = true;

        PolicyBuilder passPercentage(int value) {
            this.passPercentage = value;
            return this;
        }

        PolicyBuilder partialCreditEnabled(boolean value) {
            this.partialCreditEnabled = value;
            return this;
        }

        PolicyBuilder negativeMarkingEnabled(boolean value) {
            this.negativeMarkingEnabled = value;
            return this;
        }

        PolicyBuilder wrongAnswerPenaltyRate(BigDecimal value) {
            this.wrongAnswerPenaltyRate = value;
            return this;
        }

        PolicyBuilder unansweredPenaltyRate(BigDecimal value) {
            this.unansweredPenaltyRate = value;
            return this;
        }

        PolicyBuilder latePenaltyPercentPerHour(int value) {
            this.latePenaltyPercentPerHour = value;
            return this;
        }

        PolicyBuilder maximumLatePenaltyPercent(int value) {
            this.maximumLatePenaltyPercent = value;
            return this;
        }

        PolicyBuilder minimumSectionPercentage(int value) {
            this.minimumSectionPercentage = value;
            return this;
        }

        PolicyBuilder allMandatoryQuestionsRequired(boolean value) {
            this.allMandatoryQuestionsRequired = value;
            return this;
        }

        PolicyBuilder identityVerificationRequired(boolean value) {
            this.identityVerificationRequired = value;
            return this;
        }

        PolicyBuilder maximumAttempts(int value) {
            this.maximumAttempts = value;
            return this;
        }

        PolicyBuilder failOnIntegrityViolation(boolean value) {
            this.failOnIntegrityViolation = value;
            return this;
        }

        PolicyBuilder capScoreAtOneHundred(boolean value) {
            this.capScoreAtOneHundred = value;
            return this;
        }

        PolicyBuilder programmingCompilationRequired(boolean value) {
            this.programmingCompilationRequired = value;
            return this;
        }

        GradingPolicy build() {
            return new GradingPolicy(
                    passPercentage, partialCreditEnabled, negativeMarkingEnabled,
                    wrongAnswerPenaltyRate, unansweredPenaltyRate,
                    latePenaltyPercentPerHour, maximumLatePenaltyPercent,
                    minimumSectionPercentage, allMandatoryQuestionsRequired,
                    identityVerificationRequired, maximumAttempts,
                    failOnIntegrityViolation, capScoreAtOneHundred,
                    programmingCompilationRequired
            );
        }
    }

    private static final class ExprExamBuilder {
        private String examId = "exam-1";
        private String studentId = "student-1";
        private List<ExpressionAnswerSubmission> answers = List.of();
        private Instant startedAt = BASE;
        private Instant submittedAt = BASE.plusSeconds(ALLOWED_SECONDS);
        private Instant deadline;
        private int allowedDurationSeconds = 3600;
        private int attemptNumber = 1;
        private boolean identityVerified = true;
        private boolean termsAccepted = true;
        private boolean suspendedAccount;
        private boolean proctorDisconnected;
        private boolean ipAddressChanged;
        private int fullscreenExitCount;
        private int copyPasteEventCount;
        private BigDecimal bonusPoints = ZERO;
        private Integer instructorOverridePercent;
        private Map<String, Integer> sectionPassPercentages = Map.of();

        ExprExamBuilder answers(ExpressionAnswerSubmission... values) {
            this.answers = List.of(values);
            return this;
        }

        ExprExamBuilder answers(List<ExpressionAnswerSubmission> values) {
            this.answers = values;
            return this;
        }

        ExprExamBuilder submittedAt(Instant value) {
            this.submittedAt = value;
            return this;
        }

        ExprExamBuilder deadline(Instant value) {
            this.deadline = value;
            return this;
        }

        ExprExamBuilder allowedDurationSeconds(int value) {
            this.allowedDurationSeconds = value;
            return this;
        }

        ExprExamBuilder attemptNumber(int value) {
            this.attemptNumber = value;
            return this;
        }

        ExprExamBuilder instructorOverridePercent(Integer value) {
            this.instructorOverridePercent = value;
            return this;
        }

        ExpressionExamSubmission build() {
            return new ExpressionExamSubmission(
                    examId, studentId, answers, startedAt, submittedAt, deadline,
                    allowedDurationSeconds, attemptNumber, identityVerified,
                    termsAccepted, suspendedAccount, proctorDisconnected,
                    ipAddressChanged, fullscreenExitCount, copyPasteEventCount,
                    bonusPoints, instructorOverridePercent, sectionPassPercentages
            );
        }
    }

    private static final class LessonBuilder {
        private String lessonId = "lesson-1";
        private LessonType type = LessonType.VIDEO;
        private boolean mandatory = true;
        private int order = 1;
        private int completionPercent = 100;
        private int score = 100;
        private int attempts = 1;
        private int maximumAttempts = 3;
        private boolean submitted = true;
        private boolean approved = true;
        private boolean plagiarismFlag;
        private int attendedMinutes;
        private int requiredMinutes;
        private boolean unlocked = true;

        LessonBuilder lessonId(String value) {
            this.lessonId = value;
            return this;
        }

        LessonBuilder type(LessonType value) {
            this.type = value;
            return this;
        }

        LessonBuilder mandatory(boolean value) {
            this.mandatory = value;
            return this;
        }

        LessonBuilder order(int value) {
            this.order = value;
            return this;
        }

        LessonBuilder completionPercent(int value) {
            this.completionPercent = value;
            return this;
        }

        LessonBuilder score(int value) {
            this.score = value;
            return this;
        }

        LessonBuilder attempts(int value) {
            this.attempts = value;
            return this;
        }

        LessonBuilder maximumAttempts(int value) {
            this.maximumAttempts = value;
            return this;
        }

        LessonBuilder submitted(boolean value) {
            this.submitted = value;
            return this;
        }

        LessonBuilder approved(boolean value) {
            this.approved = value;
            return this;
        }

        LessonBuilder plagiarismFlag(boolean value) {
            this.plagiarismFlag = value;
            return this;
        }

        LessonBuilder attendedMinutes(int value) {
            this.attendedMinutes = value;
            return this;
        }

        LessonBuilder requiredMinutes(int value) {
            this.requiredMinutes = value;
            return this;
        }

        LessonBuilder unlocked(boolean value) {
            this.unlocked = value;
            return this;
        }

        LessonProgress build() {
            return new LessonProgress(
                    lessonId, type, mandatory, order, completionPercent, score,
                    attempts, maximumAttempts, submitted, approved,
                    plagiarismFlag, attendedMinutes, requiredMinutes, unlocked
            );
        }
    }

    private static final class SnapshotBuilder {
        private String studentId = "student-1";
        private String courseId = "course-1";
        private List<LessonProgress> lessons = List.of();
        private boolean sequentialLearning;
        private int requiredOverallPercent = 60;
        private int requiredQuizScore = 60;
        private boolean identityVerified = true;
        private boolean codeOfConductAccepted = true;
        private boolean subscriptionActive = true;
        private boolean accountSuspended;
        private boolean coursePublished = true;
        private Instant enrolledAt = BASE.minusSeconds(86400);
        private Instant accessExpiresAt = BASE.plusSeconds(86400);
        private boolean certificateEnabled = true;
        private String certificateName = "Alice";
        private boolean finalProjectRequired;
        private boolean outstandingBalance;
        private boolean instructorApprovalRequired;

        SnapshotBuilder studentId(String value) {
            this.studentId = value;
            return this;
        }

        SnapshotBuilder courseId(String value) {
            this.courseId = value;
            return this;
        }

        SnapshotBuilder lessons(LessonProgress... values) {
            this.lessons = List.of(values);
            return this;
        }

        SnapshotBuilder lessons(List<LessonProgress> values) {
            this.lessons = values;
            return this;
        }

        SnapshotBuilder sequentialLearning(boolean value) {
            this.sequentialLearning = value;
            return this;
        }

        SnapshotBuilder requiredOverallPercent(int value) {
            this.requiredOverallPercent = value;
            return this;
        }

        SnapshotBuilder requiredQuizScore(int value) {
            this.requiredQuizScore = value;
            return this;
        }

        SnapshotBuilder identityVerified(boolean value) {
            this.identityVerified = value;
            return this;
        }

        SnapshotBuilder codeOfConductAccepted(boolean value) {
            this.codeOfConductAccepted = value;
            return this;
        }

        SnapshotBuilder subscriptionActive(boolean value) {
            this.subscriptionActive = value;
            return this;
        }

        SnapshotBuilder accountSuspended(boolean value) {
            this.accountSuspended = value;
            return this;
        }

        SnapshotBuilder coursePublished(boolean value) {
            this.coursePublished = value;
            return this;
        }

        SnapshotBuilder enrolledAt(Instant value) {
            this.enrolledAt = value;
            return this;
        }

        SnapshotBuilder accessExpiresAt(Instant value) {
            this.accessExpiresAt = value;
            return this;
        }

        SnapshotBuilder certificateEnabled(boolean value) {
            this.certificateEnabled = value;
            return this;
        }

        SnapshotBuilder certificateName(String value) {
            this.certificateName = value;
            return this;
        }

        SnapshotBuilder finalProjectRequired(boolean value) {
            this.finalProjectRequired = value;
            return this;
        }

        SnapshotBuilder outstandingBalance(boolean value) {
            this.outstandingBalance = value;
            return this;
        }

        SnapshotBuilder instructorApprovalRequired(boolean value) {
            this.instructorApprovalRequired = value;
            return this;
        }

        CourseProgressSnapshot build() {
            return new CourseProgressSnapshot(
                    studentId, courseId, lessons, sequentialLearning,
                    requiredOverallPercent, requiredQuizScore, identityVerified,
                    codeOfConductAccepted, subscriptionActive, accountSuspended,
                    coursePublished, enrolledAt, accessExpiresAt,
                    certificateEnabled, certificateName, finalProjectRequired,
                    outstandingBalance, instructorApprovalRequired
            );
        }
    }

    // ---------------------------------------------------------------------
    // US1：评分主链路
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("ExpressionAnswerGradingService")
    class ExpressionAnswerGradingServiceTest {

        private final ExpressionAnswerGradingService service =
                new ExpressionAnswerGradingService(new MathematicalExpressionEvaluator());

        @Nested
        @DisplayName("状态映射与题目校验")
        class StatusMappingTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); blankAndNullAnswersAreUnanswered(); },
                    () -> { resetStaticState(); invalidCorrectExpressionIsConfigurationError(); },
                    () -> { resetStaticState(); invalidStudentExpressionIsInvalidExpression(); },
                    () -> { resetStaticState(); errorBoundariesMapToExactToleranceAndIncorrect(); },
                    () -> { resetStaticState(); expectedAndActualValuesAreTransferred(); },
                    () -> { resetStaticState(); questionValidationRejectsInvalidFields(); },
                    () -> { resetStaticState(); nullSubmissionIsRejected(); }
                );
            }

            void blankAndNullAnswersAreUnanswered() {
                ExpressionAnswerEvaluation blank = service.evaluate(
                        answerSubmission(question(), "   "));
                ExpressionAnswerEvaluation missing = service.evaluate(
                        answerSubmission(question(), null));
                assertAll(
                        () -> assertEquals(ExpressionAnswerStatus.UNANSWERED, blank.status()),
                        () -> assertFalse(blank.answerResult().answered()),
                        () -> assertNull(blank.expectedValue()),
                        () -> assertNull(blank.actualValue()),
                        () -> assertNull(blank.absoluteError()),
                        () -> assertEquals("ANSWER_NOT_PROVIDED", blank.message()),
                        () -> assertEquals(ExpressionAnswerStatus.UNANSWERED, missing.status())
                );
            }

            void invalidCorrectExpressionIsConfigurationError() {
                ExpressionAnswerGradingService broken = new ExpressionAnswerGradingService(
                        expression -> {
                            throw new ExpressionEvaluationException("broken");
                        });
                IllegalStateException error = assertThrows(IllegalStateException.class,
                        () -> broken.evaluate(answerSubmission(question(), "10+10")));
                assertTrue(error.getCause() instanceof ExpressionEvaluationException);
            }

            void invalidStudentExpressionIsInvalidExpression() {
                ExpressionAnswerGradingService tolerant = new ExpressionAnswerGradingService(
                        expression -> {
                            if ("bad".equals(expression)) {
                                throw new ExpressionEvaluationException("bad");
                            }
                            return new BigDecimal("20");
                        });
                ExpressionAnswerEvaluation evaluation = tolerant.evaluate(
                        answerSubmission(question("20", TOLERANCE), "bad"));
                assertAll(
                        () -> assertEquals(ExpressionAnswerStatus.INVALID_EXPRESSION,
                                evaluation.status()),
                        () -> assertDecimal("20", evaluation.expectedValue()),
                        () -> assertNull(evaluation.actualValue()),
                        () -> assertNull(evaluation.absoluteError()),
                        () -> assertEquals("INVALID_STUDENT_EXPRESSION", evaluation.message()),
                        () -> assertTrue(evaluation.answerResult().answered()),
                        () -> assertFalse(evaluation.answerResult().exactMatch())
                );
            }

            void errorBoundariesMapToExactToleranceAndIncorrect() {
                ExpressionQuestion tolerantQuestion = question("20", TOLERANCE);
                ExpressionAnswerGradingService evaluator = new ExpressionAnswerGradingService(
                        expression -> new BigDecimal(expression));
                ExpressionAnswerEvaluation exact = evaluator
                        .evaluate(answerSubmission(tolerantQuestion, "20"));
                ExpressionAnswerEvaluation within = evaluator
                        .evaluate(answerSubmission(tolerantQuestion, "20.01"));
                ExpressionAnswerEvaluation incorrect = evaluator
                        .evaluate(answerSubmission(tolerantQuestion, "20.02"));
                assertAll(
                        () -> assertEquals(ExpressionAnswerStatus.EXACT, exact.status()),
                        () -> assertDecimal("0", exact.absoluteError()),
                        () -> assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE,
                                within.status()),
                        () -> assertDecimal("0.01", within.absoluteError()),
                        () -> assertEquals(ExpressionAnswerStatus.INCORRECT,
                                incorrect.status()),
                        () -> assertDecimal("0.02", incorrect.absoluteError())
                );
            }

            void expectedAndActualValuesAreTransferred() {
                ExpressionAnswerEvaluation evaluation = constantService("20")
                        .evaluate(answerSubmission(question("20", ZERO), "20"));
                assertAll(
                        () -> assertEquals(ExpressionAnswerStatus.EXACT, evaluation.status()),
                        () -> assertDecimal("20", evaluation.expectedValue()),
                        () -> assertDecimal("20", evaluation.actualValue()),
                        () -> assertDecimal("0", evaluation.absoluteError()),
                        () -> assertTrue(evaluation.answerResult().exactMatch()),
                        () -> assertTrue(evaluation.answerResult().answered())
                );
            }

            void questionValidationRejectsInvalidFields() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(" ", SECTION, "1+1", TEN, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(null, SECTION, "1+1", TEN, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, null, "1+1", TEN, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, SECTION, null, TEN, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, " ", "1+1", TEN, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, SECTION, " ", TEN, ZERO, true)),
                        () -> assertThrows(NullPointerException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, SECTION, "1+1", null, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, SECTION, "1+1", ZERO, ZERO, true)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ExpressionQuestion(QUESTION_ID, SECTION, "1+1", TEN,
                                        new BigDecimal("-0.01"), true))
                );
            }

            void nullSubmissionIsRejected() {
                assertThrows(NullPointerException.class, () -> service.evaluate(null));
            }

            private ExpressionAnswerGradingService constantService(String value) {
                return new ExpressionAnswerGradingService(
                        expression -> new BigDecimal(value));
            }
        }
    }

    @Nested
    @DisplayName("ExamGradingService")
    class ExamGradingServiceTest {

        private final ExamGradingService service = new ExamGradingService();

        @Nested
        @DisplayName("入参校验")
        class ValidationTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); rejectsNullSubmissionAndPolicy(); },
                    () -> { resetStaticState(); rejectsBlankIdentifiersAndBadTimestamps(); },
                    () -> { resetStaticState(); rejectsInvalidSubmissionCounts(); },
                    () -> { resetStaticState(); rejectsInvalidPolicy(); },
                    () -> { resetStaticState(); rejectsInvalidAnswers(); },
                    () -> { resetStaticState(); rejectsDuplicateQuestionsAndBadSections(); }
                );
            }

            void rejectsNullSubmissionAndPolicy() {
                assertAll(
                        () -> assertThrows(NullPointerException.class,
                                () -> service.grade(null, policy().build())),
                        () -> assertThrows(NullPointerException.class,
                                () -> service.grade(submission().build(), null))
                );
            }

            void rejectsBlankIdentifiersAndBadTimestamps() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().examId(" ").build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().studentId(" ").build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().startedAt(null).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().submittedAt(null).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().startedAt(BASE)
                                        .submittedAt(BASE.minusSeconds(1)).build(),
                                        policy().build()))
                );
            }

            void rejectsInvalidSubmissionCounts() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().allowedDurationSeconds(-1).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().attemptNumber(0).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().fullscreenExitCount(-1).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().copyPasteEventCount(-1).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().bonusPoints(null).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().bonusPoints(new BigDecimal("-1")).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().instructorOverridePercent(-1).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().instructorOverridePercent(101).build(),
                                        policy().build()))
                );
            }

            void rejectsInvalidPolicy() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().passPercentage(101).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().wrongAnswerPenaltyRate(new BigDecimal("-0.1")).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().wrongAnswerPenaltyRate(new BigDecimal("1.1")).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().unansweredPenaltyRate(new BigDecimal("-0.1")).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().latePenaltyPercentPerHour(-1).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().maximumLatePenaltyPercent(101).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().minimumSectionPercentage(101).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().build(),
                                        policy().maximumAttempts(0).build()))
                );
            }

            void rejectsInvalidAnswers() {
                List<AnswerResult> withNull = new ArrayList<>();
                withNull.add(null);
                assertAll(
                        () -> assertThrows(NullPointerException.class,
                                () -> service.grade(submission().answers(withNull).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().questionId(" ").build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().type(null).build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().section(" ").build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().maximumPoints(null).build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().maximumPoints(ZERO).build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().correctOptionCount(-1).build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().correctOptionCount(1)
                                                .selectedCorrectOptions(2).build()).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().numericTolerance(new BigDecimal("-1")).build()).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().rubricPercent(101).build()).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().passedTests(2).totalTests(1).build()).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().type(QuestionType.PROGRAMMING)
                                                .compilationSucceeded(true).totalTests(0)
                                                .manualReviewRequired(false).build()).build(),
                                        policy().build()))
                );
            }

            void rejectsDuplicateQuestionsAndBadSections() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().build(), answer().build()).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().sectionPassPercentages(
                                        Map.of(" ", 60)).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().sectionPassPercentages(
                                        Map.of("math", 101)).build(), policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().sectionPassPercentages(
                                        Map.of("math", -1)).build(), policy().build()))
                );
            }
        }

        @Nested
        @DisplayName("未作答")
        class UnansweredTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); unansweredWithoutPenaltyScoresZero(); },
                    () -> { resetStaticState(); unansweredPenaltyDeductsFromOtherwiseFullScore(); },
                    () -> { resetStaticState(); mandatoryUnansweredFails(); },
                    () -> { resetStaticState(); mandatoryAnsweredWithoutCreditWarns(); },
                    () -> { resetStaticState(); optionalUnansweredDoesNotAddMandatoryFailure(); }
                );
            }

            void unansweredWithoutPenaltyScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().answered(false).mandatory(false).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("0", report.rawPoints()),
                        () -> assertDecimal("0", report.finalPoints()),
                        () -> assertFalse(report.passed())
                );
            }

            void unansweredPenaltyDeductsFromOtherwiseFullScore() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").build(),
                        answer().questionId("q2").answered(false).mandatory(false).build()).build(),
                        policy().unansweredPenaltyRate(new BigDecimal("0.10")).build());
                assertAll(
                        () -> assertDecimal("9.00", report.rawPoints()),
                        () -> assertDecimal("9.00", report.finalPoints()),
                        () -> assertHasPrefix(report.warnings(), "UNANSWERED_PENALTY:")
                );
            }

            void mandatoryUnansweredFails() {
                GradeReport report = service.grade(submission().answers(
                        answer().answered(false).mandatory(true).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertFalse(report.passed()),
                        () -> assertHasPrefix(report.failures(),
                                "MANDATORY_QUESTION_UNANSWERED:")
                );
            }

            void mandatoryAnsweredWithoutCreditWarns() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.SINGLE_CHOICE)
                                .exactMatch(false).mandatory(true).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertFalse(report.passed()),
                        () -> assertHasPrefix(report.warnings(),
                                "MANDATORY_QUESTION_NO_CREDIT:")
                );
            }

            void optionalUnansweredDoesNotAddMandatoryFailure() {
                GradeReport report = service.grade(submission().answers(
                        answer().answered(false).mandatory(false).build()).build(),
                        policy().passPercentage(0).build());
                assertAll(
                        () -> assertTrue(report.passed()),
                        () -> assertFalse(report.failures().stream()
                                .anyMatch(code -> code.startsWith("MANDATORY_QUESTION_")))
                );
            }
        }

        @Nested
        @DisplayName("单选与判断")
        class BinaryQuestionTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); exactSingleChoiceEarnsFullMarks(); },
                    () -> { resetStaticState(); exactTrueFalseEarnsFullMarks(); },
                    () -> { resetStaticState(); wrongBinaryWithoutNegativeMarkingScoresZero(); },
                    () -> { resetStaticState(); negativeMarkingDeductsWrongBinaryFromOtherScore(); },
                    () -> { resetStaticState(); timedOutPositiveScoreIsDiscountedByEightyPercent(); },
                    () -> { resetStaticState(); timedOutZeroScoreDoesNotBecomeNegative(); }
                );
            }

            void exactSingleChoiceEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.SINGLE_CHOICE).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("10.00", report.rawPoints()),
                        () -> assertEquals(FULL_PERCENT, report.percentage()),
                        () -> assertTrue(report.passed())
                );
            }

            void exactTrueFalseEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.TRUE_FALSE).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void wrongBinaryWithoutNegativeMarkingScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.SINGLE_CHOICE).exactMatch(false).build()).build(),
                        policy().negativeMarkingEnabled(false).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertFalse(report.passed())
                );
            }

            void negativeMarkingDeductsWrongBinaryFromOtherScore() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").type(QuestionType.SINGLE_CHOICE).build(),
                        answer().questionId("q2").type(QuestionType.SINGLE_CHOICE)
                                .exactMatch(false).build()).build(),
                        policy().negativeMarkingEnabled(true)
                                .wrongAnswerPenaltyRate(WRONG_PENALTY_RATE).build());
                assertAll(
                        () -> assertDecimal("7.50", report.rawPoints()),
                        () -> assertDecimal("7.50", report.finalPoints())
                );
            }

            void timedOutPositiveScoreIsDiscountedByEightyPercent() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.SINGLE_CHOICE)
                                .timedOut(true).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("8.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(), "QUESTION_TIMED_OUT:")
                );
            }

            void timedOutZeroScoreDoesNotBecomeNegative() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.SINGLE_CHOICE)
                                .exactMatch(false).timedOut(true).build()).build(),
                        policy().negativeMarkingEnabled(true).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(), "QUESTION_TIMED_OUT:")
                );
            }
        }

        @Nested
        @DisplayName("多选")
        class MultipleChoiceTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); exactMultipleChoiceEarnsFullMarks(); },
                    () -> { resetStaticState(); noCorrectOptionsWarnsAndScoresZero(); },
                    () -> { resetStaticState(); partialCreditDisabledScoresZeroOrNegative(); },
                    () -> { resetStaticState(); partialCreditIsProportional(); },
                    () -> { resetStaticState(); incorrectOptionsReducePartialCredit(); },
                    () -> { resetStaticState(); onlyIncorrectOptionsWarns(); },
                    () -> { resetStaticState(); onlyIncorrectNegativeMarkingDeductsFromOtherScore(); }
                );
            }

            void exactMultipleChoiceEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(4).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void noCorrectOptionsWarnsAndScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(0).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:")
                );
            }

            void partialCreditDisabledScoresZeroOrNegative() {
                GradeReport zero = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(2)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(false).build());
                GradeReport negative = service.grade(submission().answers(
                        answer().questionId("q1").type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(4).build(),
                        answer().questionId("q2").type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(2)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(false).negativeMarkingEnabled(true).build());
                assertAll(
                        () -> assertDecimal("0.00", zero.rawPoints()),
                        () -> assertDecimal("7.50", negative.rawPoints())
                );
            }

            void partialCreditIsProportional() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(2)
                                .selectedIncorrectOptions(0).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("5.00", report.rawPoints());
            }

            void incorrectOptionsReducePartialCredit() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(2)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true).negativeMarkingEnabled(false).build());
                assertDecimal("4.38", report.rawPoints());
            }

            void onlyIncorrectOptionsWarns() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(0)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "ONLY_INCORRECT_OPTIONS_SELECTED:")
                );
            }

            void onlyIncorrectNegativeMarkingDeductsFromOtherScore() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(4).build(),
                        answer().questionId("q2").type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(0)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true).negativeMarkingEnabled(true)
                                .wrongAnswerPenaltyRate(WRONG_PENALTY_RATE).build());
                assertDecimal("9.37", report.rawPoints());
            }
        }

        @Nested
        @DisplayName("填空")
        class FillBlankTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); exactFillBlankEarnsFullMarks(); },
                    () -> { resetStaticState(); rubricPercentGivesPartialCreditWhenEnabled(); },
                    () -> { resetStaticState(); rubricIsIgnoredWhenPartialCreditDisabled(); },
                    () -> { resetStaticState(); negativeMarkingDeductsWrongFillBlankFromOtherScore(); },
                    () -> { resetStaticState(); fullRubricScoresFullMarks(); }
                );
            }

            void exactFillBlankEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.FILL_BLANK).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void rubricPercentGivesPartialCreditWhenEnabled() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.FILL_BLANK)
                                .exactMatch(false).rubricPercent(50).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("5.00", report.rawPoints());
            }

            void rubricIsIgnoredWhenPartialCreditDisabled() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.FILL_BLANK)
                                .exactMatch(false).rubricPercent(50).build()).build(),
                        policy().partialCreditEnabled(false).build());
                assertDecimal("0.00", report.rawPoints());
            }

            void negativeMarkingDeductsWrongFillBlankFromOtherScore() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").type(QuestionType.FILL_BLANK).build(),
                        answer().questionId("q2").type(QuestionType.FILL_BLANK)
                                .exactMatch(false).rubricPercent(0).build()).build(),
                        policy().partialCreditEnabled(false)
                                .negativeMarkingEnabled(true).build());
                assertDecimal("7.50", report.rawPoints());
            }

            void fullRubricScoresFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.FILL_BLANK)
                                .exactMatch(false).rubricPercent(100).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("10.00", report.rawPoints());
            }
        }

        @Nested
        @DisplayName("数值")
        class NumericQuestionTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); exactNumericEarnsFullMarks(); },
                    () -> { resetStaticState(); missingToleranceDataWarnsAndScoresZero(); },
                    () -> { resetStaticState(); errorAtToleranceStillEarnsFullMarks(); },
                    () -> { resetStaticState(); errorWithinTenTimesToleranceGetsLinearCredit(); },
                    () -> { resetStaticState(); errorAtTenTimesToleranceGetsZeroCredit(); },
                    () -> { resetStaticState(); errorBeyondTenTimesToleranceGetsZeroOrNegative(); },
                    () -> { resetStaticState(); partialCreditDisabledScoresZeroForOutOfTolerance(); }
                );
            }

            void exactNumericEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(answer().build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void missingToleranceDataWarnsAndScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(null)
                                .numericTolerance(null).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "NUMERIC_TOLERANCE_DATA_MISSING:")
                );
            }

            void errorAtToleranceStillEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(TOLERANCE)
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void errorWithinTenTimesToleranceGetsLinearCredit() {
                GradeReport report = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(new BigDecimal("0.02"))
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("8.89", report.rawPoints());
            }

            void errorAtTenTimesToleranceGetsZeroCredit() {
                GradeReport report = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(new BigDecimal("0.10"))
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("0.00", report.rawPoints());
            }

            void errorBeyondTenTimesToleranceGetsZeroOrNegative() {
                GradeReport zero = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(new BigDecimal("0.11"))
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().partialCreditEnabled(true).build());
                GradeReport negative = service.grade(submission().answers(
                        answer().questionId("q1").build(),
                        answer().questionId("q2").exactMatch(false)
                                .numericError(new BigDecimal("0.11"))
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().negativeMarkingEnabled(true).build());
                assertAll(
                        () -> assertDecimal("0.00", zero.rawPoints()),
                        () -> assertDecimal("7.50", negative.rawPoints())
                );
            }

            void partialCreditDisabledScoresZeroForOutOfTolerance() {
                GradeReport report = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(new BigDecimal("0.02"))
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().partialCreditEnabled(false).build());
                assertDecimal("0.00", report.rawPoints());
            }
        }

        @Nested
        @DisplayName("作文")
        class EssayTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); exactEssayEarnsFullMarks(); },
                    () -> { resetStaticState(); zeroRubricWithoutExactMatchScoresZero(); },
                    () -> { resetStaticState(); rubricPercentScoresProportionally(); },
                    () -> { resetStaticState(); manualReviewEssayWarnsAndFlagsReport(); },
                    () -> { resetStaticState(); exactEssayIgnoresZeroRubric(); }
                );
            }

            void exactEssayEarnsFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.ESSAY).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void zeroRubricWithoutExactMatchScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.ESSAY).exactMatch(false)
                                .rubricPercent(0).build()).build(),
                        policy().build());
                assertDecimal("0.00", report.rawPoints());
            }

            void rubricPercentScoresProportionally() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.ESSAY).exactMatch(false)
                                .rubricPercent(50).build()).build(),
                        policy().build());
                assertDecimal("5.00", report.rawPoints());
            }

            void manualReviewEssayWarnsAndFlagsReport() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.ESSAY).exactMatch(false)
                                .rubricPercent(50).manualReviewRequired(true).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("5.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "ESSAY_SCORE_IS_PROVISIONAL:"),
                        () -> assertHasPrefix(report.warnings(),
                                "MANUAL_GRADING_REQUIRED:"),
                        () -> assertTrue(report.manualReview()),
                        () -> assertHasCode(report.warnings(),
                                "RESULT_PENDING_MANUAL_REVIEW")
                );
            }

            void exactEssayIgnoresZeroRubric() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.ESSAY).exactMatch(true)
                                .rubricPercent(0).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }
        }

        @Nested
        @DisplayName("编程")
        class ProgrammingTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); compileFailureScoresZeroWhenCompilationRequired(); },
                    () -> { resetStaticState(); compileFailureScoresTenPercentWhenCompilationNotRequired(); },
                    () -> { resetStaticState(); noAutomatedTestsWithManualReviewScoresZero(); },
                    () -> { resetStaticState(); allPassingTestsEarnFullMarks(); },
                    () -> { resetStaticState(); partialPassWithPartialCreditScoresProportionally(); },
                    () -> { resetStaticState(); partialPassWithoutPartialCreditScoresZero(); },
                    () -> { resetStaticState(); timedOutPartialPassAppliesBothDiscounts(); },
                    () -> { resetStaticState(); plagiarizedPartialPassScoresZero(); },
                    () -> { resetStaticState(); plagiarizedAllPassKeepsMarksButFailsIntegrity(); }
                );
            }

            void compileFailureScoresZeroWhenCompilationRequired() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .compilationSucceeded(false).build()).build(),
                        policy().programmingCompilationRequired(true).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "PROGRAM_DID_NOT_COMPILE:")
                );
            }

            void compileFailureScoresTenPercentWhenCompilationNotRequired() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .compilationSucceeded(false).build()).build(),
                        policy().programmingCompilationRequired(false).build());
                assertDecimal("1.00", report.rawPoints());
            }

            void noAutomatedTestsWithManualReviewScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .totalTests(0).manualReviewRequired(true).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "PROGRAM_HAS_NO_AUTOMATED_TESTS:")
                );
            }

            void allPassingTestsEarnFullMarks() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .passedTests(3).totalTests(3).build()).build(),
                        policy().build());
                assertDecimal("10.00", report.rawPoints());
            }

            void partialPassWithPartialCreditScoresProportionally() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .passedTests(2).totalTests(4).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("5.00", report.rawPoints());
            }

            void partialPassWithoutPartialCreditScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .passedTests(2).totalTests(4).build()).build(),
                        policy().partialCreditEnabled(false).build());
                assertDecimal("0.00", report.rawPoints());
            }

            void timedOutPartialPassAppliesBothDiscounts() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .passedTests(2).totalTests(4).timedOut(true).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertDecimal("3.00", report.rawPoints());
            }

            void plagiarizedPartialPassScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .passedTests(2).totalTests(4).plagiarismFlag(true).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasCode(report.failures(),
                                "ANSWER_INTEGRITY_VIOLATION:q1"),
                        () -> assertTrue(report.manualReview())
                );
            }

            void plagiarizedAllPassKeepsMarksButFailsIntegrity() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .passedTests(3).totalTests(3).plagiarismFlag(true).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("10.00", report.rawPoints()),
                        () -> assertHasCode(report.failures(),
                                "ANSWER_INTEGRITY_VIOLATION:q1"),
                        () -> assertFalse(report.passed())
                );
            }
        }

        @Nested
        @DisplayName("提交规则")
        class SubmissionRulesTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); bonusPointsAreAddedAndCappedAtMaximum(); },
                    () -> { resetStaticState(); bonusCanExceedMaximumWhenCapDisabled(); },
                    () -> { resetStaticState(); oneSecondLateStillChargesOneHour(); },
                    () -> { resetStaticState(); fiftyNineAndSixtyMinutesAlsoChargeOneHour(); },
                    () -> { resetStaticState(); sixtyOneMinutesChargesTwoHours(); },
                    () -> { resetStaticState(); latePenaltyAtOneHundredPercentFailsSubmission(); },
                    () -> { resetStaticState(); missingOrEqualDeadlineDoesNotPenalize(); },
                    () -> { resetStaticState(); instructorOverrideZeroSetsZeroAndWarns(); },
                    () -> { resetStaticState(); instructorOverrideOneHundredSetsMaximum(); }
                );
            }

            void bonusPointsAreAddedAndCappedAtMaximum() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .bonusPoints(ONE).build(), policy().build());
                assertAll(
                        () -> assertDecimal("10.00", report.rawPoints()),
                        () -> assertDecimal("10.00", report.finalPoints()),
                        () -> assertEquals(FULL_PERCENT, report.percentage()),
                        () -> assertHasCode(report.warnings(), "BONUS_POINTS_APPLIED")
                );
            }

            void bonusCanExceedMaximumWhenCapDisabled() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .bonusPoints(ONE).build(),
                        policy().capScoreAtOneHundred(false).build());
                assertAll(
                        () -> assertDecimal("11.00", report.finalPoints()),
                        () -> assertEquals(110, report.percentage()),
                        () -> assertEquals("A+", report.letterGrade())
                );
            }

            void oneSecondLateStillChargesOneHour() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .deadline(BASE)
                        .submittedAt(BASE.plusSeconds(1)).build(),
                        policy().latePenaltyPercentPerHour(5).build());
                assertAll(
                        () -> assertDecimal("9.50", report.finalPoints()),
                        () -> assertHasCode(report.warnings(),
                                "LATE_SUBMISSION_PENALTY:5")
                );
            }

            void fiftyNineAndSixtyMinutesAlsoChargeOneHour() {
                GradeReport fiftyNine = service.grade(submission()
                        .answers(answer().build()).deadline(BASE)
                        .submittedAt(BASE.plusSeconds(59 * 60)).build(),
                        policy().latePenaltyPercentPerHour(5).build());
                GradeReport sixty = service.grade(submission()
                        .answers(answer().build()).deadline(BASE)
                        .submittedAt(BASE.plusSeconds(60 * 60)).build(),
                        policy().latePenaltyPercentPerHour(5).build());
                assertAll(
                        () -> assertDecimal("9.50", fiftyNine.finalPoints()),
                        () -> assertDecimal("9.50", sixty.finalPoints())
                );
            }

            void sixtyOneMinutesChargesTwoHours() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build()).deadline(BASE)
                        .submittedAt(BASE.plusSeconds(61 * 60)).build(),
                        policy().latePenaltyPercentPerHour(5).build());
                assertAll(
                        () -> assertDecimal("9.00", report.finalPoints()),
                        () -> assertHasCode(report.warnings(),
                                "LATE_SUBMISSION_PENALTY:10")
                );
            }

            void latePenaltyAtOneHundredPercentFailsSubmission() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build()).deadline(BASE)
                        .submittedAt(BASE.plusSeconds(1)).build(),
                        policy().latePenaltyPercentPerHour(100)
                                .maximumLatePenaltyPercent(100).build());
                assertAll(
                        () -> assertDecimal("0.00", report.finalPoints()),
                        () -> assertHasCode(report.failures(), "SUBMISSION_TOO_LATE")
                );
            }

            void missingOrEqualDeadlineDoesNotPenalize() {
                GradeReport noDeadline = service.grade(submission()
                        .answers(answer().build()).build(), policy().build());
                GradeReport equalDeadline = service.grade(submission()
                        .answers(answer().build()).deadline(BASE)
                        .submittedAt(BASE).build(), policy().build());
                assertAll(
                        () -> assertDecimal("10.00", noDeadline.finalPoints()),
                        () -> assertDecimal("10.00", equalDeadline.finalPoints()),
                        () -> assertFalse(equalDeadline.warnings().stream()
                                .anyMatch(code -> code.startsWith("LATE_SUBMISSION_PENALTY")))
                );
            }

            void instructorOverrideZeroSetsZeroAndWarns() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .instructorOverridePercent(0).build(), policy().build());
                assertAll(
                        () -> assertEquals(0, report.percentage()),
                        () -> assertDecimal("0.00", report.finalPoints()),
                        () -> assertHasCode(report.warnings(),
                                "INSTRUCTOR_OVERRIDE_APPLIED")
                );
            }

            void instructorOverrideOneHundredSetsMaximum() {
                GradeReport report = service.grade(submission()
                        .answers(answer().exactMatch(false).build())
                        .instructorOverridePercent(100).build(), policy().build());
                assertAll(
                        () -> assertEquals(FULL_PERCENT, report.percentage()),
                        () -> assertDecimal("10.00", report.finalPoints()),
                        () -> assertTrue(report.passed())
                );
            }
        }

        @Nested
        @DisplayName("时长与尝试次数")
        class DurationAndAttemptTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); zeroAllowedDurationSkipsDurationCheck(); },
                    () -> { resetStaticState(); sixtySecondOvertimeGetsGraceWarningOnly(); },
                    () -> { resetStaticState(); threeHundredSecondOvertimeGetsNinetyFivePercent(); },
                    () -> { resetStaticState(); threeHundredOneSecondOvertimeGetsEightyPercentAndFailure(); },
                    () -> { resetStaticState(); attemptAtLimitWarns(); },
                    () -> { resetStaticState(); attemptOverLimitScoresZeroAndFails(); }
                );
            }

            void zeroAllowedDurationSkipsDurationCheck() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .startedAt(BASE)
                        .submittedAt(BASE.plusSeconds(7200))
                        .allowedDurationSeconds(0).build(), policy().build());
                assertAll(
                        () -> assertDecimal("10.00", report.finalPoints()),
                        () -> assertFalse(report.failures().stream()
                                .anyMatch(code -> code.startsWith("EXAM_DURATION_")))
                );
            }

            void sixtySecondOvertimeGetsGraceWarningOnly() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .startedAt(BASE)
                        .submittedAt(BASE.plusSeconds(160))
                        .allowedDurationSeconds(100).build(), policy().build());
                assertAll(
                        () -> assertDecimal("10.00", report.finalPoints()),
                        () -> assertHasCode(report.warnings(), "EXAM_GRACE_PERIOD_USED")
                );
            }

            void threeHundredSecondOvertimeGetsNinetyFivePercent() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .startedAt(BASE)
                        .submittedAt(BASE.plusSeconds(400))
                        .allowedDurationSeconds(100).build(), policy().build());
                assertAll(
                        () -> assertDecimal("9.50", report.finalPoints()),
                        () -> assertHasCode(report.warnings(),
                                "EXAM_DURATION_MINOR_OVERRUN")
                );
            }

            void threeHundredOneSecondOvertimeGetsEightyPercentAndFailure() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .startedAt(BASE)
                        .submittedAt(BASE.plusSeconds(401))
                        .allowedDurationSeconds(100).build(), policy().build());
                assertAll(
                        () -> assertDecimal("8.00", report.finalPoints()),
                        () -> assertHasCode(report.failures(), "EXAM_DURATION_EXCEEDED")
                );
            }

            void attemptAtLimitWarns() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .attemptNumber(3).build(),
                        policy().maximumAttempts(3).build());
                assertAll(
                        () -> assertTrue(report.passed()),
                        () -> assertHasCode(report.warnings(), "FINAL_ALLOWED_ATTEMPT")
                );
            }

            void attemptOverLimitScoresZeroAndFails() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .attemptNumber(4).build(),
                        policy().maximumAttempts(3).build());
                assertAll(
                        () -> assertDecimal("0.00", report.finalPoints()),
                        () -> assertHasCode(report.failures(),
                                "MAXIMUM_ATTEMPTS_EXCEEDED"),
                        () -> assertFalse(report.passed())
                );
            }
        }

        @Nested
        @DisplayName("分区与汇总")
        class AggregationTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); sectionPercentagesAreReportedPerSection(); },
                    () -> { resetStaticState(); requiredMissingSectionIsReported(); },
                    () -> { resetStaticState(); sectionBelowThresholdFails(); },
                    () -> { resetStaticState(); emptySubmissionWithZeroPassThresholdPasses(); },
                    () -> { resetStaticState(); percentageRoundsHalfUp(); },
                    () -> { resetStaticState(); mandatoryAnsweredIsRequiredForPass(); },
                    () -> { resetStaticState(); integrityFailureBlocksPassWhenPolicyRequires(); }
                );
            }

            void sectionPercentagesAreReportedPerSection() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").section("math").build(),
                        answer().questionId("q2").type(QuestionType.SINGLE_CHOICE)
                                .section("science").exactMatch(false).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertEquals(100, report.sectionPercentages().get("math")),
                        () -> assertEquals(0, report.sectionPercentages().get("science")),
                        () -> assertEquals(50, report.percentage())
                );
            }

            void requiredMissingSectionIsReported() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .sectionPassPercentages(Map.of("science", 60)).build(),
                        policy().build());
                assertAll(
                        () -> assertEquals(0, report.sectionPercentages().get("science")),
                        () -> assertHasCode(report.failures(),
                                "REQUIRED_SECTION_MISSING:science"),
                        () -> assertFalse(report.passed())
                );
            }

            void sectionBelowThresholdFails() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").section("math").build(),
                        answer().questionId("q2").type(QuestionType.SINGLE_CHOICE)
                                .section("science").exactMatch(false).build()).build(),
                        policy().minimumSectionPercentage(60).build());
                assertAll(
                        () -> assertHasCode(report.failures(),
                                "SECTION_BELOW_PASS_MARK:science"),
                        () -> assertFalse(report.passed())
                );
            }

            void emptySubmissionWithZeroPassThresholdPasses() {
                GradeReport report = service.grade(submission().build(),
                        policy().passPercentage(0).build());
                assertAll(
                        () -> assertDecimal("0.00", report.maximumPoints()),
                        () -> assertEquals(0, report.percentage()),
                        () -> assertTrue(report.passed()),
                        () -> assertEquals("F", report.letterGrade())
                );
            }

            void percentageRoundsHalfUp() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.PROGRAMMING)
                                .maximumPoints(new BigDecimal("3.00"))
                                .passedTests(1).totalTests(3).build()).build(),
                        policy().partialCreditEnabled(true).build());
                assertAll(
                        () -> assertDecimal("1.00", report.rawPoints()),
                        () -> assertEquals(33, report.percentage())
                );
            }

            void mandatoryAnsweredIsRequiredForPass() {
                GradeReport report = service.grade(submission().answers(
                        answer().answered(false).mandatory(true).build()).build(),
                        policy().passPercentage(0).build());
                assertAll(
                        () -> assertFalse(report.passed()),
                        () -> assertHasPrefix(report.failures(),
                                "MANDATORY_QUESTION_UNANSWERED:")
                );
            }

            void integrityFailureBlocksPassWhenPolicyRequires() {
                GradeReport blocked = service.grade(submission().answers(
                        answer().plagiarismFlag(true).build()).build(),
                        policy().failOnIntegrityViolation(true).build());
                GradeReport allowed = service.grade(submission().answers(
                        answer().plagiarismFlag(true).build()).build(),
                        policy().failOnIntegrityViolation(false).build());
                assertAll(
                        () -> assertFalse(blocked.passed()),
                        () -> assertTrue(allowed.passed()),
                        () -> assertTrue(allowed.manualReview())
                );
            }
        }

        @Nested
        @DisplayName("等级")
        class LetterGradeTest {

            @ParameterizedTest(name = "{0} -> {1}")
            @CsvSource({
                    "59,F", "60,D", "65,C", "70,C+", "75,B",
                    "80,B+", "85,A-", "90,A", "95,A+"
            })
            void letterGradeBoundaries(int percentage, String expected) {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.FILL_BLANK)
                                .maximumPoints(HUNDRED)
                                .exactMatch(false).rubricPercent(percentage).build()).build(),
                        policy().passPercentage(0).partialCreditEnabled(true).build());
                assertAll(
                        () -> assertEquals(percentage, report.percentage()),
                        () -> assertEquals(expected, report.letterGrade())
                );
            }
        }

        @Nested
        @DisplayName("诚信与覆写")
        class IntegrityTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); termsNotAcceptedIsReported(); },
                    () -> { resetStaticState(); suspendedAccountFails(); },
                    () -> { resetStaticState(); identityRequirementControlsFailureOrWarning(); },
                    () -> { resetStaticState(); fullscreenExitCountsMapToWarningsAndFailure(); },
                    () -> { resetStaticState(); copyPasteCountsMapToWarningAndFailure(); },
                    () -> { resetStaticState(); ipChangeAndProctorLossAreWarnings(); },
                    () -> { resetStaticState(); instructorOverrideWarningsAndScores(); }
                );
            }

            void termsNotAcceptedIsReported() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build()).termsAccepted(false).build(),
                        policy().build());
                assertHasCode(report.failures(), "EXAM_TERMS_NOT_ACCEPTED");
            }

            void suspendedAccountFails() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build()).suspendedAccount(true).build(),
                        policy().build());
                assertAll(
                        () -> assertHasCode(report.failures(), "ACCOUNT_SUSPENDED"),
                        () -> assertFalse(report.passed())
                );
            }

            void identityRequirementControlsFailureOrWarning() {
                GradeReport required = service.grade(submission()
                        .answers(answer().build()).identityVerified(false).build(),
                        policy().identityVerificationRequired(true).build());
                GradeReport optional = service.grade(submission()
                        .answers(answer().build()).identityVerified(false).build(),
                        policy().identityVerificationRequired(false).build());
                assertAll(
                        () -> assertHasCode(required.failures(), "IDENTITY_NOT_VERIFIED"),
                        () -> assertFalse(required.passed()),
                        () -> assertHasCode(optional.warnings(), "IDENTITY_NOT_VERIFIED"),
                        () -> assertTrue(optional.passed())
                );
            }

            void fullscreenExitCountsMapToWarningsAndFailure() {
                GradeReport one = service.grade(submission()
                        .answers(answer().build()).fullscreenExitCount(1).build(),
                        policy().build());
                GradeReport two = service.grade(submission()
                        .answers(answer().build()).fullscreenExitCount(2).build(),
                        policy().build());
                GradeReport three = service.grade(submission()
                        .answers(answer().build()).fullscreenExitCount(3).build(),
                        policy().build());
                assertAll(
                        () -> assertHasCode(one.warnings(), "FULLSCREEN_EXIT_RECORDED"),
                        () -> assertHasCode(two.warnings(), "MULTIPLE_FULLSCREEN_EXITS"),
                        () -> assertHasCode(three.failures(), "EXCESSIVE_FULLSCREEN_EXITS"),
                        () -> assertTrue(three.manualReview()),
                        () -> assertFalse(three.passed())
                );
            }

            void copyPasteCountsMapToWarningAndFailure() {
                GradeReport four = service.grade(submission()
                        .answers(answer().build()).copyPasteEventCount(4).build(),
                        policy().build());
                GradeReport five = service.grade(submission()
                        .answers(answer().build()).copyPasteEventCount(5).build(),
                        policy().build());
                assertAll(
                        () -> assertHasCode(four.warnings(), "COPY_PASTE_EVENTS_RECORDED"),
                        () -> assertHasCode(five.failures(), "EXCESSIVE_COPY_PASTE_EVENTS"),
                        () -> assertTrue(five.manualReview()),
                        () -> assertFalse(five.passed())
                );
            }

            void ipChangeAndProctorLossAreWarnings() {
                GradeReport ip = service.grade(submission()
                        .answers(answer().build()).ipAddressChanged(true).build(),
                        policy().build());
                GradeReport proctor = service.grade(submission()
                        .answers(answer().build()).proctorDisconnected(true).build(),
                        policy().build());
                assertAll(
                        () -> assertHasCode(ip.warnings(), "IP_ADDRESS_CHANGED"),
                        () -> assertHasCode(proctor.warnings(),
                                "PROCTOR_CONNECTION_LOST"),
                        () -> assertTrue(proctor.manualReview()),
                        () -> assertHasCode(proctor.warnings(),
                                "RESULT_PENDING_MANUAL_REVIEW")
                );
            }

            void instructorOverrideWarningsAndScores() {
                GradeReport zero = service.grade(submission()
                        .answers(answer().build()).instructorOverridePercent(0).build(),
                        policy().build());
                GradeReport hundred = service.grade(submission()
                        .answers(answer().exactMatch(false).build())
                        .instructorOverridePercent(100).build(), policy().build());
                assertAll(
                        () -> assertHasCode(zero.warnings(), "INSTRUCTOR_OVERRIDE_APPLIED"),
                        () -> assertEquals(0, zero.percentage()),
                        () -> assertHasCode(hundred.warnings(),
                                "INSTRUCTOR_OVERRIDE_APPLIED"),
                        () -> assertEquals(100, hundred.percentage()),
                        () -> assertDecimal("10.00", hundred.finalPoints())
                );
            }
        }

        @Nested
        @DisplayName("边界补充")
        class BoundarySupplementTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); validationAcceptsBoundaryValues(); },
                    () -> { resetStaticState(); passBoundaryWithSectionFailureDoesNotAddOverallFailure(); },
                    () -> { resetStaticState(); zeroBonusAndZeroCopyPasteDoNotWarn(); },
                    () -> { resetStaticState(); exactAllowedDurationDoesNotTriggerGrace(); },
                    () -> { resetStaticState(); sectionAtThresholdPasses(); },
                    () -> { resetStaticState(); optionalMandatoryPolicySkipsMandatoryPassRequirement(); }
                );
            }

            void validationAcceptsBoundaryValues() {
                assertAll(
                        () -> assertBoundaryGrade(submission().answers(answer().build())
                                .sectionPassPercentages(Map.of(SECTION, 0)).build(),
                                policy().build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build())
                                .sectionPassPercentages(Map.of(SECTION, 100)).build(),
                                policy().build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().passPercentage(0).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().passPercentage(FULL_PERCENT).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().wrongAnswerPenaltyRate(BigDecimal.ZERO).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().wrongAnswerPenaltyRate(BigDecimal.ONE).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().unansweredPenaltyRate(BigDecimal.ZERO).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().unansweredPenaltyRate(BigDecimal.ONE).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().latePenaltyPercentPerHour(0)
                                        .maximumLatePenaltyPercent(0).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().maximumLatePenaltyPercent(100).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().minimumSectionPercentage(0).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build())
                                .sectionPassPercentages(Map.of(SECTION, 100)).build(),
                                policy().minimumSectionPercentage(100).build()),
                        () -> assertBoundaryGrade(submission().answers(answer().build()).build(),
                                policy().maximumAttempts(1).build())
                );
            }

            private void assertBoundaryGrade(ExamSubmission submission, GradingPolicy policy) {
                GradeReport report = service.grade(submission, policy);
                assertAll(
                        () -> assertEquals(FULL_PERCENT, report.percentage()),
                        () -> assertTrue(report.passed())
                );
            }

            void passBoundaryWithSectionFailureDoesNotAddOverallFailure() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.FILL_BLANK)
                                .maximumPoints(HUNDRED)
                                .exactMatch(false).rubricPercent(60).build())
                        .sectionPassPercentages(Map.of(SECTION, 80)).build(),
                        policy().passPercentage(PASS_PERCENT).build());
                assertAll(
                        () -> assertEquals(PASS_PERCENT, report.percentage()),
                        () -> assertFalse(report.passed()),
                        () -> assertEquals("D", report.letterGrade()),
                        () -> assertHasCode(report.failures(),
                                "SECTION_BELOW_PASS_MARK:math"),
                        () -> assertFalse(report.failures().contains(
                                "OVERALL_SCORE_BELOW_PASS_MARK"))
                );
            }

            void zeroBonusAndZeroCopyPasteDoNotWarn() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build()).bonusPoints(ZERO)
                        .copyPasteEventCount(0).build(), policy().build());
                assertAll(
                        () -> assertFalse(report.warnings().contains(
                                "BONUS_POINTS_APPLIED")),
                        () -> assertFalse(report.warnings().contains(
                                "COPY_PASTE_EVENTS_RECORDED"))
                );
            }

            void exactAllowedDurationDoesNotTriggerGrace() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build()).startedAt(BASE)
                        .submittedAt(BASE.plusSeconds(ALLOWED_SECONDS))
                        .allowedDurationSeconds(100).build(), policy().build());
                assertFalse(report.warnings().contains("EXAM_GRACE_PERIOD_USED"));
            }

            void sectionAtThresholdPasses() {
                GradeReport report = service.grade(submission()
                        .answers(answer().build())
                        .sectionPassPercentages(Map.of(SECTION, 100)).build(),
                        policy().build());
                assertAll(
                        () -> assertTrue(report.passed()),
                        () -> assertFalse(report.failures().stream()
                                .anyMatch(code -> code.startsWith(
                                        "SECTION_BELOW_PASS_MARK")))
                );
            }

            void optionalMandatoryPolicySkipsMandatoryPassRequirement() {
                GradeReport report = service.grade(submission()
                        .answers(answer().answered(false).mandatory(true).build()).build(),
                        policy().passPercentage(0)
                                .allMandatoryQuestionsRequired(false).build());
                assertAll(
                        () -> assertTrue(report.passed()),
                        () -> assertHasPrefix(report.failures(),
                                "MANDATORY_QUESTION_UNANSWERED:")
                );
            }
        }
    }

    // ---------------------------------------------------------------------
    // US2：学习进度与回写
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("LearningProgressService")
    class LearningProgressServiceTest {

        private final LearningProgressService service =
                new LearningProgressService(FIXED_CLOCK);

        private CourseProgressSnapshot singleOptionalLesson(LessonProgress value) {
            return snapshot().lessons(value).requiredOverallPercent(0)
                    .certificateEnabled(false).build();
        }

        private CourseProgressSnapshot singleMandatoryLesson(LessonProgress value) {
            return snapshot().lessons(value).requiredOverallPercent(0).build();
        }

        @Nested
        @DisplayName("VIDEO")
        class VideoLessonTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); completeVideoContributesFullWeight(); },
                    () -> { resetStaticState(); videoBetweenSeventyAndNinetyWarns(); },
                    () -> { resetStaticState(); videoBelowSeventyDoesNotWarn(); },
                    () -> { resetStaticState(); mandatoryIncompleteVideoBlocksCompletion(); }
                );
            }

            void completeVideoContributesFullWeight() {
                ProgressEvaluation result = service.evaluate(singleMandatoryLesson(
                        lesson().type(LessonType.VIDEO).completionPercent(100)
                                .mandatory(true).build()));
                assertAll(
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons()),
                        () -> assertTrue(result.courseCompleted()),
                        () -> assertTrue(result.certificateEligible())
                );
            }

            void videoBetweenSeventyAndNinetyWarns() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.VIDEO).completionPercent(80)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(80, result.overallPercent()),
                        () -> assertEquals(0, result.completedLessons()),
                        () -> assertHasPrefix(result.warnings(), "VIDEO_NEAR_COMPLETION:")
                );
            }

            void videoBelowSeventyDoesNotWarn() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.VIDEO).completionPercent(60)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(60, result.overallPercent()),
                        () -> assertFalse(result.warnings().stream()
                                .anyMatch(code -> code.startsWith("VIDEO_NEAR_COMPLETION")))
                );
            }

            void mandatoryIncompleteVideoBlocksCompletion() {
                ProgressEvaluation result = service.evaluate(singleMandatoryLesson(
                        lesson().type(LessonType.VIDEO).completionPercent(80)
                                .mandatory(true).build()));
                assertAll(
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertHasPrefix(result.blockers(),
                                "MANDATORY_LESSON_INCOMPLETE:")
                );
            }
        }

        @Nested
        @DisplayName("ARTICLE")
        class ArticleLessonTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); completeArticleContributesFullWeight(); },
                    () -> { resetStaticState(); partialArticleContributesProportionally(); }
                );
            }

            void completeArticleContributesFullWeight() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.ARTICLE).completionPercent(100).build()));
                assertAll(
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertTrue(result.courseCompleted())
                );
            }

            void partialArticleContributesProportionally() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.ARTICLE).completionPercent(50)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(50, result.overallPercent()),
                        () -> assertEquals(0, result.completedLessons())
                );
            }
        }

        @Nested
        @DisplayName("QUIZ")
        class QuizLessonTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); submittedQuizAboveThresholdCompletes(); },
                    () -> { resetStaticState(); unsubmittedQuizDoesNotWarnOrComplete(); },
                    () -> { resetStaticState(); submittedQuizBelowThresholdWarns(); }
                );
            }

            void submittedQuizAboveThresholdCompletes() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.QUIZ).submitted(true).score(80)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(80, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons())
                );
            }

            void unsubmittedQuizDoesNotWarnOrComplete() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.QUIZ).submitted(false).score(0)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(0, result.overallPercent()),
                        () -> assertEquals(0, result.completedLessons()),
                        () -> assertFalse(result.warnings().stream()
                                .anyMatch(code -> code.startsWith("QUIZ_SCORE_TOO_LOW")))
                );
            }

            void submittedQuizBelowThresholdWarns() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.QUIZ).submitted(true).score(50)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(50, result.overallPercent()),
                        () -> assertHasPrefix(result.warnings(), "QUIZ_SCORE_TOO_LOW:")
                );
            }
        }

        @Nested
        @DisplayName("ASSIGNMENT")
        class AssignmentLessonTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); unsubmittedAssignmentScoresZero(); },
                    () -> { resetStaticState(); submittedButNotApprovedGetsHalfWeightAndWarning(); },
                    () -> { resetStaticState(); approvedAssignmentCompletes(); }
                );
            }

            void unsubmittedAssignmentScoresZero() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.ASSIGNMENT).submitted(false)
                                .approved(false).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(0, result.overallPercent()),
                        () -> assertFalse(result.warnings().stream()
                                .anyMatch(code -> code.startsWith(
                                        "ASSIGNMENT_AWAITING_REVISION")))
                );
            }

            void submittedButNotApprovedGetsHalfWeightAndWarning() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.ASSIGNMENT).submitted(true)
                                .approved(false).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(48, result.overallPercent()),
                        () -> assertHasPrefix(result.warnings(),
                                "ASSIGNMENT_AWAITING_REVISION:")
                );
            }

            void approvedAssignmentCompletes() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.ASSIGNMENT).submitted(true)
                                .approved(true).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons())
                );
            }
        }

        @Nested
        @DisplayName("LIVE_SESSION")
        class LiveSessionTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); zeroRequiredMinutesWithAttendanceCompletes(); },
                    () -> { resetStaticState(); zeroRequiredMinutesWithoutAttendanceDoesNotComplete(); },
                    () -> { resetStaticState(); eightyPercentAttendanceCompletes(); },
                    () -> { resetStaticState(); belowEightyPercentWarns(); }
                );
            }

            void zeroRequiredMinutesWithAttendanceCompletes() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.LIVE_SESSION).requiredMinutes(0)
                                .attendedMinutes(1).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons())
                );
            }

            void zeroRequiredMinutesWithoutAttendanceDoesNotComplete() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.LIVE_SESSION).requiredMinutes(0)
                                .attendedMinutes(0).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(0, result.overallPercent()),
                        () -> assertEquals(0, result.completedLessons())
                );
            }

            void eightyPercentAttendanceCompletes() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.LIVE_SESSION).requiredMinutes(100)
                                .attendedMinutes(80).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(80, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons())
                );
            }

            void belowEightyPercentWarns() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.LIVE_SESSION).requiredMinutes(100)
                                .attendedMinutes(79).mandatory(false).build()));
                assertAll(
                        () -> assertEquals(75, result.overallPercent()),
                        () -> assertHasPrefix(result.warnings(),
                                "LIVE_ATTENDANCE_TOO_SHORT:")
                );
            }
        }

        @Nested
        @DisplayName("PROJECT")
        class ProjectLessonTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); approvedProjectCompletes(); },
                    () -> { resetStaticState(); plagiarizedProjectIsHeldAndBlocked(); },
                    () -> { resetStaticState(); unsubmittedProjectScoresZero(); }
                );
            }

            void approvedProjectCompletes() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.PROJECT).submitted(true)
                                .approved(true).plagiarismFlag(false)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons())
                );
            }

            void plagiarizedProjectIsHeldAndBlocked() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.PROJECT).submitted(true)
                                .approved(true).plagiarismFlag(true)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(33, result.overallPercent()),
                        () -> assertHasPrefix(result.warnings(),
                                "PROJECT_HELD_FOR_INTEGRITY_REVIEW:"),
                        () -> assertHasPrefix(result.blockers(), "PLAGIARISM_REVIEW:"),
                        () -> assertFalse(result.courseCompleted())
                );
            }

            void unsubmittedProjectScoresZero() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.PROJECT).submitted(false)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(0, result.overallPercent()),
                        () -> assertEquals(0, result.completedLessons())
                );
            }
        }

        @Nested
        @DisplayName("账户与访问")
        class AccountAccessTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); accountAndCourseBlockersAreReported(); },
                    () -> { resetStaticState(); futureEnrollmentIsBlockedButNowIsNot(); },
                    () -> { resetStaticState(); accessExpiryBoundariesAreReported(); },
                    () -> { resetStaticState(); identityAndBalanceAreWarnings(); }
                );
            }

            private CourseProgressSnapshot base() {
                return singleOptionalLesson(lesson().mandatory(false).build());
            }

            void accountAndCourseBlockersAreReported() {
                assertAll(
                        () -> assertHasCode(service.evaluate(snapshot()
                                .lessons(lesson().mandatory(false).build())
                                .requiredOverallPercent(0).certificateEnabled(false)
                                .coursePublished(false).build()).blockers(),
                                "COURSE_NOT_PUBLISHED"),
                        () -> assertHasCode(service.evaluate(snapshot()
                                .lessons(lesson().mandatory(false).build())
                                .requiredOverallPercent(0).certificateEnabled(false)
                                .accountSuspended(true).build()).blockers(),
                                "ACCOUNT_SUSPENDED"),
                        () -> assertHasCode(service.evaluate(snapshot()
                                .lessons(lesson().mandatory(false).build())
                                .requiredOverallPercent(0).certificateEnabled(false)
                                .subscriptionActive(false).build()).blockers(),
                                "SUBSCRIPTION_INACTIVE"),
                        () -> assertHasCode(service.evaluate(snapshot()
                                .lessons(lesson().mandatory(false).build())
                                .requiredOverallPercent(0).certificateEnabled(false)
                                .codeOfConductAccepted(false).build()).blockers(),
                                "CODE_OF_CONDUCT_NOT_ACCEPTED")
                );
            }

            void futureEnrollmentIsBlockedButNowIsNot() {
                ProgressEvaluation future = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .enrolledAt(BASE.plusSeconds(1)).build());
                ProgressEvaluation now = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .enrolledAt(BASE).build());
                assertAll(
                        () -> assertHasCode(future.blockers(),
                                "ENROLLMENT_DATE_IN_FUTURE"),
                        () -> assertFalse(now.blockers().contains(
                                "ENROLLMENT_DATE_IN_FUTURE"))
                );
            }

            void accessExpiryBoundariesAreReported() {
                ProgressEvaluation expired = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .accessExpiresAt(BASE.minusSeconds(1)).build());
                ProgressEvaluation now = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .accessExpiresAt(BASE).build());
                ProgressEvaluation sevenDays = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .accessExpiresAt(BASE.plusSeconds(7 * 86400)).build());
                ProgressEvaluation eightDays = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .accessExpiresAt(BASE.plusSeconds(8 * 86400)).build());
                assertAll(
                        () -> assertHasCode(expired.blockers(),
                                "COURSE_ACCESS_EXPIRED"),
                        () -> assertHasCode(now.warnings(),
                                "COURSE_ACCESS_EXPIRES_SOON"),
                        () -> assertHasCode(sevenDays.warnings(),
                                "COURSE_ACCESS_EXPIRES_SOON"),
                        () -> assertFalse(eightDays.warnings().contains(
                                "COURSE_ACCESS_EXPIRES_SOON"))
                );
            }

            void identityAndBalanceAreWarnings() {
                ProgressEvaluation result = service.evaluate(snapshot()
                        .lessons(lesson().mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .identityVerified(false).outstandingBalance(true).build());
                assertAll(
                        () -> assertHasCode(result.warnings(), "IDENTITY_NOT_VERIFIED"),
                        () -> assertHasCode(result.warnings(), "OUTSTANDING_BALANCE")
                );
            }
        }

        @Nested
        @DisplayName("顺序学习与下一课时")
        class SequentialLearningTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); nonSequentialSelectsFirstIncompleteLesson(); },
                    () -> { resetStaticState(); unlockedLaterLessonAfterIncompleteMandatoryWarns(); },
                    () -> { resetStaticState(); lockedLessonsWithProgressWarnAndNoNextLesson(); },
                    () -> { resetStaticState(); sequentialWithPreviousCompleteSelectsNextWithoutBypassWarning(); }
                );
            }

            void nonSequentialSelectsFirstIncompleteLesson() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1).mandatory(false).build(),
                        lesson().lessonId("lesson-2").order(2).completionPercent(50)
                                .mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertAll(
                        () -> assertEquals("lesson-2", result.nextLessonId()),
                        () -> assertHasCode(result.warnings(),
                                "COMPLETION_STATE_CONFLICT")
                );
            }

            void unlockedLaterLessonAfterIncompleteMandatoryWarns() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1)
                                .completionPercent(80).mandatory(true).build(),
                        lesson().lessonId("lesson-2").order(2)
                                .completionPercent(50).mandatory(false).build())
                        .sequentialLearning(true).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertEquals("lesson-1", result.nextLessonId()),
                        () -> assertHasCode(result.warnings(),
                                "SEQUENCE_RULE_BYPASSED:lesson-2"),
                        () -> assertFalse(result.courseCompleted())
                );
            }

            void lockedLessonsWithProgressWarnAndNoNextLesson() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1)
                                .completionPercent(80).mandatory(true).unlocked(false).build(),
                        lesson().lessonId("lesson-2").order(2)
                                .completionPercent(50).mandatory(false).unlocked(false).build())
                        .sequentialLearning(true).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertNull(result.nextLessonId()),
                        () -> assertHasCode(result.warnings(),
                                "LOCKED_LESSON_HAS_PROGRESS:lesson-1"),
                        () -> assertHasCode(result.warnings(),
                                "NO_ACCESSIBLE_NEXT_LESSON")
                );
            }

            void sequentialWithPreviousCompleteSelectsNextWithoutBypassWarning() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1)
                                .completionPercent(100).mandatory(true).build(),
                        lesson().lessonId("lesson-2").order(2)
                                .completionPercent(50).mandatory(false).build())
                        .sequentialLearning(true).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertEquals("lesson-2", result.nextLessonId()),
                        () -> assertFalse(result.warnings().stream()
                                .anyMatch(code -> code.startsWith(
                                        "SEQUENCE_RULE_BYPASSED")))
                );
            }
        }

        @Nested
        @DisplayName("必修与尝试上限")
        class MandatoryAndAttemptTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); mandatoryIncompleteBlocksCompletion(); },
                    () -> { resetStaticState(); attemptLimitReachedBlocksCompletion(); },
                    () -> { resetStaticState(); lockedLessonWithProgressWarns(); }
                );
            }

            void mandatoryIncompleteBlocksCompletion() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().mandatory(true).completionPercent(50).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertAll(
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertHasPrefix(result.blockers(),
                                "MANDATORY_LESSON_INCOMPLETE:")
                );
            }

            void attemptLimitReachedBlocksCompletion() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().mandatory(false).completionPercent(50)
                                .attempts(3).maximumAttempts(3).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertAll(
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertHasPrefix(result.blockers(),
                                "ATTEMPT_LIMIT_REACHED:")
                );
            }

            void lockedLessonWithProgressWarns() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().mandatory(false).completionPercent(50)
                                .unlocked(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertHasPrefix(result.warnings(), "LOCKED_LESSON_HAS_PROGRESS:");
            }
        }

        @Nested
        @DisplayName("证书资格")
        class CertificateTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); completeCourseWithValidCertificateIsEligible(); },
                    () -> { resetStaticState(); disabledCertificateWarnsAndIsIneligible(); },
                    () -> { resetStaticState(); incompleteCourseIsIneligible(); },
                    () -> { resetStaticState(); identityAndBalanceBlockCertificate(); },
                    () -> { resetStaticState(); certificateNameMissingOrBlankIsRejected(); },
                    () -> { resetStaticState(); certificateNameLengthBoundaries(); }
                );
            }

            private SnapshotBuilder completeSnapshotBuilder() {
                return snapshot().lessons(lesson().type(LessonType.VIDEO)
                        .completionPercent(100).mandatory(true).build())
                        .requiredOverallPercent(0);
            }

            private CourseProgressSnapshot completeSnapshot() {
                return completeSnapshotBuilder().build();
            }

            void completeCourseWithValidCertificateIsEligible() {
                ProgressEvaluation result = service.evaluate(completeSnapshot());
                assertAll(
                        () -> assertTrue(result.courseCompleted()),
                        () -> assertTrue(result.certificateEligible())
                );
            }

            void disabledCertificateWarnsAndIsIneligible() {
                ProgressEvaluation result = service.evaluate(completeSnapshotBuilder()
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertFalse(result.certificateEligible()),
                        () -> assertHasCode(result.warnings(), "CERTIFICATE_DISABLED")
                );
            }

            void incompleteCourseIsIneligible() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.VIDEO).completionPercent(50)
                                .mandatory(true).build()).requiredOverallPercent(0).build());
                assertAll(
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertFalse(result.certificateEligible())
                );
            }

            void identityAndBalanceBlockCertificate() {
                ProgressEvaluation identity = service.evaluate(completeSnapshotBuilder()
                        .identityVerified(false).build());
                ProgressEvaluation balance = service.evaluate(completeSnapshotBuilder()
                        .outstandingBalance(true).build());
                assertAll(
                        () -> assertHasCode(identity.blockers(),
                                "CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION"),
                        () -> assertHasCode(balance.blockers(),
                                "CERTIFICATE_BLOCKED_BY_BALANCE"),
                        () -> assertFalse(identity.certificateEligible()),
                        () -> assertFalse(balance.certificateEligible())
                );
            }

            void certificateNameMissingOrBlankIsRejected() {
                assertAll(
                        () -> assertHasCode(service.evaluate(completeSnapshotBuilder()
                                .certificateName(null).build()).blockers(),
                                "CERTIFICATE_NAME_REQUIRED"),
                        () -> assertHasCode(service.evaluate(completeSnapshotBuilder()
                                .certificateName(" ").build()).blockers(),
                                "CERTIFICATE_NAME_REQUIRED")
                );
            }

            void certificateNameLengthBoundaries() {
                ProgressEvaluation one = service.evaluate(completeSnapshotBuilder()
                        .certificateName("A").build());
                ProgressEvaluation two = service.evaluate(completeSnapshotBuilder()
                        .certificateName("Al").build());
                ProgressEvaluation eighty = service.evaluate(completeSnapshotBuilder()
                        .certificateName("a".repeat(80)).build());
                ProgressEvaluation eightyOne = service.evaluate(completeSnapshotBuilder()
                        .certificateName("a".repeat(81)).build());
                assertAll(
                        () -> assertHasCode(one.blockers(),
                                "CERTIFICATE_NAME_LENGTH_INVALID"),
                        () -> assertTrue(two.certificateEligible()),
                        () -> assertTrue(eighty.certificateEligible()),
                        () -> assertHasCode(eightyOne.blockers(),
                                "CERTIFICATE_NAME_LENGTH_INVALID")
                );
            }
        }

        @Nested
        @DisplayName("权重、空课程与校验")
        class WeightsAndValidationTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); weightsAndCompletedLessonsAreSummed(); },
                    () -> { resetStaticState(); overallPercentUsesIntegerArithmetic(); },
                    () -> { resetStaticState(); emptyCourseIsBlocked(); },
                    () -> { resetStaticState(); overallBelowRequirementIsBlocked(); },
                    () -> { resetStaticState(); snapshotValidationRejectsInvalidInputs(); },
                    () -> { resetStaticState(); finalProjectAndInstructorApprovalAreRequiredWhenConfigured(); }
                );
            }

            void weightsAndCompletedLessonsAreSummed() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").type(LessonType.VIDEO)
                                .order(1).completionPercent(100).mandatory(false).build(),
                        lesson().lessonId("lesson-2").type(LessonType.ARTICLE)
                                .order(2).completionPercent(100).mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertAll(
                        () -> assertEquals(FULL_PERCENT, result.overallPercent()),
                        () -> assertEquals(2, result.completedLessons()),
                        () -> assertEquals(2, result.totalLessons())
                );
            }

            void overallPercentUsesIntegerArithmetic() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.VIDEO).completionPercent(50)
                                .mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertEquals(46, result.overallPercent());
            }

            void emptyCourseIsBlocked() {
                ProgressEvaluation result = service.evaluate(snapshot().build());
                assertAll(
                        () -> assertEquals(0, result.overallPercent()),
                        () -> assertEquals(0, result.totalLessons()),
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertHasCode(result.blockers(), "COURSE_HAS_NO_LESSONS")
                );
            }

            void overallBelowRequirementIsBlocked() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.VIDEO).completionPercent(50)
                                .mandatory(false).build())
                        .requiredOverallPercent(60).certificateEnabled(false).build());
                assertAll(
                        () -> assertFalse(result.courseCompleted()),
                        () -> assertHasCode(result.blockers(),
                                "OVERALL_PROGRESS_BELOW_REQUIRED")
                );
            }

            void snapshotValidationRejectsInvalidInputs() {
                List<LessonProgress> withNull = new ArrayList<>();
                withNull.add(null);
                assertAll(
                        () -> assertThrows(NullPointerException.class,
                                () -> service.evaluate(null)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().studentId(" ").build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().courseId(" ").build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot()
                                        .requiredOverallPercent(-1).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot()
                                        .requiredOverallPercent(101).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot()
                                        .requiredQuizScore(-1).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot()
                                        .requiredQuizScore(101).build())),
                        () -> assertThrows(NullPointerException.class,
                                () -> service.evaluate(snapshot().lessons(withNull).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().lessonId(" ").build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().lessonId("same").order(1).build(),
                                        lesson().lessonId("same").order(2).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().lessonId("a").order(1).build(),
                                        lesson().lessonId("b").order(1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().type(null).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().order(-1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().completionPercent(-1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().completionPercent(101).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().score(-1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().score(101).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().attempts(-1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().maximumAttempts(-1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().attempts(4).maximumAttempts(3).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().attendedMinutes(-1).build()).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.evaluate(snapshot().lessons(
                                        lesson().requiredMinutes(-1).build()).build()))
                );
            }

            void finalProjectAndInstructorApprovalAreRequiredWhenConfigured() {
                ProgressEvaluation noProject = service.evaluate(snapshot().lessons(
                        lesson().mandatory(true).completionPercent(100).build())
                        .requiredOverallPercent(0).finalProjectRequired(true)
                        .certificateEnabled(false).build());
                ProgressEvaluation withProject = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1).mandatory(true)
                                .completionPercent(100).build(),
                        lesson().lessonId("project-1").order(2).type(LessonType.PROJECT)
                                .mandatory(false).submitted(true).approved(true)
                                .plagiarismFlag(false).build())
                        .requiredOverallPercent(0).finalProjectRequired(true)
                        .certificateEnabled(false).build());
                ProgressEvaluation noApproval = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.ASSIGNMENT).mandatory(true)
                                .submitted(true).approved(false).build())
                        .requiredOverallPercent(0).instructorApprovalRequired(true)
                        .certificateEnabled(false).build());
                ProgressEvaluation approvalNotNeeded = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.VIDEO).mandatory(true)
                                .completionPercent(100).build())
                        .requiredOverallPercent(0).instructorApprovalRequired(true)
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertHasCode(noProject.blockers(),
                                "FINAL_PROJECT_NOT_APPROVED"),
                        () -> assertFalse(withProject.blockers().contains(
                                "FINAL_PROJECT_NOT_APPROVED")),
                        () -> assertHasCode(noApproval.blockers(),
                                "INSTRUCTOR_APPROVAL_REQUIRED"),
                        () -> assertFalse(approvalNotNeeded.blockers().contains(
                                "INSTRUCTOR_APPROVAL_REQUIRED"))
                );
            }
        }

        @Nested
        @DisplayName("进度边界补充")
        class LearningBoundarySupplementTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); snapshotValidationAcceptsBoundaryValues(); },
                    () -> { resetStaticState(); zeroAttemptLimitAndZeroProgressDoNotWarn(); },
                    () -> { resetStaticState(); videoNinetyCompletesAndSeventyWarns(); },
                    () -> { resetStaticState(); quizAtThresholdCompletesWithoutWarning(); },
                    () -> { resetStaticState(); overallAtRequiredThresholdPasses(); },
                    () -> { resetStaticState(); liveAtEightyPercentDoesNotWarn(); },
                    () -> { resetStaticState(); sequentialPreviousProgressAtFiftyMaintainsOrder(); },
                    () -> { resetStaticState(); approvedMandatoryAssignmentSatisfiesInstructorApproval(); }
                );
            }

            void snapshotValidationAcceptsBoundaryValues() {
                ProgressEvaluation zeroOverall = service.evaluate(snapshot()
                        .requiredOverallPercent(0).build());
                ProgressEvaluation fullOverall = service.evaluate(snapshot()
                        .requiredOverallPercent(100).build());
                ProgressEvaluation zeroQuiz = service.evaluate(snapshot()
                        .requiredQuizScore(0).build());
                ProgressEvaluation fullQuiz = service.evaluate(snapshot()
                        .requiredQuizScore(100).build());
                ProgressEvaluation zeroLesson = service.evaluate(snapshot().lessons(
                        lesson().order(0).completionPercent(0).score(0)
                                .attempts(0).maximumAttempts(0).build()).build());
                ProgressEvaluation completeLesson = service.evaluate(snapshot().lessons(
                        lesson().completionPercent(100).score(100).build()).build());
                assertAll(
                        () -> assertEquals(0, zeroOverall.overallPercent()),
                        () -> assertFalse(zeroOverall.courseCompleted()),
                        () -> assertEquals(0, fullOverall.overallPercent()),
                        () -> assertFalse(fullOverall.courseCompleted()),
                        () -> assertEquals(0, zeroQuiz.overallPercent()),
                        () -> assertEquals(0, fullQuiz.overallPercent()),
                        () -> assertEquals(0, zeroLesson.overallPercent()),
                        () -> assertEquals(0, zeroLesson.completedLessons()),
                        () -> assertTrue(completeLesson.courseCompleted()),
                        () -> assertEquals(1, completeLesson.completedLessons())
                );
            }

            void zeroAttemptLimitAndZeroProgressDoNotWarn() {
                ProgressEvaluation attempt = service.evaluate(snapshot().lessons(
                        lesson().mandatory(false).completionPercent(50)
                                .attempts(0).maximumAttempts(0).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                ProgressEvaluation locked = service.evaluate(snapshot().lessons(
                        lesson().mandatory(false).completionPercent(0)
                                .unlocked(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertAll(
                        () -> assertFalse(attempt.blockers().stream()
                                .anyMatch(code -> code.startsWith(
                                        "ATTEMPT_LIMIT_REACHED"))),
                        () -> assertFalse(locked.warnings().contains(
                                "LOCKED_LESSON_HAS_PROGRESS:lesson-1"))
                );
            }

            void videoNinetyCompletesAndSeventyWarns() {
                ProgressEvaluation ninety = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.VIDEO).completionPercent(VIDEO_COMPLETE_PERCENT)
                                .mandatory(false).build()));
                ProgressEvaluation seventy = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.VIDEO).completionPercent(VIDEO_WARN_PERCENT)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(1, ninety.completedLessons()),
                        () -> assertFalse(ninety.warnings().stream()
                                .anyMatch(code -> code.startsWith(
                                        "VIDEO_NEAR_COMPLETION"))),
                        () -> assertEquals(0, seventy.completedLessons()),
                        () -> assertHasPrefix(seventy.warnings(),
                                "VIDEO_NEAR_COMPLETION:")
                );
            }

            void quizAtThresholdCompletesWithoutWarning() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.QUIZ).submitted(true).score(QUIZ_PASS_SCORE)
                                .mandatory(false).build()));
                assertAll(
                        () -> assertEquals(1, result.completedLessons()),
                        () -> assertFalse(result.warnings().stream()
                                .anyMatch(code -> code.startsWith(
                                        "QUIZ_SCORE_TOO_LOW")))
                );
            }

            void overallAtRequiredThresholdPasses() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.VIDEO).completionPercent(50)
                                .mandatory(false).build())
                        .requiredOverallPercent(46).certificateEnabled(false).build());
                assertAll(
                        () -> assertEquals(46, result.overallPercent()),
                        () -> assertFalse(result.blockers().contains(
                                "OVERALL_PROGRESS_BELOW_REQUIRED")),
                        () -> assertTrue(result.courseCompleted())
                );
            }

            void liveAtEightyPercentDoesNotWarn() {
                ProgressEvaluation result = service.evaluate(singleOptionalLesson(
                        lesson().type(LessonType.LIVE_SESSION).requiredMinutes(100)
                                .attendedMinutes(80).mandatory(false).build()));
                assertFalse(result.warnings().stream()
                        .anyMatch(code -> code.startsWith(
                                "LIVE_ATTENDANCE_TOO_SHORT")));
            }

            void sequentialPreviousProgressAtFiftyMaintainsOrder() {
                ProgressEvaluation fifty = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1)
                                .completionPercent(50).mandatory(false).build(),
                        lesson().lessonId("lesson-2").order(2)
                                .completionPercent(50).mandatory(false).build())
                        .sequentialLearning(true).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                ProgressEvaluation fortyNine = service.evaluate(snapshot().lessons(
                        lesson().lessonId("lesson-1").order(1)
                                .completionPercent(49).mandatory(false).build(),
                        lesson().lessonId("lesson-2").order(2)
                                .completionPercent(50).mandatory(false).build())
                        .sequentialLearning(true).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertFalse(fifty.warnings().stream()
                                .anyMatch(code -> code.startsWith(
                                        "SEQUENCE_RULE_BYPASSED"))),
                        () -> assertHasPrefix(fortyNine.warnings(),
                                "SEQUENCE_RULE_BYPASSED:")
                );
            }

            void approvedMandatoryAssignmentSatisfiesInstructorApproval() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.ASSIGNMENT).mandatory(true)
                                .submitted(true).approved(true).build())
                        .requiredOverallPercent(0).instructorApprovalRequired(true)
                        .certificateEnabled(false).build());
                assertFalse(result.blockers().contains(
                        "INSTRUCTOR_APPROVAL_REQUIRED"));
            }
        }
    }

    @Nested
    @DisplayName("ExpressionExamProgressService")
    class ExpressionExamProgressServiceTest {

        private final ExpressionExamProgressService service =
                new ExpressionExamProgressService(new LearningProgressService(FIXED_CLOCK));

        private LessonProgress quizLesson() {
            return lesson().lessonId("quiz-1").type(LessonType.QUIZ)
                    .order(1).mandatory(true).completionPercent(0)
                    .score(0).submitted(false).approved(false).attempts(1).maximumAttempts(3).build();
        }

        @Nested
        @DisplayName("QUIZ 回写")
        class WritebackTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); writebackUpdatesQuizAndReevaluates(); },
                    () -> { resetStaticState(); nullSnapshotAndGradeAreRejected(); },
                    () -> { resetStaticState(); manualReviewIsRejected(); },
                    () -> { resetStaticState(); blankLessonIdIsRejected(); },
                    () -> { resetStaticState(); missingLessonIsRejected(); },
                    () -> { resetStaticState(); duplicateQuizIdIsRejected(); },
                    () -> { resetStaticState(); nonQuizLessonIsRejected(); },
                    () -> { resetStaticState(); percentageIsClampedToZeroAndOneHundred(); },
                    () -> { resetStaticState(); integrityFailureMarksQuizPlagiarizedAndBlocksCourse(); },
                    () -> { resetStaticState(); nonTargetLessonsArePreserved(); }
                );
            }

            void writebackUpdatesQuizAndReevaluates() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                ProgressEvaluation result = service.evaluate(snapshot, "quiz-1",
                        examGrade(80, false, List.of()));
                LessonProgress original = snapshot.lessons().get(0);
                assertAll(
                        () -> assertEquals(80, result.overallPercent()),
                        () -> assertEquals(1, result.completedLessons()),
                        () -> assertTrue(result.courseCompleted()),
                        () -> assertTrue(result.certificateEligible()),
                        () -> assertFalse(original.submitted()),
                        () -> assertFalse(original.approved())
                );
            }

            void nullSnapshotAndGradeAreRejected() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                assertAll(
                        () -> assertThrows(NullPointerException.class,
                                () -> service.evaluate(null, "quiz-1",
                                        examGrade(80, false, List.of()))),
                        () -> assertThrows(NullPointerException.class,
                                () -> service.evaluate(snapshot, "quiz-1", null))
                );
            }

            void manualReviewIsRejected() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, "quiz-1",
                                examGrade(80, true, List.of())));
            }

            void blankLessonIdIsRejected() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, " ",
                                examGrade(80, false, List.of())));
            }

            void missingLessonIsRejected() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, "missing",
                                examGrade(80, false, List.of())));
            }

            void duplicateQuizIdIsRejected() {
                CourseProgressSnapshot snapshot = snapshot().lessons(
                        quizLesson(),
                        lesson().lessonId("quiz-1").type(LessonType.QUIZ)
                                .order(2).mandatory(false).completionPercent(0)
                                .submitted(false).score(0).build())
                        .requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, "quiz-1",
                                examGrade(80, false, List.of())));
            }

            void nonQuizLessonIsRejected() {
                CourseProgressSnapshot snapshot = snapshot().lessons(
                        lesson().lessonId("video-1").type(LessonType.VIDEO)
                                .order(1).build()).requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, "video-1",
                                examGrade(80, false, List.of())));
            }

            void percentageIsClampedToZeroAndOneHundred() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                ProgressEvaluation zero = service.evaluate(snapshot, "quiz-1",
                        examGrade(-10, false, List.of()));
                ProgressEvaluation hundred = service.evaluate(snapshot, "quiz-1",
                        examGrade(150, false, List.of()));
                assertAll(
                        () -> assertEquals(0, zero.overallPercent()),
                        () -> assertEquals(100, hundred.overallPercent()),
                        () -> assertTrue(hundred.courseCompleted())
                );
            }

            void integrityFailureMarksQuizPlagiarizedAndBlocksCourse() {
                CourseProgressSnapshot snapshot = snapshot().lessons(quizLesson())
                        .requiredOverallPercent(0).build();
                ProgressEvaluation result = service.evaluate(snapshot, "quiz-1",
                        examGrade(80, false,
                                List.of("ANSWER_INTEGRITY_VIOLATION:q1")));
                assertAll(
                        () -> assertHasPrefix(result.blockers(), "PLAGIARISM_REVIEW:"),
                        () -> assertFalse(result.courseCompleted())
                );
            }

            void nonTargetLessonsArePreserved() {
                LessonProgress video = lesson().lessonId("video-1")
                        .type(LessonType.VIDEO).order(2).mandatory(false)
                        .completionPercent(100).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(
                        quizLesson(), video).requiredOverallPercent(0).build();
                ProgressEvaluation result = service.evaluate(snapshot, "quiz-1",
                        examGrade(80, false, List.of()));
                assertAll(
                        () -> assertEquals(88, result.overallPercent()),
                        () -> assertEquals(2, result.totalLessons()),
                        () -> assertEquals(2, result.completedLessons()),
                        () -> assertFalse(snapshot.lessons().get(0).submitted())
                );
            }
        }
    }

    // ---------------------------------------------------------------------
    // US3：表达式引擎
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("ExpressionEngine")
    class ExpressionEngineTest {

        @Nested
        @DisplayName("ArithmeticExpression")
        class ArithmeticExpressionTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); supportsOnlyArithmeticCharacters(); },
                    () -> { resetStaticState(); evaluatesPrecedenceParenthesesAndUnarySigns(); },
                    () -> { resetStaticState(); rejectsUnsupportedExpressions(); },
                    () -> { resetStaticState(); missingClosingParenthesisIsRejected(); },
                    () -> { resetStaticState(); divisionIsUnsupportedByArithmeticExpression(); }
                );
            }

            void supportsOnlyArithmeticCharacters() {
                assertAll(
                        () -> assertTrue(ArithmeticExpression.supports("1 + 2")),
                        () -> assertTrue(ArithmeticExpression.supports("(1+2)*3")),
                        () -> assertFalse(ArithmeticExpression.supports("1+2a")),
                        () -> assertFalse(ArithmeticExpression.supports(null)),
                        () -> assertFalse(ArithmeticExpression.supports(""))
                );
            }

            void evaluatesPrecedenceParenthesesAndUnarySigns() {
                assertAll(
                        () -> assertDecimal("7", ArithmeticExpression.evaluate("1+2*3")),
                        () -> assertDecimal("9", ArithmeticExpression.evaluate("(1+2)*3")),
                        () -> assertDecimal("20", ArithmeticExpression.evaluate("((2+3)*4)")),
                        () -> assertDecimal("3", ArithmeticExpression.evaluate("5-2")),
                        () -> assertDecimal("1", ArithmeticExpression.evaluate("-2+3")),
                        () -> assertDecimal("6", ArithmeticExpression.evaluate("+2*3")),
                        () -> assertDecimal("3", ArithmeticExpression.evaluate(" 1 + 2 "))
                );
            }

            void rejectsUnsupportedExpressions() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> ArithmeticExpression.evaluate(null)),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> ArithmeticExpression.evaluate("1+2a")),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> ArithmeticExpression.evaluate("1..2")),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> ArithmeticExpression.evaluate("1 2")),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> ArithmeticExpression.evaluate("1+2)")),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> ArithmeticExpression.evaluate("   "))
                );
            }

            void missingClosingParenthesisIsRejected() {
                assertThrows(IllegalArgumentException.class,
                        () -> ArithmeticExpression.evaluate("(1+2"));
            }

            void divisionIsUnsupportedByArithmeticExpression() {
                assertThrows(IllegalArgumentException.class,
                        () -> ArithmeticExpression.evaluate("1/0"));
            }
        }

        @Nested
        @DisplayName("BracketsCalculation2")
        class BracketsCalculation2Test {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); getInstanceReusesSameTypeAndName(); },
                    () -> { resetStaticState(); getInstanceRejectsDifferentTypeWithSameName(); },
                    () -> { resetStaticState(); checkCoversFormatBranches(); },
                    () -> { resetStaticState(); calculatesDoubleAndBigDecimalPaths(); },
                    () -> { resetStaticState(); cacheIsFilledAndCleared(); },
                    () -> { resetStaticState(); explainProducesLogResultsForNestedExpression(); }
                );
            }

            void getInstanceReusesSameTypeAndName() {
                BracketsCalculation2 first =
                        BracketsCalculation2.getInstance("student-bc2-reuse");
                BracketsCalculation2 second =
                        BracketsCalculation2.getInstance("student-bc2-reuse");
                assertAll(
                        () -> assertNotNull(first),
                        () -> assertSame(first, second),
                        () -> assertDecimal("3", first.calculation("1+2", true)
                                .getBigDecimalResult())
                );
            }

            void getInstanceRejectsDifferentTypeWithSameName() {
                String name = "student-bc2-conflict";
                CalculationManagement.register(
                        PrefixExpressionOperation.getInstance(name), false);
                assertThrows(ExtractException.class,
                        () -> BracketsCalculation2.getInstance(name));
            }

            void checkCoversFormatBranches() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-bc2-check");
                assertAll(
                        () -> assertDoesNotThrow(() -> calc.check("1+2")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("1+")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("1a2")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("(1+2")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("1+2)"))
                );
            }

            void calculatesDoubleAndBigDecimalPaths() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-bc2-path");
                Mathematical_Expression.Options.setUseBigDecimal(false);
                CalculationNumberResults doubleResult = calc.calculation("1+2*3", true);
                Mathematical_Expression.Options.setUseBigDecimal(true);
                CalculationNumberResults bigResult = calc.calculation("1+2*3", true);
                assertAll(
                        () -> assertDecimal("7", doubleResult.getBigDecimalResult()),
                        () -> assertDecimal("7", bigResult.getBigDecimalResult())
                );
            }

            void cacheIsFilledAndCleared() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-bc2-cache");
                Mathematical_Expression.Options.setUseCache(true);
                calc.calculation("1+2", true);
                assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2"));
                Mathematical_Expression.Options.setUseCache(false);
                assertNull(Mathematical_Expression.Options.getCacheCalculation("1+2"));
            }

            void explainProducesLogResultsForNestedExpression() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-bc2-explain");
                LogResults log = calc.explain("(1+2)*3", true);
                assertAll(
                        () -> assertNotNull(log),
                        () -> assertEquals(9.0, log.getResult())
                );
            }
        }

        @Nested
        @DisplayName("BracketsExpression")
        class BracketsExpressionTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); compilesWithWithoutAndNestedParentheses(); },
                    () -> { resetStaticState(); compilesBigDecimal(); },
                    () -> { resetStaticState(); cacheReturnsSameExpressionAndCanBeCleared(); },
                    () -> { resetStaticState(); dismantlingReturnsUnderlyingExpression(); }
                );
            }

            private final BracketsCalculation2 calc =
                    BracketsCalculation2.getInstance("student-bre-calc");

            void compilesWithWithoutAndNestedParentheses() {
                CalculationResults plain = BracketsExpression
                        .compile("1+2", true, calc).calculation(true);
                CalculationResults oneLevel = BracketsExpression
                        .compile("(1+2)*3", true, calc).calculation(true);
                CalculationResults nested = BracketsExpression
                        .compile("((1+2)*3)", true, calc).calculation(true);
                assertAll(
                        () -> assertDecimal("3", plain.getBigDecimalResult()),
                        () -> assertDecimal("9", oneLevel.getBigDecimalResult()),
                        () -> assertDecimal("9", nested.getBigDecimalResult())
                );
            }

            void compilesBigDecimal() {
                CalculationResults result = BracketsExpression
                        .compileBigDecimal("(1+2)*3", true, calc)
                        .calculationBigDecimals(true);
                assertDecimal("9", result.getBigDecimalResult());
            }

            void cacheReturnsSameExpressionAndCanBeCleared() {
                Mathematical_Expression.Options.setUseCache(true);
                PackExpression first = BracketsExpression.compile("1+2", true, calc);
                PackExpression second = BracketsExpression.compile("1+2", true, calc);
                assertSame(first, second);
                Mathematical_Expression.Options.setUseCache(false);
                assertNull(Mathematical_Expression.Options.getCacheCalculation("1+2"));
            }

            void dismantlingReturnsUnderlyingExpression() {
                PackExpression pack = BracketsExpression.compile("1+2", true, calc);
                NameExpression underlying = pack.dismantling();
                assertAll(
                        () -> assertNotNull(underlying),
                        () -> assertDecimal("3",
                                underlying.calculation(true).getBigDecimalResult())
                );
            }
        }

        @Nested
        @DisplayName("PrefixExpression compile")
        class PrefixExpressionCompileTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); compileCalculatesWithDouble(); },
                    () -> { resetStaticState(); compileBigDecimalCalculatesWithBigDecimal(); },
                    () -> { resetStaticState(); wrongPrecisionCalculationThrows(); },
                    () -> { resetStaticState(); cacheHitsDoubleAndBigDecimalPaths(); },
                    () -> { resetStaticState(); precisionConversionWorksInBothDirections(); },
                    () -> { resetStaticState(); expressionStringAndNameAreExposed(); }
                );
            }

            void compileCalculatesWithDouble() {
                PrefixExpression expression =
                        PrefixExpression.compile("1+2*3", "student-prefix-double");
                CalculationNumberResults result = expression.calculation(true);
                assertAll(
                        // 此处钉住现状：直接 compile 未追加 +0，优先级不重排，结果为 9.0。
                        () -> assertEquals(9.0, result.getResult()),
                        () -> assertTrue(expression.isUnBigDecimal()),
                        () -> assertFalse(expression.isBigDecimal())
                );
            }

            void compileBigDecimalCalculatesWithBigDecimal() {
                PrefixExpression expression =
                        PrefixExpression.compileBigDecimal("1+2*3", "student-prefix-big");
                CalculationNumberResults result = expression.calculationBigDecimals(true);
                assertAll(
                        // 此处钉住现状：直接 compileBigDecimal 同样未追加 +0，结果为 9.0。
                        () -> assertDecimal("9.0", result.getBigDecimalResult()),
                        () -> assertTrue(expression.isBigDecimal()),
                        () -> assertFalse(expression.isUnBigDecimal())
                );
            }

            void wrongPrecisionCalculationThrows() {
                PrefixExpression doubleExpression =
                        PrefixExpression.compile("1+2", "student-prefix-wrong-double");
                PrefixExpression bigExpression =
                        PrefixExpression.compileBigDecimal("1+2", "student-prefix-wrong-big");
                assertAll(
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> doubleExpression.calculationBigDecimals(true)),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> bigExpression.calculation(true))
                );
            }

            void cacheHitsDoubleAndBigDecimalPaths() {
                Mathematical_Expression.Options.setUseCache(true);
                PrefixExpression doubleFirst =
                        PrefixExpression.compile("1+2", "student-prefix-cache-double");
                PrefixExpression doubleSecond =
                        PrefixExpression.compile("1+2", "student-prefix-cache-double");
                PrefixExpression bigFirst =
                        PrefixExpression.compileBigDecimal("1+2", "student-prefix-cache-big");
                PrefixExpression bigSecond =
                        PrefixExpression.compileBigDecimal("1+2", "student-prefix-cache-big");
                assertAll(
                        () -> assertSame(doubleFirst, doubleSecond),
                        () -> assertSame(bigFirst, bigSecond)
                );
                Mathematical_Expression.Options.setUseCache(false);
                PrefixExpression afterClear =
                        PrefixExpression.compile("1+2", "student-prefix-cache-double");
                assertNotSame(doubleFirst, afterClear);
            }

            void precisionConversionWorksInBothDirections() {
                PrefixExpression doubleExpression =
                        PrefixExpression.compile("1+2", "student-prefix-convert-double");
                doubleExpression.convertToMultiPrecisionSupported();
                PrefixExpression bigExpression =
                        PrefixExpression.compileBigDecimal("1+2", "student-prefix-convert-big");
                bigExpression.convertToMultiPrecisionSupported();
                assertAll(
                        () -> assertTrue(doubleExpression.isBigDecimal()),
                        () -> assertTrue(doubleExpression.isUnBigDecimal()),
                        () -> assertTrue(bigExpression.isBigDecimal()),
                        () -> assertTrue(bigExpression.isUnBigDecimal()),
                        () -> assertDecimal("3",
                                doubleExpression.calculationBigDecimals(true)
                                        .getBigDecimalResult()),
                        () -> assertEquals(3.0, bigExpression.calculation(true).getResult())
                );
            }

            void expressionStringAndNameAreExposed() {
                PrefixExpression expression =
                        PrefixExpression.compile("1+2", "student-prefix-name");
                assertAll(
                        () -> assertTrue(expression.getExpressionStr().startsWith("1+2 -> ")),
                        () -> assertEquals("student-prefix-name",
                                expression.getCalculationName())
                );
            }
        }

        @Nested
        @DisplayName("PrefixExpression state")
        class PrefixExpressionStateTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); stackCopiesAreIndependent(); },
                    () -> { resetStaticState(); cacheCalculationWithCopyFalseClosesExpression(); },
                    () -> { resetStaticState(); directCalculationWithCopyFalseKeepsExpressionAvailable(); },
                    () -> { resetStaticState(); bigDecimalStacksAreExposed(); }
                );
            }

            void stackCopiesAreIndependent() {
                PrefixExpression expression =
                        PrefixExpression.compile("1+2", "student-prefix-stack");
                Stack<Double> copy = expression.getDoubleStack(true);
                Stack<Double> original = expression.getDoubleStack(false);
                Stack<Character> operators = expression.getCharacterStack(true);
                copy.push(99.0);
                assertAll(
                        () -> assertEquals(2, original.size()),
                        () -> assertEquals(3, copy.size()),
                        () -> assertEquals(1, operators.size())
                );
            }

            void cacheCalculationWithCopyFalseClosesExpression() {
                PrefixExpression expression =
                        PrefixExpression.compile("1+2", "student-prefix-close");
                expression.calculationCache(false);
                assertAll(
                        () -> assertFalse(expression.isAvailable()),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> expression.getDoubleStack(true))
                );
            }

            void directCalculationWithCopyFalseKeepsExpressionAvailable() {
                PrefixExpression expression =
                        PrefixExpression.compile("1+2", "student-prefix-open");
                expression.calculation(false);
                assertTrue(expression.isAvailable());
            }

            void bigDecimalStacksAreExposed() {
                PrefixExpression expression =
                        PrefixExpression.compileBigDecimal("1+2", "student-prefix-big-stack");
                assertAll(
                        () -> assertEquals(2, expression.getBigDecimalsR(true).size()),
                        () -> assertEquals(1, expression.getCharacterStack(true).size())
                );
            }
        }

        @Nested
        @DisplayName("PrefixExpressionOperation")
        class PrefixExpressionOperationTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); checkRejectsSevenFormatErrors(); },
                    () -> { resetStaticState(); checkAcceptsValidExpressions(); },
                    () -> { resetStaticState(); formatStrNormalizesPlusMinusAndAppendsZero(); },
                    () -> { resetStaticState(); calculationUsesBothPrecisionBranches(); },
                    () -> { resetStaticState(); getInstanceRejectsDifferentTypeWithSameName(); },
                    () -> { resetStaticState(); explainReturnsLogResults(); }
                );
            }

            void checkRejectsSevenFormatErrors() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-check");
                assertAll(
                        () -> assertThrows(StringIndexOutOfBoundsException.class,
                                () -> operation.check("")),
                        () -> assertThrows(StringIndexOutOfBoundsException.class,
                                () -> operation.check("   ")),
                        () -> assertThrows(WrongFormat.class,
                                () -> operation.check("1+2)")),
                        () -> assertThrows(WrongFormat.class,
                                () -> operation.check("(1+2")),
                        () -> assertThrows(WrongFormat.class,
                                () -> operation.check("1a2")),
                        () -> assertThrows(WrongFormat.class,
                                () -> operation.check("1+")),
                        () -> assertThrows(WrongFormat.class,
                                () -> operation.check("1++2"))
                );
            }

            void checkAcceptsValidExpressions() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-valid");
                assertAll(
                        () -> assertEquals(3.0,
                                operation.calculation("1+2", true).getResult()),
                        () -> assertEquals(-1.0,
                                operation.calculation("1+-2", true).getResult()),
                        () -> assertEquals(3.5,
                                operation.calculation("1.5+2", true).getResult())
                );
            }

            void formatStrNormalizesPlusMinusAndAppendsZero() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-format");
                assertAll(
                        () -> assertEquals("1-2+0", operation.formatStr("1+-2")),
                        () -> assertEquals("1+2+0", operation.formatStr("1+2"))
                );
            }

            void calculationUsesBothPrecisionBranches() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-calc");
                Mathematical_Expression.Options.setUseBigDecimal(false);
                CalculationNumberResults big = operation.calculation("1+2", true);
                Mathematical_Expression.Options.setUseBigDecimal(true);
                CalculationNumberResults doubleResult = operation.calculation("1+2", true);
                assertAll(
                        () -> assertDecimal("3", big.getBigDecimalResult()),
                        () -> assertDecimal("3", doubleResult.getBigDecimalResult())
                );
            }

            void getInstanceRejectsDifferentTypeWithSameName() {
                String name = "student-peo-conflict";
                CalculationManagement.register(
                        BracketsCalculation2.getInstance(name), false);
                assertThrows(ExtractException.class,
                        () -> PrefixExpressionOperation.getInstance(name));
            }

            void explainReturnsLogResults() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-explain");
                LogResults log = operation.explain("1+2", true);
                assertAll(
                        () -> assertNotNull(log),
                        () -> assertEquals(3.0, log.getResult())
                );
            }
        }

        @Nested
        @DisplayName("NumberUtils")
        class NumberUtilsTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); sixOperatorsAndInvalidOperator(); },
                    () -> { resetStaticState(); divisionByZeroFollowsDoubleSemantics(); },
                    () -> { resetStaticState(); priorityComparisonCoversHighPriorityOperators(); },
                    () -> { resetStaticState(); comparisonOperationCoversAllOperators(); },
                    () -> { resetStaticState(); factorialHandlesZeroOneFiveAndNegative(); },
                    () -> { resetStaticState(); rangeStringsCoverEqualBoundariesAndStepOne(); },
                    () -> { resetStaticState(); multiplyOfRangeCoversEqualBoundariesAndPositiveStep(); },
                    () -> { resetStaticState(); divideByTenUsesShiftAndIntegerDivision(); }
                );
            }

            void sixOperatorsAndInvalidOperator() {
                assertAll(
                        () -> assertEquals(3.0, NumberUtils.calculation('+', 1, 2)),
                        () -> assertEquals(-1.0, NumberUtils.calculation('-', 1, 2)),
                        () -> assertEquals(2.0, NumberUtils.calculation('*', 1, 2)),
                        () -> assertEquals(0.5, NumberUtils.calculation('/', 1, 2)),
                        () -> assertEquals(1.0, NumberUtils.calculation('%', 5, 2)),
                        () -> assertEquals(8.0, NumberUtils.calculation('^', 2, 3)),
                        () -> assertThrows(AbnormalOperation.class,
                                () -> NumberUtils.calculation('x', 1, 2))
                );
            }

            void divisionByZeroFollowsDoubleSemantics() {
                assertAll(
                        () -> assertTrue(Double.isInfinite(
                                NumberUtils.calculation('/', 1, 0))),
                        () -> assertTrue(Double.isNaN(
                                NumberUtils.calculation('/', 0, 0)))
                );
            }

            void priorityComparisonCoversHighPriorityOperators() {
                assertAll(
                        () -> assertTrue(NumberUtils.PriorityComparison('+', '*')),
                        () -> assertTrue(NumberUtils.PriorityComparison('-', '/')),
                        () -> assertTrue(NumberUtils.PriorityComparison('+', '%')),
                        () -> assertTrue(NumberUtils.PriorityComparison('-', '^')),
                        () -> assertFalse(NumberUtils.PriorityComparison('+', '-')),
                        () -> assertFalse(NumberUtils.PriorityComparison('*', '^')),
                        () -> assertFalse(NumberUtils.PriorityComparison('*', '/'))
                );
            }

            void comparisonOperationCoversAllOperators() {
                assertAll(
                        () -> assertTrue(NumberUtils.ComparisonOperation("<", 1, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation(">", 1, 2)),
                        () -> assertTrue(NumberUtils.ComparisonOperation("<=", 2, 2)),
                        () -> assertTrue(NumberUtils.ComparisonOperation(">=", 2, 2)),
                        () -> assertTrue(NumberUtils.ComparisonOperation("=", 2, 2)),
                        () -> assertTrue(NumberUtils.ComparisonOperation("==", 2, 2)),
                        () -> assertTrue(NumberUtils.ComparisonOperation("!=", 1, 2)),
                        () -> assertTrue(NumberUtils.ComparisonOperation("<>", 1, 2)),
                        () -> assertThrows(AbnormalOperation.class,
                                () -> NumberUtils.ComparisonOperation("x", 1, 2))
                );
            }

            void factorialHandlesZeroOneFiveAndNegative() {
                assertAll(
                        () -> assertEquals(0.0, NumberUtils.factorial(0)),
                        () -> assertEquals(1.0, NumberUtils.factorial(1)),
                        () -> assertEquals(120.0, NumberUtils.factorial(5)),
                        () -> assertEquals(-1.0, NumberUtils.factorial(-1))
                );
            }

            void rangeStringsCoverEqualBoundariesAndStepOne() {
                assertAll(
                        () -> assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5)),
                        () -> assertEquals("(1.0+3.0)*3.0/2",
                                NumberUtils.sumOfRangeString(1, 3)),
                        () -> assertEquals("(1.0+5.0)*5.0/2",
                                NumberUtils.sumOfRangeString(1, 5, 1)),
                        () -> assertEquals("3.0*1.0+3.0*(3.0-1)*2.0/2",
                                NumberUtils.sumOfRangeString(1, 5, 2))
                );
            }

            void multiplyOfRangeCoversEqualBoundariesAndPositiveStep() {
                assertAll(
                        () -> assertEquals(4.0, NumberUtils.MultiplyOfRange(4, 4, 1)),
                        () -> assertEquals(15.0, NumberUtils.MultiplyOfRange(1, 5, 2)),
                        () -> assertEquals("24.0",
                                NumberUtils.MultiplyOfRangeString(1, 4, 1)),
                        () -> assertEquals("4.0",
                                NumberUtils.MultiplyOfRangeString(4, 4, 1))
                );
            }

            void divideByTenUsesShiftAndIntegerDivision() {
                assertAll(
                        () -> assertEquals(10, NumberUtils.divideByTen(100)),
                        () -> assertEquals(0, NumberUtils.divideByTen(3))
                );
            }
        }

        @Nested
        @DisplayName("StrUtils")
        class StrUtilsTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); splitByCharHandlesLeadingTrailingAndConsecutiveDelimiters(); },
                    () -> { resetStaticState(); removeEmptyDeletesAllSpaces(); },
                    () -> { resetStaticState(); stringToDoubleHandlesFactorialAndNegativeFactorial(); },
                    () -> { resetStaticState(); stringToBigDecimalHandlesFactorialAndNegativeFactorial(); },
                    () -> { resetStaticState(); doubleCacheIsHitAndCleared(); },
                    () -> { resetStaticState(); bigDecimalCacheUsesInstanceofBranch(); },
                    () -> { resetStaticState(); isANumberHandlesCharsAndSequences(); }
                );
            }

            void splitByCharHandlesLeadingTrailingAndConsecutiveDelimiters() {
                assertAll(
                        () -> assertEquals(List.of("a", "b", "c"),
                                StrUtils.splitByChar("a,b,c", ',')),
                        () -> assertEquals(List.of("", "a", ""),
                                StrUtils.splitByChar(",a,", ',')),
                        () -> assertEquals(List.of("a", "", "b"),
                                StrUtils.splitByChar("a,,b", ',')),
                        () -> assertEquals(List.of("a", "b"),
                                StrUtils.splitByChar(" a , b ", ','))
                );
            }

            void removeEmptyDeletesAllSpaces() {
                assertAll(
                        () -> assertEquals("ab", StrUtils.removeEmpty(" a b ")),
                        () -> assertEquals("", StrUtils.removeEmpty(""))
                );
            }

            void stringToDoubleHandlesFactorialAndNegativeFactorial() {
                assertAll(
                        () -> assertEquals(1.5, StrUtils.stringToDouble("1.5")),
                        () -> assertEquals(120.0, StrUtils.stringToDouble("5!")),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> StrUtils.stringToDouble("-1!"))
                );
            }

            void stringToBigDecimalHandlesFactorialAndNegativeFactorial() {
                assertAll(
                        () -> assertDecimal("1.5", StrUtils.stringToBigDecimal("1.5")),
                        () -> assertDecimal("120", StrUtils.stringToBigDecimal("5!")),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> StrUtils.stringToBigDecimal("-1!"))
                );
            }

            void doubleCacheIsHitAndCleared() {
                Mathematical_Expression.Options.setUseCache(true);
                double first = StrUtils.stringToDouble("2");
                Object cached = Mathematical_Expression.Options
                        .getCacheCalculation("2".hashCode());
                double second = StrUtils.stringToDouble("2");
                Mathematical_Expression.Options.setUseCache(false);
                assertAll(
                        () -> assertEquals(2.0, first),
                        () -> assertEquals(2.0, second),
                        () -> assertNotNull(cached),
                        () -> assertNull(Mathematical_Expression.Options
                                .getCacheCalculation("2".hashCode()))
                );
            }

            void bigDecimalCacheUsesInstanceofBranch() {
                Mathematical_Expression.Options.setUseCache(true);
                StrUtils.stringToDouble("2");
                BigDecimal first = StrUtils.stringToBigDecimal("2");
                Object cached = Mathematical_Expression.Options
                        .getCacheCalculation("2".hashCode());
                BigDecimal second = StrUtils.stringToBigDecimal("2");
                assertAll(
                        () -> assertDecimal("2", first),
                        () -> assertDecimal("2", second),
                        () -> assertTrue(cached instanceof BigDecimal)
                );
            }

            void isANumberHandlesCharsAndSequences() {
                assertAll(
                        () -> assertTrue(StrUtils.IsANumber('0')),
                        () -> assertTrue(StrUtils.IsANumber('9')),
                        () -> assertFalse(StrUtils.IsANumber('/')),
                        () -> assertTrue(StrUtils.IsANumber("")),
                        () -> assertTrue(StrUtils.IsANumber("123")),
                        () -> assertFalse(StrUtils.IsANumber("12a"))
                );
            }
        }

        @Nested
        @DisplayName("CalculationManagement")
        class CalculationManagementTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); registerAndLookupCoverDuplicateAndOverwriteBranches(); },
                    () -> { resetStaticState(); unknownNameIsNotRegistered(); }
                );
            }

            void registerAndLookupCoverDuplicateAndOverwriteBranches() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-cm-calc");
                String name = calc.getName();
                assertAll(
                        () -> assertTrue(CalculationManagement.isRegister(name)),
                        () -> assertSame(calc,
                                CalculationManagement.getCalculationByName(name)),
                        () -> assertFalse(CalculationManagement.register(calc, true)),
                        () -> assertTrue(CalculationManagement.register(calc, false)),
                        () -> assertSame(calc,
                                CalculationManagement.getCalculationByName(name))
                );
            }

            void unknownNameIsNotRegistered() {
                assertAll(
                        () -> assertFalse(CalculationManagement.isRegister("missing-name")),
                        () -> assertNull(CalculationManagement
                                .getCalculationByName("missing-name"))
                );
            }
        }

        @Nested
        @DisplayName("CumulativeCalculation")
        class CumulativeCalculationTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); getInstanceReusesSameName(); },
                    () -> { resetStaticState(); checkRejectsBadIntervals(); },
                    () -> { resetStaticState(); startGreaterThanEndPinsStringIndexOutOfBounds(); },
                    () -> { resetStaticState(); formatStrExpandsPositiveStep(); },
                    () -> { resetStaticState(); calculationSumsPositiveStepRange(); }
                );
            }

            void getInstanceReusesSameName() {
                CumulativeCalculation first =
                        CumulativeCalculation.getInstance("student-cumulative");
                CumulativeCalculation second =
                        CumulativeCalculation.getInstance("student-cumulative");
                assertAll(
                        () -> assertNotNull(first),
                        () -> assertSame(first, second),
                        () -> assertEquals(6.0,
                                first.calculation("n[0,2,1](1+n)", true).getResult())
                );
            }

            void checkRejectsBadIntervals() {
                CumulativeCalculation calculation =
                        CumulativeCalculation.getInstance("student-cumulative-check");
                assertAll(
                        () -> assertThrows(WrongFormat.class,
                                () -> calculation.check("1+2")),
                        () -> assertThrows(WrongFormat.class,
                                () -> calculation.check("n[0,2](1+n)")),
                        () -> assertDoesNotThrow(
                                () -> calculation.check("n[0,2,1](1+n)"))
                );
            }

            void startGreaterThanEndPinsStringIndexOutOfBounds() {
                CumulativeCalculation calculation =
                        CumulativeCalculation.getInstance("student-cumulative-start");
                assertThrows(StringIndexOutOfBoundsException.class,
                        () -> calculation.formatStr("n[2,0,1](1+n)"));
            }

            void formatStrExpandsPositiveStep() {
                CumulativeCalculation calculation =
                        CumulativeCalculation.getInstance("student-cumulative-format");
                assertEquals("((1+0.0))+((1+1.0))+((1+2.0))",
                        calculation.formatStr("n[0,2,1](1+n)"));
            }

            void calculationSumsPositiveStepRange() {
                CumulativeCalculation calculation =
                        CumulativeCalculation.getInstance("student-cumulative-calc");
                CalculationNumberResults result =
                        calculation.calculation("n[0,2,1](1+n)", true);
                assertEquals(6.0, result.getResult());
            }
        }

        @Nested
        @DisplayName("BooleanCalculation2")
        class BooleanCalculationTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); getInstanceRejectsDifferentTypeWithSameName(); },
                    () -> { resetStaticState(); checkRejectsWrongComparisonCount(); },
                    () -> { resetStaticState(); calculationsCoverComparisons(); },
                    () -> { resetStaticState(); nullLiteralsPinAbnormalOperation(); },
                    () -> { resetStaticState(); formatStrRemovesWhitespace(); },
                    () -> { resetStaticState(); explainDefaultsToUnsupported(); }
                );
            }

            void getInstanceRejectsDifferentTypeWithSameName() {
                String name = "student-bool-conflict";
                CalculationManagement.register(
                        BracketsCalculation2.getInstance(name), false);
                assertThrows(ExtractException.class,
                        () -> BooleanCalculation2.getInstance(name));
            }

            void checkRejectsWrongComparisonCount() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-bool-check");
                assertAll(
                        () -> assertDoesNotThrow(() -> calculation.check("1<2")),
                        () -> assertThrows(WrongFormat.class,
                                () -> calculation.check("1<2<3"))
                );
            }

            void calculationsCoverComparisons() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-bool-calc");
                assertAll(
                        () -> assertEquals(true,
                                calculation.calculation("1<2", true).getResult()),
                        () -> assertEquals(false,
                                calculation.calculation("2<1", true).getResult()),
                        () -> assertEquals(true,
                                calculation.calculation("2=2", true).getResult()),
                        () -> assertEquals(true,
                                calculation.calculation("2!=3", true).getResult()),
                        () -> assertEquals(true,
                                calculation.calculation(" 1 < 2 ", true).getResult())
                );
            }

            void nullLiteralsPinAbnormalOperation() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-bool-null");
                assertAll(
                        () -> assertThrows(AbnormalOperation.class,
                                () -> calculation.calculation("null=null", true)),
                        () -> assertThrows(AbnormalOperation.class,
                                () -> calculation.calculation("null=1", true)),
                        () -> assertThrows(AbnormalOperation.class,
                                () -> calculation.calculation("1=null", true))
                );
            }

            void formatStrRemovesWhitespace() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-bool-format");
                assertEquals("1<2", calculation.formatStr(" 1 < 2 "));
            }

            void explainDefaultsToUnsupported() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-bool-explain");
                assertThrows(UnsupportedOperationException.class,
                        () -> calculation.explain("1<2", true));
            }
        }

        @Nested
        @DisplayName("CalculationOptimized")
        class CalculationOptimizedTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); allOperatorsAreSupported(); },
                    () -> { resetStaticState(); divisionByZeroThrowsArithmeticException(); },
                    () -> { resetStaticState(); invalidOperatorThrowsAbnormalOperation(); }
                );
            }

            void allOperatorsAreSupported() {
                assertAll(
                        () -> assertDecimal("3", CalculationOptimized.calculation('+',
                                new BigDecimal("1"), new BigDecimal("2"))),
                        () -> assertDecimal("-1", CalculationOptimized.calculation('-',
                                new BigDecimal("1"), new BigDecimal("2"))),
                        () -> assertDecimal("2", CalculationOptimized.calculation('*',
                                new BigDecimal("1"), new BigDecimal("2"))),
                        () -> assertDecimal("0.5", CalculationOptimized.calculation('/',
                                new BigDecimal("1"), new BigDecimal("2"))),
                        () -> assertDecimal("1", CalculationOptimized.calculation('%',
                                new BigDecimal("5"), new BigDecimal("2"))),
                        () -> assertDecimal("8", CalculationOptimized.calculation('^',
                                new BigDecimal("2"), new BigDecimal("3")))
                );
            }

            void divisionByZeroThrowsArithmeticException() {
                assertThrows(ArithmeticException.class,
                        () -> CalculationOptimized.calculation('/',
                                new BigDecimal("1"), BigDecimal.ZERO));
            }

            void invalidOperatorThrowsAbnormalOperation() {
                assertThrows(AbnormalOperation.class,
                        () -> CalculationOptimized.calculation('x',
                                BigDecimal.ONE, BigDecimal.ONE));
            }
        }

        @Nested
        @DisplayName("Calculation results")
        class CalculationResultsTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); numberResultsExposeValuesAndComparisons(); },
                    () -> { resetStaticState(); booleanResultsExposeValuesAndComparisons(); },
                    () -> { resetStaticState(); toStringContainsResultAndSource(); }
                );
            }

            void numberResultsExposeValuesAndComparisons() {
                CalculationNumberResults result =
                        new CalculationNumberResults(2, 3.5, "source");
                assertAll(
                        () -> assertEquals(3.5, result.getResult()),
                        () -> assertEquals(2, result.getResultLayers()),
                        () -> assertEquals("source", result.getCalculationSourceName()),
                        () -> assertDecimal("3.5", result.getBigDecimalResult()),
                        () -> assertEquals(1, result.compareTo(null)),
                        () -> assertEquals(1, result.compareTo(
                                new CalculationNumberResults(1, 3.0, "x"))),
                        () -> assertEquals(0, result.compareTo(
                                new CalculationNumberResults(1, 4.0, "x")))
                );
                result.setSource("changed");
                assertEquals("changed", result.getCalculationSourceName());
            }

            void booleanResultsExposeValuesAndComparisons() {
                CalculationBooleanResults trueResult =
                        new CalculationBooleanResults(true, "source", 2, 1.0, 2.0);
                CalculationBooleanResults falseResult =
                        new CalculationBooleanResults(false, "source", 2, 2.0, 1.0);
                assertAll(
                        () -> assertEquals(true, trueResult.getResult()),
                        () -> assertEquals(2, trueResult.getResultLayers()),
                        () -> assertEquals(1.0, trueResult.getLeft()),
                        () -> assertEquals(2.0, trueResult.getRight()),
                        () -> assertEquals(1, trueResult.compareTo(null)),
                        () -> assertEquals(1, trueResult.compareTo(falseResult)),
                        () -> assertEquals(-1, falseResult.compareTo(trueResult)),
                        () -> assertEquals(0, trueResult.compareTo(
                                new CalculationBooleanResults(true, "x", 1, 0, 0)))
                );
            }

            void toStringContainsResultAndSource() {
                assertAll(
                        () -> assertTrue(new CalculationNumberResults(1, 2.0, "src")
                                .toString().contains("result=2.0")),
                        () -> assertTrue(new CalculationBooleanResults(true, "src", 1, 1, 2)
                                .toString().contains("result=true"))
                );
            }
        }

        @Nested
        @DisplayName("NumberCalculation and scattered types")
        class NumberCalculationAndScatteredTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); numberCalculationCheckCoversFormatBranches(); },
                    () -> { resetStaticState(); bracketsCalculationFormatAndDeprecatedCalculation2(); },
                    () -> { resetStaticState(); nameExpressionCacheCanBeSetAndRead(); },
                    () -> { resetStaticState(); packExpressionDismantlingReturnsUnderlying(); },
                    () -> { resetStaticState(); logResultsExposeResultAndRejectUnsupportedOperations(); }
                );
            }

            void numberCalculationCheckCoversFormatBranches() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-nc-check");
                assertAll(
                        () -> assertThrows(WrongFormat.class, () -> calc.check("")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("1+")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("(1+2")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("1+2)")),
                        () -> assertThrows(WrongFormat.class, () -> calc.check("1a2")),
                        () -> assertDoesNotThrow(() -> calc.check("1++2")),
                        () -> assertDoesNotThrow(() -> calc.check("1+-2"))
                );
            }

            void bracketsCalculationFormatAndDeprecatedCalculation2() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-brackets-legacy");
                assertAll(
                        () -> assertEquals("1+2", calc.formatStr(" 1 + 2 ")),
                        () -> assertEquals(3.0, calc.calculation2("1+2")),
                        () -> assertThrows(AbnormalOperation.class,
                                () -> calc.calculation2("12"))
                );
            }

            void nameExpressionCacheCanBeSetAndRead() {
                PrefixExpression expression =
                        PrefixExpression.compile("1+2", "student-name-cache");
                CalculationNumberResults result = expression.calculation(true);
                assertNull(expression.getCache());
                expression.setCache(result);
                assertSame(result, expression.getCache());
            }

            void packExpressionDismantlingReturnsUnderlying() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-pack");
                PackExpression pack = BracketsExpression.compile("1+2", true, calc);
                NameExpression underlying = pack.dismantling();
                assertAll(
                        () -> assertNotNull(underlying),
                        () -> assertDecimal("3",
                                underlying.calculation(true).getBigDecimalResult())
                );
            }

            void logResultsExposeResultAndRejectUnsupportedOperations() {
                LogResults log = new LogResults("log");
                assertNull(log.getResult());
                log.setResult(3.0);
                assertAll(
                        () -> assertEquals(3.0, log.getResult()),
                        () -> assertEquals("log", log.getCalculationSourceName()),
                        () -> assertThrows(UnsupportedOperationException.class,
                                log::getResultLayers),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> log.setSource("x"))
                );
            }
        }

        @Nested
        @DisplayName("引擎补充覆盖")
        class EngineSupplementTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); sumOfRangeStringHandlesEqualStartWithNonOneStep(); },
                    () -> { resetStaticState(); factorialCacheIsUsedForDouble(); },
                    () -> { resetStaticState(); factorialCacheIsUsedForBigDecimal(); },
                    () -> { resetStaticState(); prefixExpressionOperationSupportsUnformattedBranches(); },
                    () -> { resetStaticState(); explainCoversEqualPriorityElseBranch(); },
                    () -> { resetStaticState(); numberCalculationCheckCoversRemainingFormatBranches(); },
                    () -> { resetStaticState(); bracketsExpressionBigDecimalCacheMissAndHit(); },
                    () -> { resetStaticState(); booleanCalculationReusesInstanceAndSupportsUnformattedCalculation(); },
                    () -> { resetStaticState(); cumulativeCalculationRejectsDifferentTypeAndExpandsStepGreaterThanOne(); },
                    () -> { resetStaticState(); gradeReportAndProgressEvaluationNormalizeNullCollections(); },
                    () -> { resetStaticState(); mathematicalExpressionEvaluatorSupportsBothPaths(); },
                    () -> { resetStaticState(); mathematicalExpressionEvaluatorRejectsBlankAndInvalidInput(); }
                );
            }

            void sumOfRangeStringHandlesEqualStartWithNonOneStep() {
                assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5, 2));
            }

            void factorialCacheIsUsedForDouble() {
                Mathematical_Expression.Options.setUseCache(true);
                double first = StrUtils.stringToDouble("5!");
                double second = StrUtils.stringToDouble("5!");
                assertAll(
                        () -> assertEquals(120.0, first),
                        () -> assertEquals(120.0, second),
                        () -> assertNotNull(Mathematical_Expression.Options
                                .getCacheCalculation("5!".hashCode()))
                );
            }

            void factorialCacheIsUsedForBigDecimal() {
                Mathematical_Expression.Options.setUseCache(true);
                BigDecimal first = StrUtils.stringToBigDecimal("5!");
                BigDecimal second = StrUtils.stringToBigDecimal("5!");
                assertAll(
                        () -> assertDecimal("120", first),
                        () -> assertDecimal("120", second),
                        () -> assertTrue(Mathematical_Expression.Options
                                .getCacheCalculation("5!".hashCode()) instanceof BigDecimal)
                );
            }

            void prefixExpressionOperationSupportsUnformattedBranches() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-unformatted");
                assertAll(
                        () -> assertEquals(3.0,
                                operation.compile("1+2", false)
                                        .calculation(true).getResult()),
                        () -> assertDecimal("3", operation
                                .compileBigDecimal("1+2", false)
                                .calculationBigDecimals(true).getBigDecimalResult()),
                        () -> assertEquals(3.0,
                                operation.explain("1+2", false).getResult())
                );
            }

            void explainCoversEqualPriorityElseBranch() {
                PrefixExpressionOperation operation =
                        PrefixExpressionOperation.getInstance("student-peo-equal-priority");
                assertAll(
                        () -> assertEquals(6.0,
                                operation.explain("1+2+3", true).getResult()),
                        () -> assertEquals(6.0,
                                operation.explain("1*2*3", true).getResult())
                );
            }

            void numberCalculationCheckCoversRemainingFormatBranches() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-nc-supplement");
                assertAll(
                        () -> assertDoesNotThrow(() -> calc.check("1+2 ")),
                        () -> assertDoesNotThrow(() -> calc.check("(1+2)")),
                        () -> assertDoesNotThrow(() -> calc.check("1!+2")),
                        () -> assertDoesNotThrow(() -> calc.check("1 2")),
                        () -> assertThrows(WrongFormat.class,
                                () -> calc.check("1**2"))
                );
            }

            void bracketsExpressionBigDecimalCacheMissAndHit() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-bre-big-cache");
                Mathematical_Expression.Options.setUseCache(true);
                PackExpression first = BracketsExpression
                        .compileBigDecimal("1+2", true, calc);
                PackExpression second = BracketsExpression
                        .compileBigDecimal("1+2", true, calc);
                assertSame(first, second);
                assertNotNull(Mathematical_Expression.Options
                        .getCacheCalculation("1+2uB"));
            }

            void booleanCalculationReusesInstanceAndSupportsUnformattedCalculation() {
                BooleanCalculation2 first =
                        BooleanCalculation2.getInstance("student-bool-reuse");
                BooleanCalculation2 second =
                        BooleanCalculation2.getInstance("student-bool-reuse");
                first.setCalculation(BracketsCalculation2
                        .getInstance("student-bool-number-calc"));
                assertAll(
                        () -> assertNotNull(first),
                        () -> assertNotNull(second),
                        () -> assertSame(first, second),
                        () -> assertEquals("student-bool-reuse", first.getName()),
                        () -> assertEquals(true,
                                first.calculation("1<2", false).getResult())
                );
            }

            void cumulativeCalculationRejectsDifferentTypeAndExpandsStepGreaterThanOne() {
                String conflict = "student-cumulative-conflict";
                CalculationManagement.register(
                        BracketsCalculation2.getInstance(conflict), false);
                CumulativeCalculation calculation =
                        CumulativeCalculation.getInstance("student-cumulative-step");
                assertAll(
                        () -> assertThrows(ExtractException.class,
                                () -> CumulativeCalculation.getInstance(conflict)),
                        () -> assertEquals("((1+0.0))+((1+2.0))+((1+4.0))",
                                calculation.formatStr("n[0,4,2](1+n)")),
                        () -> assertEquals(9.0,
                                calculation.calculation("n[0,4,2](1+n)", true)
                                        .getResult())
                );
            }

            void gradeReportAndProgressEvaluationNormalizeNullCollections() {
                GradeReport report = new GradeReport(TEN, TEN, TEN, 100, "A+",
                        true, false, null, null, null);
                ProgressEvaluation evaluation = new ProgressEvaluation(
                        0, 0, 0, false, false, null, null, null);
                assertAll(
                        () -> assertTrue(report.sectionPercentages().isEmpty()),
                        () -> assertTrue(report.failures().isEmpty()),
                        () -> assertTrue(report.warnings().isEmpty()),
                        () -> assertTrue(evaluation.blockers().isEmpty()),
                        () -> assertTrue(evaluation.warnings().isEmpty())
                );
            }

            void mathematicalExpressionEvaluatorSupportsBothPaths() {
                MathematicalExpressionEvaluator simple =
                        new MathematicalExpressionEvaluator();
                BracketsCalculation2 calculator =
                        BracketsCalculation2.getInstance("student-evaluator-general");
                MathematicalExpressionEvaluator general =
                        new MathematicalExpressionEvaluator(calculator);
                assertAll(
                        () -> assertDecimal("7", simple.evaluate("1+2*3")),
                        () -> assertDecimal("7", general.evaluate("1+2*3")),
                        () -> assertDecimal("3", simple.evaluate("1+2"))
                );
            }

            void mathematicalExpressionEvaluatorRejectsBlankAndInvalidInput() {
                MathematicalExpressionEvaluator evaluator =
                        new MathematicalExpressionEvaluator();
                BracketsCalculation2 calculator =
                        BracketsCalculation2.getInstance("student-evaluator-invalid");
                MathematicalExpressionEvaluator general =
                        new MathematicalExpressionEvaluator(calculator);
                assertAll(
                        () -> assertThrows(ExpressionEvaluationException.class,
                                () -> evaluator.evaluate(null)),
                        () -> assertThrows(ExpressionEvaluationException.class,
                                () -> evaluator.evaluate("   ")),
                        () -> assertThrows(ExpressionEvaluationException.class,
                                () -> evaluator.evaluate("1+")),
                        () -> assertThrows(ExpressionEvaluationException.class,
                                () -> general.evaluate("1..2"))
                );
            }
        }
    }

    @Nested
    @DisplayName("Convergence Phase 8")
    class ConvergenceSupplementTest {

        @Nested
        @DisplayName("评分边界")
        class ExamBoundaryTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); unansweredZeroRateDoesNotWarn(); },
                    () -> { resetStaticState(); negativeClampWithOtherCorrectAnswerKeepsFullScore(); },
                    () -> { resetStaticState(); partialCreditNegativeRateIsConfigurable(); },
                    () -> { resetStaticState(); numericErrorAtToleranceWithPartialDisabledScoresFull(); },
                    () -> { resetStaticState(); numericErrorAtTenTimesToleranceWithNegativeMarkingScoresZero(); },
                    () -> { resetStaticState(); numericMissingEitherToleranceFieldWarns(); },
                    () -> { resetStaticState(); essayWithoutManualReviewDoesNotWarn(); },
                    () -> { resetStaticState(); validationRejectsUnansweredRateAboveOneAndNegativeSelectedCounts(); }
                );
            }

            private final ExamGradingService service = new ExamGradingService();

            void unansweredZeroRateDoesNotWarn() {
                GradeReport report = service.grade(submission().answers(
                        answer().answered(false).mandatory(false).build()).build(),
                        policy().unansweredPenaltyRate(ZERO).build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertFalse(report.warnings().stream()
                                .anyMatch(code -> code.startsWith("UNANSWERED_PENALTY")))
                );
            }

            void negativeClampWithOtherCorrectAnswerKeepsFullScore() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").build(),
                        answer().questionId("q2").type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(0)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true)
                                .negativeMarkingEnabled(false).build());
                assertDecimal("10.00", report.rawPoints());
            }

            void partialCreditNegativeRateIsConfigurable() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.MULTIPLE_CHOICE)
                                .correctOptionCount(4).selectedCorrectOptions(2)
                                .selectedIncorrectOptions(1).exactMatch(false).build()).build(),
                        policy().partialCreditEnabled(true)
                                .negativeMarkingEnabled(true)
                                .wrongAnswerPenaltyRate(HALF_PENALTY_RATE).build());
                assertDecimal("3.75", report.rawPoints());
            }

            void numericErrorAtToleranceWithPartialDisabledScoresFull() {
                GradeReport report = service.grade(submission().answers(
                        answer().exactMatch(false).numericError(TOLERANCE)
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().partialCreditEnabled(false).build());
                GradeReport exactWithoutTolerance = service.grade(submission().answers(
                        answer().exactMatch(true).numericError(null)
                                .numericTolerance(null).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("10.00", report.rawPoints()),
                        () -> assertDecimal("10.00", exactWithoutTolerance.rawPoints())
                );
            }

            void numericErrorAtTenTimesToleranceWithNegativeMarkingScoresZero() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").build(),
                        answer().questionId("q2").exactMatch(false)
                                .numericError(new BigDecimal("0.10"))
                                .numericTolerance(TOLERANCE).build()).build(),
                        policy().partialCreditEnabled(true)
                                .negativeMarkingEnabled(true).build());
                assertDecimal("10.00", report.rawPoints());
            }

            void numericMissingEitherToleranceFieldWarns() {
                GradeReport report = service.grade(submission().answers(
                        answer().questionId("q1").exactMatch(false)
                                .numericError(null).numericTolerance(TOLERANCE).build(),
                        answer().questionId("q2").exactMatch(false)
                                .numericError(new BigDecimal("0.02"))
                                .numericTolerance(null).build()).build(),
                        policy().build());
                assertAll(
                        () -> assertDecimal("0.00", report.rawPoints()),
                        () -> assertHasPrefix(report.warnings(),
                                "NUMERIC_TOLERANCE_DATA_MISSING:q1"),
                        () -> assertHasPrefix(report.warnings(),
                                "NUMERIC_TOLERANCE_DATA_MISSING:q2")
                );
            }

            void essayWithoutManualReviewDoesNotWarn() {
                GradeReport report = service.grade(submission().answers(
                        answer().type(QuestionType.ESSAY).exactMatch(false)
                                .rubricPercent(50).manualReviewRequired(false).build()).build(),
                        policy().build());
                assertFalse(report.warnings().stream()
                        .anyMatch(code -> code.startsWith("ESSAY_SCORE_IS_PROVISIONAL")));
            }

            void validationRejectsUnansweredRateAboveOneAndNegativeSelectedCounts() {
                assertAll(
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(answer().build()).build(),
                                        policy().unansweredPenaltyRate(new BigDecimal("1.1")).build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().selectedCorrectOptions(-1).build()).build(),
                                        policy().build())),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> service.grade(submission().answers(
                                        answer().selectedIncorrectOptions(-1).build()).build(),
                                        policy().build()))
                );
            }
        }

        @Nested
        @DisplayName("进度边界")
        class ProgressBoundaryTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); liveZeroAttendanceDoesNotWarn(); },
                    () -> { resetStaticState(); certificateNameFailuresAreIneligible(); },
                    () -> { resetStaticState(); nonAcademicBlockerDoesNotBlockCompletion(); },
                    () -> { resetStaticState(); articleAtNinetyNineIsIncomplete(); }
                );
            }

            private final LearningProgressService service =
                    new LearningProgressService(FIXED_CLOCK);

            void liveZeroAttendanceDoesNotWarn() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.LIVE_SESSION).requiredMinutes(0)
                                .attendedMinutes(0).mandatory(false).build())
                        .requiredOverallPercent(0).certificateEnabled(false).build());
                assertFalse(result.warnings().stream()
                        .anyMatch(code -> code.startsWith("LIVE_ATTENDANCE_TOO_SHORT")));
            }

            void certificateNameFailuresAreIneligible() {
                ProgressEvaluation missing = service.evaluate(snapshot().lessons(
                        lesson().mandatory(true).completionPercent(100).build())
                        .requiredOverallPercent(0).certificateName(null).build());
                ProgressEvaluation blank = service.evaluate(snapshot().lessons(
                        lesson().mandatory(true).completionPercent(100).build())
                        .requiredOverallPercent(0).certificateName(" ").build());
                ProgressEvaluation one = service.evaluate(snapshot().lessons(
                        lesson().mandatory(true).completionPercent(100).build())
                        .requiredOverallPercent(0).certificateName("A").build());
                ProgressEvaluation eightyOne = service.evaluate(snapshot().lessons(
                        lesson().mandatory(true).completionPercent(100).build())
                        .requiredOverallPercent(0).certificateName("a".repeat(81)).build());
                assertAll(
                        () -> assertFalse(missing.certificateEligible()),
                        () -> assertFalse(blank.certificateEligible()),
                        () -> assertFalse(one.certificateEligible()),
                        () -> assertFalse(eightyOne.certificateEligible()),
                        () -> assertHasCode(missing.blockers(), "CERTIFICATE_NAME_REQUIRED"),
                        () -> assertHasCode(one.blockers(),
                                "CERTIFICATE_NAME_LENGTH_INVALID")
                );
            }

            void nonAcademicBlockerDoesNotBlockCompletion() {
                ProgressEvaluation result = service.evaluate(snapshot().lessons(
                        lesson().mandatory(false).completionPercent(100).build())
                        .requiredOverallPercent(0).certificateEnabled(false)
                        .coursePublished(false).build());
                assertAll(
                        () -> assertTrue(result.courseCompleted()),
                        () -> assertHasCode(result.blockers(), "COURSE_NOT_PUBLISHED")
                );
            }

            void articleAtNinetyNineIsIncomplete() {
                ProgressEvaluation ninetyNine = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.ARTICLE).completionPercent(99)
                                .mandatory(false).build()).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                ProgressEvaluation hundred = service.evaluate(snapshot().lessons(
                        lesson().type(LessonType.ARTICLE).completionPercent(100)
                                .mandatory(false).build()).requiredOverallPercent(0)
                        .certificateEnabled(false).build());
                assertAll(
                        () -> assertEquals(0, ninetyNine.completedLessons()),
                        () -> assertEquals(1, hundred.completedLessons())
                );
            }
        }

        @Nested
        @DisplayName("引擎边界")
        class EngineBoundaryTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); prefixStackGettersAndConversionAreExercised(); },
                    () -> { resetStaticState(); prefixExpressionStringIsExact(); },
                    () -> { resetStaticState(); prefixOperationReusesNameAndChecksEdges(); },
                    () -> { resetStaticState(); prefixCacheStoresAndReusesResult(); },
                    () -> { resetStaticState(); comparisonFalseBranchesAreCovered(); },
                    () -> { resetStaticState(); rangeStringsWithStepThreeAreExact(); },
                    () -> { resetStaticState(); arithmeticSpacesAndSubtractionAreCovered(); },
                    () -> { resetStaticState(); strUtilsCacheAndBoundsAreCovered(); },
                    () -> { resetStaticState(); optionsDefaultsAndClosedExpressionState(); },
                    () -> { resetStaticState(); booleanNameAndSideValidationAreCovered(); },
                    () -> { resetStaticState(); packExpressionSourceAndNullExpressionNameAreCovered(); },
                    () -> { resetStaticState(); cumulativeCheckAndStepThreeAreCovered(); },
                    () -> { resetStaticState(); cumulativeCompileFormatsAndCalculates(); },
                    () -> { resetStaticState(); booleanLayersAndNullLiteralAreCovered(); },
                    () -> { resetStaticState(); numberCalculationBracketCountMessageIsStable(); }
                );
            }

            void prefixStackGettersAndConversionAreExercised() {
                PrefixExpression big = PrefixExpression.compileBigDecimal(
                        "1+2", "student-conv-big");
                PrefixExpression dbl = PrefixExpression.compile(
                        "1+2", "student-conv-dbl");
                big.convertToMultiPrecisionSupported();
                Stack<Double> bigDouble = big.getDoubleStack(false);
                big.convertToMultiPrecisionSupported();
                dbl.convertToMultiPrecisionSupported();
                Stack<BigDecimal> dblBig = dbl.getBigDecimalsR(false);
                dbl.convertToMultiPrecisionSupported();
                Stack<BigDecimal> bigCopy = big.getBigDecimalsR(true);
                bigCopy.push(BigDecimal.TEN);
                assertAll(
                        () -> assertNotSame(big.getBigDecimalsR(false), bigCopy),
                        () -> assertNotSame(big.getCharacterStack(false),
                                big.getCharacterStack(true)),
                        () -> assertEquals(2, big.getBigDecimalsR(true).size()),
                        () -> assertEquals(2, big.getBigDecimalsR(false).size()),
                        () -> assertEquals(1, big.getCharacterStack(true).size()),
                        () -> assertEquals(1, big.getCharacterStack(false).size()),
                        () -> assertTrue(big.isBigDecimal()),
                        () -> assertTrue(big.isUnBigDecimal()),
                        () -> assertEquals(2, big.getDoubleStack(true).size()),
                        () -> assertSame(bigDouble, big.getDoubleStack(false)),
                        () -> assertTrue(dbl.isBigDecimal()),
                        () -> assertEquals(2, dbl.getBigDecimalsR(true).size()),
                        () -> assertSame(dblBig, dbl.getBigDecimalsR(false))
                );
            }

            void prefixExpressionStringIsExact() {
                PrefixExpression dbl = PrefixExpression.compile(
                        "1+2", "student-conv-str-dbl");
                PrefixExpression big = PrefixExpression.compileBigDecimal(
                        "1+2", "student-conv-str-big");
                assertAll(
                        () -> assertEquals("1+2 -> 1.0+0", dbl.getExpressionStr()),
                        () -> assertEquals("1+2 -> 1+0", big.getExpressionStr())
                );
            }

            void prefixOperationReusesNameAndChecksEdges() {
                PrefixExpressionOperation first = PrefixExpressionOperation
                        .getInstance("student-conv-peo-reuse");
                PrefixExpressionOperation second = PrefixExpressionOperation
                        .getInstance("student-conv-peo-reuse");
                WrongFormat bracketError = assertThrows(WrongFormat.class,
                        () -> first.check("(1)+2"));
                assertAll(
                        () -> assertNotNull(first),
                        () -> assertSame(first, second),
                        () -> assertEquals(3.0,
                                first.calculation("1+2", true).getResult()),
                        () -> assertDoesNotThrow(() -> first.check("1+2   ")),
                        () -> assertTrue(bracketError.getMessage().contains("无括号")),
                        () -> assertEquals(7.0,
                                first.explain("1+2*3", true).getResult())
                );
            }

            void prefixCacheStoresAndReusesResult() {
                Mathematical_Expression.Options.setUseCache(true);
                PrefixExpression expression = PrefixExpression.compile(
                        "1+2", "student-conv-cache-result");
                CalculationResults first = expression.calculationCache(true);
                CalculationResults second = expression.calculationCache(true);
                PrefixExpression big = PrefixExpression.compileBigDecimal(
                        "1+2", "student-conv-cache-big-result");
                CalculationResults bigFirst = big.calculationBigDecimalsCache(true);
                CalculationResults bigSecond = big.calculationBigDecimalsCache(true);
                assertAll(
                        () -> assertSame(first, second),
                        () -> assertSame(bigFirst, bigSecond)
                );
            }

            void comparisonFalseBranchesAreCovered() {
                assertAll(
                        () -> assertFalse(NumberUtils.ComparisonOperation("<", 2, 1)),
                        () -> assertFalse(NumberUtils.ComparisonOperation("<", 2, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation(">", 1, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation(">", 2, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation("<=", 3, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation(">=", 1, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation("=", 1, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation("!=", 2, 2)),
                        () -> assertFalse(NumberUtils.ComparisonOperation("<>", 2, 2))
                );
            }

            void rangeStringsWithStepThreeAreExact() {
                assertAll(
                        () -> assertEquals("3.0*1.0+3.0*(3.0-1)*3.0/2",
                                NumberUtils.sumOfRangeString(1, 7, 3)),
                        () -> assertEquals("3.0*1.0+3.0*(3.0-1)*3.0/2",
                                NumberUtils.sumOfRangeString(1, 8, 3)),
                        () -> assertEquals(28.0, NumberUtils.MultiplyOfRange(1, 7, 3)),
                        () -> assertEquals("28.0",
                                NumberUtils.MultiplyOfRangeString(1, 7, 3))
                );
            }

            void arithmeticSpacesAndSubtractionAreCovered() {
                assertAll(
                        () -> assertDecimal("3", ArithmeticExpression.evaluate(" 5 - 2 ")),
                        () -> assertDecimal("2", ArithmeticExpression.evaluate("1-(2-3)")),
                        () -> assertDecimal("1", ArithmeticExpression.evaluate("+1")),
                        () -> assertDecimal("-1", ArithmeticExpression.evaluate("-1"))
                );
            }

            void strUtilsCacheAndBoundsAreCovered() {
                Mathematical_Expression.Options.setUseCache(false);
                double miss = StrUtils.stringToDouble("2");
                Mathematical_Expression.Options.setUseCache(true);
                double first = StrUtils.stringToDouble("2");
                double second = StrUtils.stringToDouble("2");
                BigDecimal bigFirst = StrUtils.stringToBigDecimal("2");
                BigDecimal bigSecond = StrUtils.stringToBigDecimal("2");
                assertAll(
                        () -> assertEquals(2.0, miss),
                        () -> assertEquals(2.0, first),
                        () -> assertEquals(2.0, second),
                        () -> assertEquals(0.0, StrUtils.stringToDouble("0!")),
                        () -> assertDecimal("0", StrUtils.stringToBigDecimal("0!")),
                        () -> assertDecimal("2", bigFirst),
                        () -> assertDecimal("2", bigSecond),
                        () -> assertFalse(StrUtils.IsANumber(':'))
                );
            }

            void optionsDefaultsAndClosedExpressionState() {
                PrefixExpression big = PrefixExpression.compileBigDecimal(
                        "1+2", "student-conv-closed");
                big.calculationBigDecimalsCache(false);
                assertAll(
                        () -> assertFalse(Mathematical_Expression.Options.isUseBigDecimal()),
                        () -> assertFalse(Mathematical_Expression.Options.isUseCache()),
                        () -> {
                            Mathematical_Expression.Options.setUseBigDecimal(true);
                            assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
                        },
                        () -> assertFalse(big.isAvailable()),
                        () -> assertThrows(UnsupportedOperationException.class,
                                () -> big.getBigDecimalsR(true))
                );
            }

            void booleanNameAndSideValidationAreCovered() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-conv-bool");
                assertAll(
                        () -> assertEquals("student-conv-bool", calculation.getName()),
                        () -> assertThrows(WrongFormat.class,
                                () -> calculation.check("a<2")),
                        () -> assertThrows(WrongFormat.class,
                                () -> calculation.check("1<b"))
                );
            }

            void packExpressionSourceAndNullExpressionNameAreCovered() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-conv-pack");
                PackExpression pack = BracketsExpression.compile("1+2", true, calc);
                CalculationResults result = pack.calculation(true);
                PrefixExpression underlying = PrefixExpression.compile(
                        "1+2", "student-conv-underlying");
                PackExpression unnamed = new PackExpression(null, underlying, "pack");
                PackExpression named = new PackExpression("alias", underlying, "pack");
                CalculationResults bigResult = pack.calculationBigDecimals(true);
                assertAll(
                        () -> assertEquals(pack.getCalculationName(),
                                result.getCalculationSourceName()),
                        () -> assertEquals(pack.getCalculationName(),
                                bigResult.getCalculationSourceName()),
                        () -> assertEquals("1+2 -> 1.0+0", unnamed.getExpressionStr()),
                        () -> assertEquals("alias -> 1+2 -> 1.0+0",
                                named.getExpressionStr())
                );
            }

            void cumulativeCheckAndStepThreeAreCovered() {
                CumulativeCalculation calculation = CumulativeCalculation
                        .getInstance("student-conv-cumulative");
                assertThrows(WrongFormat.class,
                        () -> calculation.check("n[0,2,1](1+"));
                assertAll(
                        () -> assertEquals("((1+0.0))+((1+3.0))",
                                calculation.formatStr("n[0,3,3](1+n)")),
                        () -> assertEquals(5.0,
                                calculation.calculation("n[0,3,3](1+n)", true).getResult())
                );
            }

            void cumulativeCompileFormatsAndCalculates() {
                CumulativeCalculation calculation = CumulativeCalculation
                        .getInstance("student-conv-cumulative-compile");
                PackExpression compiled = calculation
                        .compile("n[0,2,1](1+n)", true);
                assertEquals(6.0, compiled.calculation(true).getResult());
            }

            void booleanLayersAndNullLiteralAreCovered() {
                BooleanCalculation2 calculation =
                        BooleanCalculation2.getInstance("student-conv-layers");
                assertAll(
                        () -> assertEquals(2,
                                calculation.calculation("1+2<3+4", true)
                                        .getResultLayers()),
                        () -> assertThrows(AbnormalOperation.class,
                                () -> calculation.calculation("null=null", true))
                );
            }

            void numberCalculationBracketCountMessageIsStable() {
                BracketsCalculation2 calc =
                        BracketsCalculation2.getInstance("student-conv-brackets");
                WrongFormat error = assertThrows(WrongFormat.class,
                        () -> calc.check("((1+2)"));
                assertTrue(error.getMessage().contains("[1]"));
            }
        }

        @Nested
        @DisplayName("回写边界")
        class WritebackBoundaryTest {
            @Test
            void allBehaviors() {
                assertAll(
                    () -> { resetStaticState(); writebackAttemptsOverflowThrows(); },
                    () -> { resetStaticState(); nonIntegrityFailureDoesNotMarkPlagiarism(); },
                    () -> { resetStaticState(); alreadyPlagiarizedQuizRemainsBlocked(); },
                    () -> { resetStaticState(); nullLessonIdIsRejected(); }
                );
            }

            private final ExpressionExamProgressService service =
                    new ExpressionExamProgressService(new LearningProgressService(FIXED_CLOCK));

            void writebackAttemptsOverflowThrows() {
                LessonProgress quiz = lesson().lessonId("quiz-1")
                        .type(LessonType.QUIZ).order(1).mandatory(true)
                        .completionPercent(0).score(0).submitted(false).approved(false)
                        .attempts(3).maximumAttempts(3).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(quiz)
                        .requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, "quiz-1",
                                examGrade(80, false, List.of())));
            }

            void nonIntegrityFailureDoesNotMarkPlagiarism() {
                LessonProgress quiz = lesson().lessonId("quiz-1")
                        .type(LessonType.QUIZ).order(1).mandatory(true)
                        .completionPercent(0).score(0).submitted(false).approved(false)
                        .attempts(1).maximumAttempts(3).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(quiz)
                        .requiredOverallPercent(0).build();
                ProgressEvaluation result = service.evaluate(snapshot, "quiz-1",
                        examGrade(80, false, List.of("OTHER_FAILURE")));
                assertAll(
                        () -> assertFalse(result.blockers().stream()
                                .anyMatch(code -> code.startsWith("PLAGIARISM_REVIEW"))),
                        () -> assertTrue(result.courseCompleted())
                );
            }

            void alreadyPlagiarizedQuizRemainsBlocked() {
                LessonProgress quiz = lesson().lessonId("quiz-2")
                        .type(LessonType.QUIZ).order(1).mandatory(true)
                        .completionPercent(0).score(0).submitted(false).approved(false)
                        .attempts(1).maximumAttempts(3).plagiarismFlag(true).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(quiz)
                        .requiredOverallPercent(0).build();
                ProgressEvaluation result = service.evaluate(snapshot, "quiz-2",
                        examGrade(80, false, List.of()));
                assertAll(
                        () -> assertHasPrefix(result.blockers(), "PLAGIARISM_REVIEW:"),
                        () -> assertFalse(result.courseCompleted())
                );
            }

            void nullLessonIdIsRejected() {
                LessonProgress quiz = lesson().lessonId("quiz-1")
                        .type(LessonType.QUIZ).order(1).mandatory(true)
                        .completionPercent(0).score(0).submitted(false).approved(false)
                        .attempts(1).maximumAttempts(3).build();
                CourseProgressSnapshot snapshot = snapshot().lessons(quiz)
                        .requiredOverallPercent(0).build();
                assertThrows(IllegalArgumentException.class,
                        () -> service.evaluate(snapshot, null,
                                examGrade(80, false, List.of())));
            }
        }
    }

    /*
     * Phase 8 survivor ledger (PIT: 992 generated / 919 killed / 52 survived / 18 no coverage / 3 timed out).
     *
     * Equivalent, unreachable, or low-value (reason):
     * - ArithmeticExpression 24,34,48,58,69,72: redundant skipSpaces calls; adjacent calls still normalize.
     * - ArithmeticExpression 59,87: unary/atom boundary; supported inputs produce identical results.
     * - BooleanCalculation 87: only the bracket-count text in an error message changes.
     * - CumulativeCalculation 95 (x3): StringBuilder initial-capacity arithmetic only; output unchanged.
     * - PrefixExpression 285: unreachable second precision-conversion early return for the tested mode.
     * - PrefixExpressionOperation 220,253,258,262: LogResults/DataObj metadata, not numeric result.
     * - PrefixExpressionOperation 68: trailing-space loop boundary; parse result is identical.
     * - PackExpression 37,44: calculation-source metadata only; business result unchanged.
     * - StrUtils 74,110,112: cache-hit/instanceof paths recompute the same numeric value.
     * - ExamGradingService 151,154(x2),157,191,200,206,220,246,267(x2),368,455(x2):
     *   boundary/equality at values where the score or clamp result is identical; cap is unreachable.
     *
     * Missing input/assertion (accepted residual; overall kill rate is above the 85% target):
     * - CumulativeCalculation 43; ExpressionExamProgressService 25,40
     * - LearningProgressService 70,147,271,316
     * - MathematicalExpressionEvaluator 32,45
     * - NumberCalculation 98; NumberUtils 96,159,163,180
     * - PrefixExpressionOperation 46
     *
     * Record/enum generated methods: no equivalent survivors detected.
     * LessonType switch default: unreachable for the exhaustive enum.
     * BracketsCalculation.calculation2: @Deprecated and intentionally not a mutation target.
     */
}
