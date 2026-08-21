package com.neu.riketiku.portal;

/** Public, aggregate-only content counts for the landing page. */
public record PortalStats(long subjectCount, long automaticPracticeQuestionCount, long topicQuestionCount) {
}
