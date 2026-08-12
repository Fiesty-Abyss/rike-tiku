import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = (name: string) => readFileSync(resolve(process.cwd(), `src/views/admin/${name}`), 'utf8')

describe('管理员学生与教师默认密码恢复页面', () => {
  it('学生页提供单人、批量、未选择禁用和立即清空', () => {
    const content = source('StudentsView.vue')
    expect(content).toContain('type="selection"', 'selectedStudents.length === 0', '批量恢复默认密码')
    expect(content).toContain('恢复默认密码', 'resetStudentPasswords', '立即清空', '下次登录必须修改密码')
  })

  it('教师页提供单人、批量、本人恢复退出提示和一次性清空', () => {
    const content = source('TeachersView.vue')
    expect(content).toContain('type="selection"', 'selectedTeachers.length === 0', 'resetTeacherPasswords')
    expect(content).toContain('当前登录账号也已恢复默认密码，请退出并使用默认密码重新登录。')
    expect(content).toContain('立即清空', '下次登录必须修改密码')
  })
})
