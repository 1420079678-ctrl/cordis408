<script setup lang="ts">
import { ref, computed } from 'vue'
import { SUBJECT_META } from '../api'
import VizDs from '../components/viz/VizDs.vue'
import VizCo from '../components/viz/VizCo.vue'
import VizCn from '../components/viz/VizCn.vue'
import VizOs from '../components/viz/VizOs.vue'

const order = ['ds', 'co', 'cn', 'os']
const TOPICS: Record<string, { id: string; name: string; desc: string }[]> = {
  ds: [
    { id: 'linked-list', name: '线性表 · 单链表', desc: '指针链接的结点序列' },
    { id: 'stack-queue', name: '栈与队列', desc: 'LIFO 与 FIFO' },
    { id: 'tree-traversal', name: '二叉树与三种遍历', desc: '前序 / 中序 / 后序' },
    { id: 'huffman', name: '哈夫曼树与 WPL', desc: '最优前缀编码' },
    { id: 'bst-avl', name: 'BST 与 AVL 旋转', desc: '查找树与平衡调整' },
    { id: 'graph', name: '图与最短路径', desc: 'Dijkstra 过程' },
    { id: 'sort', name: '排序算法动画', desc: '可交互步进对比' }
  ],
  co: [
    { id: 'von-neumann', name: '五大部件与系统总线', desc: '冯·诺依曼结构' },
    { id: 'cpu', name: 'CPU 内部结构', desc: 'ALU / 寄存器 / CU' },
    { id: 'mem-pyramid', name: '存储器层次金字塔', desc: '速度·容量·价格' },
    { id: 'cache', name: 'Cache 地址映射', desc: '直接 / 全相联 / 组相联' },
    { id: 'pipeline', name: '五段指令流水线', desc: 'IF ID EX MEM WB' }
  ],
  cn: [
    { id: 'layers', name: '分层体系结构', desc: 'OSI 七层 / TCP-IP 四层' },
    { id: 'handshake', name: 'TCP 三次握手·四次挥手', desc: '可靠连接的建立与释放' },
    { id: 'encapsulation', name: '数据封装与解封装', desc: '逐层加首部' },
    { id: 'window', name: '流量控制与拥塞控制', desc: '滑动窗口 / 慢开始拥塞避免' },
    { id: 'journey', name: '一次请求的完整旅程', desc: 'DNS→TCP→HTTP' }
  ],
  os: [
    { id: 'proc-state', name: '进程五状态模型', desc: '就绪/运行/阻塞转换' },
    { id: 'address-translate', name: '分页地址翻译', desc: '页号·偏移→物理地址' },
    { id: 'lru', name: '页面置换动画', desc: 'FIFO / LRU 步进' },
    { id: 'sync', name: '进程同步 · 生产者消费者', desc: '信号量 P/V' },
    { id: 'deadlock', name: '死锁四必要条件', desc: '与处理策略' },
    { id: 'disk-sched', name: '磁盘调度算法', desc: 'FCFS/SSTF/SCAN' }
  ]
}
const subj = ref('ds')
const cur = ref(TOPICS.ds[0].id)
function switchSubj(k: string) { subj.value = k; cur.value = TOPICS[k][0].id }
const topics = computed(() => TOPICS[subj.value])
const curMeta = computed(() => topics.value.find(t => t.id === cur.value))
const comp: any = { ds: VizDs, co: VizCo, cn: VizCn, os: VizOs }
</script>

<template>
  <div class="viz-page">
    <div class="viz-head">
      <div class="page-title">图解实验室</div>
      <div class="page-desc">把 408 最抽象的算法、硬件、协议与机制画成可交互结构图，看图秒懂</div>
      <div class="subj-tabs">
        <button v-for="k in order" :key="k" class="stab" :class="{on:subj===k}"
          :style="subj===k?{background:SUBJECT_META[k].color,borderColor:SUBJECT_META[k].color}:{}" @click="switchSubj(k)">
          {{ SUBJECT_META[k].name }}
        </button>
      </div>
    </div>

    <div class="viz-body">
      <aside class="topic-list card">
        <button v-for="t in topics" :key="t.id" class="topic-item" :class="{on:cur===t.id}" @click="cur=t.id">
          <span class="ti-name">{{ t.name }}</span>
          <span class="ti-desc">{{ t.desc }}</span>
        </button>
      </aside>
      <section class="stage card">
        <header class="stage-h">
          <h3>{{ curMeta?.name }}</h3>
          <span class="muted tiny">{{ curMeta?.desc }}</span>
        </header>
        <div class="stage-canvas">
          <component :is="comp[subj]" :topic="cur" />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.viz-page { padding: var(--u6) var(--u6) var(--u7); max-width: 1240px; margin: 0 auto; }
.viz-head { margin-bottom: var(--u5); }
.subj-tabs { display: flex; gap: 10px; margin-top: 16px; }
.stab { padding: 9px 20px; border-radius: 999px; border: 1px solid var(--line-strong); background: var(--surface); font-weight: 650; font-size: 14px; color: var(--text-2); transition: all .18s; white-space: nowrap; flex: none; }
.stab.on { color: #fff; border-color: transparent; box-shadow: var(--shadow-1); }
.viz-body { display: grid; grid-template-columns: 236px 1.618fr; gap: var(--u4); align-items: start; }
.topic-list { padding: 8px; }
.topic-item { display: flex; flex-direction: column; gap: 3px; text-align: left; padding: 11px 13px; border-radius: var(--r-sm); width: 100%; transition: background .15s; }
.topic-item:hover { background: var(--surface-2); }
.topic-item.on { background: var(--ink-800); }
.topic-item.on .ti-name { color: #fff; }
.topic-item.on .ti-desc { color: rgba(255,255,255,.6); }
.ti-name { font-size: 13.8px; font-weight: 650; color: var(--text-1); }
.ti-desc { font-size: 11.5px; color: var(--text-3); }
.stage { overflow: hidden; }
.stage-h { padding: 16px 24px; border-bottom: 1px solid var(--line); display: flex; align-items: baseline; gap: 12px; }
.stage-h h3 { font-size: 17px; font-weight: 720; }
.stage-canvas { padding: 24px; min-height: 460px; }
@media (max-width:900px){ .viz-body{grid-template-columns:1fr} }
</style>
