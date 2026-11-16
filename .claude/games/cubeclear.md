# Cube Clear

## Rules (reverse-engineered from the code)

- 6x6 grid of letters.
- Form a word by tracing adjacent tiles in **8 directions** (Boggle-style), no tile reused.
- The solver only uses words of **5+ letters** (`words.txt` filtered to length > 4).
- Removing a word clears its tiles **and the orthogonally adjacent tiles** (up/down/left/right) when the word has
  5+ letters. (Code says `>= 5`, a printer comment says "> 5"; the real threshold and whether shorter words are
  allowed is unconfirmed.)
- **Gravity**: remaining tiles in each column fall down.
- Goal: clear the whole grid, ideally in few words (solver caps the search at 3 levels deep).

## Config: 36 letters, column by column

`TPEDYOSORIPANTHASPATNFTABELGFGVLUHIN` — start at the **bottom-left**, read **up** the column (6 letters),
then move to the next column right. Internally `grid[x][y]` = column x, row y with y=0 at the bottom; the printer
rotates it to the normal view.

## Solver (`Solver.kt`)

Kotlin `Flow`-based DFS: find candidate words (random start tile, DFS through neighbours, prefix check), apply
the word (clear + blast + gravity), prune states where an empty middle column splits letters into two sides
(`containsIslands`, since words can't cross an empty column), recurse while path < 3. Every state is printed;
complete clears go to `cubeclear/foundPaths.txt`.

## Known issues / leads

- **Never terminates**: `findPossibleWords` is a `while(true)` random-restart flow, so the search never returns
  from the first branch at any level. It acts like a random walk under the first word picked, not a search.
- Prefix check is `validWords.none { it.startsWith(...) }` — linear over ~300k words per DFS step. Wordbind's
  prefix set (or a trie) would be much faster.
- Words are only emitted when a path hits a dead end (sub-paths ≥5 are checked then), and paths stop past 6
  letters, so many words are found late or not at all; duplicates are re-emitted.
- No dedupe of equivalent states; no ranking of candidate words (e.g. by tiles cleared).
