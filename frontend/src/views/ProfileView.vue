<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, SUBJECT_META } from '../api'
const router = useRouter()
const learner = ref<any>({ subjects: {}, accuracy: 0 })
const report = ref<any>({ weakSubjects: [], weakPoints: [] })
const order = ['ds', 'co', 'cn', 'os']
async function load() {
  learner.value = await api.learner('default')
  report.value = await api.diagnosis('default')
}
onMounted(load)
async function reset() {
  await api.resetLearner('default'); await load()
}
function level(a: number) { return a >= 80 ? '熟练' : a >= 60 ? '掌握' : a >= 30 ? '入门' : '待学习' }
</script>

<template>
  <div class="page">
    <div class="page-head row" style="justify-content:space-between;align-items:flex-end">
      <div>
        <div class="page-title">学情诊断画像</div>
        <div class="page-desc">Learner Model Agent 实时维护，Diagnosis Agent 定位薄弱考点</div>
      </div>
      <button class="btn ghost sm" @click="reset">清空学习记录</button>
    </div>

    <div class="top-row">
      <div class="card card-pad stat-card">
        <div class="stat-num">{{ learner.accuracy || 0 }}<i>%</i></div>
        <div class="stat-lbl">综合正确率</div>
      </div>
      <div class="card card-pad stat-card">
        <div class="stat-num">{{ learner.totalAsked || 0 }}</div>
        <div class="stat-lbl">累计提问</div>
      </div>
      <div class="card card-pad stat-card">
        <div class="stat-num">{{ learner.totalAnswered || 0 }}</div>
        <div class="stat-lbl">累计答题</div>
      </div>
      <div class="card card-pad stat-card">
        <div class="stat-num">{{ report.weakPoints?.length || 0 }}</div>
        <div class="stat-lbl">待攻克薄弱点</div>
      </div>
    </div>

    <div class="phi-split" style="margin-top:var(--u4);align-items:start">
      <section class="card card-pad">
        <h3 class="blk-title">四学科掌握度</h3>
        <div v-for="k in order" :key="k" class="master-row">
          <div class="row" style="width:120px;flex:none">
            <span class="m-dot" :style="{background:SUBJECT_META[k].color}"></span>
            <b>{{ SUBJECT_META[k].name }}</b>
          </div>
          <div class="bar" style="flex:1"><i :style="{width:(learner.subjects[k]?.accuracy||0)+'%',background:SUBJECT_META[k].color}"></i></div>
          <span class="m-pct">{{ learner.subjects[k]?.accuracy || 0 }}%</span>
          <span class="m-lvl">{{ level(learner.subjects[k]?.accuracy || 0) }}</span>
        </div>
        <div class="hint">掌握度 = 该学科答对题数 / 已答题数；多做练习后这里会实时更新。</div>

        <h3 class="blk-title" style="margin-top:26px">最近学习主题</h3>
        <div v-if="learner.recentTopics?.length" class="topics">
          <span v-for="(t,i) in learner.recentTopics" :key="i" class="topic-tag">{{ t }}</span>
        </div>
        <div v-else class="muted tiny">还没有学习记录，去 AI 答疑或智能练习开始吧。</div>
      </section>

      <section class="card card-pad">
        <h3 class="blk-title">薄弱考点诊断</h3>
        <div v-if="report.weakPoints?.length" class="weak-list">
          <div v-for="w in report.weakPoints" :key="w.id" class="weak-item">
            <span class="w-sub">{{ w.subject }}</span>
            <div class="w-title">{{ w.title }}</div>
            <div class="tiny muted">{{ w.summary }}</div>
          </div>
        </div>
        <div v-else class="empty-weak">
          <div class="ew-ico">✓</div>
          <p>当前没有明显薄弱点。<br>去练习中检验自己，错题会自动归集到这里。</p>
          <button class="btn gold sm" @click="router.push('/practice')">去练习</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.top-row { display:grid;grid-template-columns:repeat(4,1fr);gap:var(--u4) }
.stat-card { text-align:center;padding:24px }
.stat-num { font-size:34px;font-weight:780;color:var(--ink-800);line-height:1 }
.stat-num i{font-size:17px;font-style:normal;color:var(--text-3);font-weight:600}
.stat-lbl { color:var(--text-2);font-size:13px;margin-top:8px }
.blk-title { font-size:15.5px;font-weight:720;margin-bottom:16px }
.master-row { display:flex;align-items:center;gap:14px;margin-bottom:16px }
.m-dot{width:10px;height:10px;border-radius:3px}
.m-pct{width:46px;text-align:right;font-weight:700;font-family:var(--font-mono);font-size:14px}
.m-lvl{width:54px;text-align:center;font-size:11.5px;color:var(--text-2);background:var(--bg-soft);border-radius:99px;padding:2px 0}
.hint{font-size:12px;color:var(--text-3);margin-top:6px;line-height:1.6}
.topics{display:flex;flex-wrap:wrap;gap:8px}
.topic-tag{font-size:12.5px;background:var(--surface-2);border:1px solid var(--line);padding:5px 12px;border-radius:99px;color:var(--text-2)}
.weak-list{display:flex;flex-direction:column;gap:12px}
.weak-item{padding:12px 14px;border:1px solid var(--line);border-left:3px solid var(--st-failed);border-radius:10px;background:var(--surface-2)}
.w-sub{font-size:11px;font-weight:700;color:var(--st-failed)}
.w-title{font-weight:680;font-size:14px;margin:4px 0}
.empty-weak{text-align:center;padding:26px 10px;color:var(--text-2)}
.ew-ico{width:50px;height:50px;border-radius:50%;background:var(--st-active-soft);color:var(--st-active);display:grid;place-items:center;font-size:24px;font-weight:800;margin:0 auto 14px}
.empty-weak p{font-size:13px;line-height:1.7;margin-bottom:14px}
@media (max-width:1000px){.top-row{grid-template-columns:repeat(2,1fr)}}
</style>
