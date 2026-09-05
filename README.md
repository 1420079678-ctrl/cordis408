# Cordis408 · 时空可组合多 Agent 考研 408 智能学习系统

一个面向计算机考研 **408（数据结构 / 计算机组成原理 / 计算机网络 / 操作系统）** 的 AI 学习系统。
其后端多 Agent 协同内核，严格依据论文
**《A Programming Paradigm for Spatiotemporal Composability》（Cordis 元框架，Yifan Shi / Wei Zhang / Tianyi Cui）**
的核心范式从零实现：**时间维可组合（可逆效果 Revertible Effects）× 空间维可组合（响应式协效果 Reactive Coeffects）**，
每个 Agent 都是一个带惯性生命周期、可热插拔、可观测、可回滚的 **Cordis Component（运行实例称 Fiber）**。

---

## 一、它能做什么

| 模块 | 能力 |
|---|---|
| 学习总览 | 四科知识图谱、考点/题量统计、学习数据与正确率环 |
| AI 多 Agent 答疑 | 意图路由 → 知识检索 → 四科专家 → 教学/出题/规划，**右侧实时展示 Agent 协同链路与引用考点**；支持答疑 / 苏格拉底引导 / 出题 / 规划四种模式 |
| 四科知识体系 | 48 个高频考点按章节组织，点开看精讲与易错点 |
| **图解实验室** | 23 张可交互结构图：数据结构算法动画（排序/遍历/哈夫曼…）、组成原理硬件结构图（五大部件/CPU/存储金字塔/Cache/流水线）、计网时序图（TCP 握手挥手/封装/拥塞控制）、操作系统机制图（进程状态/分页翻译/LRU 置换/磁盘调度） |
| 智能练习 | 按学科/难度/题量抽题（出题时对学生隐藏答案），提交后才批改并给解析，自动更新学情 |
| Agent 运行时 | SVG 实时拓扑：Fiber 状态 + 协效果依赖边，**在线停用任一 Agent 可观察级联停用、恢复则级联唤醒，支持事务性热替换** |
| 学情画像 | 四科掌握度、薄弱考点诊断、最近学习主题 |

内置**确定性讲解引擎**，**零外部依赖、无需联网、无需数据库 / 消息队列 / API Key 即可完整运行**；
可选接入任意 OpenAI 兼容大模型（已预置 DeepSeek）作为增强，未配置或调用失败时自动降级回内置引擎，永不空答。

---

## 二、论文思想 → 工程架构映射

| 论文概念 | 本系统落地 | 代码位置 |
|---|---|---|
| Component = ⟨d 依赖, p 提供, e 效果⟩ | 每个 Agent 声明 `inject`(协效果依赖) / `provide`(提供能力 key) / 激活效果 | `runtime/Component.java`、`agent/*Agent.java` |
| Fiber 惯性状态机 INACTIVE→LOADING→ACTIVE→UNLOADING→FAILED | 每个 Agent 运行实例的生命周期 | `runtime/Fiber.java`、`FiberState.java` |
| 一等统一上下文 Γ∞（统一 effect/coeffect） | `RuntimeContext` 按 key 提供能力，组件只面向上下文编程、不硬耦合彼此 | `runtime/RuntimeContext.java` |
| **空间维：响应式协效果**（inject/provide，notify→refresh，activating/deactivating/neutral） | provider 状态变化经 `notify` 分类传播，只有 ACTIVE provider 真正提供能力；依赖缺失自动 INACTIVE、恢复自动唤醒 | `runtime/CordisRuntime.java` |
| **时间维：可逆效果**（每次变换携带 inverse，LIFO 追踪，卸载自动回滚） | Fiber 激活时向上下文注册能力，停用按 LIFO 注销全部副作用，回到一致静止态 | `runtime/Fiber.java` |
| Core Library（效果追踪 + 协效果解析 + 生命周期） | `runtime/` 内核包 | `runtime/` |
| Component Loader（声明式 entry、增量 reconcile、三阶段 HMR + 事务回滚） | `agents.yml` 声明 12 个 entry；`AgentLoader` 增量对账、事务性热替换（失败回滚） | `runtime/loader/`、`resources/agents.yml` |
| 静止态只由最终配置决定、依赖只约束「何时激活」不约束书写顺序 | `agents.yml` 条目顺序任意，最终拓扑只由 inject/provide 关系决定 | — |

**12 个 Agent 与协效果 key：**

```
learner-model ──provide: learner.state
retrieval     ──provide: knowledge.base
router        ──provide: router
expert-ds/co/cn/os ──inject[learner.state, knowledge.base] ──provide: expert.ds/co/cn/os   （同一组件 4 实例）
tutor/problem ──inject[learner.state, knowledge.base] ──provide: tutor / problem
diagnosis/planner ──inject[knowledge.base, learner.state] ──provide: diagnosis / planner
supervisor    ──inject[knowledge.base, learner.state, router] ──provide: supervisor（总编排）
```

> 关于技术选型的一点澄清：需求中提到「后端为 React」，React 实为**前端**框架。本系统据此采用
> **前端 Vue 3 + TypeScript + Vite + Pinia（专业级 UI、黄金分割设计系统）**，
> **后端 Spring Boot 3 + Java 21**，二者通过 REST 协作，这是更合理的前后端分工。

---

## 三、目录结构

```
408学习助手/
├─ backend/                         Spring Boot 后端（Cordis 内核 + 12 Agent）
│  ├─ src/main/java/io/cordis408/
│  │  ├─ runtime/                   论文内核：Fiber/上下文/协效果/可逆效果
│  │  │  └─ loader/                 声明式加载器：reconcile + 事务热替换
│  │  ├─ agent/                     12 个业务 Agent
│  │  ├─ knowledge/                 考点与题库模型、知识库
│  │  ├─ llm/                       可选 LLM 网关（OpenAI 兼容 / DeepSeek）
│  │  ├─ config/ web/               启动装配、REST 接口
│  │  └─ Cordis408Application.java
│  └─ src/main/resources/
│     ├─ agents.yml                 12 个声明式组件 entry
│     ├─ application.yml            服务与 LLM 配置
│     └─ data/{ds,co,cn,os}.json    48 考点 + 24 精选题
├─ frontend/                        Vue 3 前端
│  └─ src/
│     ├─ views/                     7 个页面
│     ├─ components/viz/            四科图解实验室（VizDs/Co/Cn/Os）
│     ├─ api/ router/ lib/ styles/
│     └─ App.vue main.ts
├─ docs/ARCHITECTURE.md             架构设计详解
├─ start-all.ps1                    一键启动前后端（Windows）
└─ README.md
```

---

## 四、快速启动

### 环境要求
- JDK 21、Maven 3.9+（后端）
- Node.js 20+ / npm（前端）

### 方式一：一键脚本（Windows PowerShell）
```powershell
# 在项目根目录执行，会自动安装依赖、打包并分别弹出两个窗口启动前后端
./start-all.ps1
```
启动后浏览器打开 **http://localhost:5173** （前端已配置 `/api` 代理到 8080）。

### 方式二：手动启动
```powershell
# 终端 1 —— 后端（首次会下载依赖；已提供打包好的 jar）
cd backend
mvn -DskipTests package
java -jar target/cordis408-backend-1.0.0.jar        # http://localhost:8080

# 终端 2 —— 前端
cd frontend
npm install
npm run dev                                         # http://localhost:5173
```

### 可选：接入 DeepSeek（不接也能完整使用）
编辑 `backend/src/main/resources/application.yml`：
```yaml
llm:
  enabled: true
  base-url: https://api.deepseek.com
  api-key: sk-你的key
  model: deepseek-chat
```
任意 OpenAI 兼容服务（`/v1/chat/completions`）均可；关闭或调用失败时自动用内置确定性引擎作答。

---

## 五、主要 HTTP 接口（前缀 /api）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ask` | 统一问答（body: sessionId, message, mode?, subject?） |
| GET | `/subjects` · `/subjects/{code}/points` | 学科总览 / 考点列表 |
| GET | `/practice/quiz?subject&difficulty&n` | 抽题（隐藏答案） |
| POST | `/practice/grade` | 批改（此时返回答案解析） |
| GET | `/diagnosis` · `/learner` · POST `/learner/reset` | 诊断 / 画像 / 重置 |
| GET | `/runtime/topology` | Fiber 拓扑（节点 + 协效果边） |
| POST | `/runtime/entries/{id}/toggle?disabled=` | 动态启停（级联） |
| POST | `/runtime/hot-reload/{type}` | 事务性热替换 |

---

## 六、设计与工程要点
- **黄金分割设计系统**：间距/字号沿 φ=1.618 模数展开，主内容采用 1.618:1 双栏；墨青 + 暖金学术配色，四学科各有识别色，Fiber 五状态色与后端一一对应。
- **零中间件**：不引数据库 / MQ，知识库用本地 JSON，便于离线学习与二次开发。
- **可验证**：后端 `mvn package` 编译打包通过、全部接口实测通过；前端 `npm run build` 通过并逐页浏览器验证。
