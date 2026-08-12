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
    subtitle?: string
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
        { path: 'topics/:id(\\d+)', name: 'student-topic-detail', component: () => import('../views/student/TopicLearningView.vue') },
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
        { path: '', name: 'admin-home', component: () => import('../views/AdminHomeView.vue'), meta: { title: '系统总览', subtitle: '教学组织、账号、题库与安全状态' } },
        { path: 'classes', name: 'admin-classes', component: () => import('../views/admin/ClassesView.vue'), meta: { title: '教学组织', subtitle: '班级结构与在读状态' } },
        { path: 'teachers', name: 'admin-teachers', component: () => import('../views/admin/TeachersView.vue'), meta: { title: '教师与任课', subtitle: '教师账号与班级科目范围' } },
        { path: 'questions', name: 'admin-questions', component: () => import('../views/admin/QuestionsView.vue'), meta: { title: '题库管理', subtitle: '新增、审核、发布与批量导入' } },
        { path: 'questions/import', name: 'admin-question-import', component: () => import('../views/admin/QuestionImportView.vue'), meta: { title: '批量导入题目', subtitle: '预览、校验与确认入库' } },
        { path: 'ai-models', name: 'admin-ai-models', component: () => import('../views/admin/AdminAiModelsView.vue'), meta: { title: 'AI 模型管理', subtitle: 'Provider、模型与连接状态' } },
        { path: 'ai-generation', name: 'admin-ai-generation', component: () => import('../views/admin/AiQuestionGenerationView.vue'), meta: { title: 'AI 候选题', subtitle: '变式生成、重复提示与人工质量审核' } },
        { path: 'password-recovery', name: 'admin-password-recovery', component: () => import('../views/admin/PasswordRecoveryRequestsView.vue'), meta: { title: '密码恢复通知', subtitle: '核验匿名请求并安全恢复默认密码' } },
        { path: 'students', name: 'admin-students', component: () => import('../views/admin/StudentsView.vue'), meta: { title: '学生管理', subtitle: '账号、档案与班级归属' } },
        { path: 'students/import', name: 'admin-student-import', component: () => import('../views/admin/StudentImportView.vue'), meta: { title: '批量导入学生', subtitle: 'Excel 预览、确认与一次性密码' } },
        { path: 'operation-logs', name: 'admin-operation-logs', component: () => import('../views/admin/OperationLogsView.vue'), meta: { title: '安全审计', subtitle: '管理员高风险操作事实' } },
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
