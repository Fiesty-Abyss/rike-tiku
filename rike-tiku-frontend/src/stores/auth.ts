import { defineStore } from 'pinia'

import {
  changeInitialPassword as requestInitialPasswordChange,
  changePassword as requestPasswordChange,
  fetchCurrentUser,
  login as requestLogin,
  type ChangeInitialPasswordRequest,
  type CurrentUser,
  type LoginRequest,
  type LoginResponse,
  type RoleCode,
} from '../api/auth'
import { clearStoredSession, readActiveRole, readStoredSession, saveActiveRole, saveSession } from '../auth/session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: null as string | null,
    tokenType: null as string | null,
    expiresIn: null as number | null,
    mustChangePassword: false,
    currentUser: null as CurrentUser | null,
    activeRole: null as RoleCode | null,
    profileAvatar: null as string | null,
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
      const remembered = readActiveRole() as RoleCode | null
      this.activeRole = remembered && currentUser.roles.includes(remembered) ? remembered : (currentUser.roles.length === 1 ? currentUser.roles[0] : null)
      if (this.activeRole) saveActiveRole(this.activeRole)
      return currentUser
    },
    async changeInitialPassword(request: ChangeInitialPasswordRequest) {
      const response = await requestInitialPasswordChange(request)
      this.applyLoginResponse(response)
      await this.loadCurrentUser()
    },
    async changePassword(request: ChangeInitialPasswordRequest) {
      const response = await requestPasswordChange(request)
      this.applyLoginResponse(response)
      await this.loadCurrentUser()
    },
    selectRole(role: RoleCode) {
      if (!this.roles.includes(role)) throw new Error('当前账号不具备该角色')
      this.activeRole = role
      saveActiveRole(role)
    },
    setProfileAvatar(avatar: string | null) {
      this.profileAvatar = avatar
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
      this.activeRole = null
      this.profileAvatar = null
    },
    hasAnyRole(requiredRoles: RoleCode[]) {
      return requiredRoles.some((role) => this.roles.includes(role))
    },
    getDefaultHome() {
      if (this.activeRole && this.roles.includes(this.activeRole)) return ({ STUDENT:'/student', TEACHER:'/teacher', ADMIN:'/admin' } as Record<RoleCode,string>)[this.activeRole]
      if (this.roles.includes('ADMIN')) return '/admin'
      if (this.roles.includes('TEACHER')) return '/teacher'
      return '/student'
    },
  },
})
