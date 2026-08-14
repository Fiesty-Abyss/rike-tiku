package com.neu.riketiku.aixuesheng;

record StudentAiFact(
        Long answerFactId,
        long studentId,
        Long practiceQuestionId,
        Long questionId,
        String subjectCode,
        String questionType,
        String stem,
        String optionsJson,
        String studentAnswerJson,
        String correctAnswerJson,
        String standardAnalysis,
        String knowledgePointsJson,
        boolean correct) { }
