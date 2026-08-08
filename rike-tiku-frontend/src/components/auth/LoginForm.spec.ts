// @vitest-environment jsdom
import { defineComponent, onMounted } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginForm from './LoginForm.vue'

const refreshCaptcha = vi.fn()

vi.mock('./ImageCaptcha.vue', () => ({
  default: defineComponent({
    props: { modelValue: { type: String, required: true } },
    emits: ['update:modelValue', 'challenge'],
    setup(_, { emit, expose }) {
      onMounted(() => emit('challenge', 'challenge-1'))
      expose({ refresh: refreshCaptcha, focus: vi.fn() })
      return { emit }
    },
    template: '<input data-test="captcha" :value="modelValue" @input="emit(\'update:modelValue\', $event.target.value)" />',
  }),
}))

const ElForm = defineComponent({
  emits: ['submit'],
  setup(_, { expose }) {
    expose({
      validate: () => Promise.resolve(true),
      validateField: () => Promise.resolve(true),
    })
  },
  template: '<form><slot /></form>',
})
const ElInput = {
  inheritAttrs: false,
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue'],
  template: '<input :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

function mountForm() {
  return mount(LoginForm, {
    props: { loading: false, errorMessage: '' },
    global: {
      stubs: {
        ElForm,
        ElFormItem: { template: '<div><slot /></div>' },
        ElInput,
        ElAlert: true,
        ElButton: { template: '<button type="submit"><slot /></button>' },
      },
    },
  })
}

async function enterCredentials(wrapper: ReturnType<typeof mountForm>) {
  await wrapper.get('input[placeholder="请输入用户名"]').setValue('demo_student')
  await wrapper.get('input[placeholder="请输入密码"]').setValue('a1234567')
}

async function submitForm(wrapper: ReturnType<typeof mountForm>) {
  wrapper.findComponent(ElForm).vm.$emit('submit', new Event('submit'))
  await flushPromises()
}

describe('登录表单验证码交互', () => {
  beforeEach(() => refreshCaptcha.mockReset())

  it('初始不显示验证码，第一次提交只展开验证码', async () => {
    const wrapper = mountForm()
    await enterCredentials(wrapper)
    expect(wrapper.find('[data-test="captcha"]').exists()).toBe(false)

    await submitForm(wrapper)

    expect(wrapper.find('[data-test="captcha"]').exists()).toBe(true)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('第二次提交携带 challengeId 和 captchaCode', async () => {
    const wrapper = mountForm()
    await enterCredentials(wrapper)
    await submitForm(wrapper)
    await wrapper.get('[data-test="captcha"]').setValue('aB7k')
    await submitForm(wrapper)

    expect(wrapper.emitted('submit')?.[0]).toEqual([{
      username: 'demo_student',
      password: 'a1234567',
      challengeId: 'challenge-1',
      captchaCode: 'aB7k',
    }])
  })

  it('登录失败后的刷新入口会刷新验证码而不清空账号', async () => {
    const wrapper = mountForm()
    await enterCredentials(wrapper)
    await submitForm(wrapper)

    wrapper.vm.refreshCaptcha()

    expect(refreshCaptcha).toHaveBeenCalledOnce()
    expect(wrapper.get('input[placeholder="请输入用户名"]').element.value).toBe('demo_student')
  })
})
