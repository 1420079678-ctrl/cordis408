<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
const props = defineProps<{ topic: string }>()

/* ===== 二叉树遍历 ===== */
const TREE = {
  A: { x: 300, y: 56, l: 'B', r: 'C' }, B: { x: 170, y: 150, l: 'D', r: 'E' },
  C: { x: 430, y: 150, l: 'F', r: 'G' }, D: { x: 96, y: 244 }, E: { x: 244, y: 244 },
  F: { x: 356, y: 244 }, G: { x: 504, y: 244 }
}
const EDGES: [string, string][] = [['A','B'],['A','C'],['B','D'],['B','E'],['C','F'],['C','G']]
const ORDERS: Record<string, string[]> = {
  pre: ['A','B','D','E','C','F','G'], ino: ['D','B','E','A','F','C','G'],
  post: ['D','E','B','F','G','C','A'], level: ['A','B','C','D','E','F','G']
}
const ordKind = ref('pre'); const ordStep = ref(-1)
const ordSeq = computed(() => ORDERS[ordKind.value].slice(0, ordStep.value + 1))
function playOrder() { ordStep.value = -1; const t = setInterval(() => { ordStep.value++; if (ordStep.value >= 6) clearInterval(t) }, 650); timer = t }
let timer: any = null
watch(() => props.topic, () => { ordStep.value = -1; resetSort() })

/* ===== 排序动画 ===== */
type Frame = { arr: number[]; i: number; j: number; mark: number[]; sorted: number[]; desc: string }
const algo = ref('bubble'); const frames = ref<Frame[]>([]); const fi = ref(0)
const INIT = [5, 3, 8, 1, 9, 4, 7, 2]
function genFrames(): Frame[] {
  const a = [...INIT]; const F: Frame[] = []; const sorted: number[] = []
  const push = (i: number, j: number, mark: number[], desc: string) =>
    F.push({ arr: [...a], i, j, mark: [...mark], sorted: [...sorted], desc })
  if (algo.value === 'bubble') {
    for (let p = a.length - 1; p > 0; p--) {
      for (let j = 0; j < p; j++) {
        push(-1, j, [j, j + 1], `比较 a[${j}]=${a[j]} 与 a[${j+1}]=${a[j+1]}`)
        if (a[j] > a[j + 1]) { [a[j], a[j + 1]] = [a[j + 1], a[j]]; push(-1, j, [j, j + 1], `${a[j+1]}>${a[j]} 不成立，交换`) }
      }
      sorted.unshift(p)
    }
    sorted.unshift(0)
  } else if (algo.value === 'select') {
    for (let i = 0; i < a.length - 1; i++) {
      let mn = i; push(i, -1, [mn], `第 ${i+1} 轮，假设最小下标=${mn}`)
      for (let j = i + 1; j < a.length; j++) { push(i, j, [mn, j], `比较 a[${j}]=${a[j]} 与当前最小 ${a[mn]}`); if (a[j] < a[mn]) { mn = j; push(i, j, [mn], `更新最小下标=${mn}(${a[mn]})`) } }
      if (mn !== i) { [a[i], a[mn]] = [a[mn], a[i]]; push(i, mn, [i, mn], `交换到位置 ${i}`) }
      sorted.push(i)
    }
    sorted.push(a.length - 1)
  } else {
    for (let i = 1; i < a.length; i++) {
      const x = a[i]; let j = i - 1; push(i, j, [i], `取出 a[${i}]=${x}，向前找插入位置`)
      while (j >= 0 && a[j] > x) { a[j + 1] = a[j]; push(i, j, [j, j + 1], `${a[j]}>${x}，后移`); j-- }
      a[j + 1] = x; push(i, j + 1, [j + 1], `插入 ${x} 到位置 ${j+1}`)
    }
    for (let k = 0; k < a.length; k++) sorted.push(k)
  }
  F.push({ arr: [...a], i: -1, j: -1, mark: [], sorted: a.map((_, k) => k), desc: '排序完成 ✓' })
  return F
}
function resetSort() { frames.value = genFrames(); fi.value = 0 }
function stepSort() { if (fi.value < frames.value.length - 1) fi.value++ }
let sortTimer: any = null
function playSort() { if (sortTimer) clearInterval(sortTimer); sortTimer = setInterval(() => { if (fi.value >= frames.value.length - 1) { clearInterval(sortTimer); return } fi.value++ }, 500) }
watch(algo, resetSort); resetSort()
onUnmounted(() => { clearInterval(timer); clearInterval(sortTimer) })
const curFrame = computed(() => frames.value[fi.value])
const barMax = 9
</script>

<template>
  <div class="viz-wrap">
    <!-- 单链表 -->
    <template v-if="topic==='linked-list'">
      <svg class="viz-svg" viewBox="0 0 760 250">
        <text class="lbl-sm" x="20" y="30">head 头指针</text>
        <line class="flow" x1="80" y1="60" x2="118" y2="60" marker-end="url(#ar)"/>
        <g v-for="(n,i) in [1,2,3,4]" :key="i">
          <rect class="box" :x="120+i*140" y="40" width="80" height="42" rx="8"/>
          <line :x1="120+i*140+52" y1="40" x2="120+i*140+52" y2="82" stroke="#c9c4b8"/>
          <text class="lbl" :x="120+i*140+26" y="66" text-anchor="middle">数据{{i}}</text>
          <text class="lbl-mono" :x="120+i*140+66" y="66" text-anchor="middle">next</text>
          <line v-if="i<4" class="flow" :x1="120+i*140+80" y1="61" :x2="118+(i+1)*140" y2="61" marker-end="url(#ar)"/>
        </g>
        <line class="flow" x1="680" y1="61" x2="720" y2="61" marker-end="url(#ar)"/>
        <text class="lbl" x="730" y="66">∅</text>
        <defs><marker id="ar" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
        <rect class="box-gold" x="120" y="140" width="500" height="86" rx="12"/>
        <text class="lbl-sm" x="140" y="168">• 单链表由结点（数据域 data + 指针域 next）链接，内存中不要求连续</text>
        <text class="lbl-sm" x="140" y="192">• 头插法 O(1)（新结点插在 head 后）；尾插需尾指针；查找第 i 个 O(n)</text>
        <text class="lbl-sm" x="140" y="214">• 插入/删除只需修改指针，无元素移动——相比顺序表的核心优势</text>
      </svg>
    </template>

    <!-- 栈与队列 -->
    <template v-else-if="topic==='stack-queue'">
      <svg class="viz-svg" viewBox="0 0 760 300">
        <text class="lbl" x="120" y="30" text-anchor="middle">栈 Stack（LIFO）</text>
        <rect class="box" x="60" y="46" width="130" height="200" rx="10"/>
        <g v-for="(v,i) in ['C','B','A']" :key="i">
          <rect :class="i===2?'box-ok':'box-soft'" x="68" :y="190-i*56" width="114" height="48" rx="7"/>
          <text class="lbl" x="125" :y="220-i*56" text-anchor="middle">{{ v }}</text>
        </g>
        <text class="lbl-sm" x="200" y="70">top ↑</text>
        <text class="lbl-sm" x="40" y="266">push/pop 都在栈顶，O(1)</text>
        <text class="lbl" x="470" y="30" text-anchor="middle">队列 Queue（FIFO）</text>
        <g v-for="(v,i) in ['A','B','C','D']" :key="i">
          <rect :class="i===0?'box-ok':'box-soft'" :x="330+i*80" y="110" width="72" height="60" rx="8"/>
          <text class="lbl" :x="366+i*80" y="147" text-anchor="middle">{{ v }}</text>
        </g>
        <line class="flow" x1="366" y1="190" x2="366" y2="172" marker-end="url(#ar2)"/>
        <line class="flow" x1="606" y1="190" x2="606" y2="172" marker-end="url(#ar2)"/>
        <text class="lbl-sm" x="366" y="210" text-anchor="middle">front 队头出</text>
        <text class="lbl-sm" x="606" y="210" text-anchor="middle">rear 队尾入</text>
        <defs><marker id="ar2" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
        <rect class="box-gold" x="300" y="240" width="420" height="46" rx="10"/>
        <text class="lbl-sm" x="318" y="268">循环队列：队空 front==rear；队满 (rear+1)%n==front（牺牲一格）</text>
      </svg>
    </template>

    <!-- 二叉树遍历 -->
    <template v-else-if="topic==='tree-traversal'">
      <div class="viz-controls">
        <button class="chip" :class="{on:ordKind==='pre'}" @click="ordKind='pre';ordStep=-1">前序 根左右</button>
        <button class="chip" :class="{on:ordKind==='ino'}" @click="ordKind='ino';ordStep=-1">中序 左根右</button>
        <button class="chip" :class="{on:ordKind==='post'}" @click="ordKind='post';ordStep=-1">后序 左右根</button>
        <button class="chip" :class="{on:ordKind==='level'}" @click="ordKind='level';ordStep=-1">层序 BFS</button>
        <button class="btn sm gold" @click="playOrder">▶ 播放访问顺序</button>
      </div>
      <svg class="viz-svg" viewBox="0 0 600 300">
        <line v-for="[a,b] in EDGES" :key="a+b" class="axis" :x1="TREE[a].x" :y1="TREE[a].y+18" :x2="TREE[b].x" :y2="TREE[b].y-18"/>
        <g v-for="(n,k) in TREE" :key="k">
          <circle :class="ordSeq.includes(k)?'box-ok':'box'" :r="22" :cx="n.x" :cy="n.y"/>
          <text class="lbl" :x="n.x" :y="n.y+5" text-anchor="middle">{{ k }}</text>
        </g>
      </svg>
      <div class="viz-status">访问序列：{{ ordSeq.join(' → ') || '（点击播放）' }}</div>
      <div class="viz-note"><b>记忆：</b>前/中/后序指「根」相对左右子树的访问位置；中序 + 前序（或后序）可唯一确定一棵二叉树。层序借助队列实现。</div>
    </template>

    <!-- 哈夫曼 -->
    <template v-else-if="topic==='huffman'">
      <svg class="viz-svg" viewBox="0 0 720 330">
        <!-- 最终树: 18 / a7,11 ; 11 / b5,6 ; 6 / c2,d4 -->
        <line class="axis" x1="360" y1="50" x2="180" y2="120"/><line class="axis" x1="360" y1="50" x2="540" y2="120"/>
        <line class="axis" x1="540" y1="140" x2="450" y2="210"/><line class="axis" x1="540" y1="140" x2="630" y2="210"/>
        <line class="axis" x1="630" y1="230" x2="585" y2="290"/><line class="axis" x1="630" y1="230" x2="675" y2="290"/>
        <text class="lbl-mono" x="258" y="92">0</text><text class="lbl-mono" x="455" y="92">1</text>
        <text class="lbl-mono" x="488" y="180">0</text><text class="lbl-mono" x="592" y="180">1</text>
        <circle class="box-gold" cx="360" cy="40" r="22"/><text class="lbl" x="360" y="45" text-anchor="middle">18</text>
        <circle class="box-ok" cx="180" cy="130" r="22"/><text class="lbl" x="180" y="135" text-anchor="middle">a:7</text>
        <circle class="box-gold" cx="540" cy="130" r="22"/><text class="lbl" x="540" y="135" text-anchor="middle">11</text>
        <circle class="box-ok" cx="450" cy="220" r="22"/><text class="lbl" x="450" y="225" text-anchor="middle">b:5</text>
        <circle class="box-gold" cx="630" cy="220" r="22"/><text class="lbl" x="630" y="225" text-anchor="middle">6</text>
        <circle class="box-ok" cx="585" cy="300" r="20"/><text class="lbl" x="585" y="305" text-anchor="middle">c:2</text>
        <circle class="box-ok" cx="675" cy="300" r="20"/><text class="lbl" x="675" y="305" text-anchor="middle">d:4</text>
        <rect class="box-gold" x="20" y="60" width="140" height="150" rx="12"/>
        <text class="lbl-sm" x="36" y="90">构造（每次取两个最小权）</text>
        <text class="lbl-mono" x="36" y="118">① c2+d4 = 6</text>
        <text class="lbl-mono" x="36" y="144">② b5+6 = 11</text>
        <text class="lbl-mono" x="36" y="170">③ a7+11 = 18</text>
        <text class="lbl-sm" x="36" y="200">编码 a:0 b:10</text>
      </svg>
      <div class="viz-note"><b>WPL</b> = 7×1 + 5×2 + (2+4)×3 = 7+10+18 = <b>35</b>，也等于所有非叶结点权值之和 6+11+18=35。
        哈夫曼树无度为 1 的结点，n 个叶子共 2n−1 个结点；编码是<b>前缀码</b>，频率越高编码越短，最小 WPL 唯一但树形不唯一。</div>
    </template>

    <!-- BST / AVL -->
    <template v-else-if="topic==='bst-avl'">
      <svg class="viz-svg" viewBox="0 0 760 340">
        <text class="lbl" x="180" y="28" text-anchor="middle">LL 失衡 → 右旋</text>
        <circle class="box-hot" cx="120" cy="70" r="22"/><text class="lbl" x="120" y="75" text-anchor="middle">A</text>
        <circle class="box-soft" cx="60" cy="140" r="22"/><text class="lbl" x="60" y="145" text-anchor="middle">B</text>
        <circle cx="30" cy="210" r="18" class="box"/><text class="lbl-sm" x="30" y="214" text-anchor="middle">+</text>
        <circle cx="90" cy="210" r="18" class="box"/><text class="lbl-sm" x="90" y="214" text-anchor="middle">*</text>
        <circle cx="180" cy="140" r="18" class="box"/><text class="lbl-sm" x="180" y="144" text-anchor="middle">R</text>
        <line class="axis" x1="110" y1="90" x2="70" y2="120"/><line class="axis" x1="112" y1="92" x2="40" y2="194"/><line class="axis" x1="108" y1="92" x2="84" y2="192"/><line class="axis" x1="130" y1="90" x2="172" y2="122"/>
        <text class="lbl-mono" x="250" y="140">⇒ 右旋 ⇒</text>
        <circle class="box-ok" cx="430" cy="70" r="22"/><text class="lbl" x="430" y="75" text-anchor="middle">B</text>
        <circle class="box-ok" cx="370" cy="140" r="20"/><text class="lbl" x="370" y="145" text-anchor="middle">+</text>
        <circle class="box-ok" cx="490" cy="140" r="22"/><text class="lbl" x="490" y="145" text-anchor="middle">A</text>
        <circle cx="460" cy="210" r="18" class="box"/><text class="lbl-sm" x="460" y="214" text-anchor="middle">*</text>
        <circle cx="520" cy="210" r="18" class="box"/><text class="lbl-sm" x="520" y="214" text-anchor="middle">R</text>
        <line class="axis" x1="420" y1="90" x2="380" y2="122"/><line class="axis" x1="440" y1="90" x2="480" y2="120"/><line class="axis" x1="482" y1="160" x2="466" y2="192"/><line class="axis" x1="498" y1="160" x2="514" y2="192"/>
        <rect class="box-gold" x="560" y="40" width="180" height="220" rx="12"/>
        <text class="lbl-sm" x="578" y="72">四种旋转</text>
        <text class="lbl-mono" x="578" y="100">LL：单右旋</text>
        <text class="lbl-mono" x="578" y="126">RR：单左旋</text>
        <text class="lbl-mono" x="578" y="152">LR：先左后右</text>
        <text class="lbl-mono" x="578" y="178">RL：先右后左</text>
        <text class="lbl-sm" x="578" y="210">平衡因子=左高−右高</text>
        <text class="lbl-sm" x="578" y="232">|BF|≤1 才平衡</text>
      </svg>
      <div class="viz-note"><b>BST</b> 中序遍历得到递增序列；查找/插入平均 O(log n)，退化成链时 O(n)。<b>AVL</b> 靠旋转保证任意结点左右子树高度差不超过 1，从而严格 O(log n)。红黑树放宽约束以减少旋转次数。</div>
    </template>

    <!-- 图 Dijkstra -->
    <template v-else-if="topic==='graph'">
      <svg class="viz-svg" viewBox="0 0 760 320">
        <line class="axis" x1="120" y1="120" x2="260" y2="70"/><text class="lbl-mono" x="180" y="84">4</text>
        <line class="axis" x1="120" y1="120" x2="250" y2="200"/><text class="lbl-mono" x="160" y="180">2</text>
        <line class="axis" x1="260" y1="70" x2="430" y2="110"/><text class="lbl-mono" x="350" y="78">3</text>
        <line class="axis" x1="250" y1="200" x2="430" y2="110"/><text class="lbl-mono" x="330" y="172">5</text>
        <line class="axis" x1="250" y1="200" x2="420" y2="240"/><text class="lbl-mono" x="330" y="238">1</text>
        <line class="axis" x1="430" y1="110" x2="600" y2="180"/><text class="lbl-mono" x="530" y="132">2</text>
        <line class="axis" x1="420" y1="240" x2="600" y2="180"/><text class="lbl-mono" x="520" y="232">6</text>
        <g v-for="(p,k) in {v0:[120,120],v1:[260,70],v2:[250,200],v3:[430,110],v4:[420,240],v5:[600,180]}" :key="k">
          <circle :class="k==='v0'?'box-ok':'box'" :cx="p[0]" :cy="p[1]" r="20"/><text class="lbl" :x="p[0]" :y="p[1]+5" text-anchor="middle">{{ k.slice(1) }}</text>
        </g>
        <rect class="box-gold" x="40" y="270" width="680" height="40" rx="9"/>
        <text class="lbl-mono" x="58" y="295">从 v0 最短距离 dist：v1=4  v2=2  v3=min(4+3,2+5)=7  v4=2+1=3  v5=min(7+2,3+6)=9</text>
      </svg>
      <div class="viz-note"><b>Dijkstra 贪心：</b>每步在「未确定集合」中选 dist 最小者永久确定，再用它松弛邻边。要求<b>边权非负</b>（负权需 Bellman-Ford）；用最小堆优化到 O((V+E)log V)。</div>
    </template>

    <!-- 排序动画 -->
    <template v-else>
      <div class="viz-controls">
        <select v-model="algo">
          <option value="bubble">冒泡排序</option><option value="select">简单选择排序</option><option value="insert">直接插入排序</option>
        </select>
        <button class="btn sm" @click="stepSort">单步 →</button>
        <button class="btn sm gold" @click="playSort">▶ 播放</button>
        <button class="btn ghost sm" @click="resetSort">重置</button>
        <span class="viz-status">{{ fi }}/{{ frames.length-1 }}</span>
      </div>
      <svg class="viz-svg" viewBox="0 0 720 280">
        <g v-for="(v,idx) in curFrame.arr" :key="idx">
          <rect :rx="7" :x="40+idx*80" :y="230-v/barMax*180" width="56" :height="v/barMax*180"
            :fill="curFrame.sorted.includes(idx)?'#22986a':curFrame.mark.includes(idx)?'#d0453b':'#1b7f92'" opacity="0.92"/>
          <text class="lbl" :x="68+idx*80" :y="222-v/barMax*180" text-anchor="middle">{{ v }}</text>
          <text class="lbl-mono" :x="68+idx*80" y="248" text-anchor="middle">[{{ idx }}]</text>
        </g>
      </svg>
      <div class="viz-status">{{ curFrame.desc }}</div>
      <div class="viz-legend"><span><i style="background:#1b7f92;border-color:#1b7f92"></i>未排序</span><span><i style="background:#d0453b;border-color:#d0453b"></i>当前比较/操作</span><span><i style="background:#22986a;border-color:#22986a"></i>已有序</span></div>
      <div class="viz-note">三种均为<b>内排序、稳定与否</b>：冒泡/插入稳定，选择不稳定；时间复杂度三者平均均 O(n²)，插入在基本有序时接近 O(n)。408 还常考快排 O(nlogn) 不稳定、堆排序 O(nlogn) 不稳定、归并 O(nlogn) 稳定。</div>
    </template>
  </div>
</template>
