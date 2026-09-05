<script setup lang="ts">
import { ref, computed } from 'vue'
defineProps<{ topic: string }>()
// 流水线指令条数
const nIns = ref(5)
const stages = ['IF', 'ID', 'EX', 'MEM', 'WB']
const cells = computed(() => {
  const out: any[] = []
  for (let i = 0; i < nIns.value; i++)
    stages.forEach((s, k) => out.push({ i, k, cycle: i + k, s }))
  return out
})
const totalCycle = computed(() => nIns.value + 4)
const speedup = computed(() => ((nIns.value * 5) / totalCycle.value).toFixed(2))
// Cache 映射切换
const mapKind = ref('direct')
</script>

<template>
  <div class="viz-wrap">
    <!-- 冯诺依曼 -->
    <template v-if="topic==='von-neumann'">
      <svg class="viz-svg" viewBox="0 0 760 360">
        <rect class="box-soft" x="250" y="30" width="260" height="120" rx="14"/>
        <text class="lbl" x="380" y="56" text-anchor="middle">主机</text>
        <rect class="box" x="270" y="70" width="110" height="62" rx="9"/><text class="lbl" x="325" y="98" text-anchor="middle">运算器</text><text class="lbl-sm" x="325" y="118" text-anchor="middle">ALU</text>
        <rect class="box" x="390" y="70" width="110" height="62" rx="9"/><text class="lbl" x="445" y="98" text-anchor="middle">控制器</text><text class="lbl-sm" x="445" y="118" text-anchor="middle">CU</text>
        <rect class="box-gold" x="270" y="180" width="230" height="60" rx="9"/><text class="lbl" x="385" y="208" text-anchor="middle">存储器</text><text class="lbl-sm" x="385" y="228" text-anchor="middle">主存（内存）</text>
        <rect class="box" x="60" y="180" width="140" height="60" rx="9"/><text class="lbl" x="130" y="208" text-anchor="middle">输入设备</text><text class="lbl-sm" x="130" y="228" text-anchor="middle">键盘/鼠标/磁盘</text>
        <rect class="box" x="560" y="180" width="140" height="60" rx="9"/><text class="lbl" x="630" y="208" text-anchor="middle">输出设备</text><text class="lbl-sm" x="630" y="228" text-anchor="middle">显示器/打印机</text>
        <!-- 三类总线 -->
        <line class="flow" x1="130" y1="180" x2="130" y2="280" /><line class="flow" x1="130" y1="280" x2="630" y2="280"/><line class="flow" x1="630" y1="280" x2="630" y2="240"/>
        <line class="flow" x1="385" y1="240" x2="385" y2="280"/>
        <line class="flow" x1="325" y1="132" x2="325" y2="180"/><line class="flow" x1="445" y1="132" x2="445" y2="180"/>
        <rect class="box-gold" x="220" y="295" width="320" height="48" rx="9"/>
        <text class="lbl-sm" x="380" y="318" text-anchor="middle">系统总线 = 数据总线 + 地址总线 + 控制总线</text>
        <text class="lbl-sm" x="380" y="336" text-anchor="middle">（运算器+控制器 = CPU；CPU+主存 = 主机）</text>
      </svg>
      <div class="viz-note"><b>冯·诺依曼三大思想：</b>① 采用二进制；② 存储程序（程序/数据预先存入主存，自动顺序执行）；③ 五大部件。指令 = 操作码 + 地址码，取指周期由 PC→MAR→主存→MDR→IR，执行周期由 CU 译码发控制信号。</div>
    </template>

    <!-- CPU 内部结构 -->
    <template v-else-if="topic==='cpu'">
      <svg class="viz-svg" viewBox="0 0 760 380">
        <rect x="30" y="30" width="470" height="320" rx="16" fill="var(--brand-050)" stroke="var(--brand-500)" stroke-width="1.6"/>
        <text class="lbl" x="50" y="58">CPU 内部</text>
        <rect class="box" x="60" y="80" width="120" height="56" rx="9"/><text class="lbl" x="120" y="106" text-anchor="middle">PC 程序计数器</text><text class="lbl-sm" x="120" y="125" text-anchor="middle">存下条指令地址</text>
        <rect class="box" x="200" y="80" width="120" height="56" rx="9"/><text class="lbl" x="260" y="106" text-anchor="middle">IR 指令寄存器</text><text class="lbl-sm" x="260" y="125" text-anchor="middle">存当前指令</text>
        <rect class="box-gold" x="340" y="80" width="140" height="56" rx="9"/><text class="lbl" x="410" y="106" text-anchor="middle">CU 控制单元</text><text class="lbl-sm" x="410" y="125" text-anchor="middle">译码·发控制信号</text>
        <rect class="box" x="60" y="170" width="120" height="56" rx="9"/><text class="lbl" x="120" y="196" text-anchor="middle">MAR</text><text class="lbl-sm" x="120" y="215" text-anchor="middle">访存地址</text>
        <rect class="box" x="200" y="170" width="120" height="56" rx="9"/><text class="lbl" x="260" y="196" text-anchor="middle">MDR</text><text class="lbl-sm" x="260" y="215" text-anchor="middle">访存数据</text>
        <rect class="box" x="340" y="170" width="140" height="56" rx="9"/><text class="lbl" x="410" y="196" text-anchor="middle">通用寄存器组 GPRs</text><text class="lbl-sm" x="410" y="215" text-anchor="middle">ACC/R0…Rn/PSW</text>
        <rect class="box-ok" x="180" y="262" width="180" height="62" rx="10"/><text class="lbl" x="270" y="290" text-anchor="middle">ALU 算术逻辑单元</text><text class="lbl-sm" x="270" y="310" text-anchor="middle">运算：加减与或非·移位</text>
        <line class="flow-dash" x1="120" y1="136" x2="120" y2="170"/><line class="flow-dash" x1="260" y1="136" x2="260" y2="170"/>
        <line class="flow-dash" x1="120" y1="226" x2="220" y2="262"/><line class="flow-dash" x1="260" y1="226" x2="300" y2="262"/><line class="flow-dash" x1="410" y1="226" x2="330" y2="262"/>
        <line class="flow" x1="410" y1="136" x2="410" y2="170"/>
        <!-- 主存 -->
        <rect class="box-gold" x="560" y="120" width="170" height="150" rx="12"/>
        <text class="lbl" x="645" y="156" text-anchor="middle">主存储器</text>
        <line x1="580" y1="172" x2="710" y2="172" stroke="#d8c98f"/>
        <text class="lbl-sm" x="645" y="200" text-anchor="middle">地址 → 译码 → 选中单元</text>
        <text class="lbl-sm" x="645" y="226" text-anchor="middle">按 MAR 地址读写 MDR</text>
        <line class="flow" x1="500" y1="190" x2="560" y2="190" marker-end="url(#cc)"/>
        <line class="flow" x1="560" y1="215" x2="500" y2="215" marker-end="url(#cc)"/>
        <text class="lbl-mono" x="506" y="182">地址总线</text><text class="lbl-mono" x="506" y="240">数据总线</text>
        <defs><marker id="cc" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note"><b>易考点：</b>MAR 位数决定可寻址空间（n 位 → 2ⁿ 个单元）；MDR 位数 = 存储字长 = 数据总线宽度；PC 有自增功能且对用户透明；指令周期 = 取指 + 间址（可无）+ 执行 + 中断（可无）。</div>
    </template>

    <!-- 存储层次金字塔 -->
    <template v-else-if="topic==='mem-pyramid'">
      <svg class="viz-svg" viewBox="0 0 760 380">
        <g v-for="(lv,i) in [
          {n:'寄存器 Registers',w:120,c:'var(--st-active-soft)',s:'var(--st-active)'},
          {n:'L1 Cache（片内）',w:200,c:'#e6f0fb',s:'#2f7fd0'},
          {n:'L2/L3 Cache',w:290,c:'var(--brand-050)',s:'var(--brand-500)'},
          {n:'主存 DRAM',w:380,c:'var(--gold-050)',s:'var(--gold-500)'},
          {n:'SSD / 机械硬盘',w:470,c:'#f8ebe1',s:'var(--co)'},
          {n:'光盘 / 磁带 / 云存储',w:560,c:'#f1f2f3',s:'#828c94'}]" :key="i">
          <rect :x="380-lv.w/2" :y="30+i*52" :width="lv.w" height="46" rx="8" :fill="lv.c" :stroke="lv.s" stroke-width="1.5"/>
          <text class="lbl" x="380" :y="58+i*52" text-anchor="middle">{{ lv.n }}</text>
        </g>
        <text class="lbl-sm" x="120" y="60">↑ 速度越快</text><text class="lbl-sm" x="120" y="82">↑ 单位价格越高</text>
        <text class="lbl-sm" x="620" y="300">↓ 容量越大</text><text class="lbl-sm" x="620" y="322">↓ 越慢/越便宜</text>
        <text class="lbl-mono" x="380" y="360" text-anchor="middle">Cache–主存层：解决 CPU 与主存速度不匹配；主存–辅存层：解决容量问题（虚拟内存）</text>
      </svg>
      <div class="viz-note"><b>局部性原理</b>是层次结构成立的根基：时间局部性（刚访问的很快再访问）+ 空间局部性（相邻地址很快被访问）。命中率越高，平均访问时间越接近上层。</div>
    </template>

    <!-- Cache 映射 -->
    <template v-else-if="topic==='cache'">
      <div class="viz-controls">
        <button class="chip" :class="{on:mapKind==='direct'}" @click="mapKind='direct'">直接映射</button>
        <button class="chip" :class="{on:mapKind==='full'}" @click="mapKind='full'">全相联</button>
        <button class="chip" :class="{on:mapKind==='set'}" @click="mapKind='set'">组相联</button>
      </div>
      <svg class="viz-svg" viewBox="0 0 760 300">
        <template v-if="mapKind==='direct'">
          <text class="lbl-sm" x="40" y="36">主存块 j 只能进 Cache 行：i = j mod C（C=行数）</text>
          <g v-for="(t,i) in ['行0','行1','行2','行3']" :key="i">
            <rect class="box" x="120" :y="60+i*52" width="150" height="40" rx="7"/><text class="lbl" x="195" :y="85+i*52" text-anchor="middle">{{ t }}</text>
          </g>
          <text class="lbl-mono" x="330" y="84">地址 = 标记 Tag | 行号 | 块内偏移</text>
          <text class="lbl-sm" x="330" y="120">优点：实现简单、比较器少、速度快</text>
          <text class="lbl-sm" x="330" y="148">缺点：冲突率高，块 0 与块 4 抢同一行</text>
        </template>
        <template v-else-if="mapKind==='full'">
          <text class="lbl-sm" x="40" y="36">主存任意块可放 Cache 任意行，冲突率最低</text>
          <g v-for="(t,i) in ['行0','行1','行2','行3']" :key="i">
            <rect :class="i<2?'box-ok':'box'" x="120" :y="60+i*52" width="150" height="40" rx="7"/><text class="lbl" x="195" :y="85+i*52" text-anchor="middle">{{ t }}</text>
            <line class="flow" x1="90" y1="80" :x2="120" :y2="80+i*52"/>
          </g>
          <text class="lbl-mono" x="330" y="84">地址 = 标记 Tag | 块内偏移（无行号）</text>
          <text class="lbl-sm" x="330" y="120">优点：灵活、命中率高、空间利用率高</text>
          <text class="lbl-sm" x="330" y="148">缺点：需全部行并行比较（相联存储器），成本高</text>
        </template>
        <template v-else>
          <text class="lbl-sm" x="40" y="36">分组：块 j → 组号 = j mod Q，组内任意行（折中方案，主流）</text>
          <g v-for="(t,i) in ['组0:行0/行1','组1:行2/行3']" :key="i">
            <rect class="box-soft" x="100" :y="66+i*92" width="220" height="74" rx="9"/>
            <text class="lbl" x="210" :y="96+i*92" text-anchor="middle">{{ t }}</text>
            <text class="lbl-sm" x="210" y="120+i*92" text-anchor="middle">组内 2 路相联</text>
          </g>
          <text class="lbl-mono" x="380" y="96">地址 = Tag | 组号 | 块内偏移</text>
          <text class="lbl-sm" x="380" y="132">只需组内并行比较，硬件成本与命中率折中</text>
          <text class="lbl-sm" x="380" y="160">n 路组相联：每组 n 行；是直接映射与全相联的推广</text>
        </template>
      </svg>
      <div class="viz-note"><b>替换算法：</b>随机 RAND、先进先出 FIFO、最近最少用 LRU（最常考）、最不经常用 LFU；写策略分写直达(write-through，无脏位) 与写回(write-back，有脏位)。</div>
    </template>

    <!-- 流水线 -->
    <template v-else>
      <div class="viz-controls">
        <span class="tiny muted">指令条数：</span>
        <button v-for="x in [3,5,6,8]" :key="x" class="chip" :class="{on:nIns===x}" @click="nIns=x">{{ x }}</button>
        <span class="viz-status">总周期 {{ totalCycle }}，加速比 ≈ {{ speedup }}×</span>
      </div>
      <svg class="viz-svg" :viewBox="`0 0 ${Math.max(560,totalCycle*70+120)} 300`">
        <!-- 表头周期 -->
        <text class="lbl-sm" x="60" y="34">段＼周期</text>
        <g v-for="c in totalCycle" :key="'h'+c"><text class="lbl-mono" :x="120+c*70" y="34" text-anchor="middle">{{ c }}</text></g>
        <g v-for="(s,r) in stages" :key="'r'+r">
          <text class="lbl" x="60" :y="74+r*46" text-anchor="end">{{ s }}</text>
          <line class="axis" x1="70" :y1="52+r*46" :x2="120+totalCycle*70" :y2="52+r*46"/>
        </g>
        <g v-for="c in cells" :key="c.i+'-'+c.k">
          <rect :x="86+c.cycle*70" :y="56+(4-c.k)*46" width="64" height="34" rx="7"
            :fill="['#1b7f92','#2e8b74','#c9a24b','#c0622e','#7a5ba0'][c.k]" opacity="0.88"/>
          <text :x="118+c.cycle*70" :y="78+(4-c.k)*46" text-anchor="middle" fill="#fff" font-size="12" font-weight="700">I{{ c.i+1 }}</text>
        </g>
      </svg>
      <div class="viz-note">五段：<b>IF 取指 → ID 译码/取寄存器 → EX 执行 → MEM 访存 → WB 写回</b>。理想 k 段流水线 n 条指令用时 (k+n−1)Δt，加速比趋近 k。
        冒险三类：<b>结构冒险</b>（资源冲突）、<b>数据冒险</b>（RAW/WAR/WAW，靠转发旁路/停顿解决）、<b>控制冒险</b>（转移指令，靠分支预测/延迟槽）。</div>
    </template>
  </div>
</template>
