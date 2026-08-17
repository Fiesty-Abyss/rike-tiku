// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TeacherHomeView from './TeacherHomeView.vue'

const mocks=vi.hoisted(()=>({push:vi.fn(),replace:vi.fn(),fetchScopes:vi.fn()}))
vi.mock('vue-router',()=>({useRouter:()=>({push:mocks.push,replace:mocks.replace})}))
vi.mock('../api/teacher',()=>({fetchTeachingScopes:mocks.fetchScopes}))
vi.mock('../stores/auth',()=>({useAuthStore:()=>({currentUser:{displayName:'王老师',username:'teacher',teacherNumber:'T001'},roles:['TEACHER'],profileAvatar:null,logout:vi.fn()})}))
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))
const stubs={AquaBrand:true,ChangePasswordDialog:true,ElDropdown:{template:'<div><slot /><slot name="dropdown" /></div>'},ElDropdownMenu:{template:'<div><slot /></div>'},ElDropdownItem:{template:'<button><slot /></button>'},ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'},ElAvatar:{template:'<span><slot /></span>'},ElEmpty:true,ElAlert:true,ElTag:true}

describe('教师工作台 AI 入口',()=>{
  beforeEach(()=>{vi.clearAllMocks();mocks.fetchScopes.mockResolvedValue([])})
  it('提供真实可点击的 AI 变式题生成与审核入口',async()=>{const wrapper=mount(TeacherHomeView,{global:{stubs,directives:{loading:()=>undefined}}});await flushPromises();const button=wrapper.findAll('button').find(item=>item.text().includes('AI 变式题生成与审核'));expect(button).toBeTruthy();await button!.trigger('click');expect(mocks.push).toHaveBeenCalledWith('/teacher/ai-generation')})
  it('提供组卷入口并移除开发边界提示',async()=>{const wrapper=mount(TeacherHomeView,{global:{stubs,directives:{loading:()=>undefined}}});await flushPromises();expect(wrapper.text()).not.toContain('当前工作边界');const button=wrapper.findAll('button').find(item=>item.text().includes('组卷与打印'));expect(button).toBeTruthy();await button!.trigger('click');expect(mocks.push).toHaveBeenCalledWith('/teacher/papers')})
})
