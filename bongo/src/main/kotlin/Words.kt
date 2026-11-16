/** Bongo's own word lists: `valid` = every accepted word, `happy` = common words that earn the +30% bonus. */
class Words(val valid: Set<String>, val happy: Set<String>) {
    fun isWord(s: String) = s in valid

    companion object {
        fun load(): Words {
            fun read(name: String): Set<String> =
                (Words::class.java.getResourceAsStream("/$name") ?: error("Missing word list $name on classpath."))
                    .bufferedReader().readLines()
                    .map { it.trim().uppercase() }
                    .filter { it.length in 3..SIZE && it.all { c -> c in 'A'..'Z' } }
                    .toSet()

            val happy = read("happy.txt")
            return Words(valid = read("valid.txt") + happy, happy = happy)
        }
    }
}
