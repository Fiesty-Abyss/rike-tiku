import http from '../http'
export interface PasswordRecoveryItem {id:number;userId:number;username:string;name:string;role:string;status:string;requestedAt:string;handledAt?:string;result?:string}
export const fetchPasswordRecoveries=()=>http.get<{records:PasswordRecoveryItem[];pendingCount:number}>('/admin/password-recovery-requests').then(r=>r.data)
export const resolvePasswordRecovery=(id:number)=>http.post(`/admin/password-recovery-requests/${id}/resolve`).then(r=>r.data)
export const rejectPasswordRecovery=(id:number,reason:string)=>http.post(`/admin/password-recovery-requests/${id}/reject`,{reason}).then(r=>r.data)
