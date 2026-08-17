export type ScientificTextSegment =
  | { type: 'text'; value: string }
  | { type: 'math'; value: string; display: boolean }

const MATH_DELIMITER = /\\\[([\s\S]*?)\\\]|\\\(([\s\S]*?)\\\)/g

export function parseScientificText(content: string): ScientificTextSegment[] {
  // Some legacy JSON writers escaped the runtime delimiter twice. Normalize
  // only delimiter prefixes so ordinary paths such as C:\\temp remain text.
  const normalized = content.replace(/\\\\(?=[()[\]])/g, '\\')
  const segments: ScientificTextSegment[] = []
  let cursor = 0

  for (const match of normalized.matchAll(MATH_DELIMITER)) {
    const index = match.index ?? 0
    if (index > cursor) segments.push({ type: 'text', value: normalized.slice(cursor, index) })
    const display = match[1] !== undefined
    segments.push({ type: 'math', value: (display ? match[1] : match[2]) ?? '', display })
    cursor = index + match[0].length
  }

  if (cursor < normalized.length) segments.push({ type: 'text', value: normalized.slice(cursor) })
  if (!segments.length && normalized) segments.push({ type: 'text', value: normalized })
  return segments
}
