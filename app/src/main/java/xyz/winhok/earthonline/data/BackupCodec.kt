package xyz.winhok.earthonline.data

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import xyz.winhok.earthonline.core.*

/** Explicit wire DTO mapping: obfuscation-safe; never serializes classes by reflection. */
object BackupCodec {
    const val MAX_BYTES = 8 * 1024 * 1024
    const val FORMAT = "earth-online-backup"
    const val LEGACY_VERSION = 1
    const val VERSION = 3
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

    fun encode(world: World, now: Long): String = encode(BackupSnapshot(world), now)

    fun encode(snapshot: BackupSnapshot, now: Long): String {
        val world = snapshot.world
        BackupSnapshotValidator.validate(snapshot)
        BackupSnapshotBudget.requireFits(snapshot)
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
            "narrativePreference" to obj(
                "playerId" to snapshot.narrativePreference.playerId,
                "narrativeId" to snapshot.narrativePreference.narrativeId,
            ),
            "contracts" to JSONArray(snapshot.contracts.map { c -> obj(
                "id" to c.id, "questId" to c.questId, "occurrence" to c.occurrence,
                "zoneId" to c.zoneId, "dueDay" to c.dueDay, "originalRewardXp" to c.originalRewardXp,
                "currentRewardXp" to c.currentRewardXp, "status" to c.status,
                "signedAt" to c.signedAt, "closedAt" to c.closedAt,
                "signedKind" to c.signedKind.name, "signedSkill" to c.signedSkill.name,
                "extensionCount" to c.extensionCount, "fulfilledAt" to c.fulfilledAt,
                "activeQuestId" to c.activeQuestId, "titleSnapshot" to c.titleSnapshot,
            ) }),
            "contractRevisions" to JSONArray(snapshot.contractRevisions.map { r -> obj(
                "id" to r.id, "contractId" to r.contractId, "sequence" to r.sequence,
                "kind" to r.kind, "previousDueDay" to r.previousDueDay, "newDueDay" to r.newDueDay,
                "previousRewardXp" to r.previousRewardXp, "newRewardXp" to r.newRewardXp,
                "createdAt" to r.createdAt, "idempotencyKey" to r.idempotencyKey,
            ) }),
            "consequences" to JSONArray(snapshot.consequences.map { c -> obj(
                "id" to c.id, "contractId" to c.contractId, "kind" to c.kind, "xp" to c.xp,
                "effectiveDay" to c.effectiveDay, "createdAt" to c.createdAt,
                "idempotencyKey" to c.idempotencyKey,
            ) }),
            "consequenceAdjustments" to JSONArray(snapshot.consequenceAdjustments.map { a -> obj(
                "id" to a.id, "consequenceId" to a.consequenceId, "kind" to a.kind, "xp" to a.xp,
                "reasonCategory" to a.reasonCategory, "createdAt" to a.createdAt,
                "idempotencyKey" to a.idempotencyKey,
            ) }),
            "repaymentAllocations" to JSONArray(snapshot.repaymentAllocations.map { a -> obj(
                "id" to a.id, "consequenceId" to a.consequenceId, "completionId" to a.completionId,
                "xp" to a.xp, "createdAt" to a.createdAt, "idempotencyKey" to a.idempotencyKey, "reversalOf" to a.reversalOf,
            ) }),
            "clockBoundaries" to JSONArray(snapshot.clockBoundaries.map { b -> obj(
                "id" to b.id, "zoneId" to b.zoneId, "lastSettledDay" to b.lastSettledDay,
                "lastSettledAt" to b.lastSettledAt,
            ) }),
            "recoveryRoutes" to JSONArray(snapshot.recoveryRoutes.map { r -> obj(
                "id" to r.id, "status" to r.status, "triggerKind" to r.triggerKind,
                "targetDebtXp" to r.targetDebtXp, "openedAt" to r.openedAt,
                "closedAt" to r.closedAt, "idempotencyKey" to r.idempotencyKey, "closedThroughAssessmentCount" to r.closedThroughAssessmentCount,
            ) }),
            "settlementReceipts" to JSONArray(snapshot.settlementReceipts.map {
                obj("consequenceId" to it.consequenceId, "acknowledgedAt" to it.acknowledgedAt)
            }),
            "progressHistory" to obj("id" to snapshot.progressHistory.id, "highestLevel" to snapshot.progressHistory.highestLevel),
            "effects" to obj("id" to snapshot.effects.id, "sound" to snapshot.effects.sound, "haptics" to snapshot.effects.haptics, "reducedMotion" to snapshot.effects.reducedMotion),
            "presentationPreferences" to JSONArray(snapshot.presentationPreferences.map { obj("narrativeId" to it.narrativeId, "preferenceKey" to it.preferenceKey, "enabled" to it.enabled) }),
            "recoveryNodes" to JSONArray(snapshot.recoveryNodes.map { n -> obj(
                "routeId" to n.routeId, "questId" to n.questId, "position" to n.position,
                "addedAt" to n.addedAt, "completedAt" to n.completedAt,
            ) }),
        ).toString()
        val encoded = obj("format" to FORMAT, "version" to VERSION, "exportedAt" to now,
            "sha256" to hash(payload), "payload" to payload).toString(2)
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is larger than 8 MiB" }
        return encoded
    }

    fun decode(text: String): World = decodeSnapshot(text).world

    fun decodeSnapshot(text: String): BackupSnapshot {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.strictString("format") == FORMAT)
        val version = root.strictInt("version")
        require(version in LEGACY_VERSION..VERSION)
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
        if (version <= 2) BackupValidator.validate(world)
        if (version == LEGACY_VERSION) return BackupSnapshot(world).also(BackupSnapshotValidator::validate)

        val preference = data.getJSONObject("narrativePreference")
        return BackupSnapshot(
            world = world,
            narrativePreference = NarrativePreferenceEntity(
                preference.strictInt("playerId"),
                preference.strictString("narrativeId"),
            ),
            contracts = data.getJSONArray("contracts").mapObjects { c -> ContractEntity(
                c.strictString("id"), c.strictString("questId"), c.strictString("occurrence"),
                c.strictString("zoneId"), c.strictLong("dueDay"), c.strictLong("originalRewardXp"),
                c.strictLong("currentRewardXp"), c.strictString("status"), c.strictLong("signedAt"),
                c.nullLong("closedAt"),
                if(version>=3) QuestKind.valueOf(c.strictString("signedKind")) else world.quests.first { it.id==c.strictString("questId") }.kind,
                if(version>=3) Skill.valueOf(c.strictString("signedSkill")) else world.quests.first { it.id==c.strictString("questId") }.skill,
                if(version>=3) c.strictInt("extensionCount") else ContractMath.inferExtensions(c.strictLong("originalRewardXp"),c.strictLong("currentRewardXp")),
                if(version>=3) c.nullLong("fulfilledAt") else if(c.strictString("status")=="FULFILLED") c.nullLong("closedAt") else null,
                if(version>=3) c.nullString("activeQuestId") else if(c.strictString("status")=="ACTIVE") c.strictString("questId") else null,
                if(version>=3) c.strictString("titleSnapshot") else "",
            ) },
            contractRevisions = data.getJSONArray("contractRevisions").mapObjects { r -> ContractRevisionEntity(
                r.strictString("id"), r.strictString("contractId"), r.strictInt("sequence"),
                r.strictString("kind"), r.nullLong("previousDueDay"), r.nullLong("newDueDay"),
                r.strictLong("previousRewardXp"), r.strictLong("newRewardXp"), r.strictLong("createdAt"),
                r.strictString("idempotencyKey"),
            ) },
            consequences = data.getJSONArray("consequences").mapObjects { c -> ConsequenceEventEntity(
                c.strictString("id"), c.strictString("contractId"), c.strictString("kind"),
                c.strictLong("xp"), c.strictLong("effectiveDay"), c.strictLong("createdAt"),
                c.strictString("idempotencyKey"),
            ) },
            consequenceAdjustments = data.getJSONArray("consequenceAdjustments").mapObjects { a ->
                ConsequenceAdjustmentEntity(
                    a.strictString("id"), a.strictString("consequenceId"), a.strictString("kind"),
                    a.strictLong("xp"), a.nullString("reasonCategory"), a.strictLong("createdAt"),
                    a.strictString("idempotencyKey"),
                )
            },
            repaymentAllocations = data.getJSONArray("repaymentAllocations").mapObjects { a ->
                RepaymentAllocationEntity(
                    a.strictString("id"), a.strictString("consequenceId"), a.strictString("completionId"),
                    a.strictLong("xp"), a.strictLong("createdAt"), a.strictString("idempotencyKey"), if(version>=3) a.nullString("reversalOf") else null,
                )
            },
            clockBoundaries = data.getJSONArray("clockBoundaries").mapObjects { b -> ClockBoundaryEntity(
                b.strictString("id"), b.strictString("zoneId"), b.strictLong("lastSettledDay"),
                b.strictLong("lastSettledAt"),
            ) },
            recoveryRoutes = data.getJSONArray("recoveryRoutes").mapObjects { r -> RecoveryRouteEntity(
                r.strictString("id"), r.strictString("status"), r.strictString("triggerKind"),
                r.strictLong("targetDebtXp"), r.strictLong("openedAt"), r.nullLong("closedAt"),
                r.strictString("idempotencyKey"), if(version>=3) r.strictInt("closedThroughAssessmentCount") else 0,
            ) },
            settlementReceipts = if (version >= 3) data.getJSONArray("settlementReceipts").mapObjects {
                SettlementReceiptEntity(it.strictString("consequenceId"), it.strictLong("acknowledgedAt"))
            } else emptyList(),
            progressHistory = if(version>=3) data.getJSONObject("progressHistory").let { ProgressHistoryEntity(it.strictInt("id"),it.strictInt("highestLevel")) } else ProgressHistoryEntity(),
            effects = if(version>=3) data.getJSONObject("effects").let { EffectPreferencesEntity(it.strictInt("id"),it.strictBoolean("sound"),it.strictBoolean("haptics"),it.strictBoolean("reducedMotion")) } else EffectPreferencesEntity(),
            presentationPreferences = if(version>=3) data.getJSONArray("presentationPreferences").mapObjects { PresentationPreferenceEntity(it.strictString("narrativeId"),it.strictString("preferenceKey"),it.strictBoolean("enabled")) } else emptyList(),
            recoveryNodes = data.getJSONArray("recoveryNodes").mapObjects { n -> RecoveryNodeEntity(
                n.strictString("routeId"), n.strictString("questId"), n.strictInt("position"),
                n.strictLong("addedAt"), n.nullLong("completedAt"),
            ) },
        ).also(BackupSnapshotValidator::validate)
            .also(BackupSnapshotBudget::requireFits)
    }
}
