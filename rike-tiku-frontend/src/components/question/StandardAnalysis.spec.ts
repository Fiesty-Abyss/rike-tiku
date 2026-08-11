// @vitest-environment jsdom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StandardAnalysis from './StandardAnalysis.vue'

describe('安全标准解析排版', () => {
  it('按换行生成多个语义段落并识别步骤标题', () => {
    const wrapper = mount(StandardAnalysis, { props: { content: '解题思路\n先确定研究对象。\n\n步骤 1：列出条件。\n步骤 2：代入公式。\n\n结论\n结果满足题意。' } })
    expect(wrapper.findAll('.standard-analysis__block')).toHaveLength(6)
    expect(wrapper.findAll('h4').map(node => node.text())).toEqual(['解题思路', '步骤 1', '步骤 2', '结论'])
  })

  it('公式继续交给 ScientificText 与 KaTeX 渲染', () => {
    const wrapper = mount(StandardAnalysis, { props: { content: String.raw`关键依据：\(E_k=\frac12mv^2\)。` } })
    expect(wrapper.find('.katex').exists()).toBe(true)
    expect(wrapper.text()).toContain('关键依据')
  })

  it('普通旧解析保持为安全纯文本，不执行 HTML', () => {
    const wrapper = mount(StandardAnalysis, { props: { content: '<img src=x onerror=alert(1)> 普通解析。' } })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
  })
})
