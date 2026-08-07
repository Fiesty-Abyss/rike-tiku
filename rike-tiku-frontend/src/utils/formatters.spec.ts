import { describe, expect, it } from 'vitest'
import { formatEnum, roleHome } from './formatters'

describe('统一中文显示格式化', () => {
  it('将角色和题型显示为中文', () => {
    expect(formatEnum('STUDENT')).toBe('学生')
    expect(formatEnum('MULTIPLE_CHOICE')).toBe('多选题')
    expect(formatEnum('PHYSICS')).toBe('物理')
    expect(formatEnum('ONLINE_PRACTICE')).toBe('在线练习')
    expect(formatEnum('USER_PROVIDED')).toBe('用户提供')
  })

  it('将业务状态显示为中文', () => {
    expect(formatEnum('PUBLISHED')).toBe('已发布')
    expect(formatEnum('MASTERED')).toBe('已掌握')
  })

  it('只映射真实角色工作台', () => {
    expect(roleHome('TEACHER')).toBe('/teacher')
    expect(roleHome('UNKNOWN')).toBe('/login')
  })
})
