import http from './http'

export type RoleCode = 'STUDENT' | 'TEACHER' | 'ADMIN'

export interface LoginRequest {
  username: string
  password: string
  expectedRole: RoleCode
}

export interface LoginUser {
  id: number
  username: string
  roles: RoleCode[]
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  mustChangePassword: boolean
  user: LoginUser
}

export interface CurrentUser extends LoginUser {
  mustChangePassword: boolean
  displayName: string | null
  studentNumber: string | null
  teacherNumber: string | null
}

export interface ChangeInitialPasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await http.post<LoginResponse>('/auth/login', request)
  return response.data
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  const response = await http.get<CurrentUser>('/auth/me')
  return response.data
}

export async function changeInitialPassword(
  request: ChangeInitialPasswordRequest,
): Promise<LoginResponse> {
  const response = await http.post<LoginResponse>('/auth/change-initial-password', request)
  return response.data
}
