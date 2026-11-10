package achievementsrenewed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import achievementsrenewed.api.AchievementListener;
import achievementsrenewed.api.AchievementSpec;

/**
 * Checks achievements and handles one being done: the entry in saves/common, the listeners, the counts in
 * sector memory, and the intel entry with its campaign message.
 */
public final class Tracker {

    private static final Logger LOG = Global.getLogger(Tracker.class);

    private static final List<AchievementListener> LISTENERS = new ArrayList<>();
    /** Counts of scripted achievements as of the last check, for this game session only. */
    private static final Map<String, Integer> POLLED = new HashMap<>();
    /** Scripts that threw, so the log gets one line per achievement and not one per day. */
    private static final Set<String> FAILED = new HashSet<>();
    private static boolean checking;

    private Tracker() {}

    // ---- listeners ----

    public static void addListener(AchievementListener listener) {
        if (listener == null) return;
        // Mods add theirs on every game load; a second instance of the same class replaces the first.
        LISTENERS.removeIf(known -> known == listener || known.getClass() == listener.getClass());
        LISTENERS.add(listener);
    }

    public static void removeListener(AchievementListener listener) {
        LISTENERS.remove(listener);
    }

    // ---- checks ----

    public static void onGameLoad() {
        POLLED.clear();
        FAILED.clear();
        for (AchievementSpec spec : Catalog.all()) {
            if (spec.script == null) continue;
            try {
                spec.script.onGameLoad(spec);
            } catch (Throwable t) {
                LOG.error("Achievements: script of " + spec.id + " failed on game load", t);
            }
        }
        // The first check runs on the first frame (CheckScript), after every mod's onGameLoad.
        publishCounts();
    }

    /** Polls every script and marks what reached its goal. */
    public static void check() {
        if (Global.getSector() == null || checking) return;
        checking = true;
        try {
            for (AchievementSpec spec : Catalog.all()) {
                int count = spec.script != null ? poll(spec) : SaveData.get(Ids.DATA_COUNTS, spec.id);
                if (count >= spec.goal) complete(spec);
            }
        } finally {
            checking = false;
        }
    }

    private static int poll(AchievementSpec spec) {
        int count = 0;
        try {
            count = Math.max(0, spec.script.count(spec));
        } catch (Throwable t) {
            if (FAILED.add(spec.id)) LOG.error("Achievements: script of " + spec.id + " failed", t);
        }
        POLLED.put(spec.id, count);
        return count;
    }

    /** Current count in this save: the script's count as of the last check, or the count added from code. */
    public static int progress(AchievementSpec spec) {
        if (Global.getSector() == null) return 0;
        if (spec.script == null) return SaveData.get(Ids.DATA_COUNTS, spec.id);
        Integer known = POLLED.get(spec.id);
        return known != null ? known : poll(spec);
    }

    public static void addProgress(AchievementSpec spec, int amount) {
        if (amount <= 0 || spec.script != null || Global.getSector() == null) return;
        int count = SaveData.add(Ids.DATA_COUNTS, spec.id, amount);
        if (count >= spec.goal) complete(spec);
    }

    // ---- completion ----

    /** Marks an achievement done. Returns true when it is new. */
    public static boolean complete(AchievementSpec spec) {
        if (!Store.markDone(spec.id)) return false;
        LOG.info("Achievements: " + spec.id + " done");
        publishCounts();
        for (AchievementListener listener : new ArrayList<>(LISTENERS)) {
            try {
                listener.achievementDone(spec);
            } catch (Throwable t) {
                LOG.error("Achievements: a listener failed on " + spec.id, t);
            }
        }
        try {
            IntelKeeper.addNotice(spec);
        } catch (Throwable t) {
            LOG.error("Achievements: could not add the intel entry for " + spec.id, t);
        }
        return true;
    }

    public static int doneCount() {
        int done = 0;
        for (AchievementSpec spec : Catalog.all()) if (Store.isDone(spec.id)) done++;
        return done;
    }

    /** Writes $acr_doneCount and $acr_totalCount to sector memory, for other mods and rules.csv. */
    public static void publishCounts() {
        try {
            if (Global.getSector() == null) return;
            MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
            memory.set(Ids.MEMORY_DONE, doneCount());
            memory.set(Ids.MEMORY_TOTAL, Catalog.all().size());
        } catch (Throwable t) {
            LOG.error("Achievements: could not write the counts to sector memory", t);
        }
    }
}
