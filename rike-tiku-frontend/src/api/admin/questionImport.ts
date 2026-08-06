import http from '../http'

export interface QuestionImportError { field:string; code:string; message:string }
export interface QuestionImportRow { rowNumber:number; subjectCode:string|null; questionType:string|null; usageMode:string|null; stemSummary:string; knowledgePointPaths:string[]; attachmentCount:number; contentHash:string; status:'VALID'|'INVALID'; errors:QuestionImportError[]; warnings:string[] }
export interface QuestionImportPreview { fileName:string; fileHash:string; subjectCode:string|null; totalCount:number; validCount:number; invalidCount:number; duplicateCount:number; alreadyImported:boolean; rows:QuestionImportRow[] }
export interface QuestionImportConfirm { batchCode:string; totalCount:number; importedCount:number }

function form(file:File, previewFileHash?:string) {
  const body = new FormData()
  body.append('file', file)
  if (previewFileHash) body.append('previewFileHash', previewFileHash)
  return body
}

export const previewQuestionImport = (file:File) => http.post('/admin/question-import/preview', form(file)).then(response => response.data as QuestionImportPreview)
export const confirmQuestionImport = (file:File, previewFileHash:string) => http.post('/admin/question-import/confirm', form(file, previewFileHash)).then(response => response.data as QuestionImportConfirm)
