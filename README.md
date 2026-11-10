# Achievements - Renewed

Achievements for the Starsector campaign, kept across all your saves. For Starsector 0.98a. Needs LunaLib.

- **The list**: open it from the **Achievements** entry on the intel screen.
- **Hints**: an achievement you do not have is greyed out and shows a hint. Once you get it, it shows
  what it was, and what it gave you if it unlocked something.
- **How to get them**: the LunaLib setting "Show how to get achievements" shows the exact condition of
  every achievement instead of its hint, secret ones included.
- **Progress**: counted achievements show how far you are in the current save, for example "3 / 10".
- **Chains**: some achievements come in steps. A later step shows up when the one before it is done.
- **When you get one**: a message on the campaign screen and an entry in the intel log. The LunaLib setting
  "Show a message on completion" turns the message off. Log entries go away after 30 days.
- **Other mods**: with MagicLib installed, the Other mods tab lists the achievements of MagicLib mods
  (Nexerelin and others), read-only. It also lists the milestones of Quality Perks and Quality Captains.
- Done achievements are in `saves/common/achievements_renewed.json.data`. The mod can be added to a running
  save. It keeps none of its own classes in the save, so the save still loads without it.

The mod comes with 10 achievements for the base game. Mods such as Rule Book - Renewed add their own.

## For modders

Achievements - Renewed is a library as much as a mod: any mod can add achievements with one CSV file, and
check or drive them from Java.

### Depend on it

Required, in `mod_info.json`:

```json
"dependencies": [ { "id": "achievements_renewed", "name": "Achievements - Renewed" } ]
```

Optional: ship the CSV anyway (it is ignored without this mod) and guard every Java call with
`Global.getSettings().getModManager().isModEnabled("achievements_renewed")`. Keep the calls in a class of
their own, so the game never loads it without the mod. Compile against `jars/AchievementsRenewed.jar`.

### The CSV

`data/config/achievements_renewed/achievements.csv` in your mod. All mods' files are merged; a row with an
id that exists already replaces it. A row that cannot be read is logged and left out.

| Column | Meaning |
| --- | --- |
| `id` | Unique. Start it with your mod's prefix. |
| `name` | What the player did, short and plain: "Own five colonies". Shown once done. |
| `hint` | One short line that points at it without naming it: "Expand your domain far and wide." Shown before it is done. Empty: the name is shown. |
| `howTo` | The exact condition. Shown instead of the hint when the player turns that setting on. Empty: the description, else the name. |
| `description` | Shown under the name once done. May be empty: the list then shows `howTo`. |
| `icon` | Sprite path. Empty: a trophy. Loaded for you. |
| `category` | Heading inside your mod's group. May be empty. |
| `goal` | Count needed. Empty means 1. Above 1 the list shows progress. |
| `secret` | `true`: shown as ??? until done. |
| `reward` | What it gives, as text: "Unlocks the rule Imperial Charter". Shown once done. May be empty. |
| `chain` | Achievements with the same chain are listed together under this name. |
| `step` | Order inside the chain. |
| `after` | Id of the achievement that must be done before this one is shown. Until then it reads ???. |
| `script` | How it is checked. Empty: your code calls `complete` or `addProgress`. See below. |
| `requiredMod` | Mod id, or `a\|b` for any one of several. The row is dropped when none is enabled, before its script class is loaded. |
| `order` | Sort order inside your mod's group, lowest first. Categories and chains sit where their first achievement sits. |
| `mod` | The mod id the row is listed under. Leave empty: it is your own mod, found from the file's folder. |

The `script` column takes a built-in check or a class name.

| Script | Count |
| --- | --- |
| `memory:$key` | The sector memory key is set. A number or a list counts as that many. |
| `player_memory:$key` | The same in the player character's memory. |
| `persistent:key` | The same in the sector's persistent data. |
| `level` | The player's level. |
| `colonies`, `colonies:6` | The player's colonies, or only those of size 6 and up. |
| `credits` | The player's credits. |
| `wins`, `wins:station`, `wins:<faction id>` | Battles the player's side won in this save: all, against a station, against a fleet of that faction. Counted from the day this mod is in the save. |
| `my.mod.MyScript` | A class that implements `AchievementScript`, with a no-argument constructor. |

An achievement is done when its count reaches `goal`. Done is kept for every save; the count is per save.

Example rows:

```csv
id,name,hint,howTo,description,icon,category,goal,secret,reward,chain,step,after,script,requiredMod,order,mod
mymod_gates,Finish At the Gates,Open what was shut.,Finish the mission At the Gates.,,,Story,,,Unlocks the rule Ludd's Song,,,,memory:$gaATG_missionCompleted,,10,
mymod_hunter1,Defeat 5 pirate fleets,Thin the herd.,Win 5 battles against pirate fleets in one campaign.,,,Battles,5,,,Pirate hunter,1,,wins:pirates,,20,
mymod_hunter2,Defeat 25 pirate fleets,Thin it further.,Win 25 battles against pirate fleets in one campaign.,,,Battles,25,,,Pirate hunter,2,mymod_hunter1,wins:pirates,,21,
mymod_trader,Sell 1000 units of food,Feed the Sector.,Sell 1000 units of food in one campaign.,,,Trade,1000,,,,,,,,30,
mymod_ships,Own three Onslaughts,Bigger is better.,Have three Onslaughts in your fleet.,,,Fleet,3,true,,,,,my.mod.OnslaughtCount,,40,
```

### Java

```java
import achievementsrenewed.api.Achievements;
import achievementsrenewed.api.AchievementScript;
import achievementsrenewed.api.AchievementSpec;

/** Named in the script column. Polled daily, after battles, on game load and when the list opens. */
public class OnslaughtCount implements AchievementScript {
    @Override
    public int count(AchievementSpec spec) {
        int found = 0;
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getHullSpec().getBaseHullId().equals("onslaught")) found++;
        }
        return found;
    }
}
```

```java
// An achievement with no script is driven from your code.
Achievements.addProgress("mymod_trader", unitsSold);   // done when the count reaches the goal
Achievements.complete("mymod_special");                // done now; true when it is new

// Reading.
if (Achievements.isDone("mymod_gates")) unlockMyRule();           // done in any save
int wins = Achievements.progress("mymod_hunter1");                // count in this save
for (AchievementSpec spec : Achievements.ofMod("mymod")) { ... }

// Hearing about new ones. Not saved: add it in onGameLoad. One listener per class is kept.
Achievements.addListener(spec -> {
    if (spec.id.equals("mymod_gates")) unlockMyRule();
});

// Opening the list.
Achievements.openList();        // from the campaign
Achievements.openList(ui);      // from an intel entry's buttonPressConfirmed
```

`Achievements` in full:

| Method | What it does |
| --- | --- |
| `boolean isDone(String id)` | Done in any save. |
| `int progress(String id)` | Count in this save, as of the last check. 0 if unknown. |
| `boolean complete(String id)` | Marks it done. True when newly done. Unknown ids are ignored. |
| `void addProgress(String id, int amount)` | Adds to the count of an achievement without a script. |
| `AchievementSpec get(String id)` | Null if unknown. |
| `List<AchievementSpec> all()` | Every achievement whose required mod is on, in list order. |
| `List<AchievementSpec> ofMod(String modId)` | Those of one mod. |
| `List<AchievementSpec> chain(String chain)` | Those of one chain, in step order. |
| `void addListener(AchievementListener)`, `removeListener` | Told when an achievement is done for the first time. |
| `boolean showHowTo()` | The LunaLib setting. |
| `void openList()`, `openList(IntelUIAPI)` | Opens the screen. |
| `void register(AchievementSpec)` | Adds an achievement built in code with `AchievementSpec.builder(id, modId, name)`. Call it in `onApplicationLoad`. |
| `String doneDate(String id)` | The real-world date it was done, `yyyy-MM-dd`, or null. |
| `int doneCount()`, `int totalCount()` | Done and all, over `all()`. |
| `void check()` | Runs the checks now. |

`AchievementSpec` has public final fields for every column (`id`, `modId`, `name`, `hint`, `howTo`,
`description`, `icon`, `category`, `goal`, `secret`, `reward`, `chain`, `step`, `after`, `script`, `order`)
and `isDone()`, `progress()`, `isRevealed()` (false while secret or while `after` is not done) and
`isCounted()`.

Checks run on the first frame after a game load, once a day, after every battle the player is in, and when
the list opens. The first check comes after every mod's `onGameLoad`, so a listener added there misses
nothing.

### Rules, New Game Plus and other mods

- Sector memory holds `$acr_doneCount` and `$acr_totalCount`, written on game load and on every
  completion. Use them in `rules.csv` or read them from Java without touching this mod's classes.
- Rule Book - Renewed registers its feats through the CSV and unlocks rules from `isDone` and a listener.
- New Game Plus - Renewed (`ngp_renewed`) can do the same. Achievements pay no career points on their own.

## Building

`./gradlew releaseZip` builds `jars/AchievementsRenewed.jar` and `AchievementsRenewed.zip`. Set the game
folder in `gradle.properties`. LunaLib and MagicLib must be in the game's `mods` folder to compile.
