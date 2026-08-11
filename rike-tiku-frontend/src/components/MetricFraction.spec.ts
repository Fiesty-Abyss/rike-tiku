import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MetricFraction from './MetricFraction.vue'

describe('MetricFraction', () => {
  it('renders a stacked fraction with an accessible linear label', () => {
    const wrapper = mount(MetricFraction, { props: { numerator: 5, denominator: 10, label: '已练习知识点 5 / 10' } })
    expect(wrapper.find('.metric-fraction__numerator').text()).toBe('5')
    expect(wrapper.find('.metric-fraction__denominator').text()).toBe('10')
    expect(wrapper.attributes('aria-label')).toBe('已练习知识点 5 / 10')
  })
})
