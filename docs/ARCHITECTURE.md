# Cordis408 架构设计说明书

> 关键词：Spatiotemporal Composability · Revertible Effects · Reactive Coeffects · Fiber Lifecycle · Declarative Loader · Transactional HMR

## 1. 设计目标
把论文提出的「时空两维可组合」编程范式落地为一个**多 Agent 学习后端**：Agent 之间不直接硬编码调用，而是向一个统一上下文**声明自己需要什么能力（inject）、提供什么能力（provide）**，由运行时决定每个 Agent「何时激活、何时休眠、如何回滚」。由此获得：

1. **空间可组合**：新增/移除 Agent 不需要改调用方，依赖关系自动接线、自动级联；
2. **时间可组合**：任何激活产生的副作用都可被精确、逆序地撤销，系统可安全地动态启停与热替换；
3. **可观测/可治理**：任一时刻的组件拓扑、状态、依赖边都可查询与操作。

## 2. 三层结构（对应论文第 5 章）

```
┌────────────────────────────────────────────────────────┐
│ 上层应用：408 学习业务（答疑/练习/诊断/规划）             │
├────────────────────────────────────────────────────────┤
│ Component Loader：读 agents.yml → reconcile 增量对账     │
│   声明式 entry、动态 setDisabled、事务性 hotReload        │
├────────────────────────────────────────────────────────┤
│ Core Library：CordisRuntime / RuntimeContext / Fiber     │
│   效果追踪(LIFO) · 协效果解析(notify/refresh) · 生命周期  │
└────────────────────────────────────────────────────────┘
```

## 3. 核心抽象

### 3.1 Component：三元组 ⟨d, p, e⟩
- `d`：inject 的协效果 key 集合（**需要**哪些能力就绪）；
- `p`：provide 的 key 集合（**向上下文发布**哪些能力）；
- `e`：激活效果函数，返回一个 `Disposable`（内含 inverse 回滚函数）。

业务侧用 `AbstractAgent` 统一封装，具体 Agent 只实现 `activate(ctx)`（注册能力并返回回滚）与业务方法。

### 3.2 统一上下文 RuntimeContext（Γ∞）
effect（对上下文的写/副作用）与 coeffect（从上下文读能力）都经过同一个上下文：
- `set(key, provider)` / `remove(key)`：能力的发布与撤销；
- `isProvided(key)`：当前是否存在 ACTIVE 提供者（空间维判定的唯一依据）；
- 支持 realm 作用域，`notify` 只在同一作用域传播，避免跨域误唤醒。

### 3.3 Fiber：带惯性的状态机
```
INACTIVE ──依赖满足/启用──▶ LOADING ──激活成功──▶ ACTIVE
   ▲                          │                     │
   └──── 依赖缺失/停用 ◀── UNLOADING ◀──────────────┘
                              └── 异常 ──▶ FAILED
```
- `refresh()`：依据 `desired = !disabled && 所有 inject key 均 isProvided` 与当前 `target` 比较，分类为
  **activating / deactivating / neutral**，只在状态真正需要改变时迁移（幂等）；
- 激活：LOADING → 执行效果 e → 收集 Disposable、发布 provide key、ACTIVE → `notify(provideKeys)`；
- 卸载：UNLOADING → 按 **LIFO 逆序执行 inverse** 撤销副作用、注销 key → INACTIVE → notify 依赖者；
- 这正是「时间维可逆效果」：**谁后注册，谁先回滚**，保证上下文最终一致。

### 3.4 协效果的级联（空间维）
当某 provider P 状态变化：
1. `CordisRuntime.notify(changedKeys)` 遍历所有 Fiber；
2. inject 命中 changedKeys 且同 realm 者触发 `refresh()`；
3. P 停用 → 依赖者发现 key 不再 provided → 级联 UNLOADING；
4. P 恢复 → 依赖者重新满足 → 级联激活。

> 关键实现细节：notify **不能跳过**「INACTIVE 且 target=false」的 Fiber——它们正是「因缺依赖而等待者」，
> provider 恢复时必须被重新评估唤醒（refresh 内部自带幂等，重复触发无副作用）。

## 4. Component Loader：声明式与事务热替换
- `agents.yml` 中每个 entry：`id / type / config / disabled / isolate / intercept`；
- **reconcile**：对比「目标 entry 集合」与「当前 liveFibers」，增量增删改，不动无关组件；
- **依赖只约束何时激活，不约束加载顺序**：entry 书写顺序任意，静止态只由最终配置决定；
- **hotReload(type)**：三阶段事务——① 卸载该类型全部旧实例并记录现场；② 装载新实例；
  ③ 任一步失败则按 inverse 回滚到旧版本，保证会话不中断、不产生半装状态。

## 5. 一次答疑的协同时序
```
POST /ask
 └─ Supervisor.orchestrate(WorkContext)
     1) RouterAgent        识别 mode(ask/tutor/quiz/plan) 与学科（coeffect: router）
     2) RetrievalAgent     从 KnowledgeBase 检索 Top-K 考点（coeffect: knowledge.base）
     3) SubjectExpertAgent 对应学科专家组织讲解（coeffect: expert.* + learner.state）
        · TutorAgent  改写为苏格拉底引导；ProblemAgent 出题；PlannerAgent 出阶段规划
     4) DiagnosisAgent     结合 LearnerModel 标注薄弱点
     5) Supervisor 汇总 finalAnswer / evidence / traces / followUps
```
`traces` 逐层回传，前端「协同链路」面板把这一过程可视化；每个 Agent 的激活/调用耗时也被记录。

## 6. Agent 职责一览
| Agent | 职责 | provide |
|---|---|---|
| LearnerModelAgent | 维护每会话提问/答题/正确率/薄弱考点 | learner.state |
| RetrievalAgent | 考点与题库检索 | knowledge.base |
| RouterAgent | 意图与学科路由 | router |
| SubjectExpertAgent×4 | 四科确定性讲解（同组件多实例） | expert.ds/co/cn/os |
| TutorAgent | 引导式教学 | tutor |
| ProblemAgent | 抽题组卷 | problem |
| DiagnosisAgent | 批改、薄弱诊断 | diagnosis |
| PlannerAgent | 分阶段复习路径 | planner |
| SupervisorAgent | 总编排与结果汇总 | supervisor |

## 7. 如何新增一个 Agent（扩展范式）
1. 继承 `AbstractAgent`，声明 inject/provide，实现 `activate`（返回 inverse）与业务方法；
2. 在 `agents.yml` 增加一条 entry（顺序任意）；
3. 需要被编排时在 Supervisor 中通过上下文 `get(key)` 获取——它可能为 null（未激活），代码必须优雅降级；
4. 重启或直接调用 `/runtime/hot-reload/{type}` 热装载，前端拓扑图自动出现新节点与依赖边。

## 8. 健壮性与降级
- **LLM 可选**：`LlmGateway.complete()` 返回 null（未启用/网络失败/超时）即回退内置确定性引擎；
- **无状态依赖中间件**：知识库为只读 JSON，学习者画像在内存按 sessionId 维护，可随时 reset；
- **失败隔离**：单个 Fiber 激活异常进入 FAILED，只影响其依赖链，不拖垮整个运行时。
