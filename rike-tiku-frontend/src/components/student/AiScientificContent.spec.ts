import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AiScientificContent from './AiScientificContent.vue'

describe('AiScientificContent', () => {
  it('renders the supported teaching subset with safe KaTeX', () => {
    const wrapper = mount(AiScientificContent, { props: { content: String.raw`**电场力**
\(F=qE\)
\(2\times10^{-6}\)
- 正电荷方向与电场方向相同
1. 先判断方向` } })
    expect(wrapper.find('strong').text()).toBe('电场力')
    expect(wrapper.findAll('.katex')).toHaveLength(2)
    expect(wrapper.find('ul').text()).toContain('正电荷方向')
    expect(wrapper.find('ol').text()).toContain('先判断方向')
  })

  it('keeps HTML inert and does not turn currency into math', () => {
    const wrapper = mount(AiScientificContent, { props: { content: '<img src=x onerror=alert(1)> 价格 $20' } })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>', '$20')
    expect(wrapper.find('.katex').exists()).toBe(false)
  })

  it('normalizes only formula-like dollar delimiters', () => {
    const wrapper = mount(AiScientificContent, { props: { content: '由 $F=qE$ 可知' } })
    expect(wrapper.find('.katex').exists()).toBe(true)
  })
})
