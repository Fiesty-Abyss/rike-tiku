export const enumText: Record<string, string> = {
  STUDENT:'学生', TEACHER:'教师', ADMIN:'管理员',
  PHYSICS:'物理', CHEMISTRY:'化学', BIOLOGY:'生物',
  SINGLE_CHOICE:'单选题', MULTIPLE_CHOICE:'多选题', FILL_BLANK:'填空题', SUBJECTIVE:'主观题',
  ONLINE_PRACTICE:'在线练习', TOPIC_LEARNING:'专题学习',
  DRAFT:'草稿', PENDING:'待审核', PUBLISHED:'已发布', DISABLED:'已停用',
  NEW:'新错题', REVIEWING:'复习中', MASTERED:'已掌握',
  USER_PROVIDED:'用户提供', TEACHER_CREATED:'教师创建', AUTHORIZED:'已授权', OPEN_LICENSE:'开放许可',
  PUBLIC_OFFICIAL:'公开官方来源', COPYRIGHT_UNKNOWN:'版权待确认', RESTRICTED:'受限使用', ACTIVE:'有效', ENDED:'已结束',
}
export const formatEnum = (value: string | null | undefined) => value ? enumText[value] ?? value : '—'
export const roleHome = (role: string) => ({ STUDENT:'/student', TEACHER:'/teacher', ADMIN:'/admin' }[role] ?? '/login')
