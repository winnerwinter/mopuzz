import java.io.File
import kotlin.time.measureTimedValue

fun main(vararg args: String) {
    // Example Config
    // Starting from bottom left, reading up
    // 5/21
    // TPEDYOSORIPANTHASPATNFTABELGFGVLUHIN
    val config = parseConfig(
        config = args.getOrNull(0) ?: error("Missing config."),
        outputFile = File("cubeclear/foundPaths.txt")
    )

    val solver = Solver(config)
    val (_, time) = measureTimedValue {
        solver.solve()
    }

    println("Took ${formatDuration(time)}")
}
