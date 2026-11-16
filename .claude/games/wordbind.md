# Wordbind

## Rules (inferred from the code — confirm with the owner)

- Given a string of letters (e.g. two joined words: `PASTURESWORD`, `ESCORTONESAD`).
- Find words whose letters appear **in order** in the string (a subsequence; letters may be skipped).
- A chosen letter may be **repeated consecutively** (e.g. `COTTONED` from `ESCORTONESAD` uses the single T twice).
- Only words of **6+ letters** count (`words.txt` filtered to length > 5). Longer is better.

## Config

The letter string: `./solve wordbind PASTURESWORD`.

## Solver (`Solver.kt`)

DFS over string positions: at each position take the letter 1..n times, or skip it. Pruned with a prefix
`Set<String>` built from every word. Prints all found words, longest first. Runs in milliseconds.

## Known issues / leads

- The repeat loop goes up to `n - pos` copies of a letter; the prefix set prunes it quickly, but the bound is
  arbitrary. Check whether the real game allows repeats at all, or only some.
- Building the prefix set over all 6+ letter words is the main startup cost (~seconds).
