import { describe, expect, it } from 'vitest'
import { normalizeSubjectCode, subjectTheme } from './subjectTheme'

describe('subjectTheme', () => {
  it('resolves stable subjectCode values without relying on database IDs', () => {
    expect(subjectTheme('PHYSICS')).toBe('physics')
    expect(subjectTheme(' chemistry ')).toBe('chemistry')
    expect(subjectTheme('BIOLOGY')).toBe('biology')
  })

  it('rejects unknown values', () => {
    expect(normalizeSubjectCode(1)).toBeUndefined()
    expect(subjectTheme('MATH')).toBeUndefined()
  })
})
