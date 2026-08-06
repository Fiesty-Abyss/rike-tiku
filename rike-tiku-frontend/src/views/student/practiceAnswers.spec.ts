import { describe, expect, it } from 'vitest'
import { answerComplete, answerPayload, initialAnswers } from './practiceAnswers'
import type { PracticeQuestion } from '../../api/student/practice'

const question=(type:PracticeQuestion['questionType'], blankCount=0, id=1):PracticeQuestion => ({ practiceQuestionId:id, order:id, questionType:type, stem:'题干', difficulty:2, score:1, blankCount, options:[], knowledgePoints:[] })

describe('学生练习答题状态', () => {
  it('按题型初始化受控内存答案，不写浏览器存储', () => { const values=initialAnswers([question('SINGLE_CHOICE',0,1),question('MULTIPLE_CHOICE',0,2),question('FILL_BLANK',2,3)]); expect(values).toEqual({ 1:'', 2:[], 3:['',''] }) })
  it('单选与多选使用不同提交结构', () => { expect(answerPayload(question('SINGLE_CHOICE'),' a ')).toBe(' a '); expect(answerPayload(question('MULTIPLE_CHOICE'),['B','A'])).toEqual(['B','A']) })
  it('填空和多选严格检查，未答会阻止提交', () => { expect(answerComplete('FILL_BLANK',['答案',''])).toBe(false); expect(answerComplete('FILL_BLANK',['答案','另一个'])).toBe(true); expect(answerComplete('MULTIPLE_CHOICE',[])).toBe(false); expect(answerComplete('SINGLE_CHOICE','')).toBe(false) })
})
