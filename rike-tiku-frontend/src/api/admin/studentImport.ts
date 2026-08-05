import writeExcelFile from 'write-excel-file/browser'

import http from '../http'

export interface StudentImportError {
  field: string
  code: string
  message: string
}

export interface StudentImportRow {
  rowNumber: number
  studentNumber: string
  name: string
  classCode: string
  grade: string
  username: string
  accountStatus: string
  passwordProvided: boolean
  passwordWillGenerate: boolean
  status: 'VALID' | 'INVALID'
  errors: StudentImportError[]
}

export interface StudentImportPreviewResponse {
  fileName: string
  totalCount: number
  validCount: number
  invalidCount: number
  rows: StudentImportRow[]
}

export interface StudentAccountResult {
  studentNumber: string
  name: string
  classCode: string
  username: string
  initialPassword: string
  accountStatus: string
  mustChangePassword: boolean
}

export interface StudentImportConfirmResponse {
  totalCount: number
  importedCount: number
  accounts: StudentAccountResult[]
}

function formData(file: File) {
  const data = new FormData()
  data.append('file', file)
  return data
}

export async function downloadStudentImportTemplate(): Promise<Blob> {
  const response = await http.get('/admin/student-import/template', { responseType: 'blob' })
  return response.data as Blob
}

export async function previewStudentImport(file: File): Promise<StudentImportPreviewResponse> {
  const response = await http.post<StudentImportPreviewResponse>('/admin/student-import/preview', formData(file))
  return response.data
}

export async function confirmStudentImport(file: File): Promise<StudentImportConfirmResponse> {
  const response = await http.post<StudentImportConfirmResponse>('/admin/student-import/confirm', formData(file))
  return response.data
}

export async function downloadAccountWorkbook(accounts: StudentAccountResult[]) {
  const rows = [
    ['学号', '姓名', '班级', '用户名', '初始密码', '账号状态', '首次登录提示'],
    ...accounts.map((account) => [
      account.studentNumber,
      account.name,
      account.classCode,
      account.username,
      account.initialPassword,
      account.accountStatus,
      account.mustChangePassword ? '首次登录后必须修改初始密码' : '无需修改初始密码',
    ]),
  ].map((row) => row.map((value) => ({ value, type: String, format: '@' })))
  await writeExcelFile(rows, {
    sheet: '学生账号发放表',
    columns: [12, 12, 16, 18, 18, 14, 30].map((width) => ({ width })),
  }).toFile(`学生账号发放表_${new Date().toISOString().replace(/[:.]/g, '-')}.xlsx`)
}
