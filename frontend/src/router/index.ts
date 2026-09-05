import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '学习总览', icon: '◈' } },
  { path: '/study', name: 'study', component: () => import('../views/StudyView.vue'), meta: { title: 'AI 答疑', icon: '✦' } },
  { path: '/subjects', name: 'subjects', component: () => import('../views/SubjectsView.vue'), meta: { title: '知识体系', icon: '❖' } },
  { path: '/viz', name: 'viz', component: () => import('../views/VizView.vue'), meta: { title: '图解实验室', icon: '⏣' } },
  { path: '/practice', name: 'practice', component: () => import('../views/PracticeView.vue'), meta: { title: '智能练习', icon: '✎' } },
  { path: '/agents', name: 'agents', component: () => import('../views/AgentsView.vue'), meta: { title: 'Agent 运行时', icon: '⬡' } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '学情画像', icon: '◉' } }
]

export default createRouter({ history: createWebHashHistory(), routes })
