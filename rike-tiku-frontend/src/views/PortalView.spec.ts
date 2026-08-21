import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it, vi } from 'vitest'

vi.mock('gsap', () => {
  const timeline = {
    from: vi.fn(() => timeline),
    fromTo: vi.fn(() => timeline),
    to: vi.fn(() => timeline),
  }
  return { default: {
    registerPlugin: vi.fn(),
    matchMedia: () => ({ add: vi.fn(), revert: vi.fn() }),
    context: () => ({ revert: vi.fn() }),
    timeline: () => timeline,
    utils: { toArray: () => [] },
    from: vi.fn(),
    fromTo: vi.fn(),
    to: vi.fn(() => ({ play: vi.fn(), pause: vi.fn() })),
    set: vi.fn(),
    quickTo: vi.fn(() => vi.fn()),
  } }
})
vi.mock('gsap/ScrollTrigger', () => ({ ScrollTrigger: { create: vi.fn(), refresh: vi.fn() } }))
const { fetchPortalStats } = vi.hoisted(() => ({ fetchPortalStats: vi.fn() }))
vi.mock('../api/publicPortal', () => ({ fetchPortalStats }))

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
  it('展示事实型系统信息、三科学科和动态题量', async () => {
    fetchPortalStats.mockResolvedValue({ subjectCount: 3, automaticPracticeQuestionCount: 360, topicQuestionCount: 47 })
    const text = mountPortal().text()
    expect(mountPortal().find('#portal-title').attributes('aria-label')).toBe('RIKE 理科学习辅助系统')
    expect(text).toContain('高中物理、化学、生物练习与学习管理')
    expect(text).toContain('物理')
    expect(text).toContain('化学')
    expect(text).toContain('生物')
    expect(text).toContain('力与运动')
    expect(text).toContain('电磁与场')
    expect(text).toContain('波与光学')
    await flushPromises()
    const loaded = mountPortal()
    await flushPromises()
    expect(loaded.text()).toContain('自动练习题360')
    expect(loaded.text()).toContain('专题综合题47')
  })

  it('在统计接口失败时显示占位符，不回退为旧的硬编码题量', async () => {
    fetchPortalStats.mockRejectedValue(new Error('offline'))
    const wrapper = mountPortal()
    await flushPromises()
    expect(wrapper.text()).toContain('自动练习题—')
    expect(wrapper.text()).toContain('专题综合题—')
    expect(wrapper.text()).not.toContain('自动练习题360')
    expect(wrapper.text()).not.toContain('专题综合题18')
  })

  it('移除设计自述、宣传口号和独立 AI 规划章节', () => {
    const text = mountPortal().text()
    expect(text).not.toContain('学习水流')
    expect(text).not.toContain('不是三张换色卡片')
    expect(text).not.toContain('工作节奏')
    expect(text).not.toContain('AI 智能答疑：后续能力规划')
    expect(text).not.toContain('NON-AI FOUNDATION')
    expect(text).not.toContain('本科毕业设计')
  })

  it('统一登录 CTA 继续指向现有登录页', () => {
    const wrapper = mountPortal()
    const login = wrapper.findComponent('[data-testid="portal-login"]')
    expect(login.props('to')).toBe('/login')
  })

  it('使用六段式连续场景、统一世界与原创三科主视觉', () => {
    const wrapper = mountPortal()
    expect(wrapper.findAll('[data-portal-scene]')).toHaveLength(6)
    const heroImage = wrapper.find('.portal-hero-world')
    expect(heroImage.attributes('alt')).toBe('清水与日光中的透明光学仪器连接波动、化学液面和生命叶脉')
    expect(heroImage.attributes('loading')).toBe('eager')
    expect(heroImage.attributes('fetchpriority')).toBe('high')

    const images = wrapper.findAll('.portal-physics-chapter img, .portal-discipline img')
    expect(images).toHaveLength(3)
    expect(images.map(image => image.attributes('alt'))).toEqual([
      '清水实验场中的透明透镜、钴蓝波干涉、场线和精密测量环',
      '水面实验平台上的透明器皿、梅紫液面、分子几何和光谱折射',
      '清水上下相连的巨幅叶脉、膜结构、水滴和根系生命网络',
    ])
    expect(images.every(image => image.attributes('loading') === 'lazy')).toBe(true)
  })

  it('实现 desktop pinned scrub、连续材质变化并在移动端/reduced-motion 降级', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/PortalView.vue'), 'utf8')
    const css = readFileSync(resolve(process.cwd(), 'src/styles/portal.css'), 'utf8')
    expect(source).toContain('pin: physicsPin.value')
    expect(source).toContain('scrub: 0.82')
    expect(source).toContain('(min-width: 64rem) and (prefers-reduced-motion: no-preference)')
    expect(source).toContain('(max-width: 63.99rem) and (prefers-reduced-motion: no-preference)')
    expect(source).toContain("gsap.quickTo(heroOptic.value, 'x'")
    expect(source).toContain('.physics-material-solid')
    expect(source).toContain('context?.revert()')
    expect(source).toContain('motion?.revert()')
    expect(css).toContain('@media (max-width: 24.4rem)')
    expect(css).toContain('@media (prefers-reduced-motion: reduce)')
    expect(css).toContain('overflow-x: clip')
  })
})
