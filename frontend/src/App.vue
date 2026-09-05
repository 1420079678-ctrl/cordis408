<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
const nav = [
  { to: '/dashboard', icon: '◈', label: '学习总览' },
  { to: '/study', icon: '✦', label: 'AI 多 Agent 答疑' },
  { to: '/subjects', icon: '❖', label: '四科知识体系' },
  { to: '/viz', icon: '⏣', label: '图解实验室' },
  { to: '/practice', icon: '✎', label: '智能出题练习' },
  { to: '/agents', icon: '⬡', label: 'Agent 运行时' },
  { to: '/profile', icon: '◉', label: '学情诊断画像' }
]
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">φ</div>
        <div>
          <div class="brand-name">Cordis408</div>
          <div class="brand-sub">SPATIOTEMPORAL MULTI-AGENT</div>
        </div>
      </div>
      <nav class="nav">
        <RouterLink v-for="n in nav" :key="n.to" :to="n.to" class="nav-item"
          :class="{ active: route.path.startsWith(n.to) }">
          <span class="ico">{{ n.icon }}</span><span>{{ n.label }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-foot">
        基于《A Programming Paradigm for Spatiotemporal Composability》
        可逆效果 × 响应式协效果 · Fiber 生命周期
      </div>
    </aside>
    <main class="main">
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>
