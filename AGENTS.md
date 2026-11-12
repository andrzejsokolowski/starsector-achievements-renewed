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
`achievementsrenewed.api`, the CSV columns (and what `tab` and `requiredMod` and `rewardMod` mean), the built-in script words, the sector memory keys
`$acr_doneCount` and `$acr_totalCount`, and the file name in saves/common are a contract: add to them, never
rename or remove. New Game Plus - Renewed 0.6.0 uses `register`, `get`, `complete`, `AchievementSpec.builder`
and the listener, so those are contract too.
Rule Book - Renewed 0.11.0 reads `data/config/achievements_renewed/achievements.csv` of this mod (next door, or
the installed copy) in its build to check the `rbr_*` ids, and needs this mod 0.3.0 or newer: keep that path and
the script name `achievementsrenewed.scripts.FeatScript`.
Since Rule Book 0.12.1 its challenge goals and README show the `howTo` text (not the name, which is a joke now)
of the achievements it uses: `rbr_abyss`, `acr_level15`, `rbr_oce_tutelage`, `rbr_colony6`, `rbr_sol_quest`,
`rbr_aar_ascended`, `rbr_gd_wins`, `rbr_atg`, `rbr_colonies5`. Keep `howTo` filled for those, and for every row.

This mod itself pays nothing: no career points, no rewards. A mod that wants to reward an achievement does
it in its own listener. New Game Plus - Renewed (0.7.0 on) pays career points that way, once per profile, for
every achievement of every mod except Rule Book's challenges (as its session reported Andrzej's decision of
2026-11-10). So an achievement's id and condition are a promise to players' career totals: do not rename
ids or quietly change what an achievement asks for.

## Tabs

Andrzej, 2026-11-10: "I want the Achievements tab to be only vanilla, like quality perks. Then, a tab per
mod that adds achievements. [...] I don't want my mods to take preference and other mods being delegated
that way." So: the first tab holds only rows with `tab` = `vanilla`. Every other tab is named by its rows
(`tab` text, or the defining mod's name when empty) and the tabs are in alphabetical order. Nothing in the
order or the layout may favour a mod, Oddisz's included. MagicLib and milestone achievements go in the tab
with their mod's name, never in a tab of leftovers. This mod's rows without `requiredMod` are all `vanilla`;
a row about another mod (it names it in `requiredMod`) is in that mod's tab (the build checks both). Licence: CC0 1.0 for everything (Andrzej, 2026-11-10), in `LICENSE`.

Andrzej's standing instruction: fix what you find when you find it, not in the next version.

## Rule Book's achievements (0.3.0)

Andrzej approved (Rule Book session, 2026-11-11, "Yes, send it") that this mod holds every achievement and
detects it; Rule Book - Renewed and New Game Plus - Renewed only ask. The 45 `rbr_*` rows that are not
challenges came from Rule Book 0.10.3 with ids, names, texts and conditions unchanged (only `mod` and `script`
changed; since 0.3.2 the three rule icons are copies of Rule Book's own CC0 art in `graphics/achievements_renewed/rules`). Their checks are `scripts/Feats` (a port of Rule Book's
`Feats`), reported by `scripts/FeatScript`. Never rename an `rbr_*` id or change its condition: New Game Plus
pays once per id and Rule Book unlocks rules from them. The 13 `rbr_challenge_*` rows stay in Rule Book,
which completes them with `Achievements.complete`. Rows that name a Rule Book rule in `reward` carry
`rewardMod` = `rulebook_renewed`. Checks about other mods read only what those mods leave in the save.
0.3.1 (Andrzej, "Yes, fix them"): `rbr_chicken` and `rbr_chicken_market` are gone (Chicken is in no mod any
more), and `rbr_great_mall` (id kept) is now the Gate of Babylon of The Everything Shop
(`the_everything_shop`), with the old Extra Submarkets id and its Gilgamesh Nebula still counting.

## Andrzej's list changes (0.5.0, 2026-11-12)

- The list opens from the ability `acr_achievements` (data/campaign/abilities.csv, `campaign/AchievementsAbility`)
  and the LunaLib hotkey `acr_hotkey` (`campaign/HotkeyListener`, default K). The intel screen has no
  Achievements entry any more. `campaign/AbilityKeeper` keeps the ability out of the save like IntelKeeper
  does with intel: before a save it takes the ability off the bar and the player and keeps its slots as
  text (`acr_abilitySlots`), after the save and on load it gives it back. It is put on the bar once per save
  (`acr_abilityPlaced`); a player who removes it from the bar keeps it off.
- Rows show name + hint until done. Secret ones read "Secret Achievement" (never ???). MagicLib rows have no
  source heading. Chains have a block with space around it.
- A chain is only for steps that must come in order (a count, or a quest that needs the one before). Steps
  that can be done in any order are plain rows.
- `rbr_abyss` is a Shrouded Dweller fleet only (Andrzej: "split the achievement of winning against a threat
  fleet, shrouded dweller fleet and omega fleet"). It keeps the Rule Book unlock. `acr_win_omega` and
  `acr_win_threat` are new. This is the one time an `rbr_*` condition changed, on his word.
- `acr_systems25` is gone; `acr_systems_25/50/100` (`scripts/SystemsExplored`, percent of non-hidden systems).
- `api/RewardText` + `Achievements.addRewardText`: other mods add a line under achievements (New Game Plus:
  career points). Contract from 0.5.0. Identical lines show once.
- Names and hints are Andrzej's, from his review of all 214 (0.5.1, artifact
  https://claude.ai/artifact/7xQg5jKtmXkJzSwGAXe7tr, collection `reviews`). Many names are jokes and
  pop-culture references on purpose. Do not "fix" them into plain descriptions; only fix typos, and ask him
  before changing a name. Every word of a name starts with a capital ("One For My Baby", Andrzej 2026-11-12);
  letters he wrote in capitals stay ("You WILL Dress Up For Me Now"). `ngp_*` rows also need New Game Plus's ngp_feats.csv.
- 0.5.1 removed `rbr_sol_shroud`, `rbr_starterpack` and `rbr_contract` on his word ("remove this
  achievement"). None had a Rule Book unlock.

## Quest achievements (0.4.0)

Andrzej, 2026-11-11: make achievements for other mods' quests from his quest list artifact, each only while
that mod is on. 127 `acr_q_*` rows plus 30 `ngp_*` rows. The `ngp_*` rows are New Game Plus - Renewed's feats
about other mods with its ids, names, texts and conditions: New Game Plus registers the same ids from code
(that replaces the row while it is on) and pays once per id, so never rename them. Every row reads only what
the quest's mod leaves in the save (memory keys, persistent data, a person's, faction's or entity's memory, a
skill), found in that mod's files (2026-11-11 research, not checked in game). `tab` is the mod's own name from
its mod_info.json, so the rows join that mod's MagicLib achievements. A quest whose mod already has its own
MagicLib achievement for it is left out unless New Game Plus has it.
0.6.0 (ACR-11): 9 `acr_q_tania_*` rows for Navarchy of Tania 1.4.0 (`navarchy_of_tania`), names and hints from
Andrzej's review (artifact https://claude.ai/artifact/RFABwRAEAYyyZhfqdsFSKS). Rescuing Maria Machado is not in:
the mod writes it to the defeated fleet's memory, which is gone afterwards.

## Writing

Player-facing text is short and plain, in the game's own tone. No flavour lines, no explaining paragraphs.
A hint is one short line that points at the achievement without naming it. The how-to line states the exact
condition. The name is Andrzej's (often a joke or a reference); for a new achievement, propose one and let
him decide.

## Clean room

- MagicLib (MIT) is read only through its public classes, and only from `achievementsrenewed.compat.magiclib`.
- Quality Perks and Quality Captains (AGPL) are read only through the game's own skill list. No code of
  theirs is used. Never name their classes.
- Unofficial New Game Plus (Originem) is not used here. Its licence allows using code with notice but says
  "DO NOT copy entirely"; Andrzej has allowed its code and art in Rule Book - Renewed and New Game Plus -
  Renewed. Ask him before using any of it in this mod.

## Build

`./gradlew releaseZip` compiles against the game, LunaLib and MagicLib (`gradle.properties` names the game
folder), runs the checks and writes `AchievementsRenewed.zip` in this folder. The checks: no java.io,
java.nio.file or java.lang.reflect in the classes, no MagicLib reference outside the compat package, version
metadata, no BOM, art paths, no `%` in LunaSettings.csv, a packaged LunaLib icon, and every row of the
achievement list (columns, ids, goals, `after`, script classes and built-in script words, `tab` = `vanilla`).

## Layout

- `data/config/achievements_renewed/achievements.csv` - one row per achievement. Columns:
  `id,name,hint,howTo,description,icon,category,goal,secret,reward,chain,step,after,script,requiredMod,order,mod,tab`.
  Loaded merged over all mods by `Catalog` the first time anything asks for the list. Rows whose required
  mods are off are dropped before their script class is loaded (`a|b&c|d` = (a or b) and (c or d)). The
  list order is tab (vanilla first, then by title), category, chain, step. A broken row is logged and skipped. The mod a
  row belongs to: its `mod` column, else the mod folder in the row's `fs_rowSource` (the game adds that key
  to every row of a merged spreadsheet: mod folder path plus the file path, seen in the 0.98a
  `LoadingUtils`), else this mod.
- `src/achievementsrenewed/api` - the public API: `Achievements` (static), `AchievementSpec` (immutable,
  with a builder for achievements made in code), `AchievementScript`, `AchievementListener`.
- `scripts/BuiltIn` - script column words that need no Java: `memory:$key`, `player_memory:$key`,
  `persistent:key`, `level`, `colonies[:minSize]`, `credits`, `wins[:station|:<factionId>]`,
  `industry:<id>[:<aiCoreId>[:<specialItemId>]]` (player colonies with that industry built, an empty part
  means any). Text whose first word (before the colon) has a dot is a class name, made with
  `getInstanceOfScript`. The other classes in `scripts` are the Java
  scripts of this mod's own achievements.
- `Store` - done achievements with the real-world date, in saves/common (`achievements_renewed.json`). A
  file that cannot be read counts as empty and is kept as `achievements_renewed_unreadable.json`.
- `SaveData` - per save, in the sector's persistent data, plain maps and lists only: `acr_counts` (counts
  added from code), `acr_wins` (battles won: `all`, `station`, `faction:<id>`, `abyss` = an Omega, Threat or
  Dweller fleet, `giant` = at least twice the player side's fleet points), `acr_notices` (intel log
  entries as `timestamp|id`).
- `Tracker` - the checks (`CheckScript`: first frame after a load, then daily. `BattleListener`: after
  every battle the player is in. Also when the list opens), completion (saves/common, memory keys,
  listeners, intel entry) and the listener list (one per class). One check shows at most three campaign
  messages and one line for the rest, and writes the saves/common file once.
- `IntelKeeper` - the mod's intel entries are never saved. They are removed before a save and put back
  after it (`beforeGameSave`, `afterGameSave`, `onGameSaveFailed`) and rebuilt on load, so a save holds no
  class of this mod. `AchievementNoticeIntel` is the "Achievement: name" entry, dropped after 30 days; its
  button opens the list at that achievement (`Achievements.openList(ui, id)`).
- `campaign` - the ability bar button, the hotkey and `AbilityKeeper` (see the 0.5.0 section).
- `RewardLines` - the `reward` text plus the lines of `RewardText` sources, for the list and the intel entry.
- `compat` - achievements other systems keep, shown read-only in the tab named after their mod.
  `ExternalSources` makes `compat.magiclib.MagicLibReader` by name only when
  the mod `MagicLib` is enabled (`checkMagicLibIsolated` fails the build if any other class names MagicLib).
  `MilestoneSkillReader` lists the skills under the aptitude `aptitude_milestones` (Quality Perks, Quality
  Captains) and whether the player has them.
- `ui/AchievementsPanel` - the screen: a scrolling column of tab buttons at the left, the chosen tab's
  list at the right. Every tab's list is a container panel built once when the screen opens, holding a
  scrolling TooltipMakerAPI with a content panel of labels. The tab column is built the same way. Never
  remove a scrolling TooltipMakerAPI from a panel (its scroller stays): hidden lists are moved off screen. Row backgrounds, icons, bars and buttons are
  drawn and hit-tested by the panel, which reads the content panel's real screen position each frame.
  Text heights are measured on labels in a panel that is never shown, before the content panel is made.
  `AchievementsDialog` hosts it.
