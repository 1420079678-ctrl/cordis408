// 极简、安全的 Markdown -> HTML（先转义，再按行解析常见语法）
function esc(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
function inline(s: string): string {
  let t = esc(s)
  t = t.replace(/`([^`]+)`/g, '<code>$1</code>')
  t = t.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  t = t.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  return t
}

export function renderMarkdown(src: string): string {
  if (!src) return ''
  const lines = src.replace(/\r/g, '').split('\n')
  const html: string[] = []
  let list: 'ul' | 'ol' | null = null
  let quote = false
  const closeList = () => { if (list) { html.push(`</${list}>`); list = null } }
  const closeQuote = () => { if (quote) { html.push('</blockquote>'); quote = false } }

  for (let raw of lines) {
    const line = raw.trimEnd()
    if (!line.trim()) { closeList(); closeQuote(); continue }
    let m
    if ((m = line.match(/^###\s+(.*)/))) { closeList(); closeQuote(); html.push(`<h3>${inline(m[1])}</h3>`); continue }
    if ((m = line.match(/^##\s+(.*)/))) { closeList(); closeQuote(); html.push(`<h2>${inline(m[1])}</h2>`); continue }
    if ((m = line.match(/^#\s+(.*)/))) { closeList(); closeQuote(); html.push(`<h1>${inline(m[1])}</h1>`); continue }
    if ((m = line.match(/^>\s?(.*)/))) {
      closeList(); if (!quote) { html.push('<blockquote>'); quote = true }
      html.push(`<p>${inline(m[1])}</p>`); continue
    }
    if ((m = line.match(/^\s*[-*]\s+(.*)/))) {
      closeQuote(); if (list !== 'ul') { closeList(); html.push('<ul>'); list = 'ul' }
      html.push(`<li>${inline(m[1])}</li>`); continue
    }
    if ((m = line.match(/^\s*\d+\.\s+(.*)/))) {
      closeQuote(); if (list !== 'ol') { closeList(); html.push('<ol>'); list = 'ol' }
      html.push(`<li>${inline(m[1])}</li>`); continue
    }
    closeList(); closeQuote()
    html.push(`<p>${inline(line)}</p>`)
  }
  closeList(); closeQuote()
  return html.join('\n')
}
