import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { fetchAdminDashboard } from './dashboard'

describe('管理员总览 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: {} })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })

  it('读取真实只读 dashboard 聚合接口', async () => {
    await fetchAdminDashboard()
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'get', url:'/admin/dashboard' }))
  })
})
