export type AiInline =
  | { type: 'text'; value: string }
  | { type: 'strong'; value: string }
  | { type: 'code'; value: string }
  | { type: 'math'; value: string; display: boolean }

export type AiBlock =
  | { type: 'paragraph'; content: AiInline[] }
  | { type: 'unordered-list' | 'ordered-list'; items: AiInline[][] }

const tokenPattern = /(\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\*\*[^*\n]+\*\*|`[^`\n]+`)/g

function normalizeDollarMath(value: string): string {
  return value
    .replace(/\$\$([^$\n]+)\$\$/g, String.raw`\[$1\]`)
    .replace(/(^|[\s（(，。；：])\$([^$\n]{1,160}?[=+\-*/^_\\][^$\n]{0,160})\$(?=$|[\s）)，。；：])/g,
      (_, prefix, formula) => `${prefix}\\(${formula}\\)`)
}

export function parseAiInline(raw: string): AiInline[] {
  const value = normalizeDollarMath(raw)
  const result: AiInline[] = []
  let cursor = 0
  for (const match of value.matchAll(tokenPattern)) {
    const index = match.index ?? 0
    if (index > cursor) result.push({ type: 'text', value: value.slice(cursor, index) })
    const token = match[0]
    if (token.startsWith('\\[')) result.push({ type: 'math', value: token.slice(2, -2), display: true })
    else if (token.startsWith('\\(')) result.push({ type: 'math', value: token.slice(2, -2), display: false })
    else if (token.startsWith('**')) result.push({ type: 'strong', value: token.slice(2, -2) })
    else result.push({ type: 'code', value: token.slice(1, -1) })
    cursor = index + token.length
  }
  if (cursor < value.length) result.push({ type: 'text', value: value.slice(cursor) })
  return result.length ? result : [{ type: 'text', value }]
}

export function parseAiScientificContent(raw: string): AiBlock[] {
  const lines = (raw || '').replace(/\r\n?/g, '\n').split('\n')
  const blocks: AiBlock[] = []
  let paragraph: string[] = []
  let list: { ordered: boolean; items: string[] } | undefined
  const flushParagraph = () => {
    if (paragraph.length) blocks.push({ type: 'paragraph', content: parseAiInline(paragraph.join('\n')) })
    paragraph = []
  }
  const flushList = () => {
    if (list) blocks.push({ type: list.ordered ? 'ordered-list' : 'unordered-list', items: list.items.map(parseAiInline) })
    list = undefined
  }
  for (const line of lines) {
    const item = line.match(/^\s*(?:(\d+)\.|[-*])\s+(.+)$/)
    if (item) {
      flushParagraph()
      const ordered = Boolean(item[1])
      if (list && list.ordered !== ordered) flushList()
      list ??= { ordered, items: [] }
      list.items.push(item[2])
    } else if (!line.trim()) {
      flushParagraph(); flushList()
    } else {
      flushList(); paragraph.push(line)
    }
  }
  flushParagraph(); flushList()
  return blocks
}
