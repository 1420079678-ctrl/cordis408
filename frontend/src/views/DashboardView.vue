<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api, SUBJECT_META } from '../api'
const router = useRouter()
const stats = ref<any>({})
const learner = ref<any>({ subjects: {}, accuracy: 0 })
const order = ['ds', 'co', 'cn', 'os']
const quick = [
  '哈夫曼树WPL怎么算？', '讲一下Cache的三种地址映射', 'TCP为什么要三次握手',
  'LRU页面置换怎么算缺页率', '给我出几道数据结构题', '帮我做个408复习规划'
]
onMounted(async () => {
  const s = await api.subjects(); stats.value = s.stats || {}
  learner.value = await api.learner('default')
})
const totalPoints = computed(() => order.reduce((a, k) => a + (stats.value[k]?.points || 0), 0))
const totalHigh = computed(() => order.reduce((a, k) => a + (stats.value[k]?.highFrequency || 0), 0))
const totalQuiz = computed(() => order.reduce((a, k) => a + (stats.value[k]?.quizzes || 0), 0))
function ask(q: string) { router.push({ path: '/study', query: { q } }) }
</script>

<template>
  <div class="page">
    <section class="hero card">
      <div class="hero-text">
        <div class="kicker">SPATIOTEMPORAL-COMPOSABLE AI TUTOR</div>
        <h1>408 计算机考研 · 多 Agent 智能学习系统</h1>
        <p>以「可逆效果 + 响应式协效果」运行时为内核，12 个专业 Agent 动态协同：
          意图路由 → 知识检索 → 四科专家 → 教学/出题/诊断/规划，可热插拔、可观测、可回滚。</p>
        <div class="row" style="margin-top:18px;gap:10px">
          <button class="btn gold" @click="router.push('/study')">开始 AI 答疑 →</button>
          <button class="btn ghost" @click="router.push('/agents')">查看 Agent 运行时</button>
        </div>
      </div>
      <div class="hero-stat">
        <div class="hs-num">4</div><div class="hs-lbl">核心学科</div>
        <div class="hs-num">{{ totalPoints }}</div><div class="hs-lbl">精讲考点</div>
        <div class="hs-num">{{ totalHigh }}</div><div class="hs-lbl">高频考点</div>
        <div class="hs-num">{{ totalQuiz }}</div><div class="hs-lbl">精选题库</div>
      </div>
    </section>

    <h2 class="sec-title">四科知识图谱</h2>
    <section class="subj-grid">
      <div v-for="k in order" :key="k" class="subj-card card" @click="router.push('/subjects?c='+k)">
        <div class="subj-top">
          <span class="subj-badge" :style="{background:SUBJECT_META[k].soft,color:SUBJECT_META[k].color}">
            {{ SUBJECT_META[k].en }}</span>
          <span class="muted tiny">→</span>
        </div>
        <div class="subj-name">{{ SUBJECT_META[k].name }}</div>
        <div class="subj-meta">
          <span><b>{{ stats[k]?.chapters || 0 }}</b> 章</span>
          <span><b>{{ stats[k]?.points || 0 }}</b> 考点</span>
          <span class="hf"><b>{{ stats[k]?.highFrequency || 0 }}</b> 高频</span>
          <span><b>{{ stats[k]?.quizzes || 0 }}</b> 题</span>
        </div>
        <div class="bar"><i :style="{width:(learner.subjects[k]?.accuracy||0)+'%',background:SUBJECT_META[k].color}"></i></div>
        <div class="tiny muted" style="margin-top:6px">当前掌握度 {{ learner.subjects[k]?.accuracy || 0 }}%</div>
      </div>
    </section>

    <div class="phi-split" style="margin-top:26px">
      <section class="card card-pad">
        <h2 class="sec-title" style="margin-top:0">快速提问</h2>
        <div class="quick-list">
          <button v-for="q in quick" :key="q" class="quick-item" @click="ask(q)">
            <span class="q-ico">✦</span>{{ q }}
          </button>
        </div>
      </section>
      <section class="card card-pad">
        <h2 class="sec-title" style="margin-top:0">我的学习数据</h2>
        <div class="learn-ring">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="50" class="ring-bg"/>
            <circle cx="60" cy="60" r="50" class="ring-fg"
              :stroke-dasharray="`${(learner.accuracy||0)*3.14} 314`"/>
            <text x="60" y="58" text-anchor="middle" class="ring-num">{{ learner.accuracy||0 }}%</text>
            <text x="60" y="76" text-anchor="middle" class="ring-txt">总正确率</text>
          </svg>
        </div>
        <div class="learn-fact"><span>累计提问</span><b>{{ learner.totalAsked||0 }}</b></div>
        <div class="learn-fact"><span>累计答题</span><b>{{ learner.totalAnswered||0 }}</b></div>
        <button class="btn ghost sm" style="width:100%;margin-top:12px" @click="router.push('/profile')">查看完整画像</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hero { display: grid; grid-template-columns: 1.618fr 1fr; overflow: hidden; margin-bottom: 26px; }
.hero-text { padding: 38px 40px; }
.kicker { font-family: var(--font-mono); font-size: 11px; letter-spacing: 2px; color: var(--gold-500); font-weight: 700; }
.hero-text h1 { font-size: 27px; margin: 12px 0 12px; line-height: 1.35; font-weight: 780; }
.hero-text p { color: var(--text-2); font-size: 14.5px; max-width: 560px; }
.hero-stat {
  background: linear-gradient(160deg, var(--ink-800), var(--ink-900)); color: #fff;
  padding: 32px; display: grid; grid-template-columns: 1fr 1fr; align-content: center; gap: 4px 10px;
}
.hs-num { font-size: 30px; font-weight: 780; color: var(--gold-400); line-height: 1.1; }
.hs-lbl { font-size: 12px; color: rgba(255,255,255,.6); margin-bottom: 14px; }
.sec-title { font-size: 17px; font-weight: 700; margin: 4px 0 14px; }
.subj-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--u4); }
.subj-card { padding: 20px; cursor: pointer; transition: transform .2s var(--ease-phi), box-shadow .2s; }
.subj-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-2); }
.subj-top { display: flex; justify-content: space-between; align-items: center; }
.subj-badge { padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; font-family: var(--font-mono); }
.subj-name { font-size: 18px; font-weight: 720; margin: 12px 0 12px; }
.subj-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12.5px; color: var(--text-2); margin-bottom: 12px; }
.subj-meta b { color: var(--text-1); font-size: 14px; }
.quick-list { display: flex; flex-direction: column; gap: 9px; }
.quick-item {
  display: flex; align-items: center; gap: 10px; text-align: left; padding: 12px 15px;
  background: var(--surface-2); border: 1px solid var(--line); border-radius: var(--r-md);
  font-size: 14px; color: var(--text-1); transition: all .18s;
}
.quick-item:hover { border-color: var(--brand-500); background: var(--brand-050); transform: translateX(3px); }
.q-ico { color: var(--gold-500); font-size: 12px; }
.learn-ring { display: grid; place-items: center; margin: 6px 0 14px; }
.learn-ring svg { width: 150px; height: 150px; }
.ring-bg { fill: none; stroke: var(--bg-soft); stroke-width: 10; }
.ring-fg { fill: none; stroke: url(#g); stroke: var(--brand-500); stroke-width: 10; stroke-linecap: round;
  transform: rotate(-90deg); transform-origin: center; transition: stroke-dasharray .8s var(--ease-phi); }
.ring-num { font-size: 26px; font-weight: 780; fill: var(--ink-900); }
.ring-txt { font-size: 11px; fill: var(--text-3); }
.learn-fact { display: flex; justify-content: space-between; padding: 9px 2px; border-bottom: 1px dashed var(--line); font-size: 14px; color: var(--text-2); }
.learn-fact b { color: var(--text-1); }
@media (max-width: 1100px){ .subj-grid{grid-template-columns:repeat(2,1fr)} .hero{grid-template-columns:1fr} }
</style>
