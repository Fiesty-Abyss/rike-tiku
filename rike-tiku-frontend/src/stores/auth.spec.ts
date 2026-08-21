import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import router from '../router'
import { clearStoredSession, getStoredAccessToken, readStoredSession } from '../auth/session'
import { useAuthStore } from './auth'

const authApi = vi.hoisted(() => ({
  login: vi.fn(),
  fetchCurrentUser: vi.fn(),
  changeInitialPassword: vi.fn(),
}))

vi.mock('../api/auth', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/auth')>()),
  login: authApi.login,
  fetchCurrentUser: authApi.fetchCurrentUser,
  changeInitialPassword: authApi.changeInitialPassword,
}))

const student = { id: 1, username: 'student', roles: ['STUDENT'] as const, mustChangePassword: false, displayName: null, studentNumber: null, teacherNumber: null }
const loginResponse = { accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 7200, mustChangePassword: false, user: { id: 1, username: 'student', roles: ['STUDENT'] as const } }

beforeEach(async () => {
  localStorage.clear()
  setActivePinia(createPinia())
  authApi.login.mockReset()
  authApi.fetchCurrentUser.mockReset()
  authApi.changeInitialPassword.mockReset()
  await router.replace('/')
})

describe('认证状态', () => {
  it.each(['STUDENT', 'TEACHER', 'ADMIN'] as const)('登录保留入口角色参数 %s', async (expectedRole) => {
    authApi.login.mockResolvedValue(loginResponse)
    authApi.fetchCurrentUser.mockResolvedValue(student)
    const store = useAuthStore()
    await store.login({ username: 'student', password: 'Password1', expectedRole })
    expect(authApi.login).toHaveBeenCalledWith(expect.objectContaining({ expectedRole }))
  })

  it('登录保存Token和后端真实角色', async () => {
    authApi.login.mockResolvedValue(loginResponse)
    authApi.fetchCurrentUser.mockResolvedValue(student)
    const store = useAuthStore()
    await store.login({ username: 'student', password: 'Password1', expectedRole: 'ADMIN' })
    expect(getStoredAccessToken()).toBe('new-token')
    expect(store.roles).toEqual(['STUDENT'])
  })

  it('恢复会话时调用当前用户接口', async () => {
    localStorage.setItem('rike-tiku.access-token', 'saved-token')
    authApi.fetchCurrentUser.mockResolvedValue(student)
    const store = useAuthStore()
    await store.restoreSession()
    expect(authApi.fetchCurrentUser).toHaveBeenCalledOnce()
    expect(store.isAuthenticated).toBe(true)
  })

  it('当前用户恢复失败会退出', async () => {
    localStorage.setItem('rike-tiku.access-token', 'expired-token')
    authApi.fetchCurrentUser.mockRejectedValue(new Error('expired'))
    const store = useAuthStore()
    await store.restoreSession()
    expect(store.isAuthenticated).toBe(false)
    expect(getStoredAccessToken()).toBeNull()
  })

  it('首次改密成功替换旧Token', async () => {
    const store = useAuthStore()
    store.accessToken = 'old-token'
    localStorage.setItem('rike-tiku.access-token', 'old-token')
    authApi.changeInitialPassword.mockResolvedValue(loginResponse)
    authApi.fetchCurrentUser.mockResolvedValue(student)
    await store.changeInitialPassword({ oldPassword: 'Oldpass1', newPassword: 'Newpass1', confirmPassword: 'Newpass1' })
    expect(getStoredAccessToken()).toBe('new-token')
    expect(store.mustChangePassword).toBe(false)
  })

  it('退出清理本地认证状态', () => {
    const store = useAuthStore()
    store.applyLoginResponse(loginResponse)
    store.currentUser = student
    store.setProfileAvatar('data:image/png;base64,cG5n')
    store.logout()
    expect(readStoredSession()).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.profileAvatar).toBeNull()
  })

  it('角色匹配只使用真实角色', () => {
    const store = useAuthStore()
    store.currentUser = student
    expect(store.hasAnyRole(['ADMIN'])).toBe(false)
    expect(store.hasAnyRole(['STUDENT'])).toBe(true)
  })
})

describe('路由守卫', () => {
  function authenticated(roles: Array<'STUDENT' | 'TEACHER' | 'ADMIN'>, mustChangePassword = false) {
    const store = useAuthStore()
    store.accessToken = 'token'
    store.currentUser = { ...student, roles, mustChangePassword }
    store.mustChangePassword = mustChangePassword
    store.isInitialized = true
  }

  it('未登录访问根路径保持公共门户', async () => { await router.push('/'); expect(router.currentRoute.value.path).toBe('/'); expect(router.currentRoute.value.name).toBe('portal') })
  it('已登录用户也可访问公共门户', async () => { authenticated(['STUDENT']); await router.push('/'); expect(router.currentRoute.value.path).toBe('/') })
  it('未登录访问工作台跳转统一登录页', async () => { await router.push('/teacher'); expect(router.currentRoute.value.path).toBe('/login') }, 15_000)
  it('学生不能访问教师工作台', async () => { authenticated(['STUDENT']); await router.push('/teacher'); expect(router.currentRoute.value.path).toBe('/student') }, 15_000)
  it('学生不能访问管理员工作台', async () => { authenticated(['STUDENT']); await router.push('/admin'); expect(router.currentRoute.value.path).toBe('/student') })
  it('教师不能访问管理员工作台', async () => { authenticated(['TEACHER']); await router.push('/admin'); expect(router.currentRoute.value.path).toBe('/teacher') })
  it('教师可访问 AI 候选题，学生被拒绝且教师不能打开管理员模型页', async () => { authenticated(['TEACHER']); await router.push('/teacher/ai-generation'); expect(router.currentRoute.value.path).toBe('/teacher/ai-generation'); await router.push('/admin/ai-models'); expect(router.currentRoute.value.path).toBe('/teacher'); authenticated(['STUDENT']); await router.push('/teacher/ai-generation'); expect(router.currentRoute.value.path).toBe('/student') })
  it('管理员可访问班级、单学生管理、学生导入和教师管理页面', async () => { authenticated(['ADMIN']); await router.push('/admin/classes'); expect(router.currentRoute.value.path).toBe('/admin/classes'); await router.push('/admin/students'); expect(router.currentRoute.value.path).toBe('/admin/students'); await router.push('/admin/students/import'); expect(router.currentRoute.value.path).toBe('/admin/students/import'); await router.push('/admin/teachers'); expect(router.currentRoute.value.path).toBe('/admin/teachers') })
  it.each(['STUDENT', 'TEACHER', 'ADMIN'] as const)('%s 均可访问统一个人中心', async (role) => { authenticated([role]); await router.push('/profile'); expect(router.currentRoute.value.path).toBe('/profile') })
  it('多角色用户可访问多个工作台', async () => { authenticated(['STUDENT', 'TEACHER']); await router.push('/teacher'); expect(router.currentRoute.value.path).toBe('/teacher'); await router.push('/teacher/scopes/12'); expect(router.currentRoute.value.path).toBe('/teacher/scopes/12'); await router.push('/student'); expect(router.currentRoute.value.path).toBe('/student') })
  it('首次改密标记只允许进入初始密码修改页', async () => { authenticated(['STUDENT'], true); await router.push('/profile'); expect(router.currentRoute.value.path).toBe('/change-initial-password'); await router.push('/change-initial-password'); expect(router.currentRoute.value.path).toBe('/change-initial-password') })
  it('已登录访问登录页跳转真实最高角色工作台', async () => { authenticated(['STUDENT', 'ADMIN']); await router.push('/login/student'); expect(router.currentRoute.value.path).toBe('/admin') })
})

describe('会话存储', () => {
  it('无Token时不会保留会话', () => { clearStoredSession(); expect(readStoredSession()).toBeNull() })
})
