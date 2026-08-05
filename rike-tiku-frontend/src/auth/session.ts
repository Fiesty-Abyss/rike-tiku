const ACCESS_TOKEN_KEY = 'rike-tiku.access-token'
const TOKEN_TYPE_KEY = 'rike-tiku.token-type'
const EXPIRES_IN_KEY = 'rike-tiku.expires-in'

export interface StoredSession {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export function getStoredAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function readStoredSession(): StoredSession | null {
  const accessToken = getStoredAccessToken()
  if (!accessToken) return null

  return {
    accessToken,
    tokenType: localStorage.getItem(TOKEN_TYPE_KEY) ?? 'Bearer',
    expiresIn: Number(localStorage.getItem(EXPIRES_IN_KEY) ?? 0),
  }
}

export function saveSession(session: StoredSession) {
  localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  localStorage.setItem(TOKEN_TYPE_KEY, session.tokenType)
  localStorage.setItem(EXPIRES_IN_KEY, String(session.expiresIn))
}

export function clearStoredSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(TOKEN_TYPE_KEY)
  localStorage.removeItem(EXPIRES_IN_KEY)
}
