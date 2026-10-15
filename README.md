# 医院智慧就诊预约平台（medical_cloud）

面向医院门诊场景的智慧就诊预约系统，采用微服务架构拆分为用户、医生、患者、科室、药品、预约、消息、文件等 8 个服务，覆盖患者建档、号源预约、医生接诊、停诊改约与站内通知全流程，并对高并发挂号、服务间调用故障等场景做了稳定性治理。

- GitHub：<https://github.com/tianCode-hub>
- Gitee：<https://gitee.com/tian-tian-zhen-zhen>

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | 运行环境 |
| Spring Boot | 3.2.10 | 基础框架 |
| Spring Cloud | 2023.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 微服务治理（Nacos / Sentinel） |
| Spring Cloud Gateway | - | API 网关 |
| Spring Security | - | 方法级权限控制（RBAC） |
| MyBatis-Plus | 3.5.x | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |
| Redis | - | 缓存 / Token / 验证码 / 幂等 / 分布式锁 |
| Redisson | - | 分布式锁（防号源超卖） |
| RabbitMQ | - | 消息队列（异步通知） |
| OpenFeign + LoadBalancer | - | 服务间调用与负载均衡 |
| Sentinel | - | 限流 / 熔断 / 降级 |
| 阿里云 OSS / MinIO | - | 文件存储（药品图片等） |
| JWT (java-jwt) | 4.4.0 | 无状态令牌认证 |
| Knife4j | 4.5.0 | API 文档 |
| Hutool | 5.8.28 | 工具库 |

### 前端（独立仓库 medical-vue）

| 技术 | 说明 |
|------|------|
| Vue 3 + Vite | 前端框架与构建 |
| Element Plus | UI 组件库 |
| Pinia + Vue Router | 状态管理与路由 |
| Axios | HTTP 客户端 |

## 服务模块与端口

| 服务 | 端口 | 职责 |
|------|------|------|
| medical-web-gateway | 51600 | Web 端统一网关：路由转发、登录校验、白名单 |
| medical-uniapp-gateway | 51601 | 移动端统一网关 |
| medical-user | 51602 | 用户认证：账号/验证码登录、会话管理、短信 |
| medical-doctor | 51603 | 医生管理：多条件查询、在职状态、停诊请假 |
| medical-patient | 51604 | 患者档案管理：建档、绑定登录用户、本人数据维护 |
| medical-medicine | 51605 | 药品管理：增删改查、图片上传（OSS） |
| medical-appointment | 51606 | 预约挂号：号源锁定、防超卖、取消预约 |
| medical-department | 51607 | 科室管理：自连接实现科室分级展示 |
| medical-message | 51608 | 消息中心：统一消费 MQ 落库、站内通知 |
| medical-fileUpload | 51609 | 文件服务：图片上传下载 |

## 系统架构

```
前端（medical-vue）
   │  HTTP
   ▼
网关 Gateway（登录校验 / 精确白名单 / 透传用户信息）
   │  lb:// 服务名（Nacos 注册发现 + LoadBalancer 负载均衡）
   ▼
业务服务：user / doctor / patient / medicine / appointment / department / message / fileUpload
   ├── Redis：token、验证码、幂等键、号源统计缓存、Redisson 分布式锁
   ├── RabbitMQ：预约成功 / 停诊改约通知（异步解耦，定时补偿兜底）
   ├── MySQL：业务数据落库（跨服务联表查询）
   └── OpenFeign：服务间远程调用（Sentinel 熔断 + fallback 降级兜底）
```

核心业务链路：患者建档 → 选科室/医生 → 查看号源余量 → 预约下单（分布式锁防超卖 + 幂等防重复）→ 医生端接诊；医生停诊时自动作废预约、释放号源并通知患者改约。

## 核心功能

- **预约挂号**：患者选择科室、排班医生完成挂号预约；Redisson 分布式锁解决高并发抢号超卖，Redis 幂等机制防止重复下单；当日预约不可取消；预约成功通过 RabbitMQ 异步推送患者与医生站内通知。
- **用户认证与权限**：账号密码 + 手机验证码双登录；JWT + Redis 双校验管理会话；验证码登录后立即失效并限制 60 秒发送频率，短信通过线程池并发发送；Spring Security 方法级权限控制，增删改仅管理员可用，患者医生仅可操作自身数据。
- **医生管理与停诊通知**：医生多条件查询与在职状态维护；紧急请假自动取消预约并释放号源，通过 RabbitMQ 通知患者并提供同科室可改约医生，定时补偿保障通知不遗漏。
- **科室管理**：科室信息增删改查与多条件查询，通过自连接实现科室分级展示。
- **患者档案管理**：档案增删改查，创建时自动绑定登录用户，非管理员仅可修改本人档案。
- **消息中心**：消息业务独立服务化，统一通过 RabbitMQ 消费落库，并发线程池异步处理，不阻塞核心业务。
- **药品管理**：药品信息增删改查，图片上传至阿里云 OSS，线程池并发处理，含文件名校验、大小限制与扩展名白名单。
- **服务高可用治理**：Sentinel 对预约下单核心接口限流熔断，Feign 客户端降级兜底防止服务雪崩；联表查询替代循环调用，解决跨服务 N+1 性能问题。

## 关键技术设计

| 问题 | 方案 |
|------|------|
| 高并发抢号超卖 | Redisson 分布式锁（key = 医生 + 日期）将「读余量 + 校验 + 写库」串行化，配合数据库事务双保险 |
| 重复下单 | Redis SETNX 幂等键，同一用户重复提交只处理一次 |
| 通知拖慢下单 | RabbitMQ 异步投递，消息服务统一消费落库，定时任务补偿漏发 |
| 消息可靠 | 生产者确认 + 持久化 + 手动 ack 三跳防护；消费端幂等去重 |
| 越权访问 | 方法级角色控制（@PreAuthorize）+ 业务级归属校验（操作前校验数据归属人） |
| 服务雪崩 | Feign fallbackFactory 降级兜底 + Sentinel 熔断限流，下游故障返回安全默认值 |
| 跨服务 N+1 查询 | LEFT JOIN 联表一次查完，显式列出字段避免同名列覆盖 |
| 缓存一致性 | 先更库再删缓存 + 短过期时间兜底（最终一致） |
| JWT 无法主动失效 | JWT + Redis 双校验，登出/踢人时删除 Redis 记录 |

## 项目结构

```
medical-smart-platform/
├── medical-gateway/              # 网关模块
│   ├── medical-web-gateway/      # Web 端网关（51600）
│   └── medical-uniapp-gateway/   # 移动端网关（51601）
├── medical-service/              # 业务服务集合
│   ├── medical-user/             # 用户服务（51602）
│   ├── medical-doctor/           # 医生服务（51603）
│   ├── medical-patient/          # 患者服务（51604）
│   ├── medical-medicine/         # 药品服务（51605）
│   ├── medical-appointment/      # 预约服务（51606）
│   ├── medical-department/       # 科室服务（51607）
│   ├── medical-message/          # 消息服务（51608）
│   └── medical-FileUpload/       # 文件服务（51609）
├── medical-model/                # 实体 / DTO / 常量
├── medical-feign-api/            # Feign 接口与 fallback 降级实现
├── medical-utils/                # 公共工具：JWT、Redis、拦截器、全局异常
└── pom.xml                       # 根 POM（统一版本管理）
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.x、Redis、RabbitMQ、Nacos 2.x
- 阿里云 OSS（或 MinIO）用于文件上传

### 1. 启动基础设施

按顺序启动 Nacos（默认 `127.0.0.1:8848`）、MySQL、Redis、RabbitMQ，并在 Nacos 或各服务 `application.yml` 中按实际环境调整连接信息。

### 2. 初始化数据库

创建数据库 `medical_cloud`（各服务共用），并按实体结构初始化业务表。数据库连接、OSS 凭证等敏感配置请通过配置中心 / 环境变量注入，禁止硬编码提交。

### 3. 启动服务

先启动 `medical-utils` 依赖的公共能力与注册中心，再依次启动网关和业务服务（推荐在 IDE 中运行各服务启动类）：

```
Nacos → 各业务服务（user / doctor / patient / medicine / appointment / department / message / fileUpload）→ 网关（web / uniapp）
```

### 4. 启动前端

```bash
cd medical-vue
npm install
npm run dev
```

## License

仅供学习交流使用，请勿用于商业用途。
