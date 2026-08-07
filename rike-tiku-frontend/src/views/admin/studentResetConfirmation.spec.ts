import { describe, expect, it, vi } from 'vitest'
import { confirmStudentPasswordReset } from './studentResetConfirmation'

describe('管理员重置学生密码确认', () => {
  it('取消确认时正常返回 false，不产生未处理拒绝', async () => {
    const confirm = vi.fn().mockRejectedValue('cancel')

    await expect(confirmStudentPasswordReset(confirm)).resolves.toBe(false)
    expect(confirm).toHaveBeenCalledOnce()
  })

  it('确认时返回 true', async () => {
    const confirm = vi.fn().mockResolvedValue('confirm')

    await expect(confirmStudentPasswordReset(confirm)).resolves.toBe(true)
  })
})
