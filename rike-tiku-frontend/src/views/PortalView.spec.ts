import { mount, RouterLinkStub } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it, vi } from 'vitest'

vi.mock('gsap', () => {
  const timeline = { from: vi.fn(() => timeline) }
  return { default: {
    registerPlugin: vi.fn(),
    matchMedia: () => ({ add: vi.fn(), revert: vi.fn() }),
    context: () => ({ revert: vi.fn() }),
    timeline: () => timeline,
    utils: { toArray: () => [] },
    from: vi.fn(),
    to: vi.fn(() => ({ play: vi.fn(), pause: vi.fn() })),
    set: vi.fn(),
  } }
})
vi.mock('gsap/ScrollTrigger', () => ({ ScrollTrigger: { create: vi.fn() } }))

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
    expect(text).toContain('自动练习题360')
    expect(text).toContain('专题综合题18')
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

  it('使用五段式章节、原创三科主视觉和准确替代文本', () => {
    const wrapper = mountPortal()
    expect(wrapper.findAll('.portal-hero, .portal-subject, .portal-loop')).toHaveLength(5)
    const images = wrapper.findAll('.portal-subject img')
    expect(images).toHaveLength(3)
    expect(images.map(image => image.attributes('alt'))).toEqual([
      '透明光学介质中的钴蓝波动、场线与运动轨迹',
      '日光下的玻璃器皿、梅紫液面、分子结构与光谱折射',
      '叶脉、细胞膜、遗传双螺旋与生态网络融合的生命结构',
    ])
    expect(images[0].attributes('loading')).toBe('eager')
    expect(images[0].attributes('fetchpriority')).toBe('high')
    expect(images.slice(1).every(image => image.attributes('loading') === 'lazy')).toBe(true)
  })

  it('样式包含 390px 响应式与 reduced-motion 静态回退', () => {
    const css = readFileSync(resolve(process.cwd(), 'src/styles/portal.css'), 'utf8')
    expect(css).toContain('@media (max-width: 39.99rem)')
    expect(css).toContain('@media (prefers-reduced-motion: reduce)')
    expect(css).toContain('overflow-x: clip')
  })
})
