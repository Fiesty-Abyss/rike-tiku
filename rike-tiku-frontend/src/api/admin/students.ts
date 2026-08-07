import http from '../http'

export type AccountStatus = 'ENABLED' | 'DISABLED' | 'LOCKED'
export type StudentProfileStatus = 'ACTIVE' | 'DISABLED'

export interface StudentClass {
  id: number
  classCode: string
  className: string
  grade: string
}

export interface StudentItem {
  id: number
  studentNumber: string
  name: string
  username: string
  grade: string
  currentClass: StudentClass | null
  accountStatus: AccountStatus
  profileStatus: StudentProfileStatus
}

export interface ClassHistoryItem {
  classId: number
  classCode: string
  className: string
  joinedAt: string
  exitedAt: string | null
  current: boolean
}

export interface StudentDetail {
  student: StudentItem
  roles: string[]
  classHistory: ClassHistoryItem[]
}

export interface StudentPageResponse {
  records: StudentItem[]
  total: number
  current: number
  size: number
  pages: number
}

export interface StudentQuery {
  page: number
  size: number
  studentNumber?: string
  name?: string
  username?: string
  classId?: number
  grade?: string
  accountStatus?: AccountStatus
  profileStatus?: StudentProfileStatus
}

export interface CreateStudentRequest {
  studentNumber: string
  name: string
  username: string
  grade: string
  classId: number
}

export interface UpdateStudentRequest {
  name: string
  grade: string
  accountStatus: AccountStatus
  profileStatus: StudentProfileStatus
}

export async function fetchStudents(query: StudentQuery) {
  return (await http.get<StudentPageResponse>('/admin/students', { params: query })).data
}

export async function fetchStudent(id: number) {
  return (await http.get<StudentDetail>(`/admin/students/${id}`)).data
}

export async function createStudent(request: CreateStudentRequest) {
  return (await http.post<{ student: StudentDetail; initialPassword: string }>('/admin/students', request)).data
}

export async function updateStudent(id: number, request: UpdateStudentRequest) {
  return (await http.put<StudentDetail>(`/admin/students/${id}`, request)).data
}

export async function transferStudent(id: number, classId: number) {
  return (await http.post<StudentDetail>(`/admin/students/${id}/transfer`, { classId })).data
}

export async function resetStudentPassword(id: number) {
  return (await http.post<{ initialPassword: string }>(`/admin/students/${id}/reset-password`)).data
}
