package model

import java.time.LocalDate

data class PlainEntry(
    val amount: String,
    val type: String,
    val description: String,
    val instance: String
)


data class PlainDayBalance(
    val date: String,
    val balance: String,
    val entries: List<PlainEntry>
)

data class Entry(
    val amount: Double,
    val type: String,
    val description: String,
    val instance: String
)


data class DayBalance(
    val date: LocalDate,
    val balance: Double,
    val entries: List<Entry>
)

data class DayBalancesOverview(
    val month: Int,
    val start: Double,
    val end: Double,
    val entries: Map<String, Double>
)
