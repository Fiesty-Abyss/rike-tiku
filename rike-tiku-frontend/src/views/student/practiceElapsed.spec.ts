import { describe, expect, it } from 'vitest'
import { PracticeElapsedTimer } from './practiceElapsed'

describe('学生练习用时', () => {
  it('在题目切换时累计每题内存用时', () => {
    let now = 0
    const timer = new PracticeElapsedTimer(() => now)
    timer.enter(1)
    now = 4_800
    timer.enter(2)
    now = 12_200

    expect(timer.seconds(1)).toBe(4)
    expect(timer.seconds(2)).toBe(7)
    timer.pause()
    expect(timer.seconds(2)).toBe(7)
  })

  it('不产生负数，也不依赖浏览器持久化存储', () => {
    let now = 10_000
    const timer = new PracticeElapsedTimer(() => now)
    timer.enter(1)
    now = 9_000
    expect(timer.seconds(1)).toBe(0)
  })
})
