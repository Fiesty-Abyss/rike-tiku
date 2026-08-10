package com.neu.riketiku.guanliyibiao;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDashboardDtos {
    private AdminDashboardDtos() {
    }

    public record RecentOperation(
            Long id,
            String operatorUsername,
            String module,
            String action,
            String result,
            String summary,
            LocalDateTime createdAt) {
    }

    public record Dashboard(
            long activeClassCount,
            long enabledStudentCount,
            long enabledTeacherCount,
            long publishedQuestionCount,
            long pendingQuestionCount,
            long physicsQuestionCount,
            long chemistryQuestionCount,
            long biologyQuestionCount,
            List<RecentOperation> recentOperationLogs) {
    }
}
