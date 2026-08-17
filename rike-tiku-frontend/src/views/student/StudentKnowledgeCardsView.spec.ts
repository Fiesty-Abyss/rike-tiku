// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudentKnowledgeCardsView from './StudentKnowledgeCardsView.vue'

const api=vi.hoisted(()=>({fetchKnowledgeCards:vi.fn(),fetchKnowledgeCardAttachment:vi.fn(),updateKnowledgeCardState:vi.fn()}))
vi.mock('../../api/student/knowledgeCards',()=>api)
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn(),warning:vi.fn()}}))

describe('学生高频考点页',()=>{
  beforeEach(()=>{vi.clearAllMocks();api.fetchKnowledgeCards.mockResolvedValue([{id:1,subjectId:1,subjectCode:'PHYSICS',subjectName:'物理',teachingScopeId:1,className:'',type:'SECONDARY_CONCLUSION',title:'匀变速中间时刻速度',knowledgePoints:[{id:2,name:'运动学',path:'运动学>匀变速直线运动'}],content:'中间时刻瞬时速度等于平均速度。',latex:String.raw`\(\Delta x=aT^2\)`,applicableConditions:'匀变速且时间区间连续。',derivation:'由速度定义推得。',example:'结合 v-t 图像判断。',commonMistake:'不能用于任意变速过程。',mnemonic:'先看模型条件。',sourceName:'用户提供的物化生高频考点提纲与项目整理',rightsStatus:'USER_PROVIDED',status:'PUBLISHED',sortOrder:1,favorite:false,mastery:'LEARNING',attachments:[]}])})
  it('按学科和类型阅读，不显示伪频次或学生练习生成区',async()=>{
    const wrapper=mount(StudentKnowledgeCardsView,{global:{directives:{loading:()=>undefined},stubs:{ElSelect:{template:'<div><slot/></div>'},ElOption:true,ElButton:{template:'<button @click="$emit(\'click\')"><slot/></button>'},ElEmpty:true,ElDrawer:{template:'<aside><slot/></aside>'},StudentAiLearningPanel:true}}})
    await flushPromises()
    expect(wrapper.text()).toContain('物化生高频考点与二级结论')
    expect(wrapper.text()).toContain('二级结论')
    expect(wrapper.text()).not.toContain('最近3年')
    expect(wrapper.text()).not.toContain('生成练习')
    await wrapper.find('article').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('公式、化学式与科学表达')
    expect(wrapper.text()).toContain('适用条件')
  })
})
