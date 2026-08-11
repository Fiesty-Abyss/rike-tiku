export const SUBJECT_CODES = ['PHYSICS', 'CHEMISTRY', 'BIOLOGY'] as const

export type SubjectCode = typeof SUBJECT_CODES[number]
export type SubjectTheme = 'physics' | 'chemistry' | 'biology'

export function normalizeSubjectCode(value: unknown): SubjectCode | undefined {
  const code = String(value ?? '').trim().toUpperCase()
  return SUBJECT_CODES.includes(code as SubjectCode) ? code as SubjectCode : undefined
}

export function subjectTheme(value: unknown): SubjectTheme | undefined {
  return normalizeSubjectCode(value)?.toLowerCase() as SubjectTheme | undefined
}
