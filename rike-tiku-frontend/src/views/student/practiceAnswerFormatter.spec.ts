import { describe, expect, it } from 'vitest'
import { formatPracticeAnswer } from './practiceAnswerFormatter'

describe('学生练习答案展示', () => {
  it('以题型格式化单选和多选答案', () => {
    expect(formatPracticeAnswer('SINGLE_CHOICE', 'A')).toBe('A')
    expect(formatPracticeAnswer('MULTIPLE_CHOICE', ['A', 'C'])).toBe('A、C')
  })

  it('以空位和可接受答案格式化填空答案', () => {
    expect(formatPracticeAnswer('FILL_BLANK', ['波长', '频率'])).toBe('第1空：波长；第2空：频率')
    expect(formatPracticeAnswer('FILL_BLANK', {
      blanks: [{ acceptedAnswers: ['波长', 'λ'] }, { acceptedAnswers: ['频率'] }],
    })).toBe('第1空：波长/λ；第2空：频率')
  })
})
