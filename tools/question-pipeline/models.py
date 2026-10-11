from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any


@dataclass
class Question:
    subject: str | None
    year: int | None
    region: str | None
    paperType: str | None
    questionType: str | None
    content: str
    options: list[dict[str, str]] | None
    correctAnswer: str | None
    standardAnalysis: str | None
    difficultyLevel: str | None
    knowledgePoints: list[str] | None
    sourceName: str
    sourceUrl: str
    sourceCategory: str
    licenseStatus: str
    sourceRightsEvidence: str
    answerSourceName: str | None
    answerSourceUrl: str | None
    answerSourceCategory: str | None
    answerLicenseStatus: str | None
    answerRightsEvidence: str | None
    analysisSourceName: str | None
    analysisSourceUrl: str | None
    analysisSourceCategory: str | None
    analysisLicenseStatus: str | None
    analysisRightsEvidence: str | None
    collectedAt: str
    reviewStatus: str
    questionNumber: str
    contentHash: str

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)
