package xyz.winhok.earthonline.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun dateLabel(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
