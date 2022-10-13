package parser

import model.DayBalance

interface Parser {
    fun parse(input: String): List<DayBalance>
}