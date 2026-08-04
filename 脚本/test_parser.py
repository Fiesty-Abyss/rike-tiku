from __future__ import annotations

import unittest

from parser import parse_questions


class ParserTest(unittest.TestCase):
    def test_question_answer_and_analysis_are_linked_by_number(self) -> None:
        paper = """一、选择题
1. 第一题题干
A. 甲
B. 乙

2. 第二题题干
A. 丙
B. 丁
"""
        answers = """1. A
解析：第一题解析

2. B
解析：第二题解析
"""
        metadata = {
            "sourceName": "测试数据",
            "sourceUrl": "local://test",
            "sourceCategory": "SYNTHETIC_TEST_ONLY",
            "licenseStatus": "USER_OWNED",
            "rightsEvidence": "合成单元测试，不属于真实题库数据",
            "answerSource": {
                "sourceName": "测试答案",
                "sourceUrl": "local://test-answer",
                "sourceCategory": "SYNTHETIC_TEST_ONLY",
                "licenseStatus": "USER_OWNED",
                "rightsEvidence": "合成单元测试",
            },
            "analysisSource": {
                "sourceName": "测试解析",
                "sourceUrl": "local://test-analysis",
                "sourceCategory": "SYNTHETIC_TEST_ONLY",
                "licenseStatus": "USER_OWNED",
                "rightsEvidence": "合成单元测试",
            },
        }
        questions, review = parse_questions(
            paper, answers, metadata, "2026-01-01T00:00:00+08:00"
        )
        self.assertEqual(["1", "2"], [q.questionNumber for q in questions])
        self.assertEqual(["A", "B"], [q.correctAnswer for q in questions])
        self.assertEqual(
            ["第一题解析", "第二题解析"],
            [q.standardAnalysis for q in questions],
        )
        self.assertEqual(["选择题", "选择题"], [q.questionType for q in questions])
        self.assertEqual([], review)

    def test_section_heading_is_not_attached_to_previous_question(self) -> None:
        paper = """一、选择题
1. 选择题题干
A. 甲
B. 乙

二、填空题
2. 填空题题干
"""
        metadata = {
            "sourceName": "测试数据",
            "sourceUrl": "local://test",
            "sourceCategory": "SYNTHETIC_TEST_ONLY",
            "licenseStatus": "USER_OWNED",
            "rightsEvidence": "合成单元测试",
        }
        questions, _ = parse_questions(
            paper, None, metadata, "2026-01-01T00:00:00+08:00"
        )
        self.assertEqual(["选择题", "填空题"], [q.questionType for q in questions])
        self.assertNotIn("填空题", questions[0].options[-1]["content"])

    def test_missing_answer_and_analysis_sources_are_reported(self) -> None:
        metadata = {
            "sourceName": "测试数据",
            "sourceUrl": "local://test",
            "sourceCategory": "SYNTHETIC_TEST_ONLY",
            "licenseStatus": "USER_OWNED",
            "rightsEvidence": "合成单元测试",
        }
        questions, review = parse_questions(
            "一、选择题\n1. 题干\nA. 甲\nB. 乙",
            "1. A\n解析：测试解析",
            metadata,
            "2026-01-01T00:00:00+08:00",
        )
        self.assertEqual("A", questions[0].correctAnswer)
        self.assertIn("MISSING_ANSWER_SOURCE", review[0]["issues"])
        self.assertIn("MISSING_ANALYSIS_SOURCE", review[0]["issues"])


if __name__ == "__main__":
    unittest.main()
