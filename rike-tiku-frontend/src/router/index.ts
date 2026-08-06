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

function loginPathFor(route: RouteLocationNormalized) {
  const role = route.meta.roles?.[0]
  if (role === 'TEACHER') return '/login/teacher'
  if (role === 'ADMIN') return '/login/admin'
  return '/login/student'
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login/student' },
    {
      path: '/login/:role(student|teacher|admin)',
      name: 'login',
      component: () => import('../views/auth/LoginView.vue'),
      meta: { guestOnly: true },
    },
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
        { path: 'students/import', name: 'admin-student-import', component: () => import('../views/admin/StudentImportView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/login/student' },
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
