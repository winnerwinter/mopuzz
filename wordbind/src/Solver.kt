class Solver(private val inputString: String) {

    private val words: Set<String>
    private val prefixes: Set<String>

    init {
        val lines = this::class.java.getResourceAsStream("words.txt")!!
            .bufferedReader()
            .readLines()
            .filter { it.length > 5 }
            .map { it.uppercase() }
        words = lines.toSet()
        prefixes = buildPrefixSet(words)
    }

    private fun buildPrefixSet(words: Set<String>): Set<String> {
        val prefixSet = mutableSetOf<String>()
        for (word in words) {
            for (i in 1..word.length) {
                prefixSet.add(word.substring(0, i))
            }
        }
        return prefixSet
    }

    fun solve() {
        val foundWords = mutableSetOf<String>()
        val input = inputString.uppercase()
        val n = input.length

        fun dfs(pos: Int, cur: StringBuilder) {
            if (cur.isNotEmpty()) {
                val currentWord = cur.toString()
                if (!prefixes.contains(currentWord)) return
                if (words.contains(currentWord)) {
                    foundWords.add(currentWord)
                }
            }
            if (pos >= n) return

            // 1. Try all possible repeats of current letter
            for (repeat in 1..n - pos) {
                repeat(repeat) { cur.append(input[pos]) }
                dfs(pos + 1, cur)
                repeat(repeat) { cur.deleteAt(cur.length - 1) }
            }

            // 2. Optionally, skip the current letter entirely (move to next without using input[pos])
            dfs(pos + 1, cur)
        }

        dfs(0, StringBuilder())

        println("Found ${foundWords.size} words:")
        foundWords.sortedByDescending { it.length }.forEach { println(it) }
    }
}