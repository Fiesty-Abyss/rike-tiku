import { describe, expect, it } from 'vitest'
import { resolvePostLoginPath } from './postLoginRoute'

describe('登录后导航', () => {
  it('单角色进入默认工作台', () => {
    expect(resolvePostLoginPath(false, 1, '/student')).toBe('/student')
  })

  it('多角色进入角色选择页', () => {
    expect(resolvePostLoginPath(false, 2, '/teacher')).toBe('/select-role')
  })

  it('历史首次改密标记不再改变登录后的正常路由', () => {
    expect(resolvePostLoginPath(true, 2, '/student')).toBe('/select-role')
  })
})
