# Typeshift (stashed WIP)

Code lives in `git stash` ("typeshift WIP (revisit later)"), not the working tree. The owner plans to revisit it.

## Rules (inferred)

- N columns of letters; each column slides independently. A word reads one letter from each column, left to right,
  so all words have length N.
- Goal: use **every letter in every column** in at least one valid word, ideally with the fewest words
  (lower bound = the tallest column's height).

## Config

Columns separated by `|`: `EFC|ILA|UITR|ECL|TKSY`.

## Solver state when stashed

- Enumerates all valid words (prefix-pruned DFS across columns).
- `solve()` tries only the theoretical minimum: permutes the letters of the tallest column(s) and fills the
  other columns with `slotIdx % size`, which ignores most possibilities. It printed "No minimal cover found" for
  `NSCFL|WOIU|USPAT|PUIHT|YSN`.
- `findMinimalSetCover` (brute-force combinations over the enumerated words) exists but isn't called. A proper
  set-cover search over the enumerated words (ILP or best-first) is the obvious next step.
