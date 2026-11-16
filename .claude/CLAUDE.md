# mopuzz

Kotlin solvers for daily word/puzzle games (mostly Puzzmo-style). Each game is its own
module directory; shared code and word lists live in `common/`. Owner writes most of the logic
and wants to revisit the solvers — favour clear, correct search over clever tricks, and explain
rule assumptions rather than silently baking them in.

## Layout

| Path | What |
|---|---|
| `bongo/src/main/kotlin/` | Bongo v2 (Claude-owned): exact solver for the best 5x5 board. `bongo/puzzles.txt` holds saved puzzles. See `.claude/games/bongo.md` |
| `cubeclear/src/main/kotlin/` | Cube Clear: clear a 6x6 falling-letter grid with words. See `.claude/games/cubeclear.md` |
| `wordbind/src/` | Wordbind: words formed in order from a letter string. See `.claude/games/wordbind.md` |
| `typeshift` (git stash) | Typeshift WIP, stashed — see `.claude/games/typeshift.md` |
| `common/src/` | Shared helpers (`formatDuration`) |
| `common/resources/` | Word lists — see "Word lists" below |
| `solve` | CLI runner: `./solve <puzzle> "<config>"` |

## Word lists (`common/resources/`)

- `valid.txt` (~19k, uppercase) and `happy.txt` (~8k, uppercase) were taken from **Bongo's own internal word
  lists**: everything Bongo accepts, and the "happy" common words that earn its +30% bonus. They hold 3–5 letter
  words (`valid.txt` also has two stray longer entries).
- `words.txt` (~339k, lowercase) is a big generic word list found online, added because other puzzles need
  words longer than 5 letters. It's too permissive: it includes obscure words that games reject. The owner
  has been **pruning those by hand** (e.g. "sowel"), so treat edits to it as deliberate and never regenerate or
  replace it. A solver producing an obscure word is more likely a dictionary problem than a logic bug.
- Bongo v2 uses both: `valid.txt` words score plain, `happy.txt` words get the bonus. (v1 only used happy words.)

## Build & run

- No Gradle/Maven. Originally IntelliJ-only (`.idea/` and `*.iml` are gitignored, so a fresh clone has no IDE module setup).
- `./solve` finds `kotlinc` via `$KOTLINC`, `PATH`, or the IntelliJ bundle
  (`/Applications/IntelliJ*.app/Contents/plugins/Kotlin/kotlinc`), compiles `common` then the puzzle into
  `out/cli/<module>/`, and runs `MainKt` with `common/resources` on the classpath. Stdlib + coroutines jars come
  from kotlinc's `lib/`.
- Always run from the repo root: paths are relative (`cubeclear/foundPaths.txt` output, gitignored;
  `bongo/puzzles.txt` input).
- Kotlin 2.x, JDK 23. Bongo has self-checks: `./solve bongo --check`. Nothing else has tests yet.
- Keep test runs short (well under a minute). Check behaviour incrementally on small inputs and saved puzzles
  rather than waiting on long solver runs.

## Conventions / gotchas

- Every module uses the **default package** with top-level `fun main(vararg args)` → class `MainKt`, and takes the
  puzzle as a single string arg (`args[0]`). Modules can't share one classpath (duplicate `MainKt`, `Solver`).
- File names become JVM class names. `common/src/PrintUtils.kt` has `@file:JvmName("CommonUtils")` because
  bongo/cubeclear also have a `PrintUtils.kt` — without it their `PrintUtilsKt` shadows common's and
  `formatDuration` throws `NoSuchMethodError`. Keep shared file names unique (or add `@file:JvmName`).
- Word lists are loaded with `this::class.java.getResourceAsStream("words.txt")` (root of classpath).
  `words.txt` is lowercase; solvers uppercase it. Each solver re-filters by length itself.
- Cube Clear (still v1) prints progress heavily and runs indefinitely.
- Example configs live in each `Main.kt` comment and in IntelliJ run configs (`.idea/workspace.xml`).

## Commits

- **Work directly on `main`.** No feature branches and no pull requests: commit to `main` and push to
  `origin/main`. The owner sees themselves as the author pushing to mainline, not someone asking for a merge.

- **Never add `Co-Authored-By: Claude ...` (or any AI attribution trailer) to commit messages or PR descriptions.**
- Keep the README to how things work now: no notes about AI involvement and no plans or future intentions.

## Git state notes

- Branch `v1` (on GitHub) is a frozen snapshot of the owner's original solvers. Never commit to it. v2 rewrites
  happen in place on `main` and are Claude-owned: solve the game's spec directly, don't benchmark against v1.
  Bongo v2 is done; Cube Clear v2 is next.

- `stash@{0}`: "typeshift WIP (revisit later)" — the untracked `typeshift/` module. Restore with
  `git stash pop` (check `git stash list` first; indices shift).
- `stash@{1}`: older "messing around" on `44ddc9a`, touching `bongo/Solver.kt` and `main.kt` — unreviewed.
