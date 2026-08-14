---
title: DeepSeek Harness 技术设计学习报告
date: 2026-08-14 11:36:03
tags: [TypeScript, JavaScript, Agent, Harness]
category: [Agent]
---

本文是一份面向源码学习者的技术设计参考，目标是建立一套能够解释代码结构、运行时行为和扩展方式的整体模型。它不会复制逐包 API 或生成目录；精确类型、当前包依赖和完整配置字段分别以[子系统参考](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/README.zh.md)、[模块依赖图](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/module-graph.zh.md)和[配置目录](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/config-catalog.zh.md)为准。

## 1. 如何使用这份报告

第一次阅读时，先读第 2 至 8 节，理解系统为什么被设计成“插件树 + 事件日志 + 能力 seam”；随后按第 16 节跟踪一条真实请求。准备修改代码时，再查阅第 9 至 15 节和相应包 README。

建议先具备 TypeScript、ESM、依赖注入、异步迭代器和事件溯源的基础知识。不了解 Cordis 时，应先完成 [Cordis 教程](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/cordis-tutorial/index.zh.md)；它解释本项目所依赖的上下文、服务、事件、effect 和配置加载机制。

本文描述当前源码中的职责关系，而不是承诺所有默认提供方、配置项或包名永远不变。判断机器实际启动了什么时，使用 `dsh --profile <name> --dump-config`，不要从目录存在推断插件已经挂载。

## 2. 系统定位与设计目标

DeepSeek Harness 是一个可组装的智能体运行时。它把模型调用、会话、提示词、工具、权限、持久化、子智能体、Web UI 和外部协议都实现为 Cordis 插件，并通过配置选择实现。产品的核心能力不是一个不可替换的主程序，而是一组在共享上下文中协作、可随生命周期卸载的服务和事件监听器。

这套设计主要服务五个目标：

1. **可替换性。** 模型、文件系统、子进程、沙箱、持久化和交互提供方可以在不分叉 agent loop 的前提下替换。
2. **可重建性。** 模型看见的内容来自追加式会话日志，恢复、分叉、回放、UI 投影和遥测可以共享同一事实来源。
3. **组合优先。** Web、headless 和其他运行方式在同一基础组合上叠加差异，而不是维护多套产品内核。
4. **生命周期安全。** 注册、后台任务和资源占用归属于插件 effect；卸载或启动失败时能够逆序清理。
5. **边界显式。** Host/Client、可信配置/外部输入、能力定义/实现/消费方以及源码/构建产物均有明确分工，并由类型或仓库门禁检查。

相应代价是间接层较多。理解一个行为时不能只搜索函数调用，还要同时检查 Cordis 配置项、注入服务、事件监听器、scope 和生成的跨端描述。项目用生成图、子系统参考和运行时 invariant 降低这种成本。

## 3. 技术基础与代码组织

仓库是一个使用 pnpm workspace 的 TypeScript ESM monorepo，要求 Node.js `^22.19.0 || >=24.0.0`。TypeScript 使用严格模式；Vitest 承担单元、集成、快照和浏览器相关测试，tsdown 生成包运行时产物，Vite/VitePress 分别构建 Web 应用和文档站。Python SDK、原生 Landlock 组件和 vendored Cordis 位于独立顶层目录，但主运行时由 `packages/*/*` 下的 `@deepseek-ai/dsh-*` 包组成。

源码布局表达所有权：

| 区域                                   | 学习时应理解的职责                                         | 首选入口                                                                                                                                                                                                             |
| -------------------------------------- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `apps/`                                | 最终应用入口；CLI 分发 profile，Web 入口只启动浏览器 shell | [`apps/cli/src/bin.ts`](https://github.com/deepseek-ai/deepseek-harness/tree/master/apps/cli/src/bin.ts)、[`apps/web/src/main.ts`](https://github.com/deepseek-ai/deepseek-harness/tree/master/apps/web/src/main.ts) |
| `packages/core/`                       | 会话、提示词、工具、Agent 接口和默认循环组成的产品主干     | [核心子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/core.zh.md)                                                                                                                 |
| `packages/<capability>/`               | 某项能力的定义、提供方与消费方                             | [能力图](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/capability-seams.zh.md)                                                                                                                    |
| `packages/bundle/`                     | 可安装的 profile 配置层，不是另一套运行时                  | [基础组合](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/base/README.zh.md)                                                                                                            |
| `packages/host/` 与 `packages/client/` | Web 的服务器侧和浏览器侧运行时                             | [API Gateway](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/api-gateway.zh.md)                                                                                                                    |
| `packages/typert/` 与 `packages/api/`  | 类型分析、跨端描述、RPC 注册与调用                         | [Typert 子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/typert.zh.md)                                                                                                            |
| `packages/session/`                    | 会话持久化、投影、标题和遥测等数据能力                     | [持久化子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/persistence.zh.md)                                                                                                        |
| `examples/` 与 `packages/examples/`    | 可运行叶子配置与可复用演示组合                             | [开发指南](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/development.zh.md#演示)                                                                                                                  |
| `scripts/`                             | 生成器、静态检查和 CI 门禁的真源                           | [`package.json`](https://github.com/deepseek-ai/deepseek-harness/tree/master/package.json)                                                                                                                           |
| `docs/` 与 `.agents/notes/`            | 当前设计参考与设计决策理由                                 | [文档图索引](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/graph-atlas.zh.md)、[Agent Notes](https://github.com/deepseek-ai/deepseek-harness/tree/master/.agents/notes/README.zh.md)              |

不要按目录数量理解系统复杂度。许多包只实现能力三角色中的一个角色，最终行为取决于组合后的插件树。完整包表由 [`packages/README.zh.md`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/README.zh.md) 维护，依赖关系由生成的[模块依赖图](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/module-graph.zh.md)维护。

## 4. 总体架构模型

可以把系统理解为六个由下到上组合的层次：

```text
Application surface     CLI / Web / Headless / ACP / SDK
Composition             Profile -> Bundles -> User patches -> CLI overlays
Plugin runtime          Cordis Context / Service / Event / Effect / Scope
Agent spine             Session / System Prompt / Tools / Agent / Agent Loop / LLM
Capability families     Definition / Provider / Consumer
Infrastructure          Persistence / Process / Filesystem / Sandbox / Network / Storage
```

这些层不是传统的单向调用栈。Cordis 上下文承载服务，事件把策略和观察者接入执行路径，配置决定哪些插件存在，日志又把执行事实提供给持久化、投影、UI 和下一次模型请求。更准确的关系是：**组合决定结构，服务提供操作，事件开放协作点，会话日志记录事实，投影面向读模型。**

一个最小可工作的智能体需要会话、提示词、工具注册表、Agent 接口、一个 Agent 驱动和一个 LLM 适配器。文件工具、持久化、审批、Web UI 或子智能体都是在这些接口旁挂载的插件；默认发行组合很丰富，但架构不要求所有能力同时存在。

## 5. Cordis 插件运行时

Cordis 的 `Context` 同时承担依赖注入容器、事件总线和生命周期作用域。服务类把一个具名能力注册到上下文，消费方通过注入声明依赖；插件使用 `ctx.effect()` 或 `ctx.on()` 注册可撤销行为。effect 返回的 disposer 在插件卸载时执行，因此注册表条目、监听器、文件观察器和其他资源都应有明确所有者。

需要特别掌握五个概念：

| 概念      | 在 Harness 中的含义            | 阅读代码时的问题                                 |
| --------- | ------------------------------ | ------------------------------------------------ |
| Service   | 上下文中的具名接口或实现       | 谁声明 `ctx` 键，谁提供，谁注入？                |
| Event     | 插件之间的实时扩展点           | 这是持久事实、Agent 实时事件，还是能力策略事件？ |
| Effect    | 随插件生命周期撤销的注册或资源 | disposer 是否覆盖成功、失败和热重载？            |
| Waterfall | 监听器可包装或改写结果的链     | 每个监听器是否调用 `next()` 委托下游？           |
| Scope     | 某个 Agent 的注册和分派身份    | 行为是进程级、会话级还是 Agent 级？              |

服务存在并不等于功能已经正确组装。运行时 invariant 检查由一个包拥有的真实关系，例如事件是否出现、可变数据是否满足约束；它不把方法存在或固定纯函数样例当作运行时健康证明。阅读插件时可先看 `src/index.ts` 的 `Config`、`inject`、`apply` 和服务注册，再看 `src/invariant.ts` 如何定义最低有效关系。

## 6. 启动、Profile 与 Bundle

CLI 入口 [`apps/cli/src/bin.ts`](https://github.com/deepseek-ai/deepseek-harness/tree/master/apps/cli/src/bin.ts) 先解析参数，再按模式动态导入 profile 启动、插件管理或配置输出路径。Profile 启动加载分层环境，解析 Harness home，读取 profile manifest 和各 bundle 的 patch，然后由 `app-boot` 创建根 `Context`、安装 Cordis Loader/Include/Group，挂载组合后的配置项并等待插件树完全结算。

有效配置从空列表开始，按以下优先级叠加：

1. profile manifest 中依次列出的 bundle patch；
2. profile 自己的 `cordis.patch.yml`；
3. Harness home 的 `cordis.patch.yml`；
4. 命令行 `--patch` overlay。

patch 通过稳定的配置项 id 定位目标。覆盖一个配置项时会替换其整个 `config`，不会深合并，所以必须重述要保留的字段。命名但不存在的目标会报告警告；显式给出的文件无法读取、解析或验证时启动失败，不会静默跳过。

[`dsh-base`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/base/README.zh.md) 是默认 profile 的共同底层，组装模型、核心主干、工具、持久化、权限、设置和凭证等插件。[`dsh-web-app`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/web-app/README.zh.md) 添加 HTTP Host、浏览器插件表和 Web 运行时；[`dsh-headless`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/headless/README.zh.md) 添加一次性任务驱动，不挂载 HTTP 或浏览器组件。

学习配置时最有价值的命令是：

```sh
pnpm dsh --profile web --dump-config
pnpm dsh --profile headless --dump-config
```

比较输出中的配置项 id、`name`、`inject`、`disabled` 和 `isolate`，可以直接看到两个产品表面共享什么、替换什么以及哪些服务进入独立 realm。Bundle 的 `cordis.patch.yml` 是默认架构的可执行装配图，比静态包目录更接近真实系统。

## 7. Agent 主干与执行状态机

`packages/core` 刻意分开接口和默认实现。`dsh-agent` 定义 `Agent`、注册表、inbox 和 `agent/*` 事件；`dsh-agent-loop` 提供默认的 `ReactLoopAgent`，但其他驱动可以实现同一接口。UI、工具和扩展插件依赖 Agent 接口，而不是默认 loop。

Agent 接收三类输入：follow-up 进入下一轮次并唤醒驱动，steer 进入下一步骤并唤醒驱动，inject 只排入下一步骤但不自行唤醒。单个 inbox 统一这些路径，避免多个输入通道各自拥有竞争的状态机。

一次运行的关键顺序如下；完整细节见 [Agent 生命周期](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/agent-lifecycle.zh.md)：

```text
turn/start
  inbox.claim
  systemPrompt.assemble
  agent/pre-step
  step/start
    user/message
    session.deriveMessages
    agent/request
    llm.stream
    assistant/chunk*
    assistant/message
    tool/call* -> tools pipeline -> tool/result*
  step/end
  next-step work? -> repeat step
  agent/turn-stopping
turn/end
```

一个步骤对应一次模型请求及其工具调用；一个轮次可以包含多个步骤。工具结果或 steer 输入会让同一轮次继续到下一步骤，没有待处理工作时轮次结束。即使首个输入被策略拒绝或被改写为空，已经打开的轮次也会得到对应的 `turn/end`，因此日志不会留下半个生命周期。

`ReactLoopAgent` 用显式 phase 区分 idle、maintenance 和 running，并用 `AbortController` 将取消传入预处理、模型流和工具执行。`whenIdle()` 等待当前以及被重新唤醒后替换的 activity promise，headless 驱动和安全 teardown 因而能等待真正的完全停稳。异常在 `agent/error` 的实时边界报告，同时被驱动边界包含，最终用结构化原因关闭轮次。

## 8. 会话日志：系统的事实主干

`Session` 是只追加的事件序列。核心事件包括轮次、步骤、用户消息、助手流分片、助手完成消息、工具调用和工具结果；插件通过 TypeScript declaration merging 扩展 `SessionEventMap`。事件获得单调序号并在 append 时冻结，发布到 `session/event` 供持久化、投影、遥测和 UI 消费。

最重要的不变量是：**模型可见内容必须可由会话日志重建。** 默认 loop 在每次请求前调用 `session.deriveMessages()`，而不是维护另一份隐藏对话数组。提示词组装、运行时上下文和请求配置也各自通过可追踪机制进入请求；运行时 invariant 会比较真实请求与日志推导结果。

这一选择带来四个直接结果：

1. 恢复和分叉从同一事件序列开始，不需要复制一套易漂移的对话状态。
2. 原始 `assistant/chunk` 保留流式重放和 UI 保真度，完成后的 `assistant/message` 提供语义结果。
3. UI、统计、标题、查询和遥测应该从日志或定义明确的投影读取，而不是监听若干临时回调后自行拼状态。
4. 新增模型可见输入时必须定义会话事件及其投影，否则重新加载后的请求会与实时请求不同。

会话事件和实时 Agent 事件不能混用。需要跨重启保留的事实属于 `SessionEventMap`；只在执行中的对象、取消信号、策略协作或观察行为属于 `agent/*` 或能力事件。精确事件类型及投影规则见[会话子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/session.zh.md)，生产方和消费方见[事件矩阵](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/event-producer-consumer.zh.md)。

## 9. 系统提示词与 LLM 适配

`system-prompt` 服务收集具名提示词 section 和工具提供方。每个步骤开始前，它以当前 Agent 的组装上下文生成 `PromptAssembly`；随后渲染 section、工具 schema 和运行时上下文。插件由此可以贡献稳定的全局说明或按请求计算的上下文，而不修改 agent loop。

LLM 能力由 `dsh-llm` 定义统一的消息、内容块、请求、流分片和错误类型。提供方注册适配器并解析模型相关默认值；loop 经 `agent/request` waterfall 允许策略修改最终提案，再调用绑定到该模型解析结果的 prepared call 或 `ctx.llm.stream()`。

模型输出以异步流消费。`BlockAssembler` 把文本、推理内容、工具调用、用量和结束状态组装成完整助手消息；每个分片先写入日志，结束后再写完整消息。如果适配器返回可重试错误，`agent/request-error` waterfall 可以决定是否重新请求。提供方差异因此停留在适配器和配置解析层，循环只理解统一协议。

学习这一部分时，应区分三种内容：稳定的系统提示词 section、每步骤重新计算的上下文，以及从日志派生的对话历史。三者最终都进入模型请求，但缓存影响、持久化责任和更新时机不同。详细类型见 [LLM Streaming](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/llm-streaming.zh.md) 和[系统提示词子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/system-prompt.zh.md)。

## 10. 工具注册与执行流水线

`Tools` 服务维护作用域化工具注册表。每个 `ToolDefinition` 同时声明模型 schema、执行函数、并发模式和 UI 展示意图；系统提示词组装会把当前 Agent 可见的 schema 交给模型。工具不能仅考虑执行逻辑，因为参数摘要、执行中状态、结果位置和 `generic`/`terminal`/`diff` 呈现方式也是产品行为。

一次模型工具调用依次经过参数解析、`tools/pre-execute`、`tools/execute` 和 `tools/post-execute`。策略插件可在预执行阶段拒绝或重写，执行 waterfall 可包装真实工具，后处理可整理结果；监听器必须调用 `next()` 才会继续下游。完整阶段和失败语义见[工具执行流水线](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/tool-execution-pipeline.zh.md)。

同一助手消息中的多个调用由 agent loop 调度：

- `parallel` 调用进入有界滚动池；
- `exclusive` 调用形成屏障；
- 调度可以重叠，但策略处理、写入日志的结果和下一步骤上下文保持模型给出的顺序；
- 取消会停止补充新任务、排空已开始任务，并为未开始调用写入规范化的中止结果；
- 内部调度器失败保留已经记录的调用，不伪造普通工具结果。

这套设计同时满足吞吐量与可回放顺序。添加工具时应从 [`adding-a-tool`](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/cookbook/adding-a-tool.zh.md) 开始，并验证真实组合中的模型可见 transcript，而不是只测试执行函数。

## 11. 能力 Seam 与基础设施替换

项目把一项可替换能力定义为三个完整角色：Service Definition 声明提供方无关接口，Service Provider 连接具体实现，Consumer 把能力用于产品或暴露为模型工具。角色可以同包实现，但设计时必须同时考虑；只有接口或只有工具都不是完整能力 seam。

以 shell 为例，shell 包定义请求与结果，local 或 pwsh 提供方执行命令，`tool-bash` 或对应消费方暴露模型工具；本地 shell 又通过 subprocess 服务创建进程。文件系统、终端、LSP、Web、压缩、子智能体和工作流遵循同一模式。能力图的当前关系见 [Capability Seams](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/capability-seams.zh.md)。

设计新能力时按以下问题检查：

1. 提供方无关的请求、解析后 spec、结果和失败分类是什么？
2. 哪些默认值由拥有实现的 `resolve(request)` 明确决定？
3. 实现需要哪些生命周期、取消、并发和资源所有权规则？
4. 消费方如何把能力呈现给模型或用户，模型可见内容如何写入日志？
5. 默认组合挂载哪个提供方，专用部署如何替换？
6. 单元、真实入口 e2e 和 keyless snapshot 分别验证什么？

扩展插件应依赖定义包而不是具体提供方。Bundle 可以选择并组装提供方，但普通消费方不应通过导入本地实现锁死部署方式。

## 12. Web 的 Host/Client 架构

Web 表面由两个独立 Cordis 运行时组成。Host 运行在 Node.js 中，拥有 Agent、会话、文件系统、凭证和其他可信服务；Client 运行在浏览器中，拥有 shell、会话读模型、UI slot 和界面插件。两侧不能把 Cordis `Context` 合并到同一个 TypeScript program，因为相同上下文键在两侧对应不同服务，所以根工程保持 Host 与 Client 两个显式 aggregate。

跨端调用由 Typert 和 API Gateway 连接。Host 服务以受支持的 Remote 声明标记方法；Host 构建分析 TypeScript 类型，生成方法描述、Zod 编解码器和 Host-for-Client 声明。运行时 Gateway 校验输入、恢复作用域对象、调用 Host 服务并编码结果，Client 则获得类型化 remote proxy。WebSocket/HTTP 连接负责协议传输，业务包不手写平行 DTO 和客户端桩。

浏览器会话视图通过投影和增量事件维护。Host 保持权威日志与投影快照，Client runtime 组装 conversation、队列、工具树和工作区对象，UI 插件向稳定 slot 注册展示。`apps/web/src/main.ts` 只找到 DOM 根节点并启动 `AppWebEntry`，实际浏览器插件表由 Web bundle 和 client module 系统决定。

这套拆分形成清晰的信任关系：浏览器只获得显式发布的 Remote 方法和可传输数据；Host 对线上的 JSON 做运行时验证；同进程内部的类型化调用则信任 TypeScript。详细编程模型、生成步骤和运行时调用见 [API Gateway](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/api-gateway.zh.md)，浏览器插件关系见[客户端模块子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/client-modules.zh.md)。

## 13. 持久化、投影与派生数据

`dsh-session` 只拥有内存事件日志和 `session/event`、`session/flush` 等协议。持久化 seam 单独定义后端接口，JSONL 与 SQLite 插件消费事件并按批次写入。是否以及何时要求批次完全落盘，由独立的 checkpoint policy 决定，而不是隐藏在后端或 loop 中。

默认 checkpoint policy 在模型请求、顶层工具执行和下一步骤等关键位置请求 flush，确保即将产生外部副作用的操作已经有对应的持久事实。后端即使没有该策略也能工作，但进程崩溃时可能丢失批处理窗口中的尾部事件；专用部署可以明确选择不同策略。

投影把追加日志转换成适合查询的当前视图。Projection definition 是纯折叠逻辑，projection cache 只保存可重建的加速结果，并遵循“日志领先、缓存跟随”：检查点先 flush 日志，再写缓存，因此崩溃最多让缓存落后，不能让它领先权威事实。标题、统计、全文查询和客户端 conversation 也属于派生读模型，不应反向成为会话真源。

理解数据路径时可画出三类状态：权威的 SessionEvent 日志、可重建投影、会话外的设置/凭证/工作区等独立领域数据。不要把所有持久数据都塞进会话事件，也不要用普通存储替代需要按轮次重放的模型上下文。

## 14. 安全、权限与故障处理

Harness 将“是否允许操作”和“如何约束执行”分开。Approval/permission 插件决定一次敏感请求是否获准；sandbox 服务把策略解析为进程约束；filesystem policy 约束文件读写；credential 服务在执行时解析引用而不是把秘密写入配置或发送到 Client。安全依赖必须在缺失或无法执行时快速失败，不能静默退化到更宽权限。

子进程、终端和沙箱代码遵循统一防御模式：先登记所有权，再启动异步工作；关闭入口先阻止新工作，再取消或排空已有工作；清理需要幂等；并发结果要有明确提交顺序；超时与取消保留结构化原因。启动期间任何配置项失败时，`boot()` 会释放部分 Context，使已经占用终端或文件观察器的插件获得正常 teardown。

环境配置也体现信任分层。继承进程环境的值优先于项目 `.env` 和 Harness-home `.env`；会影响进程启动、模块解析、网络信任或 `DSH_*` 行为的变量只能由启动环境提供。凭证配置保存引用，实际值由 credential provider 在操作点解析。

修改生命周期、并发、子进程或 teardown 前必须阅读[防御性模式](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/defensive-patterns.zh.md)。沙箱和审批的精确类型、失败分类分别由[沙箱子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/sandbox.zh.md)与[审批子系统](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/approval.zh.md)维护。

## 15. 构建、类型与质量体系

仓库把源码平面和产物平面分开。静态检查及多数测试通过 TypeScript `paths` 解析到 `src`，应在干净树上工作；真正消费 `lib/` 的检查会显式依赖构建。根 `tsconfig.json` 只是 solution，不包含一个全仓 program；Host 与 Client 各自有 aggregate，以避免两侧 Cordis 上下文 declaration merging 冲突。

构建顺序先生成 Host 类型和产物，再进行 Client 类型检查与产物生成，最后构建 Web。Typert 只在 Host 构建阶段分析业务声明并产生跨端描述，Client 随后消费这些生成结果。这解释了为什么 `typecheck` 先执行 Host lib 阶段，也解释了为什么某些面向产物的检查不能在未构建工作树上直接运行。

测试不是一个层级替代所有层级：

| 层级             | 主要证明                                       | 不足以替代            |
| ---------------- | ---------------------------------------------- | --------------------- |
| 单元/包测试      | 类型、状态机分支、纯投影和局部失败语义         | 真实插件装配          |
| 集成与 e2e       | Loader、真实提供方边界、进程和协议行为         | 稳定的模型 transcript |
| Keyless snapshot | 可运行应用中的模型输入、工具调用和用户可见输出 | 真实外部 API 行为     |
| Real-API e2e     | 提供方协议和真实推理行为                       | keyless CI 回归信号   |
| 静态门禁         | 包约束、生成物新鲜度、JSDoc、文档链接与格式    | 运行时行为            |

对非平凡的模型或产品可见改动，包测试之外还要通过真实示例更新 keyless snapshot。检查选择以改动影响面为准，不默认运行全仓套件；[测试策略](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/testing.zh.md)解释各层职责，[开发指南](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/development.zh.md)解释构建和日常命令。

## 16. 推荐学习路线

下面是一条从框架概念到独立扩展的渐进路径。每一步都有一个可观察结果，适合边读边验证。

### 阶段一：建立 Cordis 心智模型

完成 [Cordis 教程](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/cordis-tutorial/index.zh.md)的前六节，亲手写一个提供服务、监听事件并通过 effect 撤销注册的小插件。目标是能够解释 `Context`、fiber、service、event、waterfall、effect 与 `inject` 的关系。

随后阅读 [`packages/core/system-prompt/src/index.ts`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/core/system-prompt/src/index.ts)。它规模适中，同时展示服务、注册 disposer、作用域提供方和组装过程，是进入产品源码的好入口。

### 阶段二：读懂实际组合

运行两个 `--dump-config` 命令，对照 [`dsh-base`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/base/cordis.patch.yml)、[`dsh-web-app`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/web-app/cordis.patch.yml)和[`dsh-headless`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/bundle/headless/cordis.patch.yml)。选择一个配置项，从 id 追到包名、`package.json`、`src/index.ts`、README 和 invariant。

目标是能够回答：这个插件由哪一层插入、依赖什么服务、向上下文贡献什么、卸载时撤销什么、Web 与 headless 是否都挂载它。

### 阶段三：跟踪一轮模型请求

从 [`ReactLoopAgent`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/core/agent-loop/src/agent.ts) 的 `send()` 开始，依次跟踪 `wakeDriver()`、`turn()`、`preStep()`、`step()` 和 `buildRequest()`。同时打开[生命周期图](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/agent-lifecycle.zh.md)和[事件矩阵](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/event-producer-consumer.zh.md)，记录每个事件是持久事件还是实时事件。

目标是能够解释一个工具调用为什么通常产生多个步骤、steer 如何加入下一步骤、取消为何仍然形成闭合的轮次，以及下一次请求为何从日志推导历史。

### 阶段四：跟踪一个工具

选择文件读取或 shell 工具，从工具定义追到能力服务、具体提供方和 bundle 配置项，再回到 [`executeToolCalls`](https://github.com/deepseek-ai/deepseek-harness/tree/master/packages/core/agent-loop/src/tool-calls.ts) 查看调度。使用[工具目录](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/tool-catalog.zh.md)核对 schema 和展示意图。

目标是能够画出 Definition、Provider、Consumer、权限策略、checkpoint 和 SessionEvent 的完整路径，而不是只找到工具的 `execute()`。

### 阶段五：跟踪 Web 往返

从一个 Client remote 调用出发，查找生成的 remote 描述、Client proxy、Gateway handler、Host 服务方法和返回投影。对照 [API Gateway](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/api-gateway.zh.md)理解 JSON 校验、scope 恢复和事件下行。

目标是能够判断一段代码属于 Host、Client 还是共享协议，并知道应把类型加入哪个 compiler face，而不是让两侧 Context 进入同一 program。

### 阶段六：完成一个最小扩展

优先选择低风险练习：新增一个不产生外部副作用的提示词 section、一个只读工具，或一个已有 seam 的替代测试提供方。按对应实操手册更新包 README/JSDoc、真实示例和最小测试。

目标是让改动通过所需的类型检查、聚焦测试、snapshot 或文档门禁，并能说明每个检查覆盖了哪条行为，而不是以“全套通过”代替设计论证。

## 17. 常见误区

| 误区                           | 更准确的理解                                                           |
| ------------------------------ | ---------------------------------------------------------------------- |
| `agent-loop` 是不可替换内核    | 它是 `Agent` 接口的默认驱动；新行为通常接入事件或能力 seam。           |
| 目录里有包就表示运行时启用了它 | profile/bundle/patch 组合后的配置树才决定挂载结果。                    |
| Session 只是聊天消息数组       | 它是包含生命周期、流分片、工具和插件事件的追加式事实日志。             |
| 所有事件都应持久化             | 只有跨重载需要保留且属于会话事实的内容进入 `SessionEventMap`。         |
| 工具就是 schema 加执行函数     | 工具还包含作用域、策略 waterfall、并发模式、日志结果和 UI 展示意图。   |
| Provider 可以决定所有默认值    | 部署可变值必须是配置；请求到 spec 的默认解析由拥有实现的明确步骤完成。 |
| 浏览器可以直接复用 Host 服务   | Client 只通过发布的 Remote 和投影访问 Host，线上数据需运行时校验。     |
| 持久化后端自然决定 flush 时机  | 后端负责落盘，checkpoint policy 独立决定关键持久性屏障。               |
| 单元测试通过就证明产品行为     | 插件装配、模型 transcript、真实协议和产物消费分别需要对应层级证据。    |
| 修改配置可以依赖静默回退       | 自包含误配置应在加载时失败，缺失引用应在最早可解析点失败。             |

## 18. 源码导航与继续阅读

按问题选择真源可以显著减少搜索范围：

| 想回答的问题                           | 首选资料                                                                                                                                                                                                                                                                                                                                         |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 系统如何组合、一次轮次如何运行？       | [架构总览](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/architecture.zh.md)、[Agent 生命周期](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/agent-lifecycle.zh.md)                                                                                                                                        |
| 某个类型、事件或服务的精确定义是什么？ | [子系统参考](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/subsystems/README.zh.md)                                                                                                                                                                                                                                           |
| 哪个插件生产或消费某事件？             | [事件矩阵](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/event-producer-consumer.zh.md)                                                                                                                                                                                                                                       |
| 哪个包依赖哪个包？                     | [模块依赖图](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/module-graph.zh.md)                                                                                                                                                                                                                                                |
| 默认应用实际挂载哪些配置项？           | [CLI 组合图](https://github.com/deepseek-ai/deepseek-harness/tree/master/apps/cli/composition.zh.md)与 `--dump-config`                                                                                                                                                                                                                           |
| 某工具给模型什么 schema？              | [工具目录](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/tool-catalog.zh.md)                                                                                                                                                                                                                                                  |
| 某配置字段如何设置？                   | [配置目录](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/config-catalog.zh.md)与所属包 README                                                                                                                                                                                                                                 |
| 为什么选择了当前设计？                 | `.agents/notes/implemented/` 下的当前 Agent Note                                                                                                                                                                                                                                                                                                 |
| 如何新增包、工具或适配器？             | [实操手册](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/cookbook/adding-a-package.zh.md)、[工具手册](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/cookbook/adding-a-tool.zh.md)、[LLM 适配器手册](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/cookbook/adding-an-llm-adapter.zh.md) |
| 应运行哪些验证？                       | [测试策略](https://github.com/deepseek-ai/deepseek-harness/tree/master/docs/testing.zh.md)与根 `AGENTS.zh.md` 的 relevant checks 规则                                                                                                                                                                                                            |

完成上述路线后，学习者应能用同一套模型解释 CLI、Web、ACP 和 SDK 表面：它们选择不同组合和传输方式，但共享 Cordis 生命周期、Agent 接口、会话事实日志、能力 seam 与显式边界。真正掌握本项目的标志不是记住包名，而是能从一个可见行为反向定位它的组合配置、服务所有者、事件记录、提供方实现和验证层级。
