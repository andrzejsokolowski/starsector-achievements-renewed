package achievementsrenewed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;

import achievementsrenewed.api.AchievementScript;
import achievementsrenewed.api.AchievementSpec;
import achievementsrenewed.scripts.BuiltIn;

/**
 * Every achievement the game can offer: the rows of data/config/achievements_renewed/achievements.csv,
 * merged over all mods, plus the ones mods register from code. Rows whose required mods are off are left
 * out before their script class is loaded. A broken row is logged and skipped. The reward text of a row
 * whose rewardMod is not met is left empty (the rbr_* rows name Rule Book rules).
 *
 * The list is read the first time anything asks for it, so a mod that loads before this one can already
 * register achievements in its onApplicationLoad.
 */
public final class Catalog {

    private static final Logger LOG = Global.getLogger(Catalog.class);
    /** The game adds this to every row of a merged spreadsheet: the mod folder plus the file's path. */
    private static final String ROW_SOURCE = "fs_rowSource";

    private static final Map<String, AchievementSpec> SPECS = new LinkedHashMap<>();
    private static List<AchievementSpec> sorted = Collections.emptyList();
    private static boolean loaded;

    private Catalog() {}

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        int before = SPECS.size();
        try {
            JSONArray rows = Global.getSettings().getMergedSpreadsheetDataForMod("id", Ids.CSV, Ids.MOD_ID);
            List<ModSpecAPI> mods = Global.getSettings().getModManager().getEnabledModsCopy();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = str(row, "id");
                if (id.isEmpty() || id.startsWith("#")) continue;
                try {
                    AchievementSpec spec = parse(row, id, mods);
                    // An achievement registered from code before the list was read wins over the file.
                    if (spec != null && !SPECS.containsKey(id)) SPECS.put(id, spec);
                } catch (Throwable t) {
                    LOG.error("Achievements: " + id + " could not be read and is left out", t);
                }
            }
        } catch (Throwable t) {
            LOG.error("Achievements: could not read " + Ids.CSV, t);
        }
        resort();
        for (AchievementSpec spec : SPECS.values()) loadIcon(spec.icon);
        LOG.info("Achievements: " + (SPECS.size() - before) + " read from " + Ids.CSV + ", " + SPECS.size() + " in all");
    }

    private static AchievementSpec parse(JSONObject row, String id, List<ModSpecAPI> mods) {
        if (!modsEnabled(str(row, "requiredMod"))) return null;

        String name = str(row, "name");
        if (name.isEmpty()) throw new IllegalArgumentException("no name");
        String goalText = str(row, "goal");
        int goal = goalText.isEmpty() ? 1 : Integer.parseInt(goalText);
        if (goal < 1) throw new IllegalArgumentException("goal must be 1 or more");
        String stepText = str(row, "step");
        String orderText = str(row, "order");

        AchievementScript script = null;
        String scriptText = str(row, "script");
        if (!scriptText.isEmpty()) {
            script = BuiltIn.parse(scriptText);
            if (script == null) {
                Object made = Global.getSettings().getInstanceOfScript(scriptText);
                if (!(made instanceof AchievementScript))
                    throw new IllegalArgumentException(scriptText + " is not an AchievementScript");
                script = (AchievementScript) made;
            }
        }

        return AchievementSpec.builder(id, sourceMod(row, mods), name)
                .hint(str(row, "hint"))
                .howTo(str(row, "howTo"))
                .description(str(row, "description"))
                .icon(str(row, "icon"))
                .category(str(row, "category"))
                .goal(goal)
                .secret(isTrue(str(row, "secret")))
                .reward(modsEnabled(str(row, "rewardMod")) ? str(row, "reward") : "")
                .chain(str(row, "chain"), stepText.isEmpty() ? 0 : Integer.parseInt(stepText))
                .after(str(row, "after"))
                .script(script)
                .order(orderText.isEmpty() ? 1000f : Float.parseFloat(orderText))
                .tab(str(row, "tab"))
                .build();
    }

    /**
     * The mod a row belongs to: its mod column when filled in, otherwise the mod whose folder the row was
     * read from, otherwise this mod.
     */
    private static String sourceMod(JSONObject row, List<ModSpecAPI> mods) {
        String named = str(row, "mod");
        if (!named.isEmpty()) return named;
        String source = normalise(str(row, ROW_SOURCE));
        if (source.isEmpty()) return Ids.MOD_ID;
        String best = null;
        int bestLength = -1;
        for (ModSpecAPI mod : mods) {
            String path = normalise(mod.getPath());
            String folder = normalise(mod.getDirName());
            int length = -1;
            if (!path.isEmpty() && source.startsWith(path + "/")) length = path.length() + 1000;
            else if (!folder.isEmpty() && source.contains("/" + folder + "/data/")) length = folder.length();
            if (length > bestLength) {
                bestLength = length;
                best = mod.getId();
            }
        }
        return best != null ? best : Ids.MOD_ID;
    }

    private static String normalise(String path) {
        if (path == null) return "";
        String out = path.replace('\\', '/').toLowerCase(Locale.ROOT).trim();
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private static boolean isTrue(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        return t.equals("true") || t.equals("yes") || t.equals("1");
    }

    /** The 0.1.0 name of {@link #modsEnabled}, kept for anything compiled against it. */
    public static boolean anyModEnabled(String mods) {
        return modsEnabled(mods);
    }

    /**
     * Whether a requiredMod column is met. Empty: yes. "a|b": a or b is enabled. "a|b&c|d": (a or b) and
     * (c or d). An empty group counts as met.
     */
    public static boolean modsEnabled(String mods) {
        if (mods == null || mods.trim().isEmpty()) return true;
        for (String group : mods.split("&")) {
            if (group.trim().isEmpty()) continue;
            boolean any = false;
            for (String mod : group.split("[|]")) {
                if (!mod.trim().isEmpty() && Global.getSettings().getModManager().isModEnabled(mod.trim())) any = true;
            }
            if (!any) return false;
        }
        return true;
    }

    /** The bundled org.json returns the string "null" for a JSON null, so check isNull first. */
    private static String str(JSONObject row, String key) {
        if (!row.has(key) || row.isNull(key)) return "";
        String value = row.optString(key, "").trim();
        return value.equals("null") ? "" : value;
    }

    private static void loadIcon(String path) {
        try {
            Global.getSettings().loadTexture(path);
        } catch (Throwable ignored) {
            // A missing icon only shows as blank.
        }
    }

    /** The mod's display name, or its id when the mod is not known. */
    public static String modName(String modId) {
        try {
            ModSpecAPI mod = Global.getSettings().getModManager().getModSpec(modId);
            if (mod != null && mod.getName() != null && !mod.getName().trim().isEmpty()) return mod.getName().trim();
        } catch (Throwable ignored) {
            // Fall through to the id.
        }
        return modId == null || modId.isEmpty() ? "Other" : modId;
    }

    /**
     * List order: the vanilla tab first, then the other tabs by title, so no mod comes before another for
     * any reason but the title of its tab. Inside a tab: categories in the order of their first
     * achievement, then chains (and single achievements) in the order of their first member, the steps of
     * a chain in step order.
     */
    private static void resort() {
        List<AchievementSpec> list = new ArrayList<>(SPECS.values());
        Map<String, Float> categoryOrder = new HashMap<>();
        Map<String, Float> groupOrder = new HashMap<>();
        Map<String, String> tabs = new HashMap<>();
        for (AchievementSpec s : list) tabs.put(s.id, tabKey(s.tabName()));
        for (AchievementSpec s : list) {
            categoryOrder.merge(categoryKey(s, tabs), s.order, Math::min);
            groupOrder.merge(groupKey(s, tabs), s.order, Math::min);
        }
        list.sort(Comparator.comparing((AchievementSpec s) -> !s.isVanilla())
                .thenComparing(s -> tabs.get(s.id))
                .thenComparingDouble(s -> categoryOrder.get(categoryKey(s, tabs)))
                .thenComparing(s -> s.category)
                .thenComparingDouble(s -> groupOrder.get(groupKey(s, tabs)))
                .thenComparing(s -> s.chain.isEmpty() ? s.id : s.chain)
                .thenComparingInt(s -> s.step)
                .thenComparingDouble(s -> s.order)
                .thenComparing(s -> s.name));
        sorted = Collections.unmodifiableList(list);
    }

    /** Tabs are told apart by title, whatever its case. */
    public static String tabKey(String tabName) {
        return tabName == null ? "" : tabName.trim().toLowerCase(Locale.ROOT);
    }

    private static String categoryKey(AchievementSpec s, Map<String, String> tabs) {
        return tabs.get(s.id) + "\n" + s.category;
    }

    private static String groupKey(AchievementSpec s, Map<String, String> tabs) {
        return categoryKey(s, tabs) + "\n" + (s.chain.isEmpty() ? "id:" + s.id : "chain:" + s.chain);
    }

    public static void register(AchievementSpec spec) {
        if (spec == null) return;
        ensureLoaded();
        if (SPECS.containsKey(spec.id)) LOG.info("Achievements: " + spec.id + " registered again, replacing the earlier one");
        SPECS.put(spec.id, spec);
        loadIcon(spec.icon);
        resort();
    }

    public static AchievementSpec get(String id) {
        ensureLoaded();
        return SPECS.get(id);
    }

    public static List<AchievementSpec> all() {
        ensureLoaded();
        return sorted;
    }

    public static List<AchievementSpec> ofMod(String modId) {
        List<AchievementSpec> out = new ArrayList<>();
        for (AchievementSpec spec : all()) if (spec.modId.equals(modId)) out.add(spec);
        return Collections.unmodifiableList(out);
    }

    /** Tab titles in list order. Two spellings of one title (other case) count as one tab, under the first. */
    public static List<String> tabs() {
        Map<String, String> out = new LinkedHashMap<>();
        for (AchievementSpec spec : all()) out.putIfAbsent(tabKey(spec.tabName()), spec.tabName());
        return Collections.unmodifiableList(new ArrayList<>(out.values()));
    }

    public static List<AchievementSpec> ofTab(String tabName) {
        List<AchievementSpec> out = new ArrayList<>();
        if (tabName == null) return out;
        boolean vanilla = tabName.trim().equalsIgnoreCase(AchievementSpec.VANILLA_TAB);
        String key = tabKey(tabName);
        for (AchievementSpec spec : all()) {
            if (vanilla ? spec.isVanilla() : tabKey(spec.tabName()).equals(key)) out.add(spec);
        }
        return Collections.unmodifiableList(out);
    }

    public static List<AchievementSpec> chain(String chain) {
        List<AchievementSpec> out = new ArrayList<>();
        if (chain == null || chain.isEmpty()) return out;
        for (AchievementSpec spec : all()) if (spec.chain.equals(chain)) out.add(spec);
        out.sort(Comparator.comparingInt((AchievementSpec s) -> s.step).thenComparingDouble(s -> s.order));
        return Collections.unmodifiableList(out);
    }
}
