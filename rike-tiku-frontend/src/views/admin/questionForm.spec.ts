import { describe, expect, it } from 'vitest'
import { blankAnswer, choiceAnswer, defaultOptions, normaliseForSave, subjectiveAnswer } from './questionForm'
import type { Save } from '../../api/admin/questions'

const form = (): Save => ({ subjectId: 1, questionType: 'SINGLE_CHOICE', usageMode: 'ONLINE_PRACTICE', stem: '  题干  ', correctAnswer: '{}', difficulty: 2, autoGradable: true, options: defaultOptions(), standardAnalysis: '  解析 ', knowledgePointIds: [1], sources: ['QUESTION', 'ANSWER', 'STANDARD_ANALYSIS'].map(contentType => ({ contentType, sourceType: 'TEACHER_CREATED', sourceName: ' 来源 ', rightsStatus: 'AUTHORIZED' })) })
describe('题目表单受控答案', () => {
  it('默认单选选项不少于两个且标识有序', () => { const options = defaultOptions(); expect(options).toHaveLength(4); expect(options.slice(0, 2)).toMatchObject([{ label: 'A', correct: true }, { label: 'B', correct: false }]) })
  it('单选答案只包含唯一正确选项', () => { expect(JSON.parse(choiceAnswer('SINGLE_CHOICE', defaultOptions())).optionLabels).toEqual(['A']) })
  it('多选答案包含多个被选项', () => { const options = defaultOptions(); options[1].correct = true; expect(JSON.parse(choiceAnswer('MULTIPLE_CHOICE', options)).optionLabels).toEqual(['A', 'B']) })
  it('填空答案支持一个空位的多个可接受答案', () => { expect(JSON.parse(blankAnswer([['速度', 'v']])).blanks[0].acceptedAnswers).toEqual(['速度', 'v']) })
  it('填空答案会移除空字符串', () => { expect(JSON.parse(blankAnswer([['答案', '']])).blanks[0].acceptedAnswers).toEqual(['答案']) })
  it('主观题答案不携带选择题选项', () => { expect(JSON.parse(subjectiveAnswer())).toEqual({ schemaVersion: 1, type: 'SUBJECTIVE' }) })
  it('主观题固定专题学习并关闭自动判分', () => { const data = form(); data.questionType = 'SUBJECTIVE'; const result = normaliseForSave(data, []); expect(result).toMatchObject({ usageMode: 'TOPIC_LEARNING', autoGradable: false, options: [] }) })
  it('填空题保存时不发送选择题选项', () => { const data = form(); data.questionType = 'FILL_BLANK'; const result = normaliseForSave(data, [['答案']]); expect(result.options).toEqual([]) })
  it('保存时清除通用文本首尾空白', () => { const result = normaliseForSave(form(), []); expect(result.stem).toBe('题干'); expect(result.standardAnalysis).toBe('解析') })
})
