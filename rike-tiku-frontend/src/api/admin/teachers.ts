import http from "../http";

export type AccountStatus = "ENABLED" | "DISABLED" | "LOCKED";
export type TeacherProfileStatus = "ACTIVE" | "DISABLED";
export type TeachingAssignmentStatus = "ACTIVE" | "ENDED" | "DISABLED";

export interface TeacherItem {
  id: number;
  employeeNumber: string;
  name: string;
  displayPosition: string | null;
  username: string;
  accountStatus: AccountStatus;
  profileStatus: TeacherProfileStatus;
  roles: string[];
}
export interface TeacherPageResponse {
  records: TeacherItem[];
  total: number;
  current: number;
  size: number;
  pages: number;
}
export interface TeacherQuery {
  page: number;
  size: number;
  employeeNumber?: string;
  name?: string;
  username?: string;
  accountStatus?: AccountStatus;
  profileStatus?: TeacherProfileStatus;
}
export interface CreateTeacherRequest {
  employeeNumber: string;
  name: string;
  username: string;
  displayPosition?: string;
  initialPassword?: string;
  accountStatus: AccountStatus;
}
export interface UpdateTeacherRequest {
  name: string;
  displayPosition?: string;
  accountStatus: AccountStatus;
  profileStatus: TeacherProfileStatus;
}
export interface SubjectItem {
  id: number;
  subjectCode: string;
  subjectName: string;
}
export interface TeachingAssignment {
  id: number;
  classId: number;
  classCode: string;
  className: string;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  primary: boolean;
  status: TeachingAssignmentStatus;
  startTime: string;
  endTime: string | null;
}
export interface TeacherDetail {
  teacher: TeacherItem;
  roles: string[];
  teachingAssignments: TeachingAssignment[];
}
export interface PasswordRecoveryResponse {
  resetCount: number;
  initialPassword: string;
  mustChangePassword: boolean;
}

export async function fetchTeachers(query: TeacherQuery) {
  return (
    await http.get<TeacherPageResponse>("/admin/teachers", { params: query })
  ).data;
}
export async function fetchTeacher(id: number) {
  return (await http.get<TeacherDetail>(`/admin/teachers/${id}`)).data;
}
export async function createTeacher(request: CreateTeacherRequest) {
  return (
    await http.post<{ teacher: TeacherItem; initialPassword: string }>(
      "/admin/teachers",
      request,
    )
  ).data;
}
export async function updateTeacher(id: number, request: UpdateTeacherRequest) {
  return (await http.put<TeacherItem>(`/admin/teachers/${id}`, request)).data;
}
export async function resetTeacherPassword(id: number) {
  return (
    await http.post<PasswordRecoveryResponse>(
      `/admin/teachers/${id}/reset-password`,
    )
  ).data;
}
export async function resetTeacherPasswords(ids: number[]) {
  return (
    await http.post<PasswordRecoveryResponse>(
      "/admin/teachers/reset-passwords",
      { ids },
    )
  ).data;
}
export async function fetchSubjects() {
  return (await http.get<SubjectItem[]>("/admin/subjects")).data;
}
export async function fetchTeachingAssignments(id: number) {
  return (
    await http.get<TeachingAssignment[]>(
      `/admin/teachers/${id}/teaching-assignments`,
    )
  ).data;
}
export async function createTeachingAssignment(
  id: number,
  request: {
    classId: number;
    subjectId: number;
    primary: boolean;
    startTime: string;
  },
) {
  return (
    await http.post<TeachingAssignment>(
      `/admin/teachers/${id}/teaching-assignments`,
      request,
    )
  ).data;
}
export async function changeTeachingAssignmentStatus(
  id: number,
  status: TeachingAssignmentStatus,
) {
  return (
    await http.patch<TeachingAssignment>(
      `/admin/teaching-assignments/${id}/status`,
      { status },
    )
  ).data;
}
export async function grantTeacherAdmin(id: number) {
  return (await http.post<TeacherDetail>(`/admin/teachers/${id}/admin-role`))
    .data;
}
export async function revokeTeacherAdmin(id: number) {
  return (await http.delete<TeacherDetail>(`/admin/teachers/${id}/admin-role`))
    .data;
}
