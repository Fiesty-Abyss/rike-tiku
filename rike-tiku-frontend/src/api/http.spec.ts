import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { setAuthenticationErrorHandler } from './http'
import http from './http'

function successfulAdapter(config: AxiosRequestConfig): Promise<AxiosResponse> {
  return Promise.resolve({ data: {}, status: 200, statusText: 'OK', headers: {}, config })
}

function failedAdapter(status: number | undefined, code?: string, message?: string) {
  return () => Promise.reject({
    message,
    response: status ? { status, data: { code, message } } : undefined,
  })
}

beforeEach(() => {
  localStorage.clear()
  setAuthenticationErrorHandler(() => undefined)
})

afterEach(() => {
  localStorage.clear()
  setAuthenticationErrorHandler(() => undefined)
})

describe('Axios认证拦截器', () => {
  it('本地存在Token时自动添加Bearer请求头', async () => {
    localStorage.setItem('rike-tiku.access-token', 'test-token')
    const response = await http.get('/protected', { adapter: successfulAdapter })
    expect(response.config.headers.Authorization).toBe('Bearer test-token')
  })

  it('没有Token时不添加Authorization请求头', async () => {
    const response = await http.get('/public', { adapter: successfulAdapter })
    expect(response.config.headers.Authorization).toBeUndefined()
  })

  it.each([
    [401, 'UNAUTHENTICATED'],
    [401, 'TOKEN_EXPIRED'],
    [401, 'TOKEN_INVALID'],
  ])('%s 和 %s 会清理会话并通知认证处理器', async (status, code) => {
    localStorage.setItem('rike-tiku.access-token', 'test-token')
    const handler = vi.fn()
    setAuthenticationErrorHandler(handler)
    await expect(http.get('/protected', { adapter: failedAdapter(status, code, '认证失败') })).rejects.toMatchObject({ status, code })
    expect(localStorage.getItem('rike-tiku.access-token')).toBeNull()
    expect(handler).toHaveBeenCalledWith(expect.objectContaining({ status, code }))
  })

  it('MUST_CHANGE_PASSWORD交给处理器且不清理Token', async () => {
    localStorage.setItem('rike-tiku.access-token', 'test-token')
    const handler = vi.fn()
    setAuthenticationErrorHandler(handler)
    await expect(http.get('/protected', { adapter: failedAdapter(403, 'MUST_CHANGE_PASSWORD', '请修改密码') })).rejects.toMatchObject({ code: 'MUST_CHANGE_PASSWORD' })
    expect(localStorage.getItem('rike-tiku.access-token')).toBe('test-token')
    expect(handler).toHaveBeenCalledWith(expect.objectContaining({ code: 'MUST_CHANGE_PASSWORD' }))
  })

  it('ACCESS_DENIED交给处理器且保留有效Token', async () => {
    localStorage.setItem('rike-tiku.access-token', 'test-token')
    const handler = vi.fn()
    setAuthenticationErrorHandler(handler)
    await expect(http.get('/protected', { adapter: failedAdapter(403, 'ACCESS_DENIED', '无权限') })).rejects.toMatchObject({ status: 403, code: 'ACCESS_DENIED' })
    expect(localStorage.getItem('rike-tiku.access-token')).toBe('test-token')
    expect(handler).toHaveBeenCalledWith(expect.objectContaining({ code: 'ACCESS_DENIED' }))
  })

  it('后端错误转换为不包含响应对象的ApiError', async () => {
    await expect(http.get('/error', { adapter: failedAdapter(403, 'ROLE_MISMATCH', '入口不匹配') })).rejects.toEqual({
      status: 403,
      code: 'ROLE_MISMATCH',
      message: '入口不匹配',
    })
  })

  it('网络错误提供可读信息', async () => {
    await expect(http.get('/offline', { adapter: failedAdapter(undefined, undefined, 'Network Error') })).rejects.toEqual({
      status: undefined,
      code: undefined,
      message: 'Network Error',
    })
  })
})
