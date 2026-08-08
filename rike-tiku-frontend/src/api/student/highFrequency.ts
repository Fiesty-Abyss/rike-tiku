import http from '../http'

export interface StudentHighFrequencyPoint {
  id:number
  knowledgePointId:number
  knowledgePointName:string
  title:string
  content:string
  memoryTrick:string|null
  commonMistake:string|null
  sortOrder:number
  teacherName:string
}

export const fetchStudentHighFrequencyPoints=(subjectId:number)=>http.get<StudentHighFrequencyPoint[]>('/student/high-frequency-points',{params:{subjectId}}).then(response=>response.data)
