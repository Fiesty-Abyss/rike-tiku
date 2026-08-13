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
          PasswordRecoveryDialog: { template: '<div data-test="password-recovery" />' },
        },
      },
    })

    const homeLinks = wrapper.findAll('a[href="/"]')
    expect(homeLinks.length).toBeGreaterThanOrEqual(2)
    expect(wrapper.text()).toContain('返回首页')
    expect(wrapper.text()).toContain('高中理科学习与教学管理')
    expect(wrapper.text()).not.toContain('从清晰的练习开始')
    expect(wrapper.find('[data-test="login-form"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('遇到登录问题？')
    expect(wrapper.text()).toContain('申请密码恢复')
    const recovery = wrapper.get('.login-note-button')
    expect(recovery.attributes('aria-haspopup')).toBe('dialog')
    expect(wrapper.find('[data-test="password-recovery"]').exists()).toBe(true)
  })
})
