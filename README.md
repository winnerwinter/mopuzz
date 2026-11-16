# mopuzz

Solvers for daily word puzzles, written in Kotlin. Each puzzle has its own folder; shared code and word lists
are in `common/`.

## Requirements

- Java 17+ (`java` on your PATH)
- A Kotlin compiler. `./solve` finds one in this order: `$KOTLINC`, `kotlinc` on your PATH, or the copy
  bundled with IntelliJ IDEA.

## Usage

Run from the repo root:

```
./solve <puzzle> "<config>"
```

The first run compiles the code (a few seconds). Later runs only recompile what changed.

## Puzzles

**Wordbind**: lists words you can spell from the letters in order. Letters can be skipped or repeated.

```
./solve wordbind PASTURESWORD
```

**Bongo**: finds a high-scoring 5x5 board. The config has four parts separated by `|`:
letters | bonus-word cells (row,col) | multiplier cells (row,col,mult) | letter points.
Rows and columns start at 1.

```
./solve bongo "VKKGGGHNNIIITTTRAAAAEEESS|14243445|112522553|V70K50G45H40N20I10T10R7A5E5S5"
```

Every time it finds a better board, it prints it. Let it run, and press Ctrl-C when you're happy with the result.

**Cube Clear**: looks for word chains that clear the 6x6 grid. Enter the 36 letters column by column,
starting at the bottom-left and reading upward.

```
./solve cubeclear TPEDYOSORIPANTHASPATNFTABELGFGVLUHIN
```

Solutions are saved to `cubeclear/foundPaths.txt`. The search runs until you stop it with Ctrl-C.

## How this was built

I wrote most of the solver logic myself, with a lot of help from AI tools along the way. I'm now revisiting the
solvers with Claude Code, which wrote the `./solve` runner, keeps the notes in `.claude/` (game rules, config
formats, known issues), and reviews and helps rework the solvers. Commits don't carry AI attribution lines; this
section covers it.

## Word lists

Found in `common/resources/`:

- `words.txt`: a big general dictionary, used by puzzles that need words longer than 5 letters. Obscure
  words that games reject are removed by hand as they come up.
- `valid.txt`: Bongo's list of accepted words. It's kept for reference; the solver doesn't use it.
- `happy.txt`: Bongo's common words, which earn the bonus. The Bongo solver uses only these, since a top
  score needs every word to be a happy word.
