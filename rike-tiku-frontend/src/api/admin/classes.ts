import http from '../http'

export type ClassStatus = 'ACTIVE' | 'GRADUATED' | 'DISABLED'

export interface ClassItem {
  id: number
  classCode: string
  className: string
  grade: string
  enrollmentYear: number
  status: ClassStatus
}

export interface ClassPageResponse {
  records: ClassItem[]
  total: number
  current: number
  size: number
  pages: number
}

export interface ClassQuery {
  page: number
  size: number
  code?: string
  name?: string
  grade?: string
  status?: ClassStatus
}

export interface CreateClassRequest {
  classCode: string
  className: string
  grade: string
  enrollmentYear: number
}

export interface UpdateClassRequest {
  className: string
  grade: string
  enrollmentYear: number
}

export interface ChangeClassStatusRequest {
  status: ClassStatus
}

export async function fetchClasses(query: ClassQuery): Promise<ClassPageResponse> {
  const response = await http.get<ClassPageResponse>('/admin/classes', { params: query })
  return response.data
}

export async function fetchClass(id: number): Promise<ClassItem> {
  const response = await http.get<ClassItem>(`/admin/classes/${id}`)
  return response.data
}

export async function createClass(request: CreateClassRequest): Promise<ClassItem> {
  const response = await http.post<ClassItem>('/admin/classes', request)
  return response.data
}

export async function updateClass(id: number, request: UpdateClassRequest): Promise<ClassItem> {
  const response = await http.put<ClassItem>(`/admin/classes/${id}`, request)
  return response.data
}

export async function changeClassStatus(id: number, request: ChangeClassStatusRequest): Promise<ClassItem> {
  const response = await http.patch<ClassItem>(`/admin/classes/${id}/status`, request)
  return response.data
}
