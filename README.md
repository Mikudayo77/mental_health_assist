# 心理健康AI助手

基于大模型的心理健康服务系统，包含 AI 情绪陪伴、心理咨询预约（人工）、知识库与情绪记录。
项目分前后端两个仓库，本仓库为后端服务。

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

| 层次 | 选型 |
|---|---|
| 后端 | Spring Boot 4.1.1 / Java 17 |
| AI | Spring AI（OpenAI 兼容协议，接入 DeepSeek） |
| 持久层 | MyBatis-Plus 3.5.17 / MySQL 8.0 |
| 安全 | Spring Security + JWT，三角色（普通用户 / 管理员 / 咨询师） |
| 前端 | Vue 3 + Vite + Element Plus + ECharts |
| 密码 | BCrypt |

## 目录结构

```
├── db/init.sql                  # 数据库初始化（15 张表 + 种子数据）
├── docs/                        # 接口文档与 OpenAPI 描述
├── src/main/java/com/example/aispring/
│   ├── AiService/               # 提示词、AI 会话服务
│   ├── controller/              # 接口层
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
- Node.js 18+（前端）

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

### 5. 启动前端

前端为独立仓库（待补充链接），启动后默认 **5173**。需要确认 `vite.config.js` 的代理指向后端：

```js
proxy: { '/api': { target: 'http://localhost:1236', changeOrigin: true } }
```

图片资源地址在 `src/config/index.js` 的 `fileBaseUrl`，同样指向 `http://localhost:1236`。

### 6. 登录

```
管理员：admin / 123456
```

- 管理员可进入 `/back/*` 后台：知识文章、咨询师审核、套餐、退款审核、危机干预、通知、数据看板
- 普通用户在首页注册，可预约咨询、写情绪日记
- 咨询师需先在「成为咨询师」提交申请，管理员审核通过后**重新登录**才有咨询师权限（角色写在 JWT 里）

## 数据库说明

共 15 张表，分四组：

- 账号与会话：`user`、`consultation_session`、`consultation_message`
- 咨询业务：`counseling_counselor`、`counseling_package`、`counseling_appointment`、`counseling_order`、`counseling_payment`、`counseling_refund`、`counseling_chat_message`
- 危机与通知：`counseling_crisis_record`、`counseling_notification`
- 内容：`knowledge_category`、`knowledge_article`、`emotion_diary`

注意 `user.phone` 上有唯一索引，因此**未填手机号时必须存 NULL 而不是空字符串**，否则第二个不填手机号的用户会注册失败（代码里已做归一处理）。

## 主要接口

| 模块 | 路径前缀 | 说明 |
|---|---|---|
| 用户 | `/api/user` | 注册、登录、当前用户、登出 |
| AI 对话 | `/api/psychological-chat` | 建会话、SSE 流式对话、会话列表/历史/删除/情绪 |
| 知识库 | `/api/knowledge` | 分类树、文章增删改查 |
| 情绪日记 | `/api/emotion-diary` | 提交、后台分页、删除 |
| 咨询师 | `/api/counselor` | 申请、列表、后台分页、审核 |
| 套餐 | `/api/package` | 增删改查、上下架 |
| 预约 | `/api/appointment` | 创建、我的、咨询师的、确认/拒绝/取消/完成 |
| 订单支付 | `/api/order`、`/api/payment` | 我的订单、详情、取消、模拟支付 |
| 退款 | `/api/refund` | 预览、申请、我的、后台分页、审核 |
| 人工聊天 | `/api/counseling-chat` | 发送、增量拉取 |
| 危机干预 | `/api/crisis` | 分页、详情、标记处理 |
| 通知 | `/api/notification` | 我的通知、未读数、标记已读 |
| 数据看板 | `/api/data-analytics` | 总览统计 |

完整字段见 `docs/心理咨询模块-接口文档.md`。

## 已知限制

- **支付为模拟支付**，未接入真实支付网关；可通过 `payment.mock.fail-rate` 模拟失败路径
- **人工咨询聊天为 3 秒轮询**，非 WebSocket；AI 对话才是 SSE 流式
- **情绪分析依赖大模型**，未配置 API Key 或调用失败时，情绪面板显示中性兜底值
- **未编写单元测试**，目前靠手工回归

## 说明

本项目在原始版本（仅含登录与 AI 对话）基础上二次开发，补齐了知识库、情绪日记、数据看板、心理咨询预约与支付、危机干预等模块的前后端实现。
