import { describe, expect, it } from 'vitest'
import { canConfirmQuestionImport, maxQuestionImportFileSize, questionImportFileError } from './questionImportForm'
import type { QuestionImportPreview } from '../../api/admin/questionImport'

const file = new File(['xlsx'], 'questions.xlsx')
const preview = (patch:Partial<QuestionImportPreview> = {}):QuestionImportPreview => ({ fileName:'questions.xlsx', fileHash:'hash', subjectCode:'PHYSICS', totalCount:1, validCount:1, invalidCount:0, duplicateCount:0, alreadyImported:false, rows:[], ...patch })

describe('题库导入表单状态', () => {
  it('限制扩展名、空文件和文件大小', () => {
    expect(questionImportFileError({ name:'questions.csv', size:1 })).toContain('.xlsx')
    expect(questionImportFileError({ name:'questions.xlsx', size:0 })).toContain('不能为空')
    expect(questionImportFileError({ name:'questions.xlsx', size:maxQuestionImportFileSize + 1 })).toContain('10MB')
    expect(questionImportFileError({ name:'questions.xlsx', size:1 })).toBe('')
  })
  it('无效行、重复文件或确认中均禁止确认', () => {
    expect(canConfirmQuestionImport(file, preview({ invalidCount:1 }), false)).toBe(false)
    expect(canConfirmQuestionImport(file, preview({ alreadyImported:true }), false)).toBe(false)
    expect(canConfirmQuestionImport(file, preview(), true)).toBe(false)
    expect(canConfirmQuestionImport(file, preview(), false)).toBe(true)
  })
})
