# Bongo

v2, written and owned by Claude (the owner's original is on the `v1` branch). Goal: the provably best board.

## Rules as implemented (`Scoring.kt`)

- 5x5 board. Rack = 25 letter tiles (with duplicates, shown as stacks) + 1 wildcard. Each letter has a point
  value; the wildcard is worth 0 and can stand for any letter.
- Each row scores its **longest valid word**, which may sit anywhere in the row (not only left-aligned) and may
  be 3 to 5 letters. Tiles don't all have to be used.
- Row score = Σ(letter points × square multiplier). A "happy" (common) word gets **+30%, rounded up**
  (`happyBonus`). A valid but not happy word scores with no bonus.
- 4 marked cells, one per row, form the **bonus word**, read top to bottom. If it's a valid word it scores its
  own letters again (with multipliers), +30% if happy. A cell can be filled by a lone filler tile that isn't
  part of the row's word.
- Word lists come straight from Bongo: `valid.txt` = accepted words, `happy.txt` = bonus-eligible words.

**Unconfirmed with the owner / the live game** (the code assumes these):
1. Words may be placed at any offset in a row, and only the longest word in the row scores.
2. Not-happy valid words score their plain value.
3. A filler tile in a bonus cell is allowed (the solver only uses one when it can't change which word scores).
4. Rounding is "round up" for both the row bonus and the bonus word.

If the game ever disagrees with a predicted score, check these first.

## Input: `LETTERS|BONUS|MULTIPLIERS|POINTS`

`CGGHYMDDNULLLLIISSSAAAEEE*|13233343|112423452|C35G45H45Y35M35D30N20U15L10I9S5A5E5`

- `LETTERS`: every tile, repeated per stack height; `*` = wildcard.
- `BONUS`: 1-indexed row/col pairs, one cell per row.
- `MULTIPLIERS`: 1-indexed row/col/mult triples.
- `POINTS`: letter + value. Letters not on the rack default to 0 (only reachable via the wildcard).
- Parsed by `parsePuzzle` in `Puzzle.kt`; whitespace and case are ignored.

## Solver (`Solver.kt`)

Exact branch and bound, single-threaded:

1. **Placements**: every way to fill a row (word × offset × optional wildcard position × optional filler tile in
   the bonus cell, or empty). Placements using the same tiles are deduped to the best-scoring one.
2. **Tile prices (Lagrangian relaxation)**: each tile gets a price; a placement's "worth" is score minus the
   price of its tiles. For any prices ≥ 0: `total ≤ Σ rows (best worth) + price of all rack tiles`. Prices are
   tuned once by subgradient descent (`tunePrices`). This is what makes pruning effective, since rows compete
   for the same valuable tiles. Prices are integers in 1/64 point so bound arithmetic is exact.
3. **Bonus word first**: each candidate bonus word ("scenario") pins a letter per bonus row. Scenarios run
   best-bound-first and are skipped when their bound ≤ best found.
4. **DFS over rows** (fewest options first), lists sorted by worth. Bound at a node =
   `banked points + price of tiles still on the rack + best feasible worth of each remaining row + bonus max`.

The bound must use **banked points**, not banked worth. Using worth drops the price of already-placed tiles and
makes the bound too low (this bug happened once; the pruning check below catches it).

## Checks

`./solve bongo --check` (about 7s including compile) runs `Checks.kt`:
- hand-scored boards through `scoreBoard`,
- pruned search vs. `Solver(prune = false)` on small racks (`--exhaustive` as a 2nd CLI arg does the same by hand;
  only practical for racks of about 12 tiles or fewer),
- every puzzle in `bongo/puzzles.txt` against its recorded best.

`Main.kt` also re-scores the winning board with `scoreBoard` and warns if it disagrees with the solver.

Add each new real puzzle to `bongo/puzzles.txt`. The 2025 entries came from the owner's old configs (no wildcard).
Typical solve time is under 2 seconds; keep any test run well under a minute.

## Ideas not done

- Report ties / top-N boards, or the best board without the wildcard.
- More than one wildcard per row (the game only ever has one wildcard).
