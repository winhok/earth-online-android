package xyz.winhok.earthonline.core

/** Additional v3 invariants on top of the compatible v2 graph checks. */
object DeadlineValidation {
    fun validate(state: DeadlineState) {
        val (world, book) = state
        requireRule(book.highestLevel in 1..ProgressRules.fromXp(3_750_000).level, RuleError.INVALID_BACKUP)
        requireRule(book.routes.count { it.status == "OPEN" } <= 1, RuleError.INVALID_BACKUP)
        book.contracts.forEach { c ->
            requireRule(c.extensionCount in 0..3 && c.currentRewardXp == ContractMath.attainable(c.originalRewardXp,c.extensionCount), RuleError.INVALID_BACKUP)
            requireRule(c.signedKind != QuestKind.DAILY && c.originalRewardXp in
                (if(c.signedKind==QuestKind.BOSS) setOf(35L,50L,75L) else setOf(10L,25L,50L)), RuleError.INVALID_BACKUP)
            requireRule(c.fulfilledAt == null || c.fulfilledAt >= c.signedAt, RuleError.INVALID_BACKUP)
            if(c.status=="ACTIVE") {
                val q=world.quests.first { it.id==c.questId }
                requireRule(q.kind==c.signedKind && q.skill==c.signedSkill && q.dueDay==c.dueDay && q.state==QuestState.ACTIVE,RuleError.INVALID_BACKUP)
            }
        }
        book.liabilities.forEach { l ->
            val c=book.contracts.first { it.id==l.contractId }
            requireRule(l.xp == if(l.kind=="OVERDUE") c.originalRewardXp else ContractMath.abandonment(c.originalRewardXp),RuleError.INVALID_BACKUP)
        }
        book.routes.forEach { r ->
            requireRule(r.closedThroughAssessmentCount in 0..book.liabilities.count { it.kind=="OVERDUE" },RuleError.INVALID_BACKUP)
        }
        requireRule(book.nodes.groupBy { it.routeId }.values.all { it.map { n -> n.position }.distinct().size==it.size },RuleError.INVALID_BACKUP)
    }
    fun rewardAllowList(book: DeadlineBook): Map<String, Set<Int>> = book.contracts.groupBy { it.questId }
        .mapValues { (_,cs) -> cs.flatMap { c -> (0..c.extensionCount).map { ContractMath.attainable(c.originalRewardXp,it).toInt() } }.toSet() }
}
