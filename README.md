# 心理健康AI助手（后端）

基于大模型的心理健康服务系统后端，覆盖 AI 情绪陪伴、心理咨询预约（人工）、知识库、情绪记录与运营看板。前端为独立的 Vue 3 工程，不在本仓库内。

> **项目来源**：本项目基于公开的心理健康助手项目二次开发。原版后端只有 3 个 Controller、5 个业务接口（注册、登录、当前用户、创建会话、SSE 对话），仅 AI 对话可用。本仓库在其基础上扩展为 16 个 Controller、59 个业务接口，并补充了数据库初始化脚本。具体差异见 [二次开发说明](#二次开发说明)。

## 功能

**AI 能力**
- AI 心理咨询：基于 SSE 的流式对话，逐字返回，支持危机话术拦截
- 结构化情绪分析：对话结束后异步分析用户情绪，输出情绪类型/强度/风险等级/建议
- 危机干预：敏感词命中后调用大模型评级，高风险实时推送求助热线并通知全部管理员

**心理咨询业务**
- 咨询师入驻申请与后台审核，审核通过自动升级为咨询师角色
- 套餐管理（上下架）、预约下单、模拟支付、取消与退款
- 退款金额按预约状态与距开始时间动态计算，管理员审核时重算
- 人工咨询聊天：预约确认后在时间窗内可对话，支持增量拉取
- 站内通知：预约、退款、危机、审核四类

**内容与数据**
- 知识库：分类、文章增删改查、封面图上传，前台仅展示已发布文章
- 情绪日记：每日一条，重复提交覆盖；后台可查看并展示 AI 分析结果
- 数据看板：用户/日记/会话统计与近 7 天情绪、活跃度趋势

## 技术栈

| 层次 | 选型 | 版本 | 用途 |
|---|---|---|---|
| 运行时 | Java | 17 | LTS，Spring Boot 4 最低要求 |
| 框架 | Spring Boot | 4.1.1 | Web、校验、自动配置 |
| AI | Spring AI（OpenAI 兼容协议） | 2.1.0-M1 | 接入 DeepSeek，流式对话 + 结构化输出 |
| 持久层 | MyBatis-Plus | 3.5.17 | 单表 CRUD、分页插件、手写 SQL 混用 |
| 数据库 | MySQL | 8.0 | 字符集 utf8mb4 |
| 安全 | Spring Security + JWT | 7.1 / java-jwt 4.4.0 | 无状态鉴权，三角色 |
| 密码 | BCrypt | Spring Security 内置 | 自带盐、成本可调 |
| 工具 | Hutool | 5.8.48 | JSON、字符串、日期 |
| 前端 | Vue 3 + Vite + Element Plus + ECharts | — | 独立仓库 |

**选型说明**

- **Spring AI 而非直接发 HTTP 调模型**：统一了 `ChatClient`、消息记忆和流式返回（`Flux<String>`），换模型只改 `application.yml` 的 base-url 和模型名，代码不用动。
- **MyBatis-Plus 而非 JPA**：数据表来自历史版本，字段命名和约束不统一，需要 Wrapper 拼条件与手写聚合 SQL 混用；JPA 的实体映射在这种库上反而更费劲。
- **Spring Security + JWT 而非 Session**：前后端分离、接口无状态，token 里带角色，服务端不用存会话。代价是角色变更必须重新登录（见下方注意事项）。
- **SSE 而非 WebSocket**：消息是服务端单向推送，够用且实现简单；前端用 `fetchEventSource` 而不是原生 `EventSource`，因为后者不支持自定义请求头，没法带 JWT。
- **BCrypt 而非 MD5/SHA**：单向哈希、每次盐随机、成本因子可调，暴力破解成本高。校验必须用 `matches()`，不能用字符串比较。

## 项目结构

```
├── db/init.sql                  # 数据库初始化（15 张表 + 种子数据）
├── docs/                        # 接口文档与 OpenAPI 描述
├── src/main/java/com/example/aispring/
│   ├── AiService/               # 提示词、AI 会话服务
│   ├── controller/              # 接口层（16 个 Controller）
│   ├── service/                 # 业务层（含 service/engine 退款规则引擎）
│   ├── mapper/                  # MyBatis-Plus Mapper
│   ├── entity/                  # 实体
│   ├── DTO/                     # 入参/出参
│   ├── config/                  # 安全、静态资源、启动建表
│   └── util/                    # JWT、统一响应
└── src/main/resources/sql/      # 各模块增量建表脚本
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+（仓库自带 `mvnw`，可不装）
- MySQL 8.0+
- Node.js 18+（仅前端需要，本仓库不必装）

### 2. 初始化数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < db/init.sql
```

脚本会创建 `mental_health_assistant` 库、15 张表，并写入最小种子数据（3 个套餐、4 个知识分类、1 个管理员账号）。
**不包含任何用户真实数据**，后续注册的账号、写的日记都在你自己的库里。

### 3. 配置

参考根目录 `application-example.yml`，修改 `src/main/resources/application.yml` 中三处：

| 配置 | 说明 |
|---|---|
| `spring.datasource.url` / `username` / `password` | 你的 MySQL 地址与账号，库名默认 `mental_health_assistant` |
| `spring.ai.openai.api-key` | 大模型 API Key（DeepSeek 或任意 OpenAI 兼容服务），不填则 AI 相关功能不可用 |
| `jwt.secret` | JWT 签名密钥，自行改成一串随机字符 |

> `useSSL=false&allowPublicKeyRetrieval=true` 是本地开发配置，MySQL 8 的 `caching_sha2_password` 认证需要它。生产环境请改用 SSL。

### 4. 启动后端

```bash
./mvnw spring-boot:run
```

默认端口 **1236**。首次启动会自动补齐 `knowledge_*`、`emotion_diary` 等表（幂等，不影响已有数据），并在控制台打印各表字段清单。

### 5. 关于前端

前端为独立的 Vue 3 工程，**不在本仓库内**。本仓库只提供后端服务（端口 1236），用 Postman / Apifox 或任何 HTTP 客户端都能直接调用全部接口。

若要对接前端，需保证它指向本地后端：

```js
// vite.config.js
proxy: { '/api': { target: 'http://localhost:1236', changeOrigin: true } }
```

```js
// src/config/index.js —— 上传封面图等静态资源的访问前缀
fileBaseUrl: 'http://localhost:1236'
```

### 6. 登录

```
管理员：admin / 123456
```

- 管理员可进入 `/back/*` 后台：知识文章、咨询师审核、套餐、退款审核、危机干预、通知、数据看板
- 普通用户在首页注册，可预约咨询、写情绪日记
- 咨询师需先在「成为咨询师」提交申请，管理员审核通过后**重新登录**才有咨询师权限（角色写在 JWT 里）

## 数据库说明

完整建表语句见 [`db/init.sql`](db/init.sql)（含索引、注释与种子数据，可直接执行）。

共 15 张表，分四组：

- 账号与会话：`user`、`consultation_session`、`consultation_message`
- 咨询业务：`counseling_counselor`、`counseling_package`、`counseling_appointment`、`counseling_order`、`counseling_payment`、`counseling_refund`、`counseling_chat_message`
- 危机与通知：`counseling_crisis_record`、`counseling_notification`
- 内容：`knowledge_category`、`knowledge_article`、`emotion_diary`

## 主要接口

| 模块 | 路径前缀 | 接口数 | 说明 |
|---|---|---|---|
| 用户 | `/api/user` | 4 | 注册、登录、当前用户、登出 |
| AI 对话 | `/api/psychological-chat` | 6 | 建会话、SSE 流式对话、会话列表/历史/删除/情绪面板 |
| 知识库 | `/api/knowledge` | 7 | 分类树、文章增删改查、发布状态 |
| 文件 | `/api/file` | 1 | 封面图上传 |
| 情绪日记 | `/api/emotion-diary` | 3 | 提交（当日覆盖）、后台分页、删除 |
| 咨询师 | `/api/counselor` | 5 | 申请、可用列表、后台分页、审核 |
| 套餐 | `/api/package` | 6 | 增删改查、上下架 |
| 预约 | `/api/appointment` | 8 | 创建、我的、咨询师的、确认/拒绝/取消/完成 |
| 订单 | `/api/order` | 3 | 我的订单、详情、取消 |
| 支付 | `/api/payment` | 2 | 模拟支付、支付记录 |
| 退款 | `/api/refund` | 5 | 预览、申请、我的、后台分页、审核 |
| 人工聊天 | `/api/counseling-chat` | 2 | 发送、增量拉取 |
| 危机干预 | `/api/crisis` | 3 | 分页、详情、标记处理 |
| 通知 | `/api/notification` | 3 | 我的通知、未读数、标记已读 |
| 数据看板 | `/api/data-analytics` | 1 | 总览统计 |

完整字段见 `docs/心理咨询模块-接口文档.md`。

## 实现要点与注意事项

### AI 与流式输出

- **用 POST + SSE，不用原生 `EventSource`**。原生 `EventSource` 只能发 GET、不支持自定义请求头，JWT 无处可放。前端改用 `@microsoft/fetch-event-source`，token 走 `Authorization` 头。
- **发具名事件**：数据帧 `event: message`，结束帧 `event: done`。前端靠事件名判断流是否结束，而不是靠内容里有没有特殊标记。
- **输出节流**：`delayElements(Duration.ofMillis(50))`，避免 token 密集到达导致前端频繁重渲染。
- **情绪分析必须异步**。分析一次要 2~5 秒，若同步执行会把"出字"和"保存日记"都拖慢。现在用 `CompletableFuture` 在流结束后触发，失败只打 warn，主流程不受影响。
- **提示词约束模型只输出 JSON**，解析时再做一次规范化（分数 clamp 到 0-100、等级 clamp 到 0-3、建议最多 5 条）。模型多返回或漏返回都不会把前端搞崩；完全解析失败时降级为中性值。

### 鉴权与角色

- **JWT 是无状态的，角色写在签发那一刻**。咨询师审核通过后 `userType` 改成 3，但旧 token 里还是 1，必须重新登录才能进工作台。这是设计使然，不是 bug。
- **公开只读接口不能整个跳过过滤器**。早期做法是"公开路径直接 `return true` 跳过"，结果管理员带着 token 访问 `/api/knowledge/article/page` 时上下文为空、被当成匿名访客，看不到草稿，状态筛选也失效。现在改成"有 token 就解析，没 token 才放行"。
- **判断是否匿名不能用 `getAuthentication() == null`**。Spring Security 的 `AnonymousAuthenticationFilter` 会给匿名请求塞一个匿名 token，这个判断永远不成立。要按 `ROLE_ANONYMOUS` 或具体角色判断。
- **接口级权限用 `@PreAuthorize("hasRole('1')")`** 这类注解，1 普通用户 / 2 管理员 / 3 咨询师，与 `UserType` 枚举一致。

### 数据一致性

- **`user.phone` 上有唯一索引**，未填手机号时必须存 `NULL` 而不是空字符串。MySQL 的唯一索引允许多个 `NULL`，但 `''` 只能出现一次——否则第二个不填手机号的用户就注册失败（代码里已做归一）。
- **`emotion_diary` 上有 `(user_id, diary_date)` 唯一键**，所以日记是"当日覆盖"而不是追加。直接 `INSERT` 会在同一天第二次提交时撞键。
- **退款金额在审核时刻重算**，不是申请时刻。规则：未支付/已取消不可退；预约待确认全额退；已确认且距开始 >24h 全额、2~24h 退 50%、<2h 不可退；已结束/已取消不可退。审核与申请之间隔了几小时，按申请时刻算会多退。
- **金额一律用 `BigDecimal`**，不用 `double`。

### 数据库兼容

- **启动时执行 `sql/*.sql` 并补齐缺失列**（只加列，不删不改类型）。历史库的字段可能比代码少（比如早版本没有 `sort`、`author_name`），`CREATE TABLE IF NOT EXISTS` 不会补列，所以要单独 `ALTER`。
- **脚本必须幂等**：`CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE` / 仅当表为空时插入，重复启动不会产生重复数据。
- **MySQL 5.7+ 默认开 `only_full_group_by`**，`GROUP BY` 的表达式必须和 `SELECT` 里的非聚合列完全一致（`GROUP BY DATE(t)` 配 `SELECT DATE_FORMAT(t,...)` 会被拒）。
- **MySQL 8 的 `caching_sha2_password`** 在非 SSL 连接下需要 `allowPublicKeyRetrieval=true`，否则启动直接报 `Public Key Retrieval is not allowed`。

### 其他

- **上传文件的目录要能被匿名读取**。图片落在 `uploads/`，通过 `/uploads/**` 映射为静态资源，安全配置里必须放行该路径的 GET，否则封面图 401。
- **全局异常处理要把原因带回前端**。Spring 默认 `server.error.include-message=never`，500 响应体只有 `Internal Server Error`，前端只能弹"请求失败"。现在数据库异常、参数校验、未知异常都会返回可读原因。
- **`knowledge_article` 主键是 `VARCHAR(36)`**，因为前端新建文章时用 `crypto.randomUUID()` 生成 ID，不是自增数字。

## 二次开发说明

**原版状态**（本仓库第一次提交之前）：3 个 Controller——`User`（登录、注册、当前用户）、`PsychologicalChat`（创建会话、SSE 对话）、`Test`（健康检查），共 5 个业务接口。没有知识库、没有情绪记录、没有咨询业务，前端对应的页面全部 404。

**本仓库新增的内容**：

| 模块 | 新增接口 | 关键实现 |
|---|---|---|
| 用户与鉴权 | +1（登出） | `UserType` 增加咨询师角色，改为三级鉴权 |
| AI 咨询 | +4 | 会话列表、历史消息、删除会话、情绪面板；流式对话按用户隔离 |
| 情绪分析 | — | 新增 `EmotionAnalysisService`，对话与日记双场景落库 |
| 知识库 | +7 | 分类树、文章 CRUD、发布状态、封面图上传与静态访问 |
| 情绪日记 | +3 | 当日覆盖写，后台分页与删除 |
| 数据看板 | +1 | 总量统计 + 近 7 天趋势（缺失日期补零） |
| 咨询师 | +5 | 申请、审核、可用列表 |
| 套餐 | +6 | 增删改查、上下架 |
| 预约 | +8 | 创建即生成未支付订单，确认/拒绝/取消/完成 |
| 订单与支付 | +5 | 模拟支付、订单详情、取消 |
| 退款 | +5 | 规则引擎（按状态 + 时间窗）、预览、审核 |
| 人工聊天 | +2 | 增量拉取，限制在预约时间窗内发送 |
| 危机干预 | +3 | 敏感词 + 模型评级，HIGH 通知全部管理员 |
| 通知 | +3 | 四类站内消息、未读数、标记已读 |

**保留原样的部分**：原 5 个接口的路径与返回结构未变；提示词文件、实体定义、JWT 配置、密码加密方式沿用原实现。

**改动过原有逻辑的地方**（如实列出）：
- `JwtAuthenticationFilter`：公开路径的处理方式从"整个跳过"改为"有 token 就解析"，并补了 token 过期时的异常捕获
- `UserRegisterCommandDTO`：手机号正则允许空串，`UserService` 中空手机号归一为 `NULL`
- `GlobalExceptionHandler`：补充唯一键冲突、数据库异常、未知异常的处理器

## 已知限制

- **支付为模拟支付**，未接入真实支付网关；可通过 `payment.mock.fail-rate` 模拟失败路径
- **人工咨询聊天为 3 秒轮询**，非 WebSocket；AI 对话才是 SSE 流式
- **情绪分析依赖大模型**，未配置 API Key 或调用失败时，情绪面板显示中性兜底值
- **未编写单元测试**，目前靠手工回归
- **数据库结构来自历史版本**，`db/init.sql` 是导出的快照，没有 Flyway/Liquibase 之类的迁移管理
