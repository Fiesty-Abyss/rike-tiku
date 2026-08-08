import http from './http'

export interface ProfileAccount {
  username: string
  accountStatus: string
  roles: Array<'STUDENT' | 'TEACHER' | 'ADMIN'>
  firstLogin: boolean
  passwordChangedAt: string | null
  lastLoginAt: string | null
}

export interface StudentProfile {
  studentNumber: string
  name: string
  grade: string
  currentClass: string | null
}

export interface TeachingScopeSummary {
  teachingAssignmentId: number
  className: string
  grade: string
  subjectName: string
}

export interface TeacherProfile {
  teacherNumber: string
  name: string
  title: string | null
  teachingScopes: TeachingScopeSummary[]
}

export interface PersonalProfile {
  introduction: string | null
  avatarDataUrl: string | null
  avatarMime: string | null
  avatarUpdatedAt: string | null
}

export interface ProfileResponse {
  displayName: string
  account: ProfileAccount
  studentProfile: StudentProfile | null
  teacherProfile: TeacherProfile | null
  personal: PersonalProfile
}

export interface AvatarResponse {
  avatarDataUrl: string | null
  avatarMime: string | null
  avatarUpdatedAt: string | null
}

export async function fetchProfile() {
  return (await http.get<ProfileResponse>('/profile')).data
}

export async function updateProfile(introduction: string) {
  return (await http.put<ProfileResponse>('/profile', { introduction })).data
}

export async function uploadProfileAvatar(file: File) {
  const data = new FormData()
  data.append('file', file)
  return (await http.post<AvatarResponse>('/profile/avatar', data)).data
}

export async function deleteProfileAvatar() {
  return (await http.delete<AvatarResponse>('/profile/avatar')).data
}
