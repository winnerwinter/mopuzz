import java.io.File
import kotlin.time.measureTimedValue

/** Solves Bongo */
fun main(vararg args: String) {
    val config = parseConfig(
        config = args.getOrNull(0) ?: error("Missing config."),
        heuristicError = 25,
        verbose = true,
        outputFile = File("bongo/bongosolutions.txt")
    )

    val solver = Solver(config)
    val (solution, time) = measureTimedValue {
        solver.solve()
    }

    println("Solution found: ")
    print(logScore(config, solution))
    println("Down word: ${calculateDownWordScore(config, solution)}")
    println("Total score: ${calculateTotalScore(config, solution)}")
    println("Took ${formatDuration(time)}")
}

// val i found this on 3/23
val letterPointConfigA = mapOf(
    'A' to 5,  'B' to 50, 'C' to 40,
    'D' to 30, 'E' to 5,  'F' to 0,
    'G' to 0,  'H' to 0,  'I' to 0,
    'J' to 0,  'K' to 55, 'L' to 9,
    'M' to 35, 'N' to 25, 'O' to 7,
    'P' to 0,  'Q' to 0,  'R' to 7,
    'S' to 5,  'T' to 10, 'U' to 15,
    'V' to 70, 'W' to 0,  'X' to 0,
    'Y' to 0,  'Z' to 0,
)

// i found this on 3/24
val letterPointConfigB = mapOf(
    'A' to 5, 'B' to 0, 'C' to 35,
    'D' to 30, 'E' to 5, 'F' to 0,
    'G' to 0, 'H' to 0, 'I' to 10,
    'J' to 0, 'K' to 0, 'L' to 9,
    'M' to 40, 'N' to 25, 'O' to 7,
    'P' to 0, 'Q' to 0, 'R' to 7,
    'S' to 5, 'T' to 9, 'U' to 15,
    'V' to 70, 'W' to 0, 'X' to 0,
    'Y' to 35, 'Z' to 0,
)

// 3/25
val letterPointConfigC = mapOf(
    'A' to 5, 'B' to 40, 'C' to 40,
    'D' to 0, 'E' to 5, 'F' to 0,
    'G' to 45, 'H' to 0, 'I' to 10,
    'J' to 0, 'K' to 0, 'L' to 9,
    'M' to 0, 'N' to 20, 'O' to 7,
    'P' to 35, 'Q' to 0, 'R' to 7,
    'S' to 5, 'T' to 9, 'U' to 0,
    'V' to 0, 'W' to 0, 'X' to 0,
    'Y' to 0, 'Z' to 0,
)

// 3/26
val letterPointConfigD = mapOf(
    'A' to 5, 'B' to 0, 'C' to 40,
    'D' to 35, 'E' to 5, 'F' to 0,
    'G' to 0, 'H' to 0, 'I' to 9,
    'J' to 0, 'K' to 0, 'L' to 0,
    'M' to 0, 'N' to 20, 'O' to 7,
    'P' to 0, 'Q' to 0, 'R' to 0,
    'S' to 5, 'T' to 9, 'U' to 15,
    'V' to 80, 'W' to 0, 'X' to 0,
    'Y' to 0, 'Z' to 0,
)