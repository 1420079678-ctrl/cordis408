<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { api } from '../api'
const topo = ref<any>({ nodes: [], edges: [], summary: {} })
const sel = ref<any>(null)
const busy = ref(false)
const flash = ref('')

// 分层：体现协效果自底向上的供给方向
const LAYER: Record<string, number> = {
  'learner-model': 0, retrieval: 0, router: 0,
  'expert-ds': 1, 'expert-co': 1, 'expert-cn': 1, 'expert-os': 1, tutor: 1, problem: 1,
  diagnosis: 2, planner: 2, supervisor: 2
}
const W = 920, NODE_W = 158, NODE_H = 46
const colX = [70, 381, 692]
const pos = computed(() => {
  const m: Record<string, { x: number; y: number }> = {}
  const rows: Record<number, string[]> = { 0: [], 1: [], 2: [] }
  for (const n of topo.value.nodes) (rows[LAYER[n.entryId] ?? 1]).push(n.entryId)
  for (const layer of [0, 1, 2]) {
    const ids = rows[layer]; const gap = 560 / (ids.length + 1)
    ids.forEach((id, i) => { m[id] = { x: colX[layer], y: 30 + i * gap } })
  }
  return m
})
const H = computed(() => {
  let mx = 1
  for (const n of topo.value.nodes) mx = Math.max(mx, (Object.values(LAYER).filter(l => l === LAYER[n.entryId]).length))
  return Math.max(420, 30 + mx * 96 + 40)
})
function edgePath(e: any) {
  const a = pos.value[e.from], b = pos.value[e.to]; if (!a || !b) return ''
  const x1 = a.x + NODE_W, y1 = a.y + NODE_H / 2, x2 = b.x, y2 = b.y + NODE_H / 2
  const mx = (x1 + x2) / 2
  return `M${x1},${y1} C${mx},${y1} ${mx},${y2} ${x2 - 4},${y2}`
}
function edgeDim(e: any) {
  const a = topo.value.nodes.find((n: any) => n.entryId === e.from)
  return a && a.state !== 'ACTIVE'
}
async function load() {
  const t = await api.topology(); topo.value = t
  if (sel.value) sel.value = t.nodes.find((n: any) => n.uid === sel.value.uid) || null
}
async function toggle(n: any) {
  busy.value = true
  const r = await api.toggle(n.entryId, !n.disabled)
  topo.value = r.topology; sel.value = r.topology.nodes.find((x: any) => x.uid === n.uid) || null
  busy.value = false; notify(n.disabled ? '已启用，依赖者被级联唤醒' : '已停用，依赖者被级联停用（可逆效果 LIFO 回滚）')
}
async function hot(type: string) {
  busy.value = true
  const r = await api.hotReload(type); topo.value = r.topology
  busy.value = false; notify('事务性热替换：' + r.report.message + '（失败将自动回滚）')
}
function notify(s: string) { flash.value = s; setTimeout(() => (flash.value = ''), 3200) }
function select(n: any) { sel.value = n }
onMounted(load)
const states = ['ACTIVE', 'LOADING', 'INACTIVE', 'UNLOADING', 'FAILED']
</script>

<template>
  <div class="page">
    <div class="page-head row" style="justify-content:space-between;align-items:flex-end;flex-wrap:wrap;gap:12px">
      <div>
        <div class="page-title">Agent 运行时拓扑</div>
        <div class="page-desc">12 个 Fiber 的实时状态与协效果依赖边 · 可在线启停、级联、热替换，无需重启</div>
      </div>
      <div class="row" style="gap:8px">
        <button class="btn ghost sm" @click="load">刷新拓扑</button>
        <button class="btn ghost sm" :disabled="busy" @click="hot('subject-expert')">热替换四科专家</button>
        <button class="btn ghost sm" :disabled="busy" @click="hot('supervisor')">热替换调度器</button>
      </div>
    </div>

    <div class="summary card">
      <div v-for="s in states" :key="s" class="sum-item">
        <span class="dot" :class="'s-'+s"></span><span class="sum-lbl">{{ s }}</span>
        <b>{{ topo.summary[s] || 0 }}</b>
      </div>
      <div class="spacer"></div>
      <transition name="fade"><span v-if="flash" class="flash">{{ flash }}</span></transition>
    </div>

    <div class="phi-split" style="align-items:start">
      <div class="card graph-card">
        <div class="layer-tags">
          <span>基础能力层</span><span>学科 / 教学层</span><span>调度诊断层</span>
        </div>
        <svg :viewBox="`0 0 ${W} ${H}`" class="topo-svg">
          <defs>
            <marker id="arrow" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto">
              <path d="M0,0 L8,4.5 L0,9 Z" fill="#9aa6ad" />
            </marker>
          </defs>
          <path v-for="(e,i) in topo.edges" :key="i" :d="edgePath(e)" class="edge"
            :class="{dim:edgeDim(e)}" marker-end="url(#arrow)" />
          <g v-for="n in topo.nodes" :key="n.uid" class="node-g"
            :transform="`translate(${pos[n.entryId]?.x||0},${pos[n.entryId]?.y||0})`"
            :class="{sel:sel?.uid===n.uid}" @click="select(n)">
            <rect :width="NODE_W" :height="NODE_H" rx="11" class="node-rect" :class="'n-'+n.state" />
            <circle cx="14" cy="NODE_H/2" r="5" :class="['s-'+n.state,{pulse:n.state==='LOADING'}]" />
            <text class="node-name" x="28" y="20">{{ n.name }}</text>
            <text class="node-type" x="28" y="36">{{ n.entryId }}</text>
          </g>
        </svg>
      </div>

      <div>
        <div v-if="sel" class="card card-pad detail">
          <div class="row">
            <span class="st-badge" :class="sel.state"><span class="dot" :class="'s-'+sel.state"></span>{{ sel.state }}</span>
            <span class="spacer"></span>
            <button class="btn sm" :class="sel.disabled?'gold':''" :disabled="busy" @click="toggle(sel)">
              {{ sel.disabled ? '启用' : '停用' }}
            </button>
          </div>
          <h3>{{ sel.name }}</h3>
          <div class="tiny muted" style="font-family:var(--font-mono)">{{ sel.entryId }} · type={{ sel.type }}</div>
          <div class="kv"><span>inject 依赖</span><div>
            <span v-for="k in sel.inject" :key="k" class="key-tag need">{{ k }}</span>
            <span v-if="!sel.inject.length" class="muted tiny">无（根能力）</span>
          </div></div>
          <div class="kv"><span>provide 提供</span><div>
            <span v-for="k in sel.provide" :key="k" class="key-tag give">{{ k }}</span></div></div>
          <div class="kv"><span>激活耗时</span><b>{{ sel.activateDurationMs }} ms</b></div>
          <div v-if="sel.error" class="err-box">{{ sel.error }}</div>
        </div>
        <div class="card card-pad theory">
          <h4>这张图如何对应论文</h4>
          <ul>
            <li><b>空间维 · 协效果：</b>边即 inject→provide 契约；只有 ACTIVE 节点真正提供能力。</li>
            <li><b>级联：</b>停用 retrieval，所有依赖 knowledge.base 的节点立即 INACTIVE；恢复则级联唤醒。</li>
            <li><b>时间维 · 可逆效果：</b>停用按 LIFO 自动回滚其全部副作用，再启用得到一致静止态。</li>
            <li><b>惯性状态机：</b>INACTIVE→LOADING→ACTIVE→UNLOADING，失败进入 FAILED。</li>
            <li><b>事务热替换：</b>三阶段卸载→装载，失败自动回滚，会话不中断。</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.summary { display:flex;align-items:center;gap:20px;padding:13px 20px;margin-bottom:var(--u4);flex-wrap:wrap }
.sum-item { display:flex;align-items:center;gap:8px;font-size:13.5px;color:var(--text-2) }
.sum-item b { font-size:16px;color:var(--text-1) }
.flash { font-size:12.5px;color:var(--brand-600);background:var(--brand-050);padding:5px 12px;border-radius:99px }
.graph-card { padding: 14px 10px 8px; overflow:hidden }
.layer-tags { display:grid;grid-template-columns:repeat(3,1fr);margin:4px 0 2px;color:var(--text-3);font-size:11.5px;font-weight:600;letter-spacing:1px }
.layer-tags span { text-align:center;white-space:nowrap;overflow:hidden }
.topo-svg { width:100%;height:auto;display:block }
.edge { fill:none;stroke:#a9b4bb;stroke-width:1.6;transition:all .3s }
.edge.dim { stroke:#d8d4c9;stroke-dasharray:4 4 }
.node-g { cursor:pointer }
.node-rect { fill:#fff;stroke:var(--line-strong);stroke-width:1.4;transition:all .25s var(--ease-phi) }
.node-g:hover .node-rect { stroke:var(--brand-500);filter:drop-shadow(0 3px 6px rgba(22,105,122,.18)) }
.node-g.sel .node-rect { stroke:var(--gold-500);stroke-width:2.2 }
.node-rect.n-ACTIVE { fill:#f4fbf8 }
.node-rect.n-INACTIVE { fill:#f1f2f3;stroke:#d3d8db }
.node-rect.n-LOADING { fill:var(--st-loading-soft) }
.node-rect.n-FAILED { fill:var(--st-failed-soft) }
.node-name { font-size:12.5px;font-weight:700;fill:var(--ink-900) }
.node-type { font-size:9.5px;fill:var(--text-3);font-family:var(--font-mono) }
.n-INACTIVE ~ .node-name, .node-rect.n-INACTIVE ~ text{fill:#94a0a8}
.detail { margin-bottom: var(--u4) }
.detail h3 { margin:12px 0 2px;font-size:18px }
.kv { display:flex;justify-content:space-between;gap:12px;padding:11px 0;border-bottom:1px dashed var(--line);font-size:13px;align-items:flex-start }
.kv>span { color:var(--text-3);flex:none }
.kv b { font-family:var(--font-mono) }
.key-tag { display:inline-block;font-family:var(--font-mono);font-size:11px;padding:2px 8px;border-radius:7px;margin:2px 4px 2px 0 }
.key-tag.need { background:var(--brand-050);color:var(--brand-600) }
.key-tag.give { background:var(--gold-050);color:#9a7a2c }
.err-box { margin-top:10px;background:var(--st-failed-soft);color:var(--st-failed);padding:10px;border-radius:9px;font-size:12.5px }
.theory h4 { font-size:14.5px;margin-bottom:10px }
.theory ul { margin-left:18px;display:flex;flex-direction:column;gap:9px;font-size:12.8px;color:var(--text-2);line-height:1.6 }
.theory b { color:var(--ink-800) }
</style>
