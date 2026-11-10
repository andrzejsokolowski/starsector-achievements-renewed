package achievementsrenewed.scripts;

import java.util.Collection;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import achievementsrenewed.Ids;
import achievementsrenewed.SaveData;
import achievementsrenewed.api.AchievementScript;

/**
 * Checks the script column can name without any Java, as "word" or "word:argument":
 *
 *   memory:$key          the sector memory key is set (a number or a list counts as that many)
 *   player_memory:$key   the same in the player character's memory
 *   persistent:key       the same in the sector's persistent data
 *   level                the player's level
 *   colonies             the player's colonies; colonies:6 counts only those of size 6 or more
 *   credits              the player's credits
 *   wins                 battles the player's side won in this save; wins:station against a station;
 *                        wins:<faction id> against a fleet of that faction
 *
 * Anything with a dot in it is a class name and is not handled here.
 */
public final class BuiltIn {

    private BuiltIn() {}

    /** The script for this column text, or null when the text names a class. Throws on an unknown word. */
    public static AchievementScript parse(String text) {
        if (text.contains(".")) return null;
        int split = text.indexOf(':');
        String word = (split < 0 ? text : text.substring(0, split)).trim();
        String arg = split < 0 ? "" : text.substring(split + 1).trim();
        switch (word) {
            case "memory":
                needsArgument(word, arg);
                return spec -> value(Global.getSector().getMemoryWithoutUpdate(), arg);
            case "player_memory":
                needsArgument(word, arg);
                return spec -> value(Global.getSector().getCharacterData().getMemoryWithoutUpdate(), arg);
            case "persistent":
                needsArgument(word, arg);
                return spec -> {
                    Map<String, Object> data = Global.getSector().getPersistentData();
                    return data.containsKey(arg) ? count(data.get(arg)) : 0;
                };
            case "level":
                return spec -> Global.getSector().getPlayerStats().getLevel();
            case "colonies": {
                int minSize = arg.isEmpty() ? 1 : Integer.parseInt(arg);
                return spec -> {
                    int found = 0;
                    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                        if (market.isPlayerOwned() && market.getSize() >= minSize) found++;
                    }
                    return found;
                };
            }
            case "credits":
                return spec -> {
                    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
                    if (fleet == null) return 0;
                    return (int) Math.max(0f, Math.min(Integer.MAX_VALUE, fleet.getCargo().getCredits().get()));
                };
            case "wins": {
                String key = arg.isEmpty() ? WINS_ALL : arg.equals("station") ? WINS_STATION : WINS_FACTION + arg;
                return spec -> SaveData.get(Ids.DATA_WINS, key);
            }
            default:
                throw new IllegalArgumentException("unknown script " + text);
        }
    }

    public static final String WINS_ALL = "all";
    public static final String WINS_STATION = "station";
    public static final String WINS_FACTION = "faction:";

    private static void needsArgument(String word, String arg) {
        if (arg.isEmpty()) throw new IllegalArgumentException(word + " needs a key, as " + word + ":key");
    }

    private static int value(MemoryAPI memory, String key) {
        if (memory == null || !memory.contains(key)) return 0;
        return count(memory.get(key));
    }

    /** A flag counts as 1, false and null as 0, a number as itself, a list as its size. */
    private static int count(Object value) {
        if (value == null || Boolean.FALSE.equals(value)) return 0;
        if (value instanceof Number) return Math.max(0, ((Number) value).intValue());
        if (value instanceof Collection) return ((Collection<?>) value).size();
        if (value instanceof Map) return ((Map<?, ?>) value).size();
        return 1;
    }
}
