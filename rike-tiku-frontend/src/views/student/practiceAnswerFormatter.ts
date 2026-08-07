import type { QuestionType } from '../../api/student/practice'

export function formatPracticeAnswer(questionType: QuestionType, value: unknown) {
  const optionLabels = object(value)?.optionLabels
  if (questionType === 'SINGLE_CHOICE') return text(value) || labels(optionLabels).join('、')
  if (questionType === 'MULTIPLE_CHOICE') return Array.isArray(value) ? value.map(text).filter(Boolean).join('、') : labels(optionLabels).join('、')
  if (Array.isArray(value)) return value.map((item, index) => `第${index + 1}空：${text(item)}`).join('；')
  const blanks = object(value)?.blanks
  if (!Array.isArray(blanks)) return ''
  return blanks.map((blank, index) => {
    const accepted = Array.isArray(object(blank)?.acceptedAnswers) ? object(blank)?.acceptedAnswers : []
    return `第${index + 1}空：${accepted.map(text).filter(Boolean).join('/')}`
  }).join('；')
}

function object(value: unknown): Record<string, unknown> | null {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : null
}

function text(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function labels(value: unknown) {
  return Array.isArray(value) ? value.map(text).filter(Boolean) : []
}
