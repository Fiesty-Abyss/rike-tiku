import { beforeEach, describe, expect, it, vi } from 'vitest'

import http from './http'
import {
  deleteProfileAvatar,
  fetchProfile,
  updateProfile,
  uploadProfileAvatar,
} from './profile'

vi.mock('./http', () => ({
  default: { get: vi.fn(), put: vi.fn(), post: vi.fn(), delete: vi.fn() },
}))

const get = vi.mocked(http.get)
const put = vi.mocked(http.put)
const post = vi.mocked(http.post)
const remove = vi.mocked(http.delete)

describe('个人中心 API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('只通过当前登录身份读取和修改个人资料', async () => {
    get.mockResolvedValueOnce({ data: { displayName: '学生甲' } } as never)
    put.mockResolvedValueOnce({ data: { personal: { introduction: '物理学习' } } } as never)

    await fetchProfile()
    await updateProfile('物理学习')

    expect(get).toHaveBeenCalledWith('/profile')
    expect(put).toHaveBeenCalledWith('/profile', { introduction: '物理学习' })
  })

  it('使用 multipart 上传头像并支持删除', async () => {
    const file = new File(['png'], 'avatar.png', { type: 'image/png' })
    post.mockResolvedValueOnce({ data: { avatarDataUrl: 'data:image/png;base64,cG5n' } } as never)
    remove.mockResolvedValueOnce({ data: { avatarDataUrl: null } } as never)

    await uploadProfileAvatar(file)
    await deleteProfileAvatar()

    const form = post.mock.calls[0][1] as FormData
    expect(post).toHaveBeenCalledWith('/profile/avatar', expect.any(FormData))
    expect(form.get('file')).toBe(file)
    expect(remove).toHaveBeenCalledWith('/profile/avatar')
  })
})
