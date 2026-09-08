# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

面向希望把现实承诺转化为可执行任务与可追溯成长的中文 Android 用户。用户在日常生活中创建、筛选、完成、暂缓、撤销和复盘任务，并希望成长反馈有力量但不带羞辱性。

## Product Purpose

地球 Online 是一款离线优先的单人任务管理器，把现实行动解释为任务、修为、技能、成就与主线推进。成功意味着用户能看清当下最值得执行的下一步，完成后得到可解释、可撤销、可追溯的结算，并长期保有自己的本地存档。

## Positioning

产品只有一份由现实行动形成的底层事实，地球原生、修仙等叙事体系只是可切换的解释器。切换体系可以改变名称、图标、配色、触觉和演出，但不能改变任务、奖励、债务、完成身份或历史。

## Operating Context

用户通过指挥台查看等级、今日完成数、时间与精力筛选、Next Quest 推荐和待重审任务；通过任务、主线、角色、冒险日志和设置完成日常管理、成长回顾与本地存档导入导出。日期与每日结算遵循玩家保存的时区。

## Capabilities and Constraints

- 原生 Kotlin 与 Jetpack Compose Android 应用，最低 Android 8.0。
- 离线单人运行；无账号、云同步、AI、分析 SDK、广告、支付或商城。
- Room 是权威状态。XP 由有效完成记录推导，完成身份是 `questId + occurrence`，撤销与重做保留奖励和技能快照。
- 完成与审计事件在同一事务中变更；导入须完整校验后再以单一事务替换。
- 任务契约使用日期级截止表达，不承诺分钟级准时调度；提醒由 WorkManager 尽力执行。
- 叙事可以有明确代价与恢复路径，但不得使用随机惩罚、隐藏扣除、无限累积或羞辱性表达。
- 所有核心动作遵循 Android 原生返回、触控目标、系统 Insets、屏幕尺寸和辅助功能语义。

## Brand Commitments

- 产品名为“地球 Online”，母品牌锚点是 Planet Mark 与“唯一服、单向时间、现实结算”。
- 地球母品牌跨叙事体系保持可识别；局部世界观不能替换底层事实或母品牌。
- 行动反馈坚定、直接，失约反馈诚实但中立；始终同时给出可执行的恢复路径。
- 当前 v2 方向采用“宗门任务榜 × 现代移动战令”，材料语言为黑漆玉简、稀缺金箔、朱砂劫痕和星轨蚀刻。

## Evidence on Hand

- 已发布功能、边界与源码身份：`README.md`、`verification/STATUS.md`。
- 产品与架构事实：`docs/PRODUCT.md`、`docs/ARCHITECTURE.md`、`docs/PRIVACY.md`。
- 当前视觉系统：`DESIGN.md`、`.impeccable/design.json`。
- 已选方向稿与生成提示：`.impeccable/mocks/decision/assigned.webp`、`.impeccable/mocks/decision/assigned.webp.json`。
- 构图中的任务名称、玩家等级、修为、债务、日期与数量均为合成示例，不代表真实用户数据、产品默认值或已验证指标。

## Product Principles

- 底层事实唯一，叙事解释可切换。
- 下一步行动比装饰更醒目，完成与恢复路径始终可见。
- 结算强烈但可解释、可撤销、可追溯。
- 代价在承诺前公开，失约后不羞辱用户也不制造死局。
- 离线、隐私和数据完整性优先于扩张功能。

## Accessibility & Inclusion

界面需保持 Android 原生语义、至少 48dp 触控目标、可读对比度、非纯颜色状态表达，并适应窄屏、宽屏、大字体、系统栏、键盘与减少动态效果。修仙叙事不得依赖难读书法体或晦涩术语才能完成任务。
