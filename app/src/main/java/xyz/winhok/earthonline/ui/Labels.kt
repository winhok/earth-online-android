package xyz.winhok.earthonline.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import xyz.winhok.earthonline.core.*

// 1.0 ships Simplified Chinese only. All enum/business error copy is centralized here.
fun QuestKind.label() = when (this) { QuestKind.SIDE -> "支线"; QuestKind.DAILY -> "日常"; QuestKind.BOSS -> "Boss" }
fun Difficulty.label() = when (this) { Difficulty.EASY -> "轻量"; Difficulty.NORMAL -> "标准"; Difficulty.HARD -> "挑战" }
fun Skill.label() = when (this) { Skill.KNOWLEDGE -> "学识"; Skill.VITALITY -> "活力"; Skill.CREATION -> "创造";
    Skill.CONNECTION -> "连接"; Skill.DISCIPLINE -> "自律" }
fun ThemeMode.label() = when (this) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "明亮"; ThemeMode.DARK -> "深色" }
fun EventKind.label() = when (this) {
    EventKind.JOINED -> "玩家上线"; EventKind.CREATED -> "接取任务"; EventKind.EDITED -> "调整任务"
    EventKind.COMPLETED -> "任务完成"; EventKind.UNDONE -> "撤销完成"; EventKind.POSTPONED -> "暂缓任务"
    EventKind.PAUSED -> "暂停任务"; EventKind.RESUMED -> "重新接取"; EventKind.ARCHIVED -> "归档任务"
    EventKind.GOAL_CREATED -> "开启主线"; EventKind.GOAL_EDITED -> "调整主线"; EventKind.GOAL_ARCHIVED -> "主线归档"
    EventKind.NOTE -> "冒险手记"; EventKind.RESTORED -> "恢复存档"
}
fun RecommendationReason.label() = when (this) {
    RecommendationReason.OVERDUE -> "已过截止日"; RecommendationReason.DUE_TODAY -> "今天截止"
    RecommendationReason.HIGH_PRIORITY -> "高优先级"; RecommendationReason.GOAL -> "推进主线"
    RecommendationReason.FITS_TIME -> "时间足够"; RecommendationReason.FITS_ENERGY -> "精力匹配"
}
fun RuleError.message() = when (this) {
    RuleError.TITLE -> "标题需为 1–120 个字符。"
    RuleError.DESCRIPTION -> "内容不能为空，且不能超过 4000 个字符。"
    RuleError.MINUTES -> "预计时长需为 1–480 分钟。"
    RuleError.PRIORITY -> "优先级需为 1–3。"
    RuleError.DATE -> "日期超出支持范围，请使用 1900–2200 年的日期。"
    RuleError.PROFILE -> "请检查玩家名、服务器名称和提醒时间。"
    RuleError.MISSING_QUEST -> "任务已不存在，请刷新后重试。"
    RuleError.INACTIVE -> "请先重新接取这个任务。"
    RuleError.NOT_AVAILABLE -> "任务尚未开始、正在暂缓或已经完成。"
    RuleError.REPEAT_LOCKED -> "已有完成记录，不能更改任务类型。请归档后创建新任务。"
    RuleError.MISSING_GOAL -> "主线不存在或已归档，请重新选择。"
    RuleError.LIMIT -> "存档已达安全容量上限，本次修改未保存。请导出备份后开启新存档。"
    RuleError.INVALID_BACKUP -> "存档校验失败，原有数据没有被修改。"
}
fun dateLabel(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
fun timeLabel(millis: Long, zone: String): String = Instant.ofEpochMilli(millis).atZone(ZoneId.of(zone))
    .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
fun Quest.meta(day: Long): String = buildList {
    add(kind.label()); add("${estimatedMinutes} 分钟"); add(skill.label())
    dueDay?.let { due -> add(if (kind == QuestKind.DAILY) "${dateLabel(due)} 起" else "${dateLabel(due)} 截止") }
    snoozedUntilDay?.let { until -> if (until > day) add("暂缓至 ${dateLabel(until)}") }
}.joinToString(" · ")
fun achievementLabel(id: String) = when (id) {
    "first_step" -> "第一步 · 完成 1 项任务"
    "ten_quests" -> "初露锋芒 · 完成 10 项"
    "hundred_quests" -> "冒险家 · 完成 100 项"
    "three_days" -> "渐入佳境 · 连续 3 天"
    "seven_days" -> "稳定前行 · 连续 7 天"
    "boss_clear" -> "突破时刻 · 完成 Boss"
    "all_rounder" -> "全面探索 · 涉及 5 种技能"
    else -> id
}
val achievementIds = listOf("first_step", "ten_quests", "hundred_quests", "three_days", "seven_days", "boss_clear", "all_rounder")
