<script setup lang="ts">
import { ref, watch } from 'vue'
defineProps<{ topic: string }>()
const hsMode = ref('connect'); const hsStep = ref(0)
watch(hsMode, () => (hsStep.value = 0))
const connectMsgs = [
  { y: 90, dir: 1, t: 'SYN, seq=x', d: '客户端请求建立连接，SYN=1，同步位' },
  { y: 150, dir: -1, t: 'SYN+ACK, seq=y, ack=x+1', d: '服务器确认并同步，ack=x+1' },
  { y: 210, dir: 1, t: 'ACK, ack=y+1', d: '客户端确认，连接建立（ESTABLISHED）' }
]
const waveMsgs = [
  { y: 70, dir: 1, t: 'FIN, seq=u', d: '客户端请求释放' },
  { y: 120, dir: -1, t: 'ACK, ack=u+1', d: '半关闭：服务器仍可发数据' },
  { y: 190, dir: -1, t: 'FIN, seq=w', d: '服务器数据发完，请求释放' },
  { y: 240, dir: 1, t: 'ACK, ack=w+1', d: '客户端确认，等待 2MSL 后关闭' }
]
function maxStep() { return hsMode.value === 'connect' ? 3 : 4 }
</script>

<template>
  <div class="viz-wrap">
    <!-- 分层模型 -->
    <template v-if="topic==='layers'">
      <svg class="viz-svg" viewBox="0 0 760 380">
        <text class="lbl" x="180" y="34" text-anchor="middle">OSI 七层</text>
        <text class="lbl" x="430" y="34" text-anchor="middle">TCP/IP 四层</text>
        <text class="lbl" x="630" y="34" text-anchor="middle">典型协议/设备</text>
        <g v-for="(l,i) in [
          ['应用层','应用层','HTTP/FTP/DNS/SMTP'],
          ['表示层','应用层','加密·压缩·编码'],
          ['会话层','应用层','会话建立与同步'],
          ['传输层','传输层','TCP / UDP'],
          ['网络层','网际层','IP / ICMP / 路由器'],
          ['数据链路层','网络接口层','以太网/PPP·交换机'],
          ['物理层','网络接口层','比特流·集线器·网线']]" :key="i">
          <rect class="box" x="70" :y="50+i*44" width="220" height="38" rx="7" :fill="i<3?'var(--cn-soft)':'#fff'"/>
          <text class="lbl" x="180" :y="75+i*44" text-anchor="middle">{{ l[0] }}</text>
          <rect class="box-soft" x="320" :y="50+i*44" width="220" height="38" rx="7"/>
          <text class="lbl" x="430" :y="75+i*44" text-anchor="middle">{{ l[1] }}</text>
          <text class="lbl-mono" x="560" :y="75+i*44">{{ l[2] }}</text>
        </g>
      </svg>
      <div class="viz-note">下层为上层提供服务；<b>对等层</b>在逻辑上通信（虚线），实际数据由发送端<b>自上而下封装</b>、接收端<b>自下而上解封装</b>。交换机工作在数据链路层（识 MAC），路由器工作在网络层（识 IP）。</div>
    </template>

    <!-- 握手挥手 -->
    <template v-else-if="topic==='handshake'">
      <div class="viz-controls">
        <button class="chip" :class="{on:hsMode==='connect'}" @click="hsMode='connect'">三次握手（建立）</button>
        <button class="chip" :class="{on:hsMode==='wave'}" @click="hsMode='wave'">四次挥手（释放）</button>
        <button class="btn sm" @click="hsStep=Math.min(hsStep+1,maxStep())">单步 →</button>
        <button class="btn ghost sm" @click="hsStep=0">重置</button>
      </div>
      <svg class="viz-svg" viewBox="0 0 760 320">
        <rect class="box-soft" x="80" y="20" width="150" height="44" rx="9"/><text class="lbl" x="155" y="48" text-anchor="middle">客户端 Client</text>
        <rect class="box-soft" x="530" y="20" width="150" height="44" rx="9"/><text class="lbl" x="605" y="48" text-anchor="middle">服务器 Server</text>
        <line class="flow-dash" x1="155" y1="64" x2="155" y2="300"/><line class="flow-dash" x1="605" y1="64" x2="605" y2="300"/>
        <template v-for="(m,i) in (hsMode==='connect'?connectMsgs:waveMsgs)" :key="i">
          <g v-if="hsStep>i" style="transition:all .3s">
            <line class="flow" :x1="m.dir>0?155:605" :y1="m.y" :x2="m.dir>0?605:155" :y2="m.y" marker-end="url(#hn)"/>
            <rect class="box-gold" :x="m.dir>0?230:330" :y="m.y-20" width="200" height="26" rx="7"/>
            <text class="lbl-mono" x="330" :y="m.y-2" text-anchor="middle">{{ m.t }}</text>
            <text class="lbl-sm" x="330" :y="m.y+22" text-anchor="middle">{{ m.d }}</text>
          </g>
        </template>
        <defs><marker id="hn" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note"><b>为什么是三次？</b>两次无法确认客户端的接收能力，也无法同步双方初始序号、防止已失效连接请求突然到达。<b>挥手为何四次？</b>服务器收到 FIN 时可能还有数据要发，故 ACK 与自己的 FIN 分开发送；TIME_WAIT 等待 <b>2MSL</b> 以确保最后 ACK 到达并淘汰旧报文。</div>
    </template>

    <!-- 封装解封装 -->
    <template v-else-if="topic==='encapsulation'">
      <svg class="viz-svg" viewBox="0 0 760 360">
        <text class="lbl" x="200" y="30" text-anchor="middle">发送端：逐层加首部（封装）</text>
        <g v-for="(l,i) in [
          ['应用数据 Data','#fff'],
          ['TCP 首部 | 数据（段 Segment）','var(--cn-soft)'],
          ['IP 首部 | TCP段（数据报）','var(--brand-050)'],
          ['帧头 | IP数据报 | 帧尾 FCS（帧）','var(--gold-050)'],
          ['比特流 0/1（物理层）','#f1f2f3']]" :key="i">
          <rect :stroke="i===0?'var(--ink-700)':'var(--brand-500)'" :fill="l[1]" stroke-width="1.5" :x="70-i*8" :y="56+i*52" :width="320+i*16" height="40" rx="8"/>
          <text class="lbl-sm" :x="230" :y="82+i*52" text-anchor="middle">{{ l[0] }}</text>
        </g>
        <line class="flow" x1="400" y1="160" x2="470" y2="160" marker-end="url(#en)"/>
        <text class="lbl-sm" x="435" y="146" text-anchor="middle">传输</text>
        <text class="lbl" x="610" y="30" text-anchor="middle">接收端：逐层去首部</text>
        <g v-for="(l,i) in [5,4,3,2,1]" :key="'r'+i">
          <rect class="box" :x="480-(5-i)*8" :y="56+(5-i)*52" :width="320+(5-i)*16" height="40" rx="8"/>
        </g>
        <text class="lbl-sm" x="640" y="82">物理层收比特</text>
        <text class="lbl-sm" x="640" y="134">链路层去帧头/FCS</text>
        <text class="lbl-sm" x="640" y="186">网络层去 IP 首部</text>
        <text class="lbl-sm" x="640" y="238">传输层去 TCP 首部</text>
        <text class="lbl-sm" x="640" y="290">应用层得到数据</text>
        <defs><marker id="en" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note"><b>PDU 名称</b>：应用层—报文、传输层—段/用户数据报、网络层—分组/数据报、链路层—帧、物理层—比特。路由器只解封到网络层即可转发。</div>
    </template>

    <!-- 拥塞控制 -->
    <template v-else-if="topic==='window'">
      <svg class="viz-svg" viewBox="0 0 760 330">
        <line class="axis" x1="70" y1="270" x2="720" y2="270"/><line class="axis" x1="70" y1="270" x2="70" y2="40"/>
        <text class="lbl-sm" x="720" y="292" text-anchor="end">传输轮次 RTT →</text>
        <text class="lbl-sm" x="60" y="48" text-anchor="end">cwnd</text>
        <!-- 慢开始 1,2,4,8 到 ssthresh=8 后线性 9,10,11 -->
        <polyline fill="none" stroke="var(--st-active)" stroke-width="2.4"
          points="90,250 180,210 270,170 360,130 450,120 540,110 630,100"/>
        <line class="flow-dash" x1="70" y1="130" x2="700" y2="130"/>
        <text class="lbl-mono" x="706" y="134">ssthresh</text>
        <text class="lbl-sm" x="200" y="150" fill="var(--st-active)">慢开始：每 RTT 翻倍（指数增长）</text>
        <text class="lbl-sm" x="470" y="92" fill="var(--co)">拥塞避免：每 RTT +1（线性/加法增大）</text>
        <circle cx="90" cy="250" r="4" fill="var(--st-active)"/><circle cx="180" cy="210" r="4" fill="var(--st-active)"/><circle cx="270" cy="170" r="4" fill="var(--st-active)"/><circle cx="360" cy="130" r="4" fill="var(--gold-500)"/>
      </svg>
      <div class="viz-note"><b>四个算法：</b>慢开始（cwnd 从 1 指数增）、拥塞避免（达 ssthresh 后线性增）、快重传（收到 3 个重复 ACK 立即重传，不等超时）、快恢复（ssthresh 与 cwd 减半后直接进入拥塞避免，而非回到 1）。拥塞时 ssthresh=cwnd/2。</div>
    </template>

    <!-- 请求旅程 -->
    <template v-else>
      <svg class="viz-svg" viewBox="0 0 760 420">
        <g v-for="(s,i) in [
          ['① URL 解析 + 浏览器缓存查询'],
          ['② DNS 域名解析：递归/迭代查询得到 IP'],
          ['③ TCP 三次握手建立连接'],
          ['④ TLS 握手（HTTPS，协商对称密钥）'],
          ['⑤ 发送 HTTP 请求报文'],
          ['⑥ 服务器处理并返回响应'],
          ['⑦ 浏览器解析 HTML、构建 DOM/CSSOM、渲染'],
          ['⑧ 四次挥手释放连接']]" :key="i">
          <rect class="box" :class="i===2?'box-gold':''" x="120" :y="30+i*46" width="520" height="36" rx="9"/>
          <text class="lbl-sm" x="140" :y="54+i*46">{{ s[0] }}</text>
          <line v-if="i<7" class="flow" x1="380" :y1="66+i*46" x2="380" :y2="76+i*46" marker-end="url(#jy)"/>
        </g>
        <defs><marker id="jy" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto"><path d="M0,0 L8,4.5 L0,9 Z" fill="#16697a"/></marker></defs>
      </svg>
      <div class="viz-note">输入网址到页面呈现横跨<b>应用层(DNS/HTTP)、传输层(TCP)、网络层(IP 路由)、链路层</b>；常考：DNS 默认 UDP 53（区域传送用 TCP）、HTTP 默认 80 / HTTPS 443、输入回车后浏览器与 CDN/缓存的交互。</div>
    </template>
  </div>
</template>
