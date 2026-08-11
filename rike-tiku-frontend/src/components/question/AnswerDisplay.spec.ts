// @vitest-environment jsdom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AnswerDisplay from './AnswerDisplay.vue'

const options = [
  { label: 'A', content: '速度方向发生改变' },
  { label: 'B', content: String.raw`动能为 \(12\,\mathrm{J}\)` },
  { label: 'C', content: '合外力恒为零' },
]

describe('冻结答案展示', () => {
  it('单选显示 label 与冻结选项内容', () => {
    const wrapper = mount(AnswerDisplay, { props: { questionType: 'SINGLE_CHOICE', value: 'B', options } })
    expect(wrapper.text()).toContain('B.')
    expect(wrapper.text()).toContain('动能为')
    expect(wrapper.find('.katex').exists()).toBe(true)
  })

  it('多选逐项显示冻结选项内容', () => {
    const wrapper = mount(AnswerDisplay, { props: { questionType: 'MULTIPLE_CHOICE', value: ['A', 'C'], options } })
    expect(wrapper.text()).toContain('A.')
    expect(wrapper.text()).toContain('速度方向发生改变')
    expect(wrapper.text()).toContain('C.')
    expect(wrapper.text()).toContain('合外力恒为零')
  })

  it('选项快照内容缺失时安全回退到 label', () => {
    const wrapper = mount(AnswerDisplay, { props: { questionType: 'SINGLE_CHOICE', value: 'D', options } })
    expect(wrapper.text()).toBe('D.')
    expect(wrapper.find('[aria-label="选项快照内容缺失"]').exists()).toBe(true)
  })

  it('填空答案保留逐空结构且 canonical answer 只显示首项', () => {
    const wrapper = mount(AnswerDisplay, { props: { questionType: 'FILL_BLANK', value: { blanks: [{ acceptedAnswers: ['1/2', '0.5', '50%'] }] } } })
    expect(wrapper.text()).toContain('第 1 空')
    expect(wrapper.text()).toContain('1/2')
    expect(wrapper.text()).not.toContain('0.5')
  })
})
