import { beforeEach, describe, expect, it, vi } from 'vitest'
import { executePasswordRecovery } from './passwordRecoveryAction'

describe('管理员恢复默认密码交互', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('取消确认时不调用恢复 API', async () => {
    const recover = vi.fn()
    const result = await executePasswordRecovery(
      vi.fn().mockRejectedValue('cancel'),
      recover,
    )
    expect(result).toBeNull()
    expect(recover).not.toHaveBeenCalled()
  })

  it('确认后执行恢复并返回一次性结果', async () => {
    const response = { resetCount: 2, initialPassword: 'a1234567', mustChangePassword: true }
    const recover = vi.fn().mockResolvedValue(response)
    await expect(executePasswordRecovery(vi.fn().mockResolvedValue('confirm'), recover)).resolves.toEqual(response)
    expect(recover).toHaveBeenCalledOnce()
  })

  it('API 失败向页面抛出以显示受控错误', async () => {
    await expect(executePasswordRecovery(
      vi.fn().mockResolvedValue('confirm'),
      vi.fn().mockRejectedValue(new Error('controlled failure')),
    )).rejects.toThrow('controlled failure')
  })

  it('一次性密码不写入 Local Storage 或 Session Storage', async () => {
    await executePasswordRecovery(
      vi.fn().mockResolvedValue('confirm'),
      vi.fn().mockResolvedValue({ initialPassword: 'a1234567' }),
    )
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })
})
