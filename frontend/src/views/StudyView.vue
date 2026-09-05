<script setup lang="ts">
import { onMounted, ref, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api, SUBJECT_META } from '../api'
import Md from '../components/Md.vue'
const route = useRoute()
interface Msg { role: string; text: string; mode?: string; modeName?: string; subject?: string;
  traces?: any[]; evidence?: any[]; followUps?: string[]; plan?: any; loading?: boolean }
const messages = ref<Msg[]>([])
const input = ref('')
const busy = ref(false)
const mode = ref<string>('')  // 空=自动路由
const modes = [
  { v: '', l: '智能路由' }, { v: 'ask', l: '知识答疑' }, { v: 'tutor', l: '引导教学' },
  { v: 'quiz', l: '出题练习' }, { v: 'plan', l: '学习规划' }
]
const active = computed(() => [...messages.value].reverse().find(m => m.role === 'ai'))
const scrollEl = ref<HTMLElement>()
async function scrollDown() { await nextTick(); if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight }

async function send(text?: string) {
  const q = (text ?? input.value).trim()
  if (!q || busy.value) return
  input.value = ''
  messages.value.push({ role: 'user', text: q })
  const placeholder: Msg = { role: 'ai', text: '', loading: true }
  messages.value.push(placeholder); busy.value = true; scrollDown()
  try {
    const r = await api.ask({ sessionId: 'default', message: q, mode: mode.value || undefined })
    Object.assign(placeholder, {
      loading: false, text: r.answer, mode: r.mode, modeName: r.modeName,
      subject: r.subject, traces: r.traces, evidence: r.evidence,
      followUps: r.followUps, plan: r.plan
    })
  } catch (e: any) {
    Object.assign(placeholder, { loading: false, text: '**请求失败：** ' + e.message + '，请确认后端已在 8080 端口启动。' })
  } finally { busy.value = false; scrollDown() }
}
onMounted(() => { if (route.query.q) send(String(route.query.q)) })
</script>

<template>
  <div class="study-wrap">
    <section class="chat-col card">
      <header class="chat-head">
        <div>
          <div class="chat-title">AI 多 Agent 协同答疑</div>
          <div class="tiny muted">路由 → 检索 → 学科专家 → 教学/出题/规划，全过程可追溯</div>
        </div>
        <div class="mode-tabs">
          <button v-for="m in modes" :key="m.v" class="chip" :class="{on:mode===m.v}"
            @click="mode=m.v">{{ m.l }}</button>
        </div>
      </header>

      <div class="chat-stream" ref="scrollEl">
        <div v-if="!messages.length" class="empty">
          <div class="empty-phi">φ</div>
          <h3>问点什么？例如</h3>
          <div class="empty-sugs">
            <button class="chip" @click="send('循环队列怎么判断队空和队满？')">循环队列判空判满</button>
            <button class="chip" @click="send('IEEE754浮点数阶码为什么偏置127')">IEEE754 阶码</button>
            <button class="chip" @click="send('Dijkstra为什么不能处理负权边')">Dijkstra 与负权</button>
            <button class="chip" @click="send('TCP快恢复和慢开始区别')">TCP 拥塞控制</button>
          </div>
        </div>

        <div v-for="(m,i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar">{{ m.role==='user' ? '我' : 'φ' }}</div>
          <div class="bubble">
            <div v-if="m.loading" class="typing"><span></span><span></span><span></span></div>
            <template v-else>
              <div class="bubble-meta" v-if="m.role==='ai'">
                <span class="st-badge ACTIVE"><span class="dot s-ACTIVE"></span>{{ m.modeName }}</span>
                <span v-if="m.subject" class="tag"
                  :style="{background:SUBJECT_META[m.subject]?.soft,color:SUBJECT_META[m.subject]?.color}">
                  {{ SUBJECT_META[m.subject]?.name }}</span>
              </div>
              <Md :text="m.text" />
              <div v-if="m.followUps?.length" class="follow">
                <button v-for="f in m.followUps" :key="f" class="chip sm" @click="send(f)">{{ f }}</button>
              </div>
            </template>
          </div>
        </div>
      </div>

      <footer class="chat-input">
        <textarea v-model="input" rows="1" placeholder="输入 408 问题，Enter 发送 / Shift+Enter 换行"
          @keydown.enter.exact.prevent="send()"></textarea>
        <button class="btn gold" :disabled="busy" @click="send()">发送</button>
      </footer>
    </section>

    <aside class="trace-col">
      <div class="card card-pad trace-card">
        <div class="trace-h">协同链路 <span class="tiny muted">Agent Trace</span></div>
        <template v-if="active?.traces?.length">
          <div v-for="(t,i) in active.traces" :key="i" class="trace-item">
            <div class="trace-dot"><span class="dot s-ACTIVE"></span></div>
            <div class="trace-body">
              <div class="trace-name">{{ t.role }}</div>
              <div class="trace-action">{{ t.action }}</div>
              <div class="trace-out tiny muted">{{ t.output }} · {{ t.durationMs }}ms</div>
            </div>
          </div>
        </template>
        <div v-else class="tiny muted trace-empty">回答后这里展示问题在各 Agent 间的流转过程</div>
      </div>

      <div class="card card-pad trace-card" v-if="active?.evidence?.length">
        <div class="trace-h">引用考点 <span class="tiny muted">Evidence</span></div>
        <div v-for="e in active.evidence" :key="e.id" class="evi">
          <span class="tag" :style="{background:SUBJECT_META[e.subject]?.soft,color:SUBJECT_META[e.subject]?.color}">
            {{ e.chapter }}</span>
          <div class="evi-title">{{ e.title }}</div>
          <div class="tiny muted">{{ e.summary }}</div>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.study-wrap { display: grid; grid-template-columns: 1.618fr 1fr; gap: var(--u4); height: 100vh; padding: var(--u4); }
.chat-col { display: flex; flex-direction: column; overflow: hidden; }
.chat-head { padding: 16px 20px; border-bottom: 1px solid var(--line); display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap }
.chat-title { font-weight: 720; font-size: 16px; }
.mode-tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.mode-tabs .chip { padding: 5px 11px; font-size: 12.5px; }
.chat-stream { flex: 1; overflow-y: auto; padding: 22px; display: flex; flex-direction: column; gap: 20px; background: var(--surface-2); }
.empty { margin: auto; text-align: center; max-width: 440px; }
.empty-phi { width:64px;height:64px;margin:0 auto 14px;border-radius:18px;font-size:30px;font-weight:800;color:var(--gold-500);
  display:grid;place-items:center;background:var(--gold-050);border:1px solid #ecdcaf; }
.empty h3 { margin-bottom: 14px; font-weight: 700; }
.empty-sugs { display: flex; flex-wrap: wrap; gap: 9px; justify-content: center; }
.msg { display: flex; gap: 12px; }
.msg.user { flex-direction: row-reverse; }
.avatar { width: 36px; height: 36px; border-radius: 11px; flex: none; display: grid; place-items: center;
  font-weight: 700; font-size: 14px; }
.msg.user .avatar { background: var(--ink-800); color: #fff; }
.msg.ai .avatar { background: linear-gradient(135deg,var(--gold-400),var(--gold-500)); color:#1a1408; }
.bubble { max-width: 82%; }
.msg.user .bubble { background: var(--ink-800); color: #fff; padding: 11px 15px; border-radius: 14px 14px 4px 14px; }
.msg.user .bubble :deep(strong){color:#fff}
.msg.ai .bubble { width: 100%; max-width: 100%; }
.bubble-meta { display: flex; gap: 7px; margin-bottom: 8px; align-items: center; }
.follow { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 12px; }
.chip.sm { font-size: 12px; padding: 5px 11px; }
.typing { display: flex; gap: 5px; padding: 6px 0; }
.typing span { width: 8px; height: 8px; border-radius: 50%; background: var(--text-3); animation: blink 1.2s infinite; }
.typing span:nth-child(2){animation-delay:.2s}.typing span:nth-child(3){animation-delay:.4s}
@keyframes blink { 0%,100%{opacity:.25} 50%{opacity:1} }
.chat-input { display: flex; gap: 10px; padding: 14px 18px; border-top: 1px solid var(--line); align-items: flex-end; }
.chat-input textarea { flex:1; resize:none; border:1px solid var(--line-strong);border-radius:12px;padding:11px 14px;max-height:140px;outline:none;background:var(--surface-2) }
.chat-input textarea:focus { border-color: var(--brand-500); background:#fff; }
.trace-col { overflow-y: auto; display: flex; flex-direction: column; gap: var(--u4); padding-right: 2px; }
.trace-h { font-weight: 700; font-size: 14.5px; margin-bottom: 14px; display:flex;gap:8px;align-items:baseline }
.trace-item { display: flex; gap: 11px; position: relative; padding-bottom: 16px; }
.trace-item:not(:last-child)::before { content:''; position:absolute; left:3.5px; top:16px; bottom:0; width:2px;background:var(--line) }
.trace-dot { padding-top: 3px; z-index: 1; }
.trace-name { font-weight: 650; font-size: 13.5px; }
.trace-action { font-size: 12.5px; color: var(--text-2); margin-top: 1px; }
.trace-out { margin-top: 2px; }
.trace-empty { line-height: 1.7; }
.evi { padding: 10px 0; border-bottom: 1px dashed var(--line); }
.evi:last-child{border-bottom:none}
.evi-title { font-weight: 650; font-size: 13.5px; margin: 6px 0 3px; }
@media (max-width:1000px){ .study-wrap{grid-template-columns:1fr;height:auto}.trace-col{display:none} }
</style>
