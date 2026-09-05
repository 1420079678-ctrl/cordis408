<script setup lang="ts">
import { ref, computed, watch } from 'vue'
defineProps<{ topic: string }>()
/* 页面置换 */
const refStr = [7, 0, 1, 2, 0, 3, 0, 4, 2, 3]
const FRAMES = 3
const replAlg = ref('LRU'); const rStep = ref(0)
function simulate(alg: string) {
  const frames: number[] = []; const order: number[] = []; const lastUse: Record<number, number> = {}
  return refStr.map((p, t) => {
    let miss = false
    if (!frames.includes(p)) {
      miss = true
      if (frames.length < FRAMES) { frames.push(p); order.push(p) }
      else {
        let victim
        if (alg === 'FIFO') victim = order.shift()
        else victim = [...frames].sort((a, b) => lastUse[a] - lastUse[b])[0]
        const k = frames.indexOf(victim); frames[k] = p
        if (alg === 'FIFO') order.push(p)
      }
    }
    lastUse[p] = t
    return { page: p, frames: [...frames], miss }
  })
}
const replHistory = computed(() => simulate(replAlg.value).slice(0, rStep.value + 1))
const missCount = computed(() => replHistory.value.filter(h => h.miss).length)
watch(replAlg, () => (rStep.value = 0))
/* 磁盘调度 */
const diskAlg = ref('SSTF')
const start = 100
const reqList = [55, 58, 39, 18, 90, 160, 150, 38, 184]
const diskSeq = computed(() => {
  if (diskAlg.value === 'FCFS') return [start, ...reqList]
  if (diskAlg.value === 'SSTF') {
    const pend = [...reqList], seq = [start]; let cur = start
    while (pend.length) { pend.sort((a, b) => Math.abs(a - cur) - Math.abs(b - cur)); cur = pend.shift()!; seq.push(cur) }
    return seq
  }
  // SCAN（LOOK，向磁道增大方向）
  const up = reqList.filter(x => x >= start).sort((a, b) => a - b)
  const down = reqList.filter(x => x < start).sort((a, b) => b - a)
  return [start, ...up, ...down]
})
const diskMove = computed(() => diskSeq.value.reduce((a, v, i) => i ? a + Math.abs(v - diskSeq.value[i - 1]) : a, 0))
</script>

<template>
  <div class="viz-wrap">
    <!-- 进程状态 -->
    <template v-if="topic==='proc-state'">
      <svg class="viz-svg" viewBox="0 0 760 320">
        <rect class="box" x="40" y="130" width="110" height="56" rx="10"/><text class="lbl" x="95" y="164" text-anchor="middle">新建</text>
        <rect class="box-soft" x="230" y="130" width="110" height="56" rx="10"/><text class="lbl" x="285" y="158" text-anchor="middle">就绪</text><text class="lbl-sm" x="285" y="178" text-anchor="middle">Ready</text>
        <rect class="box-ok" x="420" y="130" width="110" height="56" rx="10"/><text class="lbl" x="475" y="158" text-anchor="middle">运行</text><text class="lbl-sm" x="475" y="178" text-anchor="middle">Running</text>
        <rect class="box-gold" x="420" y="240" width="110" height="56" rx="10"/><text class="lbl" x="475" y="268" text-anchor="middle">阻塞/等待</text>
        <rect class="box" x="610" y="130" width="110" height="56" rx="10"/><text class="lbl" x="665" y="164" text-anchor="middle">终止</text>
        <line class="flow" x1="150" y1="158" x2="228" y2="158" marker-end="url(#ps)"/><text class="lbl-mono" x="166" y="148">创建</text>
        <line class="flow" x1="340" y1="158" x2="418" y2="158" marker-end="url(#ps)"/><text class="lbl-mono" x="352" y="148">调度</text>
        <line class="flow" x1="420" y1="146" x2="342" y2="146" marker-end="url(#ps)"/><text class="lbl-mono" x="352" y="130">时间片到</text>
        <line class="flow" x1="475" y1="186" x2="475" y2="238" marker-end="url(#ps)"/><text class="lbl-mono" x="484" y="216">I/O请求/等待事件</text>
        <line class="flow" x1="440" y1="240" x2="300" y2="188" marker-end="url(#ps)"/><text class="lbl-mono" x="320" y="232">I/O完成/事件到</text>
        <line class="flow" x1="530" y1="158" x2="608" y2="158" marker-end="url(#ps)"/><text class="lbl-mono" x="548" y="148">退出</text>
        <defs><marker id="ps" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note"><b>关键：</b>运行→阻塞是<b>主动</b>行为（进程自己请求 I/O）；阻塞→就绪是<b>被动</b>唤醒（I/O 完成后只能回到就绪，不能直接运行）。就绪↔运行由进程调度切换，阻塞态不占 CPU。</div>
    </template>

    <!-- 分页地址翻译 -->
    <template v-else-if="topic==='address-translate'">
      <svg class="viz-svg" viewBox="0 0 760 330">
        <rect class="box-gold" x="40" y="50" width="200" height="70" rx="10"/><text class="lbl" x="140" y="80" text-anchor="middle">逻辑地址</text><text class="lbl-mono" x="140" y="104" text-anchor="middle">页号 P | 页内偏移 W</text>
        <line class="flow" x1="150" y1="120" x2="150" y2="170"/><text class="lbl-sm" x="160" y="150">页号查页表</text>
        <rect class="box" x="40" y="170" width="200" height="120" rx="10"/>
        <text class="lbl" x="140" y="198" text-anchor="middle">页表（每进程一张）</text>
        <line x1="60" y1="212" x2="220" y2="212" stroke="#ddd8cc"/>
        <text class="lbl-mono" x="70" y="234">页号0 → 块号5</text>
        <text class="lbl-mono" x="70" y="258">页号1 → 块号2 ✓</text>
        <text class="lbl-mono" x="70" y="282">页号2 → 块号8</text>
        <line class="flow" x1="240" y1="230" x2="420" y2="150" marker-end="url(#at)"/>
        <rect class="box-ok" x="420" y="110" width="280" height="80" rx="10"/><text class="lbl" x="560" y="142" text-anchor="middle">物理地址</text><text class="lbl-mono" x="560" y="168" text-anchor="middle">物理块号(基址) + 页内偏移 W</text>
        <line class="flow-dash" x1="240" y1="85" x2="420" y2="140"/>
        <text class="lbl-sm" x="300" y="96">偏移 W 原样拼接（页大小=块大小）</text>
        <rect class="box-gold" x="420" y="220" width="280" height="80" rx="10"/>
        <text class="lbl-sm" x="440" y="248">例：页面 4KB(=2¹²)，逻辑地址 8195</text>
        <text class="lbl-mono" x="440" y="274">P=8195÷4096=2，W=8195%4096=3</text>
        <text class="lbl-mono" x="440" y="294">块号8 → 物理=8×4096+3=32771</text>
        <defs><marker id="at" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note"><b>分页</b>对用户透明、消除外碎片（有少量内碎片）；<b>多级页表/TLB（快表）</b>减少访存次数——命中 TLB 一次访存，未命中需先查页表。逻辑地址位数 = 页号位数 + 页内偏移位数。</div>
    </template>

    <!-- 页面置换动画 -->
    <template v-else-if="topic==='lru'">
      <div class="viz-controls">
        <button class="chip" :class="{on:replAlg==='FIFO'}" @click="replAlg='FIFO'">FIFO 先进先出</button>
        <button class="chip" :class="{on:replAlg==='LRU'}" @click="replAlg='LRU'">LRU 最近最久未用</button>
        <button class="btn sm" @click="rStep=Math.min(rStep+1,refStr.length-1)">访问下一步 →</button>
        <button class="btn ghost sm" @click="rStep=0">重置</button>
        <span class="viz-status">缺页 {{ missCount }} 次 / 已访问 {{ replHistory.length }}</span>
      </div>
      <div class="refstr">访问串：
        <span v-for="(p,i) in refStr" :key="i" class="ref-p" :class="{now:i===rStep,done:i<rStep}">{{ p }}</span>
      </div>
      <svg class="viz-svg" viewBox="0 0 760 220">
        <g v-for="(h,i) in replHistory" :key="i">
          <text class="lbl-mono" :x="120+i*60" y="40" text-anchor="middle">{{ h.page }}{{ h.miss?'*':'' }}</text>
          <g v-for="(f,k) in h.frames" :key="k">
            <rect :class="h.miss&&k===h.frames.length-1?'box-hot':'box-soft'"
              :x="100+i*60" :y="60+k*48" width="40" height="40" rx="7"/>
            <text class="lbl" :x="120+i*60" :y="86+k*48" text-anchor="middle">{{ f }}</text>
          </g>
        </g>
      </svg>
      <div class="viz-note">* 为<b>缺页</b>。FIFO 淘汰最早进入内存的页，可能出现 Belady 异常（块数增多缺页反增）；<b>LRU</b> 淘汰最久未被访问的页，基于局部性，效果更好且无 Belady 异常，但需寄存器/栈支持。OPT 最优但无法实现，仅作理论下界。</div>
    </template>

    <!-- 进程同步 -->
    <template v-else-if="topic==='sync'">
      <svg class="viz-svg" viewBox="0 0 760 300">
        <rect class="box-soft" x="40" y="60" width="140" height="180" rx="12"/><text class="lbl" x="110" y="92" text-anchor="middle">生产者</text><text class="lbl-mono" x="110" y="130">P(empty)</text><text class="lbl-mono" x="110" y="158">P(mutex)</text><text class="lbl-mono" x="110" y="186">放产品 V(mutex)</text><text class="lbl-mono" x="110" y="214">V(full)</text>
        <g v-for="i in 5" :key="i"><rect :class="i<=3?'box-gold':'box'" x="260" :y="70+(i-1)*34" width="200" height="28" rx="6"/><text class="lbl-sm" x="360" :y="89+(i-1)*34" text-anchor="middle">{{ i<=3?'缓冲区产品 '+i:'空槽位' }}</text></g>
        <rect class="box-soft" x="540" y="60" width="160" height="180" rx="12"/><text class="lbl" x="620" y="92" text-anchor="middle">消费者</text><text class="lbl-mono" x="620" y="130">P(full)</text><text class="lbl-mono" x="620" y="158">P(mutex)</text><text class="lbl-mono" x="620" y="186">取产品 V(mutex)</text><text class="lbl-mono" x="620" y="214">V(empty)</text>
        <line class="flow" x1="180" y1="150" x2="258" y2="150" marker-end="url(#sy)"/><line class="flow" x1="460" y1="150" x2="538" y2="150" marker-end="url(#sy)"/>
        <defs><marker id="sy" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note"><b>信号量 P(wait)/V(signal)</b>：P 使资源数 −1，<0 则阻塞；V 使资源数 +1，≤0 则唤醒一个等待者。三信号量：mutex=1（互斥访问缓冲）、empty=n（空槽）、full=0（产品）。<b>P 顺序不能颠倒</b>（先资源后互斥，否则死锁），V 顺序无所谓。</div>
    </template>

    <!-- 死锁 -->
    <template v-else-if="topic==='deadlock'">
      <svg class="viz-svg" viewBox="0 0 760 300">
        <g v-for="(c,i) in [['互斥','资源一次只被一个进程占用'],['请求并保持','占着已有资源又请求新资源'],['不剥夺','资源只能由持有者主动释放'],['循环等待','存在进程—资源的环形等待链']]" :key="i">
          <rect class="box-hot" :x="40+(i%2)*360" :y="50+Math.floor(i/2)*110" width="320" height="80" rx="12"/>
          <text class="lbl" :x="200+(i%2)*360" :y="84+Math.floor(i/2)*110" text-anchor="middle">必要条件 {{ i+1 }}：{{ c[0] }}</text>
          <text class="lbl-sm" :x="200+(i%2)*360" :y="112+Math.floor(i/2)*110" text-anchor="middle">{{ c[1] }}</text>
        </g>
      </svg>
      <div class="viz-note">四个必要条件<b>同时成立</b>才会死锁，破坏任一即可预防。处理策略：预防（破坏条件）、避免（<b>银行家算法</b>找安全序列）、检测与解除（资源分配图，剥夺/撤销/回滚）、鸵鸟策略。</div>
    </template>

    <!-- 磁盘调度 -->
    <template v-else>
      <div class="viz-controls">
        <button class="chip" :class="{on:diskAlg==='FCFS'}" @click="diskAlg='FCFS'">FCFS</button>
        <button class="chip" :class="{on:diskAlg==='SSTF'}" @click="diskAlg='SSTF'">SSTF 最短寻道</button>
        <button class="chip" :class="{on:diskAlg==='SCAN'}" @click="diskAlg='SCAN'">SCAN/LOOK 电梯</button>
        <span class="viz-status">访问顺序：{{ diskSeq.join(' → ') }}</span>
      </div>
      <svg class="viz-svg" viewBox="0 0 760 240">
        <line class="axis" x1="50" y1="180" x2="720" y2="180"/>
        <text class="lbl-sm" x="50" y="208">0</text><text class="lbl-sm" x="700" y="208" text-anchor="end">200 磁道</text>
        <polyline fill="none" stroke="var(--brand-500)" stroke-width="2.2"
          :points="diskSeq.map((d,i)=>`${60+d*3.2},${170-i*16}`).join(' ')"/>
        <g v-for="(d,i) in diskSeq" :key="i">
          <circle :cx="60+d*3.2" :cy="170-i*16" r="6" :fill="i===0?'var(--gold-500)':'var(--brand-500)'"/>
          <text class="lbl-mono" :x="60+d*3.2" :y="160-i*16" text-anchor="middle">{{ d }}</text>
        </g>
      </svg>
      <div class="viz-status">总移动磁道数 = {{ diskMove }}</div>
      <div class="viz-note"><b>FCFS</b> 公平但寻道远；<b>SSTF</b> 每次选最近，平均更短但可能让远处请求「饥饿」；<b>SCAN/LOOK</b> 像电梯沿一个方向走到尽头（LOOK 到最远请求即折返），兼顾公平与效率。</div>
    </template>
  </div>
</template>

<style scoped>
.refstr { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; margin-bottom: 14px; font-size: 13px; color: var(--text-2); }
.ref-p { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 8px; background: var(--surface-2);
  border: 1px solid var(--line); font-family: var(--font-mono); font-weight: 700; font-size: 13px; }
.ref-p.done { background: var(--brand-050); color: var(--brand-600); }
.ref-p.now { background: var(--st-failed); color: #fff; border-color: var(--st-failed); transform: scale(1.12); }
</style>
