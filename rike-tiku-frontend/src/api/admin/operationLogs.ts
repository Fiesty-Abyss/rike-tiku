import http from '../http'

export interface OperationLogItem { id:number; operatorId?:number; operatorUsername?:string; module:string; action:string; businessObjectId?:number; result:'SUCCESS'|'FAILURE'; summary?:string; errorCode?:string; createdAt:string }
export interface OperationLogPage { records:OperationLogItem[]; total:number; page:number; size:number; pages:number }
export interface OperationLogQuery { page:number; size:number; module?:string; action?:string; result?:'SUCCESS'|'FAILURE' }
export const fetchOperationLogs=(query:OperationLogQuery)=>http.get<OperationLogPage>('/admin/operation-logs',{params:query}).then(response=>response.data)
