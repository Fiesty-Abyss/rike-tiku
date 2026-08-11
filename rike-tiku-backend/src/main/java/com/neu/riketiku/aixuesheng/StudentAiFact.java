package com.neu.riketiku.aixuesheng;

record StudentAiFact(
        long answerFactId,
        long studentId,
        long practiceQuestionId,
        long questionId,
        String subjectCode,
        String questionType,
        String stem,
        String optionsJson,
        String studentAnswerJson,
        String correctAnswerJson,
        String standardAnalysis,
        String knowledgePointsJson,
        boolean correct) { }
