package xyz.winhok.earthonline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** This is a historical fact, never a spendable XP balance. */
@Entity(tableName = "progress_history")
data class ProgressHistoryEntity(@PrimaryKey val id: Int = 1, val highestLevel: Int = 1)

@Entity(tableName = "effect_preferences")
data class EffectPreferencesEntity(@PrimaryKey val id: Int = 1,
    val sound: Boolean = false, val haptics: Boolean = true, val reducedMotion: Boolean = false)

/** Whitelisted booleans only: presentation records cannot hold XP, debt or commands. */
@Entity(tableName = "presentation_preferences", primaryKeys = ["narrativeId", "preferenceKey"])
data class PresentationPreferenceEntity(val narrativeId: String, val preferenceKey: String, val enabled: Boolean)
