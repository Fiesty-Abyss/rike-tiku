import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import ChangePasswordDialog from './ChangePasswordDialog.vue'

describe('主动修改密码对话框', () => {
  it('将取消和确认操作渲染到对话框 footer', () => {
    const wrapper = mount(ChangePasswordDialog, {
      props: { modelValue: true },
      global: {
        plugins: [createPinia()],
        stubs: {
          ElDialog: { template: '<section><slot /><slot name="footer" /></section>' },
          ElForm: { template: '<form><slot /></form>' },
          ElFormItem: { template: '<div><slot /></div>' },
          ElInput: true,
          ElButton: { template: '<button><slot /></button>' },
        },
      },
    })

    expect(wrapper.text()).toContain('取消')
    expect(wrapper.text()).toContain('确认修改')
  })
})
