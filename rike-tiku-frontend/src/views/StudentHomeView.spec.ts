// @vitest-environment jsdom
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudentHomeView from './StudentHomeView.vue'

const { route, fetchPracticeOptions } = vi.hoisted(()=>({route:{path:'/student/practice/new'},fetchPracticeOptions:vi.fn()}))
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>({push:vi.fn(),replace:vi.fn()})}))
vi.mock('../api/student/practice',()=>({fetchPracticeOptions}))
vi.mock('../stores/auth',()=>({useAuthStore:()=>({currentUser:{displayName:'学生'},roles:['STUDENT'],profileAvatar:null,logout:vi.fn()})}))
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))

const RouterLink=defineComponent({props:['to','activeClass','exactActiveClass'],template:'<a :data-to="typeof to===\'string\'?to:to.path"><slot /></a>'})

describe('学生主导航精确激活',()=>{
  beforeEach(()=>{vi.clearAllMocks();route.path='/student/practice/new';fetchPracticeOptions.mockResolvedValue({subjects:[],knowledgePoints:[]})})

  it('练习子路由只激活自主练习，不同时激活三科主页',async()=>{
    const wrapper=mount(StudentHomeView,{global:{mocks:{$route:route},stubs:{RouterLink,RouterView:true,ElDropdown:true,ElButton:true,ElAvatar:true,ChangePasswordDialog:true}}})
    await flushPromises()
    const active=wrapper.findAll('a.is-nav-active')
    expect(active).toHaveLength(1)
    expect(active[0].text()).toBe('自主练习')
  })
})
