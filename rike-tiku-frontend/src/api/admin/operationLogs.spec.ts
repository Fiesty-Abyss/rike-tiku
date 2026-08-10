import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { fetchOperationLogs } from './operationLogs'

describe('管理员操作日志 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: { records: [], total: 0, page: 1, size: 20, pages: 0 } })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })
  it('使用 ADMIN 操作日志分页接口和筛选条件', async () => { await fetchOperationLogs({ page: 2, size: 20, module: 'QUESTION', action: 'APPROVED', result: 'SUCCESS' }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/operation-logs', params: expect.objectContaining({ page: 2, module: 'QUESTION' }) })) })
})
