import type { QuestionImportPreview } from '../../api/admin/questionImport'

export const maxQuestionImportFileSize = 10 * 1024 * 1024

export function questionImportFileError(file:Pick<File, 'name'|'size'>) {
  if (!file.name.toLowerCase().endsWith('.xlsx')) return '只支持 .xlsx 文件。'
  if (file.size === 0) return '文件不能为空。'
  if (file.size > maxQuestionImportFileSize) return '文件不能超过 10MB。'
  return ''
}

export function canConfirmQuestionImport(file:File | null, preview:QuestionImportPreview | null, confirming:boolean) {
  return Boolean(file && preview && preview.validCount > 0 && preview.invalidCount === 0 && !preview.alreadyImported && !confirming)
}
