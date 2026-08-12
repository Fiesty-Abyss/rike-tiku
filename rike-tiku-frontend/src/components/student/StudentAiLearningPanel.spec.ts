// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudentAiLearningPanel from './StudentAiLearningPanel.vue'

const api = vi.hoisted(() => ({ fetchAiAnalysis:vi.fn(), generateAiAnalysis:vi.fn(), createAiConversation:vi.fn(), sendAiMessage:vi.fn() }))
const warning = vi.hoisted(() => vi.fn())
vi.mock('../../api/student/aiLearning', () => ({ ...api, fetchAiConversation:vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage:{ warning } }))
const stubs = {
  ElButton:{ props:['loading','disabled'], template:'<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' },
  ElTag:{ template:'<span><slot /></span>' }, ElSkeleton:true,
  ElInput:{ props:['modelValue','disabled'], emits:['update:modelValue'], template:'<textarea :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />' },
  ElDrawer:{ props:['title'], template:'<aside><h2>{{ title }}</h2><slot /></aside>' },
}

describe('学生 AI 学习面板', () => {
  beforeEach(() => { vi.clearAllMocks(); api.fetchAiAnalysis.mockResolvedValue({ answerFactId:19,status:'NOT_GENERATED',commonMistakes:[],reviewSuggestions:[],cached:false }) })
  it('清楚标注 AI 辅助且不展示 provider、模型或 token', async () => {
    api.fetchAiAnalysis.mockResolvedValue({ answerFactId:19,status:'SUCCESS',errorType:'CONCEPT_ERROR',errorReason:'概念混淆',correctThinking:'先判断受力',commonMistakes:['直接套公式'],reviewSuggestions:['复习受力'],cached:false })
    const wrapper = mount(StudentAiLearningPanel, { props:{ answerFactId:19,wrong:true }, global:{ stubs, directives:{ loading:()=>undefined } } })
    await flushPromises()
    expect(wrapper.text()).toContain('AI 辅助分析')
    expect(wrapper.text()).toContain('不替代标准解析与正式判分')
    expect(wrapper.text()).toContain('概念混淆')
    expect(wrapper.text().toLowerCase()).not.toContain('deepseek', 'token', 'api url')
  })
  it('失败时显示 STANDARD 不受影响的降级提示并允许重试', async () => {
    api.fetchAiAnalysis.mockRejectedValue(new Error('offline'))
    api.generateAiAnalysis.mockRejectedValue({ message:'AI 暂不可用，STANDARD 解析和学习记录不受影响' })
    const wrapper = mount(StudentAiLearningPanel, { props:{ answerFactId:19,wrong:true }, global:{ stubs, directives:{ loading:()=>undefined } } })
    await flushPromises()
    expect(wrapper.text()).toContain('标准解析仍然有效')
    await wrapper.findAll('button').find(button => button.text().includes('重试生成'))!.trigger('click')
    await flushPromises()
    expect(warning).toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('Error:', 'stack')
  })
  it('答对题不生成错因但仍提供当前题有限答疑入口', async () => {
    const wrapper = mount(StudentAiLearningPanel, { props:{ answerFactId:20,wrong:false }, global:{ stubs, directives:{ loading:()=>undefined } } })
    await flushPromises()
    expect(api.fetchAiAnalysis).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('本题已答对')
    expect(wrapper.text()).toContain('当前题目答疑')
  })
  it('创建绑定当前答题事实的会话并展示轮数，发送失败不暴露底层异常', async () => {
    api.createAiConversation.mockResolvedValue({ id:7,answerFactId:19,questionId:2,status:'ACTIVE',usedRounds:0,maxRounds:10,remainingRounds:10,messages:[] })
    api.sendAiMessage.mockRejectedValue({ message:'当前题目答疑暂不可用' })
    const wrapper = mount(StudentAiLearningPanel, { props:{ answerFactId:19,wrong:true }, global:{ stubs, directives:{ loading:()=>undefined } } })
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text().includes('当前题目答疑'))!.trigger('click')
    await flushPromises()
    expect(api.createAiConversation).toHaveBeenCalledWith(19)
    expect(wrapper.text()).toContain('RIKE 理科学习助手', '已绑定当前题目')
    expect(wrapper.text()).toContain('剩余 10 / 10 轮')
    await wrapper.find('textarea').setValue('为什么要先受力分析？')
    await wrapper.findAll('button').find(button => button.text() === '发送')!.trigger('click')
    await flushPromises()
    expect(api.sendAiMessage).toHaveBeenCalledWith(7, '为什么要先受力分析？')
    expect(warning).toHaveBeenCalledWith('当前题目答疑暂不可用')
    expect(wrapper.text()).not.toContain('stack', 'deepseek')
  })
})
