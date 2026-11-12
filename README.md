# Achievements - Renewed

Achievements for the Starsector campaign, kept across all your saves. For Starsector 0.98a. Needs LunaLib.

- **The list**: open it with the **Achievements** button on the ability bar, or with the hotkey (K by
  default, LunaLib setting "Hotkey for the list").
- **Tabs**: the first tab, Achievements, holds the base game's achievements. Every mod with achievements
  has a tab of its own, in alphabetical order. Each tab shows how many you have.
- **Hints**: an achievement you do not have is greyed out and shows its name and a hint. Secret ones read
  "Secret Achievement" until done. Once you get it, it shows what it was. Under it: what it unlocks or pays
  in other mods (Rule Book rules, New Game Plus career points).
- **How to get them**: the LunaLib setting "Show how to get achievements" shows the exact condition of
  every achievement instead of its hint, secret ones included.
- **Progress**: counted achievements show how far you are in the current save, for example "3 / 10".
- **Chains**: some achievements come in steps. A later step shows up when the one before it is done.
- **When you get one**: a message on the campaign screen and an entry in the intel log. The LunaLib setting
  "Show a message on completion" turns the message off. The log entry has a button that opens the list at
  that achievement. Log entries go away after 30 days. When a save
  earns many at once, three messages show and one line counts the rest.
- **MagicLib and Quality Perks**: the achievements of MagicLib mods (Nexerelin and others) and the
  milestones of Quality Perks and Quality Captains are listed too, read-only, among the others of the tab
  of their mod.
- Done achievements are in `saves/common/achievements_renewed.json.data`. The mod can be added to a running
  save. It keeps none of its own classes in the save, so the save still loads without it.

The mod comes with 40 achievements for the base game (story missions, colonies, battles and more) and 14
for Oddisz's other mods, plus 166 for the quests and named bounties of 38 other mods, each in that mod's tab and only while that mod is on. Some unlock Rule Book - Renewed's
rules; their reward line shows only while Rule Book is on.

Licence: CC0 1.0 (see `LICENSE`). Do what you like with it.

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
| `category` | Heading inside the tab. May be empty. |
| `goal` | Count needed. Empty means 1. Above 1 the list shows progress. |
| `secret` | `true`: shown as "Secret Achievement" until done. |
| `reward` | What it gives, as text: "Unlocks the rule Imperial Charter". Shown once done. May be empty. |
| `chain` | Achievements with the same chain are listed together under this name. |
| `step` | Order inside the chain. |
| `after` | Id of the achievement that must be done before this one is shown. Until then it reads "Next step". |
| `script` | How it is checked. Empty: your code calls `complete` or `addProgress`. See below. |
| `requiredMod` | Mod id. `a\|b` means a or b. `a\|b&c\|d` means (a or b) and (c or d). The row is dropped when this is not met, before its script class is loaded. |
| `order` | Sort order inside the tab, lowest first. Categories and chains sit where their first achievement sits. |
| `mod` | The mod id the row belongs to. Leave empty: it is your own mod, found from the file's folder. |
| `rewardMod` | Mod id, in the same form as `requiredMod`. The `reward` text shows only while it is met. Empty: always. |
| `tab` | The tab the row is listed in. Empty: a tab with your mod's name. Any text: a tab with that title, shared with every other row that names it ("Cloning"). `vanilla`: the first tab, which is for base game achievements only. |

Tabs: "Achievements" (the `vanilla` rows) comes first, the rest follow in alphabetical order. A tab exists
as long as one achievement is in it. Titles are compared without regard to case. MagicLib achievements
join the tab that has their mod's name, so `tab` = `Nexerelin` puts your rows next to Nexerelin's own.

The `script` column takes a built-in check or a class name.

| Script | Count |
| --- | --- |
| `memory:$key` | The sector memory key is set. A number or a list counts as that many. The text "false" counts as not set. |
| `memory:$a&$b`, `memory:$a\|$b` | 1 when all (`&`) or any (`\|`) of the keys are set. Do not mix `&` and `\|`. |
| `memory:$key=VALUE`, `memory:$key>=10`, `memory:$a&!$b` | 1 when the key holds that text or number, at least that number, or (`!`) is not set. Works inside `&` and `\|` too. |
| `player_memory:$key` | The same in the player character's memory. |
| `persistent:key` | The same in the sector's persistent data. |
| `person_memory:<person id>:$key` | The same in the memory of an important person. |
| `faction_memory:<faction id>:$key` | The same in the memory of a faction. |
| `entity_memory:<entity id>:$key` | The same in the memory of a station, planet or other entity. |
| `skill:<skill id>` | 1 when the player has that skill. |
| `level` | The player's level. |
| `colonies`, `colonies:6` | The player's colonies, or only those of size 6 and up. |
| `credits` | The player's credits. |
| `wins`, `wins:station`, `wins:<faction id>` | Battles the player's side won in this save: all, against a station, against a fleet of that faction. Counted from the day this mod is in the save. |
| `industry:<industry id>` | The player's colonies that have that industry, built. |
| `industry:<industry id>:<AI core id>:<special item id>` | The same, counting only those with that AI core and that special item installed. Leave a part empty for any: `industry:heavyindustry::pristine_nanoforge`. |
| `my.mod.MyScript` | A class that implements `AchievementScript`, with a no-argument constructor. |

An achievement is done when its count reaches `goal`. Done is kept for every save; the count is per save.

Example rows:

```csv
id,name,hint,howTo,description,icon,category,goal,secret,reward,chain,step,after,script,requiredMod,order,mod,tab
mymod_gates,Finish At the Gates,Open what was shut.,Finish the mission At the Gates.,,,Story,,,Unlocks the rule Ludd's Song,,,,memory:$gaATG_missionCompleted,,10,,
mymod_hunter1,Defeat 5 pirate fleets,Thin the herd.,Win 5 battles against pirate fleets in one campaign.,,,Battles,5,,,Pirate hunter,1,,wins:pirates,,20,,
mymod_hunter2,Defeat 25 pirate fleets,Thin it further.,Win 25 battles against pirate fleets in one campaign.,,,Battles,25,,,Pirate hunter,2,mymod_hunter1,wins:pirates,,21,,
mymod_trader,Sell 1000 units of food,Feed the Sector.,Sell 1000 units of food in one campaign.,,,Trade,1000,,,,,,,,30,,
mymod_ships,Own three Onslaughts,Bigger is better.,Have three Onslaughts in your fleet.,,,Fleet,3,true,,,,,my.mod.OnslaughtCount,,40,,
mymod_forge,Own a nanoforge foundry,Make your own.,Have Heavy Industry with a Pristine Nanoforge on one of your colonies.,,,,,,,,,,industry:heavyindustry::pristine_nanoforge,,50,,
mymod_nex,Win 5 battles alongside Nexerelin,Share the work.,Win 5 battles in one campaign.,,,,5,,,,,,wins,nexerelin,60,,Nexerelin
```

The first five sit in a tab named after your mod. The last one needs Nexerelin and sits in the Nexerelin
tab.

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
Achievements.openList();                // from the campaign
Achievements.openList(ui);              // from an intel entry's buttonPressConfirmed
Achievements.openList(ui, "mymod_x");   // the same, on that achievement's tab, scrolled to it

// A line under achievements: what your mod gives for them. Call it in onApplicationLoad; one per class.
Achievements.addRewardText(spec -> spec.id.startsWith("mymod_") ? "+1 medal" : null);
```

`Achievements` in full:

| Method | What it does |
| --- | --- |
| `boolean isDone(String id)` | Done in any save. |
| `int progress(String id)` | Count in this save, as of the last check. 0 if unknown. |
| `boolean complete(String id)` | Marks it done. True when newly done. Unknown ids are ignored. |
| `void addProgress(String id, int amount)` | Adds to the count of an achievement without a script. |
| `AchievementSpec get(String id)` | Null if unknown. |
| `List<AchievementSpec> all()` | Every achievement whose required mods are on, in list order. |
| `List<AchievementSpec> ofMod(String modId)` | Those of one mod. |
| `List<String> tabs()` | The tab titles in list order (this system's own tabs). |
| `List<AchievementSpec> ofTab(String tabName)` | Those of one tab, by title. `vanilla` names the first. |
| `List<AchievementSpec> chain(String chain)` | Those of one chain, in step order. |
| `void addListener(AchievementListener)`, `removeListener` | Told when an achievement is done for the first time. |
| `boolean showHowTo()` | The LunaLib setting. |
| `void openList()`, `openList(IntelUIAPI)`, `openList(IntelUIAPI, String id)` | Opens the screen, at one achievement with the id. |
| `void addRewardText(RewardText)`, `removeRewardText` | A line under achievements in the list and their intel entries. Return null for none. Identical lines show once. |
| `List<String> rewardLines(String id)` | The `reward` text, then the lines other mods add. |
| `void register(AchievementSpec)` | Adds an achievement built in code with `AchievementSpec.builder(id, modId, name)`. Call it in `onApplicationLoad`. The builder has a method per column, `tab(String)` among them. Without it the tab is your mod's name. |
| `String doneDate(String id)` | The real-world date it was done, `yyyy-MM-dd`, or null. |
| `int doneCount()`, `int totalCount()` | Done and all, over `all()`. |
| `void check()` | Runs the checks now. |

`AchievementSpec` has public final fields for every column (`id`, `modId`, `name`, `hint`, `howTo`,
`description`, `icon`, `category`, `goal`, `secret`, `reward`, `chain`, `step`, `after`, `script`, `order`,
`tab`) and `isDone()`, `progress()`, `isRevealed()` (false while secret or while `after` is not done),
`isCounted()`, `isVanilla()` and `tabName()` (the title of its tab).

Checks run on the first frame after a game load, once a day, after every battle the player is in, and when
the list opens. The first check comes after every mod's `onGameLoad`, so a listener added there misses
nothing.

### Rules, New Game Plus and other mods

- Sector memory holds `$acr_doneCount` and `$acr_totalCount`, written on game load and on every
  completion. Use them in `rules.csv` or read them from Java without touching this mod's classes.
- Rule Book - Renewed unlocks rules from `isDone` and a listener. Its `rbr_*` achievements (not the
  challenges) live in this mod since 0.3.0, ids unchanged; Rule Book reports its challenges with `complete`.
- New Game Plus - Renewed (`ngp_renewed`) registers its own from code and rewards them in its own listener.
- This mod itself gives nothing for an achievement. A mod that wants to reward one does it in its listener,
  and can say so under the achievement with `addRewardText`.

## Building

`./gradlew releaseZip` builds `jars/AchievementsRenewed.jar` and `AchievementsRenewed.zip`. Set the game
folder in `gradle.properties`. LunaLib and MagicLib must be in the game's `mods` folder to compile.
