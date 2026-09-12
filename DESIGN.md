---
name: "地球 Online"
description: "唯一服、单向时间、现实结算；同一人生可以选择不同的叙事。"
colors:
  action-green: "#176B47"
  action-on-green: "#FFFFFF"
  action-green-soft: "#C7EFD8"
  action-green-ink: "#092F1D"
  reward-gold: "#775B22"
  reward-on-gold: "#FFFFFF"
  reward-gold-soft: "#FAE5B5"
  reward-gold-ink: "#302207"
  earth-paper: "#F7FAF4"
  earth-ink: "#17251C"
  earth-mist: "#E2EBDF"
  earth-muted: "#405345"
  night-action-green: "#81D9B3"
  night-action-ink: "#053826"
  night-action-green-deep: "#1D4B37"
  night-action-green-soft: "#C6F9DC"
  night-reward-gold: "#E4C285"
  night-reward-ink: "#3F2D08"
  night-reward-gold-deep: "#524322"
  night-reward-gold-soft: "#FCE5B4"
  night-sky: "#A9CDDD"
  night-space: "#101713"
  night-ink: "#E2E9E1"
  night-surface: "#263B2F"
  night-muted: "#BDCCC0"
  night-outline: "#87978B"
  night-outline-soft: "#3C4D41"
  cultivation-action: "#146B4D"
  cultivation-jade: "#B8EBD2"
  cultivation-gold: "#755A1D"
  cultivation-gold-soft: "#F2D99B"
  cultivation-breach: "#9B3B34"
  cultivation-ground: "#EAF4EC"
  cultivation-night-action: "#63E6AF"
  cultivation-night-jade: "#123E31"
  cultivation-night-gold: "#E2BE70"
  cultivation-night-gold-deep: "#493A1D"
  cultivation-night-breach: "#E96A61"
  cultivation-night-ground: "#071A16"
typography:
  display:
    fontFamily: "Roboto, sans-serif"
    fontSize: "32sp"
    fontWeight: 700
    lineHeight: "40sp"
  headline:
    fontFamily: "Roboto, sans-serif"
    fontSize: "24sp"
    fontWeight: 400
    lineHeight: "32sp"
  title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  label:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
rounded:
  small: "12dp"
  stat: "16dp"
  medium: "20dp"
  large: "28dp"
spacing:
  micro: "4dp"
  compact: "8dp"
  small: "12dp"
  medium: "16dp"
  screen: "20dp"
  roomy: "24dp"
  fab-clearance: "104dp"
components:
  button-primary:
    backgroundColor: "{colors.action-green}"
    textColor: "{colors.action-on-green}"
    rounded: "{rounded.small}"
    padding: "8dp 24dp"
    height: "48dp"
  button-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.action-green}"
    rounded: "{rounded.small}"
    padding: "8dp 24dp"
    height: "48dp"
  card-quest:
    backgroundColor: "{colors.earth-paper}"
    textColor: "{colors.earth-ink}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  card-reward:
    backgroundColor: "{colors.reward-gold-soft}"
    textColor: "{colors.reward-gold-ink}"
    rounded: "{rounded.medium}"
    padding: "22dp"
  card-cultivation-contract:
    backgroundColor: "{colors.cultivation-night-jade}"
    textColor: "{colors.night-ink}"
    rounded: "{rounded.small}"
    padding: "20dp"
  field-outlined:
    backgroundColor: "{colors.earth-paper}"
    textColor: "{colors.earth-ink}"
    rounded: "{rounded.small}"
    padding: "16dp"
  chip-selected:
    backgroundColor: "{colors.reward-gold-soft}"
    textColor: "{colors.reward-gold-ink}"
    rounded: "{rounded.small}"
    height: "48dp"
  navigation-active:
    backgroundColor: "{colors.action-green-soft}"
    textColor: "{colors.action-green-ink}"
    rounded: "{rounded.large}"
  fab-primary:
    backgroundColor: "{colors.action-green-soft}"
    textColor: "{colors.action-green-ink}"
    rounded: "{rounded.stat}"
    height: "56dp"
---

# Design System: 地球 Online

## Overview

**Creative North Star: "唯一服 · 单向时间 · 现实结算"**

地球 Online 把现实生活视作一场无法重开的限时冒险。应用可以离线，但人生始终在线；界面不是一张安静等待遗忘的 To-do 清单，而是一台把承诺、行动、代价与成长如实结算的玩家终端。

底层事实只有一份，地球原生、修仙、武侠等体系是可以随时切换的叙事解释器。它们可以改变名称、图标、配色、触觉与结算演出，却不能改变任务、奖励、代价或历史。这份“爽感”来自确定、强烈、可追溯的行动反馈；这份重量来自公开、有限、可修复的失约代价，而不是随机惩罚或羞辱。

当前实现同时包含地球原生基线与修仙“宗门战令”世界。地球原生以翡翠行动色、稀缺奖励金和清晰圆角容器表达现实探索；修仙以黑漆玉简、矿物浅玉、金箔契印、朱砂劫痕和星轨切角表达同一份事实。Material 3 的原生结构确保任何叙事体系都不会牺牲手机效率和可访问性。

**Key Characteristics:**

- 现实任务拥有副本目标般的清晰入口和完成出口。
- XP、等级、技能、连续行动与 Boss 状态构成稳定的成长回路。
- 翡翠绿负责“现在就行动”，奖励金只在结算与突破时爆发。
- 圆润的 Material 3 容器保留亲和力，标题、数字和结算信息负责力量感。
- 世界观服务于行动；叙事可以切换，底层事实和交互位置保持稳定。
- 修仙任务页以境界/劫债战况、唯一 Next Quest 玉简和真实任务轨迹形成完整战令，而不是换词后的普通列表。

## Colors

日间方案像有生命力的地表与纸质任务面板，夜间方案像深空中的发光控制台；两者都以绿色驱动行动、以金色兑现奖励。

### Primary

- **行动翡翠**：用于主要按钮、完成状态、XP 与当前成长指标，是“继续推进”的唯一主行动色。
- **灵气浅翠**：用于欢迎卡、选中导航和主行动容器，让成长状态在大面积背景中清楚浮现。
- **夜航灵光**：在深色模式中替代日间翡翠，保持高可见度而不产生荧光刺眼感。

### Secondary

- **奖励金**：用于待重审、稀缺提示和需要额外注意的成长节点；修仙体系可将其解释为渡劫金。
- **战利品金箔**：用于 Next Quest 与奖励容器；它表达“值得拿下”，不承担普通导航。

### Tertiary

- **星轨蓝**：仅存在于夜间扩展色阶，为未来确有必要的第三层信息提供冷色分隔；不得与主行动竞争。

### Cultivation

- **宗门翡翠**（日间 `#146B4D`／夜间 `#63E6AF`）：仅用于可执行动作、有效成长与完成节点。
- **矿物浅玉**（`#EAF4EC`、`#B8EBD2`）：承载日间任务表面，不退化为泛黄羊皮纸。
- **黑漆玉简**（`#071A16`、`#123E31`）：承载夜间战况、契约主卡与任务轨迹。
- **契印金箔**（日间 `#755A1D`／夜间 `#E2BE70`）：只用于境界、奖励、签约与当前目的地。
- **朱砂劫痕**（日间 `#9B3B34`／夜间 `#E96A61`）：只用于债务、逾期与需要恢复的明确状态，并始终配合文字或形状。

### Neutral

- **地表纸白**：日间页面与卡片基础表面，略带自然绿意，避免办公软件式纯白。
- **地核墨色**：日间正文和高优先级信息，保持沉稳而非纯黑压迫。
- **薄雾表面**：统计块、次级容器和结构分区，以色调而非阴影建立层次。
- **深空玄夜**：夜间背景与基础表面，让灵气和奖励色成为真正的视觉事件。
- **月壤文字与轨道描边**：夜间正文、次级文字、卡片边界与分隔，不与发光主色抢夺注意力。

### Named Rules

**The Two-Energy Rule.** 翡翠只表达行动与有效成长，金色只表达奖励、Boss 和突破；同一屏不再增加第三种高饱和竞争色。

**The Reward Rarity Rule.** 金色必须稀缺。若普通设置、导航和说明文字都变成金色，通关就不再像通关。

**The Semantic Palette Rule.** 叙事体系可以替换具体色相，但行动、奖励、警告、失败和恢复的语义角色必须一一对应，并同时满足日间、夜间和对比度要求。

## Typography

**Display Font:** Roboto（Android 系统 sans-serif 回退）
**Body Font:** Roboto（Android 系统 sans-serif 回退）

**Character:** 字体保持 Android 原生、清晰和可信；爽感不靠奇幻字体堆砌，而靠大号等级数字、短促标题、进度对比和结算节奏建立。中文由设备系统无衬线字体自然回退。

### Hierarchy

- **Display**（粗体、32sp、40sp 行高）：角色名、核心页面宣言和大型里程碑；当前映射自加粗的 `headlineLarge`。
- **Headline**（常规、24sp、32sp 行高）：等级、Next Quest 标题和统计数值；关键数值可局部使用半粗体。
- **Title**（半粗体、22sp、28sp 行高）：顶栏、卡片组与核心模块标题。
- **Body**（常规、16sp、24sp 行高）：任务说明、日志正文和需要完整阅读的内容；较密集元数据使用 Material 3 的 14sp 与 12sp 正文角色。
- **Label**（中等字重、12sp、16sp 行高）：XP、优先级、难度、日期、筛选和状态标签；`NEXT QUEST` 这类短标识可以粗体大写，中文标签不强制大写化。

### Named Rules

**The Numbers Hit First Rule.** 等级、XP、连击天数与 Boss 进度必须先于解释文字被看见；数字承担力量感，正文承担可信度。

**The Interpreter Rule.** 叙事体系改变名称和气质，不改变信息层级。修仙不用难读的书法体，武侠不用仿古卷轴牺牲正文；所有体系继续服从 Android 可读性。

## Layout

应用默认采用单列任务流与清晰分区；修仙战令在大于等于 720dp 时把境界战况与主契约组成 42/58 双栏，轨迹仍保持单一纵向时间线。手机使用底部四目的地导航，宽屏切换为侧边 Navigation Rail；主内容最大宽度为 1200dp，全屏编辑器最大宽度为 760dp。内容区通常使用 20dp 水平边距，首屏与重点空状态可使用 24dp；列表底部保留 104dp，避免系统导航遮挡最后一项。

修仙任务首屏固定为“体系切换 → 境界/劫债战况 → 唯一 Next Quest 玉简 → 修行轨迹”。搜索与筛选按需展开，不先于当前行动争夺首屏；任务页不再叠加创建 FAB，接取入口与轨迹标题同层，避免遮挡契约内容。宽屏保留相同阅读顺序并使用 Navigation Rail；200% 字体时底栏仅显示当前目的地文字，其余目的地仍保留图标和完整无障碍语义。

空间节奏以 4dp 为基线：8dp 组织紧密元数据，12dp 组织卡片内部层级，16dp 用于任务卡内边距，20dp 用于页面节奏，24dp 用于英雄卡与空状态。筛选器在窄屏中横向滚动，不压缩成不可读的小目标。所有编辑器使用 IME Insets，系统返回与预测返回保持原生语义。

**The One Next Move Rule.** 每个页面优先暴露一个最值得立即执行的动作；手机以单个 FAB 或单个主按钮承载，不堆叠多个同等级召唤。

**The Contract Rail Rule.** 修仙任务页必须让唯一主契约先于轨迹出现；轨迹只能呈现真实任务、完成和逾期事实，不能生成装饰节点或第二份进度。

## Elevation & Depth

系统以 Material 3 的色调分层为主、组件标准 elevation 为辅。任务列表和信息容器默认使用描边或表面色差；角色总览允许 Elevated Card，主行动 FAB 保持系统悬浮层级。阴影不是装饰性光效，奖励爆发优先通过容器色、排版尺度与状态变化表达。

### Named Rules

**The Grounded Power Rule.** 平时界面稳稳落在地面上，只有当前行动、关键结算和系统级浮层获得明显高度；到处悬浮会稀释突破时刻。

## Shapes

地球原生采用柔韧、可触的圆角语言：小型控件 12dp，统计块 16dp，卡片与普通容器 20dp，大型容器 28dp。修仙体系将 Material 容器收紧为 6/12/18dp，并只在境界战况与主契约玉简使用方向明确的切角；切角表达玉简层级，不编码危险。Planet Mark 的圆形星球与椭圆轨道仍是跨体系核心识别轮廓；其他组件不复制星球造型。

描边卡片用于任务、日志与可进入的详情，填充卡片用于欢迎、推荐与奖励。形状不编码危险程度；错误和删除依赖 Material error 语义色与明确文案。

## Components

### Buttons

- **Shape:** Material 3 的紧凑圆角矩形（12dp），所有可触目标至少 48dp。
- **Primary:** 行动翡翠底配白字，用于创建角色、查看任务、确认完成等单一主动作；标准内容内边距为纵向 8dp、横向 24dp。
- **Focus / Pressed:** 沿用 Material 3 状态层与涟漪反馈，键盘焦点必须清晰，不自造网页式 hover。
- **Secondary:** Outlined Button 用于导入、导出、编辑和撤销；Text Button 用于取消、关闭和低强调操作。破坏性动作只在确认语境使用 error 色。

### Chips

- **Style:** Filter Chip 是时间、精力、任务类型、难度、技能、优先级、主线与主题选择的统一控件。
- **State:** 选中态必须同时具备容器与文字对比，未选中态保留边界；芯片行允许横向滚动，触控高度保持 48dp。

### Cards / Containers

- **Corner Style:** 普通卡片使用 20dp 圆角，统计块使用 16dp。
- **Background:** 默认任务与日志使用基础表面；欢迎卡使用灵气浅翠；Next Quest 使用战利品金箔。
- **Shadow Strategy:** 默认描边或色调分层，只有角色总览等少数聚合面板使用 elevation。
- **Internal Padding:** 任务卡 16dp，主线卡 20dp，推荐卡 22dp，欢迎与空状态 24dp。

### Inputs / Fields

- **Style:** 全宽 Material 3 Outlined Text Field，12dp 圆角，基础表面背景；标签承担字段身份，辅助文字解释边界。
- **Focus:** 使用 Material 主色描边、光标和状态层，不加入持续发光。
- **Error / Disabled:** 通过 error 语义色、明确原因与禁用状态共同表达；不得只依赖颜色。

### Navigation

- 手机使用底部 Navigation Bar，宽屏使用 Navigation Rail，固定为指挥台、任务、角色、日志四个一级目的地。
- 当前目的地使用灵气浅翠指示器与深绿前景；设置保留在顶栏动作，不升格为第五个日常目的地。
- 编辑器使用全屏 Dialog、Top App Bar、关闭图标和文字保存动作，遵循系统返回、状态栏、导航栏和键盘 Insets。

### Planet Mark

Planet Mark 由翡翠陆地、浅翠海洋和金色轨道构成，是“地球唯一服”的识别签名。它适合出现在创建角色、身份确认与稀疏英雄区域；尺寸基准为 76dp，不在每张任务卡重复盖章。

Planet Mark 是跨叙事体系不变的母品牌锚点。体系可以改变它周围的纹样、状态语言和庆祝演出，但不能把地球标记替换成门派、兵器或其他局部世界观的徽记。

### Quest Completion Action

任务卡尾部的 Filled Tonal Icon Button 是最短通关路径：勾选图标必须拥有包含任务标题的无障碍名称，忙碌或当前轮次不可完成时禁用。完成后的 Snackbar 提供明确反馈与“撤销”，形成即时但可回退的结算闭环。

### Cultivation Battle Pass

修仙任务页顶部的体系 Filter Chip 保持 48dp 触控高度，选中态可见且可聚焦。境界战况使用切角描边 Surface，先显示境界、修为进度，再以一行真实账本摘要显示当前有效修为与劫债。唯一 Next Quest 使用黑漆/浅玉高层 Surface、稀缺金色奖励和 56dp 勾选式完成按钮；详情与未逾期延期为次级动作。下方修行轨迹只复用真实 Quest Card，翠色节点表示完成；朱砂节点、明确逾期文字与恢复入口共同表示失约，逾期状态不再提供普通延期。

## Do's and Don'ts

### Do:

- **Do** 让接取、完成、XP 增长、等级推进和 Boss 状态形成一眼可读的连续反馈。
- **Do** 把最强对比、最大数字和最稀缺金色留给通关、奖励与突破时刻。
- **Do** 使用 Material 3 组件、语义色、状态层、48dp 触控目标和系统返回行为承载所有刺激感。
- **Do** 同时维护日间与夜间方案，并在大字体、窄屏、宽屏和键盘弹出时保持下一步动作可见。
- **Do** 让体系切换即时重绘同一份历史，稳定保留任务、XP、代价、完成身份和审计事实。
- **Do** 在失约前公开代价，在失约后同时展示损失与恢复路径，让惩罚有重量但不成为死局。
- **Do** 让任务首屏的体系、境界、债务、日期契约、奖励和完成动作都来自当前 Room 快照与叙事目录。

### Don't:

- **Don't** 把界面做成换了名词的普通 To-do List；没有进度、结算和成长反馈的任务卡不符合这个世界。
- **Don't** 使用隐藏扣除、按离线天数无限叠加、连续签到损失或赌场式随机惩罚制造伪刺激。
- **Don't** 让金色、粒子、发光和大标题同时出现在所有区域；没有静默期就没有爆发。
- **Don't** 引入难读的古风字体、仿羊皮纸纹理或密集战斗 HUD，破坏现代 Android 的效率与可访问性。
- **Don't** 用主题装饰遮挡真实任务标题、截止信息、完成按钮或撤销入口。
- **Don't** 让任何叙事体系修改奖励公式、代价规则、存档结构或完成历史；换体系不是换账号，更不是数值漏洞。
