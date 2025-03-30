import java.io.File
import kotlin.time.measureTimedValue

fun main() {
    val config = parseConfig(
        // 3/29
        config = "REENDRAULSTHOWEFRARIPNKCGITULCFNHEOD",
        outputFile = File("cubeclear/foundPaths.txt")
    )

    val solver = Solver(config)
    val (_, time) = measureTimedValue {
        solver.solve()
    }

    println("Took ${formatDuration(time)}")
}
