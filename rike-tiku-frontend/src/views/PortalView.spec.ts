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
  it('只展示事实型系统信息、三科学科和真实题量', () => {
    const text = mountPortal().text()
    expect(text).toContain('RIKE 理科学习辅助系统')
    expect(text).toContain('高中物理、化学、生物练习与学习管理')
    expect(text).toContain('物理')
    expect(text).toContain('化学')
    expect(text).toContain('生物')
    expect(text).toContain('360道自动练习题')
    expect(text).toContain('18道专题综合题')
  })

  it('移除设计自述、宣传口号和独立 AI 规划章节', () => {
    const text = mountPortal().text()
    expect(text).not.toContain('学习水流')
    expect(text).not.toContain('不是三张换色卡片')
    expect(text).not.toContain('工作节奏')
    expect(text).not.toContain('AI 智能答疑：后续能力规划')
    expect(text).not.toContain('NON-AI FOUNDATION')
  })

  it('统一登录 CTA 继续指向现有登录页', () => {
    const wrapper = mountPortal()
    const login = wrapper.findComponent('[data-testid="portal-login"]')
    expect(login.props('to')).toBe('/login')
  })
})
