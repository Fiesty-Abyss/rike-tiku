package com.neu.riketiku.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 课程标准通用结构下的原创 Demo360 扩充题；仅由显式本地 Demo seed 使用。 */
final class DemoCurriculumQuestionBank {
    static final int PHYSICS_COUNT = 80;
    static final int CHEMISTRY_COUNT = 81;
    static final int BIOLOGY_COUNT = 79;
    static final int TOTAL_COUNT = PHYSICS_COUNT + CHEMISTRY_COUNT + BIOLOGY_COUNT;

    private DemoCurriculumQuestionBank() {
    }

    static List<DemoDataService.Question> questions() {
        List<DemoDataService.Question> items = new ArrayList<>();
        items.addAll(DemoPhysicsCurriculumBank.questions());
        items.addAll(DemoChemistryCurriculumBank.questions());
        items.addAll(DemoBiologyCurriculumBank.questions());
        verify(items);
        return List.copyOf(items);
    }

    private static void verify(List<DemoDataService.Question> items) {
        if (items.size() != TOTAL_COUNT || new HashSet<>(items.stream().map(DemoDataService.Question::key).toList()).size() != TOTAL_COUNT) {
            throw new IllegalStateException("Demo360扩充题数量或业务编码不唯一");
        }
        Map<String, Integer> subjectCounts = Map.of(
                "PHYSICS", PHYSICS_COUNT, "CHEMISTRY", CHEMISTRY_COUNT, "BIOLOGY", BIOLOGY_COUNT);
        Map<String, Map<String, Long>> typeCounts = Map.of(
                "PHYSICS", Map.of("SINGLE_CHOICE", 30L, "MULTIPLE_CHOICE", 25L, "FILL_BLANK", 25L),
                "CHEMISTRY", Map.of("SINGLE_CHOICE", 31L, "MULTIPLE_CHOICE", 25L, "FILL_BLANK", 25L),
                "BIOLOGY", Map.of("SINGLE_CHOICE", 30L, "MULTIPLE_CHOICE", 24L, "FILL_BLANK", 25L));
        Map<String, Map<Integer, Long>> difficultyCounts = Map.of(
                "PHYSICS", Map.of(1, 25L, 2, 32L, 3, 23L),
                "CHEMISTRY", Map.of(1, 23L, 2, 35L, 3, 23L),
                "BIOLOGY", Map.of(1, 23L, 2, 34L, 3, 22L));
        for (String subject : subjectCounts.keySet()) {
            List<DemoDataService.Question> subjectItems = items.stream().filter(item -> subject.equals(item.subject())).toList();
            if (subjectItems.size() != subjectCounts.get(subject)) throw new IllegalStateException(subject + "扩充题数量错误");
            for (var entry : typeCounts.get(subject).entrySet()) {
                if (subjectItems.stream().filter(item -> entry.getKey().equals(item.type())).count() != entry.getValue()) {
                    throw new IllegalStateException(subject + "题型分布错误: " + entry.getKey());
                }
            }
            for (var entry : difficultyCounts.get(subject).entrySet()) {
                if (subjectItems.stream().filter(item -> item.difficulty() == entry.getKey()).count() != entry.getValue()) {
                    throw new IllegalStateException(subject + "难度分布错误: " + entry.getKey());
                }
            }
        }
    }

    static DemoDataService.Question single(String key, String subject, String stem, String point, int difficulty,
            List<String> options, String answer, String analysis) {
        return DemoDataService.choice(key, subject, "SINGLE_CHOICE", stem, point, difficulty,
                options, Set.of(answer), analysis).withStemPrefix("覆盖：");
    }

    static DemoDataService.Question multi(String key, String subject, String stem, String point, int difficulty,
            List<String> options, Set<String> answers, String analysis) {
        return DemoDataService.choice(key, subject, "MULTIPLE_CHOICE", stem, point, difficulty,
                options, answers, analysis).withStemPrefix("覆盖：");
    }

    static DemoDataService.Question fill(String key, String subject, String stem, String point, int difficulty,
            String answer, String analysis) {
        return DemoDataService.fill(key, subject, stem, point, difficulty, answer, analysis).withStemPrefix("覆盖：");
    }
}
