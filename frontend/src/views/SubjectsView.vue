<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api, SUBJECT_META } from '../api'
import Md from '../components/Md.vue'
const route = useRoute()
const cur = ref<string>((route.query.c as string) || 'ds')
const freqFilter = ref('')
const points = ref<any[]>([])
const open = ref<any>(null)
const loading = ref(false)
const order = ['ds', 'co', 'cn', 'os']

async function load() {
  loading.value = true; open.value = null
  points.value = await api.points(cur.value, freqFilter.value || undefined)
  loading.value = false
}
onMounted(load)
watch(cur, load); watch(freqFilter, load)
const grouped = computed(() => {
  const m: Record<string, any[]> = {}
  for (const p of points.value) (m[p.chapter] ||= []).push(p)
  return m
})
const freqTag = (f: string) => f === 'high' ? ['高频', 'var(--st-active)', 'var(--st-active-soft)']
  : f === 'mid' ? ['中频', 'var(--st-loading)', 'var(--st-loading-soft)'] : ['低频', 'var(--text-3)', 'var(--st-inactive-soft)']
</script>

<template>
  <div class="page">
    <div class="page-head row" style="align-items:flex-end;justify-content:space-between;flex-wrap:wrap;gap:12px">
      <div>
        <div class="page-title">四科知识体系</div>
        <div class="page-desc">48 个高频考点按章节组织，点击卡片查看精讲与易错点</div>
      </div>
      <div class="row">
        <button class="chip" :class="{on:freqFilter===''}" @click="freqFilter=''">全部</button>
        <button class="chip" :class="{on:freqFilter==='high'}" @click="freqFilter='high'">仅高频</button>
      </div>
    </div>

    <div class="subj-tabs">
      <button v-for="k in order" :key="k" class="subj-tab" :class="{on:cur===k}"
        :style="cur===k ? {background:SUBJECT_META[k].color} : {}" @click="cur=k">
        <b>{{ SUBJECT_META[k].name }}</b><span>{{ SUBJECT_META[k].en }}</span>
      </button>
    </div>

    <div v-if="loading" class="muted" style="padding:30px">加载中…</div>
    <div v-else class="chapters">
      <section v-for="(ps, ch) in grouped" :key="ch" class="chapter card">
        <header class="chap-h"><span class="chap-bar"></span>{{ ch }}<span class="tiny muted" style="margin-left:auto">{{ ps.length }} 考点</span></header>
        <div class="point-list">
          <button v-for="p in ps" :key="p.id" class="point-item" @click="open=p">
            <span class="freq" :style="{color:freqTag(p.frequency)[1],background:freqTag(p.frequency)[2]}">{{ freqTag(p.frequency)[0] }}</span>
            <span class="pt-title">{{ p.title }}</span>
            <span class="pt-star">{{ '★'.repeat(p.importance) }}</span>
          </button>
        </div>
      </section>
    </div>

    <Teleport to="body">
      <div v-if="open" class="modal-mask" @click.self="open=null">
        <div class="modal card">
          <header class="modal-h">
            <div>
              <span class="tag" :style="{background:SUBJECT_META[open.subject]?.soft,color:SUBJECT_META[open.subject]?.color}">{{ open.chapter }}</span>
              <h3>{{ open.title }}</h3>
            </div>
            <button class="modal-x" @click="open=null">✕</button>
          </header>
          <div class="modal-body"><Md :text="open.detail || open.summary" /></div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.subj-tabs { display: grid; grid-template-columns: repeat(4,1fr); gap: var(--u3); margin-bottom: var(--u5); }
.subj-tab { display:flex;flex-direction:column;align-items:flex-start;gap:2px;padding:15px 18px;border-radius:var(--r-md);
  background:var(--surface);border:1px solid var(--line);color:var(--text-2);text-align:left;transition:all .2s }
.subj-tab b { font-size: 15.5px; }
.subj-tab span { font-size: 11px; font-family: var(--font-mono); opacity:.75 }
.subj-tab.on { color:#fff;border-color:transparent;box-shadow:var(--shadow-1) }
.subj-tab.on span{color:rgba(255,255,255,.8)}
.chapters { display: flex; flex-direction: column; gap: var(--u4); }
.chap-h { display:flex;align-items:center;gap:10px;padding:15px 20px;border-bottom:1px solid var(--line);font-weight:700;font-size:15px }
.chap-bar { width:4px;height:17px;border-radius:4px;background:var(--gold-500) }
.point-list { padding: 8px; display: grid; grid-template-columns: 1fr 1fr; }
.point-item { display:flex;align-items:center;gap:11px;padding:11px 13px;border-radius:10px;text-align:left;transition:background .15s }
.point-item:hover { background: var(--surface-2); }
.freq { font-size:11px;font-weight:700;padding:2px 8px;border-radius:999px;flex:none }
.pt-title { font-size: 14px; flex:1; }
.pt-star { color: var(--gold-500); font-size: 11px; letter-spacing: 1px; }
.modal-mask { position:fixed;inset:0;background:rgba(14,36,48,.45);display:grid;place-items:center;z-index:50;padding:24px;backdrop-filter:blur(2px) }
.modal { width:min(720px,94vw);max-height:86vh;display:flex;flex-direction:column;overflow:hidden }
.modal-h { display:flex;justify-content:space-between;align-items:flex-start;padding:20px 24px;border-bottom:1px solid var(--line) }
.modal-h h3 { font-size: 19px; margin-top: 8px; }
.modal-x { font-size: 16px; color: var(--text-3); padding: 4px 8px; }
.modal-body { padding: 20px 24px; overflow-y: auto; }
@media (max-width:800px){ .point-list{grid-template-columns:1fr}.subj-tabs{grid-template-columns:repeat(2,1fr)} }
</style>
