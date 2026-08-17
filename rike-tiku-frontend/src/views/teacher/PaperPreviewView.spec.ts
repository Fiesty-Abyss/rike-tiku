// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PaperPreviewView from './PaperPreviewView.vue'

const api = vi.hoisted(() => ({ fetchPaper: vi.fn() }))
vi.mock('../../api/teacher/papers', () => api)
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '7', version: 'student' } }) }))

describe('试卷学生版', () => {
  it('点击打印按钮调用浏览器原生打印', async () => {
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined)
    api.fetchPaper.mockResolvedValue({ id: 7, name: '物理测试卷', subjectName: '物理', totalScore: 5, questions: [] })
    const wrapper = mount(PaperPreviewView)
    await flushPromises()
    await wrapper.get('button').trigger('click')
    expect(print).toHaveBeenCalledOnce()
    print.mockRestore()
  })

  it('显示中文主观题和足够答题空间，但隐藏答案', async () => {
    api.fetchPaper.mockResolvedValue({ id: 7, name: '物理测试卷', subjectName: '物理', totalScore: 20, questions: [{
      id: 1, order: 1, score: 20, type: 'SUBJECTIVE', stem: '专题计算题〔图片对象 I001〕', options: [], correctAnswer: '{"type":"SUBJECTIVE"}', standardAnalysis: '步骤\n列式', knowledgePoints: ['力学'], difficulty: 2, stemAttachments: [], analysisAttachments: [],
    }] })
    const wrapper = mount(PaperPreviewView)
    await flushPromises()
    expect(wrapper.text()).toContain('打印 / 另存为 PDF')
    expect(wrapper.text()).toContain('主观大题')
    expect(wrapper.find('.answer-space').classes()).toContain('answer-space')
    expect(wrapper.find('.paper-question').classes()).toContain('subjective')
    expect(wrapper.text()).not.toContain('正确答案')
    expect(wrapper.text()).not.toContain('STANDARD 解析')
  })
})
