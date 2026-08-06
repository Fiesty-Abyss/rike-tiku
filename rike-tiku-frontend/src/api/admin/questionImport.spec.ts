import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { confirmQuestionImport, previewQuestionImport } from './questionImport'

describe('管理员题库导入 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: {} })
  const file = new File(['xlsx'], 'physics.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })
  it('预检查使用固定 multipart 文件字段', async () => {
    await previewQuestionImport(file)
    const request = adapter.mock.calls[0][0]
    expect(request).toMatchObject({ method: 'post', url: '/admin/question-import/preview' })
    expect(request.data.get('file')).toBe(file)
    expect(request.data.get('previewFileHash')).toBeNull()
  })
  it('确认重新上传文件并提交预检查文件哈希，而非预览 JSON', async () => {
    await confirmQuestionImport(file, 'abc123')
    const request = adapter.mock.calls[0][0]
    expect(request).toMatchObject({ method: 'post', url: '/admin/question-import/confirm' })
    expect(request.data.get('file')).toBe(file)
    expect(request.data.get('previewFileHash')).toBe('abc123')
  })
})
