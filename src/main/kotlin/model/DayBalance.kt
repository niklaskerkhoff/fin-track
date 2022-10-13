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
    val amount: Float,
    val type: String,
    val description: String,
    val instance: String
)


data class DayBalance(
    val date: LocalDate,
    val balance: Float,
    val entries: List<Entry>
)