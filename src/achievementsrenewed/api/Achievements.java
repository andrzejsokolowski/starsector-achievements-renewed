package achievementsrenewed.api;

import java.util.List;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;

import achievementsrenewed.Catalog;
import achievementsrenewed.Settings;
import achievementsrenewed.Store;
import achievementsrenewed.Tracker;
import achievementsrenewed.ui.AchievementsDialog;

/**
 * The API other mods use. Achievements come from data/config/achievements_renewed/achievements.csv in any
 * mod (see the README), or from {@link #register}. Done achievements are kept in saves/common, so they
 * count for every save; counts are kept per save.
 */
public final class Achievements {

    private Achievements() {}

    /** Done in any save (kept in saves/common). */
    public static boolean isDone(String id) {
        return id != null && Store.isDone(id);
    }

    /** Current count in this save, as of the last check; 0 if unknown. */
    public static int progress(String id) {
        AchievementSpec spec = get(id);
        return spec == null ? 0 : Tracker.progress(spec);
    }

    /** Marks an achievement done from code. True when it is newly done. Unknown ids are ignored. */
    public static boolean complete(String id) {
        AchievementSpec spec = get(id);
        return spec != null && Tracker.complete(spec);
    }

    /**
     * Adds to the count of an achievement that has no script, in this save. It is done when the count
     * reaches its goal.
     */
    public static void addProgress(String id, int amount) {
        AchievementSpec spec = get(id);
        if (spec != null) Tracker.addProgress(spec, amount);
    }

    /** Null if unknown. */
    public static AchievementSpec get(String id) {
        return id == null ? null : Catalog.get(id);
    }

    /** Only achievements whose required mod is on, in list order. */
    public static List<AchievementSpec> all() {
        return Catalog.all();
    }

    public static List<AchievementSpec> ofMod(String modId) {
        return Catalog.ofMod(modId);
    }

    /** Not saved; mods re-add on game load. Adding a listener of a class already present replaces it. */
    public static void addListener(AchievementListener listener) {
        Tracker.addListener(listener);
    }

    public static void removeListener(AchievementListener listener) {
        Tracker.removeListener(listener);
    }

    /** The LunaLib setting "Show how to get achievements". */
    public static boolean showHowTo() {
        return Settings.showHowTo();
    }

    /** Opens the achievements screen. Campaign only; does nothing while another dialog is open. */
    public static void openList() {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return;
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getPlayerFleet() == null) return;
        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null || ui.isShowingDialog()) return;
        ui.showInteractionDialog(new AchievementsDialog(), sector.getPlayerFleet());
    }

    // ---- beyond the fixed contract ----

    /** Opens the achievements screen from an intel entry's button. */
    public static void openList(IntelUIAPI ui) {
        SectorAPI sector = Global.getSector();
        if (ui == null || sector == null || sector.getPlayerFleet() == null) return;
        ui.showDialog(sector.getPlayerFleet(), new AchievementsDialog());
    }

    /**
     * Adds an achievement from code, for mods that build theirs at run time. Call it in onApplicationLoad.
     * An achievement with the same id is replaced.
     */
    public static void register(AchievementSpec spec) {
        Catalog.register(spec);
    }

    /** The achievements of one chain, in step order. */
    public static List<AchievementSpec> chain(String chain) {
        return Catalog.chain(chain);
    }

    /** The real-world date an achievement was done, as yyyy-MM-dd; null when it is not done. */
    public static String doneDate(String id) {
        return id == null ? null : Store.date(id);
    }

    /** How many of the achievements in {@link #all()} are done. Also in sector memory as $acr_doneCount. */
    public static int doneCount() {
        return Tracker.doneCount();
    }

    /** The size of {@link #all()}. Also in sector memory as $acr_totalCount. */
    public static int totalCount() {
        return Catalog.all().size();
    }

    /** Runs the checks now instead of waiting for the next day. */
    public static void check() {
        Tracker.check();
    }
}
