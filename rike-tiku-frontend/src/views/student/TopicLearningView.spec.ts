// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TopicLearningView from './TopicLearningView.vue'

const { fetchTopics, fetchTopic, push } = vi.hoisted(() => ({ fetchTopics:vi.fn(),fetchTopic:vi.fn(),push:vi.fn() }))
vi.mock('vue-router',()=>({useRoute:()=>({params:{id:'18'},query:{subjectCode:'BIOLOGY'}}),useRouter:()=>({push,replace:vi.fn()})}))
vi.mock('../../api/student/topicLearning',()=>({fetchTopics,fetchTopic}))
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn()}}))

describe('Topic18 专题学习',()=>{
  beforeEach(()=>{
    vi.clearAllMocks()
    const item={id:18,subjectId:3,subjectCode:'BIOLOGY',subjectName:'生物',title:'遗传材料综合分析',difficulty:3,knowledgePoints:[{id:21,name:'遗传规律',path:'遗传与进化>遗传规律'}]}
    fetchTopics.mockResolvedValue([item])
    fetchTopic.mockResolvedValue({...item,material:String.raw`阅读材料，计算 \(\frac{3}{16}\)。`,standardAnalysis:String.raw`解题思路
先读取材料条件。

步骤 1：识别 \(F_1\) 基因型。
步骤 2：列出配子。
步骤 3：依据分离定律得出结论。

结论
后代表型比例为 \(\frac{3}{16}\)。

易错点
不能把基因型比例与表型比例混同。`})
  })

  it('只提供材料阅读与渐进解析，不出现答题、提交或评分',async()=>{
    const wrapper=mount(TopicLearningView,{global:{directives:{loading:()=>undefined},stubs:{
      ElSelect:{template:'<div><slot /></div>'},ElOption:true,ElEmpty:true,
      ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'},Transition:false,
    }}})
    await flushPromises()
    expect(fetchTopics).toHaveBeenCalledWith('BIOLOGY')
    expect(fetchTopic).toHaveBeenCalledWith(18)
    expect(wrapper.text()).toContain('阅读材料')
    expect(wrapper.findAll('.katex').length).toBeGreaterThan(0)
    expect(wrapper.text()).not.toContain('提交答案')
    expect(wrapper.text()).not.toContain('得分')
    expect(wrapper.text()).not.toContain('步骤 1')
    await wrapper.findAll('button').find(button=>button.text()==='查看标准解析')!.trigger('click')
    expect(wrapper.text()).toContain('步骤 1')
    expect(wrapper.findAll('.standard-analysis__block').length).toBeGreaterThanOrEqual(6)
    expect(wrapper.findAll('.katex').length).toBeGreaterThan(1)
    expect(wrapper.attributes('data-subject')).toBe('biology')
  })
})
