export type ScientificTextSegment =
  | { type: 'text'; value: string }
  | { type: 'math'; value: string; display: boolean }

const MATH_DELIMITER = /\\\[([\s\S]*?)\\\]|\\\(([\s\S]*?)\\\)/g

export function parseScientificText(content: string): ScientificTextSegment[] {
  const segments: ScientificTextSegment[] = []
  let cursor = 0

  for (const match of content.matchAll(MATH_DELIMITER)) {
    const index = match.index ?? 0
    if (index > cursor) segments.push({ type: 'text', value: content.slice(cursor, index) })
    const display = match[1] !== undefined
    segments.push({ type: 'math', value: (display ? match[1] : match[2]) ?? '', display })
    cursor = index + match[0].length
  }

  if (cursor < content.length) segments.push({ type: 'text', value: content.slice(cursor) })
  if (!segments.length && content) segments.push({ type: 'text', value: content })
  return segments
}
