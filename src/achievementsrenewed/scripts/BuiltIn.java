package achievementsrenewed.scripts;

import java.util.Collection;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;

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
 *   industry:id          player colonies with that industry built; industry:id:core:item also asks for
 *                        that AI core and that special item in it (an empty part means any, so
 *                        industry:id::item asks for the item only)
 *
 * Text that does not start with one of these words is a class name and is not handled here. A key or id
 * after the colon may hold dots.
 */
public final class BuiltIn {

    private BuiltIn() {}

    /** The script for this column text, or null when the text names a class. Throws on an unknown word. */
    // Words: memory, player_memory, persistent, level, colonies, credits, wins, industry.
    public static AchievementScript parse(String text) {
        int split = text.indexOf(':');
        String word = (split < 0 ? text : text.substring(0, split)).trim();
        String arg = split < 0 ? "" : text.substring(split + 1).trim();
        // A class name has dots and no colon. A built-in word has no dot; its argument may.
        if (word.contains(".")) return null;
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
            case "industry": {
                String[] parts = arg.split(":", -1);
                String industryId = parts[0].trim();
                String coreId = parts.length > 1 ? parts[1].trim() : "";
                String itemId = parts.length > 2 ? parts[2].trim() : "";
                if (industryId.isEmpty()) throw new IllegalArgumentException("industry needs an id, as industry:id");
                return spec -> industries(industryId, coreId, itemId);
            }
            default:
                throw new IllegalArgumentException("unknown script " + text);
        }
    }

    /**
     * Player colonies with this industry, built (not still under its first construction), and with this AI
     * core and this special item installed when those are named.
     */
    private static int industries(String industryId, String coreId, String itemId) {
        int found = 0;
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            Industry industry = market.getIndustry(industryId);
            if (industry == null || (industry.isBuilding() && !industry.isUpgrading())) continue;
            if (!coreId.isEmpty() && !coreId.equals(industry.getAICoreId())) continue;
            if (!itemId.isEmpty()) {
                SpecialItemData item = industry.getSpecialItem();
                if (item == null || !itemId.equals(item.getId())) continue;
            }
            found++;
        }
        return found;
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
