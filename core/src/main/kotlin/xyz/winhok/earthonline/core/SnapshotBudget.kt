package xyz.winhok.earthonline.core

/** Conservative upper bound for the version-1 JSON envelope, including its nested string escaping. */
object SnapshotBudget {
    const val MAX_BYTES = 8 * 1024 * 1024
    // One UTF-16 unit needs at most 8 bytes after both JSON escaping layers.
    // Fixed per-row allowances cover keys, punctuation, enums and all numeric/null fields.
    fun upperBound(world: World): Long {
        fun text(vararg values: String?): Long = values.sumOf { (it?.length ?: 0).toLong() * 8 }
        var bytes = 4_096L + text(world.player.name, world.player.server, world.player.zoneId)
        for (g in world.goals) bytes += 512 + text(g.id, g.title, g.description)
        for (q in world.quests) bytes += 768 + text(q.id, q.title, q.description, q.goalId)
        for (c in world.completions) bytes += 512 + text(c.id, c.questId, c.occurrence, c.title)
        for (e in world.events) bytes += 384 + text(e.id, e.text, e.questId)
        return bytes
    }
    fun requireFits(world: World) = requireRule(upperBound(world) <= MAX_BYTES, RuleError.LIMIT)
}
