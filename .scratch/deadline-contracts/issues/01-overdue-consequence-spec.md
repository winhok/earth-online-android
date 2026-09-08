## Problem Statement

地球 Online 当前把截止日期用于排序和提示，但一次性任务与 Boss 过期后仍可无代价完成。对希望借助应用建立行动动力的玩家而言，这让截止日期缺少承诺重量：完成有奖励，失约却没有对称后果，闯关、升级和 Boss 叙事因此缺乏真正的风险支撑。

同时，惩罚不能粗暴地变成持续扣血、无限债务或不可恢复的羞辱。地球 Online 是离线单人应用，必须尊重固定存档时区、现有完成快照、撤销重做、可靠备份和数据删除权。玩家可以失败、进入失序路线并承担真实代价，但只要继续完成现实行动，就必须始终能够恢复。

## Solution

为所有带截止日期的一次性任务和 Boss 建立明确的“时限契约”。签约时锁定奖励与罚则；截止日结束后，应用按契约时区幂等结算一次逾期代价。代价降低当前有效 XP，余额不足时形成债务，未来获得的全局 XP 优先偿债。技能成长与历史荣誉继续反映真实完成，不因单纯逾期被改写。

改期、暂缓、主动弃约、撤销、迟到完成和不可抗力分别拥有可解释的规则与不可变记录。持续失约会开启可恢复的失序路线，但不生成虚假任务或额外奖励。所有权威事实由 Room 持久化，并与审计事件在同一事务中写入；旧版任务不追溯处罚，而是逐项重新确认契约。

## User Stories

1. As a player creating a dated one-time quest, I want to see its reward and overdue cost before saving, so that I understand the commitment I am making.
2. As a player creating a dated Boss quest, I want the Boss bonus included in both reward and risk, so that the higher stakes are honest.
3. As a player creating an undated quest, I want it to remain outside the contract system, so that capturing an idea does not create an accidental liability.
4. As a player creating a daily quest, I want its date to remain a start date rather than a deadline, so that missing a daily occurrence does not create historical debt.
5. As a player choosing today as the deadline, I want to be told that only the remainder of today is available, so that I do not accept full risk unknowingly.
6. As a player creating a new contract, I want past dates rejected rather than silently changed, so that the recorded promise is logically possible.
7. As a player, I want the deadline to include the whole calendar day, so that the rule is easy to understand.
8. As a travelling player, I want the contract to keep the save time zone captured when I signed it, so that travel does not shorten or extend my commitment.
9. As a player whose app is closed at midnight, I want overdue consequences reconciled when I next return, so that correctness does not depend on a surviving process.
10. As a player returning after several offline days, I want each missed contract assessed once, so that I pay the promised cost without daily compounding.
11. As a player with multiple contracts expiring together, I want every contract accounted for independently, so that batching failures cannot reduce their cost.
12. As a player, I want one consolidated consequence summary for a batch of overdue contracts, so that the result is forceful but comprehensible.
13. As a player completing a quest before its deadline, I want the current contracted reward, so that successful action is settled immediately.
14. As a player completing a quest after its deadline, I want the late completion recorded and rewarded at its current contracted value, so that finishing still helps me recover.
15. As a player completing late, I want the original overdue consequence to remain in history, so that late action repairs rather than rewrites the failure.
16. As a player whose unchanged quest was completed late, I want its reward to offset the equal overdue cost, so that finishing restores the event to net zero rather than generating progress.
17. As a player who extended a quest before missing it, I want late completion to grant only the reduced reward, so that repeated extensions retain a real cost.
18. As a player extending a deadline once, I want the attainable reward reduced by 20% of the original reward, so that changing a commitment has weight without immediately subtracting earned XP.
19. As a player extending a deadline repeatedly, I want the reward floor to remain 40% of the original, so that the quest never becomes worthless.
20. As a player moving a deadline earlier, I want no reward reduction, so that taking on a stricter commitment is not punished.
21. As a player choosing “postpone one day” on a contracted quest, I want both its deadline and visibility date moved by one contract-time-zone day, so that the action matches its wording.
22. As a player postponing an undated quest, I want only its visibility changed, so that no contract is invented.
23. As a player postponing a daily quest, I want only the current day skipped, so that the daily start date and future occurrences remain intact.
24. As a player reaching a third extension, I want the quest marked for review, so that repeated replanning becomes visible.
25. As a player needing a fourth extension, I want to close the old contract as a half-cost abandonment and sign a new contract, so that deadlines cannot be rolled forward forever for free.
26. As a player clearing a deadline before it expires, I want to pay half of the original risk, so that abandoning a promise is cheaper than silently missing it but not free.
27. As a player pausing or archiving an active contract before its deadline, I want the same half-cost abandonment rule, so that equivalent exits have equivalent consequences.
28. As a player editing a contracted quest, I want title, description, priority and estimated time edits preserved with audit history, so that I can clarify the action without changing its stakes.
29. As a player attempting to change a contracted task’s type, skill or reward basis, I want to abandon and re-sign explicitly, so that risk cannot be reduced after acceptance.
30. As a player who already completed a contract, I want later editing, pausing or archiving to be consequence-free, so that a fulfilled promise is truly closed.
31. As a player who undoes an on-time completion after the deadline, I want a warning that the action will create the full overdue consequence, so that a one-tap undo cannot surprise me.
32. As a player undoing a completion that repaid debt, I want its reward and debt allocation reversed together, so that undo cannot erase debt for free.
33. As a player redoing an undone completion, I want the original completion reward snapshot restored, so that editing difficulty cannot farm XP.
34. As a player redoing a completion, I want its XP allocated against debts that exist at redo time, so that the current ledger remains coherent.
35. As a player, I want one contract to produce at most one liability assessment, so that overdue followed by pause or archive does not charge me twice.
36. As a player returning to an overdue task, I want to be able to sign a new deadline while retaining the old failure, so that recommitment is possible without erasing history.
37. As a player recommitting to the same one-time quest, I want the quest to retain only one completion reward slot, so that repeated contracts cannot mint multiple completion rewards.
38. As a player, I want my current effective XP and level to fall when consequences exceed current progress, so that failure has a visible cost.
39. As a new or indebted player, I want level to remain at least Lv.1 while unpaid cost remains as debt, so that zero XP does not make me immune.
40. As a player, I want my historical highest level shown separately from my current level, so that consequences do not falsify what I previously achieved.
41. As a player with debt, I want all newly earned global XP to repay debt before increasing current effective XP, so that recovery comes from real action.
42. As a player with several debts, I want a completion to repay its own linked debt first and then the oldest debt, so that each recovery is explainable.
43. As a player repaying debt, I want corresponding skill XP and completion achievements to progress normally, so that my real work is still recognized.
44. As a player who misses a deadline, I do not want unrelated skill XP removed, so that the system does not pretend learned ability disappeared.
45. As a player with active debt, I want a visible失序 state, so that I understand why new XP is not increasing my current level.
46. As a player with three full overdue breaches in 30 save-time-zone days, I want a recovery route opened, so that a repeated pattern becomes actionable.
47. As a player whose debt reaches the XP span of a full current level, I want the recovery route opened, so that a severe single loss also receives structure.
48. As a player who proactively abandons a contract, I do not want it counted as a full breach for the 30-day trigger, so that responsible cancellation differs from passive expiry.
49. As a player using an alternate narrative system, I want the same neutral recovery state rendered in that system’s language, so that switching presentation does not change consequences.
50. As a player entering recovery, I want to select real existing quests whose expected rewards cover the debt, so that the route does not invent fake work.
51. As a player in recovery, I want at most three recovery nodes emphasized at once, so that a large debt does not become another overwhelming list.
52. As a player completing recovery nodes, I want replacements selected dynamically until debt is cleared, so that the route remains focused.
53. As a player who clears or validly waives all debt, I want the recovery route completed immediately with a “return to order” record, so that redemption has a definite ending.
54. As a recovered player, I do not want old breaches to reopen the same route immediately, so that only a new full breach can begin another recovery cycle.
55. As a player affected by illness, care duties or an accident, I want to classify a contract as force majeure without entering private details, so that reality can override the game safely.
56. As a player declaring force majeure after assessment, I want the original consequence retained and a compensating waiver appended, so that history remains truthful.
57. As a player whose waived debt was already repaid, I want equivalent effective XP restored without receiving the task reward, so that waiver timing does not change the outcome.
58. As a player, I do not want inactivity, advertisements, purchases, items or annual resets to forgive debt, so that waiting and spending cannot replace action.
59. As a player whose system clock moves backward, I want existing outcomes preserved and no extra contract time granted, so that the timeline remains monotonic.
60. As a player with a clock anomaly, I want the app to remain usable and explain the anomaly, so that an offline self-management tool does not lock me out.
61. As an existing 1.0 player, I want old dated quests marked unsigned rather than retroactively penalized, so that I am not charged for rules I never accepted.
62. As an existing player, I want to review legacy contracts one by one, so that I can confirm, change or remove old dates deliberately.
63. As an existing player, I want unsigned legacy quests to remain completable under old rules until reviewed, so that upgrading does not block normal use.
64. As a player editing an unsigned legacy deadline, I want the new contract disclosure before saving, so that the task joins the new system explicitly.
65. As a player importing a backup, I want a preview of changes to quests, completions, contracts, consequences and debt, so that full replacement is informed.
66. As a player restoring an older valid backup, I want the backed-up timeline restored exactly, so that backup reliability is not sacrificed for fake anti-cheat protection.
67. As a player deleting all local data, I want contracts, debt and audit history deleted too, so that privacy rights take precedence over the no-reset metaphor.
68. As a player switching narrative systems, I want contract IDs, calculations, allocations and outcomes unchanged, so that presentation cannot become a numerical exploit.
69. As a player using assistive technology, I want rewards, costs, debt and irreversible actions expressed in text and semantics rather than color or motion alone, so that the contract remains understandable.
70. As a player who disables animations or haptics, I want every settlement to remain complete and operable, so that effects never become the correctness boundary.
71. As a player encountering a failed reconciliation write, I want the whole operation rolled back, so that consequences and audit history cannot diverge.
72. As a player tapping actions concurrently, I want database constraints and transactions to grant or assess each outcome once, so that UI debouncing is not the source of truth.

## Implementation Decisions

- Keep pure contract, consequence, progress, recovery and validation rules in `:core`; keep clocks, Room transactions, persistence, Android notifications and Compose presentation in `:app`.
- Preserve Room as the authoritative state. Do not introduce a mutable player XP balance and do not turn the display journal into a financial ledger.
- Add a versioned time-contract model with stable contract identity, quest identity, revision history, signed time, captured `ZoneId`, deadline day, task type, skill, original reward, attainable reward, extension count and lifecycle outcome.
- Model contract liability separately from fulfillment. A contract receives one mutually exclusive liability outcome: fulfilled on time, proactively abandoned, assessed overdue or exempted. An assessed overdue contract may later be fulfilled late without removing its original assessment.
- Add an immutable consequence ledger for assessments, abandonments, waivers, restitution, reward-to-debt allocations and reversals. Every event has a stable idempotency key and links to its contract, quest and completion when applicable.
- Continue using completion identity `questId + occurrence`. One-time tasks and Boss quests retain one `once` reward slot even when several successive contracts exist.
- Define original reward `R` from the existing difficulty schedule plus the existing Boss bonus and capture it at signing.
- Define attainable reward after `n` deadline extensions as the greater of 40% of `R` and `R` minus 20% of `R` per extension. Percentage results use integer ceiling so displayed and settled values match.
- Assess full overdue cost as `R`. Assess proactive abandonment as 50% of `R`, rounded upward.
- Do not cap aggregate assessments by day and do not compound one contract’s assessment by offline duration.
- Set the deadline boundary at the start of `dueDay + 1` in the contract’s captured `ZoneId`. Store instants in UTC and business days as epoch days.
- Reconcile all due contracts before every authoritative completion, undo, edit, postpone, pause, archive, restore-related mutation and state publication that claims current balances. Reconciliation and the requested mutation must have a documented transaction order.
- Make assessment idempotent under repeated startup, refresh, concurrent commands and process restart. UI busy state is not a correctness boundary.
- Treat “postpone one day” for an active dated one-time or Boss quest as an atomic one-day deadline extension plus one-day visibility snooze, one reward reduction, one revision and one audit event.
- Preserve undated postpone as visibility-only. Preserve daily postpone as skipping the current day without changing its start date or creating debt.
- Allow deadline shortening without reward reduction but retain an audit revision. Reject newly signed deadlines earlier than the current contract-time-zone day.
- On the third extension, mark the contract for review. A further extension requires proactive abandonment of the old contract and creation of a new contract.
- Treat clearing a deadline, pausing, archiving, or changing a locked task type, skill or reward basis before expiry as proactive abandonment. Allow title, description, priority and estimated-time edits with audit history while preserving the original contract snapshot.
- Close a contract normally when fulfilled on time. Later editing, pause or archive does not create liability unless the completion is revoked.
- Before undoing an on-time completion after the deadline, require explicit disclosure of the full assessment that will result. The undo reopens fulfillment, triggers the assessment and preserves the prior completion history.
- Reversing a completion also reverses its reward and recorded debt allocations. Redo restores the original completion reward snapshot and allocates it against the ledger state at redo time.
- After an overdue assessment, later pause, archive or date removal does not assess a second abandonment cost. Audit all actions without double charging.
- Allow recommitment after overdue or abandonment. Preserve the old contract and consequence, create a new contract from current task terms, and retain a single completion reward slot for the one-time occurrence.
- Derive historical earned XP from valid completion snapshots and keep a separate historical-highest-level fact. Derive current effective global XP from valid positive rewards, consequences, waivers, restitution and reversals; clamp displayed XP at zero and retain any remaining negative balance as debt.
- Allow current level to decrease but never below Lv.1. Preserve the existing level threshold formula and define a full current-level span using the existing next-level delta.
- Apply consequences only to global effective XP. Continue deriving skill XP exclusively from valid completion snapshots; undo may remove the completion contribution, but an overdue event alone cannot reduce a skill.
- Allocate all newly earned global XP to debt before current effective XP grows. Allocate first to a debt linked to the completed quest or contract, then oldest outstanding debt first. Persist allocations and their reversals for explanation and stable history.
- Define a neutral recovery state independent of narrative presentation. Any positive debt shows an out-of-order state. Open a recovery route on either three full overdue assessments in the latest 30 contract-time-zone days or debt equal to at least one full current-level span.
- Count only full overdue assessments, including post-deadline undo assessments, toward the 30-day frequency trigger. Exclude proactive abandonment, extensions and force-majeure outcomes.
- Build recovery nodes only from real existing quests selected by the player. Select enough expected reward to cover debt, emphasize no more than three at once, dynamically refill, and grant no special XP.
- Complete the recovery route when all debt is repaid or validly waived. Persist a return-to-order event and require a new full overdue assessment before the same historical window can open a new route.
- Support force majeure before or after assessment using a non-private reason category. Never delete the original assessment; append a waiver and, where necessary, restitution equal to previously repaid cost. Do not grant the quest reward.
- Provide no automatic decay, interest, annual reset, paid forgiveness, advertising forgiveness or consumable debt item.
- Persist a monotonic last-confirmed business-time boundary. A backward device-clock change cannot reverse outcomes or grant extra time; record an anomaly without networking, account enforcement or whole-app lockout.
- Upgrade Room with an explicit migration. Never use destructive fallback. Existing v1 dated tasks migrate as unsigned legacy tasks with no assessment and no active contract.
- Allow unsigned legacy tasks to complete under prior rules. Provide per-task calibration; confirming or editing a legacy deadline creates the first contract, while removing its old deadline during calibration is free.
- Upgrade the backup protocol to carry contracts, consequence events, allocations, recovery state, clock-boundary facts and unsigned legacy status. Validate the entire snapshot and budget before one-transaction replacement.
- Preserve full-snapshot restore semantics, including restoration to an older valid timeline. The preview must disclose counts and material changes, but no local penalty facts survive outside the selected snapshot.
- Full local reset deletes every new table and derived state. Do not introduce device fingerprints, external tombstones, network time, accounts or cloud retention.
- Keep canonical domain identifiers and event kinds narrative-neutral. Earth-native, cultivation and future interpreters may map labels and effects but cannot alter contract state, formulas, persistence or allocations.
- Record consequence and user-facing audit events in one transaction without storing private quest or note content in new diagnostics.
- Update capacity checks for the additional contract and ledger records before encoding or transactional replacement. Preserve bounded import behavior.

## Testing Decisions

- Test externally observable state transitions and persisted outcomes, not private helper methods or SQL implementation details.
- Use the existing pure Kotlin scenario suite as the primary seam. It should cover the complete contract state machine, formulas, time boundaries, progress derivation, debt allocation, waiver compensation and recovery-route transitions without Android or Room.
- Add table-driven reward tests for every current reward value, including Boss values, extension counts zero through three, 40% floor behavior, half-cost upward rounding and full overdue symmetry.
- Test that undated and daily quests never create deadline liability and that daily start-date semantics remain unchanged.
- Test today as the earliest valid new deadline, rejection of past signing, whole-day inclusion, exact next-day boundary, captured time-zone behavior and daylight-saving transitions.
- Test one assessment across repeated reconciliation, multi-day offline gaps and batches with several contracts due together.
- Test shortening, each of the first three extensions, fourth-extension abandonment and re-signing, deadline removal, pause, archive and locked-field changes.
- Test postpone behavior independently for dated one-time quests, dated Boss quests, undated quests and daily quests.
- Test on-time fulfillment, late fulfillment, proactive abandonment, force majeure and overdue as the contract’s exclusive liability outcomes.
- Test that an overdue contract can be fulfilled late, that its assessment remains, and that a one-time occurrence cannot mint a second reward after recommitment.
- Test post-deadline undo assessment, allocation reversal, redo snapshot preservation and redo-time allocation.
- Test effective XP, debt below zero, Lv.1 floor, current deleveling, highest-level preservation and unchanged skill XP under consequences.
- Test linked-debt-first and oldest-debt-next allocation, partial allocations, several debts, completion undo, waiver before repayment and restitution after repayment.
- Test the 30-day three-breach window, current-level-span debt trigger, exclusions for abandonment/extensions/force majeure, recovery-node selection and no immediate re-entry after return to order.
- Use Repository plus in-memory Room instrumented tests only for behavior the pure seam cannot prove: atomic reconciliation, idempotency under concurrent commands, exact transaction rollback, foreign keys, unique assessment keys and persistence after reopen.
- Add a real v1-to-v2 Room migration test using a v1 database fixture. Verify existing rows survive, dated tasks become unsigned, no penalty is created, and the exported schema comes from an actual Android build.
- Test repository ordering races at the deadline boundary: reconciliation versus completion, edit, postpone, pause, archive and undo. Exactly one explainable outcome must persist.
- Test failure injection between contract/consequence and audit writes; the transaction must leave neither partial side visible.
- Upgrade backup round-trip tests for every new record type, strict enum/id/range validation, duplicate idempotency keys, broken links, impossible allocations, future versions, capacity limits and rollback on restore failure.
- Test older valid backup restoration as complete replacement and verify the preview reports material contract and debt changes before confirmation.
- Add focused Compose instrumentation for only the irreversible user contracts: pre-save reward/cost disclosure, today-deadline warning, one-day postpone semantics, fourth-extension review, proactive-abandonment confirmation, post-deadline undo warning, force-majeure outcome and legacy calibration.
- Verify accessibility semantics announce reward, cost, debt, current versus historical level, selected action and confirmation consequence without relying on color, animation or haptics.
- Verify reduced-motion and disabled-haptics paths preserve every control and settlement result.
- Keep the existing offline core smoke command as the fast local gate and include all new pure scenarios in its existing entry point.
- Run the repository’s full Gradle gate after implementation. Run isolated-emulator instrumentation because it resets debug application data.
- Do not claim real-device, notification-delivery or store-release acceptance from pure, Room or emulator tests.

## Out of Scope

- The detailed vocabulary, iconography, palette and motion mapping for cultivation, martial-arts or other narrative interpreters.
- The final Compose layout, card-swipe gesture design, settlement animation, haptic choreography and visual polish; those belong to the subsequent Impeccable Shape and implementation work.
- Penalties for daily missed occurrences or the daily start-date field.
- Deadlines for mainlines, sub-day deadline times, timers, weekly/monthly recurrence and historical daily make-up.
- Network time, accounts, cloud synchronization, competitive leaderboards, tamper-proof anti-cheat, device fingerprinting or server-side debt retention.
- Ads, payments, consumable forgiveness items, purchasable recovery or randomized rewards.
- Automatic task generation, AI planning or fabricated recovery quests.
- Changes to the existing reward schedule, skill taxonomy or level-threshold formula beyond applying the confirmed consequence ledger.
- Retroactive assessment of unsigned 1.0 tasks.
- Store publication, production signing or claims of physical-device compatibility.

## Further Notes

- The current released behavior remains authoritative until this entire feature is implemented, migrated and tested: overdue quests are currently still completable without XP loss.
- This feature intentionally distinguishes earned history from current condition. The player can fall in current level without erasing valid past actions or the highest level previously reached.
- “Contract”, “consequence”, “debt”, “out of order” and “recovery route” are canonical neutral concepts. A narrative interpreter may render them as oath, tribulation debt, fallen path and return to cultivation without changing stored semantics.
- The next product step is a separate narrative-interpreter contract, followed by a combined Impeccable Shape for switching systems and overdue settlement. This issue should remain independently implementable at the neutral domain layer.
