# v6 迭代验收记录（F250-F349，滚动）

> 口径同 v5：每批「实现→测试全绿→代码走查→提交推送」四步留痕；红线：不做照片/视频上传；迁移幂等；utf8mb4_bin 列 NOT NULL 无 DEFAULT；每批后端 ≥10、前端 ≥3 用例。
> 规格：docs/couple-features-v6.md。版本目标：v6 全部交付后 → 1.6.0-alpha 阶梯。

## 批次二十一（F250-F259 夫妻老黄历）

- 后端：V34 七表 + couple_anniversary 加 calendar_type/lunar_md 两列（ADD COLUMN IF NOT EXISTS 沿用 V21 先例）+ 17 文件（7 实体/7 Mapper/Bank/Service/Controller）+ 17 用例（4 个按日期假设跳过属预期），全量 398 绿。
- **当场修复①**：农历表初版凭记忆默写，springFestival(2024) 偏到 2022——从 npm solarlunar@3.1.0 官方 tarball 提取标准 1900-2100 压缩表（201 值）整体替换，并在 Bank 头注明出处；教训：**记忆型数据表一律从权威源取，锚点测试（2024/2025/2026 春节+中秋+端午）先写先跑**。
- **当场修复②**：lunarYearDays 位计数把「每大月+1」写成「±1」，348 基线下小月重复扣减致漂移——改 `(info & i) == 0 ? 0 : 1`。
- **当场修复③**：V34 三列（confirmed_by/wished_by/appended_by）utf8mb4_bin 列带 DEFAULT ''，H2 直接 42001——按红线去 charset；schema.sql 基线同步。教训：charset/DEFAULT 红线在写 SQL 时逐列过一遍，别等 H2 报错。
- 已知边界：节气日期用近似表（±1 天窗口跟风容忍微差）；ANNIVM 周年月无纪念日时该节日卡隐藏；长假窗口 60 天。
- 前端：待派单（CoupleAlmanac，建议挂 shared/daily 或 rituals 子页签）。
