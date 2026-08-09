import { mount, RouterLinkStub } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PortalView from './PortalView.vue'

function mountPortal() {
  return mount(PortalView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

describe('公共门户首页', () => {
  it('展示系统名称、三科学科和学习闭环', () => {
    const text = mountPortal().text()
    expect(text).toContain('集成大模型智能答疑的在线题库实训管理系统')
    expect(text).toContain('物理')
    expect(text).toContain('化学')
    expect(text).toContain('生物')
    expect(text).toContain('练习')
    expect(text).toContain('判分')
    expect(text).toContain('标准解析')
    expect(text).toContain('掌握度 / 推荐')
  })

  it('明确 AI 智能答疑仍为后续能力规划', () => {
    const text = mountPortal().text()
    expect(text).toContain('AI 智能答疑：后续能力规划')
    expect(text).toContain('当前系统尚未实现运行时 AI 答疑')
  })

  it('统一登录 CTA 继续指向现有登录页', () => {
    const wrapper = mountPortal()
    const login = wrapper.findComponent('[data-testid="portal-login"]')
    expect(login.props('to')).toBe('/login')
  })
})
