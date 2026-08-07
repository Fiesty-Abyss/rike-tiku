import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

import type { RoleCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: RoleCode[]
    allowWhenMustChangePassword?: boolean
    guestOnly?: boolean
  }
}

function loginPathFor(_route: RouteLocationNormalized) { return '/login' }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { guestOnly: true } },
    {
      path: '/login/:role(student|teacher|admin)',
      redirect: '/login',
    },
    { path: '/select-role', name: 'select-role', component: () => import('../views/auth/RoleSelectionView.vue'), meta: { requiresAuth: true } },
    {
      path: '/change-initial-password',
      name: 'change-initial-password',
      component: () => import('../views/auth/ChangeInitialPasswordView.vue'),
      meta: { requiresAuth: true, allowWhenMustChangePassword: true },
    },
    {
      path: '/student',
      name: 'student-home',
      component: () => import('../views/StudentHomeView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
      children: [
        { path: 'subjects/:subjectCode(physics|chemistry|biology)', name: 'student-subject-dashboard', component: () => import('../views/student/SubjectDashboardView.vue') },
        { path: 'practice', name: 'student-practice', component: () => import('../views/student/PracticeNewView.vue') },
        { path: 'practice/new', name: 'student-practice-new', component: () => import('../views/student/PracticeNewView.vue') },
        { path: 'practice/:id', name: 'student-practice-session', component: () => import('../views/student/PracticeSessionView.vue') },
        { path: 'practice/:id/result', name: 'student-practice-result', component: () => import('../views/student/PracticeResultView.vue') },
        { path: 'wrong-questions', name: 'student-wrong-questions', component: () => import('../views/student/WrongQuestionsView.vue') },
      ],
    },
    {
      path: '/teacher',
      name: 'teacher-home',
      component: () => import('../views/TeacherHomeView.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        { path: '', name: 'admin-home', component: () => import('../views/AdminHomeView.vue') },
        { path: 'classes', name: 'admin-classes', component: () => import('../views/admin/ClassesView.vue') },
        { path: 'teachers', name: 'admin-teachers', component: () => import('../views/admin/TeachersView.vue') },
        { path: 'questions', name: 'admin-questions', component: () => import('../views/admin/QuestionsView.vue') },
        { path: 'questions/import', name: 'admin-question-import', component: () => import('../views/admin/QuestionImportView.vue') },
        { path: 'students', name: 'admin-students', component: () => import('../views/admin/StudentsView.vue') },
        { path: 'students/import', name: 'admin-student-import', component: () => import('../views/admin/StudentImportView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.restoreSession()

  if (authStore.mustChangePassword && !to.meta.allowWhenMustChangePassword) {
    return '/change-initial-password'
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return authStore.getDefaultHome()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return loginPathFor(to)
  }

  if (to.meta.roles && !authStore.hasAnyRole(to.meta.roles)) {
    return authStore.getDefaultHome()
  }

  return true
})

export default router
