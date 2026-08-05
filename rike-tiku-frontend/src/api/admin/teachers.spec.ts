import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { changeTeachingAssignmentStatus, createTeacher, createTeachingAssignment, fetchTeachers, updateTeacher } from './teachers'

const adapter = vi.fn((config: AxiosRequestConfig): Promise<AxiosResponse> => Promise.resolve({ data: { records: [] }, status: 200, statusText: 'OK', headers: {}, config }))
http.defaults.adapter = adapter
afterEach(() => adapter.mockClear())

describe('教师管理 API', () => {
  it('教师列表提交分页和筛选参数', async () => { await fetchTeachers({ page: 2, size: 20, employeeNumber: 'T01', name: '张', username: 'teacher', accountStatus: 'ENABLED', profileStatus: 'ACTIVE' }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/teachers', params: expect.objectContaining({ page: 2, employeeNumber: 'T01' }) })) })
  it('创建教师只提交固定教师字段', async () => { await createTeacher({ employeeNumber: 'T01', name: '张老师', username: 'teacher01', accountStatus: 'ENABLED' }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/teachers', data: expect.not.stringContaining('roles') })) })
  it('修改教师不提交工号、用户名或角色', async () => { await updateTeacher(5, { name: '张老师', accountStatus: 'DISABLED', profileStatus: 'ACTIVE' }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'put', url: '/admin/teachers/5', data: expect.not.stringContaining('employeeNumber') })) })
  it('任课关系提交真实班级和科目ID并独立变更状态', async () => { await createTeachingAssignment(5, { classId: 7, subjectId: 1, primary: true, startTime: '2026-08-05T08:00:00.000Z' }); await changeTeachingAssignmentStatus(9, 'ENDED'); expect(adapter).toHaveBeenNthCalledWith(1, expect.objectContaining({ method: 'post', url: '/admin/teachers/5/teaching-assignments', data: expect.stringContaining('classId') })); expect(adapter).toHaveBeenNthCalledWith(2, expect.objectContaining({ method: 'patch', url: '/admin/teaching-assignments/9/status', data: JSON.stringify({ status: 'ENDED' }) })) })
})
