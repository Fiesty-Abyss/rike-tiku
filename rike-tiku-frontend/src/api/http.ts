import axios from 'axios'

import { clearStoredSession, getStoredAccessToken } from '../auth/session'

export interface ApiError {
  code?: string
  message?: string
  status?: number
}

type AuthenticationErrorHandler = (error: ApiError) => void

let authenticationErrorHandler: AuthenticationErrorHandler | undefined

export function setAuthenticationErrorHandler(handler: AuthenticationErrorHandler) {
  authenticationErrorHandler = handler
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/api/v1',
  timeout: 5000,
  headers: {
    Accept: 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const accessToken = getStoredAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError: ApiError = {
      status: error.response?.status,
      code: error.response?.data?.code,
      message: error.response?.data?.message ?? error.message ?? '网络请求失败，请稍后重试。',
    }

    if (
      apiError.status === 401 ||
      apiError.code === 'TOKEN_EXPIRED' ||
      apiError.code === 'TOKEN_INVALID' ||
      apiError.code === 'UNAUTHENTICATED'
    ) {
      clearStoredSession()
      authenticationErrorHandler?.(apiError)
    } else if (apiError.status === 403) {
      authenticationErrorHandler?.(apiError)
    }

    return Promise.reject(apiError)
  },
)

export default http
