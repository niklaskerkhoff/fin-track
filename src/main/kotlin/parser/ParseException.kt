package parser

class ParseException(private val id: String, private val msg: String, private val hint: String) : Exception() {
    fun print() {
        println("Error ${id}: ${msg}:\n${hint}")
    }
}