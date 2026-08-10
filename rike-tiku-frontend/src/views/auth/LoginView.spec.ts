// @vitest-environment jsdom
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import LoginView from './LoginView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: vi.fn() }),
}))

vi.mock('../../stores/auth', () => ({
  useAuthStore: () => ({
    login: vi.fn(),
    mustChangePassword: false,
    roles: [],
    getDefaultHome: () => '/student',
  }),
}))

const RouterLink = defineComponent({
  props: { to: { type: String, required: true } },
  template: '<a :href="to"><slot /></a>',
})

describe('登录页导航与首屏结构', () => {
  it('提供清晰的返回首页入口，并保持验证码表单首屏可见', () => {
    const wrapper = mount(LoginView, {
      global: {
        stubs: {
          RouterLink,
          LoginForm: { template: '<form data-test="login-form" />' },
        },
      },
    })

    const homeLinks = wrapper.findAll('a[href="/"]')
    expect(homeLinks.length).toBeGreaterThanOrEqual(2)
    expect(wrapper.text()).toContain('返回首页')
    expect(wrapper.find('[data-test="login-form"]').exists()).toBe(true)
  })
})
