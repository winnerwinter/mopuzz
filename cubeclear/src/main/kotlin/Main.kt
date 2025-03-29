import java.io.File
import kotlin.time.measureTimedValue

fun main() {
    val config = parseConfig(
        config = "VETRINTEUNOODNARWSPONHMLALYEMAGHPGSE",
        outputFile = File("cubeclear/foundPaths.txt")
    )

    val solver = Solver(config)
    val (solution, time) = measureTimedValue {
        solver.solve()
    }

    val grids = solver.applyWords(config.startingGrid, solution)
    println("Found solution.")
    println(printMultipleGrids(grids, solution))

    println("Took ${formatDuration(time)}")
}
