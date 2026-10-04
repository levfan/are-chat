# ADR-0001：按限界上下文分包 + 单模块 Maven，不拆多 module

- 状态：已采纳（2026-10-04）
- 相关：`docs/ddd/01-context-map.md`、`docs/ddd/02-layering.md`

## 背景

are-chat 现在是单 Maven 模块、11 个按技术角色切的包（auth/im/couple/system/upload/…），couple 包内 74 个文件平铺。实测有 4 个包级循环依赖（`common↔config`、`auth↔im`、`im↔room`、`auth↔couple`），任何一处改动都可能波及全库；同时它是私有化部署产物（`deploy/` + `are-chat-1.0.0.tar` + 单一 Dockerfile），一次构建出一个可执行 jar。

## 决策

1. 包结构改为**按限界上下文**（couple/messaging/identity/platform/filestorage）+ `sharedkernel` + `bootstrap`，每个上下文内部再分 `api/application/domain/infrastructure` 四层。
2. **不拆多 Maven module**，仍是单模块；上下文边界靠包结构 + `ArchitectureGuardTest` 源码守卫维持，不靠 Maven 依赖隔离。
3. 根包 `com.smart.chat` 与 `SmartChatApplication` 位置不动。

## 理由与取舍

- 拆多 module 能拿到编译期强制边界，但要重写 pom、改构建与部署链路（tar 包结构、Dockerfile、`spring-boot-maven-plugin` 打包），且会把"一个上下文一个 module"的粒度承诺写死——而本轮只有 couple 值得完整改造，其余四个上下文只是想有个骨架。为不存在的团队规模付部署链改造成本，不值。
- 源码守卫（零新依赖，见 ADR-0004）能拦住"新代码继续越界"这一件事，已经达成 90% 的收益。
- 根包不动是有意的：`@SpringBootApplication` 组件扫描、`@ConfigurationPropertiesScan`、`@MapperScan(basePackages="com.smart.chat")` 三处以根包为扫描面，保持它们不变，才能让 Phase A 成为**行为不变的纯移动**，用编译与既有 158 测试直接证明没改坏。

## 后果

- 正面：目录即架构；新增功能时"该放哪一层"有唯一答案；循环依赖变成可被测试拦住的显式违规。
- 负面：边界不是编译硬约束（同 module 内可以互相 import），必须靠守卫测试与 code review 兜；未来若真要拆 module，包结构已经是正确切法，届时是纯 pom 工作。
