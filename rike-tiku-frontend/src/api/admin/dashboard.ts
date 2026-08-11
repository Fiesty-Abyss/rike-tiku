import http from '../http'

export interface DashboardOperation { id:number; operatorUsername?:string; module:string; action:string; result:'SUCCESS'|'FAILURE'; summary?:string; createdAt:string }
export interface AdminDashboard {
  activeClassCount:number
  enabledStudentCount:number
  enabledTeacherCount:number
  publishedQuestionCount:number
  pendingQuestionCount:number
  physicsQuestionCount:number
  chemistryQuestionCount:number
  biologyQuestionCount:number
  recentOperationLogs:DashboardOperation[]
}
export const fetchAdminDashboard=()=>http.get<AdminDashboard>('/admin/dashboard').then(response=>response.data)
