import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import model.DayBalance
import parser.FinParser
import parser.ParseException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.abs

fun String.toAmount(): String {
    val split = this.split(".")
    var first = split[0]
    val prefix = if (first.startsWith("-")) "-" else ""
    first = first.removePrefix(prefix)

    if (first.length > 3) {
        first = first.take(first.length - 3) + "." + first.takeLast(3)
    }
    if (split.size == 2) {
        var second = split[1].take(2)
        if (second.length == 1) {
            second += "0"
        }

        return "$prefix$first,$second"
    }
    return first
}

var dayBalancesState = emptyList<DayBalance>()

fun getJsonMapper(): ObjectMapper {
    val mapper = jacksonObjectMapper()
    mapper.registerModules(JavaTimeModule())
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    return mapper
}

fun saveDayBalancesState() {
    val json = getJsonMapper().writeValueAsString(dayBalancesState)

    val dir = System.getProperty("user.home") + "/.fin-track"
    val dataFilePath = Paths.get("$dir/data.json")

    Files.createDirectories(Paths.get(dir))
    Files.write(dataFilePath, json.toByteArray())
}

fun addNewDayBalances(dayBalances: List<DayBalance>) {
    val oldestNew = dayBalances.last()
    val oldEnoughData = dayBalancesState.filter { it.date < oldestNew.date }
    dayBalancesState = dayBalances + oldEnoughData
}

fun readDayBalancesState() {
    val dir = System.getProperty("user.home") + "/.fin-track"
    val dataFilePath = Paths.get("$dir/data.json")

    Files.createDirectories(Paths.get(dir))
    if (Files.notExists(dataFilePath)) {
        Files.write(dataFilePath, "[]".toByteArray())
    }
    val json = Files.readString(dataFilePath)
    dayBalancesState = getJsonMapper().readValue(json, object : TypeReference<List<DayBalance>>() {})
}

fun checkBalanceAmountDifferences() {
    for (i in 0..dayBalancesState.size - 2) {
        val amountSum = dayBalancesState[i].entries.fold(0F) { acc, entry -> acc + entry.amount }
        val balanceDifference = dayBalancesState[i].balance - dayBalancesState[i + 1].balance

        if (abs(balanceDifference - amountSum) > 0.1F) {
            throw Exception("Wrong Balance: $amountSum instead of $balanceDifference (${balanceDifference - amountSum})")
        }
    }
}

fun executeNew() {

    val scanner = Scanner(System.`in`)
    val lines = mutableListOf<String>()
    while (scanner.hasNextLine()) {
        val line = scanner.nextLine()
        if (line == "process") {
            val dayBalances = FinParser().parse(lines.joinToString("\n"))


            addNewDayBalances(dayBalances)

            saveDayBalancesState()
            /*dayBalances.forEach {
                println(it)
                println()
            }*/
            break
        }
        lines.add(line)
    }
}

fun getMonthNumberString(month: Int): String {
    if (month < 10) {
        return "0$month"
    }
    return month.toString()
}

fun getWhitespaces(n: Int): String {
    return (1..n).joinToString("") { " " }
}

data class MonthSummaryOutput(
    val month: String,
    val start: String,
    val end: String,
    val difference: String
)

fun printMonthSummaryOutput(m: MonthSummaryOutput) {
    println("| ${m.month} | ${m.start} | ${m.end} | ${m.difference} |")
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
            getWhitespaces(maxStart - it.end.length) + it.start,
            getWhitespaces(maxEnd - it.end.length) + it.end,
            getWhitespaces(maxDifference - it.difference.length) + it.difference,
        )
    }

    printMonthSummaryOutput(
        MonthSummaryOutput(
            "  ",
            "Start" + getWhitespaces(maxStart - 5),
            "End" + getWhitespaces(maxEnd - 3),
            "Diff" + getWhitespaces(maxDifference - 4)
        )
    )
    formattedMonthSummaries.forEach(::printMonthSummaryOutput)
}

fun executeCommand(command: String) {
    if (command == "new") {
        executeNew()
    } else if (command == "sum") {
        executeSum()
    }
}


fun main() = try {

    readDayBalancesState()
    checkBalanceAmountDifferences()

    val scanner = Scanner(System.`in`)


    while (scanner.hasNextLine()) {
        val command = scanner.nextLine()
        if (command == "exit" || command == "quit") {
            break
        }
        executeCommand(command)
    }

} catch (e: ParseException) {
    e.print()
}
