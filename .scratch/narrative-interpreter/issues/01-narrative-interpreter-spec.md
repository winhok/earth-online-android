## Problem Statement

地球 Online 已经拥有任务、主线、完成、XP、技能、成就、截止日期和恢复等稳定领域事实，但当前用户体验把地球原生词汇、图标、颜色和页面结构直接写在 Android UI、ViewModel 消息与通知中。若直接为修仙体系复制页面或全局替换文字，会篡改玩家原文、分裂历史、重复业务逻辑，并让不同体系获得不同能力或数值结果。

玩家需要的不只是浅层换色，而是能够随时切换的完整体验解释：同一份人生数据，在地球原生体系中是一套现实玩家终端，在修仙体系中可以成为历练、仙途、渡劫、修为和重修之路。体系应该拥有充分的叙事、视觉和交互表达空间，同时绝不能改变底层事实、公平规则、备份可靠性、Android 原生可用性或隐私边界。

## Solution

引入全局“叙事体系”契约。每个玩家存档在同一时刻激活一个体系，用户可以随时切换。体系由稳定 wire ID、类型化中立语义目录、能力清单、可选页面提供器、主题资源、效果方案和纯展示偏好组成。体系可以重新讲述系统文案、重新绘制母品牌、重组页面并增加由中立事实驱动的故事内容，但只能读取统一 UI 状态、调用稳定中立命令，不能访问 Room、改变领域对象或拥有独占业务能力。

地球原生与修仙作为首批完整内置体系。产品名和地球轮廓保持母品牌身份；叙事体系、明暗主题和语言互相独立。玩家原文永不改写，系统历史按当前体系动态解释。体系切换先完整验证，再原子持久化并重绘，同时保留当前操作状态。旧备份默认地球原生，未知体系安全回退且不阻塞数据恢复。

## User Stories

1. As a new player, I want Earth-native to be the default narrative system, so that I can begin without making a thematic choice.
2. As a new player, I want to preview the available systems during onboarding without being blocked, so that choosing a story remains optional.
3. As an existing player, I want to change the active narrative system at any time, so that I can reinterpret the same journey as my preferences change.
4. As a player switching systems, I want all quests, goals, completions, contracts, debts and achievements to remain the same, so that a presentation choice cannot change my progress.
5. As a player switching systems, I want the new experience to appear without restarting the app, so that switching feels immediate.
6. As a player, I want one active global system in the first release, so that the experience remains coherent rather than mixing incompatible vocabularies.
7. As a player, I do not want individual quests or goals assigned to separate systems, so that the data model remains neutral.
8. As a player, I want a system to change more than labels and colors, so that cultivation can feel like a genuine world rather than a find-and-replace skin.
9. As a player, I want each system to preserve every product capability, so that switching cannot remove actions I rely on.
10. As a player, I want each system to preserve every numerical rule and result, so that no system becomes an optimization exploit.
11. As a player, I want Earth Online to retain its product name under every system, so that I always recognize the app.
12. As a player, I want the planet silhouette retained as a mother-brand anchor, so that cultivation and future systems still belong to Earth Online.
13. As a player using cultivation, I want the planet mark reimagined as a spiritual chart or orbit, so that the system can express its own material world.
14. As an Android user, I want the launcher name and icon to remain stable, so that system switching does not disrupt shortcuts or app recognition.
15. As a player, I want narrative selection independent from follow-system, light and dark display modes, so that story and viewing environment remain separate choices.
16. As a player using any released system, I want both light and dark presentations, so that choosing a story does not remove display accessibility.
17. As a player, I want language independent from narrative system, so that future localization does not require new domain data.
18. As a player, I want every app-authored page title, button, status and explanation rendered by the active system, so that the world feels complete.
19. As a player, I want app-authored errors and Snackbar feedback rendered by the active system, so that failures do not break immersion.
20. As a player, I want app-authored accessibility text rendered by the active system, so that screen-reader users receive the same coherent experience.
21. As a player, I want icons, semantic colors, illustrations and effects selected by the active system, so that each world has a distinct visual grammar.
22. As a player, I want my name and server name preserved exactly, so that switching never edits my identity.
23. As a player, I want my quest and goal titles preserved exactly, so that switching never rewrites my real commitments.
24. As a player, I want descriptions and journal notes preserved exactly, so that creative narrative cannot corrupt private text.
25. As a player performing a high-impact action, I want its real action, number and consequence stated plainly alongside thematic language, so that immersion never hides consent.
26. As an Android user, I want permission dialogs and file pickers to remain native system experiences, so that the app does not imitate trusted platform UI.
27. As a player reading current quests, I want neutral task kinds, skills and statuses translated by the active system, so that structured facts participate in the world.
28. As a player reading history, I want system event types and structured parameters rendered by the current system, so that the whole journey changes coherently.
29. As a player reading history, I want the title snapshot from the original real action preserved, so that thematic switching does not falsify the event.
30. As a player with legacy journal entries, I want their raw text preserved and only their known event type translated, so that unsafe string replacement cannot alter old content.
31. As a player generating new history, I want system messages stored as neutral event keys and structured parameters, so that future systems can render them correctly.
32. As a player switching systems, I want achievement unlocks to remain unchanged, so that the same fact cannot pay twice.
33. As a player switching systems, I want achievement names, descriptions, icons and celebrations reinterpreted, so that stable achievements still belong to the current world.
34. As a cultivation player, I want `three_days` shown as an appropriate cultivation achievement, so that a neutral achievement gains thematic meaning without changing its condition.
35. As a player, I want the numerical level visible in every system, so that progression remains comparable across interpretations.
36. As a cultivation player, I want a realm title displayed beside the numerical level, so that long-term progression gains narrative milestones.
37. As a player in an out-of-order state, I want “fallen immortal” treated as a recoverable status rather than a permanent level, so that failure becomes a route rather than an identity sentence.
38. As a player, I want stable skill values under every system, so that changing labels cannot change growth.
39. As a cultivation player, I want Knowledge, Vitality, Creation, Connection and Discipline rendered as Insight, Foundation, Creation, Affinity and Dao Heart, so that the skill set fits the world without narrowing its meaning.
40. As a player, I want a system to reorganize pages and components, so that different worlds can use their own spatial and interaction language.
41. As a player, I want stable semantic destinations and actions beneath different layouts, so that all systems can navigate to the same capability.
42. As a player following a deep link or notification, I want it to open the correct semantic destination in the active system, so that layout differences do not break navigation.
43. As a player, I want cultivation to add world introductions, chapters, titles and story panels, so that the system can tell a complete story.
44. As a player, I want story unlocks derived from neutral facts, so that switching systems does not create a second progression ledger.
45. As a player, I do not want a system-specific story panel to grant unique XP or skills, so that narrative content cannot become a hidden business feature.
46. As a player, I want system-specific pages to call the same completion, edit, contract and recovery commands, so that outcomes remain consistent.
47. As a player, I want a system to inherit a shared page intentionally when it has no custom composition, so that reuse is allowed without accidental incompleteness.
48. As a player, I want incomplete systems unavailable in production, so that I never enter a half-native, half-themed experience.
49. As a player switching systems, I want my current destination and selected quest preserved, so that the switch does not interrupt my task.
50. As a player switching systems, I want current filters and scroll position preserved where the new composition supports them, so that I do not lose navigation context.
51. As a player switching while editing, I want my unsaved draft preserved and re-rendered, so that switching cannot destroy work.
52. As a player switching during a one-shot celebration, I want the animation stopped without replaying its command or reward, so that presentation cannot duplicate effects.
53. As a player switching with an open confirmation, I want the same decision and values presented in the new system, so that consent remains continuous.
54. As a player seeing a legacy unstructured transient message, I want it allowed to expire unchanged, so that the app does not guess how to rewrite it.
55. As a player, I want selection saved immediately after the target system validates, so that I cannot forget a separate settings save action.
56. As a player, I want a failed switch to leave the previous system and UI state intact, so that no partial world is committed.
57. As a player launching with a damaged or unavailable selected system, I want a complete Earth-native fallback, so that the app remains usable.
58. As a player with an unavailable selected system, I want its stable ID retained for diagnosis and future restoration, so that fallback does not erase preference history.
59. As a player, I want each system to remember whether I have seen its introduction, so that switching away and back does not repeat onboarding unnecessarily.
60. As a player, I want per-system folding and presentation preferences retained, so that each experience remains comfortable.
61. As a player, I do not want per-system display state to contain XP, debt or completion results, so that it cannot become a second save.
62. As a player exporting a backup, I want my selected system and display preferences included, so that full restore returns my familiar experience.
63. As a player importing a backup, I want the preview to disclose a narrative-system change, so that full replacement remains informed.
64. As a player importing an older backup without a system ID, I want Earth-native selected, so that compatibility is deterministic.
65. As a player importing a backup with an unknown system ID, I want all other valid data restored with safe Earth-native rendering, so that a cosmetic preference cannot block recovery.
66. As a player upgrading from version 1, I want the new narrative preference initialized without changing any quest, completion or theme value, so that migration is non-destructive.
67. As a player, I want narrative and overdue-contract data added in one coordinated database and backup migration, so that the release does not churn schemas unnecessarily.
68. As a player, I want a system content update to improve current and historical presentation without rewriting stored facts, so that the app can refine its storytelling safely.
69. As a player, I do not want old copies of every system asset stored per event, so that backup and app size remain bounded.
70. As a player, I want future martial-arts or other built-in systems possible through the same registration contract, so that expansion does not require changing domain data.
71. As a player in the first release, I want Earth-native and cultivation fully available offline, so that the core promise does not depend on a server.
72. As a player, I do not want runtime system downloads or third-party scripts in the first release, so that privacy, integrity and quality remain controlled.
73. As a cultivation player, I want Quest, Side Quest, Daily, Boss and Mainline rendered as 历练、支线历练、日课、渡劫 and 仙途, so that work structure reads naturally in the cultivation world.
74. As a cultivation player, I want XP, Level and Achievement rendered as 修为、境界 and 道果, so that progression has a coherent vocabulary.
75. As a cultivation player, I want Deadline Contract, Extension and Abandonment rendered as 天命契约、改命 and 解契, so that commitment choices fit the story.
76. As a cultivation player, I want Overdue Consequence, Debt, Out of Order and Recovery Route rendered as 劫罚、劫债、道心失序 and 重修仙途, so that failure and recovery form one narrative arc.
77. As a cultivation player declaring force majeure, I want 尘世变故 paired with the plain phrase 不可抗力, so that serious reality is not hidden by entertainment language.
78. As a cultivation player, I want level bands from 炼体 through 真仙 while retaining `Lv.N`, so that realm progression is expressive and numerically honest.
79. As a player receiving a pending reminder, I want its content generated from the active system at delivery time, so that future notifications match my current world.
80. As a player switching after a notification is already delivered, I want the existing system notification left untouched, so that switching does not create duplicate alerts.
81. As a player, I want notification channel identity and worker identity stable across systems, so that platform settings and scheduling remain reliable.
82. As a privacy-conscious player, I do not want any system to expose private quest titles in notifications, so that immersion does not weaken confidentiality.
83. As a player, I want sound off by default, so that a dramatic system does not surprise people around me.
84. As a player, I want haptics on by default while respecting system settings, so that mobile feedback feels responsive without overriding device preferences.
85. As a player, I want global sound, haptic and reduced-motion controls, so that I do not need to configure every system independently.
86. As a player who disables effects, I want every result and action still represented textually and semantically, so that effects are never the correctness boundary.
87. As a screen-reader user, I want every system to expose complete action names, states and consequences, so that custom composition remains accessible.
88. As a large-font user, I want every released system to preserve readable content and reachable actions, so that thematic layouts do not clip meaning.
89. As a phone user, I want every system verified for compact touch interaction and system back behavior, so that visual ambition still feels native.
90. As a large-screen user, I want every system verified for the supported expanded layout, so that a phone composition is not merely stretched.

## Implementation Decisions

- Keep all quest, goal, completion, progress, achievement, contract, consequence and recovery rules narrative-neutral. A narrative system is a presentation concern and cannot become an input to reward, ranking, debt or identity calculations.
- Add a stable narrative-system wire ID to the player’s persisted display preferences. Do not use a localized display name or enum ordinal as storage identity.
- Preserve the existing light/dark display mode as a separate preference. Runtime theme resolution combines one narrative system with follow-system, light or dark mode.
- Define a built-in narrative registry. The first release registers Earth-native and cultivation. The registry validates system identity, structure version, content version, supported locales, capabilities, inherited surfaces, required resources and effect declarations.
- Split the narrative contract into two presentation layers. A typed semantic catalog resolves labels, structured copy, icon roles, color roles and effect roles. Optional surface providers can supply system-specific Compose compositions.
- Keep custom surface providers behind a uniform UI-state and action boundary. They may observe presentation-ready neutral state and invoke stable commands; they cannot access Room, repositories or mutable domain entities directly.
- Define stable semantic IDs for destinations, screens, actions, fields, states, errors, notifications, achievements and system events. Deep links and notification navigation target these IDs rather than a system-specific route string.
- Maintain a required capability and state matrix for every system. It covers all current product destinations, commands, empty/loading/error states, contract outcomes and recovery actions.
- Allow explicit inheritance from shared surfaces or catalog entries in the system manifest. Missing undeclared entries are validation failures, not implicit per-key fallback.
- Validate an entire target system before persistence. Only after validation succeeds may selection be written atomically and exposed to UI state. A failed switch leaves the previous selection and visible state intact.
- Resolve a selected but unavailable or invalid system to Earth-native for effective rendering while preserving the requested wire ID. Surface a neutral availability error without blocking the rest of the application.
- Keep the application name and launcher icon stable. Preserve the planet silhouette as a mother-brand invariant while allowing each system to redraw its material, orbit, motion and surrounding visual world.
- Allow a system-specific surface to change layout, hierarchy, components and storytelling while preserving the complete capability matrix, Android back behavior, insets, touch targets and high-impact confirmations.
- Allow system-specific world introductions, chapters, titles and story panels. Derive every unlock from neutral domain facts and never persist a separate system-specific XP, debt, task or achievement ledger.
- Allow system-specific pure presentation actions such as opening lore or changing a panel. Any new domain mutation must first become a neutral capability available to every system.
- Centralize application-authored presentation behind typed semantic keys and structured parameters. Do not implement global string replacement and do not store newly rendered sentences as authoritative event facts.
- Preserve player name, server name, quest and goal titles, descriptions, notes and historical title snapshots byte-for-byte through system switching. Treat them as user-authored parameters, never translation input.
- Continue storing stable task-kind, skill, state, event, achievement and recommendation identifiers. Do not rename existing wire values merely to match a narrative system.
- For future system-generated history, persist a neutral event key with structured, validated parameters. Render it using the current system. Preserve legacy raw event text unchanged and only reinterpret the known event type around it.
- Render high-impact actions with both thematic expression and explicit real meaning. Reward, cost, deadline, deletion, undo, import replacement and irreversible consequences must retain visible numbers and unambiguous verbs.
- Preserve Android-owned permission, file-picker and operating-system UI. Do not imitate or theme trusted platform dialogs.
- Preserve selection-independent domain transactions. A UI recompose, system switch, animation cancellation or effect replay can never repeat a command, completion, reward, assessment or import.
- Hoist current destination, selected entity, filters, scroll anchors and editor drafts above system-specific surface providers using stable semantic state. Map that state into the newly selected composition during switching.
- Immediately re-render state-driven screens and confirmations after a successful switch. Cancel in-flight one-shot effects without replaying their domain trigger, then render the stable result in the new system.
- Allow legacy unstructured transient strings already on screen to expire unchanged. All new messages after the switch use the active system.
- Give each system a namespaced store for presentation-only preferences such as read introductions, folded panels and viewed effects. Validate that these records cannot carry domain progress or mutation payloads.
- Include the selected system wire ID and namespaced display preferences in full backup payloads. Import preview discloses system changes before transactional replacement.
- Default version-1 data and backups without a narrative-system field to Earth-native. Preserve unknown IDs and fall back safely instead of rejecting otherwise valid user data solely for an unavailable presentation package.
- Coordinate this feature with the deadline-contract feature in one release train using a single explicit Room v1-to-v2 migration and backup v1-to-v2 protocol change. Keep their implementation modules and tests independently understandable.
- Keep notification channel and WorkManager identities stable. Resolve pending notification title, body and accessibility copy at delivery time using the active system without exposing private quest titles.
- Do not rewrite already delivered Android notifications after switching. Update user-visible channel metadata on a best-effort basis while respecting user-controlled system settings.
- Define global sound, haptic and reduced-motion preferences. Sound defaults off; haptics default on and respect platform settings; animation respects the system remove-animations preference. No effect may contain unique information or actions.
- Treat locale as independent from narrative system. The first release provides Simplified Chinese, but semantic keys and stored IDs remain language-neutral. A missing locale invalidates the whole target system and falls back to Earth-native in that locale rather than mixing languages.
- Version each system with a stable ID, structure version and content version. Content updates may revise copy, assets, layout and effects; current and historical neutral facts render with the installed version. Do not preserve old asset bundles per event.
- Bundle Earth-native and cultivation resources in the application for offline use. Do not add network downloads, marketplaces, third-party plugins or executable custom system scripts in this release.
- Map the cultivation system’s core vocabulary as follows: Quest→历练, Side Quest→支线历练, Daily→日课, Boss→渡劫, Mainline→仙途, Complete→历练达成, XP→修为, Level→境界, Achievement→道果, Deadline Contract→天命契约, Extend Deadline→改命, Proactive Abandonment→解契, Overdue Consequence→劫罚, Debt→劫债, Out of Order→道心失序, Recovery Route→重修仙途, and Force Majeure→尘世变故／不可抗力.
- Map the stable skills in cultivation as Knowledge→悟性, Vitality→根骨, Creation→造化, Connection→因缘 and Discipline→道心.
- Retain the numeric level in cultivation and derive the display realm from level bands: Lv.1–4 炼体, Lv.5–9 练气, Lv.10–19 筑基, Lv.20–34 金丹, Lv.35–54 元婴, Lv.55–79 化神, Lv.80–109 炼虚, Lv.110–149 合体, Lv.150–199 大乘 and Lv.200+ 真仙.
- Treat fallen-immortal presentation as the cultivation interpretation of the neutral out-of-order and recovery state, not a persisted permanent level or identity.
- Keep achievement IDs and unlock conditions unchanged. Let each system map names, descriptions, icons and celebration; switching cannot unlock again or issue a second reward.

## Testing Decisions

- Test externally observable narrative outputs, capability availability, persisted selection and unchanged domain facts rather than implementation-private formatter methods or Compose node structure.
- Use one pure Kotlin narrative-contract suite as the primary seam. It receives a system ID, neutral semantic key and structured parameters and asserts the complete presentation contract without Android, Room or Compose.
- Table-drive the complete Earth-native and cultivation catalogs across every required semantic key, state, error, achievement, notification and high-impact confirmation.
- Test that every released system satisfies the capability matrix, declares all intentional inheritance and contains valid Simplified-Chinese, light, dark, accessibility and resource entries.
- Test representative structured messages with player-authored strings containing Chinese, emoji, quotes, control characters and words such as “任务” or “XP”; outputs must preserve user text exactly rather than replace substrings.
- Test that high-impact thematic messages retain the same action, amount, date and consequence as Earth-native output.
- Test all cultivation core vocabulary, five skill mappings and every level-band boundary while proving numerical level and achievement ID remain unchanged.
- Add property-style tests that interpreting or switching a system cannot mutate or change equality of neutral quests, goals, completions, progress, contracts, consequences, allocations and recovery state.
- Test Earth-native default selection, valid immediate switching, unavailable selection fallback, invalid system rejection and atomic preservation of the previous system on validation failure.
- Test unknown wire IDs are retained while effective rendering falls back, and that a later-valid registration can resolve the retained ID without data migration.
- Test current-version rendering of historical neutral events, preservation of historical player-title snapshots, and unchanged display of legacy raw journal text.
- Test system-content version changes re-render the same neutral history without modifying persisted event data or duplicating achievements.
- Use Repository plus Room tests only for persistence facts: immediate selection storage, separate light/dark mode, namespaced display preferences, v1 migration default and reopen behavior.
- Coordinate the Room migration test with deadline-contract schema additions. Start from a real v1 database fixture and prove player, tasks, goals, completions, theme and history survive while narrative defaults to Earth-native.
- Upgrade backup round-trip and replacement tests to include selected system and display preferences. Test missing fields, unknown IDs, malformed preference namespaces, capacity checks and full rollback on invalid restore.
- Test import preview reports system changes and that restoring an older valid backup restores its preference without preserving local presentation records outside the snapshot.
- Use focused Compose instrumentation for successful switching from representative destinations, preservation of selected task/filter/scroll state, and preservation of an unsaved editor draft.
- Test switching with an open high-impact confirmation and during a one-shot celebration; the visible system changes while the underlying command executes no more than once.
- Test the narrative-system and light/dark matrix for Earth-native and cultivation without conflating their stored preferences.
- Test system-specific layouts retain every required action and state using stable semantic identifiers rather than pixel coordinates.
- Test screen-reader semantics for current state, action name, reward, cost and consequence in both systems. Verify no meaning depends only on color, illustration, sound, haptic or animation.
- Test large font, compact phone, supported expanded layout, system back, IME insets and reduced-motion behavior for both released systems.
- Test sound-off default, haptic system compliance and global effect preferences across switching. Disabled effects must not remove content or actions.
- Test pending notification copy resolves with the current system at delivery, delivered notifications are not reposted on switch, internal channel identity remains stable, and private quest titles never appear.
- Verify all required system assets work offline and a missing critical resource prevents activation rather than producing a mixed system.
- Keep domain calculation tests in their existing pure rules seam; narrative tests assert only that the same input facts are presented differently, not that XP or contract mathematics work again.
- Run the full repository gate after implementation and the isolated-emulator instrumented gate for UI, Room, migration, backup and notification integration.
- Capture real emulator screenshots for phone and supported large-screen classes in light, dark and increased-font configurations before final Impeccable review. Do not treat static catalogs as visual acceptance.

## Out of Scope

- The deadline-contract, overdue consequence, debt and recovery calculations themselves; they are defined by the separate local deadline-contract specification.
- Final screen composition, gesture choreography, illustration production, animation timing and visual polish; these belong to the subsequent Impeccable Shape and implementation workflow.
- Per-goal, per-quest or simultaneous mixed narrative systems in the first release.
- A complete martial-arts system or any third narrative package beyond reserving the registration contract.
- Runtime downloads, remote marketplaces, third-party plugins, executable scripts or user-authored system code.
- Network services, cloud synchronization, accounts, analytics, AI-generated narrative or trusted server time.
- Actual multilingual content beyond keeping locale independent and language-neutral in storage.
- Dynamic Android launcher icons or changing the installed application name.
- New system-specific domain commands, rewards, skills, achievement conditions or separate progression ledgers.
- Rewriting user-authored text or retroactively parsing legacy free-form journal bodies into guessed structure.
- Duplicating historical resources so each event permanently renders with the system version active when it occurred.
- Store publication, production signing or claims of physical-device compatibility.

## Further Notes

- **Status:** ready-for-agent.
- Current version 1.0 has only the existing Earth-native Compose presentation and a separate persisted light/dark mode. Narrative switching is not yet implemented.
- Much of the current application copy is embedded directly in Compose and ViewModel messages. Implementation must centralize system-authored presentation without moving business rules into UI or rewriting user text.
- Existing completion snapshots already preserve user task titles and stable task/skill values. Existing journal events provide a neutral event kind plus legacy raw text, which forms the compatibility boundary for historical rendering.
- The next workflow step is a combined Impeccable Shape for narrative switching and overdue settlement. That design pass can choose distinct Earth-native and cultivation compositions while this specification keeps their data and capabilities equivalent.
