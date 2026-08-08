// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProfileView from './ProfileView.vue'

const mocks = vi.hoisted(() => ({
  fetchProfile: vi.fn(),
  updateProfile: vi.fn(),
  uploadProfileAvatar: vi.fn(),
  deleteProfileAvatar: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  setProfileAvatar: vi.fn(),
  logout: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
  confirm: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.push, replace: mocks.replace }),
}))
vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({
    setProfileAvatar: mocks.setProfileAvatar,
    logout: mocks.logout,
    getDefaultHome: () => '/student',
  }),
}))
vi.mock('../api/profile', () => ({
  fetchProfile: mocks.fetchProfile,
  updateProfile: mocks.updateProfile,
  uploadProfileAvatar: mocks.uploadProfileAvatar,
  deleteProfileAvatar: mocks.deleteProfileAvatar,
}))
vi.mock('../components/auth/ChangePasswordDialog.vue', () => ({
  default: { props: ['modelValue'], template: '<div v-if="modelValue" class="password-dialog">修改密码对话框</div>' },
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.success, error: mocks.error },
  ElMessageBox: { confirm: mocks.confirm },
}))

const baseProfile = {
  displayName: '199班学生01',
  account: {
    username: 'demo_199_01', accountStatus: 'ENABLED', roles: ['STUDENT'], firstLogin: false,
    passwordChangedAt: '2026-08-01T08:00:00', lastLoginAt: '2026-08-08T09:00:00',
  },
  studentProfile: { studentNumber: 'DEMO19901', name: '199班学生01', grade: '高二', currentClass: '199班' },
  teacherProfile: null,
  personal: { introduction: null, avatarDataUrl: null, avatarMime: null, avatarUpdatedAt: null },
}

const stubs = {
  ElAvatar: { props: ['src'], template: '<div class="avatar" :data-src="src"><slot /></div>' },
  ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
  ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
  ElDropdownMenu: { template: '<div><slot /></div>' },
  ElDropdownItem: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
  ElTag: { template: '<span><slot /></span>' },
  ElInput: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
}

function mountView() {
  return mount(ProfileView, {
    global: { stubs, directives: { loading: () => undefined } },
  })
}

describe('统一个人中心', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.confirm.mockResolvedValue(undefined)
    mocks.fetchProfile.mockResolvedValue(structuredClone(baseProfile))
  })

  it('显示学生资料、当前班级、默认头像和修改密码入口', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('199班学生01')
    expect(wrapper.text()).toContain('DEMO19901')
    expect(wrapper.text()).toContain('199班')
    expect(wrapper.get('.avatar').attributes('data-src')).toBeUndefined()
    await wrapper.findAll('button').find((button) => button.text() === '修改密码')!.trigger('click')
    expect(wrapper.text()).toContain('修改密码对话框')
  })

  it('显示教师档案、任课摘要和 ADMIN + TEACHER 多角色事实', async () => {
    mocks.fetchProfile.mockResolvedValue({
      ...structuredClone(baseProfile),
      displayName: '物理管理员教师',
      account: { ...baseProfile.account, username: 'demo_physics_admin', roles: ['ADMIN', 'TEACHER'] },
      studentProfile: null,
      teacherProfile: {
        teacherNumber: 'DEMO-PHY', name: '物理管理员教师', title: '物理教师',
        teachingScopes: [
          { teachingAssignmentId: 1, className: '199班', grade: '高二', subjectName: '物理' },
          { teachingAssignmentId: 2, className: '200班', grade: '高二', subjectName: '物理' },
        ],
      },
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('教师')
    expect(wrapper.text()).toContain('DEMO-PHY')
    expect(wrapper.text()).toContain('199班 · 物理')
    expect(wrapper.text()).toContain('200班 · 物理')
  })

  it('无业务档案的管理员仍可使用同一页面', async () => {
    mocks.fetchProfile.mockResolvedValue({
      ...structuredClone(baseProfile), displayName: 'demo_admin',
      account: { ...baseProfile.account, username: 'demo_admin', roles: ['ADMIN'] },
      studentProfile: null, teacherProfile: null,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('demo_admin')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).not.toContain('学生档案')
    expect(wrapper.text()).not.toContain('教师档案')
  })

  it('保存简介并使用后端返回的 trim 结果', async () => {
    mocks.updateProfile.mockResolvedValue({
      ...structuredClone(baseProfile),
      personal: { ...baseProfile.personal, introduction: '关注力学基础' },
    })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('textarea').setValue('  关注力学基础  ')
    await wrapper.findAll('button').find((button) => button.text() === '保存简介')!.trigger('click')
    await flushPromises()
    expect(mocks.updateProfile).toHaveBeenCalledWith('  关注力学基础  ')
    expect((wrapper.get('textarea').element as HTMLTextAreaElement).value).toBe('关注力学基础')
  })

  it('上传头像后即时同步页面和全局头像，并可删除恢复默认', async () => {
    mocks.uploadProfileAvatar.mockResolvedValue({ avatarDataUrl: 'data:image/png;base64,cG5n', avatarMime: 'image/png', avatarUpdatedAt: '2026-08-08T10:00:00' })
    mocks.deleteProfileAvatar.mockResolvedValue({ avatarDataUrl: null, avatarMime: null, avatarUpdatedAt: null })
    const wrapper = mountView()
    await flushPromises()
    const file = new File(['png'], 'avatar.png', { type: 'image/png' })
    Object.defineProperty(wrapper.get('input[type=file]').element, 'files', { value: [file] })
    await wrapper.get('input[type=file]').trigger('change')
    await flushPromises()
    expect(mocks.uploadProfileAvatar).toHaveBeenCalledWith(file)
    expect(wrapper.get('.avatar').attributes('data-src')).toBe('data:image/png;base64,cG5n')
    expect(mocks.setProfileAvatar).toHaveBeenLastCalledWith('data:image/png;base64,cG5n')

    await wrapper.findAll('button').find((button) => button.text() === '删除头像')!.trigger('click')
    await flushPromises()
    expect(mocks.deleteProfileAvatar).toHaveBeenCalled()
    expect(wrapper.get('.avatar').attributes('data-src')).toBeUndefined()
    expect(mocks.setProfileAvatar).toHaveBeenLastCalledWith(null)
  })

  it('非法图片在前端给出中文提示且不上传', async () => {
    const wrapper = mountView()
    await flushPromises()
    const file = new File(['text'], 'avatar.txt', { type: 'text/plain' })
    Object.defineProperty(wrapper.get('input[type=file]').element, 'files', { value: [file] })
    await wrapper.get('input[type=file]').trigger('change')
    expect(mocks.error).toHaveBeenCalledWith('头像仅支持 PNG 或 JPEG 图片。')
    expect(mocks.uploadProfileAvatar).not.toHaveBeenCalled()
  })
})
