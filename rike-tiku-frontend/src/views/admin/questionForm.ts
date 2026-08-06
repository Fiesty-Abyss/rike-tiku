import type { Option, QuestionType, Save } from '../../api/admin/questions'

export const sourceParts = ['QUESTION', 'ANSWER', 'STANDARD_ANALYSIS'] as const

export function choiceAnswer(questionType: QuestionType, options: Option[]) {
  const optionLabels = options.filter(option => option.correct).map(option => option.label)
  return JSON.stringify({ schemaVersion: 1, type: questionType, optionLabels })
}

export function blankAnswer(blanks: string[][]) {
  return JSON.stringify({ schemaVersion: 1, type: 'FILL_BLANK', blanks: blanks.map(acceptedAnswers => ({ acceptedAnswers: acceptedAnswers.filter(Boolean) })) })
}

export function subjectiveAnswer() {
  return JSON.stringify({ schemaVersion: 1, type: 'SUBJECTIVE' })
}

export function normaliseForSave(form: Save, blanks: string[][]): Save {
  const result = { ...form, stem: form.stem.trim(), standardAnalysis: form.standardAnalysis.trim(), options: form.options.map(option => ({ ...option, label: option.label.trim(), content: option.content.trim() })), sources: form.sources.map(source => ({ ...source, sourceName: source.sourceName.trim(), sourceAddress: source.sourceAddress?.trim() || undefined, rightsBasis: source.rightsBasis?.trim() || undefined })) }
  if (result.questionType === 'SUBJECTIVE') return { ...result, usageMode: 'TOPIC_LEARNING', autoGradable: false, options: [], correctAnswer: subjectiveAnswer() }
  if (result.questionType === 'FILL_BLANK') return { ...result, options: [], correctAnswer: blankAnswer(blanks) }
  return { ...result, correctAnswer: choiceAnswer(result.questionType, result.options) }
}

export function defaultOptions(): Option[] {
  return ['A', 'B', 'C', 'D'].map((label, index) => ({ label, content: '', correct: index === 0 }))
}
