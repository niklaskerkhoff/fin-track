import model.DayBalance

fun executeList(dayBalances: List<DayBalance>) {
    val grouped = groupByMonth(dayBalances)
    val headerFormat = "%d | Start: %f | End: %f | Diff: %f"

    grouped.forEach {
        println(headerFormat.format())
    }
}

fun executeSum() {
    if (dayBalancesState.isEmpty()) {
        println("No Entries")
        return
    }

    val monthSummaries = mutableListOf<MonthSummaryOutput>()

    var month = dayBalancesState.first().date.monthValue
    var balanceEnd = dayBalancesState.first().balance

    for (i in 1 until dayBalancesState.size) {
        val currentMonth = dayBalancesState[i].date.monthValue
        if (month != currentMonth) {
            val balanceStart = dayBalancesState[i].balance
            val difference = balanceEnd - balanceStart

            monthSummaries.add(
                MonthSummaryOutput(
                    getMonthNumberString(month),
                    balanceStart.toString().toAmount(),
                    balanceEnd.toString().toAmount(),
                    difference.toString().toAmount()
                )
            )
            balanceEnd = balanceStart
            month = currentMonth
        }
    }

    val maxStart = monthSummaries.maxBy { it.start.length }.start.length
    val maxEnd = monthSummaries.maxBy { it.end.length }.end.length
    val maxDifference = monthSummaries.maxBy { it.difference.length }.difference.length


    val formattedMonthSummaries = monthSummaries.map {
        MonthSummaryOutput(
            it.month,
            getChars(maxStart - it.end.length) + it.start,
            getChars(maxEnd - it.end.length) + it.end,
            getChars(maxDifference - it.difference.length) + it.difference,
        )
    }

    printMonthSummaryOutput(
        MonthSummaryOutput(
            "  ",
            "Start" + getChars(maxStart - 5),
            "End" + getChars(maxEnd - 3),
            "Diff" + getChars(maxDifference - 4)
        )
    )
    formattedMonthSummaries.forEach(::printMonthSummaryOutput)
}

fun executePrint() {
    println(getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(dayBalancesState))
}