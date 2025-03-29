typealias Grid = List<CharArray>

data class Word(
    val letters: List<Letter>
) {

    override fun toString(): String = letters.joinToString("")
}

data class Letter(
    val value: Char,
    val coord: Pair<Int, Int>
) {

    override fun toString(): String = value.toString()
}