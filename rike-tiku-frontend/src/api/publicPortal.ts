import http from './http'

export interface PortalStats {
  subjectCount: number
  automaticPracticeQuestionCount: number
  topicQuestionCount: number
}

export const fetchPortalStats = () => http.get('/public/portal-stats').then(response => response.data as PortalStats)
