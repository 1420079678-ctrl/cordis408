// 后端 API 封装：开发期经 Vite 代理到 http://localhost:8080
const BASE = '/api'

async function http<T>(path: string, opts?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
    ...opts
  })
  if (!res.ok) throw new Error('请求失败 ' + res.status)
  return res.json()
}

export const api = {
  ask: (body: any) => http('/ask', { method: 'POST', body: JSON.stringify(body) }),
  subjects: () => http('/subjects'),
  points: (code: string, frequency?: string) =>
    http(`/subjects/${code}/points` + (frequency ? `?frequency=${frequency}` : '')),
  quiz: (params: string) => http('/practice/quiz?' + params),
  grade: (body: any) => http('/practice/grade', { method: 'POST', body: JSON.stringify(body) }),
  diagnosis: (sid: string) => http('/diagnosis?sessionId=' + sid),
  learner: (sid: string) => http('/learner?sessionId=' + sid),
  resetLearner: (sid: string) => http('/learner/reset?sessionId=' + sid, { method: 'POST' }),
  topology: () => http('/runtime/topology'),
  toggle: (id: string, disabled: boolean) =>
    http(`/runtime/entries/${id}/toggle?disabled=${disabled}`, { method: 'POST' }),
  hotReload: (type: string) => http(`/runtime/hot-reload/${type}`, { method: 'POST' })
}

export const SUBJECT_META: Record<string, { name: string; en: string; color: string; soft: string }> = {
  ds: { name: '数据结构', en: 'Data Structure', color: 'var(--ds)', soft: 'var(--ds-soft)' },
  co: { name: '组成原理', en: 'Organization', color: 'var(--co)', soft: 'var(--co-soft)' },
  cn: { name: '计算机网络', en: 'Networks', color: 'var(--cn)', soft: 'var(--cn-soft)' },
  os: { name: '操作系统', en: 'Operating System', color: 'var(--os)', soft: 'var(--os-soft)' }
}
