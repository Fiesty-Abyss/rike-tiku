import { describe, expect, it, vi } from 'vitest'
import { startMessagePolling } from './messagePolling'

describe('message polling', () => {
  it('polls every seven seconds and stops when leaving the page', () => {
    vi.useFakeTimers()
    const load = vi.fn()
    const stop = startMessagePolling(load)
    vi.advanceTimersByTime(14_000)
    expect(load).toHaveBeenCalledTimes(2)
    stop()
    vi.advanceTimersByTime(7_000)
    expect(load).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })
})
