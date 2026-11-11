package achievementsrenewed;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;

import achievementsrenewed.api.AchievementSpec;

/**
 * Keeps this mod's intel entries out of the save file. They are taken out of the intel screen before a
 * save and put back after it, and rebuilt on game load: the "Achievement: name" entries from a list of
 * plain text kept in the save. (The list itself opens from the ability bar or the hotkey, not the intel screen.) A save made with this mod
 * holds no class of it and still loads without it.
 */
public final class IntelKeeper {

    private static final Logger LOG = Global.getLogger(IntelKeeper.class);
    /** Days an "Achievement: name" entry stays in the intel log. */
    private static final float NOTICE_DAYS = 30f;

    /** Entries taken out for a save, waiting to be put back. */
    private static final List<IntelInfoPlugin> STASHED = new ArrayList<>();

    private IntelKeeper() {}

    private static boolean ours(IntelInfoPlugin intel) {
        return intel instanceof AchievementNoticeIntel;
    }

    private static String entry(long doneAt, String id) {
        return doneAt + "|" + id;
    }

    /** On game load: the entries of achievements done lately in this save. */
    public static void restoreOnLoad() {
        STASHED.clear();
        IntelManagerAPI manager = Global.getSector().getIntelManager();
        for (IntelInfoPlugin intel : new ArrayList<>(manager.getIntel())) {
            if (ours(intel)) manager.removeIntel(intel);
        }

        Iterator<String> entries = SaveData.notices().iterator();
        while (entries.hasNext()) {
            String text = entries.next();
            try {
                int split = text.indexOf('|');
                long doneAt = Long.parseLong(text.substring(0, split));
                AchievementSpec spec = Catalog.get(text.substring(split + 1));
                if (Global.getSector().getClock().getElapsedDaysSince(doneAt) > NOTICE_DAYS) {
                    entries.remove();
                } else if (spec != null) {
                    addQuietly(manager, new AchievementNoticeIntel(spec, doneAt), doneAt, false);
                }
            } catch (Throwable t) {
                entries.remove();
            }
        }
    }

    /** Before a save: take every entry of this mod out of the intel screen. */
    public static void stash() {
        try {
            STASHED.clear();
            IntelManagerAPI manager = Global.getSector().getIntelManager();
            for (IntelInfoPlugin intel : new ArrayList<>(manager.getIntel())) {
                if (!ours(intel)) continue;
                STASHED.add(intel);
                manager.removeIntel(intel);
            }
        } catch (Throwable t) {
            LOG.error("Achievements: could not take the intel entries out before the save", t);
        }
    }

    /** After a save, or a failed one: put the entries back as they were. */
    public static void unstash() {
        try {
            IntelManagerAPI manager = Global.getSector().getIntelManager();
            for (IntelInfoPlugin intel : STASHED) {
                if (intel.isEnded()) continue;
                addQuietly(manager, intel, intel.getPlayerVisibleTimestamp(), intel.isNew());
            }
            STASHED.clear();
        } catch (Throwable t) {
            LOG.error("Achievements: could not put the intel entries back after the save", t);
        }
    }

    /** Adds an entry without a campaign message, keeping its date and whether it counts as new. */
    private static void addQuietly(IntelManagerAPI manager, IntelInfoPlugin intel, Long timestamp, boolean isNew) {
        manager.addIntel(intel, true);
        if (timestamp != null) intel.setPlayerVisibleTimestamp(timestamp);
        intel.setNew(isNew);
    }

    /**
     * An achievement was just done: its intel entry, with a campaign message unless that is switched off
     * or the caller asks for none.
     */
    public static void addNotice(AchievementSpec spec, boolean quiet) {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return;
        long now = Global.getSector().getClock().getTimestamp();
        SaveData.notices().add(entry(now, spec.id));
        Global.getSector().getIntelManager().addIntel(new AchievementNoticeIntel(spec, now), quiet || !Settings.showMessage());
    }

    /** The player deleted an entry, or it ran out: it does not come back on the next load. */
    public static void forget(AchievementNoticeIntel notice) {
        try {
            SaveData.notices().remove(entry(notice.doneAt(), notice.spec().id));
        } catch (Throwable ignored) {
            // The entry runs out on its own.
        }
    }

    /** Once a day: drop entries older than a month. */
    public static void prune() {
        try {
            IntelManagerAPI manager = Global.getSector().getIntelManager();
            for (IntelInfoPlugin intel : new ArrayList<>(manager.getIntel(AchievementNoticeIntel.class))) {
                AchievementNoticeIntel notice = (AchievementNoticeIntel) intel;
                if (Global.getSector().getClock().getElapsedDaysSince(notice.doneAt()) <= NOTICE_DAYS) continue;
                notice.endImmediately();
                manager.removeIntel(notice);
            }
        } catch (Throwable t) {
            LOG.error("Achievements: could not clear old intel entries", t);
        }
    }
}
