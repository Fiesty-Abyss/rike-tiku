import type { PracticeQuestion, QuestionType } from '../../api/student/practice'

export type PracticeAnswerState = Record<number, string | string[]>

export function initialAnswers(questions:PracticeQuestion[]) {
  return questions.reduce<PracticeAnswerState>((result, question) => {
    result[question.practiceQuestionId] = question.questionType === 'MULTIPLE_CHOICE'
      ? []
      : question.questionType === 'FILL_BLANK'
        ? Array.from({ length: question.blankCount }, () => '')
        : ''
    return result
  }, {})
}

export function answerPayload(question:PracticeQuestion, value:string|string[]) {
  if (question.questionType === 'MULTIPLE_CHOICE') return Array.isArray(value) ? value : []
  if (question.questionType === 'FILL_BLANK') return Array.isArray(value) ? value : [String(value ?? '')]
  return Array.isArray(value) ? '' : value
}

export function answerComplete(type:QuestionType, value:string|string[]) {
  return type === 'MULTIPLE_CHOICE' || type === 'FILL_BLANK'
    ? Array.isArray(value) && value.length > 0 && value.every(item => item.trim().length > 0)
    : typeof value === 'string' && value.trim().length > 0
}
