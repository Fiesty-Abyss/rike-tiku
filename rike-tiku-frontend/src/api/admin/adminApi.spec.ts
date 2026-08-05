import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { changeClassStatus, createClass, fetchClasses, updateClass } from './classes'
import { confirmStudentImport, previewStudentImport } from './studentImport'
import http from '../http'

const adapter = vi.fn((config: AxiosRequestConfig): Promise<AxiosResponse> => Promise.resolve({ data: { records: [], accounts: [] }, status: 200, statusText: 'OK', headers: {}, config }))

afterEach(() => { adapter.mockClear() })

describe('管理员前端 API', () => {
  it('班级列表携带分页和筛选参数', async () => {
    await fetchClasses({ page: 2, size: 20, code: 'G1', name: '一班', grade: '高一', status: 'ACTIVE' })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/classes', params: expect.objectContaining({ page: 2, size: 20, status: 'ACTIVE' }) }))
  })

  it('创建和修改班级的请求字段符合接口边界', async () => {
    await createClass({ classCode: 'G1-01', className: '高一一班', grade: '高一', enrollmentYear: 2026 })
    await updateClass(7, { className: '高一一班', grade: '高一', enrollmentYear: 2026 })
    expect(adapter).toHaveBeenNthCalledWith(1, expect.objectContaining({ method: 'post', data: expect.stringContaining('classCode') }))
    expect(adapter).toHaveBeenNthCalledWith(2, expect.objectContaining({ method: 'put', url: '/admin/classes/7', data: expect.not.stringContaining('classCode') }))
  })

  it('状态切换仅向专用接口提交状态', async () => {
    await changeClassStatus(7, { status: 'DISABLED' })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'patch', url: '/admin/classes/7/status', data: JSON.stringify({ status: 'DISABLED' }) }))
  })

  it('预检查与确认均以同一原始文件使用 multipart 上传', async () => {
    const file = new File(['xlsx-content'], 'students.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    await previewStudentImport(file)
    await confirmStudentImport(file)
    const previewRequest = adapter.mock.calls[0][0]
    const confirmRequest = adapter.mock.calls[1][0]
    expect(previewRequest.url).toBe('/admin/student-import/preview')
    expect(confirmRequest.url).toBe('/admin/student-import/confirm')
    expect(previewRequest.data).toBeInstanceOf(FormData)
    expect(confirmRequest.data).toBeInstanceOf(FormData)
    expect(previewRequest.data.get('file')).toBe(file)
    expect(confirmRequest.data.get('file')).toBe(file)
  })
})

http.defaults.adapter = adapter
