import kotlin.time.measureTimedValue

fun main(vararg args: String) {
    // Example Config
    // PASTURESWORD
    val config = args.getOrNull(0) ?: error("Missing config.")

    val solver = Solver(config)
    val (_, time) = measureTimedValue {
        solver.solve()
    }

    println("Took ${formatDuration(time)}")
}