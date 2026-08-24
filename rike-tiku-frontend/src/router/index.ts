import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

import type { RoleCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: RoleCode[]
    allowWhenMustChangePassword?: boolean
    guestOnly?: boolean
    title?: string
  }
}

function loginPathFor(_route: RouteLocationNormalized) { return '/login' }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'portal', component: () => import('../views/PortalView.vue') },
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
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true },
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
        { path: 'topics', name: 'student-topics', component: () => import('../views/student/TopicLearningView.vue') },
        { path: 'topics/units/:unitId(\\d+)', name: 'student-topic-unit', component: () => import('../views/student/TopicLearningView.vue') },
        { path: 'topics/:id(\\d+)', name: 'student-topic-detail', component: () => import('../views/student/TopicLearningView.vue') },
        { path: 'papers', name: 'student-papers', component: () => import('../views/student/StudentPapersView.vue') },
        { path: 'papers/:releaseId(\\d+)', name: 'student-paper-detail', component: () => import('../views/student/StudentPapersView.vue') },
        { path: 'knowledge-cards', name: 'student-knowledge-cards', component: () => import('../views/student/StudentKnowledgeCardsView.vue') },
      ],
    },
    {
      path: '/teacher',
      name: 'teacher-home',
      component: () => import('../views/TeacherHomeView.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },
    {
      path: '/teacher/scopes/:scopeId(\\d+)',
      name: 'teacher-scope-workspace',
      component: () => import('../views/teacher/TeacherScopeWorkspaceView.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },
    {
      path: '/teacher/ai-generation',
      name: 'teacher-ai-generation',
      component: () => import('../views/teacher/TeacherAiQuestionGenerationView.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },
    { path: '/teacher/private-questions', name: 'teacher-private-questions', component: () => import('../views/teacher/TeacherPrivateQuestionBankView.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
    { path: '/teacher/scopes/:scopeId(\\d+)/knowledge-cards', name: 'teacher-knowledge-cards', component: () => import('../views/teacher/TeacherKnowledgeCardsView.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
    { path: '/teacher/papers', name: 'teacher-papers', component: () => import('../views/teacher/TeacherPaperBuilderView.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
    { path: '/teacher/papers/:id(\\d+)/:version(student|answer)', name: 'teacher-paper-preview', component: () => import('../views/teacher/PaperPreviewView.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('../views/messages/MessagesView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT', 'TEACHER'] },
    },
    {
      path: '/messages/:id(\\d+)',
      name: 'message-conversation',
      component: () => import('../views/messages/MessageConversationView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT', 'TEACHER'] },
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        { path: '', name: 'admin-home', component: () => import('../views/AdminHomeView.vue'), meta: { title: '系统总览' } },
        { path: 'classes', name: 'admin-classes', component: () => import('../views/admin/ClassesView.vue'), meta: { title: '班级管理' } },
        { path: 'teachers', name: 'admin-teachers', component: () => import('../views/admin/TeachersView.vue'), meta: { title: '教师与任课关系' } },
        { path: 'questions', name: 'admin-questions', component: () => import('../views/admin/QuestionsView.vue'), meta: { title: '题库审核发布' } },
        { path: 'questions/import', name: 'admin-question-import', component: () => import('../views/admin/QuestionImportView.vue'), meta: { title: '题库 Excel 导入' } },
        { path: 'ai-models', name: 'admin-ai-models', component: () => import('../views/admin/AdminAiModelsView.vue'), meta: { title: 'AI 模型管理' } },
        { path: 'ai-generation', name: 'admin-ai-generation', component: () => import('../views/admin/AiQuestionGenerationView.vue'), meta: { title: 'AI 候选题' } },
        { path: 'password-recovery', name: 'admin-password-recovery', component: () => import('../views/admin/PasswordRecoveryRequestsView.vue'), meta: { title: '密码恢复通知' } },
        { path: 'students', name: 'admin-students', component: () => import('../views/admin/StudentsView.vue'), meta: { title: '学生管理' } },
        { path: 'students/import', name: 'admin-student-import', component: () => import('../views/admin/StudentImportView.vue'), meta: { title: '学生 Excel 导入' } },
        { path: 'operation-logs', name: 'admin-operation-logs', component: () => import('../views/admin/OperationLogsView.vue'), meta: { title: '管理员操作日志' } },
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
