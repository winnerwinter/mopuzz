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

**Bongo**: finds the highest-scoring 5x5 board, usually in a second or two.

```
./solve bongo "CGGHYMDDNULLLLIISSSAAAEEE*|13233343|112423452|C35G45H45Y35M35D30N20U15L10I9S5A5E5"
```

The input is four parts separated by `|`. Rows and columns count from 1, starting at the top-left.

| Part | What to type | Example |
|---|---|---|
| Letters | Every tile on the rack. Repeat a letter once per tile in its stack. `*` is the wildcard. | `CGG...EEE*` (two G's, three E's) |
| Bonus word | Row then column of each bonus-word cell. | `13233343` = column 3 of rows 1 to 4 |
| Multipliers | Row, column, multiplier for each ×2 / ×3 square. | `112423452` = ×2 at row 1 col 1, ×3 at row 4 col 2, ×2 at row 4 col 5 |
| Points | Each letter followed by the number in its corner. | `C35G45H45...` |

In the output, `[x]` marks a bonus cell, `×n` a multiplier, and a lowercase letter is the wildcard.

`./solve bongo --check` runs a quick self-test, including the saved puzzles in `bongo/puzzles.txt`.

**Cube Clear**: looks for word chains that clear the 6x6 grid. Enter the 36 letters column by column,
starting at the bottom-left and reading upward.

```
./solve cubeclear TPEDYOSORIPANTHASPATNFTABELGFGVLUHIN
```

Solutions are saved to `cubeclear/foundPaths.txt`. The search runs until you stop it with Ctrl-C.

## Word lists

Found in `common/resources/`:

- `words.txt`: a big general dictionary, used by puzzles that need words longer than 5 letters. Obscure
  words that games reject are removed by hand as they come up.
- `valid.txt`: Bongo's list of accepted words.
- `happy.txt`: Bongo's common words, which earn the +30% bonus.
