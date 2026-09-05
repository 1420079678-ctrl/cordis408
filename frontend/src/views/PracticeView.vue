<script setup lang="ts">
import { ref, computed } from 'vue'
import { api, SUBJECT_META } from '../api'
import Md from '../components/Md.vue'
const order = ['ds', 'co', 'cn', 'os']
const subject = ref('ds')
const difficulty = ref<number | ''>('')
const n = ref(5)
const stage = ref<'setup' | 'doing' | 'done'>('setup')
const papers = ref<any[]>([])
const idx = ref(0)
const picks = ref<Record<string, string>>({})
const graded = ref<Record<string, any>>({})
const busy = ref(false)
const cur = computed(() => papers.value[idx.value])
const score = computed(() => {
  const list = Object.values(graded.value); const right = list.filter((g: any) => g.correct).length
  return { right, total: list.length }
})
async function start() {
  busy.value = true
  let qs = `subject=${subject.value}&n=${n.value}`
  if (difficulty.value !== '') qs += `&difficulty=${difficulty.value}`
  papers.value = await api.quiz(qs)
  picks.value = {}; graded.value = {}; idx.value = 0
  stage.value = papers.value.length ? 'doing' : 'setup'; busy.value = false
}
async function submit() {
  if (!cur.value || busy.value) return
  busy.value = true
  const ans = picks.value[cur.value.id] || ''
  const g = await api.grade({ sessionId: 'default', quizId: cur.value.id, answer: ans })
  graded.value[cur.value.id] = g
  busy.value = false
}
function next() { if (idx.value < papers.value.length - 1) idx.value++; else stage.value = 'done' }
function restart() { stage.value = 'setup'; papers.value = [] }
function optClass(key: string) {
  const g = graded.value[cur.value?.id]
  if (!g) return picks.value[cur.value.id] === key ? 'picked' : ''
  if (key === g.rightAnswer) return 'right'
  if (key === picks.value[cur.value.id] && !g.correct) return 'wrong'
  return ''
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">智能出题练习</div>
      <div class="page-desc">Problem Agent 按学科与难度抽题，作答后 Diagnosis Agent 批改并更新学情画像</div>
    </div>

    <!-- 配置 -->
    <section v-if="stage==='setup'" class="card card-pad setup">
      <div class="cfg-block">
        <div class="cfg-lbl">选择学科</div>
        <div class="subj-pick">
          <button v-for="k in order" :key="k" class="sp" :class="{on:subject===k}"
            :style="subject===k?{background:SUBJECT_META[k].color,borderColor:SUBJECT_META[k].color}:{}" @click="subject=k">
            {{ SUBJECT_META[k].name }}</button>
        </div>
      </div>
      <div class="cfg-block">
        <div class="cfg-lbl">难度（1 基础 ~ 5 拔高，不限则混合）</div>
        <div class="row" style="gap:8px">
          <button class="chip" :class="{on:difficulty===''}" @click="difficulty=''">混合</button>
          <button v-for="d in [1,2,3,4,5]" :key="d" class="chip" :class="{on:difficulty===d}" @click="difficulty=d">{{ d }} 星</button>
        </div>
      </div>
      <div class="cfg-block">
        <div class="cfg-lbl">题量</div>
        <div class="row" style="gap:8px">
          <button v-for="x in [3,5,8,10]" :key="x" class="chip" :class="{on:n===x}" @click="n=x">{{ x }} 题</button>
        </div>
      </div>
      <button class="btn gold" style="margin-top:8px" :disabled="busy" @click="start">生成练习卷 →</button>
    </section>

    <!-- 作答 -->
    <section v-else-if="stage==='doing' && cur" class="card quiz-card">
      <header class="q-head">
        <span class="tag" :style="{background:SUBJECT_META[cur.subject]?.soft,color:SUBJECT_META[cur.subject]?.color}">{{ cur.chapter }}</span>
        <span class="tiny muted">第 {{ idx+1 }} / {{ papers.length }} 题 · 难度 {{ '★'.repeat(cur.difficulty) }}</span>
      </header>
      <div class="bar q-progress"><i :style="{width:((idx)/papers.length*100)+'%'}"></i></div>
      <div class="q-stem">{{ idx+1 }}. {{ cur.stem }}</div>
      <div class="opts">
        <button v-for="(txt,key) in cur.options" :key="key" class="opt" :class="optClass(key)"
          :disabled="!!graded[cur.id]" @click="picks[cur.id]=key">
          <span class="opt-key">{{ key }}</span><span class="opt-txt">{{ txt }}</span>
          <span v-if="graded[cur.id] && key===graded[cur.id].rightAnswer" class="opt-mark">✓</span>
          <span v-else-if="graded[cur.id] && key===picks[cur.id] && !graded[cur.id].correct" class="opt-mark x">✕</span>
        </button>
      </div>
      <div v-if="graded[cur.id]" class="analysis">
        <div class="an-verdict" :class="graded[cur.id].correct?'ok':'no'">
          {{ graded[cur.id].correct ? '回答正确' : '回答错误' }} · 正确答案 {{ graded[cur.id].rightAnswer }}
        </div>
        <Md :text="'**解析：**' + graded[cur.id].analysis" />
      </div>
      <footer class="q-foot">
        <button v-if="!graded[cur.id]" class="btn" :disabled="!picks[cur.id]||busy" @click="submit">提交批改</button>
        <button v-else class="btn gold" @click="next">{{ idx<papers.length-1?'下一题':'查看成绩' }} →</button>
      </footer>
    </section>

    <!-- 成绩 -->
    <section v-else class="card card-pad result">
      <div class="res-ring">
        <svg viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="50" class="rbg"/>
          <circle cx="60" cy="60" r="50" class="rfg" :stroke-dasharray="`${score.right/score.total*314} 314`"/>
          <text x="60" y="64" text-anchor="middle" class="rnum">{{ score.right }}/{{ score.total }}</text>
        </svg>
      </div>
      <h2>本次正确率 {{ Math.round(score.right/score.total*100) }}%</h2>
      <p class="muted">答题结果已同步至学情画像，Diagnosis Agent 会据此识别你的薄弱考点。</p>
      <div class="row" style="justify-content:center;gap:10px;margin-top:16px">
        <button class="btn gold" @click="start">再来一组</button>
        <button class="btn ghost" @click="restart">重新配置</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.setup { display: flex; flex-direction: column; gap: 22px; max-width: 720px; }
.cfg-lbl { font-size: 13px; font-weight: 650; color: var(--text-2); margin-bottom: 10px; }
.subj-pick { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; }
.sp { padding: 13px; border-radius: var(--r-md); border:1px solid var(--line-strong); background:var(--surface); font-weight:650;font-size:14px }
.sp.on { color:#fff }
.quiz-card { padding: 26px 28px; max-width: 820px; }
.q-head { display:flex;justify-content:space-between;align-items:center;margin-bottom:12px }
.q-progress { margin-bottom: 20px; }
.q-stem { font-size: 16.5px; font-weight: 650; line-height: 1.7; margin-bottom: 18px; }
.opts { display: flex; flex-direction: column; gap: 10px; }
.opt { display:flex;align-items:center;gap:13px;text-align:left;padding:14px 16px;border:1.5px solid var(--line);border-radius:var(--r-md);background:var(--surface-2);transition:all .16s }
.opt:hover:not(:disabled){border-color:var(--brand-400);background:var(--brand-050)}
.opt-key{width:30px;height:30px;border-radius:9px;background:var(--bg-soft);display:grid;place-items:center;font-weight:700;flex:none;font-family:var(--font-mono)}
.opt-txt{flex:1;font-size:14.5px;line-height:1.55}
.opt-mark{font-weight:800;color:var(--st-active);font-size:18px}
.opt-mark.x{color:var(--st-failed)}
.opt.picked{border-color:var(--brand-500);background:var(--brand-050)}
.opt.picked .opt-key{background:var(--brand-500);color:#fff}
.opt.right{border-color:var(--st-active);background:var(--st-active-soft)}
.opt.right .opt-key{background:var(--st-active);color:#fff}
.opt.wrong{border-color:var(--st-failed);background:var(--st-failed-soft)}
.opt.wrong .opt-key{background:var(--st-failed);color:#fff}
.analysis{margin-top:18px;padding:16px 18px;background:var(--surface-2);border-radius:var(--r-md);border:1px solid var(--line)}
.an-verdict{font-weight:750;margin-bottom:8px;font-size:14.5px}
.an-verdict.ok{color:var(--st-active)}.an-verdict.no{color:var(--st-failed)}
.q-foot{margin-top:20px;display:flex;justify-content:flex-end}
.result{text-align:center;padding:44px;max-width:640px;margin:0 auto}
.res-ring svg{width:160px;height:160px}
.rbg{fill:none;stroke:var(--bg-soft);stroke-width:11}
.rfg{fill:none;stroke:var(--brand-500);stroke-width:11;stroke-linecap:round;transform:rotate(-90deg);transform-origin:center;transition:stroke-dasharray .8s var(--ease-phi)}
.rnum{font-size:24px;font-weight:780;fill:var(--ink-900)}
.result h2{margin:14px 0 6px}
</style>
