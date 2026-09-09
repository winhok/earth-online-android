package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ContractSemantic(override val wireId: String): SemanticKey {
    TITLE("screen.contract-title"), DISCLOSURE("screen.contract-disclosure"), INFO("screen.contract-info"),
    SUMMARY("screen.contract-summary"), CALIBRATE("screen.contract-calibrate"), UNSIGNED("screen.contract-unsigned"),
    REVIEW("screen.contract-review"), RULES("screen.contract-rules"), CONFIRM("screen.contract-confirm"),
    FORCE_REASON("screen.force-reason"), REASON_HEALTH("screen.reason-health"), REASON_FAMILY("screen.reason-family"),
    REASON_EXTERNAL("screen.reason-external"), REASON_OTHER("screen.reason-other"),
    RECOVERY_BODY("screen.recovery-body"), RECOVERY_SELECTION("screen.recovery-selection"),
    RECOVERY_EMPTY("screen.recovery-empty"), RECOVERY_SELECT("screen.recovery-select"),
    LEDGER_TITLE("screen.ledger-title"), LEDGER_ENTRY("screen.ledger-entry"), LEDGER_EMPTY("screen.ledger-empty"),
    CLOCK_WARNING("screen.clock-warning"), SETTLEMENT("screen.settlement"),
    EFFECTS("screen.effects"), SOUND("screen.sound"), HAPTICS("screen.haptics"), REDUCED_MOTION("screen.reduced-motion"),
    EFFECTS_NOTICE("screen.effects-notice"), STORY_TITLE("screen.story-title"), STORY_BODY("screen.story-body"),
    STORY_COLLAPSE("screen.story-collapse"), STORY_EXPAND("screen.story-expand"), STORY_READ("screen.story-read"),
    INTRO_PREVIEW("screen.intro-preview"), ACTIVE_XP("screen.active-xp"), HISTORICAL_XP("screen.historical-xp"),
    HIGHEST_LEVEL("screen.highest-level"), POSTPONE_SIGNED("screen.postpone-signed"),
    ;
    override val category = SemanticCategory.SCREEN
}

data class ContractCardData(val status: String, val dueDay: Long, val zoneId: String,
    val originalReward: Long, val attainableReward: Long, val extensions: Int, val fulfilledLate: Boolean)
data class RecoverySelectionData(val selectedXp: Long, val debtXp: Long, val count: Int)
data class SettlementData(val awardedXp: Int, val repaidXp: Long, val level: Int, val previousLevel: Int,
    val achievements: Int = 0)
data class LedgerLine(val id: String, val kind: String, val title: String, val xp: Long, val createdAt: Long,
    val referenceId: String, val zoneId: String, val detail: String = "")

object ContractParameters {
    val DISCLOSURE = SemanticParameter<ContractDisclosure>("contract-disclosure")
    val CARD = SemanticParameter<ContractCardData>("contract-card")
    val SUMMARY = SemanticParameter<EffectiveProgress>("effective-progress")
    val SELECTION = SemanticParameter<RecoverySelectionData>("recovery-selection")
    val SETTLEMENT = SemanticParameter<SettlementData>("settlement")
    val LEDGER = SemanticParameter<LedgerLine>("ledger-entry")
}

/** Read-only projection. IDs and arithmetic come from the ledger, never from the current skin. */
fun DeadlineState.ledgerLines(): List<LedgerLine> {
    val contracts=book.contracts.associateBy { it.id }
    val liabilities=book.liabilities.associateBy { it.id }
    fun title(c: TimeContract?) = c?.let { contract ->
        world.events.filter { it.questId==contract.questId && it.createdAt<=contract.signedAt && it.kind in setOf(EventKind.CREATED,EventKind.EDITED) }
            .maxByOrNull { it.createdAt }?.text ?: world.quests.firstOrNull { it.id==contract.questId }?.title
    }.orEmpty()
    return buildList {
        book.revisions.forEach { r -> val c=contracts[r.contractId]; add(LedgerLine(r.id,r.kind,title(c),r.newRewardXp,r.createdAt,r.contractId,c?.zoneId ?: world.player.zoneId)) }
        book.liabilities.forEach { l -> val c=contracts[l.contractId]; add(LedgerLine(l.id,l.kind,title(c),l.xp,l.createdAt,l.contractId,c?.zoneId ?: world.player.zoneId)) }
        book.adjustments.forEach { a -> val c=contracts[liabilities[a.consequenceId]?.contractId]; add(LedgerLine(a.id,a.kind,title(c),a.xp,a.createdAt,a.consequenceId,c?.zoneId ?: world.player.zoneId,a.reasonCategory.orEmpty())) }
        book.allocations.forEach { a -> val c=contracts[liabilities[a.consequenceId]?.contractId]; val t=world.completions.firstOrNull { it.id==a.completionId }?.title.orEmpty(); add(LedgerLine(a.id,if(a.reversalOf==null)"ALLOCATION" else "REVERSAL",t,a.xp,a.createdAt,a.consequenceId,c?.zoneId ?: world.player.zoneId)) }
        book.routes.forEach { r -> add(LedgerLine("${r.id}:open","RECOVERY_OPEN", "",r.targetDebtXp,r.openedAt,r.id,world.player.zoneId)); r.closedAt?.let { add(LedgerLine("${r.id}:closed","RECOVERY_CLOSED","",0,it,r.id,world.player.zoneId)) } }
        book.clocks.filter { it.id.startsWith("clock-warning:") }.forEach { c -> add(LedgerLine(c.id,"CLOCK_BACKWARD","",0,c.lastSettledAt,c.id,c.zoneId)) }
    }.sortedWith(compareByDescending<LedgerLine> { it.createdAt }.thenBy { it.id })
}

object RealmTitles {
    fun at(level: Int): String = when {
        level<5 -> "炼体"; level<10 -> "练气"; level<20 -> "筑基"; level<35 -> "金丹";
        level<55 -> "元婴"; level<80 -> "化神"; level<110 -> "炼虚"; level<150 -> "合体";
        level<200 -> "大乘"; else -> "真仙"
    }
}

internal object DeadlineNarrative {
    fun entries(cultivation: Boolean): Map<SemanticKey,NarrativeSemanticEntry> {
        val xp=if(cultivation) "修为" else "XP"
        val contract=if(cultivation) "天命契约" else "时间契约"
        val debt=if(cultivation) "劫债" else "待偿债务"
        val recovery=if(cultivation) "重修仙途" else "恢复路线"
        fun text(value: String,role: IconRole=IconRole.NEUTRAL) = NarrativeSemanticEntry(emptySet()) { NarrativePresentation(value,role) }
        val map=linkedMapOf<SemanticKey,NarrativeSemanticEntry>(
            ContractSemantic.TITLE to text(contract,IconRole.WARNING),
            ContractSemantic.CALIBRATE to text(if(cultivation) "校准旧天命 / 重新立契" else "校准旧日期 / 重新签约"),
            ContractSemantic.UNSIGNED to text("旧任务 · 未签约。未经你确认，不追罚；仍可按原规则完成。"),
            ContractSemantic.REVIEW to text("已延期三次：请重新决策。再次延期将结算旧契约 50% 原始奖励的代价，并签订新契约。"),
            ContractSemantic.RULES to text("有截止日期的支线与 Boss 需确认契约：逾期一次扣原始奖励 100%；提前清除日期、暂停或归档扣 50%（向上取整）。延期每次减少原始奖励 20%，最低保留 40%；日常任务与无日期任务不产生债务。所有代价都会先明确展示。"),
            ContractSemantic.CONFIRM to text("理解上述数值并确认",IconRole.COMMAND),
            ContractSemantic.FORCE_REASON to text("选择原因类别即可，不需填写病情或家庭细节。原记录保留，通过补偿记录解除代价。"),
            ContractSemantic.REASON_HEALTH to text("健康原因"), ContractSemantic.REASON_FAMILY to text("照护 / 家庭"),
            ContractSemantic.REASON_EXTERNAL to text("外部意外"), ContractSemantic.REASON_OTHER to text("其他现实变故"),
            ContractSemantic.RECOVERY_BODY to text("选择真实、可执行的任务，预计奖励需覆盖债务。最多突出显示三项，完成后自动补位；没有额外奖励。债务全部偿清或豁免后，这条路线自动结束。"),
            ContractSemantic.RECOVERY_EMPTY to text("还没有足够的可执行任务。先创建几个能实际完成的小步骤，再回来选择。"),
            ContractSemantic.RECOVERY_SELECT to text(if(cultivation) "选择重修节点" else "选择恢复任务",IconRole.RECOVERY),
            ContractSemantic.LEDGER_TITLE to text(if(cultivation) "因果账簿" else "契约与结算账本",IconRole.JOURNAL),
            ContractSemantic.LEDGER_EMPTY to text("尚无契约结算。这里保留签约、改期、代价、偿还、撤销与现实豁免的记录。"),
            ContractSemantic.CLOCK_WARNING to text("检测到时钟回退：已结算结果不变，契约以已观察到的最晚时间继续判断；不锁定使用。恢复完整旧存档会恢复它自己的时间线。",IconRole.WARNING),
            ContractSemantic.EFFECTS to text("结算反馈"), ContractSemantic.SOUND to text("声音（默认关闭）"),
            ContractSemantic.HAPTICS to text("触觉（遵循系统设置）"),ContractSemantic.REDUCED_MOTION to text("减少动态效果"),
            ContractSemantic.EFFECTS_NOTICE to text("设置即时保存。关闭声音、触觉或动态效果，不会隐藏结算信息或改变奖励。"),
            ContractSemantic.STORY_TITLE to text(if(cultivation) "唯一仙途 · 单向光阴" else "唯一服务器 · 单向时间"),
            ContractSemantic.STORY_BODY to text(if(cultivation)
                "此界没有重开键。每一次现实行动留下修为，每一道天命都在立契前明示代价。境界会起伏，根基由行动积累；失序不是身份，重修始终有路。你的任务原文、数值与存档从未改变。"
                else "这里记录的是现实行动，不是你的价值。时间只能向前，但任务可以拆小、重新选择，现实变故也可以被如实记录。你拥有整份离线存档；切换叙事不改变任何领域事实。"),
            ContractSemantic.STORY_COLLAPSE to text("收起世界介绍"),ContractSemantic.STORY_EXPAND to text("展开世界介绍"),
            ContractSemantic.STORY_READ to text("已读，继续行动"),ContractSemantic.INTRO_PREVIEW to text("可选：预览叙事体系"),
            ContractSemantic.ACTIVE_XP to text(if(cultivation) "当前有效修为" else "当前有效 XP"),
            ContractSemantic.HISTORICAL_XP to text(if(cultivation) "历史行动修为" else "历史行动 XP"),
            ContractSemantic.POSTPONE_SIGNED to text(if(cultivation) "改命一日（先查看奖励变化）" else "延期一天（先查看奖励变化）"),
            ActionSemantic.RESCHEDULE_CONTRACT to text(if(cultivation) "改命 / 编辑日期" else "编辑契约日期"),
            ActionSemantic.ABANDON_CONTRACT to text(if(cultivation) "解契 / 暂停任务" else "解约 / 暂停任务"),
            ActionSemantic.RECOMMIT_CONTRACT to text(if(cultivation) "重新立契" else "重新签约"),
            ActionSemantic.SETTLE_OVERDUE to text("核对当前结算"),
            ActionSemantic.VIEW_DEBT to text(if(cultivation) "查看劫债与来路" else "查看债务及来源"),
            ActionSemantic.OPEN_RECOVERY to text(recovery,IconRole.RECOVERY),
            ActionSemantic.SELECT_RECOVERY_NODE to text("确认选中任务"),
            ActionSemantic.APPLY_FORCE_MAJEURE to text(if(cultivation) "尘世变故 / 现实豁免" else "不可抗力 / 现实豁免"),
            StateSemantic.CONTRACT_ACTIVE to text(if(cultivation) "天命在行" else "契约生效中"),
            StateSemantic.CONTRACT_FULFILLED to text("按时履约"),StateSemantic.CONTRACT_OVERDUE to text("逾期代价已结算"),
            StateSemantic.CONTRACT_ABANDONED to text("主动解约已结算"),StateSemantic.CONTRACT_EXEMPTED to text("现实变故 · 已豁免"),
            StateSemantic.DEBT to text(debt),StateSemantic.OUT_OF_ORDER to text(if(cultivation) "道心失序 · 可恢复" else "失序 · 可恢复"),
            StateSemantic.RECOVERY_OPEN to text(recovery),StateSemantic.RECOVERY_CLOSED to text(if(cultivation) "归正 · 仙途重续" else "归正 · 恢复完成"),
        )
        map[ContractSemantic.DISCLOSURE]=NarrativeSemanticEntry(setOf(ContractParameters.DISCLOSURE)) { a ->
            val d=a.require(ContractParameters.DISCLOSURE)
            NarrativePresentation(buildList {
                add(d.title)
                if(DisclosureKind.POST_DEADLINE_UNDO in d.kinds) add("撤销会移除原完成奖励 ${d.reward} $xp，并因原截止日期已过结算 ${d.immediateCost} $xp 代价。重做保留原奖励快照。")
                else if(d.immediateCost>0) add("本次先结算旧契约代价 ${d.immediateCost} $xp。")
                if(d.reward>0 && DisclosureKind.POST_DEADLINE_UNDO !in d.kinds) add("可获奖励 ${d.reward} $xp；逾期代价 ${d.overdueCost} $xp，仅结算一次，不按离线天数累加。")
                d.dueDay?.let { add("截止 ${LocalDate.ofEpochDay(it)} 当日结束 · ${d.zoneId}") }
                if(d.dueToday) add("今天截止：只剩今天的剩余时间，不是完整 24 小时。")
                if(d.extensionCount>0) add("这是第 ${d.extensionCount} 次延期；新奖励仅影响尚未取得的奖励，不重写已有完成快照。")
                if(d.extensionCount>=3) add("第三次延期后进入重审。再次延期需要解约并重新承诺。")
                if(DisclosureKind.FORCE_MAJEURE in d.kinds) add("豁免未偿代价 ${d.waivedXp} $xp；返还已偿代价 ${d.restitutionXp} $xp。不发放任务完成奖励，原始账目保留。")
                add("取消不会执行本次操作。")
            }.joinToString("\n\n"),IconRole.WARNING,ColorRole.WARNING)
        }
        map[ContractSemantic.INFO]=NarrativeSemanticEntry(setOf(ContractParameters.CARD)) { a -> val c=a.require(ContractParameters.CARD)
            val status=when(c.status){"ACTIVE"->"生效中";"FULFILLED"->"按时履约";"OVERDUE"->"逾期已结算";"ABANDONED"->"主动解约";"EXEMPTED"->"现实豁免";else->c.status}
            NarrativePresentation("$contract · $status${if(c.fulfilledLate) " · 后续行动已完成" else ""}\n${LocalDate.ofEpochDay(c.dueDay)} 当日结束 · ${c.zoneId}\n可获 ${c.attainableReward} $xp / 原始 ${c.originalReward} $xp · 延期 ${c.extensions} 次",IconRole.WARNING)
        }
        map[ContractSemantic.SUMMARY]=NarrativeSemanticEntry(setOf(ContractParameters.SUMMARY)) { a -> val p=a.require(ContractParameters.SUMMARY)
            NarrativePresentation("$debt ${p.debtXp} $xp · 当前有效 ${p.current.xp} $xp\n历史行动 ${p.earnedXp} $xp · 历史最高 Lv.${p.highestLevel}",if(p.debtXp>0) IconRole.WARNING else IconRole.CHARACTER)
        }
        map[ContractSemantic.RECOVERY_SELECTION]=NarrativeSemanticEntry(setOf(ContractParameters.SELECTION)) { a ->val s=a.require(ContractParameters.SELECTION)
            NarrativePresentation("已选 ${s.count} 项 · 预计 ${s.selectedXp} $xp / $debt ${s.debtXp} $xp")
        }
        map[ContractSemantic.SETTLEMENT]=NarrativeSemanticEntry(setOf(ContractParameters.SETTLEMENT)) { a ->val s=a.require(ContractParameters.SETTLEMENT)
            NarrativePresentation("${if(cultivation) "历练结算" else "任务结算"} · +${s.awardedXp} $xp\n偿还 $debt ${s.repaidXp} $xp · 当前 Lv.${s.level}" +
                (if(s.level>s.previousLevel) " · ${if(cultivation) "突破" else "升级"}" else "") +
                (if(s.achievements>0) " · 新成就 ${s.achievements} 项" else ""),IconRole.COMPLETION,ColorRole.REWARD,EffectRole.COMPLETION)
        }
        map[ContractSemantic.HIGHEST_LEVEL]=NarrativeSemanticEntry(setOf(SemanticParameters.LEVEL)) { a ->NarrativePresentation("历史最高 Lv.${a.require(SemanticParameters.LEVEL).value}") }
        map[ContractSemantic.LEDGER_ENTRY]=NarrativeSemanticEntry(setOf(ContractParameters.LEDGER)) { a ->val l=a.require(ContractParameters.LEDGER)
            val action=when(l.kind){"SIGNED"->"签订$contract";"POSTPONED"->"延期 · 调整可获奖励";"RESCHEDULED"->"缩短日期";"EDITED"->"编辑行动说明";"OVERDUE"->"逾期代价";"ABANDONED"->"主动解约";"WAIVER"->"现实豁免";"RESTITUTION"->"返还已偿代价";"REFUND"->"历史退款记录";"ALLOCATION"->"偿还$debt";"REVERSAL"->"撤销偿还分配";"RECOVERY_OPEN"->"开启$recovery";"RECOVERY_CLOSED"->"归正 · 恢复完成";"CLOCK_BACKWARD"->"已记录时钟回退";else->if(l.kind.startsWith("EXEMPTED_")) "现实变故 · 契约豁免" else l.kind}
            val time=Instant.ofEpochMilli(l.createdAt).atZone(ZoneId.of(l.zoneId)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            NarrativePresentation("$action · ${l.xp} $xp\n${l.title}\n$time\nID ${l.referenceId}",IconRole.JOURNAL)
        }
        check(map.keys.containsAll(ContractSemantic.entries))
        return map
    }
}
