package achievementsrenewed.scripts;

import java.util.Collection;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
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
 *                        These three also take several keys, counting 1 when the test is met: $a&$b (all
 *                        set), $a|$b (any set), $key=VALUE (the key holds that text or number). A term of
 *                        & or | may itself be key=VALUE or key>=NUMBER, and !key means the key is not set. & and | are
 *                        not mixed in one text.
 *   person_memory:id:$key   the same in the memory of an important person (by person id)
 *   faction_memory:id:$key  the same in the memory of a faction (by faction id)
 *   entity_memory:id:$key   the same in the memory of a station, planet or other entity (by entity id)
 *   skill:id             the player has that skill (1 or 0)
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
    // Words: memory, player_memory, persistent, person_memory, faction_memory, entity_memory, level, colonies,
    // credits, wins, industry, skill.
    public static AchievementScript parse(String text) {
        int split = text.indexOf(':');
        String word = (split < 0 ? text : text.substring(0, split)).trim();
        String arg = split < 0 ? "" : text.substring(split + 1).trim();
        // A class name has dots and no colon. A built-in word has no dot; its argument may.
        if (word.contains(".")) return null;
        switch (word) {
            case "memory":
                needsArgument(word, arg);
                return keys(arg, key -> {
                    MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
                    return memory != null && memory.contains(key) ? memory.get(key) : null;
                });
            case "player_memory":
                needsArgument(word, arg);
                return keys(arg, key -> {
                    MemoryAPI memory = Global.getSector().getCharacterData().getMemoryWithoutUpdate();
                    return memory != null && memory.contains(key) ? memory.get(key) : null;
                });
            case "persistent":
                needsArgument(word, arg);
                return keys(arg, key -> Global.getSector().getPersistentData().get(key));
            case "person_memory": {
                String[] parts = ownerAndKeys(word, arg);
                return keys(parts[1], key -> {
                    PersonAPI person = Global.getSector().getImportantPeople().getPerson(parts[0]);
                    MemoryAPI memory = person == null ? null : person.getMemoryWithoutUpdate();
                    return memory != null && memory.contains(key) ? memory.get(key) : null;
                });
            }
            case "faction_memory": {
                String[] parts = ownerAndKeys(word, arg);
                return keys(parts[1], key -> {
                    FactionAPI faction = Global.getSector().getFaction(parts[0]);
                    MemoryAPI memory = faction == null ? null : faction.getMemoryWithoutUpdate();
                    return memory != null && memory.contains(key) ? memory.get(key) : null;
                });
            }
            case "entity_memory": {
                String[] parts = ownerAndKeys(word, arg);
                return keys(parts[1], key -> {
                    SectorEntityToken entity = Global.getSector().getEntityById(parts[0]);
                    MemoryAPI memory = entity == null ? null : entity.getMemoryWithoutUpdate();
                    return memory != null && memory.contains(key) ? memory.get(key) : null;
                });
            }
            case "skill":
                needsArgument(word, arg);
                return spec -> {
                    MutableCharacterStatsAPI stats = Global.getSector().getPlayerStats();
                    return stats != null && stats.getSkillLevel(arg) > 0f ? 1 : 0;
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

    private interface Lookup {
        Object get(String key);
    }

    /**
     * One key: its count. Several keys joined by & or |, or key=VALUE terms: 1 when the test is met, else 0.
     */
    private static AchievementScript keys(String arg, Lookup lookup) {
        boolean all = arg.contains("&");
        boolean any = arg.contains("|");
        if (all && any) throw new IllegalArgumentException("do not mix & and | in " + arg);
        if (!all && !any && !arg.contains("=") && !arg.startsWith("!")) return spec -> count(lookup.get(arg));
        String[] terms = arg.split(all ? "&" : "[|]");
        for (String term : terms) {
            if (term.trim().isEmpty()) throw new IllegalArgumentException("empty key in " + arg);
        }
        return spec -> {
            for (String term : terms) {
                boolean met = term(term.trim(), lookup);
                if (all && !met) return 0;
                if (!all && met) return 1;
            }
            return all ? 1 : 0;
        };
    }

    /** "id:keys" split at the first colon; both parts needed. */
    private static String[] ownerAndKeys(String word, String arg) {
        int split = arg.indexOf(':');
        if (split <= 0 || split == arg.length() - 1)
            throw new IllegalArgumentException(word + " needs an id and a key, as " + word + ":id:$key");
        return new String[] {arg.substring(0, split).trim(), arg.substring(split + 1).trim()};
    }

    /**
     * key: set (counts 1 or more). key=VALUE: holds that text, or that number. key>=NUMBER: holds a number at
     * least that big. !key: not set.
     */
    private static boolean term(String term, Lookup lookup) {
        if (term.startsWith("!")) return !term(term.substring(1).trim(), lookup);
        int atLeast = term.indexOf(">=");
        if (atLeast > 0) {
            Object value = lookup.get(term.substring(0, atLeast).trim());
            try {
                double wanted = Double.parseDouble(term.substring(atLeast + 2).trim());
                if (value instanceof Number) return ((Number) value).doubleValue() >= wanted;
                return value != null && Double.parseDouble(String.valueOf(value).trim()) >= wanted;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        int eq = term.indexOf('=');
        if (eq < 0) return count(lookup.get(term)) > 0;
        Object value = lookup.get(term.substring(0, eq).trim());
        String wanted = term.substring(eq + 1).trim();
        if (value == null) return false;
        if (value instanceof Number) {
            try {
                return ((Number) value).doubleValue() == Double.parseDouble(wanted);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return String.valueOf(value).trim().equalsIgnoreCase(wanted);
    }

    /**
     * A flag counts as 1, false, null and the text "false" as 0, a number as itself, a list as its size.
     * (rules.csv stores some flags as the text "true" or "false".)
     */
    private static int count(Object value) {
        if (value == null || Boolean.FALSE.equals(value)) return 0;
        if (value instanceof String && (((String) value).trim().isEmpty() || ((String) value).trim().equalsIgnoreCase("false"))) return 0;
        if (value instanceof Number) return Math.max(0, ((Number) value).intValue());
        if (value instanceof Collection) return ((Collection<?>) value).size();
        if (value instanceof Map) return ((Map<?, ?>) value).size();
        return 1;
    }
}
