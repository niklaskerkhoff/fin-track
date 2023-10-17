import model.DayBalance
import model.DayBalancesOverview
import java.util.*

fun getMonthNumberString(month: Int): String {
    if (month < 10) {
        return "0$month"
    }
    return month.toString()
}

fun getChars(n: Int, c: Char = ' '): String {
    return (1..n).joinToString("") { c.toString() }
}

fun groupByMonth(dayBalances: List<DayBalance>) = dayBalances.groupBy { it.date.monthValue }


fun getAllOverviews(dayBalances: List<DayBalance>): List<DayBalancesOverview> {
    val grouped = groupByMonth(dayBalances)
    return grouped.mapNotNull { getOverview(dayBalances, it.key) }
}

fun getOverview(dayBalances: List<DayBalance>, month: Int): DayBalancesOverview? {
    val grouped = groupByMonth(dayBalances)
    val monthDayBalances = grouped[month] ?: return null
    val prevMonthDayBalances = grouped[month - 1] ?: return null

    val entries = monthDayBalances
        .flatMap { it.entries }
        .groupBy { mergedInstance(it.instance) }
        .map { map ->
            val key = map.value.first().instance.findOneInGroupers()
            key to map.value.sumOf { entry -> entry.amount }
        }.toMap()

    return DayBalancesOverview(
        month,
        prevMonthDayBalances.first().balance, // start
        monthDayBalances.first().balance, // end
        entries
    )
}

fun getGroupers() = listOf(
    "REWE",
    "Amazon",
    "Alnatura",
    "Aldi",
    "ARAL",
    "DM Drogeriemarkt",
    "Edeka",
    "Tegut"
)

fun getCategories() = mapOf(
    "Lebensmittel" to listOf(
        "REWE",
        "Alnatura",
        "Aldi",
        "Rudolf Neff GmbH",
        "NORMA SAGT DANKE",
        "Edeka",
        "BADISCHE BACKSTUB OST",
        "Tegut"
    ),
    "Tanken" to listOf("Tankstelle", "ARAL"),
    "Gesundheit und Pflege" to listOf("Aptheke", "DM Drogeriemarkt"),
    "Feiern" to listOf("DIE STADTMITTE KARLSRUHE", "Phono Bar"),
    "Essen gehen" to listOf("CHARLES OXFORD KARL"),
    "Monatlich" to listOf(
        "Gerold-Freddy Kerkhoff Julia Kerkhoff",
        "FRANCISCO DELLANDREA UND MICHAEL FE",
        "JOERG WALLMERSPERGER",
        "INTERLIGENT KOMMUNIZIEREN GMBH"
    )
)

private fun String.findOneInGroupers() = getGroupers().find {
    this.lowercase(Locale.getDefault()).contains(it.lowercase(Locale.getDefault()))
} ?: this

private fun String.findAllInGroupers() = getGroupers().filter {
    this.lowercase(Locale.getDefault()).contains(it.lowercase(Locale.getDefault()))
}

fun mergedInstance(instance: String): String {
    val lower = instance.lowercase(Locale.getDefault())
    val matchingGroupers = instance.findAllInGroupers()
    return when {
        matchingGroupers.size > 1 -> throw Exception("3f2933c1-7470-45e3-9df1-c10f3f14ab88")
        matchingGroupers.size == 1 -> matchingGroupers.first()
        else -> lower
    }
}