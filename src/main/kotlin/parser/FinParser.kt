package parser

import model.DayBalance
import model.Entry
import model.PlainDayBalance
import model.PlainEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FinParser : Parser {

    private val dateRegexString =
        """(Heute|Gestern|Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag), [\d.]+"""
    private val balanceRegexString = """Kontostand [\d.,\-]+ €"""
    private val amountRegexString = """-?\d+,\d+ €"""

    private val amountRegex = getExactRegex(amountRegexString)

    private fun getExactRegex(regex: String): Regex {
        return Regex("^${regex}${'$'}")
    }

    private fun hasDateAndBalanceHeader(dayBalanceSplits: List<String>): Boolean {
        if (dayBalanceSplits.size < 2) {
            return false
        }
        if (!dayBalanceSplits[0].matches(getExactRegex(dateRegexString))) {
            return false
        }
        if (!dayBalanceSplits[1].matches(getExactRegex(balanceRegexString))) {
            return false
        }
        return true
    }

    private fun getLastIndexHavingAmount(dayBalanceSplits: List<String>): Int? {
        var lastIndex = dayBalanceSplits.size - 1
        while (lastIndex >= 0 && dayBalanceSplits[lastIndex].matches(amountRegex)) {
            lastIndex--
        }

        return if (lastIndex < 2) null else lastIndex
    }

    private fun isLastDayBalanceSplitAmount(dayBalanceSplits: List<String>): Boolean {
        return dayBalanceSplits.last().matches(amountRegex)
    }

    private fun getGroupedEntrySplits(entrySplits: List<String>): List<List<String>> {
        val groupedEntrySplits = mutableListOf<MutableList<String>>()
        for (split in entrySplits) {
            if (split.matches(amountRegex)) {
                groupedEntrySplits.add(mutableListOf())
            }
            groupedEntrySplits.last().add(split)
        }
        return groupedEntrySplits
    }

    private fun getPlainEntries(groupedEntrySplits: List<List<String>>, dayBalanceString: String): List<PlainEntry> {
        return groupedEntrySplits.map {
            if (it.size == 5) {
                if (it[4].length > 2) {
                    throw ParseException(
                        "d7747ded-415f-4b01-8b61-94c2823bb2d7",
                        "Abbreviation of instance is to long (${it[3].length})",
                        dayBalanceString
                    )
                }
            } else if (it.size == 3) {
                return@map PlainEntry(it[0], it[1], it[2], it[2])
            } else if (it.size != 4) {
                throw ParseException(
                    "d239a6f1-283d-4cf8-abb6-8a6d2226b23f",
                    "Entries should contain 4 or 5 values, found (${it.size})",
                    dayBalanceString
                )
            }
            PlainEntry(it[0], it[1], it[2], it[3])
        }
    }

    private fun getEntryFromPlain(plain: PlainEntry, dayBalanceString: String): Entry {
        val splitAmount = plain.amount.split(" ")
        if (splitAmount.size != 2) {
            throw ParseException("9ee2e054-cf6f-4cf8-a30d-ae5a13ee4a39", "Unknown amount format", dayBalanceString)
        }
        val parsableAmount = splitAmount[0].replace(",", ".")

        return Entry(
            parsableAmount.toDouble(),
            plain.type,
            plain.description,
            plain.instance
        )
    }

    private fun getDayBalanceFromPlain(plain: PlainDayBalance, dayBalanceString: String): DayBalance {
        val splitDate = plain.date.split(" ")
        if (splitDate.size != 2) {
            throw ParseException("f3d53fdb-2f01-44a7-90ce-945167be237d", "Unknown date format", dayBalanceString)
        }
        val parsableDate = splitDate[1]

        val splitBalance = plain.balance.split(" ")
        if (splitBalance.size != 3) {
            throw ParseException("efada357-388c-442b-a275-35c555a52e2c", "Unknown balance format", dayBalanceString)
        }

        val parableBalance = splitBalance[1].replace(".", "").replace(",", ".")

        return DayBalance(
            LocalDate.parse(parsableDate, DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            parableBalance.toDouble(),
            plain.entries.map { getEntryFromPlain(it, dayBalanceString) }
        )
    }

    private fun splitToDayBalance(dayBalanceString: String): DayBalance {
        val splits = dayBalanceString.split("\n").filterNot { it == "Umsatzaktionen anzeigen" }.filterNot { it == "" }

        if (!hasDateAndBalanceHeader(splits)) {
            throw ParseException(
                "265994f1-c7e0-443c-858f-f5d64ce89548",
                "Header has incorrect format",
                dayBalanceString
            )
        }

        if (splits.size == 2) {
            return getDayBalanceFromPlain(PlainDayBalance(splits[0], splits[1], emptyList()), dayBalanceString)
        }

        if (!isLastDayBalanceSplitAmount(splits)) {
            throw ParseException(
                "9becc71c-d15c-4d4e-bc08-dc2f5fa17561",
                "Last line should be an amount",
                dayBalanceString
            )
        }

        val entrySplits = splits.subList(2, splits.size).reversed()
        val groupedEntrySplits = getGroupedEntrySplits(entrySplits)
        val entries = getPlainEntries(groupedEntrySplits, dayBalanceString)

        val plain = PlainDayBalance(splits[0], splits[1], entries)

        return getDayBalanceFromPlain(plain, dayBalanceString)
    }

    override fun parse(input: String): List<DayBalance> {
        val splits = input.trim().split(Regex("(?=${dateRegexString}\\n${balanceRegexString})"))
        return splits.filterNot { it == "" }.map(::splitToDayBalance)
    }
}