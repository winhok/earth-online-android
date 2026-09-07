package xyz.winhok.earthonline.data

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import xyz.winhok.earthonline.core.*

/** Explicit wire DTO mapping: obfuscation-safe; never serializes classes by reflection. */
object BackupCodec {
    const val MAX_BYTES = 8 * 1024 * 1024
    const val FORMAT = "earth-online-backup"
    const val VERSION = 1
    private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().apply {
        pairs.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }
    private fun hash(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    // JSONObject getters may coerce strings or truncate decimals; the wire protocol must not.
    private fun JSONObject.strictString(key: String): String =
        get(key) as? String ?: throw IllegalArgumentException("Expected string: $key")
    private fun JSONObject.strictBoolean(key: String): Boolean =
        get(key) as? Boolean ?: throw IllegalArgumentException("Expected boolean: $key")
    private fun JSONObject.strictLong(key: String): Long {
        val value = get(key)
        require(value is Int || value is Long) { "Expected integral number: $key" }
        return (value as Number).toLong()
    }
    private fun JSONObject.strictInt(key: String): Int {
        val value = strictLong(key)
        require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Integer overflow: $key" }
        return value.toInt()
    }
    private fun JSONObject.nullString(key: String): String? {
        require(has(key)) { "Missing nullable field: $key" }
        return if (isNull(key)) null else strictString(key)
    }
    private fun JSONObject.nullLong(key: String): Long? {
        require(has(key)) { "Missing nullable field: $key" }
        return if (isNull(key)) null else strictLong(key)
    }
    private fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> =
        (0 until length()).map { block(getJSONObject(it)) }

    fun encode(world: World, now: Long): String {
        BackupValidator.validate(world)
        val p = world.player
        val payload = obj(
            "player" to obj("name" to p.name, "server" to p.server, "zoneId" to p.zoneId,
                "joinedAt" to p.joinedAt, "onboarded" to p.onboarded, "theme" to p.theme.name,
                "remindersEnabled" to p.remindersEnabled, "reminderHour" to p.reminderHour,
                "lastReminderDay" to p.lastReminderDay),
            "goals" to JSONArray(world.goals.map { g -> obj("id" to g.id, "title" to g.title,
                "description" to g.description, "archived" to g.archived, "createdAt" to g.createdAt) }),
            "quests" to JSONArray(world.quests.map { q -> obj("id" to q.id, "title" to q.title,
                "description" to q.description, "kind" to q.kind.name, "difficulty" to q.difficulty.name,
                "skill" to q.skill.name, "priority" to q.priority, "estimatedMinutes" to q.estimatedMinutes,
                "dueDay" to q.dueDay, "goalId" to q.goalId, "state" to q.state.name,
                "snoozedUntilDay" to q.snoozedUntilDay, "postponeCount" to q.postponeCount,
                "createdAt" to q.createdAt, "updatedAt" to q.updatedAt) }),
            "completions" to JSONArray(world.completions.map { c -> obj("id" to c.id, "questId" to c.questId,
                "occurrence" to c.occurrence, "title" to c.title, "kind" to c.kind.name,
                "skill" to c.skill.name, "xp" to c.xp, "completedAt" to c.completedAt,
                "completedDay" to c.completedDay, "revokedAt" to c.revokedAt) }),
            "events" to JSONArray(world.events.map { e -> obj("id" to e.id, "kind" to e.kind.name,
                "text" to e.text, "createdAt" to e.createdAt, "questId" to e.questId, "xp" to e.xp) }),
        ).toString()
        val encoded = obj("format" to FORMAT, "version" to VERSION, "exportedAt" to now,
            "sha256" to hash(payload), "payload" to payload).toString(2)
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is larger than 8 MiB" }
        return encoded
    }

    fun decode(text: String): World {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.strictString("format") == FORMAT && root.strictInt("version") == VERSION)
        require(root.strictLong("exportedAt") >= 0) { "Invalid export timestamp" }
        val raw = root.strictString("payload")
        require(MessageDigest.isEqual(hash(raw).toByteArray(Charsets.US_ASCII),
            root.strictString("sha256").toByteArray(Charsets.US_ASCII))) { "Checksum mismatch" }
        val data = JSONObject(raw)
        val p = data.getJSONObject("player")
        val world = World(
            player = Player(p.strictString("name"), p.strictString("server"), p.strictString("zoneId"),
                p.strictLong("joinedAt"), p.strictBoolean("onboarded"), ThemeMode.valueOf(p.strictString("theme")),
                p.strictBoolean("remindersEnabled"), p.strictInt("reminderHour"), p.nullLong("lastReminderDay")),
            goals = data.getJSONArray("goals").mapObjects { g -> Goal(g.strictString("id"), g.strictString("title"),
                g.strictString("description"), g.strictBoolean("archived"), g.strictLong("createdAt")) },
            quests = data.getJSONArray("quests").mapObjects { q -> Quest(
                q.strictString("id"), q.strictString("title"), q.strictString("description"),
                QuestKind.valueOf(q.strictString("kind")), Difficulty.valueOf(q.strictString("difficulty")),
                Skill.valueOf(q.strictString("skill")), q.strictInt("priority"), q.strictInt("estimatedMinutes"),
                q.nullLong("dueDay"), q.nullString("goalId"), QuestState.valueOf(q.strictString("state")),
                q.nullLong("snoozedUntilDay"), q.strictInt("postponeCount"), q.strictLong("createdAt"), q.strictLong("updatedAt")) },
            completions = data.getJSONArray("completions").mapObjects { c -> Completion(c.strictString("id"),
                c.strictString("questId"), c.strictString("occurrence"), c.strictString("title"),
                QuestKind.valueOf(c.strictString("kind")), Skill.valueOf(c.strictString("skill")), c.strictInt("xp"),
                c.strictLong("completedAt"), c.strictLong("completedDay"), c.nullLong("revokedAt")) },
            events = data.getJSONArray("events").mapObjects { e -> JournalEvent(e.strictString("id"),
                EventKind.valueOf(e.strictString("kind")), e.strictString("text"), e.strictLong("createdAt"),
                e.nullString("questId"), e.strictInt("xp")) },
        )
        BackupValidator.validate(world)
        return world
    }
}
