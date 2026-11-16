# Bongo

## Rules (reverse-engineered from the code)

- 5x5 board, exactly **25 letter tiles** given (with duplicates). Each tile has a point value.
- Fill each of the 5 **rows** with a word, left-aligned; a row may be shorter than 5 (unused squares are blank).
- Some squares are **multipliers** (2x / 3x) on that tile's points.
- 4 marked squares form the **bonus ("down") word**, read top-to-bottom through rows; it must be a 4-letter word
  and scores its own letters again.
- Row score = Σ(letter points × square multiplier). A row word in the "happy"/common list gets
  **+30%** (`ceil(score * 0.3)`). Bonus word gets ×1.3 if it's a happy word.
- Total = all row scores (+happy bonus) + bonus-word score. Goal: maximise.

Word lists come straight from Bongo: `valid.txt` = accepted words, `happy.txt` = bonus-eligible common words.

Still to confirm with the owner: whether wildcards exist in the real game, and how many letters must be used.

## Config: `LETTERS|DOWNCELLS|MULTS|POINTS`

`VKKGGGHNNIIITTTRAAAAEEESS|14243445|112522553|V70K50G45H40N20I10T10R7A5E5S5`

- `LETTERS` — the 25 tiles.
- `DOWNCELLS` — 1-indexed `row col` pairs: `14 24 34 45` = (1,4),(2,4),(3,4),(4,5).
- `MULTS` — 1-indexed `row col mult` triples: `112 522 553` = (1,1)x2, (5,2)x2, (5,5)x3.
- `POINTS` — `<Letter><value>` pairs.
- Single digits only, so coordinates and multipliers must be 1–9.

## Solver (`Solver.kt`)

Branch-and-bound backtracking, one row at a time, rows ordered by multiplier weight (`orderToFillInWords`).
Each candidate branch is a coroutine (`async` on `Dispatchers.Default`). Candidate words must fit the remaining
letter counts and keep a 4-letter happy bonus word possible. Pruned with `calculateMaxRemainingScore`: best
remaining letters × best remaining multipliers × 1.3, **plus `heuristicError` (25)**.

## Known issues / leads

- Only `happy.txt` words are considered (`validWords = emptySet()`). This is **intentional**: a max-score board
  realistically needs every word to be happy, so not-happy valid words just enlarge the search.
- `heuristicError` is a fudge factor; the bound isn't proven admissible, so optimality isn't guaranteed.
- Best-score update is check-then-set across coroutines (not atomic as a pair) — `bestSolution` can be overwritten
  by a lower score in a race. Prints `>=` ties too.
- `calculateDownWordScore` looks up `downWordMap[index]` (index into the config list) but the map is keyed by row;
  only correct when the bonus word starts in row 1.
- `availableWildcards` "Broken", `startingWords` "Unimplemented" (per comments).
- `outputFile` (`bongo/bongosolutions.txt`) is truncated but never written.
- Takes minutes; the example reached 1025 within 90s and was still improving.
- `Main.kt` holds unused historical letter-point tables (`letterPointConfigA..D`).
