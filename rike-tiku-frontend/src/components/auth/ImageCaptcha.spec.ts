// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ImageCaptcha from './ImageCaptcha.vue'

const authApi = vi.hoisted(() => ({ fetchCaptchaChallenge: vi.fn() }))

vi.mock('../../api/auth', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/auth')>()),
  fetchCaptchaChallenge: authApi.fetchCaptchaChallenge,
}))

const ElInput = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  methods: { focus: vi.fn() },
  template: '<input data-test="code" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

describe('图形验证码组件', () => {
  beforeEach(() => {
    authApi.fetchCaptchaChallenge.mockReset()
    authApi.fetchCaptchaChallenge
      .mockResolvedValueOnce({ challengeId: 'first', image: 'data:image/png;base64,AAA', expiresAt: '2026-08-08T10:00:00Z' })
      .mockResolvedValueOnce({ challengeId: 'second', image: 'data:image/png;base64,BBB', expiresAt: '2026-08-08T10:01:00Z' })
  })

  it('挂载后获取并显示验证码图片', async () => {
    const wrapper = mount(ImageCaptcha, {
      props: { modelValue: '' },
      global: { stubs: { ElInput } },
    })
    await flushPromises()

    expect(authApi.fetchCaptchaChallenge).toHaveBeenCalledWith(undefined)
    expect(wrapper.get('img').attributes('src')).toBe('data:image/png;base64,AAA')
    expect(wrapper.emitted('challenge')?.[0]).toEqual(['first'])
  })

  it('点击图片刷新时废弃旧 challenge 并清空输入', async () => {
    const wrapper = mount(ImageCaptcha, {
      props: { modelValue: 'ABCD' },
      global: { stubs: { ElInput } },
    })
    await flushPromises()
    await wrapper.get('.captcha-image-button').trigger('click')
    await flushPromises()

    expect(authApi.fetchCaptchaChallenge).toHaveBeenLastCalledWith('first')
    expect(wrapper.emitted('update:modelValue')).toContainEqual([''])
    expect(wrapper.emitted('challenge')?.at(-1)).toEqual(['second'])
  })
})
