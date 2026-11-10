# Achievements - Renewed

These instructions apply to this repository, including when it is opened without the parent mod workspace.
The workspace `AGENTS.md` one folder up still governs releases, packaging, Git identity and communication.

## Notion

Tasks, history and test lists: the **Achievements - Renewed board** in Andrzej's Notion, under **Starsector mods**
(`collection://9b733001-ffcb-41d5-b67c-0d9e238f72a1`). Ticket ids start with ACR. Read the board with
`notion-query-data-sources` mode `view` on `view://41420ca7-5d20-81e2-832e-000c3502f48a`. Update the board
during the work, not only at release time.

## What other mods rely on

Rule Book - Renewed and New Game Plus - Renewed (`ngp_renewed`) use this mod's API. Everything in
`achievementsrenewed.api`, the CSV columns, the built-in script words, the sector memory keys
`$acr_doneCount` and `$acr_totalCount`, and the file name in saves/common are a contract: add to them, never
rename or remove. Andrzej decided on 2026-11-10 that achievements and challenges never pay New Game Plus
career points. The mod only offers the listener, `isDone` and the two memory keys.

## Writing

Player-facing text is short and plain, in the game's own tone. No flavour lines, no explaining paragraphs.
A hint is one short line that points at the achievement without naming it. The how-to line states the exact
condition. The name says what the player did ("Win 10 battles").

## Clean room

- MagicLib (MIT) is read only through its public classes, and only from `achievementsrenewed.compat.magiclib`.
- Quality Perks and Quality Captains (AGPL) are read only through the game's own skill list. No code of
  theirs is used. Never name their classes.
- Unofficial New Game Plus (Originem) is all rights reserved. Never open its `src`, jar, CSV or string files.

## Build

`./gradlew releaseZip` compiles against the game, LunaLib and MagicLib (`gradle.properties` names the game
folder), runs the checks and writes `AchievementsRenewed.zip` in this folder. The checks: no java.io,
java.nio.file or java.lang.reflect in the classes, no MagicLib reference outside the compat package, version
metadata, no BOM, art paths, no `%` in LunaSettings.csv, a packaged LunaLib icon, and every row of the
achievement list (columns, ids, goals, `after`, script classes and built-in script words).

## Layout

- `data/config/achievements_renewed/achievements.csv` - one row per achievement. Columns:
  `id,name,hint,howTo,description,icon,category,goal,secret,reward,chain,step,after,script,requiredMod,order,mod`.
  Loaded merged over all mods by `Catalog` the first time anything asks for the list. Rows whose required
  mod is off are dropped before their script class is loaded. A broken row is logged and skipped. The mod a
  row belongs to: its `mod` column, else the mod folder in the row's `fs_rowSource` (the game adds that key
  to every row of a merged spreadsheet: mod folder path plus the file path, seen in the 0.98a
  `LoadingUtils`), else this mod.
- `src/achievementsrenewed/api` - the public API: `Achievements` (static), `AchievementSpec` (immutable,
  with a builder for achievements made in code), `AchievementScript`, `AchievementListener`.
- `scripts/BuiltIn` - script column words that need no Java: `memory:$key`, `player_memory:$key`,
  `persistent:key`, `level`, `colonies[:minSize]`, `credits`, `wins[:station|:<factionId>]`. Anything with
  a dot is a class name, made with `getInstanceOfScript`. The other classes in `scripts` are the Java
  scripts of this mod's own achievements.
- `Store` - done achievements with the real-world date, in saves/common (`achievements_renewed.json`). A
  file that cannot be read counts as empty and is kept as `achievements_renewed_unreadable.json`.
- `SaveData` - per save, in the sector's persistent data, plain maps and lists only: `acr_counts` (counts
  added from code), `acr_wins` (battles won: `all`, `station`, `faction:<id>`), `acr_notices` (intel log
  entries as `timestamp|id`).
- `Tracker` - the checks (`CheckScript`: first frame after a load, then daily. `BattleListener`: after
  every battle the player is in. Also when the list opens), completion (saves/common, memory keys,
  listeners, intel entry) and the listener list (one per class).
- `IntelKeeper` - the mod's intel entries are never saved. They are removed before a save and put back
  after it (`beforeGameSave`, `afterGameSave`, `onGameSaveFailed`) and rebuilt on load, so a save holds no
  class of this mod. `AchievementsIntel` is the permanent entry. `AchievementNoticeIntel` is the
  "Achievement: name" entry, dropped after 30 days.
- `compat` - the Other mods tab. `ExternalSources` makes `compat.magiclib.MagicLibReader` by name only when
  the mod `MagicLib` is enabled (`checkMagicLibIsolated` fails the build if any other class names MagicLib).
  `MilestoneSkillReader` lists the skills under the aptitude `aptitude_milestones` (Quality Perks, Quality
  Captains) and whether the player has them.
- `ui/AchievementsPanel` - the screen. Each tab is a container panel built once, holding a scrolling
  TooltipMakerAPI with a content panel of labels. Never remove a scrolling TooltipMakerAPI from a panel
  (its scroller stays): hidden tabs are moved off screen. Row backgrounds, icons, bars and buttons are
  drawn and hit-tested by the panel, which reads the content panel's real screen position each frame.
  Text heights are measured on labels in a panel that is never shown, before the content panel is made.
  `AchievementsDialog` hosts it.
