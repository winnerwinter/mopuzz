import java.io.File
import java.io.FileOutputStream

data class CubeConfig(
    val startingGrid: List<CharArray>,
    val outputFile: File
) {
    init {
        outputFile.also {
            FileOutputStream(it).use { it.channel.truncate(0) }
        }
    }

    private val words = this::class.java.getResourceAsStream("words.txt")!!.bufferedReader()
        .readLines()
        .filter { it.length > 5 }
        .map { it.uppercase() }

    private val bongoValid = this::class.java.getResourceAsStream("valid.txt")!!.bufferedReader()
        .readLines()
        .map { it.uppercase() }

    private val bongoHappy = this::class.java.getResourceAsStream("happy.txt")!!.bufferedReader()
        .readLines()
        .map { it.uppercase() }

    val validWords = (words + bongoHappy).toHashSet()
}

fun parseConfig(config: String, outputFile: File): CubeConfig {
    require(config.length == 36) { "Config must be exactly 36 characters long" }

    val grid = config.windowed(6,6).map { it.toCharArray() }
    return CubeConfig(grid, outputFile)
}
