import { defineStore } from 'pinia'

import {
  changeInitialPassword as requestInitialPasswordChange,
  fetchCurrentUser,
  login as requestLogin,
  type ChangeInitialPasswordRequest,
  type CurrentUser,
  type LoginRequest,
  type LoginResponse,
  type RoleCode,
} from '../api/auth'
import { clearStoredSession, readStoredSession, saveSession } from '../auth/session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: null as string | null,
    tokenType: null as string | null,
    expiresIn: null as number | null,
    mustChangePassword: false,
    currentUser: null as CurrentUser | null,
    isInitialized: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.currentUser),
    roles: (state): RoleCode[] => state.currentUser?.roles ?? [],
  },
  actions: {
    applyLoginResponse(response: LoginResponse) {
      this.accessToken = response.accessToken
      this.tokenType = response.tokenType
      this.expiresIn = response.expiresIn
      this.mustChangePassword = response.mustChangePassword
      saveSession({
        accessToken: response.accessToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      })
    },
    async login(request: LoginRequest) {
      const response = await requestLogin(request)
      this.applyLoginResponse(response)
      await this.loadCurrentUser()
    },
    async loadCurrentUser() {
      const currentUser = await fetchCurrentUser()
      this.currentUser = currentUser
      this.mustChangePassword = currentUser.mustChangePassword
      return currentUser
    },
    async changeInitialPassword(request: ChangeInitialPasswordRequest) {
      const response = await requestInitialPasswordChange(request)
      this.applyLoginResponse(response)
      await this.loadCurrentUser()
    },
    async restoreSession() {
      if (this.isInitialized) return

      const session = readStoredSession()
      if (session) {
        this.accessToken = session.accessToken
        this.tokenType = session.tokenType
        this.expiresIn = session.expiresIn
        try {
          await this.loadCurrentUser()
        } catch {
          this.logout()
        }
      }
      this.isInitialized = true
    },
    logout() {
      clearStoredSession()
      this.accessToken = null
      this.tokenType = null
      this.expiresIn = null
      this.mustChangePassword = false
      this.currentUser = null
    },
    hasAnyRole(requiredRoles: RoleCode[]) {
      return requiredRoles.some((role) => this.roles.includes(role))
    },
    getDefaultHome() {
      if (this.roles.includes('ADMIN')) return '/admin'
      if (this.roles.includes('TEACHER')) return '/teacher'
      return '/student'
    },
  },
})
