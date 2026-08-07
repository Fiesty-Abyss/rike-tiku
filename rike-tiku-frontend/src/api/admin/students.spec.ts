import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import {
  createStudent,
  fetchStudent,
  fetchStudents,
  resetStudentPassword,
  transferStudent,
  updateStudent,
} from './students'

const adapter = vi.fn((config: AxiosRequestConfig): Promise<AxiosResponse> => Promise.resolve({
  data: { records: [], student: {}, classHistory: [], initialPassword: 'OnceOnly1' },
  status: 200,
  statusText: 'OK',
  headers: {},
  config,
}))
http.defaults.adapter = adapter
afterEach(() => adapter.mockClear())

describe('管理员学生管理 API', () => {
  it('列表提交分页和全部筛选参数', async () => {
    await fetchStudents({ page: 2, size: 20, studentNumber: 'S01', name: '学生', username: 'demo', classId: 7, grade: '高三', accountStatus: 'ENABLED', profileStatus: 'ACTIVE' })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/students', params: expect.objectContaining({ page: 2, classId: 7 }) }))
  })

  it('详情使用学生档案 ID', async () => {
    await fetchStudent(9)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/students/9' }))
  })

  it('新增只提交学生资料与目标班级', async () => {
    await createStudent({ studentNumber: 'S01', name: '学生', username: 'student01', grade: '高三', classId: 7 })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/students', data: expect.not.stringContaining('password') }))
  })

  it('编辑不提交用户ID、密码摘要、角色或历史关系', async () => {
    await updateStudent(9, { name: '学生', grade: '高三', accountStatus: 'DISABLED', profileStatus: 'ACTIVE' })
    const request = adapter.mock.calls[0][0]
    expect(request).toEqual(expect.objectContaining({ method: 'put', url: '/admin/students/9' }))
    expect(String(request.data)).not.toMatch(/studentNumber|username|role|password|classHistory/)
  })

  it('调班只提交目标班级ID', async () => {
    await transferStudent(9, 8)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/students/9/transfer', data: JSON.stringify({ classId: 8 }) }))
  })

  it('密码重置使用独立接口且不提交明文', async () => {
    await resetStudentPassword(9)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/students/9/reset-password', data: undefined }))
  })
})
