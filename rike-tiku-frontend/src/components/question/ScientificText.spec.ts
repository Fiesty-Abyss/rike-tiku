import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ScientificText from './ScientificText.vue'
import { parseScientificText } from './scientificText'

describe('ScientificText', () => {
  it('只解析显式数学片段，普通斜杠保持文本', () => {
    expect(parseScientificText('速度单位 m/s，路径 A/B')).toEqual([{ type: 'text', value: '速度单位 m/s，路径 A/B' }])
  })

  it('renders inline and display math with accessible KaTeX output', () => {
    const wrapper = mount(ScientificText, { props: { content: String.raw`水为 \(\mathrm{H_2O}\)，且 \[v=\frac{s}{t}\]` } })
    expect(wrapper.findAll('.katex')).toHaveLength(2)
    expect(wrapper.findAll('.katex-mathml')).toHaveLength(2)
    expect(wrapper.text()).toContain('水为')
  })

  it('supports fractions, scientific notation, physical subscripts and chemical charges', () => {
    const wrapper = mount(ScientificText, {
      props: {
        content: String.raw`\(\frac{5}{10}\)；\(v_0=2.0\times10^{8}\,\mathrm{m/s}\)；\(\mathrm{Fe^{3+}+SO_4^{2-}}\)`,
      },
    })

    expect(wrapper.findAll('.katex')).toHaveLength(3)
    expect(wrapper.findAll('.katex-mathml')).toHaveLength(3)
    expect(wrapper.html()).toContain('msupsub')
  })

  it('renders mhchem formulas without changing ordinary physics formulas', () => {
    const wrapper = mount(ScientificText, { props: { content: String.raw`\(\ce{H2SO4}\) \(\ce{Fe^{3+}}\) \(\ce{2H2 + O2 -> 2H2O}\) \(v=\frac{s}{t}\)` } })
    expect(wrapper.findAll('.katex')).toHaveLength(4)
    expect(wrapper.findAll('.katex-mathml')).toHaveLength(4)
  })

  it('repairs delimiters that were escaped twice by a legacy JSON writer', () => {
    const wrapper = mount(ScientificText, { props: { content: String.raw`\\(qvB=qE，v=\\frac{E}{B}\\)` } })
    expect(wrapper.find('.katex').exists()).toBe(true)
    expect(wrapper.find('[data-render-status="fallback"]').exists()).toBe(false)
  })

  it('falls back to visible source for an invalid expression', () => {
    const wrapper = mount(ScientificText, { props: { content: String.raw`\(\frac{1}\)` } })
    expect(wrapper.find('[data-render-status="fallback"]').text()).toBe(String.raw`\frac{1}`)
  })
})
