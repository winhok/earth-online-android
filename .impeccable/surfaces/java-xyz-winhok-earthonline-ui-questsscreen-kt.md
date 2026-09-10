---
version: 1
slug: "java-xyz-winhok-earthonline-ui-questsscreen-kt"
primary_target: "app/src/main/java/xyz/winhok/earthonline/ui/QuestsScreen.kt"
related_targets: []
---

# V2 任务战令首屏构图

- Scope: v2 修仙体系的手机任务首屏；本轮只锁定构图契约，不修改 Android 应用代码。
- Mode: Operate。
- Audience: 希望把现实承诺转化为可执行任务与可追溯成长的中文 Android 用户。
- Job: 在数秒内看清当前境界、修为债、最近日期契约和一个最值得立即执行的任务，并能完成、暂缓或进入详情。
- Flow: 查看体系与角色状态 → 识别 Next Quest 及其日期契约 → 执行动作 → 查看今日任务、逾期责任与恢复入口 → 通过四目的地导航继续。
- Content: 体系切换、日期级截止、当前修为、修为债、Next Quest、任务动作、逾期劫痕、四个稳定一级目的地。
- Constraints: 地球母品牌不变；Room 事实与任务内容不因体系切换变化；不出现分钟级截止、随机奖励、货币、商城、连续签到压力或未实现指标；48dp 触控目标、非纯颜色状态、Android Material 导航与返回语义。
- Visual world: 宗门任务榜 × 现代移动战令；黑漆玉简、稀缺金箔、朱砂劫痕、星轨蚀刻；大胆数字先于解释文字；金色只用于契印、奖励和突破。
- Synthetic content: 构图中的任务标题、等级、修为、债务、日期、奖励和数量只是布局示例，不是产品默认值、真实用户数据或已验证指标。
- Do not literalize: 生成图中的衬线／书法感字形仅是栅格渲染偏差，Android 实现必须使用 `DESIGN.md` 的 Roboto／系统无衬线 Material type scale；逾期任务上的“暂缓一天”不得照抄，应显示明确结算与“查看恢复路径”；矩形文字／箭头完成控件只表达动作位置，实现必须复用勾选式 `Filled Tonal Icon Button` 并提供包含任务标题的无障碍名称；最终体系名称、辅助文案和图标以叙事语义目录为准。
- Seed: `851ec319`；surface / operate；候选列表的第 3、5、6 构图入选。
- Build path: Comp-first；三张构图均为 841×1870（1080:2400 等比）的 Android 纵向高保真稿，`v2-battle-pass.webp` 已获批准；后续实现必须以批准稿的层级和空间关系为构图契约。
- First viewport: 顶部体系切换与境界/修为债战况，中部一枚占据绝对层级的 Next Quest 黑漆玉简，下部纵向修行轨继续显示正常、待办与逾期契约，底部为四目的地导航且任务页为当前目的地。
- Signature interaction: 选中任务玉简向右结算进入修为轨，主卡同时保留可见“完成”入口；下方任务按状态提供暂缓、结算或恢复动作，减少动态效果时以稳定状态切换替代位移动画。
- Approved comp: `.impeccable/mocks/decision/v2-battle-pass.webp`，用户于 2026-09-09 最终确认继续使用该稿。
- Generation prompt: `.impeccable/mocks/decision/v2-battle-pass.webp.json`（完整提示、输入来源与批准状态）；可编辑源提示为 `.impeccable/mocks/decision/v2-battle-pass.prompt.txt`。

## Direction contract

THESIS: 一枚现实契约玉简统领任务首屏，拒绝等级卡加普通任务列表的标准模板。

OWN-WORLD: 黑漆玉简与深玉底承载日常，翠色只表示行动，金箔只落在契印和奖励，朱砂只标债务与逾期。

STORY: 用户先看境界与债务，再确认唯一 Next Quest 的日期和收益，完成或暂缓，并沿修行轨理解后续责任与恢复入口。

FIRST VIEWPORT: 顶部体系切换和大胆境界数值，中部主玉简占最大面积且右侧完成动作始终可见，下部轨道承接今日契约，任务导航压底。

FORM: 战令修行轨，候选序列第 3 项，surface seed `851ec319`，批准构图 `v2-battle-pass.webp`。

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Finish review

- Implemented on 2026-09-10 with the approved `v2-battle-pass.webp` hierarchy: the normal phone viewport reaches the real quest track, and widths at or above 720dp use a 42/58 realm-and-contract split rather than a stretched phone column.
- The highlighted contract and ordinary quest cards share one 88dp right-swipe completion threshold, reduced-motion behavior, haptic threshold signal, visible completion control, and accessibility action.
- Overdue contracts use explicit error text plus an open-recovery action; they do not expose the ordinary postpone path. Search and filters expand directly below the track heading before any track rows.
- Actual Activity/Room/Compose acceptance covered dark and light phones, 200% font, dark and light tablet layouts, rotation draft preservation, swipe completion, immediate search, overdue recovery, and narrative-switch scroll anchoring.
- Independent finish verdict: **PASS**. All five prior P1 findings (first viewport, highlighted swipe, overdue recovery, search reachability, and true wide layout) were closed before release gating.
