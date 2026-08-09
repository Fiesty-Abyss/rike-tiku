import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import QuestionContent from './QuestionContent.vue'

const { get } = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('../../api/http', () => ({ default: { get } }))
const attachment={id:7,position:'QUESTION',type:'IMAGE',fileName:'force.png',objectMarker:'〔图片对象 I001〕',status:'ACTIVE',renderStatus:'AVAILABLE',contentUrl:'/api/v1/student/practice-sessions/1/attachments/7/content'}

describe('QuestionContent',()=>{
  afterEach(()=>vi.restoreAllMocks())
  it('renders ordinary content without an attachment',()=>expect(mount(QuestionContent,{props:{content:'普通题干',position:'QUESTION'}}).text()).toContain('普通题干'))
  it('loads an image blob and revokes it on unmount',async()=>{const url=vi.spyOn(URL,'createObjectURL').mockReturnValue('blob:test');const revoke=vi.spyOn(URL,'revokeObjectURL');get.mockResolvedValue({data:new Blob(['png'])});const wrapper=mount(QuestionContent,{props:{content:'题干〔图片对象 I001〕',attachments:[attachment],position:'QUESTION'}});await vi.waitFor(()=>expect(get).toHaveBeenCalled());await wrapper.vm.$nextTick();expect(wrapper.find('img').attributes('src')).toBe('blob:test');wrapper.unmount();expect(revoke).toHaveBeenCalledWith('blob:test');url.mockRestore()})
  it('shows a friendly placeholder after a denied or missing image',async()=>{get.mockRejectedValue(new Error('404'));const wrapper=mount(QuestionContent,{props:{content:'题干〔图片对象 I001〕',attachments:[attachment],position:'QUESTION'}});await vi.waitFor(()=>expect(wrapper.text()).toContain('图片附件暂不可用'));expect(wrapper.find('img').exists()).toBe(false)})
  it('renders analysis attachments in the permitted position',async()=>{get.mockResolvedValue({data:new Blob(['png'])});vi.spyOn(URL,'createObjectURL').mockReturnValue('blob:analysis');const wrapper=mount(QuestionContent,{props:{content:'解析〔图片对象 I001〕',attachments:[{...attachment,position:'STANDARD_ANALYSIS'}],position:'STANDARD_ANALYSIS'}});await vi.waitFor(()=>expect(wrapper.find('img').exists()).toBe(true))})
})
