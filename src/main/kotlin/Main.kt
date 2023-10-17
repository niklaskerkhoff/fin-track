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
import kotlin.math.max

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

fun Double.toAmount() = this.toString().toAmount()

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
        val amountSum = dayBalancesState[i].entries.fold(0.0) { acc, entry -> acc + entry.amount }
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


data class MonthSummaryOutput(
    val month: String,
    val start: String,
    val end: String,
    val difference: String
)

fun printMonthSummaryOutput(m: MonthSummaryOutput) {
    println("| ${m.month} | ${m.start} | ${m.end} | ${m.difference} |")
}

fun executeOverview() {
    val overview = getAllOverviews(dayBalancesState)

    overview.forEach { month ->
        val header =
            "${month.month} | start: ${month.start.toAmount()} | end: ${month.end.toAmount()} | diff: ${(month.end - month.start).toAmount()}"
        println(header)
        val outputEntries = month.entries.mapValues { it.value.toAmount() }
        val maxInstanceLength = outputEntries.maxOf { it.key.length }
        val maxAmountLength = outputEntries.maxOf { it.value.length }

        val outputWidth = max(header.length, maxInstanceLength + maxAmountLength + 3)
        println(getChars(outputWidth, '-'))

        outputEntries.forEach {
            val instanceOutput = it.key + getChars(maxInstanceLength - it.key.length)
            val amountOutput = getChars(maxAmountLength - it.value.length) + it.value
            val line = "$instanceOutput | $amountOutput"
            println(line)
        }

        println()
    }
}

fun executeCommand(command: String) {
    if (command.matches(Regex("^new${'$'}"))) {
        executeNew()
    } else if (command.matches(Regex("^sum${'$'}"))) {
        executeSum()
    } else if (command.matches(Regex("^print${'$'}"))) {
        executePrint()
    } else if (command.matches(Regex("^overview${'$'}"))) {
        executeOverview()
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
