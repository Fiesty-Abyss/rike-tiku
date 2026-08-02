import http from './http'

export interface HealthStatus {
  status: string
  database: string
}

export async function fetchHealth(): Promise<HealthStatus> {
  const response = await http.get<HealthStatus>('/health')
  return response.data
}

