# ADR-0004：分层守卫用自研零依赖源码扫描，不引 ArchUnit

- 状态：已采纳（2026-10-04）
- 相关：`docs/ddd/02-layering.md` 第五节、`docs/ddd/03-phase-plan.md` 第三节

## 背景

DDD 分层要能被机器拦住才不会被写回去。常规做法是 ArchUnit。但两个硬事实：

1. 本机 Maven 离线仓库里**没有** ArchUnit（`find ~/.m2/repository -maxdepth 4 -iname "*archunit*"` 为空），而本项目所有验证都在 `mvn -o`（离线）下跑；新增依赖意味着必须联网取包，而「不擅自装系统/构建依赖」是既有口径。
2. ArchUnit 判的是字节码上的类依赖，对"这个 import 属于哪一层"的表达要靠包名约定——我们本来就有包名约定。

## 决策

写 `src/test/java/com/smart/chat/ArchitectureGuardTest`，用 JDK 自带能力直接读 `src/main/java` 源码：

- 逐文件解析 `package` 与 `import`（正则 + 词法切分，判据同 `.tmp-audit/ddd-deps.mjs`，已在裁剪轮被验证可用）；
- 断言四条规则：`domain` 纯净、`infrastructure` 不 import 本上下文 `api`、上下文之间只能经端口/sharedkernel、`bootstrap` 不被 import；
- 未改造上下文的既存违规写进**显式 allowlist**（逐条「谁 import 了谁」），且断言 allowlist 长度只能减不能增。

## 后果与止损

- 正面：零新依赖；纯源码扫描意味着**不依赖构建产物顺序**，`mvn -o test` 里稳定可跑；报错信息直接给文件与行。
- 负面：解析不如字节码精确（例如 `import static`、注释里的假 import）——用「注释与字符串先剥离再扫」处理，并把守卫自己做成**可证伪**的：临时插一条违规 import 必须红，去掉必须绿（这条验证结果记进 `03-phase-plan.md` 的验收台账）。
- 若将来允许联网取包，可以直接换成 ArchUnit，规则一条不改（守卫接口不变，实现替换）。
